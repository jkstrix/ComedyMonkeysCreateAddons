package com.comedymonkey.createaddons.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.content.harvester.AutoHarvesterBlockEntity;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.testutil.TestConfig;

import net.minecraft.core.BlockPos;

/** Impact per RPM that keeps a machine's load the same at any speed (any kinetic block entity will do). */
class FixedStressTest {
	private AutoHarvesterBlockEntity be;

	@BeforeEach
	void setUp() {
		TestConfig.loadDefaults();
		be = new AutoHarvesterBlockEntity(BlockPos.ZERO, ModBlocks.AUTO_HARVESTER.get().defaultBlockState());
	}

	@Test
	void impactTimesSpeedIsTheWantedLoad() {
		for (float speed : new float[] {1, 8, 32, 64, 128, 256}) {
			be.setSpeed(speed);
			assertEquals(1024f, FixedStress.impact(be, 1024, false) * speed, 1e-3f, "at " + speed + " RPM");
		}
	}

	@Test
	void directionDoesNotMatter() {
		be.setSpeed(-64);
		assertEquals(16f, FixedStress.impact(be, 1024, false), 1e-6f);
		be.setSpeed(64);
		assertEquals(16f, FixedStress.impact(be, 1024, false), 1e-6f);
	}

	@Test
	void noLoadWhileIdle() {
		be.setSpeed(64);
		assertEquals(0f, FixedStress.impact(be, 1024, true));
	}

	@Test
	void noLoadWhileStopped() {
		be.setSpeed(0);
		assertEquals(0f, FixedStress.impact(be, 1024, false));
	}

	@Test
	void noLoadAsked() {
		be.setSpeed(64);
		assertEquals(0f, FixedStress.impact(be, 0, false));
	}

	@Test
	void keepDoesNothingOffANetwork() {
		be.setSpeed(64);
		assertFalse(FixedStress.keep(be, 8));
	}
}
