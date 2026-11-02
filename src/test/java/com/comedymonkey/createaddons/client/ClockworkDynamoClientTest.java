package com.comedymonkey.createaddons.client;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.testutil.TestConfig;

import net.minecraft.core.Direction;

/** The Clockwork Dynamo's client side: its screen's steps, its hum's pitch and its commutator's turn. */
class ClockworkDynamoClientTest {
	@BeforeEach
	void config() {
		TestConfig.loadDefaults();
	}

	@AfterEach
	void restore() {
		TestConfig.reset();
	}

	/** Small steps for water wheels and windmills, then the motor's, up to the most. */
	@Test
	void screenSteps() {
		assertArrayEquals(new int[] {256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 49152, 65536, 98304, 131072, 196608, 294912},
			ClockworkDynamoScreen.stressSteps());
	}

	@Test
	void screenStepsStopAtTheMost() {
		TestConfig.set(CMConfig.SERVER.motorMaxStress, 4096);
		assertArrayEquals(new int[] {256, 512, 1024, 2048, 4096}, ClockworkDynamoScreen.stressSteps());
	}

	/** A setting that isn't one of the steps (the config changed) is kept as an option, so opening the screen changes nothing. */
	@Test
	void screenKeepsAnOddSetting() {
		assertArrayEquals(new int[] {256, 512, 16384, 32768}, ClockworkDynamoScreen.withSetting(new int[] {256, 512, 32768}, 16384));
		assertArrayEquals(new int[] {256, 512}, ClockworkDynamoScreen.withSetting(new int[] {256, 512}, 512));
	}

	/** The hum rises with the setting, from 0.8 at one water wheel to 1.25 flat out. */
	@Test
	void humPitchFollowsTheSetting() {
		assertEquals(0.8f, ElectricSounds.dynamoHumPitch(256), 1e-4);
		assertEquals(1.25f, ElectricSounds.dynamoHumPitch(294912), 1e-4);
		float last = 0;
		for (int stress = 256; stress <= 294912; stress *= 2) {
			float p = ElectricSounds.dynamoHumPitch(stress);
			assertTrue(p > last, "at " + stress);
			last = p;
		}
	}

	/**
	 * The commutator turns about the model's Z, which faces away from the shaft's end: for a shaft pointing the positive
	 * way along its axis that's the world turn reversed, otherwise the same, so it always turns with the shaft.
	 */
	@Test
	void commutatorTurnsWithTheShaft() {
		for (Direction facing : Direction.values()) {
			float turn = ElectricRenderers.Dynamo.commutatorAngle(1, facing);
			assertEquals(facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? -1 : 1, turn, 1e-6, facing.toString());
		}
	}
}
