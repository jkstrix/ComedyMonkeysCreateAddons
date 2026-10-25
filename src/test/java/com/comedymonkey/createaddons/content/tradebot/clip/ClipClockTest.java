package com.comedymonkey.createaddons.content.tradebot.clip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Trade Bot's client clip clock against a server: the client counts game time itself and is snapped to the server's
 * time every 20 server ticks. The clock must never jump, and must settle on the server's real pace.
 */
class ClipClockTest {
	/** What the clock did over a run: its smallest and largest step, the biggest change between steps, its late pace. */
	private record Run(double minStep, double maxStep, double maxJerk, double latePace) {}

	/** Runs {@code clientTicks} client ticks against a server making {@code tps} ticks a second. */
	private static Run run(double tps, int clientTicks) {
		Object level = new Object();
		double server = 0, minStep = Double.MAX_VALUE, maxStep = 0, maxJerk = 0, lastStep = Double.NaN;
		long clientTime = 0, lastSync = 0;
		ClipClock.tick(level, 0);
		double before = ClipClock.now(0), lateStart = 0;
		int late = clientTicks - 200;
		for (int i = 0; i < clientTicks; i++) {
			server += tps / 20;
			clientTime++;
			// the server sends its time every 20 of its own ticks
			if ((long) server / 20 > lastSync) {
				lastSync = (long) server / 20;
				clientTime = (long) server;
			}
			ClipClock.tick(level, clientTime);
			double now = ClipClock.now(0), step = now - before;
			before = now;
			minStep = Math.min(minStep, step);
			maxStep = Math.max(maxStep, step);
			if (!Double.isNaN(lastStep))
				maxJerk = Math.max(maxJerk, Math.abs(step - lastStep));
			lastStep = step;
			if (i == late)
				lateStart = now;
		}
		return new Run(minStep, maxStep, maxJerk, (before - lateStart) / (clientTicks - 1 - late));
	}

	@Test
	void keepsRealTimeOnAHealthyServer() {
		Run run = run(20, 600);
		assertEquals(1, run.minStep(), 0.02);
		assertEquals(1, run.maxStep(), 0.02);
	}

	@Test
	void slowsSmoothlyOnALaggingServer() {
		Run run = run(15, 1200);
		// it never stands still, runs backwards or lurches, as the corrected world time does once a second
		assertTrue(run.minStep() >= 0.2, "smallest step " + run.minStep());
		assertTrue(run.maxStep() <= 1.05, "largest step " + run.maxStep());
		assertTrue(run.maxJerk() < 0.1, "biggest change between steps " + run.maxJerk());
		// and settles on the server's pace
		assertEquals(0.75, run.latePace(), 0.05);
	}
}
