package com.comedymonkey.createaddons.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.testutil.ModRecipeFiles;
import com.google.gson.JsonObject;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

/** The machine recipe types and serializers, and the crafting recipes for the machines themselves. */
class ModRecipesTest {
	/** Crafting recipes (vanilla serializers) whose results are the blocks of the machines. */
	private static final String[] MACHINE_CRAFTING = {
		"wellspring.json", "pressure_crucible.json", "incinerator.json", "string_tangler.json", "stone_infuser.json",
		"andesite_cobble_generator.json", "andesite_basalt_generator.json", "andesite_blackstone_generator.json",
		"tuff_generator.json", "wireless_andesite_cobble_generator.json", "wireless_andesite_basalt_generator.json",
		"wireless_andesite_blackstone_generator.json", "wireless_brass_cobble_generator.json",
		"wireless_brass_basalt_generator.json", "wireless_brass_blackstone_generator.json", "wireless_tuff_generator.json",
	};

	@Test
	void typesAndSerializersAreRegisteredUnderTheirIds() {
		assertEquals("comedymonkeysaddons:pressure_crucible", String.valueOf(BuiltInRegistries.RECIPE_TYPE.getKey(ModRecipes.CRUCIBLE.get())));
		assertEquals("comedymonkeysaddons:tangling", String.valueOf(BuiltInRegistries.RECIPE_TYPE.getKey(ModRecipes.TANGLING.get())));
		assertEquals("comedymonkeysaddons:infusing", String.valueOf(BuiltInRegistries.RECIPE_TYPE.getKey(ModRecipes.INFUSING.get())));
		assertEquals("comedymonkeysaddons:pressure_crucible",
			String.valueOf(BuiltInRegistries.RECIPE_SERIALIZER.getKey(ModRecipes.CRUCIBLE_SERIALIZER.get())));
		assertEquals("comedymonkeysaddons:tangling",
			String.valueOf(BuiltInRegistries.RECIPE_SERIALIZER.getKey(ModRecipes.TANGLING_SERIALIZER.get())));
		assertEquals("comedymonkeysaddons:infusing",
			String.valueOf(BuiltInRegistries.RECIPE_SERIALIZER.getKey(ModRecipes.INFUSING_SERIALIZER.get())));
	}

	@Test
	void typeNamesMatchTheirIds() {
		// RecipeType.simple names the type by its id; the JEI categories and the recipe folders use the same ids
		assertEquals("comedymonkeysaddons:pressure_crucible", ModRecipes.CRUCIBLE.get().toString());
		assertEquals("comedymonkeysaddons:tangling", ModRecipes.TANGLING.get().toString());
		assertEquals("comedymonkeysaddons:infusing", ModRecipes.INFUSING.get().toString());
	}

	@Test
	void theTwoMachinesHaveSeparateSerializers() {
		assertNotSame(ModRecipes.TANGLING_SERIALIZER.get(), ModRecipes.INFUSING_SERIALIZER.get());
		assertNotSame(ModRecipes.TANGLING_SERIALIZER.get().codec(), ModRecipes.INFUSING_SERIALIZER.get().codec());
	}

	@Test
	void machineCraftingRecipesLoadAndMakeTheMachine() {
		for (String file : MACHINE_CRAFTING) {
			JsonObject json = ModRecipeFiles.file(file);
			var result = Recipe.CODEC.parse(ModRecipeFiles.jsonOps(), json);
			assertTrue(result.isSuccess(), file + ": " + result.error().map(Object::toString).orElse(""));
			ItemStack made = result.getOrThrow().getResultItem(ModRecipeFiles.REGISTRIES);
			ResourceLocation expected = ResourceLocation.fromNamespaceAndPath("comedymonkeysaddons", file.replace(".json", ""));
			assertEquals(expected, BuiltInRegistries.ITEM.getKey(made.getItem()), file);
		}
	}
}
