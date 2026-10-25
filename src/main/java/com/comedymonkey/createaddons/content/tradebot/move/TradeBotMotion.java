package com.comedymonkey.createaddons.content.tradebot.move;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.tradebot.TradeBotEntity;
import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipLibrary;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipRoot;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

/**
 * Rolls Trade Bot along a route with his clips (design/plans/trade-bot.md, milestone 3): he sets off with
 * {@code start_rolling}, cruises on the {@code rolling} loop, takes sharp corners with {@code turn_left} /
 * {@code turn_right}, reverses with {@code turn_around}, brakes with {@code stop_rolling}, and hops steps with the
 * hop and stairs clips, each stretched to land exactly on the next block. He opens doors and gates as he goes and
 * closes the ones he opened behind him (doors that were already open stay open), and pays for every block he rolls.
 */
public class TradeBotMotion {
	/** Cruising speed, blocks a tick (3.2 blocks a second). */
	public static final double CRUISE = 0.16;
	private static final int ACCELERATE_TICKS = 12, BRAKE_TICKS = 10;
	/** Close enough to a waypoint to move on to the next. */
	private static final double REACHED = 0.3;
	/** Heading changes that get a turn clip, and that need the hop-and-spin. */
	private static final float TURN_CLIP_ANGLE = 60, TURN_AROUND_ANGLE = 150;
	/** Rises and drops: up to this rolls (his step height), up to the next is a slab, then a block. */
	static final double ROLL_STEP = 0.26, SLAB_STEP = 0.6, BLOCK_STEP = 1.05;
	private static final float TURN_SPEED = 2, TURN_SLOWDOWN = 0.6f;
	/** In a turn-around clip: when the hop's landing is done (seconds), for spinning on the spot. */
	private static final float TURN_AROUND_LANDED = 1.45f;
	/** Fleeing a mob: the wheel at double cruise speed (D8), with the panicked roll instead of the rolling clips. */
	private static final double FLEE_FACTOR = 2;
	private static final String FLEE_CLIP = "scared_flee", BUMP_CLIP = "wall_bump";
	/** How long after one wall bump the next stuck moment is just a quiet replan (ticks). */
	private static final int BUMP_COOLDOWN = 200;

	enum Phase { IDLE, FACING, STARTING, ROLLING, TURNING, TURN_AROUND, SPIN, STOPPING, STEPPING, BUMPED }

	public enum Step { SLAB_UP, BLOCK_UP, STAIRS_UP, SLAB_DOWN, BLOCK_DOWN, STAIRS_DOWN }

	private final TradeBotEntity bot;
	private Phase phase = Phase.IDLE;
	private int phaseTicks;
	private double speed;
	@Nullable
	private Path path;
	private List<Vec3> points = List.of();
	/** The route's blocks (the same length as {@link #points}). */
	private List<BlockPos> nodes = List.of();
	/**
	 * Worked out once per route: the step onto each waypoint (null to roll it), the first step at or after each
	 * waypoint ({@code points.size()} if none), and the distance rolled from the first waypoint to each.
	 */
	private Step[] steps = new Step[0];
	private int[] nextStep = new int[0];
	private double[] along = new double[0];
	/** The last hops taken, from block to block (for checking that they're all square-on). */
	private final List<Hop> hops = new ArrayList<>();
	private static final int MAX_HOPS = 64;

	public record Hop(BlockPos from, BlockPos to) {}
	private int next;
	/** Where to stop exactly at the end, and which way to face there (NaN: whichever way he arrived). */
	@Nullable
	private Vec3 finalPoint;
	private float finalYaw = Float.NaN;
	private boolean failed, noWay;
	/** Doors and gates he opened, with the waypoint each is on (-1 once that route is gone): he shuts them once past. */
	private final Map<BlockPos, Integer> openedDoors = new HashMap<>();
	private double travelled;
	@Nullable
	private Vec3 lastPos;
	/** Progress check: where he was at the start of the current one-second window, and blocked windows in a row. */
	@Nullable
	private Vec3 windowStart;
	private int windowTicks, blockedWindows, replans;
	/** When he last searched for a route (on his clip clock), for one search a tick at most. */
	private long plannedAt = Long.MIN_VALUE;
	@Nullable
	private BlockPos target;
	private boolean fleeing;
	private long lastBump = Long.MIN_VALUE / 2;
	/** Logs his phases and route troubles ("/tradebot debug"), for working out what he did. */
	public static boolean debug;
	private static final Logger LOGGER = LogUtils.getLogger();

	public TradeBotMotion(TradeBotEntity bot) {
		this.bot = bot;
	}

	// ---- orders

	/** What came of asking for a route: one to follow, none to be had, or not this tick (he's planned one already). */
	public enum Route {
		PLANNED, NONE, LATER;

		public boolean planned() {
			return this == PLANNED;
		}
	}

	/**
	 * Plans a route to stand on {@code target}, optionally at an exact point facing {@code yaw}. A route search is
	 * the dearest thing he does, so he makes one a tick at most: a second ask in the same tick is {@link Route#LATER},
	 * to ask again next tick (it says nothing about whether there's a way).
	 */
	public Route moveTo(BlockPos target, @Nullable Vec3 exact, float yaw) {
		if (!canPlan())
			return Route.LATER;
		replans = 0;
		return plan(target, exact, yaw);
	}

	/** No route search yet this tick. */
	private boolean canPlan() {
		return bot.now() != plannedAt;
	}

	private Route plan(BlockPos target, @Nullable Vec3 exact, float yaw) {
		this.target = target;
		plannedAt = bot.now();
		Path planned = bot.getNavigation().createPath(target, 0);
		bot.getNavigation().stop();
		if (planned == null || !planned.canReach()) {
			if (debug)
				log("no route to " + target.toShortString() + (planned == null ? " (none)" : " (only partway)"));
			return Route.NONE;
		}
		path = planned;
		List<BlockPos> route = new ArrayList<>();
		for (int i = 0; i < planned.getNodeCount(); i++)
			route.add(planned.getNodePos(i));
		nodes = squareSteps(bot.level(), route);
		List<Vec3> list = new ArrayList<>();
		for (BlockPos node : nodes)
			list.add(floorCentre(bot.level(), node));
		points = list;
		measure();
		next = 0;
		finalPoint = exact != null ? exact : list.get(list.size() - 1);
		finalYaw = yaw;
		failed = false;
		windowStart = null;
		blockedWindows = 0;
		openedDoors.replaceAll((pos, i) -> -1); // a new route: its waypoints don't match the old ones
		if (debug)
			log("route to " + target.toShortString() + ": " + list.size() + " waypoints");
		skipReached();
		if (phase == Phase.IDLE || phase == Phase.STOPPING)
			setPhase(Phase.FACING);
		return Route.PLANNED;
	}

	/** Fleeing: twice as fast, in a panic, straight for it (no turn clips). */
	public void setFleeing(boolean fleeing) {
		this.fleeing = fleeing;
	}

	/**
	 * Stops dead where he is, with no clip of its own, so a reaction can play straight away. Not mid-hop: check
	 * {@link #canHalt()} first.
	 */
	public void halt() {
		path = null;
		points = List.of();
		finalPoint = null;
		failed = false;
		speed = 0;
		bot.setDeltaMovement(0, bot.getDeltaMovement().y, 0);
		if (debug)
			log(phase + " -> halted");
		phase = Phase.IDLE;
		phaseTicks = 0;
		// no route now to be past them on: shut the doors he opened ahead, unless he's in the way, and those once he's clear
		openedDoors.entrySet().removeIf(entry -> !inTheWay(entry.getKey()) && shut(entry.getKey()));
		openedDoors.replaceAll((pos, i) -> -1);
	}

	/** Not hopping (a physical clip carries him until it lands). */
	public boolean canHalt() {
		return phase != Phase.STEPPING && bot.clip().root() != Clip.Root.PHYSICAL;
	}

	/** Stops wherever he is (braking if he's rolling). */
	public void stop() {
		path = null;
		points = List.of();
		finalPoint = null;
		if (phase == Phase.ROLLING || phase == Phase.STARTING || phase == Phase.TURNING)
			brake();
		else if (phase != Phase.STEPPING && phase != Phase.TURN_AROUND)
			setPhase(Phase.IDLE);
	}

	public boolean isMoving() {
		return phase != Phase.IDLE;
	}

	/** Arrived and stopped (and facing the asked way). */
	public boolean arrived() {
		return phase == Phase.IDLE && path == null && !failed;
	}

	public boolean failed() {
		return failed;
	}

	/** He gave up because a fresh route search found no way there (not just stuck, behind someone, say). */
	public boolean foundNoWay() {
		return failed && noWay;
	}

	// ---- ticking (server, from the entity's AI step)

	public void tick() {
		Level level = bot.level();
		phaseTicks++;
		payForTravel();
		closeDoorsBehind();
		Clip clip = bot.clip();
		float t = bot.clipElapsed(0);
		switch (phase) {
			case IDLE -> drive(0, null);
			case FACING -> face();
			case STARTING -> {
				speed = cruise() * Math.min(1, phaseTicks / (double) ACCELERATE_TICKS);
				follow();
				if (clip.finished(t))
					setPhase(Phase.ROLLING);
			}
			case ROLLING -> {
				speed = cruise();
				follow();
			}
			case TURNING -> {
				speed = cruise() * TURN_SLOWDOWN;
				follow();
				if (clip.finished(t))
					setPhase(Phase.ROLLING);
			}
			case TURN_AROUND -> turnAround(clip, t, false);
			case SPIN -> turnAround(clip, t, true);
			case STOPPING -> {
				Vec3 stopAt = stopPoint();
				speed = cruise() * Math.max(0, 1 - phaseTicks / (double) BRAKE_TICKS);
				if (stopAt != null) {
					double dist = horizontal(stopAt.subtract(bot.position()));
					speed = Math.max(speed, Math.min(dist, 0.04)); // creep the last bit, so he stops right on the spot
					if (dist < 0.02)
						speed = 0;
					drive(speed, stopAt);
				} else
					drive(speed, null);
				if (speed == 0 && (fleeing || clip.finished(t)))
					arriveOrStep();
			}
			case BUMPED -> {
				drive(0, null);
				if (!clip.name().equals(BUMP_CLIP) || clip.finished(t))
					replan();
			}
			case STEPPING -> {
				drive(0, null);
				if (debug && phaseTicks % 10 == 1)
					log("hop t=" + String.format(java.util.Locale.ROOT, "%.2f", t) + " at " + fmt(bot.position()) + " path " + fmt(bot.rootPosition(clip, clip.localTime(t)))
						+ " anchor " + fmt(bot.clipAnchor()) + " yaw " + bot.clipAnchorYaw() + " clip " + clip.name());
				if (clip.finished(t)) {
					if (debug)
						log("landed at " + fmt(bot.position()));
					next++;
					skipReached();
					setPhase(next < points.size() ? Phase.FACING : Phase.IDLE);
					if (next >= points.size())
						path = null;
				}
			}
		}
		if (level.getGameTime() % 5 == 0)
			openDoorsAhead();
	}

	/** Heads for the next waypoint, taking corners, braking for steps and for the end. */
	private void follow() {
		if (points.isEmpty() || next >= points.size()) {
			brake();
			return;
		}
		Vec3 pos = bot.position();
		Vec3 target = points.get(next);
		if (horizontal(target.subtract(pos)) < REACHED) {
			next++;
			if (next >= points.size()) {
				brake();
				return;
			}
			if (stepTo(next) != null) {
				brake();
				return;
			}
			float turn = Mth.wrapDegrees(yawTo(pos, points.get(next)) - bot.getYRot());
			if (fleeing) {
				// no time for turn clips: he just swerves
			} else if (Math.abs(turn) >= TURN_AROUND_ANGLE)
				setPhase(Phase.TURN_AROUND);
			else if (Math.abs(turn) >= TURN_CLIP_ANGLE && phase != Phase.TURNING) {
				bot.playClip(ClipLibrary.get().clip(turn < 0 ? "turn_left" : "turn_right"), TURN_SPEED, 1);
				phase = Phase.TURNING;
				phaseTicks = 0;
			}
			target = points.get(next);
		}
		if (remainingToStop() <= brakingDistance()) {
			brake();
			return;
		}
		drive(speed, target);
		checkStuck();
	}

	/** Rolls toward a point at {@code speed} (blocks a tick), turning to face where he goes. */
	private void drive(double speed, @Nullable Vec3 toward) {
		Vec3 motion = bot.getDeltaMovement();
		if (toward == null || speed <= 0) {
			bot.setDeltaMovement(0, motion.y, 0);
			return;
		}
		Vec3 d = toward.subtract(bot.position());
		double len = horizontal(d);
		if (len < 1e-4) {
			bot.setDeltaMovement(0, motion.y, 0);
			return;
		}
		double s = Math.min(speed, len);
		bot.setDeltaMovement(d.x / len * s, motion.y, d.z / len * s);
		if (phase != Phase.STOPPING || len > 0.1)
			turnToward(yawTo(bot.position(), toward), 12);
	}

	/** Before setting off: faces the way to go (the hop-and-spin for a reversal), then starts rolling or hops. */
	private void face() {
		drive(0, null);
		if (next >= points.size()) {
			arriveOrStep();
			return;
		}
		Step step = stepTo(next);
		float want = yawTo(bot.position(), points.get(next));
		float diff = Mth.wrapDegrees(want - bot.getYRot());
		if (step == null && Math.abs(diff) >= TURN_AROUND_ANGLE && !fleeing) {
			setPhase(Phase.TURN_AROUND);
			return;
		}
		if (Math.abs(diff) > 5) {
			turnToward(want, fleeing ? 40 : 15);
			return;
		}
		bot.setYRot(want);
		if (step != null)
			startStep(step);
		else
			setPhase(fleeing ? Phase.ROLLING : Phase.STARTING);
	}

	/** The hop-and-spin, following the clip's yaw and speed; on the spot it ends once he's landed. */
	private void turnAround(Clip clip, float t, boolean onTheSpot) {
		float startYaw = bot.clipAnchorYaw();
		bot.setYRot(ClipRoot.facing(startYaw, ClipRoot.turned(clip, t)));
		if (onTheSpot) {
			drive(0, null);
			if (t >= TURN_AROUND_LANDED || clip.finished(t)) {
				bot.setYRot(finalYaw);
				setPhase(Phase.IDLE);
				path = null;
			}
			return;
		}
		// brakes into the hop, powers off after it, like the clip
		float profile = 1 - smooth(t, 0.05f, 0.6f) + smooth(t, 1.37f, 2.3f);
		speed = cruise() * profile;
		// straight through the spin; once he's landed, steer for the route as he powers off (not past it)
		if (t >= TURN_AROUND_LANDED && next < points.size()) {
			skipReached();
			drive(speed, points.get(next));
		} else if (next < points.size())
			driveStraight(speed);
		if (clip.finished(t))
			setPhase(Phase.ROLLING);
	}

	private void driveStraight(double speed) {
		float r = bot.getYRot() * Mth.DEG_TO_RAD;
		bot.setDeltaMovement(-Mth.sin(r) * speed, bot.getDeltaMovement().y, Mth.cos(r) * speed);
	}

	private void brake() {
		if (phase == Phase.STOPPING)
			return;
		setPhase(Phase.STOPPING);
	}

	/** Stopped: hop the step ahead, or finish (turning to the asked facing). */
	private void arriveOrStep() {
		if (path != null) {
			// stopped on the waypoint before a step: the step is next
			if (next < points.size() - 1 && horizontal(points.get(next).subtract(bot.position())) < REACHED && stepTo(next + 1) != null)
				next++;
			if (next < points.size() && stepTo(next) != null) {
				setPhase(Phase.FACING);
				return;
			}
		}
		if (path != null && !Float.isNaN(finalYaw)) {
			float diff = Mth.wrapDegrees(finalYaw - bot.getYRot());
			if (Math.abs(diff) >= TURN_AROUND_ANGLE) {
				setPhase(Phase.SPIN);
				return;
			}
			if (Math.abs(diff) > 4) {
				turnToward(finalYaw, 15);
				return;
			}
			bot.setYRot(finalYaw);
		}
		path = null;
		setPhase(Phase.IDLE);
	}

	private void startStep(Step step) {
		Clip clip = ClipLibrary.get().clip(switch (step) {
			case SLAB_UP -> "hop_on_slab";
			case BLOCK_UP -> "hop_on_block";
			case STAIRS_UP -> "stairs_up";
			case SLAB_DOWN -> "hop_off_slab";
			case BLOCK_DOWN -> "hop_off_block";
			case STAIRS_DOWN -> "stairs_down";
		});
		Vec3 to = points.get(next);
		// stretch the clip's ground travel so he lands on the next block's centre
		double want = horizontal(to.subtract(bot.position()));
		double has = Math.abs(ClipRoot.travel(clip, clip.length(), new Vector3f()).z);
		bot.playClip(clip, 1, has > 1e-3 ? (float) (want / has) : 1);
		if (hops.size() >= MAX_HOPS)
			hops.remove(0);
		hops.add(new Hop(nodes.get(next - 1), nodes.get(next)));
		if (debug)
			log(step + " from " + fmt(bot.position()) + " to " + fmt(to) + ": " + String.format(java.util.Locale.ROOT, "%.2f", want) + " of "
			+ String.format(java.util.Locale.ROOT, "%.2f", has) + " (waypoint " + next + ")");
		setPhase(Phase.STEPPING, false);
	}

	// ---- route helpers

	/** The hops he has taken lately (newest last). */
	public List<Hop> hops() {
		return List.copyOf(hops);
	}

	/**
	 * Hops are always square-on: a diagonal step up or down gets a waypoint beside it at the level he's on, so he rolls
	 * there first and hops straight, clearing the edge the way the clips do. (Vanilla routes may cut corners diagonally.)
	 */
	List<BlockPos> squareSteps(Level level, List<BlockPos> route) {
		List<BlockPos> out = new ArrayList<>();
		for (int i = 0; i < route.size(); i++) {
			BlockPos node = route.get(i);
			if (i > 0) {
				BlockPos prev = out.get(out.size() - 1);
				double rise = floorCentre(level, node).y - floorCentre(level, prev).y;
				if (Math.abs(rise) > ROLL_STEP && prev.getX() != node.getX() && prev.getZ() != node.getZ()) {
					// roll beside the step on this level, then hop square; or, if there's no room here, hop square onto the
					// block beside it on the other level and roll the rest
					BlockPos beside = besideOnLevel(level, prev, node, prev);
					if (beside == null)
						beside = besideOnLevel(level, prev, node, node);
					if (beside != null)
						out.add(beside);
				}
			}
			out.add(node);
		}
		return out;
	}

	/** A block next to both {@code from} and {@code to} (diagonal neighbours), on {@code onLevelOf}'s level, he can stand on. */
	@Nullable
	private BlockPos besideOnLevel(Level level, BlockPos from, BlockPos to, BlockPos onLevelOf) {
		double floor = floorCentre(level, onLevelOf).y;
		int y = onLevelOf.getY();
		for (BlockPos beside : List.of(new BlockPos(to.getX(), y, from.getZ()), new BlockPos(from.getX(), y, to.getZ()))) {
			if (WalkNodeEvaluator.getPathTypeStatic(bot, beside) == net.minecraft.world.level.pathfinder.PathType.WALKABLE
				&& Math.abs(floorCentre(level, beside).y - floor) <= ROLL_STEP)
				return beside;
		}
		return null;
	}

	/** The step from waypoint {@code i - 1} to {@code i}, or null if he can roll it. */
	@Nullable
	Step stepTo(int i) {
		if (i <= 0 || i >= points.size() || path == null)
			return null;
		return steps[i];
	}

	/** Works out the new route's steps and distances, so nothing has to look along it again while he rolls. */
	private void measure() {
		int n = points.size();
		steps = new Step[n];
		for (int i = 1; i < n; i++)
			steps[i] = classify(points.get(i).y - points.get(i - 1).y, isStairs(nodes.get(i)), isStairs(nodes.get(i - 1)));
		nextStep = new int[n + 1];
		nextStep[n] = n;
		for (int i = n - 1; i >= 0; i--)
			nextStep[i] = steps[i] != null ? i : nextStep[i + 1];
		along = new double[n];
		for (int i = 1; i < n; i++)
			along[i] = along[i - 1] + horizontal(points.get(i).subtract(points.get(i - 1)));
	}

	/** The first step at or after waypoint {@code from} (never the first waypoint), or {@code points.size()} if none. */
	private int nextStepFrom(int from) {
		return nextStep[Math.min(Math.max(from, 1), points.size())];
	}

	/** What a change in floor height needs: nothing (roll), a slab hop, a block hop or a stairs bounce. */
	@Nullable
	public static Step classify(double rise, boolean upOntoStairs, boolean downOffStairs) {
		if (Math.abs(rise) <= ROLL_STEP)
			return null;
		if (rise > 0)
			return rise <= SLAB_STEP ? Step.SLAB_UP : upOntoStairs ? Step.STAIRS_UP : Step.BLOCK_UP;
		return -rise <= SLAB_STEP ? Step.SLAB_DOWN : downOffStairs ? Step.STAIRS_DOWN : Step.BLOCK_DOWN;
	}

	private boolean isStairs(BlockPos node) {
		return bot.level().getBlockState(node.below()).getBlock() instanceof StairBlock;
	}

	/** The centre of a waypoint block, at its floor height (a slab's top, a stair's top...). */
	static Vec3 floorCentre(Level level, BlockPos node) {
		return new Vec3(node.getX() + 0.5, WalkNodeEvaluator.getFloorLevel(level, node), node.getZ() + 0.5);
	}

	/** Where the current brake should end: the waypoint before a step, or the end of the route. */
	@Nullable
	private Vec3 stopPoint() {
		if (path == null || points.isEmpty())
			return null;
		int step = nextStepFrom(next);
		return step < points.size() ? points.get(step - 1) : finalPoint;
	}

	/** How far he has still to roll to where the current brake should end, along the route. */
	private double remainingToStop() {
		Vec3 stop = stopPoint();
		if (stop == null)
			return 0;
		int n = points.size();
		if (next >= n)
			return -horizontal(points.get(n - 1).subtract(stop));
		Vec3 pos = bot.position();
		int step = nextStepFrom(next);
		if (step == next) // the step is the very next waypoint: he stops on the one before it
			return horizontal(points.get(step - 1).subtract(pos));
		double toNext = horizontal(points.get(next).subtract(pos));
		if (step < n)
			return toNext + along[step - 1] - along[next];
		return toNext + along[n - 1] - along[next] - horizontal(points.get(n - 1).subtract(stop));
	}

	private double brakingDistance() {
		return speed * BRAKE_TICKS / 2 + 0.05;
	}

	/** Moves past waypoints he's on, or already past (nearer the next one than they are), unless a step follows. */
	private void skipReached() {
		Vec3 pos = bot.position();
		while (next < points.size() - 1 && stepTo(next + 1) == null) {
			Vec3 here = points.get(next), after = points.get(next + 1);
			boolean on = horizontal(here.subtract(pos)) < REACHED;
			boolean past = horizontal(after.subtract(pos)) < horizontal(after.subtract(here));
			if (!on && !past)
				break;
			next++;
		}
	}

	/** Rolling but getting nowhere for two seconds: plan again (twice), then give up. */
	private void checkStuck() {
		Vec3 pos = bot.position();
		if (windowStart == null) {
			windowStart = pos;
			windowTicks = 0;
			return;
		}
		if (++windowTicks < 20)
			return;
		boolean blocked = horizontal(pos.subtract(windowStart)) < CRUISE * 20 * 0.2;
		windowStart = pos;
		windowTicks = 0;
		blockedWindows = blocked ? blockedWindows + 1 : 0;
		if (blockedWindows < 2)
			return;
		// bumped into something: a wall-bump reaction first (now and then), then plan again
		long now = bot.level().getGameTime();
		if (!fleeing && now - lastBump >= BUMP_COOLDOWN) {
			lastBump = now;
			if (debug)
				log("bumped into something at " + bot.blockPosition().toShortString());
			setPhase(Phase.BUMPED, false);
			bot.playClip(BUMP_CLIP);
			return;
		}
		replan();
	}

	private void replan() {
		if (target != null && replans < 2 && !canPlan())
			return; // he planned a route this tick already: the next stuck moment tries again
		if (debug)
			log("stuck at " + bot.blockPosition().toShortString() + ", replanning");
		Vec3 exact = finalPoint;
		float yaw = finalYaw;
		Route route = target != null && replans++ < 2 ? plan(target, exact, yaw) : null;
		if (route == Route.PLANNED) {
			setPhase(Phase.FACING);
			return;
		}
		log("giving up");
		failed = true;
		noWay = route == Route.NONE;
		path = null;
		points = List.of();
		setPhase(Phase.IDLE);
	}

	private static String fmt(Vec3 v) {
		return String.format(java.util.Locale.ROOT, "%.2f %.2f %.2f", v.x, v.y, v.z);
	}

	private void log(String message) {
		if (debug)
			LOGGER.info("[Trade Bot {}] {}", bot.getId(), message);
	}

	/** He was moved there, not rolled (put back at his station, docked): nothing to pay for the jump. */
	public void resetTravel() {
		lastPos = null;
	}

	private void payForTravel() {
		Vec3 pos = bot.position();
		if (lastPos != null)
			travelled += horizontal(pos.subtract(lastPos)) + Math.abs(pos.y - lastPos.y);
		lastPos = pos;
		int blocks = (int) travelled;
		if (blocks > 0) {
			travelled -= blocks;
			bot.addCharge(-blocks * CMConfig.SERVER.tradeBotFePerBlock());
		}
	}

	// ---- doors and gates (D28)

	private void openDoorsAhead() {
		if (path == null)
			return;
		Level level = bot.level();
		for (int i = next; i < Math.min(points.size(), next + 3); i++) {
			BlockPos node = nodes.get(i);
			if (bot.position().distanceTo(Vec3.atBottomCenterOf(node)) > 2.2)
				continue;
			for (BlockPos pos : List.of(node, node.above())) {
				BlockState state = level.getBlockState(pos);
				if (state.getBlock() instanceof DoorBlock door && door.type().canOpenByHand() && !door.isOpen(state)) {
					door.setOpen(bot, level, state, pos, true);
					openedDoors.put(pos.immutable(), i);
					if (debug)
						log("opened the door at " + pos.toShortString());
				} else if (state.getBlock() instanceof FenceGateBlock && !state.getValue(FenceGateBlock.OPEN)) {
					level.setBlock(pos, state.setValue(FenceGateBlock.OPEN, true), 10);
					level.playSound(null, pos, SoundEvents.FENCE_GATE_OPEN, SoundSource.BLOCKS, 1, 1);
					openedDoors.put(pos.immutable(), i);
					if (debug)
						log("opened the gate at " + pos.toShortString());
				}
			}
		}
	}

	private void closeDoorsBehind() {
		openedDoors.entrySet().removeIf(entry -> {
			BlockPos pos = entry.getKey();
			int at = entry.getValue();
			// through it: past the waypoint after it on this route (or well clear of it), and not touching it
			boolean past = at >= 0 ? next > at + 1 : bot.position().distanceTo(Vec3.atCenterOf(pos)) > 2.5;
			if (!past || inTheWay(pos))
				return false;
			return shut(pos);
		});
	}

	/**
	 * Gone from the world: shuts the doors and gates he opened, as nothing else will, except any he's standing in. Returns
	 * the ones left open (to save, and shut once he's back and clear of them), and forgets them all.
	 */
	public List<BlockPos> closeOpenedDoors() {
		List<BlockPos> left = new ArrayList<>();
		for (BlockPos pos : openedDoors.keySet())
			if (inTheWay(pos) || !bot.level().isLoaded(pos))
				left.add(pos);
			else
				shut(pos);
		openedDoors.clear();
		return left;
	}

	/** The doors and gates he has open right now. */
	public List<BlockPos> openedDoors() {
		return List.copyOf(openedDoors.keySet());
	}

	/** A door or gate he opened before he was unloaded: shut once he's clear of it, like any other. */
	public void adoptOpenedDoor(BlockPos pos) {
		openedDoors.putIfAbsent(pos.immutable(), -1);
	}

	/** A door or fence gate (something he opens as he goes). */
	public static boolean isDoor(BlockState state) {
		return state.getBlock() instanceof DoorBlock || state.getBlock() instanceof FenceGateBlock;
	}

	/** Touching it, or too close to swing it shut. */
	private boolean inTheWay(BlockPos pos) {
		return bot.getBoundingBox().inflate(0.4).intersects(new net.minecraft.world.phys.AABB(pos)) || bot.position().distanceTo(Vec3.atCenterOf(pos)) < 1.5;
	}

	/** Shuts a door or gate he opened, if it's still open (and loaded); true, as it's done with either way. */
	private boolean shut(BlockPos pos) {
		Level level = bot.level();
		if (!level.isLoaded(pos))
			return true;
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof DoorBlock door && door.isOpen(state))
			door.setOpen(bot, level, state, pos, false);
		else if (state.getBlock() instanceof FenceGateBlock && state.getValue(FenceGateBlock.OPEN)) {
			level.setBlock(pos, state.setValue(FenceGateBlock.OPEN, false), 10);
			level.playSound(null, pos, SoundEvents.FENCE_GATE_CLOSE, SoundSource.BLOCKS, 1, 1);
		}
		return true;
	}

	// ---- phases and clips

	private void setPhase(Phase phase) {
		setPhase(phase, true);
	}

	private double cruise() {
		return fleeing ? CRUISE * FLEE_FACTOR : CRUISE;
	}

	private void setPhase(Phase phase, boolean playClip) {
		if (debug && this.phase != phase)
			log(this.phase + " -> " + phase + " at " + String.format(java.util.Locale.ROOT, "%.2f %.2f %.2f", bot.getX(), bot.getY(), bot.getZ())
				+ " yaw " + Math.round(bot.getYRot()) + " waypoint " + next + "/" + points.size());
		this.phase = phase;
		phaseTicks = 0;
		windowStart = null;
		blockedWindows = 0;
		// setting off from a held physical clip (hopped out of his station, landed a hop): let go of it, or it keeps
		// pinning him where it ended
		if (phase == Phase.FACING && bot.clip().root() == Clip.Root.PHYSICAL)
			bot.playClip(TradeBotEntity.IDLE);
		if (!playClip)
			return;
		if (fleeing && (phase == Phase.STARTING || phase == Phase.ROLLING || phase == Phase.TURNING || phase == Phase.STOPPING)) {
			if (!bot.clip().name().equals(FLEE_CLIP))
				bot.playClip(FLEE_CLIP);
			return;
		}
		switch (phase) {
			case STARTING -> bot.playClip("start_rolling");
			case ROLLING -> bot.playClip("rolling");
			case TURN_AROUND, SPIN -> bot.playClip("turn_around");
			case STOPPING -> bot.playClip("stop_rolling");
			case IDLE -> {
				speed = 0;
				if (bot.clip().root() != Clip.Root.PHYSICAL || bot.clip().mode() != Clip.Mode.HOLD)
					bot.playClip(TradeBotEntity.IDLE);
			}
			default -> {}
		}
		if (phase == Phase.TURN_AROUND || phase == Phase.SPIN)
			bot.setClipAnchorYaw(bot.getYRot());
	}

	private void turnToward(float yaw, float maxDegrees) {
		float diff = Mth.wrapDegrees(yaw - bot.getYRot());
		bot.setYRot(bot.getYRot() + Mth.clamp(diff, -maxDegrees, maxDegrees));
	}

	/** Minecraft yaw that faces from one point to another. */
	public static float yawTo(Vec3 from, Vec3 to) {
		return (float) (Mth.atan2(-(to.x - from.x), to.z - from.z) * Mth.RAD_TO_DEG);
	}

	private static double horizontal(Vec3 v) {
		return Math.sqrt(v.x * v.x + v.z * v.z);
	}

	private static float smooth(float t, float a, float b) {
		float u = Mth.clamp((t - a) / (b - a), 0, 1);
		return u * u * (3 - 2 * u);
	}
}
