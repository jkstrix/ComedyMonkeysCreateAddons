package com.comedymonkey.createaddons.content;

import net.minecraft.core.BlockPos;

/**
 * When a machine's queued sync goes out: on one tick of every interval, picked by its position, so a wall of machines
 * doesn't sync all on the same tick (the Monster Cages, the Fish Tanks).
 */
public final class SyncStagger {
	private SyncStagger() {}

	/** The tick of each {@code interval} this position syncs on, 0 to interval - 1. */
	public static int phase(BlockPos pos, int interval) {
		return Math.floorMod(pos.hashCode(), interval);
	}

	/** Whether a queued sync with this phase goes out at {@code gameTime}. */
	public static boolean due(long gameTime, int phase, int interval) {
		return (gameTime + phase) % interval == 0;
	}
}
