package com.comedymonkey.createaddons.content.wastes;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Key-Marked Stone (D73, D75): the clockwork resistance's winding key scratched into a Scorched End Stone cover stone, on
 * its {@code facing} face, one per siege segment. Never lit.
 */
public class KeyMarkedStoneBlock extends HorizontalDirectionalBlock {
	public static final MapCodec<KeyMarkedStoneBlock> CODEC = simpleCodec(KeyMarkedStoneBlock::new);

	public KeyMarkedStoneBlock(Properties properties) {
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

	/** Its key toward the player. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}
}
