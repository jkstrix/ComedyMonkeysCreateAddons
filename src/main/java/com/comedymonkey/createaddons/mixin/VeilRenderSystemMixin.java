package com.comedymonkey.createaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;

/**
 * Veil clears the screen's depth as plain depth when it can clear textures directly, which fails on a screen with a
 * stencil (Ponder, in every Create pack, gives it one): a GL error every frame while its lights are on (the Electric
 * Lights' coloured light). With a stencil it's told it can't, and clears through the framebuffer, which works for both.
 * Only while the Electric Lights' coloured light runs on Veil (LightGlow installed): Veil's other users are left alone.
 * Does nothing (and isn't applied) without Veil.
 */
@Pseudo
@Mixin(targets = "foundry.veil.api.client.render.VeilRenderSystem", remap = false)
public abstract class VeilRenderSystemMixin {
	@Inject(method = "clearTextureSupported", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
	private static void comedymonkeysaddons$noTextureClearWithStencil(CallbackInfoReturnable<Boolean> cir) {
		if (!com.comedymonkey.createaddons.compat.veil.LightGlow.installed)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc != null && mc.getMainRenderTarget() != null && mc.getMainRenderTarget().isStencilEnabled())
			cir.setReturnValue(false);
	}
}
