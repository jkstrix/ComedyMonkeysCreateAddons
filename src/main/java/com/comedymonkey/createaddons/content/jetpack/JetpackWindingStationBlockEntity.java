package com.comedymonkey.createaddons.content.jetpack;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.comedymonkey.createaddons.registry.ModSounds;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The Jetpack Winding Station (jetpack spec D38-D45). It winds the jetpack hung on it, or the one worn by a player who
 * has backed into it (on the block in front, their back to it, sneaking), from the shaft below exactly as Create's
 * Copper Backtank fills with air: each step adds {@code clamp((rpm - 100) / 20, 1, 5)} backtank-air's worth, the steps
 * coming faster as the shaft turns faster. A full base wind is a backtank's 900 air, so Overwound takes longer, as
 * Capacity does on a backtank.
 */
public class JetpackWindingStationBlockEntity extends KineticBlockEntity {
	/** What the boom is doing, sent to clients for the renderer. */
	public enum Boom { PARKED, HUNG, WORN }

	private ItemStack hung = ItemStack.EMPTY;
	/** Ticks to the next step of winding (Create's backtank's airLevelTimer). */
	private int timer;
	/** Server: the boom's last state, and the player it's winding (backed in). */
	private Boom boom = Boom.PARKED;
	@Nullable
	private Player wearer;
	private int lastComparator = -1;
	/** Client: the wearer the boom reaches for (its socket turns with their key), by entity id; -1 none. */
	private int wearerId = -1;
	/** Client: how wound the hung jetpack is, for its porthole. */
	private float hungFill;
	/** Client: the boom's reach (0 in .. 1 out) and how far down it is (0 parked .. 1 on the key), this tick and last. */
	private float out, down, lastOut, lastDown;
	/** Client: whether its reach is set for a wearer's key (shorter) or the hung jetpack's; kept while it parks. */
	private boolean forWearer;

	private final IItemHandler items = new IItemHandler() {
		@Override
		public int getSlots() {
			return 1;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			return hung;
		}

		/** Funnels and arms can hang a jetpack (D43). */
		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			if (!hung.isEmpty() || !Jetpacks.isJetpack(stack))
				return stack;
			if (!simulate)
				hang(stack.copyWithCount(1));
			return stack.copyWithCount(stack.getCount() - 1);
		}

		/** ...and take it down only once it's fully wound (D43). */
		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			if (hung.isEmpty() || amount <= 0 || !Jetpacks.full(hung))
				return ItemStack.EMPTY;
			return simulate ? hung.copy() : takeDown();
		}

		@Override
		public int getSlotLimit(int slot) {
			return 1;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return Jetpacks.isJetpack(stack);
		}
	};

	public JetpackWindingStationBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.JETPACK_WINDING_STATION.get(), pos, state);
	}

	/** The post, its boom and the hung jetpack stand most of a block above it. */
	@Override
	protected AABB createRenderBoundingBox() {
		return new AABB(worldPosition).expandTowards(0, 1, 0).inflate(0.25);
	}

	public IItemHandler items() {
		return items;
	}

	public ItemStack hung() {
		return hung;
	}

	public Boom boom() {
		return boom;
	}

	public int wearerId() {
		return wearerId;
	}

	public float hungFill() {
		return hungFill;
	}

	public Direction facing() {
		return getBlockState().getValue(JetpackWindingStationBlock.HORIZONTAL_FACING);
	}

	public void hang(ItemStack jetpack) {
		hung = jetpack;
		sound("station_hang");
		changed();
	}

	public ItemStack takeDown() {
		ItemStack taken = hung;
		hung = ItemStack.EMPTY;
		if (level != null && !level.isClientSide)
			sound("station_take");
		changed();
		return taken;
	}

	private void changed() {
		setChanged();
		if (level != null && !level.isClientSide)
			sendData();
	}

	public int comparatorLevel() {
		return hung.isEmpty() ? 0 : Math.round(Jetpacks.fill(hung) * 15);
	}

	// ---- winding

	@Override
	public void tick() {
		super.tick();
		if (level == null)
			return;
		if (level.isClientSide) {
			animate();
			return;
		}
		Player backedIn = hung.isEmpty() ? findWearer() : null;
		ItemStack target = !hung.isEmpty() ? hung : backedIn != null ? Jetpacks.worn(backedIn) : ItemStack.EMPTY;
		boolean turning = getSpeed() != 0 && !isOverStressed();
		boolean winding = turning && !target.isEmpty() && !Jetpacks.full(target);
		Boom now = !winding ? Boom.PARKED : hung.isEmpty() ? Boom.WORN : Boom.HUNG;
		Player winds = winding ? backedIn : null;
		if (now != boom || winds != wearer) {
			if (boom == Boom.PARKED && now != Boom.PARKED)
				sound("station_boom");
			else if (boom != Boom.PARKED && now == Boom.PARKED)
				sound("station_release");
			boom = now;
			wearer = winds;
			timer = 12; // the boom reaches out and engages before it starts to turn (station clips: 0.6 s)
			sendData();
		}
		if (winding) {
			if (timer == 1)
				sound("station_engage");
			if (timer > 0)
				timer--;
			else
				windStep(target);
			if (level.getGameTime() % 40 == 0)
				sound("station_winding");
		}
		if (!hung.isEmpty() && level.getGameTime() % 10 == 0) {
			int comparator = comparatorLevel();
			if (comparator != lastComparator) {
				lastComparator = comparator;
				level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
				sendData(); // the porthole's coil
			}
		}
	}

	/**
	 * Client: the boom reaches out while still parked high, then lowers in front of the jetpack onto its key (station
	 * clips: out over 0.3 s, down over 0.4 s); it parks in reverse, up first and then in, so it never passes through the
	 * jetpack. Backed in, it reaches less far, the wearer's key being nearer.
	 */
	private void animate() {
		lastOut = out;
		lastDown = down;
		boolean engaged = boom != Boom.PARKED;
		if (engaged) {
			forWearer = boom == Boom.WORN;
			if (out < 1)
				out = Math.min(1, out + 1 / 6f);
			else
				down = Math.min(1, down + 1 / 8f);
		} else if (down > 0)
			down = Math.max(0, down - 1 / 8f);
		else
			out = Math.max(0, out - 1 / 6f);
		if (boom == Boom.WORN && wearerId >= 0)
			WINDING.put(wearerId, worldPosition);
	}

	/** Ponder (whose world runs none of the server's winding): show it winding the hung jetpack or a wearer's, or parked, and how wound. */
	public void showWinding(Boom boom, float fill) {
		this.boom = boom;
		this.hungFill = Mth.clamp(fill, 0, 1);
	}

	public float boomDown() {
		return down;
	}

	public float boomDown(float partialTicks) {
		return Mth.lerp(partialTicks, lastDown, down);
	}

	public float reachOut(float partialTicks) {
		return Mth.lerp(partialTicks, lastOut, out);
	}

	public boolean reachingForWearer() {
		return forWearer;
	}

	/** Client: which station is winding which wearer's jetpack (their entity id), so their key turns with its socket. */
	public static final java.util.Map<Integer, BlockPos> WINDING = new java.util.HashMap<>();

	/** One step of a backtank's fill, scaled to the jetpack's wind; the next step comes sooner the faster it turns. */
	private void windStep(ItemStack jetpack) {
		float rpm = Math.abs(getSpeed());
		boolean wasFull = Jetpacks.full(jetpack);
		Jetpacks.setWind(jetpack, Jetpacks.wind(jetpack) + airPerStep(rpm) * Jetpacks.windPerAir());
		timer = waitAfterStep(rpm);
		if (!wasFull && Jetpacks.full(jetpack))
			sound("station_full");
		if (level != null)
			level.blockEntityChanged(worldPosition); // saved; the comparator hears of it when its level changes (tick)
	}

	/** Create's Copper Backtank: the air a step of its fill adds at this speed (1 up to 5, from 200 RPM). */
	public static int airPerStep(float rpm) {
		return Mth.clamp(((int) Math.abs(rpm) - 100) / 20, 1, 5);
	}

	/** ...and the ticks it waits before the next step (none from 100 RPM; 13 at a Hand Crank's 32). */
	public static int waitAfterStep(float rpm) {
		return Mth.clamp((int) (128f - Math.abs(rpm) / 5f) - 108, 0, 20);
	}

	/**
	 * A player backed into the station (D39): standing on the block in front of it, facing away from it within 45°,
	 * sneaking, with a jetpack on that isn't full.
	 */
	@Nullable
	private Player findWearer() {
		Direction facing = facing();
		BlockPos front = worldPosition.relative(facing);
		List<Player> players = level.getEntitiesOfClass(Player.class, new AABB(front), player -> backedIn(player, worldPosition, facing));
		for (Player player : players) {
			ItemStack jetpack = Jetpacks.worn(player);
			if (!jetpack.isEmpty() && !Jetpacks.full(jetpack))
				return player;
		}
		return null;
	}

	/** Whether a player has backed into the station at {@code station} facing {@code facing} (D39). */
	public static boolean backedIn(Player player, BlockPos station, Direction facing) {
		return !player.isSpectator() && player.isShiftKeyDown() && player.blockPosition().equals(station.relative(facing))
			&& Math.abs(Mth.wrapDegrees(player.getYRot() - facing.toYRot())) <= 45;
	}

	private void sound(String id) {
		SoundEvent sound = ModSounds.jetpack(id);
		if (sound != null && level != null)
			level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 1, 1);
	}

	// ---- goggles, saving

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).add(getBlockState().getBlock().getName().copy().append(":")).forGoggles(tooltip);
		if (hung.isEmpty())
			new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.jetpack_station.empty").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		else {
			new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID)
				.translate("gui.goggles.jetpack_station.wound", Math.round(Jetpacks.fill(hung) * 100)).style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
			Optional<Integer> seconds = secondsToFull();
			seconds.ifPresent(s -> new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID)
				.translate("gui.goggles.jetpack_station.time_to_full", String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60))
				.style(ChatFormatting.GRAY).forGoggles(tooltip, 1));
		}
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		return true;
	}

	/** How long the hung jetpack takes to fill at the shaft's speed now: empty if it isn't winding. */
	private Optional<Integer> secondsToFull() {
		float rpm = Math.abs(getSpeed());
		if (rpm == 0 || hung.isEmpty() || Jetpacks.full(hung))
			return Optional.empty();
		int perStep = airPerStep(rpm) * Jetpacks.windPerAir();
		int ticksPerStep = waitAfterStep(rpm) + 1;
		int steps = Mth.ceil((float) (Jetpacks.maxWind(hung) - Jetpacks.wind(hung)) / perStep);
		return Optional.of(steps * ticksPerStep / 20);
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		if (!hung.isEmpty())
			tag.put("Jetpack", hung.save(registries));
		tag.putInt("Timer", timer);
		if (clientPacket) {
			tag.putInt("Boom", boom.ordinal());
			tag.putInt("Wearer", wearer == null ? -1 : wearer.getId());
		}
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		hung = tag.contains("Jetpack") ? ItemStack.parse(registries, tag.getCompound("Jetpack")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
		timer = tag.getInt("Timer");
		if (clientPacket) {
			boom = Boom.values()[Mth.clamp(tag.getInt("Boom"), 0, Boom.values().length - 1)];
			wearerId = tag.getInt("Wearer");
			hungFill = hung.isEmpty() ? 0 : Jetpacks.fill(hung);
		}
	}
}
