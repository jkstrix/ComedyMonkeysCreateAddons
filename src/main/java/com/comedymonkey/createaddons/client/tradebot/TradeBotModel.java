package com.comedymonkey.createaddons.client.tradebot;

import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.comedymonkey.createaddons.content.tradebot.TradeBotEntity;
import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipPlayback;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipRoot;
import com.comedymonkey.createaddons.content.tradebot.clip.StepTrack;
import com.comedymonkey.createaddons.content.tradebot.clip.Track;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** Trade Bot posed from his clips, exactly as the Blockbench preview plays them, with a short fade between clips. */
public class TradeBotModel extends HierarchicalModel<TradeBotEntity> {
	private static final String[] CHANNELS = { "rotation", "position", "scale" };

	private final ModelPart root;
	private final Map<String, ModelPart> parts;
	private final Vector3f value = new Vector3f(), from = new Vector3f();

	public TradeBotModel(ModelPart root) {
		this.root = root;
		this.parts = TradeBotGeometry.get().parts(root, ClipRoot.BONE);
	}

	@Override
	public ModelPart root() {
		return root;
	}

	@Override
	public void setupAnim(TradeBotEntity bot, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		root.getAllParts().forEach(ModelPart::resetPose);
		double now = bot.level().getGameTime() + (ageInTicks - bot.tickCount);
		ClipPlayback playback = bot.playback;
		playback.observe(bot.clip(), bot.clipStart(), now);
		Clip clip = playback.clip();
		if (clip == null)
			return;
		float t = clip.localTime(playback.elapsed(now));
		Clip old = playback.fadeFrom(now);
		float w = playback.fade(now);
		float oldT = old == null ? 0 : old.localTime(playback.fadeFromElapsed(now));
		for (Map.Entry<String, ModelPart> entry : parts.entrySet()) {
			for (String channel : CHANNELS) {
				boolean any = sample(clip, entry.getKey(), channel, t, value);
				if (old != null)
					any |= sample(old, entry.getKey(), channel, oldT, from);
				if (!any)
					continue;
				if (old != null)
					from.lerp(value, w, value);
				apply(entry.getValue(), channel, value);
			}
		}
	}

	/** One channel of a bone at {@code t}, or its rest value if the clip leaves it alone (then returns false). */
	static boolean sample(Clip clip, String bone, String channel, float t, Vector3f out) {
		boolean scale = channel.equals("scale");
		Track track = clip.track(bone, channel);
		StepTrack step = track == null ? clip.step(bone, channel) : null;
		if (track == null && step == null || bone.equals(ClipRoot.BONE) && clip.root() == Clip.Root.PHYSICAL) {
			out.set(scale ? 1 : 0);
			return false;
		}
		if (track != null)
			track.sample(t, out);
		else
			step.sample(t, out);
		// in game clips his own movement does the travelling and turning; only the hop height stays in the pose
		if (bone.equals(ClipRoot.BONE) && clip.root() == Clip.Root.GAME) {
			if (channel.equals("rotation"))
				out.zero();
			else if (channel.equals("position"))
				out.set(0, out.y, 0);
		}
		return true;
	}

	/** Adds a Blockbench-frame value onto a part; see {@link TradeBotGeometry} for the frame change. */
	static void apply(ModelPart part, String channel, Vector3f v) {
		switch (channel) {
			case "rotation" -> {
				part.xRot += -v.x * Mth.DEG_TO_RAD;
				part.yRot += -v.y * Mth.DEG_TO_RAD;
				part.zRot += v.z * Mth.DEG_TO_RAD;
			}
			case "position" -> {
				part.x += -v.x;
				part.y += -v.y;
				part.z += v.z;
			}
			default -> {
				part.xScale *= v.x;
				part.yScale *= v.y;
				part.zScale *= v.z;
			}
		}
	}

	@Nullable
	public ModelPart part(String bone) {
		return parts.get(bone);
	}
}
