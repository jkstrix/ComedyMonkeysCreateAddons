package com.comedymonkey.createaddons.compat.jei;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.machine.MachineKind;
import com.comedymonkey.createaddons.registry.ModItems;
import com.comedymonkey.createaddons.registry.ModRecipes;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/** JEI integration: a category for Pressure Crucible recipes. Only loaded by JEI, so JEI stays optional. */
@JeiPlugin
public class CMJeiPlugin implements IModPlugin {
	@Override
	public ResourceLocation getPluginUid() {
		return ComedyMonkeysCreateAddons.asResource("jei_plugin");
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		registration.addRecipeCategories(new CrucibleCategory(registration.getJeiHelpers().getGuiHelper()));
		registration.addRecipeCategories(new TwoInputCategory(registration.getJeiHelpers().getGuiHelper(), MachineKind.TANGLING));
		registration.addRecipeCategories(new TwoInputCategory(registration.getJeiHelpers().getGuiHelper(), MachineKind.INFUSING));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		if (Minecraft.getInstance().level == null)
			return;
		registration.addRecipes(CrucibleCategory.TYPE,
			Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor(ModRecipes.CRUCIBLE.get()));
		for (MachineKind kind : MachineKind.values())
			registration.addRecipes(TwoInputCategory.type(kind),
				Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor(kind.recipeType()));
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(ModItems.PRESSURE_CRUCIBLE.get(), CrucibleCategory.TYPE);
		registration.addRecipeCatalyst(ModItems.STRING_TANGLER.get(), TwoInputCategory.type(MachineKind.TANGLING));
		registration.addRecipeCatalyst(ModItems.STONE_INFUSER.get(), TwoInputCategory.type(MachineKind.INFUSING));
	}
}
