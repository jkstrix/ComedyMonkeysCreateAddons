package com.comedymonkey.createaddons.content.tube;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Brass Clockwork Inlet's filter: on whichever hood side you look at (the four sides round its mouth), centred 4 px
 * back from the mouth, on the 4 x 4 slot painted there (design/briefs/clockwork-tubes.md, Revised 11).
 */
public class InletFilterSlot extends ValueBoxTransform.Sided {
	@Override
	protected boolean isSideActive(BlockState state, Direction direction) {
		return direction.getAxis() != state.getValue(ClockworkInletBlock.FACING).getAxis();
	}

	@Override
	public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
		Direction mouth = state.getValue(ClockworkInletBlock.FACING);
		Direction side = getSide();
		// a hair proud of the hood side (its face is 1 px in from the block's), towards the mouth end of the hood
		return VecHelper.getCenterOf(BlockPos.ZERO)
			.add(Vec3.atLowerCornerOf(side.getNormal()).scale(7.3 / 16))
			.add(Vec3.atLowerCornerOf(mouth.getNormal()).scale(4.0 / 16));
	}

	@Override
	protected Vec3 getSouthLocation() {
		return VecHelper.voxelSpace(8, 8, 15.3);
	}
}
