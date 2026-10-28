package com.comedymonkey.createaddons.client.helperbot;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipLibrary;
import com.comedymonkey.createaddons.content.tradebot.clip.StepTrack;
import com.comedymonkey.createaddons.content.tradebot.clip.Track;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.Util;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;

/**
 * One Helper Bot skeleton to pose: the shared baked model's bones (drawn by his renderer), or a Flywheel visual's own
 * cube-less copy (posed on Flywheel's workers, then copied onto its instances). Bones are held by index, in the
 * geometry's order (a parent always before its children), and each clip is compiled once into just the channels it
 * animates, so posing only touches those instead of looking up every bone's every channel.
 */
public final class HelperBotRig {
	/** A binding's channel: Blockbench rotation (degrees), position (pixels), scale. */
	static final int ROTATION = 0, POSITION = 1, SCALE = 2;
	private static final String[] CHANNELS = { "rotation", "position", "scale" };

	/** The layer's root (above {@code helper_bot}), and every bone under it, by index. */
	final ModelPart root;
	final ModelPart[] parts;
	/** The bones the animator moves itself. */
	final ModelPart post, spring, body, neck, whistle, whistleCord, capTrain, capLogistics, scheduleCard, swivel;
	/** A rig is only ever posed by one thread at a time (the main thread, or the one worker its visual runs on). */
	private final Vector3f value = new Vector3f(), from = new Vector3f();

	HelperBotRig(ModelPart root) {
		this.root = root;
		Map<String, ModelPart> byName = HelperBotModels.geometry().parts(root, HelperBotModels.ROOT);
		parts = new ModelPart[Bones.NAMES.length];
		for (int i = 0; i < parts.length; i++)
			parts[i] = byName.get(Bones.NAMES[i]);
		post = part("post");
		spring = part("spring");
		body = part("body");
		neck = part("neck");
		whistle = part("whistle");
		whistleCord = part("whistle_cord");
		capTrain = part("cap_train");
		capLogistics = part("cap_logistics");
		scheduleCard = part("schedule_card");
		swivel = part("swivel");
	}

	/** A cube-less skeleton of his own, for a Flywheel visual to pose (the shared model can't be posed off the main thread). */
	static HelperBotRig skeleton() {
		return new HelperBotRig(HelperBotModels.skeletonLayer().bakeRoot());
	}

	@Nullable
	private ModelPart part(String bone) {
		Integer i = Bones.INDEX.get(bone);
		return i == null ? null : parts[i];
	}

	static int parent(int bone) {
		return Bones.PARENT[bone];
	}

	static int bones() {
		return Bones.NAMES.length;
	}

	static String name(int bone) {
		return Bones.NAMES[bone];
	}

	/** Reads his clips and compiles them off the render thread (the clip file is big enough to hitch a frame). */
	public static CompletableFuture<Void> preload() {
		return preload(Util.backgroundExecutor());
	}

	/** As {@link #preload()}, on {@code executor} (a test sees which thread the work runs on). */
	static CompletableFuture<Void> preload(java.util.concurrent.Executor executor) {
		return CompletableFuture.runAsync(Compiled::touch, executor);
	}

	// ---------- posing ----------

	/** Back to rest, everything shown. */
	void reset() {
		for (ModelPart part : parts) {
			part.resetPose();
			part.visible = true;
		}
	}

	/**
	 * Poses from {@code clip} at {@code t}, faded in from {@code old} at {@code oldT} by {@code w} (0..1), exactly as
	 * {@code TradeBotModel.pose} does: a channel only one of the two clips animates fades from or to its rest value.
	 * (Trade Bot's trade props and root bone rules don't apply: Helper Bot has neither.)
	 */
	void pose(Clip clip, float t, @Nullable Clip old, float oldT, float w) {
		Bound cur = Compiled.bound(clip), prev = old == null ? null : Compiled.bound(old);
		for (int i : cur.used) {
			cur.sample(i, t, value);
			if (prev != null) {
				prev.sampleOrRest(i, oldT, from);
				from.lerp(value, w, value);
			}
			apply(parts[i / 3], i % 3, value.x, value.y, value.z);
		}
		if (prev == null)
			return;
		for (int i : prev.used) {
			if (cur.has(i))
				continue;
			rest(i, value);
			prev.sample(i, oldT, from);
			from.lerp(value, w, value);
			apply(parts[i / 3], i % 3, value.x, value.y, value.z);
		}
	}

	/**
	 * Hides the bones a clip has squashed flat on two or three axes (the watch, the card, the caps and the steam
	 * puff while they're put away): they and everything under them have no area left to draw, so this looks the same
	 * and skips their vertices. One flat axis still leaves a visible sheet, so that stays drawn.
	 */
	void hideCollapsed() {
		for (ModelPart part : parts) {
			int flat = (part.xScale == 0 ? 1 : 0) + (part.yScale == 0 ? 1 : 0) + (part.zScale == 0 ? 1 : 0);
			if (flat >= 2)
				part.visible = false;
		}
	}

	/** Adds a Blockbench-frame value onto a bone (scale multiplies), as {@code TradeBotModel.apply} does. */
	static void apply(@Nullable ModelPart part, int channel, float x, float y, float z) {
		if (part == null)
			return;
		switch (channel) {
			case ROTATION -> {
				part.xRot += -x * Mth.DEG_TO_RAD;
				part.yRot += -y * Mth.DEG_TO_RAD;
				part.zRot += z * Mth.DEG_TO_RAD;
			}
			case POSITION -> {
				part.x += -x;
				part.y += -y;
				part.z += z;
			}
			default -> {
				part.xScale *= x;
				part.yScale *= y;
				part.zScale *= z;
			}
		}
	}

	private static void rest(int binding, Vector3f out) {
		out.set(binding % 3 == SCALE ? 1 : 0);
	}

	// ---------- drawing (his renderer) ----------

	/**
	 * Draws the posed model into one buffer. His irises' glowing cubes are the only cubes with anything on his glow
	 * texture, and those texels are the same in every dye's texture (and plain), so they're drawn from his own
	 * texture at full brightness, in the same batch, instead of a second pass over the glow texture. (They used to
	 * be drawn lit as well and then covered entirely by the glow pass; now just the once.) If his textures are ever
	 * painted again, the iris texels must stay equal to {@code glow.png}.
	 */
	void render(PoseStack ms, VertexConsumer vc, int light, int overlay) {
		ModelPart left = Bones.GLOW_LEFT < 0 ? null : parts[Bones.GLOW_LEFT], right = Bones.GLOW_RIGHT < 0 ? null : parts[Bones.GLOW_RIGHT];
		boolean leftShown = left != null && left.visible, rightShown = right != null && right.visible;
		if (left != null)
			left.visible = false;
		if (right != null)
			right.visible = false;
		root.render(ms, vc, light, overlay);
		if (left != null)
			left.visible = leftShown;
		if (right != null)
			right.visible = rightShown;
		glow(ms, vc, overlay, Bones.GLOW_LEFT);
		glow(ms, vc, overlay, Bones.GLOW_RIGHT);
	}

	/** One glowing cube, placed by the same transforms the whole model's draw would have given it. */
	private void glow(PoseStack ms, VertexConsumer vc, int overlay, int bone) {
		if (bone < 0)
			return;
		for (int i = bone; i >= 0; i = Bones.PARENT[i])
			if (!parts[i].visible)
				return;
		int[] chain = Bones.chain(bone);
		ms.pushPose();
		root.translateAndRotate(ms);
		for (int i : chain)
			parts[i].translateAndRotate(ms);
		parts[bone].render(ms, vc, LightTexture.FULL_BRIGHT, overlay);
		ms.popPose();
	}

	// ---------- the skeleton's layout and the compiled clips (read once, shared by every rig) ----------

	/** His bones by index, from the geometry. */
	static final class Bones {
		static final String[] NAMES;
		static final Map<String, Integer> INDEX = new java.util.HashMap<>();
		static final int[] PARENT;
		/** The glowing iris bones (-1 if the geometry ever drops one), and each one's ancestors, top first. */
		static final int GLOW_LEFT, GLOW_RIGHT;
		private static final int[] CHAIN_LEFT, CHAIN_RIGHT;

		static {
			ModelPart root = HelperBotModels.skeletonLayer().bakeRoot();
			Map<String, ModelPart> parts = HelperBotModels.geometry().parts(root, HelperBotModels.ROOT);
			NAMES = parts.keySet().toArray(String[]::new);
			ModelPart[] byIndex = parts.values().toArray(ModelPart[]::new);
			PARENT = new int[NAMES.length];
			for (int i = 0; i < NAMES.length; i++) {
				INDEX.put(NAMES[i], i);
				PARENT[i] = -1;
				for (int j = 0; j < i; j++)
					if (byIndex[j].hasChild(NAMES[i]) && byIndex[j].getChild(NAMES[i]) == byIndex[i])
						PARENT[i] = j;
			}
			GLOW_LEFT = INDEX.getOrDefault("iris_glow_left", -1);
			GLOW_RIGHT = INDEX.getOrDefault("iris_glow_right", -1);
			CHAIN_LEFT = ancestors(GLOW_LEFT);
			CHAIN_RIGHT = ancestors(GLOW_RIGHT);
		}

		static boolean isGlow(int bone) {
			return bone >= 0 && (bone == GLOW_LEFT || bone == GLOW_RIGHT);
		}

		private static int[] chain(int bone) {
			return bone == GLOW_LEFT ? CHAIN_LEFT : CHAIN_RIGHT;
		}

		private static int[] ancestors(int bone) {
			List<Integer> up = new ArrayList<>();
			for (int i = bone < 0 ? -1 : PARENT[bone]; i >= 0; i = PARENT[i])
				up.add(0, i);
			return up.stream().mapToInt(Integer::intValue).toArray();
		}
	}

	/** A clip compiled against his bones: its tracks by bone * 3 + channel, and which of those it has. */
	static final class Bound {
		private final Track[] tracks;
		private final StepTrack[] steps;
		final int[] used;

		Bound(Clip clip) {
			int n = Bones.NAMES.length * 3;
			tracks = new Track[n];
			steps = new StepTrack[n];
			int[] found = new int[n];
			int count = 0;
			for (int bone = 0; bone < Bones.NAMES.length; bone++)
				for (int c = 0; c < 3; c++) {
					int i = bone * 3 + c;
					// a smooth track wins over a step one, as in TradeBotModel.sample
					tracks[i] = clip.track(Bones.NAMES[bone], CHANNELS[c]);
					steps[i] = tracks[i] == null ? clip.step(Bones.NAMES[bone], CHANNELS[c]) : null;
					if (tracks[i] != null || steps[i] != null)
						found[count++] = i;
				}
			used = java.util.Arrays.copyOf(found, count);
		}

		boolean has(int i) {
			return tracks[i] != null || steps[i] != null;
		}

		void sample(int i, float t, Vector3f out) {
			if (tracks[i] != null)
				tracks[i].sample(t, out);
			else
				steps[i].sample(t, out);
		}

		void sampleOrRest(int i, float t, Vector3f out) {
			if (has(i))
				sample(i, t, out);
			else
				rest(i, out);
		}
	}

	/** Every clip of his, compiled once, the first time any is needed (or on a background thread at client setup). */
	static final class Compiled {
		private static final Map<Clip, Bound> BOUND;

		static {
			ClipLibrary lib = HelperBotModels.clips();
			Map<Clip, Bound> bound = new IdentityHashMap<>();
			for (Clip clip : lib.all())
				bound.put(clip, new Bound(clip));
			BOUND = bound;
		}

		static void touch() {}

		static boolean has(Clip clip) {
			return BOUND.containsKey(clip);
		}

		static Bound bound(Clip clip) {
			Bound b = BOUND.get(clip);
			// (every clip comes from his library, compiled above; anything else is compiled as it comes)
			return b != null ? b : new Bound(clip);
		}
	}
}
