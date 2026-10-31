package com.comedymonkey.createaddons.content.wastes;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Derelict Street Lamp (design/specs/clockwork-wastes.md, D16, P25; brief §4): the makers' cast-iron post with the
 * uprising's hooded lens on a crook, stacked a block at a time like the Lamp Posts (foot, pole, head: each block takes
 * its part from the lamps above and below it). The head lights while its district is fed ({@link DerelictLightBlockEntity}),
 * as bright as an Electric Light; a {@link #SNAPPED} head (the crook bent down, the lens cracked) never does. Not
 * dyeable, no brightness steps; it breaks into scrap (its loot table), never into an Electric Light. The crook reaches
 * over the street toward the facing. A {@link #LEANING} post (D41; the city sets it, and a block stacked on one leans
 * with it) leans 22.5 degrees away from its crook, about its foot: each block draws its slice of the leaning post, so
 * the three line up into one.
 */
public class DerelictStreetLampBlock extends HorizontalDirectionalBlock implements EntityBlock, DerelictLightBlockEntity.DerelictLight {
	public static final MapCodec<DerelictStreetLampBlock> CODEC = simpleCodec(DerelictStreetLampBlock::new);
	public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
	public static final BooleanProperty SNAPPED = BooleanProperty.create("snapped");
	/** The post leans 22.5 degrees away from its crook, about its foot (D41). */
	public static final BooleanProperty LEANING = BooleanProperty.create("leaning");
	/** How far it leans (the preview's; the most a block model turns). */
	public static final double LEAN_DEGREES = 22.5;
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	private static final Map<Part, Map<Direction, VoxelShape>> SHAPES = new EnumMap<>(Part.class), LEANING_SHAPES = new EnumMap<>(Part.class);
	private static final Map<Direction, VoxelShape> SNAPPED_HEAD = SalvageBlock.shapes(new double[][] { { 5.5, 0, 5.5, 10.5, 15, 10.5 } });

	static {
		SHAPES.put(Part.FOOT, SalvageBlock.shapes(new double[][] { { 4, 0, 4, 12, 7, 12 }, { 6, 7, 6, 10, 16, 10 } }));
		SHAPES.put(Part.POLE, SalvageBlock.shapes(new double[][] { { 6, 0, 6, 10, 16, 10 } }));
		SHAPES.put(Part.HEAD, SalvageBlock.shapes(new double[][] { { 5.5, 0, 5.5, 10.5, 15, 10.5 }, { 5, 8, 0, 11, 16, 7 } }));
		for (Part part : Part.values())
			LEANING_SHAPES.put(part, SalvageBlock.shapes(leaningShaft(part.ordinal())));
	}

	/**
	 * A leaning post's shape in block k of it (facing north, leaning south): its 4 px shaft followed up in 4 px steps, each
	 * moved as far south as the lean carries it at that height, so the outline follows the post out over the next block
	 * south as its model does (the head's slice is all out there); the plinth and the hood are left to the shaft.
	 */
	private static double[][] leaningShaft(int k) {
		double tan = Math.tan(Math.toRadians(LEAN_DEGREES));
		java.util.List<double[]> boxes = new java.util.ArrayList<>();
		if (k == 0)
			boxes.add(new double[] { 4, 0, 4, 12, 2, 12 });
		for (int y = 0; y < 16; y += 4) {
			double dz = tan * (16 * k + y + 2);
			boxes.add(new double[] { 6, y, 6 + dz, 10, y + 4, 10 + dz });
		}
		return boxes.toArray(double[][]::new);
	}

	public DerelictStreetLampBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(PART, Part.HEAD).setValue(SNAPPED, false).setValue(LEANING, false)
			.setValue(LIT, false));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, PART, SNAPPED, LEANING, LIT);
	}

	@Override
	public boolean lightable(BlockState state) {
		return state.getValue(PART) == Part.HEAD && !state.getValue(SNAPPED);
	}

	/** As bright as a lit Electric Light. */
	public static int lightLevel(BlockState state) {
		return state.getValue(LIT) && state.getValue(PART) == Part.HEAD ? 15 : 0;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState below = context.getLevel().getBlockState(context.getClickedPos().below());
		Direction facing = below.is(this) ? below.getValue(FACING) : context.getHorizontalDirection().getOpposite();
		boolean leaning = below.is(this) && below.getValue(LEANING);
		return withPart(defaultBlockState().setValue(FACING, facing).setValue(LEANING, leaning), context.getLevel(), context.getClickedPos());
	}

	private BlockState withPart(BlockState state, LevelAccessor level, BlockPos pos) {
		boolean below = level.getBlockState(pos.below()).is(this), above = level.getBlockState(pos.above()).is(this);
		Part part = above ? (below ? Part.POLE : Part.FOOT) : Part.HEAD;
		BlockState out = state.setValue(PART, part);
		// only a head shines (a head that's now a pole goes dark)
		return part == Part.HEAD ? out : out.setValue(LIT, false);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		if (direction.getAxis() != Direction.Axis.Y)
			return state;
		BlockState out = withPart(state, level, pos);
		if (out != state)
			level.scheduleTick(pos, this, 1);
		return out;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Part part = state.getValue(PART);
		if (state.getValue(LEANING))
			return LEANING_SHAPES.get(part).get(state.getValue(FACING));
		return (part == Part.HEAD && state.getValue(SNAPPED) ? SNAPPED_HEAD : SHAPES.get(part)).get(state.getValue(FACING));
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
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	public enum Part implements StringRepresentable {
		FOOT, POLE, HEAD;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}
}
