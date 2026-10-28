package com.comedymonkey.createaddons.content.light;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.testutil.ModResourceFiles;
import com.google.gson.JsonObject;

import net.minecraft.world.item.DyeColor;

class LightColoursTest {
	@Test
	void castLightIsAtFullAndKeepsTheLensHue() {
		for (DyeColor dye : DyeColor.values()) {
			float[] c = LightColours.cast(dye, LightColours.lens(dye));
			assertEquals(1f, Math.max(c[0], Math.max(c[1], c[2])), 1e-5f, dye + " casts at full");
			for (float f : c)
				assertTrue(f >= 0 && f <= 1, dye + " stays in range");
		}
		float[] red = LightColours.cast(DyeColor.RED, LightColours.lens(DyeColor.RED));
		assertTrue(red[0] == 1f && red[1] < 0.5f && red[2] < 0.5f, "red casts red");
		float[] blue = LightColours.cast(DyeColor.BLUE, LightColours.lens(DyeColor.BLUE));
		assertTrue(blue[2] == 1f && blue[0] < 0.6f, "blue casts blue");
	}

	@Test
	void castLightIsStrongerThanTheMutedLens() {
		int lens = LightColours.lens(DyeColor.GREEN);
		float r = (lens >> 16 & 255) / 255f, g = (lens >> 8 & 255) / 255f;
		float[] c = LightColours.cast(DyeColor.GREEN, lens);
		assertTrue(c[0] / c[1] < r / g, "green's red share drops: it reads over white light");
	}

	@Test
	void undyedIsWarmAndBlackIsUltraviolet() {
		float[] warm = LightColours.cast(null, LightColours.lens(null));
		assertTrue(warm[0] == 1f && warm[2] < warm[1], "undyed is warm");
		float[] black = LightColours.cast(DyeColor.BLACK, LightColours.lens(DyeColor.BLACK));
		assertTrue(black[2] == 1f && black[0] > black[1], "black is a violet, not grey");
	}

	@Test
	void greenAndLimeCastDifferentLight() {
		assertTrue(!LightColours.castHex(DyeColor.GREEN).equals(LightColours.castHex(DyeColor.LIME)), "green casts as lime");
	}

	@Test
	void lightDyesFollowTheDyes() {
		assertEquals(LightDye.NONE, LightDye.of(null));
		for (DyeColor dye : DyeColor.values()) {
			assertEquals(dye, LightDye.of(dye).dye());
			assertEquals(dye.getSerializedName(), LightDye.of(dye).getSerializedName());
		}
	}

	/** Colorful Lighting's emitters (written by the exporter) are the colours the lights cast: every light, every dye. */
	@Test
	void emittersMatchTheCastColours() {
		JsonObject emitters = ModResourceFiles.readJson(ModResourceFiles.find("assets", "comedymonkeysaddons", "light", "emitters.json"))
			.getAsJsonObject();
		java.util.List<String> lights = new java.util.ArrayList<>();
		for (LightKind kind : LightKind.values())
			lights.add(kind.id());
		lights.add("light_strip");
		lights.add("edge_light");
		for (String light : lights) {
			String id = "comedymonkeysaddons:" + light;
			assertEquals(LightColours.castHex(null), emitters.get(id).getAsString(), id);
			for (DyeColor dye : DyeColor.values()) {
				String key = id + "[color=" + dye.getSerializedName() + "]";
				assertTrue(emitters.has(key), "no emitter for " + key);
				assertEquals(LightColours.castHex(dye), emitters.get(key).getAsString(), key);
			}
		}
	}
}
