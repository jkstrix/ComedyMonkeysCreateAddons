package com.comedymonkey.createaddons.content.machine;

import java.util.function.Supplier;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.phys.BlockHitResult;

/**
 * String Tangler and Stone Infuser: the window ({@code facing}) faces the player when placed and the shaft goes into
 * the back, like the generators. Hoppers, funnels and pipes insert both ingredients and extract the result from any
 * side; by hand, right-click with an ingredient to load it and with an empty hand to take the results.
 */
public class TwoInputMachineBlock extends KineticBlock implements IBE<TwoInputMachineBlockEntity> {
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	private final Supplier<BlockEntityType<TwoInputMachineBlockEntity>> type;

	public TwoInputMachineBlock(Properties properties, Supplier<BlockEntityType<TwoInputMachineBlockEntity>> type) {
		super(properties);
		this.type = type;
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING));
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
		if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof TwoInputMachineBlockEntity be))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (!be.acceptsInput(stack))
			// Anything else (wrenches, goggles, blocks to place...) does its usual thing instead of emptying the machine
			return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
		if (!level.isClientSide) {
			ItemStack left = be.getAutomationHandler().insertItem(be.inputSlotFor(stack), stack.copy(), false);
			if (!player.isCreative())
				player.setItemInHand(hand, left);
		}
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof TwoInputMachineBlockEntity be))
			return InteractionResult.PASS;
		if (!level.isClientSide && !be.takeContents(player))
			return InteractionResult.PASS;
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!movedByPiston && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof TwoInputMachineBlockEntity be)
			be.dropContents();
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof TwoInputMachineBlockEntity be ? be.getComparatorOutput() : 0;
	}

	@Override
	public Class<TwoInputMachineBlockEntity> getBlockEntityClass() {
		return TwoInputMachineBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends TwoInputMachineBlockEntity> getBlockEntityType() {
		return type.get();
	}
}
