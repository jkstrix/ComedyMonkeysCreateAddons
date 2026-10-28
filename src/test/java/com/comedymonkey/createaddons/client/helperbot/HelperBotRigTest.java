package com.comedymonkey.createaddons.client.helperbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.client.tradebot.TradeBotModel;
import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Helper Bot's compiled clips pose him exactly as Trade Bot's general posing does, bone for bone and bit for bit,
 * with and without a cross-fade; hiding squashed bones only hides bones with nothing left to draw (and what's under
 * them); and his one-pass drawing draws every cube once, the irises at full brightness exactly where the old glow
 * pass put them.
 */
class HelperBotRigTest {
	private static final float[] TIMES = { 0, 0.05f, 0.37f, 1, 1.618f, 2.5f, 7.25f, 30 };

	@Test
	void compiledClipsPoseExactlyAsBefore() {
		HelperBotRig rig = HelperBotRig.skeleton();
		ModelPart reference = HelperBotModels.skeletonLayer().bakeRoot();
		Map<String, ModelPart> parts = HelperBotModels.geometry().parts(reference, HelperBotModels.ROOT);
		List<Clip> clips = HelperBotModels.clips().all();
		int checked = 0;
		for (int c = 0; c < clips.size(); c++) {
			Clip clip = clips.get(c);
			// alone, and faded in from the next clip (every pair of channels: both, only one, neither)
			for (Clip old : new Clip[] { null, clips.get((c + 1) % clips.size()), clips.get((c + 7) % clips.size()) })
				for (float t : TIMES)
					for (float w : new float[] { 0, 0.3f, 1 }) {
						rig.reset();
						rig.pose(clip, clip.localTime(t), old, old == null ? 0 : old.localTime(t * 0.7f), old == null ? 1 : w);
						for (ModelPart part : parts.values()) {
							part.resetPose();
							part.visible = true;
						}
						TradeBotModel.pose(parts, clip, clip.localTime(t), old, old == null ? 0 : old.localTime(t * 0.7f), old == null ? 1 : w);
						for (int i = 0; i < HelperBotRig.bones(); i++)
							same(parts.get(HelperBotRig.name(i)), rig.parts[i], clip.name() + " from " + (old == null ? "-" : old.name())
								+ " at " + t + " w " + w + ": " + HelperBotRig.name(i));
						checked++;
					}
		}
		assertTrue(checked > 1000);
	}

	@Test
	void hidesOnlyBonesWithNoAreaLeft() {
		HelperBotRig rig = HelperBotRig.skeleton();
		for (Clip clip : HelperBotModels.clips().all())
			for (float t : TIMES) {
				rig.reset();
				rig.pose(clip, clip.localTime(t), null, 0, 1);
				rig.hideCollapsed();
				for (ModelPart part : rig.parts) {
					int flat = (part.xScale == 0 ? 1 : 0) + (part.yScale == 0 ? 1 : 0) + (part.zScale == 0 ? 1 : 0);
					assertEquals(flat < 2, part.visible, clip.name() + " at " + t);
				}
			}
	}

	/** Two or three flat axes hide a bone; one flat axis (a sheet that still shows) or none leaves it. */
	@Test
	void hidingFollowsHowManyAxesAreFlat() {
		HelperBotRig rig = HelperBotRig.skeleton();
		float[][] scales = { { 0, 0, 0 }, { 0, 0, 1 }, { 1, 0, 0 }, { 0, 1, 0 }, { 0, 1, 1 }, { 1, 0, 1 }, { 1, 1, 0 }, { 1, 1, 1 }, { 0.02f, 0.02f, 0.02f } };
		for (float[] s : scales) {
			rig.reset();
			ModelPart part = rig.parts[index("watch_held")];
			part.xScale = s[0];
			part.yScale = s[1];
			part.zScale = s[2];
			rig.hideCollapsed();
			int flat = (s[0] == 0 ? 1 : 0) + (s[1] == 0 ? 1 : 0) + (s[2] == 0 ? 1 : 0);
			assertEquals(flat < 2, part.visible, java.util.Arrays.toString(s));
			// nothing else is touched
			for (int i = 0; i < HelperBotRig.bones(); i++)
				if (rig.parts[i] != part)
					assertTrue(rig.parts[i].visible, HelperBotRig.name(i));
		}
	}

	/** A hidden bone takes everything under it out of the drawing (his watch's lid goes with the watch). */
	@Test
	void hiddenBonesHideTheirChildrenWhenDrawn() {
		HelperBotRig rig = new HelperBotRig(HelperBotModels.layer().bakeRoot());
		rig.reset();
		int all = draw(rig).vertices.size();
		int held = index("watch_held"), lid = index("watch_lid");
		assertEquals(held, HelperBotRig.parent(lid));
		rig.parts[held].xScale = rig.parts[held].yScale = rig.parts[held].zScale = 0;
		rig.hideCollapsed();
		assertFalse(rig.parts[held].visible);
		assertTrue(rig.parts[lid].visible, "the lid keeps its own flag");
		// the watch's cube and the lid's cube: 24 vertices each
		assertEquals(all - 48, draw(rig).vertices.size());

		// one flat axis: still drawn, every vertex
		rig.reset();
		rig.parts[index("spring")].yScale = 0;
		rig.hideCollapsed();
		assertEquals(all, draw(rig).vertices.size());
	}

	/**
	 * One pass, one buffer: every cube is drawn once (the irises aren't drawn lit and then again), the irises'
	 * vertices are the only full-bright ones, and they sit exactly where the whole model's own drawing puts them.
	 */
	@Test
	void irisesAreDrawnOnceAtFullBrightWhereTheModelPutsThem() {
		HelperBotRig rig = new HelperBotRig(HelperBotModels.layer().bakeRoot());
		for (Clip clip : List.of(HelperBotModels.clips().clip("idle"), HelperBotModels.clips().clip("sees_player"),
			HelperBotModels.clips().clip("duck_down")))
			for (float t : new float[] { 0, 0.4f, 1.3f }) {
				rig.reset();
				rig.pose(clip, clip.localTime(t), null, 0, 1);
				rig.hideCollapsed();
				int light = LightTexture.pack(7, 3);
				HelperBotFakes.Recorder now = draw(rig, light);

				// the old way: the whole model at its light, which drew the iris cubes as well
				HelperBotFakes.Recorder whole = new HelperBotFakes.Recorder();
				rig.root.render(new PoseStack(), whole, light, OverlayTexture.NO_OVERLAY);
				assertEquals(whole.vertices.size(), now.vertices.size(), clip.name() + " " + t);
				assertEquals(48, now.lit(LightTexture.FULL_BRIGHT), clip.name() + " " + t);
				assertEquals(now.vertices.size() - 48, now.lit(light));

				// the old glow pass's irises: the model's own recursion with every other bone's cubes skipped
				for (ModelPart part : rig.parts)
					part.skipDraw = !HelperBotRig.Bones.isGlow(java.util.Arrays.asList(rig.parts).indexOf(part));
				HelperBotFakes.Recorder irises = new HelperBotFakes.Recorder();
				rig.root.render(new PoseStack(), irises, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
				for (ModelPart part : rig.parts)
					part.skipDraw = false;
				// (left and right may come in either order)
				java.util.Comparator<HelperBotFakes.Vertex> order = java.util.Comparator.comparingDouble(HelperBotFakes.Vertex::x)
					.thenComparingDouble(HelperBotFakes.Vertex::y).thenComparingDouble(HelperBotFakes.Vertex::z);
				assertEquals(irises.vertices.stream().sorted(order).toList(),
					now.vertices.stream().filter(v -> v.light() == LightTexture.FULL_BRIGHT).sorted(order).toList(), clip.name() + " " + t);
			}
	}

	/** Every one of his clips is compiled once, with exactly the bone channels it animates. */
	@Test
	void everyClipIsCompiledWithItsChannels() {
		Set<String> bones = new HashSet<>();
		for (int i = 0; i < HelperBotRig.bones(); i++)
			bones.add(HelperBotRig.name(i));
		for (Clip clip : HelperBotModels.clips().all()) {
			assertTrue(HelperBotRig.Compiled.has(clip), clip.name());
			HelperBotRig.Bound bound = HelperBotRig.Compiled.bound(clip);
			assertSame(bound, HelperBotRig.Compiled.bound(clip), "compiled once: " + clip.name());
			Set<Integer> expected = new HashSet<>();
			String[] channels = { "rotation", "position", "scale" };
			for (int b = 0; b < HelperBotRig.bones(); b++)
				for (int c = 0; c < 3; c++)
					if (clip.track(HelperBotRig.name(b), channels[c]) != null || clip.step(HelperBotRig.name(b), channels[c]) != null)
						expected.add(b * 3 + c);
			Set<Integer> used = new HashSet<>();
			for (int i : bound.used)
				used.add(i);
			assertEquals(expected, used, clip.name());
			// and no clip animates a bone he hasn't got
			assertTrue(bones.containsAll(clip.tracks().keySet()), clip.name());
			assertTrue(bones.containsAll(clip.steps().keySet()), clip.name());
		}
	}

	/** Client setup's preload finishes without error, off the calling thread, and leaves the clips compiled. */
	@Test
	void preloadLeavesEverythingReady() throws Exception {
		Thread caller = Thread.currentThread();
		Thread[] ran = new Thread[1];
		// the thread the work itself runs on (a stage chained after it runs on the caller when the work is already done,
		// as it is when another test compiled the clips first)
		java.util.concurrent.Executor background = net.minecraft.Util.backgroundExecutor();
		HelperBotRig.preload(work -> background.execute(() -> {
			ran[0] = Thread.currentThread();
			work.run();
		})).get(30, TimeUnit.SECONDS);
		assertTrue(ran[0] != caller, "preloaded on the calling thread");
		assertEquals(38, HelperBotRig.bones());
		for (Clip clip : HelperBotModels.clips().all())
			assertTrue(HelperBotRig.Compiled.has(clip), clip.name());
	}

	private static int index(String bone) {
		for (int i = 0; i < HelperBotRig.bones(); i++)
			if (HelperBotRig.name(i).equals(bone))
				return i;
		throw new IllegalArgumentException(bone);
	}

	private static HelperBotFakes.Recorder draw(HelperBotRig rig) {
		return draw(rig, LightTexture.pack(15, 15));
	}

	private static HelperBotFakes.Recorder draw(HelperBotRig rig, int light) {
		HelperBotFakes.Recorder out = new HelperBotFakes.Recorder();
		rig.render(new PoseStack(), out, light, OverlayTexture.NO_OVERLAY);
		return out;
	}

	private static void same(ModelPart expected, ModelPart actual, String where) {
		float[] e = { expected.x, expected.y, expected.z, expected.xRot, expected.yRot, expected.zRot, expected.xScale, expected.yScale, expected.zScale };
		float[] a = { actual.x, actual.y, actual.z, actual.xRot, actual.yRot, actual.zRot, actual.xScale, actual.yScale, actual.zScale };
		for (int i = 0; i < e.length; i++)
			assertEquals(Float.floatToIntBits(e[i]), Float.floatToIntBits(a[i]), where + " [" + i + "] " + e[i] + " vs " + a[i]);
		assertEquals(expected.visible, actual.visible, where + " visible");
	}
}
