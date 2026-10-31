package com.comedymonkey.createaddons.content.wastes.city;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.content.wastes.District;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The Clockwork Wastes city's world generation data (spec P2, P3, P45, P53, D13, D66) and the layout's districts: the
 * structure set places cities 32 chunks apart, separation 12, 6 chunks from End cities (D69); an outskirts-only site's
 * cache holds one Elytra; both structures (the Wastes and the
 * highlands fallback) are the city type at the surface-structures step; the Wastes biome is in the city's tag and not in
 * the End cities'; every block of the city's footprint belongs to exactly the district its quadrant says, the heart in the
 * middle.
 */
class CityWorldgenDataTest {
	private static final String NS = "comedymonkeysaddons";

	private static JsonObject json(String path) throws IOException {
		try (InputStream in = CityWorldgenDataTest.class.getResourceAsStream(path)) {
			assertNotNull(in, "missing " + path);
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	@Test
	void theStructureSetKeepsCitiesApartAndOffEndCities() throws IOException {
		JsonObject set = json("/data/" + NS + "/worldgen/structure_set/clockwork_city.json");
		JsonObject p = set.getAsJsonObject("placement");
		assertEquals("minecraft:random_spread", p.get("type").getAsString());
		// P45, D69: 32; parity with the End-city ships comes from the outskirts-only sites' own Elytra
		assertEquals(32, CityLayout.SPACING, "spacing (P45, D69)");
		assertEquals(CityLayout.SPACING, p.get("spacing").getAsInt(), "spacing in the data pack (P45, D69)");
		assertEquals(12, p.get("separation").getAsInt(), "separation (P45)");
		JsonObject ex = p.getAsJsonObject("exclusion_zone");
		assertEquals("minecraft:end_cities", ex.get("other_set").getAsString(), "the exclusion zone is against End cities (P2)");
		assertEquals(6, ex.get("chunk_count").getAsInt(), "6 chunks (P2, D13)");
		boolean wastes = false, highlands = false;
		for (JsonElement e : set.getAsJsonArray("structures")) {
			String id = e.getAsJsonObject().get("structure").getAsString();
			wastes |= id.equals(NS + ":clockwork_city");
			highlands |= id.equals(NS + ":clockwork_city_highlands");
		}
		assertTrue(wastes && highlands, "the set holds the Wastes city and the highlands fallback (P53)");
	}

	/** D69: an outskirts-only site's cache holds exactly its one Elytra and nothing else. */
	@Test
	void theResistanceCacheHoldsOneElytra() throws IOException {
		JsonObject table = json("/data/" + NS + "/loot_table/chests/clockwork_resistance_cache.json");
		assertEquals("minecraft:chest", table.get("type").getAsString());
		int elytra = 0, entries = 0;
		for (JsonElement pool : table.getAsJsonArray("pools")) {
			JsonObject p = pool.getAsJsonObject();
			assertEquals(1, p.get("rolls").getAsInt(), "one roll a pool");
			for (JsonElement e : p.getAsJsonArray("entries")) {
				entries++;
				JsonObject entry = e.getAsJsonObject();
				assertFalse(entry.has("functions"), "no count functions on the cache's Elytra");
				if (entry.get("name").getAsString().equals("minecraft:elytra"))
					elytra++;
			}
		}
		assertEquals(CityLayout.OUTSKIRTS_ELYTRA, elytra, "Elytra in the cache");
		assertEquals(elytra, entries, "the cache holds only its Elytra");
		assertEquals(NS + ":chests/clockwork_resistance_cache", CityLayout.RESISTANCE_CACHE_LOOT.location().toString());
	}

	@Test
	void bothStructuresAreTheCityAtTheSurface() throws IOException {
		JsonObject city = json("/data/" + NS + "/worldgen/structure/clockwork_city.json");
		assertEquals(NS + ":clockwork_city", city.get("type").getAsString());
		assertEquals("#" + NS + ":has_structure/clockwork_city", city.get("biomes").getAsString());
		assertEquals("surface_structures", city.get("step").getAsString());
		assertFalse(city.get("highlands").getAsBoolean());
		JsonObject fallback = json("/data/" + NS + "/worldgen/structure/clockwork_city_highlands.json");
		assertEquals(NS + ":clockwork_city", fallback.get("type").getAsString());
		assertEquals("minecraft:end_highlands", fallback.get("biomes").getAsString());
		assertTrue(fallback.get("highlands").getAsBoolean(), "the fallback only places when its config is on (P53)");
	}

	@Test
	void theWastesHasCitiesAndNoEndCities() throws IOException {
		JsonObject tag = json("/data/" + NS + "/tags/worldgen/biome/has_structure/clockwork_city.json");
		boolean found = false;
		for (JsonElement e : tag.getAsJsonArray("values"))
			found |= e.getAsString().equals(NS + ":clockwork_wastes");
		assertTrue(found, "the Wastes is in has_structure/clockwork_city");
		try (InputStream in = CityWorldgenDataTest.class.getResourceAsStream("/data/minecraft/tags/worldgen/biome/has_structure/end_city.json")) {
			if (in != null)
				assertFalse(new String(in.readAllBytes(), StandardCharsets.UTF_8).contains("clockwork_wastes"), "the Wastes is in End cities' biomes");
		}
	}

	@Test
	void everyColumnIsInOneDistrict() {
		int h = CityLayout.HEART;
		for (int x = -CityLayout.OUT; x <= CityLayout.OUT; x++)
			for (int z = -CityLayout.OUT; z <= CityLayout.OUT; z++) {
				District d = CityLayout.districtAt(x, z);
				if (Math.abs(x) <= h && Math.abs(z) <= h)
					assertEquals(District.HEART, d);
				else if (z < 0)
					assertEquals(x < 0 ? District.FOUNDRY : District.WORKSHOPS, d, x + ", " + z);
				else
					assertEquals(x < 0 ? District.QUARTERS : District.RAIL_YARD, d, x + ", " + z);
			}
	}

	@Test
	void theTestIslandHoldsMostOfTheCityButNotItsEdges() {
		int land = 0, all = 0;
		for (int cx = -4; cx <= 4; cx++)
			for (int cz = -4; cz <= 4; cz++) {
				all++;
				if (CityBuild.islandAt(cx * 16, cz * 16))
					land++;
			}
		// world generation's island-fit rule (P46): at least half the 9 x 9-chunk footprint on land
		assertTrue(land * 2 >= all, land + " of " + all + " chunks on the test island");
		assertTrue(CityBuild.islandAt(0, 0), "the heart stands on land");
		assertFalse(CityBuild.islandAt(66, 66), "the Rail Yard's far corner stands over the void (struts)");
		assertFalse(CityBuild.islandAt(0, -80), "the north boulevard runs out over the void (a bridge)");
	}
}
