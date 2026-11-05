package com.comedymonkey.createaddons.content.line;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Blocks a Schematicannon printed this tick (see SchematicPlacementMixin): it tells no neighbours, so the wires round
 * each work their links out again at the end of the tick, each wire once however many blocks landed beside it (a
 * creative instant print sets a whole schematic down in one tick).
 */
public final class PrintedRelinks {
	private static final Map<ResourceKey<Level>, Set<BlockPos>> PRINTED = new HashMap<>();

	private PrintedRelinks() {}

	public static void printed(Level level, BlockPos pos) {
		if (level instanceof ServerLevel)
			PRINTED.computeIfAbsent(level.dimension(), k -> new LinkedHashSet<>()).add(pos.immutable());
	}

	/** The end of a level's tick: the wires round what was printed in it link up (as PowerLineBlock.refreshAround does). */
	public static void onLevelTick(LevelTickEvent.Post event) {
		if (!(event.getLevel() instanceof ServerLevel level))
			return;
		Set<BlockPos> printed = PRINTED.remove(level.dimension());
		if (printed == null)
			return;
		Set<BlockPos> relink = new LinkedHashSet<>(), beside = new LinkedHashSet<>();
		for (BlockPos pos : printed) {
			for (int dx = -1; dx <= 1; dx++)
				for (int dy = -1; dy <= 1; dy++)
					for (int dz = -1; dz <= 1; dz++)
						relink.add(pos.offset(dx, dy, dz));
			for (Direction d : Direction.values()) {
				relink.add(pos.relative(d, 2));
				beside.add(pos.relative(d));
			}
		}
		for (BlockPos p : relink)
			if (level.isLoaded(p) && level.getBlockEntity(p) instanceof PowerLineBlockEntity be)
				be.relink();
		for (BlockPos p : beside)
			if (level.isLoaded(p) && level.getBlockEntity(p) instanceof PowerLineBlockEntity be)
				be.devicesChanged();
	}

	/** The server stopped: nothing is carried over to the next world. */
	public static void clear() {
		PRINTED.clear();
	}
}
