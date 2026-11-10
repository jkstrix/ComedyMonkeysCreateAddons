package com.comedymonkey.createaddons.gametest;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.dripstone.PointedDripstoneGeneratorBlock;
import com.comedymonkey.createaddons.content.dripstone.PointedDripstoneGeneratorBlockEntity;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The Pointed Dripstone Generator (design/specs/pointed-dripstone-generator.md). */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DripstoneGeneratorGameTests {
	private static final String COURSE = "trade_bot_course";
	private static final BlockPos GEN = new BlockPos(2, 2, 2), MOTOR = GEN.south();

	private DripstoneGeneratorGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(DripstoneGeneratorGameTests.class);
	}

	private static PointedDripstoneGeneratorBlockEntity generator(GameTestHelper helper) {
		helper.setBlock(GEN, ModBlocks.POINTED_DRIPSTONE_GENERATOR.get().defaultBlockState().setValue(PointedDripstoneGeneratorBlock.FACING, Direction.NORTH));
		return helper.getBlockEntity(GEN);
	}

	/** A creative motor behind it (its shaft into the generator's back). */
	private static void motor(GameTestHelper helper, int rpm) {
		helper.setBlock(MOTOR, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "creative_motor")).defaultBlockState()
			.setValue(BlockStateProperties.FACING, Direction.NORTH));
		if (helper.getBlockEntity(MOTOR) instanceof CreativeMotorBlockEntity m)
			m.generatedSpeed.setValue(rpm);
		else
			helper.fail("no creative motor");
	}

	private static void seed(PointedDripstoneGeneratorBlockEntity be, int count) {
		be.getSeed().insertItem(0, new ItemStack(Items.POINTED_DRIPSTONE, count), false);
	}

	private static void water(PointedDripstoneGeneratorBlockEntity be, int mb) {
		be.getWaterHandler().fill(new FluidStack(Fluids.WATER, mb), IFluidHandler.FluidAction.EXECUTE);
	}

	/** The impact the network holds for it (0 off a network). */
	private static float held(PointedDripstoneGeneratorBlockEntity be) {
		if (!be.hasNetwork())
			return 0;
		Float held = be.getOrCreateNetwork().members.get(be);
		return held == null ? 0 : held;
	}

	/**
	 * At 256 RPM a cycle of four is 20 s (one every 5 s): all four tips grow (240 ticks, taking 400 mB as they start),
	 * then the hammer goes round, a quarter turn (10) and a knock (30) for each, a piece dropping on each strike (15 into
	 * the knock): at ticks 265, 305, 345 and 385.
	 */
	@GameTest(template = COURSE, timeoutTicks = 450)
	public static void growsFourEveryTwentySecondsAt256Rpm(GameTestHelper helper) {
		PointedDripstoneGeneratorBlockEntity be = generator(helper);
		seed(be, 4);
		water(be, 1000);
		motor(helper, 256);
		helper.runAtTickTime(200, () -> {
			if (be.getStored() != 0)
				helper.fail("made " + be.getStored() + " Pointed Dripstone while the tips were still growing");
			if (be.getWater().getFluidAmount() != 600)
				helper.fail("took " + (1000 - be.getWater().getFluidAmount()) + " mB of water for four tips, not 400");
			if (!be.isRunning())
				helper.fail("not running: " + be.status().getString());
			if (held(be) <= 0)
				helper.fail("running, but puts no stress on the network");
		});
		// one on each strike: two struck by tick 320, all four by 395 (before the next four start growing, at 400)
		helper.runAtTickTime(320, () -> {
			if (be.getStored() != 2)
				helper.fail("had " + be.getStored() + " Pointed Dripstone two strikes into the round, not 2");
		});
		helper.runAtTickTime(395, () -> {
			if (be.getStored() != 4)
				helper.fail("made " + be.getStored() + " Pointed Dripstone in a 20 s cycle, not 4");
			if (be.getWater().getFluidAmount() != 600)
				helper.fail("used " + (1000 - be.getWater().getFluidAmount()) + " mB of water for four, not 400");
			helper.succeed();
		});
	}

	/** Without all four seeds, or without water for the next tip, it stops and puts no stress on the network. */
	@GameTest(template = COURSE, timeoutTicks = 120)
	public static void idlesWithNoStressWithoutSeedOrWater(GameTestHelper helper) {
		PointedDripstoneGeneratorBlockEntity be = generator(helper);
		seed(be, 3);
		water(be, 1000);
		motor(helper, 256);
		helper.runAtTickTime(30, () -> {
			turning(helper, be);
			if (!be.isIdle() || be.isRunning())
				helper.fail("runs on three seeds");
			if (held(be) != 0)
				helper.fail("idle without a full seed, but holds " + held(be) + " stress");
			// the fourth seed, and the water taken away: it waits for water for its next four tips
			seed(be, 1);
			be.getWater().drain(1000, IFluidHandler.FluidAction.EXECUTE);
		});
		helper.runAtTickTime(80, () -> {
			if (!be.needsWater() || be.isRunning())
				helper.fail("doesn't wait for water: " + be.status().getString());
			if (held(be) != 0)
				helper.fail("waiting for water, but holds " + held(be) + " stress");
			helper.succeed();
		});
	}

	/** Full, or switched off by redstone, it stops with no stress. */
	@GameTest(template = COURSE, timeoutTicks = 80)
	public static void fullOrSwitchedOffItStops(GameTestHelper helper) {
		PointedDripstoneGeneratorBlockEntity be = generator(helper);
		be.setPonderState(PointedDripstoneGeneratorBlockEntity.CAPACITY, 4, 1000);
		motor(helper, 256);
		helper.runAtTickTime(20, () -> {
			turning(helper, be);
			if (be.isRunning() || held(be) != 0)
				helper.fail("full, but running (" + held(be) + " stress)");
			be.getOutputHandler().extractItem(0, 64, false);
			helper.setBlock(GEN.west(), Blocks.REDSTONE_BLOCK);
		});
		helper.runAtTickTime(40, () -> {
			if (!be.isPowered() || be.isRunning() || held(be) != 0)
				helper.fail("a redstone signal didn't switch it off");
			helper.succeed();
		});
	}

	/**
	 * Water only, and only through the sides (not the window or the shaft); funnels take Pointed Dripstone out from any
	 * side but can't put anything in, the seed least of all.
	 */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void waterInTheSidesSeedByHandOnly(GameTestHelper helper) {
		PointedDripstoneGeneratorBlockEntity be = generator(helper);
		var level = helper.getLevel();
		BlockPos at = helper.absolutePos(GEN);
		IFluidHandler side = level.getCapability(Capabilities.FluidHandler.BLOCK, at, Direction.WEST);
		if (side == null)
			helper.fail("no water in through the side");
		if (level.getCapability(Capabilities.FluidHandler.BLOCK, at, Direction.NORTH) != null
			|| level.getCapability(Capabilities.FluidHandler.BLOCK, at, Direction.SOUTH) != null)
			helper.fail("water goes in through the window or the shaft");
		if (side.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) != 0)
			helper.fail("took lava");
		if (side.fill(new FluidStack(Fluids.WATER, 5000), IFluidHandler.FluidAction.EXECUTE) != 1000)
			helper.fail("the tank doesn't hold 1,000 mB");
		if (!side.drain(100, IFluidHandler.FluidAction.EXECUTE).isEmpty())
			helper.fail("pipes can draw its water back out");
		var items = level.getCapability(Capabilities.ItemHandler.BLOCK, at, Direction.UP);
		if (items == null)
			helper.fail("no item handler");
		ItemStack left = items.insertItem(0, new ItemStack(Items.POINTED_DRIPSTONE, 4), false);
		if (left.getCount() != 4 || be.getSeedCount() != 0)
			helper.fail("a funnel could put Pointed Dripstone in (seed " + be.getSeedCount() + ")");
		helper.succeed();
	}

	/** Broken, it drops itself, its seed and its store (the water is lost). */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void breakingDropsTheSeedAndTheStore(GameTestHelper helper) {
		PointedDripstoneGeneratorBlockEntity be = generator(helper);
		be.setPonderState(5, 4, 500);
		helper.getLevel().destroyBlock(helper.absolutePos(GEN), true);
		helper.runAtTickTime(2, () -> {
			int dripstone = 0, generators = 0;
			for (ItemEntity e : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(GEN)).inflate(2))) {
				if (e.getItem().is(Items.POINTED_DRIPSTONE))
					dripstone += e.getItem().getCount();
				if (e.getItem().is(ModBlocks.POINTED_DRIPSTONE_GENERATOR.get().asItem()))
					generators += e.getItem().getCount();
			}
			if (dripstone != 9 || generators != 1)
				helper.fail("dropped " + dripstone + " Pointed Dripstone and " + generators + " generators, not 9 (4 seed, 5 stored) and 1");
			helper.succeed();
		});
	}

	/** The motor reaches it: speed and a network (so "no stress" means idle, not unpowered). */
	private static void turning(GameTestHelper helper, PointedDripstoneGeneratorBlockEntity be) {
		if (be.getSpeed() == 0 || !be.hasNetwork())
			helper.fail("the motor doesn't turn it (speed " + be.getSpeed() + ")");
	}

	/** At 128 RPM everything but the knocks takes twice as long: a cycle of four is 40 s, the first strike at 665 ticks. */
	@GameTest(template = COURSE, timeoutTicks = 760)
	public static void halfTheSpeedGrowsHalfAsFast(GameTestHelper helper) {
		PointedDripstoneGeneratorBlockEntity be = generator(helper);
		seed(be, 4);
		water(be, 1000);
		motor(helper, 128);
		helper.runAtTickTime(600, () -> {
			turning(helper, be);
			if (be.getStored() != 0)
				helper.fail("made " + be.getStored() + " at 128 RPM by 30 s; the tips take 32 s to grow");
		});
		helper.runAtTickTime(720, () -> {
			if (be.getStored() != 2)
				helper.fail("had " + be.getStored() + " at 128 RPM after 36 s, not 2 (strikes at 665 and 705)");
			helper.succeed();
		});
	}

	/** A Deployer (a fake player) can't seed it: the seed goes in by hand only. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void aDeployerCantSeedIt(GameTestHelper helper) {
		PointedDripstoneGeneratorBlockEntity be = generator(helper);
		var fake = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
		ItemStack held = new ItemStack(Items.POINTED_DRIPSTONE, 4);
		fake.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, held);
		BlockPos at = helper.absolutePos(GEN);
		var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at), Direction.NORTH, at, false);
		helper.getLevel().getBlockState(at).useItemOn(held, helper.getLevel(), fake, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
		if (be.getSeedCount() != 0 || held.getCount() != 4)
			helper.fail("a fake player seeded it (seed " + be.getSeedCount() + ")");
		helper.succeed();
	}

	/** Seeding by hand takes only what goes in: a stack that won't stack with the seed already there is left alone. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void handSeedingTakesOnlyWhatGoesIn(GameTestHelper helper) {
		PointedDripstoneGeneratorBlockEntity be = generator(helper);
		seed(be, 2);
		var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		ItemStack named = new ItemStack(Items.POINTED_DRIPSTONE, 5);
		named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Spike"));
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, named);
		BlockPos at = helper.absolutePos(GEN);
		var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at), Direction.NORTH, at, false);
		helper.getLevel().getBlockState(at).useItemOn(named, helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
		if (named.getCount() != 5 || be.getSeedCount() != 2)
			helper.fail("a stack that couldn't go in lost " + (5 - named.getCount()) + " (seed " + be.getSeedCount() + ")");
		ItemStack plain = new ItemStack(Items.POINTED_DRIPSTONE, 5);
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, plain);
		helper.getLevel().getBlockState(at).useItemOn(plain, helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
		if (plain.getCount() != 3 || be.getSeedCount() != 4)
			helper.fail("plain Pointed Dripstone: " + plain.getCount() + " left in hand, seed " + be.getSeedCount() + "; want 3 and 4");
		helper.succeed();
	}

	/** Its store never hands out the same stack twice: the screen gives what it shows to the player, so a kept one could dupe. */
	@GameTest(template = COURSE, timeoutTicks = 20)
	public static void theStoreShowsAFreshStackEachTime(GameTestHelper helper) {
		PointedDripstoneGeneratorBlockEntity be = generator(helper);
		be.setPonderState(10, 4, 0);
		ItemStack a = be.getOutputHandler().getStackInSlot(0), b = be.getOutputHandler().getStackInSlot(0);
		if (a == b)
			helper.fail("the store hands out one stack object twice");
		a.shrink(10);
		if (be.getStored() != 10 || be.getOutputHandler().getStackInSlot(0).getCount() != 10)
			helper.fail("changing a shown stack changed the store");
		helper.succeed();
	}
}
