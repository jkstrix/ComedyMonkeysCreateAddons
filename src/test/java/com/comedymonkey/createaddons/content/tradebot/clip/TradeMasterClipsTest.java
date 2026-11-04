package com.comedymonkey.createaddons.content.tradebot.clip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.comedymonkey.createaddons.content.tradebot.BotKind;
import com.comedymonkey.createaddons.testutil.ModResourceFiles;
import com.google.gson.JsonElement;

/**
 * The Trade Master's clips are Trade Bot's (design/specs/trade-master.md D39: the same animations and noises), plus
 * tracks for his own parts. The server times and moves both robots with Trade Bot's library and only the client reads
 * the Trade Master's ({@link BotKind#own}), so this has to hold exactly.
 */
class TradeMasterClipsTest {
	private static final ClipLibrary BOT = ClipLibrary.get();
	private static final ClipLibrary MASTER = BotKind.TRADE_MASTER.clips();
	/** His own parts: only these may add tracks to Trade Bot's clips. */
	private static final Set<String> HIS_OWN = Set.of("beard", "monocle", "monocle_chain", "aerial_bulb_glow");

	@Test
	void theSameClipsInTheSameOrder() {
		assertEquals(BOT.all().size(), MASTER.all().size());
		for (int id = 0; id < BOT.all().size(); id++) {
			Clip a = BOT.byId(id), b = MASTER.byId(id);
			assertEquals(a.name(), b.name(), "clip " + id);
			assertEquals(a.length(), b.length(), 1e-6, a.name());
			assertEquals(a.mode(), b.mode(), a.name());
			assertEquals(a.root(), b.root(), a.name());
			assertEquals(a.cues(), b.cues(), a.name() + " cues");
		}
	}

	@Test
	void everyTradeBotTrackIsUnchanged() {
		Vector3f p = new Vector3f(), q = new Vector3f();
		for (Clip a : BOT.all()) {
			Clip b = MASTER.clip(a.name());
			for (Map.Entry<String, Map<String, Track>> bone : a.tracks().entrySet())
				for (Map.Entry<String, Track> ch : bone.getValue().entrySet()) {
					Track ta = ch.getValue(), tb = b.track(bone.getKey(), ch.getKey());
					String where = a.name() + " " + bone.getKey() + "." + ch.getKey();
					assertNotNull(tb, where);
					assertEquals(ta.samples(), tb.samples(), where);
					for (int s = 0; s < ta.samples(); s++)
						assertEquals(ta.at(s, p), tb.at(s, q), where + " sample " + s);
				}
			for (Map.Entry<String, Map<String, StepTrack>> bone : a.steps().entrySet())
				for (String ch : bone.getValue().keySet())
					assertNotNull(b.step(bone.getKey(), ch), a.name() + " step " + bone.getKey() + "." + ch);
		}
	}

	@Test
	void onlyHisOwnPartsAddTracks() {
		for (Clip b : MASTER.all()) {
			Clip a = BOT.clip(b.name());
			for (String bone : b.tracks().keySet())
				if (!a.tracks().containsKey(bone))
					assertTrue(HIS_OWN.contains(bone), b.name() + " adds a track to " + bone);
			for (String bone : b.steps().keySet())
				if (!a.steps().containsKey(bone))
					assertTrue(HIS_OWN.contains(bone), b.name() + " adds a step to " + bone);
		}
	}

	/** The beard and the monocle's chain hang and swing in every clip; the monocle pops when he's startled (brief). */
	@Test
	void hisBeardMovesEverywhereAndTheMonoclePopsWhenStartled() {
		for (Clip b : MASTER.all()) {
			assertNotNull(b.track("beard", "rotation"), b.name() + " beard");
			assertNotNull(b.track("monocle_chain", "rotation"), b.name() + " chain");
		}
		for (String name : List.of("sees_player", "expr_startled", "scared_spot", "wall_bump"))
			assertNotNull(MASTER.clip(name).track("monocle", "position"), name + " monocle pop");
	}

	@Test
	void everyTrackIsOnABoneOfHisModel() {
		Path file = ModResourceFiles.find("assets", ComedyMonkeysCreateAddons.MOD_ID, "trade_master", "geometry.json");
		Assumptions.assumeTrue(file != null, "geometry.json not reachable");
		Set<String> bones = new HashSet<>();
		for (JsonElement bone : ModResourceFiles.readJson(file).getAsJsonObject().getAsJsonArray("bones"))
			bones.add(bone.getAsJsonObject().get("name").getAsString());
		for (Clip clip : MASTER.all()) {
			for (String bone : clip.tracks().keySet())
				assertTrue(bones.contains(bone), clip.name() + " animates unknown bone " + bone);
			for (String bone : clip.steps().keySet())
				assertTrue(bones.contains(bone), clip.name() + " switches unknown bone " + bone);
		}
	}
}
