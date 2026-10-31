package com.comedymonkey.createaddons.content.wastes.city.kit;

import java.util.Map;

import com.comedymonkey.createaddons.content.wastes.DerelictInsulatorBlock;
import com.comedymonkey.createaddons.content.wastes.city.kit.Kit.Ctx;
import com.comedymonkey.createaddons.content.wastes.city.kit.Kit.Face;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Type 1, the makers' shed (design/briefs/clockwork-wastes-buildings.md): the town's common building, a long low box under a
 * shallow slate pitch with a copper ridge line, its piers making a ribbed side. Local x along the ridge (the door gable at
 * x 0, toward the heart; the outer gable at x {@code l - 1}), z across (the front long side at z 0). The full grammar:
 * plinth and ground step, piers every 4 (pier + 3-block bay), a 1 x 2 window a bay with its sill, the lintel band, the
 * corbel course, eaves 1 over the corbels and 1 past the gables (walnut barge), a 1:2 slab pitch with a 2-wide ridge and its
 * copper roll, a truss at every inner pier, a ridge vent on every other bay of sheds 15 or longer. Small sizes (the yard's
 * control hut, the Rail Yard's signal box) keep only the corner piers.
 */
public final class Shed {
	private Shed() {}

	public enum Door {
		/** The makers' walnut door, 1 x 2, shut (sheds the uprising didn't use). */
		WALNUT,
		/** The uprising's lined cut: cut up to 2 x 3, jambs and head in Void-Alloy Pillar, spalled where the cut broke the bond. */
		LINED,
		/** A lined cut holding the district's locked Sliding Door (the uprising's door into a shed it uses). */
		SLIDING,
		/** No door in the gable (a door elsewhere, or none). */
		NONE
	}

	/** What a shed is: its size (walls x 0..l-1, z 0..d-1, wall top course at y e), its door, and the uprising's work on it. */
	public record Spec(int l, int d, int e, Door door, boolean apexInsulator, boolean threaded) {
		public boolean small() {
			return e <= 3;
		}
	}

	/** The roof's eave row: 1 past the corbels (z -2) on a full shed; a small one has no corbel course, so 1 past its wall. */
	static int eave(Spec s) {
		return s.small() ? -1 : -2;
	}

	/** The window rows: from y 2 on a full shed (y 1 sills), from y 1 on a small one. */
	static int windowY(Spec s) {
		return s.small() ? 1 : 2;
	}

	public static Model build(Ctx c, Spec s) {
		Model md = new Model();
		Masonry m = c.m();
		int l = s.l(), d = s.d(), e = s.e();
		Face front = Kit.front(l, d), back = Kit.back(l, d), east = Kit.east(l, d), west = Kit.west(l, d);
		int doorT = d / 2 - 1; // the gable door's left column (a lined cut takes doorT and doorT + 1)
		// ---- walls: the plinth course, the wall field, the lintel band, the top course
		int wy = windowY(s), lintel = wy + 2;
		for (Face f : new Face[] { front, back, east, west })
			for (int t = 0; t < f.n(); t++)
				for (int y = 1; y <= e; y++)
					f.set(md, t, y, 0, y == lintel && lintel < e + 1 ? m.band() : m.wall());
		java.util.function.IntPredicate westDoor = t -> (s.door() == Door.LINED || s.door() == Door.SLIDING) && t >= doorT - 1 && t <= doorT + 2
			|| s.door() == Door.WALNUT && t == doorT + 1;
		for (Face f : new Face[] { front, back, east, west })
			Kit.plinth(md, m, f, 1, f == west ? westDoor : t -> false);
		// ---- piers: every 4 along the long sides; the corners an L; either side of a walnut door
		for (Face f : new Face[] { front, back })
			for (int t = 0; t < f.n(); t++)
				if (t % 4 == 0 && t > 0 && t < f.n() - 1 && !s.small()) {
					f.set(md, t, 0, 1, m.plinth());
					Kit.pier(md, m, f, t, 1, e - 1, 1);
				}
		for (int[] cn : new int[][] { { 0, 0 }, { l - 1, 0 }, { l - 1, d - 1 }, { 0, d - 1 } }) {
			Direction ox = cn[0] == 0 ? Direction.WEST : Direction.EAST, oz = cn[1] == 0 ? Direction.NORTH : Direction.SOUTH;
			Kit.cornerPier(md, m, cn[0], cn[1], ox, oz, 0, 0);
			for (int x : new int[] { cn[0] + ox.getStepX() }) {
				md.set(x, 0, cn[1], m.plinth());
				md.set(cn[0], 0, cn[1] + oz.getStepZ(), m.plinth());
				md.set(x, 0, cn[1] + oz.getStepZ(), m.plinth());
			}
			Kit.cornerPier(md, m, cn[0], cn[1], ox, oz, 1, e - 1);
		}
		if (s.door() == Door.WALNUT && !s.small())
			for (int t : new int[] { doorT, doorT + 2 }) {
				west.set(md, t, 0, 1, m.plinth());
				Kit.pier(md, m, west, t, 1, e - 1, 1);
			}
		// ---- windows: one a bay, centred, 1 x 2 with its sill; the outer gable's bays too
		for (Face f : new Face[] { front, back })
			for (int t = 2; t < f.n() - 1; t += 4)
				Kit.window(md, m, f, s.small() ? f.n() / 2 : t, wy, 2);
		if (s.small()) {
			// one window a long side is enough (the loop above put the middle one each time)
		} else {
			Kit.window(md, m, east, 2, wy, 2);
			Kit.window(md, m, east, d - 3, wy, 2);
		}
		// ---- the small-brick panels between the windows' sill and lintel (lighter, non-bearing infill: a fixed place)
		if (!s.small())
			for (Face f : new Face[] { front, back })
				for (int t = 2; t < f.n() - 1; t += 4)
					for (int dt : new int[] { -1, 1 })
						f.set(md, t + dt, wy + 1, 0, m.second());
		// ---- the corbel course under the eaves (long sides) and across the gables
		if (!s.small()) {
			for (Face f : new Face[] { front, back })
				Kit.corbels(md, m, f, e, false, 0, f.n() - 1);
			for (Face f : new Face[] { east, west })
				Kit.corbels(md, m, f, e, false, 1, f.n() - 2);
		}
		// ---- the roof: eaves at z -2 and d + 1, 1 past the gables (walnut barge), a 1:2 pitch, a 2-wide ridge, its roll
		int zE = eave(s), zFar = d - 1 - zE;
		int hMax = 0;
		for (int z = zE; z <= zFar; z++)
			hMax = Math.max(hMax, Kit.pitchHalf(z, zE, zFar, e));
		for (int x = -1; x <= l; x++)
			for (int z = zE; z <= zFar; z++) {
				int h = Kit.pitchHalf(z, zE, zFar, e);
				boolean gable = x == 0 || x == l - 1;
				boolean barge = x == -1 || x == l;
				if (gable && z >= 0 && z <= d - 1) {
					// the gable: small-brick infill up under the pitch, its top a full block where the slate is a top slab
					Kit.gableFill(md, x, z, h, e + 1, m.second(), m.dressSlab());
					if (h % 2 == 1)
						md.set(x, h / 2, z, m.second());
					else
						Kit.pitchSlab(md, x, z, h, Masonry.slateSlab());
					continue;
				}
				Kit.pitchSlab(md, x, z, h, barge ? Masonry.walnutSlab() : Masonry.slateSlab());
				// where the roof crosses the long walls' line: its two halves as one block (the wall plate)
				if (!barge && (z == 0 || z == d - 1) && h % 2 == 1)
					md.set(x, h / 2, z, Masonry.slateFull());
			}
		// the copper ridge roll on the 2-wide ridge
		int rollY = hMax % 2 == 1 ? hMax / 2 + 1 : hMax / 2;
		for (int x = -1; x <= l; x++)
			for (int z = zE; z <= zFar; z++)
				if (Kit.pitchHalf(z, zE, zFar, e) == hMax) {
					if (hMax % 2 == 1)
						md.set(x, rollY, z, Masonry.ridge());
					else
						md.set(x, rollY, z, Masonry.ridge());
				}
		// ---- trusses at every inner pier: a walnut slab tie beam and a walnut fence king post
		int zc = d / 2 - 1;
		if (!s.small())
			for (int x = 4; x < l - 1; x += 4)
				truss(md, x, d, e, zc, hMax);
		// ---- a ridge vent on every other bay of a long shed: a 2-wide louvred box (walnut trapdoors) under a slate lid
		if (l >= 15)
			for (int bay = 1; 4 * bay + 3 < l; bay += 2)
				for (int x = 4 * bay + 1; x <= 4 * bay + 3; x++) {
					int y = rollY;
					md.set(x, y, zc, louvre(Direction.SOUTH));
					md.set(x, y, zc + 1, louvre(Direction.NORTH));
					md.set(x, y + 1, zc, Kit.slab(Masonry.slateSlab(), false));
					md.set(x, y + 1, zc + 1, Kit.slab(Masonry.slateSlab(), false));
				}
		// ---- the gable door (toward the heart), its plaque on the first wall block to its left, 1 up
		boolean askew = c.wear().atLeast(Wear.DAMAGED);
		if (s.door() == Door.LINED || s.door() == Door.SLIDING)
			linedCut(md, m, west, doorT, 1);
		if (s.door() == Door.SLIDING) {
			Map<BlockPos, BlockState> door = new java.util.LinkedHashMap<>();
			for (int y = 0; y <= 2; y++)
				for (int t = doorT; t <= doorT + 1; t++)
					door.put(new BlockPos(west.x(t, 0), y, west.z(t, 0)), ModWastes.SLIDING_DOOR.get().defaultBlockState());
			int ax = west.x(doorT, 0), az = west.z(doorT, 0), bx = west.x(doorT + 1, 0), bz = west.z(doorT + 1, 0);
			md.special(door, site -> site.doorAcross(ax, az, bx, bz, Direction.WEST, false));
		}
		if (s.door() == Door.WALNUT) {
			int t = s.small() ? d / 2 : doorT + 1;
			BlockState door = Blocks.DARK_OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, west.in());
			west.set(md, t, 0, 0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
			west.set(md, t, 1, 0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
			west.remove(md, t, 0, 1);
		}
		if (s.door() != Door.NONE) {
			int pt = s.door() == Door.LINED || s.door() == Door.SLIDING ? doorT - 2 : s.small() ? d / 2 - 1 : doorT - 1;
			int px = west.x(pt, 1), pz = west.z(pt, 1);
			plaque(md, px, pz, west.out(), askew);
		}
		// ---- the uprising on it: a saddle clamp on the door gable's apex holding an insulator; cable threaded through two windows
		if (s.apexInsulator()) {
			md.set(0, rollY, zc, Kit.clampSaddle(Direction.SOUTH));
			md.mark(Model.Cause.CLAMP, 0, rollY - 1, zc, 1);
			int iy = rollY + 1;
			md.special(Map.of(new BlockPos(0, iy, zc), ModWastes.DERELICT_INSULATOR.get().defaultBlockState()),
				site -> site.insulator(0, iy, zc, DerelictInsulatorBlock.Mount.TOP, Direction.NORTH));
		}
		if (s.threaded() && l >= 9)
			threaded(md, front, 2, 6, wy);
		// ---- the state
		damage(md, c, s, front, east, hMax);
		return md;
	}

	/** The cable threaded in through one window and out through the next (the threaded join), on the sills and inside the wall. */
	static void threaded(Model md, Face f, int t0, int t1, int wy) {
		Map<BlockPos, BlockState> cells = new java.util.LinkedHashMap<>();
		BlockState cable = ModWastes.DERELICT_CABLE.get().defaultBlockState();
		for (int t : new int[] { t0, t1 }) {
			cells.put(new BlockPos(f.x(t, 1), wy, f.z(t, 1)), cable);
			cells.put(new BlockPos(f.x(t, 0), wy, f.z(t, 0)), cable);
		}
		for (int t = t0; t <= t1; t++)
			cells.put(new BlockPos(f.x(t, -1), wy, f.z(t, -1)), cable);
		Direction out = f.out();
		md.special(cells, site -> {
			for (int t : new int[] { t0, t1 }) {
				site.cable(f.x(t, 1), wy, f.z(t, 1), Direction.DOWN);
				site.cable(f.x(t, 0), wy, f.z(t, 0), Direction.DOWN);
			}
			for (int t = t0; t <= t1; t++)
				site.cable(f.x(t, -1), wy, f.z(t, -1), out);
		});
	}

	/**
	 * The shed's state, damage starting at its outer end (x {@code l - 1}): worn, a strike on the outward gable and slates
	 * slipped at the outer eaves; damaged, the roof holed over the outer bay showing its truss and rafters (one broken and
	 * hanging, fallen slates on the floor), the outward windows gone with fire soot above them; ruined, the outer gable and
	 * last bay down into a rubble cone, the far end standing to its corbels on a stepped diagonal, the roof gone.
	 */
	static void damage(Model md, Ctx c, Spec s, Face front, Face east, int hMax) {
		Masonry m = c.m();
		int l = s.l(), d = s.d(), e = s.e(), wy = windowY(s);
		if (c.wear() == Wear.INTACT)
			return;
		if (c.wear() == Wear.RUINED) {
			ruin(md, c, s, front, east);
			return;
		}
		// worn: one strike on the outward gable; a few slates slipped at the outer eaves, one lying below
		if (c.outward(Direction.EAST))
			Kit.strike(md, m, east, d / 2 - 1, wy + 1, 2, 2, c.seed());
		int zs = eave(s);
		for (int x : new int[] { l - 2, l - 4 })
			md.remove(x, Kit.pitchHalf(zs, zs, d - 1 - zs, e) / 2, zs);
		md.remove(l - 3, Kit.pitchHalf(zs + 1, zs, d - 1 - zs, e) / 2, zs + 1);
		md.set(l - 3, 0, zs, Kit.slab(Masonry.slateSlab(), false));
		if (c.wear() != Wear.DAMAGED)
			return;
		// damaged: the roof over the outer bay holed (its truss and a rafter showing, one rafter broken and hanging)
		int p = s.small() ? 0 : 4 * ((l - 2) / 4);
		int x0 = p + 1, x1 = l - 2;
		int ze = eave(s);
		for (int x = x0; x <= x1; x++)
			for (int z = 1; z <= 3; z++) {
				int h = Kit.pitchHalf(z, ze, d - 1 - ze, e);
				md.remove(x, h / 2, z);
			}
		int rx = (x0 + x1) / 2;
		for (int z = 1; z <= 3; z++) {
			int h = Kit.pitchHalf(z, ze, d - 1 - ze, e);
			Kit.pitchSlab(md, rx, z, h, Masonry.walnutSlab());
		}
		// the rafter's foot on the wall plate, through a walnut post; the next rafter broken, hanging from the tie beam
		md.set(rx, Kit.pitchHalf(0, ze, d - 1 - ze, e) / 2, 1, Masonry.walnutFence());
		if (!s.small())
			md.set(p, e, 2, Masonry.walnutFence());
		md.set(x0, 0, 1, Kit.slab(Masonry.slateSlab(), false));
		md.set(x1, 0, 2, Kit.slab(Masonry.slateSlab(), true));
		// the outward windows gone (the outer half of the front, the outer gable), fire soot up the face above the outermost
		for (int t = 0; t < front.n(); t++)
			if (front.x(t, 0) >= l / 2 && md.get(front.x(t, 0), wy, front.z(t, 0)) != null
				&& md.get(front.x(t, 0), wy, front.z(t, 0)).is(ModWastes.BROKEN_MAKERS_WINDOW.get()))
				for (int y = wy; y < wy + 2; y++)
					front.remove(md, t, y, 0);
		for (int t = 0; t < east.n(); t++)
			for (int y = wy; y < wy + 2; y++)
				if (east.get(md, t, y, 0) != null && east.get(md, t, y, 0).is(ModWastes.BROKEN_MAKERS_WINDOW.get()))
					east.remove(md, t, y, 0);
		int fx = s.small() ? l / 2 : l - 3, ft = l - 1 - fx;
		if (c.outward(Direction.NORTH))
			Kit.sootFan(md, front, ft, wy + 2, 1, 2, 3, c.seed() + 1, Model.Cause.FIRE);
	}

	/**
	 * Ruined: the roof gone; the walls standing to their corbels at the heart end, falling on a stepped diagonal (a block a
	 * block) to the last bay's sills and the outer gable down to its plinth; the corner piers standing as stubs; the outer
	 * gable's blocks in a rubble cone outside it; the break line's top stones cracked.
	 */
	static void ruin(Model md, Ctx c, Spec s, Face front, Face east) {
		int l = s.l(), d = s.d(), e = s.e();
		int xb = Math.max(1, (int) Math.round(l * 0.4));
		int fallen = 0;
		java.util.List<int[]> breakTops = new java.util.ArrayList<>();
		for (var en : md.blocks().entrySet()) {
			BlockPos p = en.getKey();
			int x = p.getX(), y = p.getY(), z = p.getZ();
			if (y <= 0)
				continue;
			int top = x <= xb ? e : Math.max(x >= l - 1 ? 0 : 1, e - (x - xb));
			boolean cornerStub = (x == -1 || x == 0 || x == l - 1 || x == l) && (z == -1 || z == 0 || z == d - 1 || z == d) && y <= 3;
			if (y > top && !cornerStub) {
				md.remove(x, y, z);
				if (x >= l - 2)
					fallen++;
			}
		}
		// the break line's tops: cracked where the wall broke (its cause), every other one
		for (Face f : new Face[] { front, Kit.back(l, d) })
			for (int t = 0; t < f.n(); t++) {
				int x = f.x(t, 0);
				int top = x <= xb ? e : Math.max(x >= l - 1 ? 0 : 1, e - (x - xb));
				if (x > xb && top >= 1 && t % 2 == 0 && md.get(x, top, f.z(t, 0)) != null && md.get(x, top, f.z(t, 0)).is(c.m().wall().getBlock())) {
					md.set(x, top, f.z(t, 0), c.m().spalled());
					breakTops.add(new int[] { x, top, f.z(t, 0) });
				}
			}
		for (int[] b : breakTops)
			md.mark(Model.Cause.BREAK, b[0], b[1], b[2], 1);
		md.settle();
		Kit.rubbleCone(md, c.m(), east, 0, east.n() - 1, Math.max(fallen, 12), c.seed());
	}

	/** A walnut trapdoor stood open as a louvre (its panel on the side away from {@code facing}). */
	static BlockState louvre(Direction facing) {
		return Blocks.DARK_OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, facing).setValue(TrapDoorBlock.OPEN, true);
	}

	/** A truss across the shed at x: a walnut slab tie beam at the wall top, a walnut fence king post up to the ridge. */
	static void truss(Model md, int x, int d, int e, int zc, int hMax) {
		for (int z = 1; z <= d - 2; z++)
			md.set(x, e + 1, z, Kit.slab(Masonry.walnutSlab(), false));
		int top = hMax % 2 == 1 ? hMax / 2 : hMax / 2 - 1;
		for (int y = e + 2; y <= top; y++)
			md.setIfEmpty(x, y, zc, Masonry.walnutFence());
	}

	/** A Brass Plaque (D72) in front of the wall block at local (x, 1, z), its face toward {@code out}; askew from the damaged ring. */
	static void plaque(Model md, int x, int z, Direction out, boolean askew) {
		md.remove(x, 1, z);
		md.special(Map.of(new BlockPos(x, 1, z), ModWastes.BRASS_PLAQUE.get().defaultBlockState()), site -> site.plaque(x, 1, z, out, askew));
	}

	/**
	 * The lined cut (the hero join): a makers' doorway cut up to 2 x 3 at t, t + 1, its cut edges lined with Void-Alloy
	 * Pillar (jambs and a head), a stone or two spalled where the cut broke the bond; the plinth and step cut through.
	 */
	static void linedCut(Model md, Masonry m, Face f, int t, int wallDepth) {
		Direction.Axis along = f.along().getAxis();
		for (int y = 0; y <= 2; y++)
			for (int dd = 0; dd < wallDepth; dd++) {
				f.remove(md, t, y, -dd);
				f.remove(md, t + 1, y, -dd);
				f.set(md, t - 1, y, -dd, Kit.pillar());
				f.set(md, t + 2, y, -dd, Kit.pillar());
			}
		f.remove(md, t, 0, 1);
		f.remove(md, t + 1, 0, 1);
		for (int dt = -1; dt <= 2; dt++)
			for (int y = 1; y <= 2; y++)
				if (f.get(md, t + dt, y, 1) != null && f.get(md, t + dt, y, 1).getBlock() instanceof net.minecraft.world.level.block.StairBlock)
					f.remove(md, t + dt, y, 1);
		for (int dt = -1; dt <= 2; dt++)
			for (int dd = 0; dd < wallDepth; dd++)
				f.set(md, t + dt, 3, -dd, Kit.pillar(along));
		md.mark(Model.Cause.CUT, f.x(t, 0), 1, f.z(t, 0), 2);
		md.mark(Model.Cause.CUT, f.x(t + 1, 0), 1, f.z(t + 1, 0), 2);
		f.set(md, t + 3, 2, 0, m.spalled());
		f.set(md, t - 2, 3, 0, m.spalled());
	}
}
