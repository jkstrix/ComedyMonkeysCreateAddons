package com.comedymonkey.createaddons.content.wastes.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The terrace's edge (D91) as numbers, on made-up ground: the ground eases back over the band with no step over 2 where
 * the island's own ground rises or falls by at most 1 a block; the edge wobbles, it isn't a square, the outskirts stand
 * inside it and the ground never reaches past {@link CityEdge#REACH}.
 */
class CityEdgeTest {
	private static final long[] SEEDS = { 0, 1, 2, 7, 42, 12345, -99, 8_675_309 };

	/** Smooth made-up ground, {@code d} above the floor at (x, z): rising and falling by at most 1 a block. */
	private static int ground(long seed, int x, int z, int amp, int base) {
		double a = (seed & 15) * 0.4, b = (seed >> 4 & 15) * 0.3;
		return (int) Math.round(base + amp * Math.sin(x / 13.0 + a) * Math.cos(z / 17.0 + b));
	}

	/** The column's top (from the floor) as the applier builds a plain outskirts column. */
	private static int top(CityEdge e, int x, int z, int d) {
		return CityEdge.top(d, e.across(x, z), e.width(x, z));
	}

	@Test
	void theGroundEasesBackWithNoCliffs() {
		// ground anywhere the levelling takes it: up to 12 above (cut) or 8 below (filled)
		int[][] terrains = { { 0, 0 }, { 6, 0 }, { 6, 6 }, { 4, -4 }, { 4, 8 }, { 2, -6 }, { 2, 10 }, { 8, 3 } };
		for (long seed : SEEDS) {
			CityEdge e = new CityEdge(seed);
			for (int[] t : terrains)
				for (int x = -CityEdge.REACH; x < CityEdge.REACH; x++)
					for (int z = -CityEdge.REACH; z < CityEdge.REACH; z++) {
						if (Math.abs(x) <= CityLayout.CORE + 1 && Math.abs(z) <= CityLayout.CORE + 1)
							continue;
						double a = e.across(x, z);
						if (a <= 0 || a >= 1)
							continue;
						int d = ground(seed, x, z, t[0], t[1]);
						int here = top(e, x, z, d);
						for (int[] n : new int[][] { { x + 1, z }, { x - 1, z }, { x, z + 1 }, { x, z - 1 } }) {
							int dn = ground(seed, n[0], n[1], t[0], t[1]);
							int step = Math.abs(top(e, n[0], n[1], dn) - here);
							assertTrue(step <= 2, "a " + step + "-block step at " + x + ", " + z + " (seed " + seed + ", ground " + d + ")");
						}
					}
		}
	}

	@Test
	void theEdgeWobblesAndIsNoSquare() {
		for (long seed : SEEDS) {
			CityEdge e = new CityEdge(seed);
			int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE, turns = 0, last = 0, lastDir = 0;
			for (int z = -50; z <= 50; z++) {
				// (bounded by the reach: past it there's no ground, levelled or not)
				int x = CityLayout.CORE;
				while (x < CityEdge.REACH && e.across(x + 1, z) <= 0)
					x++;
				assertTrue(x < CityEdge.REACH, "levelled out to the reach at z " + z + " (seed " + seed + ")");
				lo = Math.min(lo, x);
				hi = Math.max(hi, x);
				if (z > -50 && x != last) {
					int dir = Integer.signum(x - last);
					if (lastDir != 0 && dir != lastDir)
						turns++;
					lastDir = dir;
				}
				last = x;
			}
			assertTrue(hi - lo >= 3, "the east edge runs straight (" + lo + "-" + hi + ", seed " + seed + ")");
			assertTrue(turns >= 2, "the east edge only bends " + turns + " times (seed " + seed + ")");
			// rounded: along the diagonal the levelled ground ends well inside where it does on the axes
			int diag = CityLayout.CORE;
			while (diag < CityEdge.REACH && e.across(diag + 1, diag + 1) <= 0)
				diag++;
			assertTrue(diag <= lo - 6, "the corner reaches " + diag + ", the side " + lo + " (seed " + seed + ")");
		}
	}

	@Test
	void theOutskirtsStandInsideAndTheGroundStopsAtTheReach() {
		for (long seed : SEEDS) {
			CityEdge e = new CityEdge(seed);
			int r = CityEdge.REACH + 4;
			for (int x = -r; x <= r; x++)
				for (int z = -r; z <= r; z++) {
					double a = e.across(x, z);
					if (Math.abs(x) > CityEdge.REACH || Math.abs(z) > CityEdge.REACH)
						assertTrue(a >= 1 + CityEdge.FAN, "ground past the reach at " + x + ", " + z);
					// everything the outskirts put (wrecks to 90 out, the siege lines, the war-frames) stands on levelled ground
					double norm = Math.sqrt(Math.sqrt((double) x * x * x * x + (double) z * z * z * z));
					if (norm <= CityEdge.TERRACE - CityEdge.WOBBLE)
						assertTrue(a <= 0, "not levelled at " + x + ", " + z + " (seed " + seed + ")");
				}
		}
	}

	@Test
	void theLevellingEasesAndTheLimitsSoften() {
		assertEquals(1, CityEdge.levelling(-0.5));
		assertEquals(0, CityEdge.levelling(1.2));
		double last = 1;
		for (int i = 1; i < 100; i++) {
			double w = CityEdge.levelling(i / 100.0);
			assertTrue(w < last && last - w <= 1.25 / 100 + 1e-9, "levelling at " + i / 100.0);
			last = w;
		}
		// a hill or dip past the limits: levelled part way, rising straight to the island's own, no step over 1.75 a block
		assertEquals(0, CityEdge.inner(12));
		assertEquals(0, CityEdge.inner(-8));
		assertEquals(12 + CityEdge.SOFT, CityEdge.inner(12 + CityEdge.SOFT));
		assertEquals(-8 - CityEdge.SOFT, CityEdge.inner(-8 - CityEdge.SOFT));
		for (int d = -40; d < 40; d++)
			assertTrue(Math.abs(CityEdge.inner(d + 1) - CityEdge.inner(d)) <= 1.75 + 1e-9, "inner " + d);
	}
}
