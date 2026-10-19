package com.comedymonkey.createaddons.content.crucible;

import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Pressure Crucible: sits on a Blaze Burner, takes rotation through a shaft in its back wall ({@code facing} points
 * at the back) and presses dust into gems or scrap (see {@link CrucibleRecipe}). Right-click with dust to load it, right-click with an empty hand to take the
 * results out; funnels and other item transport work from any side.
 */
public class PressureCrucibleBlock extends HorizontalKineticBlock implements IBE<PressureCrucibleBlockEntity> {
	public PressureCrucibleBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.block();
	}

	/** Back towards an adjacent shaft if there is one, otherwise away from the player so the open front faces them. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction preferred = getPreferredHorizontalFacing(context);
		Direction back = preferred != null && (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown())
			? preferred
			: context.getHorizontalDirection();
		return defaultBlockState().setValue(HORIZONTAL_FACING, back);
	}

	@Override
	public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
		return face == state.getValue(HORIZONTAL_FACING);
	}

	@Override
	public Direction.Axis getRotationAxis(BlockState state) {
		return state.getValue(HORIZONTAL_FACING).getAxis();
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
											  Player player, InteractionHand hand, BlockHitResult hit) {
		if (stack.isEmpty())
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (!(level.getBlockEntity(pos) instanceof PressureCrucibleBlockEntity be) || !be.isValidInput(stack))
			// Anything else (wrenches, goggles, blocks to place...) does its usual thing
			return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
		if (!level.isClientSide) {
			ItemStack left = be.getInventory().insertItem(PressureCrucibleBlockEntity.INPUT, stack.copy(), false);
			if (left.getCount() != stack.getCount()) {
				player.setItemInHand(hand, left);
				level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.6f, 0.8f);
			}
		}
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
											   BlockHitResult hit) {
		// Sneak + empty hand is left to Carry On
		if (player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof PressureCrucibleBlockEntity be))
			return InteractionResult.PASS;
		if (!level.isClientSide && !be.takeContents(player))
			return InteractionResult.PASS;
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		// Blocks being moved (contraptions, Aeronautics physics objects) carry their contents with them
		if (!movedByPiston && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PressureCrucibleBlockEntity be)
			be.dropContents();
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof PressureCrucibleBlockEntity be ? be.getComparatorOutput() : 0;
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	@Override
	public Class<PressureCrucibleBlockEntity> getBlockEntityClass() {
		return PressureCrucibleBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends PressureCrucibleBlockEntity> getBlockEntityType() {
		return ModBlockEntities.PRESSURE_CRUCIBLE.get();
	}
}
