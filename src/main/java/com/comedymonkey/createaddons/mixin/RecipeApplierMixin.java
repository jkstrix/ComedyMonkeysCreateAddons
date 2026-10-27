package com.comedymonkey.createaddons.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.foundation.recipe.RecipeApplier;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

/**
 * Create's fan processing makes fresh results, so washing the dye off a named Helper Bot (or a Fluid or Power Line)
 * forgot his anvil name. A result that's the same item as what went in keeps it.
 */
@Mixin(value = RecipeApplier.class, remap = false)
public abstract class RecipeApplierMixin {
	@Inject(method = "applyRecipeOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/crafting/Recipe;Z)Ljava/util/List;",
		at = @At("RETURN"))
	private static void comedymonkeysaddons$keepName(Level level, ItemStack stackIn, Recipe<?> recipe, boolean remainder,
													 CallbackInfoReturnable<List<ItemStack>> cir) {
		Component name = stackIn.get(DataComponents.CUSTOM_NAME);
		if (name == null)
			return;
		for (ItemStack out : cir.getReturnValue())
			if (out.is(stackIn.getItem()) && !out.has(DataComponents.CUSTOM_NAME))
				out.set(DataComponents.CUSTOM_NAME, name);
	}
}
