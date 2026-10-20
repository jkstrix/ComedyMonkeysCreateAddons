package com.comedymonkey.createaddons.compat.curios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

class AviatorGogglesCuriosTest {
	@Test
	void gogglesIdIsAeronautics() {
		assertEquals(ResourceLocation.fromNamespaceAndPath("aeronautics", "aviators_goggles"), AviatorGogglesCurios.GOGGLES_ID);
		assertEquals(AviatorGogglesCurios.GOGGLES_ID, AviatorGogglesCurios.AVIATORS_GOGGLES.getId());
	}

	@Test
	void otherItemsAreNotAviatorsGoggles() {
		assertFalse(AviatorGogglesCurios.isAviatorsGoggles(ItemStack.EMPTY));
		assertFalse(AviatorGogglesCurios.isAviatorsGoggles(new ItemStack(Items.LEATHER_HELMET)));
	}

	@Test
	void recognisesAeronauticsGoggles() {
		assumeTrue(BuiltInRegistries.ITEM.containsKey(AviatorGogglesCurios.GOGGLES_ID), "Create Aeronautics isn't loaded");
		assertTrue(AviatorGogglesCurios.AVIATORS_GOGGLES.isBound());
		assertTrue(AviatorGogglesCurios.isAviatorsGoggles(new ItemStack(BuiltInRegistries.ITEM.get(AviatorGogglesCurios.GOGGLES_ID))));
	}
}
