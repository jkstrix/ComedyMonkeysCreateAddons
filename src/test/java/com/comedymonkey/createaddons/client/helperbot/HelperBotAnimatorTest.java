package com.comedymonkey.createaddons.client.helperbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.client.helperbot.HelperBotAnimator.Mode;
import com.comedymonkey.createaddons.client.helperbot.HelperBotAnimator.Situation;

/**
 * Helper Bot's state machine, run headless through whole situations (a train's run, a monster at the shop, a Ponder
 * beat): the fixes from the review, each as the player would see it.
 */
class HelperBotAnimatorTest {
	private static final Consumer<String> QUIET = id -> {};

	private long now = 1000;

	/** One tick of the train's run: its speed, and what the bot played. */
	private record Frame(float speed, String clip) {}

	/** Standing, pulling away, cruising, slowing to a stop, standing (speeds in blocks per tick). */
	private static float[] run() {
		List<Float> speeds = new ArrayList<>();
		for (int i = 0; i < 40; i++)
			speeds.add(0f);
		for (int i = 1; i <= 60; i++)
			speeds.add(i * 0.01f);
		for (int i = 0; i < 300; i++)
			speeds.add(0.6f);
		for (int i = 1; i <= 120; i++)
			speeds.add(Math.max(0, 0.6f - i * 0.005f));
		for (int i = 0; i < 100; i++)
			speeds.add(0f);
		float[] out = new float[speeds.size()];
		for (int i = 0; i < out.length; i++)
			out[i] = speeds.get(i);
		return out;
	}

	private static Situation driving(boolean forward) {
		Situation s = new Situation();
		s.mode = Mode.DRIVING;
		s.hasSchedule = true;
		s.forward = forward;
		s.along = forward ? 1 : -1;
		return s;
	}

	private List<Frame> drive(HelperBotAnimator a, Situation s, float[] speeds, int honkAt) {
		List<Frame> frames = new ArrayList<>();
		for (int i = 0; i < speeds.length; i++) {
			s.speed = speeds[i];
			s.honk = i == honkAt;
			a.tick(s, now++, QUIET);
			frames.add(new Frame(speeds[i], a.clipName()));
		}
		return frames;
	}

	private void ticks(HelperBotAnimator a, Situation s, int n) {
		for (int i = 0; i < n; i++)
			a.tick(s, now++, QUIET);
	}

	/** Ticks until he plays {@code clip}, up to {@code max} ticks; whether he did. */
	private boolean playsWithin(HelperBotAnimator a, Situation s, String clip, int max) {
		for (int i = 0; i < max; i++) {
			a.tick(s, now++, QUIET);
			if (a.clipName().equals(clip))
				return true;
		}
		return false;
	}

	@Test
	void theFrontDriverBrakesAsTheTrainSlowsNotAfterItStops() {
		List<Frame> frames = drive(new HelperBotAnimator(), driving(true), run(), -1);
		assertTrue(frames.stream().anyMatch(f -> f.clip().equals("depart")), "he should toot and pull away");
		Frame brake = frames.stream().filter(f -> f.clip().equals("brake")).findFirst().orElse(null);
		assertTrue(brake != null, "he should pull the brake");
		assertTrue(brake.speed() > 0.1f, "he should brake while the train is still moving, braked at " + brake.speed());
	}

	@Test
	void theFarEndOfATwoWayTrainRidesAlong() {
		List<Frame> frames = drive(new HelperBotAnimator(), driving(false), run(), 200);
		for (String clip : Set.of("depart", "brake", "throttle_up", "honk"))
			assertFalse(frames.stream().anyMatch(f -> f.clip().equals(clip)), "the bot at the back shouldn't play " + clip);
	}

	@Test
	void theFrontDriverHonksForThePlayerDriving() {
		// in game only a player at the controls can honk, so a honk always comes while he has his arms folded
		HelperBotAnimator front = new HelperBotAnimator(), back = new HelperBotAnimator();
		Situation f = driving(true), b = driving(false);
		f.manual = b.manual = true;
		ticks(front, f, 20);
		ticks(back, b, 20);
		f.honk = b.honk = true;
		front.tick(f, now, QUIET);
		back.tick(b, now++, QUIET);
		assertEquals("honk", front.clipName(), "he pulls the whistle cord for the player");
		assertNotEquals("honk", back.clipName(), "only the bot at the front pulls it");
		f.honk = false;
		assertTrue(playsWithin(front, f, "manual_folded", 200), "then folds his arms again");
	}

	@Test
	void aScareAtOneStationDoesntCarryToTheNext() {
		HelperBotAnimator a = new HelperBotAnimator();
		Situation s = driving(true);
		ticks(a, s, 20);
		s.hostileNear = true;
		a.tick(s, now++, QUIET);
		assertEquals("hostile_shrink", a.clipName());
		// the train pulls out while he's still shrinking, with the monster still there; it's left behind
		float[] rest = java.util.Arrays.copyOfRange(run(), 40, run().length);
		drive(a, s, java.util.Arrays.copyOfRange(rest, 0, 5), -1);
		s.hostileNear = false;
		List<Frame> frames = drive(a, s, java.util.Arrays.copyOfRange(rest, 5, rest.length), -1);
		assertFalse(frames.stream().anyMatch(fr -> fr.clip().equals("hostile_recover")), "no relief at the next station: he got over it");
		assertTrue(frames.stream().anyMatch(fr -> fr.clip().equals("brake")), "and he brakes into it");
	}

	@Test
	void heBrakesForEverySlowDownNotJustTheFirst() {
		// cruising, slowing for a curve, steady through it, then slowing into a station
		List<Float> speeds = new ArrayList<>();
		for (int i = 0; i < 40; i++)
			speeds.add(0f);
		for (int i = 1; i <= 60; i++)
			speeds.add(i * 0.01f);
		for (int i = 0; i < 200; i++)
			speeds.add(0.6f);
		for (int i = 1; i <= 60; i++)
			speeds.add(0.6f - i * 0.005f);
		for (int i = 0; i < 200; i++)
			speeds.add(0.3f);
		int stopping = speeds.size();
		for (int i = 1; i <= 60; i++)
			speeds.add(Math.max(0, 0.3f - i * 0.005f));
		for (int i = 0; i < 60; i++)
			speeds.add(0f);
		float[] run = new float[speeds.size()];
		for (int i = 0; i < run.length; i++)
			run[i] = speeds.get(i);
		List<Frame> frames = drive(new HelperBotAnimator(), driving(true), run, -1);
		assertTrue(frames.subList(stopping, frames.size()).stream().anyMatch(fr -> fr.clip().equals("brake")),
			"after a slow-down for a curve he should still brake into the station");
	}

	@Test
	void aBotFacingAcrossTheTrainDoesntLeanForeOrAft() {
		HelperBotAnimator a = new HelperBotAnimator();
		Situation s = new Situation();
		s.mode = Mode.PASSENGER;
		s.along = 0;
		float[] speeds = run();
		for (int i = 0; i < 75; i++) {
			s.speed = speeds[i];
			a.tick(s, now++, QUIET);
		}
		assertEquals(0, a.lean(), 1e-4f);
	}

	@Test
	void hisVoiceComesBackWhenTheAnimationClockWraps() {
		HelperBotAnimator a = new HelperBotAnimator();
		Situation s = driving(true);
		s.hasSchedule = false;
		List<String> heard = new ArrayList<>();
		now = 1_727_000;
		for (int i = 0; i < 400; i++)
			a.tick(s, now++, heard::add);
		heard.clear();
		now = 10;
		for (int i = 0; i < 1200; i++)
			a.tick(s, now++, heard::add);
		assertTrue(heard.contains("curious_chirp"), "after the wrap he should still chirp now and then, heard " + heard);
	}

	@Test
	void aBotFacingBackwardsLeansTheOtherWay() {
		HelperBotAnimator front = new HelperBotAnimator(), back = new HelperBotAnimator();
		Situation f = driving(true), b = driving(false);
		float[] speeds = run();
		float leanFront = 0, leanBack = 0;
		for (int i = 0; i < 75; i++) {
			f.speed = b.speed = speeds[i];
			front.tick(f, now, QUIET);
			back.tick(b, now++, QUIET);
			leanFront = front.lean();
			leanBack = back.lean();
		}
		assertTrue(Math.abs(leanFront) > 1, "pulling away should lean him, leaned " + leanFront);
		assertEquals(leanFront, -leanBack, 1e-4f, "facing back he should lean the other way");
	}

	@Test
	void ponderKeepsHisArmsFoldedAndHasNoMannerisms() {
		HelperBotAnimator a = new HelperBotAnimator();
		Situation s = driving(true);
		s.manual = true;
		a.force("manual_folded", now, Mode.DRIVING, true);
		for (int i = 0; i < 400; i++) {
			a.tick(s, now++, QUIET);
			assertEquals("manual_folded", a.clipName(), "his arms should stay folded (tick " + i + ")");
		}

		HelperBotAnimator b = new HelperBotAnimator();
		Situation waiting = driving(true);
		b.force("station_wait", now, Mode.DRIVING, true);
		for (int i = 0; i < 2000; i++) {
			b.tick(waiting, now++, QUIET);
			assertEquals("station_wait", b.clipName(), "no mannerism should cut into a Ponder beat (tick " + i + ")");
		}
	}

	@Test
	void aShopKeeperCowersFromMonstersAndComesBack() {
		HelperBotAnimator a = new HelperBotAnimator();
		Situation s = new Situation();
		s.mode = Mode.SHOP;
		ticks(a, s, 20);
		s.hostileNear = true;
		a.tick(s, now++, QUIET);
		assertEquals("hostile_shrink", a.clipName());
		assertTrue(playsWithin(a, s, "hostile_cower", 200), "he should cower while it's near");
		s.hostileNear = false;
		a.tick(s, now++, QUIET);
		assertEquals("hostile_recover", a.clipName());
		assertTrue(playsWithin(a, s, "shop_idle", 200), "and then keep shop again");
	}

	@Test
	void hostilesAndWeatherReachHimOnTheJob() {
		Situation shop = new Situation();
		shop.mode = Mode.SHOP;
		shop.raining = true;
		assertTrue(playsWithin(new HelperBotAnimator(), shop, "rain_wipe", 600), "rain should get a lens wipe in a shop");

		Situation riding = new Situation();
		riding.mode = Mode.PASSENGER;
		riding.raining = true;
		assertTrue(playsWithin(new HelperBotAnimator(), riding, "rain_wipe", 600), "and riding along");

		Situation standing = driving(true);
		HelperBotAnimator a = new HelperBotAnimator();
		ticks(a, standing, 20);
		standing.hostileNear = true;
		assertTrue(playsWithin(a, standing, "hostile_shrink", 5), "a monster by a standing train should scare its driver");
	}

	@Test
	void heLooksAtPlayersFromATrainWhenHisHandsAreFree() {
		Situation riding = new Situation();
		riding.mode = Mode.PASSENGER;
		riding.playerYaw = 60;
		HelperBotAnimator passenger = new HelperBotAnimator();
		ticks(passenger, riding, 200);
		assertEquals(60, passenger.swivel(), 1, "a passenger should turn to the player");

		Situation waiting = driving(true);
		waiting.hasSchedule = false;
		waiting.playerYaw = -40;
		HelperBotAnimator noSchedule = new HelperBotAnimator();
		ticks(noSchedule, waiting, 200);
		assertEquals(-40, noSchedule.swivel(), 1, "waiting for a schedule he should look at the player");

		Situation busy = driving(true);
		busy.playerYaw = 60;
		HelperBotAnimator driver = new HelperBotAnimator();
		ticks(driver, busy, 200);
		assertEquals(0, driver.swivel(), 0.01f, "driving to a schedule he keeps his eyes on the line");
	}

	@Test
	void heWavesAtSomeoneByTheTrack() {
		HelperBotAnimator a = new HelperBotAnimator();
		Situation s = driving(true);
		float[] speeds = run();
		for (int i = 0; i < 250; i++) {
			s.speed = speeds[i];
			a.tick(s, now++, QUIET);
		}
		assertEquals("cruise", a.clipName());
		s.playerYaw = 30;
		assertTrue(playsWithin(a, s, "wave", 5), "a player turning up by the track should get a wave");
	}

	@Test
	void aTrainAlreadyDerailedWhenHeLoadsGetsASulkNotACrash() {
		HelperBotAnimator a = new HelperBotAnimator();
		Situation s = driving(true);
		s.derailed = true;
		for (int i = 0; i < 200; i++) {
			a.tick(s, now++, QUIET);
			assertNotEquals("crash", a.clipName(), "he wasn't there for the crash");
		}
		assertEquals("sulk", a.clipName());

		HelperBotAnimator b = new HelperBotAnimator();
		Situation live = driving(true);
		ticks(b, live, 20);
		live.derailed = true;
		b.tick(live, now++, QUIET);
		assertEquals("crash", b.clipName(), "a derailment he's there for still throws him into the levers");
	}

	@Test
	void heCarriesOnWhenTheAnimationClockWraps() {
		HelperBotAnimator a = new HelperBotAnimator();
		Situation s = new Situation();
		now = 1_727_900;
		ticks(a, s, 50);
		now = 10;
		boolean mannerism = false;
		for (int i = 0; i < 600 && !mannerism; i++) {
			a.tick(s, now++, QUIET);
			mannerism = !a.clipName().equals("idle");
		}
		assertTrue(mannerism, "after the clock wraps he should get on with his mannerisms");
	}

	@Test
	void leavingTheShopUnderARoofDoesntDuckHimAgain() {
		HelperBotAnimator a = new HelperBotAnimator();
		Situation s = new Situation();
		s.mode = Mode.SHOP;
		ticks(a, s, 20);
		s.ducked = true;
		ticks(a, s, 20);
		s.mode = Mode.OFF;
		for (int i = 0; i < 100; i++) {
			a.tick(s, now++, QUIET);
			assertNotEquals("duck_down", a.clipName(), "he was already ducked");
		}
	}

	@Test
	void aDuckedShopKeepersCapIsSquashedUnderTheRoof() {
		HelperBotAnimator a = new HelperBotAnimator();
		Situation s = new Situation();
		s.mode = Mode.SHOP;
		s.ducked = true;
		ticks(a, s, 30);
		HelperBotRig rig = HelperBotRig.skeleton();
		a.pose(rig, s, now, 0);
		assertTrue(rig.capLogistics.visible, "he wears his logistics cap in a shop");
		assertTrue(rig.capLogistics.yScale <= 0.25f + 1e-4f, "ducked, the cap should squash like the train cap, was " + rig.capLogistics.yScale);
	}

	@Test
	void theFlatIconIsOpaque() {
		assertEquals(0xFF, HelperBotClient.iconTint(null, 1) >>> 24, "the plain panels need alpha");
		for (net.minecraft.world.item.DyeColor dye : net.minecraft.world.item.DyeColor.values())
			assertEquals(0xFF, HelperBotClient.iconTint(dye, 1) >>> 24, dye + " panels need alpha");
		assertEquals(-1, HelperBotClient.iconTint(net.minecraft.world.item.DyeColor.RED, 0), "the drawing layer isn't tinted");
	}

	@Test
	void howMuchHeFacesTheMove() {
		net.minecraft.world.phys.Vec3 north = new net.minecraft.world.phys.Vec3(0, 0, -1);
		assertEquals(1, HelperBotClient.along(0, 0, -0.4, north), 1e-5f);
		assertEquals(-1, HelperBotClient.along(0, 0, 0.4, north), 1e-5f);
		assertEquals(0, HelperBotClient.along(0.4, 0, 0, north), 1e-5f);
		// a rotated carriage gives a facing with float noise across the track: still across, not ahead or back
		assertEquals(0, HelperBotClient.along(0.4, 0, 0, new net.minecraft.world.phys.Vec3(6e-17, 0, -1)), 1e-5f);
	}

	@Test
	void aCarriageRoofKeepsTheRainOffHim() {
		net.minecraft.core.BlockPos bot = new net.minecraft.core.BlockPos(2, 1, 0);
		java.util.Map<net.minecraft.core.BlockPos, Object> blocks = new java.util.HashMap<>();
		blocks.put(bot, "bot");
		blocks.put(bot.below(), "floor");
		assertFalse(HelperBotClient.roofed(blocks, bot), "an open wagon lets the rain in");
		blocks.put(bot.above(3), "roof");
		assertTrue(HelperBotClient.roofed(blocks, bot), "a roof three blocks up keeps it off");
	}

	@Test
	void yawIsMeasuredFromHisFacingOfAnyLength() {
		// facing north, a player to the west is on his left (+), to the east on his right
		assertEquals(90, HelperBotClient.yawTo(0, -1, -5, 0), 1e-4f);
		assertEquals(-90, HelperBotClient.yawTo(0, -1, 5, 0), 1e-4f);
		// a ship turned 30 degrees: the facing comes back as a scaled vector, the angle doesn't change
		double c = Math.cos(Math.toRadians(30)), si = Math.sin(Math.toRadians(30));
		assertEquals(HelperBotClient.yawTo(c, si, 3, 4), HelperBotClient.yawTo(c * 0.3, si * 0.3, 3, 4), 1e-4f);
	}
}
