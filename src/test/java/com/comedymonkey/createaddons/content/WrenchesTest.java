package com.comedymonkey.createaddons.content;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;

import com.simibubi.create.content.equipment.wrench.WrenchItem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

class WrenchesTest {
	@Test
	void ordinaryItemsAreNotWrenches() {
		assertFalse(Wrenches.isWrench(ItemStack.EMPTY));
		assertFalse(Wrenches.isWrench(new ItemStack(Items.STICK)));
		assertFalse(Wrenches.isWrench(new ItemStack(Items.IRON_PICKAXE)));
	}

	@Test
	void createsWrenchIsAWrench() {
		Item wrench = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "wrench"));
		assumeTrue(wrench instanceof WrenchItem, "Create's wrench isn't registered in this test run");
		assertTrue(Wrenches.isWrench(new ItemStack(wrench)));
	}
}
