package com.comedymonkey.createaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.comedymonkey.createaddons.content.wastes.CityLocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A locked Clockwork Wastes block (a dormant district's vault, door, Substation or lift) has bedrock's hardness, so
 * pistons, Create's drills and saws and its contraptions all pass it by; woken, it has its own hardness and is an
 * ordinary block (design/specs/clockwork-wastes.md, D42, P21, P43). Block hardness is one number per block in vanilla,
 * so the locked state answers here.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
	@Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true, require = 1)
	private void comedymonkeysaddons$lockedCityBlock(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
		BlockState state = (BlockState) (Object) this;
		if (state.getBlock() instanceof CityLocks.Lockable lockable && lockable.locked(state))
			cir.setReturnValue(-1f);
	}
}
