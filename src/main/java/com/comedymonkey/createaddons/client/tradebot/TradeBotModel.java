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

	public TradeBotModel(ModelPart root) {
		this.root = root;
		this.parts = TradeBotGeometry.get().parts(root, TradeBotGeometry.BOT_ROOTS);
	}

	@Override
	public ModelPart root() {
		return root;
	}

	@Override
	public void setupAnim(TradeBotEntity bot, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		root.getAllParts().forEach(ModelPart::resetPose);
		float partialTick = ageInTicks - bot.tickCount;
		pose(parts, bot, bot.level().getGameTime() + partialTick);
		// docked and charging: the station's winder keeps turning his key
		ModelPart key = parts.get("key");
		if (key != null)
			key.zRot += -bot.windSpin(partialTick) * Mth.DEG_TO_RAD;
		// rolling along: the wheel turns with the ground he covers, not the clip's own pace
		ModelPart wheel = parts.get("wheel");
		if (wheel != null && bot.clip().root() == Clip.Root.GAME)
			wheel.xRot = wheel.getInitialPose().xRot + bot.wheelAngle(partialTick);
	}

	/** Poses these parts (his, or his station's) as the bot's playback has them at {@code now}, with the fade. */
	public static void pose(Map<String, ModelPart> parts, TradeBotEntity bot, double now) {
		ClipPlayback playback = bot.playback;
		playback.observe(bot.clip(), bot.clipStart(), bot.clipSpeed(), now);
		Clip clip = playback.clip();
		if (clip == null)
			return;
		Clip old = playback.fadeFrom(now);
		pose(parts, clip, clip.localTime(playback.elapsed(now)), old, old == null ? 0 : old.localTime(playback.fadeFromElapsed(now)),
			playback.fade(now));
	}

	/** Poses parts from {@code clip} at {@code t}, faded in from {@code old} at {@code oldT} by {@code w} (0..1). */
	public static void pose(Map<String, ModelPart> parts, Clip clip, float t, @Nullable Clip old, float oldT, float w) {
		// the trade props only exist in the clips that use them
		for (String prop : TradeBotGeometry.get().children(TradeBotGeometry.PROPS)) {
			ModelPart part = parts.get(prop);
			if (part != null)
				part.visible = clip.track(prop, "scale") != null || clip.step(prop, "scale") != null;
		}
		Vector3f value = new Vector3f(), from = new Vector3f();
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

	public Map<String, ModelPart> parts() {
		return parts;
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
