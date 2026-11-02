package com.comedymonkey.createaddons.content.wastes.city;

import com.comedymonkey.createaddons.content.wastes.city.kit.Masonry;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * Where a city's levelled ground meets the island (D91: "slopes plus scree"). The terrace is a rounded square (a
 * superellipse) whose edge noise pushes in and out; past it the ground eases from the floor back to the island's own over
 * a band 6-10 blocks wide (noise again), and in the outskirts a hill or a dip a little past the levelling's limits is
 * levelled part way rather than left as a cliff. Scree lies where ground was cut or filled: loose stone on the cut,
 * rubble down a filled slope and a fan of it on the ground below. All of it from the plan's seed and each column's own
 * ground, so a chunk builds the same whichever order the chunks come in.
 */
public final class CityEdge {
	/** The terrace's edge (its superellipse norm, before the wobble): the outskirts' wrecks and siege lines stand inside it. */
	public static final int TERRACE = 100;
	/** Noise moves the edge in or out by up to this. */
	public static final double WOBBLE = 4;
	/** The band the ground eases back over, its width by noise. */
	public static final int BAND_MIN = 6, BAND_MAX = 10;
	/** Debris fans this share of the band's width past its outer edge, below filled ground. */
	public static final double FAN = 0.4;
	/** The furthest column (each way from the middle) the city's ground reaches. */
	public static final int REACH = TERRACE + (int) Math.ceil(WOBBLE) + (int) Math.ceil(BAND_MAX * (1 + FAN));
	/** Outskirts ground up to this far past the levelling's limits is levelled part way, less the further past. */
	public static final int SOFT = 16;
	/** Past the edge the ground climbs or falls from the floor no faster than this a block (blocks). */
	public static final double SLOPE = 1.25;

	private static final int SALT = 9101;

	private final SimplexNoise wobble, band;

	public CityEdge(CityPlan p) {
		this(p.random(SALT).nextLong());
	}

	public CityEdge(long seed) {
		RandomSource r = RandomSource.create(seed);
		wobble = new SimplexNoise(r);
		band = new SimplexNoise(r);
	}

	/**
	 * Noise round the city, by bearing only ({@code turns}: how often it changes going round), so it moves the edge along its
	 * normal and never steepens the climb out across it.
	 */
	private static double round(SimplexNoise noise, int dx, int dz, double turns, double offset) {
		double r = Math.sqrt((double) dx * dx + (double) dz * dz);
		double c = r == 0 ? 1 : dx / r, s = r == 0 ? 0 : dz / r;
		return Math.clamp(noise.getValue(c * turns + offset, s * turns - offset), -1, 1);
	}

	/** The band's width at (dx, dz) from the middle. */
	public double width(int dx, int dz) {
		return (BAND_MIN + BAND_MAX) / 2.0 + (BAND_MAX - BAND_MIN) / 2.0 * round(band, dx, dz, 1.5, 0);
	}

	/** Where (dx, dz) lies across the edge: up to 0 levelled, 0-1 the band, past 1 the island's own (the fan to 1 + FAN). */
	public double across(int dx, int dz) {
		double x2 = (double) dx * dx, z2 = (double) dz * dz;
		double norm = Math.sqrt(Math.sqrt(x2 * x2 + z2 * z2));
		double n = 0.7 * round(wobble, dx, dz, 2.5, 0) + 0.3 * round(wobble, dx, dz, 7, 40);
		return (norm + WOBBLE * n - TERRACE) / width(dx, dz);
	}

	/** The share of the levelling a column gets for where it lies across the edge: all inside, easing to none over the band. */
	public static double levelling(double across) {
		if (across <= 0)
			return 1;
		if (across >= 1)
			return 0;
		// half straight, half smoothstep: eased at both ends, never steeper than 1.25 / width
		return 1 - (across + across * across * (3 - 2 * across)) / 2;
	}

	/**
	 * Inside the terrace, an outskirts column's top (from the floor) for its ground {@code d} above it: the floor within the
	 * levelling's limits, the island's own {@link #SOFT} past them, rising straight from one to the other between (a cut
	 * hill's flank or a filled dip's rim as a slope, not a wall).
	 */
	public static double inner(int d) {
		int limit = d > 0 ? CityApplier.OUTSKIRT_ABOVE : CityApplier.OUTSKIRT_BELOW, past = Math.abs(d) - limit;
		if (past <= 0)
			return 0;
		return past >= SOFT ? d : Math.signum(d) * past * (limit + SOFT) / (double) SOFT;
	}

	/**
	 * A plain outskirts column's new top, from the floor, for its ground {@code d} above it: as {@link #inner} inside the
	 * terrace, easing to the island's own over the band; and past the edge never further from that than a {@link #SLOPE}
	 * talus from the edge allows, so a hill or a deep dip at the edge takes the band and a little more rather than a cliff.
	 */
	public static int top(int d, double across, double width) {
		double inner = inner(d), rest = (d - inner) * (1 - levelling(across));
		if (across > 0)
			rest = Math.clamp(rest, -SLOPE * across * width, SLOPE * across * width);
		return (int) Math.round(inner + rest);
	}

	/** Below filled ground, the chance of debris {@code past} (0-1) of the way across the fan. */
	public static double fan(double past, int below) {
		// (none on ground far below: rubble doesn't carry down a cliff into a valley)
		return past >= 1 || below <= 0 || below > CityApplier.OUTSKIRT_BELOW + SOFT ? 0 : 0.35 * (1 - past) * Math.min(1, below / 6.0);
	}

	/**
	 * The chance of scree on a part-levelled column whose ground was {@code d} from the floor: more the more ground the
	 * levelling moved there, lying toward a cut's foot (in toward the terrace) or down a fill (out), so it lies in patches
	 * across the band, not along one contour, and hardly at all where the island was nearly level already.
	 */
	public static double scree(int d, double across) {
		double moved = Math.min(1, Math.abs(d - inner(d)) / 6.0);
		double lie = across <= 0 ? 0.5 : d > 0 ? 1 - 0.7 * Math.min(1, across) : 0.3 + 0.7 * Math.min(1, across);
		return moved * (0.1 + 0.3 * lie);
	}

	/** The chance of a loose stone lying on such a column. */
	public static double loose(int d, double across) {
		return 0.4 * scree(d, across);
	}

	/** The column's hash for {@code salt}, mixed with the plan's seed so each world's scree lies differently. */
	private static int hash(long seed, int x, int z, int salt) {
		return CityLayout.hash(x, z, salt ^ (int) seed ^ (int) (seed >>> 32) * 0x2F0B3A49);
	}

	/** Whether the column's hash (one draw a {@code salt}) falls under {@code chance}. */
	public static boolean roll(long seed, int x, int z, int salt, double chance) {
		return hash(seed, x, z, SALT + salt) % 1000 < chance * 1000;
	}

	/** The scree: the city's scorched ground and its rusted and littered stone, End Stone, stained brick rubble, the makers' rubble. */
	public static BlockState screeBlock(long seed, int x, int z) {
		int h = hash(seed, x, z, SALT + 50) % 20;
		if (h < 6)
			return ModWastes.SCORCHED_END_STONE.get().defaultBlockState();
		if (h < 9)
			return ModWastes.RUSTED_SCORCHED_END_STONE.get().defaultBlockState();
		if (h < 12)
			return ModWastes.LITTERED_SCORCHED_END_STONE.get().defaultBlockState();
		if (h < 15)
			return Blocks.END_STONE.defaultBlockState();
		if (h < 18)
			return ModWastes.CRACKED_STAINED_END_STONE_BRICKS.get().defaultBlockState();
		return Masonry.rubble();
	}

	/** A loose stone lying on the scree: a broken stained brick, or the makers' rubble. */
	public static BlockState looseBlock(long seed, int x, int z) {
		return hash(seed, x, z, SALT + 60) % 10 < 7 ? ModWastes.STAINED_END_STONE_BRICK_SLAB.get().defaultBlockState()
			: Blocks.COBBLED_DEEPSLATE_SLAB.defaultBlockState();
	}
}
