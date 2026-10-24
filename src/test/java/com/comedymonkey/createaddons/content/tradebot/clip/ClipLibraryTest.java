package com.comedymonkey.createaddons.content.tradebot.clip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joml.Vector3f;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.testutil.ModResourceFiles;
import com.google.gson.JsonElement;

class ClipLibraryTest {
	private static final ClipLibrary LIB = ClipLibrary.get();
	// chained without a pop: these all start (and the turns also end) on the rolling loop's first frame
	private static final List<String> FROM_ROLLING = List.of("turn_left", "turn_right", "turn_around", "stop_rolling");
	// the wheel and the wind-up key turn on from clip to clip (whole turns look the same); the root is the game's
	private static final Set<String> CONTINUOUS = Set.of("wheel", "key", "trade_bot");

	@Test
	void everyClipFromTheBriefIsThere() {
		assertEquals(43, LIB.all().size());
		for (String name : List.of("idle", "rolling", "start_rolling", "stop_rolling", "turn_left", "turn_right", "turn_around",
			"wall_bump", "sees_player", "dock_lights_out", "action_comes_in", "chest_take", "chest_put", "barrel_take", "barrel_put",
			"chest_take_high", "chest_put_high", "depot_place", "depot_take", "belt_place", "trade_villager", "trade_villager_open",
			"hop_on_slab", "hop_off_slab", "hop_on_block", "hop_off_block", "stairs_up", "stairs_down", "scared_spot", "scared_flee",
			"scared_hide", "scared_cower", "scared_recover"))
			assertNotNull(LIB.find(name), name);
	}

	@Test
	void idsRoundTrip() {
		for (Clip clip : LIB.all())
			assertSame(clip, LIB.byId(LIB.id(clip)));
	}

	@Test
	void rootModesMatchThePlan() {
		for (String name : List.of("hop_on_slab", "hop_off_block", "stairs_up", "dock_lights_out", "action_comes_in"))
			assertEquals(Clip.Root.PHYSICAL, LIB.clip(name).root(), name);
		for (String name : List.of("rolling", "start_rolling", "stop_rolling", "turn_left", "turn_around"))
			assertEquals(Clip.Root.GAME, LIB.clip(name).root(), name);
		assertEquals(Clip.Root.VISUAL, LIB.clip("wall_bump").root());
		assertEquals(Clip.Mode.LOOP, LIB.clip("rolling").mode());
		assertEquals(Clip.Mode.HOLD, LIB.clip("dock_lights_out").mode());
	}

	@Test
	void everyTrackIsOnABoneOfTheModel() {
		Path file = ModResourceFiles.find("assets", ComedyMonkeysCreateAddons.MOD_ID, "trade_bot", "geometry.json");
		Assumptions.assumeTrue(file != null, "geometry.json not reachable");
		Set<String> bones = new HashSet<>();
		for (JsonElement bone : ModResourceFiles.readJson(file).getAsJsonObject().getAsJsonArray("bones"))
			bones.add(bone.getAsJsonObject().get("name").getAsString());
		for (Clip clip : LIB.all()) {
			for (String bone : clip.tracks().keySet())
				assertTrue(bones.contains(bone), clip.name() + " animates unknown bone " + bone);
			for (String bone : clip.steps().keySet())
				assertTrue(bones.contains(bone), clip.name() + " switches unknown bone " + bone);
		}
	}

	@Test
	void loopsWrapOntoThemselves() {
		for (Clip clip : LIB.all()) {
			if (clip.mode() != Clip.Mode.LOOP)
				continue;
			for (Map.Entry<String, Map<String, Track>> bone : clip.tracks().entrySet()) {
				if (CONTINUOUS.contains(bone.getKey()))
					continue;
				for (Map.Entry<String, Track> ch : bone.getValue().entrySet())
					assertNear(ch.getValue().at(0, new Vector3f()), ch.getValue().at(ch.getValue().samples() - 1, new Vector3f()),
						clip.name() + " " + bone.getKey() + "." + ch.getKey());
			}
		}
	}

	@Test
	void locomotionClipsChainWithoutAPop() {
		Clip rolling = LIB.clip("rolling");
		assertSameFrame(LIB.clip("start_rolling"), true, rolling, false);
		for (String name : FROM_ROLLING)
			assertSameFrame(rolling, false, LIB.clip(name), false);
		for (String name : List.of("turn_left", "turn_right", "turn_around"))
			assertSameFrame(LIB.clip(name), true, rolling, false);
	}

	@Test
	void catmullRomPassesThroughItsKeys() {
		Track track = new Track(20, true, new float[] { 0, 0, 0, 10, 5, -5, 4, 2, 8, 0, 0, 0 });
		assertNear(new Vector3f(10, 5, -5), track.sample(0.05f, new Vector3f()), "second key");
		assertNear(new Vector3f(4, 2, 8), track.sample(0.10f, new Vector3f()), "third key");
		assertNear(new Vector3f(0, 0, 0), track.sample(5f, new Vector3f()), "held after the end");
	}

	@Test
	void loopsWrapAndOneShotsHold() {
		Clip rolling = LIB.clip("rolling");
		assertEquals(0.5f, rolling.localTime(rolling.length() + 0.5f), 1e-4);
		Clip stop = LIB.clip("stop_rolling");
		assertEquals(stop.length(), stop.localTime(stop.length() + 3), 1e-4);
		assertTrue(stop.finished(stop.length()));
	}

	/** Every shared channel of {@code a}'s first or last frame equals {@code b}'s first or last frame. */
	private static void assertSameFrame(Clip a, boolean aEnd, Clip b, boolean bEnd) {
		int compared = 0;
		for (Map.Entry<String, Map<String, Track>> bone : a.tracks().entrySet()) {
			if (CONTINUOUS.contains(bone.getKey()))
				continue;
			for (Map.Entry<String, Track> ch : bone.getValue().entrySet()) {
				Track other = b.track(bone.getKey(), ch.getKey());
				if (other == null)
					continue;
				Track mine = ch.getValue();
				assertNear(mine.at(aEnd ? mine.samples() - 1 : 0, new Vector3f()), other.at(bEnd ? other.samples() - 1 : 0, new Vector3f()),
					a.name() + " → " + b.name() + " " + bone.getKey() + "." + ch.getKey());
				compared++;
			}
		}
		assertTrue(compared >= 5, "too few shared channels between " + a.name() + " and " + b.name());
	}

	private static void assertNear(Vector3f expected, Vector3f actual, String what) {
		assertTrue(expected.distance(actual) < 0.01f, what + ": expected " + expected + " but was " + actual);
	}
}
