package com.comedymonkey.createaddons.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Only the nearest few of each kind of machine loop play, and a playing one isn't swapped for one just as near. */
class NearestLoopsTest {
	private static Object[] owners(int n) {
		Object[] owners = new Object[n];
		for (int i = 0; i < n; i++)
			owners[i] = new Object();
		return owners;
	}

	@Test
	void theNearestFourPlay() {
		NearestLoops kind = new NearestLoops();
		Object[] tanks = owners(6);
		// offered from far to near: the order offered doesn't matter
		for (int i = 5; i >= 0; i--)
			assertFalse(kind.offer(tanks[i], (i + 1) * (i + 1), false), "nothing plays before the first pick");
		NearestLoops.rank();
		assertEquals(4, NearestLoops.PER_KIND);
		for (int i = 0; i < 6; i++)
			assertEquals(i < 4, kind.allows(tanks[i]), "tank " + i);
		// the next tick's offers answer from that pick
		assertTrue(kind.offer(tanks[0], 1, false));
		assertFalse(kind.offer(tanks[5], 36, false));
	}

	@Test
	void kindsAreCappedApart() {
		NearestLoops pumps = new NearestLoops(), bubbles = new NearestLoops();
		Object[] tanks = owners(4);
		for (int i = 0; i < 4; i++) {
			pumps.offer(tanks[i], i, false);
			bubbles.offer(tanks[i], i, false);
		}
		NearestLoops.rank();
		for (Object tank : tanks)
			assertTrue(pumps.allows(tank) && bubbles.allows(tank));
	}

	/** A playing loop counts as a little nearer, so walking past two at much the same distance doesn't swap them. */
	@Test
	void aPlayingLoopKeepsItsPlaceAgainstOneJustAsNear() {
		NearestLoops kind = new NearestLoops();
		Object playing = new Object();
		Object[] others = owners(4);
		kind.offer(playing, 100, true);
		for (int i = 0; i < 4; i++)
			kind.offer(others[i], 80 + i, false);
		NearestLoops.rank();
		assertTrue(kind.allows(playing), "100 playing beats 83 waiting");
		assertFalse(kind.allows(others[3]));
	}

	/** ...but not against ones clearly nearer. */
	@Test
	void aPlayingLoopGivesWayToClearlyNearerOnes() {
		NearestLoops kind = new NearestLoops();
		Object playing = new Object();
		Object[] others = owners(4);
		kind.offer(playing, 200, true);
		for (int i = 0; i < 4; i++)
			kind.offer(others[i], 100, false);
		NearestLoops.rank();
		assertFalse(kind.allows(playing));
		for (Object other : others)
			assertTrue(kind.allows(other));
	}

	/** A tick without offers (the world not ticking) keeps what plays for a while, then lets the machines go. */
	@Test
	void noOffersKeepsThePickThenForgetsIt() {
		NearestLoops kind = new NearestLoops();
		Object tank = new Object();
		kind.offer(tank, 4, false);
		NearestLoops.rank();
		for (int tick = 0; tick < 100; tick++)
			NearestLoops.rank();
		assertTrue(kind.allows(tank));
		NearestLoops.rank();
		assertFalse(kind.allows(tank));
	}

	@Test
	void machinesThatStopOfferingDropOut() {
		NearestLoops kind = new NearestLoops();
		Object near = new Object(), far = new Object();
		kind.offer(near, 1, false);
		kind.offer(far, 9, false);
		NearestLoops.rank();
		kind.offer(far, 9, true);
		NearestLoops.rank();
		assertFalse(kind.allows(near));
		assertTrue(kind.allows(far));
	}

	/** A block on a ship the client can't place is endlessly far (see HearingTest): it's never among the nearest. */
	@Test
	void anUnplaceableBlockRanksLast() {
		NearestLoops kind = new NearestLoops();
		Object lost = new Object();
		Object[] tanks = owners(4);
		kind.offer(lost, Hearing.distanceSqr(null, net.minecraft.world.phys.Vec3.ZERO), true);
		for (int i = 0; i < 4; i++)
			kind.offer(tanks[i], 10_000 + i, false);
		NearestLoops.rank();
		assertFalse(kind.allows(lost));
	}

	@Test
	void forgetLetsEverythingGo() {
		NearestLoops kind = new NearestLoops();
		Object tank = new Object();
		kind.offer(tank, 1, false);
		NearestLoops.rank();
		NearestLoops.forget();
		assertFalse(kind.allows(tank));
	}
}
