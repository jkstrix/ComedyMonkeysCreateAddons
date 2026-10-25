package com.comedymonkey.createaddons.content.pen;

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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Animal Pen and Brass Animal Pen: a cobble-generator-style box that keeps one captured animal and breeds it up to the
 * pen's capacity, with a shaft turning in the back and its food in the trough. Use a sword on it to take half the
 * animals (their drops), shears to shear its sheep, a bucket or bowl to milk it; empty-handed, use takes out what
 * it's made; a wrench opens its screen. See {@link AnimalPenBlockEntity}.
 */
public class AnimalPenBlock extends KineticBlock implements IBE<AnimalPenBlockEntity> {
	/** The front, with the window. The shaft goes into the opposite face. */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	private final boolean brass;

	public AnimalPenBlock(Properties properties, boolean brass) {
		super(properties);
		this.brass = brass;
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	public boolean isBrass() {
		return brass;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING));
	}

	/** Window towards the player, like a furnace; power then has to come in from behind. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** Only the back takes a shaft. */
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

	/** Bottle, sword, shears, bucket, bowl or food; anything else is left to the item (wrenches and the like). */
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
											  InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof AnimalPenBlockEntity be) || !be.use(player, hand, stack))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	/** Empty hand: what it's made into the inventory. Also called for items that didn't use it, hence the check. */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty() || !(level.getBlockEntity(pos) instanceof AnimalPenBlockEntity be)
			|| !be.hasOutput())
			return InteractionResult.PASS;
		if (!level.isClientSide)
			be.giveOutputTo(player);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	/** A wrench opens the pen's screen (sneak-wrenching still picks the block up). */
	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (!(level.getBlockEntity(pos) instanceof AnimalPenBlockEntity be))
			return InteractionResult.PASS;
		if (context.getPlayer() instanceof ServerPlayer player)
			player.openMenu(be);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		// Moved blocks (contraptions, Aeronautics physics objects) keep their animals
		if (!movedByPiston && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AnimalPenBlockEntity be)
			be.dropContents();
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	/** How full of animals it is, so a comparator can call for a swipe when it's full. */
	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof AnimalPenBlockEntity be ? be.getComparatorOutput() : 0;
	}

	@Override
	public Class<AnimalPenBlockEntity> getBlockEntityClass() {
		return AnimalPenBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends AnimalPenBlockEntity> getBlockEntityType() {
		return ModBlockEntities.ANIMAL_PEN.get();
	}
}
