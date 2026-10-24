package com.comedymonkey.createaddons.client.tradebot;

import java.util.Map;

import com.comedymonkey.createaddons.content.tradebot.TradeBotEntity;
import com.comedymonkey.createaddons.content.tradebot.TradeBotStationBlock;
import com.comedymonkey.createaddons.content.tradebot.TradeBotStationBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the whole winding station from the same geometry and texture as Trade Bot. Its moving parts follow his
 * current clip (the winder swings in as he docks, the flag springs up as he leaves, the glow takes over his lights),
 * and its winder keeps turning while it charges him.
 */
public class TradeBotStationRenderer extends SafeBlockEntityRenderer<TradeBotStationBlockEntity> {
	private final ModelPart root;
	private final Map<String, ModelPart> parts;

	public TradeBotStationRenderer(BlockEntityRendererProvider.Context context) {
		root = context.bakeLayer(TradeBotGeometry.STATION_LAYER);
		parts = TradeBotGeometry.get().parts(root, "station");
	}

	@Override
	protected void renderSafe(TradeBotStationBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
		root.getAllParts().forEach(ModelPart::resetPose);
		TradeBotEntity bot = be.clientBot();
		if (bot != null && be.getLevel() != null) {
			TradeBotModel.pose(parts, bot, be.getLevel().getGameTime() + partialTicks);
			float spin = bot.windSpin(partialTicks);
			turn("winder_head", 0, 0, -spin);
			turn("post_cog", spin, 0, 0);
		} else if (!be.isBotDocked()) {
			ModelPart glow = parts.get("station_glow");
			if (glow != null)
				glow.xScale = glow.yScale = glow.zScale = 0;
		}
		render(ms, buffers, light, overlay, be.facing(), root);
	}

	/** Draws a station-frame model the way an entity at the station's dock point would be drawn (see TradeBotGeometry). */
	static void render(PoseStack ms, MultiBufferSource buffers, int light, int overlay, Direction facing, ModelPart model) {
		Vec3 dock = TradeBotStationBlock.dockPoint(BlockPos.ZERO, facing);
		ms.pushPose();
		ms.translate(dock.x, dock.y, dock.z);
		ms.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
		ms.scale(-1, -1, 1);
		ms.translate(0, -1.501, 0);
		model.render(ms, buffers.getBuffer(RenderType.entityCutoutNoCull(TradeBotRenderer.TEXTURE)), light, overlay);
		model.render(ms, buffers.getBuffer(RenderType.entityCutoutNoCull(TradeBotRenderer.GLOW)), LightTexture.FULL_BRIGHT, overlay);
		ms.popPose();
	}

	/** Adds a turn in Blockbench degrees to one of the station's parts. */
	private void turn(String bone, float x, float y, float z) {
		ModelPart part = parts.get(bone);
		if (part == null)
			return;
		part.xRot += -x * Mth.DEG_TO_RAD;
		part.yRot += -y * Mth.DEG_TO_RAD;
		part.zRot += z * Mth.DEG_TO_RAD;
	}
}
