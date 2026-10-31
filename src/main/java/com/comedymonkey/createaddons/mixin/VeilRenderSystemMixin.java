package com.comedymonkey.createaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;

/**
 * Veil clears the screen's depth as plain depth when it can clear textures directly, which fails on a screen with a
 * stencil (Ponder, in every Create pack, gives it one): a GL error every frame while its lights are on (the Electric
 * Lights' coloured light). With a stencil it's told it can't, and clears through the framebuffer, which works for both.
 * Only while the Electric Lights' coloured light runs on Veil (LightGlow installed): Veil's other users are left alone.
 * Does nothing (and isn't applied) without Veil.
 * <p>
 * Veil also makes its light-shadow grid (a 64-cube texture) from memory the first time it needs it, without setting
 * the texture-upload ("unpack") state. Minecraft's own uploads leave that state set (row length, skipped pixels and
 * rows: every glyph of text drawn for the first time is uploaded that way), so Veil's upload was read with the wrong
 * strides and offsets and the driver ran off the end of its buffer: a hard crash (EXCEPTION_ACCESS_VIOLATION in
 * nvoglv64.dll, VoxelShadowGrid.ensureTexture). Seen when hovering a Brass Clockwork Inlet with coloured light on: its
 * filter box's text uploads new glyphs. Before Veil draws its lights, the unpack state goes back to OpenGL's defaults.
 */
@Pseudo
@Mixin(targets = "foundry.veil.api.client.render.VeilRenderSystem", remap = false)
public abstract class VeilRenderSystemMixin {
	private static final Logger COMEDYMONKEYSADDONS$LOGGER = LogUtils.getLogger();
	private static boolean comedymonkeysaddons$reported;

	@Inject(method = "drawLights", at = @At("HEAD"), require = 0, remap = false)
	private static void comedymonkeysaddons$defaultUnpackState(CallbackInfoReturnable<Boolean> cir) {
		int rowLength = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL12.GL_UNPACK_ROW_LENGTH);
		int skipPixels = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL12.GL_UNPACK_SKIP_PIXELS);
		int skipRows = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL12.GL_UNPACK_SKIP_ROWS);
		int skipImages = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL12.GL_UNPACK_SKIP_IMAGES);
		int imageHeight = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL12.GL_UNPACK_IMAGE_HEIGHT);
		int buffer = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
		if ((rowLength | skipPixels | skipRows | skipImages | imageHeight | buffer) == 0)
			return;
		if (!comedymonkeysaddons$reported) {
			comedymonkeysaddons$reported = true;
			COMEDYMONKEYSADDONS$LOGGER.info("Veil's lights: put the texture-upload state back to OpenGL's defaults (it was row length {}, skip"
				+ " pixels {}, rows {}, images {}, image height {}, unpack buffer {}); logged once", rowLength, skipPixels, skipRows,
				skipImages, imageHeight, buffer);
		}
		org.lwjgl.opengl.GL11.glPixelStorei(org.lwjgl.opengl.GL12.GL_UNPACK_ROW_LENGTH, 0);
		org.lwjgl.opengl.GL11.glPixelStorei(org.lwjgl.opengl.GL12.GL_UNPACK_SKIP_PIXELS, 0);
		org.lwjgl.opengl.GL11.glPixelStorei(org.lwjgl.opengl.GL12.GL_UNPACK_SKIP_ROWS, 0);
		org.lwjgl.opengl.GL11.glPixelStorei(org.lwjgl.opengl.GL12.GL_UNPACK_SKIP_IMAGES, 0);
		org.lwjgl.opengl.GL11.glPixelStorei(org.lwjgl.opengl.GL12.GL_UNPACK_IMAGE_HEIGHT, 0);
		if (buffer != 0)
			org.lwjgl.opengl.GL15.glBindBuffer(org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER, 0);
	}
	@Inject(method = "clearTextureSupported", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
	private static void comedymonkeysaddons$noTextureClearWithStencil(CallbackInfoReturnable<Boolean> cir) {
		if (!com.comedymonkey.createaddons.compat.veil.LightGlow.installed)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc != null && mc.getMainRenderTarget() != null && mc.getMainRenderTarget().isStencilEnabled())
			cir.setReturnValue(false);
	}
}
