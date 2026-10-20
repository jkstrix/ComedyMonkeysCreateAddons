package com.comedymonkey.createaddons.content.wellspring;

import static com.comedymonkey.createaddons.content.wellspring.WellspringTestSupport.fill;
import static com.comedymonkey.createaddons.content.wellspring.WellspringTestSupport.install;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.testutil.TestConfig;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** The Wellspring's upgrade slots: which upgrade goes where, the configured limits, and when one may come out. */
class WellspringUpgradeInventoryTest {
	private static final int FIRST_CAPACITY = WellspringUpgradeInventory.FIRST_CAPACITY_SLOT;

	private WellspringBlockEntity be;
	private WellspringUpgradeInventory upgrades;

	@BeforeEach
	void setUp() {
		TestConfig.loadDefaults();
		be = WellspringTestSupport.create();
		upgrades = be.getUpgrades();
	}

	@AfterEach
	void restore() {
		TestConfig.reset();
	}

	private static ItemStack stack(WellspringUpgrade upgrade) {
		return new ItemStack(upgrade.item());
	}

	// ---------------------------------------------------------------- layout and limits

	@Test
	void layout() {
		assertEquals(6, upgrades.getSlots());
		for (int slot = 0; slot < upgrades.getSlots(); slot++) {
			assertEquals(slot >= 3, WellspringUpgradeInventory.isCapacitySlot(slot), "slot " + slot);
			assertEquals(1, upgrades.getSlotLimit(slot), "slot " + slot);
		}
	}

	@Test
	void everySlotIsUnlockedByDefault() {
		for (int slot = 0; slot < upgrades.getSlots(); slot++)
			assertTrue(WellspringUpgradeInventory.isUnlocked(slot), "slot " + slot);
	}

	@Test
	void unlockedSlotsFollowTheConfig() {
		TestConfig.set(CMConfig.SERVER.maxUpgrades, 1);
		TestConfig.set(CMConfig.SERVER.maxCapacityUpgrades, 2);
		assertTrue(WellspringUpgradeInventory.isUnlocked(0));
		assertFalse(WellspringUpgradeInventory.isUnlocked(1));
		assertFalse(WellspringUpgradeInventory.isUnlocked(2));
		assertTrue(WellspringUpgradeInventory.isUnlocked(FIRST_CAPACITY));
		assertTrue(WellspringUpgradeInventory.isUnlocked(FIRST_CAPACITY + 1));
		assertFalse(WellspringUpgradeInventory.isUnlocked(FIRST_CAPACITY + 2));

		TestConfig.set(CMConfig.SERVER.maxCapacityUpgrades, 0);
		for (int i = 0; i < WellspringUpgradeInventory.CAPACITY_SLOTS; i++)
			assertFalse(WellspringUpgradeInventory.isUnlocked(FIRST_CAPACITY + i));
	}

	// ---------------------------------------------------------------- what goes where

	@Test
	void generationSlotsTakeAnyGenerationUpgrade() {
		for (WellspringUpgrade upgrade : WellspringUpgrade.values())
			for (int slot = 0; slot < WellspringUpgradeInventory.GENERATION_SLOTS; slot++)
				assertEquals(upgrade.isGeneration(), upgrades.isItemValid(slot, stack(upgrade)), upgrade + " in " + slot);
	}

	@Test
	void capacitySlotsTakeOnlyCapacityUpgrades() {
		for (WellspringUpgrade upgrade : WellspringUpgrade.values())
			for (int i = 0; i < WellspringUpgradeInventory.CAPACITY_SLOTS; i++)
				assertEquals(upgrade == WellspringUpgrade.CAPACITY, upgrades.isItemValid(FIRST_CAPACITY + i, stack(upgrade)),
					upgrade + " in " + (FIRST_CAPACITY + i));
	}

	@Test
	void otherItemsAreRefused() {
		for (int slot = 0; slot < upgrades.getSlots(); slot++) {
			assertFalse(upgrades.isItemValid(slot, new ItemStack(Items.WATER_BUCKET)));
			assertFalse(upgrades.isItemValid(slot, ItemStack.EMPTY));
		}
	}

	@Test
	void oneKindOfGenerationUpgradeAtATime() {
		install(be, 0, WellspringUpgrade.WATER);
		assertTrue(upgrades.isItemValid(1, stack(WellspringUpgrade.WATER)));
		assertFalse(upgrades.isItemValid(1, stack(WellspringUpgrade.DRIPSTONE)));
		assertFalse(upgrades.isItemValid(2, stack(WellspringUpgrade.HONEY)));
		// Swapping the only one for another kind is fine: the slot being replaced doesn't count
		assertTrue(upgrades.isItemValid(0, stack(WellspringUpgrade.DRIPSTONE)));
		// Capacity Upgrades don't care
		assertTrue(upgrades.isItemValid(FIRST_CAPACITY, stack(WellspringUpgrade.CAPACITY)));
	}

	@Test
	void generationUpgradeMustMatchTheStoredFluid() {
		assertEquals(1000, fill(be, new FluidStack(Fluids.LAVA, 1000)));
		assertFalse(upgrades.isItemValid(0, stack(WellspringUpgrade.WATER)));
		assertTrue(upgrades.isItemValid(0, stack(WellspringUpgrade.DRIPSTONE)));
		// Capacity is never blocked by the contents
		assertTrue(upgrades.isItemValid(FIRST_CAPACITY, stack(WellspringUpgrade.CAPACITY)));
	}

	@Test
	void lockedSlotsRefuseUpgrades() {
		TestConfig.set(CMConfig.SERVER.maxUpgrades, 1);
		TestConfig.set(CMConfig.SERVER.maxCapacityUpgrades, 1);
		assertTrue(upgrades.isItemValid(0, stack(WellspringUpgrade.WATER)));
		assertFalse(upgrades.isItemValid(1, stack(WellspringUpgrade.WATER)));
		assertTrue(upgrades.isItemValid(FIRST_CAPACITY, stack(WellspringUpgrade.CAPACITY)));
		assertFalse(upgrades.isItemValid(FIRST_CAPACITY + 1, stack(WellspringUpgrade.CAPACITY)));
	}

	@Test
	void lockedSlotsCanStillBeEmptied() {
		install(be, 2, WellspringUpgrade.WATER);
		install(be, FIRST_CAPACITY + 2, WellspringUpgrade.CAPACITY);
		TestConfig.set(CMConfig.SERVER.maxUpgrades, 1);
		TestConfig.set(CMConfig.SERVER.maxCapacityUpgrades, 1);
		assertFalse(upgrades.extractItem(2, 1, false).isEmpty());
		assertFalse(upgrades.extractItem(FIRST_CAPACITY + 2, 1, false).isEmpty());
	}

	// ---------------------------------------------------------------- inserting

	@Test
	void insertOneFillsTheFirstFreeSlotWithOne() {
		ItemStack water = new ItemStack(WellspringUpgrade.WATER.item(), 16);
		assertTrue(upgrades.insertOne(water));
		assertEquals(1, upgrades.getStackInSlot(0).getCount());
		// The caller consumes the item, not insertOne
		assertEquals(16, water.getCount());

		assertTrue(upgrades.insertOne(stack(WellspringUpgrade.CAPACITY)));
		assertSame(WellspringUpgrade.CAPACITY.item(), upgrades.getStackInSlot(FIRST_CAPACITY).getItem());
	}

	@Test
	void insertOneStopsWhenFull() {
		for (int i = 0; i < 3; i++) {
			assertTrue(upgrades.insertOne(stack(WellspringUpgrade.DRIPSTONE)), "dripstone " + i);
			assertTrue(upgrades.insertOne(stack(WellspringUpgrade.CAPACITY)), "capacity " + i);
		}
		assertFalse(upgrades.insertOne(stack(WellspringUpgrade.DRIPSTONE)));
		assertFalse(upgrades.insertOne(stack(WellspringUpgrade.CAPACITY)));
	}

	@Test
	void insertOneRespectsTheLimits() {
		TestConfig.set(CMConfig.SERVER.maxUpgrades, 2);
		TestConfig.set(CMConfig.SERVER.maxCapacityUpgrades, 0);
		assertTrue(upgrades.insertOne(stack(WellspringUpgrade.WATER)));
		assertTrue(upgrades.insertOne(stack(WellspringUpgrade.WATER)));
		assertFalse(upgrades.insertOne(stack(WellspringUpgrade.WATER)));
		assertFalse(upgrades.insertOne(stack(WellspringUpgrade.CAPACITY)));
	}

	@Test
	void insertOneRefusesAMixedKind() {
		assertTrue(upgrades.insertOne(stack(WellspringUpgrade.WATER)));
		assertFalse(upgrades.insertOne(stack(WellspringUpgrade.CHOCOLATE)));
	}

	@Test
	void automationInsertsOneAtATime() {
		ItemStack left = upgrades.insertItem(0, new ItemStack(WellspringUpgrade.WATER.item(), 16), false);
		assertEquals(15, left.getCount());
		assertEquals(1, upgrades.getStackInSlot(0).getCount());
		ItemStack refused = upgrades.insertItem(FIRST_CAPACITY, stack(WellspringUpgrade.WATER), false);
		assertEquals(1, refused.getCount());
	}

	// ---------------------------------------------------------------- removing

	@Test
	void capacityUpgradeStaysWhileTheFluidWouldNotFitWithoutIt() {
		install(be, FIRST_CAPACITY, WellspringUpgrade.CAPACITY);
		be.getTank().setCapacity(CMConfig.SERVER.tankCapacity(1));
		assertEquals(10000, fill(be, new FluidStack(Fluids.WATER, 10000)));

		assertTrue(upgrades.extractItem(FIRST_CAPACITY, 1, true).isEmpty());
		assertTrue(upgrades.extractItem(FIRST_CAPACITY, 1, false).isEmpty());
		assertFalse(upgrades.getStackInSlot(FIRST_CAPACITY).isEmpty(), "upgrade must stay in");

		// Down to exactly the base capacity: it may come out now
		be.getTank().drain(10000 - CMConfig.SERVER.tankCapacity(0), IFluidHandler.FluidAction.EXECUTE);
		assertFalse(upgrades.extractItem(FIRST_CAPACITY, 1, true).isEmpty());
		assertFalse(upgrades.extractItem(FIRST_CAPACITY, 1, false).isEmpty());
		assertTrue(upgrades.getStackInSlot(FIRST_CAPACITY).isEmpty());
	}

	@Test
	void capacityCheckCountsTheOtherUpgrades() {
		install(be, FIRST_CAPACITY, WellspringUpgrade.CAPACITY);
		install(be, FIRST_CAPACITY + 1, WellspringUpgrade.CAPACITY);
		be.getTank().setCapacity(CMConfig.SERVER.tankCapacity(2));
		// 8000 + 16000 = 24000 still fits with one upgrade left
		fill(be, new FluidStack(Fluids.LAVA, 24000));
		assertFalse(upgrades.extractItem(FIRST_CAPACITY + 1, 1, true).isEmpty());
		fill(be, new FluidStack(Fluids.LAVA, 1));
		assertTrue(upgrades.extractItem(FIRST_CAPACITY + 1, 1, true).isEmpty());
	}

	@Test
	void generationUpgradesCanAlwaysComeOut() {
		install(be, 0, WellspringUpgrade.WATER);
		fill(be, new FluidStack(Fluids.WATER, be.getTank().getCapacity()));
		assertFalse(upgrades.extractItem(0, 1, false).isEmpty());
	}

	@Test
	void clearQuietlyEmptiesEverySlot() {
		install(be, 0, WellspringUpgrade.HONEY);
		install(be, FIRST_CAPACITY, WellspringUpgrade.CAPACITY);
		upgrades.clearQuietly();
		for (int slot = 0; slot < upgrades.getSlots(); slot++)
			assertTrue(upgrades.getStackInSlot(slot).isEmpty(), "slot " + slot);
	}
}
