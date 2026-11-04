package com.comedymonkey.createaddons.content.tradebot;

import java.util.List;
import java.util.Optional;

import com.comedymonkey.createaddons.registry.ModDataComponents;
import com.comedymonkey.createaddons.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe;
import com.simibubi.create.foundation.mixin.accessor.ShapedRecipeAccessor;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Upgrading a Trade Bot into the Trade Master (design/specs/trade-master.md D16, D27, P10): Create's mechanical crafting,
 * written in Create's own format, with the packed Trade Bot's data carried onto the result: his charge, his load and
 * earnings, his reputation with each villager, and his name. His container links stay behind: the Trade Master works
 * only through Trade Boards (D40). The recipe keeps Create's type, so mechanical crafters and JEI treat it as theirs.
 */
public class TradeMasterUpgradeRecipe extends MechanicalCraftingRecipe {
	public TradeMasterUpgradeRecipe(MechanicalCraftingRecipe recipe) {
		super(recipe.getGroup(), recipe.category(), ((ShapedRecipeAccessor) recipe).create$getPattern(), recipe.getResultItem(RegistryAccess.EMPTY),
			recipe.acceptsMirrored());
	}

	@Override
	public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
		ItemStack out = super.assemble(input, registries);
		for (ItemStack in : input.items()) {
			TradeBotData data = in.get(ModDataComponents.TRADE_BOT_DATA.get());
			if (data == null)
				continue;
			out.set(ModDataComponents.TRADE_BOT_DATA.get(), new TradeBotData(data.charge(), data.emeralds(), data.xpBank(), data.reputation(),
				data.load(), List.of(), Optional.empty(), data.tradedAgo()));
			Component name = in.get(DataComponents.CUSTOM_NAME);
			if (name != null)
				out.set(DataComponents.CUSTOM_NAME, name);
		}
		return out;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return ModRecipes.TRADE_MASTER_UPGRADE_SERIALIZER.get();
	}

	/** Create's mechanical-crafting format. */
	public static class Serializer implements RecipeSerializer<TradeMasterUpgradeRecipe> {
		private static final MapCodec<TradeMasterUpgradeRecipe> CODEC =
			MechanicalCraftingRecipe.Serializer.CODEC.xmap(TradeMasterUpgradeRecipe::new, r -> r);
		private static final StreamCodec<RegistryFriendlyByteBuf, TradeMasterUpgradeRecipe> STREAM_CODEC =
			MechanicalCraftingRecipe.Serializer.STREAM_CODEC.map(TradeMasterUpgradeRecipe::new, r -> r);

		@Override
		public MapCodec<TradeMasterUpgradeRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, TradeMasterUpgradeRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
