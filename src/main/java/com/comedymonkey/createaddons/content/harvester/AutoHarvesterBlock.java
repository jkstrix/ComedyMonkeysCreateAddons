package com.comedymonkey.createaddons.content.harvester;

import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Auto Harvester: a cobble-generator-style box with a planter inside. Use a seed, crop or sapling on it to plant it;
 * with a shaft turning in the back it grows the plant and harvests it over and over, the plant itself never used up.
 * Empty-handed, use takes out the harvest and sneak-use takes the plant back; a wrench opens a screen with both.
 * See {@link AutoHarvesterBlockEntity}.
 */
public class AutoHarvesterBlock extends KineticBlock implements IBE<AutoHarvesterBlockEntity> {
	/** The front, with the window. The shaft goes into the opposite face. */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	/** What's planted, for the soil: farmland under crops, dirt otherwise. */
	public static final EnumProperty<PlantKind> PLANT = EnumProperty.create("plant", PlantKind.class);

	public AutoHarvesterBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(PLANT, PlantKind.NONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING, PLANT));
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

	/** A plantable item goes into an empty planter; anything else is left to the item (wrenches and the like). */
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
											  InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof AutoHarvesterBlockEntity be)
			|| HarvesterPlants.kindOf(stack) == PlantKind.NONE || be.hasPlant())
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (!level.isClientSide)
			be.plant(stack.consumeAndReturn(1, player));
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	/**
	 * Empty hand: the harvest into the inventory, or (sneaking) the plant back out. Also called for items that
	 * didn't use the block, so it checks the hand really is empty.
	 */
	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!player.getMainHandItem().isEmpty() || !(level.getBlockEntity(pos) instanceof AutoHarvesterBlockEntity be))
			return InteractionResult.PASS;
		if (player.isShiftKeyDown() ? !be.hasPlant() : !be.hasOutput())
			return InteractionResult.PASS;
		if (!level.isClientSide) {
			if (player.isShiftKeyDown())
				player.getInventory().placeItemBackInInventory(be.unplant());
			else
				be.giveOutputTo(player);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	/** A wrench opens the planter and harvest screen (sneak-wrenching still picks the block up). */
	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (!(level.getBlockEntity(pos) instanceof AutoHarvesterBlockEntity be))
			return InteractionResult.PASS;
		if (context.getPlayer() instanceof ServerPlayer player)
			player.openMenu(be);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		// Moved blocks (contraptions, Aeronautics physics objects) keep their plant and harvest
		if (!movedByPiston && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AutoHarvesterBlockEntity be)
			be.dropContents();
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof AutoHarvesterBlockEntity be ? be.getComparatorOutput() : 0;
	}

	@Override
	public Class<AutoHarvesterBlockEntity> getBlockEntityClass() {
		return AutoHarvesterBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends AutoHarvesterBlockEntity> getBlockEntityType() {
		return ModBlockEntities.AUTO_HARVESTER.get();
	}
}
