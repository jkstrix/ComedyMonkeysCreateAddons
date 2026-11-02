package com.comedymonkey.createaddons.content.wastes.overgrowth;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Shieldcrown Leaves (D92): deep blue small leaves; {@link #RIM} gives a crown's edge block its lit top edge. */
public final class ShieldcrownLeavesBlock extends LeavesBlock {
	public static final MapCodec<ShieldcrownLeavesBlock> CODEC = simpleCodec(ShieldcrownLeavesBlock::new);
	public static final BooleanProperty RIM = BooleanProperty.create("rim");

	public ShieldcrownLeavesBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(RIM, false));
	}

	@Override
	public MapCodec<ShieldcrownLeavesBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(RIM);
	}
}
