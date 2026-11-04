package com.comedymonkey.createaddons.gametest;

import java.util.List;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.content.MachineEnchantments;
import com.comedymonkey.createaddons.content.jetpack.JetpackFlight;
import com.comedymonkey.createaddons.content.jetpack.JetpackState;
import com.comedymonkey.createaddons.content.jetpack.JetpackWindingStationBlock;
import com.comedymonkey.createaddons.content.jetpack.JetpackWindingStationBlockEntity;
import com.comedymonkey.createaddons.content.jetpack.Jetpacks;
import com.comedymonkey.createaddons.content.juggernaut.SpringEngineBlock;
import com.comedymonkey.createaddons.content.juggernaut.SpringEngineBlockEntity;
import com.comedymonkey.createaddons.network.JetpackInputPayload;
import com.comedymonkey.createaddons.registry.ModAttachments;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModDataComponents;
import com.comedymonkey.createaddons.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Clockwork Jetpack in a world (jetpack spec): its Winding Station winds a hung jetpack and a backed-in wearer's from
 * a shaft (D38-D40), funnels only take a full one off (D43), breaking it drops the jetpack with its wind (D44); wind is
 * spent at thrust and hover rates (D12), the wings fold on landing (D14); the Jetpack slot comes first (D18, D21);
 * Overwound doubles a jetpack's wind and a Spring Engine's at III and stays with the engine (D34-D36), and Create's
 * Capacity doesn't go on a jetpack (D25).
 */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class JetpackGameTests {
	/** 24 x 12 x 24 of air on a floor. */
	private static final String COURSE = "battery_course";
	private static final BlockPos STATION = new BlockPos(12, 1, 12);

	private JetpackGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(JetpackGameTests.class);
	}

	/** A station facing north, a Creative Motor under it set to {@code rpm}. */
	private static JetpackWindingStationBlockEntity station(GameTestHelper helper, int rpm) {
		helper.setBlock(STATION, ModBlocks.JETPACK_WINDING_STATION.get().defaultBlockState()
			.setValue(JetpackWindingStationBlock.HORIZONTAL_FACING, Direction.NORTH));
		BlockState motor = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "creative_motor")).defaultBlockState()
			.setValue(BlockStateProperties.FACING, Direction.UP);
		helper.setBlock(STATION.below(), motor);
		if (helper.getBlockEntity(STATION.below()) instanceof com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity creative)
			creative.getBehaviour(com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour.TYPE).setValue(rpm);
		return helper.getBlockEntity(STATION);
	}

	private static Holder<Enchantment> enchantment(GameTestHelper helper, ResourceKey<Enchantment> key) {
		return helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
	}

	/** D38-D40: a hung jetpack winds from the shaft below, the boom reaching out to it. */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void theStationWindsAHungJetpack(GameTestHelper helper) {
		JetpackWindingStationBlockEntity station = station(helper, 256);
		station.hang(new ItemStack(ModItems.CLOCKWORK_JETPACK.get()));
		helper.succeedWhen(() -> {
			helper.assertTrue(Jetpacks.wind(station.hung()) > 0, "the hung jetpack should be winding, has " + Jetpacks.wind(station.hung()));
			helper.assertTrue(station.boom() == JetpackWindingStationBlockEntity.Boom.HUNG, "the boom should be down on it, is " + station.boom());
		});
	}

	/** D43: a funnel can't take a jetpack off until it's full; then it can, and the boom parks. */
	@GameTest(template = COURSE, timeoutTicks = 100)
	public static void funnelsOnlyTakeAFullJetpack(GameTestHelper helper) {
		JetpackWindingStationBlockEntity station = station(helper, 256);
		ItemStack jetpack = new ItemStack(ModItems.CLOCKWORK_JETPACK.get());
		Jetpacks.setWind(jetpack, Jetpacks.maxWind(jetpack) - 30);
		helper.assertTrue(station.items().insertItem(0, jetpack, false).isEmpty(), "a funnel should hang a jetpack");
		helper.assertTrue(station.items().extractItem(0, 1, true).isEmpty(), "not full: nothing should come off");
		helper.succeedWhen(() -> {
			helper.assertTrue(Jetpacks.full(station.hung()), "it should wind full");
			helper.assertTrue(station.items().extractItem(0, 1, true).is(ModItems.CLOCKWORK_JETPACK.get()), "full: a funnel should take it off");
			helper.assertTrue(station.boom() == JetpackWindingStationBlockEntity.Boom.PARKED, "full: the boom should park");
		});
	}

	/**
	 * D39: right-clicked wearing a jetpack, an empty station takes it off your back and hangs it, wind and all; clicked
	 * again it goes back on; with your slot already taken, it comes off into your inventory instead.
	 */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void rightClickSwapsYourJetpackWithTheStation(GameTestHelper helper) {
		JetpackWindingStationBlockEntity station = station(helper, 0);
		ServerPlayer player = player(helper);
		ItemStack jetpack = new ItemStack(ModItems.CLOCKWORK_JETPACK.get());
		Jetpacks.setWind(jetpack, 1234);
		helper.assertTrue(Jetpacks.putOn(player, jetpack), "the jetpack should go on");
		BlockPos pos = helper.absolutePos(STATION);
		BlockHitResult hit = new BlockHitResult(pos.getCenter(), Direction.NORTH, pos, false);
		BlockState state = helper.getLevel().getBlockState(pos);

		state.useWithoutItem(helper.getLevel(), player, hit);
		helper.assertTrue(Jetpacks.worn(player).isEmpty(), "it should come off your back");
		helper.assertTrue(Jetpacks.wind(station.hung()) == 1234, "and hang on the station with its wind, has " + Jetpacks.wind(station.hung()));

		state.useWithoutItem(helper.getLevel(), player, hit);
		helper.assertTrue(station.hung().isEmpty(), "clicked again, it should come off the station");
		helper.assertTrue(Jetpacks.wind(Jetpacks.worn(player)) == 1234, "and go back on your back");

		station.hang(new ItemStack(ModItems.NETHERITE_CLOCKWORK_JETPACK.get()));
		state.useWithoutItem(helper.getLevel(), player, hit);
		helper.assertTrue(Jetpacks.worn(player).is(ModItems.CLOCKWORK_JETPACK.get()), "with a jetpack on, the one you wear stays on");
		helper.assertTrue(player.getInventory().contains(new ItemStack(ModItems.NETHERITE_CLOCKWORK_JETPACK.get())), "and the station's goes in your inventory");
		helper.succeed();
	}

	/** D44: broken, it drops the hung jetpack with its wind, and itself. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void breakingTheStationDropsTheJetpack(GameTestHelper helper) {
		JetpackWindingStationBlockEntity station = station(helper, 0);
		ItemStack jetpack = new ItemStack(ModItems.CLOCKWORK_JETPACK.get());
		Jetpacks.setWind(jetpack, 1234);
		station.hang(jetpack);
		helper.getLevel().destroyBlock(helper.absolutePos(STATION), true);
		helper.runAfterDelay(2, () -> {
			List<ItemEntity> items = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(STATION)).inflate(2));
			helper.assertTrue(items.stream().anyMatch(e -> e.getItem().is(ModItems.CLOCKWORK_JETPACK.get()) && Jetpacks.wind(e.getItem()) == 1234),
				"the jetpack should drop with its wind, drops " + items);
			helper.assertTrue(items.stream().anyMatch(e -> e.getItem().is(ModItems.JETPACK_WINDING_STATION.get())), "the station should drop itself");
			helper.succeed();
		});
	}

	/**
	 * D39: a player on the block in front, back to it, sneaking, has backed in; facing the station, standing, or a block
	 * away, they haven't. (A fake player: mock players added to the level trip other mods' join packets.)
	 */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void backingInIsSneakingWithYourBackToIt(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		BlockPos station = helper.absolutePos(STATION), front = station.north();
		player.moveTo(front.getX() + 0.5, front.getY(), front.getZ() + 0.5, Direction.NORTH.toYRot() + 30, 0);
		player.setShiftKeyDown(true);
		helper.assertTrue(JetpackWindingStationBlockEntity.backedIn(player, station, Direction.NORTH), "back to it within 45 degrees, sneaking: backed in");
		player.setYRot(Direction.SOUTH.toYRot());
		helper.assertFalse(JetpackWindingStationBlockEntity.backedIn(player, station, Direction.NORTH), "facing the station: not backed in");
		player.setYRot(Direction.NORTH.toYRot());
		player.setShiftKeyDown(false);
		helper.assertFalse(JetpackWindingStationBlockEntity.backedIn(player, station, Direction.NORTH), "standing: not backed in");
		player.setShiftKeyDown(true);
		player.moveTo(front.getX() + 0.5, front.getY(), front.getZ() - 1.5, Direction.NORTH.toYRot(), 0);
		helper.assertFalse(JetpackWindingStationBlockEntity.backedIn(player, station, Direction.NORTH), "a block away: not backed in");
		helper.succeed();
	}

	/** A server player outside the level (NeoForge's fake player): its packets go nowhere. It starts high over the course. */
	private static ServerPlayer player(GameTestHelper helper) {
		ServerPlayer player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(helper.getLevel(),
			new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "jetpack_test"));
		Vec3 high = helper.absoluteVec(new Vec3(4.5, 8, 4.5));
		player.moveTo(high.x, high.y, high.z, 0, 0);
		player.setGameMode(GameType.SURVIVAL);
		player.setData(ModAttachments.JETPACK, JetpackState.DEFAULT);
		for (EquipmentSlot slot : EquipmentSlot.values())
			player.setItemSlot(slot, ItemStack.EMPTY);
		return player;
	}

	/**
	 * The jetpack's advancements (approved 2026-11-04): Liftoff on thrust, Station Keeping on hover, Clockwork Wings when
	 * they spread, What Goes Up when the wind runs out in the air, Overwound for wearing III, and The Floor Is Lava for a
	 * Netherite one running in lava. A plain one in lava earns nothing (it doesn't run there).
	 */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void flyingEarnsTheJetpacksAdvancements(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		java.util.Set<String> earned = java.util.concurrent.ConcurrentHashMap.newKeySet();
		ModAdvancements.Award listener = (who, action, value) -> {
			if (who.equals(player.getUUID()) && (!action.equals(ModAdvancements.JETPACK_OVERWOUND) || value >= 3))
				earned.add(action);
		};
		ModAdvancements.TEST_AWARDS.add(listener);
		try {
			ItemStack jetpack = new ItemStack(ModItems.CLOCKWORK_JETPACK.get());
			jetpack.enchant(enchantment(helper, MachineEnchantments.OVERWOUND), 3);
			Jetpacks.setWind(jetpack, 1000);
			player.setItemSlot(EquipmentSlot.CHEST, jetpack);
			player.setOnGround(false);
			JetpackFlight.setInput(player, JetpackInputPayload.UP);
			JetpackFlight.serverTick(player);
			JetpackFlight.serverTick(player);
			helper.assertTrue(earned.contains(ModAdvancements.JETPACK_FLIGHT), "thrust: Liftoff");
			helper.assertTrue(earned.contains(ModAdvancements.JETPACK_OVERWOUND), "wearing Overwound III");
			JetpackFlight.setInput(player, 0);
			player.setData(ModAttachments.JETPACK, JetpackState.DEFAULT.withHover(true));
			JetpackFlight.serverTick(player);
			helper.assertTrue(earned.contains(ModAdvancements.JETPACK_HOVER), "hover holding him: Station Keeping");
			player.setData(ModAttachments.JETPACK, JetpackState.DEFAULT);
			JetpackFlight.toggle(player, com.comedymonkey.createaddons.network.JetpackTogglePayload.Toggle.WINGS);
			helper.assertTrue(earned.contains(ModAdvancements.JETPACK_WINGS), "the wings spread: Clockwork Wings");
			player.setData(ModAttachments.JETPACK, JetpackState.DEFAULT);
			player.stopFallFlying(); // the glide over: the engine thrusts again
			Jetpacks.setWind(player.getItemBySlot(EquipmentSlot.CHEST), 3);
			JetpackFlight.setInput(player, JetpackInputPayload.UP);
			for (int i = 0; i < 6; i++)
				JetpackFlight.serverTick(player);
			helper.assertTrue(earned.contains(ModAdvancements.JETPACK_RUN_DOWN), "out of wind in the air: What Goes Up");
			// lava: the plain one doesn't run there; the Netherite one does
			BlockPos lava = new BlockPos(10, 2, 10);
			helper.setBlock(lava, net.minecraft.world.level.block.Blocks.LAVA);
			Vec3 in = helper.absoluteVec(new Vec3(10.5, 2.1, 10.5));
			player.moveTo(in.x, in.y, in.z, 0, 0);
			player.baseTick(); // reads the fluid he's in
			Jetpacks.setWind(player.getItemBySlot(EquipmentSlot.CHEST), 1000);
			for (int i = 0; i < 3; i++)
				JetpackFlight.serverTick(player);
			helper.assertFalse(earned.contains(ModAdvancements.JETPACK_LAVA), "a plain jetpack in lava");
			ItemStack netherite = new ItemStack(ModItems.NETHERITE_CLOCKWORK_JETPACK.get());
			Jetpacks.setWind(netherite, 1000);
			player.setItemSlot(EquipmentSlot.CHEST, netherite);
			for (int i = 0; i < 3; i++)
				JetpackFlight.serverTick(player);
			helper.assertTrue(earned.contains(ModAdvancements.JETPACK_LAVA), "a Netherite jetpack in lava: The Floor Is Lava");
			helper.succeed();
		} finally {
			ModAdvancements.TEST_AWARDS.remove(listener);
		}
	}

	/** D12: 40 ticks of thrust spend 40 of wind; 40 of hover, 20; on the ground nothing runs. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void thrustAndHoverSpendWind(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ItemStack jetpack = new ItemStack(ModItems.CLOCKWORK_JETPACK.get());
		Jetpacks.setWind(jetpack, 1000);
		player.setItemSlot(EquipmentSlot.CHEST, jetpack);
		player.setOnGround(false);
		JetpackFlight.setInput(player, JetpackInputPayload.UP);
		for (int i = 0; i < 40; i++)
			JetpackFlight.serverTick(player);
		int afterThrust = Jetpacks.wind(player.getItemBySlot(EquipmentSlot.CHEST));
		helper.assertTrue(afterThrust == 960, "40 ticks of thrust should spend 40, left " + afterThrust);
		JetpackFlight.setInput(player, 0);
		player.setData(ModAttachments.JETPACK, JetpackState.DEFAULT.withHover(true));
		for (int i = 0; i < 40; i++)
			JetpackFlight.serverTick(player);
		int afterHover = Jetpacks.wind(player.getItemBySlot(EquipmentSlot.CHEST));
		helper.assertTrue(afterHover == 940, "40 ticks of hover should spend 20, left " + afterHover);
		player.setData(ModAttachments.JETPACK, JetpackState.DEFAULT);
		player.setOnGround(true);
		JetpackFlight.setInput(player, JetpackInputPayload.DOWN);
		for (int i = 0; i < 40; i++)
			JetpackFlight.serverTick(player);
		helper.assertTrue(Jetpacks.wind(player.getItemBySlot(EquipmentSlot.CHEST)) == 940, "sneaking on the ground shouldn't spend wind");
		helper.succeed();
	}

	/**
	 * Hover only catches a real drop (user, 2026-11-03): off the ground but half a block or less over it (stepping down
	 * a stair or off a slab) it lets you land, spending nothing; higher, it holds you.
	 */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void hoverLetsYouStepDownHalfABlock(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ItemStack jetpack = new ItemStack(ModItems.CLOCKWORK_JETPACK.get());
		Jetpacks.setWind(jetpack, 1000);
		player.setItemSlot(EquipmentSlot.CHEST, jetpack);
		JetpackState hover = JetpackState.DEFAULT.withHover(true);
		player.setData(ModAttachments.JETPACK, hover);
		player.setOnGround(false);
		JetpackFlight.setInput(player, 0);
		BlockPos step = new BlockPos(6, 1, 6);
		helper.setBlock(step, net.minecraft.world.level.block.Blocks.STONE);
		for (double above : new double[] { 0.1, 0.3, 0.5 }) {
			Vec3 at = helper.absoluteVec(new Vec3(6.5, 2 + above, 6.5));
			player.moveTo(at.x, at.y, at.z, 0, 0);
			helper.assertFalse(JetpackFlight.hovering(player, hover), above + " over the step: hover shouldn't catch you");
			helper.assertFalse(JetpackFlight.move(player, jetpack, 0, 0, 0), above + " over the step: the jetpack shouldn't run");
			for (int i = 0; i < 40; i++)
				JetpackFlight.serverTick(player);
			helper.assertTrue(Jetpacks.wind(player.getItemBySlot(EquipmentSlot.CHEST)) == 1000, above + " over the step: no wind should be spent");
		}
		Vec3 at = helper.absoluteVec(new Vec3(6.5, 2.8, 6.5));
		player.moveTo(at.x, at.y, at.z, 0, 0);
		helper.assertTrue(JetpackFlight.hovering(player, hover), "0.8 over the step: hover should hold you");
		helper.assertTrue(JetpackFlight.move(player, jetpack, 0, 0, 0), "0.8 over the step: the jetpack should run");

		// walking down stairs you fall behind the steps: just past a slab, 0.55 over the floor, the slab still counts
		helper.setBlock(new BlockPos(14, 1, 6), net.minecraft.world.level.block.Blocks.STONE_SLAB);
		at = helper.absoluteVec(new Vec3(14.5, 1.55, 7.3));
		player.moveTo(at.x, at.y, at.z, 0, 0);
		helper.assertFalse(JetpackFlight.hovering(player, hover), "just past a step down, falling behind it: hover shouldn't catch you");
		// a wall at your height isn't a floor: hovering beside one still holds you
		for (int y = 1; y <= 8; y++)
			helper.setBlock(new BlockPos(18, y, 12), net.minecraft.world.level.block.Blocks.STONE);
		at = helper.absoluteVec(new Vec3(19.3, 4.3, 12.5));
		player.moveTo(at.x, at.y, at.z, 0, 0);
		helper.assertTrue(JetpackFlight.hovering(player, hover), "beside a wall, high over the floor: hover should hold you");
		helper.succeed();
	}

	/**
	 * Jump and Sneak held together keep you level: falling or rising, the thrust brings you to a stop and holds you
	 * there. On the ground together they do nothing, so you don't hop up or spend wind.
	 */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void jumpAndSneakTogetherHoldHeight(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ItemStack jetpack = new ItemStack(ModItems.CLOCKWORK_JETPACK.get());
		Jetpacks.setWind(jetpack, 1000);
		player.setItemSlot(EquipmentSlot.CHEST, jetpack);
		int both = JetpackInputPayload.UP | JetpackInputPayload.DOWN;
		player.setOnGround(false);
		for (double start : new double[] { -0.6, 0.4 }) {
			player.setDeltaMovement(0, start, 0);
			for (int i = 0; i < 40; i++)
				helper.assertTrue(JetpackFlight.move(player, jetpack, both, 0, 0), "in the air, both keys should run the jetpack");
			helper.assertTrue(player.getDeltaMovement().y == 0, "from " + start + ", both keys should hold height, moving " + player.getDeltaMovement().y);
		}
		player.setOnGround(true);
		player.setDeltaMovement(Vec3.ZERO);
		helper.assertFalse(JetpackFlight.move(player, jetpack, both, 0, 0), "on the ground, both keys shouldn't lift you");
		JetpackFlight.setInput(player, both);
		for (int i = 0; i < 40; i++)
			JetpackFlight.serverTick(player);
		helper.assertTrue(Jetpacks.wind(player.getItemBySlot(EquipmentSlot.CHEST)) == 1000, "on the ground, both keys shouldn't spend wind");
		helper.succeed();
	}

	/** D16: with the wings out, gliding is an Elytra's and spends nothing; holding Jump boosts, 40 ticks for 20 of wind. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void glidingIsFreeAndTheBoostSpendsWind(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ItemStack jetpack = new ItemStack(ModItems.CLOCKWORK_JETPACK.get());
		Jetpacks.setWind(jetpack, 1000);
		player.setItemSlot(EquipmentSlot.CHEST, jetpack);
		player.setOnGround(false);
		player.setData(ModAttachments.JETPACK, JetpackState.DEFAULT.withWings(true));
		JetpackFlight.setInput(player, 0);
		for (int i = 0; i < 40; i++)
			JetpackFlight.serverTick(player);
		int afterGlide = Jetpacks.wind(player.getItemBySlot(EquipmentSlot.CHEST));
		helper.assertTrue(afterGlide == 1000, "gliding shouldn't spend wind, left " + afterGlide);
		JetpackFlight.setInput(player, JetpackInputPayload.UP);
		for (int i = 0; i < 40; i++)
			JetpackFlight.serverTick(player);
		JetpackFlight.setInput(player, 0);
		int afterBoost = Jetpacks.wind(player.getItemBySlot(EquipmentSlot.CHEST));
		helper.assertTrue(afterBoost == 980, "40 ticks of boost should spend 20, left " + afterBoost);
		helper.succeed();
	}

	/**
	 * D16 (user, 2026-11-03): held, the boost keeps speeding you up from a glide, every tick, until 1.25 x a
	 * rocket-boosted Elytra's speed, and holds there.
	 */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void aHeldBoostKeepsSpeedingUpToItsTop(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ItemStack jetpack = new ItemStack(ModItems.CLOCKWORK_JETPACK.get());
		Jetpacks.setWind(jetpack, 1000);
		player.setItemSlot(EquipmentSlot.CHEST, jetpack);
		player.setOnGround(false);
		player.setData(ModAttachments.JETPACK, JetpackState.DEFAULT.withWings(true));
		player.startFallFlying();
		player.setXRot(0);
		player.setYRot(0);
		player.setDeltaMovement(player.getLookAngle().scale(0.5));
		double top = JetpackFlight.boostSpeed(), last = player.getDeltaMovement().length();
		for (int i = 0; i < 60; i++) {
			helper.assertTrue(JetpackFlight.move(player, jetpack, JetpackInputPayload.UP, 0, 0), "holding Jump with the wings out should boost");
			double speed = player.getDeltaMovement().length();
			if (last < top)
				helper.assertTrue(speed > last, "under top speed, each tick of boost should speed you up (" + last + " to " + speed + ")");
			last = speed;
		}
		helper.assertTrue(last >= top && last < top + 0.1, "the boost should top out at " + top + ", is " + last);
		helper.succeed();
	}

	/** D14: the wings fold on landing. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void theWingsFoldOnLanding(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CLOCKWORK_JETPACK.get()));
		player.setData(ModAttachments.JETPACK, JetpackState.DEFAULT.withWings(true));
		player.setOnGround(true);
		JetpackFlight.serverTick(player);
		helper.assertFalse(player.getData(ModAttachments.JETPACK).wings(), "on the ground, the wings should fold");
		helper.succeed();
	}

	/** D18, D21: with Curios, a jetpack in the Jetpack slot is the one worn, even with another in the chest. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void theJetpackSlotComesFirst(GameTestHelper helper) {
		if (!ModList.get().isLoaded(ComedyMonkeysCreateAddons.CURIOS_ID)) {
			helper.succeed();
			return;
		}
		ServerPlayer player = player(helper);
		ItemStack inSlot = new ItemStack(ModItems.NETHERITE_CLOCKWORK_JETPACK.get());
		player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CLOCKWORK_JETPACK.get()));
		helper.assertTrue(com.comedymonkey.createaddons.compat.curios.JetpackCurios.tryEquip(player, inSlot.copy()), "the Jetpack slot should take it");
		helper.assertTrue(Jetpacks.worn(player).is(ModItems.NETHERITE_CLOCKWORK_JETPACK.get()), "the slot's jetpack should be the one worn");
		helper.succeed();
	}

	/**
	 * D23: a backtank and a Clockwork Jetpack are never worn together. With a backtank on, the Jetpack slot won't take one;
	 * with one in the slot, a backtank put in the chest comes straight off into the inventory.
	 */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void aBacktankIsNeverWornWithAJetpack(GameTestHelper helper) {
		if (!ModList.get().isLoaded(ComedyMonkeysCreateAddons.CURIOS_ID)) {
			helper.succeed();
			return;
		}
		ServerPlayer player = player(helper);
		ItemStack backtank = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "copper_backtank")));
		helper.assertTrue(Jetpacks.isBacktank(backtank), "a Copper Backtank should count as a backtank");
		player.setItemSlot(EquipmentSlot.CHEST, backtank.copy());
		helper.assertFalse(Jetpacks.putOn(player, new ItemStack(ModItems.CLOCKWORK_JETPACK.get())), "with a backtank on, the jetpack shouldn't go on");
		helper.assertTrue(Jetpacks.worn(player).isEmpty(), "and none should be worn");

		player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
		helper.assertTrue(Jetpacks.putOn(player, new ItemStack(ModItems.CLOCKWORK_JETPACK.get())), "without one, the jetpack should go on");
		player.setItemSlot(EquipmentSlot.CHEST, backtank.copy());
		JetpackFlight.onEquipmentChange(new LivingEquipmentChangeEvent(player, EquipmentSlot.CHEST, ItemStack.EMPTY, player.getItemBySlot(EquipmentSlot.CHEST)));
		helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).isEmpty(), "the backtank should come off the chest");
		helper.assertTrue(player.getInventory().contains(backtank), "into the inventory");
		helper.assertFalse(Jetpacks.worn(player).isEmpty(), "the jetpack should stay on");
		helper.succeed();
	}

	/** D16, rule 1 (user, 2026-11-03): with the engine off the wings still glide, but holding Jump never boosts or spends wind. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void engineOffGlidesButNeverBoosts(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ItemStack jetpack = new ItemStack(ModItems.CLOCKWORK_JETPACK.get());
		Jetpacks.setWind(jetpack, 1000);
		player.setItemSlot(EquipmentSlot.CHEST, jetpack);
		player.setOnGround(false);
		player.setData(ModAttachments.JETPACK, new JetpackState(false, false, true));
		JetpackFlight.setInput(player, JetpackInputPayload.UP);
		for (int i = 0; i < 40; i++)
			JetpackFlight.serverTick(player);
		JetpackFlight.setInput(player, 0);
		helper.assertTrue(player.getData(ModAttachments.JETPACK).wings() && player.isFallFlying(), "the wings should still glide");
		int left = Jetpacks.wind(player.getItemBySlot(EquipmentSlot.CHEST));
		helper.assertTrue(left == 1000, "engine off, Jump shouldn't spend wind, left " + left);
		helper.succeed();
	}

	/** D14: out of wind, the wings glide on as a plain Elytra; Jump does nothing and wind stays at 0. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void outOfWindTheWingsGlideOn(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CLOCKWORK_JETPACK.get()));
		player.setOnGround(false);
		player.setData(ModAttachments.JETPACK, JetpackState.DEFAULT.withWings(true));
		JetpackFlight.setInput(player, JetpackInputPayload.UP);
		for (int i = 0; i < 20; i++)
			JetpackFlight.serverTick(player);
		JetpackFlight.setInput(player, 0);
		helper.assertTrue(player.getData(ModAttachments.JETPACK).wings() && player.isFallFlying(), "out of wind, the wings should keep gliding");
		helper.assertTrue(Jetpacks.wind(player.getItemBySlot(EquipmentSlot.CHEST)) == 0, "and the wind stays at 0");
		helper.succeed();
	}

	/** D27: Creative never spends wind. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void creativeSpendsNoWind(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		player.setGameMode(GameType.CREATIVE);
		player.getAbilities().flying = false;
		ItemStack jetpack = new ItemStack(ModItems.CLOCKWORK_JETPACK.get());
		Jetpacks.setWind(jetpack, 100);
		player.setItemSlot(EquipmentSlot.CHEST, jetpack);
		player.setOnGround(false);
		JetpackFlight.setInput(player, JetpackInputPayload.UP);
		for (int i = 0; i < 40; i++)
			JetpackFlight.serverTick(player);
		JetpackFlight.setInput(player, 0);
		helper.assertTrue(Jetpacks.wind(player.getItemBySlot(EquipmentSlot.CHEST)) == 100, "Creative shouldn't spend wind");
		helper.succeed();
	}

	/** Wind is written onto the jetpack as it's spent, so taking it off mid-flight never leaves any unspent (review fix). */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void takingItOffKeepsTheWindSpent(GameTestHelper helper) {
		ServerPlayer player = player(helper);
		ItemStack jetpack = new ItemStack(ModItems.CLOCKWORK_JETPACK.get());
		Jetpacks.setWind(jetpack, 1000);
		player.setItemSlot(EquipmentSlot.CHEST, jetpack);
		player.setOnGround(false);
		player.setData(ModAttachments.JETPACK, JetpackState.DEFAULT.withHover(true));
		JetpackFlight.setInput(player, 0);
		for (int i = 0; i < 10; i++)
			JetpackFlight.serverTick(player);
		ItemStack off = player.getItemBySlot(EquipmentSlot.CHEST).copy();
		player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
		JetpackFlight.serverTick(player);
		helper.assertTrue(Jetpacks.wind(off) == 995, "10 ticks of hover should already be on the jetpack taken off, has " + Jetpacks.wind(off));
		helper.succeed();
	}

	/** D23 (user, 2026-11-03): a chest Elytra glides as normal with the engine on, and gives way only while the jetpack thrusts. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void aChestElytraGlidesUnlessTheJetpackThrusts(GameTestHelper helper) {
		if (!ModList.get().isLoaded(ComedyMonkeysCreateAddons.CURIOS_ID)) {
			helper.succeed();
			return;
		}
		ServerPlayer player = player(helper);
		ItemStack jetpack = new ItemStack(ModItems.CLOCKWORK_JETPACK.get());
		Jetpacks.setWind(jetpack, 1000);
		helper.assertTrue(Jetpacks.putOn(player, jetpack), "the jetpack should go in the slot");
		player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
		player.setOnGround(false);
		player.startFallFlying();
		JetpackFlight.setInput(player, 0);
		JetpackFlight.serverTick(player);
		helper.assertTrue(player.isFallFlying(), "not thrusting, the Elytra should keep gliding");
		JetpackFlight.setInput(player, JetpackInputPayload.UP);
		JetpackFlight.serverTick(player);
		JetpackFlight.setInput(player, 0);
		helper.assertFalse(player.isFallFlying(), "thrusting, the Elytra should give way");
		helper.succeed();
	}

	/** D40: a stopped shaft winds nothing; D26: the comparator reads a hung jetpack's wind (half wound: 8). */
	@GameTest(template = COURSE, timeoutTicks = 60)
	public static void aStoppedStationWindsNothingAndItsComparatorReads(GameTestHelper helper) {
		JetpackWindingStationBlockEntity station = station(helper, 0);
		ItemStack jetpack = new ItemStack(ModItems.CLOCKWORK_JETPACK.get());
		Jetpacks.setWind(jetpack, Jetpacks.maxWind(jetpack) / 2);
		station.hang(jetpack);
		helper.assertTrue(station.comparatorLevel() == 8, "half wound should read 8, reads " + station.comparatorLevel());
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(Jetpacks.wind(station.hung()) == Jetpacks.maxWind(jetpack) / 2, "a stopped shaft shouldn't wind it");
			helper.assertTrue(station.boom() == JetpackWindingStationBlockEntity.Boom.PARKED, "and the boom should stay parked");
			helper.succeed();
		});
	}

	/** D35: Overwound comes from Librarians and the anvil, never the enchanting table. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void overwoundOnlyFromLibrarians(GameTestHelper helper) {
		Holder<Enchantment> overwound = enchantment(helper, MachineEnchantments.OVERWOUND);
		helper.assertTrue(overwound.is(EnchantmentTags.TRADEABLE), "Librarians should sell it");
		helper.assertFalse(overwound.is(EnchantmentTags.IN_ENCHANTING_TABLE), "the enchanting table shouldn't offer it");
		helper.succeed();
	}

	/** D19: the smithing table's Netherite upgrade keeps the wind and the enchantments. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void netheriteSmithingKeepsWindAndEnchantments(GameTestHelper helper) {
		ItemStack jetpack = new ItemStack(ModItems.CLOCKWORK_JETPACK.get());
		jetpack.enchant(enchantment(helper, MachineEnchantments.OVERWOUND), 2);
		Jetpacks.setWind(jetpack, 4321);
		SmithingRecipeInput input = new SmithingRecipeInput(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), jetpack, new ItemStack(Items.NETHERITE_INGOT));
		ItemStack out = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, helper.getLevel())
			.map(r -> r.value().assemble(input, helper.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
		helper.assertTrue(out.is(ModItems.NETHERITE_CLOCKWORK_JETPACK.get()), "it should upgrade to the Netherite jetpack, made " + out);
		helper.assertTrue(Jetpacks.wind(out) == 4321, "keeping its wind, has " + Jetpacks.wind(out));
		helper.assertTrue(Jetpacks.overwound(out) == 2, "and its Overwound, has " + Jetpacks.overwound(out));
		helper.succeed();
	}

	/** D34, D25: Overwound III doubles a jetpack's wind; Create's Capacity doesn't go on one. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void overwoundDoublesAJetpacksWind(GameTestHelper helper) {
		ItemStack jetpack = new ItemStack(ModItems.CLOCKWORK_JETPACK.get());
		Holder<Enchantment> overwound = enchantment(helper, MachineEnchantments.OVERWOUND);
		helper.assertTrue(jetpack.supportsEnchantment(overwound), "Overwound should go on a jetpack");
		jetpack.enchant(overwound, 3);
		helper.assertTrue(Jetpacks.maxWind(jetpack) == 2 * Jetpacks.baseWind(), "Overwound III should double it, is " + Jetpacks.maxWind(jetpack));
		ResourceKey<Enchantment> capacity = ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("create", "capacity"));
		helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(capacity).ifPresent(c ->
			helper.assertFalse(new ItemStack(ModItems.CLOCKWORK_JETPACK.get()).supportsEnchantment(c), "Capacity shouldn't go on a jetpack"));
		helper.succeed();
	}

	/** D36, Juggernaut D63: an engine placed with Overwound III holds twice as much, and drops with it again. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void anOverwoundSpringEngineHoldsTwice(GameTestHelper helper) {
		BlockPos out = new BlockPos(12, 0, 10);
		BlockState state = ModBlocks.SPRING_ENGINE.get().defaultBlockState().setValue(SpringEngineBlock.HORIZONTAL_FACING, Direction.NORTH);
		helper.setBlock(out, state.setValue(SpringEngineBlock.PART, SpringEngineBlock.Part.OUTPUT));
		helper.setBlock(out.south(), state.setValue(SpringEngineBlock.PART, SpringEngineBlock.Part.WINDING));
		SpringEngineBlockEntity engine = helper.getBlockEntity(out);
		ItemStack item = new ItemStack(ModItems.SPRING_ENGINE.get());
		item.enchant(enchantment(helper, MachineEnchantments.OVERWOUND), 3);
		item.set(ModDataComponents.SPRING_WIND.get(), SpringEngineBlockEntity.capacity() + 5);
		engine.applyComponentsFromItemStack(item);
		helper.assertTrue(engine.capacityHere() == 2 * SpringEngineBlockEntity.capacity(), "Overwound III should double it, holds " + engine.capacityHere());
		helper.assertTrue(engine.stored() == SpringEngineBlockEntity.capacity() + 5, "it should keep wind past a plain engine's full");
		List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(helper.getBlockState(out), helper.getLevel(), helper.absolutePos(out), engine);
		helper.assertTrue(drops.size() == 1 && MachineEnchantments.levelIn(drops.get(0).getTagEnchantments(), MachineEnchantments.OVERWOUND) == 3,
			"the engine should drop with its Overwound, drops " + drops);
		helper.succeed();
	}
}
