package com.comedymonkey.createaddons.client.wastes.bot;

import com.comedymonkey.createaddons.content.wastes.bot.StarlightBolt;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** A starlight bolt: a short glowing streak along its flight, a pale core in a dimmer sheath (brief: "a short starlight bolt"). */
public class StarlightBoltRenderer extends EntityRenderer<StarlightBolt> {
	/** Blocks long. */
	static final double LENGTH = 0.7;

	public StarlightBoltRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(StarlightBolt bolt, float yaw, float partialTick, PoseStack ms, MultiBufferSource buffers, int light) {
		Vec3 v = bolt.getDeltaMovement();
		if (v.lengthSqr() < 1e-6)
			return;
		Vec3 half = v.normalize().scale(LENGTH / 2);
		VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
		Vec3 mid = new Vec3(0, bolt.getBbHeight() / 2, 0);
		BotRenderer.ribbon(vc, ms.last().pose(), mid.subtract(half), mid.add(half), 0.06f, BotRenderer.CORE, 0.85f);
		BotRenderer.ribbon(vc, ms.last().pose(), mid.subtract(half.scale(1.3)), mid.add(half), 0.16f, BotRenderer.STAR, 0.3f);
		super.render(bolt, yaw, partialTick, ms, buffers, light);
	}

	@SuppressWarnings("deprecation")
	@Override
	public ResourceLocation getTextureLocation(StarlightBolt bolt) {
		return TextureAtlas.LOCATION_BLOCKS;
	}
}
