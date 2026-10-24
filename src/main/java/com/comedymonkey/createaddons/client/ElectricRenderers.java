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
	public static final ModelResourceLocation TURBINE_ROTOR = model("geothermal_turbine_rotor");
	public static final ModelResourceLocation NEEDLE_ENERGY = model("geothermal_turbine_needle_energy");
	public static final ModelResourceLocation NEEDLE_STEAM = model("geothermal_turbine_needle_steam");
	public static final ModelResourceLocation LAMP_OUTPUT = model("geothermal_turbine_lamp_output");
	public static final ModelResourceLocation LAMP_STALL = model("geothermal_turbine_lamp_stall");

	private ElectricRenderers() {}

	private static ModelResourceLocation model(String name) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/" + name));
	}

	public static void registerModels(ModelEvent.RegisterAdditional event) {
		for (ModelResourceLocation m : new ModelResourceLocation[] {GENERATOR_ROTOR, TURBINE, TURBINE_GLOW, GAUGE_LAVA, GAUGE_WATER,
			TURBINE_ROTOR, NEEDLE_ENERGY, NEEDLE_STEAM, LAMP_OUTPUT, LAMP_STALL})
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

	/**
	 * The whole formed turbine, drawn round its middle block: the rotor turning behind its window, the firebox glow while
	 * it runs, the lava and water gauges, the energy and steam dials, and the output and stall lamps. Pivots are in the
	 * formed model's pixels (design/models/geothermal-turbine.gen.js).
	 */
	public static class Turbine extends SafeBlockEntityRenderer<GeothermalControllerBlockEntity> {
		/** The rotor turns about X through (y 20, z 2). */
		private static final float ROTOR_Y = 20, ROTOR_Z = 2;
		/** The energy dial on the panel (turns about Z) and the steam dial on the boiler's west end (turns about X). */
		private static final float ENERGY_X = 2, ENERGY_Y = 1, ENERGY_Z = -15.75f, STEAM_X = -13.25f, STEAM_Y = 9, STEAM_Z = 18;
		/** The gauge windows' foot: the fills grow up from here. */
		private static final float GAUGE_FOOT = -3;

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
			boolean lit = state.getValue(GeothermalControllerBlock.LIT);
			drawModel(ms, vc, state, TURBINE, front);
			// The firebox only glows while it burns; off, the grates are dark iron
			if (lit)
				drawModel(ms, vc, state, TURBINE_GLOW, LightTexture.FULL_BRIGHT);
			turned(ms, vc, state, TURBINE_ROTOR, front, Axis.XP, -be.rotorAngle(partialTicks), 0, ROTOR_Y, ROTOR_Z);
			// Energy dial sweeps clockwise from lower left (empty) to lower right (full); steam follows the load
			turned(ms, vc, state, NEEDLE_ENERGY, front, Axis.ZP, -135 + 270 * be.energyFill(), ENERGY_X, ENERGY_Y, ENERGY_Z);
			turned(ms, vc, state, NEEDLE_STEAM, front, Axis.XP, 70 - 140 * be.load() * Math.min(1, be.rotorSpeed() * 4), STEAM_X, STEAM_Y, STEAM_Z);
			if (lit)
				drawModel(ms, vc, state, LAMP_OUTPUT, LightTexture.FULL_BRIGHT);
			if (be.isStalled())
				drawModel(ms, vc, state, LAMP_STALL, LightTexture.FULL_BRIGHT);
			gauge(ms, vc, state, GAUGE_LAVA, be.lavaFill());
			gauge(ms, vc, state, GAUGE_WATER, be.waterFill());
			ms.popPose();
		}

		/** A part turned by {@code degrees} about {@code axis} through a pivot given in model pixels. */
		private static void turned(PoseStack ms, VertexConsumer vc, BlockState state, ModelResourceLocation model, int light,
								   Axis axis, float degrees, float x, float y, float z) {
			ms.pushPose();
			ms.translate(x / 16, y / 16, z / 16);
			ms.mulPose(axis.rotationDegrees(degrees));
			ms.translate(-x / 16, -y / 16, -z / 16);
			drawModel(ms, vc, state, model, light);
			ms.popPose();
		}

		/** A gauge's fill, from the foot of its window up to its level. */
		private static void gauge(PoseStack ms, VertexConsumer vc, BlockState state, ModelResourceLocation model, float fill) {
			if (fill <= 0.005f)
				return;
			ms.pushPose();
			ms.translate(0, GAUGE_FOOT / 16, 0);
			ms.scale(1, Math.min(1, fill), 1);
			ms.translate(0, -GAUGE_FOOT / 16, 0);
			drawModel(ms, vc, state, model, LightTexture.FULL_BRIGHT);
			ms.popPose();
		}
	}
}
