package com.comedymonkey.createaddons.content.wastes.overgrowth;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Shiftcap Shelf (D92): bracket fungi on a wall, facing out from it, aging blue to plum as the clusters do. Light 2;
 * bone meal ages it.
 */
public final class ShiftcapShelfBlock extends HorizontalDirectionalBlock implements BonemealableBlock {
	public static final MapCodec<ShiftcapShelfBlock> CODEC = simpleCodec(ShiftcapShelfBlock::new);
	private static final Map<Direction, VoxelShape> SHAPES = Map.of(
		Direction.NORTH, Block.box(1, 0, 9, 15, 14, 16), Direction.SOUTH, Block.box(1, 0, 0, 15, 14, 7),
		Direction.WEST, Block.box(9, 0, 1, 16, 14, 15), Direction.EAST, Block.box(0, 0, 1, 7, 14, 15));

	public ShiftcapShelfBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(ShiftcapBlock.AGE, ShiftcapBlock.Age.YOUNG));
	}

	@Override
	protected MapCodec<ShiftcapShelfBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, ShiftcapBlock.AGE);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction out = state.getValue(FACING);
		BlockPos wall = pos.relative(out.getOpposite());
		return level.getBlockState(wall).isFaceSturdy(level, wall, out);
	}

	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		for (Direction d : context.getNearestLookingDirections())
			if (d.getAxis().isHorizontal()) {
				BlockState s = defaultBlockState().setValue(FACING, d.getOpposite());
				if (s.canSurvive(context.getLevel(), context.getClickedPos()))
					return s;
			}
		return null;
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		return dir == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(ShiftcapBlock.AGE) == ShiftcapBlock.Age.OLD && random.nextInt(40) == 0)
			ShiftcapBlock.spores(state, level, pos, random, 1);
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
		return state.getValue(ShiftcapBlock.AGE) != ShiftcapBlock.Age.OLD;
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
		ShiftcapBlock.Age a = state.getValue(ShiftcapBlock.AGE);
		if (a != ShiftcapBlock.Age.OLD)
			level.setBlock(pos, state.setValue(ShiftcapBlock.AGE, ShiftcapBlock.Age.values()[a.ordinal() + 1]), UPDATE_ALL);
	}
}
