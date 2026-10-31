package com.comedymonkey.createaddons.content.tube;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.config.CMConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How Clockwork Tubes join (design/specs/clockwork-tubes.md D5, D16, D29, D30). A tube joins a tube beside it, an Inlet
 * whose back faces it, or a Receiver whose input faces it, unless the joint is shut with a wrench (on either side) or
 * refused: joined, it would bring two Receivers into one network, or grow a Receiver's network past the most tubes. A
 * refused or shut joint shows capped. A side is worked out when what's on that side changes; a refused joint is looked
 * at again by its tube now and then, so it opens once its reason has gone ({@link ClockworkTubeBlockEntity}).
 */
public final class TubeJoints {
	private TubeJoints() {}

	/** Why a joint is capped (for the goggles). */
	public enum Refusal {
		NONE, SHUT, TWO_RECEIVERS, TOO_LONG
	}

	/** How many changes a level remembers where they were (a Receiver further behind reads its network again anyway). */
	private static final int REMEMBERED = 4096;

	/** A level's changes: how many there have been, and where the last {@link #REMEMBERED} were. */
	private static final class Changes {
		int version;
		final long[] at = new long[REMEMBERED];
	}

	/** Each level's tube, Inlet and Receiver changes (placed, broken, turned, joints changed, loaded). */
	private static final Map<LevelAccessor, Changes> CHANGES = new WeakHashMap<>();

	public static int version(LevelAccessor level) {
		synchronized (CHANGES) {
			Changes c = CHANGES.get(level);
			return c == null ? 0 : c.version;
		}
	}

	/** Something at {@code pos} changed: the Receivers whose networks it's on or beside read them again. */
	public static void changed(LevelAccessor level, BlockPos pos) {
		synchronized (CHANGES) {
			Changes c = CHANGES.computeIfAbsent(level, l -> new Changes());
			c.version++;
			c.at[Math.floorMod(c.version, REMEMBERED)] = pos.asLong();
		}
	}

	/** Where the changes after {@code since} were, or null if there have been too many since to remember. */
	@Nullable
	public static long[] changesSince(LevelAccessor level, int since) {
		synchronized (CHANGES) {
			Changes c = CHANGES.get(level);
			int now = c == null ? 0 : c.version, n = now - since;
			if (n < 0 || n > REMEMBERED)
				return null;
			long[] out = new long[n];
			for (int i = 0; i < n; i++)
				out[i] = c.at[Math.floorMod(since + 1 + i, REMEMBERED)];
			return out;
		}
	}

	static boolean loaded(LevelAccessor level, BlockPos pos) {
		return !(level instanceof Level l) || l.isLoaded(pos);
	}

	/** Whether a tube at {@code pos} could join what's on its {@code side}. Never loads a chunk. */
	public static boolean joinable(LevelAccessor level, BlockPos pos, Direction side) {
		BlockPos n = pos.relative(side);
		if (!loaded(level, n))
			return false;
		return joinable(level.getBlockState(n), side);
	}

	/** Whether a tube joins {@code neighbour} on its {@code side}: a tube, or an Inlet's back or Receiver's input facing it. */
	static boolean joinable(BlockState neighbour, Direction side) {
		if (neighbour.getBlock() instanceof ClockworkTubeBlock)
			return true;
		// an Inlet's back, or a Receiver's input, faces this tube: their facing points away from it
		if (neighbour.getBlock() instanceof ClockworkInletBlock || neighbour.getBlock() instanceof ClockworkReceiverBlock)
			return neighbour.getValue(ClockworkInletBlock.FACING) == side;
		return false;
	}

	/** What a joint on a tube reaches: its tubes and Receivers (at most the most tubes and one more, then it stops). */
	public record Reach(Set<BlockPos> tubes, Set<BlockPos> receivers) {}

	/**
	 * The tubes and Receivers reached from the tube at {@code start} through joints open on both sides, never crossing
	 * the joint on {@code skip}'s side of {@code skipAt} (either way). {@code startJoints}: the start's joints, where its
	 * block state doesn't have them yet (it's being placed, or this side is being worked out again).
	 */
	static Reach reach(LevelAccessor level, BlockPos start, BlockPos skipAt, Direction skip, @Nullable Map<Direction, TubeJoint> startJoints,
					   int limit) {
		Set<BlockPos> tubes = new HashSet<>(), receivers = new HashSet<>();
		BlockPos skipOther = skipAt.relative(skip);
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		tubes.add(start.immutable());
		queue.add(start.immutable());
		while (!queue.isEmpty() && tubes.size() <= limit) {
			BlockPos p = queue.poll();
			BlockState ps = level.getBlockState(p);
			boolean isStart = p.equals(start) && startJoints != null;
			for (Direction side : Direction.values()) {
				TubeJoint joint = isStart ? startJoints.getOrDefault(side, TubeJoint.NONE)
					: ps.getBlock() instanceof ClockworkTubeBlock ? ps.getValue(ClockworkTubeBlock.JOINTS.get(side)) : TubeJoint.NONE;
				if (joint != TubeJoint.OPEN)
					continue;
				if ((p.equals(skipAt) && side == skip) || (p.equals(skipOther) && side == skip.getOpposite()))
					continue;
				BlockPos n = p.relative(side);
				if (!loaded(level, n))
					continue;
				BlockState s = level.getBlockState(n);
				if (s.getBlock() instanceof ClockworkTubeBlock) {
					// both sides must say joined, except from the tube being worked out (its neighbours don't know it yet)
					if (!p.equals(start) && s.getValue(ClockworkTubeBlock.JOINTS.get(side.getOpposite())) != TubeJoint.OPEN)
						continue;
					if (tubes.add(n.immutable()))
						queue.add(n.immutable());
				} else if (s.getBlock() instanceof ClockworkReceiverBlock && s.getValue(ClockworkReceiverBlock.FACING) == side) {
					receivers.add(n.immutable());
				}
			}
		}
		return new Reach(tubes, receivers);
	}

	/**
	 * Whether the joint on {@code side} of the tube at {@code pos} must be refused, given the tube's other joints. The
	 * same answer from either side of the joint, so the two tubes agree. A Receiver joining a long run of tubes is never
	 * refused for its length: it serves the nearest of them (D30).
	 */
	public static Refusal refusal(LevelAccessor level, BlockPos pos, Direction side, @Nullable Map<Direction, TubeJoint> joints) {
		BlockPos n = pos.relative(side);
		BlockState ns = level.getBlockState(n);
		boolean receiver = ns.getBlock() instanceof ClockworkReceiverBlock;
		if (!receiver && !(ns.getBlock() instanceof ClockworkTubeBlock))
			// an Inlet brings no Receiver and no tubes
			return Refusal.NONE;
		int max = CMConfig.SERVER.clockworkTubeMaxLength();
		Reach here = reach(level, pos, pos, side, joints, max);
		Set<BlockPos> receivers = new HashSet<>(here.receivers());
		if (receiver) {
			receivers.add(n.immutable());
			return receivers.size() > 1 ? Refusal.TWO_RECEIVERS : Refusal.NONE;
		}
		Reach there = reach(level, n, pos, side, null, max);
		receivers.addAll(there.receivers());
		if (receivers.size() > 1)
			return Refusal.TWO_RECEIVERS;
		if (!receivers.isEmpty()) {
			Set<BlockPos> tubes = new HashSet<>(here.tubes());
			tubes.addAll(there.tubes());
			if (tubes.size() > max)
				return Refusal.TOO_LONG;
		}
		return Refusal.NONE;
	}

	/** Shut with a wrench, on this tube's side or the neighbouring tube's. */
	static boolean shut(LevelAccessor level, BlockPos pos, Direction side) {
		if (level.getBlockEntity(pos) instanceof ClockworkTubeBlockEntity be && be.isShut(side))
			return true;
		BlockPos n = pos.relative(side);
		return loaded(level, n) && level.getBlockEntity(n) instanceof ClockworkTubeBlockEntity other && other.isShut(side.getOpposite());
	}

	/**
	 * One side's joint, given the tube's other joints. A neighbour whose chunk isn't loaded keeps the joint as it was
	 * ({@code was}), so a joint never comes apart on one side only.
	 */
	static TubeJoint side(LevelAccessor level, BlockPos pos, Direction side, Map<Direction, TubeJoint> joints, TubeJoint was,
						  Map<Direction, Refusal> why) {
		if (!loaded(level, pos.relative(side))) {
			why.put(side, Refusal.NONE);
			return was;
		}
		if (!joinable(level, pos, side)) {
			why.put(side, Refusal.NONE);
			return TubeJoint.NONE;
		}
		if (shut(level, pos, side)) {
			why.put(side, Refusal.SHUT);
			return TubeJoint.CAPPED;
		}
		Map<Direction, TubeJoint> others = new EnumMap<>(joints);
		others.put(side, TubeJoint.NONE);
		Refusal r = refusal(level, pos, side, others);
		why.put(side, r);
		return r == Refusal.NONE ? TubeJoint.OPEN : TubeJoint.CAPPED;
	}

	/** All a tube's joints. {@code current}: its state now (null while it's being placed: then only the sides worked out so far count). */
	static Map<Direction, TubeJoint> compute(LevelAccessor level, BlockPos pos, @Nullable BlockState current, Map<Direction, Refusal> why) {
		Map<Direction, TubeJoint> joints = new EnumMap<>(Direction.class);
		for (Direction side : Direction.values())
			joints.put(side, current == null ? TubeJoint.NONE : current.getValue(ClockworkTubeBlock.JOINTS.get(side)));
		for (Direction side : Direction.values())
			joints.put(side, side(level, pos, side, joints, joints.get(side), why));
		return joints;
	}
}
