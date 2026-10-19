package com.comedymonkey.createaddons.client;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.engine.MagmaEngineBlock;
import com.comedymonkey.createaddons.content.engine.MagmaEngineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Turns the Magma Engine's output shaft and bobs its three cylinder heads, one after another, once per turn of the
 * shaft, so they speed up and stop with it.
 */
public class MagmaEngineRenderer extends SafeBlockEntityRenderer<MagmaEngineBlockEntity> {
	/** One cylinder head, modelled on the left-hand cylinder with the front facing north. */
	public static final ModelResourceLocation HEAD =
		ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/magma_engine_head"));
	private static final int CYLINDERS = 3;
	/** How far a head lifts, in pixels. */
	private static final float STROKE = 1f;
	/** Each cylinder is 4.5 pixels along from the last. */
	private static final float CYLINDER_SPACING = 4.5f / 16f;

	public MagmaEngineRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	protected void renderSafe(MagmaEngineBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
							  int light, int overlay) {
		BlockState state = be.getBlockState();
		if (!state.hasProperty(MagmaEngineBlock.FACING))
			return;
		Direction out = state.getValue(MagmaEngineBlock.FACING);
		Direction.Axis axis = out.getAxis();
		float angle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), axis);
		// One buffer for everything: asking for another render type would finish this one mid-draw
		VertexConsumer vc = buffer.getBuffer(RenderType.solid());

		// The shaft stub, spun about the world axis like the shaft it meets
		ms.pushPose();
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(axis == Direction.Axis.X ? Axis.XP.rotation(angle) : Axis.ZP.rotation(angle));
		ms.mulPose(Axis.YP.rotationDegrees(-(out.toYRot() + 180)));
		ms.translate(-0.5, -0.5, -0.5);
		render(ms, vc, state, CobbleGeneratorRenderer.SHAFT, light, overlay);
		ms.popPose();

		if (MagmaEngineSimModels.LOADED) {
			renderSimulatedParts(be, partialTicks, ms, vc, light, overlay, out);
			return;
		}

		// The heads, turned with the engine (modelled facing north) and a third of a turn apart
		ms.pushPose();
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-(out.toYRot() + 180)));
		ms.translate(-0.5, -0.5, -0.5);
		for (int i = 0; i < CYLINDERS; i++) {
			float lift = STROKE / 16f * (0.5f + 0.5f * Mth.sin(angle - i * Mth.TWO_PI / CYLINDERS));
			ms.pushPose();
			ms.translate(i * CYLINDER_SPACING, lift, 0);
			render(ms, vc, state, HEAD, light, overlay);
			ms.popPose();
		}
		ms.popPose();
	}

	/**
	 * Simulated's Portable Engine parts, turned so its hatch is at the back: the hatch swings open while the engine
	 * burns, and the exhaust pipes and outlets pulse while it turns, much as Simulated animates them.
	 */
	private static void renderSimulatedParts(MagmaEngineBlockEntity be, float partialTicks, PoseStack ms, VertexConsumer vc,
											 int light, int overlay, Direction out) {
		BlockState state = be.getBlockState();
		ms.pushPose();
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-MagmaEngineSimModels.yRotation(out)));
		ms.translate(-0.5, -0.5, -0.5);

		render(ms, vc, state, MagmaEngineSimModels.HATCH_TOP, light, overlay);
		// The lower hatch swings down about its hinge
		float open = be.getHatchOpen(partialTicks) * 0.65f;
		ms.pushPose();
		ms.translate(0, 0.30625, 0.23125);
		ms.mulPose(Axis.XP.rotation(-open));
		ms.translate(0, -0.30625, -0.23125);
		render(ms, vc, state, MagmaEngineSimModels.HATCH_BOTTOM, light, overlay);
		ms.popPose();

		// Pipes and outlets breathe in step while the shaft turns, and glow full-bright while it burns
		boolean lit = state.getValue(MagmaEngineBlock.LIT);
		int pipeLight = lit ? LightTexture.FULL_BRIGHT : light;
		float strength = be.getSpeed() == 0 ? 0 : Math.min(1, Math.abs(be.getSpeed()) / 64f);
		float time = net.createmod.catnip.animation.AnimationTickHolder.getRenderTime(be.getLevel()) / 20f * 7f;
		float pipeScale = 1 + (float) (Math.max(Math.sin(time) + 0.65, 0) - 0.65) * 0.033f * strength;
		float outletScale = 1 + (float) (Math.max(Math.sin(time - 1.15) + 0.65, 0) - 0.65) * 0.03f * strength;
		pulse(ms, vc, state, lit ? MagmaEngineSimModels.PIPE_RIGHT_LIT : MagmaEngineSimModels.PIPE_RIGHT, 14 / 16f, pipeScale, pipeLight, overlay);
		pulse(ms, vc, state, lit ? MagmaEngineSimModels.OUTLET_RIGHT_LIT : MagmaEngineSimModels.OUTLET_RIGHT, 14 / 16f, outletScale, pipeLight, overlay);
		pulse(ms, vc, state, lit ? MagmaEngineSimModels.PIPE_LEFT_LIT : MagmaEngineSimModels.PIPE_LEFT, 2 / 16f, pipeScale, pipeLight, overlay);
		pulse(ms, vc, state, lit ? MagmaEngineSimModels.OUTLET_LEFT_LIT : MagmaEngineSimModels.OUTLET_LEFT, 2 / 16f, outletScale, pipeLight, overlay);
		ms.popPose();
	}

	private static void pulse(PoseStack ms, VertexConsumer vc, BlockState state, ModelResourceLocation part, float centreX,
							  float scale, int light, int overlay) {
		ms.pushPose();
		ms.translate(centreX, 10 / 16f, 0.5);
		ms.scale(scale, scale, scale);
		ms.translate(-centreX, -10 / 16f, -0.5);
		render(ms, vc, state, part, light, overlay);
		ms.popPose();
	}

	private static void render(PoseStack ms, VertexConsumer vc, BlockState state, ModelResourceLocation location,
							   int light, int overlay) {
		BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
		Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(ms.last(), vc, state, model,
			1, 1, 1, light, overlay, ModelData.EMPTY, RenderType.solid());
	}
}
