package com.comedymonkey.createaddons.content.wastes.bot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipLibrary;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The bots' exported assets (design/models/clockwork-wastes-bots.game-export.js) against the code that uses them: every
 * clip the code plays is in its bot's clips (with the mode the code expects), every bone the code poses is in its
 * geometry, the textures and glow textures are there (the glow holding only some pixels), every move lands inside its
 * clip, and the names, eggs and loot tables exist.
 */
class WastesBotAssetsTest {
	private static final String NS = "comedymonkeysaddons";
	/** The clips each bot's code plays, by name. */
	private static final Map<String, List<String>> CLIPS = Map.of(
		"scuttler", List.of("wake", "idle", "walk", "leap", "sees_player", "wipe_lens", "run", "climb", "hostile", "flipped", "grief",
			"vent_burst", "death", "sniff", "chitter", "stretch", "huddle_right", "huddle_left", "pick_scrap", "curious"),
		"sentry", List.of("unfold", "watching", "fire", "fold", "target", "aim", "hit", "death", "barrel_check", "petal_twitch", "blink", "listen"),
		"custodian", List.of("waking", "bow", "idle", "walk", "slam", "shockwave", "bolt_volley", "chain_arc", "wrath", "shield_down",
			"overload", "kneel", "death", "call_scuttlers", "hit", "straighten_plate", "mourn", "count", "glance_vault"));

	private static JsonObject json(String path) throws IOException {
		try (InputStream in = WastesBotAssetsTest.class.getResourceAsStream(path)) {
			assertNotNull(in, "missing " + path);
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	private static BufferedImage image(String path) throws IOException {
		try (InputStream in = WastesBotAssetsTest.class.getResourceAsStream(path)) {
			assertNotNull(in, "missing " + path);
			return ImageIO.read(in);
		}
	}

	@Test
	void everyClipTheCodePlaysIsExported() {
		for (var bot : CLIPS.entrySet()) {
			ClipLibrary lib = ClipLibrary.of("/assets/" + NS + "/" + bot.getKey() + "/clips.json");
			for (String name : bot.getValue())
				assertNotNull(lib.find(name), bot.getKey() + " has no clip " + name);
		}
		ClipLibrary custodian = ClipLibrary.of(Custodian.CLIPS);
		// held poses: the dormant ones are a clip's first frame; these end held
		for (String hold : List.of("waking", "bow", "kneel", "death"))
			assertEquals(Clip.Mode.HOLD, custodian.clip(hold).mode(), hold + " should hold its last frame");
		for (String loop : List.of("idle", "walk"))
			assertEquals(Clip.Mode.LOOP, custodian.clip(loop).mode(), loop + " should loop");
		assertEquals(Clip.Mode.ONCE, custodian.clip("slam").mode(), "a move plays once");
		assertEquals(Clip.Mode.LOOP, ClipLibrary.of(Sentry.CLIPS).clip("watching").mode());
		assertEquals(Clip.Mode.ONCE, ClipLibrary.of(Sentry.CLIPS).clip("fire").mode());
		assertEquals(Clip.Mode.HOLD, ClipLibrary.of(Scuttler.CLIPS).clip("wake").mode());
		// the clip pass (D59): the gaits and the Sentry's aiming loop; the reactions that end in a pose hold it
		ClipLibrary scuttler = ClipLibrary.of(Scuttler.CLIPS), sentry = ClipLibrary.of(Sentry.CLIPS);
		for (String loop : List.of("walk", "run", "climb"))
			assertEquals(Clip.Mode.LOOP, scuttler.clip(loop).mode(), "the Scuttler's " + loop + " should loop");
		for (String hold : List.of("hostile", "grief", "vent_burst", "death"))
			assertEquals(Clip.Mode.HOLD, scuttler.clip(hold).mode(), "the Scuttler's " + hold + " should hold");
		assertEquals(Clip.Mode.ONCE, scuttler.clip("flipped").mode());
		assertEquals(Clip.Mode.LOOP, sentry.clip("aim").mode(), "the Sentry aims round and round between shots");
		for (String hold : List.of("target", "death"))
			assertEquals(Clip.Mode.HOLD, sentry.clip(hold).mode(), "the Sentry's " + hold + " should hold");
		// every mannerism the code picks from is a clip that plays once
		for (String m : Scuttler.MANNERISMS)
			assertEquals(Clip.Mode.ONCE, scuttler.clip(m).mode(), "Scuttler mannerism " + m);
		for (String m : Sentry.MANNERISMS)
			assertEquals(Clip.Mode.ONCE, sentry.clip(m).mode(), "Sentry mannerism " + m);
		for (String m : Custodian.MANNERISMS)
			assertEquals(Clip.Mode.ONCE, custodian.clip(m).mode(), "Custodian mannerism " + m);
	}

	/** D60 and the previews' timings the code keeps to. */
	@Test
	void clipTimingsMatchTheCode() {
		ClipLibrary scuttler = ClipLibrary.of(Scuttler.CLIPS);
		// the walk clip's tripod takes 0.3 s; played at WALK_SPEED a cycle takes 0.45 s (D60); the run's is 0.3 s, at 1
		assertEquals(0.45f, 0.3f / Scuttler.WALK_SPEED, 1e-4f);
		assertEquals(0, scuttler.clip("walk").length() % 0.3f, 1e-3f);
		assertEquals(0, scuttler.clip("run").length() % 0.3f, 1e-3f);
		// on its back until it has righted itself (by 1.35 s), within the clip
		assertTrue(Scuttler.FLIP_TICKS / 20f >= 1.3f && Scuttler.FLIP_TICKS / 20f <= scuttler.clip("flipped").length());
		// out of a vent: still for its whole vent_burst clip
		assertEquals(scuttler.clip("vent_burst").length(), Scuttler.VENT_TICKS / 20f, 0.05f);
		// a called Scuttler's clip starts 0.3 s into its vent's burst (the vent preview's shift)
		assertEquals(0.3f, Custodian.VENT_DELAY / 20f, 1e-4f);
	}

	@Test
	void movesLandInsideTheirClips() {
		ClipLibrary lib = ClipLibrary.of(Custodian.CLIPS);
		for (Custodian.Move m : Custodian.Move.values()) {
			Clip clip = lib.clip(m.clip);
			for (float t : m.hits)
				assertTrue(t > 0 && t < clip.length(), m + " lands at " + t + " s, outside its " + clip.length() + " s clip");
		}
		assertTrue(Sentry.SHOT_AT < ClipLibrary.of(Sentry.CLIPS).clip("fire").length(), "the Sentry's shot is past its fire clip");
		// P37: the volley's three bolts are 0.2 s apart
		float[] v = Custodian.Move.VOLLEY.hits;
		assertEquals(3, v.length);
		assertEquals(0.2f, v[1] - v[0], 1e-4f);
		assertEquals(0.2f, v[2] - v[1], 1e-4f);
	}

	@Test
	void geometryHasTheBonesTheCodePoses() throws IOException {
		for (String bot : CLIPS.keySet()) {
			JsonObject g = json("/assets/" + NS + "/" + bot + "/geometry.json");
			Set<String> bones = new HashSet<>();
			for (JsonElement b : g.getAsJsonArray("bones"))
				bones.add(b.getAsJsonObject().get("name").getAsString());
			assertTrue(bones.contains(bot), bot + "'s root bone is missing");
			if (bot.equals("sentry"))
				assertTrue(bones.contains("yaw") && bones.contains("pitch"), "the Sentry's aim bones are missing");
			if (bot.equals("custodian")) {
				for (int k = 1; k <= 4; k++)
					assertTrue(bones.contains("socket_" + k), "the Custodian's socket_" + k + " is missing");
				// the clip pass's hidden lens layers: ember (wrath, top and bottom) and blaze (overload, all three)
				for (String lens : List.of("top", "mid", "bottom"))
					assertTrue(bones.contains("lens_" + lens + "_blaze"), "no blaze layer on its " + lens + " lens");
				for (String lens : List.of("top", "bottom"))
					assertTrue(bones.contains("lens_" + lens + "_ember"), "no ember layer on its " + lens + " lens");
				for (String bone : List.of("waist", "chest", "neck", "head"))
					assertTrue(bones.contains(bone), "the Custodian's " + bone + " (its look turns it) is missing");
			}
			if (bot.equals("sentry"))
				for (int k = 1; k <= 6; k++)
					assertTrue(bones.contains("lens_" + k + "_dim") && bones.contains("lens_" + k + "_pupil"), "the Sentry's lens " + k + " layers are missing");
			if (bot.equals("scuttler"))
				assertTrue(bones.contains("lens_crack"), "the Scuttler's lens crack is missing");
			// every animated bone is a bone of the model
			ClipLibrary lib = ClipLibrary.of("/assets/" + NS + "/" + bot + "/clips.json");
			for (Clip clip : lib.all()) {
				for (String b : clip.tracks().keySet())
					assertTrue(bones.contains(b), bot + " " + clip.name() + " animates a bone it hasn't: " + b);
				for (String b : clip.steps().keySet())
					assertTrue(bones.contains(b), bot + " " + clip.name() + " animates a bone it hasn't: " + b);
			}
		}
	}

	@Test
	void texturesAndGlowsExist() throws IOException {
		for (String bot : CLIPS.keySet()) {
			BufferedImage tex = image("/assets/" + NS + "/textures/entity/" + bot + ".png");
			BufferedImage glow = image("/assets/" + NS + "/textures/entity/" + bot + "_glow.png");
			assertEquals(tex.getWidth(), glow.getWidth(), bot + ": glow texture size");
			assertEquals(tex.getHeight(), glow.getHeight(), bot + ": glow texture size");
			int lit = 0, solid = 0;
			for (int x = 0; x < tex.getWidth(); x++)
				for (int y = 0; y < tex.getHeight(); y++) {
					if ((glow.getRGB(x, y) >>> 24) != 0)
						lit++;
					if ((tex.getRGB(x, y) >>> 24) != 0)
						solid++;
				}
			assertTrue(lit > 0 && lit < solid / 2, bot + ": the glow should hold some of its pixels, not most (" + lit + " of " + solid + ")");
		}
	}

	@Test
	void namesEggsAndLootExist() throws IOException {
		JsonObject lang = json("/assets/" + NS + "/lang/en_us.json");
		for (String id : List.of("scuttler", "sentry", "custodian", "starlight_bolt"))
			assertTrue(lang.has("entity." + NS + "." + id), "no name for " + id);
		for (String bot : CLIPS.keySet()) {
			assertTrue(lang.has("item." + NS + "." + bot + "_spawn_egg"), "no name for the " + bot + " egg");
			json("/assets/" + NS + "/models/item/" + bot + "_spawn_egg.json");
		}
		for (String adv : List.of("wastes_short_circuit", "wastes_custodian_down")) {
			assertTrue(lang.has("advancements." + NS + "." + adv) && lang.has("advancements." + NS + "." + adv + ".desc"), "no text for " + adv);
			json("/data/" + NS + "/advancement/" + adv + ".json");
		}
		// P18: 0-2 scrap
		JsonObject scuttler = json("/data/" + NS + "/loot_table/entities/scuttler.json");
		assertTrue(scuttler.toString().contains(NS + ":clockwork_scrap"), "the Scuttler should drop scrap");
		// P40: an Unknown Device, a player's kill only
		JsonObject custodian = json("/data/" + NS + "/loot_table/entities/custodian.json");
		assertTrue(custodian.toString().contains(NS + ":unknown_device") && custodian.toString().contains("minecraft:killed_by_player"),
			"the Custodian's Unknown Device should need a player's kill");
	}

	/**
	 * The clip pass's lens layers glow (ember, blaze, the Sentry's dim lenses and ember pupils) and the dying Scuttler's
	 * crack is dark: the glow texture holds the first's pixels and not the crack's.
	 */
	@Test
	void lensLayersGlowAndTheCrackDoesnt() throws IOException {
		Map<String, List<String>> glowing = Map.of("custodian", List.of("lens_top_ember_lit", "lens_bottom_ember_lit", "lens_mid_blaze_lit"),
			"sentry", List.of("lens_1_dim_lit", "lens_1_pupil_lit"));
		for (var e : glowing.entrySet()) {
			BufferedImage glow = image("/assets/" + NS + "/textures/entity/" + e.getKey() + "_glow.png");
			for (String cube : e.getValue())
				assertTrue(litPixels(glow, cube(e.getKey(), cube)) > 0, e.getKey() + ": " + cube + " should glow");
		}
		BufferedImage glow = image("/assets/" + NS + "/textures/entity/scuttler_glow.png"), tex = image("/assets/" + NS + "/textures/entity/scuttler.png");
		JsonObject crack = cube("scuttler", "lens_crack_dark");
		assertTrue(litPixels(tex, crack) > 0, "the crack should be painted");
		assertEquals(0, litPixels(glow, crack), "the crack should be dark, not in the glow");
	}

	/** The Custodian's tether cables' materials (its renderer's): the cable, void alloy and the bead, 16 px each. */
	@Test
	void tetherTextureExists() throws IOException {
		BufferedImage t = image("/assets/" + NS + "/textures/entity/custodian_tether.png");
		assertEquals(48, t.getWidth());
		assertEquals(16, t.getHeight());
		// the bead is the starlight edge tone (#7f76a8)
		assertEquals(0x7f76a8, t.getRGB(40, 8) & 0xFFFFFF);
	}

	private static JsonObject cube(String bot, String name) throws IOException {
		for (JsonElement b : json("/assets/" + NS + "/" + bot + "/geometry.json").getAsJsonArray("bones"))
			for (JsonElement c : b.getAsJsonObject().getAsJsonArray("cubes"))
				if (c.getAsJsonObject().get("name").getAsString().equals(name))
					return c.getAsJsonObject();
		throw new AssertionError(bot + " has no cube " + name);
	}

	/** Opaque pixels of a cube's box-UV net (its north face, where a lens is painted). */
	private static int litPixels(BufferedImage img, JsonObject cube) {
		int u = cube.getAsJsonArray("uv").get(0).getAsInt(), v = cube.getAsJsonArray("uv").get(1).getAsInt();
		float[] from = new float[3], to = new float[3];
		for (int i = 0; i < 3; i++) {
			from[i] = cube.getAsJsonArray("from").get(i).getAsFloat();
			to[i] = cube.getAsJsonArray("to").get(i).getAsFloat();
		}
		int w = (int) Math.ceil(to[0] - from[0]), h = (int) Math.ceil(to[1] - from[1]), d = (int) Math.ceil(to[2] - from[2]);
		int n = 0;
		for (int x = u + d; x < u + d + w; x++)
			for (int y = v + d; y < v + d + h; y++)
				if (x < img.getWidth() && y < img.getHeight() && (img.getRGB(x, y) >>> 24) != 0)
					n++;
		return n;
	}

	/** One pool's item, count range and conditions, flattened for checking. */
	private static String pool(JsonObject table, int i) {
		return table.getAsJsonArray("pools").get(i).toString();
	}

	/** D53: the Sentry's 1-2 scrap and 15% Electron Tube, the Custodian's 4-8 scrap beside its Unknown Device, all for a player's kill. */
	@Test
	void botLootIsAsAgreed() throws IOException {
		JsonObject sentry = json("/data/" + NS + "/loot_table/entities/sentry.json");
		assertEquals(2, sentry.getAsJsonArray("pools").size());
		String scrap = pool(sentry, 0), tube = pool(sentry, 1);
		assertTrue(scrap.contains(NS + ":clockwork_scrap") && scrap.contains("\"min\":1") && scrap.contains("\"max\":2")
			&& scrap.contains("minecraft:killed_by_player"), "Sentry: 1-2 scrap for a player's kill: " + scrap);
		assertTrue(tube.contains("create:electron_tube") && tube.contains("\"chance\":0.15") && tube.contains("minecraft:killed_by_player"),
			"Sentry: a 15% Electron Tube for a player's kill: " + tube);
		JsonObject custodian = json("/data/" + NS + "/loot_table/entities/custodian.json");
		assertEquals(2, custodian.getAsJsonArray("pools").size());
		String device = pool(custodian, 0), cScrap = pool(custodian, 1);
		assertTrue(device.contains(NS + ":unknown_device") && device.contains("minecraft:killed_by_player"), "Custodian: its Unknown Device: " + device);
		assertTrue(cScrap.contains(NS + ":clockwork_scrap") && cScrap.contains("\"min\":4") && cScrap.contains("\"max\":8")
			&& cScrap.contains("minecraft:killed_by_player"), "Custodian: 4-8 scrap for a player's kill: " + cScrap);
		// the Scuttler's scrap drops on any death (no condition)
		assertTrue(!json("/data/" + NS + "/loot_table/entities/scuttler.json").toString().contains("killed_by_player"), "the Scuttler's scrap should drop on any death");
	}

	/**
	 * D67: the Scuttler's flip comes at most once every 4 s, and it's righted well inside that; it's curious for longer
	 * than its curious clip takes to pose it, within about 8 blocks, and not again for a good while after.
	 */
	@Test
	void flipWindowAndCuriosityMatchD67() {
		ClipLibrary scuttler = ClipLibrary.of(Scuttler.CLIPS);
		assertEquals(4f, Scuttler.FLIP_EVERY / 20f, 1e-4f, "a hit flips it at most once every 4 s");
		assertTrue(Scuttler.FLIP_TICKS < Scuttler.FLIP_EVERY, "it should be righted before it can be flipped again");
		assertEquals(Clip.Mode.HOLD, scuttler.clip("curious").mode(), "curious poses it and holds while it watches");
		assertTrue(Scuttler.CURIOUS_TICKS / 20f >= scuttler.clip("curious").length(), "it should stay curious until the clip has posed it");
		assertEquals(8, Scuttler.CURIOUS_RANGE, 1e-9, "about 8 blocks (D67)");
		assertTrue(Scuttler.CURIOUS_COOLDOWN > Scuttler.CURIOUS_TICKS, "the cooldown should be longer than the curiosity, or it loops");
	}

	/**
	 * D67: which side a hit lands on, against the bot's facing (yaw 0 faces south, its right to the west, as the Scuttler's
	 * huddle has it); the left-side clip only once it's exported, and the flag goes with the clips: on exactly when both
	 * the Sentry and the Custodian have hit_left.
	 */
	@Test
	void hitSideIsToldFromTheFacing() {
		net.minecraft.world.phys.Vec3 up = new net.minecraft.world.phys.Vec3(0, 1, 0), at = net.minecraft.world.phys.Vec3.ZERO;
		net.minecraft.world.phys.Vec3 south = CityBot.facing(0);
		assertEquals(0, south.x, 1e-6);
		assertEquals(1, south.z, 1e-6);
		assertEquals(CityBot.Side.RIGHT, CityBot.struckSide(at, south, up, new net.minecraft.world.phys.Vec3(-3, 1, 0)), "facing south, west is its right");
		assertEquals(CityBot.Side.LEFT, CityBot.struckSide(at, south, up, new net.minecraft.world.phys.Vec3(3, 1, 0)), "facing south, east is its left");
		// turned to face west (yaw 90): its right is north
		net.minecraft.world.phys.Vec3 west = CityBot.facing(90);
		assertEquals(-1, west.x, 1e-6);
		assertEquals(CityBot.Side.RIGHT, CityBot.struckSide(at, west, up, new net.minecraft.world.phys.Vec3(0.5, 0, -4)));
		assertEquals(CityBot.Side.LEFT, CityBot.struckSide(at, west, up, new net.minecraft.world.phys.Vec3(0.5, 0, 4)));
		// a Sentry on a wall facing east (its up is east, its head facing south): a floor Sentry rolled onto its left side,
		// so its right is up and its left down
		net.minecraft.world.phys.Vec3 east = new net.minecraft.world.phys.Vec3(1, 0, 0);
		assertEquals(CityBot.Side.RIGHT, CityBot.struckSide(at, south, east, new net.minecraft.world.phys.Vec3(0, 3, 0)), "on its side, its right is up");
		assertEquals(CityBot.Side.LEFT, CityBot.struckSide(at, south, east, new net.minecraft.world.phys.Vec3(0, -3, 0)), "on its side, its left is down");
		// dead ahead counts as its right (the clip there is)
		assertEquals(CityBot.Side.RIGHT, CityBot.struckSide(at, south, up, new net.minecraft.world.phys.Vec3(0, 0, 5)));
		// the clips: the left one only when it's in
		assertEquals("hit", CityBot.hitClip(CityBot.Side.LEFT, false));
		assertEquals("hit", CityBot.hitClip(CityBot.Side.RIGHT, false));
		assertEquals("hit_left", CityBot.hitClip(CityBot.Side.LEFT, true));
		assertEquals("hit", CityBot.hitClip(CityBot.Side.RIGHT, true));
		for (String clips : List.of(Sentry.CLIPS, Custodian.CLIPS)) {
			ClipLibrary lib = ClipLibrary.of(clips);
			assertNotNull(lib.find(CityBot.HIT), clips + " has no hit");
			assertEquals(CityBot.LEFT_HIT_CLIPS, lib.find(CityBot.HIT_LEFT) != null,
				clips + ": LEFT_HIT_CLIPS should be on exactly when hit_left is exported (the user approves the preview first)");
		}
	}

	/** D56: the shockwave ring starts at the clip's stamp and has run its 8 blocks before the clip ends. */
	@Test
	void shockwaveRingRunsOutWithinItsClip() {
		Clip clip = ClipLibrary.of(Custodian.CLIPS).clip(Custodian.Move.SHOCKWAVE.clip);
		float stamp = Custodian.Move.SHOCKWAVE.hits[0];
		double run = Custodian.SHOCKWAVE_RADIUS / Custodian.RING_SPEED / 20;
		assertTrue(stamp + run <= clip.length(), "the ring runs until " + (stamp + run) + " s, past the " + clip.length() + " s clip");
	}
}
