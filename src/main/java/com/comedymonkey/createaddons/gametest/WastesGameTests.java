package com.comedymonkey.createaddons.gametest;

import java.util.List;
import java.util.UUID;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.wastes.AssemblyClampBlock;
import com.comedymonkey.createaddons.content.wastes.BrassPlaqueBlock;
import com.comedymonkey.createaddons.content.wastes.DerelictCableBlock;
import com.comedymonkey.createaddons.content.wastes.EndBiomes;
import com.comedymonkey.createaddons.content.wastes.ShellBlock;
import com.comedymonkey.createaddons.content.wastes.ThreePartBlock;
import com.comedymonkey.createaddons.registry.ModWastes;
import com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import terrablender.api.EndBiomeRegistry;

/**
 * The Clockwork Wastes, phase 1 (design/specs/clockwork-wastes.md): wrench salvage (by a player's sneak-click and by a
 * Deployer) gives the full salvage and removes the block, any other break gives one Clockwork Scrap, the decor drops
 * itself (a three-long one once), and the biome is in TerraBlender's highlands and midlands with the End able to hold it.
 */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WastesGameTests {
	private static final String COURSE = "trade_bot_course";
	private static final BlockPos AT = new BlockPos(2, 1, 2);

	private WastesGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(WastesGameTests.class);
	}

	private static Item createItem(String id) {
		return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create", id));
	}

	private static List<ItemEntity> drops(GameTestHelper helper, BlockPos pos) {
		return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(1.5));
	}

	private static int count(List<ItemEntity> drops, Item item) {
		return drops.stream().filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
	}

	private static int countCreate(List<ItemEntity> drops) {
		return drops.stream().filter(e -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).getNamespace().equals("create"))
			.mapToInt(e -> e.getItem().getCount()).sum();
	}

	/** A player sneak-clicks the block with a Create Wrench, through the Wrench's own use (as the game routes it). */
	private static void sneakWrench(GameTestHelper helper, BlockPos pos) {
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setShiftKeyDown(true);
		ItemStack wrench = new ItemStack(createItem("wrench"));
		player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
		BlockPos abs = helper.absolutePos(pos);
		wrench.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
	}

	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void wrenchSalvagesBrokenMachine(GameTestHelper helper) {
		helper.setBlock(AT, ModWastes.BROKEN_PRESS.get().defaultBlockState());
		sneakWrench(helper, AT);
		helper.assertBlockPresent(Blocks.AIR, AT);
		List<ItemEntity> drops = drops(helper, AT);
		int scrap = count(drops, ModWastes.CLOCKWORK_SCRAP.get()), parts = countCreate(drops);
		helper.assertTrue(scrap >= 1 && scrap <= 2, "1-2 scrap from a full salvage, got " + scrap);
		helper.assertTrue(parts >= 1 && parts <= 4, "1-3 Create parts (and maybe a Precision Mechanism), got " + parts);
		helper.succeed();
	}

	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void wrenchSalvagesShell(GameTestHelper helper) {
		helper.setBlock(AT, ModWastes.WORKER_SHELL.get().defaultBlockState().setValue(ShellBlock.Worker.POSE, ShellBlock.WorkerPose.FALLEN));
		sneakWrench(helper, AT);
		helper.assertBlockPresent(Blocks.AIR, AT);
		List<ItemEntity> drops = drops(helper, AT);
		int scrap = count(drops, ModWastes.CLOCKWORK_SCRAP.get()), tubes = countCreate(drops);
		helper.assertTrue(scrap >= 2 && scrap <= 3, "2-3 scrap from a bot shell, got " + scrap);
		helper.assertTrue(tubes <= 1 && tubes == count(drops, createItem("electron_tube")), "at most an Electron Tube besides, got " + tubes);
		helper.succeed();
	}

	/** A Deployer holding a Wrench clicks the block as Create's DeployerHandler does (the block's use, by its fake player). */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void deployerWithWrenchSalvages(GameTestHelper helper) {
		helper.setBlock(AT, ModWastes.BROKEN_MECHANICAL_ARM.get().defaultBlockState());
		DeployerFakePlayer deployer = new DeployerFakePlayer(helper.getLevel(), UUID.randomUUID());
		ItemStack wrench = new ItemStack(createItem("wrench"));
		deployer.setItemInHand(InteractionHand.MAIN_HAND, wrench);
		BlockPos abs = helper.absolutePos(AT);
		helper.getBlockState(AT).useItemOn(wrench, helper.getLevel(), deployer, InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
		helper.assertBlockPresent(Blocks.AIR, AT);
		int scrap = count(drops(helper, AT), ModWastes.CLOCKWORK_SCRAP.get());
		helper.assertTrue(scrap >= 1 && scrap <= 2, "the Deployer's salvage gives 1-2 scrap, got " + scrap);
		helper.assertTrue(countCreate(drops(helper, AT)) >= 1, "the Deployer's salvage gives Create parts");
		helper.succeed();
	}

	/** A plain (unsneaking) player's wrench click turns it, as any wrenchable block; it isn't salvaged. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void plainWrenchClickTurns(GameTestHelper helper) {
		helper.setBlock(AT, ModWastes.BROKEN_MIXER.get().defaultBlockState());
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack wrench = new ItemStack(createItem("wrench"));
		player.setItemInHand(InteractionHand.MAIN_HAND, wrench);
		BlockPos abs = helper.absolutePos(AT);
		BlockState before = helper.getBlockState(AT);
		before.useItemOn(wrench, helper.getLevel(), player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
		helper.assertBlockPresent(ModWastes.BROKEN_MIXER.get(), AT);
		helper.assertTrue(drops(helper, AT).isEmpty(), "a player's plain click salvages nothing");
		helper.succeed();
	}

	/** A pickaxe, drill, saw or blast breaks it into one scrap (its ordinary loot table). */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void otherBreaksGiveOneScrap(GameTestHelper helper) {
		BlockPos pick = AT, drill = AT.east(3);
		helper.setBlock(pick, ModWastes.BROKEN_DEPLOYER.get().defaultBlockState());
		helper.setBlock(drill, ModWastes.RESISTANCE_SHELL.get().defaultBlockState());
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
		BlockPos abs = helper.absolutePos(pick);
		BlockState state = helper.getBlockState(pick);
		helper.assertTrue(state.is(BlockTags.MINEABLE_WITH_PICKAXE), "salvage is mined with a pickaxe");
		Block.dropResources(state, helper.getLevel(), abs, null, player, pickaxe);
		helper.getLevel().removeBlock(abs, false);
		// a contraption's drill or a blast breaks it without a player
		helper.getLevel().destroyBlock(helper.absolutePos(drill), true);
		helper.assertTrue(count(drops(helper, pick), ModWastes.CLOCKWORK_SCRAP.get()) == 1 && drops(helper, pick).size() == 1,
			"a pickaxe gives exactly one scrap");
		helper.assertTrue(count(drops(helper, drill), ModWastes.CLOCKWORK_SCRAP.get()) == 1 && drops(helper, drill).size() == 1,
			"a drill gives exactly one scrap");
		helper.succeed();
	}

	/** Decor drops itself; a three-long block broken at its top drops once and takes its other parts with it. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void decorDropsItself(GameTestHelper helper) {
		BlockPos plate = AT.east(4);
		helper.setBlock(plate, ModWastes.SEAMED_VOID_ALLOY_PLATE.get().defaultBlockState());
		helper.getLevel().destroyBlock(helper.absolutePos(plate), true);
		helper.assertTrue(count(drops(helper, plate), ModWastes.SEAMED_VOID_ALLOY_PLATE.asItem()) == 1, "a plate drops itself");

		BlockState alcove = ModWastes.CHARGING_ALCOVE.get().defaultBlockState();
		for (ThreePartBlock.Part part : ThreePartBlock.Part.values())
			helper.setBlock(AT.above(part.ordinal()), alcove.setValue(ThreePartBlock.PART, part));
		helper.getLevel().destroyBlock(helper.absolutePos(AT.above(2)), true);
		for (int k = 0; k < 3; k++)
			helper.assertBlockPresent(Blocks.AIR, AT.above(k));
		int dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(AT)).inflate(1.5).expandTowards(0, 3, 0))
			.stream().filter(e -> e.getItem().is(ModWastes.CHARGING_ALCOVE.asItem())).mapToInt(e -> e.getItem().getCount()).sum();
		helper.assertTrue(dropped == 1, "a Charging Alcove drops once, got " + dropped);
		helper.succeed();
	}

	/** The lying statue's parts run east of a north-facing one, and stand or fall together. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void toppledStatueRunsSideways(GameTestHelper helper) {
		ThreePartBlock block = ModWastes.TOPPLED_MAKER_STATUE.get();
		helper.assertTrue(block.run(Direction.NORTH) == Direction.EAST && block.run(Direction.EAST) == Direction.SOUTH,
			"a toppled statue lies toward its facing's clockwise side");
		BlockState state = block.defaultBlockState().setValue(ThreePartBlock.FACING, Direction.NORTH);
		for (ThreePartBlock.Part part : ThreePartBlock.Part.values())
			helper.setBlock(AT.east(part.ordinal()), state.setValue(ThreePartBlock.PART, part));
		helper.getLevel().destroyBlock(helper.absolutePos(AT), true);
		helper.assertBlockPresent(Blocks.AIR, AT.east(1));
		helper.assertBlockPresent(Blocks.AIR, AT.east(2));
		helper.succeed();
	}

	/** P1, D28: the Wastes is in TerraBlender's highlands and midlands at its configured weight, and this End can hold it. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void biomeInTerraBlenderSlots(GameTestHelper helper) {
		int weight = CMConfig.COMMON.clockworkWastesWeight();
		helper.assertTrue(weight > 0, "the Wastes is on by default");
		helper.assertTrue(EndBiomeRegistry.getHighlandsBiomes().stream().anyMatch(w -> w.data().equals(EndBiomes.CLOCKWORK_WASTES) && w.getWeight().asInt() == weight),
			"the Wastes is in the highlands at weight " + weight);
		helper.assertTrue(EndBiomeRegistry.getMidlandsBiomes().stream().anyMatch(w -> w.data().equals(EndBiomes.CLOCKWORK_WASTES) && w.getWeight().asInt() == weight),
			"the Wastes is in the midlands at weight " + weight);
		helper.assertTrue(EndBiomeRegistry.getIslandBiomes().stream().noneMatch(w -> w.data().equals(EndBiomes.CLOCKWORK_WASTES)),
			"never the island slot (bugged on 1.21.1)");
		if (!EndBiomes.checkEnd(helper.getLevel().getServer()))
			throw new GameTestAssertException("the test server's End can't hold the Wastes");
		helper.succeed();
	}

	// ================================================================ the city props (D72)

	/** The Assembly Clamp (D72) salvages like a Worker Shell: the wrench gives 2-3 scrap and at most an Electron Tube; a pickaxe one scrap. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void wrenchSalvagesAssemblyClamp(GameTestHelper helper) {
		BlockPos pick = AT.east(3);
		for (int stage = 1; stage <= 5; stage++) {
			helper.setBlock(AT, ModWastes.ASSEMBLY_CLAMP.get().defaultBlockState().setValue(AssemblyClampBlock.STAGE, stage));
			helper.assertTrue(helper.getBlockState(AT).is(ModWastes.ASSEMBLY_CLAMP.get()), "no clamp at stage " + stage);
			sneakWrench(helper, AT);
			helper.assertBlockPresent(Blocks.AIR, AT);
			List<ItemEntity> drops = drops(helper, AT);
			int scrap = count(drops, ModWastes.CLOCKWORK_SCRAP.get()), tubes = countCreate(drops);
			helper.assertTrue(scrap >= 2 && scrap <= 3, "stage " + stage + ": 2-3 scrap from the clamp, got " + scrap);
			helper.assertTrue(tubes <= 1 && tubes == count(drops, createItem("electron_tube")), "stage " + stage + ": at most an Electron Tube besides, got " + tubes);
			drops.forEach(ItemEntity::discard);
		}
		helper.setBlock(pick, ModWastes.ASSEMBLY_CLAMP.get().defaultBlockState());
		BlockState state = helper.getBlockState(pick);
		helper.assertTrue(state.is(BlockTags.MINEABLE_WITH_PICKAXE), "the clamp is mined with a pickaxe");
		Block.dropResources(state, helper.getLevel(), helper.absolutePos(pick), null, helper.makeMockPlayer(GameType.SURVIVAL), new ItemStack(Items.IRON_PICKAXE));
		helper.getLevel().removeBlock(helper.absolutePos(pick), false);
		helper.assertTrue(count(drops(helper, pick), ModWastes.CLOCKWORK_SCRAP.get()) == 1 && drops(helper, pick).size() == 1, "a pickaxe gives the clamp's one scrap");
		helper.succeed();
	}

	/** The Brass Plaque (D72) drops itself; the item places the whole Worker (its icon's stage). */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void brassPlaqueDropsItself(GameTestHelper helper) {
		helper.setBlock(AT, ModWastes.BRASS_PLAQUE.get().defaultBlockState().setValue(BrassPlaqueBlock.ASKEW, true));
		helper.getLevel().destroyBlock(helper.absolutePos(AT), true);
		helper.assertTrue(count(drops(helper, AT), ModWastes.BRASS_PLAQUE.asItem()) == 1, "a plaque drops itself");
		helper.assertTrue(ModWastes.ASSEMBLY_CLAMP.get().defaultBlockState().getValue(AssemblyClampBlock.STAGE) == AssemblyClampBlock.ITEM_STAGE,
			"the clamp's item places stage " + AssemblyClampBlock.ITEM_STAGE);
		helper.succeed();
	}

	/**
	 * The mast riser (D72): cable placed on the side of an upright Create Metal Girder climbs it (foot, run, top as it goes
	 * up the column), its foot joins the floor cable beside it, and a riser whose girder goes falls while the one above
	 * becomes the new foot.
	 */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void cableClimbsAGirder(GameTestHelper helper) {
		BlockState girder = com.comedymonkey.createaddons.content.wastes.city.CityPlan.with(
			com.comedymonkey.createaddons.content.wastes.city.CityPlan.block("create:metal_girder", Blocks.IRON_BARS), "axis", "y");
		helper.assertTrue(DerelictCableBlock.uprightGirder(girder), "no upright Metal Girder to climb");
		for (int y = 0; y < 3; y++)
			helper.setBlock(AT.above(y), girder);
		DerelictCableBlock cable = ModWastes.DERELICT_CABLE.get();
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		ItemStack stack = new ItemStack(cable);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		for (int y = 0; y < 3; y++) {
			BlockPos g = helper.absolutePos(AT.above(y));
			BlockPlaceContext ctx = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
				new BlockHitResult(Vec3.atCenterOf(g).relative(Direction.NORTH, 0.5), Direction.NORTH, g, false));
			BlockState placed = cable.getStateForPlacement(ctx);
			helper.assertTrue(placed != null, "no riser on the girder's side at " + y);
			helper.setBlock(AT.north().above(y), placed);
		}
		DerelictCableBlock.Girder[] want = { DerelictCableBlock.Girder.FOOT, DerelictCableBlock.Girder.RUN, DerelictCableBlock.Girder.TOP };
		for (int y = 0; y < 3; y++) {
			BlockState s = helper.getBlockState(AT.north().above(y));
			helper.assertTrue(s.getValue(DerelictCableBlock.GIRDER) == want[y] && s.getValue(DerelictCableBlock.SURFACE) == Direction.SOUTH,
				"riser " + y + " is " + s);
		}
		// floor cable beside the foot: both join
		BlockPos floor = AT.north().west();
		helper.setBlock(floor.below(), Blocks.STONE.defaultBlockState());
		BlockPos f = helper.absolutePos(floor);
		BlockState fc = cable.getStateForPlacement(new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
			new BlockHitResult(Vec3.atBottomCenterOf(f), Direction.UP, f.below(), false)));
		helper.assertTrue(fc != null && fc.getValue(DerelictCableBlock.EAST), "the floor cable doesn't join the riser's foot: " + fc);
		helper.setBlock(floor, fc);
		helper.assertTrue(helper.getBlockState(AT.north()).getValue(DerelictCableBlock.WEST), "the riser's foot doesn't join the floor cable");
		// the bottom girder goes: its riser falls, the next one up is the foot now
		helper.setBlock(AT, Blocks.AIR.defaultBlockState());
		helper.assertBlockPresent(Blocks.AIR, AT.north());
		helper.assertTrue(helper.getBlockState(AT.north().above()).getValue(DerelictCableBlock.GIRDER) == DerelictCableBlock.Girder.FOOT,
			"the riser over a fallen one should be the foot now");
		helper.succeed();
	}

	// ---------------------------------------------------------------- D76: the Derelict Cable's arms point their way

	/** A resource of this mod (a blockstate or block model), read as JSON from the mod's own files. */
	private static com.google.gson.JsonObject modJson(String path) {
		try (java.io.InputStream in = WastesGameTests.class.getResourceAsStream("/assets/" + ComedyMonkeysCreateAddons.MOD_ID + "/" + path)) {
			if (in == null)
				throw new GameTestAssertException("missing " + path);
			return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (java.io.IOException e) {
			throw new GameTestAssertException("can't read " + path + ": " + e);
		}
	}

	/** Whether a multipart 'when' holds for a block state (values split by '|', OR and AND lists). */
	private static boolean holds(com.google.gson.JsonObject when, BlockState state) {
		if (when.has("OR")) {
			for (var w : when.getAsJsonArray("OR"))
				if (holds(w.getAsJsonObject(), state))
					return true;
			return false;
		}
		if (when.has("AND")) {
			for (var w : when.getAsJsonArray("AND"))
				if (!holds(w.getAsJsonObject(), state))
					return false;
			return true;
		}
		for (var e : when.entrySet()) {
			net.minecraft.world.level.block.state.properties.Property<?> p = state.getBlock().getStateDefinition().getProperty(e.getKey());
			if (p == null)
				throw new GameTestAssertException("no property " + e.getKey());
			if (!List.of(e.getValue().getAsString().split("\\|")).contains(valueName(state, p)))
				return false;
		}
		return true;
	}

	private static <T extends Comparable<T>> String valueName(BlockState state, net.minecraft.world.level.block.state.properties.Property<T> p) {
		return p.getName(state.getValue(p));
	}

	/**
	 * Where each arm model a cable state draws runs, as the game turns it: each applied 'derelict_cable_arm' model (not its
	 * lit line, not the knot), its elements turned by the blockstate's x then y as vanilla's BlockModelRotation turns a model
	 * (v -> Ry(-y) Rx(-x) v about the block's middle), and the side of the block its middle lies toward.
	 */
	private static List<Direction> drawnArms(com.google.gson.JsonObject blockstate, BlockState state) {
		List<Direction> out = new java.util.ArrayList<>();
		for (var p : blockstate.getAsJsonArray("multipart")) {
			com.google.gson.JsonObject part = p.getAsJsonObject();
			if (part.has("when") && !holds(part.getAsJsonObject("when"), state))
				continue;
			com.google.gson.JsonObject apply = part.getAsJsonObject("apply");
			String model = apply.get("model").getAsString();
			String name = model.substring(model.lastIndexOf('/') + 1);
			if (!name.startsWith("derelict_cable_arm") || name.contains("glow"))
				continue;
			int rx = apply.has("x") ? apply.get("x").getAsInt() : 0, ry = apply.has("y") ? apply.get("y").getAsInt() : 0;
			org.joml.Quaternionf turn = new org.joml.Quaternionf().rotateYXZ((float) Math.toRadians(-ry), (float) Math.toRadians(-rx), 0);
			float[] lo = { 99, 99, 99 }, hi = { -99, -99, -99 };
			for (var el : modJson("models/block/wastes/" + name + ".json").getAsJsonArray("elements")) {
				var from = el.getAsJsonObject().getAsJsonArray("from");
				var to = el.getAsJsonObject().getAsJsonArray("to");
				for (int k = 0; k < 8; k++) {
					org.joml.Vector3f v = new org.joml.Vector3f(((k & 1) == 0 ? from : to).get(0).getAsFloat() - 8,
						((k & 2) == 0 ? from : to).get(1).getAsFloat() - 8, ((k & 4) == 0 ? from : to).get(2).getAsFloat() - 8);
					turn.transform(v);
					float[] c = { v.x, v.y, v.z };
					for (int i = 0; i < 3; i++) {
						lo[i] = Math.min(lo[i], c[i]);
						hi[i] = Math.max(hi[i], c[i]);
					}
				}
			}
			// the arm runs from the block's middle to one side along its surface: its bounds' middle lies furthest off centre that way
			// (of the two axes along the surface; across it, the arm lies on the surface)
			float[] mid = { (lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2, (lo[2] + hi[2]) / 2 };
			int across = state.getValue(DerelictCableBlock.SURFACE).getAxis().ordinal(), axis = -1;
			for (int i = 0; i < 3; i++)
				if (i != across && (axis < 0 || Math.abs(mid[i]) > Math.abs(mid[axis])))
					axis = i;
			if (Math.abs(mid[axis]) < 2 || mid[across] * state.getValue(DerelictCableBlock.SURFACE).getAxisDirection().getStep() < 4)
				throw new GameTestAssertException(name + " (x " + rx + ", y " + ry + ") doesn't run out to a side along its surface: " + java.util.Arrays.toString(mid));
			out.add(Direction.fromAxisAndDirection(Direction.Axis.values()[axis],
				mid[axis] > 0 ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE));
		}
		return out;
	}

	/** The sides a cable state joins. */
	private static java.util.Set<Direction> stateArms(BlockState state) {
		java.util.Set<Direction> out = java.util.EnumSet.noneOf(Direction.class);
		for (Direction d : Direction.values())
			if (state.getValue(DerelictCableBlock.side(d)))
				out.add(d);
		return out;
	}

	/**
	 * D76: every arm the Derelict Cable draws runs toward the side it joins, on every surface (on a wall its 'up' and 'down'
	 * arms were drawn on each other's half: derelict_cable_arm_south was turned y 90, so it ran north). A run along a wall
	 * that turns a corner up it, placed as a player places it: the corner joins west and up and draws its arms west and up,
	 * the blocks above join down and up and draw them so; then every surface with each single arm along it, dark and lit.
	 */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void cableWallArmsPointTheirWay(GameTestHelper helper) {
		DerelictCableBlock cable = ModWastes.DERELICT_CABLE.get();
		com.google.gson.JsonObject blockstate = modJson("blockstates/derelict_cable.json");
		// a stone wall (z 4), the cable on its north face (surface south): along it at y 2 from x 1 to the corner at x 3, then up
		// the wall at x 3 to y 4
		for (int x = 0; x <= 5; x++)
			for (int y = 1; y <= 5; y++)
				helper.setBlock(new BlockPos(x, y, 4), Blocks.STONE.defaultBlockState());
		List<BlockPos> run = List.of(new BlockPos(1, 2, 3), new BlockPos(2, 2, 3), new BlockPos(3, 2, 3), new BlockPos(3, 3, 3), new BlockPos(3, 4, 3));
		Player player = helper.makeMockPlayer(GameType.CREATIVE);
		ItemStack stack = new ItemStack(cable);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		for (BlockPos p : run) {
			BlockPos wall = helper.absolutePos(p.south());
			BlockState placed = cable.getStateForPlacement(new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
				new BlockHitResult(Vec3.atCenterOf(wall).relative(Direction.NORTH, 0.5), Direction.NORTH, wall, false)));
			helper.assertTrue(placed != null && placed.getValue(DerelictCableBlock.SURFACE) == Direction.SOUTH, "no wall cable at " + p + ": " + placed);
			helper.setBlock(p, placed);
		}
		java.util.Map<BlockPos, java.util.Set<Direction>> want = java.util.Map.of(
			run.get(0), java.util.EnumSet.of(Direction.WEST, Direction.EAST),
			run.get(1), java.util.EnumSet.of(Direction.WEST, Direction.EAST),
			run.get(2), java.util.EnumSet.of(Direction.WEST, Direction.UP),
			run.get(3), java.util.EnumSet.of(Direction.DOWN, Direction.UP),
			run.get(4), java.util.EnumSet.of(Direction.DOWN, Direction.UP));
		for (BlockPos p : run) {
			BlockState s = helper.getBlockState(p);
			helper.assertTrue(stateArms(s).equals(want.get(p)), "the wall cable at " + p + " joins " + stateArms(s) + ", not " + want.get(p));
			List<Direction> drawn = drawnArms(blockstate, s);
			helper.assertTrue(drawn.size() == want.get(p).size() && java.util.EnumSet.copyOf(drawn).equals(want.get(p)),
				"the wall cable at " + p + " joins " + want.get(p) + " but its arms are drawn " + drawn);
		}
		// every surface, each arm along it alone, dark and lit: the one arm model drawn runs that way
		for (Direction surface : Direction.values())
			for (Direction arm : DerelictCableBlock.along(surface))
				for (boolean lit : new boolean[] { false, true }) {
					BlockState s = cable.defaultBlockState().setValue(DerelictCableBlock.SURFACE, surface).setValue(DerelictCableBlock.LIT, lit);
					for (Direction d : Direction.values())
						s = s.setValue(DerelictCableBlock.side(d), d == arm);
					List<Direction> drawn = drawnArms(blockstate, s);
					helper.assertTrue(drawn.equals(List.of(arm)), "cable on " + surface + " with one arm " + arm + " draws " + drawn);
				}
		helper.succeed();
	}

	// ---------------------------------------------------------------- D76: the building block set's recipes

	private static Item modItem(String id) {
		return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ComedyMonkeysCreateAddons.asResource(id));
	}

	/** Exactly one recipe of the type matches the input (no clash with vanilla's, Create's or another mod's), making count of out. */
	private static <I extends net.minecraft.world.item.crafting.RecipeInput, R extends net.minecraft.world.item.crafting.Recipe<I>> void makes(
		GameTestHelper helper, net.minecraft.world.item.crafting.RecipeType<R> type, I input, Item out, int count, String what) {
		var matches = helper.getLevel().getRecipeManager().getRecipesFor(type, input, helper.getLevel());
		helper.assertTrue(matches.size() == 1, what + ": " + matches.size() + " recipes match " + matches.stream().map(h -> h.id().toString()).toList());
		ItemStack result = matches.get(0).value().assemble(input, helper.getLevel().registryAccess());
		helper.assertTrue(result.is(out) && result.getCount() == count, what + " makes " + result + ", not " + count + " " + out);
	}

	private static net.minecraft.world.item.crafting.CraftingInput grid(int w, int h, Item... items) {
		List<ItemStack> stacks = new java.util.ArrayList<>();
		for (Item i : items)
			stacks.add(i == null ? ItemStack.EMPTY : new ItemStack(i));
		return net.minecraft.world.item.crafting.CraftingInput.of(w, h, stacks);
	}

	/** Everything a Stonecutter makes from one of an item: result -> count. */
	private static java.util.Map<Item, Integer> cuts(GameTestHelper helper, Item from) {
		var one = new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(from));
		java.util.Map<Item, Integer> out = new java.util.HashMap<>();
		for (var h : helper.getLevel().getRecipeManager().getRecipesFor(net.minecraft.world.item.crafting.RecipeType.STONECUTTING, one, helper.getLevel())) {
			ItemStack r = h.value().assemble(one, helper.getLevel().registryAccess());
			out.put(r.getItem(), r.getCount());
		}
		return out;
	}

	/**
	 * D76: the building block set's recipes, through the server's own recipe manager (what JEI lists): the Void-Alloy Plate
	 * cuts into its stairs, slab (two), wall, band, grille and clamp bracket; Stained End Stone Bricks from eight End Stone
	 * Bricks round a brown dye, their stairs, slab and wall by vanilla's shapes and cut, cracked by smelting; the Patina Rib
	 * from two copper blocks stacked (any weathering, waxed or not); the Broken Makers' Window from iron bars and glass; nothing
	 * makes Key-Marked Stone or a Scarred Plate (city-only). Each crafting grid matches exactly one recipe.
	 */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void buildingBlockRecipes(GameTestHelper helper) {
		var craft = net.minecraft.world.item.crafting.RecipeType.CRAFTING;
		java.util.Map<Item, Integer> plate = java.util.Map.of(modItem("void_alloy_stairs"), 1, modItem("void_alloy_slab"), 2, modItem("void_alloy_wall"), 1,
			modItem("void_alloy_band"), 1, modItem("slotted_grille"), 1, modItem("clamp_bracket"), 1);
		helper.assertTrue(cuts(helper, modItem("void_alloy_plate")).equals(plate), "the plate cuts into " + cuts(helper, modItem("void_alloy_plate")) + ", not " + plate);
		Item esb = Items.END_STONE_BRICKS, seb = modItem("stained_end_stone_bricks");
		makes(helper, craft, grid(3, 3, esb, esb, esb, esb, Items.BROWN_DYE, esb, esb, esb, esb), seb, 8, "8 End Stone Bricks round a brown dye");
		makes(helper, craft, grid(3, 3, seb, null, null, seb, seb, null, seb, seb, seb), modItem("stained_end_stone_brick_stairs"), 4, "stained brick stairs");
		makes(helper, craft, grid(3, 1, seb, seb, seb), modItem("stained_end_stone_brick_slab"), 6, "stained brick slab");
		makes(helper, craft, grid(3, 2, seb, seb, seb, seb, seb, seb), modItem("stained_end_stone_brick_wall"), 6, "stained brick wall");
		java.util.Map<Item, Integer> bricks = java.util.Map.of(modItem("stained_end_stone_brick_stairs"), 1, modItem("stained_end_stone_brick_slab"), 2,
			modItem("stained_end_stone_brick_wall"), 1);
		helper.assertTrue(cuts(helper, seb).equals(bricks), "stained bricks cut into " + cuts(helper, seb) + ", not " + bricks);
		makes(helper, net.minecraft.world.item.crafting.RecipeType.SMELTING, new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(seb)),
			modItem("cracked_stained_end_stone_bricks"), 1, "smelted stained bricks");
		for (Item copper : List.of(Items.COPPER_BLOCK, Items.EXPOSED_COPPER, Items.WEATHERED_COPPER, Items.OXIDIZED_COPPER, Items.WAXED_COPPER_BLOCK,
			Items.WAXED_EXPOSED_COPPER, Items.WAXED_WEATHERED_COPPER, Items.WAXED_OXIDIZED_COPPER))
			makes(helper, craft, grid(1, 2, copper, copper), modItem("patina_rib"), 2, "two " + copper + " stacked");
		makes(helper, craft, grid(3, 2, Items.IRON_BARS, Items.GLASS, Items.IRON_BARS, Items.IRON_BARS, Items.GLASS, Items.IRON_BARS),
			modItem("broken_makers_window"), 4, "iron bars and glass");
		for (var h : helper.getLevel().getRecipeManager().getRecipes()) {
			ItemStack r = h.value().getResultItem(helper.getLevel().registryAccess());
			helper.assertTrue(!r.is(modItem("key_marked_stone")) && !r.is(modItem("scarred_void_alloy_plate")), h.id() + " makes " + r + ", which is city-only");
		}
		helper.succeed();
	}
}
