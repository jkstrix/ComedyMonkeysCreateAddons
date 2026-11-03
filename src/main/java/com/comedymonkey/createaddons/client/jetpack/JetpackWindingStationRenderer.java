package com.comedymonkey.createaddons.client.jetpack;

import com.comedymonkey.createaddons.client.parts.PartSink;
import com.comedymonkey.createaddons.content.jetpack.JetpackWindingStationBlockEntity;
import com.comedymonkey.createaddons.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * The Jetpack Winding Station's moving parts (the approved model's renderer parts): the drive turning in the post's slot
 * with the shaft below; the boom, which reaches out (its arm telescoping forward while still parked high), lowers in
 * front of the jetpack and drops its socket onto the key, turning it while it winds, then lifts and draws back; and the
 * jetpack hung on it, turned round so its key points out the front, its porthole showing how wound it is.
 */
public class JetpackWindingStationRenderer extends SafeBlockEntityRenderer<JetpackWindingStationBlockEntity> {
	private static final ModelResourceLocation DRIVE = JetpackModel.part("jetpack_winding_station_drive"),
		BOOM = JetpackModel.part("jetpack_winding_station_boom"), REACH = JetpackModel.part("jetpack_winding_station_reach"),
		SOCKET = JetpackModel.part("jetpack_winding_station_socket");
	/** The generator's: the boom parks this far up; its reach is built pulled in, and slides this far out for each case. */
	public static final float BOOM_PARK = 60, REACH_HUNG = -5.5f, REACH_WORN = -1.5f;

	public JetpackWindingStationRenderer(BlockEntityRendererProvider.Context context) {}

	static void registerModels(ModelEvent.RegisterAdditional event) {
		for (ModelResourceLocation part : new ModelResourceLocation[] { DRIVE, BOOM, REACH, SOCKET })
			event.register(part);
	}

	@Override
	protected void renderSafe(JetpackWindingStationBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light,
							  int overlay) {
		parts(be, partialTicks, ms, PartSink.forRenderer(be, buffer, overlay), light);
	}

	/** How far the socket has turned the key (degrees): the shaft's turn, in 30° ticks, while the boom is down. */
	public static float socketDegrees(JetpackWindingStationBlockEntity be) {
		if (be.boomDown() < 0.99f || be.getLevel() == null)
			return 0;
		float shaft = (float) Math.toDegrees(KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), Direction.Axis.Y));
		return (float) Math.floor(shaft * 0.5f / 30) * 30;
	}

	/** Its part models (for this renderer, or Flywheel's visual), posed in the block's north-facing model space. */
	public static void parts(JetpackWindingStationBlockEntity be, float partialTicks, PoseStack ms, PartSink sink, int light) {
		RenderType type = RenderType.cutout();
		ms.pushPose();
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(Axis.YP.rotationDegrees(-(be.facing().toYRot() + 180)));
		ms.translate(-0.5, -0.5, -0.5);

		// the drive turning in the post's slot with the shaft below
		float shaft = be.getLevel() == null ? 0 : (float) Math.toDegrees(KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), Direction.Axis.Y));
		pivot(ms, 8, 0, 13, Axis.YP, shaft, () -> sink.part(DRIVE, ms, light, type));

		// the boom: up when parked, down in front of the jetpack; its reach slides out first and in last
		float down = be.boomDown(partialTicks), out = be.reachOut(partialTicks);
		float socket = socketDegrees(be);
		ms.pushPose();
		ms.translate(8 / 16f, 26 / 16f, 14 / 16f);
		ms.mulPose(Axis.XP.rotationDegrees(BOOM_PARK * (1 - down)));
		ms.translate(-8 / 16f, -26 / 16f, -14 / 16f);
		sink.part(BOOM, ms, light, type);
		ms.translate(0, 0, (be.reachingForWearer() ? REACH_WORN : REACH_HUNG) * out / 16);
		sink.part(REACH, ms, light, type);
		pivot(ms, 8, 20, 7.375f, Axis.ZP, socket, () -> sink.part(SOCKET, ms, light, type));
		ms.popPose();

		// the hung jetpack, turned round on the post (its plate against it, key out the front), its key turning with the socket
		if (!be.hung().isEmpty()) {
			ms.pushPose();
			ms.translate(8 / 16f, 0, 8.25 / 16f);
			ms.mulPose(Axis.YP.rotationDegrees(180));
			ms.translate(-8 / 16f, 8 / 16f, -8.25 / 16f);
			JetpackModel.Variant variant = be.hung().is(ModItems.NETHERITE_CLOCKWORK_JETPACK.get()) ? JetpackModel.Variant.NETHERITE
				: JetpackModel.Variant.CLOCKWORK;
			JetpackModel.draw(ms, sink, light, type, variant, new JetpackModel.Pose(be.hungFill(), -socket,
				be.boom() == JetpackWindingStationBlockEntity.Boom.HUNG ? JetpackWorn.pawlClick(socket) : 0, 0, 0, 0, 0, JetpackModel.Harness.NONE, false));
			ms.popPose();
		}
		ms.popPose();
	}

	private static void pivot(PoseStack ms, float px, float py, float pz, Axis axis, float degrees, Runnable draw) {
		ms.pushPose();
		ms.translate(px / 16, py / 16, pz / 16);
		ms.mulPose(axis.rotationDegrees(degrees));
		ms.translate(-px / 16, -py / 16, -pz / 16);
		draw.run();
		ms.popPose();
	}

}
