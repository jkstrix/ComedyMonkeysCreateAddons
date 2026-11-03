package com.comedymonkey.createaddons.client.jetpack;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.parts.PartSink;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * The Clockwork Jetpack's part models and rig (design/models/clockwork-jetpack.gen.js, written by its .export.js): one
 * model per moving bone, in the jetpack's own block space (its plate against the wearer at z 4, facing north, the drum
 * and key out behind). The pivots and angles below are the generator's; worn on a player, hung on its station, or
 * anywhere else, everything poses the jetpack through {@link #draw}.
 */
public final class JetpackModel {
	public enum Variant {
		CLOCKWORK("clockwork_jetpack"), NETHERITE("netherite_clockwork_jetpack");

		final ModelResourceLocation frame, drum, ducts, flange, key, pawl, harness, harnessShirt;
		final ModelResourceLocation[] coils = new ModelResourceLocation[4];
		/** [side 0 left, 1 right][part] */
		final ModelResourceLocation[] gears = new ModelResourceLocation[2], rotors = new ModelResourceLocation[2], hubs = new ModelResourceLocation[2];
		final ModelResourceLocation[][] blades = new ModelResourceLocation[2][5], elbows = new ModelResourceLocation[2][5];

		Variant(String name) {
			frame = part(name + "_frame");
			drum = part(name + "_drum");
			ducts = part(name + "_ducts");
			flange = part(name + "_flange");
			key = part(name + "_key");
			pawl = part(name + "_pawl");
			harness = part(name + "_harness");
			harnessShirt = part(name + "_harness_shirt");
			for (int k = 0; k < 4; k++)
				coils[k] = part(name + "_coil_" + k);
			for (int s = 0; s < 2; s++) {
				String side = s == 0 ? "l" : "r";
				gears[s] = part(name + "_gear_" + side);
				rotors[s] = part(name + "_rotor_" + side);
				hubs[s] = part(name + "_hub_" + side);
				for (int k = 0; k < 5; k++) {
					blades[s][k] = part(name + "_blade_" + side + (k + 1));
					elbows[s][k] = part(name + "_elbow_" + side + (k + 1));
				}
			}
		}

		void register(ModelEvent.RegisterAdditional event) {
			for (ModelResourceLocation m : new ModelResourceLocation[] { frame, drum, ducts, flange, key, pawl, harness, harnessShirt })
				event.register(m);
			for (ModelResourceLocation m : coils)
				event.register(m);
			for (int s = 0; s < 2; s++) {
				event.register(gears[s]);
				event.register(rotors[s]);
				event.register(hubs[s]);
				for (int k = 0; k < 5; k++) {
					event.register(blades[s][k]);
					event.register(elbows[s][k]);
				}
			}
		}
	}

	// the rig, from the generator (pixels, jetpack space)
	private static final float[] HUB_X = { 4.5f, 11.5f };
	private static final float HUB_Y = 15;
	private static final float[] INNER = { 10.5f, 10, 10, 10, 10 };
	/** Each rib's turn from folded (straight down) to open: the open fan's angles (+5..-31) plus the fold's 90 (D16 rev.). */
	private static final float[] DELTA = { 95, 86, 77, 68, 59 };
	private static final float FOLDED = 90, DIHEDRAL = 20, LAYER_T = 0.3f;
	/** Built into a backpack (pack mode): how far out the drum stands through its face, and how far the ducts drop under it. */
	private static final float PACK_OUT = 1, PACK_DROP = 2.5f;
	/** With the wings out, how far the drum slides in against the plate, into the gap they left. */
	private static final float TUCK = 3.5f;

	private JetpackModel() {}

	static ModelResourceLocation part(String name) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/" + name));
	}

	/** None of the parts is named by a blockstate, so they're loaded explicitly. */
	public static void registerModels(ModelEvent.RegisterAdditional event) {
		for (Variant variant : Variant.values())
			variant.register(event);
		JetpackWindingStationRenderer.registerModels(event);
	}

	/** Rib k's layer (0-based), its inner half or the outer half that folds back beside it. */
	private static float layer(int k, boolean outer) {
		return 5.1f + (2 * (4 - k) + (outer ? 0 : 1)) * LAYER_T;
	}

	/**
	 * How a jetpack stands: how wound (the porthole's coil), its key's, pawl's, fans' and gears' turns (degrees), its
	 * wings ({@code open} 0 folded .. 1 open: the fan spreads first, then the elbows straighten, then the wings lift into
	 * their V), how far the drum's tucked in against the plate once they're out (0 .. 1), its harness if it's worn, and
	 * whether it's built into a backpack worn with it (pack mode: the drum through the pack's face in its flange, the ducts
	 * under it, the folded wings inside it).
	 */
	public record Pose(float fill, float key, float pawl, float fans, float gears, float open, float tuck, Harness harness, boolean pack) {
		public static Pose still(float fill) {
			return new Pose(fill, 0, 0, 0, 0, 0, 0, Harness.NONE, false);
		}
	}

	/** Worn, the straps and belt hug what's under them: a chestplate, or the bare shirt (gen.js SHELL). */
	public enum Harness {
		NONE, CHESTPLATE, SHIRT
	}

	private static float stage(float t, float from, float length) {
		float f = Mth.clamp((t - from) / length, 0, 1);
		return f * f * (3 - 2 * f);
	}

	/** Draws a jetpack, posed, about {@code ms} at its block space's corner. */
	public static void draw(PoseStack ms, PartSink sink, int light, RenderType type, Variant v, Pose pose) {
		if (pose.harness() != Harness.NONE)
			sink.part(pose.harness() == Harness.SHIRT ? v.harnessShirt : v.harness, ms, light, type);
		if (pose.pack())
			sink.part(v.flange, ms, light, type);
		ms.pushPose();
		if (pose.pack())
			ms.translate(0, 0, PACK_OUT / 16);
		sink.part(v.frame, ms, light, type);
		// the drum and all it carries
		ms.pushPose();
		ms.translate(0, 0, -TUCK * pose.tuck() / 16);
		sink.part(v.drum, ms, light, type);
		ms.pushPose();
		if (pose.pack())
			ms.translate(0, -PACK_DROP / 16, 0);
		sink.part(v.ducts, ms, light, type);
		for (int s = 0; s < 2; s++)
			turn(ms, sink, v.rotors[s], s == 0 ? 5.5f : 10.5f, 4.45f, 10, Axis.YP, (s == 0 ? 1 : -1) * pose.fans(), light, type);
		ms.popPose();
		sink.part(v.coils[Mth.clamp((int) (pose.fill() * 4), 0, 3)], ms, light, type);
		turn(ms, sink, v.key, 8, 12, 12, Axis.ZP, pose.key(), light, type);
		turn(ms, sink, v.pawl, 11.6f, 14, 11.6f, Axis.ZP, pose.pawl(), light, type);
		for (int s = 0; s < 2; s++)
			turn(ms, sink, v.gears[s], s == 0 ? 5.5f : 10.5f, 8.25f, 12.15f, Axis.ZP, (s == 0 ? 1 : -1) * pose.gears(), light, type);
		ms.popPose();
		for (int s = 0; s < 2; s++)
			wing(ms, sink, v, s, pose.open(), light, type);
		ms.popPose();
	}

	/** One wing: its precision-mechanism hub, then each rib on its hub and its outer half on its elbow. */
	private static void wing(PoseStack ms, PartSink sink, Variant v, int s, float open, int light, RenderType type) {
		float dir = s == 0 ? -1 : 1, hx = HUB_X[s];
		float fan = stage(open, 0, 0.5f), elbow = stage(open, 0.3f, 0.4f), lift = stage(open, 0.55f, 0.45f);
		turn(ms, sink, v.hubs[s], hx, HUB_Y, 8.3f, Axis.ZP, dir * 90 * fan, light, type);
		ms.pushPose();
		pivot(ms, hx, HUB_Y, 6.6f, Axis.YP, -dir * DIHEDRAL * lift);
		for (int k = 0; k < 5; k++) {
			ms.pushPose();
			pivot(ms, hx, HUB_Y, layer(k, true) + LAYER_T, Axis.ZP, dir * (-FOLDED + DELTA[k] * fan));
			sink.part(v.blades[s][k], ms, light, type);
			float h = k == 0 ? 0.75f : 0.5f;
			turn(ms, sink, v.elbows[s][k], hx + dir * INNER[k], HUB_Y + h / 2, layer(k, true) + LAYER_T, Axis.ZP,
				dir * (-180 + 180 * elbow), light, type);
			ms.popPose();
		}
		ms.popPose();
	}

	private static void pivot(PoseStack ms, float px, float py, float pz, Axis axis, float degrees) {
		ms.translate(px / 16, py / 16, pz / 16);
		ms.mulPose(axis.rotationDegrees(degrees));
		ms.translate(-px / 16, -py / 16, -pz / 16);
	}

	private static void turn(PoseStack ms, PartSink sink, ModelResourceLocation part, float px, float py, float pz, Axis axis, float degrees,
							 int light, RenderType type) {
		ms.pushPose();
		pivot(ms, px, py, pz, axis, degrees);
		sink.part(part, ms, light, type);
		ms.popPose();
	}
}
