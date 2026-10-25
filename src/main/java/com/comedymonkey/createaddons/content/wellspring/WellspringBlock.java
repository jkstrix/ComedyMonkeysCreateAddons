package com.comedymonkey.createaddons.content.wellspring;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.Wrenches;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

public class WellspringBlock extends BaseEntityBlock implements IWrenchable {
	public static final MapCodec<WellspringBlock> CODEC = simpleCodec(WellspringBlock::new);

	/** Fluid the wellspring is set up for (from its upgrades, or else its contents). Drives lamp colour and light. */
	public static final EnumProperty<WellspringSource> SOURCE = EnumProperty.create("source", WellspringSource.class);
	/** Installed upgrade count; drives how many indicator lamps are lit. */
	public static final IntegerProperty UPGRADES = IntegerProperty.create("upgrades", 0, 3);

	public WellspringBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any()
			.setValue(SOURCE, WellspringSource.NONE)
			.setValue(UPGRADES, 0));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(SOURCE, UPGRADES);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new WellspringBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide)
			return createTickerHelper(type, ModBlockEntities.WELLSPRING.get(), (l, p, s, be) -> be.clientTick());
		return createTickerHelper(type, ModBlockEntities.WELLSPRING.get(), (l, p, s, be) -> be.serverTick());
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
											  Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof WellspringBlockEntity be))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

		// Wrench opens the upgrade GUI (sneaking with it still dismantles, as in Create)
		if (Wrenches.isWrench(stack))
			return openGui(level, pos, player, be) == InteractionResult.PASS
				? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
				: ItemInteractionResult.sidedSuccess(level.isClientSide);

		if (stack.getItem() instanceof WellspringUpgradeItem upgrade) {
			// Sneaking with an upgrade opens the GUI (the item lets sneak-use reach the block)
			if (player.isShiftKeyDown())
				return openGui(level, pos, player, be) == InteractionResult.PASS
					? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
					: ItemInteractionResult.sidedSuccess(level.isClientSide);
			if (!level.isClientSide)
				be.tryInstallUpgrade(upgrade, stack, player);
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		}

		// Buckets and other fluid containers
		if (FluidUtil.getFluidHandler(stack).isPresent()) {
			if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection()))
				return ItemInteractionResult.sidedSuccess(level.isClientSide);
			return ItemInteractionResult.CONSUME;
		}

		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
											   BlockHitResult hit) {
		// Sneak + right-click opens the upgrade GUI
		if (!player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof WellspringBlockEntity be))
			return InteractionResult.PASS;
		return openGui(level, pos, player, be);
	}

	private static InteractionResult openGui(Level level, BlockPos pos, Player player, WellspringBlockEntity be) {
		if (!level.isClientSide && player instanceof ServerPlayer serverPlayer)
			serverPlayer.openMenu(be);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		// Only drop upgrades when actually broken. Blocks being moved (Create contraptions, Sable/Aeronautics
		// physics objects assembling or disassembling) carry their block entity data across, so dropping here
		// would duplicate the upgrades.
		if (!movedByPiston && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof WellspringBlockEntity be)
			be.dropUpgrades();
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof WellspringBlockEntity be))
			return 0;
		var tank = be.getTank();
		if (tank.isEmpty())
			return 0;
		return 1 + (int) (14f * tank.getFluidAmount() / tank.getCapacity());
	}
}
