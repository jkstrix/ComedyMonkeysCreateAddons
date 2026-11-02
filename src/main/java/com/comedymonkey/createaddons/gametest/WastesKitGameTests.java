package com.comedymonkey.createaddons.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.wastes.CityDecorLightBlock;
import com.comedymonkey.createaddons.content.wastes.CityLink;
import com.comedymonkey.createaddons.content.wastes.CityRecord;
import com.comedymonkey.createaddons.content.wastes.District;
import com.comedymonkey.createaddons.content.wastes.ScarredPlateBlock;
import com.comedymonkey.createaddons.content.wastes.city.CityPlan;
import com.comedymonkey.createaddons.content.wastes.city.kit.Catalogue;
import com.comedymonkey.createaddons.content.wastes.city.kit.CatalogueBuild;
import com.comedymonkey.createaddons.content.wastes.city.kit.Kit;
import com.comedymonkey.createaddons.content.wastes.city.kit.KitCity;
import com.comedymonkey.createaddons.content.wastes.city.kit.Masonry;
import com.comedymonkey.createaddons.content.wastes.city.kit.Model;
import com.comedymonkey.createaddons.content.wastes.city.kit.ModelChecks;
import com.comedymonkey.createaddons.content.wastes.city.kit.Tower;
import com.comedymonkey.createaddons.content.wastes.city.kit.Wear;
import com.comedymonkey.createaddons.content.wastes.city.kit.Yard;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Clockwork Wastes building kit (D71, D73, design/briefs/clockwork-wastes-buildings.md, "Tests over checks"): every
 * building type in every state the catalogue holds stands up (no block left hanging), keeps the anti-monoblock rule
 * ({@link ModelChecks#MAX_SHARE}), uses none of the excluded blocks, puts every cracked, spalled and soot block near its
 * cause, and is damaged only as its ring allows; the swap's buildings stay inside their lots and leave the city's own
 * pieces where they were; and the catalogue builds into a world with every city block bound and lighting when fed.
 */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WastesKitGameTests {
	private static final String COURSE = "trade_bot_course";
	/** The cities the swap's checks draw: several seeds and origins (the kit's dice and the plan's both change with them). */
	private static final long[] SEEDS = { 0, 1, 12345 };
	private static final BlockPos[] ORIGINS = { new BlockPos(1000, 70, -2000), new BlockPos(-37_000, 64, 52_000) };

	private WastesKitGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(WastesKitGameTests.class);
	}

	private static void check(boolean ok, String message) {
		if (!ok)
			throw new GameTestAssertException(message);
	}

	private static void none(List<String> problems, String what) {
		if (!problems.isEmpty())
			try {
				java.nio.file.Files.write(java.nio.file.Path.of("kit-" + what.replaceAll("[^a-z]+", "-") + ".txt"), problems);
			} catch (java.io.IOException ignored) {
			}
		if (!problems.isEmpty())
			throw new GameTestAssertException(problems.size() + " " + what + ": " + String.join("; ", problems.subList(0, Math.min(12, problems.size()))));
	}

	/**
	 * Every exhibit of the catalogue (every type in each of its states): nothing hangs in the air, no block makes up more than
	 * the agreed share of a face, no excluded block, every mark near its cause.
	 */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void everyKitBuildingKeepsTheGrammar(GameTestHelper helper) {
		Catalogue.Built b = Catalogue.plan(new BlockPos(-40_000, 90, -40_000), 0);
		check(b.placed().size() >= 35, "only " + b.placed().size() + " exhibits");
		List<String> floating = new ArrayList<>(), mono = new ArrayList<>(), excluded = new ArrayList<>(), marks = new ArrayList<>();
		for (Catalogue.Placed p : b.placed())
			for (Model md : p.models()) {
				for (BlockPos f : ModelChecks.floating(md))
					floating.add(p.id() + " " + md.get(f.getX(), f.getY(), f.getZ()).getBlock() + " at " + f.toShortString());
				// the siege works are a low line of cover, not a building with faces: the rule is for buildings
				if (!p.id().equals("siege") && !p.id().equals("corner_mast"))
					for (ModelChecks.Share s : ModelChecks.elevations(md))
						// a lattice mast is an open frame, not a wall
						if (s.share() > ModelChecks.MAX_SHARE && s.seen() > 20 && !net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.block()).getPath().contains("girder"))
							mono.add(p.id() + " " + s.side() + " " + s.block() + " " + Math.round(s.share() * 100) + "%");
				for (var e : md.blocks().entrySet())
					if (ModelChecks.excluded(e.getValue()))
						excluded.add(p.id() + " " + e.getValue().getBlock() + " at " + e.getKey().toShortString());
				for (String m : ModelChecks.marksWithoutCause(md))
					marks.add(p.id() + " " + m);
			}
		none(floating, "blocks hanging in the air");
		none(mono, "faces over the anti-monoblock share (" + Math.round(ModelChecks.MAX_SHARE * 100) + "%)");
		none(excluded, "excluded blocks");
		none(marks, "marks with no cause");
		helper.succeed();
	}

	/**
	 * The rings (D73): intact to 22, worn to 45, damaged to 70, ruined beyond. An intact building carries no war marks (no
	 * strike, no scarred plate); a worn one at most one strike and one chip a face, on outward faces only; the sky tower holds
	 * its live span (never worse than worn) and the yard is never worse than worn either.
	 */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void damageFollowsTheRings(GameTestHelper helper) {
		check(Wear.forRing(0) == Wear.INTACT && Wear.forRing(22) == Wear.INTACT && Wear.forRing(23) == Wear.WORN && Wear.forRing(45) == Wear.WORN
			&& Wear.forRing(46) == Wear.DAMAGED && Wear.forRing(70) == Wear.DAMAGED && Wear.forRing(71) == Wear.RUINED && Wear.forRing(-90) == Wear.RUINED,
			"the rings aren't intact to 22, worn to 45, damaged to 70, ruined beyond");
		Catalogue.Built b = Catalogue.plan(new BlockPos(-40_000, 90, -40_000), 0);
		for (Catalogue.Placed p : b.placed())
			for (Model md : p.models()) {
				if (p.wear() == Wear.INTACT) {
					check(md.strikes == 0, p.id() + " is intact but struck");
					check(ModelChecks.scars(md) == 0, p.id() + " is intact but has " + ModelChecks.scars(md) + " scarred plates");
					for (Model.Mark m : md.marks)
						check(m.cause() != Model.Cause.STRIKE && m.cause() != Model.Cause.FIRE && m.cause() != Model.Cause.BREAK,
							p.id() + " is intact but has a " + m.cause() + " mark");
				}
				if (p.wear() == Wear.WORN) {
					check(md.strikes <= 1, p.id() + " is worn but struck " + md.strikes + " times");
					for (Direction d : md.scarredFaces)
						check(d == Direction.EAST || d == Direction.NORTH, p.id() + " has a scar on an inward face " + d);
					// one chip a face: count chipped plates by the face they look out of
					java.util.Map<Direction, Integer> chips = new java.util.EnumMap<>(Direction.class);
					for (BlockState s : md.blocks().values())
						if (s.is(ModWastes.SCARRED_VOID_ALLOY_PLATE.get()) && s.getValue(ScarredPlateBlock.SCAR) == ScarredPlateBlock.Scar.CHIPPED)
							chips.merge(s.getValue(ScarredPlateBlock.FACING), 1, Integer::sum);
					for (var e : chips.entrySet())
						check(e.getValue() <= 1, p.id() + " has " + e.getValue() + " chips on its " + e.getKey() + " face");
				}
			}
		// the sky tower and the yard are never worse than worn, whatever their ring
		Kit.Ctx ruined = new Kit.Ctx(Masonry.workshops(), Wear.RUINED, java.util.EnumSet.of(Direction.EAST, Direction.NORTH), 1);
		Model sky = Tower.sky(ruined, 24).model();
		check(sky.strikes == 0 && ModelChecks.floating(sky).isEmpty(), "the sky tower snapped in the outer ring");
		Model yard = Yard.build(ruined).model();
		check(yard.strikes == 0, "the yard was struck");
		helper.succeed();
	}

	/** The catalogue's exhibits stand apart: in each row their boxes are at least the gap apart. */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void catalogueExhibitsStandApart(GameTestHelper helper) {
		Catalogue.Built b = Catalogue.plan(new BlockPos(-40_000, 90, -40_000), 0);
		for (Catalogue.Placed p : b.placed())
			for (Catalogue.Placed q : b.placed()) {
				if (p == q)
					continue;
				boolean apart = p.max().getX() + Catalogue.GAP <= q.min().getX() || q.max().getX() + Catalogue.GAP <= p.min().getX()
					|| p.max().getZ() < q.min().getZ() || q.max().getZ() < p.min().getZ();
				check(apart, p.id() + " and " + q.id() + " overlap or crowd each other");
			}
		helper.succeed();
	}

	/**
	 * The swap (D71, {@link KitCity}): with the kit on, every city building stands inside its lot (footprints and eaves
	 * clear of the streets), the city's own pieces are all still put (Substations, vaults, doors, the lift, every bot), and
	 * every city block is bound to the district it stands in.
	 */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void theSwapFitsTheLots(GameTestHelper helper) {
		List<String> out = new ArrayList<>();
		for (BlockPos o : ORIGINS)
			for (long seed : SEEDS)
				for (String problem : KitCity.problems(CityPlan.of(o, seed, false, true)))
					out.add("seed " + seed + " at " + o.toShortString() + ": " + problem);
		none(out, "problems with the kit in the city");
		BlockPos o = ORIGINS[0];
		CityPlan kit = CityPlan.of(o, 0, false, true);
		CityPlan boxes = CityPlan.of(o, 0, false, false);
		// the city's own pieces are all still there: P4's bots per district, the spans, the doors, the vaults, the lift
		for (District d : District.DISTRICTS) {
			long sentries = kit.ops().stream().filter(op -> op instanceof CityPlan.Op.Sentry s && s.district() == d).count();
			long scuttlers = kit.ops().stream().filter(op -> op instanceof CityPlan.Op.Scuttler s && s.district() == d).count();
			check(sentries >= 2 && sentries <= 4 && scuttlers >= 3 && scuttlers <= 5, d + " has " + sentries + " Sentries and " + scuttlers + " Scuttlers");
		}
		check(count(kit, CityPlan.Op.Span.class) == count(boxes, CityPlan.Op.Span.class), "the kit's city has " + count(kit, CityPlan.Op.Span.class)
			+ " spans, the boxes' " + count(boxes, CityPlan.Op.Span.class));
		for (var block : new net.minecraft.world.level.block.Block[] { ModWastes.SUBSTATION.get(), ModWastes.CITY_VAULT.get(), ModWastes.SLIDING_DOOR.get(),
			ModWastes.LIFT.get(), ModWastes.CONDUIT_PYLON.get(), ModWastes.WALL_VENT.get(), ModWastes.ASSEMBLY_CLAMP.get() })
			check(blocks(kit, block) == blocks(boxes, block), "the kit's city puts " + blocks(kit, block) + " " + block + ", the boxes' " + blocks(boxes, block));
		helper.succeed();
	}

	/**
	 * D79, the lore pass: the terraces' roof hatches (one or two houses, worn and damaged only, at least one left open), the
	 * wrecked winding tower in every state (keeps the grammar; its spring out through its west wall in a coil on the ground,
	 * the hole's edge cracked by its cause; its drive shaft snapped; never up to the spire). The tower waits on its Blockbench
	 * preview before it goes into the city.
	 */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void loreDetailsKeepTheirRules(GameTestHelper helper) {
		java.util.Set<Direction> out = java.util.EnumSet.of(Direction.EAST, Direction.NORTH);
		// the hatches
		for (Wear w : Wear.values()) {
			Model t = com.comedymonkey.createaddons.content.wastes.city.kit.Terrace.build(new Kit.Ctx(Masonry.quarters(), w, out, 3), 4);
			int hatches = 0, open = 0;
			for (var e : t.blocks().entrySet())
				if (e.getValue().is(net.minecraft.world.level.block.Blocks.DARK_OAK_TRAPDOOR) && e.getKey().getY() > com.comedymonkey.createaddons.content.wastes.city.kit.Terrace.E) {
					hatches++;
					if (e.getValue().getValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN))
						open++;
				}
			boolean want = w == Wear.WORN || w == Wear.DAMAGED;
			check(want ? hatches >= 1 && hatches <= 2 && open >= 1 : hatches == 0, "a " + w + " terrace has " + hatches + " roof hatches (" + open + " open)");
			check(ModelChecks.floating(t).isEmpty(), "a " + w + " terrace has blocks hanging");
			// each hatch's thin walnut U (D85): walnut either side and across its head, none below it
			if (want)
				for (int h : com.comedymonkey.createaddons.content.wastes.city.kit.Terrace.HATCH_HOUSES) {
					BlockPos p = com.comedymonkey.createaddons.content.wastes.city.kit.Terrace.hatchAt(h);
					int walnut = 0;
					for (int dx = -1; dx <= 1; dx++)
						for (int dz = -1; dz <= 1; dz++)
							for (int dy = -1; dy <= 1; dy++) {
								BlockState s = t.get(p.getX() + dx, p.getY() + dy, p.getZ() + dz);
								if (s != null && s.is(net.minecraft.world.level.block.Blocks.DARK_OAK_STAIRS)) {
									walnut++;
									check(dz >= 0, "a " + w + " terrace's hatch " + h + " has walnut below it");
								}
							}
					check(walnut == 5, "a " + w + " terrace's hatch " + h + " has " + walnut + " walnut curb blocks (a U is 5)");
				}
			// the ridge: Patina Ridge Rolls, each pair faced toward each other (D85)
			int rolls = 0;
			for (var e : t.blocks().entrySet())
				if (e.getValue().is(ModWastes.PATINA_ROLL.get())) {
					rolls++;
					Direction toward = e.getValue().getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
					// the ridge line is between z 2 and 3: the near half faces south, the far half north (its other half may have
					// fallen with the roof)
					Direction want2 = e.getKey().getZ() == com.comedymonkey.createaddons.content.wastes.city.kit.Terrace.D / 2 - 1 ? Direction.SOUTH : Direction.NORTH;
					BlockState other = t.get(e.getKey().getX() + toward.getStepX(), e.getKey().getY(), e.getKey().getZ() + toward.getStepZ());
					check(toward == want2 && (other == null || other.is(ModWastes.PATINA_ROLL.get())
						&& other.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING) == toward.getOpposite()),
						"a " + w + " terrace's ridge roll at " + e.getKey().toShortString() + " isn't faced toward its other half");
				}
			check(w == Wear.RUINED || rolls > 0, "a " + w + " terrace has no ridge roll");
		}
		// the winding tower in every state
		List<String> problems = new ArrayList<>();
		for (long seed : new long[] { 5, 0, 1, 12345, -7 })
		for (Wear w : Wear.values()) {
			Model md = com.comedymonkey.createaddons.content.wastes.city.kit.WindingTower.build(new Kit.Ctx(Masonry.quarters(), w, out, seed));
			String id = "winding tower " + w + " (seed " + seed + ")";
			for (BlockPos f : ModelChecks.floating(md))
				problems.add(id + ": " + md.get(f.getX(), f.getY(), f.getZ()).getBlock() + " hanging at " + f.toShortString());
			for (ModelChecks.Share s : ModelChecks.elevations(md))
				if (s.share() > ModelChecks.MAX_SHARE && s.seen() > 20)
					problems.add(id + ": " + s.side() + " is " + Math.round(s.share() * 100) + "% " + s.block());
			for (var e : md.blocks().entrySet())
				if (ModelChecks.excluded(e.getValue()))
					problems.add(id + ": excluded " + e.getValue().getBlock());
			for (String m : ModelChecks.marksWithoutCause(md))
				problems.add(id + ": " + m);
			int outside = 0, onGround = 0;
			// (the spring's strip: every cell its prop pieces reach, D87)
			for (BlockPos q : new java.util.HashSet<>(com.comedymonkey.createaddons.content.wastes.city.kit.WindingTower.sprungReach(md)))
				if (q.getX() < -1) {
					outside++;
					if (q.getY() == 0)
						onGround++;
				}
			if (outside < 30 || onGround < 2)
				problems.add(id + ": its spring has " + outside + " blocks outside the west wall, " + onGround + " on the ground");
			if (md.marks.stream().noneMatch(m -> m.cause() == Model.Cause.SPRUNG))
				problems.add(id + ": no hole where the spring went out");
			if (w != Wear.RUINED) {
				int c = com.comedymonkey.createaddons.content.wastes.city.kit.WindingTower.C;
				if (md.has(c, 5, 7) || md.has(c, 6, 7) || !md.has(c, 4, 7) || !md.has(c, 7, 7))
					problems.add(id + ": its drive shaft isn't snapped (standing below, hanging above, a gap between)");
				if (w.atLeast(Wear.DAMAGED) || w == Wear.WORN)
					if (md.strikes < 1)
						problems.add(id + ": no strike in the " + w + " ring");
			}
			if (w == Wear.INTACT && md.strikes > 0)
				problems.add(id + ": struck");
			if (com.comedymonkey.createaddons.content.wastes.city.kit.WindingTower.height(md) >= 30)
				problems.add(id + ": stands " + com.comedymonkey.createaddons.content.wastes.city.kit.WindingTower.height(md) + " high (it mustn't rival the spire)");
		}
		none(problems, "winding tower problems");
		// the swap's one tower (D84, D85): in the Quarters, in its ring's state, its west (spring) side toward a street
		for (BlockPos o : ORIGINS)
			for (long seed : SEEDS)
				swapTower(CityPlan.of(o, seed, false, true), "seed " + seed + " at " + o.toShortString());
		helper.succeed();
	}

	/** The swap's one winding tower, as {@link #loreDetailsKeepTheirRules} says. */
	private static void swapTower(CityPlan kit, String city) {
		List<KitCity.Placement> towers = new ArrayList<>();
		for (Object o : kit.buildings)
			if (o instanceof KitCity.Placement pl && pl.what().equals("winding tower"))
				towers.add(pl);
		check(towers.size() == 1, city + ": the kit's city has " + towers.size() + " winding towers");
		KitCity.Placement tw = towers.get(0);
		check(tw.f().d() == District.QUARTERS, city + ": the winding tower is in " + tw.f().d());
		check(!tw.site().mirrored(), city + ": the winding tower stands in a mirrored frame (its modelled spring can't mirror)");
		int ring = 0;
		for (BlockPos q : tw.model().blocks().keySet()) {
			BlockPos wpos = tw.site().at(q.getX(), q.getY(), q.getZ()).subtract(kit.origin);
			ring = Math.max(ring, Wear.ring(wpos.getX(), wpos.getZ()));
		}
		check(Wear.forRing(ring) == Wear.DAMAGED && tw.model().marks.stream().anyMatch(m -> m.cause() == Model.Cause.BREAK) && tw.model().strikes >= 1,
			city + ": the winding tower reaches ring " + ring + " (" + Wear.forRing(ring) + ") but isn't built damaged (its top broken, struck)");
		// out from the middle of its west wall (local -x), a street cell within 14 blocks, and nothing of another building between
		java.util.Set<BlockPos> others = new java.util.HashSet<>();
		for (Object o : kit.buildings)
			if (o instanceof KitCity.Placement pl && pl != tw)
				others.addAll(KitCity.cells(pl).keySet());
		int c = com.comedymonkey.createaddons.content.wastes.city.kit.WindingTower.C;
		int toStreet = -1;
		for (int k = 1; k <= 14 && toStreet < 0; k++) {
			if (KitCity.isStreet(tw.f(), tw.site().at(-k, 0, c))) {
				toStreet = k;
				break;
			}
			for (int y = 0; y <= 4; y++)
				check(!others.contains(tw.site().at(-k, y, c)), city + ": another building stands between the winding tower and the street, " + k + " out");
		}
		check(toStreet > 0, city + ": the winding tower's spring side doesn't face a street");
	}

	/**
	 * The winding tower's props can be turned but not mirrored (their models only turn): put in a mirrored site, the tower
	 * fails loudly rather than laying a spring that runs the wrong way; turned any way, it lays.
	 */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void aMirroredWindingTowerThrows(GameTestHelper helper) {
		Model md = com.comedymonkey.createaddons.content.wastes.city.kit.WindingTower.build(
			new Kit.Ctx(Masonry.quarters(), Wear.WORN, java.util.EnumSet.of(Direction.EAST, Direction.NORTH), 5));
		BlockPos at = new BlockPos(-50_000, 90, -50_000);
		for (Direction a : Direction.Plane.HORIZONTAL) {
			boolean[] threw = { false };
			CityPlan.drawn(at, 0, p -> {
				com.comedymonkey.createaddons.content.wastes.city.kit.Site mirrored = new com.comedymonkey.createaddons.content.wastes.city.kit.Site(p,
					p.origin, a, a.getCounterClockWise(), p.origin, District.QUARTERS);
				check(mirrored.mirrored(), "the test's site isn't mirrored");
				try {
					md.emit(mirrored);
				} catch (IllegalStateException e) {
					threw[0] = true;
				}
			});
			check(threw[0], "a winding tower in a mirrored site facing " + a + " didn't throw");
			CityPlan.drawn(at, 0, p -> md.emit(new com.comedymonkey.createaddons.content.wastes.city.kit.Site(p, p.origin, a, a.getClockWise(), p.origin,
				District.QUARTERS)));
		}
		helper.succeed();
	}

	/**
	 * D79: under each void-alloy bridge (the four boulevards past the core), in the boxes' city and the kit's alike, the
	 * makers' line shaft runs down the middle two below the deck, over the void only, unbroken from the core's edge to the
	 * bridge's end or its snap; a cogwheel at each pier (every 8), a chain hanger every 4; past a snap nothing but the one
	 * length hanging from the deck that fell.
	 */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void bridgesCarryTheOldLineShaft(GameTestHelper helper) {
		for (boolean withKit : new boolean[] { false, true }) {
			CityPlan p = CityPlan.of(new BlockPos(1000, 70, -2000), 0, false, withKit);
			java.util.Map<BlockPos, CityPlan.Op.Put> under = new java.util.HashMap<>();
			for (CityPlan.Op op : p.ops())
				if (op instanceof CityPlan.Op.Put put) {
					BlockPos q = put.at().subtract(p.origin);
					String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(put.state().getBlock()).toString();
					if (id.equals("create:shaft") || id.equals("create:cogwheel") || put.state().is(net.minecraft.world.level.block.Blocks.CHAIN))
						if (Math.max(Math.abs(q.getX()), Math.abs(q.getZ())) > com.comedymonkey.createaddons.content.wastes.city.CityLayout.CORE && (q.getX() == 0 || q.getZ() == 0))
							under.put(q, put);
				}
			String city = withKit ? "the kit's city" : "the boxes' city";
			// north (snaps), east (snaps), south, west
			int[][] bridges = { { 0, -1, 1 }, { 1, 0, 1 }, { 0, 1, 0 }, { -1, 0, 0 } };
			int y = com.comedymonkey.createaddons.content.wastes.city.CityLayout.SHAFT_Y;
			for (int[] b : bridges) {
				boolean snaps = b[2] == 1;
				int end = snaps ? com.comedymonkey.createaddons.content.wastes.city.CityLayout.SNAP_FAR : com.comedymonkey.createaddons.content.wastes.city.CityLayout.OUT;
				String name = city + "'s bridge toward " + b[0] + ", " + b[1];
				for (int far = com.comedymonkey.createaddons.content.wastes.city.CityLayout.CORE + 1; far <= com.comedymonkey.createaddons.content.wastes.city.CityLayout.OUT; far++) {
					CityPlan.Op.Put s = under.get(new BlockPos(b[0] * far, y, b[1] * far)), h = under.get(new BlockPos(b[0] * far, y + 1, b[1] * far));
					if (far > end) {
						check(s == null && h == null, name + " has its shaft past the snap at " + far);
						continue;
					}
					check(s != null && s.need() == CityPlan.OVER_VOID, name + " has no shaft (over the void only) at " + far);
					String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.state().getBlock()).getPath();
					check(id.equals(far % 8 == 0 ? "cogwheel" : "shaft"), name + " has a " + id + " at " + far);
					check(s.state().getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS) == (b[0] == 0 ? Direction.Axis.Z : Direction.Axis.X),
						name + "'s shaft runs across the bridge at " + far);
					check((h != null) == (far % com.comedymonkey.createaddons.content.wastes.city.CityLayout.HANGER_EVERY == 0), name + " has its hanger wrong at " + far);
				}
				if (snaps) {
					int far = com.comedymonkey.createaddons.content.wastes.city.CityLayout.SNAP_FAR + 1;
					check(under.containsKey(new BlockPos(b[0] * far, -4, b[1] * far)) && under.containsKey(new BlockPos(b[0] * far, -5, b[1] * far)),
						name + " has no length hanging where it snaps");
				}
			}
		}
		helper.succeed();
	}

	/**
	 * D90: the catalogue rebuilds in place. Its plan is all core: every block put anywhere, none only where the ground is
	 * near the floor (on a rebuild the last build's own walls stood in for the ground, so the drum's new diagonals were
	 * skipped and its old stepped cells left), and its drum's diagonals are in its plan.
	 */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void catalogueRebuildsInPlace(GameTestHelper helper) {
		Catalogue.Built b = Catalogue.plan(new BlockPos(-40_000, 90, -40_000), 0);
		long notAnywhere = b.plan().ops().stream().filter(op -> op instanceof CityPlan.Op.Put p && p.need() != CityPlan.ANYWHERE).count();
		check(notAnywhere == 0, notAnywhere + " of the catalogue's blocks are put only on ground or only over the void");
		check(blocks(b.plan(), ModWastes.STAINED_END_STONE_BRICK_DIAGONAL.get()) > 0 && blocks(b.plan(), ModWastes.CUT_DEEPSLATE_BRICK_DIAGONAL.get()) > 0,
			"the catalogue's drum has no 45-degree diagonals");
		helper.succeed();
	}

	/**
	 * D90, choice 39: the Cut Deepslate Brick Diagonal is the water table when placed against the lower half of a block's
	 * side or on top of the block below, the plain course on an upper half or under a block.
	 */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void diagonalCourseGivesTheWaterTableLow(GameTestHelper helper) {
		var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
		var stack = new net.minecraft.world.item.ItemStack(ModWastes.CUT_DEEPSLATE_BRICK_DIAGONAL.get());
		BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
		java.util.function.BiFunction<Direction, Double, com.comedymonkey.createaddons.content.wastes.CutDeepslateDiagonalBlock.Course> place = (face, y) -> {
			var hit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(at.getX() + 0.5, at.getY() + y, at.getZ() + 0.5), face, at, false);
			var ctx = new net.minecraft.world.item.context.BlockPlaceContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, stack, hit);
			return ModWastes.CUT_DEEPSLATE_BRICK_DIAGONAL.get().getStateForPlacement(ctx).getValue(com.comedymonkey.createaddons.content.wastes.CutDeepslateDiagonalBlock.COURSE);
		};
		var water = com.comedymonkey.createaddons.content.wastes.CutDeepslateDiagonalBlock.Course.WATER_TABLE;
		var course = com.comedymonkey.createaddons.content.wastes.CutDeepslateDiagonalBlock.Course.COURSE;
		check(place.apply(Direction.NORTH, 0.25) == water, "a side's lower half doesn't give the water table");
		check(place.apply(Direction.NORTH, 0.75) == course, "a side's upper half doesn't give the course");
		check(place.apply(Direction.UP, 1.0) == water, "on top of a block doesn't give the water table");
		check(place.apply(Direction.DOWN, 0.0) == course, "under a block doesn't give the course");
		helper.succeed();
	}

	/** How many distinct cells end up holding this block. */
	static long blocks(CityPlan p, net.minecraft.world.level.block.Block b) {
		java.util.Map<BlockPos, BlockState> last = new java.util.HashMap<>();
		for (CityPlan.Op op : p.ops())
			if (op instanceof CityPlan.Op.Put put)
				last.put(put.at(), put.state());
		return last.values().stream().filter(s -> s.is(b)).count();
	}

	static long count(CityPlan p, Class<?> c) {
		return p.ops().stream().filter(c::isInstance).count();
	}

	// ================================================================ the catalogue in a world

	private static final BlockPos CATALOGUE = new BlockPos(-30_000, 110, 30_000);
	private static final int CATALOGUE_TIMEOUT = 1200;

	/**
	 * {@code /wastes buildcatalogue} builds and binds: every city block in it is bound to the catalogue's record and to its
	 * exhibit's district; every block stands joined to the ground; asleep nothing glows; woken and fed every grille, band,
	 * seamed plate and cracked scar of it lights.
	 */
	@GameTest(template = COURSE, timeoutTicks = CATALOGUE_TIMEOUT, batch = "wastes_kit_catalogue")
	public static void catalogueBuildsAndBinds(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// somewhere new each run (the game-test world keeps the last run's catalogue, its blocks and their state)
		BlockPos o = CATALOGUE.offset(level.random.nextInt(4096) * 1024, 0, level.random.nextInt(4096) * 1024);
		Catalogue.Built plan = Catalogue.plan(o, 0);
		List<ChunkPos> chunks = new ArrayList<>();
		for (int cx = (plan.minX() - 8) >> 4; cx <= (plan.maxX() + 8) >> 4; cx++)
			for (int cz = (plan.minZ() - 8) >> 4; cz <= (plan.maxZ() + 34) >> 4; cz++) {
				chunks.add(new ChunkPos(cx, cz));
				level.setChunkForced(cx, cz, true);
			}
		BlockPos[] rec = { null };
		Runnable release = () -> {
			for (ChunkPos c : chunks)
				level.setChunkForced(c.x, c.z, false);
			if (rec[0] != null)
				CityRecord.of(level).forget(rec[0]);
		};
		try {
			catalogueChecks(helper, level, o, rec, release);
		} catch (RuntimeException e) {
			release.run();
			throw e;
		}
	}

	/** The catalogue test's checks; {@code release} lets its chunks and record go (at the end, or when a check fails). */
	private static void catalogueChecks(GameTestHelper helper, ServerLevel level, BlockPos o, BlockPos[] recOut, Runnable release) {
		Catalogue.Built b = CatalogueBuild.build(level, o, 0);
		BlockPos rec = b.recordOrigin();
		recOut[0] = rec;
		check(CityRecord.of(level).cities().contains(rec), "the catalogue has no record");
		List<BlockPos> lights = new ArrayList<>();
		int members = 0;
		for (Catalogue.Placed p : b.placed())
			for (BlockPos q : BlockPos.betweenClosed(p.min(), p.max())) {
				BlockEntity be = level.getBlockEntity(q);
				if (be instanceof CityLink.Member m) {
					members++;
					check(rec.equals(m.cityLink().origin()), p.id() + ": " + be.getBlockState().getBlock() + " at " + q.toShortString() + " isn't bound to the catalogue");
					check(m.cityLink().district() == p.district(), p.id() + ": " + be.getBlockState().getBlock() + " is bound to " + m.cityLink().district());
				}
				BlockState s = level.getBlockState(q);
				if (s.getBlock() instanceof CityDecorLightBlock && s.hasProperty(CityDecorLightBlock.LIT)
					&& !(s.getBlock() instanceof ScarredPlateBlock && s.getValue(ScarredPlateBlock.SCAR) != ScarredPlateBlock.Scar.CRACKED))
					lights.add(q.immutable());
			}
		check(members > 300, "only " + members + " city blocks in the catalogue");
		check(lights.size() > 200, "only " + lights.size() + " lights");
		// every block joined to the ground (the pad), face to face, within its exhibit's box and the pad under it
		for (Catalogue.Placed p : b.placed()) {
			List<String> hanging = hanging(level, p);
			check(hanging.isEmpty(), p.id() + " has blocks hanging in the world: " + String.join(", ", hanging.subList(0, Math.min(6, hanging.size()))));
		}
		var seq = helper.startSequence();
		seq.thenExecuteAfter(5, releasing(helper, release, false, () -> {
			for (BlockPos q : lights)
				check(!level.getBlockState(q).getValue(CityDecorLightBlock.LIT), level.getBlockState(q).getBlock() + " at " + q.toShortString() + " glows asleep");
			CityRecord.of(level).wakeAll(level, rec, null);
			CityRecord.of(level).burnHeart(level, rec, level.getGameTime() + 20 * 60 * 10, null);
		}));
		seq.thenExecuteAfter(120, releasing(helper, release, true, () -> {
			for (BlockPos q : lights)
				check(level.getBlockState(q).getValue(CityDecorLightBlock.LIT), level.getBlockState(q).getBlock() + " at " + q.toShortString() + " is dark awake and fed");
		}));
		seq.thenSucceed();
	}

	/**
	 * A sequence step that lets the test's chunks and record go ({@code release}) once it has failed for good: an error at
	 * once, a check (which the sequence retries every tick) as the test times out; and after it, if it's the {@code last}.
	 */
	private static Runnable releasing(GameTestHelper helper, Runnable release, boolean last, Runnable step) {
		return () -> {
			try {
				step.run();
			} catch (GameTestAssertException e) {
				if (helper.getTick() >= CATALOGUE_TIMEOUT - 3)
					release.run();
				throw e;
			} catch (RuntimeException e) {
				release.run();
				throw e;
			}
			if (last)
				release.run();
		};
	}

	/** Blocks in an exhibit's box not joined, face to face, to the ground under it. */
	static List<String> hanging(ServerLevel level, Catalogue.Placed p) {
		BlockPos min = p.min().offset(-2, -1, -2), max = p.max().offset(2, 0, 2);
		// the box's blocks, joined from the pad (its lowest row) by the kit's own rule (props stand in all they reach, D87)
		java.util.Map<BlockPos, BlockState> blocks = new java.util.HashMap<>();
		for (BlockPos q : BlockPos.betweenClosed(min, max))
			if (!level.getBlockState(q).isAir())
				blocks.put(q.immutable(), level.getBlockState(q));
		Set<BlockPos> seen = ModelChecks.joined(blocks, q -> q.getY() <= min.getY());
		List<String> out = new ArrayList<>();
		for (BlockPos q : BlockPos.betweenClosed(p.min(), p.max()))
			if (!level.getBlockState(q).isAir() && !seen.contains(q))
				out.add(level.getBlockState(q).getBlock() + " at " + q.toShortString());
		return out;
	}

	/** Unused: kept so the district enum's import is used where the checks name a district. */
	static District heart() {
		return District.HEART;
	}

	static boolean isCity(BlockState s) {
		return s.getBlock() instanceof EntityBlock;
	}
}
