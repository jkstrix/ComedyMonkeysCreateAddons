package com.comedymonkey.createaddons.content.wastes;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModWastes;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Star Reactor (design/specs/clockwork-wastes.md, D23, D29, D33, P13, P14, P44): a Create brass casing round an
 * automaton core, built in a Mechanical Crafter from three Unknown Devices. A Nether Star in it (by hand, funnel, arm,
 * Deployer or package) burns for 8 hours of running, flat out at 8,192 FE a tick out of its copper terminal (its
 * {@link #FACING}, on the far side from whoever placed it: its front faces the placer, any of six ways, D47). An Unknown Device fitted ({@link #UPGRADED}, by hand or
 * Deployer) makes each star burn 12 hours, with a visible orbit ring. See {@link StarReactorBlockEntity}.
 */
public class StarReactorBlock extends DirectionalBlock implements EntityBlock {
	public static final MapCodec<StarReactorBlock> CODEC = simpleCodec(StarReactorBlock::new);
	/** Overhauled: an Unknown Device fitted (P44). */
	public static final BooleanProperty UPGRADED = BooleanProperty.create("upgraded");

	public StarReactorBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.SOUTH).setValue(UPGRADED, false));
	}

	@Override
	protected MapCodec<? extends DirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, UPGRADED);
	}

	/**
	 * Its front toward the placer, whichever of the six ways they look, like the mod's other machines (D47), so its copper
	 * terminal is on the far side; an overhauled one stays overhauled (its item says so).
	 */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		boolean upgraded = context.getItemInHand().getOrDefault(ModWastes.REACTOR_OVERHAULED.get(), false);
		return defaultBlockState().setValue(FACING, context.getNearestLookingDirection()).setValue(UPGRADED, upgraded);
	}

	/** A Nether Star in, or an Unknown Device fitted: by hand, or by a Deployer (it uses the block the same way). */
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
		InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof StarReactorBlockEntity be))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (StarReactorBlockEntity.isStar(stack)) {
			if (level.isClientSide)
				return be.canTakeStar() ? ItemInteractionResult.SUCCESS : ItemInteractionResult.CONSUME;
			if (be.insertStar(player))
				stack.consume(1, player);
			return ItemInteractionResult.SUCCESS;
		}
		if (stack.is(ModWastes.UNKNOWN_DEVICE.get())) {
			if (level.isClientSide)
				return state.getValue(UPGRADED) ? ItemInteractionResult.CONSUME : ItemInteractionResult.SUCCESS;
			if (be.fitUpgrade())
				stack.consume(1, player);
			return ItemInteractionResult.SUCCESS;
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof StarReactorBlockEntity be)
			be.setPaused(level.hasNeighborSignal(pos));
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	/** The star left (P14). */
	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof StarReactorBlockEntity be ? be.comparator() : 0;
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new StarReactorBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide || type != ModWastes.STAR_REACTOR_BE.get())
			return null;
		return (l, p, s, be) -> ((StarReactorBlockEntity) be).tick();
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}
}
