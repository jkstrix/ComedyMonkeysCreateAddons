package com.comedymonkey.createaddons.content.pen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.util.RandomSource;

/**
 * Where the window's animals start out. Walking ({@code tick}) needs a real entity, which needs a world, so only the
 * starting spot is checked here.
 */
class PenAnimalTest {
	@Test
	void startsOnTheStrawStandingStill() {
		for (long seed = 0; seed < 200; seed++) {
			PenAnimal animal = new PenAnimal(null, RandomSource.create(seed));
			float x = animal.x(0), z = animal.z(0);
			assertTrue(x >= 3.5f && x <= 12.5f, "x " + x);
			assertTrue(z >= 2.5f && z <= 9.5f, "z " + z);
			// Nowhere to come from yet: every partial tick is the same spot
			assertEquals(x, animal.x(1), 1e-6);
			assertEquals(z, animal.z(1), 1e-6);
			assertEquals(animal.yaw(0), animal.yaw(1), 1e-4);
		}
	}

	@Test
	void sameSeedSameSpot() {
		PenAnimal a = new PenAnimal(null, RandomSource.create(42));
		PenAnimal b = new PenAnimal(null, RandomSource.create(42));
		assertEquals(a.x(0), b.x(0));
		assertEquals(a.z(0), b.z(0));
		assertEquals(a.yaw(0), b.yaw(0));
	}
}
