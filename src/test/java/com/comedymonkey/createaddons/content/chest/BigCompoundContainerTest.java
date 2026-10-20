package com.comedymonkey.createaddons.content.chest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.content.chest.UpgradedChestMenu.ClientChestContainer;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

class BigCompoundContainerTest {
	@Test
	void limitsComeFromTheMultiplierNotTheItem() {
		BigCompoundContainer both = new BigCompoundContainer(new ClientChestContainer(27, 8),
			new ClientChestContainer(27, 8), () -> 8);
		assertEquals(512, both.getMaxStackSize());
		assertEquals(512, both.getMaxStackSize(new ItemStack(Items.STONE)));
		assertEquals(128, both.getMaxStackSize(new ItemStack(Items.ENDER_PEARL)));
		assertEquals(1, both.getMaxStackSize(new ItemStack(Items.DIAMOND_SWORD)));
	}

	@Test
	void followsTheMultiplierAsItChanges() {
		AtomicInteger multiplier = new AtomicInteger(2);
		BigCompoundContainer both = new BigCompoundContainer(new ClientChestContainer(27, 2),
			new ClientChestContainer(27, 2), multiplier::get);
		assertEquals(128, both.getMaxStackSize(new ItemStack(Items.STONE)));
		multiplier.set(16);
		assertEquals(1024, both.getMaxStackSize(new ItemStack(Items.STONE)));
	}

	@Test
	void mapsSlotsOntoTheTwoHalves() {
		ClientChestContainer first = new ClientChestContainer(27, 8);
		ClientChestContainer second = new ClientChestContainer(27, 8);
		BigCompoundContainer both = new BigCompoundContainer(first, second, () -> 8);
		assertEquals(54, both.getContainerSize());
		assertTrue(both.contains(first));
		assertTrue(both.contains(second));

		ItemStack top = new ItemStack(Items.STONE, 400);
		ItemStack bottom = new ItemStack(Items.DIRT, 300);
		both.setItem(26, top);
		both.setItem(27, bottom);
		assertSame(top, first.getItem(26));
		assertSame(bottom, second.getItem(0));
		assertEquals(400, both.getItem(26).getCount());
		assertEquals(300, both.getItem(27).getCount());
	}

	@Test
	void holdsDiamondDoubleChests() {
		BigCompoundContainer both = new BigCompoundContainer(new ClientChestContainer(54, 8),
			new ClientChestContainer(54, 8), () -> 8);
		assertEquals(LargeUpgradedChestMenu.SLOTS, both.getContainerSize());
	}
}
