package com.comedymonkey.createaddons.content.wastes.overgrowth;

import java.util.Map;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The Dusk Moss Carpet (D92): a 1 px layer of moss whose edge drapes a ragged fringe down any side of the block it lies on
 * that's open (north, east, south, west: a drape each). A drape also hangs on the block beside the one below, which
 * sends it no update: a random tick works its drapes out again, so they catch up a while after that block changes.
 */
public final class MossCarpetBlock extends CarpetBlock {
	public static final MapCodec<MossCarpetBlock> CODEC = simpleCodec(MossCarpetBlock::new);
	public static final Map<Direction, BooleanProperty> DRAPES = Map.of(Direction.NORTH, PipeBlock.NORTH, Direction.EAST, PipeBlock.EAST,
		Direction.SOUTH, PipeBlock.SOUTH, Direction.WEST, PipeBlock.WEST);

	public MossCarpetBlock(Properties properties) {
		super(properties);
		BlockState s = defaultBlockState();
		for (BooleanProperty p : DRAPES.values())
			s = s.setValue(p, false);
		registerDefaultState(s);
	}

	@Override
	public MapCodec<MossCarpetBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(PipeBlock.NORTH, PipeBlock.EAST, PipeBlock.SOUTH, PipeBlock.WEST);
	}

	/** It drapes down {@code dir}'s side when the block it lies on shows that side and nothing covers it. */
	public static boolean drapes(BlockGetter level, BlockPos pos, Direction dir) {
		BlockPos under = pos.below(), beside = under.relative(dir);
		return level.getBlockState(under).isFaceSturdy(level, under, dir) && !level.getBlockState(beside).isFaceSturdy(level, beside, dir.getOpposite())
			&& level.getBlockState(pos.relative(dir)).isAir();
	}

	/** {@code state} with its drapes worked out where it lies. */
	public static BlockState draped(BlockState state, BlockGetter level, BlockPos pos) {
		for (var e : DRAPES.entrySet())
			state = state.setValue(e.getValue(), drapes(level, pos, e.getKey()));
		return state;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return draped(defaultBlockState(), context.getLevel(), context.getClickedPos());
	}

	/** The drapes catch up with the blocks beside the one below (no update reaches it from them). */
	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		BlockState now = draped(state, level, pos);
		if (now != state)
			level.setBlock(pos, now, UPDATE_CLIENTS);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		BlockState out = state;
		for (var e : DRAPES.entrySet())
			out = out.setValue(DRAPES.get(rotation.rotate(e.getKey())), state.getValue(e.getValue()));
		return out;
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		BlockState out = state;
		for (var e : DRAPES.entrySet())
			out = out.setValue(DRAPES.get(mirror.mirror(e.getKey())), state.getValue(e.getValue()));
		return out;
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		if (!state.canSurvive(level, pos))
			return Blocks.AIR.defaultBlockState();
		return draped(state, level, pos);
	}
}
