package com.comedymonkey.createaddons.client.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;

import org.junit.jupiter.api.Test;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.level.block.state.properties.ChestType;

/** The chest visual draws what vanilla's ChestRenderer draws: the same model per type, the same lid easing, the same Christmas days. */
class VanillaChestVisualTest {
	@Test
	void eachTypeUsesVanillasModel() {
		assertEquals(ModelLayers.CHEST, VanillaChestVisual.layer(ChestType.SINGLE));
		assertEquals(ModelLayers.DOUBLE_CHEST_LEFT, VanillaChestVisual.layer(ChestType.LEFT));
		assertEquals(ModelLayers.DOUBLE_CHEST_RIGHT, VanillaChestVisual.layer(ChestType.RIGHT));
	}

	@Test
	void theLidEasesAsVanillasDoes() {
		for (int i = 0; i <= 20; i++) {
			float openness = i / 20f;
			// ChestRenderer.render: f1 = 1 - f1; f1 = 1 - f1 * f1 * f1; lid.xRot = -(f1 * PI / 2)
			float f1 = 1.0F - openness;
			f1 = 1.0F - f1 * f1 * f1;
			float vanilla = -(f1 * (float) (Math.PI / 2));
			assertEquals(vanilla, VanillaChestVisual.lidAngle(openness), 0f, "at " + openness);
		}
		assertEquals(0f, VanillaChestVisual.lidAngle(0f), 0f);
		assertEquals(-(float) (Math.PI / 2), VanillaChestVisual.lidAngle(1f), 1e-6f);
	}

	@Test
	void christmasIsTheTwentyFourthToTheTwentySixthOfDecember() {
		assertTrue(VanillaChestVisual.christmas(day(Calendar.DECEMBER, 24)));
		assertTrue(VanillaChestVisual.christmas(day(Calendar.DECEMBER, 25)));
		assertTrue(VanillaChestVisual.christmas(day(Calendar.DECEMBER, 26)));
		assertFalse(VanillaChestVisual.christmas(day(Calendar.DECEMBER, 23)));
		assertFalse(VanillaChestVisual.christmas(day(Calendar.DECEMBER, 27)));
		assertFalse(VanillaChestVisual.christmas(day(Calendar.NOVEMBER, 25)));
	}

	private static Calendar day(int month, int day) {
		Calendar c = Calendar.getInstance();
		c.clear();
		c.set(2026, month, day);
		return c;
	}
}
