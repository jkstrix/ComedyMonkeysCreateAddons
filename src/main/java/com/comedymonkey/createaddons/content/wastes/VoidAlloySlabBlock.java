package com.comedymonkey.createaddons.content.wastes;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * The Void-Alloy Slab (D75): a vanilla slab on the plain plate; doubled it's the Void-Alloy Plate's look, so it keeps
 * the bond ({@link VoidAlloyBond}) for its double.
 */
public class VoidAlloySlabBlock extends SlabBlock {
	public static final MapCodec<VoidAlloySlabBlock> CODEC = simpleCodec(VoidAlloySlabBlock::new);

	public VoidAlloySlabBlock(Properties properties) {
		super(properties);
	}

	@Override
	public MapCodec<? extends SlabBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(VoidAlloyBond.NS, VoidAlloyBond.EW);
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		return state == null ? null : VoidAlloyBond.at(state, context.getClickedPos());
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		VoidAlloyBond.follow(level, pos, state);
	}
}
