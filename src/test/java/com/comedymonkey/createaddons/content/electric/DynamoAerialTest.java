package com.comedymonkey.createaddons.content.electric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The Clockwork Dynamo's aerial: the approved clips' curves, and nothing replaying when it comes into view. */
class DynamoAerialTest {
	private static final double SECOND = 20;

	@Test
	void comesIntoViewAsItStands() {
		DynamoAerial running = new DynamoAerial();
		running.update(true, 256, 100);
		assertEquals(0, running.pitch(100.5, false), 1e-6);
		DynamoAerial idle = new DynamoAerial();
		idle.update(false, 256, 100);
		assertEquals(DynamoAerial.FOLD, idle.pitch(100.5, false), 1e-6);
	}

	/** Starting: it springs up past upright towards the front, then settles upright (start clip). */
	@Test
	void springsUpWithAnOvershoot() {
		DynamoAerial a = new DynamoAerial();
		a.update(false, 256, 0);
		a.update(true, 256, 10);
		assertEquals(DynamoAerial.FOLD, a.pitch(10, false), 1e-6);
		float lowest = Float.MAX_VALUE;
		for (double t = 10; t < 10 + SECOND; t += 0.25)
			lowest = Math.min(lowest, a.pitch(t, false));
		assertTrue(lowest < -10 && lowest > -25, "overshoot " + lowest);
		assertEquals(0, a.pitch(10 + 1.2 * SECOND, false), 1.5);
		assertEquals(0, a.pitch(10 + 4 * SECOND, false), 1e-6);
	}

	/** Stopping: it sags back to folded with a small bounce, and never lies further back than a few degrees past it. */
	@Test
	void foldsBackWithASmallBounce() {
		DynamoAerial a = new DynamoAerial();
		a.update(true, 256, 0);
		a.update(false, 256, 10);
		float most = -Float.MAX_VALUE;
		for (double t = 10; t < 10 + 3 * SECOND; t += 0.25)
			most = Math.max(most, a.pitch(t, false));
		assertTrue(most > DynamoAerial.FOLD && most < DynamoAerial.FOLD + 10, "bounce to " + most);
		assertEquals(DynamoAerial.FOLD, a.pitch(10 + 2 * SECOND, false), 2);
	}

	/** Stopping half way up folds from where it was, not from upright. */
	@Test
	void foldsFromWhereItIs() {
		DynamoAerial a = new DynamoAerial();
		a.update(false, 256, 0);
		a.update(true, 256, 10);
		float mid = a.pitch(12, false);
		a.update(false, 256, 12);
		assertEquals(mid, a.pitch(12, false), 1e-4);
	}

	/** A new setting while working: a wobble with a sideways lean, which dies away. */
	@Test
	void wobblesAtANewSetting() {
		DynamoAerial a = new DynamoAerial();
		a.update(true, 256, 0);
		a.update(true, 4096, 10);
		float swing = 0, lean = 0;
		for (double t = 10; t < 10 + SECOND; t += 0.25) {
			swing = Math.max(swing, Math.abs(a.pitch(t, false)));
			lean = Math.max(lean, Math.abs(a.roll(t)));
		}
		assertTrue(swing > 5 && swing <= 12, "swing " + swing);
		assertTrue(lean > 1 && lean <= 5, "lean " + lean);
		assertEquals(0, a.pitch(10 + 4 * SECOND, false), 1e-6);
		assertEquals(0, a.roll(10 + 4 * SECOND), 1e-6);
	}

	/** Changing the setting while it's idle doesn't wobble a folded aerial. */
	@Test
	void noWobbleWhileIdle() {
		DynamoAerial a = new DynamoAerial();
		a.update(false, 256, 0);
		a.update(false, 4096, 10);
		assertEquals(DynamoAerial.FOLD, a.pitch(15, false), 1e-6);
		assertEquals(0, a.roll(15), 1e-6);
	}

	/** Until it has ticked (first frame in view, or on a contraption) it shows what its block shows. */
	@Test
	void followsTheBlockUntilItTicks() {
		DynamoAerial a = new DynamoAerial();
		assertEquals(0, a.pitch(0, true), 1e-6);
		assertEquals(DynamoAerial.FOLD, a.pitch(0, false), 1e-6);
	}

	/** A new setting just after starting wobbles on from where it is, without a jump. */
	@Test
	void wobbleCarriesOnFromWhereItIs() {
		DynamoAerial a = new DynamoAerial();
		a.update(false, 256, 0);
		a.update(true, 256, 10);
		float before = a.pitch(15, false);
		a.update(true, 4096, 15);
		assertEquals(before, a.pitch(15, false), 1e-3);
	}

	/** Stopping in the middle of a wobble lets its lean settle out instead of cutting it. */
	@Test
	void leanSettlesOutWhenItStops() {
		DynamoAerial a = new DynamoAerial();
		a.update(true, 256, 0);
		a.update(true, 4096, 10);
		float lean = a.roll(12);
		a.update(false, 4096, 12);
		assertEquals(lean, a.roll(12), 1e-3);
		assertEquals(0, a.roll(12 + 4 * SECOND), 1e-6);
	}
}
