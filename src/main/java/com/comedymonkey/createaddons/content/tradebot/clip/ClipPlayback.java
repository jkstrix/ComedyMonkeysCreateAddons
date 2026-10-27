package com.comedymonkey.createaddons.content.tradebot.clip;

import java.util.Set;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

/**
 * What one bot's client copy is playing: the clip the server chose, a short cross-fade from the one before, and which
 * sound cues have fired. The server only sends a clip and the tick it started; everything here follows from that.
 */
public final class ClipPlayback {
	/** Seconds to blend from one clip into the next. Designed chains (rolling into a turn) match anyway. */
	public static final float FADE = 0.15f;
	/** Looping sounds are played by their own sound instances, not as cues. */
	private static final Set<String> LOOPS = Set.of("roll_loop", "roll_fast_loop", "tremble_loop");
	/** A clip first seen more than this many seconds in started before this client saw it. */
	private static final float LATE = 0.25f;
	/** His voice: a clip's voice cues all play, but a new clip doesn't speak within this long of another one (D43). */
	static final Set<String> VOICE = Set.of("curious_chirp", "sad_aww", "sigh", "snore", "startled_chirp", "puzzled_hmm", "huh",
		"pleased_boop", "hi_boop", "bye_boop", "found_it", "sleepy_mmm", "haggle_chatter", "plead_chirp", "wheee", "merchant_trill",
		"grumble", "scared_yelp", "panic_chatter", "whimper", "relief_sigh");
	static final double VOICE_COOLDOWN_TICKS = 80;

	@Nullable
	private Clip clip;
	private double start;
	private float speed = 1;
	@Nullable
	private Clip fadeFrom;
	private double fadeFromStart;
	private float fadeFromSpeed = 1;
	private double fadeStart = Double.NEGATIVE_INFINITY;
	private float lastCueTime = Float.NaN;
	/** When the voice last spoke, and in which clip (by its start tick). */
	private double lastVoice = Double.NEGATIVE_INFINITY;
	private double lastVoiceClip = Double.NEGATIVE_INFINITY;
	private int lastVoiceCycle;
	private double voiceCooldown = VOICE_COOLDOWN_TICKS;
	/** Helper Bot: every round of a looping clip is a new line, so it waits out the cooldown too. */
	private boolean everyRoundSpeaks;
	/** Helper Bot has no looping sound instances: his loop sounds (2 s, a round of their clip) play as cues. */
	private boolean loopsAsCues;

	/** A bot whose voice waits {@code ticks} between lines, a looping clip's rounds included (Helper Bot, D27). */
	public void voiceCooldown(double ticks) {
		voiceCooldown = ticks;
		everyRoundSpeaks = true;
	}

	/** Plays the loop sounds as ordinary cues (for a bot without looping sound instances). */
	public void loopsAsCues() {
		loopsAsCues = true;
	}

	/** The clip clock started again (it wraps after a day of play): his last line was "in the future", so forget it. */
	public void restartClock() {
		lastVoice = Double.NEGATIVE_INFINITY;
		lastVoiceClip = Double.NEGATIVE_INFINITY;
	}

	/**
	 * Follows the server's choice; a change starts a cross-fade at {@code now}. Times are in ticks with the partial tick,
	 * on the bot's clip clock ({@code TradeBotEntity.clipNow}).
	 */
	public void observe(@Nullable Clip current, double startTick, float speed, double now) {
		if (current == clip && startTick == start)
			return;
		if (clip != null) {
			fadeFrom = clip;
			fadeFromStart = start;
			fadeFromSpeed = this.speed;
			fadeStart = now;
		}
		clip = current;
		start = startTick;
		this.speed = speed;
		lastCueTime = Float.NaN;
	}

	/**
	 * Takes another playback's clip, its timing and its cross-fade, so a copy posed off the thread that ticks the
	 * original (Helper Bot's Flywheel visuals) poses exactly as the original would. Sound cues aren't copied.
	 */
	public void copyTimeline(ClipPlayback from) {
		clip = from.clip;
		start = from.start;
		speed = from.speed;
		fadeFrom = from.fadeFrom;
		fadeFromStart = from.fadeFromStart;
		fadeFromSpeed = from.fadeFromSpeed;
		fadeStart = from.fadeStart;
	}

	@Nullable
	public Clip clip() {
		return clip;
	}

	/** Seconds the current clip has played at {@code now}. */
	public float elapsed(double now) {
		return (float) ((now - start) / 20) * speed;
	}

	/** The clip being faded out of at {@code now}, or null once the fade is done. */
	@Nullable
	public Clip fadeFrom(double now) {
		return fade(now) < 1 ? fadeFrom : null;
	}

	public float fadeFromElapsed(double now) {
		return (float) ((now - fadeFromStart) / 20) * fadeFromSpeed;
	}

	/** How far the cross-fade has got: 0 just switched, 1 done. */
	public float fade(double now) {
		if (fadeFrom == null)
			return 1;
		float w = (float) ((now - fadeStart) / 20 / FADE);
		if (w >= 1) {
			fadeFrom = null;
			return 1;
		}
		return Math.max(0, w);
	}

	/**
	 * Fires the cues passed since the last call (once per client tick); loops wrap. Voice cues play unless another clip
	 * spoke in the last few seconds, and in a looping clip only every other time round, so it doesn't nag.
	 */
	public void tickCues(double now, Consumer<String> play) {
		if (clip == null)
			return;
		float elapsed = elapsed(now);
		if (clip.finished(elapsed) && !Float.isNaN(lastCueTime) && lastCueTime >= clip.length())
			return;
		float t = clip.localTime(elapsed);
		// first seen well after it started (joined late, or set up already docked): its earlier cues are history
		if (Float.isNaN(lastCueTime) && elapsed > LATE) {
			lastCueTime = clip.finished(elapsed) ? clip.length() : t;
			return;
		}
		float from = Float.isNaN(lastCueTime) ? -1e-4f : lastCueTime;
		for (Clip.Cue cue : clip.cues()) {
			if (!loopsAsCues && LOOPS.contains(cue.event()) || cue.event().startsWith("villager_"))
				continue;
			boolean passed = t >= from ? cue.time() > from && cue.time() <= t : cue.time() > from || cue.time() <= t;
			if (!passed)
				continue;
			if (VOICE.contains(cue.event())) {
				int round = clip.mode() == Clip.Mode.LOOP ? (int) (elapsed / clip.length()) : 0;
				boolean sameLine = start == lastVoiceClip && (!everyRoundSpeaks || round == lastVoiceCycle);
				if (!sameLine && now - lastVoice < voiceCooldown)
					continue;
				if (round % 2 == 1)
					continue;
				lastVoice = now;
				lastVoiceClip = start;
				lastVoiceCycle = round;
			}
			play.accept(cue.event());
		}
		lastCueTime = clip.mode() == Clip.Mode.LOOP ? t : Math.max(t, clip.finished(elapsed) ? clip.length() : t);
	}
}
