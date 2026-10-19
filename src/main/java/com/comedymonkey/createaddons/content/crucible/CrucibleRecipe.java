package com.comedymonkey.createaddons.content.crucible;

import com.comedymonkey.createaddons.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.HeatCondition;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * A Pressure Crucible recipe: {@code count} of the ingredient are pressed into the result over a Blaze Burner of at
 * least {@code heat_requirement}. {@code processing_time} is in ticks at 16 RPM; faster shafts divide it, as with
 * Create's Millstone.
 *
 * <pre>{
 *   "type": "comedymonkeysaddons:pressure_crucible",
 *   "ingredient": {"item": "comedymonkeysaddons:emerald_dust"},
 *   "count": 9,
 *   "heat_requirement": "heated",
 *   "processing_time": 200,
 *   "result": {"id": "minecraft:emerald"}
 * }</pre>
 */
public record CrucibleRecipe(Ingredient ingredient, int count, HeatCondition heat, int processingTime, ItemStack result)
	implements Recipe<SingleRecipeInput> {

	public static final MapCodec<CrucibleRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
		Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CrucibleRecipe::ingredient),
		ExtraCodecs.intRange(1, 64).optionalFieldOf("count", 9).forGetter(CrucibleRecipe::count),
		HeatCondition.CODEC.optionalFieldOf("heat_requirement", HeatCondition.NONE).forGetter(CrucibleRecipe::heat),
		ExtraCodecs.POSITIVE_INT.optionalFieldOf("processing_time", 200).forGetter(CrucibleRecipe::processingTime),
		ItemStack.STRICT_CODEC.fieldOf("result").forGetter(CrucibleRecipe::result)
	).apply(i, CrucibleRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, CrucibleRecipe> STREAM_CODEC = StreamCodec.composite(
		Ingredient.CONTENTS_STREAM_CODEC, CrucibleRecipe::ingredient,
		ByteBufCodecs.VAR_INT, CrucibleRecipe::count,
		HeatCondition.STREAM_CODEC, CrucibleRecipe::heat,
		ByteBufCodecs.VAR_INT, CrucibleRecipe::processingTime,
		ItemStack.STREAM_CODEC, CrucibleRecipe::result,
		CrucibleRecipe::new);

	@Override
	public boolean matches(SingleRecipeInput input, Level level) {
		return ingredient.test(input.item()) && input.item().getCount() >= count;
	}

	@Override
	public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
		return result.copy();
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return true;
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registries) {
		return result;
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		return NonNullList.of(Ingredient.EMPTY, ingredient);
	}

	/** Keeps it out of the vanilla recipe book. */
	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return ModRecipes.CRUCIBLE_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return ModRecipes.CRUCIBLE.get();
	}

	public static class Serializer implements RecipeSerializer<CrucibleRecipe> {
		@Override
		public MapCodec<CrucibleRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, CrucibleRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
