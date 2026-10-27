package com.comedymonkey.createaddons.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

/** How far a machine's sound is heard from: a block that can't be placed (on a ship the client doesn't know) is endlessly far. */
class HearingTest {
	@Test
	void anUnplaceableBlockIsEndlesslyFar() {
		assertEquals(Double.POSITIVE_INFINITY, Hearing.distanceSqr(null, Vec3.ZERO));
	}

	@Test
	void aPlacedOneIsMeasured() {
		assertEquals(25, Hearing.distanceSqr(new Vec3(3, 4, 0), Vec3.ZERO), 1e-9);
	}
}
