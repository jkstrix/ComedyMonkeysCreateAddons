package com.comedymonkey.createaddons.content.chest;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** A block that holds a chest upgrade: upgraded chests (single or double) and upgraded barrels. */
public interface TieredStorageBlock {
	ChestTier tierOf(BlockState state);

	/** The positions sharing the one upgrade: just {@code pos}, or both halves of a double chest. */
	List<BlockPos> parts(Level level, BlockPos pos, BlockState state);

	/** Changes the tier of every part. Callers check the contents fit first. */
	void setTier(Level level, BlockPos pos, ChestTier tier);

	/** Turns every part back into its vanilla block, keeping the contents. Callers check they fit first. */
	void revertToVanilla(Level level, BlockPos pos);
}
