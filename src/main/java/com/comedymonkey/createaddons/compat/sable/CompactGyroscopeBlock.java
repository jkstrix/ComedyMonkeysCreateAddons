package com.comedymonkey.createaddons.compat.sable;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** A framed glass cube with a flywheel spinning inside. Driven through a vertical shaft from above or below. */
public class CompactGyroscopeBlock extends KineticBlock implements IBE<CompactGyroscopeBlockEntity> {
	public CompactGyroscopeBlock(Properties properties) {
		super(properties);
	}

	@Override
	public Direction.Axis getRotationAxis(BlockState state) {
		return Direction.Axis.Y;
	}

	@Override
	public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
		return face.getAxis() == Direction.Axis.Y;
	}

	@Override
	public Class<CompactGyroscopeBlockEntity> getBlockEntityClass() {
		return CompactGyroscopeBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends CompactGyroscopeBlockEntity> getBlockEntityType() {
		return AeronauticsContent.COMPACT_GYROSCOPE_BE.get();
	}
}
