package com.comedymonkey.createaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.comedymonkey.createaddons.content.electric.BatteryBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A Lightning Rod standing on a Battery or an Industrial Battery charges it when lightning strikes the rod. */
@Mixin(LightningRodBlock.class)
public abstract class LightningRodBlockMixin {
	@Inject(method = "onLightningStrike", at = @At("TAIL"), require = 1)
	private void comedymonkeysaddons$chargeBattery(BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
		if (!level.isClientSide && state.getValue(LightningRodBlock.FACING) == Direction.UP
			&& level.getBlockEntity(pos.below()) instanceof BatteryBlockEntity battery)
			battery.struck(pos.below());
	}
}
