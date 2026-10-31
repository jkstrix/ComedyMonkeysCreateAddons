package com.comedymonkey.createaddons.content.tube;

import java.util.List;
import java.util.Locale;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Runs a Clockwork Tubes network (design/specs/clockwork-tubes.md D11, D13, D17, D25, D26, D39). While its cog turns and
 * its chunk is running, each tick it moves the loads in its tubes on towards itself, nearest first: each tube holds
 * one, and an empty tube takes the load that has waited longest behind it, so a full tube holds back the ones behind
 * and every branch gets its turn. Its Inlets take loads in turn, RPM / 64 loads a second between them. A load reaching
 * it rises through its throat and the middle of its cog for a block's travel, then goes into whatever is in front of
 * its output, or drops out if nothing is there; if that can't take it, everything waits. It sounds like a cogwheel and
 * nothing else.
 */
public class ClockworkReceiverBlockEntity extends KineticBlockEntity {
	/** What it's doing, for the goggles and Display Links. */
	public enum Status {
		RUNNING, OUTPUT_FULL, NOT_TURNING, OVERSTRESSED, SHARED
	}

	private TubeNetwork network;
	/** The change count (TubeJoints.version) it last looked at. */
	private int seen;
	/** The load passing through its throat and cog (one at a time; saved, dropped if it's broken), and when it came in. */
	private ItemStack throat = ItemStack.EMPTY;
	private long throatSince;
	/** The Inlet whose turn it is next. */
	private int turn;
	/** Loads it may take, carried between ticks (never more than one in hand). */
	private double pulls;
	private Status status = Status.NOT_TURNING;
	/** Items delivered in each of the last 60 seconds. */
	private final int[] perSecond = new int[60];
	private int second;
	private long lastAward = Long.MIN_VALUE / 2;
	/** Synced figures for the goggles and Display Links. */
	private int itemsPerMinute, inTransit, inletCount, tubeCount;
	private boolean tooLong;
	/** Client (Ponder): how far through the throat to show its load, or below 0 to work it out from the time. */
	public float throatShown = -1;

	public ClockworkReceiverBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CLOCKWORK_RECEIVER.get(), pos, state);
	}

	/** A cogwheel's sound only: Create plays it for small cogs; this turns off its general machine hum (D37). */
	@Override
	protected boolean isNoisy() {
		return false;
	}

	public Status getStatus() {
		return status;
	}

	public int getItemsPerMinute() {
		return itemsPerMinute;
	}

	/** What's in its throat (dropped if it's broken; drawn rising through its cog). */
	public ItemStack getThroat() {
		return throat;
	}

	/** How far through its throat its load is, 0 at its input to 1 at its output (client). */
	public float throatProgress(double time) {
		if (throatShown >= 0)
			return throatShown;
		return (float) Math.max(0, Math.min(1, (time - throatSince) / passTicks()));
	}

	/** Client only (Ponder): shows a load in its throat without telling anyone. */
	public void showThroat(ItemStack stack, float progress) {
		throat = stack;
		throatShown = progress;
	}

	/** Blocks a tick its loads travel at this speed (RPM / 16 blocks a second by default). */
	public static float travelPerTick(float rpm) {
		return (float) (Math.abs(rpm) * CMConfig.SERVER.clockworkTubeBlocksPerRpm() / 20);
	}

	/** Ticks a load takes through it at its speed now: a block's travel, and at least a few ticks so it's seen. */
	public double passTicks() {
		float perTick = travelPerTick(getSpeed());
		return perTick <= 0 ? 20 : Math.max(4, 1 / perTick);
	}

	// ---------- running ----------

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide)
			return;
		long now = level.getGameTime();
		int version = TubeJoints.version(level);
		if (network == null || version != seen && touches(TubeJoints.changesSince(level, seen)))
			network = TubeNetwork.read(level, worldPosition, ClockworkReceiverBlock.input(getBlockState()), version);
		seen = version;
		if (now % 20 == 0)
			for (TubeNetwork.Entry e : network.inlets)
				if (ClockworkTubeBlock.running(level, e.inlet()) && level.getBlockEntity(e.inlet()) instanceof ClockworkInletBlockEntity inlet)
					inlet.seen(now);

		float rpm = Math.abs(getSpeed());
		Status was = status;
		boolean moving = was == Status.RUNNING || was == Status.OUTPUT_FULL;
		if (network.shared || rpm == 0) {
			status = network.shared ? Status.SHARED : isOverStressed() ? Status.OVERSTRESSED : Status.NOT_TURNING;
			// stopped: the loads stand where they are (and the client stops gliding them)
			if (moving)
				freeze(now);
			pulls = 0;
		} else {
			float speed = travelPerTick(rpm);
			boolean full = pass(now);
			full |= move(now, speed);
			pull(now, rpm, speed);
			status = full ? Status.OUTPUT_FULL : Status.RUNNING;
		}

		if (now % 20 == 0) {
			second = (second + 1) % perSecond.length;
			perSecond[second] = 0;
		}
		if (now % 10 == 0 || status != was)
			refreshFigures(status != was);
	}

	/**
	 * Whether any of these changes is on its network or beside it (so it may have grown, shrunk or lost a joint): only then
	 * is it read again. Null (too many changes to remember): yes.
	 */
	private boolean touches(@org.jetbrains.annotations.Nullable long[] changes) {
		if (changes == null)
			return true;
		BlockPos input = worldPosition.relative(ClockworkReceiverBlock.input(getBlockState()));
		for (long at : changes) {
			BlockPos p = BlockPos.of(at);
			if (p.equals(worldPosition) || p.equals(input) || network.home.containsKey(p))
				return true;
			for (Direction side : Direction.values())
				if (network.home.containsKey(p.relative(side)))
					return true;
		}
		return false;
	}

	/** The load in its throat goes out once it's through; true if it's through and its output can't take it. */
	private boolean pass(long now) {
		if (throat.isEmpty() || now - throatSince < passTicks())
			return false;
		ItemStack rest = deliver(throat);
		boolean changed = rest.getCount() != throat.getCount();
		throat = rest;
		if (changed) {
			setChanged();
			sendData();
		}
		return !throat.isEmpty();
	}

	/** Where a load is at {@code now}, unclamped (1 or more: at its tube's far face). */
	private static float at(TubeLoad load, long now) {
		return load.progress + (now - load.since) * load.speed;
	}

	/** When a load reached its tube's far face (the earliest goes first at a join). */
	private static double arrived(TubeLoad load) {
		return load.speed == 0 ? load.since : load.since + (1 - load.progress) / load.speed;
	}

	/**
	 * Every load moves on, nearest the Receiver first. A load at the far face of the first tube goes into the throat if
	 * it's free; any other waits there until the tube ahead is empty; an empty tube takes, of the loads waiting behind
	 * it, the one that has waited longest (D26). True if a load is waiting for the throat.
	 */
	private boolean move(long now, float speed) {
		boolean full = false;
		for (BlockPos tube : network.tubes) {
			if (!ClockworkTubeBlock.running(level, tube) || !(level.getBlockEntity(tube) instanceof ClockworkTubeBlockEntity be))
				continue;
			Direction home = network.home.get(tube);
			TubeLoad load = be.getLoad();
			if (load != null) {
				float at = at(load, now);
				// a changed network turns it, and a changed speed sets it gliding at the new one
				if (load.to != home || (load.speed != speed && at < 1)) {
					load.to = home;
					anchor(load, Math.min(at, 1), now, at < 1 ? speed : 0);
					be.loadChanged();
				}
				if (at < 1)
					continue;
				if (tube.equals(network.first) && throat.isEmpty()) {
					throat = load.stack;
					throatSince = now;
					be.loadMovedOn(worldPosition);
					setChanged();
					sendData();
				} else {
					full |= tube.equals(network.first);
					if (load.speed != 0) {
						// it waits at the face (the tube ahead, or the throat, is full): the client stops it there itself
						anchor(load, 1, now, 0);
						be.loadWaiting();
					}
					continue;
				}
			}
			// empty: it takes the load that has waited longest in the tubes behind it
			ClockworkTubeBlockEntity from = null;
			Direction fromSide = null;
			double earliest = Double.MAX_VALUE;
			for (Direction side : Direction.values()) {
				BlockPos behind = tube.relative(side);
				if (network.home.get(behind) != side.getOpposite() || !ClockworkTubeBlock.running(level, behind)
					|| !(level.getBlockEntity(behind) instanceof ClockworkTubeBlockEntity b) || b.getLoad() == null || at(b.getLoad(), now) < 1)
					continue;
				double when = arrived(b.getLoad());
				if (when < earliest) {
					earliest = when;
					from = b;
					fromSide = side;
				}
			}
			if (from != null) {
				TubeLoad waiting = from.getLoad();
				be.setLoad(new TubeLoad(waiting.stack, fromSide, home, Math.min(at(waiting, now) - 1, 0.99f), now, speed));
				from.loadMovedOn(tube);
			}
		}
		return full;
	}

	private static void anchor(TubeLoad load, float progress, long now, float speed) {
		load.progress = progress;
		load.since = now;
		load.speed = speed;
	}

	/** Stopped: every load in its tubes stands still where it is. */
	private void freeze(long now) {
		for (BlockPos tube : network.tubes)
			if (ClockworkTubeBlock.running(level, tube) && level.getBlockEntity(tube) instanceof ClockworkTubeBlockEntity be && be.getLoad() != null
				&& be.getLoad().speed != 0) {
				TubeLoad load = be.getLoad();
				anchor(load, load.progressAt(now), now, 0);
				be.loadChanged();
			}
	}

	/** The Inlets take loads in turn, as many as its speed allows. */
	private void pull(long now, float rpm, float speed) {
		pulls = Math.min(pulls + rpm * CMConfig.SERVER.clockworkTubeLoadsPerRpm() / 20, 1 + 1e-9);
		int size = network.inlets.size();
		int cooldown = CMConfig.SERVER.clockworkInletCooldown();
		while (pulls >= 1 && size > 0) {
			boolean took = false;
			for (int k = 0; k < size && !took; k++) {
				int i = (turn + k) % size;
				TubeNetwork.Entry e = network.inlets.get(i);
				if (!ClockworkTubeBlock.running(level, e.inlet()) || !(level.getBlockEntity(e.inlet()) instanceof ClockworkInletBlockEntity inlet)
					|| !inlet.ready(now, cooldown))
					continue;
				if (e.tube() == null) {
					// right against its input (a short run): straight into its throat, once the last load is through
					if (!throat.isEmpty())
						continue;
					ItemStack stack = inlet.pull(now);
					if (stack.isEmpty())
						continue;
					throat = stack;
					throatSince = now;
					setChanged();
					sendData();
				} else {
					if (!ClockworkTubeBlock.running(level, e.tube()) || !(level.getBlockEntity(e.tube()) instanceof ClockworkTubeBlockEntity tube)
						|| tube.getLoad() != null)
						continue;
					ItemStack stack = inlet.pull(now);
					if (stack.isEmpty())
						continue;
					tube.setLoad(new TubeLoad(stack, e.side(), network.home.get(e.tube()), 0, now, speed));
				}
				turn = (i + 1) % size;
				took = true;
			}
			if (!took)
				break;
			pulls -= 1;
		}
	}

	/**
	 * Out through its cog: into whatever takes items in front of its output (a chest, a funnel, a belt, a depot), or
	 * dropped out if there's nothing solid there. What it couldn't put anywhere comes back.
	 */
	ItemStack deliver(ItemStack stack) {
		Direction out = getBlockState().getValue(ClockworkReceiverBlock.FACING);
		BlockPos target = worldPosition.relative(out);
		if (!level.isLoaded(target))
			return stack;
		ItemStack rest;
		IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, target, out.getOpposite());
		DirectBeltInputBehaviour input = handler != null ? null : BlockEntityBehaviour.get(level, target, DirectBeltInputBehaviour.TYPE);
		if (handler != null) {
			rest = ItemHandlerHelper.insertItemStacked(handler, stack.copy(), false);
		} else if (input != null) {
			rest = input.canInsertFromSide(out.getOpposite()) ? input.handleInsertion(stack.copy(), out.getOpposite(), false) : stack;
		} else if (level.getBlockState(target).getCollisionShape(level, target).isEmpty()) {
			// nothing there: it pops out, moving gently away, as out of a Chute
			Vec3 at = Vec3.atCenterOf(worldPosition).add(Vec3.atLowerCornerOf(out.getNormal()).scale(0.7));
			ItemEntity item = new ItemEntity(level, at.x, at.y - (out.getAxis().isHorizontal() ? 0.1 : 0), at.z, stack.copy());
			item.setDeltaMovement(Vec3.atLowerCornerOf(out.getNormal()).scale(0.12).add(0, out.getAxis().isHorizontal() ? 0.08 : 0, 0));
			item.setDefaultPickUpDelay();
			level.addFreshEntity(item);
			rest = ItemStack.EMPTY;
		} else {
			rest = stack;
		}
		int moved = stack.getCount() - rest.getCount();
		if (moved > 0) {
			perSecond[second] += moved;
			if (level.getGameTime() - lastAward >= 20) {
				lastAward = level.getGameTime();
				ModAdvancements.awardNearby(level, worldPosition, ModAdvancements.TUBE_DELIVERY, stack, moved);
			}
		}
		return rest;
	}

	private void refreshFigures(boolean force) {
		int perMinute = 0;
		for (int n : perSecond)
			perMinute += n;
		int transit = throat.getCount();
		for (BlockPos tube : network.tubes)
			if (ClockworkTubeBlock.running(level, tube) && level.getBlockEntity(tube) instanceof ClockworkTubeBlockEntity be && be.getLoad() != null)
				transit += be.getLoad().stack.getCount();
		if (force || perMinute != itemsPerMinute || transit != inTransit || network.inlets.size() != inletCount || network.tubes.size() != tubeCount
			|| network.tooLong != tooLong) {
			itemsPerMinute = perMinute;
			inTransit = transit;
			inletCount = network.inlets.size();
			tubeCount = network.tubes.size();
			tooLong = network.tooLong;
			sendData();
		}
	}

	// ---------- goggles and saving ----------

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		lang().translate("gui.goggles.clockwork_receiver").forGoggles(tooltip);
		switch (status) {
			case RUNNING -> lang().translate("gui.goggles.clockwork_receiver.running",
					String.format(Locale.ROOT, "%.2f", Math.abs(getSpeed()) * CMConfig.SERVER.clockworkTubeLoadsPerRpm()))
				.style(ChatFormatting.AQUA).forGoggles(tooltip, 1);
			case OUTPUT_FULL -> lang().translate("gui.goggles.clockwork_receiver.output_full").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
			case OVERSTRESSED -> lang().translate("gui.goggles.clockwork_receiver.overstressed").style(ChatFormatting.RED).forGoggles(tooltip, 1);
			case NOT_TURNING -> lang().translate("gui.goggles.clockwork_receiver.not_turning").style(ChatFormatting.RED).forGoggles(tooltip, 1);
			case SHARED -> lang().translate("gui.goggles.clockwork_receiver.shared").style(ChatFormatting.RED).forGoggles(tooltip, 1);
		}
		lang().translate("gui.goggles.clockwork_receiver.network", inletCount, tubeCount).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		if (tooLong)
			lang().translate("gui.goggles.clockwork_receiver.too_long", CMConfig.SERVER.clockworkTubeMaxLength())
				.style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
		lang().translate("gui.goggles.clockwork_receiver.in_transit", inTransit).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		lang().translate("gui.goggles.clockwork_receiver.per_minute", itemsPerMinute).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		return true;
	}

	private static LangBuilder lang() {
		return new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID);
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		if (!clientPacket) {
			tag.putInt("Turn", turn);
			tag.putDouble("Pulls", pulls);
		}
		if (!throat.isEmpty()) {
			tag.put("Throat", throat.save(registries));
			tag.putLong("ThroatSince", throatSince);
		}
		tag.putByte("Status", (byte) status.ordinal());
		tag.putInt("PerMinute", itemsPerMinute);
		tag.putInt("InTransit", inTransit);
		tag.putInt("Inlets", inletCount);
		tag.putInt("Tubes", tubeCount);
		tag.putBoolean("TooLong", tooLong);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		if (!clientPacket) {
			turn = tag.getInt("Turn");
			pulls = tag.getDouble("Pulls");
		}
		long was = throatSince;
		throat = tag.contains("Throat") ? ItemStack.parseOptional(registries, tag.getCompound("Throat")) : ItemStack.EMPTY;
		throatSince = tag.getLong("ThroatSince");
		// a load came into its throat: the tube on its input has passed it on (that tube's own update isn't sent)
		if (clientPacket && !throat.isEmpty() && throatSince != was && level != null) {
			Direction input = ClockworkReceiverBlock.input(getBlockState());
			if (level.getBlockEntity(worldPosition.relative(input)) instanceof ClockworkTubeBlockEntity tube && tube.getLoad() != null
				&& tube.getLoad().to == input.getOpposite() && tube.getLoad().progressAt(level.getGameTime()) > 0.5f)
				tube.showLoad(null);
		}
		status = Status.values()[Math.floorMod(tag.getByte("Status"), Status.values().length)];
		itemsPerMinute = tag.getInt("PerMinute");
		inTransit = tag.getInt("InTransit");
		inletCount = tag.getInt("Inlets");
		tubeCount = tag.getInt("Tubes");
		tooLong = tag.getBoolean("TooLong");
	}
}
