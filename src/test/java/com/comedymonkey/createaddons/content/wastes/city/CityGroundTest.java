package com.comedymonkey.createaddons.content.wastes.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.content.wastes.city.kit.Wear;

import net.minecraft.core.BlockPos;

/**
 * The city's End Stone patches (the user: "the End Stone that is slightly darker should be textured with regular End Stone
 * in places"): patches, not a speckle; few in the heart, more outward, a dip in the war-torn damaged ring; the same for a
 * city every time.
 */
class CityGroundTest {
	private static final long[] SEEDS = { 0, 1, 7, 42, 12345, -99 };
	private static final BlockPos[] ORIGINS = { new BlockPos(8, 70, 8), new BlockPos(2584, 64, 1136), new BlockPos(-48_000, 90, 31_000) };
	private static final int R = 104;

	private static boolean[][] grid(long key) {
		boolean[][] g = new boolean[2 * R + 1][2 * R + 1];
		for (int x = -R; x <= R; x++)
			for (int z = -R; z <= R; z++)
				g[x + R][z + R] = CityGround.unscorched(key, x, z);
		return g;
	}

	@Test
	void theEndStoneLiesInPatchesNotASpeckle() {
		for (long seed : SEEDS)
			for (BlockPos o : ORIGINS) {
				boolean[][] g = grid(CityGround.key(seed, o));
				int cells = 0, held = 0, patches = 0, inPatches = 0;
				boolean[][] seen = new boolean[g.length][g.length];
				for (int x = 1; x < g.length - 1; x++)
					for (int z = 1; z < g.length - 1; z++) {
						if (!g[x][z])
							continue;
						cells++;
						int n = (g[x + 1][z] ? 1 : 0) + (g[x - 1][z] ? 1 : 0) + (g[x][z + 1] ? 1 : 0) + (g[x][z - 1] ? 1 : 0);
						if (n >= 2)
							held++;
						if (seen[x][z])
							continue;
						// the patch it's in
						int size = 0;
						ArrayDeque<int[]> todo = new ArrayDeque<>();
						todo.add(new int[] { x, z });
						seen[x][z] = true;
						while (!todo.isEmpty()) {
							int[] c = todo.poll();
							size++;
							for (int[] d : new int[][] { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } }) {
								int a = c[0] + d[0], b = c[1] + d[1];
								if (a >= 0 && b >= 0 && a < g.length && b < g.length && g[a][b] && !seen[a][b]) {
									seen[a][b] = true;
									todo.add(new int[] { a, b });
								}
							}
						}
						patches++;
						if (size >= 6)
							inPatches += size;
					}
				String at = " (seed " + seed + " at " + o.toShortString() + ")";
				assertTrue(cells > 2000, "only " + cells + " End Stone columns" + at);
				// a speckle this thick would have about a quarter of its cells held by two neighbours
				assertTrue(held > 0.6 * cells, "only " + held + " of " + cells + " End Stone columns sit in a patch" + at);
				assertTrue(inPatches > 0.8 * cells, "only " + inPatches + " of " + cells + " End Stone columns in patches of 6 or more" + at);
				assertTrue(cells / (double) patches > 8, "patches average " + cells / (double) patches + " columns" + at);
			}
	}

	@Test
	void moreEndStoneOutwardFewestInTheHeart() {
		// by ring: the heart, worn, damaged, the outskirts; each city's patches are big, so the rings compare over all of them
		double[] sum = new double[4];
		for (long seed : SEEDS)
			for (BlockPos o : ORIGINS) {
				long key = CityGround.key(seed, o);
				double[] count = new double[4], all = new double[4];
				for (int x = -100; x <= 100; x++)
					for (int z = -100; z <= 100; z++) {
						int ring = Wear.ring(x, z);
						int band = ring <= Wear.INTACT_TO ? 0 : ring <= Wear.WORN_TO ? 1 : ring <= Wear.DAMAGED_TO ? 2 : 3;
						all[band]++;
						if (CityGround.unscorched(key, x, z))
							count[band]++;
					}
				String at = " (seed " + seed + " at " + o.toShortString() + "): " + java.util.Arrays.toString(count) + " of " + java.util.Arrays.toString(all);
				assertTrue(count[0] / all[0] < 0.15, "too much End Stone in the heart" + at);
				assertTrue(count[3] / all[3] > 0.15 && count[3] / all[3] < 0.4, "the outskirts' End Stone is off" + at);
				assertTrue(count[3] / all[3] > count[0] / all[0] && count[3] / all[3] > count[2] / all[2], "no more End Stone outward" + at);
				for (int b = 0; b < 4; b++)
					sum[b] += count[b] / all[b] / (SEEDS.length * ORIGINS.length);
			}
		String mean = " (heart " + sum[0] + ", worn " + sum[1] + ", damaged " + sum[2] + ", outskirts " + sum[3] + ")";
		assertTrue(sum[0] < 0.07, "too much End Stone in the heart" + mean);
		assertTrue(sum[0] < sum[1] && sum[1] < sum[3], "End Stone doesn't grow outward" + mean);
		assertTrue(sum[2] < sum[1] - 0.02, "the war-torn damaged ring has as much End Stone as the worn one" + mean);
		// the wanted share: rising outward, a dip in the damaged ring
		assertTrue(CityGround.share(10) < CityGround.share(34) && CityGround.share(34) < CityGround.share(84) && CityGround.share(84) < CityGround.share(100));
		assertTrue(CityGround.share(58) < (CityGround.share(40) + CityGround.share(76)) / 2, "no dip in the damaged ring");
	}

	@Test
	void theSameForACityEveryTime() {
		BlockPos o = ORIGINS[1];
		long key = CityGround.key(3, o);
		for (int x = -60; x <= 60; x += 7)
			for (int z = -60; z <= 60; z += 5)
				assertEquals(CityGround.unscorched(key, x, z), CityGround.unscorched(CityGround.key(3, o.immutable()), x, z));
		// and another city's lie differently
		long other = CityGround.key(3, o.offset(512, 0, 0));
		int differ = 0;
		for (int x = -60; x <= 60; x++)
			for (int z = -60; z <= 60; z++)
				if (CityGround.unscorched(key, x, z) != CityGround.unscorched(other, x, z))
					differ++;
		assertTrue(differ > 500, "two cities' patches lie alike (" + differ + " columns differ)");
	}
}
