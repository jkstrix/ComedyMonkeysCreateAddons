package com.comedymonkey.createaddons.content.chest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.content.chest.UpgradedChestMenu.ClientChestContainer;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

class ChestSortingTest {
	private static ItemStack named(Item item, String name, int count) {
		ItemStack stack = new ItemStack(item, count);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
		return stack;
	}

	/** One entry per distinct item + components with its total count. */
	private static List<ItemStack> totals(Container container) {
		List<ItemStack> totals = new ArrayList<>();
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack stack = container.getItem(slot);
			if (stack.isEmpty())
				continue;
			ItemStack found = null;
			for (ItemStack total : totals)
				if (ItemStack.isSameItemSameComponents(total, stack))
					found = total;
			if (found == null)
				totals.add(stack.copy());
			else
				found.grow(stack.getCount());
		}
		return totals;
	}

	private static void assertSameTotals(List<ItemStack> expected, List<ItemStack> actual) {
		assertEquals(expected.size(), actual.size());
		for (ItemStack want : expected) {
			int count = 0;
			for (ItemStack have : actual)
				if (ItemStack.isSameItemSameComponents(want, have))
					count = have.getCount();
			assertEquals(want.getCount(), count, want.toString());
		}
	}

	@Test
	void mergesPartialStacksUpToTheMultipliedLimit() {
		ClientChestContainer chest = new ClientChestContainer(9, 8);
		chest.setItem(2, new ItemStack(Items.STONE, 300));
		chest.setItem(5, new ItemStack(Items.STONE, 300));
		chest.setItem(8, new ItemStack(Items.STONE, 300));
		ChestSorting.sort(chest);
		assertEquals(512, chest.getItem(0).getCount());
		assertEquals(388, chest.getItem(1).getCount());
		for (int i = 2; i < 9; i++)
			assertTrue(chest.getItem(i).isEmpty());
	}

	@Test
	void ordersByItemId() {
		ClientChestContainer chest = new ClientChestContainer(9, 2);
		chest.setItem(0, new ItemStack(Items.STONE, 3));
		chest.setItem(4, new ItemStack(Items.DIRT, 2));
		chest.setItem(8, new ItemStack(Items.COBBLESTONE, 1));
		ChestSorting.sort(chest);
		assertTrue(chest.getItem(0).is(Items.COBBLESTONE));
		assertTrue(chest.getItem(1).is(Items.DIRT));
		assertTrue(chest.getItem(2).is(Items.STONE));
	}

	@Test
	void sameItemIsOrderedByName() {
		ClientChestContainer chest = new ClientChestContainer(9, 2);
		chest.setItem(0, named(Items.DIAMOND, "Beta", 1));
		chest.setItem(1, named(Items.DIAMOND, "Alpha", 1));
		ChestSorting.sort(chest);
		assertEquals("Alpha", chest.getItem(0).getHoverName().getString());
		assertEquals("Beta", chest.getItem(1).getHoverName().getString());
	}

	@Test
	void keepsItemsWithDifferentComponentsApart() {
		ClientChestContainer chest = new ClientChestContainer(9, 8);
		chest.setItem(0, new ItemStack(Items.DIAMOND, 10));
		chest.setItem(1, named(Items.DIAMOND, "Shiny", 10));
		chest.setItem(2, new ItemStack(Items.DIAMOND, 10));
		List<ItemStack> before = totals(chest);
		ChestSorting.sort(chest);
		assertSameTotals(before, totals(chest));
		assertTrue(chest.getItem(2).isEmpty());
	}

	@Test
	void neverStacksUnstackables() {
		ClientChestContainer chest = new ClientChestContainer(9, 64);
		chest.setItem(1, new ItemStack(Items.DIAMOND_SWORD));
		chest.setItem(4, new ItemStack(Items.DIAMOND_SWORD));
		chest.setItem(7, new ItemStack(Items.DIAMOND_SWORD));
		ChestSorting.sort(chest);
		for (int i = 0; i < 3; i++) {
			assertTrue(chest.getItem(i).is(Items.DIAMOND_SWORD));
			assertEquals(1, chest.getItem(i).getCount());
		}
		assertTrue(chest.getItem(3).isEmpty());
	}

	@Test
	void respectsSmallerStackSizes() {
		ClientChestContainer chest = new ClientChestContainer(9, 2);
		chest.setItem(0, new ItemStack(Items.ENDER_PEARL, 16));
		chest.setItem(1, new ItemStack(Items.ENDER_PEARL, 16));
		chest.setItem(2, new ItemStack(Items.ENDER_PEARL, 16));
		ChestSorting.sort(chest);
		assertEquals(32, chest.getItem(0).getCount());
		assertEquals(16, chest.getItem(1).getCount());
		assertTrue(chest.getItem(2).isEmpty());
	}

	@Test
	void conservesEveryItemInAMixedChest() {
		ClientChestContainer chest = new ClientChestContainer(27, 4);
		chest.setItem(0, new ItemStack(Items.STONE, 200));
		chest.setItem(3, new ItemStack(Items.DIRT, 64));
		chest.setItem(6, new ItemStack(Items.STONE, 256));
		chest.setItem(9, new ItemStack(Items.DIAMOND_SWORD));
		chest.setItem(12, new ItemStack(Items.ENDER_PEARL, 60));
		chest.setItem(15, named(Items.STONE, "Odd", 5));
		chest.setItem(18, new ItemStack(Items.DIRT, 250));
		chest.setItem(26, new ItemStack(Items.ENDER_PEARL, 10));
		List<ItemStack> before = totals(chest);
		ChestSorting.sort(chest);
		assertSameTotals(before, totals(chest));
		for (int i = 0; i < 27; i++)
			assertTrue(chest.getItem(i).getCount() <= chest.getMaxStackSize(chest.getItem(i)), "slot " + i);
	}

	@Test
	void leavesTheChestAloneIfTheLayoutWouldNotFit() {
		ClientChestContainer chest = new ClientChestContainer(2, 1);
		chest.setItem(0, new ItemStack(Items.STONE, 64));
		chest.setItem(1, new ItemStack(Items.DIRT, 64));
		// Over the limit (e.g. the multiplier was lowered): it would take three slots to lay out
		chest.getItem(0).setCount(100);
		ChestSorting.sort(chest);
		assertTrue(chest.getItem(0).is(Items.STONE));
		assertEquals(100, chest.getItem(0).getCount());
		assertTrue(chest.getItem(1).is(Items.DIRT));
		assertEquals(64, chest.getItem(1).getCount());
	}

	@Test
	void emptyChestStaysEmpty() {
		ClientChestContainer chest = new ClientChestContainer(27, 8);
		ChestSorting.sort(chest);
		assertTrue(chest.isEmpty());
	}

	@Test
	void sortsAcrossBothHalvesOfADoubleChest() {
		ClientChestContainer top = new ClientChestContainer(27, 4);
		ClientChestContainer bottom = new ClientChestContainer(27, 4);
		BigCompoundContainer both = new BigCompoundContainer(top, bottom, () -> 4);
		bottom.setItem(10, new ItemStack(Items.STONE, 200));
		bottom.setItem(20, new ItemStack(Items.STONE, 200));
		top.setItem(26, new ItemStack(Items.COBBLESTONE, 3));
		ChestSorting.sort(both);
		assertTrue(top.getItem(0).is(Items.COBBLESTONE));
		assertEquals(256, top.getItem(1).getCount());
		assertEquals(144, top.getItem(2).getCount());
		assertTrue(bottom.isEmpty());
	}
}
