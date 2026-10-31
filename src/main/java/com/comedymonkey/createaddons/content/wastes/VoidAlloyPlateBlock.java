package com.comedymonkey.createaddons.content.wastes;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** The Void-Alloy Plate (D37), its faces in the void-alloy bond (D75, {@link VoidAlloyBond}). Decor: drops itself. */
public class VoidAlloyPlateBlock extends Block {
	public static final MapCodec<VoidAlloyPlateBlock> CODEC = simpleCodec(VoidAlloyPlateBlock::new);

	public VoidAlloyPlateBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends Block> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(VoidAlloyBond.NS, VoidAlloyBond.EW);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return VoidAlloyBond.at(defaultBlockState(), context.getClickedPos());
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		VoidAlloyBond.follow(level, pos, state);
	}
}
