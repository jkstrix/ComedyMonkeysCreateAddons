package com.comedymonkey.createaddons.content.helperbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.testutil.ModResourceFiles;
import com.google.gson.JsonObject;

import net.minecraft.world.item.DyeColor;

/**
 * The review's data fixes: every colour's recipe keeps his name, breaking him keeps it, the goggles and settings have
 * their text, the Ponder scene no longer sends players to the ticker, and the new mixins are listed.
 */
class HelperBotResourcesTest {
	private static JsonObject json(String... path) {
		Path file = ModResourceFiles.find(path);
		assumeTrue(file != null, "resources not reachable");
		return ModResourceFiles.readJson(file).getAsJsonObject();
	}

	@Test
	void everyDyeRecipeKeepsHisName() {
		for (DyeColor dye : DyeColor.values()) {
			JsonObject recipe = json("data", "comedymonkeysaddons", "recipe", "dyed", dye.getName() + "_helper_bot.json");
			assertEquals("comedymonkeysaddons:helper_bot_dye", recipe.get("type").getAsString(), dye + " should use the name-keeping recipe");
			assertEquals(dye.getName(), recipe.getAsJsonObject("result").getAsJsonObject("components").get("comedymonkeysaddons:helper_bot_dye").getAsString());
		}
	}

	@Test
	void breakingHimKeepsHisColourAndName() {
		String loot = json("data", "comedymonkeysaddons", "loot_table", "blocks", "helper_bot.json").toString();
		assertTrue(loot.contains("comedymonkeysaddons:helper_bot_dye") && loot.contains("minecraft:custom_name"), loot);
	}

	@Test
	void theNewTextIsThere() {
		JsonObject lang = json("assets", "comedymonkeysaddons", "lang", "en_us.json");
		for (String side : new String[] { "front", "behind", "left", "right" })
			assertTrue(lang.has("comedymonkeysaddons.gui.goggles.helper_bot.shop." + side), "goggles text for a ticker " + side);
		assertFalse(lang.has("comedymonkeysaddons.gui.goggles.helper_bot.shop"), "the old sideless line is gone");
		assertTrue(lang.has("comedymonkeysaddons.configuration.helperBotVoiceCooldown"), "the voice cooldown setting's name");
		String text = lang.get("comedymonkeysaddons.ponder.helper_bot_shop.text_3").getAsString();
		assertFalse(text.contains("or the ticker"), "the ticker opens Create's category editor, not the shop: " + text);
	}

	@Test
	void theNewMixinsAreListed() {
		String mixins = json("comedymonkeysaddons.mixins.json").getAsJsonArray("mixins").toString();
		assertTrue(mixins.contains("\"RecipeApplierMixin\"") && mixins.contains("\"ContraptionMixin\""), mixins);
	}
}
