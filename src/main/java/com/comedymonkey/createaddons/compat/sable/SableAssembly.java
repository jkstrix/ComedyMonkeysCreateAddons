package com.comedymonkey.createaddons.compat.sable;

import java.util.List;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Making blocks a Sable physics object, as Simulated's Physics Assembler does, for the game tests. Kept here so the
 * test classes name no Sable type: NeoForge loads every game test class whether or not Sable is installed.
 */
public final class SableAssembly {
	private SableAssembly() {}

	/** Assembles {@code blocks} around {@code anchor} into a new physics object; returns where the anchor went. */
	public static BlockPos assemble(ServerLevel level, BlockPos anchor, List<BlockPos> blocks) {
		ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, anchor, blocks, BoundingBox3i.from(blocks));
		return subLevel.getPlot().getCenterBlock();
	}
}
