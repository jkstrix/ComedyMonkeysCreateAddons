package com.comedymonkey.createaddons.content.clock;

/**
 * Game time for the Cuckoo Clock. A day is 24000 ticks from 6:00, so 8:00 is tick 2000 and 12:00 is tick 6000.
 * The day is split into two <i>slots</i>: the morning slot runs from 8:00 to noon, the noon slot from noon to 8:00 the
 * next day. Slots are numbered from day 0's morning slot, so they count up as time passes (and back down if
 * {@code /time set} winds the clock back).
 * <p>
 * Also the timeline of the show each new slot opens with: the chime, the villager rolling out with a workstation,
 * the cuckoo calling the hour, and everything going back in. The server plays the sounds, the renderer the moves,
 * both from {@link #showLength} and the tick constants here.
 */
public final class ClockTime {
	public static final int TICKS_PER_DAY = 24000;
	/** 8:00 and 12:00, in ticks into the day. */
	public static final int MORNING = 2000, NOON = 6000;

	// the show, in ticks from its start
	/** The four chime notes, eight ticks apart. */
	public static final int CHIME_NOTES = 4, CHIME_GAP = 8;
	/** The villager's doors open, then it rolls out onto the balcony. */
	public static final int LOW_DOORS_OPEN = 20, VILLAGER_OUT = 28, VILLAGER_OUT_TICKS = 16;
	/** The cuckoo's doors open, then it comes out onto its perch. */
	public static final int BIRD_DOORS_OPEN = 40, BIRD_OUT = 46, DOOR_TICKS = 6, BIRD_TICKS = 6;
	/** The first call, and the gap between calls; the second note of each call comes this long after the first. */
	public static final int FIRST_CALL = 56, CALL_GAP = 20, CALL_SECOND_NOTE = 6;

	private ClockTime() {}

	/** Ticks into the day, 0 (6:00) to 23999. */
	public static int timeOfDay(long dayTime) {
		return (int) Math.floorMod(dayTime, (long) TICKS_PER_DAY);
	}

	/** Which restock slot {@code dayTime} falls in: even for a morning, odd for a noon. */
	public static long slot(long dayTime) {
		long sinceMorning = dayTime - MORNING;
		long day = Math.floorDiv(sinceMorning, (long) TICKS_PER_DAY);
		return day * 2 + (Math.floorMod(sinceMorning, (long) TICKS_PER_DAY) >= NOON - MORNING ? 1 : 0);
	}

	/** Ticks since the slot {@code dayTime} is in began. */
	public static int intoSlot(long dayTime) {
		int t = timeOfDay(dayTime);
		if (t >= NOON)
			return t - NOON;
		if (t >= MORNING)
			return t - MORNING;
		return t + TICKS_PER_DAY - NOON;
	}

	/** Ticks until the next 8:00 or 12:00. */
	public static int untilNextSlot(long dayTime) {
		int t = timeOfDay(dayTime);
		if (t < MORNING)
			return MORNING - t;
		if (t < NOON)
			return NOON - t;
		return TICKS_PER_DAY - t + MORNING;
	}

	/** The hour a slot opens at, which is how many times the cuckoo calls: 8 or 12. */
	public static int hourOf(long slot) {
		return Math.floorMod(slot, 2L) == 0 ? 8 : 12;
	}

	/** The clock face's reading: hours 0-23 and minutes. */
	public static int hours(long dayTime) {
		return (timeOfDay(dayTime) / 1000 + 6) % 24;
	}

	public static int minutes(long dayTime) {
		return timeOfDay(dayTime) % 1000 * 60 / 1000;
	}

	// ---------------------------------------------------------------- the show

	/** Tick of call {@code i} (from 0). */
	public static int callTick(int i) {
		return FIRST_CALL + i * CALL_GAP;
	}

	/** When the last call is done and the bird starts back in (its doors close after it). */
	public static int callsEnd(int calls) {
		return callTick(calls);
	}

	/** Then the villager rolls back in and its doors close. */
	public static int villagerIn(int calls) {
		return callsEnd(calls) + 10;
	}

	public static int showLength(int calls) {
		return villagerIn(calls) + VILLAGER_OUT_TICKS + DOOR_TICKS + 2;
	}

	/** How far open the cuckoo's doors are at {@code t}, 0 to 1. */
	public static float birdDoors(float t, int calls) {
		return opening(t, BIRD_DOORS_OPEN, DOOR_TICKS) - opening(t, callsEnd(calls) + BIRD_TICKS, DOOR_TICKS);
	}

	/** How far out the cuckoo is, 0 (inside) to 1 (on its perch). */
	public static float birdOut(float t, int calls) {
		return opening(t, BIRD_OUT, BIRD_TICKS) - opening(t, callsEnd(calls), BIRD_TICKS);
	}

	/** How far open the villager's doors are. */
	public static float villagerDoors(float t, int calls) {
		return opening(t, LOW_DOORS_OPEN, DOOR_TICKS) - opening(t, villagerIn(calls) + VILLAGER_OUT_TICKS, DOOR_TICKS);
	}

	/** How far out the villager is, 0 (inside) to 1 (out on the balcony). */
	public static float villagerOut(float t, int calls) {
		return opening(t, VILLAGER_OUT, VILLAGER_OUT_TICKS) - opening(t, villagerIn(calls), VILLAGER_OUT_TICKS);
	}

	/** The bird's bob as it calls: 1 at the first note of each call, easing back by the next. */
	public static float birdCall(float t, int calls) {
		if (t < FIRST_CALL || t >= callsEnd(calls))
			return 0;
		float into = (t - FIRST_CALL) % CALL_GAP;
		return into < CALL_SECOND_NOTE * 2 ? 1 - into / (CALL_SECOND_NOTE * 2) : 0;
	}

	/** 0 before {@code start}, easing up to 1 over {@code length} ticks. */
	static float opening(float t, int start, int length) {
		if (t <= start)
			return 0;
		if (t >= start + length)
			return 1;
		float f = (t - start) / length;
		return f * f * (3 - 2 * f);
	}
}
