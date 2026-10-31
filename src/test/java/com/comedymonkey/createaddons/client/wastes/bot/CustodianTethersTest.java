package com.comedymonkey.createaddons.client.wastes.bot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.content.wastes.bot.Custodian.Phase;

/**
 * The Custodian's tether beads (D59, D60, D67): they crawl while it's shielded (from 3.2 s of its waking, until 0.3 s into
 * a reset's kneel, never as it dies), and only along a standing pylon's cable, as the arcs run only from standing pylons.
 */
class CustodianTethersTest {
	@Test
	void beadsCrawlOnlyAlongStandingPylonsCables() {
		assertTrue(CustodianTethers.beads(Phase.FIGHT, "idle", 1, true, true), "a standing pylon's cable carries beads in the fight");
		assertFalse(CustodianTethers.beads(Phase.FIGHT, "idle", 1, true, false), "a broken pylon's cable carries none (D67)");
		assertTrue(CustodianTethers.beads(Phase.RETURNING, "walk", 1, true, true), "walking home shielded, the beads still run");
		assertFalse(CustodianTethers.beads(Phase.RETURNING, "walk", 1, true, false), "walking home, still none on a broken pylon's");
	}

	@Test
	void beadsFollowTheShieldAndItsClips() {
		assertFalse(CustodianTethers.beads(Phase.FIGHT, "idle", 1, false, true), "unshielded, no beads");
		assertFalse(CustodianTethers.beads(Phase.DORMANT, "waking", 0, true, true), "asleep, no beads");
		assertFalse(CustodianTethers.beads(Phase.WAKING, "waking", 3.1, true, true), "not before 3.2 s of its waking");
		assertTrue(CustodianTethers.beads(Phase.WAKING, "waking", 3.2, true, true), "from 3.2 s of its waking");
		assertFalse(CustodianTethers.beads(Phase.WAKING, "waking", 3.5, true, false), "waking, none on a broken pylon's");
		assertTrue(CustodianTethers.beads(Phase.KNEELING, "kneel", 0.2, true, true), "the first 0.3 s of a reset's kneel");
		assertFalse(CustodianTethers.beads(Phase.KNEELING, "kneel", 0.3, true, true), "gone at 0.3 s of the kneel");
		assertFalse(CustodianTethers.beads(Phase.DYING, "death", 1, true, true), "never as it dies");
	}
}
