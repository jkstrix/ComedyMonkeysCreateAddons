package com.comedymonkey.createaddons.compat.sable;

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A propeller that turns rather than pushes. Mounted like an Aeronautics propeller: its base against the block it was
 * placed on, the shaft coming in from behind, the blades pointing {@code facing}. It turns the ship about that axis.
 */
public class TorqueRotorBlock extends DirectionalKineticBlock implements IBE<TorqueRotorBlockEntity> {
	/** Mounting plate and hub, by facing (indexed by ordinal); the blades are only drawn, so they don't collide. */
	private static final VoxelShape[] SHAPES = {
		box(1, 6, 1, 15, 16, 15),  // down
		box(1, 0, 1, 15, 10, 15),  // up
		box(1, 1, 6, 15, 15, 16),  // north
		box(1, 1, 0, 15, 15, 10),  // south
		box(6, 1, 1, 16, 15, 15),  // west
		box(0, 1, 1, 10, 15, 15),  // east
	};

	public TorqueRotorBlock(Properties properties) {
		super(properties);
	}

	/** Blades point away from the face it was placed against. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getClickedFace());
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES[state.getValue(FACING).ordinal()];
	}

	@Override
	public Direction.Axis getRotationAxis(BlockState state) {
		return state.getValue(FACING).getAxis();
	}

	/** The shaft comes in through the mounting plate. */
	@Override
	public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
		return face == state.getValue(FACING).getOpposite();
	}

	@Override
	public Class<TorqueRotorBlockEntity> getBlockEntityClass() {
		return TorqueRotorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends TorqueRotorBlockEntity> getBlockEntityType() {
		return AeronauticsContent.TORQUE_ROTOR_BE.get();
	}
}
