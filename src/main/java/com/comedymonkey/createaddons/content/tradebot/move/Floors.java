package com.comedymonkey.createaddons.content.tradebot.move;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * How high he stands in a block. Vanilla's floor level only looks at the block below, but a low block the path finder
 * walks through (carpet, snow layers, a daylight sensor, a repeater) lifts him too: planning his spot on the floor
 * under the carpet left him not fitting there, so he never traded or emptied a chest from a carpeted floor (user,
 * 2026-11-05).
 */
public final class Floors {
	private Floors() {}

	/** The height he stands at in {@code pos}: the top of a low block in it, or else the top of the block below. */
	public static double height(BlockGetter level, BlockPos pos) {
		double below = WalkNodeEvaluator.getFloorLevel(level, pos);
		VoxelShape own = level.getBlockState(pos).getCollisionShape(level, pos);
		if (own.isEmpty())
			return below;
		double top = own.max(Direction.Axis.Y);
		// a block as tall as the space isn't a floor in it (he can't stand there at all)
		return top < 1 ? Math.max(below, pos.getY() + top) : below;
	}
}
