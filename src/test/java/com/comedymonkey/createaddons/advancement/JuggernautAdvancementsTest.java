package com.comedymonkey.createaddons.advancement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.testutil.ModResourceFiles;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import net.minecraft.advancements.Advancement;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.RegistryOps;

/** JUG-07: the Juggernaut's three advancements from D41 exist, read, are named in the lang file, and hang in the tree. */
class JuggernautAdvancementsTest {
	private static final String NS = ComedyMonkeysCreateAddons.MOD_ID;
	/** File, title, parent: the titles are the spec's own (D41). */
	private static final Map<String, String[]> ADVANCEMENTS = Map.of(
		"wound_up", new String[] { "Wound Up", NS + ":root" },
		"run_down", new String[] { "Run Down", NS + ":wound_up" },
		"tightly_wound", new String[] { "Tightly Wound", NS + ":run_down" });

	private static JsonObject read(String... path) {
		Path file = ModResourceFiles.find(path);
		assertNotNull(file, String.join("/", path) + " is missing");
		return ModResourceFiles.readJson(file).getAsJsonObject();
	}

	@Test
	void theyReadAndAreNamed() {
		RegistryOps<JsonElement> ops = VanillaRegistries.createLookup().createSerializationContext(JsonOps.INSTANCE);
		JsonObject lang = read("assets", NS, "lang", "en_us.json");
		ADVANCEMENTS.forEach((id, expect) -> {
			JsonObject json = read("data", NS, "advancement", id + ".json");
			var parsed = Advancement.CODEC.parse(ops, json);
			assertTrue(parsed.result().isPresent(), id + ": " + parsed.error().map(Object::toString).orElse(""));
			assertEquals(expect[1], json.get("parent").getAsString(), id + "'s parent");
			String key = "advancements." + NS + "." + id;
			assertEquals(expect[0], lang.get(key).getAsString(), id + "'s title");
			assertTrue(lang.has(key + ".desc"), id + " has no description");
		});
	}

	/** "Run Down" is the vanilla kill trigger on him: the killer gets it, as for any mob. */
	@Test
	void runDownIsKillingHim() {
		JsonObject done = read("data", NS, "advancement", "run_down.json").getAsJsonObject("criteria").getAsJsonObject("done");
		assertEquals("minecraft:player_killed_entity", done.get("trigger").getAsString());
		assertEquals(NS + ":clockwork_juggernaut", done.getAsJsonObject("conditions").getAsJsonObject("entity").get("type").getAsString());
	}
}
