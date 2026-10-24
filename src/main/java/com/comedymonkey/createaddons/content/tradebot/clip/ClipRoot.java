package com.comedymonkey.createaddons.content.tradebot.clip;

import org.joml.Vector3f;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Where a clip's root bone ({@code trade_bot}) takes him, relative to where the clip started. Server and client use
 * the same maths: the server moves him along it in {@link Clip.Root#PHYSICAL} clips, and the client draws him on it.
 * <p>
 * Blockbench's frame has him facing -Z with his right at +X; the root's yaw (+ turns him to his left) and the travel
 * are both measured from the clip's first frame, so a clip that starts turned round (hopping off a block backwards
 * into the preview's camera) still moves him the way he faces.
 */
public final class ClipRoot {
	public static final String BONE = "trade_bot";
	private static final float PX = 1 / 16f;

	private ClipRoot() {}

	/** His travel since the first frame, in blocks: x to his right, y up, z forward. */
	public static Vector3f travel(Clip clip, float seconds, Vector3f out) {
		Track pos = clip.track(BONE, "position");
		if (pos == null)
			return out.zero();
		Vector3f start = pos.at(0, new Vector3f());
		Vector3f now = pos.sample(seconds, new Vector3f()).sub(start);
		// undo the first frame's turn, so the travel is in his own frame
		float yaw0 = startYaw(clip) * Mth.DEG_TO_RAD;
		float c = Mth.cos(-yaw0), s = Mth.sin(-yaw0);
		float x = now.x * c + now.z * s, z = -now.x * s + now.z * c;
		return out.set(x * PX, now.y * PX, -z * PX);
	}

	/** Degrees he has turned since the first frame; + is to his left. */
	public static float turned(Clip clip, float seconds) {
		Track rot = clip.track(BONE, "rotation");
		if (rot == null)
			return 0;
		return rot.sample(seconds, new Vector3f()).y - rot.at(0, new Vector3f()).y;
	}

	/** The world position for {@code travel} from a start point, facing {@code yaw} (Minecraft degrees). */
	public static Vec3 place(Vec3 start, float yaw, Vector3f travel) {
		float r = yaw * Mth.DEG_TO_RAD;
		double fx = -Mth.sin(r), fz = Mth.cos(r); // forward
		double rx = -Mth.cos(r), rz = -Mth.sin(r); // his right
		return start.add(rx * travel.x + fx * travel.z, travel.y, rz * travel.x + fz * travel.z);
	}

	/** His facing (Minecraft degrees) after turning {@code turned} degrees to his left from {@code yaw}. */
	public static float facing(float yaw, float turned) {
		return yaw - turned; // Minecraft yaw grows clockwise seen from above, so a left turn lowers it
	}

	private static float startYaw(Clip clip) {
		Track rot = clip.track(BONE, "rotation");
		return rot == null ? 0 : rot.at(0, new Vector3f()).y;
	}
}
