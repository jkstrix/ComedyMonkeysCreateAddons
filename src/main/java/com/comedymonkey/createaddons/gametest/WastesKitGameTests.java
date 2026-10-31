package com.comedymonkey.createaddons.gametest;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashSet;
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
		BlockPos o = new BlockPos(1000, 70, -2000);
		CityPlan kit = CityPlan.of(o, 0, false, true);
		List<String> out = KitCity.problems(kit);
		none(out, "problems with the kit in the city");
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

	/**
	 * {@code /wastes buildcatalogue} builds and binds: every city block in it is bound to the catalogue's record and to its
	 * exhibit's district; every block stands joined to the ground; asleep nothing glows; woken and fed every grille, band,
	 * seamed plate and cracked scar of it lights.
	 */
	@GameTest(template = COURSE, timeoutTicks = 1200, batch = "wastes_kit_catalogue")
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
		Catalogue.Built b = CatalogueBuild.build(level, o, 0);
		BlockPos rec = b.recordOrigin();
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
		seq.thenExecuteAfter(5, () -> {
			for (BlockPos q : lights)
				check(!level.getBlockState(q).getValue(CityDecorLightBlock.LIT), level.getBlockState(q).getBlock() + " at " + q.toShortString() + " glows asleep");
			CityRecord.of(level).wakeAll(level, rec, null);
			CityRecord.of(level).burnHeart(level, rec, level.getGameTime() + 20 * 60 * 10, null);
		});
		seq.thenExecuteAfter(120, () -> {
			for (BlockPos q : lights)
				check(level.getBlockState(q).getValue(CityDecorLightBlock.LIT), level.getBlockState(q).getBlock() + " at " + q.toShortString() + " is dark awake and fed");
			for (ChunkPos c : chunks)
				level.setChunkForced(c.x, c.z, false);
			CityRecord.of(level).forget(rec);
		});
		seq.thenSucceed();
	}

	/** Blocks in an exhibit's box not joined, face to face, to the ground under it. */
	static List<String> hanging(ServerLevel level, Catalogue.Placed p) {
		BlockPos min = p.min().offset(-2, -1, -2), max = p.max().offset(2, 0, 2);
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> todo = new ArrayDeque<>();
		for (int x = min.getX(); x <= max.getX(); x++)
			for (int z = min.getZ(); z <= max.getZ(); z++) {
				BlockPos q = new BlockPos(x, min.getY(), z);
				if (!level.getBlockState(q).isAir() && seen.add(q))
					todo.add(q);
			}
		while (!todo.isEmpty()) {
			BlockPos q = todo.poll();
			for (Direction d : Direction.values()) {
				BlockPos n = q.relative(d);
				if (n.getX() < min.getX() || n.getX() > max.getX() || n.getZ() < min.getZ() || n.getZ() > max.getZ() || n.getY() < min.getY() || n.getY() > max.getY())
					continue;
				if (!level.getBlockState(n).isAir() && seen.add(n))
					todo.add(n);
			}
		}
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
