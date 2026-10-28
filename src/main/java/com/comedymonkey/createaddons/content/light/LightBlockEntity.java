package com.comedymonkey.createaddons.content.light;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.line.PowerLines;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.comedymonkey.createaddons.registry.ModSounds;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * An Electric Light's power and looks. It never ticks: its store runs down as time passes and is worked out only when
 * something asks (the network delivering, its scheduled tick), and a scheduled tick wakes it when the store is due to
 * run out. Lit, it draws its kind's FE a tick at its brightness step; with nothing coming in it stays lit while its
 * store lasts (2 s by default, covering the network's once-a-second updates), then flickers for a second and goes dark
 * (P3). Switched off (its circuit reaches no power at all) it goes out at once. Coming on, lights wait up to a second
 * at random, so a street lights up as a ripple (P5); the High-Bay and Yard Light warm up (P4), drawn by the renderer.
 */
public class LightBlockEntity extends BlockEntity implements PowerLines.Lamp, IHaveGoggleInformation {
	/** Ticks a flicker lasts, going out or coming back. */
	public static final int FLICKER_TICKS = 20;
	/** Ticks the sodium lamps take to warm up. */
	public static final int WARM_TICKS = 80;

	public enum Phase {
		DARK, LIT,
		/** Starved: flickering, about to go dark. */
		FADING
	}

	/**
	 * Client hooks: a flicker or a warm-up just started here (plays its sound, capped nearby), or it went between dark
	 * and lit (its visual is made again: lit lights have one, dark ones don't).
	 */
	public static java.util.function.ObjIntConsumer<LightBlockEntity> clientCue = (be, cue) -> {};
	public static final int CUE_FLICKER = 1, CUE_WARM = 2, CUE_LIT = 3;
	/** Client hook: it was loaded, or its looks changed (the coloured light it casts with Veil follows). */
	public static java.util.function.Consumer<LightBlockEntity> clientLooks = be -> {};

	private int step;
	private Phase phase = Phase.DARK;
	/** It last went out because it starved: it waits until its store is full before it comes back (no brownout cycling). */
	private boolean starved;
	private double energy;
	/** Game time its store was last worked out; -1 until it is (after loading, so no time passes while unloaded). */
	private long settled = -1;
	/** When it may come on (a random wait, so a street ripples); MAX when it isn't waiting to. */
	private long onAt = Long.MAX_VALUE;
	/** When its flicker began, and which way it goes; when it began warming. */
	private long flickerAt = Long.MIN_VALUE / 2, warmAt = Long.MIN_VALUE / 2;
	private boolean flickerOn;
	/**
	 * When a network last said its circuit reaches power, and which network; when one last said it reaches none (it was
	 * switched off). A light can touch several lines: it's off only while the one that says so is all that feeds it.
	 */
	private long sourceAt = Long.MIN_VALUE / 2, cutAt = Long.MIN_VALUE / 2;
	@Nullable
	private Object sourceFrom;
	/** A Floodlight's target, and the spot of light it put where its beam lands (Floodlights). */
	@Nullable
	private BlockPos aim, spot;
	/** The face of its target it's aimed at. */
	@Nullable
	private Direction aimFace;
	/** On the client: where a Floodlight's yoke and head were last drawn turned to, and when (game time, part ticks). */
	private float yawShown, pitchShown;
	private double aimShownAt = Double.NaN;
	/** On the client: switched off (for the goggles). */
	private boolean cutShown;

	private final IEnergyStorage storage = new IEnergyStorage() {
		@Override
		public int receiveEnergy(int max, boolean simulate) {
			return receive(max, simulate);
		}

		@Override
		public int extractEnergy(int max, boolean simulate) {
			return 0;
		}

		@Override
		public int getEnergyStored() {
			settle();
			return (int) Math.floor(energy);
		}

		@Override
		public int getMaxEnergyStored() {
			return (int) Math.ceil(capacity());
		}

		@Override
		public boolean canExtract() {
			return false;
		}

		@Override
		public boolean canReceive() {
			return true;
		}
	};

	public LightBlockEntity(BlockPos pos, BlockState state) {
		this(ModBlockEntities.LIGHT.get(), pos, state);
	}

	protected LightBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	/** What its block draws and whether it warms up (a light, or a piece of a Light Strip). */
	protected LitBlock lit() {
		return getBlockState().getBlock() instanceof LitBlock b ? b : step -> 0;
	}

	public IEnergyStorage energy() {
		return storage;
	}

	public LightKind kind() {
		return getBlockState().getBlock() instanceof LightBlock b ? b.kind() : LightKind.BULKHEAD;
	}

	/** Its glow's dye, or null: kept in its block state (LightBlock.COLOR). */
	@Nullable
	public DyeColor dye() {
		BlockState state = getBlockState();
		return state.hasProperty(LightBlock.COLOR) ? state.getValue(LightBlock.COLOR).dye() : null;
	}

	public int step() {
		return step;
	}

	public Phase phase() {
		return phase;
	}

	/** Switched off (on the server, by the networks' word; on the client, as last synced). */
	public boolean isCut() {
		return level == null || level.isClientSide ? cutShown : cut(level.getGameTime());
	}

	public long flickerAt() {
		return flickerAt;
	}

	public boolean flickerOn() {
		return flickerOn;
	}

	public long warmAt() {
		return warmAt;
	}

	/** FE a tick it draws while lit. */
	public double draw() {
		return lit().draw(step);
	}

	private double capacity() {
		return Math.max(4, draw() * 20 * CMConfig.SERVER.lightBufferSeconds());
	}

	// ---------- power ----------

	/** Ticks a switch-off holds: told again each second while it lasts, so a light that's since fed another way recovers. */
	private static final int CUT_HOLD = 40;
	/** Longest a lit light goes without being looked at while its chunk runs (deliveries each tick, else its checks). */
	private static final int MAX_GAP = 45;
	/** Longest between its checks while lit: a change of draw or of power is noticed within a second. */
	private static final int CHECK = 20;

	/** Its circuit reaches no power at all, by the last word (and no other line has vouched for it since). */
	private boolean cut(long now) {
		return cutAt > sourceAt && now - cutAt <= CUT_HOLD;
	}

	/**
	 * Works out what's left in its store: lit (or flickering) it has drawn since it was last worked out, unless that
	 * was longer ago than its chunk can have been running (it waited in a chunk out of simulation range: no draw then).
	 */
	private void settle() {
		if (level == null)
			return;
		long now = level.getGameTime(), dt = now - settled;
		if (settled >= 0 && phase != Phase.DARK && dt <= MAX_GAP)
			energy = Math.max(0, energy - draw() * dt);
		settled = now;
	}

	/** Enough in store to come on: half full, or full after it starved. */
	private boolean ready() {
		return energy >= (starved ? capacity() - 1 : capacity() / 2);
	}

	protected int receive(int max, boolean simulate) {
		if (level == null || level.isClientSide)
			return 0;
		long now = level.getGameTime();
		if (cut(now))
			return 0;
		settle();
		int took = (int) Math.max(0, Math.min(max, Math.floor(capacity() - energy)));
		if (!simulate) {
			energy += took;
			if (phase == Phase.DARK && ready()) {
				// power: come on after a short random wait (the street's ripple)
				if (onAt == Long.MAX_VALUE)
					onAt = now + 1 + level.random.nextInt(20);
				if (now >= onAt)
					lightUp(starved);
				else
					schedule((int) (onAt - now));
			}
		}
		return took;
	}

	/** A network's word, once a second: does its circuit reach any power at all? */
	@Override
	public void supplied(boolean source, Object network) {
		if (level == null || level.isClientSide)
			return;
		long now = level.getGameTime();
		if (source) {
			sourceAt = now;
			sourceFrom = network;
			return;
		}
		// another line feeds it (a different network said so within the last second): this one doesn't switch it off
		if (sourceFrom != network && now - sourceAt <= 22)
			return;
		boolean was = cut(now);
		cutAt = now;
		if (was)
			return;
		// switched off: out at once, no flicker, and it comes back on without one
		settle();
		energy = 0;
		starved = false;
		onAt = Long.MAX_VALUE;
		if (phase != Phase.DARK) {
			phase = Phase.DARK;
			show(false);
		}
		sync();
	}

	@Override
	public void onLoad() {
		super.onLoad();
		// moved, carried or loaded: look at itself next tick (it has no tick waiting if it was carried in lit)
		if (level != null && !level.isClientSide)
			schedule(1);
		else if (level != null)
			clientLooks.accept(this);
	}

	/** Its scheduled tick: comes on, notices its store has run out or it was switched off, or ends its flicker. */
	void wake() {
		if (level == null)
			return;
		settle();
		long now = level.getGameTime();
		boolean off = cut(now);
		switch (phase) {
			case DARK -> {
				show(false);
				if (!off && ready()) {
					if (onAt == Long.MAX_VALUE)
						onAt = now + 1 + level.random.nextInt(20);
					if (now >= onAt)
						lightUp(starved);
					else
						schedule((int) (onAt - now));
				}
			}
			case LIT -> {
				if (off)
					goDark(false);
				else if (energy <= 0) {
					// starved: a second's flicker, then dark (the world's light changes once, at the end: P28)
					phase = Phase.FADING;
					flickerAt = now;
					flickerOn = false;
					starved = true;
					schedule(FLICKER_TICKS);
					sync();
				} else {
					show(true);
					scheduleCheck();
				}
			}
			case FADING -> {
				if (off)
					goDark(false);
				else if (now < flickerAt + FLICKER_TICKS)
					schedule((int) (flickerAt + FLICKER_TICKS - now));
				else if (ready())
					lightUp(true);
				else
					goDark(true);
			}
		}
	}

	private void lightUp(boolean flicker) {
		long now = level.getGameTime();
		boolean fromDark = phase == Phase.DARK;
		phase = Phase.LIT;
		onAt = Long.MAX_VALUE;
		starved = false;
		if (flicker) {
			flickerAt = now;
			flickerOn = true;
		}
		if (fromDark && lit().warms())
			warmAt = now;
		show(true);
		scheduleCheck();
		sync();
	}

	private void goDark(boolean starved) {
		phase = Phase.DARK;
		this.starved = starved;
		onAt = Long.MAX_VALUE;
		show(false);
		sync();
	}

	/** The next check while lit: when its store runs out, and at most a second away. */
	private void scheduleCheck() {
		double d = draw();
		schedule(d <= 0 ? CHECK : (int) Math.max(1, Math.min(CHECK, Math.floor(energy / d))));
	}

	private void schedule(int delay) {
		if (level != null && !level.getBlockTicks().hasScheduledTick(worldPosition, getBlockState().getBlock()))
			level.scheduleTick(worldPosition, getBlockState().getBlock(), delay);
	}

	/** Lit at its brightness step, or dark: the light it casts on the world. */
	private void show(boolean lit) {
		setState(LightBlock.LIGHT, lit ? step + 1 : 0);
		shown(lit);
	}

	/**
	 * Lit or dark, as each wake finds it (at least once a second while lit): what it lights besides itself follows. A
	 * Floodlight's spot goes where its beam lands now while it's lit, none while it's dark.
	 */
	protected void shown(boolean lit) {
		if (kind() == LightKind.FLOODLIGHT && !isRemoved())
			Floodlights.beam(this, lit);
	}

	/** A remote spot of light at p is this light's (LightSpotBlock asks, to know it's still wanted). */
	public boolean ownsSpot(BlockPos p) {
		return p.equals(spot);
	}

	private <T extends Comparable<T>> void setState(net.minecraft.world.level.block.state.properties.Property<T> property, T value) {
		// never from a light that's gone (an energy handler kept by another mod): it would put its block back
		if (level == null || isRemoved())
			return;
		BlockState state = level.getBlockState(worldPosition);
		if (state.getBlock() != getBlockState().getBlock())
			return;
		// clients only: its light or colour changes, but its neighbours (the wire it's fed by) needn't look again
		if (level != null && state.hasProperty(property) && !state.getValue(property).equals(value))
			level.setBlock(worldPosition, state.setValue(property, value),
				net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);
	}

	// ---------- hands ----------

	public void setDye(@Nullable DyeColor dye) {
		setState(LightBlock.COLOR, LightDye.of(dye));
		// its spots of light (a Floodlight's, a Festoon's) take the colour too
		if (phase == Phase.LIT)
			show(true);
	}

	// ---------- the Floodlight ----------

	public @Nullable BlockPos aim() {
		return aim;
	}

	public @Nullable BlockPos spot() {
		return spot;
	}

	/** Aimed at a block (Floodlights.aim has checked it's in range and sight): it turns there, and its spot moves. */
	public @Nullable Direction aimFace() {
		return aimFace;
	}

	void setAim(BlockPos target, Direction face) {
		aim = target.immutable();
		aimFace = face;
		if (phase != Phase.DARK)
			show(true);
		sync();
	}

	void setSpot(@Nullable BlockPos at) {
		if (java.util.Objects.equals(spot, at))
			return;
		spot = at;
		sync();
	}

	/**
	 * On the client: the yaw and pitch a Floodlight's yoke and head are drawn at, easing toward {@code want} over about
	 * half a second (the brief), so a re-aimed light swings round.
	 */
	public float[] aimShown(long time, float pt, float yaw, float pitch) {
		double t = time + (double) pt;
		if (Double.isNaN(aimShownAt)) {
			yawShown = yaw;
			pitchShown = pitch;
		} else {
			float k = (float) (1 - Math.exp(-Math.max(0, t - aimShownAt) / 3.0));
			yawShown += net.minecraft.util.Mth.wrapDegrees(yaw - yawShown) * k;
			pitchShown += (pitch - pitchShown) * k;
		}
		aimShownAt = t;
		return new float[] {yawShown, pitchShown};
	}

	public float yawShown() {
		return yawShown;
	}

	public float pitchShown() {
		return pitchShown;
	}

	/** The wrench: the next brightness step (D22), with a click pitched by the step (Full highest). */
	public void stepBrightness() {
		stepTo((step + 1) % LightKind.STEPS.length, true);
	}

	/** To a brightness step, with the step's click or quietly (the rest of a run stepped together). */
	public void stepTo(int to, boolean sound) {
		if (level == null)
			return;
		settle();
		step = Math.floorMod(to, LightKind.STEPS.length);
		energy = Math.min(energy, capacity());
		if (phase == Phase.LIT)
			show(true);
		if (sound)
			level.playSound(null, worldPosition, ModSounds.light("brightness_step"), SoundSource.BLOCKS, 0.8f, 1.2f - 0.1f * step);
		schedule(1);
		sync();
	}

	// ---------- saving and syncing ----------

	private void sync() {
		if (level != null && !level.isClientSide) {
			setChanged();
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		writeLooks(tag);
		settle();
		tag.putDouble("Energy", energy);
		tag.putBoolean("Starved", starved);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		readLooks(tag);
		energy = tag.getDouble("Energy");
		starved = tag.getBoolean("Starved");
		settled = -1;
	}

	private void writeLooks(CompoundTag tag) {
		tag.putInt("Step", step);
		tag.putInt("Phase", phase.ordinal());
		tag.putLong("FlickerAt", flickerAt);
		tag.putBoolean("FlickerOn", flickerOn);
		tag.putLong("WarmAt", warmAt);
		if (aim != null)
			tag.putLong("Aim", aim.asLong());
		if (aimFace != null)
			tag.putInt("AimFace", aimFace.get3DDataValue());
		if (spot != null)
			tag.putLong("Spot", spot.asLong());
	}

	/** The Warning Beacon's rotor: a turn a second (D16, brief). */
	private static final float SPIN_PER_TICK = 18;
	/** On the client: where the Warning Beacon's rotor stopped, when it started turning again (-1: still), and where it was last drawn. */
	private float spinRest, spinShown;
	private long spinFrom = -1;

	/** On the client: the Warning Beacon's rotor angle this frame (degrees), turning while it's lit, else still where it stopped. */
	public float spin(long time, float pt, boolean turning) {
		if (turning) {
			if (spinFrom < 0)
				spinFrom = time;
			// (a whole turn is 20 ticks: kept small so a float stays exact)
			spinShown = spinRest + ((time - spinFrom) % 20 + pt) * SPIN_PER_TICK;
		} else if (spinFrom >= 0) {
			spinRest = spinShown % 360;
			spinFrom = -1;
		}
		return spinShown;
	}

	/** On the client: the dye it was last seen with, so a re-dyed lit light has its coloured light worked out again. */
	@Nullable
	private DyeColor dyeShown;

	private void readLooks(CompoundTag tag) {
		long oldFlicker = flickerAt, oldWarm = warmAt;
		boolean wasDark = phase == Phase.DARK;
		step = Math.floorMod(tag.getInt("Step"), LightKind.STEPS.length);
		phase = Phase.values()[Math.floorMod(tag.getInt("Phase"), Phase.values().length)];
		flickerAt = tag.contains("FlickerAt") ? tag.getLong("FlickerAt") : Long.MIN_VALUE / 2;
		flickerOn = tag.getBoolean("FlickerOn");
		warmAt = tag.contains("WarmAt") ? tag.getLong("WarmAt") : Long.MIN_VALUE / 2;
		aim = tag.contains("Aim") ? BlockPos.of(tag.getLong("Aim")) : null;
		aimFace = tag.contains("AimFace") ? Direction.from3DDataValue(tag.getInt("AimFace")) : null;
		spot = tag.contains("Spot") ? BlockPos.of(tag.getLong("Spot")) : null;
		if (level != null && level.isClientSide) {
			long now = level.getGameTime();
			if (flickerAt != oldFlicker && now - flickerAt < FLICKER_TICKS)
				clientCue.accept(this, CUE_FLICKER);
			if (warmAt != oldWarm && now - warmAt < WARM_TICKS)
				clientCue.accept(this, CUE_WARM);
			if (wasDark != (phase == Phase.DARK))
				clientCue.accept(this, CUE_LIT);
			// re-dyed while lit: a colour change alone isn't a change of light to the light engine, so a coloured-light
			// mod (Colorful Lighting) is asked to look at it again
			DyeColor dye = dye();
			if (dye != dyeShown && phase != Phase.DARK)
				level.getChunkSource().getLightEngine().checkBlock(worldPosition);
			dyeShown = dye;
			clientLooks.accept(this);
		}
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		writeLooks(tag);
		tag.putBoolean("Cut", level != null && cut(level.getGameTime()));
		return tag;
	}

	@Override
	public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
		readLooks(tag);
		cutShown = tag.getBoolean("Cut");
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
		handleUpdateTag(pkt.getTag(), registries);
	}

	// ---------- goggles ----------

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
		lang().translate("gui.goggles.light").forGoggles(tooltip);
		String state = phase == Phase.LIT ? "lit" : phase == Phase.FADING ? "starved" : isCut() ? "switched_off" : "no_power";
		lang().translate("gui.goggles.light." + state).style(phase == Phase.LIT ? ChatFormatting.GOLD : ChatFormatting.GRAY).forGoggles(tooltip, 1);
		lang().translate("gui.goggles.light.draw", String.format(Locale.ROOT, "%.2f", draw()).replaceAll("\\.?0+$", ""))
			.style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		lang().translate("gui.goggles.light.step." + step).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		DyeColor dye = dye();
		Component colour = dye == null ? Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.goggles.light.plain")
			: Component.translatable("color.minecraft." + dye.getName());
		lang().translate("gui.goggles.light.colour", colour).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		if (kind() == LightKind.FLOODLIGHT) {
			if (aim == null)
				lang().translate("gui.goggles.light.not_aimed").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
			else
				lang().translate("gui.goggles.light.aimed", aim.getX(), aim.getY(), aim.getZ()).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		}
		return true;
	}

	private static LangBuilder lang() {
		return new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID);
	}
}
