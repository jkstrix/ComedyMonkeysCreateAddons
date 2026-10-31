package com.comedymonkey.createaddons.content.tube;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Clockwork Receiver: one to a network. It's a small cog with a hollow middle that meshes with Create's cogwheels (no
 * shaft); turned, it pulls loads from its network's Inlets down the tubes and pushes them out through the middle of the
 * cog, out of its {@link #FACING} side (design/specs/clockwork-tubes.md D1, D3, D3a, D6, D11, D12, D27). Its tube joins
 * on the opposite side.
 */
public class ClockworkReceiverBlock extends DirectionalKineticBlock implements IBE<ClockworkReceiverBlockEntity>, ICogWheel {
	public ClockworkReceiverBlock(Properties properties) {
		super(properties);
	}

	/**
	 * Placed on a tube, or on an Inlet's back (a short run), its input joins it and its output faces away; on anything
	 * else, its output faces into it.
	 */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		BlockState against = context.getLevel().getBlockState(context.getClickedPos().relative(face.getOpposite()));
		boolean joins = against.getBlock() instanceof ClockworkTubeBlock
			|| against.getBlock() instanceof ClockworkInletBlock && against.getValue(ClockworkInletBlock.FACING) == face.getOpposite();
		return defaultBlockState().setValue(FACING, joins ? face : face.getOpposite());
	}

	/** Its input's tube refused it (the network has a Receiver already): the player who placed it is told. */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level.isClientSide || !(placer instanceof Player player))
			return;
		// asked directly: while a player places it, the tube's own joint hasn't been worked out yet
		Direction input = state.getValue(FACING).getOpposite();
		BlockPos tube = pos.relative(input);
		if (level.isLoaded(tube) && level.getBlockState(tube).getBlock() instanceof ClockworkTubeBlock
			&& TubeJoints.refusal(level, tube, input.getOpposite(), null) == TubeJoints.Refusal.TWO_RECEIVERS)
			player.displayClientMessage(Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".clockwork_receiver.refused"), true);
	}

	/** Input (its tube's side) is opposite its output. */
	public static Direction input(BlockState state) {
		return state.getValue(FACING).getOpposite();
	}

	@Override
	public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
		return false;
	}

	@Override
	public Direction.Axis getRotationAxis(BlockState state) {
		return state.getValue(FACING).getAxis();
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (!level.isClientSide)
			TubeJoints.changed(level, pos);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && !level.isClientSide) {
			// a load waiting in its throat drops, as a broken tube's does
			if (level.getBlockEntity(pos) instanceof ClockworkReceiverBlockEntity be && !be.getThroat().isEmpty())
				net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, be.getThroat());
			TubeJoints.changed(level, pos);
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	// ---------- shape: the housings, and the ring gear's disc round its middle ----------

	private static final java.util.Map<Direction.Axis, VoxelShape> SHAPES = new java.util.EnumMap<>(Direction.Axis.class);
	static {
		SHAPES.put(Direction.Axis.Y, Block.box(1, 0, 1, 15, 16, 15));
		SHAPES.put(Direction.Axis.X, Block.box(0, 1, 1, 16, 15, 15));
		SHAPES.put(Direction.Axis.Z, Block.box(1, 1, 0, 15, 15, 16));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING).getAxis());
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	@Override
	public Class<ClockworkReceiverBlockEntity> getBlockEntityClass() {
		return ClockworkReceiverBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ClockworkReceiverBlockEntity> getBlockEntityType() {
		return ModBlockEntities.CLOCKWORK_RECEIVER.get();
	}
}
