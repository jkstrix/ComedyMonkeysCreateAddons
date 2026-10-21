package com.comedymonkey.createaddons.content.clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Restock slots at 8:00 and 12:00, the clock face's reading, and the show's timeline. */
class ClockTimeTest {
	private static final int DAY = ClockTime.TICKS_PER_DAY;

	@Test
	void slotsChangeAtEightAndNoon() {
		assertEquals(-1, ClockTime.slot(0), "6:00 is still the night before's noon slot");
		assertEquals(-1, ClockTime.slot(1999));
		assertEquals(0, ClockTime.slot(2000), "8:00 opens the morning slot");
		assertEquals(0, ClockTime.slot(5999));
		assertEquals(1, ClockTime.slot(6000), "noon opens the next");
		assertEquals(1, ClockTime.slot(DAY - 1));
		assertEquals(1, ClockTime.slot(DAY + 1999), "it runs until 8:00 the next day");
		assertEquals(2, ClockTime.slot(DAY + 2000));
		assertEquals(41, ClockTime.slot(20L * DAY + 7000), "day 20's noon");
	}

	@Test
	void slotsOnlyCountUpAsTimePasses() {
		long previous = ClockTime.slot(0);
		for (long t = 0; t < 5L * DAY; t += 250) {
			long slot = ClockTime.slot(t);
			assertTrue(slot == previous || slot == previous + 1, "at " + t);
			previous = slot;
		}
	}

	@Test
	void eightThenTwelveCalls() {
		assertEquals(8, ClockTime.hourOf(ClockTime.slot(2000)));
		assertEquals(12, ClockTime.hourOf(ClockTime.slot(6000)));
		assertEquals(12, ClockTime.hourOf(-1));
		assertEquals(8, ClockTime.hourOf(4));
	}

	@Test
	void timeIntoAndUntilTheSlots() {
		assertEquals(0, ClockTime.intoSlot(2000));
		assertEquals(150, ClockTime.intoSlot(6150));
		assertEquals(DAY - 6000, ClockTime.intoSlot(0), "6:00 is 18 hours after noon");
		assertEquals(2000, ClockTime.untilNextSlot(0));
		assertEquals(4000, ClockTime.untilNextSlot(2000));
		assertEquals(DAY - 6000 + 2000, ClockTime.untilNextSlot(6000));
		assertEquals(1, ClockTime.untilNextSlot(DAY + 1999));
	}

	@Test
	void faceReadsTheGameClock() {
		assertEquals(6, ClockTime.hours(0));
		assertEquals(8, ClockTime.hours(2000));
		assertEquals(12, ClockTime.hours(6000));
		assertEquals(0, ClockTime.hours(18000), "midnight");
		assertEquals(30, ClockTime.minutes(2500));
		assertEquals(8, ClockTime.hours(DAY * 3L + 2000), "any day");
		assertEquals(6, ClockTime.hours(-DAY), "time set back below zero still reads");
	}

	@Test
	void theShowOpensAndClosesInOrder() {
		for (int calls : new int[] {8, 12}) {
			int end = ClockTime.showLength(calls);
			// every door and figure is in at the start and the end
			for (float t : new float[] {0, end}) {
				assertEquals(0, ClockTime.birdDoors(t, calls), 1e-6);
				assertEquals(0, ClockTime.birdOut(t, calls), 1e-6);
				assertEquals(0, ClockTime.villagerDoors(t, calls), 1e-6);
				assertEquals(0, ClockTime.villagerOut(t, calls), 1e-6);
			}
			// the doors are fully open whenever their figure is out
			for (float t = 0; t < end; t += 0.5f) {
				if (ClockTime.birdOut(t, calls) > 0)
					assertEquals(1, ClockTime.birdDoors(t, calls), 1e-6, "bird at " + t);
				if (ClockTime.villagerOut(t, calls) > 0)
					assertEquals(1, ClockTime.villagerDoors(t, calls), 1e-6, "villager at " + t);
			}
			// the bird is out on its perch for every call
			for (int i = 0; i < calls; i++)
				assertEquals(1, ClockTime.birdOut(ClockTime.callTick(i), calls), 1e-6);
			assertEquals(0, ClockTime.birdCall(ClockTime.callTick(calls), calls), 1e-6);
		}
	}

	@Test
	void openingEasesFromZeroToOne() {
		assertEquals(0, ClockTime.opening(5, 10, 4), 1e-6);
		assertEquals(0.5, ClockTime.opening(12, 10, 4), 1e-6);
		assertEquals(1, ClockTime.opening(20, 10, 4), 1e-6);
	}
}
