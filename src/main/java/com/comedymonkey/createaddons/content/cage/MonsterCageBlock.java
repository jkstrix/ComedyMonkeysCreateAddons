package com.comedymonkey.createaddons.content.cage;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Monster Cages (design/specs/monster-cage-and-fish-tank.md, Part 1): Andesite and Brass, each dry or Flooded,
 * and the Netherite cage for the Wither. A sister of the Animal Pens: the shaft goes into the back, items in and out
 * on every face, a wrench opens the screen. The Netherite cage remembers who placed it: only they (and their team) can
 * wrench or break it, and nobody can while its field is down (D41, D46). See {@link MonsterCageBlockEntity}.
 */
public class MonsterCageBlock extends KineticBlock implements IBE<MonsterCageBlockEntity> {
	/** The front, with the bars. The shaft goes into the opposite face. */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	private final CageTier tier;
	private final boolean flooded;

	public MonsterCageBlock(Properties properties, CageTier tier, boolean flooded) {
		super(properties);
		this.tier = tier;
		this.flooded = flooded;
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	public CageTier tier() {
		return tier;
	}

	public boolean isFlooded() {
		return flooded;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** The Netherite cage belongs to whoever placed it. */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (placer instanceof Player player && level.getBlockEntity(pos) instanceof MonsterCageBlockEntity be)
			be.setOwner(player);
	}

	@Override
	public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
		return face == state.getValue(FACING).getOpposite();
	}

	@Override
	public Direction.Axis getRotationAxis(BlockState state) {
		return state.getValue(FACING).getAxis();
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
											  InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof MonsterCageBlockEntity be) || !be.use(player, hand, stack))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	/** Empty hand: what it's made into the inventory. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty() || !(level.getBlockEntity(pos) instanceof MonsterCageBlockEntity be)
			|| !be.hasOutput())
			return InteractionResult.PASS;
		if (!level.isClientSide)
			be.giveOutputTo(player);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	/** A wrench opens the screen (sneak-wrenching still picks it up); the Netherite cage only for its owner. */
	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (!(level.getBlockEntity(pos) instanceof MonsterCageBlockEntity be))
			return InteractionResult.PASS;
		if (context.getPlayer() instanceof ServerPlayer player) {
			if (!be.mayManage(player))
				player.displayClientMessage(Component.translatable("comedymonkeysaddons.gui.monster_cage.not_owner", be.getOwnerName()), true);
			else
				player.openMenu(be);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
		if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof MonsterCageBlockEntity be
			&& context.getPlayer() != null && !be.mayBreak(context.getPlayer())) {
			if (!context.getLevel().isClientSide)
				context.getPlayer().displayClientMessage(be.whyNotBreak(context.getPlayer()), true);
			return InteractionResult.FAIL;
		}
		return super.onSneakWrenched(state, context);
	}

	/** Nobody but its owner can break a Netherite cage, and nobody while its field is down. */
	@Override
	protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
		if (level.getBlockEntity(pos) instanceof MonsterCageBlockEntity be && !be.mayBreak(player))
			return 0;
		return super.getDestroyProgress(state, player, level, pos);
	}

	@Override
	public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest,
									   FluidState fluid) {
		if (level.getBlockEntity(pos) instanceof MonsterCageBlockEntity be && !be.mayBreak(player)) {
			if (!level.isClientSide)
				player.displayClientMessage(be.whyNotBreak(player), true);
			return false;
		}
		return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!movedByPiston && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MonsterCageBlockEntity be)
			be.dropContents();
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	/** The hatch flapping open as a trident goes out (block event 1), on every client in sight. */
	@Override
	protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
		super.triggerEvent(state, level, pos, id, param);
		return level.getBlockEntity(pos) instanceof MonsterCageBlockEntity be && be.triggerEvent(id, param);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof MonsterCageBlockEntity be ? be.getComparatorOutput() : 0;
	}

	@Override
	public Class<MonsterCageBlockEntity> getBlockEntityClass() {
		return MonsterCageBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends MonsterCageBlockEntity> getBlockEntityType() {
		return ModBlockEntities.MONSTER_CAGE.get();
	}
}
