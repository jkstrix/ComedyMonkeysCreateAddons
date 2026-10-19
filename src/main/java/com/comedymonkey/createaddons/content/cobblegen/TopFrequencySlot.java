package com.comedymonkey.createaddons.content.cobblegen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The two Redstone Link frequency slots on a Wireless Cobblestone Generator: lying flat on raised pads on top (red
 * and blue, as on a Redstone Link), side by side towards the front (the antenna stands at the back). Offsets are
 * given for a south-facing front, which Create's {@code rotateHorizontally} then turns to the real facing.
 */
public class TopFrequencySlot extends ValueBoxTransform.Dual {
	public TopFrequencySlot(boolean first) {
		super(first);
	}

	@Override
	public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
		return rotateHorizontally(state, VecHelper.voxelSpace(isFirst() ? 10.5 : 5.5, 17.05, 10.5));
	}

	@Override
	public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
		Direction facing = state.getValue(CobbleGeneratorBlock.FACING);
		ms.mulPose(Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing) + 180));
		ms.mulPose(Axis.XP.rotationDegrees(90));
	}

	@Override
	public float getScale() {
		return 0.4f;
	}
}
