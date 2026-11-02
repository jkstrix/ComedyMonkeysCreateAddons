package com.comedymonkey.createaddons.content.wastes.overgrowth;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Corkscrew Rush and the Vigil Bell (D92): small plants in the cracks; bone meal sets another nearby. */
public final class CrackPlantBlock extends OvergrowthPlantBlock {
	public static final MapCodec<CrackPlantBlock> CODEC = simpleCodec(CrackPlantBlock::new);

	public CrackPlantBlock(Properties properties) {
		super(properties, Block.box(3, 0, 3, 13, 13, 13));
	}

	@Override
	protected MapCodec<CrackPlantBlock> codec() {
		return CODEC;
	}

	@Override
	@Nullable
	protected BlockState grown(BlockState state) {
		return null;
	}
}
