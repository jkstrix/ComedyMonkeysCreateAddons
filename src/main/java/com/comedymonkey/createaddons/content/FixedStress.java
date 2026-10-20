package com.comedymonkey.createaddons.content;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

/**
 * Machines that load the network with the same stress at any speed (the Auto Harvester, the Animal Pens). Create
 * counts a block's stress as its impact per RPM times its speed, so the impact is the SU divided by the speed.
 */
public final class FixedStress {
	private FixedStress() {}

	/** Impact per RPM that comes to {@code su} at the block's current speed; none while idle or stopped. */
	public static float impact(KineticBlockEntity be, float su, boolean idle) {
		float speed = Math.abs(be.getTheoreticalSpeed());
		return idle || speed == 0 ? 0 : su / speed;
	}

	/**
	 * The network works out our stress as the impact it was last given times our current speed, and it isn't always
	 * given a new impact when the speed changes (Create attaches a block to a network before or after setting its
	 * speed, depending on how the power came back). So whenever the impact it holds isn't {@code wanted}, it's
	 * corrected; otherwise a machine re-powered at a different speed reads as overstressed. Call every server tick
	 * and on speed changes; true if it changed anything.
	 */
	public static boolean keep(KineticBlockEntity be, float wanted) {
		if (!be.hasNetwork())
			return false;
		var network = be.getOrCreateNetwork();
		Float held = network.members.get(be);
		if (held == null || Math.abs(held - wanted) <= 1e-6f)
			return false;
		network.updateStressFor(be, be.calculateStressApplied());
		return true;
	}
}
