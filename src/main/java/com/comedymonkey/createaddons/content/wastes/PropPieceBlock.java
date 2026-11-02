package com.comedymonkey.createaddons.content.wastes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A piece of a multi-block prop (the winding tower's Sprung Mainspring and Fallen Rafters, D80-D87, {@link WindingTowerPieces}):
 * a block whose model reaches into the blocks round its own (a strip's turns, a beam, running on through them). Its
 * {@linkplain #reach reach} is the cells its model runs through; the city counts it standing in all of them (a prop that
 * stands on the ground at one cell holds all its pieces: the kit's ModelChecks). No collision (its blocks are mostly air
 * round a thin strip); its outline is the part of it in its own block. Decor: it drops itself (D37). Faces the player who
 * places it; turned with its facing, never mirrored.
 */
public final class PropPieceBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<PropPieceBlock> CODEC = simpleCodec(PropPieceBlock::new);

	private final Map<Direction, VoxelShape> shapes;
	/** The cells its model runs through, from its own block, facing north: dx, dy, dz triples. */
	private final int[] reach;

	public PropPieceBlock(Properties properties) {
		this(properties, new double[][] { { 0, 0, 0, 16, 16, 16 } }, new int[] { 0, 0, 0 });
	}

	/**
	 * @param northBoxes its outline facing north, in pixels: {x0, y0, z0, x1, y1, z1} each
	 * @param reach the cells its model runs through, from its own block, facing north: dx, dy, dz triples
	 */
	public PropPieceBlock(Properties properties, double[][] northBoxes, int[] reach) {
		super(properties);
		this.shapes = SalvageBlock.shapes(northBoxes);
		this.reach = reach.clone();
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	/** The cells a piece's model runs through, for the piece at {@code pos} as {@code state} faces (its own block among them). */
	public static List<BlockPos> reach(BlockPos pos, BlockState state) {
		List<BlockPos> out = new ArrayList<>();
		out.add(pos);
		if (!(state.getBlock() instanceof PropPieceBlock piece))
			return out;
		Direction facing = state.getValue(FACING);
		for (int i = 0; i + 2 < piece.reach.length; i += 3) {
			int dx = piece.reach[i], dz = piece.reach[i + 2];
			// turned about the block's middle as the blockstate turns the model (y 90 a step, north to east)
			int x = switch (facing) {
				case EAST -> -dz;
				case SOUTH -> -dx;
				case WEST -> dz;
				default -> dx;
			};
			int z = switch (facing) {
				case EAST -> dx;
				case SOUTH -> -dz;
				case WEST -> -dx;
				default -> dz;
			};
			out.add(pos.offset(x, piece.reach[i + 1], z));
		}
		return out;
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	/**
	 * Kept as it faces, never spun half round (as {@link HorizontalDirectionalBlock}'s mirror does along its axis). A
	 * mirrored prop can't be exact: there are no mirrored models, so its pieces move to their mirrored cells but each still
	 * faces as before, and pieces whose models run into each other may no longer line up.
	 */
	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapes.get(state.getValue(FACING));
	}
}
