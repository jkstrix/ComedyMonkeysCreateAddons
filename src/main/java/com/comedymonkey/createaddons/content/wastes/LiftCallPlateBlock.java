package com.comedymonkey.createaddons.content.wastes;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Lift's call plate (design/specs/clockwork-wastes.md, P49, D40): the lens-in-cog on a wall at a landing. Its level is
 * a landing of the lift whose shaft it stands near ({@link LiftBlockEntity#PLATE_REACH}); a player using it calls the
 * deck there. Locked until its district wakes, an ordinary block afterwards (D42). Its lens is dark until it's pressed,
 * lit while its call waits, and dark again when the deck arrives (D62). Placed
 * against a wall's face, it points away from it.
 */
public class LiftCallPlateBlock extends HorizontalDirectionalBlock implements EntityBlock, CityLocks.Lockable {
	public static final MapCodec<LiftCallPlateBlock> CODEC = simpleCodec(LiftCallPlateBlock::new);
	public static final BooleanProperty LOCKED = SubstationBlock.LOCKED;
	public static final float HARDNESS = 3f, RESISTANCE = 9f;
	/** The mount and plate, 10 px up the wall (the preview's), the part in this block. */
	private static final Map<Direction, VoxelShape> SHAPES = SalvageBlock.shapes(new double[][] { { 4, 10, 14.75, 12, 16, 16 } });

	public LiftCallPlateBlock(Properties properties) {
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

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		return defaultBlockState().setValue(FACING, face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite());
	}

	@Override
	public boolean locked(BlockState state) {
		return state.getValue(LOCKED);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide)
			return InteractionResult.SUCCESS;
		if (state.getValue(LOCKED)) {
			player.displayClientMessage(Component.translatable("comedymonkeysaddons.wastes.lift_dark"), true);
			return InteractionResult.CONSUME;
		}
		LiftBlockEntity lift = LiftBlockEntity.serving(level, pos);
		if (lift != null)
			lift.call(player, pos.getY(), pos);
		return InteractionResult.CONSUME;
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
		if (level.getBlockEntity(pos) instanceof LiftCallPlateBlockEntity be)
			be.scheduledTick();
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new LiftCallPlateBlockEntity(pos, state);
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
