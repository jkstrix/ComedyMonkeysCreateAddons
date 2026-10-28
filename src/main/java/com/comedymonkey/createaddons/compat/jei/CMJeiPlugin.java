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

	/** Each dye of Helper Bot is its own item in JEI (one per colour, not 16 duplicates). */
	@Override
	public void registerItemSubtypes(mezz.jei.api.registration.ISubtypeRegistration registration) {
		registration.registerSubtypeInterpreter(ModItems.HELPER_BOT.get(), new mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter<>() {
			@Override
			public Object getSubtypeData(net.minecraft.world.item.ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
				return stack.get(com.comedymonkey.createaddons.registry.ModDataComponents.HELPER_BOT_DYE.get());
			}

			@Override
			@SuppressWarnings("removal")
			public String getLegacyStringSubtypeInfo(net.minecraft.world.item.ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
				Object dye = getSubtypeData(stack, context);
				return dye == null ? "" : dye.toString();
			}
		});
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		registration.addRecipeCategories(new CrucibleCategory(registration.getJeiHelpers().getGuiHelper()));
		registration.addRecipeCategories(new TwoInputCategory(registration.getJeiHelpers().getGuiHelper(), MachineKind.TANGLING));
		registration.addRecipeCategories(new TwoInputCategory(registration.getJeiHelpers().getGuiHelper(), MachineKind.INFUSING));
	}

	/** The retired Pipez Covers are kept for old worlds only, so JEI doesn't list them. */
	@Override
	public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime) {
		runtime.getIngredientManager().removeIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK,
			java.util.List.of(new net.minecraft.world.item.ItemStack(com.comedymonkey.createaddons.registry.ModItems.SNUG_PIPEZ_COVER.get()),
				new net.minecraft.world.item.ItemStack(com.comedymonkey.createaddons.registry.ModItems.FULL_PIPEZ_COVER.get())));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		lineInfo(registration);
		if (Minecraft.getInstance().level == null)
			return;
		registration.addRecipes(CrucibleCategory.TYPE,
			Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor(ModRecipes.CRUCIBLE.get()));
		for (MachineKind kind : MachineKind.values())
			registration.addRecipes(TwoInputCategory.type(kind),
				Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor(kind.recipeType()));
	}

	/** Info pages for the Fluid and Power Lines, the Line Post and the fittings. */
	private static void lineInfo(IRecipeRegistration registration) {
		info(registration, "fluid_line", ModItems.COPPER_FLUID_LINE.get(), ModItems.BRASS_FLUID_LINE.get());
		info(registration, "power_line", ModItems.COPPER_POWER_LINE.get(), ModItems.BRASS_POWER_LINE.get());
		info(registration, "line_post", ModItems.LINE_POST.get());
		info(registration, "intake", ModItems.INTAKE.get());
		info(registration, "valve", ModItems.VALVE.get());
		info(registration, "filter", ModItems.FILTER.get());
		info(registration, "meter", ModItems.METER.get());
		info(registration, "splitter", ModItems.SPLITTER.get());
		info(registration, "breaker", ModItems.BREAKER.get());
		info(registration, "light_switch", ModItems.LIGHT_SWITCH.get());
		info(registration, "light_strip", ModItems.LIGHT_STRIP.get());
		info(registration, "edge_light", ModItems.EDGE_LIGHT.get());
		info(registration, "lamp_post", ModItems.CAST_IRON_LAMP_POST.get(), ModItems.STEEL_LAMP_POST.get());
		info(registration, "hanging_lamp", ModItems.LIGHTS.get(com.comedymonkey.createaddons.content.light.LightKind.PENDANT).get(),
			ModItems.LIGHTS.get(com.comedymonkey.createaddons.content.light.LightKind.CHANDELIER).get());
		// the Electric Lights: dyed in place, so there are no colour recipes to show it (P20)
		info(registration, "light", ModItems.LIGHTS.values().stream().map(net.neoforged.neoforge.registries.DeferredItem::get)
			.toArray(net.minecraft.world.level.ItemLike[]::new));
	}

	private static void info(IRecipeRegistration registration, String key, net.minecraft.world.level.ItemLike... items) {
		for (var item : items)
			registration.addItemStackInfo(new net.minecraft.world.item.ItemStack(item),
				net.minecraft.network.chat.Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".gui.jei.info." + key));
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
		registration.addRecipeCatalyst(ModItems.PRESSURE_CRUCIBLE.get(), CrucibleCategory.TYPE);
		registration.addRecipeCatalyst(ModItems.STRING_TANGLER.get(), TwoInputCategory.type(MachineKind.TANGLING));
		registration.addRecipeCatalyst(ModItems.STONE_INFUSER.get(), TwoInputCategory.type(MachineKind.INFUSING));
	}
}
