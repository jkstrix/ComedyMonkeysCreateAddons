package com.comedymonkey.createaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.comedymonkey.createaddons.registry.ModAttachments;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Pipez draws a pipe's extracting collars itself; under an opaque cover they're hidden anyway, and would only flicker
 * through where they touch the cover's face.
 */
@Pseudo
@Mixin(targets = "de.maxhenkel.pipez.blocks.tileentity.render.PipeRenderer")
public abstract class PipeRendererMixin {
	@Inject(method = "render(Lde/maxhenkel/pipez/blocks/tileentity/PipeTileEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
		at = @At("HEAD"), cancellable = true, require = 0)
	private void comedymonkeysaddons$hideUnderCover(@Coerce BlockEntity pipe, float partialTicks, PoseStack ms,
													MultiBufferSource buffer, int light, int overlay, CallbackInfo ci) {
		if (pipe.hasData(ModAttachments.PIPE_COVER) && pipe.getData(ModAttachments.PIPE_COVER).isOpaque())
			ci.cancel();
	}
}
