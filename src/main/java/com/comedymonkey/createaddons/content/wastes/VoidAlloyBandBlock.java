package com.comedymonkey.createaddons.content.wastes;

import java.util.Map;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * The Void-Alloy Band (D71, D75; the buildings brief's block set, #5): the trim and cornice. A raised 10 px band round
 * the block's sides with one horizontal seam along its middle, its top and bottom 3 px set back 1 px; where a block
 * touches a side, that side's set-back is filled flush (the connection states, so a run of bands or a band between plates
 * reads as one course). Its seam glows edge tone while its district is fed. {@link #AXIS} is its ring's axis: y upright;
 * x or z lying on its side, for a fallen tower top (D75).
 */
public class VoidAlloyBandBlock extends CityDecorLightBlock {
	public static final MapCodec<VoidAlloyBandBlock> CODEC = simpleCodec(VoidAlloyBandBlock::new);
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
	/** A block touches this side (only the sides round its axis count). */
	public static final Map<Direction, BooleanProperty> JOINS = PipeBlock.PROPERTY_BY_DIRECTION;
	/** A player sneaking lays a band along the face clicked, as a log (provisional, for the user's word: progress doc, "D75"). */
	public static final boolean LYING_WHEN_SNEAKING = true;

	public VoidAlloyBandBlock(Properties properties) {
		super(properties);
		BlockState s = defaultBlockState().setValue(LIT, false).setValue(AXIS, Direction.Axis.Y);
		for (BooleanProperty p : JOINS.values())
			s = s.setValue(p, false);
		registerDefaultState(s);
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(AXIS);
		JOINS.values().forEach(builder::add);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction.Axis axis = LYING_WHEN_SNEAKING && context.getPlayer() != null && context.getPlayer().isShiftKeyDown()
			? context.getClickedFace().getAxis() : Direction.Axis.Y;
		return joined(defaultBlockState().setValue(AXIS, axis), context.getLevel(), context.getClickedPos());
	}

	/** Its joins to what's round it. */
	public static BlockState joined(BlockState state, BlockGetter level, BlockPos pos) {
		for (Direction d : Direction.values())
			state = state.setValue(JOINS.get(d), joins(state, level, pos, d));
		return state;
	}

	/** A side round its axis whose neighbour's face against it is whole. */
	private static boolean joins(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
		if (side.getAxis() == state.getValue(AXIS))
			return false;
		BlockPos n = pos.relative(side);
		return level.getBlockState(n).isFaceSturdy(level, n, side.getOpposite());
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction side, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		return state.setValue(JOINS.get(side), joins(state, level, pos, side));
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		BlockState out = state;
		for (Direction d : Direction.Plane.HORIZONTAL)
			out = out.setValue(JOINS.get(rotation.rotate(d)), state.getValue(JOINS.get(d)));
		Direction.Axis axis = state.getValue(AXIS);
		if (axis != Direction.Axis.Y && (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90))
			out = out.setValue(AXIS, axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
		return out;
	}
}
