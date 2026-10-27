package com.comedymonkey.createaddons.content;

/**
 * The stress impact a machine gives its network, settled: a rise goes in at once (the load is never under-counted), a
 * fall only once it has lasted {@link #SETTLE_TICKS}, so a machine flickering between hungry and fed doesn't have its
 * whole network worked out again each time (the Monster Cages, the Fish Tanks).
 */
public final class SettlingImpact {
	public static final int SETTLE_TICKS = 20;

	/** NaN until first settled. */
	private float applied = Float.NaN;
	private int fallingFor;

	/** Once a tick on the server, with the impact the machine wants now. */
	public void settle(float wanted) {
		if (Float.isNaN(applied) || wanted >= applied || ++fallingFor >= SETTLE_TICKS) {
			applied = wanted;
			fallingFor = 0;
		}
	}

	/** The impact the network should hold; NaN until it has first settled. */
	public float applied() {
		return applied;
	}
}
