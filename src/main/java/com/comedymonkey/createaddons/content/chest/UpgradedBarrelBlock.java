package com.comedymonkey.createaddons.content.chest;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.Wrenches;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A barrel fitted with a chest upgrade: same tiers and rules as upgraded chests (no doubles). Created by using an
 * upgrade on a vanilla barrel and turned back into one when the upgrade is removed. Breaking it drops a barrel plus
 * the upgrade.
 */
public class UpgradedBarrelBlock extends BaseEntityBlock implements IWrenchable, TieredStorageBlock {
	public static final MapCodec<UpgradedBarrelBlock> CODEC = simpleCodec(UpgradedBarrelBlock::new);
	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final EnumProperty<ChestTier> TIER = UpgradedChestBlock.TIER;
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

	public UpgradedBarrelBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any()
			.setValue(FACING, Direction.NORTH)
			.setValue(TIER, ChestTier.COPPER)
			.setValue(OPEN, false));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, TIER, OPEN);
	}

	/** Only reached through Carry On or pick-block placement; faces the player like a vanilla barrel. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new UpgradedBarrelBlockEntity(pos, state);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	/** Scheduled by the opener counter: drops viewers that walked away and closes the lid. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof UpgradedBarrelBlockEntity barrel)
			barrel.recheckOpen();
	}

	/** Wrench opens the upgrade GUI (sneaking with it still dismantles, as in Create). */
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
											  Player player, InteractionHand hand, BlockHitResult hit) {
		if (!Wrenches.isWrench(stack))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (!level.isClientSide && player instanceof ServerPlayer serverPlayer)
			ChestUpgrades.openUpgradeGui(serverPlayer, pos);
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
											   BlockHitResult hit) {
		if (level.isClientSide)
			return InteractionResult.SUCCESS;
		if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof UpgradedBarrelBlockEntity barrel)
			ChestUpgrades.openChestMenu(serverPlayer, barrel, barrel.getDisplayName(), barrel.multiplier());
		return InteractionResult.CONSUME;
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		// Moved blocks (contraptions, physics objects) and Carry On keep their contents in the block entity data
		if (!movedByPiston && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof UpgradedBarrelBlockEntity barrel) {
			Containers.dropContents(level, pos, barrel);
			Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(state.getValue(TIER).upgradeItem()));
			level.updateNeighbourForOutputSignal(pos, this);
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
		return UpgradedBarrelItem.of(state.getValue(TIER));
	}

	// ---------------------------------------------------------------- TieredStorageBlock

	/** Netherite ones shrug off explosions. */
	@Override
	public float getExplosionResistance(BlockState state, net.minecraft.world.level.BlockGetter level,
										 net.minecraft.core.BlockPos pos, net.minecraft.world.level.Explosion explosion) {
		return tierOf(state).blastProof() ? 1200 : super.getExplosionResistance(state, level, pos, explosion);
	}

	@Override
	public ChestTier tierOf(BlockState state) {
		return state.getValue(TIER);
	}

	@Override
	public List<BlockPos> parts(Level level, BlockPos pos, BlockState state) {
		return List.of(pos);
	}

	@Override
	public void setTier(Level level, BlockPos pos, ChestTier tier) {
		BarrelUpgrades.setTier(level, pos, tier);
	}

	@Override
	public void revertToVanilla(Level level, BlockPos pos) {
		BarrelUpgrades.revertToVanilla(level, pos);
	}
}
