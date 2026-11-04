package com.comedymonkey.createaddons.client.tradebot;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.tradebot.BotKind;
import com.comedymonkey.createaddons.content.tradebot.TradeBotEntity;
import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Draws Trade Bot. In physical clips he's drawn on the clip's own path from where it started, worked out here from
 * the clip time, rather than where network smoothing has got to, so his body and his travel can't drift apart.
 */
public final class TradeBotRenderer extends MobRenderer<TradeBotEntity, TradeBotModel> {
	public static final ResourceLocation TEXTURE = ComedyMonkeysCreateAddons.asResource("textures/entity/trade_bot.png");
	public static final ResourceLocation GLOW = ComedyMonkeysCreateAddons.asResource("textures/entity/trade_bot_glow.png");

	private final BotKind kind;

	public TradeBotRenderer(EntityRendererProvider.Context context) {
		this(context, BotKind.TRADE_BOT);
	}

	/** One kind's renderer: his own model, texture and glow (the Trade Master's beard, monocle and Stock Link bulb). */
	public TradeBotRenderer(EntityRendererProvider.Context context, BotKind kind) {
		super(context, new TradeBotModel(context.bakeLayer(TradeBotGeometry.layerOf(kind)), TradeBotGeometry.of(kind)), 0.35f);
		this.kind = kind;
		addLayer(new Glow(this, GlowModel.of(TradeBotGeometry.of(kind), kind.glowTexture(), getModel().parts(), TradeBotGeometry.BOT_ROOTS),
			kind.glowTexture()));
	}

	@Override
	public ResourceLocation getTextureLocation(TradeBotEntity bot) {
		return kind.texture();
	}

	@Override
	public void render(TradeBotEntity bot, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
		Clip clip = bot.clip();
		if (clip.root() != Clip.Root.PHYSICAL) {
			super.render(bot, entityYaw, partialTick, poseStack, buffers, light);
			return;
		}
		Vec3 offset = bot.rootPosition(clip, clip.localTime(bot.clipElapsed(partialTick))).subtract(bot.getPosition(partialTick));
		poseStack.pushPose();
		poseStack.translate(offset.x, offset.y, offset.z);
		super.render(bot, entityYaw, partialTick, poseStack, buffers, light);
		poseStack.popPose();
	}

	@Override
	protected void setupRotations(TradeBotEntity bot, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale) {
		Clip clip = bot.clip();
		if (clip.root() == Clip.Root.PHYSICAL)
			yBodyRot = bot.rootYaw(clip, clip.localTime(bot.clipElapsed(partialTick)));
		super.setupRotations(bot, poseStack, bob, yBodyRot, partialTick, scale);
	}

	/** His irises, chest lamp and station glow at full brightness, whenever the clip has them lit. */
	private static class Glow extends RenderLayer<TradeBotEntity, TradeBotModel> {
		/** Just the cubes with any glow on them, posed as he is. */
		private final GlowModel glow;
		private final ResourceLocation texture;

		Glow(RenderLayerParent<TradeBotEntity, TradeBotModel> parent, GlowModel glow, ResourceLocation texture) {
			super(parent);
			this.glow = glow;
			this.texture = texture;
		}

		@Override
		public void render(PoseStack poseStack, MultiBufferSource buffers, int light, TradeBotEntity bot, float limbSwing,
			float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
			glow.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), OverlayTexture.NO_OVERLAY);
		}
	}
}
