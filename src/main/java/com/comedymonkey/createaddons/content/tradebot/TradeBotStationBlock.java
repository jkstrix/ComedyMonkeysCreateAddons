package com.comedymonkey.createaddons.content.tradebot;

import java.util.EnumMap;
import java.util.Map;

import com.comedymonkey.createaddons.content.Wrenches;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Trade Bot's winding station: his home, where he waits, charges from FE fed in underneath, and glows while he's
 * docked (design/specs/trade-bot.md D2, D16, D21). Only its owner's wrench can pack it up (D27, D51); nothing else
 * breaks it. The block-entity renderer draws all of it, from the same geometry as the bot.
 */
public class TradeBotStationBlock extends Block implements IBE<TradeBotStationBlockEntity>, IWrenchable {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	/** He's docked and charging: the station glows (a little light, muted like his irises). */
	public static final BooleanProperty GLOW = BooleanProperty.create("glow");

	/** Where his wheel sits in the cradle, for a station facing north: 2 px up, 1.5 px toward the front. */
	private static final double DOCK_UP = 2 / 16.0, DOCK_FORWARD = 1.5 / 16.0;

	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);
	static {
		// the plinth and deck, and the winder post at the back (shapes drawn for a station facing north)
		for (Direction facing : Direction.Plane.HORIZONTAL)
			SHAPES.put(facing, Shapes.or(rotated(facing, 1, 0, 1, 15, 4, 15), rotated(facing, 6.5, 4, 13, 9.5, 15, 15)));
	}

	public TradeBotStationBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(GLOW, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, GLOW);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/** Where he sits when docked, facing the station's front. */
	public static Vec3 dockPoint(BlockPos pos, Direction facing) {
		return Vec3.atBottomCenterOf(pos).add(facing.getStepX() * DOCK_FORWARD, DOCK_UP, facing.getStepZ() * DOCK_FORWARD);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	/** A wrench doesn't turn it (he's sitting in it); sneaking with one packs it up (Create's wrench calls these). */
	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		return InteractionResult.PASS;
	}

	@Override
	public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
		if (context.getLevel().isClientSide)
			return InteractionResult.SUCCESS;
		if (context.getPlayer() instanceof ServerPlayer player
			&& context.getLevel().getBlockEntity(context.getClickedPos()) instanceof TradeBotStationBlockEntity station)
			station.pack(player);
		return InteractionResult.SUCCESS;
	}

	/** Other mods' wrenches (the c:tools/wrench tag) pack it up too. */
	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
		InteractionHand hand, BlockHitResult hit) {
		if (!Wrenches.isWrench(stack) || stack.getItem() instanceof com.simibubi.create.content.equipment.wrench.WrenchItem)
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (!player.isShiftKeyDown())
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof TradeBotStationBlockEntity station)
			station.pack(serverPlayer);
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
		super.neighborChanged(state, level, pos, block, from, moving);
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof TradeBotStationBlockEntity station)
			station.setPaused(level.hasNeighborSignal(pos));
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof TradeBotStationBlockEntity station ? station.comparatorOutput() : 0;
	}

	/** However it goes (creative breaking, a command), its bot goes with it unless it was packed up properly. */
	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof TradeBotStationBlockEntity station)
			station.removed();
		super.onRemove(state, level, pos, newState, moving);
	}

	@Override
	public Class<TradeBotStationBlockEntity> getBlockEntityClass() {
		return TradeBotStationBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends TradeBotStationBlockEntity> getBlockEntityType() {
		return ModBlockEntities.TRADE_BOT_STATION.get();
	}

	/** A box in pixels, drawn for a station facing north, turned to face {@code facing}. */
	private static VoxelShape rotated(Direction facing, double x0, double y0, double z0, double x1, double y1, double z1) {
		int turns = switch (facing) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
		for (int i = 0; i < turns; i++) {
			// a quarter turn clockwise seen from above: (x, z) -> (16 - z, x)
			double nx0 = 16 - z1, nx1 = 16 - z0, nz0 = x0, nz1 = x1;
			x0 = nx0;
			x1 = nx1;
			z0 = nz0;
			z1 = nz1;
		}
		return Block.box(x0, y0, z0, x1, y1, z1);
	}
}
