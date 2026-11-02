package com.comedymonkey.createaddons.content.wastes.overgrowth;

import com.comedymonkey.createaddons.registry.ModWastes;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Dusk Moss (D92): blue cushion moss. It never spreads by itself; bone meal spreads it to stone round it, as vanilla's moss
 * does, and now and then sets a carpet or a crack plant on it.
 */
public final class DuskMossBlock extends Block implements BonemealableBlock {
	public static final MapCodec<DuskMossBlock> CODEC = simpleCodec(DuskMossBlock::new);

	public DuskMossBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<DuskMossBlock> codec() {
		return CODEC;
	}

	/** Stone the moss can take over: End Stone, the Wastes' scorched ground, the Overworld's stone. */
	public static boolean takes(BlockState s) {
		return s.is(Blocks.END_STONE) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(ModWastes.SCORCHED_END_STONE.get())
			|| s.is(ModWastes.RUSTED_SCORCHED_END_STONE.get()) || s.is(ModWastes.LITTERED_SCORCHED_END_STONE.get());
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
		return level.getBlockState(pos.above()).isAir();
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
		for (int i = 0; i < 12; i++) {
			BlockPos p = pos.offset(random.nextInt(7) - 3, random.nextInt(3) - 1, random.nextInt(7) - 3);
			BlockState s = level.getBlockState(p);
			if (takes(s) && level.getBlockState(p.above()).isAir())
				level.setBlock(p, defaultBlockState(), UPDATE_ALL);
			else if (s.is(this) && level.getBlockState(p.above()).isAir() && random.nextInt(4) == 0) {
				Block on = random.nextInt(3) == 0 ? ModWastes.CORKSCREW_RUSH.get() : ModWastes.DUSK_MOSS_CARPET.get();
				BlockState plant = on.defaultBlockState();
				if (plant.canSurvive(level, p.above()))
					level.setBlock(p.above(), Block.updateFromNeighbourShapes(plant, level, p.above()), UPDATE_ALL);
			}
		}
	}
}
