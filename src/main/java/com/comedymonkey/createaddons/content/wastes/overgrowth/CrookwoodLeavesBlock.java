package com.comedymonkey.createaddons.content.wastes.overgrowth;

import java.util.Locale;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Crookwood Leaves (D92, D94): pale blue leaves; {@link #HANGING} makes one a see-through strand of the willow's curtain
 * (the middle of a strand or its end), which hangs from the leaves above it and has nothing to stand on.
 */
public final class CrookwoodLeavesBlock extends LeavesBlock {
	public static final MapCodec<CrookwoodLeavesBlock> CODEC = simpleCodec(CrookwoodLeavesBlock::new);
	public static final EnumProperty<Hanging> HANGING = EnumProperty.create("hanging", Hanging.class);
	private static final VoxelShape STRAND = Block.box(2, 0, 2, 14, 16, 14);

	public CrookwoodLeavesBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(HANGING, Hanging.NONE));
	}

	@Override
	public MapCodec<CrookwoodLeavesBlock> codec() {
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(HANGING);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(HANGING) == Hanging.NONE ? Shapes.block() : STRAND;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(HANGING) == Hanging.NONE ? Shapes.block() : Shapes.empty();
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return state.getValue(HANGING) == Hanging.NONE || level.getBlockState(pos.above()).is(this);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
		if (dir == Direction.UP && !state.canSurvive(level, pos))
			return Blocks.AIR.defaultBlockState();
		return super.updateShape(state, dir, neighbour, level, pos, neighbourPos);
	}

	/** A strand's part: none (an ordinary leaf block), the middle of a strand, its end. */
	public enum Hanging implements StringRepresentable {
		NONE, MID, END;

		@Override
		public String getSerializedName() {
			return name().toLowerCase(Locale.ROOT);
		}
	}
}
