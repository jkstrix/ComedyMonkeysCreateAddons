package com.comedymonkey.createaddons.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The cages' and tanks' stress impact: a rise at once, a fall only once it has lasted 20 ticks. */
class SettlingImpactTest {
	@Test
	void unsettledUntilTheFirstTick() {
		assertTrue(Float.isNaN(new SettlingImpact().applied()));
	}

	@Test
	void theFirstValueAppliesAtOnce() {
		SettlingImpact impact = new SettlingImpact();
		impact.settle(8);
		assertEquals(8, impact.applied());
	}

	@Test
	void aRiseAppliesAtOnce() {
		SettlingImpact impact = new SettlingImpact();
		impact.settle(0);
		impact.settle(8);
		assertEquals(8, impact.applied());
	}

	@Test
	void aFallAppliesOnlyAfterTwentyTicks() {
		SettlingImpact impact = new SettlingImpact();
		impact.settle(8);
		for (int tick = 1; tick < SettlingImpact.SETTLE_TICKS; tick++) {
			impact.settle(0);
			assertEquals(8, impact.applied(), "still the old load after " + tick + " ticks");
		}
		impact.settle(0);
		assertEquals(0, impact.applied());
		assertEquals(20, SettlingImpact.SETTLE_TICKS);
	}

	/** Hungry for a few ticks, fed again: the network is never given the fall. */
	@Test
	void aFlickerNeverFalls() {
		SettlingImpact impact = new SettlingImpact();
		impact.settle(8);
		for (int round = 0; round < 50; round++) {
			for (int i = 0; i < 5; i++)
				impact.settle(0);
			impact.settle(8);
			assertEquals(8, impact.applied());
		}
	}

	@Test
	void aFallCountsFromItsStartAgainAfterARise() {
		SettlingImpact impact = new SettlingImpact();
		impact.settle(8);
		for (int i = 0; i < 15; i++)
			impact.settle(0);
		impact.settle(8);
		for (int i = 0; i < 15; i++)
			impact.settle(0);
		assertEquals(8, impact.applied(), "15 + 15 ticks apart isn't 20 in a row");
	}
}
