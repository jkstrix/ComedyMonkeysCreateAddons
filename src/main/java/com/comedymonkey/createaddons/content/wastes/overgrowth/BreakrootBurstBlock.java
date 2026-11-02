package com.comedymonkey.createaddons.content.wastes.overgrowth;

import java.util.Map;

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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Breakroot Burst (D92, D94): three roots out of a crack with the stone they threw out, up from the ground
 * ({@code facing} up) or out of a wall's foot (facing out from the wall).
 */
public final class BreakrootBurstBlock extends Block {
	public static final MapCodec<BreakrootBurstBlock> CODEC = simpleCodec(BreakrootBurstBlock::new);
	public static final DirectionProperty FACING = DirectionProperty.create("facing", d -> d != Direction.DOWN);
	private static final Map<Direction, VoxelShape> SHAPES = Map.of(Direction.UP, Block.box(1, 0, 1, 15, 15, 15),
		Direction.NORTH, Block.box(1, 0, 4, 15, 14, 16), Direction.SOUTH, Block.box(1, 0, 0, 15, 14, 12),
		Direction.WEST, Block.box(4, 0, 1, 16, 14, 15), Direction.EAST, Block.box(0, 0, 1, 12, 14, 15));

	public BreakrootBurstBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP));
	}

	@Override
	protected MapCodec<BreakrootBurstBlock> codec() {
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

	/** Turned with the wall it grows out of; one growing up stays up. */
	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	/** Mirrored by its facing (its three roots can't be mirrored themselves). */
	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
	}

	/** The block it grows out of: the one below, or the wall behind. */
	public static BlockPos support(BlockState state, BlockPos pos) {
		return pos.relative(state.getValue(FACING).getOpposite());
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockPos s = support(state, pos), below = pos.below();
		boolean ground = level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
		return ground && (state.getValue(FACING) == Direction.UP || level.getBlockState(s).isFaceSturdy(level, s, state.getValue(FACING)));
	}

	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		BlockState s = defaultBlockState().setValue(FACING, face == Direction.DOWN ? Direction.UP : face);
		return s.canSurvive(context.getLevel(), context.getClickedPos()) ? s : null;
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		return !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState() : state;
	}
}
