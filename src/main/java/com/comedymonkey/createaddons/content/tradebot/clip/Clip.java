package com.comedymonkey.createaddons.content.tradebot.clip;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * One of Trade Bot's animations, exported from the Blockbench preview by {@code design/models/trade-bot.gen.js --game}.
 * Channels are keyed by bone name, then "rotation", "position" or "scale".
 */
public record Clip(String name, float length, Mode mode, Root root, Map<String, Map<String, Track>> tracks,
	Map<String, Map<String, StepTrack>> steps, List<Cue> cues) {

	public enum Mode {
		/** Plays round and round. */
		LOOP,
		/** Plays once; the game moves on when it ends. */
		ONCE,
		/** Plays once and holds its last frame until the game moves on. */
		HOLD
	}

	/** What the root bone's movement means in game. */
	public enum Root {
		/** He really goes there: the server moves him along it (hops, stairs, docking). */
		PHYSICAL,
		/** His own movement does the travelling; only the turn's yaw timing and the hop height are used. */
		GAME,
		/** Part of the pose only (a recoil, a spin): drawn, but he stays where he is. */
		VISUAL
	}

	/** A sound at a time into the clip. */
	public record Cue(float time, String event) {}

	/** Seconds into the clip for {@code elapsed} seconds of playing: wrapped for loops, held at the end otherwise. */
	public float localTime(float elapsed) {
		if (mode == Mode.LOOP)
			return length > 0 ? elapsed % length : 0;
		return Math.min(Math.max(elapsed, 0), length);
	}

	public boolean finished(float elapsed) {
		return mode != Mode.LOOP && elapsed >= length;
	}

	@Nullable
	public Track track(String bone, String channel) {
		Map<String, Track> channels = tracks.get(bone);
		return channels == null ? null : channels.get(channel);
	}

	@Nullable
	public StepTrack step(String bone, String channel) {
		Map<String, StepTrack> channels = steps.get(bone);
		return channels == null ? null : channels.get(channel);
	}
}
