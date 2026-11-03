package com.comedymonkey.createaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.comedymonkey.createaddons.content.jetpack.JetpackFlight;

import net.minecraft.world.entity.LivingEntity;

/**
 * Vanilla only keeps a player gliding while their chest item can fly as an Elytra. The Clockwork Jetpack's wings are
 * the Elytra (jetpack spec D6, D14), worn in the Jetpack slot or the chest: while they're out, the glide is left as it is.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityFallFlyingMixin {
	@Inject(method = "updateFallFlying", at = @At("HEAD"), cancellable = true)
	private void comedymonkeysaddons$keepJetpackWings(CallbackInfo ci) {
		if (JetpackFlight.keepsWings((LivingEntity) (Object) this))
			ci.cancel();
	}
}
