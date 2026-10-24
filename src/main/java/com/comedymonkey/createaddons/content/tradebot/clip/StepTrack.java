package com.comedymonkey.createaddons.content.tradebot.clip;

import org.joml.Vector3f;

/** A channel that jumps between values (a light switching off, the package appearing in his claw). */
public final class StepTrack {
	private final float[] times;
	private final float[] values; // x, y, z per key

	public StepTrack(float[] times, float[] values) {
		if (times.length == 0 || values.length != times.length * 3)
			throw new IllegalArgumentException("a step track needs one x, y, z per key");
		this.times = times;
		this.values = values;
	}

	/** The last key at or before {@code seconds} (the first key before it starts). */
	public Vector3f sample(float seconds, Vector3f out) {
		int k = 0;
		while (k + 1 < times.length && times[k + 1] <= seconds)
			k++;
		return out.set(values[k * 3], values[k * 3 + 1], values[k * 3 + 2]);
	}
}
