package com.comedymonkey.createaddons.content.electric;

/**
 * The Clockwork Dynamo's aerial, Trade Bot's copper aerial on its top: upright while it makes power, folded back along
 * the top when it doesn't, and a little spring wobble when its output changes (design/briefs/clockwork-dynamo.md).
 * The curves are the approved Blockbench clips' (design/models/clockwork-dynamo.gen.js), worked out from the time since
 * the last change rather than stepped each tick, so the 4 Hz wobble stays smooth at any frame rate. Each move starts
 * from wherever the aerial is, so nothing jumps. Times are animation ticks (they run in Ponder too). Client-side only;
 * plain maths so it can be tested.
 */
public final class DynamoAerial {
	/** Degrees it leans back towards the base when folded. */
	public static final float FOLD = 70;
	/** After this many seconds every movement has settled. */
	private static final double SETTLED = 3;

	private enum Move {UP, FOLD, BOING}

	private boolean known;
	private boolean working;
	private int setting;
	private Move move;
	private long start;
	private float from, fromRoll;

	/**
	 * Each client tick: what the dynamo is doing now. Starting or stopping springs it up or folds it; a new setting
	 * while it works makes it wobble. The first call only notes where things stand (it was already running when it came
	 * into view), so nothing replays.
	 */
	public void update(boolean working, int setting, long tick) {
		if (!known) {
			known = true;
			this.working = working;
			this.setting = setting;
			return;
		}
		if (working != this.working || working && setting != this.setting) {
			// from where it is now, whatever it was doing
			from = pitch(tick, this.working);
			fromRoll = roll(tick);
			move = working != this.working ? working ? Move.UP : Move.FOLD : Move.BOING;
			start = tick;
		}
		this.working = working;
		this.setting = setting;
	}

	/**
	 * Degrees about its hinge, positive leaning back towards the base, at {@code time} in ticks (with partial ticks).
	 * {@code shown}: whether its block shows it working, used until it has ticked (in view for the first frame, or on a
	 * contraption, where block entities don't tick).
	 */
	public float pitch(double time, boolean shown) {
		boolean on = known ? working : shown;
		double s = seconds(time);
		if (move == null || s > SETTLED)
			return on ? 0 : FOLD;
		return switch (move) {
			case UP -> (float) spring(s, 0.05, from, 0, 2.8, 0.42);
			case FOLD -> (float) spring(s, 0.1, from, FOLD, 1.4, 0.55);
			// the wobble rings about upright, on top of settling back from wherever it was
			case BOING -> (float) (spring(s, 0, from, 0, 2.8, 0.42) + ring(s, 0, -12, 4.3, 0.22));
		};
	}

	/** Degrees it leans to one side: the wobble's lean, which settles back out (never cut off) when another move starts. */
	public float roll(double time) {
		double s = seconds(time);
		if (move == null || s > SETTLED)
			return 0;
		double settle = spring(s, 0, fromRoll, 0, 3, 0.6);
		return (float) (move == Move.BOING ? settle + ring(s, 0.05, 5, 2.6, 0.25) : settle);
	}

	private double seconds(double time) {
		return Math.max(0, (time - start) / 20.0);
	}

	/** A damped spring from {@code from} to {@code to}, let go at {@code t0} seconds. */
	static double spring(double t, double t0, double from, double to, double hz, double damping) {
		if (t <= t0)
			return from;
		double w = 2 * Math.PI * hz, wd = w * Math.sqrt(1 - damping * damping), s = t - t0;
		return to + (from - to) * Math.exp(-damping * w * s)
			* (Math.cos(wd * s) + damping / Math.sqrt(1 - damping * damping) * Math.sin(wd * s));
	}

	/** A kick that rings down: {@code amplitude} degrees at {@code hz}, dying away over {@code decay} seconds. */
	static double ring(double t, double t0, double amplitude, double hz, double decay) {
		return t <= t0 ? 0 : amplitude * Math.exp(-(t - t0) / decay) * Math.sin(2 * Math.PI * hz * (t - t0));
	}
}
