package com.comedymonkey.createaddons.content.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.testutil.TestConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** The Magma Engine's tank and output, on a block entity with no level. */
class MagmaEngineBlockEntityTest {
	@BeforeEach
	void config() {
		TestConfig.loadDefaults();
	}

	@AfterEach
	void restore() {
		TestConfig.reset();
	}

	private static MagmaEngineBlockEntity engine(MagmaEngineBlock block) {
		return new MagmaEngineBlockEntity(BlockPos.ZERO, block.defaultBlockState());
	}

	@Test
	void tankSizeMatchesTheEngine() {
		assertEquals(4000, engine(ModBlocks.MAGMA_ENGINE.get()).getTank().getCapacity());
		assertEquals(8000, engine(ModBlocks.SUPERCHARGED_MAGMA_ENGINE.get()).getTank().getCapacity());
	}

	@Test
	void tankTakesLavaOnly() {
		FluidTank tank = engine(ModBlocks.MAGMA_ENGINE.get()).getTank();
		assertEquals(0, tank.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE));
		assertEquals(4000, tank.fill(new FluidStack(Fluids.LAVA, 9000), FluidAction.EXECUTE));
		assertEquals(4000, tank.getFluidAmount());
	}

	@Test
	void doesNotTurnUntilItBurns() {
		assertEquals(0f, engine(ModBlocks.MAGMA_ENGINE.get()).getGeneratedSpeed());
		assertEquals(0f, engine(ModBlocks.SUPERCHARGED_MAGMA_ENGINE.get()).getGeneratedSpeed());
	}

	@Test
	void capacityPerRpmComesFromItsOwnBlock() {
		TestConfig.set(CMConfig.SERVER.magmaEngineCapacity, 100.0);
		TestConfig.set(CMConfig.SERVER.superchargedMagmaEngineCapacity, 300.0);
		assertEquals(100.0, engine(ModBlocks.MAGMA_ENGINE.get()).capacityPerRpm(), 1e-9);
		assertEquals(300.0, engine(ModBlocks.SUPERCHARGED_MAGMA_ENGINE.get()).capacityPerRpm(), 1e-9);
	}
}
