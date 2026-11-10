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
			@SuppressWarnings({"removal", "deprecation"}) // kept for JEI before 19.9, which still asks for it
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

	/** Create's Bulk Washing page. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static final mezz.jei.api.recipe.RecipeType<net.minecraft.world.item.crafting.RecipeHolder<com.simibubi.create.content.kinetics.fan.processing.SplashingRecipe>> FAN_WASHING =
		new mezz.jei.api.recipe.RecipeType(ResourceLocation.fromNamespaceAndPath("create", "fan_washing"), net.minecraft.world.item.crafting.RecipeHolder.class);

	/**
	 * Washing ages copper a stage (see CopperWashing): done in code, for any mod's copper, so its stages are shown on
	 * Create's Bulk Washing page as recipes made here.
	 */
	private static void copperWashing(IRecipeRegistration registration) {
		java.util.List<net.minecraft.world.item.crafting.RecipeHolder<com.simibubi.create.content.kinetics.fan.processing.SplashingRecipe>> recipes = new java.util.ArrayList<>();
		for (var stage : com.comedymonkey.createaddons.content.CopperWashing.stages()) {
			ResourceLocation from = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stage.getKey());
			ResourceLocation id = ComedyMonkeysCreateAddons.asResource("washing/copper/" + from.getNamespace() + "/" + from.getPath());
			recipes.add(new net.minecraft.world.item.crafting.RecipeHolder<>(id,
				new com.simibubi.create.content.processing.recipe.StandardProcessingRecipe.Builder<>(
					com.simibubi.create.content.kinetics.fan.processing.SplashingRecipe::new, id)
					.require(stage.getKey())
					.output(stage.getValue())
					.build()));
		}
		registration.addRecipes(FAN_WASHING, recipes);
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		lineInfo(registration);
		copperWashing(registration);
		info(registration, "pointed_dripstone_generator", ModItems.POINTED_DRIPSTONE_GENERATOR.get(), ModItems.WIRELESS_POINTED_DRIPSTONE_GENERATOR.get());
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
		info(registration, "floodlight", ModItems.LIGHTS.get(com.comedymonkey.createaddons.content.light.LightKind.FLOODLIGHT).get());
		info(registration, "warning_beacon", ModItems.LIGHTS.get(com.comedymonkey.createaddons.content.light.LightKind.WARNING_BEACON).get());
		info(registration, "festoon_lights", ModItems.FESTOON_LIGHTS.get());
		// the Clockwork Jetpack: its keys and flight aren't in any recipe
		info(registration, "clockwork_jetpack", ModItems.CLOCKWORK_JETPACK.get(), ModItems.NETHERITE_CLOCKWORK_JETPACK.get());
		info(registration, "jetpack_winding_station", ModItems.JETPACK_WINDING_STATION.get());
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
