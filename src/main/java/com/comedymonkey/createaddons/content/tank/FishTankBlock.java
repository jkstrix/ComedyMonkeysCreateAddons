package com.comedymonkey.createaddons.content.tank;

import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Fish Tank and Brass Fish Tank (design/specs/monster-cage-and-fish-tank.md, Part 2): the Animal Pens' breeding,
 * underwater, for fish, squid and axolotls. Glass on three sides; the shaft goes into the back, water and items on
 * every other face. A tank of glow squid gives light (F15). See {@link FishTankBlockEntity}.
 */
public class FishTankBlock extends KineticBlock implements IBE<FishTankBlockEntity> {
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	/** Holding glow squid: light level 6. */
	public static final BooleanProperty GLOWING = BooleanProperty.create("glowing");

	private final boolean brass;

	public FishTankBlock(Properties properties, boolean brass) {
		super(properties.lightLevel(state -> state.getValue(GLOWING) ? 6 : 0));
		this.brass = brass;
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(GLOWING, false));
	}

	public boolean isBrass() {
		return brass;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING, GLOWING));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
		if (!(level.getBlockEntity(pos) instanceof FishTankBlockEntity be) || !be.use(player, hand, stack))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty() || !(level.getBlockEntity(pos) instanceof FishTankBlockEntity be) || !be.hasOutput())
			return InteractionResult.PASS;
		if (!level.isClientSide)
			be.giveOutputTo(player);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		if (!(level.getBlockEntity(context.getClickedPos()) instanceof FishTankBlockEntity be))
			return InteractionResult.PASS;
		if (context.getPlayer() instanceof ServerPlayer player)
			player.openMenu(be);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!movedByPiston && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof FishTankBlockEntity be)
			be.dropContents();
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof FishTankBlockEntity be ? be.getComparatorOutput() : 0;
	}

	@Override
	public Class<FishTankBlockEntity> getBlockEntityClass() {
		return FishTankBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends FishTankBlockEntity> getBlockEntityType() {
		return ModBlockEntities.FISH_TANK.get();
	}
}
