package com.comedymonkey.createaddons.content.electric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.testutil.TestConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** The motor's settings, power draw and saved state, on a block entity with no level. */
class ElectricMotorBlockEntityTest {
	private static HolderLookup.Provider registries;

	@BeforeAll
	static void lookup() {
		registries = VanillaRegistries.createLookup();
	}

	@BeforeEach
	void config() {
		TestConfig.loadDefaults();
	}

	@AfterEach
	void restore() {
		TestConfig.reset();
	}

	private static ElectricMotorBlockEntity motor(Direction facing) {
		BlockState state = ModBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(ElectricMotorBlock.FACING, facing);
		return new ElectricMotorBlockEntity(BlockPos.ZERO, state);
	}

	private static ElectricMotorBlockEntity motor() {
		return motor(Direction.NORTH);
	}

	// ---------------------------------------------------------------- power draw

	@Test
	void usageIsStressTimesFePerStress() {
		assertEquals(1024.0, ElectricMotorBlockEntity.usageFor(16384), 1e-9);
		assertEquals(18432.0, ElectricMotorBlockEntity.usageFor(294912), 1e-9);
		assertEquals(0.0, ElectricMotorBlockEntity.usageFor(0), 1e-9);
	}

	@Test
	void usageFollowsTheConfig() {
		TestConfig.set(CMConfig.SERVER.motorFePerStress, 0.5);
		assertEquals(128.0, ElectricMotorBlockEntity.usageFor(256), 1e-9);
		TestConfig.set(CMConfig.SERVER.motorFePerStress, 0.0);
		assertEquals(0.0, ElectricMotorBlockEntity.usageFor(16384), 1e-9);
	}

	@Test
	void maxStressFollowsTheConfig() {
		assertEquals(294912, ElectricMotorBlockEntity.maxStress());
		TestConfig.set(CMConfig.SERVER.motorMaxStress, 4096);
		assertEquals(4096, ElectricMotorBlockEntity.maxStress());
	}

	@Test
	void usagePerTickMatchesTheStressSetting() {
		ElectricMotorBlockEntity motor = motor();
		assertEquals(ElectricMotorBlockEntity.usageFor(motor.getStressSetting()), motor.usagePerTick(), 1e-9);
	}

	// ---------------------------------------------------------------- energy store

	@Test
	void bufferHoldsASecondAtFullStress() {
		// 294,912 SU x 0.0625 FE x 20 ticks
		assertEquals(368_640, motor().getEnergy().getMaxEnergyStored());
	}

	@Test
	void bufferIsNeverBelowAHundredThousand() {
		TestConfig.set(CMConfig.SERVER.motorMaxStress, 256);
		assertEquals(100_000, motor().getEnergy().getMaxEnergyStored());
	}

	@Test
	void bufferSaturatesInsteadOfOverflowing() {
		TestConfig.set(CMConfig.SERVER.motorMaxStress, 4194304);
		TestConfig.set(CMConfig.SERVER.motorFePerStress, 64.0);
		assertEquals(Integer.MAX_VALUE, motor().getEnergy().getMaxEnergyStored());
	}

	@Test
	void energyGoesInButNeverComesOut() {
		IEnergyStorage energy = motor().getEnergy();
		assertTrue(energy.canReceive());
		assertFalse(energy.canExtract());
		assertEquals(5000, energy.receiveEnergy(5000, false));
		assertEquals(0, energy.extractEnergy(5000, false));
		assertEquals(5000, energy.getEnergyStored());
	}

	// ---------------------------------------------------------------- settings from the screen (server-side checks)

	@Test
	void hasSensibleDefaults() {
		ElectricMotorBlockEntity motor = motor();
		assertEquals(64, motor.getSpeedSetting());
		assertEquals(16384, motor.getStressSetting());
		assertFalse(motor.isReversed());
		assertFalse(motor.isRunning());
	}

	@Test
	void appliesValidSettings() {
		ElectricMotorBlockEntity motor = motor();
		motor.applySettings(128, 32768, true);
		assertEquals(128, motor.getSpeedSetting());
		assertEquals(32768, motor.getStressSetting());
		assertTrue(motor.isReversed());
	}

	@Test
	void clampsSpeed() {
		ElectricMotorBlockEntity motor = motor();
		motor.applySettings(0, 16384, false);
		assertEquals(ElectricMotorBlockEntity.MIN_SPEED, motor.getSpeedSetting());
		motor.applySettings(-1000, 16384, false);
		assertEquals(ElectricMotorBlockEntity.MIN_SPEED, motor.getSpeedSetting());
		motor.applySettings(10_000, 16384, false);
		assertEquals(ElectricMotorBlockEntity.MAX_SPEED, motor.getSpeedSetting());
		motor.applySettings(Integer.MAX_VALUE, 16384, false);
		assertEquals(ElectricMotorBlockEntity.MAX_SPEED, motor.getSpeedSetting());
	}

	@Test
	void snapsStressDownToWholeSteps() {
		ElectricMotorBlockEntity motor = motor();
		motor.applySettings(64, 16384 + 255, false);
		assertEquals(16384, motor.getStressSetting());
		motor.applySettings(64, 1000, false);
		assertEquals(768, motor.getStressSetting());
		assertEquals(0, motor.getStressSetting() % ElectricMotorBlockEntity.STRESS_STEP);
	}

	@Test
	void clampsStressToItsRange() {
		ElectricMotorBlockEntity motor = motor();
		motor.applySettings(64, 0, false);
		assertEquals(ElectricMotorBlockEntity.STRESS_STEP, motor.getStressSetting());
		motor.applySettings(64, -50_000, false);
		assertEquals(ElectricMotorBlockEntity.STRESS_STEP, motor.getStressSetting());
		motor.applySettings(64, Integer.MIN_VALUE, false);
		assertEquals(ElectricMotorBlockEntity.STRESS_STEP, motor.getStressSetting());
		motor.applySettings(64, Integer.MAX_VALUE, false);
		assertEquals(ElectricMotorBlockEntity.maxStress(), motor.getStressSetting());
	}

	@Test
	void stressCapFollowsTheConfig() {
		TestConfig.set(CMConfig.SERVER.motorMaxStress, 8192);
		ElectricMotorBlockEntity motor = motor();
		motor.applySettings(64, 100_000, false);
		assertEquals(8192, motor.getStressSetting());
	}

	// ---------------------------------------------------------------- output

	@Test
	void givesNothingWhileStopped() {
		ElectricMotorBlockEntity motor = motor();
		assertEquals(0f, motor.getGeneratedSpeed());
		assertEquals(0f, motor.calculateAddedStressCapacity());
	}

	@Test
	void runningGivesItsSetSpeedInTheShaftsDirection() {
		ElectricMotorBlockEntity south = motor(Direction.SOUTH);
		south.read(running(64, 16384, false), registries, false);
		assertEquals(64f, south.getGeneratedSpeed());

		ElectricMotorBlockEntity north = motor(Direction.NORTH);
		north.read(running(64, 16384, false), registries, false);
		assertEquals(-64f, north.getGeneratedSpeed());

		ElectricMotorBlockEntity reversed = motor(Direction.SOUTH);
		reversed.read(running(32, 16384, true), registries, false);
		assertEquals(-32f, reversed.getGeneratedSpeed());
	}

	@Test
	void runningCapacityIsTheSetStressAtAnySpeed() {
		for (int speed : new int[] {1, 16, 64, 256}) {
			ElectricMotorBlockEntity motor = motor(Direction.SOUTH);
			motor.read(running(speed, 16384, false), registries, false);
			assertEquals(16384f, motor.calculateAddedStressCapacity() * speed, 1e-2f);
		}
	}

	private static CompoundTag running(int speed, int stress, boolean reversed) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("SpeedSetting", speed);
		tag.putInt("StressSetting", stress);
		tag.putBoolean("Reversed", reversed);
		tag.putBoolean("Running", true);
		return tag;
	}

	// ---------------------------------------------------------------- saving

	@Test
	void savesAndLoadsEverything() {
		ElectricMotorBlockEntity motor = motor();
		motor.applySettings(128, 65536, true);
		motor.getEnergy().receiveEnergy(12_345, false);
		CompoundTag tag = new CompoundTag();
		motor.write(tag, registries, false);

		ElectricMotorBlockEntity loaded = motor();
		loaded.read(tag, registries, false);
		assertEquals(128, loaded.getSpeedSetting());
		assertEquals(65536, loaded.getStressSetting());
		assertTrue(loaded.isReversed());
		assertFalse(loaded.isRunning());
		assertEquals(12_345, loaded.getEnergy().getEnergyStored());
	}

	@Test
	void oldSavesWithoutSettingsKeepTheDefaults() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("Energy", 100);
		ElectricMotorBlockEntity loaded = motor();
		loaded.read(tag, registries, false);
		assertEquals(64, loaded.getSpeedSetting());
		assertEquals(16384, loaded.getStressSetting());
		assertEquals(100, loaded.getEnergy().getEnergyStored());
	}

	@Test
	void loadedEnergyIsClampedToTheBuffer() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("Energy", Integer.MAX_VALUE);
		ElectricMotorBlockEntity loaded = motor();
		loaded.read(tag, registries, false);
		assertEquals(loaded.getEnergy().getMaxEnergyStored(), loaded.getEnergy().getEnergyStored());

		tag.putInt("Energy", -5);
		loaded.read(tag, registries, false);
		assertEquals(0, loaded.getEnergy().getEnergyStored());
	}

	@Test
	void loadedSpeedIsClamped() {
		ElectricMotorBlockEntity loaded = motor(Direction.SOUTH);
		loaded.read(running(0, 16384, false), registries, false);
		assertTrue(loaded.getSpeedSetting() >= ElectricMotorBlockEntity.MIN_SPEED);
		assertTrue(Float.isFinite(loaded.calculateAddedStressCapacity()));
	}

	@Test
	void loadedStressIsClampedToTheConfig() {
		ElectricMotorBlockEntity saved = motor();
		saved.applySettings(64, 294912, false);
		CompoundTag tag = new CompoundTag();
		saved.write(tag, registries, false);

		TestConfig.set(CMConfig.SERVER.motorMaxStress, 16384);
		ElectricMotorBlockEntity loaded = motor();
		loaded.read(tag, registries, false);
		assertTrue(loaded.getStressSetting() <= ElectricMotorBlockEntity.maxStress());
	}
}
