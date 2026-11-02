package com.comedymonkey.createaddons.content.wastes.overgrowth;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A plant of the overgrowth that stands on a block (D92): on any sturdy up-face, since they grow in the cracks. They never
 * spread or grow by themselves; bone meal grows one ({@link #grown}) or, once grown, sets a young one on a spot nearby.
 */
public abstract class OvergrowthPlantBlock extends BushBlock implements BonemealableBlock {
	/** How far (x and z) bone meal sets a young one. */
	private static final int SPREAD = 2;
	private final VoxelShape shape;

	protected OvergrowthPlantBlock(Properties properties, VoxelShape shape) {
		super(properties);
		this.shape = shape;
	}

	@Override
	protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
		return state.isFaceSturdy(level, pos, Direction.UP);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		VoxelShape s = shape(state);
		Vec3 off = state.getOffset(level, pos);
		return off == Vec3.ZERO ? s : s.move(off.x, off.y, off.z);
	}

	/** Its outline in this state, before the random offset. */
	protected VoxelShape shape(BlockState state) {
		return shape;
	}

	/** The state bone meal grows it to, or null once it's grown (bone meal then sets a young one beside it). */
	@Nullable
	protected abstract BlockState grown(BlockState state);

	/** Bone meal takes only when it can grow, or there's a spot nearby a young one could stand on. */
	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
		return grown(state) != null || hasRoom(level, pos);
	}

	/** Whether a spot within reach of {@link #performBonemeal}'s scatter is empty and holds a young one. */
	private boolean hasRoom(LevelReader level, BlockPos pos) {
		BlockState young = defaultBlockState();
		for (BlockPos p : BlockPos.betweenClosed(pos.offset(-SPREAD, -1, -SPREAD), pos.offset(SPREAD, 1, SPREAD)))
			if (level.isEmptyBlock(p) && young.canSurvive(level, p))
				return true;
		return false;
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
		BlockState next = grown(state);
		if (next != null) {
			level.setBlock(pos, next, UPDATE_ALL);
			return;
		}
		BlockState young = defaultBlockState();
		for (int i = 0; i < 8; i++) {
			BlockPos p = pos.offset(random.nextInt(2 * SPREAD + 1) - SPREAD, random.nextInt(3) - 1, random.nextInt(2 * SPREAD + 1) - SPREAD);
			if (level.isEmptyBlock(p) && young.canSurvive(level, p)) {
				level.setBlock(p, young, UPDATE_ALL);
				return;
			}
		}
	}
}
