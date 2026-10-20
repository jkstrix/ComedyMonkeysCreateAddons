package com.comedymonkey.createaddons.content.chest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.registry.ModItems;

import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;

class UpgradedChestItemTest {
	@Test
	void tierRoundTripsThroughTheBlockStateComponent() {
		for (ChestTier tier : ChestTier.values()) {
			ItemStack stack = UpgradedChestItem.of(tier);
			assertTrue(stack.is(ModItems.UPGRADED_CHEST.get()));
			assertEquals(tier, UpgradedChestItem.tierOf(stack));
		}
	}

	@Test
	void aPlainItemIsCopper() {
		assertEquals(ChestTier.COPPER, UpgradedChestItem.tierOf(new ItemStack(ModItems.UPGRADED_CHEST.get())));
	}

	@Test
	void namedAfterItsTier() {
		ItemStack stack = UpgradedChestItem.of(ChestTier.DIAMOND);
		TranslatableContents name = assertInstanceOf(TranslatableContents.class,
			ModItems.UPGRADED_CHEST.get().getName(stack).getContents());
		assertEquals("comedymonkeysaddons.chest.diamond", name.getKey());
	}
}
