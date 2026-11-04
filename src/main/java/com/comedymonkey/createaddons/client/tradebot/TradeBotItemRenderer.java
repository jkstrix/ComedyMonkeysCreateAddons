package com.comedymonkey.createaddons.client.tradebot;

import java.util.Map;

import com.comedymonkey.createaddons.content.tradebot.BotKind;
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

	private final BotKind kind;

	public TradeBotItemRenderer() {
		this(BotKind.TRADE_BOT);
	}

	/** One kind's item: Trade Bot's, or the Trade Master's (his own model, folded up the same way). */
	public TradeBotItemRenderer(BotKind kind) {
		super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
		this.kind = kind;
	}

	/** After a resource reload: bake the models again next time. */
	public static void forget() {
		reloads++;
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
		if (builtFor != reloads) {
			builtFor = reloads;
			TradeBotGeometry geometry = TradeBotGeometry.of(kind);
			station = Minecraft.getInstance().getEntityModels().bakeLayer(TradeBotGeometry.stationLayerOf(kind));
			bot = Minecraft.getInstance().getEntityModels().bakeLayer(TradeBotGeometry.layerOf(kind));
			stationParts = geometry.parts(station, "station");
			botParts = geometry.parts(bot, TradeBotGeometry.BOT_ROOTS);
			stationGlow = GlowModel.of(geometry, kind.glowTexture(), stationParts, "station");
			botGlow = GlowModel.of(geometry, kind.glowTexture(), botParts, TradeBotGeometry.BOT_ROOTS);
		}
		Clip packed = kind.own(ClipLibrary.get().clip(PACKED));
		station.getAllParts().forEach(ModelPart::resetPose);
		bot.getAllParts().forEach(ModelPart::resetPose);
		TradeBotModel.pose(stationParts, packed, packed.length(), null, 0, 1);
		TradeBotModel.pose(botParts, packed, packed.length(), null, 0, 1);
		TradeBotStationRenderer.render(ms, buffers, light, overlay, Direction.NORTH, station, stationGlow, kind);
		TradeBotStationRenderer.render(ms, buffers, light, overlay, Direction.NORTH, bot, botGlow, kind);
	}
}
