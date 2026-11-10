package com.comedymonkey.createaddons.content.dripstone;

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
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * The Pointed Dripstone Generator (design/specs/pointed-dripstone-generator.md): a stone generator's sealed box with a
 * little dripstone cave inside. The window ({@code facing}) faces the player when placed and a shaft goes into the back,
 * as the Cobblestone Generators; four Pointed Dripstone put in by hand seed it, water comes in from the sides, and the
 * grown tips are knocked off into its store (see {@link PointedDripstoneGeneratorBlockEntity}). The wrench opens its
 * screen.
 */
public final class PointedDripstoneGeneratorBlock extends KineticBlock implements IBE<PointedDripstoneGeneratorBlockEntity> {
	/** The front, with the window. The shaft goes into the opposite face. */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	/** Held off by a redstone signal (lights the Wireless one's antenna). */
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	private final boolean wireless;

	/** {@code wireless}: has an antenna on top that listens on a Redstone Link frequency. */
	public PointedDripstoneGeneratorBlock(Properties properties, boolean wireless) {
		super(properties);
		this.wireless = wireless;
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
	}

	public boolean isWireless() {
		return wireless;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING, POWERED));
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
	@SuppressWarnings("deprecation") // BlockState.rotate inside mirror() is vanilla's own pattern
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	/**
	 * Right-click with Pointed Dripstone: it takes as many as its seed still needs, up to four. By hand only: a Deployer
	 * (a fake player) can't seed it (D15).
	 */
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
											  InteractionHand hand, BlockHitResult hit) {
		if (player instanceof FakePlayer || !(level.getBlockEntity(pos) instanceof PointedDripstoneGeneratorBlockEntity be) || !be.seeds(stack))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (!level.isClientSide)
			be.addSeed(player, stack);
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	/** The wrench opens its screen (sneaking, it picks the block up, as Create's wrench does). */
	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		if (!(level.getBlockEntity(context.getClickedPos()) instanceof PointedDripstoneGeneratorBlockEntity be))
			return InteractionResult.PASS;
		// (not for a Deployer's fake player, which would hold the screen open forever)
		if (context.getPlayer() instanceof ServerPlayer player && !(player instanceof FakePlayer))
			player.openMenu(be, buf -> buf.writeBlockPos(be.getBlockPos()));
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	/** A redstone signal switches it off (nothing grows, no stress). */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbour, BlockPos neighbourPos,
								   boolean movedByPiston) {
		super.neighborChanged(state, level, pos, neighbour, neighbourPos, movedByPiston);
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof PointedDripstoneGeneratorBlockEntity be)
			be.setWiredSignal(level.hasNeighborSignal(pos));
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		// Moved blocks (contraptions, Aeronautics physics objects) keep their seed, water and store
		if (!movedByPiston && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PointedDripstoneGeneratorBlockEntity be)
			be.dropContents();
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof PointedDripstoneGeneratorBlockEntity be ? be.getComparatorOutput() : 0;
	}

	@Override
	public Class<PointedDripstoneGeneratorBlockEntity> getBlockEntityClass() {
		return PointedDripstoneGeneratorBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends PointedDripstoneGeneratorBlockEntity> getBlockEntityType() {
		return ModBlockEntities.POINTED_DRIPSTONE_GENERATOR.get();
	}
}
