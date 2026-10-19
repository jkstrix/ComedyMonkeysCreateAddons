package com.comedymonkey.createaddons.registry;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.crucible.CrucibleRecipe;
import com.comedymonkey.createaddons.content.machine.MachineKind;
import com.comedymonkey.createaddons.content.machine.TwoInputRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
	public static final DeferredRegister<RecipeType<?>> TYPES =
		DeferredRegister.create(Registries.RECIPE_TYPE, ComedyMonkeysCreateAddons.MOD_ID);
	public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
		DeferredRegister.create(Registries.RECIPE_SERIALIZER, ComedyMonkeysCreateAddons.MOD_ID);

	public static final DeferredHolder<RecipeType<?>, RecipeType<CrucibleRecipe>> CRUCIBLE = TYPES.register(
		"pressure_crucible", () -> RecipeType.simple(ComedyMonkeysCreateAddons.asResource("pressure_crucible")));

	public static final DeferredHolder<RecipeSerializer<?>, CrucibleRecipe.Serializer> CRUCIBLE_SERIALIZER =
		SERIALIZERS.register("pressure_crucible", CrucibleRecipe.Serializer::new);

	public static final DeferredHolder<RecipeType<?>, RecipeType<TwoInputRecipe>> TANGLING = TYPES.register(
		"tangling", () -> RecipeType.simple(ComedyMonkeysCreateAddons.asResource("tangling")));
	public static final DeferredHolder<RecipeSerializer<?>, TwoInputRecipe.Serializer> TANGLING_SERIALIZER =
		SERIALIZERS.register("tangling", () -> new TwoInputRecipe.Serializer(MachineKind.TANGLING));

	public static final DeferredHolder<RecipeType<?>, RecipeType<TwoInputRecipe>> INFUSING = TYPES.register(
		"infusing", () -> RecipeType.simple(ComedyMonkeysCreateAddons.asResource("infusing")));
	public static final DeferredHolder<RecipeSerializer<?>, TwoInputRecipe.Serializer> INFUSING_SERIALIZER =
		SERIALIZERS.register("infusing", () -> new TwoInputRecipe.Serializer(MachineKind.INFUSING));

	private ModRecipes() {}
}
