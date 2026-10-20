package com.comedymonkey.createaddons.content.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.registry.ModRecipes;
import com.comedymonkey.createaddons.testutil.ModResourceFiles;
import com.google.gson.JsonObject;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/** The two machine kinds: their ids, recipe types and serializers. */
class MachineKindTest {
	@Test
	void ids() {
		assertEquals("tangling", MachineKind.TANGLING.id());
		assertEquals("infusing", MachineKind.INFUSING.id());
	}

	@Test
	void recipeTypes() {
		assertSame(ModRecipes.TANGLING.get(), MachineKind.TANGLING.recipeType());
		assertSame(ModRecipes.INFUSING.get(), MachineKind.INFUSING.recipeType());
		assertNotSame(MachineKind.TANGLING.recipeType(), MachineKind.INFUSING.recipeType());
	}

	@Test
	void recipeTypesAndSerializersAreRegisteredUnderTheId() {
		for (MachineKind kind : MachineKind.values()) {
			var type = BuiltInRegistries.RECIPE_TYPE.getKey(kind.recipeType());
			assertEquals("comedymonkeysaddons:" + kind.id(), String.valueOf(type));
			TwoInputRecipe recipe = new TwoInputRecipe(kind, Ingredient.of(Items.STRING), 1, Ingredient.of(Items.SLIME_BALL), 1,
				20, new ItemStack(Items.COBWEB));
			assertEquals("comedymonkeysaddons:" + kind.id(),
				String.valueOf(BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer())));
			assertSame(kind.recipeType(), recipe.getType());
		}
	}

	@Test
	void gogglesHeadersAreTranslated() {
		Path lang = ModResourceFiles.find("assets", "comedymonkeysaddons", "lang", "en_us.json");
		assumeTrue(lang != null, "en_us.json not found");
		JsonObject json = ModResourceFiles.readJson(lang).getAsJsonObject();
		for (MachineKind kind : MachineKind.values())
			assertTrue(json.has("comedymonkeysaddons.gui.goggles." + kind.id()), kind.id());
		for (String status : new String[] {"empty", "making", "no_rotation", "output_full", "needs_more", "needs_other", "output"})
			assertTrue(json.has("comedymonkeysaddons.gui.goggles.machine." + status), status);
	}
}
