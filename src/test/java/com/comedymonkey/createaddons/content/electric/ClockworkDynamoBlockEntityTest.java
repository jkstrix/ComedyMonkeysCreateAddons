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

/** The Clockwork Dynamo's output, loss, settings, paper rule and saved state, on a block entity with no level. */
class ClockworkDynamoBlockEntityTest {
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

	private static ClockworkDynamoBlockEntity dynamo() {
		BlockState state = ModBlocks.CLOCKWORK_DYNAMO.get().defaultBlockState().setValue(ClockworkDynamoBlock.FACING, Direction.NORTH);
		return new ClockworkDynamoBlockEntity(BlockPos.ZERO, state);
	}

	// ---------------------------------------------------------------- output and the loss

	@Test
	void outputIsThreeQuartersOfWhatAMotorDraws() {
		assertEquals(12.0, ClockworkDynamoBlockEntity.outputFor(256), 1e-9);
		assertEquals(192.0, ClockworkDynamoBlockEntity.outputFor(4096), 1e-9);
		assertEquals(768.0, ClockworkDynamoBlockEntity.outputFor(16384), 1e-9);
		assertEquals(13824.0, ClockworkDynamoBlockEntity.outputFor(294912), 1e-9);
	}

	/** More power always takes more stress, in step (D8). */
	@Test
	void outputIsInStepWithStress() {
		assertEquals(2 * ClockworkDynamoBlockEntity.outputFor(8192), ClockworkDynamoBlockEntity.outputFor(16384), 1e-9);
	}

	/** A motor and a dynamo at the same setting: the motor always needs more than the dynamo makes (D2). */
	@Test
	void aMotorAndADynamoCannotRunEachOther() {
		for (int stress = 256; stress <= 294912; stress *= 2)
			assertTrue(ElectricMotorBlockEntity.usageFor(stress) > ClockworkDynamoBlockEntity.outputFor(stress), "at " + stress + " SU");
	}

	@Test
	void outputFollowsTheEfficiencySetting() {
		TestConfig.set(CMConfig.SERVER.dynamoEfficiency, 0.5);
		assertEquals(512.0, ClockworkDynamoBlockEntity.outputFor(16384), 1e-9);
		TestConfig.set(CMConfig.SERVER.dynamoEfficiency, 1.0);
		assertEquals(ElectricMotorBlockEntity.usageFor(16384), ClockworkDynamoBlockEntity.outputFor(16384), 1e-9);
	}

	/** Two seconds of its most output: 552,960 FE at the defaults. */
	@Test
	void storeHoldsTwoSecondsFlatOut() {
		assertEquals(552960, ClockworkDynamoBlockEntity.storeSize());
		assertEquals(552960, dynamo().getEnergy().getMaxEnergyStored());
	}

	// ---------------------------------------------------------------- settings

	@Test
	void startsAtOneWaterWheel() {
		assertEquals(256, dynamo().getStressSetting());
	}

	@Test
	void settingsSnapToStepsWithinRange() {
		assertEquals(256, ClockworkDynamoBlockEntity.clampStress(0));
		assertEquals(256, ClockworkDynamoBlockEntity.clampStress(300));
		assertEquals(16384, ClockworkDynamoBlockEntity.clampStress(16400));
		assertEquals(294912, ClockworkDynamoBlockEntity.clampStress(10_000_000));
		TestConfig.set(CMConfig.SERVER.motorMaxStress, 4096);
		assertEquals(4096, ClockworkDynamoBlockEntity.clampStress(16384));
	}

	@Test
	void applySettingsWithoutALevelKeepsTheSetting() {
		ClockworkDynamoBlockEntity d = dynamo();
		d.applySettings(16400);
		assertEquals(16384, d.getStressSetting());
	}

	// ---------------------------------------------------------------- power out

	/** Pipes and cables can only take power from it, never put any in. */
	@Test
	void energyCanOnlyBeTaken() {
		ClockworkDynamoBlockEntity d = dynamo();
		d.getEnergy().setEnergy(1000);
		IEnergyStorage out = d.getEnergyOut();
		assertFalse(out.canReceive());
		assertEquals(0, out.receiveEnergy(500, false));
		assertEquals(400, out.extractEnergy(400, false));
		assertEquals(600, d.getEnergy().getEnergyStored());
	}

	@Test
	void onlyTheBaseGivesPower() {
		for (Direction facing : Direction.values()) {
			BlockState state = ModBlocks.CLOCKWORK_DYNAMO.get().defaultBlockState().setValue(ClockworkDynamoBlock.FACING, facing);
			for (Direction side : Direction.values())
				assertEquals(side == facing.getOpposite(), ClockworkDynamoBlock.isOutput(state, side), facing + " / " + side);
		}
	}

	// ---------------------------------------------------------------- paper (D11, D12)

	/** Each free source's capacity becomes its share of the whole network's. */
	@Test
	void freeSourcesShareTheWhole() {
		var shares = ClockworkDynamoBlockEntity.shares(java.util.Map.of(new BlockPos(1, 0, 0), 1024.0, new BlockPos(2, 0, 0), 3072.0), 8192);
		assertEquals(0.125, shares.get(new BlockPos(1, 0, 0)), 1e-9);
		assertEquals(0.375, shares.get(new BlockPos(2, 0, 0)), 1e-9);
	}

	@Test
	void noWholeNoShares() {
		assertTrue(ClockworkDynamoBlockEntity.shares(java.util.Map.of(BlockPos.ZERO, 1024.0), 0).isEmpty());
	}

	@Test
	void aShareIsNeverMoreThanAll() {
		assertEquals(1.0, ClockworkDynamoBlockEntity.shares(java.util.Map.of(BlockPos.ZERO, 5000.0), 4096).get(BlockPos.ZERO), 1e-9);
	}

	/** Its free sources are saved (server side), so its record is right from its first tick after a load. */
	@Test
	void freeSourcesAreSaved() {
		CompoundTag tag = new CompoundTag();
		net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
		CompoundTag one = new CompoundTag();
		one.putLong("Pos", new BlockPos(3, 4, 5).asLong());
		one.putDouble("Share", 0.5);
		list.add(one);
		tag.put("FreeSources", list);
		ClockworkDynamoBlockEntity d = dynamo();
		d.loadWithComponents(tag, registries);
		assertEquals(0.5f, d.getFreeShare(), 1e-6);
		ClockworkDynamoBlockEntity back = dynamo();
		back.loadWithComponents(d.saveWithoutMetadata(registries), registries);
		assertEquals(0.5f, back.getFreeShare(), 1e-6);
	}

	// ---------------------------------------------------------------- saving

	@Test
	void settingStoreAndStateSurviveSaving() {
		ClockworkDynamoBlockEntity d = dynamo();
		d.applySettings(16384);
		d.getEnergy().setEnergy(12345);
		CompoundTag tag = d.saveWithoutMetadata(registries);
		ClockworkDynamoBlockEntity back = dynamo();
		back.loadWithComponents(tag, registries);
		assertEquals(16384, back.getStressSetting());
		assertEquals(12345, back.getEnergy().getEnergyStored());
	}

	/** A setting saved before the most was lowered in the config comes back within it. */
	@Test
	void loadingClampsTheSetting() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("StressSetting", 294912);
		TestConfig.set(CMConfig.SERVER.motorMaxStress, 4096);
		ClockworkDynamoBlockEntity d = dynamo();
		d.loadWithComponents(tag, registries);
		assertEquals(4096, d.getStressSetting());
	}
}
