package com.comedymonkey.createaddons.content.tradebot.clip;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

class ClipRootTest {
	private static final ClipLibrary LIB = ClipLibrary.get();

	@Test
	void hoppingOnABlockLandsHimOnTop() {
		Clip hop = LIB.clip("hop_on_block");
		Vector3f end = ClipRoot.travel(hop, hop.length(), new Vector3f());
		assertEquals(1.0, end.y, 1e-3, "one block up");
		assertEquals(18 / 16f, end.z, 1e-3, "18 px forward");
		assertEquals(0, end.x, 1e-3);
	}

	@Test
	void hoppingOffGoesTheWayHeFacesThoughTheClipStartsTurnedRound() {
		Clip hop = LIB.clip("hop_off_block");
		Vector3f end = ClipRoot.travel(hop, hop.length(), new Vector3f());
		assertEquals(-1.0, end.y, 1e-3, "one block down");
		assertEquals(24.5f / 16, end.z, 1e-3, "forward, not back toward the preview camera");
		assertEquals(0, ClipRoot.turned(hop, hop.length()), 1e-3, "no turn of his own");
	}

	@Test
	void turnsTurnTheRightWay() {
		assertEquals(90, ClipRoot.turned(LIB.clip("turn_left"), 3), 0.5);
		assertEquals(-90, ClipRoot.turned(LIB.clip("turn_right"), 3), 0.5);
		assertEquals(180, ClipRoot.turned(LIB.clip("turn_around"), 3), 0.5);
	}

	@Test
	void forwardAndRightFollowHisFacing() {
		Vector3f oneForward = new Vector3f(0, 0, 1), oneRight = new Vector3f(1, 0, 0);
		// facing south (yaw 0): forward is +Z, his right is west (-X)
		assertVec(new Vec3(0, 0, 1), ClipRoot.place(Vec3.ZERO, 0, oneForward));
		assertVec(new Vec3(-1, 0, 0), ClipRoot.place(Vec3.ZERO, 0, oneRight));
		// facing west (yaw 90): forward is -X, his right is north (-Z)
		assertVec(new Vec3(-1, 0, 0), ClipRoot.place(Vec3.ZERO, 90, oneForward));
		assertVec(new Vec3(0, 0, -1), ClipRoot.place(Vec3.ZERO, 90, oneRight));
		// a left turn from south faces him east, which is yaw -90
		assertEquals(-90, ClipRoot.facing(0, 90), 1e-6);
	}

	private static void assertVec(Vec3 expected, Vec3 actual) {
		assertEquals(0, expected.distanceTo(actual), 1e-5, "expected " + expected + " but was " + actual);
	}
}
