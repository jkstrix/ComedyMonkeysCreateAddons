package com.comedymonkey.createaddons.content.wastes;

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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Brass Plaque (D72; world brief: the makers' only writing, worn smooth and unreadable): a thin plate of old brass on a
 * wall, a raised rim and three worn raised bars, three steel bolts with the fourth lost; {@link #ASKEW}, it hangs from its
 * last bolt. {@code facing} is the way its face looks, its wall behind it. Decor: it drops itself. It needs no wall to
 * stay up (provisional, for the user's word: progress doc, "D72").
 */
public class BrassPlaqueBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<BrassPlaqueBlock> CODEC = simpleCodec(BrassPlaqueBlock::new);
	public static final BooleanProperty ASKEW = BooleanProperty.create("askew");
	/** Its outline facing north, in pixels (the exporter's bounds: square, and hanging askew, each kept to the block). */
	private static final Map<Direction, VoxelShape> SQUARE = SalvageBlock.shapes(new double[][] { { 2, 3, 14.25, 14, 12, 16 } }),
		HANGING = SalvageBlock.shapes(new double[][] { { 0, 0, 14.25, 13.5, 12.25, 16 } });

	public BrassPlaqueBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(ASKEW, false));
	}

	@Override
	protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, ASKEW);
	}

	/** On the side of the block clicked, facing out from it; on a floor or ceiling, facing the player. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		return defaultBlockState().setValue(FACING, face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return (state.getValue(ASKEW) ? HANGING : SQUARE).get(state.getValue(FACING));
	}
}
