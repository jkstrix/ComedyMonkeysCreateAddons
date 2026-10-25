package com.comedymonkey.createaddons.content.tradebot.react;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Where a frightened Trade Bot hides (D8): of the spots he can roll to within a few blocks, the one with the most walls
 * around it, as far from the mob as it can be. A corner (two walls or more) beats any open spot; among corners, the
 * farther the better. Pure: the world is asked through the two predicates, so it's testable on a fixture grid.
 */
public final class CornerFinder {
	/** How much one wall around a spot is worth, in blocks of distance from the mob. */
	static final double WALL_WEIGHT = 3;
	/** Level first, then a step up or down. */
	private static final int[] STEPS = { 0, 1, -1 };

	private CornerFinder() {}

	/**
	 * The best hiding spot reachable from {@code from} in at most {@code radius} steps (level, or one block up or down),
	 * or null if there's nowhere but where he is.
	 */
	@Nullable
	public static BlockPos find(BlockPos from, Vec3 threat, int radius, Predicate<BlockPos> standableAt, Predicate<BlockPos> wallAt) {
		// each block is asked about once, however many spots are next to it
		Map<BlockPos, Boolean> standables = new HashMap<>(), walls = new HashMap<>();
		Predicate<BlockPos> standable = pos -> standables.computeIfAbsent(pos, standableAt::test);
		Predicate<BlockPos> wall = pos -> walls.computeIfAbsent(pos, wallAt::test);
		Set<BlockPos> seen = new HashSet<>();
		Deque<BlockPos> queue = new ArrayDeque<>();
		Deque<Integer> depth = new ArrayDeque<>();
		seen.add(from);
		queue.add(from);
		depth.add(0);
		BlockPos best = null;
		double bestScore = score(from, threat, wall);
		while (!queue.isEmpty()) {
			BlockPos at = queue.poll();
			int d = depth.poll();
			if (!at.equals(from)) {
				double s = score(at, threat, wall);
				if (s > bestScore) {
					bestScore = s;
					best = at;
				}
			}
			if (d >= radius)
				continue;
			for (Direction side : Direction.Plane.HORIZONTAL)
				for (int dy : STEPS) {
					BlockPos next = at.relative(side).above(dy);
					// a spot already found is one he can stand on (where he started may not be)
					boolean found = seen.contains(next) && !next.equals(from);
					if (!found && !standable.test(next))
						continue;
					if (seen.add(next)) {
						queue.add(next);
						depth.add(d + 1);
					}
					break;
				}
		}
		return best;
	}

	/** Walls around the spot (worth {@link #WALL_WEIGHT} blocks each, corners only) plus its distance from the mob. */
	static double score(BlockPos pos, Vec3 threat, Predicate<BlockPos> wall) {
		int walls = walls(pos, wall);
		double distance = Vec3.atBottomCenterOf(pos).distanceTo(threat);
		return (walls >= 2 ? walls * WALL_WEIGHT : 0) + distance;
	}

	static int walls(BlockPos pos, Predicate<BlockPos> wall) {
		int walls = 0;
		for (Direction side : Direction.Plane.HORIZONTAL)
			if (wall.test(pos.relative(side)))
				walls++;
		return walls;
	}
}
