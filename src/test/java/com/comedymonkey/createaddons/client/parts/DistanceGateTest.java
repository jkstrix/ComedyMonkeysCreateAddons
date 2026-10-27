package com.comedymonkey.createaddons.client.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.engine_room.flywheel.api.visual.DistanceUpdateLimiter;

/**
 * The gate for visuals Flywheel's own culling can't place (Create's contraption actors, and visuals embedded in a ship
 * or a contraption): posed every frame up close, at Flywheel's own distance limiter's rate further out, never beyond
 * render distance.
 */
class DistanceGateTest {
	private static final double[] DISTANCES = { 0, 5, 30, 44, 46, 64, 70, 90, 120, 160, 200, 260, 400, 1000 };

	/** The same bands as Flywheel's BandedPrimeLimiter, measured by driving the real one. */
	@Test
	void sameRateAsFlywheelsLimiter() throws ReflectiveOperationException {
		Class<?> limiterClass = Class.forName("dev.engine_room.flywheel.impl.visual.BandedPrimeLimiter");
		for (double d : DISTANCES) {
			double sq = d * d;
			DistanceUpdateLimiter limiter = (DistanceUpdateLimiter) limiterClass.getConstructor().newInstance();
			java.lang.reflect.Method tick = limiterClass.getMethod("tick");
			int first = -1, period = -1;
			for (int frame = 1; frame <= 200 && period < 0; frame++) {
				tick.invoke(limiter);
				if (limiter.shouldUpdate(sq)) {
					if (first < 0)
						first = frame;
					else
						period = frame - first;
				}
			}
			assertEquals(period, DistanceGate.divisor(sq), d + " blocks");
		}
	}

	@Test
	void everyFrameUpCloseLessOftenFarNeverBeyondRenderDistance() {
		double max = 12 * 16 * 12 * 16.0;
		for (double d : DISTANCES) {
			DistanceGate gate = new DistanceGate(7);
			int updates = 0;
			for (int frame = 0; frame < 31 * 29; frame++)
				if (gate.shouldUpdate(d * d, max))
					updates++;
			if (d * d > max)
				assertEquals(0, updates, d + " blocks: beyond render distance");
			else
				assertTrue(Math.abs(updates - 31 * 29 / (double) DistanceGate.divisor(d * d)) <= 1, d + " blocks: " + updates);
		}
		// up close: every single frame
		DistanceGate gate = new DistanceGate(123);
		for (int frame = 0; frame < 100; frame++)
			assertTrue(gate.shouldUpdate(10 * 10, max));
	}

	/** Bots made the same frame (a train's) don't all pose on the same frames when far. */
	@Test
	void seedsStaggerFarUpdates() {
		double far = 150 * 150, max = 1e9;
		int divisor = DistanceGate.divisor(far);
		assertTrue(divisor > 1);
		DistanceGate a = new DistanceGate(0), b = new DistanceGate(1);
		boolean together = true;
		for (int frame = 0; frame < divisor * 3; frame++)
			together &= a.shouldUpdate(far, max) == b.shouldUpdate(far, max);
		assertFalse(together);
	}
}
