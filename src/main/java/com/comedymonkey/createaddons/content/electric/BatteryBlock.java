package com.comedymonkey.createaddons.content.electric;

import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * Battery: a store of Forge Energy. Its connectors are its two sides, its front and its back (none on top or bottom),
 * and Batteries that touch there join into one bank with one store ({@link BatteryStore}), a copper bus bar between
 * them. Batteries filling a 3×3 footprint 3 to 9 high form an Industrial Battery ({@link BatteryTowers}): FORMED, they
 * show nothing of their own (the tower's tile for each block is drawn instead) and connect only through the tower's
 * insulators.
 */
public class BatteryBlock extends Block implements IBE<BatteryBlockEntity>, IWrenchable {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty FORMED = BooleanProperty.create("formed");
	/** Joined to a Battery on that side: a bus bar instead of a terminal (left and right as you face its front). */
	public static final BooleanProperty LEFT = BooleanProperty.create("left");
	public static final BooleanProperty RIGHT = BooleanProperty.create("right");
	public static final BooleanProperty BACK = BooleanProperty.create("back");

	public BatteryBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(FORMED, false)
			.setValue(LEFT, false).setValue(RIGHT, false).setValue(BACK, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, FORMED, LEFT, RIGHT, BACK);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
		return joins(state, context.getLevel(), context.getClickedPos());
	}

	/** The model's left side is the west of a Battery facing north: counter-clockwise of its front. */
	public static Direction left(BlockState state) {
		return state.getValue(FACING).getCounterClockWise();
	}

	/** Connectors: the four sides round it, while it's a plain Battery. */
	public static boolean isConnector(BlockState state, Direction side) {
		return side != null && side.getAxis().isHorizontal() && !state.getValue(FORMED);
	}

	static boolean isFormed(BlockState state) {
		return state.getBlock() instanceof BatteryBlock && state.getValue(FORMED);
	}

	/** A plain Battery: it joins those beside it. */
	static boolean isPlain(BlockState state) {
		return state.getBlock() instanceof BatteryBlock && !state.getValue(FORMED);
	}

	/** Its bus bars: one on each side where a plain Battery touches it (a formed one shows none). */
	static BlockState joins(BlockState state, BlockGetter level, BlockPos pos) {
		if (state.getValue(FORMED))
			return state.setValue(LEFT, false).setValue(RIGHT, false).setValue(BACK, false);
		Direction left = left(state);
		return state.setValue(LEFT, isPlain(level.getBlockState(pos.relative(left))))
			.setValue(RIGHT, isPlain(level.getBlockState(pos.relative(left.getOpposite()))))
			.setValue(BACK, isPlain(level.getBlockState(pos.relative(state.getValue(FACING).getOpposite()))));
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos,
									 BlockPos neighborPos) {
		return direction.getAxis().isHorizontal() ? joins(state, level, pos) : state;
	}

	/** Placed (or put back by a contraption): it may complete a tower, worked out on the next tick. */
	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (!oldState.is(this) && level instanceof ServerLevel server) {
			level.scheduleTick(pos, this, 1);
			// it may join banks, and the lines that meet them, into one network
			com.comedymonkey.createaddons.content.line.PowerLines.of(server).changed(pos);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		BatteryTowers.placed(level, pos);
	}

	/** Broken, blown up or carried off: it keeps its share, and a tower it was part of comes apart or re-forms. */
	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof BatteryBlockEntity be) {
			BatteryTowers.removed(server, pos, state, be, movedByPiston);
			com.comedymonkey.createaddons.content.line.PowerLines.of(server).changed(pos);
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	/** Forming and lightning are shown on the client through block events, which go to the block entity. */
	@Override
	protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
		super.triggerEvent(state, level, pos, id, param);
		net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
		return be != null && be.triggerEvent(id, param);
	}

	/** A tower's blocks don't darken each other's faces where their tiles meet (the seams would show). */
	@Override
	protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
		return state.getValue(FORMED) ? 1 : super.getShadeBrightness(state, level, pos);
	}

	/** Shears on an Industrial Battery take down the spans on the insulator nearest where it was clicked. */
	@Override
	protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos,
																   net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
																   net.minecraft.world.phys.BlockHitResult hit) {
		if (!(stack.getItem() instanceof net.minecraft.world.item.ShearsItem) || !state.getValue(FORMED)
			|| !(level.getBlockEntity(pos) instanceof BatteryBlockEntity be) || !(be.spanHolder() instanceof BatteryBlockEntity master))
			return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		int insulator = master.nearestInsulator(hit.getLocation());
		if (master.spansAt(insulator).isEmpty())
			return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (!level.isClientSide) {
			for (net.minecraft.world.item.ItemStack wire : master.takeDown(insulator))
				if (!player.getInventory().add(wire))
					Block.popResource(level, pos, wire);
			stack.hurtAndBreak(1, player, hand == net.minecraft.world.InteractionHand.MAIN_HAND ? net.minecraft.world.entity.EquipmentSlot.MAINHAND
				: net.minecraft.world.entity.EquipmentSlot.OFFHAND);
			level.playSound(null, pos, net.minecraft.sounds.SoundEvents.SHEEP_SHEAR, net.minecraft.sounds.SoundSource.BLOCKS, 0.8f, 1.2f);
		}
		return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
	}

	/** Turned by a wrench while plain; part of a tower, it stays as it is. */
	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		if (state.getValue(FORMED))
			return InteractionResult.PASS;
		return IWrenchable.super.onWrenched(state, context);
	}

	/** Turned with what's round it (a structure, a contraption): its bus bars, being relative to its front, still hold. */
	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	public Class<BatteryBlockEntity> getBlockEntityClass() {
		return BatteryBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends BatteryBlockEntity> getBlockEntityType() {
		return ModBlockEntities.BATTERY.get();
	}
}
