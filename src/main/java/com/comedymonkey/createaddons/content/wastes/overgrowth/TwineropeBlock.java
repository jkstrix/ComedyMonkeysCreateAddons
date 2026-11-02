package com.comedymonkey.createaddons.content.wastes.overgrowth;

import com.comedymonkey.createaddons.registry.ModWastes;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GrowingPlantBodyBlock;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Twinerope (D92): a hanging rope of two strands, as weeping vines hang, its tip ending in a hook and a seed pod. Climbable
 * (#minecraft:climbable). It never grows by itself; bone meal lengthens it by a block or two.
 */
public final class TwineropeBlock extends GrowingPlantHeadBlock {
	public static final MapCodec<TwineropeBlock> CODEC = simpleCodec(TwineropeBlock::new);
	static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);

	public TwineropeBlock(Properties properties) {
		super(properties, Direction.DOWN, SHAPE, false, 0);
	}

	@Override
	protected MapCodec<TwineropeBlock> codec() {
		return CODEC;
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return false;
	}

	@Override
	protected int getBlocksToGrowWhenBonemealed(RandomSource random) {
		return 1 + random.nextInt(2);
	}

	@Override
	protected boolean canGrowInto(BlockState state) {
		return state.isAir();
	}

	@Override
	protected Block getBodyBlock() {
		return ModWastes.TWINEROPE_PLANT.get();
	}

	/** A length of the rope above its tip. */
	public static final class Plant extends GrowingPlantBodyBlock {
		public static final MapCodec<Plant> CODEC = simpleCodec(Plant::new);

		public Plant(Properties properties) {
			super(properties, Direction.DOWN, SHAPE, false);
		}

		@Override
		protected MapCodec<Plant> codec() {
			return CODEC;
		}

		@Override
		protected GrowingPlantHeadBlock getHeadBlock() {
			return ModWastes.TWINEROPE.get();
		}
	}
}
