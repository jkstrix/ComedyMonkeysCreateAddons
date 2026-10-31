package com.comedymonkey.createaddons.content.wastes.city.kit;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.wastes.District;
import com.comedymonkey.createaddons.content.wastes.city.CityPlan;
import com.comedymonkey.createaddons.content.wastes.city.kit.Kit.Ctx;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The building kit's catalogue (D71, design/briefs/clockwork-wastes-buildings.md, "The Catalogue"): every building type in
 * its states, each on its own pad, footprints 10 apart, in five rows running east, the camera side south. Every exhibit is
 * bound to a district of one test city (the catalogue's own record), so {@code /wastes wake all} and
 * {@code /wastes heart <minutes>} light every glow in it. Each exhibit's fronts face south (local north is world south,
 * local east is world east: a mirrored frame), its outer end east, the heart (for which faces count as outward and which way
 * fallen tops lie) stood in for to the north-west.
 */
public final class Catalogue {
	private Catalogue() {}

	/** Gap between footprints, and between rows' fronts (the deepest exhibit is about 40). */
	public static final int GAP = 10, ROW = 70;

	/** One piece of an exhibit: a model in a frame inside the exhibit's (identity, or a hut turned in a yard). */
	public record Part(Model model, int ox, int oz, Direction la, Direction lb) {
		static Part of(Model m) {
			return new Part(m, 0, 0, Direction.EAST, Direction.SOUTH);
		}

		/** A local cell of this part in the exhibit's frame. */
		int[] toExhibit(int x, int z) {
			return new int[] { ox + la.getStepX() * x + lb.getStepX() * z, oz + la.getStepZ() * x + lb.getStepZ() * z };
		}
	}

	interface Drawer {
		List<Part> draw(Ctx c, Map<String, BlockPos> anchorsLocal);
	}

	/** After an exhibit is put: its spans, given its site and every exhibit's named anchors (world positions). */
	interface After {
		void tie(Site site, Map<String, BlockPos> anchors);
	}

	record Exhibit(String id, String label, int row, @Nullable District district, Masonry m, Wear wear, Drawer draw, @Nullable After after) {}

	/** Where an exhibit stands in the world (its blocks' box, its floor y) and what it is: for the camera marks and the tests. */
	public record Placed(String id, String label, int row, Wear wear, @Nullable District district, BlockPos min, BlockPos max, List<Model> models) {
		public int height() {
			return max.getY() - min.getY() + 1;
		}
	}

	public record Built(CityPlan plan, List<Placed> placed, BlockPos recordOrigin, int minX, int maxX, int minZ, int maxZ) {}

	static final Set<Direction> OUT = EnumSet.of(Direction.EAST, Direction.NORTH);

	static Exhibit shed(String id, String label, District d, Masonry m, Wear w, Shed.Spec spec) {
		return new Exhibit(id, label, 0, d, m, w, (c, a) -> List.of(Part.of(Shed.build(c, spec))), null);
	}

	/** The catalogue's rows (the brief's table), in order. */
	static List<Exhibit> exhibits() {
		List<Exhibit> out = new ArrayList<>();
		Shed.Spec std = new Shed.Spec(13, 10, 5, Shed.Door.LINED, true, true);
		Shed.Spec plain = new Shed.Spec(13, 10, 5, Shed.Door.WALNUT, false, false);
		// row 1: the makers' sheds
		out.add(shed("shed_foundry", "Makers' shed, Foundry stone, intact", District.FOUNDRY, Masonry.foundry(), Wear.INTACT, std));
		out.add(shed("shed_workshops", "Makers' shed, Workshops stone, intact (walnut door, unused)", District.WORKSHOPS, Masonry.workshops(), Wear.INTACT, plain));
		out.add(shed("shed_rail_yard", "Makers' shed, Rail Yard stone, intact", District.RAIL_YARD, Masonry.railYard(), Wear.INTACT, std));
		out.add(shed("shed_quarters", "Makers' shed, Quarters stone, intact", District.QUARTERS, Masonry.quarters(), Wear.INTACT, std));
		out.add(shed("shed_worn", "Makers' shed, worn", District.WORKSHOPS, Masonry.workshops(), Wear.WORN, std));
		out.add(shed("shed_damaged", "Makers' shed, damaged", District.WORKSHOPS, Masonry.workshops(), Wear.DAMAGED, std));
		out.add(shed("shed_ruined", "Makers' shed, ruined", District.WORKSHOPS, Masonry.workshops(), Wear.RUINED, new Shed.Spec(13, 10, 5, Shed.Door.LINED, false, false)));
		out.add(new Exhibit("control_hut", "The yard's control hut", 0, District.WORKSHOPS, Masonry.workshops(), Wear.INTACT,
			(c, a) -> List.of(Part.of(Shed.build(c, new Shed.Spec(4, 4, 2, Shed.Door.WALNUT, false, false)))), null));
		out.add(new Exhibit("signal_box", "The Rail Yard's signal box", 0, District.RAIL_YARD, Masonry.railYard(), Wear.INTACT,
			(c, a) -> List.of(Part.of(Shed.build(c, new Shed.Spec(5, 4, 3, Shed.Door.WALNUT, false, false)))), null));
		// row 2: the makers' set pieces
		for (Wear w : new Wear[] { Wear.WORN, Wear.DAMAGED, Wear.RUINED })
			out.add(new Exhibit("furnace_" + w.name().toLowerCase(), "Furnace shed, chimney and blast furnace, " + w.name().toLowerCase(), 1, District.FOUNDRY,
				Masonry.foundry(), w, (c, a) -> List.of(Part.of(FurnaceShed.build(c))), null));
		for (Wear w : new Wear[] { Wear.INTACT, Wear.DAMAGED, Wear.RUINED })
			out.add(new Exhibit("terrace_" + w.name().toLowerCase(), "Terrace of four, " + w.name().toLowerCase(), 1, District.QUARTERS, Masonry.quarters(), w,
				(c, a) -> List.of(Part.of(Terrace.build(c, 4))), null));
		for (Wear w : new Wear[] { Wear.INTACT, Wear.DAMAGED, Wear.RUINED })
			out.add(new Exhibit("workshop_" + w.name().toLowerCase(), "Long workshop, " + w.name().toLowerCase(), 1, District.WORKSHOPS, Masonry.workshops(), w,
				(c, a) -> List.of(Part.of(Workshop.build(c))), null));
		// row 3: the uprising
		for (Wear w : new Wear[] { Wear.INTACT, Wear.DAMAGED, Wear.RUINED })
			out.add(new Exhibit("tower3_" + w.name().toLowerCase(), "Slotted tower 3 x 3 on old foundations, " + w.name().toLowerCase(), 2, District.WORKSHOPS,
				Masonry.workshops(), w, (c, a) -> List.of(Part.of(Tower.build(c, 3, 22, Tower.Footing.PLINTH))), null));
		out.add(new Exhibit("tower5_roof", "Slotted tower 5 x 5 rising through a shed roof", 2, District.WORKSHOPS, Masonry.workshops(), Wear.INTACT,
			(c, a) -> List.of(Part.of(Tower.throughRoof(c, 26))), null));
		out.add(new Exhibit("sky_tower", "Sky tower with its T-head, spans to a dummy mast", 2, District.WORKSHOPS, Masonry.workshops(), Wear.WORN, (c, a) -> {
			Tower.SkyTower t = Tower.sky(c, 24);
			Model mast = new Model();
			a.put("mastIns", Yard.lattice(mast, 0, 0, true, Direction.NORTH).offset(-16, 0, 1));
			a.put("head", t.head());
			return List.of(Part.of(t.model()), new Part(mast, -16, 1, Direction.EAST, Direction.SOUTH));
		}, (site, anchors) -> {
			BlockPos head = anchors.get("sky_tower.head"), mast = anchors.get("sky_tower.mastIns");
			site.plan.span(head, mast, 1, false);
			site.plan.hop(mast, 1);
			site.plan.hop(head, 2);
		}));
		for (Wear w : new Wear[] { Wear.WORN, Wear.DAMAGED })
			out.add(new Exhibit("vault_" + w.name().toLowerCase(), "Vault house, " + w.name().toLowerCase(), 2, District.FOUNDRY, Masonry.foundry(), w,
				(c, a) -> List.of(Part.of(VaultHouse.build(c))), null));
		out.add(new Exhibit("alcove_wall", "Alcove wall: three tiers, switchback ramp, lift, trinkets and vines", 2, District.QUARTERS, Masonry.quarters(), Wear.WORN,
			(c, a) -> List.of(Part.of(AlcoveWall.build(c, 16, 3))), null));
		out.add(new Exhibit("split_tower", "The split alcove tower", 2, District.QUARTERS, Masonry.quarters(), Wear.DAMAGED,
			(c, a) -> List.of(Part.of(AlcoveWall.split(c))), null));
		for (Wear w : new Wear[] { Wear.INTACT, Wear.DAMAGED })
			out.add(new Exhibit("gantry_" + w.name().toLowerCase(), "Assembly gantry, " + (w == Wear.INTACT ? "intact with its five clamps" : "damaged (buckled leg)"), 2,
				District.WORKSHOPS, Masonry.workshops(), w, (c, a) -> List.of(Part.of(Gantry.build(c))), null));
		// row 4: the yard and the heart
		out.add(new Exhibit("yard", "Substation yard, its lattice mast and riser", 3, District.WORKSHOPS, Masonry.workshops(), Wear.WORN, (c, a) -> {
			Yard.Built y = Yard.build(c);
			a.put("mastIns", y.mastInsulator());
			a.put("sub", y.substation());
			return List.of(Part.of(y.model()), new Part(y.hut(), 4, 1, Direction.WEST, Direction.SOUTH));
		}, (site, anchors) -> {
			BlockPos mast = anchors.get("yard.mastIns"), sub = anchors.get("yard.sub");
			site.plan.span(mast, sub, 0, false);
			site.plan.hop(mast, 1);
		}));
		out.add(new Exhibit("corner_mast", "Corner mast at a mock crossroads, its street cables", 3, District.WORKSHOPS, Masonry.workshops(), Wear.INTACT, (c, a) -> {
			Model md = new Model();
			a.put("ins", Yard.cornerMast(md, 14));
			return List.of(Part.of(md));
		}, (site, anchors) -> {
			BlockPos ins = anchors.get("corner_mast.ins"), yard = anchors.get("yard.mastIns");
			if (yard != null)
				site.plan.span(ins, yard, 1, false);
			site.plan.hop(ins, 2);
		}));
		out.add(new Exhibit("canopy", "Rail Yard platform canopy", 3, District.RAIL_YARD, Masonry.railYard(), Wear.INTACT,
			(c, a) -> List.of(Part.of(Canopy.build(c, 25))), null));
		out.add(new Exhibit("drum", "A quarter of the heart drum: two flats, three buttresses, a heart portal, two ribs (one snapped)", 3, District.HEART,
			Masonry.quarters(), Wear.INTACT, (c, a) -> List.of(Part.of(Drum.build(c, (x, z) -> z <= -11 && x >= -15, EnumSet.of(Direction.NORTH), -8))), null));
		out.add(new Exhibit("spire", "The spire, full height, on a drum stub", 3, District.HEART, Masonry.quarters(), Wear.INTACT, (c, a) -> {
			Drum.Spire s = Drum.spire(Drum.SPIRE_BASE);
			Model md = s.model();
			stub(md);
			for (int i = 0; i < 4; i++)
				a.put("crown" + i, s.crown()[i]);
			return List.of(Part.of(md));
		}, (site, anchors) -> {
			for (int i = 0; i < 4; i++) {
				BlockPos p = anchors.get("spire.crown" + i);
				site.plan.snapped(p, site.w(new Direction[] { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST }[i]).toYRot());
			}
		}));
		// row 5: the outskirts
		out.add(new Exhibit("siege", "Siege-works salient: strongpoint, shells, a breach and its scorch on the city side", 4, District.QUARTERS, Masonry.workshops(),
			Wear.RUINED, (c, a) -> List.of(Part.of(Siege.build(c, 7))), null));
		out.add(shed("outskirts_shed", "A ruined shed, as the outskirts hold them", District.RAIL_YARD, Masonry.railYard(), Wear.RUINED,
			new Shed.Spec(9, 6, 4, Shed.Door.WALNUT, false, false)));
		out.set(out.size() - 1, withRow(out.get(out.size() - 1), 4));
		out.add(new Exhibit("outskirts_stub", "A ruined tower stub, as the outskirts hold them", 4, District.RAIL_YARD, Masonry.railYard(), Wear.RUINED,
			(c, a) -> List.of(Part.of(Tower.build(c, 3, 18, Tower.Footing.PLINTH))), null));
		return out;
	}

	static Exhibit withRow(Exhibit e, int row) {
		return new Exhibit(e.id(), e.label(), row, e.district(), e.m(), e.wear(), e.draw(), e.after());
	}

	/** A drum stub for the spire to rise out of: a ring of the drum's wall (stained brick on its plinth) round the collar, to y 13. */
	static void stub(Model md) {
		for (int x = -5; x <= 5; x++)
			for (int z = -5; z <= 5; z++) {
				boolean ring = Math.abs(x) >= 4 || Math.abs(z) >= 4;
				if (!ring)
					continue;
				for (int y = 0; y <= Drum.TOP; y++)
					md.set(x, y, z, y <= 1 ? Drum.dress() : y == 9 || y == 3 ? Drum.dress() : Drum.brick());
			}
		for (int x = -6; x <= 6; x++)
			for (int z = -6; z <= 6; z++)
				if (Math.abs(x) == 6 || Math.abs(z) == 6) {
					md.set(x, 0, z, Drum.dress());
					md.set(x, 1, z, Drum.dress());
					Direction in = Math.abs(x) == 6 ? (x > 0 ? Direction.WEST : Direction.EAST) : (z > 0 ? Direction.NORTH : Direction.SOUTH);
					md.set(x, 2, z, Kit.stairs(Drum.dressStairs(), in, false));
					md.set(x, Drum.TOP, z, Kit.stairs(Drum.dressStairs(), in, true));
					md.set(x, Drum.TOP + 1, z, com.comedymonkey.createaddons.registry.ModWastes.STAINED_END_STONE_BRICK_WALL.get().defaultBlockState());
				}
		// the roof over the stub (the drum's top) the collar stands on
		for (int x = -3; x <= 3; x++)
			for (int z = -3; z <= 3; z++)
				if (Math.abs(x) == 3 || Math.abs(z) == 3)
					for (int y = 2; y < Drum.SPIRE_BASE; y++)
						md.setIfEmpty(x, y, z, Drum.brick());
		// the spire's shaft is hollow above the collar; below it, inside the stub, its legs (as in the hall)
		for (int[] l : new int[][] { { -2, -2 }, { 2, -2 }, { -2, 2 }, { 2, 2 } })
			for (int y = 0; y < Drum.SPIRE_BASE; y++)
				md.set(l[0], y, l[1], Kit.pillar());
	}

	/**
	 * The catalogue's plan beside {@code o} (the floor at o's y): rows going north from o, each exhibit's front on its row's
	 * line, the camera side south. Returns the plan and where every exhibit stands. Bound to the record at the catalogue's
	 * middle ({@code recordOrigin}).
	 */
	public static Built plan(BlockPos o, long seed) {
		List<Exhibit> exhibits = exhibits();
		// first lay them out (bounds from their models), so the record's origin can sit in the middle
		record Laid(Exhibit e, List<Part> parts, Map<String, BlockPos> anchors, int minX, int maxX, int minZ, int maxZ, int minY, int maxY) {}
		List<Laid> laid = new ArrayList<>();
		for (Exhibit e : exhibits) {
			Ctx c = new Ctx(e.m(), e.wear(), OUT, seed + e.id().hashCode());
			Map<String, BlockPos> anchors = new LinkedHashMap<>();
			List<Part> parts = e.draw().draw(c, anchors);
			int[] b = { Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE };
			for (Part p : parts)
				for (BlockPos q : p.model().blocks().keySet()) {
					int[] xz = p.toExhibit(q.getX(), q.getZ());
					b[0] = Math.min(b[0], xz[0]);
					b[1] = Math.max(b[1], xz[0]);
					b[2] = Math.min(b[2], xz[1]);
					b[3] = Math.max(b[3], xz[1]);
					b[4] = Math.min(b[4], q.getY());
					b[5] = Math.max(b[5], q.getY());
				}
			laid.add(new Laid(e, parts, anchors, b[0], b[1], b[2], b[3], b[4], b[5]));
		}
		int[] cursor = new int[5];
		int maxLen = 0;
		Map<Laid, BlockPos> origins = new HashMap<>();
		for (Laid l : laid) {
			int row = l.e().row();
			int front = o.getZ() - ROW * row;
			// local x -> world +x; local z -> world -z (front, local -z, faces world south): world z = origin z - local z
			BlockPos origin = new BlockPos(o.getX() + cursor[row] - l.minX(), o.getY(), front + l.minZ());
			origins.put(l, origin);
			cursor[row] += l.maxX() - l.minX() + 1 + GAP;
			maxLen = Math.max(maxLen, cursor[row]);
		}
		BlockPos record = new BlockPos(o.getX() + maxLen / 2, o.getY(), o.getZ() - ROW * 2);
		BlockPos heart = new BlockPos(o.getX() - 1000, o.getY(), o.getZ() - 1500);
		List<Placed> placed = new ArrayList<>();
		Map<String, BlockPos> anchors = new HashMap<>();
		int[] box = { Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE };
		CityPlan plan = CityPlan.drawn(record, seed, p -> {
			List<Site> sites = new ArrayList<>();
			for (Laid l : laid) {
				Site site = new Site(p, origins.get(l), Direction.EAST, Direction.NORTH, heart, l.e().district());
				sites.add(site);
				// the pad of Scorched End Stone under it, 2 round its blocks
				for (int x = l.minX() - 2; x <= l.maxX() + 2; x++)
					for (int z = l.minZ() - 2; z <= l.maxZ() + 2; z++)
						site.put(x, -1, z, com.comedymonkey.createaddons.registry.ModWastes.SCORCHED_END_STONE.get().defaultBlockState());
				List<Model> models = new ArrayList<>();
				for (Part part : l.parts()) {
					Site s = part.ox() == 0 && part.oz() == 0 && part.la() == Direction.EAST && part.lb() == Direction.SOUTH ? site
						: site.sub(part.ox(), part.oz(), part.la(), part.lb());
					part.model().emit(s);
					models.add(part.model());
				}
				// anchors are drawn in the exhibit's own frame
				for (var en : l.anchors().entrySet())
					anchors.put(l.e().id() + "." + en.getKey(), site.at(en.getValue().getX(), en.getValue().getY(), en.getValue().getZ()));
				BlockPos a = site.at(l.minX(), l.minY(), l.minZ()), b = site.at(l.maxX(), l.maxY(), l.maxZ());
				BlockPos min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
				BlockPos max = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
				placed.add(new Placed(l.e().id(), l.e().label(), l.e().row(), l.e().wear(), l.e().district(), min, max, models));
				box[0] = Math.min(box[0], min.getX() - 3);
				box[1] = Math.max(box[1], max.getX() + 3);
				box[2] = Math.min(box[2], min.getZ() - 3);
				box[3] = Math.max(box[3], max.getZ() + 3);
				p.buildings.add(placed.get(placed.size() - 1));
			}
			for (int i = 0; i < laid.size(); i++) {
				After after = laid.get(i).e().after();
				Site s = sites.get(i);
				if (after != null)
					s.bound(() -> after.tie(s, anchors));
			}
		});
		return new Built(plan, placed, record, box[0], box[1], box[2], box[3]);
	}
}
