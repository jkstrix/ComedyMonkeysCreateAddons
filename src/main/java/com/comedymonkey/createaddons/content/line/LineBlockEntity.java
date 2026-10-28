package com.comedymonkey.createaddons.content.line;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.registry.ModDataComponents;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * What a piece of line (fluid or power) remembers beyond its blockstate: its dye, the joints a wrench changed, the one
 * fitting clipped onto it and that fitting's settings, the block it's encased in, and the Meter's reading. Every change
 * that affects where things flow is saved, sent to clients and makes the line's network be worked out again.
 */
public abstract class LineBlockEntity extends BlockEntity implements IHaveGoggleInformation,
	com.simibubi.create.api.contraption.transformable.TransformableBlockEntity {
	@Nullable
	private DyeColor dye;
	private final Map<Direction, Joint> joints = new EnumMap<>(Direction.class);
	private Fitting fitting = Fitting.NONE;
	/** The Intake's side (the tank it faces), or the side an in-line fitting is turned towards (its axis). */
	private Direction fittingSide = Direction.NORTH;
	private FluidStack filter = FluidStack.EMPTY;
	/** The Valve's wheel, in tenths open (10: fully open). */
	private int valveOpen = 10;
	/** The Breaker's lever. */
	private boolean leverOn = true;
	/** A redstone signal shuts a Valve or a Breaker. */
	private boolean powered;
	private SplitMode splitMode = SplitMode.EVEN;
	@Nullable
	private BlockState encasing;
	/** The Meter's last reading: the amount through it a tick (mB or FE), and as a share of the line's rate. */
	private int meterFlow;
	private float meterLevel;
	private long meterSynced;
	/** The reading clients last got. */
	private float meterSent;
	/** Client only: where the fitting's moving part (wheel, needle, dial) was last drawn, eased towards its setting, and when. */
	private float shown = Float.NaN;
	private long shownAt;
	/** A Valve or Filter with fluid against it that it won't let past: its signal flag is up. */
	private boolean holding;
	/** Client only: where the signal flag was last drawn (degrees up from hanging), and when. */
	private float flag = Float.NaN;
	private long flagAt;

	protected LineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public abstract LineTier tier();

	/** Most a tick of this line's kind: mB for fluid lines, FE for power lines. */
	public abstract int rate();

	/** The blockstate's sides need working out again. */
	protected abstract void refreshState();

	/** Where things flow changed: the network of this line (and its neighbours') must be found again. */
	protected abstract void networkChanged(ServerLevel level);

	/** It loaded: by default, its network is worked out again. */
	protected void networkLoaded(ServerLevel level) {
		networkChanged(level);
	}

	/** Its chunk unloaded: by default, its network is worked out again without it. */
	protected void networkUnloaded(ServerLevel level) {
		networkChanged(level);
	}

	// ---------- reading ----------

	@Nullable
	public DyeColor dye() {
		return dye;
	}

	public Joint joint(Direction d) {
		return joints.getOrDefault(d, Joint.AUTO);
	}

	public boolean isCut(Direction d) {
		return joint(d) == Joint.CUT;
	}

	public Fitting fitting() {
		return fitting;
	}

	/** Turned with a contraption or schematic: the fitting's side and the wrenched joints turn with the block. */
	@Override
	public void transform(BlockEntity be, com.simibubi.create.content.contraptions.StructureTransform transform) {
		fittingSide = transform.rotateFacing(transform.mirrorFacing(fittingSide));
		Map<Direction, Joint> turned = new EnumMap<>(Direction.class);
		joints.forEach((d, j) -> turned.put(transform.rotateFacing(transform.mirrorFacing(d)), j));
		joints.clear();
		joints.putAll(turned);
		setChanged();
	}

	public Direction fittingSide() {
		return fittingSide;
	}

	@Nullable
	public Direction intakeSide() {
		return fitting == Fitting.INTAKE ? fittingSide : null;
	}

	public FluidStack filter() {
		return filter;
	}

	public int valveOpen() {
		return valveOpen;
	}

	public boolean leverOn() {
		return leverOn;
	}

	public boolean powered() {
		return powered;
	}

	public SplitMode splitMode() {
		return splitMode;
	}

	@Nullable
	public BlockState encasing() {
		return encasing;
	}

	public int meterFlow() {
		return meterFlow;
	}

	public float meterLevel() {
		return meterLevel;
	}

	/**
	 * Client only: the moving part eased towards {@code target}, closing {@code rate} of the gap every 60th of a second
	 * whatever the frame rate, and settling once it's close.
	 */
	public float easeShown(float target, float rate) {
		long now = System.nanoTime();
		shown = ease(shown, shownAt, now, target, rate);
		shownAt = now;
		return shown;
	}

	/** Client only: the signal flag eased up (90) or down (0), the same way. */
	public float easeFlag(float target, float rate) {
		long now = System.nanoTime();
		flag = ease(flag, flagAt, now, target, rate);
		flagAt = now;
		return flag;
	}

	private static float ease(float from, long then, long now, float target, float rate) {
		if (Float.isNaN(from))
			return target;
		float frames = Math.min(6f, (now - then) / 16_666_667f);
		float next = from + (target - from) * (1 - (float) Math.pow(1 - rate, frames));
		return Math.abs(next - target) < 0.05f ? target : next;
	}

	public boolean holding() {
		return holding;
	}

	/** Set by the line's network: this Valve or Filter has fluid against it that it won't let past. */
	void setHolding(boolean holding) {
		if (this.holding == holding)
			return;
		this.holding = holding;
		sync();
	}

	/** How much of the line's rate may pass this block a tick: a shut Valve or an open Breaker lets nothing through. */
	public int passes() {
		return switch (fitting) {
			case VALVE -> powered ? 0 : rate() * valveOpen / 10;
			case BREAKER -> powered || !leverOn ? 0 : Integer.MAX_VALUE;
			// a Light Switch: by hand only
			case SWITCH -> leverOn ? Integer.MAX_VALUE : 0;
			default -> Integer.MAX_VALUE;
		};
	}

	/**
	 * Whether this piece and the line next to it through side d join: neither joint cut, the same colour (plain is a
	 * colour of its own: two colours never join, so different lines can run side by side), and the same tier.
	 */
	public static boolean joins(LineBlockEntity a, Direction d, LineBlockEntity b) {
		Joint here = a.joint(d), there = b.joint(d.getOpposite());
		if (here == Joint.CUT || there == Joint.CUT || a.dye != b.dye)
			return false;
		// brass and copper: only at a Breaker, and only once that joint is wrenched
		if (a.tier() != b.tier())
			return breakerJoin(a, d, b) && here == Joint.JOINED && there == Joint.JOINED;
		return true;
	}

	/** Brass and copper Power Lines can join only at a Breaker, along its run (brass feeds copper through it), when wrenched. */
	static boolean breakerJoin(LineBlockEntity a, Direction d, LineBlockEntity b) {
		return a instanceof PowerLineBlockEntity && b instanceof PowerLineBlockEntity
			&& (a.fitting() == Fitting.BREAKER && a.fittingSide().getAxis() == d.getAxis()
				|| b.fitting() == Fitting.BREAKER && b.fittingSide().getAxis() == d.getAxis());
	}

	// ---------- changes ----------

	/** Its colour (a line is dyed as an item, before it's placed: this is for its saved data and tests). */
	public void setDye(@Nullable DyeColor color) {
		if (dye == color)
			return;
		dye = color;
		changed(true);
		rerender();
	}

	/** The wrench on side d: cut a joined joint; join a cut one to the line next to it (brass to copper at a Breaker). */
	public void wrenchJoint(Direction d) {
		if (level == null)
			return;
		boolean joinedNow = level.getBlockEntity(worldPosition.relative(d)) instanceof LineBlockEntity other && joins(this, d, other);
		Joint next;
		if (joinedNow)
			next = Joint.CUT;
		// brass and copper at a Breaker: joined by the wrench (two colours never join)
		else if (level.getBlockEntity(worldPosition.relative(d)) instanceof LineBlockEntity other
			&& other.tier() != tier() && other.dye == dye && breakerJoin(this, d, other))
			next = Joint.JOINED;
		else
			next = Joint.AUTO;
		setJoint(d, next);
		if (level.getBlockEntity(worldPosition.relative(d)) instanceof LineBlockEntity other)
			other.setJoint(d.getOpposite(), next);
		level.playSound(null, worldPosition, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 0.7f, next == Joint.CUT ? 0.8f : 1.2f);
	}

	/** The line beside side d went: whatever the wrench did to that joint goes with it (quietly: no new shape). */
	public void forgetJoint(Direction d) {
		if (joints.remove(d) != null)
			setChanged();
	}

	protected void setJoint(Direction d, Joint j) {
		if (joint(d) == j)
			return;
		if (j == Joint.AUTO)
			joints.remove(d);
		else
			joints.put(d, j);
		changed(true);
	}

	public void setFitting(Fitting f, Direction side) {
		fitting = f;
		fittingSide = side;
		filter = FluidStack.EMPTY;
		valveOpen = 10;
		leverOn = true;
		splitMode = SplitMode.EVEN;
		meterFlow = 0;
		meterLevel = 0;
		if (f == Fitting.INTAKE)
			joints.remove(side);
		changed(true);
		if (level != null)
			level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
		rerender();
	}

	/** Takes the fitting off, into the player's hands (or onto the ground). */
	public void removeFitting(@Nullable Player player) {
		if (fitting == Fitting.NONE || level == null)
			return;
		ItemStack item = new ItemStack(fitting.item());
		setFitting(Fitting.NONE, fittingSide);
		give(player, item);
		level.playSound(null, worldPosition, SoundEvents.COPPER_BREAK, SoundSource.BLOCKS, 0.7f, 1.2f);
	}

	public void setFilter(FluidStack fluid) {
		filter = fluid.isEmpty() ? FluidStack.EMPTY : fluid.copyWithAmount(1);
		settingChanged();
	}

	/** Turns the Valve's wheel by {@code steps} tenths. */
	public void turnValve(int steps) {
		int next = Math.clamp(valveOpen + steps, 0, 10);
		if (next != valveOpen) {
			valveOpen = next;
			settingChanged();
		}
	}

	public void flipLever() {
		leverOn = !leverOn;
		settingChanged();
	}

	public void nextSplitMode() {
		splitMode = splitMode.next();
		settingChanged();
	}

	public void setPowered(boolean on) {
		if (powered != on) {
			powered = on;
			settingChanged();
		}
	}

	public void setEncasing(@Nullable BlockState state) {
		encasing = state;
		settingChanged();
		rerender();
	}

	/** Set by the network each tick; sent to clients only when it moves noticeably (or now and then), for the needle. */
	void readMeter(int flow) {
		float lvl = rate() <= 0 ? 0 : Math.min(1f, flow / (float) rate());
		int oldSignal = comparatorSignal();
		meterFlow = flow;
		meterLevel = lvl;
		// against what clients last got, not last tick: a change held back by the throttle still goes out after it
		boolean moved = Math.abs(lvl - meterSent) > 0.02f || (flow == 0) != (meterSent == 0);
		long now = level == null ? 0 : level.getGameTime();
		if (moved && now - meterSynced >= 4) {
			meterSynced = now;
			meterSent = flow == 0 ? 0 : Math.max(lvl, 1e-6f);
			sync();
		}
		if (level != null && comparatorSignal() != oldSignal)
			level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
	}

	public int comparatorSignal() {
		if (fitting != Fitting.METER || meterFlow <= 0)
			return 0;
		return Math.max(1, Math.round(meterLevel * 15));
	}

	/** Saved, sent to clients, and (when {@code shape}) the blockstate's sides worked out again; always the network. */
	protected void changed(boolean shape) {
		setChanged();
		sync();
		if (shape)
			refreshState();
		if (level instanceof ServerLevel server)
			networkChanged(server);
	}

	/** A setting that only changes how much passes (the networks read it each tick): saved and sent, nothing rebuilt. */
	private void settingChanged() {
		setChanged();
		sync();
	}

	protected void sync() {
		if (level != null && !level.isClientSide)
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
	}

	/** Client: its fitting went on or off, or its encasing changed (its Flywheel visual is made again); set by the client setup. */
	public static java.util.function.Consumer<LineBlockEntity> clientRefitted = be -> {};

	/** The block model shows the dye and the encasing, so the chunk is drawn again on the client. */
	private void rerender() {
		requestModelDataUpdate();
		if (level != null && level.isClientSide)
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL_IMMEDIATE);
	}

	protected void give(@Nullable Player player, ItemStack item) {
		if (level == null || item.isEmpty())
			return;
		if (player == null || !player.getInventory().add(item))
			Block.popResource(level, worldPosition, item);
	}

	// ---------- saving and syncing ----------

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		write(tag, registries);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		BlockState oldEncasing = encasing;
		DyeColor oldDye = dye;
		Fitting oldFitting = fitting;
		dye = tag.contains("Dye") ? DyeColor.byId(tag.getInt("Dye")) : null;
		holding = tag.getBoolean("Holding");
		joints.clear();
		CompoundTag j = tag.getCompound("Joints");
		for (Direction d : Direction.values())
			if (j.contains(d.getSerializedName())) {
				Joint joint = Joint.values()[Math.clamp(j.getInt(d.getSerializedName()), 0, Joint.values().length - 1)];
				// a wrenched join is only brass to copper at a Breaker now: one between two colours (saved before) goes
				if (joint == Joint.JOINED && !(this instanceof PowerLineBlockEntity)) {
					staleJoins = true;
					continue;
				}
				joints.put(d, joint);
			}
		// older lines saved the Intake on its own
		fitting = tag.contains("Fitting") ? Fitting.byName(tag.getString("Fitting")) : tag.contains("Intake") ? Fitting.INTAKE : Fitting.NONE;
		fittingSide = Direction.from3DDataValue(tag.contains("FittingSide") ? tag.getInt("FittingSide") : tag.getInt("Intake"));
		filter = tag.contains("Filter") ? FluidStack.parseOptional(registries, tag.getCompound("Filter")) : FluidStack.EMPTY;
		valveOpen = tag.contains("ValveOpen") ? Math.clamp(tag.getInt("ValveOpen"), 0, 10) : 10;
		leverOn = !tag.getBoolean("LeverOff");
		powered = tag.getBoolean("Powered");
		splitMode = SplitMode.values()[Math.clamp(tag.getInt("SplitMode"), 0, SplitMode.values().length - 1)];
		encasing = tag.contains("Encasing") ? NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("Encasing")) : null;
		if (encasing != null && encasing.isAir())
			encasing = null;
		meterFlow = tag.getInt("MeterFlow");
		meterLevel = tag.getFloat("MeterLevel");
		if (level != null && level.isClientSide && (oldEncasing != encasing || oldDye != dye || oldFitting != fitting))
			rerender();
		// a fitting on or off, or the line encased: its parts' visual is made again (a plain line has one that does nothing)
		if (level != null && level.isClientSide && (oldEncasing != encasing || oldFitting != fitting))
			clientRefitted.accept(this);
		// new data on a line already in the world (a command, a schematic): its sides and network are worked out again
		if (level instanceof ServerLevel server) {
			server.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
			networkChanged(server);
		}
	}

	private void write(CompoundTag tag, HolderLookup.Provider registries) {
		if (dye != null)
			tag.putInt("Dye", dye.getId());
		if (!joints.isEmpty()) {
			CompoundTag j = new CompoundTag();
			joints.forEach((d, joint) -> j.putInt(d.getSerializedName(), joint.ordinal()));
			tag.put("Joints", j);
		}
		if (fitting != Fitting.NONE) {
			tag.putString("Fitting", fitting.getSerializedName());
			tag.putInt("FittingSide", fittingSide.get3DDataValue());
		}
		if (!filter.isEmpty())
			tag.put("Filter", filter.save(registries));
		if (valveOpen != 10)
			tag.putInt("ValveOpen", valveOpen);
		if (!leverOn)
			tag.putBoolean("LeverOff", true);
		if (powered)
			tag.putBoolean("Powered", true);
		if (splitMode != SplitMode.EVEN)
			tag.putInt("SplitMode", splitMode.ordinal());
		if (encasing != null)
			tag.put("Encasing", NbtUtils.writeBlockState(encasing));
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		// never empty: a client drops an empty update, and would go on showing what was just taken away
		tag.putBoolean("Sync", true);
		if (holding)
			tag.putBoolean("Holding", true);
		write(tag, registries);
		if (meterFlow != 0) {
			tag.putInt("MeterFlow", meterFlow);
			tag.putFloat("MeterLevel", meterLevel);
		}
		writeClient(tag, registries);
		return tag;
	}

	/** What only clients need (the flow for the sight slots), on top of what's saved. */
	protected void writeClient(CompoundTag tag, HolderLookup.Provider registries) {}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	// the dye travels with the item when the line is broken or picked

	@Override
	protected void applyImplicitComponents(DataComponentInput input) {
		super.applyImplicitComponents(input);
		dye = input.get(ModDataComponents.LINE_DYE.get());
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (dye != null)
			components.set(ModDataComponents.LINE_DYE.get(), dye);
	}

	@Override
	public void removeComponentsFromTag(CompoundTag tag) {
		tag.remove("Dye");
	}

	// ---------- lifecycle ----------

	/** A join between two colours was read from the save and dropped: the sides are worked out again once it's loaded. */
	private boolean staleJoins;

	@Override
	public void onLoad() {
		super.onLoad();
		if (level instanceof ServerLevel server) {
			networkLoaded(server);
			if (staleJoins) {
				staleJoins = false;
				setChanged();
				server.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
			}
		}
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		if (level instanceof ServerLevel server)
			networkUnloaded(server);
	}

	// ---------- goggles ----------

	/** What the goggles say a Splitter does (a Power Line's says how it ranks its branches). */
	protected Component splitterLine() {
		return Component.translatable(key("splitter_" + splitMode.key()));
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).add(LineItem.named(dye, getBlockState().getBlock().getName())).forGoggles(tooltip);
		addLineTooltip(tooltip);
		switch (fitting) {
			case INTAKE -> line(tooltip, filter.isEmpty() ? Component.translatable(key("intake_any"))
				: Component.translatable(key("intake_filter"), filter.getHoverName()));
			case FILTER -> line(tooltip, filter.isEmpty() ? Component.translatable(key("filter_empty"))
				: Component.translatable(key("filter_passes"), filter.getHoverName()));
			case VALVE -> line(tooltip, powered ? Component.translatable(key("valve_shut_redstone"))
				: Component.translatable(key("valve_open"), valveOpen * 10));
			default -> {}
		}
		if (holding && (fitting == Fitting.VALVE || fitting == Fitting.FILTER))
			line(tooltip, Component.translatable(key("holding_back")));
		switch (fitting) {
			case BREAKER -> line(tooltip, Component.translatable(key(powered ? "breaker_redstone" : leverOn ? "breaker_on" : "breaker_off")));
			case SWITCH -> line(tooltip, Component.translatable(key(leverOn ? "switch_on" : "switch_off")));
			case SPLITTER -> line(tooltip, splitterLine());
			case METER -> line(tooltip, Component.translatable(key(meterUnitKey()), meterFlow));
			default -> {}
		}
		return true;
	}

	/** The kind's own goggle lines (capacity, what's moving). */
	protected abstract void addLineTooltip(List<Component> tooltip);

	protected abstract String meterUnitKey();

	protected static String key(String name) {
		return "comedymonkeysaddons.gui.goggles." + name;
	}

	protected static void line(List<Component> tooltip, Component text) {
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).add(text.copy()).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
	}

	@Nullable
	public static LineBlockEntity at(Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof LineBlockEntity l ? l : null;
	}
}
