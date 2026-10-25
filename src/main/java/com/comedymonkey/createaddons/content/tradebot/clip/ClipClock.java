package com.comedymonkey.createaddons.content.tradebot.clip;

/**
 * The clock Trade Bot's clips run on in a client's world, in server ticks. The client counts game time itself and has
 * it corrected to the server's every 20 ticks, so on a server running behind (or over a jittery connection) the world
 * time jumps under his clips about once a second. This clock never jumps: it advances every client tick at the rate
 * the server's ticks are really arriving, so a lagging server slows his clips down smoothly, in step with how fast the
 * server actually moves him, instead of them running ahead and being pulled back.
 * <p>
 * Ticked by the client ({@code ComedyMonkeysCreateAddonsClient}); the server and Ponder keep their own clocks.
 */
public final class ClipClock {
	/** Client ticks over which the server's pace is measured, and how often it's re-measured. */
	private static final int WINDOW = 40, EVERY = 10;
	/** How much of each new measurement is taken in, so one late time packet doesn't show. */
	private static final float EASE = 0.3f;
	/** Server ticks per client tick: never stops him outright, and only a little faster than real time to catch up. */
	private static final float SLOWEST = 0.2f, FASTEST = 1.05f;

	private static final long[] SAMPLES = new long[WINDOW];
	private static double now;
	private static float rate = 1;
	private static int ticks;
	private static Object world;

	private ClipClock() {}

	/** Once per client tick while the world is running, before its entities tick, with the client's game time. */
	public static void tick(Object level, long gameTime) {
		if (level != world) {
			world = level;
			ticks = 0;
			rate = 1;
		}
		now += rate;
		SAMPLES[ticks % WINDOW] = gameTime;
		ticks++;
		if (ticks > WINDOW && ticks % EVERY == 0) {
			// the oldest sample in the ring is WINDOW - 1 client ticks before this one
			float measured = (gameTime - SAMPLES[ticks % WINDOW]) / (float) (WINDOW - 1);
			rate = Math.clamp(rate + (measured - rate) * EASE, SLOWEST, FASTEST);
		}
	}

	/** The time, in server ticks, {@code partialTick} into the current client tick. */
	public static double now(float partialTick) {
		return now + partialTick * rate;
	}
}
