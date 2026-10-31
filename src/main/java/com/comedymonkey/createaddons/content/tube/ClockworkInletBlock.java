package com.comedymonkey.createaddons.content.tube;

import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Clockwork Inlet (and the Brass Clockwork Inlet, with a filter): a hood that takes items for its network's Receiver
 * from what's in front of its mouth (a chest, a depot, a belt, or items lying in the block space there), one item at a
 * time, or up to a stack for the brass one. It faces any way; its tube joins at its back. Redstone switches it off
 * (design/specs/clockwork-tubes.md D4, D19, D21, D22, D27, D28).
 */
public class ClockworkInletBlock extends Block implements IBE<ClockworkInletBlockEntity>, IWrenchable {
	/** Where its mouth points. */
	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	private final boolean brass;

	public ClockworkInletBlock(Properties properties, boolean brass) {
		super(properties);
		this.brass = brass;
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.DOWN).setValue(POWERED, false));
	}

	public boolean isBrass() {
		return brass;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, POWERED);
	}

	/**
	 * Placed on a tube, or on a Receiver's input (a short run), it joins it: its back to it, its mouth away. Placed on
	 * anything else, its mouth points into that block (D27).
	 */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		BlockState against = context.getLevel().getBlockState(context.getClickedPos().relative(face.getOpposite()));
		boolean joins = against.getBlock() instanceof ClockworkTubeBlock
			|| against.getBlock() instanceof ClockworkReceiverBlock && ClockworkReceiverBlock.input(against) == face;
		return defaultBlockState().setValue(FACING, joins ? face : face.getOpposite())
			.setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
		boolean powered = level.hasNeighborSignal(pos);
		if (powered != state.getValue(POWERED))
			level.setBlock(pos, state.setValue(POWERED, powered), 2);
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		boolean turned = state.is(oldState.getBlock()) && oldState.getValue(FACING) != state.getValue(FACING);
		if (!level.isClientSide && (!state.is(oldState.getBlock()) || turned))
			TubeJoints.changed(level, pos);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && !level.isClientSide)
			TubeJoints.changed(level, pos);
		IBE.onRemove(state, level, pos, newState);
	}

	/** The gulp: the server tells the client it took something. */
	@Override
	protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
		BlockEntity be = level.getBlockEntity(pos);
		return be != null && be.triggerEvent(id, param);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	// ---------- shape: the hood ----------

	private static final java.util.Map<Direction, VoxelShape> SHAPES = new java.util.EnumMap<>(Direction.class);
	static {
		SHAPES.put(Direction.DOWN, Block.box(1, 0, 1, 15, 16, 15));
		SHAPES.put(Direction.UP, Block.box(1, 0, 1, 15, 16, 15));
		SHAPES.put(Direction.NORTH, Block.box(1, 1, 0, 15, 15, 16));
		SHAPES.put(Direction.SOUTH, Block.box(1, 1, 0, 15, 15, 16));
		SHAPES.put(Direction.WEST, Block.box(0, 1, 1, 16, 15, 15));
		SHAPES.put(Direction.EAST, Block.box(0, 1, 1, 16, 15, 15));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	@Override
	public Class<ClockworkInletBlockEntity> getBlockEntityClass() {
		return ClockworkInletBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ClockworkInletBlockEntity> getBlockEntityType() {
		return ModBlockEntities.CLOCKWORK_INLET.get();
	}
}
