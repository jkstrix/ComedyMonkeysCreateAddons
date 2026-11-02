package com.comedymonkey.createaddons.content.wastes;

import java.util.EnumMap;
import java.util.Map;

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
 * Stained End Stone Brick Diagonal (D86, D88; design/models/clockwork-wastes-drum-diagonal.gen.js): half a cell cut on
 * the 45-degree line through two of its corners, its face looking toward {@code corner}, so a run of them along an
 * octagon's diagonal makes one flat face where full blocks can only make steps. Its {@code form}: {@code full}, or one of
 * the blind arch's springers ({@code arch_neg}, {@code arch_pos}: the top half full, the bottom half only on one side
 * along the face). The heart drum lays it between its courses with a recessed panel and a stair-headed arch. Never lit.
 * It stops light by its shape, as stairs do: its back is a full face, its open half lets light through.
 */
public final class StainedBrickDiagonalBlock extends Block {
	public static final MapCodec<StainedBrickDiagonalBlock> CODEC = simpleCodec(StainedBrickDiagonalBlock::new);

	/** Which way the face looks: the corner of the cell it runs across from. */
	public enum Corner implements StringRepresentable {
		SOUTH_EAST(1, 1), SOUTH_WEST(-1, 1), NORTH_WEST(-1, -1), NORTH_EAST(1, -1);

		public final int sx, sz;

		Corner(int sx, int sz) {
			this.sx = sx;
			this.sz = sz;
		}

		public static Corner of(int sx, int sz) {
			for (Corner c : values())
				if (c.sx == Integer.signum(sx) && c.sz == Integer.signum(sz))
					return c;
			throw new IllegalArgumentException("no diagonal corner " + sx + ", " + sz);
		}

		/** Quarter turns clockwise (from above) from {@link #SOUTH_EAST}, as the blockstate turns its model. */
		public int quarters() {
			return ordinal();
		}

		/** Turned {@code rotation}'s quarters clockwise. */
		public Corner turned(Rotation rotation) {
			int q = (quarters() + rotation.ordinal()) & 3;
			for (Corner c : values())
				if (c.quarters() == q)
					return c;
			throw new IllegalStateException();
		}

		/** Mirrored: each of its two sides goes to its mirror image, as {@code kit/Site.orient} turns it. */
		public Corner mirrored(Mirror mirror) {
			Direction x = mirror.mirror(sx > 0 ? Direction.EAST : Direction.WEST), z = mirror.mirror(sz > 0 ? Direction.SOUTH : Direction.NORTH);
			return of(x.getStepX(), z.getStepZ());
		}

		@Override
		public String getSerializedName() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	/** The whole half cell, or one of the blind arch's springers (the bottom half only on the -u or +u side). */
	public enum Form implements StringRepresentable {
		FULL, ARCH_NEG, ARCH_POS;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}

		public Form mirrored() {
			return this == ARCH_NEG ? ARCH_POS : this == ARCH_POS ? ARCH_NEG : this;
		}
	}

	public static final EnumProperty<Corner> CORNER = EnumProperty.create("corner", Corner.class);
	public static final EnumProperty<Form> FORM = EnumProperty.create("form", Form.class);

	private static final Map<Corner, Map<Form, VoxelShape>> SHAPES = new EnumMap<>(Corner.class);

	static {
		for (Corner c : Corner.values()) {
			Map<Form, VoxelShape> m = new EnumMap<>(Form.class);
			for (Form f : Form.values())
				m.put(f, shape(c, f));
			SHAPES.put(c, m);
		}
	}

	/**
	 * The along-face direction u (as the model's box x turned 45 degrees), in the cell for {@code c}: for south-east, toward
	 * the north-east corner (+x, -z); turned a quarter clockwise per corner.
	 */
	public static int[] u(Corner c) {
		int ux = 1, uz = -1;
		for (int k = 0; k < c.quarters(); k++) {
			int t = ux;
			ux = -uz;
			uz = t;
		}
		return new int[] { ux, uz };
	}

	/** Whether the point (x, z) of the cell (0-16) at the given half lies in the block, drawn in 2 px cells. */
	private static boolean inside(Corner c, Form f, double x, double z, boolean lower) {
		// the face's normal from the cell's middle; inside is the half behind the face
		double nx = c.sx, nz = c.sz;
		if ((x - 8) * nx + (z - 8) * nz > 0)
			return false;
		if (!lower || f == Form.FULL)
			return true;
		int[] u = u(c);
		double along = (x - 8) * u[0] + (z - 8) * u[1];
		return f == Form.ARCH_NEG ? along <= 0 : along >= 0;
	}

	private static VoxelShape shape(Corner c, Form f) {
		VoxelShape s = Shapes.empty();
		for (int half = 0; half < 2; half++) {
			boolean lower = half == 0;
			for (int z = 0; z < 16; z += 2) {
				int x0 = -1;
				for (int x = 0; x <= 16; x += 2) {
					boolean in = x < 16 && inside(c, f, x + 1, z + 1, lower);
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

	public StainedBrickDiagonalBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(CORNER, Corner.SOUTH_EAST).setValue(FORM, Form.FULL));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CORNER, FORM);
	}

	/** Its face toward the player: the diagonal nearest the way back to them. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		// the player's yaw: 0 looks south, 90 west; the face looks the other way
		double back = Math.toRadians(context.getRotation() + 180);
		double x = -Math.sin(back), z = Math.cos(back);
		return defaultBlockState().setValue(CORNER, Corner.of(x >= 0 ? 1 : -1, z >= 0 ? 1 : -1));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(CORNER)).get(state.getValue(FORM));
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(CORNER, state.getValue(CORNER).turned(rotation));
	}

	/** A mirror turns its corner and swaps its arch springers' sides (the along-face direction flips). */
	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		if (mirror == Mirror.NONE)
			return state;
		return state.setValue(CORNER, state.getValue(CORNER).mirrored(mirror)).setValue(FORM, state.getValue(FORM).mirrored());
	}

	@Override
	protected boolean useShapeForLightOcclusion(BlockState state) {
		return true;
	}
}
