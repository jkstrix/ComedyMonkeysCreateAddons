package com.comedymonkey.createaddons.client.helperbot;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipLibrary;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipPlayback;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * One Helper Bot's animation, client side: which clip he plays, chosen each tick from his {@link Situation} (the
 * state table in the brief's Motion Set), and the layers the game adds on top of the clips: his caps and schedule
 * card shown by state, the duck, the train's pull on his gimbal spring, and his swivel toward a nearby player.
 */
public final class HelperBotAnimator {
	public enum Mode {
		/** Placed, with no job: idle, mannerisms, reactions. */
		OFF,
		/** Beside a Stock Ticker: keeping shop, in his logistics cap. */
		SHOP,
		/** On a train, directly behind its controls. */
		DRIVING,
		/** On a contraption without being in control (a second bot, or anything that isn't a train). */
		PASSENGER
	}

	/** What's around him this tick. */
	public static final class Situation {
		public Mode mode = Mode.OFF;
		public boolean hasSchedule;
		/** His train's speed, in blocks per tick. */
		public float speed;
		/** Whether his train moves (or last moved) the way he faces: on a two-way train the bot at the front drives. */
		public boolean forward = true;
		/** How much he faces the way the train moves: 1 ahead, -1 back, 0 across (the share of its pull that leans him). */
		public float along = 1;
		public boolean honk;
		public boolean ducked;
		/** Degrees to the nearest player within reach: 0 straight ahead, + to his left; NaN for nobody. */
		public float playerYaw = Float.NaN;
		public boolean hostileNear;
		public boolean raining;
		public boolean thundering;
		public boolean night;
		public boolean wave;
		/** At his shop, an order just went out. */
		public boolean order;
		/** From the server ({@code HelperBotStatusPayload}): the schedule is paused, a player is driving, lost, derailed. */
		public boolean paused, manual, noPath, derailed;
	}

	/** Off-duty mannerisms (one every 6–15 s), and the ones he does at the controls while the train waits. */
	private static final List<String> MANNERISMS = List.of("check_watch", "polish_lens", "toot_tune", "expr_curious", "expr_proud");
	private static final List<String> DRIVING_MANNERISMS = List.of("adjust_cap", "dust_controls");
	/** A train moving slower than this (blocks per tick) is standing. */
	private static final float MOVING = 0.01f;
	/** A train losing speed faster than this (blocks per tick, each tick; smoothed) is braking. */
	private static final float BRAKING = 0.0015f;

	private final ClipLibrary lib = HelperBotModels.clips();
	private final ClipPlayback playback = new ClipPlayback();
	private final RandomSource random = RandomSource.create();
	private Clip clip;
	private long start;
	private Mode lastMode = Mode.OFF;
	private boolean lastSchedule, lastDucked, lastMoving, lastPlayerNear, scared, honking, crashed, braked;
	private int lastStatus;
	private long nextMannerism, lastSawPlayer = Long.MIN_VALUE / 2, lastThrottle = Long.MIN_VALUE / 2, lastNoPath = Long.MIN_VALUE / 2;
	private float lastSpeed, accel;
	/** Swivel toward a player (degrees, his left +), and the train's pull on his gimbal (degrees, top back +). */
	private float swivel, swivelOld, lean, leanOld, leanVel;
	private boolean started;

	public HelperBotAnimator() {
		clip = lib.clip("idle");
		playback.loopsAsCues();
	}

	/** His voice's cooldown (D27), from the client config. */
	private static double voiceCooldownTicks() {
		try {
			return com.comedymonkey.createaddons.config.CMConfig.CLIENT.helperBotVoiceCooldown.get() * 20.0;
		} catch (IllegalStateException e) {
			return 400;
		}
	}

	/** Once a client tick: picks his clip, plays its sound cues, eases the swivel and the lean. */
	public void tick(Situation s, long now, Consumer<String> sounds) {
		playback.voiceCooldown(voiceCooldownTicks());
		// the animation clock starts again (it wraps after a day of play): so does he
		if (started && now < start) {
			started = false;
			lastSawPlayer = lastThrottle = lastNoPath = Long.MIN_VALUE / 2;
			playback.restartClock();
		}
		if (!started) {
			started = true;
			start = now;
			lastSchedule = s.hasSchedule;
			lastDucked = s.ducked;
			lastMode = s.mode;
			// already derailed when he's first seen: he sulks, he doesn't crash again
			crashed = s.derailed;
			nextMannerism = now + 120 + random.nextInt(180);
			clip = lib.clip(base(s, false));
		}
		boolean busy = clip.mode() != Clip.Mode.LOOP && !clip.finished((now - start) / 20f);
		if (s.mode != lastMode) {
			lastMode = s.mode;
			busy = false;
			lastSchedule = s.hasSchedule;
			scared = false;
		}
		accel = accel * 0.75f + (s.speed - lastSpeed) * 0.25f;
		lastSpeed = s.speed;
		boolean moving = s.speed > MOVING;
		switch (s.mode) {
			case DRIVING -> {
				int status = (s.paused ? 1 : 0) | (s.manual ? 2 : 0) | (s.noPath ? 4 : 0) | (s.derailed ? 8 : 0);
				if (status != lastStatus) {
					lastStatus = status;
					busy = false;
				}
				if (!s.derailed)
					crashed = false;
				// he's only scared at a standing train; once it moves, or someone else has it, he's back to work
				if (moving || s.manual || s.paused || s.derailed)
					scared = false;
				if (s.derailed) {
					// thrown forward into the levers, dizzy, then a sulk until the train's back on the rails
					if (!crashed) {
						crashed = true;
						play("crash", now);
					} else if (!busy)
						loop("sulk", now);
				} else if (s.honk && !honking && !busy && s.forward)
					// only a player at the controls can honk: he pulls the whistle cord for them
					play("honk", now);
				else if (s.manual) {
					if (!busy)
						loop("manual_folded", now);
				} else if (s.paused) {
					if (!busy)
						loop("paused_point", now);
				} else if (s.hasSchedule != lastSchedule) {
					lastSchedule = s.hasSchedule;
					play(s.hasSchedule ? "schedule_given" : "schedule_taken", now);
				} else if (moving && !s.forward) {
					// the far end of a two-way train: the bot at the front drives, he rides along
					if (!busy)
						loop("cruise", now);
				} else if (!moving && hostiles(s, now, busy)) {
					// a monster by the train while it stands: he shrinks down at the controls
				} else if (!busy) {
					if (!s.hasSchedule)
						loop("no_schedule", now);
					else if (s.noPath && !moving) {
						// lost: turns the schedule round and upside down now and then
						if (now - lastNoPath > 200) {
							lastNoPath = now;
							play("no_path", now);
						} else
							loop("drive_ready", now);
					} else if (moving && !lastMoving) {
						braked = false;
						play("depart", now);
					} else if (moving && !braked && accel < -BRAKING) {
						// hauls back on the brake as the train slows, not once it's stopped
						braked = true;
						play("brake", now);
					} else if (!moving && lastMoving && !braked && s.forward)
						play("brake", now);
					else if (moving) {
						// done slowing (steady again, or speeding up): the next slow-down is a new brake
						if (accel > -BRAKING / 4)
							braked = false;
						boolean near = !Float.isNaN(s.playerYaw);
						if (accel > 0.0015f && now - lastThrottle > 200) {
							lastThrottle = now;
							play("throttle_up", now);
						} else if (near && !lastPlayerNear && now - lastSawPlayer > 600) {
							// the occasional wave at someone by the track
							lastSawPlayer = now;
							play("wave", now);
						} else
							loop("cruise", now);
					} else if (s.thundering && random.nextInt(400) == 0)
						play("thunder_flinch", now);
					else if (now >= nextMannerism && isLoop("station_wait")) {
						nextMannerism = now + 240 + random.nextInt(240);
						play(s.raining && random.nextBoolean() ? "rain_wipe" : DRIVING_MANNERISMS.get(random.nextInt(DRIVING_MANNERISMS.size())), now);
					} else
						loop("station_wait", now);
				}
				honking = s.honk;
			}
			case PASSENGER -> {
				if (hostiles(s, now, busy))
					break;
				if (!busy)
					weatherOr("passenger", s, now);
			}
			case SHOP -> {
				boolean near = !Float.isNaN(s.playerYaw);
				if (s.order)
					play("shop_order_nod", now);
				else if (hostiles(s, now, busy)) {
					// a monster in the shop: he shrinks behind the counter
				} else if (s.wave || near && !lastPlayerNear && now - lastSawPlayer > 600) {
					lastSawPlayer = now;
					play("shop_greet", now);
				} else if (!busy)
					weatherOr("shop_idle", s, now);
			}
			case OFF -> offDuty(s, now, busy);
		}
		lastMoving = moving;
		lastPlayerNear = !Float.isNaN(s.playerYaw);
		// he only ducks and stands up as a clip off duty; on the job the duck is a layer, so it's never stale
		if (s.mode != Mode.OFF)
			lastDucked = s.ducked;

		playback.observe(clip, start, 1, now);
		playback.tickCues(now, sounds);

		swivelOld = swivel;
		// he turns to look at a player off duty, in a shop, riding along, and at the controls waiting for a schedule
		boolean looks = s.mode == Mode.OFF || s.mode == Mode.SHOP || s.mode == Mode.PASSENGER || s.mode == Mode.DRIVING && !s.hasSchedule;
		float target = looks && !Float.isNaN(s.playerYaw) && !s.ducked ? Mth.clamp(s.playerYaw, -100, 100) : 0;
		swivel += (target - swivel) * 0.08f;
		leanOld = lean;
		// the train's pull leans him as much as he faces along it: back the other way, across not at all
		float leanTarget = s.mode == Mode.DRIVING || s.mode == Mode.PASSENGER ? Mth.clamp(accel * 800, -8, 8) * s.along : 0;
		leanVel = (leanVel + (leanTarget - lean) * 0.3f) * 0.75f;
		lean += leanVel;
	}

	private void offDuty(Situation s, long now, boolean busy) {
		if (s.ducked != lastDucked) {
			lastDucked = s.ducked;
			play(s.ducked ? "duck_down" : "duck_up", now);
			return;
		}
		if (hostiles(s, now, busy))
			return;
		if (s.wave) {
			play("wave", now);
			return;
		}
		if (busy)
			return;
		if (s.ducked) {
			loop("idle_ducked", now);
			return;
		}
		boolean near = !Float.isNaN(s.playerYaw);
		if (near && !lastPlayerNear && now - lastSawPlayer > 600) {
			lastSawPlayer = now;
			play("sees_player", now);
		} else if (s.thundering && random.nextInt(400) == 0)
			play("thunder_flinch", now);
		else if (now >= nextMannerism) {
			nextMannerism = now + 120 + random.nextInt(180);
			if (s.raining && random.nextBoolean())
				play("rain_wipe", now);
			else if (s.night && !near && random.nextInt(3) == 0)
				play("doze_off", now);
			else
				play(MANNERISMS.get(random.nextInt(MANNERISMS.size())), now);
		} else
			loop("idle", now);
	}

	/** A monster near him, wherever he is (D33): he shrinks down and cowers, and comes back up once it's gone. */
	private boolean hostiles(Situation s, long now, boolean busy) {
		if (s.hostileNear && !scared) {
			scared = true;
			play("hostile_shrink", now);
			return true;
		}
		if (!s.hostileNear && scared) {
			scared = false;
			play("hostile_recover", now);
			return true;
		}
		if (scared && !busy)
			loop("hostile_cower", now);
		return scared;
	}

	/** Rain and thunder on the job (D34): a flinch, a lens wipe now and then; otherwise the given loop. */
	private void weatherOr(String loop, Situation s, long now) {
		if (s.thundering && random.nextInt(400) == 0)
			play("thunder_flinch", now);
		else if (s.raining && now >= nextMannerism) {
			nextMannerism = now + 120 + random.nextInt(180);
			play("rain_wipe", now);
		} else
			loop(loop, now);
	}

	/** Plays a clip now, whatever he's doing (Ponder scenes); {@code schedule} is the schedule he's to be seen holding. */
	public void force(String name, long now, Mode mode, boolean schedule) {
		started = true;
		// a scene's beats are the scene's: no mannerisms cutting in
		nextMannerism = Long.MAX_VALUE / 2;
		lastMode = mode;
		lastSchedule = schedule;
		play(name, now);
	}

	private void play(String name, long now) {
		clip = lib.clip(name);
		start = now;
	}

	/** Plays a looping clip, unless it's already playing (then it carries on round). */
	private void loop(String name, long now) {
		if (!isLoop(name))
			play(name, now);
	}

	private boolean isLoop(String name) {
		return clip.name().equals(name);
	}

	/** For tests: the clip he's playing, his lean and his swivel (degrees). */
	String clipName() {
		return clip.name();
	}

	float lean() {
		return lean;
	}

	float swivel() {
		return swivel;
	}

	/** Poses the shared model for this bot at {@code now} (ticks with the partial tick), on the main thread. */
	public void pose(HelperBotRig rig, Situation s, double now, float partialTick) {
		pose(rig, s, now, partialTick, playback);
	}

	/**
	 * Poses a Flywheel visual's own skeleton, off the main thread: {@code own} is the visual's playback, which takes
	 * this bot's clips and fades as the client tick left them, so it poses just as the shared model would and never
	 * writes the playback the tick runs on. (Ticks and Flywheel's frame never run at the same time.)
	 */
	public void pose(HelperBotRig rig, Situation s, double now, float partialTick, ClipPlayback own) {
		if (own != playback)
			own.copyTimeline(playback);
		// start from rest, everything shown (the shared model is posed for every bot in turn)
		rig.reset();
		own.observe(clip, start, 1, now);
		Clip current = own.clip();
		if (current == null)
			return;
		Clip old = own.fadeFrom(now);
		rig.pose(current, current.localTime(own.elapsed(now)), old, old == null ? 0 : old.localTime(own.fadeFromElapsed(now)), own.fade(now));

		// caps and the schedule card follow what he's doing (the clips that pop them on and off handle their own)
		if (!current.name().startsWith("schedule_")) {
			boolean trainCap = s.mode == Mode.DRIVING && s.hasSchedule;
			show(rig.capTrain, trainCap);
			show(rig.capLogistics, s.mode == Mode.SHOP && !trainCap);
			if (!trainCap)
				show(rig.scheduleCard, false);
		}
		// under a roof every clip plays ducked (the duck clips are ducked already)
		if (s.ducked && !current.name().contains("duck"))
			duck(rig);
		// the train's pull on his gimbal spring, and a little rattle while it moves
		float leanNow = Mth.lerp(partialTick, leanOld, lean);
		float rattle = s.speed > MOVING && (s.mode == Mode.DRIVING || s.mode == Mode.PASSENGER)
			? 0.35f * Mth.sin((float) now * Mth.TWO_PI * 9 / 20) + 0.2f * Mth.sin((float) now * Mth.TWO_PI * 13 / 20 + 1) : 0;
		HelperBotRig.apply(rig.body, HelperBotRig.ROTATION, leanNow + rattle, 0, 0);
		float swivelNow = Mth.lerp(partialTick, swivelOld, swivel);
		if (swivelNow != 0)
			HelperBotRig.apply(rig.swivel, HelperBotRig.ROTATION, 0, swivelNow, 0);
		// what's squashed to nothing isn't drawn at all
		rig.hideCollapsed();
	}

	/** The duck pose (the brief's as-built numbers), added onto whatever he's playing. */
	private static void duck(HelperBotRig rig) {
		HelperBotRig.apply(rig.post, HelperBotRig.POSITION, 0, -3.75f, 0);
		HelperBotRig.apply(rig.spring, HelperBotRig.SCALE, 1, 0.02f, 1);
		HelperBotRig.apply(rig.body, HelperBotRig.POSITION, 0, -1, 0);
		HelperBotRig.apply(rig.neck, HelperBotRig.POSITION, 0, -1, 0);
		HelperBotRig.apply(rig.whistle, HelperBotRig.ROTATION, 100, 0, 0);
		HelperBotRig.apply(rig.whistleCord, HelperBotRig.ROTATION, -100, 0, 0);
		HelperBotRig.apply(rig.capTrain, HelperBotRig.SCALE, 1, 0.25f, 1);
		HelperBotRig.apply(rig.capLogistics, HelperBotRig.SCALE, 1, 0.25f, 1);
	}

	private static void show(@Nullable ModelPart part, boolean on) {
		if (part == null)
			return;
		part.visible = on;
		if (on) {
			part.xScale = 1;
			part.yScale = 1;
			part.zScale = 1;
		}
	}

	/** Which clip he settles into for a situation. */
	private static String base(Situation s, boolean moving) {
		return switch (s.mode) {
			case DRIVING -> !s.hasSchedule ? "no_schedule" : moving ? "cruise" : "station_wait";
			case PASSENGER -> "passenger";
			case SHOP -> "shop_idle";
			case OFF -> s.ducked ? "idle_ducked" : "idle";
		};
	}
}
