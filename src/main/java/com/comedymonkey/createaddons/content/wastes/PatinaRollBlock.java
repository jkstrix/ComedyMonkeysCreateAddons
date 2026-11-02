package com.comedymonkey.createaddons.content.wastes;

import java.util.EnumMap;
import java.util.Map;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Patina Ridge Roll (D83, D85: option C of design/models/clockwork-wastes-ridge.gen.js): the makers' copper ridge roll at
 * half the weight of the Patina Rib, in its aged patina. Each block is one half of a 2-wide ridge: a stepped quarter-round
 * (a 4 px base the full width, a 4 px step set back 4, a 2 px top at its {@code facing} edge, 10 px high there), so two
 * halves faced toward each other meet as one round cap along the ridge. Never lit.
 */
public final class PatinaRollBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<PatinaRollBlock> CODEC = simpleCodec(PatinaRollBlock::new);
	private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

	static {
		// drawn facing south (its high edge at +z), turned for the others
		for (Direction d : Direction.Plane.HORIZONTAL)
			SHAPES.put(d, Shapes.or(box(d, 0, 0, 16, 4, 0, 16), box(d, 0, 4, 16, 8, 4, 16), box(d, 0, 8, 16, 10, 10, 16)));
	}

	/** A box drawn facing south, turned to face {@code d}. */
	private static VoxelShape box(Direction d, double x0, double y0, double x1, double y1, double z0, double z1) {
		return switch (d) {
			case NORTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
			case EAST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
			case WEST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
			default -> Block.box(x0, y0, z0, x1, y1, z1);
		};
	}

	public PatinaRollBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	/** Its high edge away from the player (placed from the eaves, it rises toward the ridge line the player looks at). */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(FACING));
	}

	@Override
	protected boolean useShapeForLightOcclusion(BlockState state) {
		return true;
	}
}
