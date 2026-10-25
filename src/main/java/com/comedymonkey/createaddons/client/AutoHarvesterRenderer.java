package com.comedymonkey.createaddons.client;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.parts.PartSink;
import com.comedymonkey.createaddons.content.harvester.AutoHarvesterBlock;
import com.comedymonkey.createaddons.content.harvester.AutoHarvesterBlockEntity;
import com.comedymonkey.createaddons.content.harvester.PlantKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * Draws the moving inside of an Auto Harvester behind its glass: the plant growing in the planter, and
 * the head on the gantry rail. Parked at one end while the plant grows; each harvest it plunges, sweeps across the
 * planter with its reel (crops) or saw (trees) spinning, lifts and returns. Also turns the shaft end in the back.
 * All of it is modelled with the front facing north; the pixel numbers below are in that model space.
 */
public class AutoHarvesterRenderer extends SafeBlockEntityRenderer<AutoHarvesterBlockEntity> {
	public static final ModelResourceLocation CARRIAGE = model("auto_harvester_carriage");
	public static final ModelResourceLocation POLE = model("auto_harvester_pole");
	public static final ModelResourceLocation REEL = model("auto_harvester_reel");
	public static final ModelResourceLocation SAW = model("auto_harvester_saw");
	public static final ModelResourceLocation CROP = model("auto_harvester_crop");
	public static final ModelResourceLocation TREE = model("auto_harvester_tree");

	/**
	 * Hub of the tool when parked (its carriage at the rail's west end), and the far end of the sweep. The tools are
	 * 5 px across, so these keep them off the side walls and the girder.
	 */
	private static final float PARK_X = 4.5f, PARK_Y = 11, SWEEP_END_X = 11.5f;
	/** Hub height while cutting: the tool just clears the planter's rim. */
	private static final float CUT_Y = 6;
	/** Underside of the carriage, where the pole comes out. */
	private static final float CARRIAGE_BOTTOM = 9;
	/** Soil level, which the plant grows from. */
	private static final float SOIL_Y = 3;
	/** Floor of the tray in front of the planter, where the harvest waits. */
	private static final float TRAY_Y = 2.5f;
	private static final float REEL_DEGREES_PER_TICK = 18, SAW_DEGREES_PER_TICK = 45;

	public AutoHarvesterRenderer(BlockEntityRendererProvider.Context context) {}

	private static ModelResourceLocation model(String name) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/" + name));
	}

	public static void registerModels(ModelEvent.RegisterAdditional event) {
		for (ModelResourceLocation m : new ModelResourceLocation[] {CARRIAGE, POLE, REEL, SAW, CROP, TREE})
			event.register(m);
	}

	@Override
	protected void renderSafe(AutoHarvesterBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
							  int light, int overlay) {
		BlockState state = be.getBlockState();
		if (!state.hasProperty(AutoHarvesterBlock.FACING))
			return;
		Direction front = state.getValue(AutoHarvesterBlock.FACING);
		boolean behind = CameraView.of(be, ms).behindGlass(front);
		parts(be, partialTicks, ms, PartSink.forRenderer(be, buffer, overlay), light, !behind);
		// Everything else is inside, seen only through the front glass
		if (behind)
			return;

		PlantKind kind = state.getValue(AutoHarvesterBlock.PLANT);
		ms.pushPose();
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-(front.toYRot() + 180)));
		ms.translate(-0.5, -0.5, -0.5);
		if (kind != PlantKind.NONE) {
			float growth = be.getGrowth(partialTicks);
			if (!RealPlants.render(be.getPlant(), kind, growth, ms, buffer, light, overlay))
				renderPlant(ms, PartSink.screen(buffer, state, overlay), kind, growth, light);
		}
		TrayItems.render(be.getOutput(), TRAY_Y, be.getLevel(), ms, buffer, light, overlay);
		ms.popPose();
	}

	/** The part models: the shaft end and the head on its rail (for this renderer, or Flywheel's visual). */
	public static void parts(AutoHarvesterBlockEntity be, float partialTicks, PoseStack ms, PartSink sink, int light) {
		parts(be, partialTicks, ms, sink, light, true);
	}

	/** The head is inside, left out when it can't be seen ({@code inside} false). */
	private static void parts(AutoHarvesterBlockEntity be, float partialTicks, PoseStack ms, PartSink sink, int light,
							  boolean inside) {
		BlockState state = be.getBlockState();
		if (!state.hasProperty(AutoHarvesterBlock.FACING))
			return;
		Direction front = state.getValue(AutoHarvesterBlock.FACING);
		CobbleGeneratorRenderer.shaft(be, front.getOpposite(), ms, sink, light);
		if (!inside)
			return;
		ms.pushPose();
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-(front.toYRot() + 180)));
		ms.translate(-0.5, -0.5, -0.5);
		renderHead(ms, sink, state.getValue(AutoHarvesterBlock.PLANT), be.getHarvestProgress(partialTicks), light);
		ms.popPose();
	}

	/** The generic plant, for anything {@link RealPlants} can't draw: crops shoot up, trees grow all over. */
	private static void renderPlant(PoseStack ms, PartSink sink, PlantKind kind, float growth, int light) {
		ms.pushPose();
		ms.translate(0.5, SOIL_Y / 16, 0.5);
		if (kind == PlantKind.TREE) {
			float s = 0.3f + 0.7f * growth;
			ms.scale(s, s, s);
		} else {
			float wide = 0.7f + 0.3f * growth;
			ms.scale(wide, 0.15f + 0.85f * growth, wide);
		}
		ms.translate(-0.5, -SOIL_Y / 16, -0.5);
		sink.part(kind == PlantKind.TREE ? TREE : CROP, ms, light);
		ms.popPose();
	}

	/** Carriage on the rail, the pole down to the tool, and the tool itself, at {@code t} (0-1) into a harvest. */
	private static void renderHead(PoseStack ms, PartSink sink, PlantKind kind, float t, int light) {
		float x = PARK_X, y = PARK_Y, spin = 0;
		if (t >= 0) {
			// Plunge, sweep across the planter, lift, return; the cut comes at the end of the sweep
			if (t < 0.15f)
				y = Mth.lerp(ease(t / 0.15f), PARK_Y, CUT_Y);
			else if (t < 0.65f) {
				x = Mth.lerp((t - 0.15f) / 0.5f, PARK_X, SWEEP_END_X);
				y = CUT_Y;
			} else if (t < 0.8f) {
				x = SWEEP_END_X;
				y = Mth.lerp(ease((t - 0.65f) / 0.15f), CUT_Y, PARK_Y);
			} else
				x = Mth.lerp(ease((t - 0.8f) / 0.2f), SWEEP_END_X, PARK_X);
			float ticks = t * AutoHarvesterBlockEntity.HARVEST_TICKS;
			spin = ticks * (kind == PlantKind.TREE ? SAW_DEGREES_PER_TICK : REEL_DEGREES_PER_TICK);
		}

		ms.pushPose();
		ms.translate((x - PARK_X) / 16, 0, 0);
		sink.part(CARRIAGE, ms, light);
		if (y < CARRIAGE_BOTTOM) {
			ms.pushPose();
			ms.translate(0, y / 16, 0);
			ms.scale(1, CARRIAGE_BOTTOM - y, 1);
			sink.part(POLE, ms, light);
			ms.popPose();
		}
		ms.popPose();

		// The tools are modelled around the block's centre, with the hub at (8, 8)
		ms.pushPose();
		ms.translate((x - 8) / 16, (y - 8) / 16, 0);
		ms.translate(0.5, 0.5, 0);
		ms.mulPose(Axis.ZP.rotationDegrees(-spin));
		ms.translate(-0.5, -0.5, 0);
		sink.part(kind == PlantKind.TREE ? SAW : REEL, ms, light);
		ms.popPose();
	}

	private static float ease(float f) {
		return f * f * (3 - 2 * f);
	}

}
