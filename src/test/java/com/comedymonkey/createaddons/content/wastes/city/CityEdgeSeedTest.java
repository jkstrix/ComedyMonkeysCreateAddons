package com.comedymonkey.createaddons.content.wastes.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The edge's scree rolls (D91) follow the plan's seed: the same seed lays the same, another lays it elsewhere. */
class CityEdgeSeedTest {
	/** Which of a 64 x 64 patch's columns roll under {@code chance}. */
	private static boolean[] rolls(long seed, double chance) {
		boolean[] out = new boolean[64 * 64];
		for (int x = 0; x < 64; x++)
			for (int z = 0; z < 64; z++)
				out[x * 64 + z] = CityEdge.roll(seed, 1000 + x, -2000 + z, 1, chance);
		return out;
	}

	private static int differ(boolean[] a, boolean[] b) {
		int n = 0;
		for (int i = 0; i < a.length; i++)
			if (a[i] != b[i])
				n++;
		return n;
	}

	@Test
	void theSeedMovesTheScree() {
		long[] seeds = { 0, 1, 42, -7, 0x1234_5678_9ABCL, 1L << 40 };
		for (long a : seeds) {
			assertEquals(0, differ(rolls(a, 0.3), rolls(a, 0.3)), "seed " + a + " rolls differently twice");
			for (long b : seeds)
				if (a != b) {
					int n = differ(rolls(a, 0.3), rolls(b, 0.3));
					// two independent 30% patterns differ on about 42% of columns
					assertTrue(n > 64 * 64 / 4, "seeds " + a + " and " + b + " lay nearly the same scree (" + n + " columns differ)");
				}
		}
	}

	@Test
	void theChanceStillHolds() {
		for (long seed : new long[] { 0, 99, -123_456_789L }) {
			int hits = 0;
			for (boolean r : rolls(seed, 0.3))
				if (r)
					hits++;
			assertTrue(Math.abs(hits / 4096.0 - 0.3) < 0.03, "seed " + seed + " rolls " + hits + " of 4096 at 30%");
		}
	}
}
