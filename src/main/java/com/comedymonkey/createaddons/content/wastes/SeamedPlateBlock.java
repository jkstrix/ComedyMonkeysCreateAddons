package com.comedymonkey.createaddons.content.wastes;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The Seamed Void-Alloy Plate (D37, brief §1): a decor block that drops itself, whose seams light with starlight while
 * its district is fed, as the city's lamps do ({@link DerelictLightBlockEntity}: the lit seams are an emissive overlay
 * in its block model, so a wall of them costs nothing to draw). One a player places belongs to no city and stays dark.
 * It casts no light.
 */
public class SeamedPlateBlock extends Block implements EntityBlock, DerelictLightBlockEntity.DerelictLight {
	public static final MapCodec<SeamedPlateBlock> CODEC = simpleCodec(SeamedPlateBlock::new);
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public SeamedPlateBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(LIT, false));
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	public boolean lightable(BlockState state) {
		return true;
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockEntity(pos) instanceof DerelictLightBlockEntity be)
			be.scheduledTick();
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DerelictLightBlockEntity(pos, state);
	}
}
