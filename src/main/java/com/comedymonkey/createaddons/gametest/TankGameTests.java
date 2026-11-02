package com.comedymonkey.createaddons.gametest;

import java.util.List;
import java.util.UUID;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.cage.MonsterCageBlock;
import com.comedymonkey.createaddons.content.cage.MonsterCageBlockEntity;
import com.comedymonkey.createaddons.content.cage.MonsterCageMenu;
import com.comedymonkey.createaddons.content.cage.MonsterJarItem;
import com.comedymonkey.createaddons.content.pen.AnimalCapture;
import com.comedymonkey.createaddons.content.pen.AnimalPenBlockEntity;
import com.comedymonkey.createaddons.content.pen.BottledAnimalItem;
import com.comedymonkey.createaddons.content.tank.FishTankBlock;
import com.comedymonkey.createaddons.content.tank.FishTankBlockEntity;
import com.comedymonkey.createaddons.content.tank.TankRules;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModItems;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Fish Tank's rules (design/specs/monster-cage-and-fish-tank.md, F5–F20), and the cages' and tanks' jars and bottles
 * coming back out as they went in, and Dispensers catching and releasing with them, on the Trade Bot's empty course (run with ./gradlew runGameTestServer).
 */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TankGameTests {
	private static final String COURSE = "trade_bot_course";

	private TankGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(TankGameTests.class);
	}

	/** A bottled fish put in and taken back out leaves the one bottle it came in, not a spare empty one as well. */
	@GameTest(template = COURSE)
	public static void tankBottleIsNotDuplicated(GameTestHelper helper) {
		FishTankBlockEntity tank = tank(helper, new BlockPos(4, 1, 4));
		FakePlayer player = player(helper);
		player.setItemInHand(InteractionHand.MAIN_HAND, bottled(helper, EntityType.COD));
		helper.useBlock(new BlockPos(4, 1, 4), player);
		if (!tank.hasCreature())
			helper.fail("the cod didn't go in");
		if (count(player, ModItems.CAPTURE_BOTTLE.get()) + count(player, ModItems.BOTTLED_ANIMAL.get()) != 0)
			helper.fail("putting the cod in handed back a bottle");
		ItemStack out = tank.getCaptiveSlot().extractItem(0, 1, false);
		if (!(out.getItem() instanceof BottledAnimalItem) || tank.hasCreature())
			helper.fail("taking it out gave " + out);
		helper.succeed();
	}

	/** The same for a jarred zombie in a Monster Cage: one jar in, the same jar out, no spare empty jar. */
	@GameTest(template = COURSE)
	public static void cageJarIsNotDuplicated(GameTestHelper helper) {
		BlockPos pos = new BlockPos(4, 1, 4);
		helper.setBlock(pos, ModBlocks.MONSTER_CAGE.get().defaultBlockState().setValue(MonsterCageBlock.FACING, Direction.NORTH));
		MonsterCageBlockEntity cage = helper.getBlockEntity(pos);
		FakePlayer player = player(helper);
		ItemStack jar = ModItems.IRON_JAR.get().fill(EntityType.ZOMBIE.create(helper.getLevel()));
		player.setItemInHand(InteractionHand.MAIN_HAND, jar);
		helper.useBlock(pos, player);
		if (!cage.hasMonster())
			helper.fail("the zombie didn't go in");
		if (count(player, ModItems.IRON_JAR.get()) != 0)
			helper.fail("putting the zombie in handed back a jar");
		ItemStack out = cage.getCaptiveSlot().extractItem(0, 1, false);
		if (!MonsterJarItem.isFilled(out) || cage.hasMonster())
			helper.fail("taking it out gave " + out);
		helper.succeed();
	}

	/** Tadpoles are fish to the game, but not to the tanks (F8, F20); fish, squid and axolotls are. */
	@GameTest(template = COURSE)
	public static void tadpolesStayOut(GameTestHelper helper) {
		FakePlayer player = player(helper);
		if (TankRules.isTankCreature(EntityType.TADPOLE.create(helper.getLevel())))
			helper.fail("a tadpole counts as a tank creature");
		if (AnimalCapture.canCapture(player, EntityType.TADPOLE.create(helper.getLevel())))
			helper.fail("the Capture Bottle catches tadpoles");
		for (EntityType<?> type : new EntityType<?>[] {EntityType.COD, EntityType.SALMON, EntityType.TROPICAL_FISH,
			EntityType.PUFFERFISH, EntityType.SQUID, EntityType.GLOW_SQUID, EntityType.AXOLOTL})
			if (!TankRules.isTankCreature(type.create(helper.getLevel())))
				helper.fail(type.getDescriptionId() + " isn't a tank creature");
		helper.succeed();
	}

	/** An Animal Pen refuses a bottled fish (with a hint) and keeps the bottle in the player's hand. */
	@GameTest(template = COURSE)
	public static void pensRefuseFish(GameTestHelper helper) {
		BlockPos pos = new BlockPos(4, 1, 4);
		helper.setBlock(pos, ModBlocks.ANIMAL_PEN.get());
		AnimalPenBlockEntity pen = helper.getBlockEntity(pos);
		FakePlayer player = player(helper);
		player.setItemInHand(InteractionHand.MAIN_HAND, bottled(helper, EntityType.AXOLOTL));
		helper.useBlock(pos, player);
		if (pen.hasAnimal())
			helper.fail("the pen took an axolotl");
		if (!(player.getMainHandItem().getItem() instanceof BottledAnimalItem))
			helper.fail("the bottle left the player's hand");
		helper.succeed();
	}

	/**
	 * A water bucket scoops a live fish out, never the last one; on a squid tank (squid don't go in buckets) or an empty
	 * one it fills the tank instead, and never puts water in the world.
	 */
	@GameTest(template = COURSE)
	public static void waterBucketScoopsOrFills(GameTestHelper helper) {
		FakePlayer player = player(helper);
		BlockPos codAt = new BlockPos(2, 1, 4), lastAt = new BlockPos(4, 1, 4), squidAt = new BlockPos(6, 1, 4), emptyAt = new BlockPos(8, 1, 4);
		FishTankBlockEntity cod = tank(helper, codAt), last = tank(helper, lastAt), squid = tank(helper, squidAt), empty = tank(helper, emptyAt);
		cod.setPonderSchool(bottled(helper, EntityType.COD), 3, 0, 0);
		last.setPonderSchool(bottled(helper, EntityType.COD), 1, 0, 0);
		squid.setPonderSchool(bottled(helper, EntityType.SQUID), 3, 0, 0);

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
		helper.useBlock(codAt, player);
		if (!player.getMainHandItem().is(Items.COD_BUCKET) || cod.getAdults() != 2)
			helper.fail("scooping gave " + player.getMainHandItem() + " and left " + cod.getAdults() + " adults");

		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
		helper.useBlock(lastAt, player);
		if (!player.getMainHandItem().is(Items.WATER_BUCKET) || last.getAdults() != 1)
			helper.fail("the last cod was scooped out");

		for (BlockPos at : new BlockPos[] {squidAt, emptyAt}) {
			FishTankBlockEntity tank = at == squidAt ? squid : empty;
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
			helper.useBlock(at, player);
			if (tank.getWater().getFluidAmount() != 1000 || !player.getMainHandItem().is(Items.BUCKET))
				helper.fail("a water bucket on " + (tank == squid ? "a squid tank" : "an empty tank") + " left "
					+ tank.getWater().getFluidAmount() + " mB and " + player.getMainHandItem());
		}
		if (squid.getAdults() != 3)
			helper.fail("the squid were touched");
		for (BlockPos at : new BlockPos[] {codAt, lastAt, squidAt, emptyAt})
			for (Direction d : Direction.values())
				if (helper.getLevel().getFluidState(helper.absolutePos(at.relative(d))).is(Fluids.WATER))
					helper.fail("water was put in the world beside a tank");
		helper.succeed();
	}

	/** A sword culls half the school for drops, and never the last one (F6, F18). */
	@GameTest(template = COURSE)
	public static void swordCullsHalf(GameTestHelper helper) {
		FakePlayer player = player(helper);
		BlockPos halfAt = new BlockPos(3, 1, 4), lastAt = new BlockPos(6, 1, 4);
		FishTankBlockEntity half = tank(helper, halfAt), last = tank(helper, lastAt);
		half.setPonderSchool(bottled(helper, EntityType.COD), 8, 0, 0);
		last.setPonderSchool(bottled(helper, EntityType.COD), 1, 0, 0);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
		helper.useBlock(halfAt, player);
		helper.useBlock(lastAt, player);
		if (half.getAdults() != 4)
			helper.fail("culling 8 cod left " + half.getAdults());
		boolean drops = false;
		for (int i = 0; i < half.getOutput().getSlots(); i++)
			drops |= half.getOutput().getStackInSlot(i).is(Items.COD);
		if (!drops)
			helper.fail("the cull gave no cod");
		if (last.getAdults() != 1)
			helper.fail("the last cod was culled");
		helper.succeed();
	}

	/**
	 * Breeding runs at the shaft's speed (four times as fast at 128 RPM as at 32), and not at all without water (F11,
	 * F14). Measured over 100 ticks once the shafts are up to speed.
	 */
	@GameTest(template = COURSE, timeoutTicks = 200)
	public static void breedingFollowsSpeedAndWater(GameTestHelper helper) {
		BlockPos slowAt = new BlockPos(2, 1, 3), fastAt = new BlockPos(5, 1, 3), dryAt = new BlockPos(8, 1, 3);
		FishTankBlockEntity slow = stocked(helper, slowAt, true), fast = stocked(helper, fastAt, true), dry = stocked(helper, dryAt, false);
		helper.runAtTickTime(2, () -> {
			motor(helper, slowAt.south(), 32);
			motor(helper, fastAt.south(), 128);
			motor(helper, dryAt.south(), 128);
		});
		float[] start = new float[2];
		helper.runAtTickTime(40, () -> {
			start[0] = slow.getBreedProgress();
			start[1] = fast.getBreedProgress();
		});
		helper.runAtTickTime(140, () -> {
			float slowGain = slow.getBreedProgress() - start[0], fastGain = fast.getBreedProgress() - start[1];
			if (slowGain <= 0)
				helper.fail("the 32 RPM tank didn't breed at all (speed " + slow.getSpeed() + ")");
			float ratio = fastGain / slowGain;
			if (ratio < 3.6f || ratio > 4.4f)
				helper.fail("128 RPM bred " + ratio + " times as fast as 32 RPM, not 4");
			if (dry.isBreeding() || dry.getBreedProgress() > 0)
				helper.fail("a tank without water bred");
			helper.succeed();
		});
	}


	/**
	 * A Dispenser catches with an empty Iron Jar and lets the monster back out with the full one, swapping the jars in its
	 * own slots (D48); a full Netherite Jar does nothing, since a Wither only goes into a Netherite cage (D50).
	 */
	@GameTest(template = COURSE)
	public static void dispenserCatchesAndReleasesWithJars(GameTestHelper helper) {
		BlockPos at = new BlockPos(2, 1, 4), front = at.east();
		DispenserBlockEntity dispenser = dispenser(helper, at);
		dispenser.setItem(0, new ItemStack(ModItems.IRON_JAR.get(), 2));
		helper.spawnWithNoFreeWill(EntityType.ZOMBIE, front);
		dispense(helper, at, 0);
		helper.assertEntityNotPresent(EntityType.ZOMBIE, front);
		int filled = slotWith(dispenser, stack -> MonsterJarItem.isFilled(stack));
		if (filled < 0 || dispenser.getItem(0).getCount() != 1)
			helper.fail("the jars after catching: " + dispenser.getItem(0) + ", filled in slot " + filled);
		dispense(helper, at, filled);
		helper.assertEntityPresent(EntityType.ZOMBIE, front);
		if (slotWith(dispenser, stack -> MonsterJarItem.isFilled(stack)) >= 0 || count(dispenser, ModItems.IRON_JAR.get()) != 2)
			helper.fail("releasing didn't leave two empty jars");

		ItemStack wither = ModItems.NETHERITE_JAR.get().fill(EntityType.WITHER.create(helper.getLevel()));
		dispenser.setItem(1, wither);
		dispense(helper, at, 1);
		helper.assertEntityNotPresent(EntityType.WITHER);
		if (!MonsterJarItem.isFilled(dispenser.getItem(1)))
			helper.fail("a Dispenser let a Wither out");
		helper.succeed();
	}

	/** The same with the Capture Bottle and a cow (D64). */
	@GameTest(template = COURSE)
	public static void dispenserCatchesAndReleasesWithBottles(GameTestHelper helper) {
		BlockPos at = new BlockPos(2, 1, 4), front = at.east();
		DispenserBlockEntity dispenser = dispenser(helper, at);
		dispenser.setItem(0, new ItemStack(ModItems.CAPTURE_BOTTLE.get()));
		helper.spawnWithNoFreeWill(EntityType.COW, front);
		dispense(helper, at, 0);
		helper.assertEntityNotPresent(EntityType.COW, front);
		if (!(dispenser.getItem(0).getItem() instanceof BottledAnimalItem))
			helper.fail("the bottle after catching: " + dispenser.getItem(0));
		dispense(helper, at, 0);
		helper.assertEntityPresent(EntityType.COW, front);
		if (!dispenser.getItem(0).is(ModItems.CAPTURE_BOTTLE.get()))
			helper.fail("the bottle after releasing: " + dispenser.getItem(0));
		helper.succeed();
	}


	/**
	 * A Dispenser on a Create contraption has no block entity: a stack of jars still catches, and the full jar comes out
	 * of the front instead of the mob being lost. And nothing is let out where its body doesn't fit (into glass).
	 */
	@GameTest(template = COURSE)
	public static void dispenserCatchesOnContraptionsAndNotIntoWalls(GameTestHelper helper) {
		BlockPos at = new BlockPos(2, 1, 4), front = at.east();
		dispenser(helper, at);
		BlockPos abs = helper.absolutePos(at);
		helper.spawnWithNoFreeWill(EntityType.ZOMBIE, front);
		ItemStack jars = new ItemStack(ModItems.IRON_JAR.get(), 2);
		ItemStack left = DispenserBlock.DISPENSER_REGISTRY.get(ModItems.IRON_JAR.get())
			.dispense(new BlockSource(helper.getLevel(), abs, helper.getLevel().getBlockState(abs), null), jars);
		helper.assertEntityNotPresent(EntityType.ZOMBIE, front);
		if (left.getCount() != 1)
			helper.fail("the jars left after a contraption catch: " + left);
		boolean thrown = !helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
			new net.minecraft.world.phys.AABB(helper.absolutePos(front)).inflate(2), item -> MonsterJarItem.isFilled(item.getItem())).isEmpty();
		if (!thrown)
			helper.fail("the full jar didn't come out of the contraption's dispenser");

		BlockPos walled = new BlockPos(6, 1, 4);
		DispenserBlockEntity dispenser = dispenser(helper, walled);
		helper.setBlock(walled.east(), Blocks.GLASS);
		dispenser.setItem(0, ModItems.IRON_JAR.get().fill(EntityType.ZOMBIE.create(helper.getLevel())));
		dispense(helper, walled, 0);
		if (!MonsterJarItem.isFilled(dispenser.getItem(0)))
			helper.fail("a zombie was let out into glass");
		helper.succeed();
	}

	/**
	 * A Netherite cage locked down (a Wither in, its field down) keeps its jar: the jar slot won't let it be picked up,
	 * and a shift-click, by the click or straight into the menu, hands out no copy of it (D41). An ordinary cage still
	 * hands its jar out on a shift-click, once, and is empty after.
	 */
	@GameTest(template = COURSE)
	public static void lockedDownCageKeepsItsJarOnShiftClick(GameTestHelper helper) {
		BlockPos lockedAt = new BlockPos(3, 1, 4), openAt = new BlockPos(6, 1, 4);
		helper.setBlock(lockedAt, ModBlocks.NETHERITE_MONSTER_CAGE.get().defaultBlockState().setValue(MonsterCageBlock.FACING, Direction.NORTH));
		helper.setBlock(openAt, ModBlocks.MONSTER_CAGE.get().defaultBlockState().setValue(MonsterCageBlock.FACING, Direction.NORTH));
		MonsterCageBlockEntity locked = helper.getBlockEntity(lockedAt), open = helper.getBlockEntity(openAt);
		ItemStack wither = ModItems.NETHERITE_JAR.get().fill(EntityType.WITHER.create(helper.getLevel()));
		if (!locked.getCaptiveSlot().insertItem(0, wither, false).isEmpty() || !locked.isLockedDown())
			helper.fail("the Netherite cage isn't locked down with a Wither and no power");
		open.getCaptiveSlot().insertItem(0, ModItems.IRON_JAR.get().fill(EntityType.ZOMBIE.create(helper.getLevel())), false);
		FakePlayer player = player(helper);

		MonsterCageMenu menu = new MonsterCageMenu(0, player.getInventory(), locked);
		if (menu.getSlot(0).mayPickup(player))
			helper.fail("the jar slot lets the jar be picked up during lock-down");
		menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
		ItemStack moved = menu.quickMoveStack(player, 0);
		if (!moved.isEmpty() || count(player, ModItems.NETHERITE_JAR.get()) != 0)
			helper.fail("a shift-click took a jar out of a locked-down cage: " + moved);
		if (!locked.hasMonster())
			helper.fail("the locked-down cage lost its Wither");

		MonsterCageMenu openMenu = new MonsterCageMenu(1, player.getInventory(), open);
		openMenu.clicked(0, 0, ClickType.QUICK_MOVE, player);
		if (count(player, ModItems.IRON_JAR.get()) != 1 || open.hasMonster())
			helper.fail("a shift-click on an ordinary cage left " + count(player, ModItems.IRON_JAR.get()) + " jars and "
				+ (open.hasMonster() ? "the zombie in" : "the cage empty"));
		helper.succeed();
	}

	/**
	 * An Iron Jar edited to hold a Wither (only a command or an NBT editor makes one) lets nothing out and no cage takes
	 * it: a Wither only ever comes out into a Netherite cage, from a Netherite Jar (D21). The same jar with a zombie
	 * still lets it out.
	 */
	@GameTest(template = COURSE)
	public static void wrongJarLetsNothingOut(GameTestHelper helper) {
		MonsterJarItem iron = ModItems.IRON_JAR.get();
		ItemStack bad = iron.fill(EntityType.WITHER.create(helper.getLevel()));
		if (MonsterJarItem.create(bad, helper.getLevel()).isPresent())
			helper.fail("an Iron Jar made a Wither");
		FakePlayer player = player(helper);
		player.setItemInHand(InteractionHand.MAIN_HAND, bad);
		releaseOnto(helper, player, new BlockPos(2, 0, 2));
		List<WitherBoss> out = helper.getLevel().getEntitiesOfClass(WitherBoss.class, new AABB(helper.absolutePos(new BlockPos(2, 1, 2))).inflate(8));
		if (!out.isEmpty()) {
			out.forEach(Entity::discard);
			helper.fail("an Iron Jar let a Wither out");
		}
		if (!MonsterJarItem.isFilled(player.getMainHandItem()))
			helper.fail("the jar was emptied");

		BlockPos cageAt = new BlockPos(6, 1, 4);
		helper.setBlock(cageAt, ModBlocks.NETHERITE_MONSTER_CAGE.get().defaultBlockState().setValue(MonsterCageBlock.FACING, Direction.NORTH));
		MonsterCageBlockEntity cage = helper.getBlockEntity(cageAt);
		if (cage.getCaptiveSlot().isItemValid(0, bad) || cage.getCaptiveSlot().insertItem(0, bad.copy(), false).isEmpty())
			helper.fail("a Netherite cage's slot took a Wither in an Iron Jar");
		helper.useBlock(cageAt, player);
		if (cage.hasMonster())
			helper.fail("a Netherite cage took a Wither in an Iron Jar from a click");

		player.setItemInHand(InteractionHand.MAIN_HAND, iron.fill(EntityType.ZOMBIE.create(helper.getLevel())));
		releaseOnto(helper, player, new BlockPos(2, 0, 6));
		helper.assertEntityPresent(EntityType.ZOMBIE, new BlockPos(2, 1, 6));
		helper.succeed();
	}

	/** Clicks the held jar on top of a block, as a player letting its monster out. */
	private static void releaseOnto(GameTestHelper helper, FakePlayer player, BlockPos floor) {
		BlockPos abs = helper.absolutePos(floor);
		player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false)));
	}

	/**
	 * Mechanical Drills never break a Netherite cage (it's in Create's non_breakable tag, D46), while the same drill
	 * breaks an ordinary cage beside it.
	 */
	@GameTest(template = COURSE, timeoutTicks = 300)
	public static void drillsLeaveTheNetheriteCage(GameTestHelper helper) {
		BlockPos netheriteAt = new BlockPos(5, 1, 2), ordinaryAt = new BlockPos(5, 1, 6);
		helper.setBlock(netheriteAt, ModBlocks.NETHERITE_MONSTER_CAGE.get().defaultBlockState().setValue(MonsterCageBlock.FACING, Direction.NORTH));
		helper.setBlock(ordinaryAt, ModBlocks.MONSTER_CAGE.get().defaultBlockState().setValue(MonsterCageBlock.FACING, Direction.NORTH));
		var netherite = helper.getLevel().getBlockState(helper.absolutePos(netheriteAt));
		if (BlockBreakingKineticBlockEntity.isBreakable(netherite, netherite.getDestroySpeed(helper.getLevel(), helper.absolutePos(netheriteAt))))
			helper.fail("Create counts the Netherite cage as breakable");
		for (BlockPos at : new BlockPos[] {netheriteAt, ordinaryAt}) {
			helper.setBlock(at.west(), BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "mechanical_drill"))
				.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));
			BlockPos motorAt = at.west(2);
			helper.setBlock(motorAt, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "creative_motor")).defaultBlockState()
				.setValue(BlockStateProperties.FACING, Direction.EAST));
			if (helper.getBlockEntity(motorAt) instanceof CreativeMotorBlockEntity motor)
				motor.generatedSpeed.setValue(256);
			else
				helper.fail("no creative motor");
		}
		helper.runAtTickTime(250, () -> {
			if (!helper.getLevel().getBlockState(helper.absolutePos(ordinaryAt)).isAir())
				helper.fail("the drill didn't break the ordinary cage, so the test proves nothing");
			if (!helper.getLevel().getBlockState(helper.absolutePos(netheriteAt)).is(ModBlocks.NETHERITE_MONSTER_CAGE.get()))
				helper.fail("a drill broke the Netherite cage");
			helper.succeed();
		});
	}

	/**
	 * A blue axolotl's young are all blue (F13): bred with the breeding and growing cut to a second, the school's blue
	 * count is all its adults, two scoops both give blue axolotls, and the count still matches after. A save from before,
	 * which counted only the captive blue, loads all blue. Its own batch: it changes the breeding times.
	 */
	@GameTest(template = COURSE, batch = "tank_blue_breeds_blue", timeoutTicks = 300)
	public static void blueCaptiveBreedsBlue(GameTestHelper helper) {
		int breed = CMConfig.Server.get(CMConfig.SERVER.fishTankBreedSeconds), grow = CMConfig.Server.get(CMConfig.SERVER.penGrowSeconds);
		CMConfig.SERVER.fishTankBreedSeconds.set(1);
		CMConfig.SERVER.penGrowSeconds.set(1);
		Runnable restore = () -> {
			CMConfig.SERVER.fishTankBreedSeconds.set(breed);
			CMConfig.SERVER.penGrowSeconds.set(grow);
		};
		BlockPos at = new BlockPos(4, 1, 4);
		FishTankBlockEntity tank;
		try {
			tank = tank(helper, at);
			Axolotl blue = EntityType.AXOLOTL.create(helper.getLevel());
			blue.setVariant(Axolotl.Variant.BLUE);
			tank.setPonderSchool(BottledAnimalItem.of(blue), 2, 0, 2);
			tank.getAutomationHandler().insertItem(0, new ItemStack(ModItems.FISH_FOOD.get(), 64), false);
			tank.getWaterHandler().fill(new FluidStack(Fluids.WATER, 16000), IFluidHandler.FluidAction.EXECUTE);
			motor(helper, at.south(), 64);
		} catch (RuntimeException e) {
			restore.run();
			throw e;
		}
		helper.runAtTickTime(200, () -> {
			try {
				if (tank.getAdults() <= 2)
					helper.fail("the tank bred no grown young in 10 s (" + tank.getAdults() + " adults, " + tank.getYoung() + " young)");
				if (tank.getBlue() != tank.getAdults())
					helper.fail(tank.getBlue() + " of " + tank.getAdults() + " adults are blue");
				FakePlayer player = player(helper);
				for (int i = 0; i < 2; i++) {
					player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
					helper.useBlock(at, player);
					ItemStack bucket = player.getMainHandItem();
					var data = bucket.get(DataComponents.BUCKET_ENTITY_DATA);
					if (!bucket.is(Items.AXOLOTL_BUCKET) || data == null || data.copyTag().getInt("Variant") != Axolotl.Variant.BLUE.getId())
						helper.fail("scoop " + (i + 1) + " gave " + bucket + " " + data);
				}
				if (tank.getBlue() != tank.getAdults())
					helper.fail("after scooping, " + tank.getBlue() + " of " + tank.getAdults() + " adults are blue");

				var registries = helper.getLevel().registryAccess();
				var saved = tank.saveWithoutMetadata(registries);
				saved.putInt("Blue", 1);
				tank.loadWithComponents(saved, registries);
				if (tank.getBlue() != tank.getAdults())
					helper.fail("an older save loaded " + tank.getBlue() + " of " + tank.getAdults() + " adults blue");
			} finally {
				restore.run();
			}
			helper.succeed();
		});
	}

	/**
	 * A school of another colour holds one blue axolotl at most (F13): with every roll made to hit and the breeding and
	 * growing cut to a second, litter after litter still gives just the one. The window shows it, it's the first scooped
	 * and the next is the captive's colour, and once it's out the next litter can have a blue one again. Its own batch:
	 * it changes the breeding times.
	 */
	@GameTest(template = COURSE, batch = "tank_one_blue_at_a_time", timeoutTicks = 400)
	public static void oneBlueAtATime(GameTestHelper helper) {
		int breed = CMConfig.Server.get(CMConfig.SERVER.fishTankBreedSeconds), grow = CMConfig.Server.get(CMConfig.SERVER.penGrowSeconds);
		CMConfig.SERVER.fishTankBreedSeconds.set(1);
		CMConfig.SERVER.penGrowSeconds.set(1);
		Runnable restore = () -> {
			CMConfig.SERVER.fishTankBreedSeconds.set(breed);
			CMConfig.SERVER.penGrowSeconds.set(grow);
		};
		BlockPos at = new BlockPos(4, 1, 4);
		FishTankBlockEntity tank;
		try {
			tank = tank(helper, at);
			Axolotl wild = EntityType.AXOLOTL.create(helper.getLevel());
			wild.setVariant(Axolotl.Variant.WILD);
			tank.setPonderSchool(BottledAnimalItem.of(wild), 2, 0, 0);
			tank.forceBlueRolls(true);
			tank.getAutomationHandler().insertItem(0, new ItemStack(ModItems.FISH_FOOD.get(), 64), false);
			tank.getWaterHandler().fill(new FluidStack(Fluids.WATER, 16000), IFluidHandler.FluidAction.EXECUTE);
			motor(helper, at.south(), 64);
		} catch (RuntimeException e) {
			restore.run();
			throw e;
		}
		helper.runAtTickTime(200, () -> {
			try {
				// several litters by now: 2 adults make 2, then 4, then 8...
				if (tank.getAdults() < 8)
					helper.fail("too few litters to show anything: " + tank.getAdults() + " adults, " + tank.getYoung() + " young");
				if (tank.getBlueHeld() != 1 || tank.getBlue() != 1)
					helper.fail("forced rolls over several litters made " + tank.getBlueHeld() + " blue (" + tank.getBlue() + " grown)");
				int size = Math.min(tank.getCount(), FishTankBlockEntity.SHOWN), shownBlue = 0;
				for (int i = 0; i < size; i++)
					if (tank.shownBlue(i, size))
						shownBlue++;
				if (shownBlue != 1)
					helper.fail("the window shows " + shownBlue + " blue of " + size);

				FakePlayer player = player(helper);
				int[] variants = new int[2];
				for (int i = 0; i < 2; i++) {
					player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
					helper.useBlock(at, player);
					ItemStack bucket = player.getMainHandItem();
					var data = bucket.get(DataComponents.BUCKET_ENTITY_DATA);
					if (!bucket.is(Items.AXOLOTL_BUCKET) || data == null)
						helper.fail("scoop " + (i + 1) + " gave " + bucket);
					variants[i] = data.copyTag().getInt("Variant");
				}
				if (variants[0] != Axolotl.Variant.BLUE.getId() || variants[1] != Axolotl.Variant.WILD.getId())
					helper.fail("the scoops gave variants " + variants[0] + " then " + variants[1] + ", not blue then wild");
				if (tank.getBlueHeld() != 0)
					helper.fail("after the blue was scooped " + tank.getBlueHeld() + " blue are left");
				// the litters so far ate the food and drank the water: more of both for the next
				tank.getAutomationHandler().insertItem(0, new ItemStack(ModItems.FISH_FOOD.get(), 16), false);
				tank.getWaterHandler().fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
			} catch (RuntimeException e) {
				restore.run();
				throw e;
			}
		});
		// the scoops made room: the next litter has a blue one again
		helper.runAtTickTime(300, () -> {
			try {
				if (tank.getBlueHeld() != 1)
					helper.fail("once the blue was out, a forced roll made " + tank.getBlueHeld() + " blue");
			} finally {
				tank.forceBlueRolls(false);
				restore.run();
			}
			helper.succeed();
		});
	}

	// ---------------------------------------------------------------- helpers

	/** A Dispenser facing east. */
	private static DispenserBlockEntity dispenser(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
		return helper.getBlockEntity(pos);
	}

	/** Fires the Dispenser with the stack in {@code slot}, as a redstone pulse would (it picks a slot at random). */
	private static void dispense(GameTestHelper helper, BlockPos pos, int slot) {
		BlockPos abs = helper.absolutePos(pos);
		DispenserBlockEntity dispenser = helper.getBlockEntity(pos);
		ItemStack stack = dispenser.getItem(slot);
		DispenseItemBehavior behaviour = DispenserBlock.DISPENSER_REGISTRY.get(stack.getItem());
		if (behaviour == null)
			helper.fail("no dispense behaviour for " + stack);
		dispenser.setItem(slot, behaviour.dispense(new BlockSource(helper.getLevel(), abs, helper.getLevel().getBlockState(abs), dispenser), stack));
	}

	private static int slotWith(DispenserBlockEntity dispenser, java.util.function.Predicate<ItemStack> test) {
		for (int i = 0; i < dispenser.getContainerSize(); i++)
			if (!dispenser.getItem(i).isEmpty() && test.test(dispenser.getItem(i)))
				return i;
		return -1;
	}

	private static int count(DispenserBlockEntity dispenser, Item item) {
		int n = 0;
		for (int i = 0; i < dispenser.getContainerSize(); i++)
			if (dispenser.getItem(i).is(item))
				n += dispenser.getItem(i).getCount();
		return n;
	}

	private static FishTankBlockEntity tank(GameTestHelper helper, BlockPos pos) {
		helper.setBlock(pos, ModBlocks.FISH_TANK.get().defaultBlockState().setValue(FishTankBlock.FACING, Direction.NORTH));
		return helper.getBlockEntity(pos);
	}

	/** Two cod with food, and water if asked. */
	private static FishTankBlockEntity stocked(GameTestHelper helper, BlockPos pos, boolean water) {
		FishTankBlockEntity tank = tank(helper, pos);
		tank.setPonderSchool(bottled(helper, EntityType.COD), 2, 0, 0);
		tank.getAutomationHandler().insertItem(0, new ItemStack(ModItems.FISH_FOOD.get(), 16), false);
		if (water)
			tank.getWaterHandler().fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
		return tank;
	}

	/** A Creative Motor behind the tank (its back faces south), turning at {@code rpm}. */
	private static void motor(GameTestHelper helper, BlockPos pos, int rpm) {
		helper.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "creative_motor")).defaultBlockState()
			.setValue(BlockStateProperties.FACING, Direction.NORTH));
		if (helper.getBlockEntity(pos) instanceof CreativeMotorBlockEntity motor)
			motor.generatedSpeed.setValue(rpm);
		else
			helper.fail("no creative motor");
	}

	private static ItemStack bottled(GameTestHelper helper, EntityType<?> type) {
		Entity entity = type.create(helper.getLevel());
		if (entity == null)
			throw new IllegalStateException("can't make " + type);
		return BottledAnimalItem.of(entity);
	}

	private static int count(FakePlayer player, Item item) {
		return player.getInventory().countItem(item);
	}

	private static FakePlayer player(GameTestHelper helper) {
		FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "tester"));
		Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 1, 6)));
		player.moveTo(at.x, at.y, at.z, 0, 0);
		return player;
	}
}
