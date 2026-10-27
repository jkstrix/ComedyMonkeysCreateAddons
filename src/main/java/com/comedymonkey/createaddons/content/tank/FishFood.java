package com.comedymonkey.createaddons.content.tank;

import com.comedymonkey.createaddons.registry.ModItems;

import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Fish Food in the world (D65): fish swim after a player holding it. (Axolotls breed on it through the
 * {@code minecraft:axolotl_food} tag, as on a Bucket of Tropical Fish.)
 */
public final class FishFood {
	private FishFood() {}

	public static void onEntityJoin(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractFish fish))
			return;
		fish.goalSelector.addGoal(2, new TemptGoal(fish, 1.25, Ingredient.of(ModItems.FISH_FOOD.get()), false));
	}
}
