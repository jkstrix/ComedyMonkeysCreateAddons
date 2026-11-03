package com.comedymonkey.createaddons.content.wastes.city;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.wastes.city.kit.Wear;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The city's bare ground: Scorched End Stone, and patches of plain End Stone where the scorching never reached, a stray or
 * two off their edges. Fewest in the heart and the war-torn damaged ring, more outward to the terrace's edge. Patch noise
 * from the city's seed and middle only, so every chunk agrees whatever order they come in.
 */
public final class CityGround {
	private CityGround() {}

	/** The noise's key for a city: its seed and its middle. */
	public static long key(long seed, BlockPos origin) {
		return mix(seed * 0x9E3779B97F4A7C15L ^ origin.asLong());
	}

	// ---------------------------------------------------------------- the noise

	private static long mix(long h) {
		h ^= h >>> 33;
		h *= 0xFF51AFD7ED558CCDL;
		h ^= h >>> 33;
		h *= 0xC4CEB9FE1A85EC53L;
		return h ^ h >>> 33;
	}

	private static double lattice(long key, int x, int z) {
		return (mix(key ^ x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL) >>> 11) * 0x1.0p-53;
	}

	/** Smoothed value noise, 0-1. */
	private static double value(long key, double x, double z) {
		int x0 = Mth.floor(x), z0 = Mth.floor(z);
		double fx = x - x0, fz = z - z0;
		fx = fx * fx * (3 - 2 * fx);
		fz = fz * fz * (3 - 2 * fz);
		double a = Mth.lerp(fx, lattice(key, x0, z0), lattice(key, x0 + 1, z0));
		double b = Mth.lerp(fx, lattice(key, x0, z0 + 1), lattice(key, x0 + 1, z0 + 1));
		return Mth.lerp(fz, a, b);
	}

	/** Patch noise at (dx, dz): blobs about 9 across, their edges ragged. */
	static double patch(long key, int dx, int dz) {
		return 0.7 * value(key, dx / 9.0, dz / 9.0) + 0.3 * value(key + 1, dx / 3.0, dz / 3.0);
	}

	/** The patch noise's quantiles, so a share comes out as that share of the ground. */
	private static final double[] QUANTILES = new double[201];

	static {
		double[] s = new double[256 * 256];
		for (int x = 0; x < 256; x++)
			for (int z = 0; z < 256; z++)
				s[x * 256 + z] = patch(0x5EEDL, x - 128, z - 128);
		Arrays.sort(s);
		for (int i = 0; i <= 200; i++)
			QUANTILES[i] = s[Math.min(s.length - 1, i * (s.length - 1) / 200)];
	}

	/** The patch noise at (dx, dz) as a rank, 0-1, evenly spread: under {@code share} on that share of the ground. */
	public static double rank(long key, int dx, int dz) {
		int i = Arrays.binarySearch(QUANTILES, patch(key, dx, dz));
		return Mth.clamp((i < 0 ? -i - 1 : i) / 200.0, 0, 1);
	}

	/** A column's own draw, 0-1. */
	static double roll(long key, int dx, int dz, int salt) {
		return lattice(key + salt * 0x632BE59BD9B4E019L, dx, dz);
	}

	// ---------------------------------------------------------------- the ground

	/** The share of plain End Stone by ring: hardly any in the heart, some in the worn ring, less in the war-torn damaged ring, most out to the edge. */
	private static final int[] RINGS = { 0, Wear.INTACT_TO, 34, Wear.WORN_TO, 58, Wear.DAMAGED_TO, 84, 100 };
	private static final double[] SHARES = { 0.02, 0.03, 0.13, 0.12, 0.05, 0.10, 0.22, 0.32 };

	/** The share of plain End Stone wanted at {@code ring} from the middle. */
	public static double share(int ring) {
		if (ring >= RINGS[RINGS.length - 1])
			return SHARES[SHARES.length - 1];
		int i = 0;
		while (RINGS[i + 1] < ring)
			i++;
		return Mth.lerp((ring - RINGS[i]) / (double) (RINGS[i + 1] - RINGS[i]), SHARES[i], SHARES[i + 1]);
	}

	/** Whether the scorching missed (dx, dz): in a patch of End Stone, or a stray just off one. */
	public static boolean unscorched(long key, int dx, int dz) {
		return unscorched(key, dx, dz, share(Wear.ring(dx, dz)));
	}

	/** As {@link #unscorched(long, int, int)}, for this share of End Stone. */
	public static boolean unscorched(long key, int dx, int dz, double share) {
		double r = rank(key, dx, dz), t = 1 - share;
		return r >= t || r >= t - 0.04 && roll(key, dx, dz, 1) < 0.25;
	}

	/**
	 * Across the terrace's edge band, the city's ground ({@link #top}) where it still reaches, else null (the island's own):
	 * it thins out in patches as {@code w} goes from 1 inside to 0 out, not a speckle.
	 */
	@Nullable
	public static BlockState across(long key, int dx, int dz, double w) {
		return rank(key + 2, dx, dz) < w ? top(key, dx, dz) : null;
	}

	/** The levelled ground's top at (dx, dz): End Stone where the scorching missed, else the scorched ground. */
	public static BlockState top(long key, int dx, int dz) {
		return unscorched(key, dx, dz) ? Blocks.END_STONE.defaultBlockState() : CityLayout.scorched(dx, dz);
	}
}
