package com.comedymonkey.createaddons.content.wastes;

import java.util.EnumMap;
import java.util.Map;

import com.comedymonkey.createaddons.content.wastes.StainedBrickDiagonalBlock.Corner;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cut Deepslate Brick Diagonal (D89; design/models/clockwork-wastes-drum-diagonal.gen.js): the heart drum's courses on
 * its 45-degree diagonals, in Create's cut deepslate brick, the same half cell as the {@link StainedBrickDiagonalBlock} so
 * the courses run flat with the brick face. Its {@code course}: {@code course} (the sill and lintel: the plain half cell)
 * or {@code water_table} (its lower half 4 px proud of the face, the upper half flush: the stair's slope set on the
 * diagonal). It shares the brick's {@code corner}. Never lit. It stops light by its shape, as the brick diagonal does.
 */
public final class CutDeepslateDiagonalBlock extends Block {
	public static final MapCodec<CutDeepslateDiagonalBlock> CODEC = simpleCodec(CutDeepslateDiagonalBlock::new);

	/** The course a diagonal block makes. */
	public enum Course implements StringRepresentable {
		COURSE, WATER_TABLE;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	public static final EnumProperty<Corner> CORNER = StainedBrickDiagonalBlock.CORNER;
	public static final EnumProperty<Course> COURSE = EnumProperty.create("course", Course.class);
	/** The water table's lower half stands this far proud of the face (px). */
	public static final double PROUD = 4;

	private static final Map<Corner, Map<Course, VoxelShape>> SHAPES = new EnumMap<>(Corner.class);

	static {
		for (Corner c : Corner.values()) {
			Map<Course, VoxelShape> m = new EnumMap<>(Course.class);
			for (Course k : Course.values())
				m.put(k, shape(c, k));
			SHAPES.put(c, m);
		}
	}

	/** In 2 px cells: behind the face (the half cell), or for the water table's lower half up to {@link #PROUD} in front of it. */
	private static VoxelShape shape(Corner c, Course k) {
		VoxelShape s = Shapes.empty();
		for (int half = 0; half < 2; half++) {
			boolean lower = half == 0;
			// the distance in front of the face it may reach, along the face's normal (sx, sz) / sqrt 2
			double reach = lower && k == Course.WATER_TABLE ? PROUD * Math.sqrt(2) : 0;
			for (int z = 0; z < 16; z += 2) {
				int x0 = -1;
				for (int x = 0; x <= 16; x += 2) {
					boolean in = x < 16 && (x + 1 - 8) * c.sx + (z + 1 - 8) * c.sz <= reach;
					if (in && x0 < 0)
						x0 = x;
					if (!in && x0 >= 0) {
						s = Shapes.or(s, Block.box(x0, lower ? 0 : 8, z, x, lower ? 8 : 16, z + 2));
						x0 = -1;
					}
				}
			}
		}
		return s.optimize();
	}

	public CutDeepslateDiagonalBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(CORNER, Corner.SOUTH_EAST).setValue(COURSE, Course.COURSE));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CORNER, COURSE);
	}

	/**
	 * Its face toward the player, as the brick diagonal's; the water table when placed against the lower half of a block's
	 * side, or on top of a block that isn't a diagonal (the first course), the plain course otherwise (D90, choice 39 as
	 * revised: courses stacked on a diagonal are plain).
	 */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		double back = Math.toRadians(context.getRotation() + 180);
		double x = -Math.sin(back), z = Math.cos(back);
		return defaultBlockState().setValue(CORNER, Corner.of(x >= 0 ? 1 : -1, z >= 0 ? 1 : -1)).setValue(COURSE, courseFor(context));
	}

	/**
	 * The water table on a lower half: a click on a side face below its middle, or on the top of the block below unless
	 * that's a diagonal (brick or course: this one is stacked on them).
	 */
	public static Course courseFor(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		if (face == Direction.UP)
			return context.getLevel().getBlockState(context.getClickedPos().below()).hasProperty(CORNER) ? Course.COURSE : Course.WATER_TABLE;
		if (face == Direction.DOWN)
			return Course.COURSE;
		double y = context.getClickLocation().y - context.getClickedPos().getY();
		return y < 0.5 ? Course.WATER_TABLE : Course.COURSE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(CORNER)).get(state.getValue(COURSE));
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(CORNER, state.getValue(CORNER).turned(rotation));
	}

	/** A mirror turns its corner (both courses are the same either way along the face). */
	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.setValue(CORNER, state.getValue(CORNER).mirrored(mirror));
	}

	@Override
	protected boolean useShapeForLightOcclusion(BlockState state) {
		return true;
	}
}
