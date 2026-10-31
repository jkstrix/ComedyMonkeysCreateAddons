package com.comedymonkey.createaddons.content.line;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.content.electric.ClockworkDynamoBlockEntity;
import com.comedymonkey.createaddons.content.electric.GeothermalCasingBlockEntity;
import com.comedymonkey.createaddons.content.electric.GeothermalControllerBlockEntity;
import com.comedymonkey.createaddons.content.electric.LavaGeneratorBlockEntity;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.testutil.TestConfig;

import net.minecraft.core.BlockPos;

/**
 * Each generator says what kind of record it keeps without making one (the grid compares kinds every tick), and the
 * record it makes when it needs one is of that kind.
 */
class GeneratorRecordKindTest {
	@BeforeEach
	void config() {
		TestConfig.loadDefaults();
	}

	@AfterEach
	void restore() {
		TestConfig.reset();
	}

	private static void same(PowerGrid.Generator g, String kind, int fuels) {
		assertEquals(kind, g.paperKind());
		assertEquals(fuels, g.paperFuels());
		PowerGrid.Paper p = g.newPaper();
		assertEquals(kind, p.kind);
		assertEquals(fuels, p.fuels.length);
	}

	@Test
	void eachGeneratorNamesItsRecord() {
		same(new LavaGeneratorBlockEntity(BlockPos.ZERO, ModBlocks.LAVA_GENERATOR.get().defaultBlockState()), "lava_generator", 1);
		same(new GeothermalControllerBlockEntity(BlockPos.ZERO, ModBlocks.GEOTHERMAL_CONTROLLER.get().defaultBlockState()), "geothermal_turbine", 2);
		same(new ClockworkDynamoBlockEntity(BlockPos.ZERO, ModBlocks.CLOCKWORK_DYNAMO.get().defaultBlockState()), "clockwork_dynamo", 0);
	}

	/** A turbine's casing is only part of one: no record of its own. */
	@Test
	void aCasingKeepsNone() {
		GeothermalCasingBlockEntity casing = new GeothermalCasingBlockEntity(BlockPos.ZERO, ModBlocks.GEOTHERMAL_CASING.get().defaultBlockState());
		assertNull(casing.paperKind());
		assertNull(casing.newPaper());
	}
}
