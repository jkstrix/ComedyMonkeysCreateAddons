package com.comedymonkey.createaddons.content.tradebot;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.tradebot.work.ContainerVisit;
import com.comedymonkey.createaddons.content.tradebot.work.ContainerWork;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipLibrary;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipRoot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

/**
 * What Trade Bot is doing (server): docked at home, leaving, out on a trip, rolling home and docking. Jobs (fetching,
 * trading, delivering) come with milestones 4 and 5; for now trips come from {@code /tradebot goto}. Wherever he is,
 * he heads home when his station's redstone pauses him, at night, in a thunderstorm, or when his charge only just
 * covers the way back (D3, D32, D38, D49).
 */
public class TradeBotBrain {
	public static final String LEAVE = "action_comes_in";
	/** How long he waits at a trip's end before heading home (ticks). */
	private static final int WAIT_TICKS = 60;
	/** Homeward route failures before he's put back in his station anyway (so he can always be packed up). */
	private static final int MAX_HOME_FAILURES = 3;

	public enum Task { HOME, LEAVING, GOING, WAITING, APPROACHING, WORKING, WAITING_OUTPUT, RETURNING, DOCKING, STRANDED }
	/** How long he waits beside a full output before trying again (D22). */
	private static final int OUTPUT_RETRY_TICKS = 100;

	private final TradeBotEntity bot;
	private Task task = Task.HOME;
	/** Where he's going, in order (a trip has one stop; a trading run, several). */
	private final Deque<BlockPos> goals = new ArrayDeque<>();
	/** Blocks he has stood on this trip, for checking a run in tests. */
	private final List<BlockPos> visited = new ArrayList<>();
	private boolean commanded;
	private boolean routed;
	private int ticks;
	private int homeFailures;
	/** The current job: the inputs in this trip's order, the output, and what he takes (milestone 5 puts trading in between). */
	@Nullable
	private Job job;
	@Nullable
	private ContainerWork work;
	/** Which input goes first next time, so they take turns (D47). */
	private int roundRobin;

	private enum Phase { TAKING, DELIVERING, RETURNING_ITEMS }

	private static final class Job {
		final List<BlockPos> inputs;
		final BlockPos output;
		final Predicate<ItemStack> wanted;
		Phase phase = Phase.TAKING;
		int input;
		int taken, delivered, returned;
		/** What he took from each input, so unsold items go back where they came from (D62). */
		final Map<BlockPos, List<ItemStack>> takenFrom = new LinkedHashMap<>();
		/** Where unsold items still have to go: their own inputs first, then any input for what's left over. */
		final Deque<BlockPos> returns = new ArrayDeque<>();
		boolean leftoversQueued;

		Job(List<BlockPos> inputs, BlockPos output, Predicate<ItemStack> wanted) {
			this.inputs = inputs;
			this.output = output;
			this.wanted = wanted;
		}

		/** Stacks in his load that came from {@code input} (or, once leftovers are queued, anything). */
		Predicate<ItemStack> from(BlockPos input) {
			List<ItemStack> kinds = takenFrom.getOrDefault(input, List.of());
			return stack -> leftoversQueued || kinds.stream().anyMatch(k -> ItemStack.isSameItemSameComponents(k, stack));
		}
	}

	public TradeBotBrain(TradeBotEntity bot) {
		this.bot = bot;
	}

	public Task task() {
		return task;
	}

	/** The stops reached on this trip, in order. */
	public List<BlockPos> visited() {
		return List.copyOf(visited);
	}

	// ---- orders

	/** Sends him to stand on {@code target}, leaving his station first if he's docked. */
	public boolean goTo(BlockPos target) {
		return tour(List.of(target));
	}

	/** Visits each block in turn (a short wait at each), then goes home. */
	public boolean tour(List<BlockPos> stops) {
		if (stops.isEmpty())
			return false;
		goals.clear();
		goals.addAll(stops);
		visited.clear();
		commanded = true;
		if (bot.isDocked())
			return leave();
		set(Task.GOING);
		return true;
	}

	/**
	 * A job: from each of his station's inputs in turn, take what's {@code wanted} until he's full; (milestone 5: trade);
	 * deliver his earnings, emeralds and Nuggets of Experience, to the output, waiting beside it if it's full (D22, D48,
	 * D57); then, on the way home, put every unsold item back in the input it came from (D62).
	 */
	public boolean fetchAndDeliver(Predicate<ItemStack> wanted) {
		if (!(bot.level().getBlockEntity(bot.getStation()) instanceof TradeBotStationBlockEntity station) || station.outputPosition() == null)
			return false;
		List<BlockPos> inputs = new ArrayList<>(station.inputPositions());
		if (!inputs.isEmpty())
			java.util.Collections.rotate(inputs, -(roundRobin++ % inputs.size()));
		job = new Job(inputs, station.outputPosition(), wanted);
		goals.clear();
		visited.clear();
		commanded = true;
		if (bot.isDocked())
			return leave();
		nextJobStep();
		return true;
	}

	/** Items taken and delivered on the current or last job. */
	public int taken() {
		return job == null ? 0 : job.taken;
	}

	public int delivered() {
		return job == null ? 0 : job.delivered;
	}

	public int returned() {
		return job == null ? 0 : job.returned;
	}

	/** Home to his station (wrench, redstone pause, packing, night...). */
	public void goHome() {
		if (task == Task.RETURNING || task == Task.DOCKING || task == Task.HOME && bot.isDocked())
			return;
		if (work != null) {
			work.abort();
			work = null;
		}
		bot.motion.stop();
		set(Task.RETURNING);
	}

	/** The next container: inputs with something wanted, then the output (if he has earnings), then returns; then home. */
	private void nextJobStep() {
		Job j = job;
		if (j == null) {
			set(Task.RETURNING);
			return;
		}
		if (j.phase == Phase.TAKING) {
			while (j.input < j.inputs.size() && !ContainerWork.full(bot.getLoad())) {
				BlockPos input = j.inputs.get(j.input);
				ContainerVisit visit = hasWanted(input, j.wanted) ? ContainerVisit.plan(bot, input) : null;
				if (visit != null && approach(visit, ContainerWork.Mode.TAKE, j.wanted))
					return;
				j.input++;
			}
			j.phase = Phase.DELIVERING; // (milestone 5 trades here)
		}
		if (j.phase == Phase.DELIVERING) {
			if (bot.hasEarnings()) {
				ContainerVisit visit = ContainerVisit.plan(bot, j.output);
				if (visit != null && approach(visit, ContainerWork.Mode.DELIVER_EARNINGS, stack -> false))
					return;
			}
			j.phase = Phase.RETURNING_ITEMS;
			j.returns.addAll(nearestFirst(new ArrayList<>(j.takenFrom.keySet())));
		}
		while (!isEmpty(bot.getLoad())) {
			if (j.returns.isEmpty()) {
				if (j.leftoversQueued)
					break; // every input is full: he keeps the rest for next time
				// left over (their own input was full): any input with room
				j.leftoversQueued = true;
				j.returns.addAll(nearestFirst(new ArrayList<>(j.inputs)));
			}
			BlockPos input = j.returns.poll();
			if (!carries(j.from(input)))
				continue;
			ContainerVisit visit = ContainerVisit.plan(bot, input);
			if (visit != null && approach(visit, ContainerWork.Mode.RETURN, j.from(input)))
				return;
		}
		set(Task.RETURNING);
	}

	private boolean approach(ContainerVisit visit, ContainerWork.Mode mode, Predicate<ItemStack> which) {
		if (!bot.motion.moveTo(visit.standBlock(), visit.stand(), visit.yaw()))
			return false;
		work = new ContainerWork(bot, visit, mode, which);
		if (com.comedymonkey.createaddons.content.tradebot.move.TradeBotMotion.debug)
			com.mojang.logging.LogUtils.getLogger().info("[Trade Bot {}] {} {} at {} from the {} side, standing at {}", bot.getId(), mode,
				visit.kind(), visit.container().toShortString(), visit.side(), visit.standBlock().toShortString());
		set(Task.APPROACHING);
		return true;
	}

	/** Orders containers so each next one is the nearest to the last (from where he is now). */
	private List<BlockPos> nearestFirst(List<BlockPos> containers) {
		List<BlockPos> ordered = new ArrayList<>();
		net.minecraft.world.phys.Vec3 from = bot.position();
		while (!containers.isEmpty()) {
			net.minecraft.world.phys.Vec3 at = from;
			BlockPos nearest = containers.stream().min(java.util.Comparator.comparingDouble(c -> c.getCenter().distanceToSqr(at))).orElseThrow();
			containers.remove(nearest);
			ordered.add(nearest);
			from = nearest.getCenter();
		}
		return ordered;
	}

	private boolean carries(Predicate<ItemStack> which) {
		for (int slot = 0; slot < bot.getLoad().getSlots(); slot++)
			if (!bot.getLoad().getStackInSlot(slot).isEmpty() && which.test(bot.getLoad().getStackInSlot(slot)))
				return true;
		return false;
	}

	private boolean hasWanted(BlockPos pos, Predicate<ItemStack> wanted) {
		var handler = bot.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, null);
		if (handler == null)
			return false;
		for (int slot = 0; slot < handler.getSlots(); slot++)
			if (!handler.getStackInSlot(slot).isEmpty() && wanted.test(handler.getStackInSlot(slot)))
				return true;
		return false;
	}

	private static boolean isEmpty(net.neoforged.neoforge.items.IItemHandler handler) {
		for (int slot = 0; slot < handler.getSlots(); slot++)
			if (!handler.getStackInSlot(slot).isEmpty())
				return false;
		return true;
	}

	// ---- ticking

	public void tick() {
		ticks++;
		BlockPos station = bot.getStation();
		if (station == null || !(bot.level().getBlockState(station).getBlock() instanceof TradeBotStationBlock)) {
			if (task != Task.STRANDED) {
				bot.motion.stop();
				set(Task.STRANDED);
			}
			return;
		}
		if ((task == Task.GOING || task == Task.WAITING || task == Task.APPROACHING || task == Task.WORKING || task == Task.WAITING_OUTPUT)
			&& shouldGoHome())
			goHome();
		switch (task) {
			case HOME -> {
				if (!bot.isDocked() && !bot.motion.isMoving())
					set(Task.RETURNING);
			}
			case LEAVING -> {
				Clip clip = bot.clip();
				if (!clip.name().equals(LEAVE) || clip.finished(bot.clipElapsed(0)))
					if (job != null)
						nextJobStep();
					else
						set(!goals.isEmpty() ? Task.GOING : Task.RETURNING);
			}
			case APPROACHING -> {
				ContainerWork w = work;
				if (w == null)
					nextJobStep();
				else if (bot.motion.arrived()) {
					ContainerVisit visit = w.visit();
					bot.moveTo(visit.stand().x, visit.stand().y, visit.stand().z, visit.yaw(), 0);
					bot.setYRot(visit.yaw());
					w.start();
					set(Task.WORKING);
				} else if (bot.motion.failed()) {
					work = null;
					if (job != null && job.phase == Phase.TAKING)
						job.input++;
					else if (job != null && job.phase == Phase.DELIVERING)
						job.phase = Phase.RETURNING_ITEMS; // can't reach the output: keep the earnings, return the items
					nextJobStep();
				}
			}
			case WORKING -> {
				ContainerWork w = work;
				if (w == null || w.tick()) {
					work = null;
					if (job != null && w != null) {
						switch (w.mode()) {
							case TAKE -> {
								job.taken += w.moved();
								for (ItemStack stack : w.movedStacks())
									job.takenFrom.computeIfAbsent(w.visit().container(), c -> new ArrayList<>()).add(stack.copyWithCount(1));
								job.input++;
							}
							case DELIVER_EARNINGS -> {
								job.delivered += w.moved();
								if (bot.hasEarnings()) { // the output's full: wait beside it, worried, and try again (D22)
									bot.playClip("expr_sad");
									set(Task.WAITING_OUTPUT);
									return;
								}
							}
							case RETURN -> job.returned += w.moved();
						}
					}
					nextJobStep();
				}
			}
			case WAITING_OUTPUT -> {
				if (ticks >= OUTPUT_RETRY_TICKS)
					nextJobStep();
			}
			case GOING -> {
				if (!routed) {
					routed = true;
					if (goals.isEmpty() || !bot.motion.moveTo(goals.peek(), null, Float.NaN))
						set(Task.RETURNING);
				} else if (bot.motion.arrived()) {
					visited.add(goals.poll());
					set(Task.WAITING);
					bot.playClip("count_stock");
				} else if (bot.motion.failed())
					set(Task.RETURNING);
			}
			case WAITING -> {
				if (ticks >= WAIT_TICKS)
					set(goals.isEmpty() ? Task.RETURNING : Task.GOING);
			}
			case RETURNING -> returnHome(station);
			case DOCKING -> {
				Clip clip = bot.clip();
				if (!clip.name().equals(TradeBotEntity.DOCKED) || clip.finished(bot.clipElapsed(0))) {
					bot.dockAt(station, facing(station));
					commanded = false;
					work = null;
					set(Task.HOME);
				}
			}
			case STRANDED -> set(Task.HOME);
		}
	}

	private void returnHome(BlockPos station) {
		Direction facing = facing(station);
		Vec3 start = dockStart(station, facing);
		if (!routed) {
			routed = true;
			if (!bot.motion.moveTo(BlockPos.containing(start), start, facing.toYRot()))
				homeFailed(station, facing);
			return;
		}
		if (bot.motion.failed()) {
			routed = false;
			homeFailed(station, facing);
		} else if (bot.motion.arrived()) {
			// right on the spot, facing away from the station: back in and hop up into the cradle
			bot.moveTo(start.x, start.y, start.z, facing.toYRot(), 0);
			bot.setYRot(facing.toYRot());
			bot.playClip(ClipLibrary.get().clip(TradeBotEntity.DOCKED));
			set(Task.DOCKING);
		}
	}

	private void homeFailed(BlockPos station, Direction facing) {
		if (++homeFailures >= MAX_HOME_FAILURES) {
			bot.dockAt(station, facing);
			set(Task.HOME);
		} else
			ticks = -40; // try again in a couple of seconds
	}

	/** Hops out of the cradle forward (D3). If the second block in front is blocked, the hop is shortened to the first. */
	private boolean leave() {
		BlockPos station = bot.getStation();
		if (station == null)
			return false;
		Direction facing = facing(station);
		Clip clip = ClipLibrary.get().clip(LEAVE);
		Vec3 from = bot.position();
		Vector3f travel = ClipRoot.travel(clip, clip.length(), new Vector3f());
		Vec3 landing = ClipRoot.place(from, facing.toYRot(), travel);
		float scale = 1;
		if (!standable(BlockPos.containing(landing))) {
			Vec3 front = Vec3.atBottomCenterOf(station.relative(facing));
			if (!standable(station.relative(facing)))
				return false;
			scale = (float) (horizontal(front.subtract(from)) / Math.abs(travel.z));
		}
		bot.setYRot(facing.toYRot());
		bot.leaveDock();
		bot.playClip(clip, 1, scale);
		set(Task.LEAVING);
		return true;
	}

	private boolean standable(BlockPos pos) {
		return WalkNodeEvaluator.getPathTypeStatic(bot, pos) == PathType.WALKABLE;
	}

	/** Where docking starts: on the ground in front of the station, facing away from it (the docking clip backs in). */
	public static Vec3 dockStart(BlockPos station, Direction facing) {
		Clip dock = ClipLibrary.get().clip(TradeBotEntity.DOCKED);
		Vec3 travelled = ClipRoot.place(Vec3.ZERO, facing.toYRot(), ClipRoot.travel(dock, dock.length(), new Vector3f()));
		return TradeBotStationBlock.dockPoint(station, facing).subtract(travelled);
	}

	/** Home rules: paused by redstone, night or a thunderstorm (unless sent by command), or charge running out. */
	private boolean shouldGoHome() {
		if (bot.level().getBlockEntity(bot.getStation()) instanceof TradeBotStationBlockEntity station && station.isPaused())
			return true;
		if (!commanded && (!bot.level().isDay() || bot.level().isThundering()))
			return true;
		double home = bot.position().distanceTo(Vec3.atCenterOf(bot.getStation()));
		return bot.getCharge() <= (home * 1.5 + 4) * CMConfig.SERVER.tradeBotFePerBlock();
	}

	private Direction facing(BlockPos station) {
		BlockState state = bot.level().getBlockState(station);
		return state.getValue(TradeBotStationBlock.FACING);
	}

	private void set(Task task) {
		this.task = task;
		ticks = 0;
		routed = false;
		if (task == Task.HOME)
			homeFailures = 0;
	}

	private static double horizontal(Vec3 v) {
		return Math.sqrt(v.x * v.x + v.z * v.z);
	}
}
