package com.comedymonkey.createaddons.content.wastes;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Broken Makers' Window (D75): the makers' window as the city's states have it, an aged iron frame of four panes, the
 * glass grimed dark with dust in its lower rows, most panes broken out to shards at the frame. A 2 px pane in the middle
 * of its block, as the preview drew it, across {@link #AXIS} (z: its glass faces north and south). Three ways broken,
 * picked by its model's weighted variants. Not a pane: it joins nothing (provisional).
 */
public class BrokenMakersWindowBlock extends Block {
	public static final MapCodec<BrokenMakersWindowBlock> CODEC = simpleCodec(BrokenMakersWindowBlock::new);
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
	private static final VoxelShape ACROSS_Z = Block.box(0, 0, 7, 16, 16, 9), ACROSS_X = Block.box(7, 0, 0, 9, 16, 16);

	public BrokenMakersWindowBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Z));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS);
	}

	/** Its glass toward the player. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(AXIS) == Direction.Axis.Z ? ACROSS_Z : ACROSS_X;
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90
			? state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.Z ? Direction.Axis.X : Direction.Axis.Z) : state;
	}
}
