package com.comedymonkey.createaddons.content.wastes;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The Scrap Heap: a half-block mound of bent iron, chain and cog teeth, the Wastes' outskirts filler (decor, D37). */
public class ScrapHeapBlock extends Block {
	public static final MapCodec<ScrapHeapBlock> CODEC = simpleCodec(ScrapHeapBlock::new);
	private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);

	public ScrapHeapBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}
}
