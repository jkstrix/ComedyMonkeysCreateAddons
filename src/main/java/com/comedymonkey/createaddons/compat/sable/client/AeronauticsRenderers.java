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
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Quaternionf;

/** Block entity renderers for the Sable blocks. Only touched when Sable is installed. */
public final class AeronauticsRenderers {
	/** Create's own flywheel, drawn shrunk inside the Compact Gyroscope. */
	public static final ModelResourceLocation FLYWHEEL =
		ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("create", "block/flywheel/block"));
	/** The Torque Rotor's hub and four pitched blades, modelled pointing up. */
	public static final ModelResourceLocation ROTOR_BLADES =
		ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/torque_rotor_blades"));
	private static final ResourceLocation CREATE_SHAFT = ResourceLocation.fromNamespaceAndPath("create", "shaft");
	/** Shrinks the 2.75-block flywheel to 12 pixels across, so it clears the 2-pixel frame. */
	private static final float FLYWHEEL_SCALE = 0.26f;

	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(AeronauticsContent.COMPACT_GYROSCOPE_BE.get(), GyroscopeRenderer::new);
		event.registerBlockEntityRenderer(AeronauticsContent.TORQUE_ROTOR_BE.get(), RotorRenderer::new);
		event.registerBlockEntityRenderer(AeronauticsContent.FLIGHT_INSTRUMENTS_BE.get(), FlightInstrumentsRenderer::new);
	}

	public static void registerModels(ModelEvent.RegisterAdditional event) {
		event.register(FLYWHEEL);
		event.register(ROTOR_BLADES);
	}

	/** A through-shaft along the axis, turning exactly as Create turns the shafts it meets. */
	private static void renderShaft(KineticBlockEntity be, Direction.Axis axis, float angle, PoseStack ms,
									VertexConsumer vc, int light, int overlay) {
		BlockState shaft = BuiltInRegistries.BLOCK.get(CREATE_SHAFT).defaultBlockState().setValue(RotatedPillarKineticBlock.AXIS, axis);
		ms.pushPose();
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(rotationAbout(axis, angle));
		ms.translate(-0.5, -0.5, -0.5);
		renderBaked(ms, vc, be.getBlockState(), Minecraft.getInstance().getBlockRenderer().getBlockModel(shaft), light, overlay);
		ms.popPose();
	}

	private static org.joml.Quaternionf rotationAbout(Direction.Axis axis, float angle) {
		return switch (axis) {
			case X -> Axis.XP.rotation(angle);
			case Y -> Axis.YP.rotation(angle);
			case Z -> Axis.ZP.rotation(angle);
		};
	}

	private static void renderBaked(PoseStack ms, VertexConsumer vc, BlockState state, BakedModel model, int light, int overlay) {
		Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(ms.last(), vc, state, model,
			1, 1, 1, light, overlay, ModelData.EMPTY, RenderType.solid());
	}

	private static BakedModel model(ModelResourceLocation location) {
		return Minecraft.getInstance().getModelManager().getModel(location);
	}

	/** The flywheel on its shaft, spinning behind the glass. */
	public static class GyroscopeRenderer extends SafeBlockEntityRenderer<CompactGyroscopeBlockEntity> {
		public GyroscopeRenderer(BlockEntityRendererProvider.Context context) {}

		@Override
		protected void renderSafe(CompactGyroscopeBlockEntity be, float partialTicks, PoseStack ms,
								  MultiBufferSource buffer, int light, int overlay) {
			VertexConsumer vc = buffer.getBuffer(RenderType.solid());
			float angle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), Direction.Axis.Y);
			renderShaft(be, Direction.Axis.Y, angle, ms, vc, light, overlay);
			ms.pushPose();
			ms.translate(0.5, 0.5, 0.5);
			ms.mulPose(Axis.YP.rotation(angle));
			ms.scale(FLYWHEEL_SCALE, FLYWHEEL_SCALE, FLYWHEEL_SCALE);
			ms.translate(-0.5, -0.5, -0.5);
			renderBaked(ms, vc, be.getBlockState(), model(FLYWHEEL), light, overlay);
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
			// The propeller is modelled pointing up
			ms.mulPose(new Quaternionf().rotateTo(0, 1, 0, facing.getStepX(), facing.getStepY(), facing.getStepZ()));
			ms.translate(-0.5, -0.5, -0.5);
			renderBaked(ms, vc, state, model(ROTOR_BLADES), light, overlay);
			ms.popPose();
		}
	}

	private AeronauticsRenderers() {}
}
