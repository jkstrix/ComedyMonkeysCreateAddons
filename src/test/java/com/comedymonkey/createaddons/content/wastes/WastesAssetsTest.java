package com.comedymonkey.createaddons.content.wastes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The Clockwork Wastes' phase-1 assets, as design/models/clockwork-wastes.export.js writes them, checked where only a
 * client would otherwise find a fault: every blockstate's models exist, every texture they name is there, the shells'
 * OBJ models are whole (quads with real indices, UVs on the texture, each material a texture of the model), every
 * block and item has a name, and every salvage block has both loot tables.
 */
class WastesAssetsTest {
	private static final String NS = "comedymonkeysaddons";
	private static final List<String> BLOCKS = List.of("scorched_end_stone", "rusted_scorched_end_stone", "littered_scorched_end_stone",
		"void_alloy_plate", "seamed_void_alloy_plate", "void_alloy_pillar", "charging_alcove", "maker_statue", "toppled_maker_statue",
		"scrap_heap", "broken_press", "broken_mixer", "broken_deployer", "broken_mechanical_arm", "broken_crushing_wheel",
		"worker_shell", "resistance_shell", "assembly_clamp", "brass_plaque");
	private static final List<String> SALVAGE = List.of("broken_press", "broken_mixer", "broken_deployer", "broken_mechanical_arm",
		"broken_crushing_wheel", "worker_shell", "resistance_shell", "assembly_clamp");

	private static InputStream open(String path) {
		return WastesAssetsTest.class.getResourceAsStream(path);
	}

	private static JsonObject json(String path) throws IOException {
		try (InputStream in = open(path)) {
			assertNotNull(in, "missing " + path);
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	private static String modelPath(String ref) {
		String[] p = ref.split(":", 2);
		return "/assets/" + p[0] + "/models/" + p[1] + ".json";
	}

	private static void checkTextures(String model, JsonObject m) {
		if (!m.has("textures"))
			return;
		for (var e : m.getAsJsonObject("textures").entrySet()) {
			String ref = e.getValue().getAsString();
			if (ref.startsWith("#") || !ref.startsWith(NS + ":"))
				continue;
			assertNotNull(open("/assets/" + NS + "/textures/" + ref.substring(NS.length() + 1) + ".png"), model + ": no texture " + ref);
		}
	}

	@Test
	void everyBlockstateModelAndTextureExists() throws IOException {
		for (String id : BLOCKS) {
			JsonObject state = json("/assets/" + NS + "/blockstates/" + id + ".json");
			Set<String> models = new HashSet<>();
			// (the Seamed Plate and the Charging Alcove are multiparts: their lit overlays)
			if (state.has("multipart"))
				state.getAsJsonArray("multipart").forEach(p -> addModels(p.getAsJsonObject().get("apply"), models));
			else
				for (var v : state.getAsJsonObject("variants").entrySet())
					addModels(v.getValue(), models);
			assertTrue(!models.isEmpty(), id + " has no models");
			for (String ref : models)
				checkTextures(ref, json(modelPath(ref)));
			JsonObject item = json("/assets/" + NS + "/models/item/" + id + ".json");
			checkTextures("item " + id, item);
		}
	}

	/** Phase 2's powered city blocks (names and lang are checked with the rest by whoever adds them to a lang test). */
	static final List<String> CITY = List.of("substation", "city_vault", "sliding_door", "derelict_street_lamp", "derelict_wall_lamp",
		"derelict_cable", "conduit_pylon", "derelict_star_reactor", "star_reactor");
	/** The part models their renderers draw (loaded standalone). */
	private static final List<String> CITY_PARTS = List.of("substation_lever", "substation_seams", "substation_sigil", "substation_flash",
		"substation_column", "city_vault_blade", "city_vault_blade_seam", "city_vault_pip", "sliding_door_leaf_l", "sliding_door_leaf_r",
		"sliding_door_seam", "sliding_door_chain", "sliding_door_jamb_glow", "derelict_street_lamp_glow", "derelict_street_lamp_glow_leaning",
		"derelict_wall_lamp_glow",
		"conduit_pylon_crown", "conduit_pylon_fin", "conduit_pylon_fin_stub", "conduit_pylon_core", "conduit_pylon_core_cracked",
		"conduit_pylon_core_dark", "conduit_pylon_core_blaze", "conduit_pylon_seams", "conduit_pylon_groove_glow", "derelict_star_reactor_arm",
		"derelict_star_reactor_arm_seam", "derelict_star_reactor_core", "derelict_star_reactor_grooves_glow", "star_reactor_orbit",
		"star_reactor_pip", "star_reactor_ports_lit");

	private static void addModels(JsonElement e, Set<String> models) {
		if (e.isJsonArray())
			e.getAsJsonArray().forEach(x -> models.add(x.getAsJsonObject().get("model").getAsString()));
		else
			models.add(e.getAsJsonObject().get("model").getAsString());
	}

	@Test
	void cityBlockstatesPartsAndTexturesExist() throws IOException {
		for (String id : CITY) {
			JsonObject state = json("/assets/" + NS + "/blockstates/" + id + ".json");
			Set<String> models = new HashSet<>();
			if (state.has("variants"))
				for (var v : state.getAsJsonObject("variants").entrySet())
					addModels(v.getValue(), models);
			else
				state.getAsJsonArray("multipart").forEach(p -> addModels(p.getAsJsonObject().get("apply"), models));
			assertTrue(!models.isEmpty(), id + " has no models");
			for (String ref : models)
				checkTextures(ref, json(modelPath(ref)));
			JsonObject item = json("/assets/" + NS + "/models/item/" + id + ".json");
			checkTextures("item " + id, item);
			if (item.has("loader"))
				assertNotNull(open("/assets/" + NS + "/" + item.get("model").getAsString().split(":", 2)[1]), "no OBJ for item " + id);
			assertNotNull(open("/data/" + NS + "/loot_table/blocks/" + id + ".json"), "no loot table for " + id);
		}
		for (String name : CITY_PARTS)
			checkTextures(name, json("/assets/" + NS + "/models/block/wastes/" + name + ".json"));
		assertNotNull(open("/data/" + NS + "/loot_table/salvage/derelict_star_reactor.json"), "no salvage table for the Derelict Star Reactor");
	}

	@Test
	void shellObjModelsAreWhole() throws IOException {
		Set<String> materials = objMaterials();
		for (String shell : List.of("worker_shell", "resistance_shell"))
			for (String pose : shell.startsWith("worker") ? List.of("slumped", "fallen", "frozen") : List.of("slumped", "fallen", "reaching"))
				checkObj(shell + "_" + pose, materials);
	}

	private static Set<String> objMaterials() throws IOException {
		Set<String> materials = new HashSet<>();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(open("/assets/" + NS + "/models/block/wastes/wastes_shells.mtl"), StandardCharsets.UTF_8))) {
			for (String line; (line = r.readLine()) != null;)
				if (line.startsWith("newmtl "))
					materials.add(line.substring(7).trim());
		}
		return materials;
	}

	/**
	 * A baked OBJ model is whole: its model JSON loads it through NeoForge's OBJ loader, its faces are quads pointing at
	 * real vertices, its UVs lie on the texture, each material is in the shared .mtl and is a texture of the model, and no
	 * vertex is more than a block or so outside its own. Returns its vertices (block units).
	 */
	private static List<float[]> checkObj(String name, Set<String> materials) throws IOException {
		JsonObject model = json("/assets/" + NS + "/models/block/wastes/" + name + ".json");
		assertEquals("neoforge:obj", model.get("loader").getAsString(), name);
		Map<String, String> textures = new HashMap<>();
		model.getAsJsonObject("textures").entrySet().forEach(e -> textures.put(e.getKey(), e.getValue().getAsString()));
		List<float[]> v = new ArrayList<>(), vt = new ArrayList<>();
		int vn = 0, faces = 0;
		String path = model.get("model").getAsString().split(":", 2)[1];
		try (BufferedReader r = new BufferedReader(new InputStreamReader(open("/assets/" + NS + "/" + path), StandardCharsets.UTF_8))) {
			for (String line; (line = r.readLine()) != null;) {
				String[] t = line.trim().split("\\s+");
				switch (t[0]) {
					case "v" -> v.add(new float[] { Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3]) });
					case "vt" -> {
						float[] uv = { Float.parseFloat(t[1]), Float.parseFloat(t[2]) };
						assertTrue(uv[0] >= 0 && uv[0] <= 1 && uv[1] >= 0 && uv[1] <= 1, name + ": UV off the texture " + line);
						vt.add(uv);
					}
					case "vn" -> vn++;
					case "usemtl" -> {
						assertTrue(materials.contains(t[1]), name + ": no material " + t[1]);
						assertTrue(textures.containsKey(t[1]), name + ": the model names no texture " + t[1]);
					}
					case "f" -> {
						assertEquals(5, t.length, name + ": faces are quads");
						for (int k = 1; k < 5; k++) {
							String[] i = t[k].split("/");
							assertTrue(Integer.parseInt(i[0]) <= v.size() && Integer.parseInt(i[1]) <= vt.size() && Integer.parseInt(i[2]) <= vn,
								name + ": a face points past its vertices: " + line);
						}
						faces++;
					}
					default -> {}
				}
			}
		}
		assertTrue(faces > 50, name + " has only " + faces + " faces");
		// in block units, reaching at most a block past its own (a fallen one lies across two)
		for (float[] p : v)
			for (float c : p)
				assertTrue(c > -2 && c < 3, name + ": a vertex far outside the block: " + c);
		checkTextures(name, model);
		return v;
	}

	@Test
	void everythingHasANameAndLoot() throws IOException {
		JsonObject lang = json("/assets/" + NS + "/lang/en_us.json");
		for (String id : BLOCKS) {
			assertTrue(lang.has("block." + NS + "." + id), "no name for " + id);
			assertNotNull(open("/data/" + NS + "/loot_table/blocks/" + id + ".json"), "no loot table for " + id);
		}
		for (String id : SALVAGE) {
			assertNotNull(open("/data/" + NS + "/loot_table/salvage/" + id + ".json"), "no salvage table for " + id);
			assertTrue(lang.has("block." + NS + "." + id + ".tooltip.summary"), "no tooltip for " + id);
		}
		for (String id : List.of("clockwork_scrap", "unknown_device")) {
			assertTrue(lang.has("item." + NS + "." + id), "no name for " + id);
			assertTrue(lang.has("item." + NS + "." + id + ".tooltip.summary"), "no tooltip for " + id);
			assertNotNull(open("/assets/" + NS + "/textures/item/" + id + ".png"), "no icon for " + id);
		}
		assertTrue(lang.has("biome." + NS + ".clockwork_wastes"), "no name for the biome");
		// the powered city: names, tooltips (Create's summaries are registered for all of them), the goggles and districts
		for (String id : CITY) {
			assertTrue(lang.has("block." + NS + "." + id), "no name for " + id);
			assertTrue(lang.has("block." + NS + "." + id + ".tooltip.summary"), "no tooltip for " + id);
		}
		for (String d : List.of("foundry", "workshops", "rail_yard", "quarters", "heart"))
			assertTrue(lang.has(NS + ".wastes.district." + d), "no name for the district " + d);
		for (String key : List.of("wastes.wakes", "wastes.vault_sealed", "wastes.heart_vault_sealed", "gui.goggles.substation",
			"gui.goggles.substation.charge", "gui.goggles.substation.fed", "gui.goggles.substation.unfed", "gui.goggles.star_reactor",
			"gui.goggles.star_reactor.running", "gui.goggles.star_reactor.left", "gui.goggles.star_reactor.overhauled",
			"gui.goggles.derelict_star_reactor"))
			assertTrue(lang.has(NS + "." + key), "no text for " + key);
		for (String a : List.of("wastes_lights_on", "wastes_city_of_lights", "wastes_star_power")) {
			assertTrue(lang.has("advancements." + NS + "." + a), "no title for the advancement " + a);
			assertNotNull(open("/data/" + NS + "/advancement/" + a + ".json"), "no advancement " + a);
		}
		for (String table : List.of("clockwork_city_vault", "clockwork_city_heart_vault"))
			assertNotNull(open("/data/" + NS + "/loot_table/chests/" + table + ".json"), "no loot table " + table);
		assertNotNull(open("/data/" + NS + "/recipe/mechanical_crafting/star_reactor.json"), "no Star Reactor recipe");
	}

	// ---------------------------------------------------------------- the lifts (P49, D40)
	static final List<String> LIFT = List.of("lift", "lift_call_plate");
	/** The part models LiftRenderers draws. */
	private static final List<String> LIFT_PARTS = List.of("lift_deck_w", "lift_deck_e", "lift_deck_glow_w", "lift_deck_glow_e", "lift_head_nw",
		"lift_head_ne", "lift_head_sw", "lift_head_se", "lift_guides_w", "lift_guides_e", "lift_guide_seams_w", "lift_guide_seams_e",
		"lift_call_plate_lens");

	@Test
	void liftModelsNamesLootAndTags() throws IOException {
		JsonObject lang = json("/assets/" + NS + "/lang/en_us.json");
		String pick = new String(open("/data/minecraft/tags/block/mineable/pickaxe.json").readAllBytes());
		String carry = new String(open("/data/carryon/tags/block/block_blacklist.json").readAllBytes());
		for (String id : LIFT) {
			JsonObject state = json("/assets/" + NS + "/blockstates/" + id + ".json");
			Set<String> models = new HashSet<>();
			for (var v : state.getAsJsonObject("variants").entrySet())
				addModels(v.getValue(), models);
			assertTrue(!models.isEmpty(), id + " has no models");
			for (String ref : models)
				checkTextures(ref, json(modelPath(ref)));
			// its item is a flat icon (D65), item/<id>.png
			JsonObject item = json("/assets/" + NS + "/models/item/" + id + ".json");
			assertEquals("minecraft:item/generated", item.get("parent").getAsString(), id + ": its item should be a flat icon");
			assertEquals(NS + ":item/" + id, item.getAsJsonObject("textures").get("layer0").getAsString(), id + ": its icon");
			assertNotNull(open("/assets/" + NS + "/textures/item/" + id + ".png"), "no icon for " + id);
			assertNotNull(open("/data/" + NS + "/loot_table/blocks/" + id + ".json"), "no loot table for " + id);
			assertTrue(lang.has("block." + NS + "." + id), "no name for " + id);
			assertTrue(lang.has("block." + NS + "." + id + ".tooltip.summary"), "no tooltip for " + id);
			assertTrue(pick.contains("\"" + NS + ":" + id + "\""), id + " isn't mined with a pickaxe");
			assertTrue(carry.contains("\"" + NS + ":" + id + "\""), id + " can be carried off by Carry On");
		}
		for (String name : LIFT_PARTS)
			checkTextures(name, json("/assets/" + NS + "/models/block/wastes/" + name + ".json"));
		assertTrue(lang.has("entity." + NS + ".lift_deck"), "no name for the lift's deck");
		assertTrue(lang.has(NS + ".wastes.lift_dark"), "no text for a dark lift");
	}

	// ---------------------------------------------------------------- the wall vent (D57, D59, D60)
	/** The part models WastesRenderers.vent draws: the hatch (an OBJ: its louvres are turned 35 degrees) and its glows. */
	private static final List<String> VENT_PARTS = List.of("wall_vent_hatch", "wall_vent_seam", "wall_vent_flash", "wall_vent_contact", "wall_vent_glint");

	@Test
	void wallVentModelsNamesLootAndTags() throws IOException {
		JsonObject lang = json("/assets/" + NS + "/lang/en_us.json");
		String pick = new String(open("/data/minecraft/tags/block/mineable/pickaxe.json").readAllBytes());
		String carry = new String(open("/data/carryon/tags/block/block_blacklist.json").readAllBytes());
		JsonObject state = json("/assets/" + NS + "/blockstates/wall_vent.json");
		// every facing and state has its model
		for (String f : List.of("north", "east", "south", "west"))
			for (String st : List.of("sealed", "live", "open"))
				assertTrue(state.getAsJsonObject("variants").has("facing=" + f + ",state=" + st), "no wall vent model for " + f + " " + st);
		Set<String> models = new HashSet<>();
		for (var v : state.getAsJsonObject("variants").entrySet())
			addModels(v.getValue(), models);
		for (String ref : models)
			checkTextures(ref, json(modelPath(ref)));
		checkTextures("item wall_vent", json("/assets/" + NS + "/models/item/wall_vent.json"));
		for (String name : VENT_PARTS)
			checkTextures(name, json("/assets/" + NS + "/models/block/wastes/" + name + ".json"));
		// the OBJs and the material file they share, with a material for each texture they use
		String mtl = new String(open("/assets/" + NS + "/models/block/wastes/wastes_shells.mtl").readAllBytes());
		for (String obj : List.of("wall_vent_hatch", "wall_vent_item")) {
			InputStream in = open("/assets/" + NS + "/models/block/wastes/" + obj + ".obj");
			assertNotNull(in, "no " + obj + ".obj");
			for (String line : new String(in.readAllBytes()).split("\n"))
				if (line.startsWith("usemtl "))
					assertTrue(mtl.contains("newmtl " + line.substring(7).trim()), obj + ": no material " + line);
		}
		assertNotNull(open("/data/" + NS + "/loot_table/blocks/wall_vent.json"), "no loot table for the wall vent");
		assertTrue(lang.has("block." + NS + ".wall_vent") && lang.has("block." + NS + ".wall_vent.tooltip.summary"), "no name or tooltip for the wall vent");
		assertTrue(pick.contains("\"" + NS + ":wall_vent\""), "the wall vent isn't mined with a pickaxe");
		assertTrue(carry.contains("\"" + NS + ":wall_vent\""), "the wall vent can be carried off by Carry On");
	}

	// ---------------------------------------------------------------- the Derelict Insulator (D61, D64)
	/** The textures its span renderer draws with (no block model uses the span ones, so only the exporter keeps them). */
	private static final List<String> INSULATOR_SPAN_TEXTURES = List.of("span_side", "span_side_worn", "span_top", "span_top_worn", "span_break",
		"flash", "ring_lit");

	/**
	 * Every mount, facing and lit state draws exactly its mount's body and, when lit, its mount's neck ring (emissive in the
	 * block model), turned to its facing (west as drawn: the wall mount's wall east); the span textures, item, loot and
	 * pickaxe tag are there.
	 */
	@Test
	void derelictInsulatorModelsAndSpanTextures() throws IOException {
		JsonObject state = json("/assets/" + NS + "/blockstates/derelict_insulator.json");
		Map<String, Integer> turn = Map.of("west", 0, "north", 90, "east", 180, "south", 270);
		String prefix = NS + ":block/wastes/derelict_insulator_";
		for (String mount : List.of("top", "wall", "hanging"))
			for (String facing : List.of("north", "east", "south", "west"))
				for (String lit : List.of("false", "true")) {
					Map<String, String> st = Map.of("mount", mount, "facing", facing, "lit", lit);
					List<String> bodies = new ArrayList<>(), rings = new ArrayList<>();
					for (JsonElement p : state.getAsJsonArray("multipart")) {
						JsonObject part = p.getAsJsonObject();
						if (!applies(part, st))
							continue;
						JsonObject apply = part.getAsJsonObject("apply");
						String model = apply.get("model").getAsString();
						(model.endsWith("_ring") ? rings : bodies).add(model);
						int y = apply.has("y") ? apply.get("y").getAsInt() : 0;
						assertEquals((int) turn.get(facing), y,"insulator " + st + ": " + model + " turned");
					}
					assertEquals(List.of(prefix + mount), bodies, "insulator " + st + ": body models");
					assertEquals(lit.equals("true") ? List.of(prefix + mount + "_ring") : List.of(), rings, "insulator " + st + ": ring models");
				}
		for (String mount : List.of("top", "wall", "hanging")) {
			checkTextures(mount, json(modelPath(prefix + mount)));
			JsonObject ring = json(modelPath(prefix + mount + "_ring"));
			checkTextures(mount + " ring", ring);
			assertTrue(ring.getAsJsonArray("elements").size() > 0, mount + ": an empty ring");
			// lit full-bright in the block model itself, as the Derelict Cable's line
			for (JsonElement e : ring.getAsJsonArray("elements")) {
				JsonObject el = e.getAsJsonObject();
				assertTrue(el.has("shade") && !el.get("shade").getAsBoolean(), mount + " ring: shaded " + el.get("name"));
				assertEquals(15, el.getAsJsonObject("neoforge_data").get("block_light").getAsInt(), mount + " ring: not emissive " + el.get("name"));
			}
		}
		checkTextures("item derelict_insulator", json("/assets/" + NS + "/models/item/derelict_insulator.json"));
		for (String tex : INSULATOR_SPAN_TEXTURES)
			assertNotNull(open("/assets/" + NS + "/textures/block/wastes/" + tex + ".png"), "no span texture " + tex);
		assertNotNull(open("/data/" + NS + "/loot_table/blocks/derelict_insulator.json"), "no loot table for the Derelict Insulator");
		String pick = new String(open("/data/minecraft/tags/block/mineable/pickaxe.json").readAllBytes());
		assertTrue(pick.contains("\"" + NS + ":derelict_insulator\""), "the Derelict Insulator isn't mined with a pickaxe");
		// city-linked like the lift and vent, so Carry On leaves it (D69)
		String carry = new String(open("/data/carryon/tags/block/block_blacklist.json").readAllBytes());
		assertTrue(carry.contains("\"" + NS + ":derelict_insulator\""), "the Derelict Insulator can be carried off by Carry On");
	}

	/** D46, D59 (option A): the City Vault's own band texture, the inside of the kit's 14 px band. */
	@Test
	void cityVaultBandExists() throws IOException {
		try (InputStream in = open("/assets/" + NS + "/textures/gui/city_vault_band.png")) {
			assertNotNull(in, "no city_vault_band.png");
			java.awt.image.BufferedImage band = javax.imageio.ImageIO.read(in);
			assertEquals(174, band.getWidth());
			assertEquals(12, band.getHeight());
			// its foot is the lit starlight seam (#7f76a8), stopping short of the corners
			assertEquals(0x7f76a8, band.getRGB(87, 11) & 0xFFFFFF, "the band's seam");
			assertTrue((band.getRGB(1, 11) & 0xFFFFFF) != 0x7f76a8, "the seam should stop short of the corner");
		}
	}

	/** A multipart part applies to a state (its "when": each key one of its "|" values, or any of an OR list). */
	private static boolean applies(JsonObject part, Map<String, String> state) {
		if (!part.has("when"))
			return true;
		JsonObject when = part.getAsJsonObject("when");
		if (when.has("OR")) {
			for (JsonElement e : when.getAsJsonArray("OR"))
				if (matches(e.getAsJsonObject(), state))
					return true;
			return false;
		}
		return matches(when, state);
	}

	private static boolean matches(JsonObject when, Map<String, String> state) {
		for (var e : when.entrySet())
			if (!List.of(e.getValue().getAsString().split("\\|")).contains(state.get(e.getKey())))
				return false;
		return true;
	}

	/**
	 * The Derelict Cable on every side of its block (a wall or ceiling run as the floor's, turned): for each surface, each
	 * arm along it draws one arm (and one line when lit), and no arm into or out of the surface is ever drawn; the
	 * leaning street lamp (D41) has exactly one model for every state.
	 */
	@Test
	void cableOnEverySideAndLeaningLampsHaveModels() throws IOException {
		JsonObject cable = json("/assets/" + NS + "/blockstates/derelict_cable.json");
		String[] dirs = { "north", "east", "south", "west", "up", "down" };
		Map<String, String> opposite = Map.of("north", "south", "south", "north", "east", "west", "west", "east", "up", "down", "down", "up");
		for (String surface : dirs)
			for (String arm : dirs) {
				boolean along = !arm.equals(surface) && !arm.equals(opposite.get(surface));
				Map<String, String> st = new HashMap<>(Map.of("surface", surface, "lit", "true", "girder", "none"));
				for (String d : dirs)
					st.put(d, String.valueOf(d.equals(arm)));
				int arms = 0, glows = 0;
				for (JsonElement p : cable.getAsJsonArray("multipart")) {
					JsonObject part = p.getAsJsonObject();
					if (!applies(part, st))
						continue;
					String model = part.getAsJsonObject("apply").get("model").getAsString();
					if (model.contains("arm_glow"))
						glows++;
					else if (model.contains("arm"))
						arms++;
				}
				assertEquals(along ? 1 : 0, arms, "cable on " + surface + ", arm " + arm + ": arm models");
				assertEquals(along ? 1 : 0, glows, "cable on " + surface + ", arm " + arm + ": lit line models");
			}
		JsonObject lamp = json("/assets/" + NS + "/blockstates/derelict_street_lamp.json").getAsJsonObject("variants");
		for (String facing : List.of("north", "east", "south", "west"))
			for (String part : List.of("foot", "pole", "head"))
				for (String snapped : List.of("false", "true"))
					for (String leaning : List.of("false", "true")) {
						Map<String, String> st = Map.of("facing", facing, "part", part, "snapped", snapped, "leaning", leaning);
						long hits = lamp.keySet().stream().filter(k -> {
							for (String kv : k.split(",")) {
								String[] p = kv.split("=");
								if (!p[1].equals(st.get(p[0])))
									return false;
							}
							return true;
						}).count();
						assertEquals(1, hits, "street lamp " + st + ": variants");
					}
		for (String m : List.of("derelict_street_lamp_foot_leaning", "derelict_street_lamp_pole_leaning", "derelict_street_lamp_head_leaning",
			"derelict_street_lamp_head_snapped_leaning", "derelict_cable_arm_south", "derelict_cable_arm_glow_south"))
			checkTextures(m, json("/assets/" + NS + "/models/block/wastes/" + m + ".json"));
	}

	// ---------------------------------------------------------------- the city props (D72)
	private static final Map<String, Integer> FACING_Y = Map.of("north", 0, "east", 90, "south", 180, "west", 270);

	/** The models (with their y turns, "model@y") a multipart blockstate draws for a state. */
	private static List<String> modelsFor(JsonObject state, Map<String, String> st) {
		List<String> out = new ArrayList<>();
		for (JsonElement p : state.getAsJsonArray("multipart")) {
			JsonObject part = p.getAsJsonObject();
			if (!applies(part, st))
				continue;
			JsonObject apply = part.getAsJsonObject("apply");
			out.add(apply.get("model").getAsString().replace(NS + ":block/wastes/", "") + "@" + (apply.has("y") ? apply.get("y").getAsInt() : 0));
		}
		out.sort(null);
		return out;
	}

	private static List<String> sorted(List<String> l) {
		List<String> out = new ArrayList<>(l);
		out.sort(null);
		return out;
	}

	private static List<String> elementNames(JsonObject model) {
		List<String> out = new ArrayList<>();
		model.getAsJsonArray("elements").forEach(e -> out.add(e.getAsJsonObject().has("name") ? e.getAsJsonObject().get("name").getAsString() : ""));
		return out;
	}

	private static double maxCoord(JsonObject model, int axis) {
		double max = Double.NEGATIVE_INFINITY;
		for (JsonElement e : model.getAsJsonArray("elements"))
			max = Math.max(max, e.getAsJsonObject().getAsJsonArray("to").get(axis).getAsDouble());
		return max;
	}

	private static void assertEmissive(String name, JsonObject model) {
		assertTrue(model.getAsJsonArray("elements").size() > 0, name + ": empty");
		for (JsonElement e : model.getAsJsonArray("elements")) {
			JsonObject el = e.getAsJsonObject();
			assertTrue(el.has("shade") && !el.get("shade").getAsBoolean(), name + ": shaded " + el.get("name"));
			assertEquals(15, el.getAsJsonObject("neoforge_data").get("block_light").getAsInt(), name + ": not emissive " + el.get("name"));
		}
	}

	/**
	 * The Assembly Clamp (D72): a baked OBJ for each of its five stages, whole, the tallest two reaching past a block
	 * model's 32 px (why it's an OBJ) and the first three not; every facing and stage drawn, turned; its flat icon (16 x 16,
	 * outlined); salvaged like the shells (both loot tables, in the salvage tag) and kept off Carry On.
	 */
	@Test
	void assemblyClampModelsIconAndData() throws IOException {
		JsonObject variants = json("/assets/" + NS + "/blockstates/assembly_clamp.json").getAsJsonObject("variants");
		Set<String> materials = objMaterials();
		for (int stage = 1; stage <= 5; stage++) {
			String name = "assembly_clamp_" + stage;
			double top = 0;
			for (float[] p : checkObj(name, materials))
				top = Math.max(top, p[1]);
			if (stage >= 4)
				assertTrue(top > 2.0, name + " should stand past 32 px (" + top * 16 + " px)");
			else
				assertTrue(top <= 2.0, name + " stands " + top * 16 + " px");
			for (String facing : List.of("north", "east", "south", "west")) {
				JsonObject v = variants.getAsJsonObject("facing=" + facing + ",stage=" + stage);
				assertNotNull(v, "no Assembly Clamp model for " + facing + " stage " + stage);
				assertEquals(NS + ":block/wastes/" + name, v.get("model").getAsString());
				assertEquals((int) FACING_Y.get(facing), v.has("y") ? v.get("y").getAsInt() : 0, facing + " stage " + stage + ": turned");
			}
		}
		assertEquals(20, variants.size(), "Assembly Clamp variants");
		JsonObject item = json("/assets/" + NS + "/models/item/assembly_clamp.json");
		assertEquals("minecraft:item/generated", item.get("parent").getAsString(), "the Assembly Clamp's item should be a flat icon");
		try (InputStream in = open("/assets/" + NS + "/textures/item/assembly_clamp.png")) {
			assertNotNull(in, "no icon for the Assembly Clamp");
			java.awt.image.BufferedImage icon = javax.imageio.ImageIO.read(in);
			assertEquals(16, icon.getWidth());
			assertEquals(16, icon.getHeight());
			boolean outline = false;
			for (int x = 0; x < 16; x++)
				for (int y = 0; y < 16; y++)
					outline |= (icon.getRGB(x, y) & 0xFFFFFFFF) == 0xFF121118;
			assertTrue(outline, "the icon has no #121118 outline");
		}
		String salvage = new String(open("/data/" + NS + "/tags/block/salvage.json").readAllBytes());
		assertTrue(salvage.contains("\"" + NS + ":assembly_clamp\""), "the Assembly Clamp isn't in the salvage tag");
		String table = new String(open("/data/" + NS + "/loot_table/salvage/assembly_clamp.json").readAllBytes());
		assertTrue(table.contains(NS + ":clockwork_scrap") && table.contains("create:electron_tube"), "the clamp's salvage isn't the shells'");
		String carry = new String(open("/data/carryon/tags/block/block_blacklist.json").readAllBytes());
		assertFalse(carry.contains("\"" + NS + ":assembly_clamp\""), "the Assembly Clamp can't be carried, unlike the shells (D74)");
		String pick = new String(open("/data/minecraft/tags/block/mineable/pickaxe.json").readAllBytes());
		assertTrue(pick.contains("\"" + NS + ":assembly_clamp\"") && pick.contains("\"" + NS + ":brass_plaque\""), "the props aren't mined with a pickaxe");
	}

	/**
	 * The Charging Alcove (D72): its middle block has the corner shelf and no ledge or toy cart; for every facing, part,
	 * trinket, vines and lit state it draws exactly its part, its lit contact when lit, its trinket in the middle block and
	 * its vines in the middle and top, all turned to its facing.
	 */
	@Test
	void alcoveShelfTrinketsAndVines() throws IOException {
		JsonObject state = json("/assets/" + NS + "/blockstates/charging_alcove.json");
		for (String facing : List.of("north", "east", "south", "west"))
			for (String part : List.of("bottom", "middle", "top"))
				for (String trinket : List.of("none", "cog", "key", "cart", "flower"))
					for (String vines : List.of("false", "true"))
						for (String lit : List.of("false", "true")) {
							Map<String, String> st = Map.of("facing", facing, "part", part, "trinket", trinket, "vines", vines, "lit", lit);
							int y = FACING_Y.get(facing);
							List<String> want = new ArrayList<>(List.of("charging_alcove_" + part + "@" + y));
							if (lit.equals("true"))
								want.add("charging_alcove_contact_" + part + "@" + y);
							if (part.equals("middle") && !trinket.equals("none"))
								want.add("charging_alcove_trinket_" + trinket + "@" + y);
							if (vines.equals("true") && !part.equals("bottom"))
								want.add("charging_alcove_vines_" + part + "@" + y);
							assertEquals(sorted(want), modelsFor(state, st), "alcove " + st);
						}
		List<String> middle = elementNames(json(modelPath(NS + ":block/wastes/charging_alcove_middle")));
		assertTrue(middle.contains("shelf"), "the alcove has no corner shelf");
		assertTrue(middle.stream().noneMatch(n -> n.equals("ledge") || n.startsWith("cart")), "the alcove still has its ledge or cart: " + middle);
		for (String m : List.of("cog", "key", "cart", "flower"))
			checkTextures(m, json(modelPath(NS + ":block/wastes/charging_alcove_trinket_" + m)));
		for (String part : List.of("middle", "top")) {
			JsonObject vines = json(modelPath(NS + ":block/wastes/charging_alcove_vines_" + part));
			checkTextures("vines " + part, vines);
			assertTrue(vines.getAsJsonObject("textures").toString().contains("cp_vine"), part + ": the vines aren't drawn with the vine cards");
		}
	}

	/** The Brass Plaque (D72): square and askew for every facing, turned; the askew one hangs from its one bolt left. */
	@Test
	void brassPlaqueModelsItemAndLoot() throws IOException {
		JsonObject variants = json("/assets/" + NS + "/blockstates/brass_plaque.json").getAsJsonObject("variants");
		for (String facing : List.of("north", "east", "south", "west"))
			for (String askew : List.of("false", "true")) {
				JsonObject v = variants.getAsJsonObject("facing=" + facing + ",askew=" + askew);
				assertNotNull(v, "no plaque model for " + facing + " askew " + askew);
				assertEquals(NS + ":block/wastes/brass_plaque" + (askew.equals("true") ? "_askew" : ""), v.get("model").getAsString());
				assertEquals((int) FACING_Y.get(facing), v.has("y") ? v.get("y").getAsInt() : 0, "plaque " + facing + ": turned");
			}
		List<String> square = elementNames(json(modelPath(NS + ":block/wastes/brass_plaque")));
		JsonObject askewModel = json(modelPath(NS + ":block/wastes/brass_plaque_askew"));
		List<String> askew = elementNames(askewModel);
		assertEquals(3, square.stream().filter(n -> n.startsWith("bolt_")).count(), "the plaque has three bolts, one lost");
		assertEquals(List.of("bolt_nw"), askew.stream().filter(n -> n.startsWith("bolt_")).toList(), "the askew plaque hangs from one bolt");
		for (JsonElement e : askewModel.getAsJsonArray("elements"))
			assertEquals(-22.5, e.getAsJsonObject().getAsJsonObject("rotation").get("angle").getAsDouble(), "askew: every part hangs at 22.5");
		checkTextures("item brass_plaque", json("/assets/" + NS + "/models/item/brass_plaque.json"));
		String loot = new String(open("/data/" + NS + "/loot_table/blocks/brass_plaque.json").readAllBytes());
		assertTrue(loot.contains("\"" + NS + ":brass_plaque\""), "the plaque doesn't drop itself");
	}

	/**
	 * The mast riser (D72): Derelict Cable with girder foot, run or top draws exactly its riser part (and its lit line when
	 * lit), turned so the girder is on its surface side, and none of the floor, wall or ceiling run's models; the foot also
	 * draws a floor arm toward each floor cable it joins, and the knot where one meets its run at a corner. Its models reach
	 * into the girder's cell (z past 16 with the girder south), the top's jumper up into the next block, and each run block
	 * has its clamp.
	 */
	@Test
	void mastRiserModels() throws IOException {
		JsonObject state = json("/assets/" + NS + "/blockstates/derelict_cable.json");
		Map<String, Integer> girderY = Map.of("south", 0, "west", 90, "north", 180, "east", 270);
		Map<String, String> opposite = Map.of("north", "south", "south", "north", "east", "west", "west", "east");
		String[] dirs = { "north", "east", "south", "west", "up", "down" };
		for (String surface : girderY.keySet())
			for (String part : List.of("foot", "run", "top"))
				for (String lit : List.of("false", "true"))
					for (String arm : new String[] { "", "north", "east", "south", "west" }) {
						if (arm.equals(surface))
							continue;
						Map<String, String> st = new HashMap<>(Map.of("surface", surface, "girder", part, "lit", lit));
						for (String d : dirs)
							st.put(d, String.valueOf(d.equals(arm)));
						int y = girderY.get(surface);
						List<String> want = new ArrayList<>(List.of("derelict_cable_riser_" + part + "@" + y));
						if (lit.equals("true"))
							want.add("derelict_cable_riser_" + part + "_glow@" + y);
						List<String> got = modelsFor(state, st);
						if (part.equals("foot") && !arm.isEmpty()) {
							// one floor arm (and its line when lit) toward the cable it joins; a knot at a corner
							assertEquals(1, got.stream().filter(m -> m.startsWith("derelict_cable_arm_") || m.startsWith("derelict_cable_arm@")).filter(m -> !m.contains("glow")).count(),
								"riser foot " + st + ": floor arms " + got);
							assertEquals(lit.equals("true") ? 1 : 0, got.stream().filter(m -> m.contains("arm_glow")).count(), "riser foot " + st + ": arm lines " + got);
							assertEquals(arm.equals(opposite.get(surface)) ? 0 : 1, got.stream().filter(m -> m.startsWith("derelict_cable_knot")).count(),
								"riser foot " + st + ": knot " + got);
							got = got.stream().filter(m -> m.startsWith("derelict_cable_riser")).toList();
						}
						assertEquals(sorted(want), got, "riser " + st);
					}
		JsonObject foot = json(modelPath(NS + ":block/wastes/derelict_cable_riser_foot")), run = json(modelPath(NS + ":block/wastes/derelict_cable_riser_run")),
			top = json(modelPath(NS + ":block/wastes/derelict_cable_riser_top"));
		for (var e : Map.of("foot", foot, "run", run, "top", top).entrySet()) {
			checkTextures("riser " + e.getKey(), e.getValue());
			assertTrue(maxCoord(e.getValue(), 2) > 16, "the riser's " + e.getKey() + " doesn't reach into the girder's cell");
			assertEmissive("riser " + e.getKey() + " glow", json(modelPath(NS + ":block/wastes/derelict_cable_riser_" + e.getKey() + "_glow")));
		}
		assertTrue(elementNames(run).stream().anyMatch(n -> n.contains("clamp")), "a riser run has no clamp");
		assertTrue(elementNames(foot).stream().anyMatch(n -> n.startsWith("guard_")), "the riser's foot has no guard");
		assertTrue(elementNames(top).stream().anyMatch(n -> n.startsWith("pot_")) && maxCoord(top, 1) > 16, "the riser's top has no pothead or jumper up to the insulator");
	}

	// ---------------------------------------------------------------- D76: the cable's knot clears every arm

	/**
	 * A model's elements as boxes {x0, y0, z0, x1, y1, z1, then 1 or 0 for each face drawn: west, down, north, east, up, south},
	 * turned about y by a blockstate's y (vanilla: Ry(-y) about the middle, so y 90 takes north to east).
	 */
	private static List<double[]> boxes(JsonObject model, int y) {
		List<double[]> out = new ArrayList<>();
		double a = Math.toRadians(-y), c = Math.round(Math.cos(a)), s = Math.round(Math.sin(a));
		for (JsonElement e : model.getAsJsonArray("elements")) {
			JsonObject el = e.getAsJsonObject();
			assertFalse(el.has("rotation"), "a turned element in a cable model");
			double[] f = new double[3], t = new double[3];
			for (int i = 0; i < 3; i++) {
				f[i] = el.getAsJsonArray("from").get(i).getAsDouble();
				t[i] = el.getAsJsonArray("to").get(i).getAsDouble();
			}
			// both corners turned (x' = x cos + z sin, z' = -x sin + z cos about 8, 8), then ordered
			double fx = (f[0] - 8) * c + (f[2] - 8) * s + 8, fz = -(f[0] - 8) * s + (f[2] - 8) * c + 8;
			double tx = (t[0] - 8) * c + (t[2] - 8) * s + 8, tz = -(t[0] - 8) * s + (t[2] - 8) * c + 8;
			List<String> ring = List.of("north", "east", "south", "west");
			Set<String> faces = new HashSet<>();
			for (String d : el.getAsJsonObject("faces").keySet())
				faces.add(ring.contains(d) ? ring.get((ring.indexOf(d) + y / 90) % 4) : d);
			double[] b = { Math.min(fx, tx), f[1], Math.min(fz, tz), Math.max(fx, tx), t[1], Math.max(fz, tz), 0, 0, 0, 0, 0, 0 };
			String[] order = { "west", "down", "north", "east", "up", "south" };
			for (int i = 0; i < 6; i++)
				b[6 + i] = faces.contains(order[i]) ? 1 : 0;
			out.add(b);
		}
		return out;
	}

	/** Two boxes with drawn faces on (or within 0.2 px of) the same plane, overlapping: they z-fight. */
	private static boolean shareFace(double[] p, double[] q) {
		for (int ax = 0; ax < 3; ax++) {
			int u = (ax + 1) % 3, v = (ax + 2) % 3;
			double ou = Math.min(p[u + 3], q[u + 3]) - Math.max(p[u], q[u]), ov = Math.min(p[v + 3], q[v + 3]) - Math.max(p[v], q[v]);
			if (ou <= 1e-6 || ov <= 1e-6)
				continue;
			for (int i : new int[] { ax, ax + 3 })
				for (int j : new int[] { ax, ax + 3 })
					if (p[6 + i] > 0 && q[6 + j] > 0 && Math.abs(p[i] - q[j]) < 0.2)
						return true;
		}
		return false;
	}

	/**
	 * D74/D76: the Derelict Cable's corner knot is 2.5 px wide (x 6.75-9.25, as previewed) and, at every corner, T and cross
	 * a floor cable can make (and so every wall and ceiling one, the same models turned), no face of it lies on a face of any
	 * arm or lit line drawn with it. At 2 px it was exactly as wide as the strand pair, so the strands along its long side
	 * shared its side faces.
	 */
	@Test
	void cableKnotClearsEveryArm() throws IOException {
		JsonObject knot = json(modelPath(NS + ":block/wastes/derelict_cable_knot"));
		assertEquals(1, knot.getAsJsonArray("elements").size());
		double[] k = boxes(knot, 0).get(0);
		assertEquals(6.75, k[0], 1e-6, "the knot's west face");
		assertEquals(9.25, k[3], 1e-6, "the knot's east face");
		JsonObject state = json("/assets/" + NS + "/blockstates/derelict_cable.json");
		List<String> sides = List.of("north", "east", "south", "west");
		int checked = 0;
		for (int mask = 0; mask < 16; mask++) {
			Map<String, String> st = new HashMap<>(Map.of("surface", "down", "lit", "true", "girder", "none", "up", "false", "down", "false"));
			for (int i = 0; i < 4; i++)
				st.put(sides.get(i), String.valueOf((mask & (1 << i)) != 0));
			List<String> models = modelsFor(state, st);
			List<String> knots = models.stream().filter(m -> m.startsWith("derelict_cable_knot@")).toList();
			if (knots.isEmpty())
				continue;
			checked++;
			List<double[]> knotBoxes = boxes(knot, Integer.parseInt(knots.get(0).split("@")[1]));
			for (String m : models) {
				if (m.startsWith("derelict_cable_knot"))
					continue;
				String[] p = m.split("@");
				for (double[] arm : boxes(json(modelPath(NS + ":block/wastes/" + p[0])), Integer.parseInt(p[1])))
					for (double[] kb : knotBoxes)
						assertFalse(shareFace(kb, arm), "the knot z-fights " + p[0] + " (y " + p[1] + ") at the corner " + st);
			}
		}
		// every corner (four), T (four) and the cross
		assertEquals(9, checked, "corner states with a knot");
	}
}
