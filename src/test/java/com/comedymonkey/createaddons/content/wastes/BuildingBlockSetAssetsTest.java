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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * The Clockwork Wastes' building block set and its base surface (D75), as design/models/clockwork-wastes.export.js
 * writes them: every block's state, models, textures, item, name, loot and tags; the void-alloy bond's state drawing
 * exactly one plate on each face, the one its value names; the striped 'va_tile' gone from every model; the bond's
 * courses as the approved preview lays them.
 */
class BuildingBlockSetAssetsTest {
	private static final String NS = "comedymonkeysaddons";
	static final List<String> BLOCKS = List.of("void_alloy_stairs", "void_alloy_slab", "void_alloy_wall", "slotted_grille", "void_alloy_band",
		"clamp_bracket", "scarred_void_alloy_plate", "stained_end_stone_bricks", "cracked_stained_end_stone_bricks", "stained_end_stone_brick_stairs",
		"stained_end_stone_brick_slab", "stained_end_stone_brick_wall", "key_marked_stone", "broken_makers_window", "patina_rib");
	private static final String[] LETTERS = { "s", "l", "r" };
	private static final List<String> SIDES = List.of("north", "east", "south", "west");

	private static InputStream open(String path) {
		return BuildingBlockSetAssetsTest.class.getResourceAsStream(path);
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

	private static JsonObject state(String id) throws IOException {
		return json("/assets/" + NS + "/blockstates/" + id + ".json");
	}

	/** Every model a blockstate can draw (variants or multipart, weighted lists included). */
	private static Set<String> allModels(JsonObject state) {
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

	/** A model's own textures (of this mod) exist, and its parent, if it's one of ours. */
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
		if (m.has("parent"))
			checkModel(m.get("parent").getAsString());
	}

	@Test
	void everyBlockHasStatesModelsTexturesItemNameLootAndTags() throws IOException {
		JsonObject lang = json("/assets/" + NS + "/lang/en_us.json");
		String pick = text("/data/minecraft/tags/block/mineable/pickaxe.json");
		for (String id : BLOCKS) {
			Set<String> models = allModels(state(id));
			assertFalse(models.isEmpty(), id + " draws nothing");
			for (String m : models)
				checkModel(m);
			JsonObject item = json("/assets/" + NS + "/models/item/" + id + ".json");
			checkModel(item.get("parent").getAsString());
			assertTrue(lang.has("block." + NS + "." + id), "no name for " + id);
			// decor: it drops itself (D37)
			String loot = text("/data/" + NS + "/loot_table/blocks/" + id + ".json");
			assertTrue(loot.contains("\"" + NS + ":" + id + "\""), id + " doesn't drop itself");
			assertTrue(pick.contains("\"" + NS + ":" + id + "\""), id + " isn't mined with a pickaxe");
		}
		// a double slab drops two
		assertTrue(text("/data/" + NS + "/loot_table/blocks/void_alloy_slab.json").contains("\"double\""), "a double slab drops one");
		// vanilla's shape tags: walls join only walls in #walls
		for (String[] t : new String[][] { { "walls", "_wall" }, { "stairs", "_stairs" }, { "slabs", "_slab" } })
			for (String id : BLOCKS)
				if (id.endsWith(t[1])) {
					assertTrue(text("/data/minecraft/tags/block/" + t[0] + ".json").contains(NS + ":" + id), id + " isn't in #" + t[0]);
					assertTrue(text("/data/minecraft/tags/item/" + t[0] + ".json").contains(NS + ":" + id), id + "'s item isn't in #" + t[0]);
				}
		// the Plate's state and item too
		for (String m : allModels(state("void_alloy_plate")))
			checkModel(m);
		checkModel(json("/assets/" + NS + "/models/item/void_alloy_plate.json").get("parent").getAsString());
	}

	/** A multipart part applies to a state (its "when": each key one of its "|" values). */
	private static boolean applies(JsonObject part, Map<String, String> state) {
		if (!part.has("when"))
			return true;
		for (var e : part.getAsJsonObject("when").entrySet()) {
			String v = state.get(e.getKey());
			if (v == null || !List.of(e.getValue().getAsString().split("\\|")).contains(v))
				return false;
		}
		return true;
	}

	/** What a multipart draws for a state: "model@x/y". */
	private static List<String> drawn(JsonObject state, Map<String, String> st) {
		List<String> out = new ArrayList<>();
		for (JsonElement p : state.getAsJsonArray("multipart")) {
			JsonObject part = p.getAsJsonObject();
			if (!applies(part, st))
				continue;
			JsonObject a = part.getAsJsonObject("apply");
			out.add(a.get("model").getAsString().replace(NS + ":block/wastes/", "") + "@" + (a.has("x") ? a.get("x").getAsInt() : 0) + "/"
				+ (a.has("y") ? a.get("y").getAsInt() : 0));
		}
		out.sort(null);
		return out;
	}

	private static Map<String, String> st(String... kv) {
		Map<String, String> m = new HashMap<>();
		for (int i = 0; i < kv.length; i += 2)
			m.put(kv[i], kv[i + 1]);
		return m;
	}

	/** The plate drawn on each side for a bond: north and south from bond_ns, east and west from bond_ew; up and down 1 x 1. */
	private static List<String> plateFaces(String ns, String ew, String skip) {
		List<String> out = new ArrayList<>();
		Map<String, String> side = Map.of("north", ns.substring(0, 1), "south", ns.substring(1), "east", ew.substring(0, 1), "west", ew.substring(1),
			"up", "s", "down", "s");
		for (var e : side.entrySet())
			if (!e.getKey().equals(skip))
				out.add("void_alloy_plate_" + e.getKey() + "_" + e.getValue() + "@0/0");
		return out;
	}

	@Test
	void theBondDrawsOnePlateOnEachFace() throws IOException {
		JsonObject plate = state("void_alloy_plate"), slab = state("void_alloy_slab"), scarred = state("scarred_void_alloy_plate");
		for (String a : LETTERS) for (String b : LETTERS) for (String c : LETTERS) for (String d : LETTERS) {
			String ns = a + b, ew = c + d;
			List<String> want = plateFaces(ns, ew, null);
			want.sort(null);
			assertEquals(want, drawn(plate, st("bond_ns", ns, "bond_ew", ew)), "plate " + ns + " " + ew);
			assertEquals(want, drawn(slab, st("type", "double", "bond_ns", ns, "bond_ew", ew, "waterlogged", "false")), "double slab " + ns + " " + ew);
			assertEquals(List.of("void_alloy_slab@0/0"), drawn(slab, st("type", "bottom", "bond_ns", ns, "bond_ew", ew, "waterlogged", "false")));
			// the scarred face draws its scar (turned to its facing), the others the bond; only a cracked one, lit, glows
			for (String facing : SIDES) for (String scar : List.of("chipped", "cracked", "heat")) for (String lit : List.of("false", "true")) {
				int y = Map.of("north", 0, "east", 90, "south", 180, "west", 270).get(facing);
				List<String> w = plateFaces(ns, ew, facing);
				w.add("scarred_void_alloy_plate_" + scar + "@0/" + y);
				if (scar.equals("cracked") && lit.equals("true"))
					w.add("scarred_void_alloy_plate_glow@0/" + y);
				w.sort(null);
				assertEquals(w, drawn(scarred, st("facing", facing, "scar", scar, "lit", lit, "bond_ns", ns, "bond_ew", ew)), "scarred " + facing + " " + scar);
			}
		}
	}

	@Test
	void theGrilleAndBandDrawTheirPartsInEveryOrientation() throws IOException {
		JsonObject grille = state("slotted_grille"), band = state("void_alloy_band");
		for (String axis : List.of("x", "z", "x_lying", "z_lying", "up_x", "up_z"))
			for (String lit : List.of("false", "true")) {
				List<String> d = drawn(grille, st("axis", axis, "lit", lit, "bond_ns", "lr", "bond_ew", "sl"));
				String body = axis.contains("lying") ? "slotted_grille_lying" : "slotted_grille";
				assertEquals(1, d.stream().filter(m -> m.startsWith(body + "@")).count(), axis + ": its body once");
				assertEquals(lit.equals("true") ? 1 : 0, d.stream().filter(m -> m.startsWith(body + "_glow@")).count(), axis + ": its pane lit with it");
				// one plate for each of its six model sides (each has bonded faces), all turned alike
				assertEquals(6, d.stream().filter(m -> m.matches(body + "_(north|east|south|west|up|down)_[slr]@.*")).count(), axis + ": " + d);
				assertEquals(1, d.stream().map(m -> m.substring(m.indexOf('@'))).distinct().count(), axis + ": parts turned differently " + d);
			}
		// upright x: the model turned a quarter, so its south (slotted) face looks west: bond_ew's west plate ('l' in "sl")
		assertTrue(drawn(grille, st("axis", "x", "lit", "false", "bond_ns", "lr", "bond_ew", "sl")).contains("slotted_grille_south_l@0/90"));
		Map<String, List<String>> round = Map.of("y", SIDES, "x", List.of("north", "south", "up", "down"), "z", List.of("east", "west", "up", "down"));
		for (String axis : round.keySet())
			for (String side : List.of("north", "east", "south", "west", "up", "down")) {
				Map<String, String> s = st("axis", axis, "lit", "true", "north", "false", "east", "false", "south", "false", "west", "false", "up", "false",
					"down", "false");
				s.put(side, "true");
				List<String> d = drawn(band, s);
				long fills = d.stream().filter(m -> m.startsWith("void_alloy_band_fill_")).count();
				assertEquals(round.get(axis).contains(side) ? 1 : 0, fills, "band " + axis + " joined " + side + ": " + d);
				assertEquals(1, d.stream().filter(m -> m.startsWith("void_alloy_band_glow@")).count(), "band " + axis + " lit");
			}
	}

	@Test
	void noModelKeepsTheStripedSurface() throws IOException {
		// every Wastes model the blockstates draw: the old sheen texture is gone (D75: everywhere)
		for (String id : List.of("void_alloy_plate", "seamed_void_alloy_plate", "void_alloy_pillar", "charging_alcove", "city_vault", "sliding_door",
			"conduit_pylon", "derelict_star_reactor", "wall_vent", "derelict_cable", "assembly_clamp")) {
			for (String m : allModels(state(id)))
				if (m.startsWith(NS + ":block/wastes/"))
					assertFalse(text("/assets/" + NS + "/models/" + m.substring(NS.length() + 1) + ".json").contains("va_tile"), m + " still uses va_tile");
		}
		assertTrue(open("/assets/" + NS + "/textures/block/wastes/va_tile.png") == null, "the striped texture is still shipped");
		for (String t : List.of("va_plain", "va_bond_s", "va_bond_l", "va_bond_r", "va_field"))
			assertNotNull(open("/assets/" + NS + "/textures/block/wastes/" + t + ".png"), "no " + t);
	}

	/**
	 * D76: the city's machines and props are cast in one piece, so their bodies take the frameless field (va_field) and no
	 * model they draw uses the framed plain plate (va_plain), whose lit and dark lines would cross one object at every block
	 * line; the framed plate stays on the stairs, slabs and walls (and not the field), and the bond on full plates.
	 */
	@Test
	void machinesAndPropsTakeTheFrameslessField() throws IOException {
		Set<String> drawn = new HashSet<>();
		for (String id : List.of("charging_alcove", "city_vault", "sliding_door", "conduit_pylon", "derelict_star_reactor", "wall_vent", "assembly_clamp",
			"derelict_cable", "lift", "lift_call_plate", "seamed_void_alloy_plate", "void_alloy_pillar"))
			for (String m : allModels(state(id)))
				if (m.startsWith(NS + ":block/wastes/"))
					drawn.add(m.substring((NS + ":block/wastes/").length()));
		// the parts their renderers draw
		drawn.addAll(List.of("sliding_door_leaf_l", "sliding_door_leaf_r", "sliding_door_seam", "sliding_door_chain", "sliding_door_jamb_glow",
			"derelict_star_reactor_arm", "lift_deck_w", "lift_deck_e", "lift_guides_w", "lift_guides_e", "lift_guide_seams_w", "lift_guide_seams_e",
			"charging_alcove_trinket_cog", "charging_alcove_trinket_key", "charging_alcove_trinket_cart", "charging_alcove_trinket_flower"));
		for (String m : drawn)
			assertFalse(text("/assets/" + NS + "/models/block/wastes/" + m + ".json").contains("block/wastes/va_plain"), m + " uses the framed plain plate");
		// each machine's and prop's body is the field
		for (String m : List.of("charging_alcove_bottom", "charging_alcove_middle", "charging_alcove_top", "city_vault", "sliding_door_0_0", "sliding_door_1_2",
			"sliding_door_leaf_l", "sliding_door_leaf_r", "conduit_pylon_bottom", "conduit_pylon_middle", "conduit_pylon_top", "derelict_star_reactor",
			"derelict_star_reactor_arm", "lift_deck_w", "lift_deck_e", "lift_guides_w", "lift_guides_e", "wall_vent", "derelict_cable_riser_foot",
			"assembly_clamp_1", "assembly_clamp_5"))
			assertTrue(text("/assets/" + NS + "/models/block/wastes/" + m + ".json").contains("block/wastes/va_field"), m + " has no frameless field");
		assertTrue(text("/assets/" + NS + "/models/item/city_vault.json").contains("va_field") || text("/assets/" + NS + "/models/block/wastes/city_vault_item.obj").contains("va_field"),
			"the vault's item has no field");
		// the framed plate on the shaped blocks, never the field
		for (String m : List.of("void_alloy_stairs", "void_alloy_stairs_inner", "void_alloy_stairs_outer", "void_alloy_slab", "void_alloy_slab_top",
			"void_alloy_wall_post", "void_alloy_wall_side", "void_alloy_wall_side_tall")) {
			String t = text("/assets/" + NS + "/models/block/wastes/" + m + ".json");
			assertTrue(t.contains("block/wastes/va_plain"), m + " lost the framed plate");
			assertFalse(t.contains("block/wastes/va_field"), m + " takes the field");
		}
	}

	/** The bond as the approved preview lays it (OPTS.C): l r s l r along a course, each course shifted, 2 x 1s never broken. */
	@Test
	void theBondsCoursesAreWhole() {
		for (int y = -7; y < 8; y++)
			for (int x = -12; x < 12; x++) {
				BlockPos p = new BlockPos(x, y, 3), east = p.east(), west = p.west();
				// the south face reads left to right with x: a left half always has its right half east of it
				char s = VoidAlloyBond.plate(p, Direction.SOUTH);
				if (s == 'l')
					assertEquals('r', VoidAlloyBond.plate(east, Direction.SOUTH), p + " south");
				if (s == 'r')
					assertEquals('l', VoidAlloyBond.plate(west, Direction.SOUTH), p + " south");
				// the north face reads left to right against x
				char n = VoidAlloyBond.plate(p, Direction.NORTH);
				if (n == 'l')
					assertEquals('r', VoidAlloyBond.plate(west, Direction.NORTH), p + " north");
				BlockPos q = new BlockPos(3, y, x);
				char w = VoidAlloyBond.plate(q, Direction.WEST), e = VoidAlloyBond.plate(q, Direction.EAST);
				if (w == 'l')
					assertEquals('r', VoidAlloyBond.plate(q.south(), Direction.WEST), q + " west");
				if (e == 'l')
					assertEquals('r', VoidAlloyBond.plate(q.north(), Direction.EAST), q + " east");
				assertEquals('s', VoidAlloyBond.plate(p, Direction.UP));
			}
		// five blocks a course (two 2 x 1s and a 1 x 1), and the next course shifted (the preview's SH: 0, 3, 1, 4, 2)
		StringBuilder c0 = new StringBuilder(), c1 = new StringBuilder();
		for (int x = 0; x < 5; x++) {
			c0.append(VoidAlloyBond.plate(new BlockPos(x, 0, 0), Direction.SOUTH));
			c1.append(VoidAlloyBond.plate(new BlockPos(x, 1, 0), Direction.SOUTH));
		}
		assertEquals("lrslr", c0.toString());
		assertEquals("lrlrs", c1.toString());
	}
}
