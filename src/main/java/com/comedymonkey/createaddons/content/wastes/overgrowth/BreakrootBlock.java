package com.comedymonkey.createaddons.content.wastes.overgrowth;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Breakroot lying on the ground (D92): the Knuckle (an arch out of the ground and back) or the Run (a length along it),
 * along x or z. It stands on any sturdy up-face.
 */
public final class BreakrootBlock extends Block {
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
	private static final VoxelShape KNUCKLE_X = Block.box(0, 0, 5.5, 16, 8, 10.5), KNUCKLE_Z = Block.box(5.5, 0, 0, 10.5, 8, 16),
		RUN_X = Block.box(0, 0, 5, 16, 4.5, 11), RUN_Z = Block.box(5, 0, 0, 11, 4.5, 16);
	private final boolean run;

	public BreakrootBlock(Properties properties, boolean run) {
		super(properties);
		this.run = run;
		registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.X));
	}

	@Override
	protected MapCodec<BreakrootBlock> codec() {
		return simpleCodec(p -> new BreakrootBlock(p, run));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		boolean x = state.getValue(AXIS) == Direction.Axis.X;
		return run ? x ? RUN_X : RUN_Z : x ? KNUCKLE_X : KNUCKLE_Z;
	}

	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
	}

	/** A quarter turn swaps x and z; a mirror leaves it (the root is the same either way along it). */
	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		if (rotation != Rotation.CLOCKWISE_90 && rotation != Rotation.COUNTERCLOCKWISE_90)
			return state;
		return state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		return dir == Direction.DOWN && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}
}
