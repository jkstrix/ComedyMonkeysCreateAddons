package com.comedymonkey.createaddons.content.wastes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The overgrowth (D91, D92, D94) as design/models/clockwork-wastes-overgrowth.export.js writes it: every block's state,
 * models, textures, item, name and loot; the thin plants' loot asking for shears or Silk Touch (D92), the rest dropping
 * themselves; the mining and climbing tags; Carry On kept off the thin plants and the pots and on the rest; the cutout
 * render types (leaves cutout_mipped, never translucent); the glow parts lit full-bright; the alcove's dead and living
 * vines; the Shiftcaps' light for Colorful Lighting; the Vigil Bell's dye.
 */
class OvergrowthAssetsTest {
	private static final String NS = "comedymonkeysaddons";
	/** The blocks that are items. */
	static final List<String> ITEMS = List.of("knotvine", "scale_lichen", "twinerope", "dusk_moss", "dusk_moss_carpet", "springcurl", "corkscrew_rush",
		"vigil_bell", "bramblecoil", "shiftcap", "shiftcap_shelf", "breakroot_knuckle", "breakroot_run", "breakroot_burst", "crookwood_log",
		"crookwood_leaves", "shieldcrown_log", "shieldcrown_leaves");
	static final List<String> THIN = List.of("knotvine", "scale_lichen", "twinerope", "dusk_moss_carpet", "springcurl", "corkscrew_rush",
		"crookwood_leaves", "shieldcrown_leaves");
	static final List<String> POTS = List.of("potted_springcurl", "potted_corkscrew_rush", "potted_vigil_bell", "potted_shiftcap");

	private static InputStream open(String path) {
		return OvergrowthAssetsTest.class.getResourceAsStream(path);
	}

	private static JsonObject json(String path) throws IOException {
		try (InputStream in = open(path)) {
			assertNotNull(in, "missing " + path);
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	private static String text(String path) throws IOException {
		try (InputStream in = open(path)) {
			assertNotNull(in, "missing " + path);
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static Set<String> models(JsonObject state) {
		Set<String> out = new HashSet<>();
		List<JsonElement> applies = new ArrayList<>();
		if (state.has("variants"))
			state.getAsJsonObject("variants").entrySet().forEach(e -> applies.add(e.getValue()));
		else
			state.getAsJsonArray("multipart").forEach(p -> applies.add(p.getAsJsonObject().get("apply")));
		for (JsonElement a : applies)
			if (a.isJsonArray())
				a.getAsJsonArray().forEach(x -> out.add(x.getAsJsonObject().get("model").getAsString()));
			else
				out.add(a.getAsJsonObject().get("model").getAsString());
		return out;
	}

	/** A model's own textures exist, its parent's too; it's never translucent; its unshaded parts are full-bright. */
	private static void checkModel(String ref) throws IOException {
		if (!ref.startsWith(NS + ":"))
			return;
		JsonObject m = json("/assets/" + NS + "/models/" + ref.substring(NS.length() + 1) + ".json");
		if (m.has("textures"))
			for (var e : m.getAsJsonObject("textures").entrySet()) {
				String t = e.getValue().getAsString();
				if (t.startsWith(NS + ":"))
					assertNotNull(open("/assets/" + NS + "/textures/" + t.substring(NS.length() + 1) + ".png"), ref + ": no texture " + t);
			}
		if (m.has("render_type"))
			assertFalse(m.get("render_type").getAsString().contains("translucent"), ref + " is translucent");
		if (m.has("elements"))
			for (JsonElement el : m.getAsJsonArray("elements")) {
				JsonObject o = el.getAsJsonObject();
				if (o.has("shade") && !o.get("shade").getAsBoolean())
					assertTrue(o.has("neoforge_data"), ref + ": a glow part that isn't full-bright");
			}
		if (m.has("parent"))
			checkModel(m.get("parent").getAsString());
	}

	@Test
	void everyBlockHasStatesModelsTexturesItemNameAndLoot() throws IOException {
		JsonObject lang = json("/assets/" + NS + "/lang/en_us.json");
		List<String> all = new ArrayList<>(ITEMS);
		all.add("twinerope_plant");
		all.addAll(POTS);
		for (String id : all) {
			Set<String> ms = models(json("/assets/" + NS + "/blockstates/" + id + ".json"));
			assertFalse(ms.isEmpty(), id + " draws nothing");
			for (String m : ms)
				checkModel(m);
			assertTrue(lang.has("block." + NS + "." + id), "no name for " + id);
			assertNotNull(open("/data/" + NS + "/loot_table/blocks/" + id + ".json"), "no loot for " + id);
		}
		for (String id : ITEMS) {
			JsonObject item = json("/assets/" + NS + "/models/item/" + id + ".json");
			if (item.has("parent"))
				checkModel(item.get("parent").getAsString());
			if (item.has("textures"))
				for (var e : item.getAsJsonObject("textures").entrySet())
					assertNotNull(open("/assets/" + NS + "/textures/" + e.getValue().getAsString().substring(NS.length() + 1) + ".png"), id + ": no item texture");
		}
		// leaves mipped, the rest cutout (or solid, a full block)
		for (String leaves : List.of("crookwood_leaves", "shieldcrown_leaves"))
			assertEquals("minecraft:cutout_mipped", json("/assets/" + NS + "/models/block/wastes/overgrowth/" + leaves + ".json").get("render_type").getAsString());
		assertEquals("minecraft:cutout", json("/assets/" + NS + "/models/block/wastes/overgrowth/knotvine_a.json").get("render_type").getAsString());
	}

	@Test
	void thinPlantsNeedShearsOrSilkTouchTheRestDropThemselves() throws IOException {
		for (String id : ITEMS) {
			String loot = text("/data/" + NS + "/loot_table/blocks/" + id + ".json");
			assertTrue(loot.contains("\"" + NS + ":" + id + "\""), id + " doesn't drop itself");
			boolean asks = loot.contains("shears_dig") && loot.contains("minecraft:silk_touch");
			assertEquals(THIN.contains(id), asks, id + (asks ? " asks for shears though it isn't thin" : " drops without shears though it's thin"));
		}
		assertTrue(text("/data/" + NS + "/loot_table/blocks/twinerope_plant.json").contains("\"" + NS + ":twinerope\""), "the rope's body doesn't give the rope");
		String dye = text("/data/" + NS + "/recipe/blue_dye_from_vigil_bell.json");
		assertTrue(dye.contains("minecraft:blue_dye") && dye.contains(NS + ":vigil_bell"), "no blue dye from the Vigil Bell");
	}

	@Test
	void tagsCarryOnLightAndTheAlcovesVines() throws IOException {
		String axe = text("/data/minecraft/tags/block/mineable/axe.json"), hoe = text("/data/minecraft/tags/block/mineable/hoe.json");
		for (String id : List.of("breakroot_knuckle", "breakroot_run", "breakroot_burst", "crookwood_log", "shieldcrown_log", "knotvine", "scale_lichen"))
			assertTrue(axe.contains(NS + ":" + id), id + " isn't an axe's");
		for (String id : List.of("crookwood_leaves", "shieldcrown_leaves", "dusk_moss"))
			assertTrue(hoe.contains(NS + ":" + id), id + " isn't a hoe's");
		String climb = text("/data/minecraft/tags/block/climbable.json");
		for (String id : List.of("knotvine", "twinerope", "twinerope_plant"))
			assertTrue(climb.contains(NS + ":" + id + "\""), id + " isn't climbable");
		assertFalse(climb.contains(NS + ":scale_lichen"), "Scale Lichen is climbable");
		String carry = text("/data/carryon/tags/block/block_blacklist.json");
		for (String id : List.of("knotvine", "scale_lichen", "twinerope", "twinerope_plant", "dusk_moss_carpet", "springcurl", "corkscrew_rush", "vigil_bell",
			"shiftcap", "shiftcap_shelf", "potted_shiftcap"))
			assertTrue(carry.contains("\"" + NS + ":" + id + "\""), id + " can be carried");
		for (String id : List.of("dusk_moss", "bramblecoil", "breakroot_knuckle", "crookwood_log", "crookwood_leaves", "shieldcrown_log"))
			assertFalse(carry.contains("\"" + NS + ":" + id + "\""), id + " can't be carried");
		JsonObject emitters = json("/assets/" + NS + "/light/emitters.json");
		assertTrue(emitters.has(NS + ":shiftcap") && emitters.has(NS + ":shiftcap_shelf"), "the Shiftcaps cast no coloured light");
		// blue while young, plum where the plum glow leads (the brief's edge tones)
		for (String key : List.of("shiftcap[age=young]", "shiftcap_shelf[age=young]", "shiftcap_shelf[age=mature]", "potted_shiftcap"))
			assertEquals("#4F6BB5", emitters.get(NS + ":" + key).getAsString(), key);
		for (String key : List.of("shiftcap[age=mature]", "shiftcap[age=old]", "shiftcap_shelf[age=old]"))
			assertEquals("#8A4FA0", emitters.get(NS + ":" + key).getAsString(), key);
		List<String> keys = new ArrayList<>(emitters.keySet()), sorted = new ArrayList<>(keys);
		sorted.sort(null);
		assertEquals(sorted, keys, "the emitters aren't sorted");
		String alcove = text("/assets/" + NS + "/blockstates/charging_alcove.json");
		assertTrue(alcove.contains("\"vines\": \"dead\"") && alcove.contains("\"vines\": \"living\"") && !alcove.contains("\"vines\": \"true\""),
			"the alcove's vines aren't none / dead / living");
		for (String part : List.of("middle", "top"))
			checkModel(NS + ":block/wastes/charging_alcove_vines_living_" + part);
	}
}
