package com.comedymonkey.createaddons.content.machine;

import com.comedymonkey.createaddons.registry.ModRecipes;

import net.minecraft.world.item.crafting.RecipeType;

/** The two-input processing machines: which recipes they run and what their goggles call them. */
public enum MachineKind {
	/** String Tangler: string and slime into cobwebs. */
	TANGLING("tangling"),
	/** Stone Infuser: cobblestone and crushed ore into Create's ore-bearing stones. */
	INFUSING("infusing");

	private final String id;

	MachineKind(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public RecipeType<TwoInputRecipe> recipeType() {
		return this == TANGLING ? ModRecipes.TANGLING.get() : ModRecipes.INFUSING.get();
	}
}
