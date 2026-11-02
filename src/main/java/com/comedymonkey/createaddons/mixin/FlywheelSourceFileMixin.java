package com.comedymonkey.createaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.resources.ResourceLocation;

/**
 * Flywheel's shader files pass through {@code SourceFile.parse} as they load: its common fragment shader is given
 * Veil's albedo and normal outputs there, so instanced parts take the Electric Lights' coloured light properly (see
 * {@link com.comedymonkey.createaddons.compat.veil.VeilFlywheelOutputs}). Flywheel comes inside Create; if its loader
 * ever changes, this simply doesn't apply.
 */
@Pseudo
@Mixin(targets = "dev.engine_room.flywheel.backend.glsl.SourceFile", remap = false)
public abstract class FlywheelSourceFileMixin {
	@ModifyVariable(method = "parse", at = @At("HEAD"), argsOnly = true, require = 0, remap = false)
	private static String comedymonkeysaddons$veilOutputs(String source, @Local(argsOnly = true) ResourceLocation name) {
		return com.comedymonkey.createaddons.compat.veil.VeilFlywheelOutputs.onLoad(name.toString(), source);
	}
}
