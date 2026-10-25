package com.comedymonkey.createaddons.content.wellspring;

import static com.comedymonkey.createaddons.content.wellspring.WellspringTestSupport.REGISTRIES;
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
import com.comedymonkey.createaddons.testutil.ReflectionAccess;
import com.comedymonkey.createaddons.testutil.TestConfig;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** A Fluid Wellspring with no level: generation, capacity, fill readouts and saved/synced data. */
class WellspringBlockEntityTest {
	private static final int CAPACITY_SLOT = WellspringUpgradeInventory.FIRST_CAPACITY_SLOT;

	private WellspringBlockEntity be;

	@BeforeEach
	void setUp() {
		TestConfig.loadDefaults();
		be = WellspringTestSupport.create();
	}

	@AfterEach
	void restore() {
		TestConfig.reset();
	}

	/** Runs server ticks. Callers keep the tank capacity in step with the upgrades, so no sync (which needs a level) is sent. */
	private void ticks(int count) {
		for (int i = 0; i < count; i++)
			be.serverTick();
	}

	/** Lets a tick that queues a sync run without a level: the sync is held back by its cooldown. */
	private void holdBackSync() {
		ReflectionAccess.set(be, "syncCooldown", 1000);
	}

	private int progress() {
		return be.getGenerationProgress();
	}

	// ---------------------------------------------------------------- upgrades

	@Test
	void startsEmptyAtTheBaseCapacity() {
		assertTrue(be.getTank().isEmpty());
		assertEquals(8000, be.getTank().getCapacity());
		assertEquals(0, be.getGenerationUpgradeCount());
		assertEquals(0, be.getCapacityUpgradeCount());
		assertEquals(WellspringSource.NONE, be.getGenerationSource());
		assertEquals(0, be.getGenerationRate());
	}

	@Test
	void countsAndSourceFollowTheSlots() {
		install(be, 0, WellspringUpgrade.DRIPSTONE);
		install(be, 2, WellspringUpgrade.DRIPSTONE);
		install(be, CAPACITY_SLOT + 1, WellspringUpgrade.CAPACITY);
		assertEquals(2, be.getGenerationUpgradeCount());
		assertEquals(1, be.getCapacityUpgradeCount());
		assertEquals(WellspringSource.LAVA, be.getGenerationSource());
	}

	@Test
	void generationRateFollowsTheUpgradeCount() {
		int[] expected = {100, 250, 500};
		for (int i = 0; i < 3; i++) {
			install(be, i, WellspringUpgrade.WATER);
			assertEquals(expected[i], be.getGenerationRate(), (i + 1) + " upgrades");
		}
	}

	// ---------------------------------------------------------------- generation

	@Test
	void waterIsGeneratedAtItsRatePerSecond() {
		install(be, 0, WellspringUpgrade.WATER);
		ticks(20);
		assertEquals(100, be.getTank().getFluidAmount());
		assertTrue(be.getTank().getFluid().is(Fluids.WATER));
	}

	@Test
	void slowRatesCarryTheirRemainderOver() {
		// 10 mB/s is half a millibucket per tick
		install(be, 0, WellspringUpgrade.DRIPSTONE);
		ticks(1);
		assertEquals(0, be.getTank().getFluidAmount());
		assertEquals(10, progress());
		ticks(2);
		assertEquals(1, be.getTank().getFluidAmount());
		assertEquals(10, progress());
		ticks(17);
		assertEquals(10, be.getTank().getFluidAmount());
		assertTrue(be.getTank().getFluid().is(Fluids.LAVA));
	}

	@Test
	void threeUpgradesMakeTheTopRate() {
		for (int i = 0; i < 3; i++)
			install(be, i, WellspringUpgrade.WATER);
		ticks(40);
		assertEquals(1000, be.getTank().getFluidAmount());
	}

	@Test
	void nothingIsGeneratedWithoutUpgrades() {
		ticks(100);
		assertTrue(be.getTank().isEmpty());
	}

	@Test
	void generationStopsAtCapacity() {
		install(be, 0, WellspringUpgrade.WATER);
		fill(be, new FluidStack(Fluids.WATER, 7998));
		ticks(1);
		assertEquals(8000, be.getTank().getFluidAmount());
		ticks(5);
		assertEquals(8000, be.getTank().getFluidAmount());
		assertEquals(0, progress(), "progress is dropped while full");
	}

	@Test
	void rateChangesWithTheConfig() {
		TestConfig.set(CMConfig.SERVER.waterTier1, 40);
		install(be, 0, WellspringUpgrade.WATER);
		ticks(20);
		assertEquals(40, be.getTank().getFluidAmount());
	}

	// ---------------------------------------------------------------- capacity

	@Test
	void capacityFollowsCapacityUpgrades() {
		install(be, CAPACITY_SLOT, WellspringUpgrade.CAPACITY);
		holdBackSync();
		ticks(1);
		assertEquals(24000, be.getTank().getCapacity());
		install(be, CAPACITY_SLOT + 1, WellspringUpgrade.CAPACITY);
		install(be, CAPACITY_SLOT + 2, WellspringUpgrade.CAPACITY);
		ticks(1);
		assertEquals(56000, be.getTank().getCapacity());
	}

	@Test
	void shrinkingCapacityNeverLeavesTheTankOverfull() {
		be.getTank().setCapacity(24000);
		fill(be, new FluidStack(Fluids.WATER, 20000));
		holdBackSync();
		ticks(1);
		assertEquals(8000, be.getTank().getCapacity());
		assertEquals(8000, be.getTank().getFluidAmount());
	}

	// ---------------------------------------------------------------- fluid rules

	@Test
	void takesWaterOrLavaWithoutUpgrades() {
		FluidTank tank = be.getTank();
		assertTrue(tank.isFluidValid(new FluidStack(Fluids.WATER, 1)));
		assertTrue(tank.isFluidValid(new FluidStack(Fluids.LAVA, 1)));
	}

	@Test
	void takesOnlyTheUpgradesFluid() {
		install(be, 0, WellspringUpgrade.DRIPSTONE);
		FluidTank tank = be.getTank();
		assertFalse(tank.isFluidValid(new FluidStack(Fluids.WATER, 1)));
		assertTrue(tank.isFluidValid(new FluidStack(Fluids.LAVA, 1)));
		assertEquals(0, fill(be, new FluidStack(Fluids.WATER, 1000)));
		assertEquals(1000, fill(be, new FluidStack(Fluids.LAVA, 1000)));
	}

	// ---------------------------------------------------------------- readouts

	@Test
	void fillPercent() {
		assertEquals(0, be.getFillPercent());
		fill(be, new FluidStack(Fluids.WATER, 1));
		assertEquals(1, be.getFillPercent(), "never 0 while holding fluid");
		fill(be, new FluidStack(Fluids.WATER, 3999));
		assertEquals(50, be.getFillPercent());
		assertEquals(0.5f, be.getFillFraction(), 1e-6);
		fill(be, new FluidStack(Fluids.WATER, 3999));
		assertEquals(99, be.getFillPercent(), "only 100 when completely full");
		fill(be, new FluidStack(Fluids.WATER, 1));
		assertEquals(100, be.getFillPercent());
	}

	@Test
	void millibucketsAreGroupedInThousands() {
		assertEquals("12,000", WellspringBlockEntity.mb(12000));
		assertEquals("500", WellspringBlockEntity.mb(500));
	}

	// ---------------------------------------------------------------- saving and syncing

	@Test
	void saveAndLoadKeepEverything() {
		install(be, 0, WellspringUpgrade.WATER);
		install(be, CAPACITY_SLOT, WellspringUpgrade.CAPACITY);
		be.getTank().setCapacity(24000);
		fill(be, new FluidStack(Fluids.WATER, 20000));
		be.setGenerationProgress(15);

		CompoundTag tag = be.saveWithFullMetadata(REGISTRIES);
		WellspringBlockEntity loaded = WellspringTestSupport.create();
		loaded.loadWithComponents(tag, REGISTRIES);

		assertEquals(24000, loaded.getTank().getCapacity(), "capacity is restored before the fluid");
		assertEquals(20000, loaded.getTank().getFluidAmount());
		assertTrue(loaded.getTank().getFluid().is(Fluids.WATER));
		assertSame(WellspringUpgrade.WATER.item(), loaded.getUpgrades().getStackInSlot(0).getItem());
		assertSame(WellspringUpgrade.CAPACITY.item(), loaded.getUpgrades().getStackInSlot(CAPACITY_SLOT).getItem());
		assertEquals(15, loaded.getGenerationProgress());
	}

	@Test
	void updateTagSyncsTankAndUpgrades() {
		install(be, 1, WellspringUpgrade.DRIPSTONE);
		fill(be, new FluidStack(Fluids.LAVA, 500));

		WellspringBlockEntity client = WellspringTestSupport.create();
		client.loadWithComponents(be.getUpdateTag(REGISTRIES), REGISTRIES);
		assertEquals(500, client.getTank().getFluidAmount());
		assertTrue(client.getTank().getFluid().is(Fluids.LAVA));
		assertEquals(WellspringSource.LAVA, client.getGenerationSource());
	}

	/** The packets sent while it runs carry the upgrades only after they change; the client keeps what it had. */
	@Test
	void runningSyncsLeaveOutUnchangedUpgrades() {
		install(be, 0, WellspringUpgrade.DRIPSTONE);
		install(be, 1, WellspringUpgrade.DRIPSTONE);
		fill(be, new FluidStack(Fluids.LAVA, 500));
		ownSync();
		CompoundTag first = packet();
		assertTrue(first.contains("UpgradeSlots"));

		WellspringBlockEntity client = WellspringTestSupport.create();
		client.loadWithComponents(first, REGISTRIES);
		be.getTank().drain(200, IFluidHandler.FluidAction.EXECUTE);
		CompoundTag second = packet();
		assertFalse(second.contains("UpgradeSlots"));
		assertFalse(second.contains("Capacity"));
		client.loadWithComponents(second, REGISTRIES);
		assertEquals(300, client.getTank().getFluidAmount());
		assertEquals(2, client.getGenerationUpgradeCount(), "the client keeps the upgrades it was sent");
		assertEquals(be.getTank().getCapacity(), client.getTank().getCapacity());

		install(be, CAPACITY_SLOT, WellspringUpgrade.CAPACITY);
		assertTrue(packet().contains("UpgradeSlots"), "a change to the upgrades goes out");
	}

	/** A refused break puts the block back on a player's screen from a fresh copy: anything but its own sync gets the lot. */
	@Test
	void otherUpdatePacketsCarryTheUpgrades() {
		install(be, 0, WellspringUpgrade.DRIPSTONE);
		ownSync();
		packet();
		ReflectionAccess.set(be, "ownSyncAt", -10L);
		assertTrue(packet().contains("UpgradeSlots"));
	}

	@Test
	void theUpgradesAreResentNowAndThen() {
		install(be, 0, WellspringUpgrade.DRIPSTONE);
		ownSync();
		packet();
		assertFalse(packet().contains("UpgradeSlots"));
		ReflectionAccess.set(be, "upgradesSentAt", -1000L);
		assertTrue(packet().contains("UpgradeSlots"));
	}

	/** As if it had just sent a sync of its own (with no level it's at game time 0). */
	private void ownSync() {
		ReflectionAccess.set(be, "ownSyncAt", 0L);
	}

	private CompoundTag packet() {
		return (CompoundTag) ReflectionAccess.call(be, "updatePacketTag", REGISTRIES);
	}

	@Test
	void updateTagEmptiesTheClientTankWhenDrained() {
		fill(be, new FluidStack(Fluids.WATER, 500));
		WellspringBlockEntity client = WellspringTestSupport.create();
		client.loadWithComponents(be.getUpdateTag(REGISTRIES), REGISTRIES);
		assertEquals(500, client.getTank().getFluidAmount());

		be.getTank().drain(500, IFluidHandler.FluidAction.EXECUTE);
		client.loadWithComponents(be.getUpdateTag(REGISTRIES), REGISTRIES);
		assertTrue(client.getTank().isEmpty(), "a drained tank must not stay full on the client");
	}

	@Test
	void legacyUpgradeListIsSortedIntoSlots() {
		ListTag list = new ListTag();
		for (String name : new String[] {"water", "capacity", "bogus", "water", "water", "water", "capacity"})
			list.add(StringTag.valueOf(name));
		CompoundTag tag = new CompoundTag();
		tag.put("Upgrades", list);

		be.loadWithComponents(tag, REGISTRIES);
		var upgrades = be.getUpgrades();
		for (int slot = 0; slot < 3; slot++)
			assertSame(WellspringUpgrade.WATER.item(), upgrades.getStackInSlot(slot).getItem(), "slot " + slot);
		assertSame(WellspringUpgrade.CAPACITY.item(), upgrades.getStackInSlot(CAPACITY_SLOT).getItem());
		assertSame(WellspringUpgrade.CAPACITY.item(), upgrades.getStackInSlot(CAPACITY_SLOT + 1).getItem());
		assertTrue(upgrades.getStackInSlot(CAPACITY_SLOT + 2).isEmpty());
		assertEquals(3, be.getGenerationUpgradeCount(), "a fourth water upgrade has nowhere to go");
	}

	@Test
	void clearContentEmptiesEverything() {
		install(be, 0, WellspringUpgrade.WATER);
		install(be, CAPACITY_SLOT, WellspringUpgrade.CAPACITY);
		fill(be, new FluidStack(Fluids.WATER, 3000));
		be.setGenerationProgress(7);

		be.clearContent();
		assertTrue(be.getTank().isEmpty());
		assertEquals(0, be.getGenerationUpgradeCount());
		assertEquals(0, be.getCapacityUpgradeCount());
		assertEquals(0, be.getGenerationProgress());
	}
}
