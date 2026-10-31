package com.comedymonkey.createaddons.gametest;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.wastes.AssemblyClampBlock;
import com.comedymonkey.createaddons.content.wastes.BrassPlaqueBlock;
import com.comedymonkey.createaddons.content.wastes.ChargingAlcoveBlock;
import com.comedymonkey.createaddons.content.wastes.CityLink;
import com.comedymonkey.createaddons.content.wastes.CityRecord;
import com.comedymonkey.createaddons.content.wastes.CityVaultBlock;
import com.comedymonkey.createaddons.content.wastes.DerelictCableBlock;
import com.comedymonkey.createaddons.content.wastes.DerelictInsulatorBlock;
import com.comedymonkey.createaddons.content.wastes.DerelictInsulatorBlockEntity;
import com.comedymonkey.createaddons.content.wastes.District;
import com.comedymonkey.createaddons.content.wastes.EndBiomes;
import com.comedymonkey.createaddons.content.wastes.LiftBlock;
import com.comedymonkey.createaddons.content.wastes.LiftBlockEntity;
import com.comedymonkey.createaddons.content.wastes.ShellBlock;
import com.comedymonkey.createaddons.content.wastes.SlidingDoorBlock;
import com.comedymonkey.createaddons.content.wastes.SubstationBlock;
import com.comedymonkey.createaddons.content.wastes.ThreePartBlock;
import com.comedymonkey.createaddons.content.wastes.WallVentBlock;
import com.comedymonkey.createaddons.content.wastes.bot.CityBot;
import com.comedymonkey.createaddons.content.wastes.bot.Custodian;
import com.comedymonkey.createaddons.content.wastes.bot.Scuttler;
import com.comedymonkey.createaddons.content.wastes.bot.Sentry;
import com.comedymonkey.createaddons.content.wastes.bot.WastesBots;
import com.comedymonkey.createaddons.content.wastes.city.CityApplier;
import com.comedymonkey.createaddons.content.wastes.city.CityBuild;
import com.comedymonkey.createaddons.content.wastes.city.CityLayout;
import com.comedymonkey.createaddons.content.wastes.city.CityPlan;
import com.comedymonkey.createaddons.content.wastes.city.ClockworkCityStructure;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.TheEndBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.structures.EndCityPieces;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Clockwork Wastes city (D66) and its place in the End (P2, P45-P47, D13):
 * <ul>
 * <li>the city plan puts every city block and bot in its own district (crown insulators excepted: they hold their district's
 * sky-cable at the spire);</li>
 * <li>the dev-built city ({@code /wastes buildcity}) is whole and bound, and wakes district by district: its vault and
 * doors open, its bots stir, the others stay asleep; the sky-cable bead reaches each insulator 2 s a span; four awake
 * opens the heart's doors and wakes the Custodian;</li>
 * <li>the structure set keeps cities 6 chunks from any End city;</li>
 * <li>an outskirts-only site holds its own Elytra (D69), built as world generation builds it;</li>
 * <li>island fit over 200 sites in the Wastes, reported, on several seeds;</li>
 * <li>Elytra parity at spacing 32 on several seeds: the Wastes' cities and outskirts-only sites give at least as many
 * Elytra as the End-city ships whose places they took (D69).</li>
 * </ul>
 */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WastesWorldGameTests {
	private static final String COURSE = "trade_bot_course";
	private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
	private static final ResourceKey<StructureSet> CITY_SET = ResourceKey.create(Registries.STRUCTURE_SET,
		ComedyMonkeysCreateAddons.asResource("clockwork_city"));

	private WastesWorldGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(WastesWorldGameTests.class);
	}

	private static void check(boolean ok, String message) {
		if (!ok)
			throw new GameTestAssertException(message);
	}

	/** The district a city block or bot at {@code pos} should be bound to. */
	private static District expected(BlockPos origin, BlockPos pos) {
		for (District d : District.DISTRICTS)
			if (pos.equals(origin.offset(CityLayout.crownInsulator(d))))
				return d;
		return CityLayout.districtAt(pos.getX() - origin.getX(), pos.getZ() - origin.getZ());
	}

	// ================================================================ the plan

	/**
	 * Every city block the plan puts, and every bot, is bound to the district it stands in (its quadrant; the heart in the
	 * middle), the crown insulators to the district whose sky-cable they hold; no city block goes unbound; each district
	 * has one Substation, one vault, 2-4 Sentries and 3-5 Scuttlers; the heart has 4 pylons, 8 vents, its vault and one
	 * Custodian.
	 */
	@GameTest(template = COURSE, timeoutTicks = 40)
	public static void everyPlacedBlockIsInItsDistrict(GameTestHelper helper) {
		for (long seed : new long[] { 0, 1, 12345 }) {
			BlockPos o = new BlockPos(1000, 70, -2000);
			CityPlan plan = CityPlan.of(o, seed, false);
			Map<District, int[]> counts = new EnumMap<>(District.class);
			for (District d : District.values())
				counts.put(d, new int[6]);
			for (CityPlan.Op op : plan.ops()) {
				switch (op) {
					case CityPlan.Op.Put p -> {
						BlockState s = p.state();
						if (!(s.getBlock() instanceof net.minecraft.world.level.block.EntityBlock eb))
							continue;
						BlockEntity be = eb.newBlockEntity(p.at(), s);
						if (!(be instanceof CityLink.Member))
							continue;
						check(p.district() != null, "an unbound city block " + s + " at " + p.at().subtract(o));
						check(p.district() == expected(o, p.at()), s.getBlock() + " at " + p.at().subtract(o) + " is bound to " + p.district()
							+ ", but it stands in " + expected(o, p.at()));
						int[] c = counts.get(p.district());
						if (s.getBlock() instanceof SubstationBlock && s.getValue(SubstationBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER)
							c[0]++;
						if (s.getBlock() instanceof CityVaultBlock)
							c[1]++;
						if (s.getBlock() instanceof WallVentBlock)
							c[4]++;
						if (s.is(ModWastes.CONDUIT_PYLON.get()) && s.getValue(com.comedymonkey.createaddons.content.wastes.ThreePartBlock.PART)
							== com.comedymonkey.createaddons.content.wastes.ThreePartBlock.Part.BOTTOM)
							c[5]++;
					}
					case CityPlan.Op.Sentry s -> {
						check(s.district() == expected(o, s.at()), "a Sentry at " + s.at().subtract(o) + " is the " + s.district() + "'s");
						counts.get(s.district())[2]++;
					}
					case CityPlan.Op.Scuttler s -> {
						check(s.district() == expected(o, s.at()), "a Scuttler at " + s.at().subtract(o) + " is the " + s.district() + "'s");
						counts.get(s.district())[3]++;
					}
					case CityPlan.Op.Custodian c -> {
						check(expected(o, c.at()) == District.HEART, "the Custodian kneels outside the heart");
						check(c.pylons().size() == 4, "the Custodian has " + c.pylons().size() + " pylons");
					}
					default -> {}
				}
			}
			for (District d : District.DISTRICTS) {
				int[] c = counts.get(d);
				check(c[0] == 1, d + " has " + c[0] + " Substations");
				check(c[1] == 1, d + " has " + c[1] + " vaults");
				check(c[2] >= 2 && c[2] <= 4, d + " has " + c[2] + " Sentries (P4: 2-4)");
				check(c[3] >= 3 && c[3] <= 5, d + " has " + c[3] + " Scuttlers (P4: 3-5)");
			}
			int[] h = counts.get(District.HEART);
			check(h[1] == 1 && h[4] == 8 && h[5] == 4, "the heart has " + h[1] + " vaults, " + h[4] + " vents, " + h[5] + " pylons");
			check(plan.ops().stream().filter(op -> op instanceof CityPlan.Op.Custodian).count() == 1, "one Custodian");
		}
		helper.succeed();
	}

	// ================================================================ the dev-built city

	/**
	 * The dev city stands far from every other test, its chunks held loaded while it runs; somewhere new each run (the
	 * game-test world is kept between runs, with the last run's city and record in it).
	 */
	private static final BlockPos DEV_CITY = new BlockPos(24_000, 100, 24_000);

	private static List<ChunkPos> cityChunks(BlockPos o) {
		List<ChunkPos> out = new ArrayList<>();
		int r = (CityLayout.OUT >> 4) + 1;
		ChunkPos c = new ChunkPos(o);
		for (int dx = -r; dx <= r; dx++)
			for (int dz = -r; dz <= r; dz++)
				out.add(new ChunkPos(c.x + dx, c.z + dz));
		return out;
	}

	/** Every city block entity of the city, loaded. */
	private static List<BlockEntity> cityBlocks(ServerLevel level, BlockPos o) {
		List<BlockEntity> out = new ArrayList<>();
		for (ChunkPos c : cityChunks(o))
			for (BlockEntity be : level.getChunk(c.x, c.z).getBlockEntities().values())
				if (be instanceof CityLink.Member)
					out.add(be);
		return out;
	}

	/**
	 * The dev-built city (D66) is whole and bound, and wakes district by district: every city block is bound to the city
	 * and its own district; each district's vault and doors stay shut and its bots asleep until it wakes, then its vault
	 * and doors open and its bots stir within 5 s (P8), while the others don't; the Quarters' lift works once its district
	 * wakes; with the Foundry fed as it wakes the sky-cable bead reaches the mast's insulator at 2 s, the tower's at 4 s and
	 * the crown's at 6 s (D64); the fourth wake opens the heart's doors and wakes the Custodian.
	 */
	@GameTest(template = COURSE, timeoutTicks = 2400, batch = "wastes_dev_city")
	public static void devCityBindsAndWakesPerDistrict(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos o = DEV_CITY.offset(level.random.nextInt(256) * 1024, 0, level.random.nextInt(256) * 1024);
		for (ChunkPos c : cityChunks(o))
			level.setChunkForced(c.x, c.z, true);
		CityBuild.build(level, o, 0, true);
		// a top-down picture of it, for reviewing the layout (run-gametest/wastes-city-map.png)
		try {
			CityBuild.writeMap(level, o, level.getServer().getServerDirectory().resolve("wastes-city-map.png"));
		} catch (java.io.IOException e) {
			LOG.warn("couldn't write the city's map", e);
		}
		CityRecord record = CityRecord.of(level);
		check(record.cities().contains(o), "the city has no record");
		List<BlockEntity> blocks = cityBlocks(level, o);
		check(blocks.size() > 500, "only " + blocks.size() + " city blocks");
		for (BlockEntity be : blocks) {
			CityLink link = ((CityLink.Member) be).cityLink();
			check(o.equals(link.origin()), be.getBlockState() + " at " + be.getBlockPos().subtract(o) + " isn't bound to the city");
			check(link.district() == expected(o, be.getBlockPos()), be.getBlockState().getBlock() + " at " + be.getBlockPos().subtract(o)
				+ " is the " + link.district() + "'s, standing in " + expected(o, be.getBlockPos()));
		}
		// every span ties to something on both ends
		int spans = 0;
		for (BlockEntity be : blocks)
			if (be instanceof DerelictInsulatorBlockEntity ins)
				for (var s : ins.spans())
					if (!s.snapped()) {
						spans++;
						check(DerelictInsulatorBlockEntity.tie(level, s.other()) != null, "a span from " + be.getBlockPos().subtract(o) + " ties to nothing");
					}
		check(spans == 16, spans + " sky-cable spans (4 a district: 3 up the chain, 1 to the corner mast, D72)");
		// the heart's blocks: pylons, vents
		long vents = blocks.stream().filter(be -> be.getBlockState().getBlock() instanceof WallVentBlock).count();
		check(vents == 8, vents + " wall vents");
		// D72: the masts' risers stand beside their girders (two masts a district), dark asleep
		List<BlockPos> risers = blocks.stream().filter(be -> be.getBlockState().getBlock() instanceof DerelictCableBlock
			&& be.getBlockState().getValue(DerelictCableBlock.GIRDER) != DerelictCableBlock.Girder.NONE).map(BlockEntity::getBlockPos).toList();
		check(risers.size() == 4 * 2 * CityLayout.MAST_HEIGHT, risers.size() + " riser blocks");
		for (BlockPos r : risers) {
			BlockState s = level.getBlockState(r);
			check(s.canSurvive(level, r), "the riser at " + r.subtract(o) + " has no girder");
			check(!s.getValue(DerelictCableBlock.LIT), "the riser at " + r.subtract(o) + " is lit asleep");
		}
		Map<District, BlockPos> subs = new EnumMap<>(District.class), vaults = new EnumMap<>(District.class);
		Map<District, List<BlockPos>> doors = new EnumMap<>(District.class);
		BlockPos lift = null;
		for (BlockEntity be : blocks) {
			District d = ((CityLink.Member) be).cityLink().district();
			BlockState s = be.getBlockState();
			if (s.getBlock() instanceof SubstationBlock && s.getValue(SubstationBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER)
				subs.put(d, be.getBlockPos());
			if (s.getBlock() instanceof CityVaultBlock)
				vaults.put(d, be.getBlockPos());
			if (s.getBlock() instanceof SlidingDoorBlock && SlidingDoorBlock.isController(s))
				doors.computeIfAbsent(d, k -> new ArrayList<>()).add(be.getBlockPos());
			if (s.getBlock() instanceof LiftBlock)
				lift = be.getBlockPos();
		}
		check(lift != null && level.getBlockEntity(lift) instanceof LiftBlockEntity, "no lift");
		check(doors.getOrDefault(District.HEART, List.of()).size() == 3, "the heart has " + doors.getOrDefault(District.HEART, List.of()).size() + " doors");
		BlockPos liftPos = lift;
		var seq = helper.startSequence();
		// asleep: everything locked, every bot dormant (after its first tick's catch-up)
		seq.thenExecuteAfter(5, () -> {
			for (District d : District.values())
				check(level.getBlockState(vaults.get(d)).getValue(CityVaultBlock.LOCKED), d + "'s vault is open asleep");
			for (CityBot bot : WastesBots.loaded(level, CityBot.class, o, b -> true))
				check(bot.dormant(), bot + " is awake asleep");
			check(WastesBots.loaded(level, Sentry.class, o, b -> true).size() >= 8, "too few Sentries");
			check(WastesBots.loaded(level, Scuttler.class, o, b -> true).size() >= 12, "too few Scuttlers");
			check(WastesBots.loaded(level, Custodian.class, o, b -> true).size() == 1, "no Custodian");
			check(level.getBlockState(liftPos).getValue(LiftBlock.LOCKED), "the lift works asleep");
			// the Foundry wakes by power, as a player would wake it: the wake charge into its Substation, then its upkeep
			// every tick after (so it stays fed while the bead runs)
			var energy = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, subs.get(District.FOUNDRY), null);
			check(energy != null, "the Foundry's Substation takes no FE");
			for (int left = com.comedymonkey.createaddons.config.CMConfig.SERVER.wastesWakeCharge(); left > 0; ) {
				int got = energy.receiveEnergy(Math.min(left, 100_000), false);
				check(got > 0, "the Substation stopped taking FE");
				left -= got;
			}
			helper.onEachTick(() -> energy.receiveEnergy(com.comedymonkey.createaddons.config.CMConfig.SERVER.wastesUpkeep() * 40, false));
		});
		seq.thenWaitUntil(() -> check(record.awake(o, District.FOUNDRY), "the Foundry hasn't woken"));
		// the bead: the crown's ring is still dark at 5 s, lit by 6.5 s; the mast's lit by 2.5 s
		seq.thenExecuteAfter(50, () -> {
			check(ringLit(level, o, District.FOUNDRY, 1), "the Foundry mast's ring is dark 2.5 s after the wake");
			check(!ringLit(level, o, District.FOUNDRY, 3), "the Foundry crown's ring lit before the bead got there");
		});
		seq.thenExecuteAfter(80, () -> {
			check(ringLit(level, o, District.FOUNDRY, 3), "the Foundry crown's ring is dark 6.5 s after the wake");
			// D72: the Foundry's mast risers light with it (the yard's and the corner's); the others' stay dark
			for (BlockPos r : risers) {
				BlockState s = level.getBlockState(r);
				District d = ((CityLink.Member) level.getBlockEntity(r)).cityLink().district();
				check(s.getValue(DerelictCableBlock.LIT) == (d == District.FOUNDRY), d + "'s riser at " + r.subtract(o) + (s.getValue(DerelictCableBlock.LIT) ? " lit" : " dark"));
			}
		});
		// its bots stir within 5 s; its vault and doors open; the other districts' stay as they were
		seq.thenWaitUntil(() -> {
			for (CityBot bot : WastesBots.loaded(level, CityBot.class, o, b -> !(b instanceof Custodian)))
				check(bot.dormant() == (bot.district() != District.FOUNDRY), bot.district() + "'s " + bot.getClass().getSimpleName()
					+ (bot.dormant() ? " still asleep" : " awake"));
			check(!level.getBlockState(vaults.get(District.FOUNDRY)).getValue(CityVaultBlock.LOCKED), "the Foundry's vault is still locked");
			for (BlockPos d : doors.get(District.FOUNDRY))
				check(level.getBlockState(d).getValue(SlidingDoorBlock.OPEN), "a Foundry door is shut");
			for (District d : new District[] { District.WORKSHOPS, District.RAIL_YARD, District.QUARTERS, District.HEART })
				check(level.getBlockState(vaults.get(d)).getValue(CityVaultBlock.LOCKED), d + "'s vault opened with the Foundry");
			for (BlockPos d : doors.get(District.WORKSHOPS))
				check(!level.getBlockState(d).getValue(SlidingDoorBlock.OPEN), "a Workshops door opened with the Foundry");
		});
		// the others, one by one; the Quarters' lift wakes with it
		for (District d : new District[] { District.WORKSHOPS, District.RAIL_YARD, District.QUARTERS }) {
			seq.thenExecute(() -> record.wake(level, o, d, subs.get(d)));
			seq.thenWaitUntil(() -> {
				check(!level.getBlockState(vaults.get(d)).getValue(CityVaultBlock.LOCKED), d + "'s vault is still locked");
				for (BlockPos door : doors.get(d))
					check(level.getBlockState(door).getValue(SlidingDoorBlock.OPEN), "a door of the " + d + " is shut");
				for (CityBot bot : WastesBots.loaded(level, CityBot.class, o, b -> b.district() == d))
					check(!bot.dormant(), d + "'s bots still asleep");
			});
		}
		seq.thenWaitUntil(() -> {
			check(!level.getBlockState(liftPos).getValue(LiftBlock.LOCKED), "the Quarters' lift is still locked");
			check(((LiftBlockEntity) level.getBlockEntity(liftPos)).landings().size() >= 3, "the lift has too few landings");
			for (BlockPos d : doors.get(District.HEART))
				check(level.getBlockState(d).getValue(SlidingDoorBlock.OPEN), "a heart door is shut with four awake");
			check(record.custodianAwake(o), "the Custodian didn't wake with the fourth district");
		});
		seq.thenExecute(() -> {
			for (ChunkPos c : cityChunks(o))
				level.setChunkForced(c.x, c.z, false);
		});
		seq.thenSucceed();
	}

	/** The ring of the district's insulator at this hop of its chain (1 mast, 2 tower, 3 crown) is lit. */
	private static boolean ringLit(ServerLevel level, BlockPos o, District d, int hop) {
		for (BlockEntity be : cityBlocks(level, o))
			if (be instanceof DerelictInsulatorBlockEntity ins && ins.cityLink().district() == d && ins.hop() == hop)
				return be.getBlockState().getValue(DerelictInsulatorBlock.LIT);
		throw new GameTestAssertException("no insulator at hop " + hop + " for the " + d);
	}

	// ================================================================ the city props (D72)

	/** A district's frame (u, v) of a world position: u out along x, v out along z, from 4 blocks off the axes. */
	private static int[] frame(BlockPos o, BlockPos p) {
		int dx = p.getX() - o.getX(), dz = p.getZ() - o.getZ();
		return new int[] { Math.abs(dx) - 4, Math.abs(dz) - 4 };
	}

	/**
	 * D72 in the city's plan (three seeds), as built (the last block put at each place): the Workshops' assembly line holds
	 * Assembly Clamps at stages 1-5 and no frozen Worker stands outside the heart; one alcove in each five the plan puts
	 * holds a trinket, of more than one kind; dead vines only in the Quarters' alcove wall and split tower, about one in
	 * four of them; each district has its yard mast's riser and a corner mast with its riser, every riser block beside an
	 * upright girder with the mast's insulator on top, all bound to the district; the corner mast's span ties to the yard
	 * mast's, on the bead's path as CityLayout says; a plaque in front of a wall beside every shed door, askew only out past
	 * PLAQUE_ASKEW_FROM.
	 */
	@GameTest(template = COURSE, timeoutTicks = 40)
	public static void cityPropsArePlacedAndBound(GameTestHelper helper) {
		for (long seed : new long[] { 0, 1, 12345 }) {
			BlockPos o = new BlockPos(1000, 70, -2000);
			CityPlan plan = CityPlan.of(o, seed, false);
			Map<BlockPos, CityPlan.Op.Put> last = new java.util.HashMap<>();
			List<CityPlan.Op.Put> alcoves = new ArrayList<>();
			Map<BlockPos, District> spanOwners = new java.util.HashMap<>();
			Map<BlockPos, Integer> hops = new java.util.HashMap<>();
			List<CityPlan.Op.Span> spans = new ArrayList<>();
			for (CityPlan.Op op : plan.ops())
				switch (op) {
					case CityPlan.Op.Put p -> {
						last.put(p.at(), p);
						if (p.state().is(ModWastes.CHARGING_ALCOVE.get()) && p.state().getValue(ThreePartBlock.PART) == ThreePartBlock.Part.BOTTOM)
							alcoves.add(p);
					}
					case CityPlan.Op.Span s -> spans.add(s);
					case CityPlan.Op.Hop h -> hops.put(h.at(), h.hop());
					default -> {}
				}
			// the assembly line: stages 1-5, all in the Workshops; the frozen pose only in the heart
			List<Integer> stages = new ArrayList<>();
			for (CityPlan.Op.Put p : last.values()) {
				BlockState s = p.state();
				if (s.is(ModWastes.ASSEMBLY_CLAMP.get())) {
					check(expected(o, p.at()) == District.WORKSHOPS, "a clamp outside the Workshops at " + p.at().subtract(o));
					stages.add(s.getValue(AssemblyClampBlock.STAGE));
				}
				if (s.is(ModWastes.WORKER_SHELL.get()) && s.getValue(ShellBlock.Worker.POSE) == ShellBlock.WorkerPose.FROZEN)
					check(expected(o, p.at()) == District.HEART, "a frozen Worker outside the heart at " + p.at().subtract(o));
			}
			stages.sort(null);
			check(stages.equals(List.of(1, 2, 3, 4, 5)), "seed " + seed + ": the assembly line's clamps are at stages " + stages);
			// trinkets: one in each five alcoves, in the order the plan puts them; vines only on the wall and split tower
			check(alcoves.size() >= 20, "only " + alcoves.size() + " alcoves");
			java.util.Set<ChargingAlcoveBlock.Trinket> kinds = java.util.EnumSet.noneOf(ChargingAlcoveBlock.Trinket.class);
			int vines = 0, vineSites = 0;
			for (int i = 0; i < alcoves.size(); i += CityPlan.TRINKET_EVERY) {
				int n = 0, group = Math.min(CityPlan.TRINKET_EVERY, alcoves.size() - i);
				for (int k = i; k < i + group; k++) {
					ChargingAlcoveBlock.Trinket t = alcoves.get(k).state().getValue(ChargingAlcoveBlock.TRINKET);
					if (t != ChargingAlcoveBlock.Trinket.NONE) {
						n++;
						kinds.add(t);
					}
				}
				check(group == CityPlan.TRINKET_EVERY ? n == 1 : n <= 1, "seed " + seed + ": alcoves " + i + "-" + (i + group - 1) + " hold " + n + " trinkets");
			}
			check(kinds.size() >= 2, "seed " + seed + ": every trinket is " + kinds);
			for (CityPlan.Op.Put p : alcoves) {
				// the whole alcove, as built, carries its trinket and vines in all three blocks
				for (int k = 0; k < 3; k++) {
					CityPlan.Op.Put part = last.get(p.at().above(k));
					check(part != null && part.state().is(ModWastes.CHARGING_ALCOVE.get())
						&& part.state().getValue(ChargingAlcoveBlock.TRINKET) == p.state().getValue(ChargingAlcoveBlock.TRINKET)
						&& part.state().getValue(ChargingAlcoveBlock.VINES) == p.state().getValue(ChargingAlcoveBlock.VINES),
						"the alcove at " + p.at().subtract(o) + " isn't whole as built");
				}
				int[] f = frame(o, p.at());
				boolean site = expected(o, p.at()) == District.QUARTERS && (f[1] == 40 && f[0] >= 38 && f[0] <= 52 || f[1] == 62 && (f[0] == 60 || f[0] == 63));
				if (site)
					vineSites++;
				if (p.state().getValue(ChargingAlcoveBlock.VINES)) {
					check(site, "dead vines on an alcove at " + p.at().subtract(o) + " (frame " + f[0] + ", " + f[1] + ")");
					vines++;
				}
			}
			check(vineSites == 51, vineSites + " alcoves in the Quarters' wall and split tower");
			check(vines >= 4 && vines <= 25, "seed " + seed + ": vines on " + vines + " of " + vineSites + " (about one in " + CityLayout.VINES_ONE_IN + ")");
			// risers: two masts a district, each riser block beside an upright girder, the insulator on top, bound
			Map<District, List<CityPlan.Op.Put>> risers = new EnumMap<>(District.class);
			for (CityPlan.Op.Put p : last.values())
				if (p.state().is(ModWastes.DERELICT_CABLE.get()) && p.state().getValue(DerelictCableBlock.GIRDER) != DerelictCableBlock.Girder.NONE) {
					check(p.district() == expected(o, p.at()), "a riser at " + p.at().subtract(o) + " is the " + p.district() + "'s");
					risers.computeIfAbsent(p.district(), k -> new ArrayList<>()).add(p);
				}
			for (District d : District.DISTRICTS) {
				List<CityPlan.Op.Put> rs = risers.getOrDefault(d, List.of());
				check(rs.size() == 2 * CityLayout.MAST_HEIGHT, d + " has " + rs.size() + " riser blocks");
				int feet = 0;
				for (CityPlan.Op.Put p : rs) {
					Direction side = p.state().getValue(DerelictCableBlock.SURFACE);
					BlockPos girder = p.at().relative(side);
					check(last.containsKey(girder) && DerelictCableBlock.uprightGirder(last.get(girder).state()), d + "'s riser at " + p.at().subtract(o) + " has no girder");
					DerelictCableBlock.Girder part = p.state().getValue(DerelictCableBlock.GIRDER);
					int y = p.at().getY() - o.getY();
					check(part == (y == 0 ? DerelictCableBlock.Girder.FOOT : y == CityLayout.MAST_HEIGHT - 1 ? DerelictCableBlock.Girder.TOP : DerelictCableBlock.Girder.RUN),
						d + "'s riser at " + p.at().subtract(o) + " is its " + part);
					if (part == DerelictCableBlock.Girder.TOP) {
						CityPlan.Op.Put ins = last.get(girder.above());
						check(ins != null && ins.state().is(ModWastes.DERELICT_INSULATOR.get()) && ins.state().getValue(DerelictInsulatorBlock.MOUNT) == DerelictInsulatorBlock.Mount.TOP
							&& ins.district() == d, d + "'s mast at " + girder.subtract(o) + " has no insulator of its own on top");
						spanOwners.put(girder.above(), d);
					}
					if (part == DerelictCableBlock.Girder.FOOT) {
						feet++;
						boolean joined = false;
						for (Direction a : Direction.Plane.HORIZONTAL)
							joined |= p.state().getValue(DerelictCableBlock.side(a));
						check(joined, d + "'s riser foot at " + p.at().subtract(o) + " joins no ground cable");
					}
				}
				check(feet == 2, d + " has " + feet + " riser feet");
			}
			// the corner masts' spans to the yard masts, on the bead's path (or not) as CityLayout says
			for (District d : District.DISTRICTS) {
				List<CityPlan.Op.Span> ds = spans.stream().filter(s -> expected(o, s.at()) == d || s.at().equals(o.offset(CityLayout.crownInsulator(d)))).toList();
				check(ds.size() == 4, d + " has " + ds.size() + " spans");
				List<BlockPos> masts = spanOwners.entrySet().stream().filter(e -> e.getValue() == d).map(Map.Entry::getKey).toList();
				check(masts.size() == 2, d + " has " + masts.size() + " masts");
				long cornerSpans = ds.stream().filter(s -> masts.contains(s.at()) && masts.contains(s.other())).count();
				check(cornerSpans == 1, d + ": " + cornerSpans + " spans between its corner mast and its yard mast");
				CityPlan.Op.Span corner = ds.stream().filter(s -> masts.contains(s.at()) && masts.contains(s.other())).findFirst().orElseThrow();
				int hop = hops.getOrDefault(corner.at(), -99);
				check(CityLayout.CORNER_MAST_ON_BEAD_PATH ? corner.stage() == 1 && hop == 2 && hops.get(corner.other()) == 1 : corner.stage() < 0 && hop == 0,
					d + "'s corner mast: stage " + corner.stage() + ", hop " + hop);
			}
			// plaques: on a wall, beside each shed door; askew only out past PLAQUE_ASKEW_FROM
			int plaques = 0;
			for (CityPlan.Op.Put p : last.values())
				if (p.state().is(ModWastes.BRASS_PLAQUE.get())) {
					plaques++;
					Direction out = p.state().getValue(BrassPlaqueBlock.FACING);
					CityPlan.Op.Put wall = last.get(p.at().relative(out.getOpposite()));
					check(wall != null && !wall.state().isAir() && !wall.state().is(ModWastes.BRASS_PLAQUE.get()), "a plaque at " + p.at().subtract(o) + " has no wall behind it");
					check(p.at().getY() - o.getY() == CityLayout.PLAQUE_Y, "a plaque at " + p.at().subtract(o) + " isn't " + CityLayout.PLAQUE_Y + " up");
					int[] f = frame(o, p.at().relative(out.getOpposite()));
					check(p.state().getValue(BrassPlaqueBlock.ASKEW) == Math.max(f[0], f[1]) >= CityLayout.PLAQUE_ASKEW_FROM,
						"the plaque at " + p.at().subtract(o) + " (frame " + f[0] + ", " + f[1] + ") askew " + p.state().getValue(BrassPlaqueBlock.ASKEW));
				}
			check(plaques >= 4 * 3, "seed " + seed + ": only " + plaques + " plaques");
		}
		helper.succeed();
	}

	// ================================================================ an outskirts-only site (P46, D69)

	/**
	 * An outskirts-only site's own Elytra (D69): its plan puts the resistance's cache (a barrel at the site's middle with the
	 * one-Elytra loot table) and binds nothing, no bot, no record; a city's plan has no cache (its Elytra is the heart
	 * vault's). Built a chunk at a time as world generation builds it, on the worst ground there is (only the middle column,
	 * the one world generation makes sure of, has land): the barrel stands, rolls exactly {@link CityLayout#OUTSKIRTS_ELYTRA}
	 * Elytra, and nothing else floats over the void.
	 */
	@GameTest(template = COURSE, timeoutTicks = 100, batch = "wastes_outskirts")
	public static void outskirtsSitesHoldOneElytra(GameTestHelper helper) {
		for (long seed : new long[] { 0, 1, 12345 }) {
			BlockPos o = new BlockPos(1000, 70, -2000), cache = o.offset(CityLayout.RESISTANCE_CACHE);
			int caches = 0;
			for (CityPlan.Op op : CityPlan.of(o, seed, true).ops()) {
				switch (op) {
					case CityPlan.Op.Loot l -> {
						check(l.table().equals(CityLayout.RESISTANCE_CACHE_LOOT) && l.at().equals(cache), "a loot op " + l);
						caches++;
					}
					case CityPlan.Op.Put p -> {
						if (p.at().equals(cache))
							check(p.state().is(Blocks.BARREL) && p.need() == CityPlan.ON_GROUND, "the cache is " + p);
						else
							check(p.need() != CityPlan.ANYWHERE, p.state() + " at " + p.at().subtract(o) + " could float: an outskirts-only site has no core");
						check(p.district() == null, "a bound block at an outskirts-only site: " + p.state());
						if (p.state().getBlock() instanceof net.minecraft.world.level.block.EntityBlock eb)
							check(!(eb.newBlockEntity(p.at(), p.state()) instanceof CityLink.Member), "a city block at an outskirts-only site: " + p.state());
					}
					case CityPlan.Op.Scuttler b -> check(false, "a Scuttler at an outskirts-only site");
					case CityPlan.Op.Sentry b -> check(false, "a Sentry at an outskirts-only site");
					case CityPlan.Op.Custodian b -> check(false, "a Custodian at an outskirts-only site");
					default -> {}
				}
			}
			check(caches == 1, caches + " Elytra caches at an outskirts-only site");
			check(CityPlan.of(o, seed, false).ops().stream().noneMatch(op -> op instanceof CityPlan.Op.Loot),
				"a city has an outskirts cache as well as its heart vault");
		}
		// built as world generation builds it, a chunk at a time, high over the void with land under the middle column only
		ServerLevel level = helper.getLevel();
		BlockPos o = new BlockPos(-24_000 - level.random.nextInt(256) * 1024, 290, 24_000 + level.random.nextInt(256) * 1024);
		List<ChunkPos> chunks = cityChunks(o);
		for (ChunkPos c : chunks)
			level.setChunkForced(c.x, c.z, true);
		level.setBlock(o.below(), Blocks.END_STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
		CityPlan plan = CityPlan.of(o, 7, true);
		for (ChunkPos c : chunks) {
			BoundingBox box = new BoundingBox(c.getMinBlockX(), level.getMinBuildHeight(), c.getMinBlockZ(), c.getMaxBlockX(),
				level.getMaxBuildHeight() - 1, c.getMaxBlockZ());
			// the live level's own surface (a loaded chunk keeps no world-generation heightmaps)
			new CityApplier.Gen(level, box, o) {
				@Override
				protected int terrainTop(int x, int z) {
					int h = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
					return h <= level.getMinBuildHeight() ? Integer.MIN_VALUE : h;
				}
			}.apply(plan.opsIn(c));
		}
		check(level.getBlockState(o).is(Blocks.BARREL), "no cache at the site's middle: " + level.getBlockState(o));
		check(level.getBlockEntity(o) instanceof RandomizableContainerBlockEntity be && CityLayout.RESISTANCE_CACHE_LOOT.equals(be.getLootTable()),
			"the cache has no loot table");
		RandomizableContainerBlockEntity barrel = (RandomizableContainerBlockEntity) level.getBlockEntity(o);
		barrel.unpackLootTable(null);
		int elytra = 0, other = 0;
		for (int slot = 0; slot < barrel.getContainerSize(); slot++) {
			var stack = barrel.getItem(slot);
			if (stack.is(Items.ELYTRA))
				elytra += stack.getCount();
			else if (!stack.isEmpty())
				other++;
		}
		check(elytra == CityLayout.OUTSKIRTS_ELYTRA && other == 0, "the cache holds " + elytra + " Elytra and " + other + " other stacks");
		// nothing stands over the void: every block above the floor is the cache's column
		int floating = 0;
		for (BlockPos p : BlockPos.betweenClosed(o.offset(-CityLayout.OUT, 0, -CityLayout.OUT), o.offset(CityLayout.OUT, 8, CityLayout.OUT)))
			if (!(p.getX() == o.getX() && p.getZ() == o.getZ()) && !level.getBlockState(p).isAir())
				floating++;
		check(floating == 0, floating + " outskirts blocks over the void");
		check(!CityRecord.of(level).cities().contains(o), "an outskirts-only site has a city record");
		for (ChunkPos c : chunks)
			level.setChunkForced(c.x, c.z, false);
		helper.succeed();
	}

	// ================================================================ the End

	/** The End as a seed would make it: its generator, noise, structure placement and the Wastes. */
	private record EndView(ServerLevel end, ChunkGenerator generator, RandomState random, ChunkGeneratorStructureState state, long seed) {
		boolean wastesAt(int x, int z) {
			return generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(64), QuartPos.fromBlock(z), random.sampler())
				.is(EndBiomes.CLOCKWORK_WASTES);
		}
	}

	/** Seeds the End tests check besides the game-test world's own (P45: parity over more than one seed). */
	static final long[] OTHER_SEEDS = { 1L, 42L, 8_675_309L, -20_241_029L };

	private static EndView end(GameTestHelper helper) {
		ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
		check(end != null, "no End");
		check(EndBiomes.checkEnd(end.getServer()), "TerraBlender's End isn't running");
		ChunkGeneratorStructureState state = end.getChunkSource().getGeneratorState();
		return new EndView(end, end.getChunkSource().getGenerator(), end.getChunkSource().randomState(), state, state.getLevelSeed());
	}

	/**
	 * The game-test world's End, then the same End as each of {@link #OTHER_SEEDS} would make it: a fresh TerraBlender End
	 * biome source seeded for it, the End's noise settings with that seed's islands, and its structure placement state.
	 * Nothing is generated; it's only sampled.
	 */
	private static List<EndView> ends(GameTestHelper helper) {
		EndView live = end(helper);
		List<EndView> out = new ArrayList<>(List.of(live));
		var access = live.end().registryAccess();
		var settings = ((NoiseBasedChunkGenerator) live.generator()).generatorSettings();
		for (long seed : OTHER_SEEDS) {
			TheEndBiomeSource source = TheEndBiomeSource.create(access.lookupOrThrow(Registries.BIOME));
			((terrablender.worldgen.IExtendedTheEndBiomeSource) source).initializeForTerraBlender(access, seed);
			NoiseBasedChunkGenerator gen = new NoiseBasedChunkGenerator(source, settings);
			RandomState random = RandomState.create(settings.value(), access.lookupOrThrow(Registries.NOISE), seed);
			var state = ChunkGeneratorStructureState.createForNormal(random, seed, source, access.lookupOrThrow(Registries.STRUCTURE_SET));
			out.add(new EndView(live.end(), gen, random, state, seed));
		}
		return out;
	}

	private static RandomSpreadStructurePlacement placement(ServerLevel level, ResourceKey<StructureSet> set) {
		StructurePlacement p = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).getOrThrow(set).placement();
		return (RandomSpreadStructurePlacement) p;
	}

	/** The structure set as data: spacing 32 (P45, D69), separation 12, 6 chunks from End cities; the Wastes not in End cities' biomes. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void theCitySetIsAsTheSpecSays(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		StructureSet set = level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET).getOrThrow(CITY_SET);
		RandomSpreadStructurePlacement p = (RandomSpreadStructurePlacement) set.placement();
		check(p.spacing() == 32 && p.spacing() == CityLayout.SPACING && p.separation() == 12, "spacing " + p.spacing() + ", separation " + p.separation());
		var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
		TagKey<Biome> endCity = TagKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("has_structure/end_city"));
		check(!biomes.getHolderOrThrow(EndBiomes.CLOCKWORK_WASTES).is(endCity), "End cities may start in the Wastes");
		TagKey<Biome> city = TagKey.create(Registries.BIOME, ComedyMonkeysCreateAddons.asResource("has_structure/clockwork_city"));
		check(biomes.getHolderOrThrow(EndBiomes.CLOCKWORK_WASTES).is(city), "cities can't start in the Wastes");
		// and off until the user approves the city (D66)
		check(!com.comedymonkey.createaddons.config.CMConfig.SERVER.clockworkCities.getDefault(), "cities generate by default");
		helper.succeed();
	}

	/**
	 * The exclusion zone (P2, D13): no city chunk the set allows has an End city start within 6 chunks of it, and the zone
	 * really turns some city chunks away. Checked over the outer End, 20,000 blocks each way, on every seed of {@link #ends}.
	 */
	@GameTest(template = COURSE, timeoutTicks = 40)
	public static void citiesKeepSixChunksFromEndCities(GameTestHelper helper) {
		for (EndView v : ends(helper)) {
			RandomSpreadStructurePlacement city = placement(v.end(), CITY_SET), endCity = placement(v.end(), BuiltinStructureSets.END_CITIES);
			long seed = v.seed();
			int allowed = 0, turnedAway = 0, cells = 20_000 / 16 / city.spacing();
			for (int i = -cells; i <= cells; i++)
				for (int j = -cells; j <= cells; j++) {
					ChunkPos c = city.getPotentialStructureChunk(seed, i * city.spacing(), j * city.spacing());
					if (!city.isStructureChunk(v.state(), c.x, c.z)) {
						turnedAway++;
						continue;
					}
					allowed++;
					for (int dx = -6; dx <= 6; dx++)
						for (int dz = -6; dz <= 6; dz++) {
							ChunkPos e = endCity.getPotentialStructureChunk(seed, c.x + dx, c.z + dz);
							check(!(e.x == c.x + dx && e.z == c.z + dz), "seed " + seed + ": an End city start at " + e + " is within 6 chunks of a city at " + c);
						}
				}
			check(allowed > 0 && turnedAway > 0, "seed " + seed + ": " + allowed + " city chunks allowed, " + turnedAway + " turned away by the End cities");
			LOG.info("Clockwork cities, seed {}: {} placements allowed, {} kept off End cities' 6-chunk zones", seed, allowed, turnedAway);
		}
		helper.succeed();
	}

	/** City placements the set allows whose heart is in the Wastes, out from the middle until there are {@code want}. */
	private static List<ChunkPos> wastesSites(EndView v, int want, int maxCells) {
		RandomSpreadStructurePlacement city = placement(v.end(), CITY_SET);
		long seed = v.seed();
		List<ChunkPos> out = new ArrayList<>();
		for (int ring = 0; ring <= maxCells && out.size() < want; ring++)
			for (int i = -ring; i <= ring && out.size() < want; i++)
				for (int j = -ring; j <= ring && out.size() < want; j++) {
					if (Math.max(Math.abs(i), Math.abs(j)) != ring)
						continue;
					ChunkPos c = city.getPotentialStructureChunk(seed, i * city.spacing(), j * city.spacing());
					if (city.isStructureChunk(v.state(), c.x, c.z) && v.wastesAt(c.getMiddleBlockX(), c.getMiddleBlockZ()))
						out.add(c);
				}
		return out;
	}

	/**
	 * Island fit (P46): 200 city sites in the Wastes on every seed of {@link #ends}, judged as world generation judges them
	 * (End Stone under the heart, and at least half the 9 x 9-chunk footprint End Stone at the heart's height). Reports how
	 * many fit (a city), how many get the outskirts only (D69: with their own Elytra) and how many nothing; some must fit.
	 */
	@GameTest(template = COURSE, timeoutTicks = 40)
	public static void islandFitSampling(GameTestHelper helper) {
		int fitAll = 0;
		for (EndView v : ends(helper)) {
			List<ChunkPos> sites = wastesSites(v, 200, 160);
			check(sites.size() >= 50, "seed " + v.seed() + ": only " + sites.size() + " city sites in the Wastes");
			int fit = 0, outskirts = 0, none = 0;
			double sum = 0;
			for (ChunkPos c : sites) {
				Optional<ClockworkCityStructure.Site> s = ClockworkCityStructure.site(v.generator(), v.random(), v.end(), c, 0.5);
				if (s.isEmpty()) {
					none++;
					continue;
				}
				check(s.get().fit() >= 0 && s.get().fit() <= 1, "a fit of " + s.get().fit());
				sum += s.get().fit();
				if (s.get().fits())
					fit++;
				else
					outskirts++;
			}
			LOG.info("Clockwork cities, seed {}, island fit over {} sites in the Wastes: {} fit, {} outskirts only (an Elytra each), {} no ground under the heart"
				+ " (mean footprint on land {})", v.seed(), sites.size(), fit, outskirts, none, String.format("%.2f", sum / Math.max(1, sites.size() - none)));
			fitAll += fit;
		}
		check(fitAll > 0, "no site in the Wastes fits a city");
		helper.succeed();
	}

	/** One seed's parity count (P45, D69). */
	record Parity(long seed, int cities, int outskirts, int displaced, int ships) {
		/** A city's heart vault holds one; an outskirts-only site's cache {@link CityLayout#OUTSKIRTS_ELYTRA}. */
		int elytra() {
			return cities + outskirts * CityLayout.OUTSKIRTS_ELYTRA;
		}
	}

	/**
	 * Elytra parity (P45, D18, D69): over the outer End, on every seed of {@link #ends}, the Wastes' sites that would place
	 * (allowed by the set and the exclusion zone, ground under the heart) each hold Elytra: a city one in its heart vault,
	 * an outskirts-only site (its island too small) its cache's. The End cities whose places the Wastes took (End-city
	 * starts in the Wastes, generated as if it were vanilla highlands) are counted for their ships. At spacing 32 the sites
	 * must give at least as many Elytra as the ships they replaced, on every seed.
	 */
	@GameTest(template = COURSE, timeoutTicks = 40)
	public static void elytraParity(GameTestHelper helper) {
		List<Parity> results = new ArrayList<>();
		for (EndView v : ends(helper))
			results.add(parity(v, 20_000));
		StringBuilder fails = new StringBuilder();
		for (Parity p : results) {
			LOG.info("Elytra parity, seed {}, over +-20000 blocks: {} Elytra ({} cities, {} outskirts-only sites) against {} End-city ships ({} End cities displaced)",
				p.seed(), p.elytra(), p.cities(), p.outskirts(), p.ships(), p.displaced());
			if (p.displaced() == 0)
				fails.append(" seed ").append(p.seed()).append(": no End city places in the Wastes to compare against;");
			else if (p.elytra() < p.ships())
				fails.append(" seed ").append(p.seed()).append(": ").append(p.elytra()).append(" Elytra against ").append(p.ships()).append(" ships;");
		}
		check(fails.isEmpty(), "Elytra parity fails:" + fails);
		helper.succeed();
	}

	private static Parity parity(EndView v, int range) {
		ServerLevel end = v.end();
		RandomSpreadStructurePlacement city = placement(end, CITY_SET), endCity = placement(end, BuiltinStructureSets.END_CITIES);
		long seed = v.seed();
		// the Wastes' sites at the set's own spacing
		int cities = 0, outskirts = 0, cells = range / 16 / city.spacing();
		for (int i = -cells; i <= cells; i++)
			for (int j = -cells; j <= cells; j++) {
				ChunkPos c = city.getPotentialStructureChunk(seed, i * city.spacing(), j * city.spacing());
				if (!v.wastesAt(c.getMiddleBlockX(), c.getMiddleBlockZ()) || !city.isStructureChunk(v.state(), c.x, c.z))
					continue;
				var s = ClockworkCityStructure.site(v.generator(), v.random(), end, c, 0.5);
				if (s.isPresent() && s.get().fits())
					cities++;
				else if (s.isPresent())
					outskirts++;
			}
		// the End cities the Wastes displaced, and their ships
		Structure endCityStructure = end.registryAccess().registryOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.END_CITY);
		StructurePieceSerializationContext ctx = StructurePieceSerializationContext.fromLevel(end);
		int displaced = 0, ships = 0;
		int endCells = range / 16 / endCity.spacing();
		for (int i = -endCells; i <= endCells; i++)
			for (int j = -endCells; j <= endCells; j++) {
				ChunkPos c = endCity.getPotentialStructureChunk(seed, i * endCity.spacing(), j * endCity.spacing());
				if (!endCity.isStructureChunk(v.state(), c.x, c.z) || !v.wastesAt(c.getMiddleBlockX(), c.getMiddleBlockZ()))
					continue;
				StructureStart start = endCityStructure.generate(end.registryAccess(), v.generator(), v.generator().getBiomeSource(), v.random(),
					end.getServer().getStructureManager(), seed, c, 0, end, b -> true);
				if (!start.isValid())
					continue;
				displaced++;
				for (StructurePiece piece : start.getPieces())
					if (piece instanceof EndCityPieces.EndCityPiece && piece.createTag(ctx).getString("Template").endsWith("ship")) {
						ships++;
						break;
					}
			}
		return new Parity(seed, cities, outskirts, displaced, ships);
	}
}
