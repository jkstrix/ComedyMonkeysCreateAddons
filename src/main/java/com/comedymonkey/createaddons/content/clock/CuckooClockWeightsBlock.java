package com.comedymonkey.createaddons.content.clock;

import java.util.Map;

import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.mojang.serialization.MapCodec;

/**
 * The bottom block of a Cuckoo Clock: where its pendulum, weights and the villager's balcony hang. It has no model
 * of its own (the clock above draws all of it) and no item; it comes and goes with the clock, and breaking it breaks
 * the clock, which drops as usual.
 */
public class CuckooClockWeightsBlock extends HorizontalDirectionalBlock {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final MapCodec<CuckooClockWeightsBlock> CODEC = simpleCodec(CuckooClockWeightsBlock::new);

	/** The bottom of the case with the balcony, and the weights and pendulum under it; front facing north. */
	private static final Map<Direction, VoxelShape> SHAPES = CuckooClockBlock.shapes(new double[][] {
		{2.25, 12, 3.25, 13.75, 16, 16}, {3.5, 1.5, 8, 12.5, 12, 14}});

	public CuckooClockWeightsBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	/** Without its clock above it is nothing. */
	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
									 BlockPos pos, BlockPos neighbourPos) {
		if (direction == Direction.UP && !neighbour.is(ModBlocks.CUCKOO_CLOCK.get()))
			return Blocks.AIR.defaultBlockState();
		return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return level.getBlockState(pos.above()).is(ModBlocks.CUCKOO_CLOCK.get());
	}

	/** Left behind without a shape update (a contraption taking the clock, say), it clears itself up before long. */
	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!canSurvive(state, level, pos))
			level.removeBlock(pos, false);
	}

	/** Breaking the weights breaks the clock, which drops itself (unless in creative). */
	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		BlockPos above = pos.above();
		if (!level.isClientSide && level.getBlockState(above).is(ModBlocks.CUCKOO_CLOCK.get()))
			level.destroyBlock(above, !player.isCreative(), player);
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
		return new ItemStack(ModItems.CUCKOO_CLOCK.get());
	}
}
