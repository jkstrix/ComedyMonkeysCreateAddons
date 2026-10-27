package com.comedymonkey.createaddons.content.tradebot.clip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The voice rules for one bot's clips: Helper Bot's long cooldown holds across a looping clip's rounds (a parked
 * train's chirp, a station's grumble), his tremble plays as a cue, and Trade Bot's rules stay as they were.
 */
class ClipPlaybackVoiceTest {
	private static final ClipLibrary HELPER_BOT = ClipLibrary.of("/assets/comedymonkeysaddons/helper_bot/clips.json");

	/** Plays a clip for {@code ticks} ticks from tick 0; the sound cues heard. */
	private static List<String> listen(ClipPlayback playback, Clip clip, int ticks) {
		List<String> heard = new ArrayList<>();
		for (int t = 0; t <= ticks; t++) {
			playback.observe(clip, 0, 1, t);
			playback.tickCues(t, heard::add);
		}
		return heard;
	}

	private static long voices(List<String> heard) {
		return heard.stream().filter(ClipPlayback.VOICE::contains).count();
	}

	@Test
	void aParkedTrainsChirpWaitsOutTheCooldown() {
		Clip noSchedule = HELPER_BOT.clip("no_schedule");
		ClipPlayback playback = new ClipPlayback();
		playback.voiceCooldown(400);
		long chirps = voices(listen(playback, noSchedule, 2400));
		// 2 minutes parked: before, a chirp every other 3 s round (20); now one every 20 s at most
		assertTrue(chirps >= 1 && chirps <= 7, "a chirp every 20 s at most, heard " + chirps);
	}

	@Test
	void tradeBotsVoiceIsUnchanged() {
		Clip noSchedule = HELPER_BOT.clip("no_schedule");
		// without the Helper Bot rule a looping clip still speaks every other round, as Trade Bot always has
		long chirps = voices(listen(new ClipPlayback(), noSchedule, 2400));
		assertEquals(20, chirps, 1);
	}

	@Test
	void hisTrembleIsHeardWhileHeCowers() {
		Clip cower = HELPER_BOT.clip("hostile_cower");
		ClipPlayback helper = new ClipPlayback();
		helper.loopsAsCues();
		assertTrue(listen(helper, cower, 200).contains("tremble_loop"), "Helper Bot has no loop instance: the tremble is a cue");
		assertTrue(!listen(new ClipPlayback(), cower, 200).contains("tremble_loop"), "Trade Bot plays it from its own sound instance");
	}
}
