package com.comedymonkey.createaddons.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.comedymonkey.createaddons.content.CopperWashing;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Bulk washing ages copper a stage a pass where no washing recipe takes it (see {@link CopperWashing}). */
@Mixin(value = AllFanProcessingTypes.SplashingType.class, remap = false)
public abstract class SplashingTypeMixin {
	@Inject(method = "canProcess(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;)Z", at = @At("RETURN"), cancellable = true)
	private void comedymonkeysaddons$canWashCopper(ItemStack stack, Level level, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && CopperWashing.washed(stack) != null)
			cir.setReturnValue(true);
	}

	@Inject(method = "process(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;)Ljava/util/List;", at = @At("RETURN"), cancellable = true)
	private void comedymonkeysaddons$washCopper(ItemStack stack, Level level, CallbackInfoReturnable<List<ItemStack>> cir) {
		List<ItemStack> out = cir.getReturnValue();
		if (out != null && !out.isEmpty())
			return;
		ItemStack washed = CopperWashing.washed(stack);
		if (washed != null) {
			List<ItemStack> result = new ArrayList<>();
			result.add(washed);
			cir.setReturnValue(result);
		}
	}
}
