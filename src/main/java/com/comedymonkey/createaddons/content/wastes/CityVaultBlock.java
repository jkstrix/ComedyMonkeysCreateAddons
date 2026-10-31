package com.comedymonkey.createaddons.content.wastes;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
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
 * A City Vault (design/specs/clockwork-wastes.md, P11, P17, P21, P23, D31): a deep void-alloy block with an iris door.
 * Locked (unbreakable, blast-proof, contraption-proof, P21) until its district wakes, then the iris opens and it's a
 * shared chest of 27 slots, filled from its loot table the first time anyone opens it: a district vault's
 * ({@code chests/clockwork_city_vault}) or, {@link #HEART}, the heart vault's ({@code chests/clockwork_city_heart_vault},
 * the Elytra), which opens on the Custodian's death, or on Peaceful once all four districts are awake (P41).
 */
public class CityVaultBlock extends HorizontalDirectionalBlock implements EntityBlock, CityLocks.Lockable {
	public static final MapCodec<CityVaultBlock> CODEC = simpleCodec(CityVaultBlock::new);
	public static final BooleanProperty LOCKED = SubstationBlock.LOCKED;
	/** The heart's vault (the Elytra), not a district's. */
	public static final BooleanProperty HEART = BooleanProperty.create("heart");
	public static final float HARDNESS = 6f, RESISTANCE = 9f;
	private static final Map<Direction, VoxelShape> SHAPES = SalvageBlock.shapes(new double[][] { { 0, 0, 1, 16, 16, 16 } });

	public CityVaultBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(LOCKED, true).setValue(HEART, false));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LOCKED, HEART);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide)
			return InteractionResult.SUCCESS;
		if (!(level.getBlockEntity(pos) instanceof CityVaultBlockEntity be))
			return InteractionResult.PASS;
		if (state.getValue(LOCKED)) {
			player.displayClientMessage(Component.translatable(state.getValue(HEART) ? "comedymonkeysaddons.wastes.heart_vault_sealed"
				: "comedymonkeysaddons.wastes.vault_sealed"), true);
			return InteractionResult.CONSUME;
		}
		be.open(player);
		return InteractionResult.CONSUME;
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
		if (!newState.is(this) && level.getBlockEntity(pos) instanceof CityVaultBlockEntity be) {
			Containers.dropContents(level, pos, be.container());
			level.updateNeighbourForOutputSignal(pos, this);
		}
		super.onRemove(state, level, pos, newState, moved);
	}

	@Override
	public boolean locked(BlockState state) {
		return state.getValue(LOCKED);
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
		if (level.getBlockEntity(pos) instanceof CityVaultBlockEntity be)
			be.scheduledTick();
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof CityVaultBlockEntity be
			? net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromContainer(be.container()) : 0;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CityVaultBlockEntity(pos, state);
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
