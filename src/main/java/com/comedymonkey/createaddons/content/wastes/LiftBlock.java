package com.comedymonkey.createaddons.content.wastes;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModWastes;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A city Lift (design/specs/clockwork-wastes.md, P49, D10, D24, D40; look: design/models/clockwork-wastes-lift.gen.js):
 * the drive at the head of a 2 x 2 shaft. It stands in one corner cell of the shaft's top: the shaft runs from it toward
 * its facing's clockwise side and away from its front (its open sides, for the 2-wide doors, face front and back). Its
 * deck ({@link LiftDeck}) rides four guide rails down to the shaft's floor; it stops at the floor and at each landing a
 * {@link LiftCallPlateBlock} marks. Locked (unbreakable, blast- and contraption-proof, {@link CityLocks}) and dark until
 * its district wakes; then it works for good (D24) and is an ordinary block (D42). Its renderer draws all of it: the
 * frame and drum on the rails' tops, the rails down the shaft, the deck.
 */
public class LiftBlock extends HorizontalDirectionalBlock implements EntityBlock, CityLocks.Lockable {
	public static final MapCodec<LiftBlock> CODEC = simpleCodec(LiftBlock::new);
	public static final BooleanProperty LOCKED = SubstationBlock.LOCKED;
	/** Its hardness and blast resistance once woken (bedrock's while locked: see CityLocks). */
	public static final float HARDNESS = 5f, RESISTANCE = 9f;
	/**
	 * What of the head reaches this corner (model coords, the preview's: the side and cross beams on the rails' tops, the
	 * drum and its axle), so it can be aimed at and broken; most of the head lies round the shaft's top, not in it.
	 */
	private static final Map<Direction, VoxelShape> SHAPES = SalvageBlock.shapes(new double[][] {
		{ -3, 0, 0, -1, 4, 16 }, { -3, 0, -2, 16, 3, 0 }, { 12, 2, -1, 16, 10, 3 }, { 14, 4, -3, 16, 8, -1 } });

	public LiftBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(LOCKED, true));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LOCKED);
	}

	/** Placed with its front toward the placer: the shaft runs ahead of them and to their left. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public boolean locked(BlockState state) {
		return state.getValue(LOCKED);
	}

	/** The shaft's four columns (this one, one toward its clockwise side, and the two behind them). */
	public static BlockPos[] footprint(BlockPos pos, Direction facing) {
		Direction side = facing.getClockWise(), back = facing.getOpposite();
		return new BlockPos[] { pos, pos.relative(side), pos.relative(back), pos.relative(side).relative(back) };
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
		if (!newState.is(this) && level.getBlockEntity(pos) instanceof LiftBlockEntity be)
			be.removed();
		super.onRemove(state, level, pos, newState, moved);
	}

	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		return CityLocks.progress(state, player, level, pos, state.getValue(LOCKED), HARDNESS);
	}

	@Override
	public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
		return CityLocks.resistance(state.getValue(LOCKED), RESISTANCE);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof LiftBlockEntity be)
			be.scheduledTick();
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new LiftBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (type != ModWastes.LIFT_BE.get())
			return null;
		return (l, p, s, be) -> ((LiftBlockEntity) be).tick();
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}
}
