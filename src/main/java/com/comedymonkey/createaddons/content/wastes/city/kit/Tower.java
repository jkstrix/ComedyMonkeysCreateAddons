package com.comedymonkey.createaddons.content.wastes.city.kit;

import java.util.LinkedHashMap;
import java.util.Map;

import com.comedymonkey.createaddons.content.wastes.DerelictInsulatorBlock;
import com.comedymonkey.createaddons.content.wastes.ScarredPlateBlock;
import com.comedymonkey.createaddons.content.wastes.SlottedGrilleBlock;
import com.comedymonkey.createaddons.content.wastes.city.kit.Kit.Ctx;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * Type 5, the void-alloy slotted tower, and type 12, the sky tower (design brief): the uprising's commonest building and
 * the skyline's teeth. A tall dark shaft (x, z 0..w-1, w 3 or 5) with Void-Alloy Pillar corners, plate faces, one Slotted
 * Grille run (1 x 3) centred on a face every 7 blocks (alternating faces on a 3 x 3), a Band course every 7 between the
 * runs, a stepped chamfer and a small cap; a 5 x 5 sets off to 3 x 3 at two-thirds. On old foundations it stands on a
 * makers' plinth fragment one wider all round, strapped to its ledge (the stone under the jaws spalled: the bite).
 * Snapped (damaged) on a stepped diagonal with cracked scarred plates along the break and its top lying in the street toward
 * the heart; ruined, a stub with a snapped insulator.
 */
public final class Tower {
	private Tower() {}

	/** How a tower stands. */
	public enum Footing {
		/** On a makers' plinth fragment one wider all round, clamped (stood on). */
		PLINTH,
		/** On the floor of a makers' shed it breaks out of (rising through); the shed draws the roof ring. */
		FLOOR
	}

	/** The tower's body from y {@code y0} to its top course {@code top} (the chamfer on top of it). */
	public static Model build(Ctx c, int w, int height, Footing footing) {
		Model md = new Model();
		int y0 = footing == Footing.PLINTH ? 2 : 0, top = y0 + height - 1;
		if (footing == Footing.PLINTH)
			plinth(md, c.m(), w);
		body(md, w, y0, top);
		// worn: one chip on each outward face, mid-height
		if (c.wear() == Wear.WORN)
			chips(md, c, w, y0, top);
		if (c.wear() == Wear.DAMAGED)
			snap(md, c, w, y0, top);
		if (c.wear() == Wear.RUINED)
			stub(md, c, w, y0);
		return md;
	}

	/** The old plinth one wider all round: its body course, a dressed cap, a ground step; straps at the tower's foot biting it. */
	static void plinth(Model md, Masonry m, int w) {
		for (int x = -1; x <= w; x++)
			for (int z = -1; z <= w; z++) {
				md.set(x, 0, z, m.plinth());
				md.set(x, 1, z, m.band());
			}
		for (int i = -1; i <= w; i++) {
			md.set(i, 0, -2, Kit.stairs(m.plinthStairs(), Direction.SOUTH, false));
			md.set(i, 0, w + 1, Kit.stairs(m.plinthStairs(), Direction.NORTH, false));
			md.set(-2, 0, i, Kit.stairs(m.plinthStairs(), Direction.EAST, false));
			md.set(w + 1, 0, i, Kit.stairs(m.plinthStairs(), Direction.WEST, false));
		}
		// the straps: on each face's middle at y 2, their lower jaws on the ledge, the cap stone under them spalled
		int mid = w / 2;
		for (Direction d : Direction.Plane.HORIZONTAL) {
			int x = d == Direction.EAST ? w : d == Direction.WEST ? -1 : mid, z = d == Direction.SOUTH ? w : d == Direction.NORTH ? -1 : mid;
			md.set(x, 2, z, Kit.clampWall(d));
			md.set(x, 1, z, m.spalled());
			md.mark(Model.Cause.CLAMP, x, 1, z, 1);
		}
	}

	/** Which faces carry a grille run in segment k: every face on a 5 x 5; on a 3 x 3 north and south, then east and west. */
	static boolean runOn(int w, int k, Direction face) {
		return w >= 5 || (k % 2 == 0) == (face.getAxis() == Direction.Axis.Z);
	}

	/** The shaft, hollow, from y0 to top, its chamfered top and cap; a 5 x 5 setting off to 3 x 3 at two-thirds. */
	static void body(Model md, int w, int y0, int top) {
		int setOff = w >= 5 ? y0 + (top - y0) * 2 / 3 : Integer.MAX_VALUE;
		for (int y = y0; y <= top; y++) {
			boolean narrow = y > setOff;
			int lo = narrow ? 1 : 0, hi = narrow ? w - 2 : w - 1, mid = w / 2;
			int seg = (y - y0) % 7, k = (y - y0) / 7;
			for (int x = lo; x <= hi; x++)
				for (int z = lo; z <= hi; z++) {
					boolean ex = x == lo || x == hi, ez = z == lo || z == hi;
					if (!ex && !ez)
						continue;
					BlockState s;
					if (seg == 6)
						s = Kit.band();
					else if (ex && ez)
						s = Kit.pillar();
					else if (seg >= 2 && seg <= 4 && (x == mid || z == mid)) {
						Direction face = x == lo ? Direction.WEST : x == hi ? Direction.EAST : z == lo ? Direction.NORTH : Direction.SOUTH;
						s = runOn(w, k, face) ? Kit.grille(face) : Kit.plate();
					} else
						s = Kit.plate();
					md.set(x, y, z, s);
				}
			if (y == setOff) {
				// the set-off: a stair course sloping in from 5 x 5 to 3 x 3, on top of this course
				Kit.chamfer(md, 0, 0, w - 1, w - 1, y + 1, null);
				for (int x = 1; x <= w - 2; x++)
					for (int z = 1; z <= w - 2; z++)
						md.remove(x, y + 1, z);
			}
		}
		int lo = top > setOff ? 1 : 0, hi = top > setOff ? w - 2 : w - 1;
		Kit.chamfer(md, lo, lo, hi, hi, top + 1, Kit.plate());
		md.set(w / 2, top + 2, w / 2, Kit.vaSlab(false));
	}

	static void chips(Model md, Ctx c, int w, int y0, int top) {
		// a plain course (the second of a segment: never a band or a grille) about half way up
		int y = y0 + 7 * Math.max(0, (top - y0) / 14) + 1;
		for (Direction d : Direction.Plane.HORIZONTAL)
			if (c.outward(d)) {
				int x = d == Direction.EAST ? w - 1 : d == Direction.WEST ? 0 : 1, z = d == Direction.SOUTH ? w - 1 : d == Direction.NORTH ? 0 : 1;
				if (w >= 5) {
					x = d.getAxis() == Direction.Axis.Z ? 1 : x;
					z = d.getAxis() == Direction.Axis.X ? 1 : z;
				}
				BlockState s = md.get(x, y, z);
				if (s != null && s.is(ModWastes.VOID_ALLOY_PLATE.get())) {
					md.set(x, y, z, Kit.scarred(ScarredPlateBlock.Scar.CHIPPED, d));
					md.scarredFaces.add(d);
				}
			}
	}

	/** The break's height over (x, z): highest on the heart side (x 0), a block lower a block outward, the back row one lower. */
	static int breakAt(int hb, int x, int z, int w) {
		return hb - x - (z >= w - 1 ? 1 : 0);
	}

	/**
	 * Snapped: on a stepped diagonal at about 65% of its height; cracked scarred plates in the top two blocks of each outward
	 * face's middle column along the break (the break's cause); heat tint above the outward grilles below it; the top lying
	 * on its side in the street toward the heart (local -x), its band ring and grilles still recognisable, unlit.
	 */
	static void snap(Model md, Ctx c, int w, int y0, int top) {
		int hb = y0 + (int) Math.round((top - y0) * 0.65);
		Map<BlockPos, BlockState> topPart = new LinkedHashMap<>();
		for (var en : md.blocks().entrySet()) {
			BlockPos p = en.getKey();
			if (p.getX() < 0 || p.getX() >= w || p.getZ() < 0 || p.getZ() >= w || p.getY() < y0)
				continue;
			int keep = breakAt(hb, p.getX(), p.getZ(), w);
			if (p.getY() > keep) {
				if (p.getY() >= hb + 2)
					topPart.put(p, en.getValue());
				md.remove(p.getX(), p.getY(), p.getZ());
			}
		}
		// the break's scars: the outward faces' middle columns, the top two blocks, cracked (they glow dim when fed)
		int mid = w / 2;
		for (Direction d : Direction.Plane.HORIZONTAL) {
			if (!c.outward(d))
				continue;
			int x = d == Direction.EAST ? w - 1 : d == Direction.WEST ? 0 : mid, z = d == Direction.SOUTH ? w - 1 : d == Direction.NORTH ? 0 : mid;
			int keep = breakAt(hb, x, z, w);
			for (int y = keep - 1; y <= keep; y++)
				if (md.get(x, y, z) != null && !md.get(x, y, z).is(ModWastes.VOID_ALLOY_PILLAR.get()))
					md.set(x, y, z, Kit.scarred(ScarredPlateBlock.Scar.CRACKED, d));
			md.mark(Model.Cause.BREAK, x, keep, z, 1);
			md.scarredFaces.add(d);
			// heat tint above the outward grilles (the strikes that took it)
			for (int y = y0; y < keep - 1; y++)
				if (md.get(x, y, z) != null && md.get(x, y, z).is(ModWastes.SLOTTED_GRILLE.get()) && md.get(x, y + 1, z) != null
					&& md.get(x, y + 1, z).is(ModWastes.VOID_ALLOY_PLATE.get()))
					md.set(x, y + 1, z, Kit.scarred(ScarredPlateBlock.Scar.HEAT, d));
		}
		md.strikes++;
		// the top, fallen toward the heart on its side: tower (x, y, z) -> street (xs - (y - y1), x, z), turned a quarter about z
		int y1 = hb + 2, xs = c.wear() == Wear.DAMAGED ? -4 : -3;
		for (var en : topPart.entrySet()) {
			BlockPos p = en.getKey();
			BlockState s = lying(en.getValue(), p.getX(), w);
			if (s == null)
				continue;
			md.set(xs - (p.getY() - y1), p.getX(), p.getZ(), s);
		}
		md.mark(Model.Cause.FALL, xs, 0, mid, w);
	}

	/** A tower block as it lies, fallen toward -x (its x across becomes its height; a stair of the chamfer becomes the tip's slope). */
	static BlockState lying(BlockState s, int x, int w) {
		var b = s.getBlock();
		if (b == ModWastes.VOID_ALLOY_PILLAR.get())
			return Kit.pillar(Direction.Axis.X);
		if (b == ModWastes.VOID_ALLOY_BAND.get())
			return Kit.band(Direction.Axis.X);
		if (b == ModWastes.SLOTTED_GRILLE.get())
			return Kit.grille(s.getValue(SlottedGrilleBlock.AXIS) == SlottedGrilleBlock.Orientation.Z ? SlottedGrilleBlock.Orientation.Z_LYING
				: SlottedGrilleBlock.Orientation.UP_X);
		if (b == ModWastes.VOID_ALLOY_STAIRS.get()) {
			// the chamfer at the tip: its top edge slopes down to the tip, its underside up to it, the sides plain plate
			if (x == 0)
				return Kit.vaStairs(Direction.EAST, true);
			if (x == w - 1 || x == w - 2 && s.getValue(StairBlock.HALF) == Half.BOTTOM && w <= 3)
				return Kit.vaStairs(Direction.EAST, false);
			return Kit.plate();
		}
		if (b == ModWastes.VOID_ALLOY_SLAB.get())
			return null;
		return Kit.plate();
	}

	/** Ruined: a stub 3-7 high on a slant, cracked plates along its break, a snapped insulator on its highest corner. */
	static void stub(Model md, Ctx c, int w, int y0) {
		int hb = y0 + 6;
		for (var en : md.blocks().entrySet()) {
			BlockPos p = en.getKey();
			if (p.getY() < y0 || p.getX() < 0 || p.getX() >= w || p.getZ() < 0 || p.getZ() >= w)
				continue;
			if (p.getY() > breakAt(hb, p.getX(), p.getZ(), w) - (w > 3 ? 2 : 0))
				md.remove(p.getX(), p.getY(), p.getZ());
		}
		for (Direction d : Direction.Plane.HORIZONTAL)
			if (c.outward(d)) {
				int x = d == Direction.EAST ? w - 1 : d == Direction.WEST ? 0 : w / 2, z = d == Direction.SOUTH ? w - 1 : d == Direction.NORTH ? 0 : w / 2;
				int keep = breakAt(hb, x, z, w) - (w > 3 ? 2 : 0);
				if (md.get(x, keep, z) != null && !md.get(x, keep, z).is(ModWastes.VOID_ALLOY_PILLAR.get()))
					md.set(x, keep, z, Kit.scarred(ScarredPlateBlock.Scar.CRACKED, d));
				md.mark(Model.Cause.BREAK, x, keep, z, 1);
			}
		md.strikes++;
		// the snapped insulator where one was: on the highest corner (the heart side)
		int iy = breakAt(hb, 0, 0, w) - (w > 3 ? 2 : 0) + 1;
		md.special(Map.of(new BlockPos(0, iy, 0), ModWastes.DERELICT_INSULATOR.get().defaultBlockState()), site -> {
			BlockPos ins = site.insulator(0, iy, 0, DerelictInsulatorBlock.Mount.TOP, Direction.EAST);
			site.plan.snapped(ins, site.w(Direction.EAST).toYRot());
		});
	}

	// ================================================================ the sky tower (type 12)

	/** The sky tower's arm and insulators, for its spans: [the T-head's heart-side hanging insulator, the lower wall insulator]. */
	public record SkyTower(Model model, BlockPos head, BlockPos low) {}

	/**
	 * Type 12: a 5 x 5 slotted tower on old foundations, {@code height} to its arm (24-28), bands every 7; a T-head: a twin
	 * cross-arm of Void-Alloy Pillars reaching 2 toward the heart and 1 back, the sky-cable's insulator hanging under its
	 * heart-side end; the lower insulator with its snapped span (as built) on a clamp on the outward face. Intact or worn
	 * only: its top never snapped (it holds a live span). Returns the model and its two insulators' local cells (the caller
	 * ties the spans).
	 */
	public static SkyTower sky(Ctx c, int height) {
		Ctx kept = new Ctx(c.m(), c.wear().atMost(Wear.WORN), c.outward(), c.seed());
		int w = 5;
		Model md = new Model();
		int y0 = 2, top = y0 + height - 1;
		plinth(md, kept.m(), w);
		// the shaft without the set-off (it carries the arm at full width), chamfered under the arm
		for (int y = y0; y <= top; y++) {
			int seg = (y - y0) % 7, k = (y - y0) / 7;
			for (int x = 0; x < w; x++)
				for (int z = 0; z < w; z++) {
					boolean ex = x == 0 || x == w - 1, ez = z == 0 || z == w - 1;
					if (!ex && !ez)
						continue;
					BlockState s = seg == 6 ? Kit.band() : ex && ez ? Kit.pillar()
						: seg >= 2 && seg <= 4 && (x == 2 || z == 2) ? Kit.grille(x == 0 ? Direction.WEST : x == w - 1 ? Direction.EAST : z == 0 ? Direction.NORTH : Direction.SOUTH)
						: Kit.plate();
					md.set(x, y, z, s);
				}
		}
		Kit.chamfer(md, 0, 0, w - 1, w - 1, top + 1, Kit.plate());
		// the T-head: two pillars along x at z 1 and 3 from x -2 to w, a plate between them at the ends (the arm's web)
		int ay = top + 2;
		for (int x = -2; x <= w; x++) {
			md.set(x, ay, 1, Kit.pillar(Direction.Axis.X));
			md.set(x, ay, 3, Kit.pillar(Direction.Axis.X));
			md.set(x, ay, 2, x < 0 || x >= w ? Kit.plate() : Kit.band(Direction.Axis.X));
		}
		// the arm's struts at its root: upside-down stairs under the reach toward the heart
		md.set(-1, ay - 1, 1, Kit.vaStairs(Direction.EAST, true));
		md.set(-1, ay - 1, 3, Kit.vaStairs(Direction.EAST, true));
		BlockPos head = new BlockPos(-2, ay - 1, 2), low = new BlockPos(w, y0 + 9, 2);
		md.special(Map.of(head, ModWastes.DERELICT_INSULATOR.get().defaultBlockState()),
			site -> site.insulator(head.getX(), head.getY(), head.getZ(), DerelictInsulatorBlock.Mount.HANGING, Direction.WEST));
		md.set(w, y0 + 8, 2, Kit.clampWall(Direction.EAST));
		md.special(Map.of(low, ModWastes.DERELICT_INSULATOR.get().defaultBlockState()), site -> {
			BlockPos ins = site.insulator(low.getX(), low.getY(), low.getZ(), DerelictInsulatorBlock.Mount.WALL, Direction.EAST);
			site.plan.snapped(ins, site.w(Direction.EAST).toYRot());
		});
		if (kept.wear() == Wear.WORN)
			chips(md, kept, w, y0, top);
		return new SkyTower(md, head, low);
	}

	/**
	 * Type 5 rising through a roof: a 5 x 5 slotted tower standing on the floor of a big makers' shed (17 x 10) and breaking
	 * out of its roof, the slates gone in a 1-block ring round it, the cut tie beam clamped to it.
	 */
	public static Model throughRoof(Ctx c, int height) {
		Shed.Spec spec = new Shed.Spec(17, 10, 5, Shed.Door.LINED, false, false);
		Ctx shedCtx = new Ctx(c.m(), Wear.INTACT, c.outward(), c.seed());
		Model md = Shed.build(shedCtx, spec);
		int tx0 = 6, tz0 = 3, w = 5;
		// the ring: everything over the walls' top inside the tower's footprint and one round it goes
		for (int x = tx0 - 1; x <= tx0 + w; x++)
			for (int z = tz0 - 1; z <= tz0 + w; z++)
				for (int y = spec.e() + 1; y <= spec.e() + 8; y++)
					md.remove(x, y, z);
		Model t = new Model();
		body(t, w, 0, height - 1);
		if (c.wear() == Wear.WORN)
			chips(t, c, w, 0, height - 1);
		md.copyFrom(t, tx0, 0, tz0);
		// the cut truss (x 8 runs into the tower): its stubs clamped to the tower's faces
		for (int z : new int[] { tz0 - 1, tz0 + w })
			md.set(8, spec.e() + 1, z, Kit.slab(Masonry.walnutSlab(), false));
		md.set(8, spec.e() + 2, tz0 - 1, Kit.clampWall(Direction.NORTH));
		md.set(8, spec.e() + 2, tz0 + w, Kit.clampWall(Direction.SOUTH));
		return md;
	}
}
