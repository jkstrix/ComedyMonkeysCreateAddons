package com.comedymonkey.createaddons.content.wastes.city.kit;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiPredicate;

import com.comedymonkey.createaddons.content.wastes.DerelictInsulatorBlock;
import com.comedymonkey.createaddons.content.wastes.city.CityPlan;
import com.comedymonkey.createaddons.content.wastes.city.kit.Kit.Ctx;
import com.comedymonkey.createaddons.content.wastes.city.kit.Kit.Face;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Type 9, the heart drum and the hall's exterior, and type 10, the spire (design brief). Local coordinates are the drum's
 * own: x, z from its middle (the city's origin), y from the hall's floor. The drum is the city's octagon (flats at 16-17,
 * corners cut at |x| + |z| = 30, the 31 x 31 hall inside), 2 thick, of Stained End Stone Brick on a two-course cut deepslate
 * plinth with a stair water table; eight buttresses at the octagon's corners (2 wide, 2 proud, set-offs at y 5 and y 10)
 * carrying the dome's ribs; on each axis flat tall makers' windows (2 x 4, round-headed with a stair arch) in panels
 * recessed 1; a sill course, a lintel band at y 9, a corbel table at y 12, a parapet of stained brick wall at y 13, gapped
 * only where a snapped rib fell. The dome's skin stripped for cable copper (D73): eight bare Patina Rib ribs from the
 * buttress tops to the spire's collar, three snapped short, a row or two of weathered copper shingles left at its foot. The
 * three heart doors are lined cuts with projecting void-alloy portals and a plaque beside each. Intact (the heart is nearly
 * whole): no war marks, no soot.
 */
public final class Drum {
	private Drum() {}

	/** The drum's wall cells (the city's octagon less the hall). */
	public static boolean inHall(int x, int z) {
		return Math.abs(x) <= 15 && Math.abs(z) <= 15 && Math.abs(x) + Math.abs(z) <= 27;
	}

	public static boolean inDrum(int x, int z) {
		return Math.abs(x) <= 17 && Math.abs(z) <= 17 && Math.abs(x) + Math.abs(z) <= 30;
	}

	public static final int TOP = 12, FLAT = 13;

	static BlockState dress() {
		return CityPlan.block("create:cut_deepslate_bricks", Blocks.DEEPSLATE_BRICKS);
	}

	static BlockState dressStairs() {
		return CityPlan.block("create:cut_deepslate_brick_stairs", Blocks.DEEPSLATE_BRICK_STAIRS);
	}

	static BlockState dressSlab() {
		return CityPlan.block("create:cut_deepslate_brick_slab", Blocks.DEEPSLATE_BRICK_SLAB);
	}

	static BlockState brick() {
		return ModWastes.STAINED_END_STONE_BRICKS.get().defaultBlockState();
	}

	/** Which ribs are snapped (as built: three of eight). */
	static boolean snapped(int k) {
		return k % 3 == 1;
	}

	/**
	 * The drum, its cells limited to {@code include} (the catalogue's quarter; the city's whole), its doors on the given
	 * axis flats (north, east, west in the city), the spire's collar at (0, {@code spireZ}) for the ribs to reach.
	 */
	public static Model build(Ctx c, BiPredicate<Integer, Integer> include, java.util.Set<Direction> doors, int spireZ) {
		Model md = new Model();
		// ---- the wall, 2 thick (the octagon less the hall), on its plinth courses
		for (int x = -17; x <= 17; x++)
			for (int z = -17; z <= 17; z++) {
				if (!inDrum(x, z) || inHall(x, z) || !include.test(x, z))
					continue;
				for (int y = 0; y <= TOP; y++)
					md.set(x, y, z, y <= 1 ? dress() : brick());
			}
		// ---- the axis flats' faces: north, east, south, west (each runs t 0..26 from its viewer's left, |coord| <= 13)
		Face[] flats = { new Face(FLAT, -17, Direction.NORTH, 2 * FLAT + 1), new Face(17, FLAT, Direction.EAST, 2 * FLAT + 1),
			new Face(-FLAT, 17, Direction.SOUTH, 2 * FLAT + 1), new Face(-17, -FLAT, Direction.WEST, 2 * FLAT + 1) };
		for (Face f : flats) {
			boolean door = doors.contains(f.out());
			for (int t = 0; t < f.n(); t++) {
				if (!include.test(f.x(t, 0), f.z(t, 0)))
					continue;
				boolean cut = door && t >= FLAT - 1 && t <= FLAT + 2;
				if (!cut) {
					f.set(md, t, 0, 1, dress());
					f.set(md, t, 1, 1, dress());
					f.set(md, t, 2, 1, Kit.stairs(dressStairs(), f.in(), false));
				}
				f.set(md, t, 3, 0, dress());
				f.set(md, t, 9, 0, dress());
				f.set(md, t, TOP, 1, t % 2 == 0 ? Kit.stairs(dressStairs(), f.in(), true) : Kit.slab(dressSlab(), true));
				f.set(md, t, TOP + 1, 1, ModWastes.STAINED_END_STONE_BRICK_WALL.get().defaultBlockState());
			}
			// the windows: two a flat (the middle holds a portal, or the heart vault behind the south flat)
			for (int wt : new int[] { 5, 2 * FLAT - 6 })
				if (include.test(f.x(wt, 0), f.z(wt, 0)) && include.test(f.x(wt + 1, 0), f.z(wt + 1, 0)))
					window(md, f, wt);
			// the buttresses at the flat's two ends (the octagon's corners)
			for (int bt : new int[] { 0, 2 * FLAT - 1 })
				if (include.test(f.x(bt, 0), f.z(bt, 0)))
					buttress(md, f, bt);
			if (door && include.test(f.x(FLAT, 0), f.z(FLAT, 0)))
				portal(md, c, f);
		}
		// ---- the diagonal flats: stepped, the courses following the steps (plinth and water table, sill and lintel, corbels, parapet)
		for (int x = -17; x <= 17; x++)
			for (int z = -17; z <= 17; z++) {
				if (!inDrum(x, z) || inHall(x, z) || !include.test(x, z) || Math.abs(x) <= FLAT || Math.abs(z) <= FLAT)
					continue;
				for (Direction d : Direction.Plane.HORIZONTAL) {
					int ox = x + d.getStepX(), oz = z + d.getStepZ();
					if (inDrum(ox, oz) || Math.abs(ox) <= FLAT + 1 && Math.abs(oz) > 17 || Math.abs(oz) <= FLAT + 1 && Math.abs(ox) > 17)
						continue;
					md.setIfEmpty(ox, 0, oz, dress());
					md.setIfEmpty(ox, 1, oz, dress());
					md.setIfEmpty(ox, 2, oz, Kit.stairs(dressStairs(), d.getOpposite(), false));
					md.set(x, 3, z, dress());
					md.set(x, 9, z, dress());
					md.setIfEmpty(ox, TOP, oz, Kit.stairs(dressStairs(), d.getOpposite(), true));
					md.setIfEmpty(ox, TOP + 1, oz, ModWastes.STAINED_END_STONE_BRICK_WALL.get().defaultBlockState());
				}
			}
		// ---- the dome: eight ribs from the buttress tops to the collar, three snapped; shingles at its foot; the parapet gapped
		int[][] feet = { { FLAT, -18 }, { 18, -FLAT }, { 18, FLAT }, { FLAT, 18 }, { -FLAT, 18 }, { -18, FLAT }, { -18, -FLAT }, { -FLAT, -18 } };
		for (int k = 0; k < 8; k++) {
			int fx = feet[k][0], fz = feet[k][1];
			if (!include.test(Math.max(-17, Math.min(17, fx)), Math.max(-17, Math.min(17, fz))))
				continue;
			rib(md, fx, fz, spireZ, snapped(k));
			if (snapped(k)) {
				// the parapet gap where the snapped rib fell, the bricks it broke cracked (its cause), the fallen pieces at the foot
				// the flat this foot stands on: out is the way its parapet looks, along runs back toward the flat's middle
				boolean onX = Math.abs(fz) == 18;
				Direction out = onX ? (fz < 0 ? Direction.NORTH : Direction.SOUTH) : (fx < 0 ? Direction.WEST : Direction.EAST);
				int sx = onX ? -Integer.signum(fx) : 0, sz = onX ? 0 : -Integer.signum(fz);
				for (int i = 1; i <= 3; i++)
					md.remove(fx + sx * i, TOP + 1, fz + sz * i);
				int wx = fx + sx * 2 - out.getStepX(), wz = fz + sz * 2 - out.getStepZ();
				md.mark(Model.Cause.FALL, wx, TOP, wz, 3);
				if (md.has(wx, TOP, wz))
					md.set(wx, TOP, wz, ModWastes.CRACKED_STAINED_END_STONE_BRICKS.get().defaultBlockState());
				int ox = fx + sx * 2 + out.getStepX() * 3, oz = fz + sz * 2 + out.getStepZ() * 3;
				md.mark(Model.Cause.FALL, ox, 0, oz, 2);
				md.setIfEmpty(ox, 0, oz, ModWastes.STAINED_END_STONE_BRICK_WALL.get().defaultBlockState());
				md.setIfEmpty(ox + sx, 0, oz + sz, Kit.slab(ModWastes.STAINED_END_STONE_BRICK_SLAB.get().defaultBlockState(), false));
				md.setIfEmpty(ox + out.getStepX(), 0, oz + out.getStepZ(), ModWastes.PATINA_RIB.get().defaultBlockState());
			}
		}
		// shingles: a row of weathered copper shingle stairs on the wall's inner layer top, rising inward, between the ribs
		BlockState shingle = CityPlan.block("create:weathered_copper_shingle_stairs", Blocks.WEATHERED_CUT_COPPER_STAIRS);
		for (int x = -17; x <= 17; x++)
			for (int z = -17; z <= 17; z++) {
				if (!inDrum(x, z) || inHall(x, z) || !include.test(x, z))
					continue;
				boolean inner = inHall(x + 1, z) || inHall(x - 1, z) || inHall(x, z + 1) || inHall(x, z - 1);
				if (!inner || md.has(x, TOP + 1, z))
					continue;
				Direction in = Math.abs(x) >= Math.abs(z) ? (x > 0 ? Direction.WEST : Direction.EAST) : (z > 0 ? Direction.NORTH : Direction.SOUTH);
				md.set(x, TOP + 1, z, Kit.stairs(shingle, in, false));
			}
		return md;
	}

	/** A window 2 x 4 at t, t + 1 in the inner layer, round-headed (a stair arch), in a panel recessed 1 (the outer layer cut back). */
	static void window(Model md, Face f, int wt) {
		BlockState win = Masonry.window().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_AXIS,
			f.out().getAxis() == Direction.Axis.Z ? Direction.Axis.Z : Direction.Axis.X);
		for (int t = wt - 1; t <= wt + 2; t++)
			for (int y = 4; y <= 8; y++)
				f.remove(md, t, y, 0);
		for (int y = 4; y <= 7; y++) {
			f.set(md, wt, y, -1, win);
			f.set(md, wt + 1, y, -1, win);
		}
		BlockState st = ModWastes.STAINED_END_STONE_BRICK_STAIRS.get().defaultBlockState();
		f.set(md, wt, 8, -1, Kit.stairs(st, f.along().getOpposite(), true));
		f.set(md, wt + 1, 8, -1, Kit.stairs(st, f.along(), true));
	}

	/** A buttress at t, t + 1: 2 proud to the set-off at y 5, 1 proud to y 10, flush above; its plinth and water table stepping out with it. */
	static void buttress(Model md, Face f, int bt) {
		for (int t = bt; t <= bt + 1; t++) {
			for (int y = 0; y <= 1; y++)
				for (int d = 1; d <= 3; d++)
					f.set(md, t, y, d, dress());
			f.set(md, t, 2, 3, Kit.stairs(dressStairs(), f.in(), false));
			for (int y = 2; y <= 4; y++) {
				f.set(md, t, y, 1, brick());
				f.set(md, t, y, 2, brick());
			}
			f.set(md, t, 5, 1, brick());
			f.set(md, t, 5, 2, Kit.stairs(ModWastes.STAINED_END_STONE_BRICK_STAIRS.get().defaultBlockState(), f.in(), false));
			for (int y = 6; y <= 9; y++)
				f.set(md, t, y, 1, brick());
			f.set(md, t, 10, 1, Kit.stairs(ModWastes.STAINED_END_STONE_BRICK_STAIRS.get().defaultBlockState(), f.in(), false));
		}
	}

	/**
	 * A heart door: the lined cut through both layers (t 13-14, 2 x 3: pillar jambs and head lining it), the Sliding Door (heart)
	 * in the inner layer, the portal projecting 1 (pillar jambs, a band head, a stair chamfer), the plaque on the first brick
	 * past the portal at 1.5 up (on the plinth's face).
	 */
	static void portal(Model md, Ctx c, Face f) {
		int t0 = FLAT;
		for (int y = 0; y <= 2; y++) {
			for (int d = 2; d >= -1; d--) {
				f.remove(md, t0, y, d);
				f.remove(md, t0 + 1, y, d);
			}
			for (int d = -1; d <= 1; d++) {
				f.set(md, t0 - 1, y, d, Kit.pillar());
				f.set(md, t0 + 2, y, d, Kit.pillar());
			}
		}
		Direction.Axis along = f.along().getAxis();
		for (int t = t0 - 1; t <= t0 + 2; t++) {
			f.set(md, t, 3, 0, Kit.pillar(along));
			f.set(md, t, 3, -1, Kit.pillar(along));
			f.set(md, t, 3, 1, Kit.band());
			f.set(md, t, 4, 1, Kit.vaStairs(f.in(), false));
		}
		md.mark(Model.Cause.CUT, f.x(t0, 0), 1, f.z(t0, 0), 2);
		Map<BlockPos, BlockState> door = new LinkedHashMap<>();
		for (int y = 0; y <= 2; y++)
			for (int t = t0; t <= t0 + 1; t++)
				door.put(new BlockPos(f.x(t, -1), y, f.z(t, -1)), ModWastes.SLIDING_DOOR.get().defaultBlockState());
		int ax = f.x(t0, -1), az = f.z(t0, -1), bx = f.x(t0 + 1, -1), bz = f.z(t0 + 1, -1);
		Direction out = f.out();
		md.special(door, site -> site.bound(() -> site.plan.doorAcross(site.at(ax, 0, az), site.at(bx, 0, bz), site.w(out), true)));
		Shed.plaque(md, f.x(t0 - 2, 2), f.z(t0 - 2, 2), f.out(), false);
	}

	/**
	 * A dome rib (Patina Rib, D75: 1 wide, 2 deep) from the buttress top at (fx, fz) toward the spire's collar at (0, spireZ),
	 * rising from the parapet's height to the collar's on a quarter sine; snapped ones stop at 40%.
	 */
	static void rib(Model md, int fx, int fz, int spireZ, boolean snapped) {
		BlockState rib = ModWastes.PATINA_RIB.get().defaultBlockState();
		double dx = 0 - fx, dz = spireZ - fz, len = Math.sqrt(dx * dx + dz * dz);
		double stop = len - 4;
		int n = (int) Math.ceil(stop);
		int last = snapped ? (int) Math.round(n * 0.4) : n;
		int px = Integer.MIN_VALUE, pz = 0, py = 0;
		for (int s = 0; s <= last; s++) {
			double f = s / (double) n;
			int x = (int) Math.round(fx + dx * f * stop / len), z = (int) Math.round(fz + dz * f * stop / len);
			int y = TOP + 1 + (int) Math.round(8 * Math.sin(Math.PI / 2 * f));
			if (px != Integer.MIN_VALUE) {
				// keep the rib joined face to face: from the last cell along x, then z, then up
				md.set(x, py, pz, rib);
				md.set(x, py, z, rib);
				for (int yy = Math.min(py, y); yy <= Math.max(py, y); yy++)
					md.set(x, yy, z, rib);
			}
			md.set(x, y, z, rib);
			md.set(x, y - 1, z, rib);
			px = x;
			pz = z;
			py = y;
		}
		if (snapped)
			md.mark(Model.Cause.FALL, px, py, pz, 1);
	}

	// ================================================================ the spire (type 10)

	/** The spire's crown and its arms' insulators (local cells; north, east, south, west). */
	public record Spire(Model model, BlockPos[] crown) {}

	public static final int SPIRE_BASE = 14, WAIST = 40, CROWN = 58;

	/**
	 * The spire (a needle): a collar where it rises out of the dome (a ring of Band and plate one wider than the shaft, clamped
	 * to the rib heads), a 5 x 5 shaft to y 40 with Seamed Plate corners (they climb, lit with the heart), plate faces with a
	 * Slotted Grille run every 6 blocks on each face and a Band ring between the runs; a stair course sloping in to 3 x 3 at
	 * 40; the crown at 58: a seamed cap, four arms of horizontal pillars reaching 8 (each braced at its root by two upside-down
	 * stairs), a hanging insulator under each tip; the needle tip of Void-Alloy Wall posts. Local (x, z) from the spire's axis;
	 * {@code base} is the y it rises from (the drum's roof, 14, in the city). Intact: no marks.
	 */
	public static Spire spire(int base) {
		return spire(base, true);
	}

	/** {@code insulators}: hang the crown's insulators (the catalogue); in the city each district hangs its own. */
	public static Spire spire(int base, boolean insulators) {
		Model md = new Model();
		for (int y = base; y < CROWN; y++) {
			int half = y < WAIST ? 2 : 1;
			int seg = (y - base) % 6;
			for (int x = -half; x <= half; x++)
				for (int z = -half; z <= half; z++) {
					boolean ex = Math.abs(x) == half, ez = Math.abs(z) == half;
					if (!ex && !ez)
						continue;
					BlockState s;
					if (ex && ez)
						s = Kit.seamed();
					else if (seg == 5)
						s = Kit.band();
					else if (seg >= 1 && seg <= 3 && (x == 0 || z == 0))
						s = Kit.grille(x == -half ? Direction.WEST : x == half ? Direction.EAST : z == -half ? Direction.NORTH : Direction.SOUTH);
					else
						s = Kit.plate();
					md.set(x, y, z, s);
				}
			if (y == WAIST - 1) {
				Kit.chamfer(md, -2, -2, 2, 2, WAIST, null);
				for (int x = -1; x <= 1; x++)
					for (int z = -1; z <= 1; z++)
						md.remove(x, WAIST, z);
			}
		}
		// the collar: a ring one wider than the shaft at its foot, a band over a plate course
		for (int x = -3; x <= 3; x++)
			for (int z = -3; z <= 3; z++)
				if (Math.abs(x) == 3 || Math.abs(z) == 3) {
					md.set(x, base, z, Kit.plate());
					md.set(x, base + 1, z, Kit.band());
				}
		// the crown: the seamed cap, four arms reaching 8 with struts, a hanging insulator under each tip
		for (int x = -1; x <= 1; x++)
			for (int z = -1; z <= 1; z++)
				md.set(x, CROWN, z, Kit.seamed());
		BlockPos[] crown = new BlockPos[4];
		int i = 0;
		for (Direction d : new Direction[] { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST }) {
			for (int k = 2; k <= 9; k++)
				md.set(d.getStepX() * k, CROWN, d.getStepZ() * k, Kit.pillar(d.getAxis()));
			// the struts at the root: two upside-down stairs under the arm, their backs to the shaft
			md.set(d.getStepX() * 2, CROWN - 1, d.getStepZ() * 2, Kit.vaStairs(d.getOpposite(), true));
			md.set(d.getStepX() * 3, CROWN - 1, d.getStepZ() * 3, Kit.vaStairs(d.getOpposite(), true));
			BlockPos tip = new BlockPos(d.getStepX() * 9, CROWN - 1, d.getStepZ() * 9);
			crown[i++] = tip;
			if (insulators)
				md.special(Map.of(tip, ModWastes.DERELICT_INSULATOR.get().defaultBlockState()),
				site -> site.insulator(tip.getX(), tip.getY(), tip.getZ(), DerelictInsulatorBlock.Mount.HANGING, Direction.NORTH));
		}
		// the needle: Void-Alloy Wall posts stacked over a plate on the cap
		md.set(0, CROWN + 1, 0, Kit.plate());
		for (int y = CROWN + 2; y <= CROWN + 6; y++)
			md.set(0, y, 0, Kit.vaWall());
		return new Spire(md, crown);
	}
}
