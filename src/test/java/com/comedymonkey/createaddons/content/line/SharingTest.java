package com.comedymonkey.createaddons.content.line;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

class SharingTest {
	private static Sharing.Claim claim(int room, int length) {
		return new Sharing.Claim(room, length, null, SplitMode.EVEN, List.of());
	}

	private static Sharing.Claim claim(int room, int length, BlockPos splitter, SplitMode mode) {
		return new Sharing.Claim(room, length, splitter, mode, List.of());
	}

	@Test
	void sharesEvenly() {
		assertArrayEquals(new int[] {50, 50}, Sharing.share(100, new int[] {1000, 1000}));
	}

	@Test
	void theSmallTakeWhatTheyCanAndTheRestIsSplit() {
		assertArrayEquals(new int[] {10, 45, 45}, Sharing.share(100, new int[] {10, 1000, 1000}));
	}

	@Test
	void unevenRemaindersAreNotLost() {
		int[] out = Sharing.share(100, new int[] {1000, 1000, 1000});
		assertEquals(100, Arrays.stream(out).sum());
	}

	@Test
	void neverMoreThanThereIsRoomFor() {
		assertArrayEquals(new int[] {5, 7}, Sharing.share(100, new int[] {5, 7}));
	}

	@Test
	void nearestFirstFillsTheNearEndFirst() {
		BlockPos s = BlockPos.ZERO;
		int[] out = Sharing.allocate(100, List.of(claim(1000, 9, s, SplitMode.NEAREST), claim(1000, 2, s, SplitMode.NEAREST)), new HashMap<>(), new HashMap<>());
		assertArrayEquals(new int[] {0, 100}, out);
	}

	@Test
	void nearestFirstPassesOnWhatTheNearCantTake() {
		BlockPos s = BlockPos.ZERO;
		int[] out = Sharing.allocate(100, List.of(claim(1000, 9, s, SplitMode.NEAREST), claim(30, 2, s, SplitMode.NEAREST)), new HashMap<>(), new HashMap<>());
		assertArrayEquals(new int[] {70, 30}, out);
	}

	@Test
	void roundRobinGivesOneEndATurnEachTick() {
		BlockPos s = BlockPos.ZERO;
		Map<BlockPos, Integer> turns = new HashMap<>();
		List<Sharing.Claim> claims = List.of(claim(1000, 1, s, SplitMode.ROUND_ROBIN), claim(1000, 1, s, SplitMode.ROUND_ROBIN));
		assertArrayEquals(new int[] {100, 0}, Sharing.allocate(100, claims, new HashMap<>(), turns));
		assertArrayEquals(new int[] {0, 100}, Sharing.allocate(100, claims, new HashMap<>(), turns));
	}

	@Test
	void aThrottleCapsEveryEndBeyondIt() {
		BlockPos valve = new BlockPos(1, 0, 0);
		Map<BlockPos, Integer> cap = new HashMap<>(Map.of(valve, 40));
		List<Sharing.Claim> claims = List.of(new Sharing.Claim(1000, 1, null, SplitMode.EVEN, List.of(valve)),
			new Sharing.Claim(1000, 1, null, SplitMode.EVEN, List.of(valve)));
		int[] out = Sharing.allocate(100, claims, cap, new HashMap<>());
		assertEquals(40, Arrays.stream(out).sum());
	}

	/** What a throttle holds back goes to the ends that aren't behind it, so the line still moves its whole rate. */
	@Test
	void aThrottlesShareGoesToTheOthers() {
		BlockPos valve = new BlockPos(1, 0, 0);
		Map<BlockPos, Integer> cap = new HashMap<>(Map.of(valve, 50));
		List<Sharing.Claim> claims = List.of(new Sharing.Claim(1000, 1, null, SplitMode.EVEN, List.of(valve)),
			new Sharing.Claim(1000, 1, null, SplitMode.EVEN, List.of(valve)), claim(1000, 1));
		int[] out = Sharing.allocate(100, claims, cap, new HashMap<>());
		assertEquals(100, Arrays.stream(out).sum());
		assertEquals(50, out[0] + out[1]);
	}

	/** Many ends behind a narrow throttle still get its whole allowance between them, not a rounded-down nothing. */
	@Test
	void aNarrowThrottlePassesItsAllowanceToManyEnds() {
		BlockPos valve = new BlockPos(1, 0, 0);
		Map<BlockPos, Integer> cap = new HashMap<>(Map.of(valve, 10));
		List<Sharing.Claim> claims = new java.util.ArrayList<>();
		for (int i = 0; i < 12; i++)
			claims.add(new Sharing.Claim(1000, 1, null, SplitMode.EVEN, List.of(valve)));
		int[] out = Sharing.allocate(100, claims, cap, new HashMap<>());
		assertEquals(10, Arrays.stream(out).sum());
	}

	@Test
	void endsWithNoSplitterShareWithTheOthers() {
		assertArrayEquals(new int[] {50, 50}, Sharing.allocate(100, List.of(claim(1000, 1), claim(1000, 5)), new HashMap<>(), new HashMap<>()));
	}
}
