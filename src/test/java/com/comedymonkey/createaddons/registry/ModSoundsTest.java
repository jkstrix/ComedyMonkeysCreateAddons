package com.comedymonkey.createaddons.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipLibrary;
import com.comedymonkey.createaddons.testutil.ModResourceFiles;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.core.registries.BuiltInRegistries;

class ModSoundsTest {
	private static final String MOD = ComedyMonkeysCreateAddons.MOD_ID;

	@Test
	void tradeBotHasEverySoundOfItsDesign() {
		assertEquals(80, ModSounds.TRADE_BOT.size());
		for (var holder : ModSounds.TRADE_BOT.values())
			assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(holder.getId()), holder.getId().toString());
	}

	@Test
	void everyClipCueHasASound() {
		for (Clip clip : ClipLibrary.get().all())
			for (Clip.Cue cue : clip.cues())
				if (!cue.event().startsWith("villager_")) // vanilla villager sounds in game
					assertNotNull(ModSounds.TRADE_BOT.get(cue.event()), clip.name() + " cues unknown sound " + cue.event());
	}

	@Test
	void everySoundHasItsFilesAndASubtitle() {
		Path sounds = ModResourceFiles.find("assets", MOD, "sounds.json");
		Path lang = ModResourceFiles.find("assets", MOD, "lang", "en_us.json");
		Assumptions.assumeTrue(sounds != null && lang != null, "resources not reachable");
		JsonObject subtitles = ModResourceFiles.readJson(lang).getAsJsonObject();
		for (Map.Entry<String, JsonElement> entry : ModResourceFiles.readJson(sounds).getAsJsonObject().entrySet()) {
			JsonObject event = entry.getValue().getAsJsonObject();
			assertTrue(subtitles.has(event.get("subtitle").getAsString()), entry.getKey() + " has no subtitle text");
			for (JsonElement file : event.getAsJsonArray("sounds")) {
				String[] id = file.getAsString().split(":");
				assertEquals(MOD, id[0]);
				assertNotNull(ModResourceFiles.find("assets", MOD, "sounds", id[1] + ".ogg"), "missing " + file.getAsString());
			}
		}
	}
}
