package com.comedymonkey.createaddons.content.tradebot.clip;

import org.joml.Vector3f;

/**
 * One bone channel of a clip, sampled at a fixed rate in Blockbench units (degrees, pixels, scale). Looping clips are
 * sampled linearly and the rest with Catmull-Rom, the way Blockbench plays them, so the game matches the preview.
 */
public final class Track {
	private final float fps;
	private final boolean catmullRom;
	private final float[] values; // x, y, z per sample
	private final int count;

	public Track(float fps, boolean catmullRom, float[] values) {
		if (values.length == 0 || values.length % 3 != 0)
			throw new IllegalArgumentException("a track needs whole x, y, z samples");
		this.fps = fps;
		this.catmullRom = catmullRom;
		this.values = values;
		this.count = values.length / 3;
	}

	public int samples() {
		return count;
	}

	/** The value at {@code seconds}, held at the first and last sample outside the track. */
	public Vector3f sample(float seconds, Vector3f out) {
		float f = Math.max(0, seconds * fps);
		int i = (int) f;
		if (i >= count - 1)
			return at(count - 1, out);
		float t = f - i;
		if (!catmullRom) {
			for (int a = 0; a < 3; a++)
				out.setComponent(a, lerp(values[i * 3 + a], values[(i + 1) * 3 + a], t));
			return out;
		}
		int i0 = Math.max(0, i - 1), i3 = Math.min(count - 1, i + 2);
		for (int a = 0; a < 3; a++)
			out.setComponent(a, catmullRom(values[i0 * 3 + a], values[i * 3 + a], values[(i + 1) * 3 + a], values[i3 * 3 + a], t));
		return out;
	}

	public Vector3f at(int sample, Vector3f out) {
		return out.set(values[sample * 3], values[sample * 3 + 1], values[sample * 3 + 2]);
	}

	private static float lerp(float a, float b, float t) {
		return a + (b - a) * t;
	}

	static float catmullRom(float p0, float p1, float p2, float p3, float t) {
		float t2 = t * t, t3 = t2 * t;
		return 0.5f * (2 * p1 + (p2 - p0) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (3 * p1 - p0 - 3 * p2 + p3) * t3);
	}
}
