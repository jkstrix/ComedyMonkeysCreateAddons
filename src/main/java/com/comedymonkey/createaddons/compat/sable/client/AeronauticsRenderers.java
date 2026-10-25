package com.comedymonkey.createaddons.compat.sable.client;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.compat.sable.AeronauticsContent;
import com.comedymonkey.createaddons.compat.sable.CompactGyroscopeBlockEntity;
import com.comedymonkey.createaddons.compat.sable.TorqueRotorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.comedymonkey.createaddons.client.CobbleGeneratorRenderer;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Quaternionf;

/** Block entity renderers for the Sable blocks. Only touched when Sable is installed. */
public final class AeronauticsRenderers {
	/** The Compact Gyroscope's gimbal (design/briefs/compact-gyroscope.md): the rings, the flywheel, the brake jaws. */
	private static final ModelResourceLocation GYRO_OUTER = part("compact_gyroscope_outer_ring");
	private static final ModelResourceLocation GYRO_INNER = part("compact_gyroscope_inner_ring");
	private static final ModelResourceLocation GYRO_FLYWHEEL = part("compact_gyroscope_flywheel");
	private static final ModelResourceLocation GYRO_JAWS_UP = part("compact_gyroscope_jaws_up");
	private static final ModelResourceLocation GYRO_JAWS_DOWN = part("compact_gyroscope_jaws_down");
	/** The shaft's two ends in the cage's collars. */
	private static final ModelResourceLocation GYRO_SHAFT = part("compact_gyroscope_shaft");
	/** The Torque Rotor's hub and spinner, and one blade (drawn three times), modelled pointing up. */
	public static final ModelResourceLocation ROTOR_BLADES = part("torque_rotor_blades");
	private static final ModelResourceLocation ROTOR_BLADE = part("torque_rotor_blade");
	private static final int BLADES = 3;
	/** How far the brake jaws travel from open to clamped on the flywheel's rim, in blocks. */
	private static final float JAW_TRAVEL = 0.25f / 16;

	private static ModelResourceLocation part(String name) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/" + name));
	}

	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(AeronauticsContent.COMPACT_GYROSCOPE_BE.get(), GyroscopeRenderer::new);
		event.registerBlockEntityRenderer(AeronauticsContent.TORQUE_ROTOR_BE.get(), RotorRenderer::new);
		event.registerBlockEntityRenderer(AeronauticsContent.FLIGHT_INSTRUMENTS_BE.get(), FlightInstrumentsRenderer::new);
	}

	public static void registerModels(ModelEvent.RegisterAdditional event) {
		for (ModelResourceLocation m : new ModelResourceLocation[] {GYRO_OUTER, GYRO_INNER, GYRO_FLYWHEEL, GYRO_JAWS_UP,
			GYRO_JAWS_DOWN, GYRO_SHAFT, ROTOR_BLADES, ROTOR_BLADE})
			event.register(m);
	}

	private static org.joml.Quaternionf rotationAbout(Direction.Axis axis, float angle) {
		return switch (axis) {
			case X -> Axis.XP.rotation(angle);
			case Y -> Axis.YP.rotation(angle);
			case Z -> Axis.ZP.rotation(angle);
		};
	}

	private static void renderBaked(PoseStack ms, VertexConsumer vc, BlockState state, BakedModel model, int light, int overlay) {
		renderBaked(ms, vc, state, model, light, overlay, RenderType.solid());
	}

	private static void renderBaked(PoseStack ms, VertexConsumer vc, BlockState state, BakedModel model, int light, int overlay,
									RenderType type) {
		Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(ms.last(), vc, state, model,
			1, 1, 1, light, overlay, ModelData.EMPTY, type);
	}

	private static BakedModel model(ModelResourceLocation location) {
		return Minecraft.getInstance().getModelManager().getModel(location);
	}

	/**
	 * The gyroscope's gimbal behind the portholes: the outer ring turns about the block's Z and the inner ring about
	 * its X so that the flywheel stays level with the world while the ship tilts round it (the angles come from the
	 * block entity, eased); the flywheel spins at its own braked speed, and the brake jaws close on its rim with the
	 * redstone signal. The shaft's ends turn with the shaft in the top and bottom collars.
	 */
	public static class GyroscopeRenderer extends SafeBlockEntityRenderer<CompactGyroscopeBlockEntity> {
		public GyroscopeRenderer(BlockEntityRendererProvider.Context context) {}

		@Override
		protected void renderSafe(CompactGyroscopeBlockEntity be, float partialTicks, PoseStack ms,
								  MultiBufferSource buffer, int light, int overlay) {
			BlockState state = be.getBlockState();
			RenderType cutout = RenderType.cutout();
			VertexConsumer vc = buffer.getBuffer(cutout);
			float angle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), Direction.Axis.Y);
			ms.pushPose();
			ms.translate(0.5, 0.5, 0.5);
			ms.pushPose();
			ms.mulPose(Axis.YP.rotation(angle));
			drawCentred(ms, vc, state, GYRO_SHAFT, light, overlay, cutout);
			ms.popPose();

			// The outer ring, then the inner ring inside it
			ms.mulPose(Axis.ZP.rotation(be.getGimbalOuter(partialTicks)));
			drawCentred(ms, vc, state, GYRO_OUTER, light, overlay, cutout);
			ms.mulPose(Axis.XP.rotation(be.getGimbalInner(partialTicks)));
			drawCentred(ms, vc, state, GYRO_INNER, light, overlay, cutout);
			// The jaws close on the rim as the signal rises
			float clamp = JAW_TRAVEL * be.getBrake() / 15f;
			ms.pushPose();
			ms.translate(0, -clamp, 0);
			drawCentred(ms, vc, state, GYRO_JAWS_UP, light, overlay, cutout);
			ms.translate(0, 2 * clamp, 0);
			drawCentred(ms, vc, state, GYRO_JAWS_DOWN, light, overlay, cutout);
			ms.popPose();
			// The flywheel keeps its own angle: the brake slows it while the shaft turns on
			ms.mulPose(Axis.YP.rotation(be.getFlywheelAngle(partialTicks)));
			drawCentred(ms, vc, state, GYRO_FLYWHEEL, light, overlay, cutout);
			ms.popPose();
		}

		/** A part modelled in block pixels, drawn with the pose at the block's centre. */
		private static void drawCentred(PoseStack ms, VertexConsumer vc, BlockState state, ModelResourceLocation part,
										int light, int overlay, RenderType type) {
			ms.pushPose();
			ms.translate(-0.5, -0.5, -0.5);
			renderBaked(ms, vc, state, model(part), light, overlay, type);
			ms.popPose();
		}
	}

	/** The propeller spinning on its mount, and the shaft end going into the base. */
	public static class RotorRenderer extends SafeBlockEntityRenderer<TorqueRotorBlockEntity> {
		public RotorRenderer(BlockEntityRendererProvider.Context context) {}

		@Override
		protected void renderSafe(TorqueRotorBlockEntity be, float partialTicks, PoseStack ms,
								  MultiBufferSource buffer, int light, int overlay) {
			BlockState state = be.getBlockState();
			if (!state.hasProperty(DirectionalKineticBlock.FACING))
				return;
			Direction facing = state.getValue(DirectionalKineticBlock.FACING);
			Direction.Axis axis = facing.getAxis();
			// One buffer for everything: asking for a second render type would finish this one mid-draw
			VertexConsumer vc = buffer.getBuffer(RenderType.solid());
			float angle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), axis);
			// Spin about the world axis exactly as Create spins shafts, then stand the parts on the facing
			ms.pushPose();
			ms.translate(0.5, 0.5, 0.5);
			ms.mulPose(rotationAbout(axis, angle));
			ms.pushPose();
			// The shaft stub is modelled going into the north face; it goes into the back here
			Direction back = facing.getOpposite();
			ms.mulPose(new Quaternionf().rotateTo(0, 0, -1, back.getStepX(), back.getStepY(), back.getStepZ()));
			ms.translate(-0.5, -0.5, -0.5);
			renderBaked(ms, vc, state, model(CobbleGeneratorRenderer.SHAFT), light, overlay);
			ms.popPose();
			// The propeller is modelled pointing up: the hub and spinner, and one blade drawn three times round it
			ms.mulPose(new Quaternionf().rotateTo(0, 1, 0, facing.getStepX(), facing.getStepY(), facing.getStepZ()));
			for (int i = 0; i < BLADES; i++) {
				ms.pushPose();
				ms.mulPose(Axis.YP.rotation(i * Mth.TWO_PI / BLADES));
				ms.translate(-0.5, -0.5, -0.5);
				renderBaked(ms, vc, state, model(ROTOR_BLADE), light, overlay);
				ms.popPose();
			}
			ms.translate(-0.5, -0.5, -0.5);
			renderBaked(ms, vc, state, model(ROTOR_BLADES), light, overlay);
			ms.popPose();
		}
	}

	private AeronauticsRenderers() {}
}
