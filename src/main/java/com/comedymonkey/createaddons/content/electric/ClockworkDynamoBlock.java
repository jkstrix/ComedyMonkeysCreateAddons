package com.comedymonkey.createaddons.content.electric;

import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Clockwork Dynamo: the Electric Motor run backwards. It takes the stress it's set to from the shaft at its front, at any
 * speed, and makes three quarters of the FE a motor would draw for it (configurable). Power goes out only through its
 * base, opposite the shaft, whichever way it's placed. A wrench opens its settings.
 */
public class ClockworkDynamoBlock extends DirectionalKineticBlock implements IBE<ClockworkDynamoBlockEntity> {
	/** Making power: the lamp lights cyan (and its aerial stands up, drawn by the renderer). */
	public static final BooleanProperty WORKING = BooleanProperty.create("working");

	public ClockworkDynamoBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(WORKING, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(WORKING));
	}

	@Override
	public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
		return face == state.getValue(FACING);
	}

	@Override
	public Direction.Axis getRotationAxis(BlockState state) {
		return state.getValue(FACING).getAxis();
	}

	/** Power goes out through the base only: the side opposite the shaft. */
	public static boolean isOutput(BlockState state, Direction side) {
		return side == state.getValue(FACING).getOpposite();
	}

	/** Broken or replaced: its line forgets its record (as the Lava Generator's does), so nothing is credited from it. */
	@Override
	public void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && level instanceof net.minecraft.server.level.ServerLevel server)
			com.comedymonkey.createaddons.content.line.PowerGrid.of(server).forget(pos);
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	/** A wrench opens the dynamo's settings (instead of turning it); sneaking with one still picks it up. */
	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		if (context.getLevel().isClientSide)
			com.comedymonkey.createaddons.client.ClockworkDynamoScreen.open(context.getClickedPos());
		return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
	}

	@Override
	public Class<ClockworkDynamoBlockEntity> getBlockEntityClass() {
		return ClockworkDynamoBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ClockworkDynamoBlockEntity> getBlockEntityType() {
		return ModBlockEntities.CLOCKWORK_DYNAMO.get();
	}
}
