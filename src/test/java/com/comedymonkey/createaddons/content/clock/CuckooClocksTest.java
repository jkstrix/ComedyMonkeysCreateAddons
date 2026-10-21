package com.comedymonkey.createaddons.content.clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.testutil.TestConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.phys.Vec3;

/**
 * Which villagers a Cuckoo Clock reaches, and the restock rule it gives them. A clock reaches a cylinder round its
 * block's centre: {@code radius + 0.5} across the ground, and as far up and down.
 */
class CuckooClocksTest {
	private static final BlockPos CLOCK = new BlockPos(0, 64, 0);
	/** The clock block's centre, which reach is measured from. */
	private static final double CX = 0.5, CY = 64.5, CZ = 0.5;
	private static final int R = 64;
	private static final double EDGE = R + 0.5;

	@BeforeEach
	void setUp() {
		TestConfig.loadDefaults();
	}

	@AfterEach
	void tearDown() {
		TestConfig.reset();
	}

	private static boolean reaches(double dx, double dy, double dz) {
		return CuckooClocks.reaches(CLOCK, new Vec3(CX + dx, CY + dy, CZ + dz), R);
	}

	// ---------------------------------------------------------------- reach across the ground

	@Test
	void reachesRightUpToTheEdgeInEveryDirection() {
		for (double[] d : new double[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
			assertTrue(reaches(d[0] * EDGE, 0, d[1] * EDGE), "at the edge " + d[0] + "," + d[1]);
			assertFalse(reaches(d[0] * (EDGE + 0.01), 0, d[1] * (EDGE + 0.01)), "just past " + d[0] + "," + d[1]);
		}
	}

	@Test
	void theEdgeIsARoundCircleNotASquare() {
		double diagonal = EDGE / Math.sqrt(2);
		assertTrue(reaches(diagonal - 0.01, 0, diagonal - 0.01), "just inside on the diagonal");
		assertFalse(reaches(diagonal + 0.05, 0, diagonal + 0.05), "just outside on the diagonal");
		assertFalse(reaches(R, 0, R), "the corner of the square around the circle");
	}

	@Test
	void reachesTheWholeOfABigHallFromOneWall() {
		// a 60 x 40 hall with the clock in the middle of a short wall
		for (int x = 0; x <= 60; x += 5)
			for (int z = -20; z <= 20; z += 5)
				assertTrue(reaches(x, 0, z), "villager at " + x + "," + z);
	}

	// ---------------------------------------------------------------- reach up and down

	@Test
	void reachesAsFarUpAndDownAsAcross() {
		assertTrue(reaches(0, EDGE, 0));
		assertTrue(reaches(0, -EDGE, 0));
		assertFalse(reaches(0, EDGE + 0.01, 0));
		assertFalse(reaches(0, -EDGE - 0.01, 0));
	}

	@Test
	void isACylinderNotASphere() {
		// 60 out and 60 up is 85 from the clock in a straight line, but inside the cylinder
		assertTrue(reaches(60, 60, 0));
		assertTrue(reaches(-45, -60, 45));
		// inside the circle across the ground but too far up
		assertFalse(reaches(10, 70, 10));
	}

	@Test
	void villagersOnOtherFloorsOfTheHallCount() {
		for (int floor = -3; floor <= 3; floor++)
			assertTrue(reaches(30, floor * 8, 10), "floor " + floor);
	}

	// ---------------------------------------------------------------- anywhere in the world

	@Test
	void measuredFromTheClockWhereverItIs() {
		BlockPos far = new BlockPos(-30_000, -40, 12_345);
		Vec3 centre = far.getCenter();
		assertTrue(CuckooClocks.reaches(far, centre.add(-EDGE, 0, 0), R));
		assertFalse(CuckooClocks.reaches(far, centre.add(-EDGE - 0.01, 0, 0), R));
		assertTrue(CuckooClocks.reaches(far, centre.add(0, -EDGE, 0), R), "below y 0 too");
		assertFalse(CuckooClocks.reaches(far, new Vec3(CX, CY, CZ), R), "not the origin's clock");
	}

	@Test
	void theClocksOwnBlockAndItsNeighboursAreInReach() {
		assertTrue(reaches(0, 0, 0));
		assertTrue(CuckooClocks.reaches(CLOCK, new Vec3(1.5, 64, 0.5), 1), "next door, at radius 1");
		assertFalse(CuckooClocks.reaches(CLOCK, new Vec3(2.5, 64, 0.5), 1), "two along, at radius 1");
	}

	// ---------------------------------------------------------------- the configured radius

	@Test
	void defaultReachIsBig() {
		assertEquals(64, CuckooClocks.radius());
	}

	@Test
	void followsTheConfiguredRadius() {
		TestConfig.set(CMConfig.SERVER.clockRadius, 16);
		int r = CuckooClocks.radius();
		assertEquals(16, r);
		assertTrue(CuckooClocks.reaches(CLOCK, new Vec3(CX + 16, CY, CZ), r));
		assertFalse(CuckooClocks.reaches(CLOCK, new Vec3(CX + 30, CY, CZ), r), "64 reach would have it");
		TestConfig.set(CMConfig.SERVER.clockRadius, 256);
		assertTrue(CuckooClocks.reaches(CLOCK, new Vec3(CX + 200, CY - 100, CZ), CuckooClocks.radius()));
	}

	// ---------------------------------------------------------------- several clocks

	@Test
	void anyRunningClockWillDo() {
		Vec3 villager = new Vec3(150.5, 64, 0.5);
		assertFalse(CuckooClocks.anyReaches(Set.of(), villager, R), "no clocks");
		assertFalse(CuckooClocks.anyReaches(List.of(CLOCK), villager, R), "too far from the only clock");
		assertTrue(CuckooClocks.anyReaches(List.of(CLOCK, new BlockPos(100, 64, 0)), villager, R), "the second reaches it");
		assertTrue(CuckooClocks.anyReaches(List.of(new BlockPos(100, 64, 0), CLOCK), villager, R), "in either order");
	}

	// ---------------------------------------------------------------- the restock rule

	private static final ClockCheck SEVEN = new ClockCheck(1000, 1000);

	@Test
	void outOfReachIsLeftToVanilla() {
		assertNull(CuckooClocks.decide(false, SEVEN, new ClockCheck(3000, 3000), true));
		assertNull(CuckooClocks.decide(false, ClockCheck.NEVER, new ClockCheck(3000, 3000), true));
	}

	@Test
	void inReachItRestocksOnceAfterTheHourIfATradeWasUsed() {
		ClockCheck halfPastEight = new ClockCheck(3500, 2500);
		assertEquals(Boolean.TRUE, CuckooClocks.decide(true, SEVEN, halfPastEight, true));
		assertEquals(Boolean.FALSE, CuckooClocks.decide(true, SEVEN, halfPastEight, false), "nothing to restock");
		assertEquals(Boolean.FALSE, CuckooClocks.decide(true, halfPastEight, new ClockCheck(5000, 4000), true),
			"already checked since 8:00");
	}

	@Test
	void aVillagerThatNeverAskedRestocksOnItsFirstCheck() {
		assertEquals(Boolean.TRUE, CuckooClocks.decide(true, ClockCheck.NEVER, new ClockCheck(10, 10), true));
	}

	@Test
	void eightAndNoonAreTheOnlyHours() {
		long day = ClockTime.TICKS_PER_DAY;
		assertTrue(check(1999).hourStruckBy(0, 2000), "8:00");
		assertFalse(check(2000).hourStruckBy(0, 5999), "through the morning");
		assertTrue(check(5999).hourStruckBy(0, 6000), "noon");
		assertFalse(check(6000).hourStruckBy(0, day + 1999), "through the afternoon and night");
		assertTrue(check(6000).hourStruckBy(0, day + 2000), "the next 8:00");
	}

	@Test
	void frozenTimeNeverStrikes() {
		// doDaylightCycle false: the day stands still while the game ticks on
		for (long t : new long[] {0, 2000, 6000, 9000}) {
			ClockCheck first = new ClockCheck(500, t);
			for (long later = 800; later < 100_000; later += 12_345)
				assertFalse(first.hourStruckBy(later, t), "held at " + t + ", " + later + " ticks on");
		}
	}

	@Test
	void anHourThatComesRoundAgainAfterTimeSetCountsAgain() {
		// checked at 9:00 (game tick 5000), then /time set back to 7:00 and played on past 8:00 to 8:10
		ClockCheck nine = new ClockCheck(5000, 3000);
		assertTrue(nine.hourStruckBy(5000 + 1170, 2170));
		// ...but not straight after the rewind, before 8:00 has come round again
		assertFalse(nine.hourStruckBy(5000 + 100, 1100));
	}

	@Test
	void skippingTimeForwardCounts() {
		// /time add a whole day: same time of day, but two hours have struck
		assertTrue(new ClockCheck(0, 3000).hourStruckBy(10, 3000 + ClockTime.TICKS_PER_DAY));
	}

	@Test
	void sleepingThroughTheNightDoesNotRestockBeforeEight() {
		// bed at 19:00, up at 6:00 the next day: no 8:00 or noon in between
		assertFalse(new ClockCheck(0, 13000).hourStruckBy(100, ClockTime.TICKS_PER_DAY));
	}

	private static ClockCheck check(long dayTime) {
		return new ClockCheck(0, dayTime);
	}

	// ---------------------------------------------------------------- who counts as a trader

	@Test
	void onlyGrownVillagersWithATradeCount() {
		assertTrue(CuckooClocks.canTrade(VillagerProfession.FLETCHER, false));
		assertTrue(CuckooClocks.canTrade(VillagerProfession.LIBRARIAN, false));
		assertFalse(CuckooClocks.canTrade(VillagerProfession.FLETCHER, true), "babies");
		assertFalse(CuckooClocks.canTrade(VillagerProfession.NONE, false), "the unemployed");
		assertFalse(CuckooClocks.canTrade(VillagerProfession.NITWIT, false), "nitwits");
	}
}
