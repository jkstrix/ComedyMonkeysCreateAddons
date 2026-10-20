package com.comedymonkey.createaddons.content.cobblegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.testutil.ModResourceFiles;
import com.google.gson.JsonObject;

import net.minecraft.world.item.Items;

/** What each generator makes, how hard it is to "mine", and how it's shown. */
class GeneratorProductTest {
	@Test
	void items() {
		assertSame(Items.COBBLESTONE, GeneratorProduct.COBBLESTONE.item());
		assertSame(Items.BASALT, GeneratorProduct.BASALT.item());
		assertSame(Items.BLACKSTONE, GeneratorProduct.BLACKSTONE.item());
		assertSame(Items.TUFF, GeneratorProduct.TUFF.item());
	}

	@Test
	void hardnessIsTheBlocks() {
		// Sets the drill-paced cycle time: these are the vanilla destroy times
		assertEquals(2.0f, GeneratorProduct.COBBLESTONE.hardness(), 1e-6);
		assertEquals(1.25f, GeneratorProduct.BASALT.hardness(), 1e-6);
		assertEquals(1.5f, GeneratorProduct.BLACKSTONE.hardness(), 1e-6);
		assertEquals(1.5f, GeneratorProduct.TUFF.hardness(), 1e-6);
		for (GeneratorProduct product : GeneratorProduct.values())
			assertTrue(product.hardness() > 0, product.name() + " must take time to make");
	}

	@Test
	void blueIceOnlyForBasaltAndBlackstone() {
		assertFalse(GeneratorProduct.COBBLESTONE.usesBlueIce());
		assertTrue(GeneratorProduct.BASALT.usesBlueIce());
		assertTrue(GeneratorProduct.BLACKSTONE.usesBlueIce());
		assertFalse(GeneratorProduct.TUFF.usesBlueIce());
	}

	@Test
	void spritesAreVanillaBlockTextures() {
		for (GeneratorProduct product : GeneratorProduct.values()) {
			assertEquals("minecraft", product.sprite().getNamespace(), product.name());
			assertTrue(product.sprite().getPath().startsWith("block/"), product.name());
		}
	}

	@Test
	void spriteTexturesExist() {
		for (GeneratorProduct product : GeneratorProduct.values()) {
			String texture = "assets/minecraft/textures/" + product.sprite().getPath() + ".png";
			assumeTrue(GeneratorProductTest.class.getClassLoader().getResource("assets/minecraft/textures/block/stone.png") != null,
				"vanilla assets not on the classpath");
			assertTrue(GeneratorProductTest.class.getClassLoader().getResource(texture) != null, texture);
		}
	}

	@Test
	void langKeysAreDistinctAndTranslated() {
		Path lang = ModResourceFiles.find("assets", "comedymonkeysaddons", "lang", "en_us.json");
		assumeTrue(lang != null, "en_us.json not found");
		JsonObject json = ModResourceFiles.readJson(lang).getAsJsonObject();

		Set<String> keys = new HashSet<>();
		for (GeneratorProduct product : GeneratorProduct.values()) {
			assertTrue(keys.add(product.langKey()), "duplicate " + product.langKey());
			String key = "comedymonkeysaddons.gui.goggles." + product.langKey();
			assertTrue(json.has(key), "missing " + key);
		}
		for (String state : new String[] {"off", "full", "idle", "per_minute"})
			assertTrue(json.has("comedymonkeysaddons.gui.goggles.cobble_generator." + state), state);
	}
}
