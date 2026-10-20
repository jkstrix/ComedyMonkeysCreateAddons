package com.comedymonkey.createaddons.content.harvester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.testutil.TestConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * The Auto Harvester's automation and planter handlers, grow times, fixed stress and saved state, on a block
 * entity with no level. With no level the planter's {@code setStackInSlot} takes its client path (no sound, no
 * block update), and automation is only tried simulated, as a real insert plants through the level.
 */
class AutoHarvesterBlockEntityTest {
	private static final int PLANTER = 9;
	private static HolderLookup.Provider registries;

	@BeforeAll
	static void lookup() {
		registries = VanillaRegistries.createLookup();
	}

	@BeforeEach
	void config() {
		TestConfig.loadDefaults();
	}

	@AfterEach
	void restore() {
		TestConfig.reset();
	}

	private static AutoHarvesterBlockEntity harvester() {
		return new AutoHarvesterBlockEntity(BlockPos.ZERO, ModBlocks.AUTO_HARVESTER.get().defaultBlockState());
	}

	private static AutoHarvesterBlockEntity planted(ItemStack plant) {
		AutoHarvesterBlockEntity be = harvester();
		be.setPonderPlant(plant, 0);
		return be;
	}

	// ---------------------------------------------------------------- automation

	@Test
	void automationSeesNineOutputSlotsAndThePlanter() {
		IItemHandler automation = harvester().getAutomationHandler();
		assertEquals(10, automation.getSlots());
		for (int slot = 0; slot < PLANTER; slot++)
			assertEquals(64, automation.getSlotLimit(slot));
		assertEquals(1, automation.getSlotLimit(PLANTER));
	}

	@Test
	void automationCannotPutAnythingIntoTheOutput() {
		AutoHarvesterBlockEntity be = harvester();
		IItemHandler automation = be.getAutomationHandler();
		for (int slot = 0; slot < PLANTER; slot++) {
			ItemStack wheat = new ItemStack(Items.WHEAT, 10);
			assertEquals(10, automation.insertItem(slot, wheat, false).getCount());
			assertFalse(automation.isItemValid(slot, new ItemStack(Items.WHEAT_SEEDS)));
		}
		assertFalse(be.hasOutput());
	}

	@Test
	void automationPlantsOneSeedIntoAnEmptyPlanter() {
		AutoHarvesterBlockEntity be = harvester();
		IItemHandler automation = be.getAutomationHandler();
		assertTrue(automation.isItemValid(PLANTER, new ItemStack(Items.WHEAT_SEEDS)));
		ItemStack left = automation.insertItem(PLANTER, new ItemStack(Items.WHEAT_SEEDS, 5), true);
		assertEquals(4, left.getCount());
		assertTrue(ItemStack.isSameItem(left, new ItemStack(Items.WHEAT_SEEDS)));
		// Only simulated
		assertFalse(be.hasPlant());
	}

	@Test
	void automationRefusesWhatCannotBePlanted() {
		IItemHandler automation = harvester().getAutomationHandler();
		assertFalse(automation.isItemValid(PLANTER, new ItemStack(Items.STICK)));
		assertFalse(automation.isItemValid(PLANTER, new ItemStack(Items.DIRT)));
		assertEquals(3, automation.insertItem(PLANTER, new ItemStack(Items.STICK, 3), true).getCount());
		assertTrue(automation.insertItem(PLANTER, ItemStack.EMPTY, true).isEmpty());
	}

	@Test
	void automationRefusesASecondPlant() {
		AutoHarvesterBlockEntity be = planted(new ItemStack(Items.WHEAT_SEEDS));
		IItemHandler automation = be.getAutomationHandler();
		assertFalse(automation.isItemValid(PLANTER, new ItemStack(Items.CARROT)));
		assertEquals(5, automation.insertItem(PLANTER, new ItemStack(Items.CARROT, 5), true).getCount());
	}

	@Test
	void automationNeverTakesThePlantOut() {
		AutoHarvesterBlockEntity be = planted(new ItemStack(Items.WHEAT_SEEDS));
		IItemHandler automation = be.getAutomationHandler();
		assertTrue(automation.extractItem(PLANTER, 1, false).isEmpty());
		assertTrue(automation.extractItem(PLANTER, 64, true).isEmpty());
		assertTrue(be.hasPlant());
		assertTrue(ItemStack.isSameItem(automation.getStackInSlot(PLANTER), new ItemStack(Items.WHEAT_SEEDS)));
	}

	@Test
	void automationTakesTheHarvestOut() {
		AutoHarvesterBlockEntity be = harvester();
		be.getOutput().setStackInSlot(3, new ItemStack(Items.WHEAT, 10));
		assertTrue(be.hasOutput());
		IItemHandler automation = be.getAutomationHandler();
		assertEquals(10, automation.getStackInSlot(3).getCount());

		assertEquals(4, automation.extractItem(3, 4, true).getCount());
		assertEquals(10, be.getOutput().getStackInSlot(3).getCount());
		ItemStack taken = automation.extractItem(3, 4, false);
		assertEquals(4, taken.getCount());
		assertTrue(taken.is(Items.WHEAT));
		assertEquals(6, be.getOutput().getStackInSlot(3).getCount());
		assertEquals(6, automation.extractItem(3, 64, false).getCount());
		assertFalse(be.hasOutput());
	}

	@Test
	void comparatorReadsTheHarvestBuffer() {
		AutoHarvesterBlockEntity be = harvester();
		assertEquals(0, be.getComparatorOutput());
		be.getOutput().setStackInSlot(0, new ItemStack(Items.WHEAT, 1));
		assertTrue(be.getComparatorOutput() > 0);
		for (int slot = 0; slot < PLANTER; slot++)
			be.getOutput().setStackInSlot(slot, new ItemStack(Items.WHEAT, 64));
		assertEquals(15, be.getComparatorOutput());
	}

	// ---------------------------------------------------------------- the planter slot (screen)

	@Test
	void planterHoldsOneAndHandsOutCopies() {
		AutoHarvesterBlockEntity be = harvester();
		IItemHandlerModifiable planter = be.getPlanter();
		assertEquals(1, planter.getSlots());
		assertEquals(1, planter.getSlotLimit(0));
		planter.setStackInSlot(0, new ItemStack(Items.OAK_SAPLING, 12));
		assertTrue(be.hasPlant());
		assertEquals(1, be.getPlant().getCount());

		ItemStack shown = planter.getStackInSlot(0);
		shown.setCount(0);
		assertTrue(be.hasPlant(), "changing what the slot shows mustn't change the plant");
	}

	@Test
	void planterOnlyTakesPlantsAndOnlyWhenEmpty() {
		AutoHarvesterBlockEntity be = harvester();
		IItemHandlerModifiable planter = be.getPlanter();
		assertFalse(planter.isItemValid(0, new ItemStack(Items.STICK)));
		assertTrue(planter.isItemValid(0, new ItemStack(Items.OAK_SAPLING)));
		assertEquals(2, planter.insertItem(0, new ItemStack(Items.STICK, 2), true).getCount());
		assertEquals(1, planter.insertItem(0, new ItemStack(Items.WHEAT_SEEDS, 2), true).getCount());
		assertFalse(be.hasPlant());

		planter.setStackInSlot(0, new ItemStack(Items.WHEAT_SEEDS));
		assertEquals(2, planter.insertItem(0, new ItemStack(Items.CARROT, 2), true).getCount());
	}

	@Test
	void takingThePlantOutOfThePlanterEmptiesIt() {
		AutoHarvesterBlockEntity be = harvester();
		IItemHandlerModifiable planter = be.getPlanter();
		assertTrue(planter.extractItem(0, 1, false).isEmpty());
		planter.setStackInSlot(0, new ItemStack(Items.CARROT));

		assertTrue(planter.extractItem(0, 0, false).isEmpty());
		ItemStack simulated = planter.extractItem(0, 1, true);
		assertTrue(simulated.is(Items.CARROT));
		assertTrue(be.hasPlant());

		ItemStack taken = planter.extractItem(0, 1, false);
		assertTrue(taken.is(Items.CARROT));
		assertEquals(1, taken.getCount());
		assertFalse(be.hasPlant());
	}

	// ---------------------------------------------------------------- grow time

	@Test
	void cropsTakeTheConfiguredTimeScaledByTheirGrowth() {
		// 40 s of growing, then the 4 s harvest
		assertEquals(44f, planted(new ItemStack(Items.WHEAT_SEEDS)).getSecondsPerHarvest(), 1e-4f);
		// 800 ticks x 16/49 = 261
		assertEquals((261 + AutoHarvesterBlockEntity.HARVEST_TICKS) / 20f,
			planted(new ItemStack(Items.SUGAR_CANE)).getSecondsPerHarvest(), 1e-4f);
		// held at a fifth: 160 ticks
		assertEquals(12f, planted(new ItemStack(Items.BAMBOO)).getSecondsPerHarvest(), 1e-4f);
	}

	@Test
	void treesTakeTheConfiguredTreeTime() {
		assertEquals(84f, planted(new ItemStack(Items.OAK_SAPLING)).getSecondsPerHarvest(), 1e-4f);
		assertEquals(84f, planted(new ItemStack(Items.MANGROVE_PROPAGULE)).getSecondsPerHarvest(), 1e-4f);
	}

	@Test
	void growTimeFollowsTheConfig() {
		TestConfig.set(CMConfig.SERVER.harvesterCropSeconds, 10);
		TestConfig.set(CMConfig.SERVER.harvesterTreeSeconds, 30);
		assertEquals(14f, planted(new ItemStack(Items.WHEAT_SEEDS)).getSecondsPerHarvest(), 1e-4f);
		assertEquals(34f, planted(new ItemStack(Items.OAK_SAPLING)).getSecondsPerHarvest(), 1e-4f);
	}

	@Test
	void growingNeverTakesLessThanASecond() {
		TestConfig.set(CMConfig.SERVER.harvesterCropSeconds, 1);
		// 20 ticks x 0.2 would be 4 ticks; it's held at 20
		assertEquals(5f, planted(new ItemStack(Items.BAMBOO)).getSecondsPerHarvest(), 1e-4f);
	}

	@Test
	void growthIsAFractionOfTheGrowTime() {
		AutoHarvesterBlockEntity be = harvester();
		assertEquals(0f, be.getGrowth(0));
		be.setPonderPlant(new ItemStack(Items.WHEAT_SEEDS), 0.25f);
		assertEquals(0.25f, be.getGrowth(0), 1e-6f);
		assertEquals(0.25f, be.getGrowth(1), 1e-6f, "doesn't creep on while it isn't running");
		assertEquals(-1f, be.getHarvestProgress(0));
		be.startPonderHarvest();
		assertEquals(1f, be.getGrowth(0), 1e-6f);
		assertEquals(0f, be.getHarvestProgress(0));
	}

	// ---------------------------------------------------------------- fixed stress

	@Test
	void idleWithNothingPlanted() {
		AutoHarvesterBlockEntity be = harvester();
		assertTrue(be.isIdle());
		assertFalse(be.isRunning());
		be.setSpeed(64);
		assertEquals(0f, be.calculateStressApplied());
	}

	@Test
	void sameStressAtAnySpeed() {
		AutoHarvesterBlockEntity be = planted(new ItemStack(Items.WHEAT_SEEDS));
		assertFalse(be.isIdle());
		for (float speed : new float[] {1, 16, 64, -128, 256}) {
			be.setSpeed(speed);
			assertEquals(512f, be.calculateStressApplied() * Math.abs(speed), 1e-3f, "at " + speed + " RPM");
		}
	}

	@Test
	void noStressWhileStopped() {
		AutoHarvesterBlockEntity be = planted(new ItemStack(Items.WHEAT_SEEDS));
		be.setSpeed(0);
		assertEquals(0f, be.calculateStressApplied());
		assertFalse(be.isRunning());
	}

	@Test
	void stressFollowsTheConfig() {
		TestConfig.set(CMConfig.SERVER.harvesterStress, 2048);
		AutoHarvesterBlockEntity be = planted(new ItemStack(Items.OAK_SAPLING));
		be.setSpeed(32);
		assertEquals(64f, be.calculateStressApplied(), 1e-4f);
	}

	@Test
	void runsWhenPlantedAndTurning() {
		AutoHarvesterBlockEntity be = planted(new ItemStack(Items.WHEAT_SEEDS));
		be.setSpeed(-16);
		assertTrue(be.isRunning());
		assertTrue(be.isWorking());
	}

	// ---------------------------------------------------------------- saving

	@Test
	void savesAndLoadsPlantGrowthAndHarvest() {
		AutoHarvesterBlockEntity be = harvester();
		be.setPonderPlant(new ItemStack(Items.CARROT), 0.5f);
		be.getOutput().setStackInSlot(0, new ItemStack(Items.CARROT, 7));
		be.getOutput().setStackInSlot(8, new ItemStack(Items.POISONOUS_POTATO, 1));
		CompoundTag tag = new CompoundTag();
		be.write(tag, registries, false);

		AutoHarvesterBlockEntity loaded = harvester();
		loaded.read(tag, registries, false);
		assertTrue(loaded.getPlant().is(Items.CARROT));
		assertEquals(1, loaded.getPlant().getCount());
		assertEquals(0.5f, loaded.getGrowth(0), 1e-6f);
		assertEquals(44f, loaded.getSecondsPerHarvest(), 1e-4f);
		assertEquals(7, loaded.getOutput().getStackInSlot(0).getCount());
		assertTrue(loaded.getOutput().getStackInSlot(0).is(Items.CARROT));
		assertTrue(loaded.getOutput().getStackInSlot(8).is(Items.POISONOUS_POTATO));
		assertFalse(loaded.isIdle());
	}

	@Test
	void loadingRecomputesGrowTimeFromTheCurrentConfig() {
		AutoHarvesterBlockEntity be = harvester();
		be.setPonderPlant(new ItemStack(Items.WHEAT_SEEDS), 0.5f);
		CompoundTag tag = new CompoundTag();
		be.write(tag, registries, false);

		TestConfig.set(CMConfig.SERVER.harvesterCropSeconds, 20);
		AutoHarvesterBlockEntity loaded = harvester();
		loaded.read(tag, registries, false);
		assertEquals(24f, loaded.getSecondsPerHarvest(), 1e-4f);
		// 400 of the old 800 ticks is the whole of the new 400
		assertEquals(1f, loaded.getGrowth(0), 1e-6f);
	}

	@Test
	void anUnplacedHarvestKeepsItIdleAfterLoading() {
		AutoHarvesterBlockEntity be = harvester();
		be.setPonderPlant(new ItemStack(Items.WHEAT_SEEDS), 0);
		CompoundTag tag = new CompoundTag();
		be.write(tag, registries, false);
		ListTag pending = new ListTag();
		pending.add(new ItemStack(Items.WHEAT, 5).save(registries));
		tag.put("Pending", pending);

		AutoHarvesterBlockEntity loaded = harvester();
		loaded.read(tag, registries, false);
		assertTrue(loaded.isIdle());
		loaded.setSpeed(64);
		assertFalse(loaded.isRunning());
		assertEquals(0f, loaded.calculateStressApplied());
	}

	@Test
	void anEmptySaveLoadsEmpty() {
		AutoHarvesterBlockEntity loaded = planted(new ItemStack(Items.WHEAT_SEEDS));
		loaded.read(new CompoundTag(), registries, false);
		assertFalse(loaded.hasPlant());
		assertFalse(loaded.hasOutput());
		assertEquals(-1f, loaded.getHarvestProgress(0));
		assertEquals(0, loaded.getBountifulLevel());
	}

	@Test
	void clearContentEmptiesEverything() {
		AutoHarvesterBlockEntity be = planted(new ItemStack(Items.WHEAT_SEEDS));
		be.getOutput().setStackInSlot(0, new ItemStack(Items.WHEAT, 3));
		be.clearContent();
		assertFalse(be.hasPlant());
		assertFalse(be.hasOutput());
	}
}
