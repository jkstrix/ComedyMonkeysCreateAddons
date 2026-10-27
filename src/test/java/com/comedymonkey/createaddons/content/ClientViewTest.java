package com.comedymonkey.createaddons.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import net.minecraft.world.level.Level;

/** Which of a window's display creatures are kept and built, by the camera's distance, and how many are built a tick. */
class ClientViewTest {
	private static double sq(double d) {
		return d * d;
	}

	@Test
	void keptUpToFiftySixBlocks() {
		assertEquals(4, ClientView.keep(4, 0));
		assertEquals(4, ClientView.keep(4, sq(40)));
		assertEquals(4, ClientView.keep(4, sq(56)));
		assertEquals(0, ClientView.keep(4, sq(56.1)));
		assertEquals(0, ClientView.keep(4, sq(200)));
	}

	@Test
	void builtOnlyWithinFortyBlocks() {
		assertTrue(ClientView.builds(0));
		assertTrue(ClientView.builds(sq(32)));
		assertTrue(ClientView.builds(sq(40)));
		assertFalse(ClientView.builds(sq(40.1)));
		assertFalse(ClientView.builds(sq(56)));
	}

	/** A block on a ship the client can't place is endlessly far: nothing built, nothing kept. */
	@Test
	void anUnplaceableBlockIsNeitherBuiltNorKept() {
		assertFalse(ClientView.builds(Double.POSITIVE_INFINITY));
		assertEquals(0, ClientView.keep(4, Double.POSITIVE_INFINITY));
	}

	/** Built before they're drawn (to 32 blocks), and kept a good way past where they're built, so walking the edge doesn't rebuild them. */
	@Test
	void theBandsNest() {
		assertTrue(ClientView.BUILD_WITHIN > 32);
		assertTrue(ClientView.CLEAR_BEYOND > ClientView.BUILD_WITHIN + 8);
	}

	/** Sixteen builds a tick, then none until the next tick starts the count again. */
	@Test
	void aBudgetAllowsSixteenATick() {
		ClientView.BuildBudget budget = new ClientView.BuildBudget(ClientView.BUILDS_PER_TICK);
		for (int i = 0; i < 16; i++)
			assertTrue(budget.take(), "build " + (i + 1));
		assertFalse(budget.take());
		assertFalse(budget.take(), "refusals don't count up past the cap");
		budget.newTick();
		assertTrue(budget.take());
	}

	/**
	 * Only the client's own world is counted. Ponder pauses the game, so its clock stands still: counted, its windows
	 * would never be built again after the first sixteen (a replayed scene's tank stayed empty).
	 */
	@Test
	void onlyTheClientsWorldIsCounted() {
		Predicate<Level> was = ClientView.counted;
		try {
			ClientView.counted = level -> level != null;
			ClientView.BUILDS.newTick();
			// a level that isn't counted (here null, as Ponder's would be) builds as many as it needs, spending nothing
			for (int i = 0; i < 100; i++)
				assertTrue(ClientView.mayBuild(null));
			ClientView.counted = level -> true;
			for (int i = 0; i < 16; i++)
				assertTrue(ClientView.mayBuild(null));
			assertFalse(ClientView.mayBuild(null));
		} finally {
			ClientView.counted = was;
			ClientView.BUILDS.newTick();
		}
	}
}
