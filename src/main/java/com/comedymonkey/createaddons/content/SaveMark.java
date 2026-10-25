package com.comedymonkey.createaddons.content;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Marks a block entity's chunk as needing a save, without the comparator update {@code setChanged()} also does. For
 * machines without a comparator output, whose tanks and energy stores change several times a tick: telling the
 * comparators around them every time would be for nothing. Marking is cheap, so it's done on every change.
 */
public final class SaveMark {
	private SaveMark() {}

	public static void mark(BlockEntity be) {
		Level level = be.getLevel();
		if (level != null)
			level.blockEntityChanged(be.getBlockPos());
	}
}
