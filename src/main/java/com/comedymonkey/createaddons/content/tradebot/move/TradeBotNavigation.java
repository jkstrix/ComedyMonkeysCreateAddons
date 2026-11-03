package com.comedymonkey.createaddons.content.tradebot.move;

import java.util.IdentityHashMap;
import java.util.Map;

import com.comedymonkey.createaddons.content.tradebot.TradeBotStationBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;

/**
 * Trade Bot's routes (D18, D28, D29): over the ground, up or down at most one block (as a hop), through doors and
 * fence gates (he opens them himself), never through water or lava, and never up ladders or bubble columns. Hops
 * cost what they take, so he rolls round a step when that's quicker. It only plans; {@link TradeBotMotion} does the
 * rolling, turning and hopping along the route.
 */
public final class TradeBotNavigation extends GroundPathNavigation {
	/**
	 * What a hop adds to a route, in blocks of rolling (user, 2026-11-03: "hopping takes longer than rolling"): braking,
	 * the 2.4-2.9 s clip and setting off again take about 3.2 s, the time to roll 10 blocks at cruise, and the hop
	 * itself covers one.
	 */
	static final float HOP_COST = 9;

	public TradeBotNavigation(Mob mob, Level level) {
		super(mob, level);
		setCanOpenDoors(true);
		setCanPassDoors(true);
		setCanFloat(false);
	}

	/** He plans routes whenever asked, even while a clip holds him (hopping, docked); vanilla waits for the ground. */
	@Override
	protected boolean canUpdatePath() {
		return true;
	}

	@Override
	protected PathFinder createPathFinder(int maxVisitedNodes) {
		nodeEvaluator = new NodeEvaluator();
		nodeEvaluator.setCanPassDoors(true);
		nodeEvaluator.setCanOpenDoors(true);
		return new PathFinder(nodeEvaluator, maxVisitedNodes);
	}

	/** Closed fence gates count as doors he can open, like wooden doors (vanilla treats them as fences); hops cost extra. */
	static class NodeEvaluator extends WalkNodeEvaluator {
		/** Each node's own malus, before a hop's cost went on top of it for one step. */
		private final Map<Node, Float> malus = new IdentityHashMap<>();

		@Override
		public void prepare(PathNavigationRegion region, Mob mob) {
			super.prepare(region, mob);
			malus.clear();
		}

		@Override
		public void done() {
			super.done();
			malus.clear();
		}

		@Override
		public PathType getPathType(PathfindingContext context, int x, int y, int z) {
			BlockState state = context.getBlockState(new BlockPos(x, y, z));
			if (state.getBlock() instanceof FenceGateBlock && !state.getValue(FenceGateBlock.OPEN))
				return PathType.DOOR_WOOD_CLOSED;
			// a station's low base reads as floor, but its post blocks him: he only ever backs into one from its front
			// (the dock clip), never rolls across it
			if (state.getBlock() instanceof TradeBotStationBlock)
				return PathType.BLOCKED;
			return super.getPathType(context, x, y, z);
		}

		/**
		 * The path finder adds each neighbour's malus to the step it has just asked for, so a hop's cost goes on for that
		 * step alone: reached by rolling, the same block costs only its own malus.
		 */
		@Override
		public int getNeighbors(Node[] out, Node node) {
			int n = super.getNeighbors(out, node);
			double floor = getFloorLevel(node.asBlockPos());
			for (int i = 0; i < n; i++) {
				Node next = out[i];
				float own = malus.computeIfAbsent(next, k -> k.costMalus);
				if (own < 0)
					continue;
				next.costMalus = hop(floor, getFloorLevel(next.asBlockPos())) ? own + HOP_COST : own;
			}
			return n;
		}
	}

	/** Whether going from one floor height to another is a hop (or a stairs bounce), not a roll. */
	static boolean hop(double fromFloor, double toFloor) {
		return Math.abs(toFloor - fromFloor) > TradeBotMotion.ROLL_STEP;
	}
}
