package com.comedymonkey.createaddons.content.engine;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * When a Magma Engine runs and how its lava goes. Stopped, it starts once the tank holds a second's worth; running, it
 * stops when the tank is empty. So a trickle of lava runs it in spells of a second or more instead of stopping and
 * starting the whole kinetic network every tick or two.
 */
final class EngineBurner {
	/** mB/s accumulated each tick; every 20 is one millibucket burned. */
	private int burnProgress;
	private boolean running;

	boolean isRunning() {
		return running;
	}

	/** One tick: decides whether it runs, and burns. Returns whether it runs. */
	boolean tick(FluidTank tank, int lavaPerSecond) {
		running = running ? !tank.isEmpty() : tank.getFluidAmount() >= reserve(lavaPerSecond);
		if (running) {
			burnProgress += lavaPerSecond;
			int burn = burnProgress / 20;
			if (burn > 0) {
				burnProgress -= burn * 20;
				tank.drain(burn, FluidAction.EXECUTE);
			}
		}
		return running;
	}

	/** Lava it wants in the tank before it starts: a second's worth, and at least a millibucket. */
	static int reserve(int lavaPerSecond) {
		return Math.max(1, lavaPerSecond);
	}

	/** Its lava account; whether it runs is saved by the engine as "Running", which this reads back. */
	void write(CompoundTag tag) {
		tag.putInt("BurnProgress", burnProgress);
	}

	void read(CompoundTag tag) {
		burnProgress = tag.getInt("BurnProgress");
		running = tag.getBoolean("Running");
	}
}
