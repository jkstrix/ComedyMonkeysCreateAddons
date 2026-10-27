package com.comedymonkey.createaddons.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;

/** The cages' and tanks' queued syncs, spread over the interval by position. */
class SyncStaggerTest {
	private static final int INTERVAL = 10;

	@Test
	void phaseIsWithinTheInterval() {
		for (int x = -40; x <= 40; x++)
			for (int z = -40; z <= 40; z += 7) {
				int phase = SyncStagger.phase(new BlockPos(x, -12 + x % 5, z), INTERVAL);
				assertTrue(phase >= 0 && phase < INTERVAL, "phase " + phase);
			}
	}

	/** A wall of ten cages side by side syncs on ten different ticks, not all on one. */
	@Test
	void aRowSpreadsOverEveryTick() {
		for (int z : new int[] {0, 5, -300}) {
			Set<Integer> phases = new HashSet<>();
			for (int x = 100; x < 100 + INTERVAL; x++)
				phases.add(SyncStagger.phase(new BlockPos(x, 64, z), INTERVAL));
			assertEquals(INTERVAL, phases.size());
		}
	}

	@Test
	void eachPhaseIsDueOnceAnInterval() {
		for (int phase = 0; phase < INTERVAL; phase++) {
			int due = 0;
			for (long time = 1000; time < 1000 + 5 * INTERVAL; time++)
				if (SyncStagger.due(time, phase, INTERVAL))
					due++;
			assertEquals(5, due, "phase " + phase);
		}
	}

	/** In any one tick only the cages of one phase sync. */
	@Test
	void differentPhasesAreDueOnDifferentTicks() {
		for (long time = 0; time < INTERVAL; time++) {
			int due = 0;
			for (int phase = 0; phase < INTERVAL; phase++)
				if (SyncStagger.due(time, phase, INTERVAL))
					due++;
			assertEquals(1, due);
		}
	}
}
