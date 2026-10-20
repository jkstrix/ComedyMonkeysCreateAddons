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
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** The turbine controller's inlets, energy outlet, geometry and saved state, on a block entity with no level. */
class GeothermalControllerBlockEntityTest {
	private static final BlockPos POS = new BlockPos(10, 64, 10);
	private static HolderLookup.Provider registries;

	@BeforeAll
	static void lookup() {
		registries = VanillaRegistries.createLookup();
	}

	@BeforeEach
	void config() {
		TestConfig.loadDefaults();
	}

	/** A controller of a formed turbine (loaded as a saved one would be; there's no level to build it in). */
	private static GeothermalControllerBlockEntity controller(Direction facing) {
		GeothermalControllerBlockEntity be = unformed(facing);
		CompoundTag tag = be.saveWithoutMetadata(registries);
		tag.putBoolean("Formed", true);
		be.loadWithComponents(tag, registries);
		return be;
	}

	private static GeothermalControllerBlockEntity unformed(Direction facing) {
		return new GeothermalControllerBlockEntity(POS, ModBlocks.GEOTHERMAL_CONTROLLER.get().defaultBlockState()
			.setValue(GeothermalControllerBlock.FACING, facing));
	}

	private static GeothermalControllerBlockEntity controller() {
		return controller(Direction.NORTH);
	}

	// ---------------------------------------------------------------- inlets

	@Test
	void hasALavaTankAndAWaterTank() {
		IFluidHandler fluids = controller().getFluids();
		assertEquals(2, fluids.getTanks());
		assertEquals(GeothermalControllerBlockEntity.TANK, fluids.getTankCapacity(0));
		assertEquals(GeothermalControllerBlockEntity.TANK, fluids.getTankCapacity(1));
		assertTrue(fluids.isFluidValid(0, new FluidStack(Fluids.LAVA, 1)));
		assertFalse(fluids.isFluidValid(0, new FluidStack(Fluids.WATER, 1)));
		assertTrue(fluids.isFluidValid(1, new FluidStack(Fluids.WATER, 1)));
		assertFalse(fluids.isFluidValid(1, new FluidStack(Fluids.LAVA, 1)));
	}

	/** A pipe that cached the inlets of a turbine since broken up mustn't pour fluid into nothing. */
	@Test
	void anUnformedTurbineTakesNoFluid() {
		IFluidHandler fluids = unformed(Direction.NORTH).getFluids();
		assertEquals(0, fluids.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE));
		assertEquals(0, fluids.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE));
		assertTrue(fluids.getFluidInTank(0).isEmpty());
	}

	@Test
	void lavaAndWaterEachGoToTheirOwnTank() {
		GeothermalControllerBlockEntity be = controller();
		IFluidHandler fluids = be.getFluids();
		assertEquals(1000, fluids.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE));
		assertEquals(4000, fluids.fill(new FluidStack(Fluids.WATER, 4000), FluidAction.EXECUTE));
		assertEquals(1000, fluids.getFluidInTank(0).getAmount());
		assertTrue(fluids.getFluidInTank(0).is(Fluids.LAVA));
		assertEquals(4000, fluids.getFluidInTank(1).getAmount());
		assertTrue(fluids.getFluidInTank(1).is(Fluids.WATER));
		assertEquals(1000f / GeothermalControllerBlockEntity.TANK, be.lavaFill(), 1e-6f);
		assertEquals(4000f / GeothermalControllerBlockEntity.TANK, be.waterFill(), 1e-6f);
	}

	@Test
	void flowingLavaCountsAsLava() {
		IFluidHandler fluids = controller().getFluids();
		assertEquals(500, fluids.fill(new FluidStack(Fluids.FLOWING_LAVA, 500), FluidAction.EXECUTE));
		assertEquals(500, fluids.getFluidInTank(0).getAmount());
		assertTrue(fluids.getFluidInTank(1).isEmpty());
	}

	@Test
	void fillsNoMoreThanATank() {
		IFluidHandler fluids = controller().getFluids();
		assertEquals(GeothermalControllerBlockEntity.TANK,
			fluids.fill(new FluidStack(Fluids.LAVA, GeothermalControllerBlockEntity.TANK + 5000), FluidAction.EXECUTE));
		// A full lava tank doesn't spill lava into the water tank
		assertEquals(0, fluids.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE));
		assertTrue(fluids.getFluidInTank(1).isEmpty());
	}

	@Test
	void simulatedFillChangesNothing() {
		IFluidHandler fluids = controller().getFluids();
		assertEquals(1000, fluids.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.SIMULATE));
		assertTrue(fluids.getFluidInTank(1).isEmpty());
	}

	@Test
	void rejectsAnEmptyStack() {
		IFluidHandler fluids = controller().getFluids();
		assertEquals(0, fluids.fill(FluidStack.EMPTY, FluidAction.EXECUTE));
		assertTrue(fluids.getFluidInTank(0).isEmpty());
		assertTrue(fluids.getFluidInTank(1).isEmpty());
	}

	@Test
	void nothingComesBackOut() {
		IFluidHandler fluids = controller().getFluids();
		fluids.fill(new FluidStack(Fluids.LAVA, 2000), FluidAction.EXECUTE);
		fluids.fill(new FluidStack(Fluids.WATER, 2000), FluidAction.EXECUTE);
		assertTrue(fluids.drain(1000, FluidAction.EXECUTE).isEmpty());
		assertTrue(fluids.drain(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE).isEmpty());
		assertTrue(fluids.drain(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE).isEmpty());
		assertEquals(2000, fluids.getFluidInTank(0).getAmount());
		assertEquals(2000, fluids.getFluidInTank(1).getAmount());
	}

	// ---------------------------------------------------------------- energy

	@Test
	void energyOutletOnlyGivesEnergy() {
		IEnergyStorage out = controller().getEnergyOut();
		assertFalse(out.canReceive());
		assertTrue(out.canExtract());
		assertEquals(0, out.receiveEnergy(10_000, false));
		assertEquals(0, out.getEnergyStored());
		assertEquals(4_000_000, out.getMaxEnergyStored());
	}

	@Test
	void storedEnergyCanBeTakenOut() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("Energy", 1234);
		GeothermalControllerBlockEntity be = controller();
		be.read(tag, registries, false);
		IEnergyStorage out = be.getEnergyOut();
		assertEquals(1234, out.extractEnergy(5000, true));
		assertEquals(1000, out.extractEnergy(1000, false));
		assertEquals(234, out.getEnergyStored());
	}

	// ---------------------------------------------------------------- geometry and state

	@Test
	void startsUnformed() {
		assertFalse(unformed(Direction.NORTH).isFormed());
	}

	@Test
	void centreIsTheBlockBehindTheController() {
		assertEquals(new BlockPos(10, 64, 11), controller(Direction.NORTH).centre());
		assertEquals(new BlockPos(10, 64, 9), controller(Direction.SOUTH).centre());
		assertEquals(new BlockPos(9, 64, 10), controller(Direction.EAST).centre());
		assertEquals(new BlockPos(11, 64, 10), controller(Direction.WEST).centre());
	}

	@Test
	void savesAndLoadsTanksEnergyAndFormation() {
		GeothermalControllerBlockEntity be = controller();
		be.getFluids().fill(new FluidStack(Fluids.LAVA, 3000), FluidAction.EXECUTE);
		be.getFluids().fill(new FluidStack(Fluids.WATER, 7000), FluidAction.EXECUTE);
		CompoundTag tag = new CompoundTag();
		be.write(tag, registries, false);
		tag.putInt("Energy", 50_000);
		tag.putBoolean("Formed", true);

		GeothermalControllerBlockEntity loaded = controller();
		loaded.read(tag, registries, false);
		assertEquals(3000, loaded.getFluids().getFluidInTank(0).getAmount());
		assertEquals(7000, loaded.getFluids().getFluidInTank(1).getAmount());
		assertEquals(50_000, loaded.getEnergyOut().getEnergyStored());
		assertTrue(loaded.isFormed());
	}

	@Test
	void loadingReplacesWhatWasInTheTanks() {
		GeothermalControllerBlockEntity empty = controller();
		CompoundTag tag = new CompoundTag();
		empty.write(tag, registries, false);

		GeothermalControllerBlockEntity loaded = controller();
		loaded.getFluids().fill(new FluidStack(Fluids.LAVA, 3000), FluidAction.EXECUTE);
		loaded.read(tag, registries, false);
		assertTrue(loaded.getFluids().getFluidInTank(0).isEmpty());
		assertEquals(0f, loaded.lavaFill());
	}
}
