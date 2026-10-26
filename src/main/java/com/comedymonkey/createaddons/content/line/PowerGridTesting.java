package com.comedymonkey.createaddons.content.line;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

/** Game tests only: hooks into the Power Lines' grid that a test can't reach from outside (no chunk unloading in a test). */
public final class PowerGridTesting {
	private PowerGridTesting() {}

	/**
	 * Treats the generator at key as away (as if its chunk weren't running: it doesn't run, its line runs it on paper), or
	 * as back. A mark lasts at most {@code ticks}, so a test that fails half way leaves nothing behind for long.
	 */
	public static void away(ServerLevel level, BlockPos key, boolean away, int ticks) {
		if (away)
			PowerGrid.AWAY.put(key.immutable(), level.getGameTime() + ticks);
		else
			PowerGrid.AWAY.remove(key);
	}

	/** The FE in the record of the generator at key, or -1 if it has none. */
	public static double store(ServerLevel level, BlockPos key) {
		PowerGrid.Paper p = PowerGrid.of(level).paper(key);
		return p == null ? -1 : p.store;
	}

	/** Draws a little from the record of the generator at key, with the grid marked clean first: is it marked to be saved? */
	public static boolean drawMarksDirty(ServerLevel level, BlockPos key) {
		PowerGrid grid = PowerGrid.of(level);
		PowerGrid.Paper p = grid.paper(key);
		if (p == null)
			return false;
		grid.records().setDirty(false);
		return grid.draw(p, 100, level.getGameTime(), false, level) > 0 && grid.records().isDirty();
	}

	/** The FE in the record of the generator at key once the grid has been saved and loaded back. */
	public static double storeAfterReload(ServerLevel level, BlockPos key) {
		PowerGrid.Records records = PowerGrid.of(level).records();
		PowerGrid.Records copy = PowerGrid.Records.load(records.save(new CompoundTag(), level.registryAccess()), level.registryAccess());
		PowerGrid.Paper p = copy.generators.get(key);
		return p == null ? -1 : p.store;
	}

	/** The FE in the record of the shared store keyed key (a Battery bank, an Industrial Battery), or -1 if it has none. */
	public static double storeRecord(ServerLevel level, BlockPos key) {
		PowerGrid.StorePaper p = PowerGrid.of(level).storePaper(key);
		return p == null ? -1 : p.energy();
	}

	/** The same once the grid has been saved and loaded back. */
	public static double storeRecordAfterReload(ServerLevel level, BlockPos key) {
		PowerGrid.Records records = PowerGrid.of(level).records();
		PowerGrid.Records copy = PowerGrid.Records.load(records.save(new CompoundTag(), level.registryAccess()), level.registryAccess());
		PowerGrid.StorePaper p = copy.stores.get(key);
		return p == null ? -1 : p.energy();
	}

	/**
	 * A wire as a save from before lines had colours left it: no colour on it or its links, waiting to be read again
	 * when its chunk next loads. Its network is worked out again with it like that.
	 */
	public static void asOldSave(ServerLevel level, BlockPos pos) {
		PowerGrid grid = PowerGrid.of(level);
		PowerGrid.Node node = new PowerGrid.Node(pos.immutable(), PowerGrid.Node.WIRE);
		PowerGrid.Piece p = grid.piece(node);
		if (p == null)
			return;
		p.dye = null;
		p.stale = true;
		java.util.List<PowerGrid.Link> links = new java.util.ArrayList<>(p.links);
		p.links.clear();
		for (PowerGrid.Link l : links)
			p.links.add(new PowerGrid.Link(l.to(), l.tier(), null, l.via()));
		PowerLines.of(level).rebuildAt(node);
	}
}
