package com.comedymonkey.createaddons.content.tradebot.react;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.content.tradebot.clip.ClipLibrary;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipPlayback;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Milestone 6's pure parts: where he hides from a mob, how often he reacts, and how often he speaks. */
class FeelingAliveTest {
	/**
	 * A 7 x 7 room (x and z 0..6) walled all round, floor everywhere inside. He's in the middle, the mob by the west wall:
	 * the far (east) corners are best.
	 */
	private static boolean inRoom(BlockPos pos) {
		return pos.getY() == 0 && pos.getX() >= 0 && pos.getX() <= 6 && pos.getZ() >= 0 && pos.getZ() <= 6;
	}

	private static boolean roomWall(BlockPos pos) {
		return pos.getY() == 0 && !inRoom(pos) && pos.getX() >= -1 && pos.getX() <= 7 && pos.getZ() >= -1 && pos.getZ() <= 7;
	}

	@Test
	void heHidesInTheCornerFarthestFromTheMob() {
		BlockPos corner = CornerFinder.find(new BlockPos(3, 0, 3), new Vec3(0.5, 0, 3.5), 8, FeelingAliveTest::inRoom, FeelingAliveTest::roomWall);
		assertTrue(Set.of(new BlockPos(6, 0, 0), new BlockPos(6, 0, 6)).contains(corner), "hid at " + corner);
	}

	@Test
	void aCornerBeatsAnOpenSpotFartherAway() {
		// an open floor 20 wide with one corner nook near him; the open far edge is farther from the mob but has no walls
		Set<BlockPos> walls = Set.of(new BlockPos(2, 0, -1), new BlockPos(3, 0, 0));
		BlockPos hide = CornerFinder.find(new BlockPos(0, 0, 0), new Vec3(-3, 0, 0.5), 6,
			pos -> pos.getY() == 0 && Math.abs(pos.getX()) <= 10 && Math.abs(pos.getZ()) <= 10 && !walls.contains(pos), walls::contains);
		// (2, 0, 0) and (3, 0, -1) are both nooks between the two walls
		assertEquals(2, CornerFinder.walls(hide, walls::contains), "hid in the open at " + hide);
	}

	@Test
	void onlyWhereHeCanGetTo() {
		// boxed in where he stands: nowhere to go
		assertNull(CornerFinder.find(new BlockPos(0, 0, 0), new Vec3(5, 0, 5), 8, pos -> pos.equals(BlockPos.ZERO), pos -> !pos.equals(BlockPos.ZERO)));
		// spots beyond the radius don't count
		BlockPos near = CornerFinder.find(new BlockPos(0, 0, 0), new Vec3(-1, 0, 0), 2, pos -> pos.getY() == 0, pos -> false);
		assertTrue(near == null || near.distManhattan(BlockPos.ZERO) <= 2, "went to " + near);
	}

	@Test
	void reactionsWaitTheirCooldown() {
		Reactions reactions = new Reactions();
		assertTrue(reactions.tryPlay(Reactions.Kind.WALL_BUMP, 100));
		assertFalse(reactions.tryPlay(Reactions.Kind.WALL_BUMP, 299));
		assertTrue(reactions.tryPlay(Reactions.Kind.WALL_BUMP, 300));
		assertTrue(reactions.tryPlay(Reactions.Kind.CROSS, 101), "each kind has its own cooldown");
	}

	@Test
	void aPlayerIsGreetedOnlyAfterBeingAwayAMinute() {
		Reactions reactions = new Reactions();
		UUID player = UUID.randomUUID();
		assertTrue(reactions.arrived(player, 0));
		assertFalse(reactions.arrived(player, 20), "still around");
		assertFalse(reactions.arrived(player, 20 + Reactions.AWAY_TICKS - 1));
		assertTrue(reactions.arrived(player, 20 + Reactions.AWAY_TICKS - 1 + Reactions.AWAY_TICKS), "back after a minute away");
	}

	@Test
	void playersLongGoneAreForgotten() {
		Reactions reactions = new Reactions();
		for (int i = 0; i < 20; i++)
			reactions.arrived(UUID.randomUUID(), i);
		UUID back = UUID.randomUUID();
		assertTrue(reactions.arrived(back, 20 + Reactions.AWAY_TICKS));
		assertEquals(1, reactions.remembered(), "the twenty who left a minute ago would be greeted afresh anyway");
	}

	/** Plays {@code clip} from {@code start} for {@code seconds}, a tick at a time; returns the events fired. */
	private static List<String> play(ClipPlayback playback, String clip, long start, float seconds) {
		List<String> fired = new ArrayList<>();
		playback.observe(ClipLibrary.get().clip(clip), start, 1, start);
		for (long tick = start; tick <= start + (long) (seconds * 20); tick++)
			playback.tickCues(tick, fired::add);
		return fired;
	}

	@Test
	void aClipSaysEverythingButTheNextOneWaitsItsTurn() {
		ClipPlayback playback = new ClipPlayback();
		List<String> trade = play(playback, "trade_villager", 0, 7.2f);
		assertTrue(trade.containsAll(List.of("hi_boop", "haggle_chatter", "plead_chirp", "pleased_boop", "merchant_trill", "bye_boop")),
			"a clip's own voice cues all play: " + trade);
		// bye_boop at 6.45 s; a turn-around a second later doesn't shout "wheee" over it
		List<String> turn = play(playback, "turn_around", 150, 2);
		assertFalse(turn.contains("wheee"));
		assertTrue(turn.contains("hop_launch"), "only the voice waits");
		List<String> later = play(playback, "turn_around", 400, 2);
		assertTrue(later.contains("wheee"), "a few seconds on, he speaks again");
	}

	@Test
	void aLoopingClipSpeaksEveryOtherTimeRound() {
		ClipPlayback playback = new ClipPlayback();
		List<String> flee = play(playback, "scared_flee", 0, 6);
		assertEquals(2, flee.stream().filter("panic_chatter"::equals).count(), "four loops: " + flee);
	}
}
