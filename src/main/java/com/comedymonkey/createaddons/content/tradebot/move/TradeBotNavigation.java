package com.comedymonkey.createaddons.content.tradebot.move;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;

/**
 * Trade Bot's routes (D18, D28, D29): over the ground, up or down at most one block (as a hop), through doors and
 * fence gates (he opens them himself), never through water or lava, and never up ladders or bubble columns. It only
 * plans; {@link TradeBotMotion} does the rolling, turning and hopping along the route.
 */
public class TradeBotNavigation extends GroundPathNavigation {
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

	/** Closed fence gates count as doors he can open, like wooden doors (vanilla treats them as fences). */
	static class NodeEvaluator extends WalkNodeEvaluator {
		@Override
		public PathType getPathType(PathfindingContext context, int x, int y, int z) {
			BlockState state = context.getBlockState(new BlockPos(x, y, z));
			if (state.getBlock() instanceof FenceGateBlock && !state.getValue(FenceGateBlock.OPEN))
				return PathType.DOOR_WOOD_CLOSED;
			return super.getPathType(context, x, y, z);
		}
	}
}
