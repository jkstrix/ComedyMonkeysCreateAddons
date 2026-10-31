package com.comedymonkey.createaddons.content.wastes.city.kit;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.comedymonkey.createaddons.content.wastes.DerelictInsulatorBlock;
import com.comedymonkey.createaddons.content.wastes.District;
import com.comedymonkey.createaddons.content.wastes.city.CityPlan;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The swap (D71: "approved buildings then replace the boxes in the city"): every building the city's boxes stood for,
 * drawn from the kit in the same lots, each in the state its ring gives it ({@link Wear#forRing}, of its outer end), its
 * outer end and outward faces worked out from the heart. {@code CityLayout} calls these in place of its boxes when
 * {@code CityLayout.BUILDING_KIT} is on (off until the user approves the catalogue). Lot rectangles are in a district's
 * frame (u out along x, v out along z, from 4 blocks off the axes), as {@code CityLayout} lays them out.
 */
public final class KitCity {
	private KitCity() {}

	/** A district's frame (as {@code CityLayout.Q}). */
	public record Frame(CityPlan p, District d, int sx, int sz) {
		public BlockPos at(int u, int y, int v) {
			return p.at(sx * (4 + u), y, sz * (4 + v));
		}

		public Direction U() {
			return sx > 0 ? Direction.EAST : Direction.WEST;
		}

		public Direction V() {
			return sz > 0 ? Direction.SOUTH : Direction.NORTH;
		}

		/** The frame's (u, v) of a world block. */
		int[] uv(BlockPos w) {
			return new int[] { sx * (w.getX() - p.origin.getX()) - 4, sz * (w.getZ() - p.origin.getZ()) - 4 };
		}
	}

	/** A kit building put into the city: its lot (in its frame), its site, its model; the plan's ops it put (from, to). */
	public record Placement(String what, Frame f, int u0, int v0, int u1, int v1, Site site, Model model, int from, int to) {}

	static Site site(Frame f, int u, int v, Direction a, Direction b) {
		return new Site(f.p(), f.at(u, 0, v), a, b, f.p().origin, f.d());
	}

	static Kit.Ctx ctx(Frame f, Site s, Wear wear, long salt) {
		Set<Direction> out = EnumSet.noneOf(Direction.class);
		for (Direction d : Direction.Plane.HORIZONTAL)
			if (s.outward(d))
				out.add(d);
		return new Kit.Ctx(Masonry.of(f.d()), wear, out, f.p().seed * 31 + salt);
	}

	/** The ring of the farthest of the given frame cells (a building's outer end). */
	static Wear wear(Frame f, int u, int v) {
		BlockPos w = f.at(u, 0, v);
		return Wear.forRing(Wear.ring(w.getX() - f.p().origin.getX(), w.getZ() - f.p().origin.getZ()));
	}

	static Placement put(String what, Frame f, int u0, int v0, int u1, int v1, Site s, Model md) {
		int from = f.p().ops().size();
		md.emit(s);
		Placement pl = new Placement(what, f, u0, v0, u1, v1, s, md, from, f.p().ops().size());
		f.p().buildings.add(pl);
		return pl;
	}

	// ================================================================ the district's buildings

	/** The Substation yard (u, v 19-29; gate to street A): returns the chain so far (the Substation, the mast's insulator). */
	public static BlockPos[] yard(Frame f) {
		Site s = site(f, 29, 29, f.U().getOpposite(), f.V().getOpposite());
		Kit.Ctx c = ctx(f, s, wear(f, 29, 29).atMost(Wear.WORN), 11);
		Yard.Built y = Yard.build(c);
		put("yard", f, 19, 19, 29, 29, s, y.model());
		put("control hut", f, 19, 19, 29, 29, Yard.hutSite(s), y.hut());
		BlockPos m = y.mastInsulator();
		return new BlockPos[] { s.at(y.substation().getX(), 0, y.substation().getZ()), s.at(m.getX(), m.getY(), m.getZ()) };
	}

	/** The sky tower (5 x 5 at u 11-15, v 22-26, on its 7 x 7 plinth): returns its T-head's insulator (the sky-cable's). */
	public static BlockPos skyTower(Frame f) {
		Site s = site(f, 11, 22, f.U(), f.V());
		Kit.Ctx c = ctx(f, s, wear(f, 16, 27).atMost(Wear.WORN), 12);
		Tower.SkyTower t = Tower.sky(c, 24);
		put("sky tower", f, 0, 19, 18, 29, s, t.model());
		return s.at(t.head().getX(), t.head().getY(), t.head().getZ());
	}

	/** The vault house (u 20-28, v 4-12, its portal toward the boulevard). */
	public static void vaultHouse(Frame f) {
		Site s = site(f, 20, 4, f.U(), f.V());
		put("vault house", f, 18, 0, 29, 18, s, VaultHouse.build(ctx(f, s, wear(f, 29, 13), 13)));
	}

	/** Lot A's two sheds and lot N1's small shed (their machines and shells are CityLayout's, at their kit places). */
	public static void lotA(Frame f) {
		// shed 1 (walls u 2-13, v 37-49), the uprising's open lined cut toward the heart
		Site s1 = site(f, 2, 37, f.V(), f.U());
		put("shed 1", f, 0, 35, 29, 66, s1, Shed.build(ctx(f, s1, wear(f, 13, 49), 21), new Shed.Spec(13, 12, 5, Shed.Door.LINED, false, true)));
		// shed 2 (walls u 17-28, v 40-60) behind its locked Sliding Door
		Site s2 = site(f, 18, 40, f.V(), f.U());
		put("shed 2", f, 0, 35, 29, 66, s2, Shed.build(ctx(f, s2, wear(f, 27, 60), 22), new Shed.Spec(21, 10, 5, Shed.Door.SLIDING, true, false)));
		// the small shed (walls u 2-5, v 22-26), clear of the sky tower's plinth
		Site s3 = site(f, 2, 22, f.V(), f.U());
		put("small shed", f, 0, 19, 8, 29, s3, Shed.build(ctx(f, s3, wear(f, 5, 26), 23), new Shed.Spec(5, 4, 3, Shed.Door.WALNUT, false, false)));
	}

	/** The district's corner mast (D74: at CORNER_MAST by the street crossing), a 3 x 3 lattice (D73), its riser on the street side. */
	public static BlockPos cornerMast(Frame f, BlockPos yardIns, boolean onBeadPath) {
		Site s = site(f, 35, 27, f.U(), f.V());
		Model md = new Model();
		BlockPos ins = Yard.lattice(md, 0, 0, true, Direction.SOUTH);
		put("corner mast", f, 34, 26, 38, 30, s, md); // its cross-arm reaches over the gutter
		BlockPos w = s.at(ins.getX(), ins.getY(), ins.getZ());
		f.p().span(w, yardIns, onBeadPath ? 1 : -1, false);
		f.p().hop(w, onBeadPath ? 2 : 0);
		return w;
	}

	// ================================================================ the set pieces' buildings

	/** The Foundry's furnace shed, chimney and blast furnace in lot B (its casting floor across street A in lot C). */
	public static void furnaceShed(Frame f) {
		Site s = site(f, 42, 22, f.U(), f.V().getOpposite());
		// its toppled chimney lies across street B
		put("furnace shed", f, 30, 0, 66, 29, s, FurnaceShed.build(ctx(f, s, wear(f, 66, 29), 31)));
	}

	/** The Workshops' two long sheds in lot B (their loading doors toward street B). */
	public static void workshops(Frame f) {
		for (int i = 0; i < 2; i++) {
			Site s = site(f, 36, 2 + 13 * i, f.U(), f.V());
			put("long workshop " + (i + 1), f, 35, 0, 66, 26, s, Workshop.build(ctx(f, s, wear(f, 65, 13 + 13 * i), 32 + i)));
		}
	}

	/** The Workshops' assembly gantry over its five clamps in lot C (legs at u 39-40 and 62-63, the clamps at v 52). */
	public static void gantry(Frame f) {
		Site s = site(f, 39, 50, f.U(), f.V());
		put("assembly gantry", f, 35, 35, 66, 66, s, Gantry.build(ctx(f, s, wear(f, 63, 56), 34)));
	}

	/** The Rail Yard's platform canopy (u 36-64, v 50-53) and its signal box in lot B. */
	public static void railYard(Frame f) {
		Site s = site(f, 36, 50, f.U(), f.V());
		put("platform canopy", f, 35, 35, 66, 66, s, Canopy.build(ctx(f, s, wear(f, 64, 53), 35), 29, false));
		Site b = site(f, 38, 3, f.U(), f.V());
		put("signal box", f, 35, 0, 66, 29, b, Shed.build(ctx(f, b, wear(f, 42, 6), 36), new Shed.Spec(5, 4, 3, Shed.Door.WALNUT, false, false)));
	}

	/** The Quarters' terrace of four (lot B), the alcove wall with its ramps and lift and the split tower (lot C). */
	public static void quarters(Frame f) {
		Site t = site(f, 38, 6, f.U(), f.V());
		put("terrace", f, 35, 0, 66, 29, t, Terrace.build(ctx(f, t, wear(f, 59, 12), 41), 4));
		Site w = site(f, 38, 41, f.U(), f.V().getOpposite());
		put("alcove wall", f, 35, 35, 66, 66, w, AlcoveWall.build(ctx(f, w, wear(f, 57, 41), 42), 16, 3));
		Site sp = site(f, 59, 61, f.U(), f.V());
		put("split alcove tower", f, 35, 35, 66, 66, sp, AlcoveWall.split(ctx(f, sp, Wear.DAMAGED, 43)));
	}

	// ================================================================ the heart

	/** The drum (the whole octagon, doors north, east and west, pilasters inside) and the spire (its crown's insulators the districts'). */
	public static void heart(CityPlan p, int spireZ) {
		Site s = new Site(p, p.origin, Direction.EAST, Direction.SOUTH, p.origin, District.HEART);
		Kit.Ctx c = new Kit.Ctx(Masonry.quarters(), Wear.INTACT, EnumSet.allOf(Direction.class), p.seed);
		Model drum = Drum.build(c, (x, z) -> true, EnumSet.of(Direction.NORTH, Direction.EAST, Direction.WEST), spireZ);
		// inside: void-alloy pilasters clamped to the hall wall under the wall lamps and either side of the vents
		for (int sx = -1; sx <= 1; sx += 2)
			for (int z : new int[] { -4, 4 }) {
				for (int y = 0; y <= 3; y++)
					drum.set(sx * 15, y, z, Kit.pillar());
				drum.set(sx * 15, 3, z + 1, Kit.clampWall(sx > 0 ? Direction.WEST : Direction.EAST));
			}
		// the places the hall's own pieces go into the wall: the eight vents and the heart vault's surround
		for (int sx = -1; sx <= 1; sx += 2)
			for (int sz = -1; sz <= 1; sz += 2) {
				drum.remove(sx * 16, 0, sz * 9);
				drum.remove(sx * 9, 0, sz * 16);
			}
		drum.clear(-1, 0, 16, 1, 5, 16);
		Frame hf = new Frame(p, District.HEART, 1, 1);
		int from = p.ops().size();
		drum.emit(s);
		p.buildings.add(new Placement("heart drum", hf, -99, -99, 99, 99, s, drum, from, p.ops().size()));
		Site sp = new Site(p, p.at(0, 0, spireZ), Direction.EAST, Direction.SOUTH, p.origin, District.HEART);
		Model spire = Drum.spire(Drum.SPIRE_BASE, false).model();
		from = p.ops().size();
		spire.emit(sp);
		p.buildings.add(new Placement("spire", hf, -99, -99, 99, 99, sp, spire, from, p.ops().size()));
	}

	// ================================================================ the outskirts' siege works

	/**
	 * The resistance's siege works at {@code ring} round the city (D73, type 13): salients along each side of the ring,
	 * their cover facing the city, clear of the boulevards' bridges; everything in them only where there's ground.
	 */
	public static void siege(CityPlan p, int ring) {
		int i = 0;
		for (Direction out : Direction.Plane.HORIZONTAL)
			for (int along : new int[] { -66, -38, 8, 32 }) {
				// the side's line: out from the middle by ring; local +x along the side, local +z outward (away from the city)
				Direction a = out.getCounterClockWise();
				BlockPos o = p.origin.offset(out.getStepX() * ring + a.getStepX() * along, 0, out.getStepZ() * ring + a.getStepZ() * along);
				Site s = new Site(p, o, a, out, p.origin, null);
				Kit.Ctx c = new Kit.Ctx(Masonry.workshops(), Wear.RUINED, EnumSet.of(Direction.NORTH), p.seed + 50 + i++);
				Model md = Siege.build(c, 7);
				int from = p.ops().size();
				// outskirts: on ground only (CityPlan.put does this outside the core)
				md.emit(s);
				p.buildings.add(new Placement("siege salient", new Frame(p, null, 1, 1), -999, -999, 999, 999, s, md, from, p.ops().size()));
			}
	}

	// ================================================================ the checks

	/**
	 * What's wrong with the kit in a plan (empty: nothing): a building's block outside its lot, or a later op of the city's
	 * putting something into one of a building's blocks (a machine in a pier, a lamp in an eave).
	 */
	public static List<String> problems(CityPlan p) {
		List<String> out = new ArrayList<>();
		Map<BlockPos, String> owner = new HashMap<>();
		int lastBuilding = 0;
		for (Object o : p.buildings) {
			if (!(o instanceof Placement pl))
				continue;
			lastBuilding = Math.max(lastBuilding, pl.to());
			for (var e : pl.model().blocks().entrySet()) {
				BlockPos q = e.getKey();
				BlockPos w = pl.site().at(q.getX(), q.getY(), q.getZ());
				if (pl.f().d() != null && pl.u0() > -99) {
					int[] uv = pl.f().uv(w);
					if (uv[0] < pl.u0() || uv[0] > pl.u1() || uv[1] < pl.v0() || uv[1] > pl.v1())
						out.add(pl.f().d() + " " + pl.what() + ": " + e.getValue().getBlock() + " at u " + uv[0] + ", v " + uv[1] + " is outside its lot");
				}
				if (q.getY() >= 0)
					owner.put(w, pl.f().d() + " " + pl.what());
			}
		}
		// every op after a building's own that lands in one of its blocks (cables come last: they're worked out after)
		List<CityPlan.Op> ops = p.ops();
		java.util.Set<Integer> own = new java.util.HashSet<>();
		for (Object o : p.buildings)
			if (o instanceof Placement pl)
				for (int i = pl.from(); i < pl.to(); i++)
					own.add(i);
		for (int i = 0; i < ops.size(); i++) {
			if (own.contains(i) || !(ops.get(i) instanceof CityPlan.Op.Put put))
				continue;
			// cables are worked out last, the kit's own among them (and the streets' stay in the streets)
			if (put.state().getBlock() instanceof com.comedymonkey.createaddons.content.wastes.DerelictCableBlock)
				continue;
			String who = owner.get(put.at());
			if (who == null)
				continue;
			// a put before the buildings is overwritten by them; only later ones matter
			if (i < firstOwn(p, put.at()))
				continue;
			BlockState s = put.state();
			out.add(s.getBlock() + " at " + put.at().subtract(p.origin).toShortString() + " is put into " + who);
		}
		return out;
	}

	/** The first op index of the building that holds this block. */
	static int firstOwn(CityPlan p, BlockPos w) {
		for (Object o : p.buildings)
			if (o instanceof Placement pl)
				for (BlockPos q : pl.model().blocks().keySet())
					if (pl.site().at(q.getX(), q.getY(), q.getZ()).equals(w))
						return pl.from();
		return Integer.MAX_VALUE;
	}

	/** For the crown insulators: the spire's arm tips (as {@code CityLayout.crownInsulator}). */
	static DerelictInsulatorBlock.Mount crownMount() {
		return DerelictInsulatorBlock.Mount.HANGING;
	}
}
