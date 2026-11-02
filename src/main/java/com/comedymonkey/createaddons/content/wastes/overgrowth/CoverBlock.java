package com.comedymonkey.createaddons.content.wastes.overgrowth;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.MultifaceSpreader;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * Knotvine and Scale Lichen (D92): a cover on any face of the blocks round it, as glow lichen is, and like it it can
 * stand in water. It never spreads by itself; bone meal spreads it to a face nearby. Knotvine (#minecraft:climbable) is
 * climbed only where it covers a side, as vines are: on a floor or a ceiling it's walked over.
 */
public final class CoverBlock extends MultifaceBlock implements BonemealableBlock, SimpleWaterloggedBlock {
	public static final MapCodec<CoverBlock> CODEC = simpleCodec(CoverBlock::new);
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
	private final MultifaceSpreader spreader = new MultifaceSpreader(this);

	public CoverBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
	}

	@Override
	protected MapCodec<CoverBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(WATERLOGGED);
	}

	@Override
	public MultifaceSpreader getSpreader() {
		return spreader;
	}

	/** Whether it covers a side (north, east, south or west) of a block round it. */
	public static boolean onASide(BlockState state) {
		return Direction.Plane.HORIZONTAL.stream().anyMatch(d -> hasFace(state, d));
	}

	/** The climbable tag still decides which covers climb (Scale Lichen doesn't); a side face decides where. */
	@Override
	public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
		return state.is(BlockTags.CLIMBABLE) && onASide(state);
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
		return Direction.stream().anyMatch(d -> spreader.canSpreadInAnyDirection(state, level, pos, d.getOpposite()));
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
		spreader.spreadFromRandomFaceTowardRandomDirection(state, level, pos, random);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		if (state.getValue(WATERLOGGED))
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		return super.updateShape(state, dir, neighbour, level, pos, neighbourPos);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return state.getFluidState().isEmpty();
	}
}
