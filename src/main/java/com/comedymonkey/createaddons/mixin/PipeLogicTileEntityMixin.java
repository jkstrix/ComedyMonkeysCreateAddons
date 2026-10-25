package com.comedymonkey.createaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import com.comedymonkey.createaddons.compat.pipez.PipezBlazeBurnerCompat;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

/**
 * Marks a Pipez pipe's tick, so a Blaze Burner set to take fuel from Pipez pipes only can tell a pipe is asking without
 * walking the call stack on every transfer. The mark comes off however the tick ends, a crash included (a server that
 * removes erroring block entities carries on). Does nothing (and isn't applied) without Pipez.
 */
@Pseudo
@Mixin(targets = "de.maxhenkel.pipez.blocks.tileentity.PipeLogicTileEntity")
public abstract class PipeLogicTileEntityMixin {
	@WrapMethod(method = "tick", require = 0)
	private void comedymonkeysaddons$markTick(Operation<Void> original) {
		PipezBlazeBurnerCompat.enterPipeTick();
		try {
			original.call();
		} finally {
			PipezBlazeBurnerCompat.exitPipeTick();
		}
	}
}
