package com.comedymonkey.createaddons.content.electric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.testutil.TestConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** The Lava Generator's tank, energy store and saved state, on a block entity with no level. */
class LavaGeneratorBlockEntityTest {
	private static HolderLookup.Provider registries;

	@BeforeAll
	static void lookup() {
		registries = VanillaRegistries.createLookup();
	}

	@BeforeEach
	void config() {
		TestConfig.loadDefaults();
	}

	private static LavaGeneratorBlockEntity generator() {
		return new LavaGeneratorBlockEntity(BlockPos.ZERO, ModBlocks.LAVA_GENERATOR.get().defaultBlockState());
	}

	@Test
	void tankTakesEightBucketsOfLavaOnly() {
		FluidTank tank = generator().getTank();
		assertEquals(8000, tank.getCapacity());
		assertEquals(0, tank.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE));
		assertEquals(8000, tank.fill(new FluidStack(Fluids.LAVA, 10_000), FluidAction.EXECUTE));
		assertEquals(8000, tank.getFluidAmount());
	}

	@Test
	void energyCanOnlyBeMadeInsideAndTakenOut() {
		EnergyBuffer energy = generator().getEnergy();
		assertEquals(100_000, energy.getMaxEnergyStored());
		assertFalse(energy.canReceive());
		assertEquals(0, energy.receiveEnergy(1000, false));

		assertEquals(100_000, energy.generate(250_000));
		IEnergyStorage out = energy.extractOnly();
		assertEquals(0, out.receiveEnergy(1000, false));
		assertEquals(40_000, out.extractEnergy(40_000, false));
		assertEquals(60_000, energy.getEnergyStored());
	}

	@Test
	void savesAndLoadsTankAndEnergy() {
		LavaGeneratorBlockEntity be = generator();
		be.getTank().fill(new FluidStack(Fluids.LAVA, 2500), FluidAction.EXECUTE);
		be.getEnergy().generate(31_000);
		CompoundTag tag = new CompoundTag();
		be.write(tag, registries, false);

		LavaGeneratorBlockEntity loaded = generator();
		loaded.read(tag, registries, false);
		assertEquals(2500, loaded.getTank().getFluidAmount());
		assertTrue(loaded.getTank().getFluid().is(Fluids.LAVA));
		assertEquals(31_000, loaded.getEnergy().getEnergyStored());
	}

	@Test
	void loadingAnEmptyTankEmptiesIt() {
		CompoundTag tag = new CompoundTag();
		generator().write(tag, registries, false);
		LavaGeneratorBlockEntity loaded = generator();
		loaded.getTank().fill(new FluidStack(Fluids.LAVA, 4000), FluidAction.EXECUTE);
		loaded.read(tag, registries, false);
		assertTrue(loaded.getTank().isEmpty());
	}

	@Test
	void rotorStartsStill() {
		assertEquals(0f, generator().getRotorAngle(0.5f));
	}
}
