package com.comedymonkey.createaddons.content.pen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.testutil.TestConfig;

import net.minecraft.core.BlockPos;

class AnimalPenMenuTest {
	@BeforeAll
	static void config() {
		TestConfig.loadDefaults();
	}

	private static AnimalPenBlockEntity pen(boolean brass) {
		return new AnimalPenBlockEntity(BlockPos.ZERO,
			(brass ? ModBlocks.BRASS_ANIMAL_PEN.get() : ModBlocks.ANIMAL_PEN.get()).defaultBlockState());
	}

	@Test
	void outputColumns() {
		assertEquals(3, AnimalPenMenu.columns(pen(false)));
		assertEquals(6, AnimalPenMenu.columns(pen(true)));
	}

	/** The output grid and the trough row have to fit the machine panel, in whole rows, without overlapping. */
	@Test
	void outputGridAndTroughFitThePanel() {
		for (boolean brass : new boolean[] {false, true}) {
			AnimalPenBlockEntity pen = pen(brass);
			int columns = AnimalPenMenu.columns(pen);
			int rows = pen.getOutput().getSlots() / columns;
			assertEquals(0, pen.getOutput().getSlots() % columns);
			// A slot's frame starts a pixel before its item and is 18 across
			assertTrue(AnimalPenMenu.OUTPUT_X - 1 + columns * 18 <= AnimalPenMenu.WIDTH - 7, "output too wide");
			assertTrue(AnimalPenMenu.OUTPUT_Y - 1 + rows * 18 < AnimalPenMenu.TROUGH_Y - 1, "output runs into the trough");
			assertTrue(AnimalPenMenu.TROUGH_X - 1 + pen.getFood().getSlots() * 18 <= AnimalPenMenu.WIDTH - 7,
				"trough too wide");
		}
	}
}
