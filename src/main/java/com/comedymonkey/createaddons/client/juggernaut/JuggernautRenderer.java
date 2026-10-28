package com.comedymonkey.createaddons.client.juggernaut;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.tradebot.GlowModel;
import com.comedymonkey.createaddons.content.juggernaut.ClockworkJuggernaut;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws the Clockwork Juggernaut at full size (1 px = 1/16 block, D53), with his frost-blue runes, cracks and slit at
 * full brightness from a second texture holding only those pixels.
 */
public class JuggernautRenderer extends MobRenderer<ClockworkJuggernaut, JuggernautModel> {
	public static final ResourceLocation TEXTURE = ComedyMonkeysCreateAddons.asResource("textures/entity/clockwork_juggernaut.png");
	public static final ResourceLocation GLOW = ComedyMonkeysCreateAddons.asResource("textures/entity/clockwork_juggernaut_glow.png");

	public JuggernautRenderer(EntityRendererProvider.Context context) {
		super(context, new JuggernautModel(context.bakeLayer(JuggernautModel.LAYER)), 3.0f);
		addLayer(new Glow(this, GlowModel.of(JuggernautModel.geometry(), GLOW, getModel().parts(), JuggernautModel.ROOT)));
	}

	@Override
	public ResourceLocation getTextureLocation(ClockworkJuggernaut juggernaut) {
		return TEXTURE;
	}

	@Override
	protected float getFlipDegrees(ClockworkJuggernaut juggernaut) {
		return 0; // he plays his own death; he never topples over sideways
	}

	private static class Glow extends RenderLayer<ClockworkJuggernaut, JuggernautModel> {
		private final GlowModel glow;

		Glow(RenderLayerParent<ClockworkJuggernaut, JuggernautModel> parent, GlowModel glow) {
			super(parent);
			this.glow = glow;
		}

		@Override
		public void render(PoseStack poseStack, MultiBufferSource buffers, int light, ClockworkJuggernaut juggernaut, float limbSwing,
			float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
			if (juggernaut.isInvisible())
				return;
			glow.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(GLOW)), OverlayTexture.NO_OVERLAY);
		}
	}
}
