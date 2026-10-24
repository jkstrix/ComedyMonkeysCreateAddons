package com.comedymonkey.createaddons.client.tradebot;

import java.util.Map;

import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipLibrary;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipRoot;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * The Trade Bot item in 3D everywhere, the inventory included: his station with him folded up on it, lights off
 * (design/briefs/trade-bot-package.md). Placing it sets him down unfolded.
 */
public class TradeBotItemRenderer extends BlockEntityWithoutLevelRenderer {
	/** The folded-up pose, exported from the Blockbench preview like his other clips. */
	public static final String PACKED = "packed";

	private ModelPart station, bot;

	public TradeBotItemRenderer() {
		super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
		if (station == null) {
			station = Minecraft.getInstance().getEntityModels().bakeLayer(TradeBotGeometry.STATION_LAYER);
			bot = Minecraft.getInstance().getEntityModels().bakeLayer(TradeBotGeometry.BOT_LAYER);
		}
		Clip packed = ClipLibrary.get().clip(PACKED);
		station.getAllParts().forEach(ModelPart::resetPose);
		bot.getAllParts().forEach(ModelPart::resetPose);
		Map<String, ModelPart> stationParts = TradeBotGeometry.get().parts(station, "station");
		Map<String, ModelPart> botParts = TradeBotGeometry.get().parts(bot, ClipRoot.BONE);
		TradeBotModel.pose(stationParts, packed, packed.length(), null, 0, 1);
		TradeBotModel.pose(botParts, packed, packed.length(), null, 0, 1);
		TradeBotStationRenderer.render(ms, buffers, light, overlay, Direction.NORTH, station);
		TradeBotStationRenderer.render(ms, buffers, light, overlay, Direction.NORTH, bot);
	}
}
