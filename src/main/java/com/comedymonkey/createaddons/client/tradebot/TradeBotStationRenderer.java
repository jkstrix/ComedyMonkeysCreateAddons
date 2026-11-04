package com.comedymonkey.createaddons.client.tradebot;

import java.util.EnumMap;
import java.util.Map;

import com.comedymonkey.createaddons.content.tradebot.BotKind;
import com.comedymonkey.createaddons.content.tradebot.TradeBotEntity;
import com.comedymonkey.createaddons.content.tradebot.TradeBotStationBlock;
import com.comedymonkey.createaddons.content.tradebot.TradeBotStationBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.model.geom.ModelPart;
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
	/** One station model per kind (Trade Bot's, the Trade Master's), and just its cubes with any glow on them. */
	private record Station(ModelPart root, Map<String, ModelPart> parts, GlowModel glow) {}

	private final Map<BotKind, Station> stations = new EnumMap<>(BotKind.class);

	public TradeBotStationRenderer(BlockEntityRendererProvider.Context context) {
		for (BotKind kind : BotKind.values()) {
			ModelPart root = context.bakeLayer(TradeBotGeometry.stationLayerOf(kind));
			TradeBotGeometry geometry = TradeBotGeometry.of(kind);
			Map<String, ModelPart> parts = geometry.parts(root, "station");
			stations.put(kind, new Station(root, parts, GlowModel.of(geometry, kind.glowTexture(), parts, "station")));
		}
	}

	@Override
	protected void renderSafe(TradeBotStationBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
		// with Flywheel on, its visual draws it (TradeBotStationVisual)
		if (com.comedymonkey.createaddons.client.parts.PartSink.instanced(be))
			return;
		Station station = stations.get(be.kind());
		pose(station.parts(), station.root(), be, partialTicks, null);
		render(ms, buffers, light, overlay, be.facing(), station.root(), station.glow(), be.kind());
	}

	/**
	 * Poses the station's bones for this frame: following Trade Bot's clip (through {@code playback}, or his own if null),
	 * its winder turning while it charges him.
	 */
	static void pose(Map<String, ModelPart> parts, ModelPart root, TradeBotStationBlockEntity be, float partialTicks,
					 @org.jetbrains.annotations.Nullable com.comedymonkey.createaddons.content.tradebot.clip.ClipPlayback playback) {
		root.getAllParts().forEach(ModelPart::resetPose);
		TradeBotEntity bot = be.clientBot();
		if (bot != null && be.getLevel() != null) {
			TradeBotModel.pose(parts, bot, playback != null ? playback : bot.playback, bot.clipNow(partialTicks));
			float spin = bot.windSpin(partialTicks);
			turn(parts, "winder_head", 0, 0, -spin);
			turn(parts, "post_cog", spin, 0, 0);
		} else if (!be.isBotDocked()) {
			ModelPart glow = parts.get("station_glow");
			if (glow != null)
				glow.xScale = glow.yScale = glow.zScale = 0;
		}
	}

	/**
	 * Draws a station-frame model the way an entity at the station's dock point would be drawn (see TradeBotGeometry),
	 * then its glowing cubes at full brightness.
	 */
	static void render(PoseStack ms, MultiBufferSource buffers, int light, int overlay, Direction facing, ModelPart model,
					   GlowModel glow, BotKind kind) {
		Vec3 dock = TradeBotStationBlock.dockPoint(BlockPos.ZERO, facing);
		ms.pushPose();
		ms.translate(dock.x, dock.y, dock.z);
		ms.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
		ms.scale(-1, -1, 1);
		ms.translate(0, -1.501, 0);
		model.render(ms, buffers.getBuffer(RenderType.entityCutoutNoCull(kind.texture())), light, overlay);
		glow.render(ms, buffers.getBuffer(RenderType.entityCutoutNoCull(kind.glowTexture())), overlay);
		ms.popPose();
	}

	/** Adds a turn in Blockbench degrees to one of the station's parts. */
	private static void turn(Map<String, ModelPart> parts, String bone, float x, float y, float z) {
		ModelPart part = parts.get(bone);
		if (part == null)
			return;
		part.xRot += -x * Mth.DEG_TO_RAD;
		part.yRot += -y * Mth.DEG_TO_RAD;
		part.zRot += z * Mth.DEG_TO_RAD;
	}
}
