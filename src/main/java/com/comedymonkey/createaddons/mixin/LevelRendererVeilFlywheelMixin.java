package com.comedymonkey.createaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.LevelRenderer;

/**
 * Flywheel draws its instances just before the level renderer turns to block entities (its own hook sits before the
 * "blockentities" profiler section starts). Veil's albedo and normal buffers, bound for that draw by the Electric Lights'
 * coloured light, are unbound straight after it (see
 * {@link com.comedymonkey.createaddons.compat.veil.VeilFlywheelOutputs}). Does nothing when they weren't bound.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererVeilFlywheelMixin {
	@Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
		args = "ldc=blockentities", shift = At.Shift.AFTER), require = 0)
	private void comedymonkeysaddons$afterFlywheel(CallbackInfo ci) {
		com.comedymonkey.createaddons.compat.veil.VeilFlywheelOutputs.afterFlywheel();
	}
}
