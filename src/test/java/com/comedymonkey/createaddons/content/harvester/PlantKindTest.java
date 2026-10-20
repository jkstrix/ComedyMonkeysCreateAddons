package com.comedymonkey.createaddons.content.harvester;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class PlantKindTest {
	@Test
	void hasNoneCropAndTree() {
		assertArrayEquals(new PlantKind[] {PlantKind.NONE, PlantKind.CROP, PlantKind.TREE}, PlantKind.values());
	}

	@Test
	void serializesAsLowerCaseNames() {
		assertEquals("none", PlantKind.NONE.getSerializedName());
		assertEquals("crop", PlantKind.CROP.getSerializedName());
		assertEquals("tree", PlantKind.TREE.getSerializedName());
	}

	@Test
	void blockStatePropertyHasEveryKindAndParsesItsNames() {
		assertTrue(AutoHarvesterBlock.PLANT.getPossibleValues().containsAll(java.util.List.of(PlantKind.values())));
		for (PlantKind kind : PlantKind.values())
			assertEquals(Optional.of(kind), AutoHarvesterBlock.PLANT.getValue(kind.getSerializedName()));
	}
}
