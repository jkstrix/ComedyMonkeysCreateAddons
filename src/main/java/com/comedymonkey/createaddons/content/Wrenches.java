package com.comedymonkey.createaddons.content;

import com.simibubi.create.content.equipment.wrench.WrenchItem;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Wrench detection: Create's wrench, or anything tagged as a wrench by other mods. */
public final class Wrenches {
	private static final TagKey<Item> WRENCH_TAG = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "tools/wrench"));

	private Wrenches() {}

	public static boolean isWrench(ItemStack stack) {
		return stack.getItem() instanceof WrenchItem || stack.is(WRENCH_TAG);
	}
}
