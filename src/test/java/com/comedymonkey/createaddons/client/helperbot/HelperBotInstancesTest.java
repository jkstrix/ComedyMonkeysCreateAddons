package com.comedymonkey.createaddons.client.helperbot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.joml.Matrix4f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.model.part.ModelTree;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;

/**
 * His Flywheel instances (HelperBotInstances, used by the placed and the riding visual) against his own model:
 * posed from the same clips they end up where the model's drawing puts each bone and show exactly the bones it draws
 * (a hidden bone hiding everything below it), with one shared material per dye, his irises at full brightness, and
 * placed in his block exactly as his renderer places him.
 */
class HelperBotInstancesTest {
	private static final float EPSILON = 1e-4f;
	private Function<Material, ModelTree> realTrees;
	private final List<Material> asked = new ArrayList<>();

	@BeforeEach
	void fakeTrees() {
		realTrees = HelperBotInstances.trees;
		HelperBotInstances.trees = material -> {
			asked.add(material);
			return HelperBotFakes.tree();
		};
	}

	@AfterEach
	void realTrees() {
		HelperBotInstances.trees = realTrees;
	}

	@Test
	void instancesFollowTheModelBoneForBone() {
		HelperBotFakes.Provider provider = new HelperBotFakes.Provider();
		HelperBotInstances instances = new HelperBotInstances(provider, DyeColor.CYAN, false);
		HelperBotRig rig = HelperBotRig.skeleton();
		Matrix4f place = HelperBotInstances.place(new Matrix4f(), 3, -2, 7, Direction.EAST);
		List<Clip> clips = HelperBotModels.clips().all();
		int hiddenUnderAHiddenParent = 0, checked = 0;
		for (int c = 0; c < clips.size(); c++) {
			Clip clip = clips.get(c), old = clips.get((c + 5) % clips.size());
			for (float t : new float[] { 0, 0.2f, 0.9f, 1.7f, 3.1f })
				for (boolean fading : new boolean[] { false, true }) {
					rig.reset();
					rig.pose(clip, clip.localTime(t), fading ? old : null, fading ? old.localTime(t) : 0, fading ? 0.4f : 1);
					rig.hideCollapsed();
					instances.copy(rig);
					instances.update(place, false);
					for (int i = 0; i < HelperBotRig.bones(); i++) {
						TransformedInstance instance = provider.byBone.get(HelperBotRig.name(i));
						if (instance == null)
							continue;
						boolean drawn = HelperBotFakes.drawn(rig, i);
						String where = clip.name() + (fading ? " from " + old.name() : "") + " at " + t + ": " + HelperBotRig.name(i);
						assertEquals(drawn, provider.handle(instance).visible, where + " shown");
						if (drawn)
							assertClose(HelperBotFakes.boneMatrix(rig, i, place), instance.pose, where);
						else if (rig.parts[i].visible)
							hiddenUnderAHiddenParent++;
						checked++;
					}
				}
		}
		assertTrue(checked > 5000);
		// the case Flywheel's own visibility gets wrong (a parent hidden, the child still flagged visible) came up
		assertTrue(hiddenUnderAHiddenParent > 0, "no clip hid a parent with its child still flagged visible");
	}

	/** A frame where nothing moved costs nothing: no instance is sent again. */
	@Test
	void anUnchangedPoseUploadsNothing() {
		HelperBotFakes.Provider provider = new HelperBotFakes.Provider();
		HelperBotInstances instances = new HelperBotInstances(provider, null, false);
		HelperBotRig rig = HelperBotRig.skeleton();
		Matrix4f place = HelperBotInstances.place(new Matrix4f(), 0, 0, 0, Direction.NORTH);
		Clip idle = HelperBotModels.clips().clip("idle");
		rig.reset();
		rig.pose(idle, 0.5f, null, 0, 1);
		rig.hideCollapsed();
		instances.copy(rig);
		instances.light(LightTexture.pack(4, 9));
		instances.update(place, true);
		int sent = provider.changes();
		instances.copy(rig);
		instances.light(LightTexture.pack(4, 9));
		instances.update(place, false);
		assertEquals(sent, provider.changes());
		// a new frame of the clip does send them
		rig.reset();
		rig.pose(idle, 0.6f, null, 0, 1);
		instances.copy(rig);
		instances.update(place, false);
		assertTrue(provider.changes() > sent);
	}

	/**
	 * One material per dye (and plain), placed and embedded, shared by every bot, made as his renderer's entity cutout
	 * render type draws. None shades him with ambient occlusion; embedded (trains, ships) he takes the world's light
	 * flat, block by block, rather than Flywheel's smoothed and occluded default; placed, the default stays (it adds
	 * nothing outside an embedding, so he's lit by his own block, as his renderer lights him).
	 */
	@Test
	void oneSharedMaterialPerDye() {
		Set<Material> seen = new HashSet<>();
		List<DyeColor> dyes = new ArrayList<>(List.of(DyeColor.values()));
		dyes.add(null);
		for (boolean embedded : new boolean[] { false, true })
			for (DyeColor dye : dyes) {
				String where = dye + (embedded ? " embedded" : " placed");
				Material material = HelperBotInstances.material(dye, embedded);
				assertSame(material, HelperBotInstances.material(dye, embedded), "cached: " + where);
				assertTrue(seen.add(material), "shared with another dye: " + where);
				assertEquals(HelperBotModels.texture(dye), material.texture());
				assertSame(CutoutShaders.ONE_TENTH, material.cutout());
				assertFalse(material.backfaceCulling());
				assertFalse(material.mipmap());
				assertEquals(CardinalLightingMode.ENTITY, material.cardinalLightingMode());
				assertFalse(material.ambientOcclusion(), "occlusion would darken him: " + where);
				assertSame(embedded ? LightShaders.FLAT : LightShaders.SMOOTH_WHEN_EMBEDDED, material.light(), where);

				// and it's the one his tree is made in, for every bot of that colour
				asked.clear();
				new HelperBotInstances(new HelperBotFakes.Provider(), dye, embedded);
				new HelperBotInstances(new HelperBotFakes.Provider(), dye, embedded);
				assertEquals(List.of(material, material), asked);
			}
		assertEquals(34, seen.size());
	}

	/**
	 * Only what moved is sent again: between two frames of a clip, exactly the instances whose bone (or a bone above
	 * it) changed, and in idle that's far from all of him.
	 */
	@Test
	void onlyTheBonesThatMovedAreSentAgain() {
		HelperBotFakes.Provider provider = new HelperBotFakes.Provider();
		HelperBotInstances instances = new HelperBotInstances(provider, DyeColor.GREEN, false);
		HelperBotRig rig = HelperBotRig.skeleton(), before = HelperBotRig.skeleton();
		Matrix4f place = HelperBotInstances.place(new Matrix4f(), 1, 2, 3, Direction.SOUTH);
		int frames = 0, fewer = 0;
		for (String name : List.of("idle", "shop_idle", "station_wait", "cruise", "check_watch")) {
			Clip clip = HelperBotModels.clips().clip(name);
			float t0 = 0.4f;
			pose(before, clip, t0);
			pose(rig, clip, t0);
			instances.copy(rig);
			instances.update(place, true);
			for (float t = t0 + 0.05f; t < t0 + 2; t += 0.05f) {
				pose(rig, clip, t);
				Map<TransformedInstance, Integer> was = new java.util.IdentityHashMap<>();
				for (TransformedInstance instance : provider.byBone.values())
					was.put(instance, provider.handle(instance).changes);
				instances.copy(rig);
				instances.update(place, false);
				int sent = 0, instanced = 0;
				for (int i = 0; i < HelperBotRig.bones(); i++) {
					TransformedInstance instance = provider.byBone.get(HelperBotRig.name(i));
					if (instance == null)
						continue;
					instanced++;
					boolean expected = HelperBotFakes.drawn(rig, i) && (movedOrAbove(before, rig, i) || !HelperBotFakes.drawn(before, i));
					boolean actual = provider.handle(instance).changes > was.get(instance);
					assertEquals(expected, actual, name + " at " + t + ": " + HelperBotRig.name(i) + " sent");
					if (actual)
						sent++;
					// and whatever was sent sits where the model puts it
					if (HelperBotFakes.drawn(rig, i))
						assertClose(HelperBotFakes.boneMatrix(rig, i, place), instance.pose, name + " at " + t + ": " + HelperBotRig.name(i));
				}
				frames++;
				if (sent < instanced)
					fewer++;
				pose(before, clip, t);
			}
		}
		assertTrue(fewer > frames / 2, "most frames should send only part of him: " + fewer + " of " + frames);
	}

	/**
	 * A bone hidden while what it hangs from moves (his watch, put away while his arm swings) is in the right place
	 * when it shows again, even though its own pose never changed.
	 */
	@Test
	void aBoneShownAgainFollowsWhatMovedWhileItWasHidden() {
		HelperBotFakes.Provider provider = new HelperBotFakes.Provider();
		HelperBotInstances instances = new HelperBotInstances(provider, null, false);
		HelperBotRig rig = HelperBotRig.skeleton();
		Matrix4f place = HelperBotInstances.place(new Matrix4f(), 0, 0, 0, Direction.WEST);
		int held = index("watch_held"), lid = index("watch_lid"), arm = index("arm_left");
		rig.reset();
		instances.copy(rig);
		instances.update(place, true);
		// put away
		rig.parts[held].xScale = rig.parts[held].yScale = rig.parts[held].zScale = 0;
		rig.hideCollapsed();
		instances.copy(rig);
		instances.update(place, false);
		assertFalse(provider.handle(provider.byBone.get("watch_held")).visible);
		// the arm swings while it's away
		rig.reset();
		rig.parts[held].xScale = rig.parts[held].yScale = rig.parts[held].zScale = 0;
		rig.parts[arm].xRot += 1.2f;
		rig.hideCollapsed();
		instances.copy(rig);
		instances.update(place, false);
		// out again, its own pose as it always was
		rig.reset();
		rig.parts[arm].xRot += 1.2f;
		rig.hideCollapsed();
		instances.copy(rig);
		instances.update(place, false);
		for (int bone : new int[] { held, lid }) {
			TransformedInstance instance = provider.byBone.get(HelperBotRig.name(bone));
			assertTrue(provider.handle(instance).visible, HelperBotRig.name(bone));
			assertClose(HelperBotFakes.boneMatrix(rig, bone, place), instance.pose, HelperBotRig.name(bone));
		}
	}

	private static void pose(HelperBotRig rig, Clip clip, float t) {
		rig.reset();
		rig.pose(clip, clip.localTime(t), null, 0, 1);
		rig.hideCollapsed();
	}

	/** Whether the bone, or any bone above it, has a different pose in the two rigs. */
	private static boolean movedOrAbove(HelperBotRig a, HelperBotRig b, int bone) {
		for (int i = bone; i >= 0; i = HelperBotRig.parent(i)) {
			ModelPart x = a.parts[i], y = b.parts[i];
			if (x.x != y.x || x.y != y.y || x.z != y.z || x.xRot != y.xRot || x.yRot != y.yRot || x.zRot != y.zRot || x.xScale != y.xScale
				|| x.yScale != y.yScale || x.zScale != y.zScale)
				return true;
		}
		return false;
	}

	private static int index(String bone) {
		for (int i = 0; i < HelperBotRig.bones(); i++)
			if (HelperBotRig.name(i).equals(bone))
				return i;
		throw new IllegalArgumentException(bone);
	}

	/** His irises glow (full brightness) whatever light he stands in; everything else takes that light. */
	@Test
	void irisesAreFullBrightAndTheRestTakeTheLight() {
		HelperBotFakes.Provider provider = new HelperBotFakes.Provider();
		HelperBotInstances instances = new HelperBotInstances(provider, DyeColor.ORANGE, true);
		for (int light : new int[] { LightTexture.pack(0, 0), LightTexture.pack(12, 3), LightTexture.FULL_BRIGHT }) {
			instances.light(light);
			for (Map.Entry<String, TransformedInstance> e : provider.byBone.entrySet())
				assertEquals(e.getKey().startsWith("iris_glow_") ? LightTexture.FULL_BRIGHT : light, e.getValue().light, e.getKey());
		}
		assertNotNull(provider.byBone.get("iris_glow_left"));
		assertNotNull(provider.byBone.get("iris_glow_right"));
	}

	/** A change of colour makes him again in the new material, and lets the old instances go. */
	@Test
	void aNewDyeRemakesHimInItsMaterial() {
		HelperBotFakes.Provider provider = new HelperBotFakes.Provider();
		HelperBotInstances instances = new HelperBotInstances(provider, null, false);
		List<TransformedInstance> before = new ArrayList<>(provider.all);
		asked.clear();
		instances.dye(null);
		assertTrue(asked.isEmpty(), "same colour: nothing remade");
		instances.dye(DyeColor.LIME);
		assertEquals(List.of(HelperBotInstances.material(DyeColor.LIME, false)), asked);
		for (TransformedInstance old : before)
			assertTrue(provider.handle(old).deleted);
		TransformedInstance iris = provider.byBone.get("iris_glow_left");
		assertNotSame(before.get(0), provider.all.get(provider.all.size() - 1));
		assertEquals(LightTexture.FULL_BRIGHT, iris.light);
		instances.delete();
		for (TransformedInstance instance : provider.all)
			assertTrue(provider.handle(instance).deleted);
	}

	/** The visuals stand him where his renderer's draw does, for every facing (2 px forward, turned to face it). */
	@Test
	void placedWhereHisRendererDrawsHim() {
		HelperBotRig rig = new HelperBotRig(HelperBotModels.layer().bakeRoot());
		rig.reset();
		HelperBotModels.Baked model = new HelperBotModels.Baked(rig.root, rig);
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			HelperBotFakes.Recorder drawn = new HelperBotFakes.Recorder();
			HelperBotModels.draw(new PoseStack(), type -> drawn, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, facing, null, model);
			PoseStack ms = new PoseStack();
			ms.last().pose().set(HelperBotInstances.place(new Matrix4f(), 0, 0, 0, facing));
			HelperBotFakes.Recorder placed = new HelperBotFakes.Recorder();
			rig.render(ms, placed, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
			assertEquals(drawn.vertices.size(), placed.vertices.size());
			for (int i = 0; i < drawn.vertices.size(); i++) {
				HelperBotFakes.Vertex a = drawn.vertices.get(i), b = placed.vertices.get(i);
				assertEquals(a.x(), b.x(), EPSILON, facing + " vertex " + i);
				assertEquals(a.y(), b.y(), EPSILON, facing + " vertex " + i);
				assertEquals(a.z(), b.z(), EPSILON, facing + " vertex " + i);
			}
		}
	}

	private static void assertClose(Matrix4f expected, Matrix4f actual, String where) {
		for (int c = 0; c < 4; c++)
			for (int r = 0; r < 4; r++)
				assertEquals(expected.get(c, r), actual.get(c, r), EPSILON, where + " [" + c + "," + r + "]\n" + expected + "\n" + actual);
	}
}
