package com.comedymonkey.createaddons.content.line;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * A Fluid Line: a slim pipe that moves fluid with no pump and no power. Each side joins the next piece of line (the
 * same tier, and the same colour unless a wrench joined them), or a block that holds fluid (a tank), or draws from a
 * tank through an Intake. The fluid only moves from Intakes (see {@link FluidLines}); the line holds none itself.
 */
public class FluidLineBlock extends BaseEntityBlock implements IWrenchable, SimpleWaterloggedBlock, LineBlock {
	public static final MapCodec<FluidLineBlock> CODEC = simpleCodec(p -> new FluidLineBlock(LineTier.COPPER, p));
	public static final Map<Direction, EnumProperty<LineSide>> SIDES = new EnumMap<>(Direction.class);
	/** Anything but a straight piece through the block (a bend, a junction, an end) has a round hub in the middle. */
	public static final BooleanProperty HUB = BooleanProperty.create("hub");
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	static {
		for (Direction d : Direction.values())
			SIDES.put(d, EnumProperty.create(d.getSerializedName(), LineSide.class));
	}

	private final LineTier tier;
	private final Map<BlockState, VoxelShape> shapes = new ConcurrentHashMap<>();

	public FluidLineBlock(LineTier tier, Properties properties) {
		super(properties);
		this.tier = tier;
		BlockState state = stateDefinition.any().setValue(HUB, true).setValue(WATERLOGGED, false);
		for (EnumProperty<LineSide> side : SIDES.values())
			state = state.setValue(side, LineSide.NONE);
		registerDefaultState(state);
	}

	public LineTier tier() {
		return tier;
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		SIDES.values().forEach(builder::add);
		builder.add(HUB, WATERLOGGED);
	}

	public static LineSide side(BlockState state, Direction d) {
		return state.getValue(SIDES.get(d));
	}

	@Override
	public boolean takes(Fitting fitting) {
		return fitting.fitsFluidLines();
	}

	@Nullable
	@Override
	public Direction straightSide(Level level, BlockPos pos, BlockState state) {
		if (state.getValue(HUB))
			return null;
		for (Direction d : Direction.values())
			if (side(state, d).connected())
				return d;
		return null;
	}

	// ---------- connections ----------

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = defaultBlockState().setValue(WATERLOGGED,
			context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
		return connected(state, context.getLevel(), context.getClickedPos());
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
									 BlockPos pos, BlockPos neighborPos) {
		if (state.getValue(WATERLOGGED))
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		if (!(neighborState.getBlock() instanceof LineBlock) && level.getBlockEntity(pos) instanceof LineBlockEntity be)
			be.forgetJoint(direction);
		return withHub(state.setValue(SIDES.get(direction), sideFor(level, pos, direction)));
	}

	/** {@code state} with every side worked out from the world around it. */
	public BlockState connected(BlockState state, LevelAccessor level, BlockPos pos) {
		for (Direction d : Direction.values())
			state = state.setValue(SIDES.get(d), sideFor(level, pos, d));
		return withHub(state);
	}

	/** Works the sides out again after the block entity's joints, dye or fitting changed. */
	public static void refresh(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof FluidLineBlock line) {
			BlockState next = line.connected(state, level, pos);
			if (next != state)
				level.setBlock(pos, next, Block.UPDATE_ALL);
		}
	}

	private LineSide sideFor(LevelAccessor level, BlockPos pos, Direction d) {
		LineBlockEntity be = level.getBlockEntity(pos) instanceof LineBlockEntity l ? l : null;
		if (be != null && be.intakeSide() == d)
			return LineSide.INTAKE;
		if (be != null && be.isCut(d))
			return LineSide.NONE;
		BlockPos next = pos.relative(d);
		// beside an unloaded chunk: keep the side as it was (reading the neighbour would load its chunk)
		if (level instanceof Level l && !l.isLoaded(next) && level.getBlockState(pos).getBlock() == this)
			return side(level.getBlockState(pos), d);
		BlockState other = level.getBlockState(next);
		if (other.getBlock() instanceof FluidLineBlock line) {
			if (line.tier != tier)
				return LineSide.NONE;
			LineBlockEntity o = level.getBlockEntity(next) instanceof LineBlockEntity l ? l : null;
			// no block entities yet (while being placed): joined as plain line (worked out again once its colour is in)
			if (be == null || o == null)
				return (be == null || be.dye() == null) && (o == null || o.dye() == null) ? LineSide.LINE : LineSide.NONE;
			return LineBlockEntity.joins(be, d, o) ? LineSide.LINE : LineSide.NONE;
		}
		if (other.getBlock() instanceof LineBlock)
			return LineSide.NONE;
		if (level instanceof Level l && l.getCapability(Capabilities.FluidHandler.BLOCK, next, d.getOpposite()) != null)
			return LineSide.TANK;
		return LineSide.NONE;
	}

	private static BlockState withHub(BlockState state) {
		int count = 0;
		Direction first = null;
		boolean straight = false;
		for (Direction d : Direction.values()) {
			if (!side(state, d).connected())
				continue;
			count++;
			if (first == null)
				first = d;
			else if (d == first.getOpposite())
				straight = true;
		}
		return state.setValue(HUB, !(count == 2 && straight));
	}

	// ---------- turning (structures, schematics, contraptions) ----------

	@Override
	protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
		BlockState out = state;
		for (Direction d : Direction.values())
			out = out.setValue(SIDES.get(rotation.rotate(d)), side(state, d));
		return out;
	}

	@Override
	protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
		BlockState out = state;
		for (Direction d : Direction.values())
			out = out.setValue(SIDES.get(mirror.mirror(d)), side(state, d));
		return out;
	}

	// ---------- shape ----------

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		if (level.getBlockEntity(pos) instanceof LineBlockEntity be && be.encasing() != null)
			return Shapes.block();
		return shapes.computeIfAbsent(state, FluidLineBlock::makeShape);
	}

	@Override
	public VoxelShape lineShape(BlockState state, BlockGetter level, BlockPos pos) {
		return shapes.computeIfAbsent(state, FluidLineBlock::makeShape);
	}

	private static VoxelShape makeShape(BlockState state) {
		VoxelShape shape = state.getValue(HUB) ? Block.box(5, 5, 5, 11, 11, 11) : Shapes.empty();
		for (Direction d : Direction.values()) {
			LineSide side = side(state, d);
			if (!side.connected())
				continue;
			shape = Shapes.or(shape, arm(d, 6, 10, 0, 8));
			if (side == LineSide.TANK)
				shape = Shapes.or(shape, arm(d, 4, 12, 0, 1));
			if (side == LineSide.INTAKE)
				shape = Shapes.or(shape, arm(d, 4, 12, 0, 1), arm(d, 5, 11, 1, 5));
		}
		// an in-line fitting sits round the middle of a straight piece
		if (!state.getValue(HUB))
			shape = Shapes.or(shape, Block.box(5, 5, 5, 11, 11, 11));
		return shape.optimize();
	}

	/** A box w0-w1 across, from {@code near} to {@code far} px in from the face on side d. */
	static VoxelShape arm(Direction d, double w0, double w1, double near, double far) {
		return switch (d) {
			case NORTH -> Block.box(w0, w0, near, w1, w1, far);
			case SOUTH -> Block.box(w0, w0, 16 - far, w1, w1, 16 - near);
			case WEST -> Block.box(near, w0, w0, far, w1, w1);
			case EAST -> Block.box(16 - far, w0, w0, 16 - near, w1, w1);
			case DOWN -> Block.box(w0, near, w0, w1, far, w1);
			case UP -> Block.box(w0, 16 - far, w0, w1, 16 - near, w1);
		};
	}

	// ---------- use ----------

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
											  InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof LineBlockEntity be))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		return LineInteractions.useItem(stack, be, level, pos, player);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof LineBlockEntity be))
			return InteractionResult.PASS;
		return LineInteractions.useEmpty(be, level, pos, player);
	}

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof LineBlockEntity be))
			return InteractionResult.PASS;
		Direction side = nearestSide(context.getClickedPos(), context.getClickLocation(), context.getClickedFace());
		return LineInteractions.wrench(be, context.getLevel(), context.getPlayer(), side, false);
	}

	/** Sneaking, the wrench takes an encasing off first; a bare line it picks up, as with any Create block. */
	@Override
	public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
		if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof LineBlockEntity be && be.encasing() != null)
			return LineInteractions.wrench(be, context.getLevel(), context.getPlayer(), context.getClickedFace(), true);
		return IWrenchable.super.onSneakWrenched(state, context);
	}

	/** The side whose arm the click landed on: the axis the hit is furthest out along, or the clicked face at the hub. */
	public static Direction nearestSide(BlockPos pos, Vec3 hit, Direction face) {
		Vec3 local = hit.subtract(Vec3.atCenterOf(pos));
		double ax = Math.abs(local.x), ay = Math.abs(local.y), az = Math.abs(local.z);
		double max = Math.max(ax, Math.max(ay, az));
		if (max < 3.5 / 16)
			return face;
		if (max == ax)
			return local.x > 0 ? Direction.EAST : Direction.WEST;
		if (max == ay)
			return local.y > 0 ? Direction.UP : Direction.DOWN;
		return local.z > 0 ? Direction.SOUTH : Direction.NORTH;
	}

	/** A redstone signal shuts a Valve. */
	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos from, boolean moving) {
		super.neighborChanged(state, level, pos, neighbor, from, moving);
		if (!level.isClientSide && level.getBlockEntity(pos) instanceof LineBlockEntity be && be.fitting() == Fitting.VALVE)
			be.setPowered(level.hasNeighborSignal(pos));
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	/** A Meter's reading: the flow through it as a share of the line's rate. */
	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof LineBlockEntity be ? be.comparatorSignal() : 0;
	}

	/** A fitting and an encasing come off with the line. */
	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof LineBlockEntity be) {
			if (be.fitting() != Fitting.NONE)
				Block.popResource(level, pos, new ItemStack(be.fitting().item()));
			if (be.encasing() != null)
				Block.popResource(level, pos, new ItemStack(be.encasing().getBlock()));
		}
		if (!state.is(newState.getBlock()) && level instanceof ServerLevel server)
			FluidLines.of(server).changed(pos);
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	/** A dyed line's colour arrives with its block entity, after the sides were first worked out: they're redone. */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!level.isClientSide) {
			refresh(level, pos);
			for (Direction d : Direction.values())
				refresh(level, pos.relative(d));
		}
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (state != oldState && level instanceof ServerLevel server) {
			FluidLines.of(server).changed(pos);
			if (oldState.is(this) && level.getBlockEntity(pos) instanceof LineBlockEntity be && be.fitting() != Fitting.NONE
				&& be.fitting() != Fitting.INTAKE && straightSide(level, pos, state) == null)
				be.removeFitting(null);
		}
		// put there some other way than by hand (a command, a structure, a schematic): its sides are worked out next tick
		if (!oldState.is(this) && !level.isClientSide)
			level.scheduleTick(pos, this, 1);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
		refresh(level, pos);
	}

	/** Picked, a line of its colour. */
	@Override
	public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target, net.minecraft.world.level.LevelReader level, BlockPos pos,
		Player player) {
		return LineItem.of(this, level, pos);
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FluidLineBlockEntity(pos, state);
	}
}
