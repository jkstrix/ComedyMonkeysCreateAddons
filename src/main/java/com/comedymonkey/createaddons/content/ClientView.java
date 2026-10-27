package com.comedymonkey.createaddons.content;

import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * What the windowed block entities (the Monster Cages, the Fish Tanks) ask the client about its camera, to keep their
 * display creatures only where they can be seen soon. Set by the client setup; the defaults build everything, and a
 * dedicated server never asks.
 */
public final class ClientView {
	/** Display creatures are built within this many blocks of the camera (they're drawn to 32)... */
	public static final double BUILD_WITHIN = 40;
	/** ...and let go past this, so a big base's windows don't all hold theirs. */
	public static final double CLEAR_BEYOND = 56;
	/** Built per tick in the client's world, all windows together, so arriving at a big base doesn't build hundreds in one tick. */
	public static final int BUILDS_PER_TICK = 16;
	/** This tick's builds; started again each client tick by the client setup. */
	public static final BuildBudget BUILDS = new BuildBudget(BUILDS_PER_TICK);

	/**
	 * Squared distance from the camera to a block entity: 0 where the camera's place doesn't matter (Ponder,
	 * contraptions), endless for a block on a ship the client can't place.
	 */
	public static ToDoubleFunction<BlockEntity> distanceSqr = be -> 0;
	/**
	 * Whether a level's builds are counted: only the client's own world. Ponder (which pauses the game, so its clock
	 * stands still) and other virtual worlds build what they need at once.
	 */
	public static Predicate<Level> counted = level -> false;

	private ClientView() {}

	/** How many of the {@code want} display creatures a window holds {@code distanceSqr} from the camera: none well out of sight. */
	public static int keep(int want, double distanceSqr) {
		return distanceSqr <= CLEAR_BEYOND * CLEAR_BEYOND ? want : 0;
	}

	/** Whether new display creatures are built this far from the camera. */
	public static boolean builds(double distanceSqr) {
		return distanceSqr <= BUILD_WITHIN * BUILD_WITHIN;
	}

	/** Whether another display creature may be built in this level now (and counts it). */
	public static boolean mayBuild(Level level) {
		return !counted.test(level) || BUILDS.take();
	}

	/** So many builds a tick: {@link #take} counts one, {@link #newTick} starts again. */
	public static final class BuildBudget {
		private final int perTick;
		private int built;

		BuildBudget(int perTick) {
			this.perTick = perTick;
		}

		public void newTick() {
			built = 0;
		}

		/** Whether another build fits this tick (and counts it). */
		public boolean take() {
			if (built >= perTick)
				return false;
			built++;
			return true;
		}
	}
}
