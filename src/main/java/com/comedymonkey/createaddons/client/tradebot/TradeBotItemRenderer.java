package com.comedymonkey.createaddons.client.tradebot;

import java.util.Map;

import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipLibrary;
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

	/** Counts resource reloads: the models are baked again after each, as the glow texture may have changed. */
	private static int reloads;
	private int builtFor = -1;
	private ModelPart station, bot;
	private Map<String, ModelPart> stationParts, botParts;
	private GlowModel stationGlow, botGlow;

	public TradeBotItemRenderer() {
		super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
	}

	/** After a resource reload: bake the models again next time. */
	public static void forget() {
		reloads++;
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
		if (builtFor != reloads) {
			builtFor = reloads;
			station = Minecraft.getInstance().getEntityModels().bakeLayer(TradeBotGeometry.STATION_LAYER);
			bot = Minecraft.getInstance().getEntityModels().bakeLayer(TradeBotGeometry.BOT_LAYER);
			stationParts = TradeBotGeometry.get().parts(station, "station");
			botParts = TradeBotGeometry.get().parts(bot, TradeBotGeometry.BOT_ROOTS);
			stationGlow = GlowModel.of(stationParts, "station");
			botGlow = GlowModel.of(botParts, TradeBotGeometry.BOT_ROOTS);
		}
		Clip packed = ClipLibrary.get().clip(PACKED);
		station.getAllParts().forEach(ModelPart::resetPose);
		bot.getAllParts().forEach(ModelPart::resetPose);
		TradeBotModel.pose(stationParts, packed, packed.length(), null, 0, 1);
		TradeBotModel.pose(botParts, packed, packed.length(), null, 0, 1);
		TradeBotStationRenderer.render(ms, buffers, light, overlay, Direction.NORTH, station, stationGlow);
		TradeBotStationRenderer.render(ms, buffers, light, overlay, Direction.NORTH, bot, botGlow);
	}
}
