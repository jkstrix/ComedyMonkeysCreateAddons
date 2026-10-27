package com.comedymonkey.createaddons.content.cage;

import com.comedymonkey.createaddons.config.CMConfig;

/**
 * The three Monster Cage tiers (D23): how many each holds, how fast monsters arrive (at its reference speed, and times
 * the monster's chow cost for the Andesite and Brass ones), the stress per RPM, and the slots. Flooded is a separate
 * flag on the block (Andesite and Brass only).
 */
public enum CageTier {
	ANDESITE(64, 2, 9),
	BRASS(256, 8, 18),
	NETHERITE(3, 8, 9);

	/** Default capacity, Monster Chow slots and output slots (the slots are fixed; capacity is in the config). */
	private final int defaultCapacity, chowSlots, outputSlots;

	CageTier(int defaultCapacity, int chowSlots, int outputSlots) {
		this.defaultCapacity = defaultCapacity;
		this.chowSlots = chowSlots;
		this.outputSlots = outputSlots;
	}

	public int chowSlots() {
		return chowSlots;
	}

	public int outputSlots() {
		return outputSlots;
	}

	public int capacity() {
		return switch (this) {
			case ANDESITE -> CMConfig.Server.get(CMConfig.SERVER.andesiteCageCapacity);
			case BRASS -> CMConfig.Server.get(CMConfig.SERVER.brassCageCapacity);
			case NETHERITE -> CMConfig.Server.get(CMConfig.SERVER.netheriteCageCapacity);
		};
	}

	/** Stress per RPM while it works (Create multiplies by the speed). */
	public double stressPerRpm() {
		return switch (this) {
			case ANDESITE -> CMConfig.Server.get(CMConfig.SERVER.andesiteCageStress);
			case BRASS -> CMConfig.Server.get(CMConfig.SERVER.brassCageStress);
			case NETHERITE -> CMConfig.Server.get(CMConfig.SERVER.netheriteCageStress);
		};
	}

	/** The speed its intervals are given at: 64 RPM, or 256 for the Withers. */
	public float referenceRpm() {
		return this == NETHERITE ? 256 : 64;
	}

	/** Seconds per new monster at the reference speed, before the chow cost (the Withers' interval stands alone). */
	public int baseSeconds() {
		return switch (this) {
			case ANDESITE -> CMConfig.Server.get(CMConfig.SERVER.andesiteCageSeconds);
			case BRASS -> CMConfig.Server.get(CMConfig.SERVER.brassCageSeconds);
			case NETHERITE -> CMConfig.Server.get(CMConfig.SERVER.witherCageSeconds);
		};
	}

	public int defaultCapacity() {
		return defaultCapacity;
	}
}
