package com.comedymonkey.createaddons.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;

/**
 * Whether a block's chunk is loaded, asked without loading it. The game marks every form of this check on a
 * {@link LevelReader} deprecated, as a "use with care" (asking about an unloaded chunk the wrong way loads it), so the
 * mod asks it here, in one place, the safe way.
 */
public final class Loaded {
	private Loaded() {}

	/** The chunk holding {@code pos} is loaded. */
	@SuppressWarnings("deprecation") // the game's own chunk check: no undeprecated form on a LevelReader
	public static boolean at(LevelReader level, BlockPos pos) {
		return level.hasChunkAt(pos);
	}

	/** Every chunk of the box between {@code a} and {@code b} (corners in any order) is loaded, and the box is within the build height. */
	@SuppressWarnings("deprecation") // as above
	public static boolean between(LevelReader level, BlockPos a, BlockPos b) {
		// the game's check assumes its corners in order
		return level.hasChunksAt(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
			Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
	}
}
