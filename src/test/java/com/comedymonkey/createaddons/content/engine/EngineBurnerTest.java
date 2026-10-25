package com.comedymonkey.createaddons.content.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** When a Magma Engine runs: a second's reserve to start, a stop when dry, and no free power. */
class EngineBurnerTest {
	private static FluidTank tank(int amount) {
		FluidTank tank = new FluidTank(8000);
		if (amount > 0)
			tank.fill(new FluidStack(Fluids.LAVA, amount), FluidAction.EXECUTE);
		return tank;
	}

	@Test
	void anEmptyEngineNeverStarts() {
		EngineBurner burner = new EngineBurner();
		FluidTank tank = tank(0);
		for (int t = 0; t < 100; t++)
			assertFalse(burner.tick(tank, 20));
	}

	@Test
	void itStartsOnlyWithASecondsWorth() {
		EngineBurner burner = new EngineBurner();
		assertFalse(burner.tick(tank(19), 20));
		assertTrue(burner.tick(tank(20), 20));
	}

	@Test
	void aFullTankRunsItWithoutStopping() {
		EngineBurner burner = new EngineBurner();
		FluidTank tank = tank(4000);
		for (int t = 0; t < 2000; t++)
			assertTrue(burner.tick(tank, 20), "tick " + t);
		assertEquals(4000 - 2000, tank.getFluidAmount(), "20 mB/s is a millibucket a tick");
	}

	@Test
	void runningDryItStopsAndWaitsForAReserve() {
		EngineBurner burner = new EngineBurner();
		FluidTank tank = tank(20);
		int running = 0;
		while (burner.tick(tank, 20))
			running++;
		assertEquals(20, running, "a millibucket a tick, and not a tick more");
		tank.fill(new FluidStack(Fluids.LAVA, 19), FluidAction.EXECUTE);
		assertFalse(burner.tick(tank, 20), "not a second's worth yet");
		tank.fill(new FluidStack(Fluids.LAVA, 1), FluidAction.EXECUTE);
		assertTrue(burner.tick(tank, 20));
	}

	/**
	 * A Supercharged engine (20 mB/s) fed by one Lava Upgrade's Wellspring (10 mB/s, a millibucket every other tick):
	 * it used to stop and start every tick or two, each time across the whole kinetic network.
	 */
	@Test
	void aStarvedEngineDoesNotFlipMoreThanOnceASecond() {
		EngineBurner burner = new EngineBurner();
		FluidTank tank = tank(0);
		boolean was = false;
		int flips = 0, runningTicks = 0;
		int ticks = 20 * 120, supplied = 0;
		for (int t = 0; t < ticks; t++) {
			if (t % 2 == 0)
				supplied += tank.fill(new FluidStack(Fluids.LAVA, 1), FluidAction.EXECUTE);
			boolean running = burner.tick(tank, 20);
			if (running != was)
				flips++;
			was = running;
			if (running)
				runningTicks++;
		}
		assertTrue(flips <= ticks / 20, flips + " flips in " + ticks / 20 + " seconds");
		assertTrue(flips > 0, "it still runs in spells");
		// No free power: what it ran on is what it was given, give or take its reserve
		int burned = runningTicks;  // a millibucket a tick
		assertTrue(burned <= supplied && supplied - burned <= 20,
			"burned " + burned + " mB of " + supplied + " supplied");
	}

	@Test
	void itsAccountIsSavedAndOldSavesStillLoad() {
		EngineBurner burner = new EngineBurner();
		burner.tick(tank(40), 10);
		CompoundTag tag = new CompoundTag();
		burner.write(tag);
		tag.putBoolean("Running", burner.isRunning());
		EngineBurner loaded = new EngineBurner();
		loaded.read(tag);
		assertTrue(loaded.isRunning());
		assertEquals(10, tag.getInt("BurnProgress"));
		// a save from when it ran on credit
		tag.putInt("Owed", 30);
		new EngineBurner().read(tag);
	}
}
