package com.comedymonkey.createaddons.client.helperbot;

import com.comedymonkey.createaddons.content.helperbot.HelperBotBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;

/**
 * Draws a placed Helper Bot when Flywheel can't ({@link HelperBotVisual} draws him when it can), and in Ponder (on
 * contraptions {@code HelperBotMovement} draws him instead).
 */
public class HelperBotRenderer implements BlockEntityRenderer<HelperBotBlockEntity> {
	public HelperBotRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public void render(HelperBotBlockEntity be, float partialTick, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
		HelperBotClient.renderPlaced(be, partialTick, ms, buffers, light, overlay);
	}

	/** He's half a block taller than his block, and his arms and swivel reach past its sides (worked out once). */
	@Override
	public AABB getRenderBoundingBox(HelperBotBlockEntity be) {
		return HelperBotClient.renderBox(be);
	}
}
