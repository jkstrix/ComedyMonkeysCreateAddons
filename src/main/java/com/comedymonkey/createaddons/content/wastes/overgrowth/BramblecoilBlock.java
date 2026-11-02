package com.comedymonkey.createaddons.content.wastes.overgrowth;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Bramblecoil (D92): a bush of coiled thorny stems, small or large; it slows what pushes through it but never hurts (G10). */
public final class BramblecoilBlock extends OvergrowthPlantBlock {
	public static final MapCodec<BramblecoilBlock> CODEC = simpleCodec(BramblecoilBlock::new);
	public static final BooleanProperty LARGE = BooleanProperty.create("large");
	private static final VoxelShape SMALL = Block.box(2, 0, 2, 14, 11, 14);

	public BramblecoilBlock(Properties properties) {
		super(properties, Block.box(1, 0, 1, 15, 15, 15));
		registerDefaultState(defaultBlockState().setValue(LARGE, false));
	}

	@Override
	protected MapCodec<BramblecoilBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LARGE);
	}

	@Override
	protected VoxelShape shape(BlockState state) {
		return state.getValue(LARGE) ? super.shape(state) : SMALL;
	}

	@Override
	@Nullable
	protected BlockState grown(BlockState state) {
		return state.getValue(LARGE) ? null : state.setValue(LARGE, true);
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
		if (entity instanceof LivingEntity)
			entity.makeStuckInBlock(state, new Vec3(0.8, 0.75, 0.8));
	}
}
