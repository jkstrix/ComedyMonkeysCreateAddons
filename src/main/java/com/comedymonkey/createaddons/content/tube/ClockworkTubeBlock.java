package com.comedymonkey.createaddons.content.tube;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Clockwork Tube: glass between steel rails, carrying one load at a time towards its network's Receiver. It joins what
 * it can on all six sides (see {@link TubeJoints}); a wrench on one of its arms shuts that joint, or opens it again, so
 * networks can run side by side (design/specs/clockwork-tubes.md D5, D14, D20, D29). Broken, the load inside drops.
 */
public class ClockworkTubeBlock extends Block implements IBE<ClockworkTubeBlockEntity>, IWrenchable, SimpleWaterloggedBlock {
	public static final Map<Direction, EnumProperty<TubeJoint>> JOINTS = new EnumMap<>(Direction.class);
	static {
		for (Direction side : Direction.values())
			JOINTS.put(side, EnumProperty.create(side.getSerializedName(), TubeJoint.class));
	}
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	public ClockworkTubeBlock(Properties properties) {
		super(properties);
		BlockState state = stateDefinition.any().setValue(WATERLOGGED, false);
		for (EnumProperty<TubeJoint> p : JOINTS.values())
			state = state.setValue(p, TubeJoint.NONE);
		registerDefaultState(state);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		JOINTS.values().forEach(builder::add);
		builder.add(WATERLOGGED);
	}

	// ---------- joints ----------

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Map<Direction, TubeJoints.Refusal> why = new EnumMap<>(Direction.class);
		BlockState state = withJoints(defaultBlockState(), TubeJoints.compute(context.getLevel(), context.getClickedPos(), null, why));
		return state.setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos,
									 BlockPos neighbourPos) {
		if (state.getValue(WATERLOGGED))
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		// only the side that changed (its other joints are looked at again by the tube itself when anything changes)
		Map<Direction, TubeJoint> joints = new EnumMap<>(Direction.class);
		for (Direction side : Direction.values())
			joints.put(side, state.getValue(JOINTS.get(side)));
		Map<Direction, TubeJoints.Refusal> why = new EnumMap<>(Direction.class);
		TubeJoint joint = TubeJoints.side(level, pos, direction, joints, joints.get(direction), why);
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ClockworkTubeBlockEntity be)
			be.setRefusal(direction, why.get(direction));
		return state.setValue(JOINTS.get(direction), joint);
	}

	/** Turned or mirrored (a schematic, a contraption): its joints turn with it. */
	@Override
	protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
		BlockState out = state;
		for (Direction side : Direction.values())
			out = out.setValue(JOINTS.get(rotation.rotate(side)), state.getValue(JOINTS.get(side)));
		return out;
	}

	@Override
	protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
		BlockState out = state;
		for (Direction side : Direction.values())
			out = out.setValue(JOINTS.get(mirror.mirror(side)), state.getValue(JOINTS.get(side)));
		return out;
	}

	/** Its joints worked out again (and why any is capped, for the goggles). */
	static BlockState rejoin(BlockState state, LevelAccessor level, BlockPos pos) {
		Map<Direction, TubeJoints.Refusal> why = new EnumMap<>(Direction.class);
		BlockState joined = withJoints(state, TubeJoints.compute(level, pos, state, why));
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ClockworkTubeBlockEntity be)
			be.setRefusals(why);
		return joined;
	}

	static BlockState withJoints(BlockState state, Map<Direction, TubeJoint> joints) {
		for (Map.Entry<Direction, TubeJoint> e : joints.entrySet())
			state = state.setValue(JOINTS.get(e.getKey()), e.getValue());
		return state;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (!level.isClientSide)
			TubeJoints.changed(level, pos);
	}

	/** Broken: the load inside drops where it was (D20); the network notices. */
	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
		if (!state.is(newState.getBlock())) {
			if (level.getBlockEntity(pos) instanceof ClockworkTubeBlockEntity be && be.getLoad() != null)
				Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, be.getLoad().stack);
			if (!level.isClientSide)
				TubeJoints.changed(level, pos);
		}
		super.onRemove(state, level, pos, newState, movedByPiston);
	}

	// ---------- the wrench: shut or open the joint on the arm it touches ----------

	@Override
	public InteractionResult onWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Vec3 rel = context.getClickLocation().subtract(Vec3.atCenterOf(pos));
		Direction side = Direction.getNearest(rel.x, rel.y, rel.z);
		if (!TubeJoints.joinable(level, pos, side) || !(level.getBlockEntity(pos) instanceof ClockworkTubeBlockEntity be))
			return InteractionResult.PASS;
		if (level.isClientSide)
			return InteractionResult.SUCCESS;
		boolean shut = !TubeJoints.shut(level, pos, side);
		be.setShut(side, shut);
		// opening clears both sides' marks, so the joint can join again
		if (!shut && level.getBlockEntity(pos.relative(side)) instanceof ClockworkTubeBlockEntity other)
			other.setShut(side.getOpposite(), false);
		level.setBlock(pos, rejoin(state, level, pos), 3);
		BlockPos n = pos.relative(side);
		BlockState ns = level.getBlockState(n);
		if (ns.getBlock() instanceof ClockworkTubeBlock)
			level.setBlock(n, rejoin(ns, level, n), 3);
		level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.4f, shut ? 0.9f : 1.2f);
		return InteractionResult.SUCCESS;
	}

	// ---------- shape: the hub or the straight middle, and an arm per joint ----------

	private static final VoxelShape CORE = Block.box(3, 3, 3, 13, 13, 13);
	private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);
	static {
		ARMS.put(Direction.NORTH, Block.box(3, 3, 0, 13, 13, 3));
		ARMS.put(Direction.SOUTH, Block.box(3, 3, 13, 13, 13, 16));
		ARMS.put(Direction.WEST, Block.box(0, 3, 3, 3, 13, 13));
		ARMS.put(Direction.EAST, Block.box(13, 3, 3, 16, 13, 13));
		ARMS.put(Direction.DOWN, Block.box(3, 0, 3, 13, 3, 13));
		ARMS.put(Direction.UP, Block.box(3, 13, 3, 13, 16, 13));
	}
	private static final Map<BlockState, VoxelShape> SHAPES = new ConcurrentHashMap<>();

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.computeIfAbsent(state, s -> {
			VoxelShape shape = CORE;
			for (Direction side : Direction.values())
				if (s.getValue(JOINTS.get(side)) != TubeJoint.NONE)
					shape = Shapes.or(shape, ARMS.get(side));
			return shape.optimize();
		});
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType type) {
		return false;
	}

	@Override
	public Class<ClockworkTubeBlockEntity> getBlockEntityClass() {
		return ClockworkTubeBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends ClockworkTubeBlockEntity> getBlockEntityType() {
		return ModBlockEntities.CLOCKWORK_TUBE.get();
	}

	/** True for game tests and the receiver: the level is a server level and its chunk at {@code pos} is running. */
	static boolean running(Level level, BlockPos pos) {
		return level instanceof ServerLevel && level.isLoaded(pos) && level.shouldTickBlocksAt(pos);
	}
}
