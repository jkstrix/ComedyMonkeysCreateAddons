package com.comedymonkey.createaddons.content.machine;

import com.comedymonkey.createaddons.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * A recipe for the String Tangler or Stone Infuser: {@code count} of the main ingredient and {@code extra_count} of
 * the extra one make the result. {@code processing_time} is in ticks at 16 RPM; faster shafts divide it.
 *
 * <pre>{
 *   "type": "comedymonkeysaddons:tangling",
 *   "ingredient": {"item": "minecraft:string"}, "count": 4,
 *   "extra": {"tag": "c:slime_balls"}, "extra_count": 1,
 *   "processing_time": 160,
 *   "result": {"id": "minecraft:cobweb"}
 * }</pre>
 */
public record TwoInputRecipe(MachineKind kind, Ingredient ingredient, int count, Ingredient extra, int extraCount,
							 int processingTime, ItemStack result) implements Recipe<RecipeInput> {

	@Override
	public boolean matches(RecipeInput input, Level level) {
		return input.size() >= 2 && ingredient.test(input.getItem(0)) && input.getItem(0).getCount() >= count
			&& extra.test(input.getItem(1)) && input.getItem(1).getCount() >= extraCount;
	}

	@Override
	public ItemStack assemble(RecipeInput input, HolderLookup.Provider registries) {
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
		return NonNullList.of(Ingredient.EMPTY, ingredient, extra);
	}

	/** Keeps it out of the vanilla recipe book. */
	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return kind == MachineKind.TANGLING ? ModRecipes.TANGLING_SERIALIZER.get() : ModRecipes.INFUSING_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return kind.recipeType();
	}

	/** One serializer per machine; the JSON is the same shape for both. */
	public static class Serializer implements RecipeSerializer<TwoInputRecipe> {
		private final MapCodec<TwoInputRecipe> codec;
		private final StreamCodec<RegistryFriendlyByteBuf, TwoInputRecipe> streamCodec;

		public Serializer(MachineKind kind) {
			codec = RecordCodecBuilder.mapCodec(i -> i.group(
				Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(TwoInputRecipe::ingredient),
				ExtraCodecs.intRange(1, 64).optionalFieldOf("count", 1).forGetter(TwoInputRecipe::count),
				Ingredient.CODEC_NONEMPTY.fieldOf("extra").forGetter(TwoInputRecipe::extra),
				ExtraCodecs.intRange(1, 64).optionalFieldOf("extra_count", 1).forGetter(TwoInputRecipe::extraCount),
				ExtraCodecs.POSITIVE_INT.optionalFieldOf("processing_time", 200).forGetter(TwoInputRecipe::processingTime),
				ItemStack.STRICT_CODEC.fieldOf("result").forGetter(TwoInputRecipe::result)
			).apply(i, (a, ac, b, bc, t, r) -> new TwoInputRecipe(kind, a, ac, b, bc, t, r)));
			streamCodec = StreamCodec.composite(
				Ingredient.CONTENTS_STREAM_CODEC, TwoInputRecipe::ingredient,
				ByteBufCodecs.VAR_INT, TwoInputRecipe::count,
				Ingredient.CONTENTS_STREAM_CODEC, TwoInputRecipe::extra,
				ByteBufCodecs.VAR_INT, TwoInputRecipe::extraCount,
				ByteBufCodecs.VAR_INT, TwoInputRecipe::processingTime,
				ItemStack.STREAM_CODEC, TwoInputRecipe::result,
				(a, ac, b, bc, t, r) -> new TwoInputRecipe(kind, a, ac, b, bc, t, r));
		}

		@Override
		public MapCodec<TwoInputRecipe> codec() {
			return codec;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, TwoInputRecipe> streamCodec() {
			return streamCodec;
		}
	}
}
