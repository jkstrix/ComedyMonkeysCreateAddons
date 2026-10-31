package com.comedymonkey.createaddons.content.wastes;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.util.StringRepresentable;
import com.simibubi.create.content.decoration.girder.GirderBlock;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Derelict Cable (D16, P16, P25; brief §4): copper drawn from the makers' pipes on void-alloy clips, laid flat along a
 * floor, a wall or a ceiling ({@link #SURFACE}: the side of its block it lies on; placed on the face clicked) and joining
 * the cable beside it on that same side like a wire (on a floor, also the Substations and street lamps it runs to).
 * While its district is fed, a hairline of starlight runs along it (a lit part in its block model, so a street of it
 * costs nothing to draw). It casts no light, isn't walked into, and breaks into a little scrap (one in four).
 * <p>
 * The mast riser (D72): on the side of a Create Metal Girder standing upright, cable rises up a lattice mast ({@link #GIRDER}
 * foot, run or top; {@link #SURFACE} is the girder's side of it). Its model reaches into the girder's cell: a riser guard
 * over the foot, a clamp biting the girder every block, a pothead at the top and the jumper up to the tie knob of the
 * insulator on the mast. The foot joins the floor cable beside it as floor cable does; a riser's line lights with its
 * district as any cable's. Which part a riser is follows the girders and risers round it (the top where the girder
 * column ends, the foot with no riser under it).
 */
public class DerelictCableBlock extends BaseEntityBlock implements DerelictLightBlockEntity.DerelictLight {
	public static final MapCodec<DerelictCableBlock> CODEC = simpleCodec(DerelictCableBlock::new);
	public static final BooleanProperty NORTH = BlockStateProperties.NORTH, EAST = BlockStateProperties.EAST,
		SOUTH = BlockStateProperties.SOUTH, WEST = BlockStateProperties.WEST, UP = BlockStateProperties.UP, DOWN = BlockStateProperties.DOWN,
		LIT = BlockStateProperties.LIT;
	/** Up a girder (D72): none for cable on a floor, wall or ceiling; foot, run, top for a mast riser. */
	public static final EnumProperty<Girder> GIRDER = EnumProperty.create("girder", Girder.class);
	/** The side of its block it lies on: down on a floor, up on a ceiling, a wall's side. */
	public static final DirectionProperty SURFACE = DirectionProperty.create("surface");
	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		SHAPES.put(Direction.DOWN, Block.box(0, 0, 0, 16, 1.5, 16));
		SHAPES.put(Direction.UP, Block.box(0, 14.5, 0, 16, 16, 16));
		SHAPES.put(Direction.NORTH, Block.box(0, 0, 0, 16, 16, 1.5));
		SHAPES.put(Direction.SOUTH, Block.box(0, 0, 14.5, 16, 16, 16));
		SHAPES.put(Direction.WEST, Block.box(0, 0, 0, 1.5, 16, 16));
		SHAPES.put(Direction.EAST, Block.box(14.5, 0, 0, 16, 16, 16));
	}

	public DerelictCableBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(SURFACE, Direction.DOWN).setValue(NORTH, false).setValue(EAST, true).setValue(SOUTH, false)
			.setValue(WEST, true).setValue(UP, false).setValue(DOWN, false).setValue(LIT, false).setValue(GIRDER, Girder.NONE));
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(SURFACE, NORTH, EAST, SOUTH, WEST, UP, DOWN, LIT, GIRDER);
	}

	@Override
	public boolean lightable(BlockState state) {
		return true;
	}

	public static BooleanProperty side(Direction d) {
		return switch (d) {
			case NORTH -> NORTH;
			case EAST -> EAST;
			case SOUTH -> SOUTH;
			case WEST -> WEST;
			case UP -> UP;
			case DOWN -> DOWN;
		};
	}

	/** The ways an arm can run on a surface: along it, never into or out of it. */
	public static List<Direction> along(Direction surface) {
		List<Direction> out = new ArrayList<>(4);
		for (Direction d : Direction.values())
			if (d.getAxis() != surface.getAxis())
				out.add(d);
		return out;
	}

	/**
	 * What a cable joins: more cable on the same side; on a floor, a Substation's foot or a street lamp's foot too, and a
	 * mast riser's foot (D72). A riser's run and top join nothing.
	 */
	private boolean joins(Direction surface, BlockState there) {
		if (there.is(this))
			return there.getValue(GIRDER) == Girder.NONE ? there.getValue(SURFACE) == surface
				: surface == Direction.DOWN && there.getValue(GIRDER) == Girder.FOOT;
		return surface == Direction.DOWN && (there.getBlock() instanceof SubstationBlock && there.getValue(SubstationBlock.HALF) == DoubleBlockHalf.LOWER
			|| there.getBlock() instanceof DerelictStreetLampBlock && there.getValue(DerelictStreetLampBlock.PART) == DerelictStreetLampBlock.Part.FOOT);
	}

	/**
	 * Its arms: toward everything it joins along its side; a cable joining one thing runs straight on past it; alone, it
	 * runs level (east to west on a floor, a ceiling or a north or south wall; north to south on an east or west wall).
	 */
	public BlockState connected(BlockState state, BlockGetter level, BlockPos pos) {
		Direction surface = state.getValue(SURFACE);
		Girder girder = state.getValue(GIRDER);
		if (girder != Girder.NONE) {
			// a riser: no arms but the foot's floor ones, toward the floor cable beside it (never toward its girder)
			BlockState out = state;
			for (Direction d : Direction.values())
				out = out.setValue(side(d), girder == Girder.FOOT && d.getAxis().isHorizontal() && d != surface
					&& joins(Direction.DOWN, level.getBlockState(pos.relative(d))));
			return out;
		}
		Set<Direction> join = EnumSet.noneOf(Direction.class);
		for (Direction d : along(surface))
			if (joins(surface, level.getBlockState(pos.relative(d))))
				join.add(d);
		if (join.isEmpty()) {
			Direction run = surface.getAxis() == Direction.Axis.X ? Direction.NORTH : Direction.EAST;
			join.add(run);
			join.add(run.getOpposite());
		} else if (join.size() == 1)
			join.add(join.iterator().next().getOpposite());
		BlockState out = state;
		for (Direction d : Direction.values())
			out = out.setValue(side(d), join.contains(d));
		return out;
	}

	/**
	 * On the face clicked: the floor of the block below, a wall beside it, the ceiling above; on the side of an upright
	 * Create Metal Girder, a mast riser (its part from what's round it).
	 */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction surface = context.getClickedFace().getOpposite();
		BlockPos pos = context.getClickedPos();
		BlockState state = defaultBlockState().setValue(SURFACE, surface);
		if (surface.getAxis().isHorizontal() && uprightGirder(context.getLevel().getBlockState(pos.relative(surface))))
			state = state.setValue(GIRDER, riserPart(context.getLevel(), pos, surface));
		if (!canSurvive(state, context.getLevel(), pos))
			return null;
		return connected(state, context.getLevel(), pos);
	}

	/** A Create Metal Girder standing upright (a mast's), the only thing a riser climbs. */
	public static boolean uprightGirder(BlockState state) {
		return state.getBlock() instanceof GirderBlock && state.getValue(GirderBlock.AXIS) == Direction.Axis.Y;
	}

	/**
	 * Which part of a riser on the {@code side} of a girder at {@code pos} is: the top where the girder column ends (the
	 * insulator on it), the foot where no riser on the same side is under it, a run between.
	 */
	public Girder riserPart(BlockGetter level, BlockPos pos, Direction side) {
		if (!uprightGirder(level.getBlockState(pos.relative(side).above())))
			return Girder.TOP;
		BlockState below = level.getBlockState(pos.below());
		boolean riserBelow = below.is(this) && below.getValue(GIRDER) != Girder.NONE && below.getValue(SURFACE) == side;
		return riserBelow ? Girder.RUN : Girder.FOOT;
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		Direction surface = state.getValue(SURFACE);
		if (direction == surface && !canSurvive(state, level, pos))
			return Blocks.AIR.defaultBlockState();
		if (state.getValue(GIRDER) != Girder.NONE)
			return connected(state.setValue(GIRDER, riserPart(level, pos, surface)), level, pos);
		return direction.getAxis() != surface.getAxis() ? connected(state, level, pos) : state;
	}

	/** On a sturdy face, as a wire; a riser beside an upright girder. */
	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		Direction surface = state.getValue(SURFACE);
		BlockPos on = pos.relative(surface);
		if (state.getValue(GIRDER) != Girder.NONE)
			return uprightGirder(level.getBlockState(on));
		return level.getBlockState(on).isFaceSturdy(level, on, surface.getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(SURFACE));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof DerelictLightBlockEntity be)
			be.scheduledTick();
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DerelictLightBlockEntity(pos, state);
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		BlockState out = state.setValue(SURFACE, rotation.rotate(state.getValue(SURFACE)));
		for (Direction d : Direction.Plane.HORIZONTAL)
			out = out.setValue(side(rotation.rotate(d)), state.getValue(side(d)));
		return out;
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		BlockState out = state.setValue(SURFACE, mirror.mirror(state.getValue(SURFACE)));
		for (Direction d : Direction.Plane.HORIZONTAL)
			out = out.setValue(side(mirror.mirror(d)), state.getValue(side(d)));
		return out;
	}

	/** A cable's place up a girder (D72): none (on a floor, wall or ceiling), or a mast riser's foot, run or top. */
	public enum Girder implements StringRepresentable {
		NONE, FOOT, RUN, TOP;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}
}
