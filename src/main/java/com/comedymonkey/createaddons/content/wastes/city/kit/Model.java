package com.comedymonkey.createaddons.content.wastes.city.kit;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One kit building drawn in its local frame ({@link Site}) before it's put into the plan: every block it puts, the city
 * pieces placed whole (doors, alcoves, cables, insulators, the lift: {@link #special}), and the causes its marks have
 * (design/briefs/clockwork-wastes-buildings.md: "each variant has a fixed place or a cause, never a dice roll on an open
 * wall"). The builder draws the intact building, the damage passes take from and add to it, and {@link #emit} puts it into
 * the world through its site, so the plan tests can read the building as it was drawn.
 */
public final class Model {
	/** What a mark is there for: the cracked, spalled and soot blocks each lie within their band of one of these. */
	public enum Cause {
		/** A Clamp Bracket's jaws: the stone under them is the cracked variant (the bite). r 1. */
		CLAMP,
		/** A lined cut's edge: the stones where the cut broke the bond. r 1. */
		CUT,
		/** A war bolt or shell from outside: a hole, a cracked ring, a soot fan above it. */
		STRIKE,
		/** The top of a broken wall or a snapped tower (the break line). r 1. */
		BREAK,
		/** A window or door fire vented from: a soot fan above it. */
		FIRE,
		/** A chimney's or flue's top (smoke, not war): soot in its top courses. */
		CHIMNEY,
		/** A furnace mouth or tap hole (heat). */
		FURNACE,
		/** Arcing contacts (the yard's transformer bushings). */
		ARC,
		/** Where a fallen block landed and cracked what it hit. r 1. */
		FALL
	}

	/** A cause at local (x, y, z), reaching {@code reach} blocks (a soot fan's height, a crack ring's radius). */
	public record Mark(Cause cause, int x, int y, int z, int reach) {}

	private final LinkedHashMap<Long, BlockState> cells = new LinkedHashMap<>();
	/** Cells a special holds (their states for reading; the special puts them). */
	private final Map<Long, BlockState> specialCells = new LinkedHashMap<>();
	private final List<Consumer<Site>> specials = new ArrayList<>();
	public final List<Mark> marks = new ArrayList<>();
	/** Strikes and other war marks the damage passes made (counted against the ring's allowance). */
	public int strikes;
	/** The faces (local) that carry a chipped or other scar, one a face at most in the worn ring. */
	public final Set<Direction> scarredFaces = new HashSet<>();

	public static long key(int x, int y, int z) {
		return BlockPos.asLong(x, y, z);
	}

	public void set(int x, int y, int z, BlockState s) {
		long k = key(x, y, z);
		cells.remove(k);
		if (s.isAir())
			return;
		cells.put(k, s);
	}

	@Nullable
	public BlockState get(int x, int y, int z) {
		long k = key(x, y, z);
		BlockState s = cells.get(k);
		return s != null ? s : specialCells.get(k);
	}

	public boolean has(int x, int y, int z) {
		return get(x, y, z) != null;
	}

	public void remove(int x, int y, int z) {
		cells.remove(key(x, y, z));
	}

	/** Replaces the block at a cell if there's one (and it isn't a special's). */
	public void replace(int x, int y, int z, BlockState s) {
		if (cells.containsKey(key(x, y, z)))
			set(x, y, z, s);
	}

	public void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
		for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
			for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
				for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
					set(x, y, z, s);
	}

	public void clear(int x0, int y0, int z0, int x1, int y1, int z1) {
		for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
			for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
				for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
					remove(x, y, z);
	}

	/** Only where nothing is yet. */
	public void setIfEmpty(int x, int y, int z, BlockState s) {
		if (!has(x, y, z))
			set(x, y, z, s);
	}

	public void mark(Cause c, int x, int y, int z, int reach) {
		marks.add(new Mark(c, x, y, z, reach));
	}

	/**
	 * A city piece put whole through the site (a door, an alcove, a cable, an insulator, a lift), holding these local
	 * cells (their states as drawn locally, for reading).
	 */
	public void special(Map<BlockPos, BlockState> holds, Consumer<Site> put) {
		for (var e : holds.entrySet()) {
			long k = key(e.getKey().getX(), e.getKey().getY(), e.getKey().getZ());
			cells.remove(k);
			specialCells.put(k, e.getValue());
		}
		specials.add(put);
	}

	/** Every block and special cell, local position to state (a copy). */
	public Map<BlockPos, BlockState> blocks() {
		Map<BlockPos, BlockState> out = new LinkedHashMap<>();
		cells.forEach((k, s) -> out.put(BlockPos.of(k), s));
		specialCells.forEach((k, s) -> out.put(BlockPos.of(k), s));
		return out;
	}

	public int size() {
		return cells.size() + specialCells.size();
	}

	/** The model's bounds (min x, y, z, max x, y, z), specials included; all zero if empty. */
	public int[] bounds() {
		int[] b = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE };
		for (BlockPos p : blocks().keySet()) {
			b[0] = Math.min(b[0], p.getX());
			b[1] = Math.min(b[1], p.getY());
			b[2] = Math.min(b[2], p.getZ());
			b[3] = Math.max(b[3], p.getX());
			b[4] = Math.max(b[4], p.getY());
			b[5] = Math.max(b[5], p.getZ());
		}
		return b[0] == Integer.MAX_VALUE ? new int[6] : b;
	}

	/**
	 * Takes away every block no longer held up (not joined, face to face, through other blocks to one standing on the ground
	 * at y 0 or below): what a collapse leaves hanging falls. Returns how many fell. Specials hold and are held like blocks.
	 */
	public int settle() {
		Set<Long> held = held();
		List<Long> fell = new ArrayList<>();
		for (long k : cells.keySet())
			if (!held.contains(k))
				fell.add(k);
		fell.forEach(cells::remove);
		return fell.size();
	}

	/** The cells joined to the ground (y 0 or below) through other cells, face to face. */
	public Set<Long> held() {
		Set<Long> all = new HashSet<>(cells.keySet());
		all.addAll(specialCells.keySet());
		Set<Long> held = new HashSet<>();
		ArrayDeque<Long> todo = new ArrayDeque<>();
		for (long k : all)
			if (BlockPos.getY(k) <= 0) {
				held.add(k);
				todo.add(k);
			}
		while (!todo.isEmpty()) {
			long k = todo.poll();
			int x = BlockPos.getX(k), y = BlockPos.getY(k), z = BlockPos.getZ(k);
			for (Direction d : Direction.values()) {
				long n = key(x + d.getStepX(), y + d.getStepY(), z + d.getStepZ());
				if (all.contains(n) && held.add(n))
					todo.add(n);
			}
		}
		return held;
	}

	/** Copies another model's blocks in, shifted (it must have no city pieces of its own). */
	public void copyFrom(Model o, int dx, int dy, int dz) {
		if (!o.specials.isEmpty())
			throw new IllegalArgumentException("a model with city pieces can't be copied");
		o.cells.forEach((k, s) -> set(BlockPos.getX(k) + dx, BlockPos.getY(k) + dy, BlockPos.getZ(k) + dz, s));
		for (Mark m : o.marks)
			marks.add(new Mark(m.cause(), m.x() + dx, m.y() + dy, m.z() + dz, m.reach()));
		strikes += o.strikes;
		scarredFaces.addAll(o.scarredFaces);
	}

	/** Puts the building into the plan through its site: its blocks in the order drawn, then its city pieces. */
	public void emit(Site site) {
		cells.forEach((k, s) -> site.put(BlockPos.getX(k), BlockPos.getY(k), BlockPos.getZ(k), s));
		for (Consumer<Site> sp : specials)
			sp.accept(site);
	}
}
