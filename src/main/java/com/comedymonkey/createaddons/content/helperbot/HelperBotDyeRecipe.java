package com.comedymonkey.createaddons.content.helperbot;

import com.comedymonkey.createaddons.registry.ModRecipes;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

/** Helper Bot and one dye, shapeless (D16), written as vanilla's; the bot's anvil name carries over to the dyed bot. */
public class HelperBotDyeRecipe extends ShapelessRecipe {
	public HelperBotDyeRecipe(ShapelessRecipe recipe) {
		super(recipe.getGroup(), recipe.category(), recipe.getResultItem(RegistryAccess.EMPTY), recipe.getIngredients());
	}

	@Override
	public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
		ItemStack out = super.assemble(input, registries);
		for (ItemStack in : input.items()) {
			Component name = in.get(DataComponents.CUSTOM_NAME);
			if (name != null && in.is(out.getItem()))
				out.set(DataComponents.CUSTOM_NAME, name);
		}
		return out;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return ModRecipes.HELPER_BOT_DYE_SERIALIZER.get();
	}

	/** Vanilla's shapeless format. */
	public static class Serializer implements RecipeSerializer<HelperBotDyeRecipe> {
		@SuppressWarnings("unchecked")
		private static final RecipeSerializer<ShapelessRecipe> SHAPELESS = (RecipeSerializer<ShapelessRecipe>) RecipeSerializer.SHAPELESS_RECIPE;
		private static final MapCodec<HelperBotDyeRecipe> CODEC = SHAPELESS.codec().xmap(HelperBotDyeRecipe::new, r -> r);
		private static final StreamCodec<RegistryFriendlyByteBuf, HelperBotDyeRecipe> STREAM_CODEC =
			SHAPELESS.streamCodec().map(HelperBotDyeRecipe::new, r -> r);

		@Override
		public MapCodec<HelperBotDyeRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, HelperBotDyeRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
