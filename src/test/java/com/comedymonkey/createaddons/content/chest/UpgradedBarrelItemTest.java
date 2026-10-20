package com.comedymonkey.createaddons.content.chest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.registry.ModItems;

import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;

class UpgradedBarrelItemTest {
	@Test
	void tierRoundTripsThroughTheBlockStateComponent() {
		for (ChestTier tier : ChestTier.values()) {
			ItemStack stack = UpgradedBarrelItem.of(tier);
			assertTrue(stack.is(ModItems.UPGRADED_BARREL.get()));
			assertEquals(tier, UpgradedBarrelItem.tierOf(stack));
		}
	}

	@Test
	void aPlainItemIsCopper() {
		assertEquals(ChestTier.COPPER, UpgradedBarrelItem.tierOf(new ItemStack(ModItems.UPGRADED_BARREL.get())));
	}

	@Test
	void namedAfterItsTier() {
		TranslatableContents name = assertInstanceOf(TranslatableContents.class,
			ModItems.UPGRADED_BARREL.get().getName(UpgradedBarrelItem.of(ChestTier.NETHERITE)).getContents());
		assertEquals("comedymonkeysaddons.barrel.netherite", name.getKey());
	}
}
