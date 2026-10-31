package com.comedymonkey.createaddons.client.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.content.line.Fitting;
import com.comedymonkey.createaddons.content.line.FluidLineBlockEntity;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.testutil.TestConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** FLY-06: which Fluid Lines get a parts visual (a frame's work each) and which an idle one. */
class MachineVisualsTest {
	@BeforeAll
	static void setUp() {
		TestConfig.loadDefaults();
	}

	private static FluidLineBlockEntity line(Fitting fitting) {
		FluidLineBlockEntity be = ModBlockEntities.FLUID_LINE.get().create(BlockPos.ZERO, ModBlocks.COPPER_FLUID_LINE.get().defaultBlockState());
		if (fitting != Fitting.NONE)
			be.setFitting(fitting, Direction.NORTH);
		return be;
	}

	/** Only an in-line fitting has parts: a plain line has none, and an Intake's renderer draws all it shows (its fluid). */
	@Test
	void onlyInLineFittingsHaveParts() {
		for (Fitting fitting : Fitting.values())
			assertEquals(fitting != Fitting.NONE && fitting != Fitting.INTAKE, MachineVisuals.fluidLineHasParts(line(fitting)), fitting.name());
	}

	/** Encased, the casing's block model hides it all. */
	@Test
	void encasedLinesHaveNone() {
		for (Fitting fitting : Fitting.values()) {
			FluidLineBlockEntity be = line(fitting);
			be.setEncasing(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
			assertEquals(false, MachineVisuals.fluidLineHasParts(be), fitting.name());
		}
	}
}
