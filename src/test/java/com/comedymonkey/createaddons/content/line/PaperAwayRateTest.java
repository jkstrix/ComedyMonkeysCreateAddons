package com.comedymonkey.createaddons.content.line;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

/**
 * A generator's record with free sources (the Clockwork Dynamo's): its full rate while it runs, so its network shows
 * what it really makes, and only its free sources' share while it's on paper. (Which sources count while away, by
 * whether they're running, needs a level: the game tests cover that.)
 */
class PaperAwayRateTest {
	private static final BlockPos WHEEL = new BlockPos(1, 0, 0), MILL = new BlockPos(2, 0, 0);

	private static PowerGrid.Paper record(int rate, Map<BlockPos, Double> shares) {
		PowerGrid.Paper p = new PowerGrid.Paper("test", 0);
		p.set(rate, 0, 1_000_000);
		if (shares != null)
			p.away(shares);
		return p;
	}

	@Test
	void runningMakesTheFullRate() {
		assertEquals(768, record(768, Map.of()).making(null, false), 1e-9);
	}

	@Test
	void awayMakesTheFreeSourcesShare() {
		assertEquals(0, record(768, Map.of()).making(null, true), 1e-9);
		assertEquals(384, record(768, Map.of(WHEEL, 0.25, MILL, 0.25)).making(null, true), 1e-9);
	}

	/** Without free sources (every other generator) it's the same running or away. */
	@Test
	void noFreeSourcesIsTheFullRate() {
		assertEquals(1024, record(1024, null).making(null, true), 1e-9);
		assertEquals(1024, record(1024, null).making(null, false), 1e-9);
	}

	/** Never more away than running, whatever the shares add up to. */
	@Test
	void awayIsCappedByTheRate() {
		assertEquals(100, record(100, Map.of(WHEEL, 0.8, MILL, 0.8)).making(null, true), 1e-9);
	}

	/** Worked forward while away, its store fills at the away rate. */
	@Test
	void storeFillsAtTheAwayRate() {
		PowerGrid.Paper p = record(768, Map.of(WHEEL, 0.5));
		p.at = 0;
		p.advance(100, null);
		assertEquals(38_400, p.store, 1e-6);
	}
}
