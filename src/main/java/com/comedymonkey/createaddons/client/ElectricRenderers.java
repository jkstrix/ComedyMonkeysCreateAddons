package com.comedymonkey.createaddons.client;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.electric.ElectricMotorBlock;
import com.comedymonkey.createaddons.content.electric.ElectricMotorBlockEntity;
import com.comedymonkey.createaddons.content.electric.GeothermalControllerBlock;
import com.comedymonkey.createaddons.content.electric.GeothermalControllerBlockEntity;
import com.comedymonkey.createaddons.content.electric.LavaGeneratorBlock;
import com.comedymonkey.createaddons.content.electric.LavaGeneratorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/** The moving and whole-machine parts of the Electric Motor, Lava Generator and Geothermal Turbine. */
public final class ElectricRenderers {
	public static final ModelResourceLocation GENERATOR_ROTOR = model("lava_generator_rotor");
	public static final ModelResourceLocation TURBINE = model("geothermal_turbine");
	public static final ModelResourceLocation TURBINE_GLOW = model("geothermal_turbine_glow");
	public static final ModelResourceLocation GAUGE_LAVA = model("geothermal_turbine_gauge_lava");
	public static final ModelResourceLocation GAUGE_WATER = model("geothermal_turbine_gauge_water");

	private ElectricRenderers() {}

	private static ModelResourceLocation model(String name) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/" + name));
	}

	public static void registerModels(ModelEvent.RegisterAdditional event) {
		for (ModelResourceLocation m : new ModelResourceLocation[] {GENERATOR_ROTOR, TURBINE, TURBINE_GLOW, GAUGE_LAVA, GAUGE_WATER})
			event.register(m);
	}

	private static void drawModel(PoseStack ms, VertexConsumer vc, BlockState state, ModelResourceLocation location, int light) {
		Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(ms.last(), vc, state,
			Minecraft.getInstance().getModelManager().getModel(location), 1, 1, 1, light,
			net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.cutout());
	}

	/** Turns a model made facing north to face {@code facing}, about the block's centre. */
	private static void face(PoseStack ms, Direction facing) {
		ms.translate(0.5, 0.5, 0.5);
		if (facing == Direction.UP)
			ms.mulPose(Axis.XP.rotationDegrees(90));
		else if (facing == Direction.DOWN)
			ms.mulPose(Axis.XP.rotationDegrees(-90));
		else
			ms.mulPose(Axis.YP.rotationDegrees(-(facing.toYRot() + 180)));
		ms.translate(-0.5, -0.5, -0.5);
	}

	/** The motor's shaft end, turning with the network. */
	public static class Motor extends SafeBlockEntityRenderer<ElectricMotorBlockEntity> {
		public Motor(BlockEntityRendererProvider.Context context) {}

		@Override
		protected void renderSafe(ElectricMotorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
								  int light, int overlay) {
			BlockState state = be.getBlockState();
			Direction facing = state.getValue(ElectricMotorBlock.FACING);
			Direction.Axis axis = facing.getAxis();
			float angle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), axis);
			ms.pushPose();
			ms.translate(0.5, 0.5, 0.5);
			ms.mulPose(switch (axis) {
				case X -> Axis.XP.rotation(angle);
				case Y -> Axis.YP.rotation(angle);
				case Z -> Axis.ZP.rotation(angle);
			});
			ms.translate(-0.5, -0.5, -0.5);
			face(ms, facing);
			drawModel(ms, buffer.getBuffer(RenderType.cutout()), state, CobbleGeneratorRenderer.SHAFT, light);
			ms.popPose();
		}
	}

	/** The generator's turbine wheel, spinning behind its intake glass while it burns. */
	public static class Generator extends SafeBlockEntityRenderer<LavaGeneratorBlockEntity> {
		public Generator(BlockEntityRendererProvider.Context context) {}

		@Override
		protected void renderSafe(LavaGeneratorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
								  int light, int overlay) {
			BlockState state = be.getBlockState();
			ms.pushPose();
			face(ms, state.getValue(LavaGeneratorBlock.FACING));
			ms.translate(8 / 16f, 11.5 / 16f, 4 / 16f);
			ms.mulPose(Axis.ZP.rotationDegrees(be.getRotorAngle(partialTicks)));
			ms.translate(-8 / 16f, -11.5 / 16f, -4 / 16f);
			drawModel(ms, buffer.getBuffer(RenderType.cutout()), state, GENERATOR_ROTOR, light);
			ms.popPose();
		}
	}

	/** The whole formed turbine, drawn round its middle block, with its gauges showing the tanks. */
	public static class Turbine extends SafeBlockEntityRenderer<GeothermalControllerBlockEntity> {
		public Turbine(BlockEntityRendererProvider.Context context) {}

		@Override
		public boolean shouldRenderOffScreen(GeothermalControllerBlockEntity be) {
			return true;
		}

		@Override
		public int getViewDistance() {
			return 96;
		}

		@Override
		protected void renderSafe(GeothermalControllerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
								  int light, int overlay) {
			BlockState state = be.getBlockState();
			if (!state.getValue(GeothermalControllerBlock.FORMED) || be.getLevel() == null)
				return;
			Direction facing = state.getValue(GeothermalControllerBlock.FACING);
			// Lit like the air in front of the panel, so the big model isn't dark from sampling its own inside
			int front = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(facing));
			VertexConsumer vc = buffer.getBuffer(RenderType.cutout());
			ms.pushPose();
			Direction back = facing.getOpposite();
			ms.translate(back.getStepX(), 0, back.getStepZ());
			face(ms, facing);
			drawModel(ms, vc, state, TURBINE, front);
			drawModel(ms, vc, state, TURBINE_GLOW, state.getValue(GeothermalControllerBlock.LIT) ? LightTexture.FULL_BRIGHT : front);
			gauge(ms, vc, state, GAUGE_LAVA, be.lavaFill());
			gauge(ms, vc, state, GAUGE_WATER, be.waterFill());
			ms.popPose();
		}

		/** A gauge's fill, from the bottom of its window (y -3 in the model) up to its level. */
		private static void gauge(PoseStack ms, VertexConsumer vc, BlockState state, ModelResourceLocation model, float fill) {
			if (fill <= 0.005f)
				return;
			ms.pushPose();
			ms.translate(0, -3 / 16f, 0);
			ms.scale(1, Math.min(1, fill), 1);
			ms.translate(0, 3 / 16f, 0);
			drawModel(ms, vc, state, model, LightTexture.FULL_BRIGHT);
			ms.popPose();
		}
	}
}
