package com.comedymonkey.createaddons.gametest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.wastes.ChargingAlcoveBlock;
import com.comedymonkey.createaddons.content.wastes.city.CityEdge;
import com.comedymonkey.createaddons.content.wastes.city.CityLayout;
import com.comedymonkey.createaddons.content.wastes.city.CityPlan;
import com.comedymonkey.createaddons.content.wastes.city.Overgrowth;
import com.comedymonkey.createaddons.content.wastes.city.kit.Wear;
import com.comedymonkey.createaddons.content.wastes.overgrowth.ShiftcapBlock;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The overgrowth (D91, D92, D94; design/briefs/clockwork-wastes-overgrowth.md): its density tiers and ring caps (no
 * Shiftcaps near the heart, some in the worn ring), the grip rule (nothing on void-alloy but Knotvine on a scarred plate),
 * the never list, one tree of each kind, a plan the same every time it's drawn, and what each plant drops. That every
 * plant stands on what it needs is checked on the built city ({@link #plantsSurvive}, from the dev city's test).
 */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WastesOvergrowthGameTests {
	private static final String COURSE = "trade_bot_course";
	private static final long[] SEEDS = { 0, 1, 12345 };
	private static final BlockPos O = new BlockPos(1000, 70, -2000);

	private WastesOvergrowthGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(WastesOvergrowthGameTests.class);
	}

	private static void check(boolean ok, String message) {
		if (!ok)
			throw new GameTestAssertException(message);
	}

	private static boolean overgrowth(BlockState s) {
		return ModWastes.overgrowth().stream().anyMatch(b -> s.is(b.get())) || s.is(ModWastes.TWINEROPE_PLANT.get());
	}

	/** A plant op: its place (a ground plant's column at the floor plus one), state, whether it's only on the floor, and whether it cracks its brick. */
	private record Plant(BlockPos at, BlockState state, boolean ground, boolean floorOnly, boolean cracks) {}

	/** The plan's overgrowth ops, and every block the plan puts as it ends up. */
	private static List<Plant> plants(CityPlan plan, Map<BlockPos, BlockState> world) {
		List<Plant> out = new ArrayList<>();
		for (CityPlan.Op op : plan.ops())
			switch (op) {
				case CityPlan.Op.Put put -> {
					world.put(put.at(), put.state());
					if (overgrowth(put.state()))
						out.add(new Plant(put.at(), put.state(), false, false, false));
				}
				case CityPlan.Op.Surface s -> out.add(new Plant(s.at(), s.state(), true, s.floorOnly(), s.cracks()));
				default -> {}
			}
		return out;
	}

	private static int ring(BlockPos o, BlockPos w) {
		return Wear.ring(w.getX() - o.getX(), w.getZ() - o.getZ());
	}

	// ================================================================ density

	/**
	 * The tiers follow the rings: the heart light (one medium patch by a door), the worn ring medium at most, heavy and bare
	 * both in the damaged ring; no Shiftcap, Bramblecoil, Breakroot or Twinerope in the heart; Shiftcaps out in the worn ring
	 * (D94, medium patches); past the terrace's edge only the light fringe's plants.
	 */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void densityTiersFollowTheRings(GameTestHelper helper) {
		boolean wornShiftcaps = false;
		for (long seed : SEEDS) {
			CityPlan plan = CityPlan.of(O, seed, false);
			Overgrowth.Tiers tiers = Overgrowth.tiers(plan);
			int[] door = Overgrowth.heartPatchAt(plan);
			check(door != null, "seed " + seed + ": no heart door for the heart's medium patch");
			int heavy = 0, bare = 0, damaged = 0, heartMedium = 0;
			for (int dx = -CityLayout.CORE; dx <= CityLayout.CORE; dx++)
				for (int dz = -CityLayout.CORE; dz <= CityLayout.CORE; dz++) {
					int r = Wear.ring(dx, dz);
					Overgrowth.Tier t = tiers.at(dx, dz);
					if (r <= Wear.INTACT_TO) {
						check(t != Overgrowth.Tier.HEAVY, "seed " + seed + ": heavy growth in the heart at " + dx + ", " + dz);
						if (t == Overgrowth.Tier.MEDIUM) {
							heartMedium++;
							check(Math.abs(dx - door[0]) <= Overgrowth.HEART_PATCH && Math.abs(dz - door[1]) <= Overgrowth.HEART_PATCH,
								"seed " + seed + ": medium growth in the heart at " + dx + ", " + dz + ", away from its door at " + door[0] + ", " + door[1]);
						}
					} else if (r <= Wear.WORN_TO)
						check(t != Overgrowth.Tier.HEAVY, "seed " + seed + ": heavy growth in the worn ring at " + dx + ", " + dz);
					else {
						damaged++;
						heavy += t == Overgrowth.Tier.HEAVY ? 1 : 0;
						bare += t == Overgrowth.Tier.BARE ? 1 : 0;
					}
				}
			check(heavy > damaged / 10 && bare > damaged / 20, "seed " + seed + ": the damaged ring is " + heavy + " heavy and " + bare + " bare of " + damaged);
			check(heartMedium > 0 && heartMedium <= 81, "seed " + seed + ": " + heartMedium + " medium columns in the heart (one patch by a door)");
			Map<BlockPos, BlockState> world = new HashMap<>();
			CityEdge edge = new CityEdge(plan);
			for (Plant p : plants(plan, world)) {
				int r = ring(O, p.at());
				BlockState s = p.state();
				boolean shiftcap = s.is(ModWastes.SHIFTCAP.get()) || s.is(ModWastes.SHIFTCAP_SHELF.get());
				if (r <= Wear.INTACT_TO)
					for (Block heavyOnly : new Block[] { ModWastes.SHIFTCAP.get(), ModWastes.SHIFTCAP_SHELF.get(), ModWastes.BRAMBLECOIL.get(),
						ModWastes.BREAKROOT_KNUCKLE.get(), ModWastes.BREAKROOT_RUN.get(), ModWastes.BREAKROOT_BURST.get(), ModWastes.TWINEROPE.get() })
						check(!s.is(heavyOnly), "seed " + seed + ": " + heavyOnly + " in the heart at " + p.at().subtract(O));
				if (shiftcap && r > Wear.INTACT_TO && r <= Wear.WORN_TO)
					wornShiftcaps = true;
				int dx = p.at().getX() - O.getX(), dz = p.at().getZ() - O.getZ();
				if (r > CityLayout.CORE && edge.across(dx, dz) > 0)
					check(s.is(ModWastes.CORKSCREW_RUSH.get()) || s.is(ModWastes.SPRINGCURL.get()) || s.is(ModWastes.DUSK_MOSS_CARPET.get())
						|| s.is(ModWastes.VIGIL_BELL.get()) || s.is(ModWastes.CROOKWOOD_LEAVES.get()) || s.is(ModWastes.CROOKWOOD_LOG.get())
						|| s.is(ModWastes.SHIELDCROWN_LEAVES.get()) || s.is(ModWastes.SHIELDCROWN_LOG.get()),
						"seed " + seed + ": " + s.getBlock() + " in the fringe past the terrace at " + dx + ", " + dz);
			}
		}
		check(wornShiftcaps, "no Shiftcaps in the worn ring (D94: they grow in medium patches)");
		helper.succeed();
	}

	// ================================================================ the grip rule and the never list

	private static boolean uprising(BlockState s) {
		String id = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
		return id.contains("void_alloy") || id.contains("seamed") || id.contains("slotted_grille") || id.contains("scarred") || id.contains("derelict")
			|| id.contains("sliding_door") || id.contains("city_vault") || id.contains("charging_alcove");
	}

	/**
	 * Plants hold to the makers' work, never the uprising's (the grip rule): every face of a cover, every shelf's wall, every
	 * rope's anchor and every moss carpet's bed; Knotvine on a scarred plate the one exception.
	 */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void plantsGripOnlyTheMakersWork(GameTestHelper helper) {
		for (long seed : SEEDS) {
			Map<BlockPos, BlockState> world = new HashMap<>();
			List<Plant> plants = plants(CityPlan.of(O, seed, false), world);
			int covers = 0;
			for (Plant p : plants) {
				if (p.ground())
					continue;
				BlockState s = p.state();
				List<BlockPos> holds = new ArrayList<>();
				if (s.getBlock() instanceof MultifaceBlock) {
					for (Direction d : Direction.values())
						if (s.getValue(MultifaceBlock.getFaceProperty(d)))
							holds.add(p.at().relative(d));
					covers++;
				} else if (s.is(ModWastes.SHIFTCAP_SHELF.get()))
					holds.add(p.at().relative(s.getValue(HorizontalDirectionalBlock.FACING).getOpposite()));
				else if (s.is(ModWastes.DUSK_MOSS_CARPET.get()))
					holds.add(p.at().below());
				else if (s.is(ModWastes.TWINEROPE_PLANT.get()) || s.is(ModWastes.TWINEROPE.get())) {
					BlockState above = world.get(p.at().above());
					if (above == null || !above.is(ModWastes.TWINEROPE_PLANT.get()))
						holds.add(p.at().above());
				}
				for (BlockPos h : holds) {
					BlockState on = world.get(h);
					check(on != null, "seed " + seed + ": " + s.getBlock() + " at " + p.at().subtract(O) + " holds to nothing the plan put");
					boolean ok = !uprising(on) || s.is(ModWastes.KNOTVINE.get()) && on.is(ModWastes.SCARRED_VOID_ALLOY_PLATE.get());
					check(ok, "seed " + seed + ": " + s.getBlock() + " at " + p.at().subtract(O) + " grips the uprising's " + on.getBlock());
				}
			}
			check(covers > 200, "seed " + seed + ": only " + covers + " Knotvine and Scale Lichen covers");
		}
		helper.succeed();
	}

	/**
	 * The never list: nothing grows within reach of a sliding door, vault, Substation, insulator, pylon, lift, vent or a
	 * lamp's head, nothing on Key-Marked Stone, nothing in the heart's hall (the Custodian's arena).
	 */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void nothingGrowsWhereItMustNot(GameTestHelper helper) {
		for (long seed : SEEDS) {
			Map<BlockPos, BlockState> world = new HashMap<>();
			List<Plant> plants = plants(CityPlan.of(O, seed, false), world);
			for (Plant p : plants) {
				int dx = p.at().getX() - O.getX(), dz = p.at().getZ() - O.getZ();
				check(!(Math.abs(dx) <= 15 && Math.abs(dz) <= 15 && Math.abs(dx) + Math.abs(dz) <= 27),
					"seed " + seed + ": " + p.state().getBlock() + " in the heart's hall (the Custodian's arena) at " + dx + ", " + dz);
				for (BlockPos q : BlockPos.betweenClosed(p.at().offset(-1, -1, -1), p.at().offset(1, 1, 1))) {
					BlockState n = world.get(q);
					if (n == null)
						continue;
					String id = BuiltInRegistries.BLOCK.getKey(n.getBlock()).getPath();
					boolean never = id.equals("sliding_door") || id.equals("city_vault") || id.equals("substation") || id.equals("derelict_insulator")
						|| id.equals("conduit_pylon") || id.equals("lift") || id.equals("wall_vent")
						|| id.equals("derelict_street_lamp") && n.getValue(com.comedymonkey.createaddons.content.wastes.DerelictStreetLampBlock.PART)
							== com.comedymonkey.createaddons.content.wastes.DerelictStreetLampBlock.Part.HEAD;
					check(!never, "seed " + seed + ": " + p.state().getBlock() + " at " + p.at().subtract(O) + " beside " + id);
					if (n.is(ModWastes.KEY_MARKED_STONE.get()) && p.state().getBlock() instanceof MultifaceBlock)
						for (Direction d : Direction.values())
							check(!(p.at().relative(d).equals(q.immutable()) && p.state().getValue(MultifaceBlock.getFaceProperty(d))),
								"seed " + seed + ": a cover over Key-Marked Stone at " + p.at().subtract(O));
				}
			}
		}
		helper.succeed();
	}

	/**
	 * D87: a prop stands in every cell its model runs through (the winding tower's spring and rafters): no plant, cover,
	 * carpet, tree or ground plant in any of them.
	 */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void nothingGrowsInAPropsReach(GameTestHelper helper) {
		for (long seed : SEEDS) {
			CityPlan plan = CityPlan.of(O, seed, false);
			Map<BlockPos, BlockState> world = new HashMap<>();
			java.util.Set<BlockPos> reach = new java.util.HashSet<>();
			List<CityPlan.Op> ops = plan.ops();
			for (int i = 0; i < Math.min(ops.size(), plan.overgrowthFrom); i++)
				if (ops.get(i) instanceof CityPlan.Op.Put put)
					world.put(put.at(), put.state());
			for (var e : world.entrySet())
				if (e.getValue().getBlock() instanceof com.comedymonkey.createaddons.content.wastes.PropPieceBlock)
					reach.addAll(com.comedymonkey.createaddons.content.wastes.PropPieceBlock.reach(e.getKey(), e.getValue()));
			check(reach.size() > 30, "seed " + seed + ": only " + reach.size() + " cells in the props' reach (no winding tower?)");
			for (CityPlan.Op op : ops.subList(plan.overgrowthFrom, ops.size()))
				check(!reach.contains(op.at()), "seed " + seed + ": " + op + " grows in a prop's reach at " + op.at().subtract(O));
		}
		helper.succeed();
	}

	/**
	 * D92: a Breakroot Burst cracks the brick it bursts from only as it grows (the applier cracks it with the burst), so the
	 * overgrowth puts no cracked brick of its own; each cracking burst's brick is a Stained End Stone Brick in its own chunk.
	 * (The kit's walls stand on deepslate plinths, so in the city a burst seldom or never bursts from that brick.)
	 */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void burstsCrackTheirBrickOnlyAsTheyGrow(GameTestHelper helper) {
		for (long seed : SEEDS) {
			CityPlan plan = CityPlan.of(O, seed, false);
			Map<BlockPos, BlockState> world = new HashMap<>();
			List<CityPlan.Op> ops = plan.ops();
			for (int i = 0; i < Math.min(ops.size(), plan.overgrowthFrom); i++)
				if (ops.get(i) instanceof CityPlan.Op.Put put)
					world.put(put.at(), put.state());
			for (CityPlan.Op op : ops.subList(plan.overgrowthFrom, ops.size())) {
				if (op instanceof CityPlan.Op.Put put)
					check(!put.state().is(ModWastes.CRACKED_STAINED_END_STONE_BRICKS.get()), "seed " + seed + ": the overgrowth puts a cracked brick at " + put.at().subtract(O));
				if (op instanceof CityPlan.Op.Surface s && s.cracks()) {
					check(s.state().is(ModWastes.BREAKROOT_BURST.get()) && s.floorOnly(), "seed " + seed + ": " + s.state() + " cracks a brick at " + s.at().subtract(O));
					BlockPos brick = s.at().relative(s.state().getValue(com.comedymonkey.createaddons.content.wastes.overgrowth.BreakrootBurstBlock.FACING).getOpposite());
					check(world.getOrDefault(brick, Blocks.AIR.defaultBlockState()).is(ModWastes.STAINED_END_STONE_BRICKS.get())
						&& brick.getX() >> 4 == s.at().getX() >> 4 && brick.getZ() >> 4 == s.at().getZ() >> 4,
						"seed " + seed + ": the burst at " + s.at().subtract(O) + " cracks no brick of its own chunk");
				}
			}
		}
		helper.succeed();
	}

	// ================================================================ the trees, the alcoves, the same every time

	/**
	 * One Crookwood (its trunk's foot in the grapple) and one Shieldcrown a city; the alcoves' vines dead or living, living
	 * only where the growth is heavy; the overgrowth the same each time the plan's drawn.
	 */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void oneTreeOfEachAndTheSamePlanEveryTime(GameTestHelper helper) {
		for (long seed : SEEDS) {
			Map<BlockPos, BlockState> world = new HashMap<>();
			CityPlan plan = CityPlan.of(O, seed, false);
			Overgrowth.Tiers tiers = Overgrowth.tiers(plan);
			List<Plant> plants = plants(plan, world);
			for (Block log : new Block[] { ModWastes.CROOKWOOD_LOG.get(), ModWastes.SHIELDCROWN_LOG.get() }) {
				long feet = world.entrySet().stream().filter(e -> e.getValue().is(log) && e.getKey().getY() == O.getY()).count();
				check(feet == 1, "seed " + seed + ": " + feet + " " + log + " feet");
			}
			// each tree whole: the Crookwood's every log, the Shieldcrown's stem unbroken from its foot past the old roof line
			long crook = world.values().stream().filter(st -> st.is(ModWastes.CROOKWOOD_LOG.get())).count();
			check(crook == 27, "seed " + seed + ": the Crookwood has " + crook + " of its 27 logs");
			BlockPos foot = world.entrySet().stream().filter(e -> e.getValue().is(ModWastes.SHIELDCROWN_LOG.get()) && e.getKey().getY() == O.getY())
				.map(Map.Entry::getKey).findFirst().orElseThrow();
			int stem = 0;
			while (world.getOrDefault(foot.above(stem), Blocks.AIR.defaultBlockState()).is(ModWastes.SHIELDCROWN_LOG.get()))
				stem++;
			check(stem >= 12, "seed " + seed + ": the Shieldcrown's stem is " + stem + " high from its foot");
			int[] g = CityLayout.grapple(true);
			check(world.getOrDefault(O.offset(g[0] + 13, 0, g[1] + 1), Blocks.AIR.defaultBlockState()).is(ModWastes.CROOKWOOD_LOG.get()),
				"seed " + seed + ": the Crookwood isn't in the grapple");
			for (var e : world.entrySet())
				if (e.getValue().getBlock() instanceof ChargingAlcoveBlock && e.getValue().getValue(ChargingAlcoveBlock.VINES) == ChargingAlcoveBlock.Vines.LIVING) {
					int dx = e.getKey().getX() - O.getX(), dz = e.getKey().getZ() - O.getZ();
					check(tiers.at(dx, dz) == Overgrowth.Tier.HEAVY, "seed " + seed + ": living vines on an alcove in light growth at " + dx + ", " + dz);
				}
			List<Plant> again = plants(CityPlan.of(O, seed, false), new HashMap<>());
			check(again.equals(plants), "seed " + seed + ": the overgrowth came out different drawn twice");
			check(plants.stream().anyMatch(p -> p.state().is(ModWastes.SHIFTCAP.get()) && p.state().getValue(ShiftcapBlock.AGE) == ShiftcapBlock.Age.OLD),
				"seed " + seed + ": no old Shiftcaps");
		}
		helper.succeed();
	}

	// ================================================================ drops (D92: thin plants with shears or Silk Touch)

	/** The thin plants drop themselves only to shears or Silk Touch; the rest drop themselves to a bare hand. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void plantsDropAsTheBriefSays(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
		ItemStack hand = ItemStack.EMPTY, shears = new ItemStack(Items.SHEARS), silk = new ItemStack(Items.DIAMOND_PICKAXE);
		silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
		Map<Block, Boolean> thin = new java.util.LinkedHashMap<>();
		for (Block b : new Block[] { ModWastes.KNOTVINE.get(), ModWastes.SCALE_LICHEN.get(), ModWastes.TWINEROPE.get(), ModWastes.DUSK_MOSS_CARPET.get(),
			ModWastes.SPRINGCURL.get(), ModWastes.CORKSCREW_RUSH.get(), ModWastes.CROOKWOOD_LEAVES.get(), ModWastes.SHIELDCROWN_LEAVES.get() })
			thin.put(b, true);
		for (Block b : new Block[] { ModWastes.DUSK_MOSS.get(), ModWastes.VIGIL_BELL.get(), ModWastes.BRAMBLECOIL.get(), ModWastes.SHIFTCAP.get(),
			ModWastes.SHIFTCAP_SHELF.get(), ModWastes.BREAKROOT_KNUCKLE.get(), ModWastes.BREAKROOT_RUN.get(), ModWastes.BREAKROOT_BURST.get(),
			ModWastes.CROOKWOOD_LOG.get(), ModWastes.SHIELDCROWN_LOG.get() })
			thin.put(b, false);
		for (var e : thin.entrySet()) {
			BlockState s = e.getKey().defaultBlockState();
			if (s.getBlock() instanceof MultifaceBlock)
				s = s.setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true);
			Item item = e.getKey().asItem();
			for (ItemStack tool : new ItemStack[] { hand, shears, silk }) {
				List<ItemStack> drops = Block.getDrops(s, level, pos, null, null, tool);
				boolean self = drops.stream().anyMatch(d -> d.is(item));
				boolean want = !e.getValue() || tool != hand;
				check(self == want, e.getKey() + " with " + (tool.isEmpty() ? "a bare hand" : tool.getItem()) + (want ? " drops nothing of itself" : " drops itself"));
			}
		}
		// the rope above its tip gives the rope; a pot gives the pot and its plant
		check(Block.getDrops(ModWastes.TWINEROPE_PLANT.get().defaultBlockState(), level, pos, null, null, shears).stream().anyMatch(d -> d.is(ModWastes.TWINEROPE.get().asItem())),
			"the Twinerope's body doesn't give the rope");
		List<ItemStack> pot = Block.getDrops(ModWastes.POTTED_SHIFTCAP.get().defaultBlockState(), level, pos, null, null, hand);
		check(pot.stream().anyMatch(d -> d.is(Items.FLOWER_POT)) && pot.stream().anyMatch(d -> d.is(ModWastes.SHIFTCAP.get().asItem())), "a potted Shiftcap gives " + pot);
		helper.succeed();
	}

	// ================================================================ the built city

	/**
	 * Every plant the plan grows, as built into {@code level} round {@code o}: found where it was planned, as planned, and on
	 * what it needs ({@code canSurvive}). A put plant is at its place; a ground plant on its column's ground, the lowest match
	 * within the outskirts' reach of the floor (exactly on the floor where it leans on a wall). A plant may be missing only
	 * where its column has no ground: a core column decked over the void, an outskirts column with nothing near the floor,
	 * or a top the applier won't plant on (not sturdy: a scree slab, a rail).
	 * Called from the dev city's test, which builds one.
	 */
	public static void plantsSurvive(ServerLevel level, BlockPos o, long seed) {
		Map<BlockPos, BlockState> world = new HashMap<>();
		List<Plant> plants = plants(CityPlan.of(o, seed, false), world);
		java.util.Set<BlockPos> putPlants = new java.util.HashSet<>();
		for (Plant p : plants)
			if (!p.ground())
				putPlants.add(p.at());
		int below = com.comedymonkey.createaddons.content.wastes.city.CityApplier.OUTSKIRT_BELOW,
			above = com.comedymonkey.createaddons.content.wastes.city.CityApplier.OUTSKIRT_ABOVE;
		int found = 0;
		List<String> lost = new ArrayList<>();
		for (Plant p : plants) {
			BlockPos at = null;
			if (!p.ground())
				at = same(level.getBlockState(p.at()), p.state()) ? p.at() : null;
			else
				for (int dy = p.floorOnly() ? 0 : -below; at == null && dy <= (p.floorOnly() ? 0 : above + 1); dy++) {
					BlockPos q = p.at().above(dy);
					if (!putPlants.contains(q) && same(level.getBlockState(q), p.state()) && !level.getBlockState(q.below()).isAir())
						at = q;
				}
			// a burst cracks its brick only if it grew (D92): never a cracked brick by no root
			BlockPos brick = p.cracks() ? p.at().relative(p.state().getValue(com.comedymonkey.createaddons.content.wastes.overgrowth.BreakrootBurstBlock.FACING).getOpposite()) : null;
			if (brick != null)
				check(level.getBlockState(brick).is(ModWastes.CRACKED_STAINED_END_STONE_BRICKS.get()) == (at != null),
					"the brick behind the burst at " + p.at().subtract(o) + " is " + level.getBlockState(brick).getBlock() + (at != null ? " (it grew)" : " (it didn't grow)"));
			if (at == null) {
				if (!groundless(level, o, p.at(), below, above))
					lost.add(p.state().getBlock() + " at " + p.at().subtract(o).toShortString() + " (" + level.getBlockState(p.at()).getBlock() + " there)");
				continue;
			}
			found++;
			BlockState s = level.getBlockState(at);
			check(s.canSurvive(level, at), s.getBlock() + " at " + at.subtract(o) + " has nothing to stand on (" + level.getBlockState(at.below()).getBlock() + " below)");
		}
		if (!lost.isEmpty())
			try {
				java.nio.file.Files.write(java.nio.file.Path.of("overgrowth-lost.txt"), lost);
			} catch (java.io.IOException ignored) {
			}
		check(lost.isEmpty(), lost.size() + " planned plants missing where their column has ground: " + String.join(", ", lost.subList(0, Math.min(8, lost.size()))));
		check(found > plants.size() / 2, "only " + found + " of " + plants.size() + " planned plants built");
	}

	/**
	 * A column with no ground for a plant: in the core, decked over the void (void-alloy or nothing under the floor); in the
	 * outskirts, nothing near the floor, or a bridge's deck (void-alloy) its top there.
	 */
	private static boolean groundless(ServerLevel level, BlockPos o, BlockPos at, int below, int above) {
		BlockPos col = new BlockPos(at.getX(), o.getY(), at.getZ());
		if (Math.max(Math.abs(at.getX() - o.getX()), Math.abs(at.getZ() - o.getZ())) <= CityLayout.CORE) {
			BlockState floor = level.getBlockState(col.below());
			return floor.isAir() || BuiltInRegistries.BLOCK.getKey(floor.getBlock()).getPath().contains("void_alloy")
				|| !floor.isFaceSturdy(level, col.below(), Direction.UP);
		}
		// (a bridge's void-alloy deck isn't ground)
		for (int dy = above; dy >= -below - 1; dy--) {
			BlockPos p = col.above(dy);
			BlockState s = level.getBlockState(p);
			if (!s.isAir() && !overgrowth(s))
				return BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath().contains("void_alloy") || !s.isFaceSturdy(level, p, Direction.UP);
		}
		return true;
	}

	/** The same block in the same state, but for what the world changes on its own (a leaf's distance, water). */
	private static boolean same(BlockState built, BlockState planned) {
		if (!built.is(planned.getBlock()))
			return false;
		for (var prop : planned.getProperties())
			if (prop != LeavesBlock.DISTANCE && prop != net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED
				&& !built.getValue(prop).equals(planned.getValue(prop)))
				return false;
		return true;
	}
}
