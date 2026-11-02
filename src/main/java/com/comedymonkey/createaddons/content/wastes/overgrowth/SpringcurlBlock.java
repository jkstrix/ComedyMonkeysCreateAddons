package com.comedymonkey.createaddons.content.wastes.overgrowth;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Springcurl (D92): a fern of coiled fronds in the cracks, young (all coils) or grown (fronds unrolled); bone meal grows it. */
public final class SpringcurlBlock extends OvergrowthPlantBlock {
	public static final MapCodec<SpringcurlBlock> CODEC = simpleCodec(SpringcurlBlock::new);
	public static final BooleanProperty GROWN = BooleanProperty.create("grown");
	private static final VoxelShape YOUNG = Block.box(3, 0, 3, 13, 9, 13);

	public SpringcurlBlock(Properties properties) {
		super(properties, Block.box(3, 0, 3, 13, 13, 13));
		registerDefaultState(defaultBlockState().setValue(GROWN, false));
	}

	@Override
	protected MapCodec<SpringcurlBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(GROWN);
	}

	@Override
	protected VoxelShape shape(BlockState state) {
		return state.getValue(GROWN) ? super.shape(state) : YOUNG;
	}

	@Override
	@Nullable
	protected BlockState grown(BlockState state) {
		return state.getValue(GROWN) ? null : state.setValue(GROWN, true);
	}
}
