package com.comedymonkey.createaddons.advancement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.LinkedHashMap;
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

/** The Trade Master's and the Clockwork Jetpack's advancements (approved 2026-11-04) read, are named, and hang in the tree. */
class TradeMasterAndJetpackAdvancementsTest {
	private static final String NS = ComedyMonkeysCreateAddons.MOD_ID;
	/** File, then its title and parent, as approved. */
	private static final Map<String, String[]> ADVANCEMENTS = new LinkedHashMap<>();

	static {
		ADVANCEMENTS.put("trade_master", new String[] { "Middle Management", NS + ":trade_bot" });
		ADVANCEMENTS.put("trade_master_fill", new String[] { "Trade Master", NS + ":trade_master" });
		ADVANCEMENTS.put("trade_master_supply_chain", new String[] { "Supply Chain", NS + ":trade_master_fill" });
		ADVANCEMENTS.put("trade_master_book", new String[] { "Book Club", NS + ":trade_master_fill" });
		ADVANCEMENTS.put("trade_master_stocked", new String[] { "Fully Stocked", NS + ":trade_master_fill" });
		ADVANCEMENTS.put("jetpack_wound", new String[] { "Wound and Ready", NS + ":flight_instruments" });
		ADVANCEMENTS.put("jetpack_flight", new String[] { "Liftoff", NS + ":jetpack_wound" });
		ADVANCEMENTS.put("jetpack_wings", new String[] { "Clockwork Wings", NS + ":jetpack_flight" });
		ADVANCEMENTS.put("jetpack_hover", new String[] { "Station Keeping", NS + ":jetpack_flight" });
		ADVANCEMENTS.put("jetpack_run_down", new String[] { "What Goes Up", NS + ":jetpack_flight" });
		ADVANCEMENTS.put("jetpack_overwound", new String[] { "Overwound", NS + ":jetpack_flight" });
		ADVANCEMENTS.put("jetpack_lava", new String[] { "The Floor Is Lava", NS + ":jetpack_flight" });
	}

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

	/** The challenges ask for their numbers: 8 full gauges at once, Overwound III. */
	@Test
	void theChallengesAskForTheirNumbers() {
		assertEquals(ModAdvancements.TRADE_MASTER_STOCKED_GAUGES, minValue("trade_master_stocked"));
		assertEquals(3, minValue("jetpack_overwound"));
	}

	private static int minValue(String id) {
		return read("data", NS, "advancement", id + ".json").getAsJsonObject("criteria").getAsJsonObject("done").getAsJsonObject("conditions")
			.get("min_value").getAsInt();
	}
}
