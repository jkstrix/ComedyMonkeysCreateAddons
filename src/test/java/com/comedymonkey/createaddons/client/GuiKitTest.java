package com.comedymonkey.createaddons.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

class GuiKitTest {
	@Test
	void countsUpTo999AreShownWhole() {
		assertEquals("1", GuiKit.shortCount(1));
		assertEquals("448", GuiKit.shortCount(448));
		assertEquals("999", GuiKit.shortCount(999));
	}

	/** Rounded down, so a slot never claims more than it holds. */
	@Test
	void thousandsAreShortenedAndRoundedDown() {
		assertEquals("1.0k", GuiKit.shortCount(1000));
		assertEquals("4.0k", GuiKit.shortCount(4096));
		assertEquals("6.3k", GuiKit.shortCount(6336));
		assertEquals("9.9k", GuiKit.shortCount(9999));
		assertEquals("10k", GuiKit.shortCount(10_000));
		assertEquals("12k", GuiKit.shortCount(12_800));
	}

	@Test
	void onlyShortenedCountsGetTheExactNumberInTheTooltip() {
		assertNull(GuiKit.exactCount(new ItemStack(Items.STONE, 999)));
		assertNotNull(GuiKit.exactCount(new ItemStack(Items.STONE, 4096)));
	}
}
