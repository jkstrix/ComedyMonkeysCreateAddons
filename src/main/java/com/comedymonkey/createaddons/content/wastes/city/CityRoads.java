package com.comedymonkey.createaddons.content.wastes.city;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.comedymonkey.createaddons.content.wastes.DerelictCableBlock;
import com.comedymonkey.createaddons.content.wastes.DerelictStreetLampBlock;
import com.comedymonkey.createaddons.content.wastes.KeyMarkedStoneBlock;
import com.comedymonkey.createaddons.content.wastes.PropPieceBlock;
import com.comedymonkey.createaddons.content.wastes.SalvageBlock;
import com.comedymonkey.createaddons.content.wastes.ThreePartBlock;
import com.comedymonkey.createaddons.content.wastes.city.kit.Masonry;
import com.comedymonkey.createaddons.content.wastes.city.kit.Wear;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The roads' wear (the user, walking a city: "the roads should be more broken up"): cracked and missing setts, setts heaved
 * up or sunk, potholes down to the scorched ground, rubble spilling across, ruts along the boulevards. Heavier outward
 * through the wear rings ({@link Wear}) and round what broke them (strikes, craters and scorch fans, wrecks, the dead, the
 * barricades); tidy at the heart. Worn last, over the finished city: only paving left showing, never under or into anything
 * standing on it, and most of every street still paving.
 */
public final class CityRoads {
	/** What a paved cell is: the heart's plaza, a boulevard, a district street, or a road's scorched verge. */
	public enum Kind {
		PLAZA, BOULEVARD, STREET, VERGE
	}

	/** A paved cell: {@code across} its road (a boulevard's -3..3, a street's 30..34) and the op that paved it. */
	private record Cell(BlockPos at, Kind kind, int across, CityPlan.Op.Put op) {}

	/** Causes reach this far (blocks). */
	public static final int CAUSE_REACH = 7;
	private static final int SALT = 9401;

	private final Map<Long, Cell> cells = new LinkedHashMap<>();

	/** Paves (x, -1, z) from the middle with {@code s} and keeps it for the wear. */
	void pave(CityPlan p, int x, int z, BlockState s, Kind kind, int across) {
		pave(p, p.at(x, -1, z), s, kind, across);
	}

	void pave(CityPlan p, BlockPos at, BlockState s, Kind kind, int across) {
		p.put(at, s);
		cells.put(BlockPos.asLong(at.getX(), 0, at.getZ()), new Cell(at.immutable(), kind, across, (CityPlan.Op.Put) p.ops().getLast()));
	}

	/** A paved cell as it was paved, before the wear (for the tests). */
	public record Paved(BlockPos at, Kind kind, BlockState state) {}

	public java.util.List<Paved> paved() {
		return cells.values().stream().map(c -> new Paved(c.at(), c.kind(), c.op().state())).toList();
	}

	// ---------------------------------------------------------------- how worn

	/** The wear a road gets by its ring alone, 0-1: light at the heart, heavier outward. */
	public static double ringWear(int ring) {
		if (ring <= Wear.INTACT_TO)
			return 0.04;
		if (ring <= Wear.WORN_TO)
			return 0.12 + 0.2 * (ring - Wear.INTACT_TO) / (double) (Wear.WORN_TO - Wear.INTACT_TO);
		return 0.38 + 0.37 * Math.min(1, (ring - Wear.WORN_TO) / (double) (Wear.DAMAGED_TO - Wear.WORN_TO));
	}

	/** How much a block breaks the road round it (0: none): strikes and their soot, wrecks, the dead, barricades. */
	public static double cause(BlockState s) {
		Block b = s.getBlock();
		if (Masonry.isSoot(s))
			return 1;
		if (b == ModWastes.SCRAP_HEAP.get() || b instanceof KeyMarkedStoneBlock)
			return 0.8;
		if (b instanceof SalvageBlock || b instanceof PropPieceBlock)
			return 0.6;
		return 0;
	}

	/** Blocks with a working space round them (doors, vaults, the Substation, lifts, pylons, statues): nothing goes near. */
	private static boolean keepsClear(BlockState s) {
		Block b = s.getBlock();
		return (b instanceof EntityBlock || b instanceof ThreePartBlock) && !(b instanceof DerelictStreetLampBlock) && !(b instanceof DerelictCableBlock)
			&& !(b instanceof SalvageBlock);
	}

	// ---------------------------------------------------------------- the wear

	/** Wears the paving the finished city left showing. Reads only the plan. */
	void wear(CityPlan p) {
		if (cells.isEmpty())
			return;
		int ox = p.origin.getX(), oy = p.origin.getY(), oz = p.origin.getZ();
		long key = CityGround.key(p.seed, p.origin) + SALT;
		int half = CityLayout.CORE + CAUSE_REACH + 1, side = 2 * half + 1;
		Map<Long, CityPlan.Op.Put> floors = new HashMap<>();
		Set<Long> taken = new HashSet<>(), clear = new HashSet<>();
		Map<Long, Double> causes = new HashMap<>();
		for (CityPlan.Op op : p.ops()) {
			if (!(op instanceof CityPlan.Op.Put put)) {
				// a bot's place stays clear
				if (op instanceof CityPlan.Op.Scuttler || op instanceof CityPlan.Op.Sentry || op instanceof CityPlan.Op.Custodian)
					stamp(clear, op.at(), 1);
				continue;
			}
			BlockPos at = put.at();
			int y = at.getY() - oy;
			long col = BlockPos.asLong(at.getX(), 0, at.getZ());
			if (y == -1 && cells.containsKey(col))
				floors.put(col, put);
			if (y >= 0 && y <= 1)
				for (BlockPos q : PropPieceBlock.reach(at, put.state()))
					if (q.getY() - oy == 0)
						taken.add(BlockPos.asLong(q.getX(), 0, q.getZ()));
			if (y < -1 || y > 6)
				continue;
			double c = cause(put.state());
			if (c > 0)
				causes.merge(col, c, Math::max);
			if (y <= 2 && keepsClear(put.state()))
				stamp(clear, at, 2);
		}
		// the cables are set last (CityPlan.finish): their places are taken already
		for (Cell c : cells.values())
			if (p.cableAt(c.at().above()))
				taken.add(BlockPos.asLong(c.at().getX(), 0, c.at().getZ()));
		float[] heat = heat(causes, ox, oz, half, side);
		// what may be worn: the paving still showing, and what each cell may take
		Map<Long, Double> wear = new LinkedHashMap<>();
		Set<Long> free = new HashSet<>();
		for (var e : cells.entrySet()) {
			Cell c = e.getValue();
			if (floors.get(e.getKey()) != c.op())
				continue;
			int dx = c.at().getX() - ox, dz = c.at().getZ() - oz;
			wear.put(e.getKey(), intensity(key, dx, dz, heatAt(heat, dx, dz, half, side)));
			if (!taken.contains(e.getKey()) && !clear.contains(e.getKey()))
				free.add(e.getKey());
		}
		// the potholes first (a hole's rim breaks with it): only in a road's free paving
		Map<Long, Double> open = new HashMap<>();
		for (long col : free) {
			Kind k = cells.get(col).kind();
			if (k == Kind.BOULEVARD || k == Kind.STREET)
				open.put(col, wear.get(col));
		}
		Set<Long> holes = new HashSet<>();
		for (long col : open.keySet()) {
			BlockPos at = cells.get(col).at();
			if (holeAt(key, at.getX() - ox, at.getZ() - oz, open, ox, oz))
				holes.add(col);
		}
		for (var e : wear.entrySet()) {
			Cell c = cells.get(e.getKey());
			wearCell(p, key, c, c.at().getX() - ox, c.at().getZ() - oz, e.getValue(), heatAt(heat, c.at().getX() - ox, c.at().getZ() - oz, half, side),
				free.contains(e.getKey()), holes, ox, oz);
		}
	}

	private static void stamp(Set<Long> set, BlockPos at, int r) {
		for (int x = -r; x <= r; x++)
			for (int z = -r; z <= r; z++)
				set.add(BlockPos.asLong(at.getX() + x, 0, at.getZ() + z));
	}

	/** Each cause's reach round it, the strongest where they overlap. */
	private static float[] heat(Map<Long, Double> causes, int ox, int oz, int half, int side) {
		float[] heat = new float[side * side];
		int r = CAUSE_REACH;
		for (var e : causes.entrySet()) {
			int cx = BlockPos.getX(e.getKey()) - ox, cz = BlockPos.getZ(e.getKey()) - oz;
			if (Math.abs(cx) > half || Math.abs(cz) > half)
				continue;
			for (int x = Math.max(-half, cx - r); x <= Math.min(half, cx + r); x++)
				for (int z = Math.max(-half, cz - r); z <= Math.min(half, cz + r); z++) {
					double d = Math.sqrt((double) (x - cx) * (x - cx) + (double) (z - cz) * (z - cz));
					if (d > r)
						continue;
					float h = (float) (e.getValue() * (1 - d / (r + 1)));
					int i = (x + half) * side + z + half;
					if (h > heat[i])
						heat[i] = h;
				}
		}
		return heat;
	}

	private static double heatAt(float[] heat, int dx, int dz, int half, int side) {
		return Math.abs(dx) > half || Math.abs(dz) > half ? 0 : heat[(dx + half) * side + dz + half];
	}

	/** A cell's wear, 0-1: its ring's, in patches, and its causes'. Never more than a little in the heart. */
	static double intensity(long key, int dx, int dz, double heat) {
		int ring = Wear.ring(dx, dz);
		double w = ringWear(ring) * (0.5 + CityGround.rank(key + 5, dx, dz)) + 0.6 * heat;
		return Mth.clamp(ring <= Wear.INTACT_TO ? Math.min(w, 0.08) : w, 0, 1);
	}

	/** A pothole's middle: a few of the most worn cells. */
	private static boolean holeMiddle(long key, int dx, int dz, double wear) {
		return CityGround.roll(key, dx, dz, 1) < 0.035 * Math.pow(wear, 1.5);
	}

	/** In a pothole: its middle, or beside one and broken in with it ({@code open}: the cells a hole may take, and their wear). */
	private static boolean holeAt(long key, int dx, int dz, Map<Long, Double> open, int ox, int oz) {
		for (int x = -1; x <= 1; x++)
			for (int z = -1; z <= 1; z++) {
				Double w = open.get(BlockPos.asLong(ox + dx + x, 0, oz + dz + z));
				if (w != null && holeMiddle(key, dx + x, dz + z, w) && (x == 0 && z == 0 || CityGround.roll(key, dx, dz, 2) < 0.4))
					return true;
			}
		return false;
	}

	private void wearCell(CityPlan p, long key, Cell c, int dx, int dz, double w, double heat, boolean free, Set<Long> holes, int ox, int oz) {
		BlockPos floor = c.at();
		BlockState s = c.op().state();
		long col = BlockPos.asLong(floor.getX(), 0, floor.getZ());
		double r1 = CityGround.roll(key, dx, dz, 3), r2 = CityGround.roll(key, dx, dz, 4), r3 = CityGround.roll(key, dx, dz, 5);
		double r4 = CityGround.roll(key, dx, dz, 7), r5 = CityGround.roll(key, dx, dz, 8);
		boolean inner = c.kind() == Kind.PLAZA;
		// rubble spilling across from what broke it, more outward (on the verges too)
		if (free && !inner && r3 < 0.004 + 0.05 * w * w + 0.25 * heat * heat) {
			rubble(p, key, floor, dx, dz);
			return;
		}
		if (c.kind() == Kind.VERGE)
			return;
		if (holes.contains(col)) {
			// a pothole down to the scorched ground (on land only, never through a deck)
			p.put(floor, Blocks.AIR.defaultBlockState(), CityPlan.ON_LAND);
			p.put(floor.below(), CityLayout.scorched(dx, dz), CityPlan.ON_LAND);
			return;
		}
		boolean rim = false;
		for (Direction d : Direction.Plane.HORIZONTAL)
			rim |= holes.contains(BlockPos.asLong(floor.getX() + d.getStepX(), 0, floor.getZ() + d.getStepZ()));
		// ruts worn along the boulevards, in runs
		if (free && c.kind() == Kind.BOULEVARD && Math.abs(c.across()) == 1) {
			boolean alongX = Math.abs(dx) > Math.abs(dz);
			if (CityGround.rank(key + 7 + c.across(), (alongX ? dx : dz) * 3, alongX ? 40 : 80) > 0.45 - 0.2 * w) {
				p.put(floor, slab(s, r2 < 0.5 ? Blocks.ANDESITE_SLAB : null));
				return;
			}
		}
		if (free && !inner && r1 < 0.01 + 0.05 * w) {
			// a sett gone, the scorched ground showing
			p.put(floor, CityLayout.scorched(dx, dz));
			return;
		}
		if (free && (rim ? r4 < 0.5 : r4 < (inner ? 0.003 : 0.02 + 0.08 * w))) {
			// a sett sunk (round a pothole, tipping in)
			p.put(floor, slab(s, null));
			return;
		}
		if (free && !inner && r2 < 0.005 + 0.06 * w) {
			// a sett heaved up: a slab, or one tipped on its edge
			BlockState up = r3 < 0.7 ? slab(s, null)
				: stairs(s).setValue(HorizontalDirectionalBlock.FACING, Direction.from2DDataValue((int) (r1 * 1000) & 3));
			p.put(floor.above(), up);
			return;
		}
		if (rim || r5 < (inner ? 0.05 : 0.08 + 0.55 * w)) {
			BlockState cracked = cracked(s, r3);
			if (cracked != s)
				p.put(floor, cracked);
		}
	}

	/** Rubble on the road: broken stone and brick, a block of the makers' rubble now and then, a spill of gravel. */
	private static void rubble(CityPlan p, long key, BlockPos floor, int dx, int dz) {
		double r = CityGround.roll(key, dx, dz, 6);
		if (r < 0.1)
			p.put(floor, Blocks.GRAVEL.defaultBlockState(), CityPlan.ON_LAND);
		else
			p.put(floor.above(), r < 0.55 ? Blocks.COBBLED_DEEPSLATE_SLAB.defaultBlockState()
				: r < 0.8 ? ModWastes.STAINED_END_STONE_BRICK_SLAB.get().defaultBlockState() : r < 0.9 ? Masonry.rubble()
				: Blocks.GRAVEL.defaultBlockState());
	}

	// ---------------------------------------------------------------- the paving's broken forms

	/** A sett's cracked form: brick cracks, polished andesite wears rough or cracks. */
	static BlockState cracked(BlockState s, double r) {
		if (s.is(Blocks.STONE_BRICKS))
			return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
		if (s.is(Blocks.POLISHED_ANDESITE))
			return (r < 0.6 ? Blocks.ANDESITE : Blocks.CRACKED_STONE_BRICKS).defaultBlockState();
		return s;
	}

	/** A sett's slab ({@code worn}: that slab instead), sunk half a block. */
	static BlockState slab(BlockState s, Block worn) {
		Block b = worn != null ? worn : s.is(Blocks.POLISHED_ANDESITE) ? Blocks.POLISHED_ANDESITE_SLAB
			: s.is(Blocks.ANDESITE) ? Blocks.ANDESITE_SLAB : Blocks.STONE_BRICK_SLAB;
		return b.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
	}

	static BlockState stairs(BlockState s) {
		return (s.is(Blocks.POLISHED_ANDESITE) ? Blocks.POLISHED_ANDESITE_STAIRS : s.is(Blocks.ANDESITE) ? Blocks.ANDESITE_STAIRS : Blocks.STONE_BRICK_STAIRS)
			.defaultBlockState();
	}

	/** The paving and its broken forms (a street read from the air is still these). */
	public static boolean isPaving(BlockState s) {
		return s.is(Blocks.POLISHED_ANDESITE) || s.is(Blocks.ANDESITE) || s.is(Blocks.STONE_BRICKS) || s.is(Blocks.CRACKED_STONE_BRICKS)
			|| s.is(Blocks.POLISHED_ANDESITE_SLAB) || s.is(Blocks.ANDESITE_SLAB) || s.is(Blocks.STONE_BRICK_SLAB);
	}
}
