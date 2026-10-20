package com.comedymonkey.createaddons.content.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.testutil.TestConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/** The Magma Engines' numbers from the config, and their shaft. */
class MagmaEngineBlockTest {
	@BeforeEach
	void config() {
		TestConfig.loadDefaults();
	}

	@AfterEach
	void restore() {
		TestConfig.reset();
	}

	private static MagmaEngineBlock plain() {
		return ModBlocks.MAGMA_ENGINE.get();
	}

	private static MagmaEngineBlock supercharged() {
		return ModBlocks.SUPERCHARGED_MAGMA_ENGINE.get();
	}

	@Test
	void knowsWhichOneItIs() {
		assertFalse(plain().isSupercharged());
		assertTrue(supercharged().isSupercharged());
	}

	@Test
	void defaultNumbers() {
		assertEquals(64, plain().speed());
		assertEquals(128.0, plain().capacityPerRpm(), 1e-9);
		assertEquals(10, plain().lavaPerSecond());
		assertEquals(4000, plain().tankCapacity());

		assertEquals(128, supercharged().speed());
		assertEquals(128.0, supercharged().capacityPerRpm(), 1e-9);
		assertEquals(20, supercharged().lavaPerSecond());
		assertEquals(8000, supercharged().tankCapacity());
	}

	@Test
	void defaultsGiveTheAdvertisedStress() {
		// 64 RPM x 128 = 8,192 SU; 128 RPM x 128 = 16,384 SU
		assertEquals(8192.0, plain().speed() * plain().capacityPerRpm(), 1e-9);
		assertEquals(16384.0, supercharged().speed() * supercharged().capacityPerRpm(), 1e-9);
	}

	@Test
	void eachReadsItsOwnConfig() {
		TestConfig.set(CMConfig.SERVER.magmaEngineSpeed, 32);
		TestConfig.set(CMConfig.SERVER.magmaEngineCapacity, 64.0);
		TestConfig.set(CMConfig.SERVER.magmaEngineLavaPerSecond, 5);
		TestConfig.set(CMConfig.SERVER.superchargedMagmaEngineSpeed, 256);
		TestConfig.set(CMConfig.SERVER.superchargedMagmaEngineCapacity, 512.0);
		TestConfig.set(CMConfig.SERVER.superchargedMagmaEngineLavaPerSecond, 100);

		assertEquals(32, plain().speed());
		assertEquals(64.0, plain().capacityPerRpm(), 1e-9);
		assertEquals(5, plain().lavaPerSecond());
		assertEquals(256, supercharged().speed());
		assertEquals(512.0, supercharged().capacityPerRpm(), 1e-9);
		assertEquals(100, supercharged().lavaPerSecond());
	}

	@Test
	void hasAShaftOnlyOutOfItsFront() {
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			BlockState state = plain().defaultBlockState().setValue(MagmaEngineBlock.FACING, facing);
			for (Direction face : Direction.values())
				assertEquals(face == facing, plain().hasShaftTowards(null, BlockPos.ZERO, state, face),
					"facing " + facing + ", face " + face);
			assertEquals(facing.getAxis(), plain().getRotationAxis(state));
		}
	}

	@Test
	void startsUnlit() {
		assertFalse(plain().defaultBlockState().getValue(MagmaEngineBlock.LIT));
		assertFalse(supercharged().defaultBlockState().getValue(MagmaEngineBlock.LIT));
	}

	@Test
	void rotatesAndMirrors() {
		BlockState north = plain().defaultBlockState().setValue(MagmaEngineBlock.FACING, Direction.NORTH);
		assertEquals(Direction.EAST, north.rotate(Rotation.CLOCKWISE_90).getValue(MagmaEngineBlock.FACING));
		assertEquals(Direction.SOUTH, north.mirror(Mirror.LEFT_RIGHT).getValue(MagmaEngineBlock.FACING));
		assertEquals(Direction.NORTH, north.mirror(Mirror.FRONT_BACK).getValue(MagmaEngineBlock.FACING));
	}
}
