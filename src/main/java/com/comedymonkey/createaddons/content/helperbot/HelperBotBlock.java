package com.comedymonkey.createaddons.content.helperbot;

import java.util.EnumMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.comedymonkey.createaddons.registry.ModDataComponents;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import com.simibubi.create.content.logistics.stockTicker.StockTickerInteractionHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Helper Bot as a block: he never moves from it, so he can't fall off a train. 1.5 blocks tall, but he collides as one
 * block, and ducks into one block under anything solid (D17, D22). He turns to face Train Controls that face him (D21).
 * His look and animation are all client side (the renderer, and {@link HelperBotMovement} on contraptions).
 */
public class HelperBotBlock extends HorizontalDirectionalBlock implements EntityBlock, IWrenchable {
	public static final MapCodec<HelperBotBlock> CODEC = simpleCodec(HelperBotBlock::new);
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	/** Something solid above: his body slides down his post and he ducks. */
	public static final BooleanProperty DUCKED = BooleanProperty.create("ducked");
	/** Block event: right-clicked off duty, so he waves. */
	public static final int WAVE = 1;
	/** Block event: an order was placed at the Stock Ticker he keeps shop for, so he nods. */
	public static final int ORDER = 2;

	// facing north: his round foot plate sits 2 px forward of the middle, toward the controls
	private static final Map<Direction, VoxelShape> COLLISION = shapes(new double[][] { { 2, 0, 0, 14, 16, 12 } });
	private static final Map<Direction, VoxelShape> OUTLINE = shapes(new double[][] { { 2, 0, 0, 14, 2, 12 }, { 3, 2, 1, 13, 24, 11 } });
	private static final Map<Direction, VoxelShape> OUTLINE_DUCKED = shapes(new double[][] { { 2, 0, 0, 14, 2, 12 }, { 3, 2, 1, 13, 16, 11 } });

	public HelperBotBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(DUCKED, false));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, DUCKED);
	}

	/** He faces whoever placed him, or the Train Controls facing him if there are any. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Direction controls = HelperBots.controlsSide(level, pos);
		return defaultBlockState().setValue(FACING, controls != null ? controls : context.getHorizontalDirection().getOpposite())
			.setValue(DUCKED, HelperBots.roofAbove(level, pos));
	}

	/** Ducks or stands with the block above, and turns to Train Controls placed facing him. */
	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos,
									 BlockPos neighbourPos) {
		if (direction == Direction.UP)
			return state.setValue(DUCKED, HelperBots.roofAbove(level, pos));
		if (direction.getAxis().isHorizontal()) {
			Direction controls = HelperBots.controlsSide(level, pos);
			if (controls != null)
				return state.setValue(FACING, controls);
		}
		return state;
	}

	/**
	 * Beside a Stock Ticker he keeps shop: using him opens its shop, as with a Blaze Burner (D35). Off duty an empty
	 * hand gets a wave. A dye does nothing here: he's dyed as an item (D16). Create's wrench still turns him.
	 */
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
											 BlockHitResult hit) {
		if (stack.getItem() instanceof WrenchItem)
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		BlockPos ticker = CMConfig.SERVER.helperBotShopKeeper.get() ? HelperBots.stockTicker(level, pos) : null;
		if (ticker != null) {
			if (!level.isClientSide)
				StockTickerInteractionHandler.interactWithLogisticsManagerAt(player, level, ticker);
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		}
		if (!stack.isEmpty())
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (!level.isClientSide)
			level.blockEvent(pos, this, WAVE, 0);
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
		BlockEntity be = level.getBlockEntity(pos);
		return be != null && be.triggerEvent(id, param);
	}

	/** Picking him keeps his colour. */
	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
		ItemStack stack = super.getCloneItemStack(level, pos, state);
		if (level.getBlockEntity(pos) instanceof HelperBotBlockEntity be && be.dye() != null)
			stack.set(ModDataComponents.HELPER_BOT_DYE.get(), be.dye());
		return stack;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return (state.getValue(DUCKED) ? OUTLINE_DUCKED : OUTLINE).get(state.getValue(FACING));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return COLLISION.get(state.getValue(FACING));
	}

	/** The block entity's renderer draws him; the block model only carries his particle texture. */
	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
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
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new HelperBotBlockEntity(pos, state);
	}

	/** Only the client ticks him: his animation, mannerisms and sounds. */
	@Override
	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide && type == ModBlockEntities.HELPER_BOT.get() ? (l, p, s, be) -> HelperBotBlockEntity.clientTick((HelperBotBlockEntity) be) : null;
	}

	/** North-facing boxes (pixels) turned to each facing the way the renderer turns him: y 90 takes north to east. */
	private static Map<Direction, VoxelShape> shapes(double[][] northBoxes) {
		Map<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			VoxelShape shape = Shapes.empty();
			for (double[] b : northBoxes) {
				double[] p = turn(b[0], b[2], facing), q = turn(b[3], b[5], facing);
				shape = Shapes.or(shape, Block.box(Math.min(p[0], q[0]), b[1], Math.min(p[1], q[1]), Math.max(p[0], q[0]), b[4], Math.max(p[1], q[1])));
			}
			map.put(facing, shape);
		}
		return map;
	}

	/** A north-facing (x, z) point in pixels, turned about the block's middle to {@code facing}. */
	private static double[] turn(double x, double z, Direction facing) {
		return switch (facing) {
			case EAST -> new double[] { 16 - z, x };
			case SOUTH -> new double[] { 16 - x, 16 - z };
			case WEST -> new double[] { z, 16 - x };
			default -> new double[] { x, z };
		};
	}
}
