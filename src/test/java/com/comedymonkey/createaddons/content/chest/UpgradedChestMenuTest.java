package com.comedymonkey.createaddons.content.chest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.content.chest.UpgradedChestMenu.BigSlot;
import com.comedymonkey.createaddons.content.chest.UpgradedChestMenu.ClientChestContainer;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The menu itself needs a player; these cover its slot and client-container pieces. */
class UpgradedChestMenuTest {
	@Test
	void clientContainerMatchesTheServersLimits() {
		ClientChestContainer chest = new ClientChestContainer(54, 4);
		assertEquals(54, chest.getContainerSize());
		assertEquals(256, chest.getMaxStackSize());
		assertEquals(256, chest.getMaxStackSize(new ItemStack(Items.STONE)));
		assertEquals(64, chest.getMaxStackSize(new ItemStack(Items.ENDER_PEARL)));
		assertEquals(1, chest.getMaxStackSize(new ItemStack(Items.DIAMOND_SWORD)));
	}

	@Test
	void clientContainerKeepsOversizedStacksFromTheServer() {
		ClientChestContainer chest = new ClientChestContainer(27, 16);
		chest.setItem(0, new ItemStack(Items.STONE, 1024));
		assertEquals(1024, chest.getItem(0).getCount());
	}

	@Test
	void bigSlotTakesItsLimitFromTheContainer() {
		ClientChestContainer chest = new ClientChestContainer(27, 8);
		BigSlot slot = new BigSlot(chest, 4, 0, 0);
		assertEquals(512, slot.getMaxStackSize());
		assertEquals(512, slot.getMaxStackSize(new ItemStack(Items.STONE)));
		assertEquals(128, slot.getMaxStackSize(new ItemStack(Items.ENDER_PEARL)));
		assertEquals(1, slot.getMaxStackSize(new ItemStack(Items.DIAMOND_SWORD)));
	}

	@Test
	void bigSlotNeverHandsOutMoreThanANormalStack() {
		ClientChestContainer chest = new ClientChestContainer(27, 16);
		chest.setItem(2, new ItemStack(Items.STONE, 700));
		BigSlot slot = new BigSlot(chest, 2, 0, 0);
		ItemStack taken = slot.remove(700);
		assertEquals(64, taken.getCount());
		assertEquals(636, chest.getItem(2).getCount());
		assertEquals(10, slot.remove(10).getCount());
		assertEquals(626, chest.getItem(2).getCount());
	}

	@Test
	void bigSlotCapsRemovalAtTheItemsOwnStackSize() {
		ClientChestContainer chest = new ClientChestContainer(27, 8);
		chest.setItem(0, new ItemStack(Items.ENDER_PEARL, 100));
		assertEquals(16, new BigSlot(chest, 0, 0, 0).remove(64).getCount());
	}

	@Test
	void bigSlotAcceptsUpToTheMultipliedLimit() {
		ClientChestContainer chest = new ClientChestContainer(27, 4);
		BigSlot slot = new BigSlot(chest, 0, 0, 0);
		ItemStack carried = new ItemStack(Items.STONE, 300);
		ItemStack rest = slot.safeInsert(carried);
		assertEquals(256, chest.getItem(0).getCount());
		assertEquals(44, rest.getCount());
		// Topping up an existing stack
		chest.getItem(0).shrink(6);
		assertEquals(38, slot.safeInsert(rest).getCount());
		assertEquals(256, chest.getItem(0).getCount());
	}

	@Test
	void anOversizedStackCantBeSwappedOntoTheCursor() {
		// Vanilla swaps the cursor with the slot only if mayPlace(cursor) - that would put all 700 on the cursor
		ClientChestContainer chest = new ClientChestContainer(27, 16);
		chest.setItem(0, new ItemStack(Items.STONE, 700));
		BigSlot slot = new BigSlot(chest, 0, 0, 0);
		assertFalse(slot.mayPlace(new ItemStack(Items.DIRT, 5)));
		// The same item still merges in, and a normal-sized stack can still be swapped
		assertTrue(slot.mayPlace(new ItemStack(Items.STONE, 5)));
		chest.setItem(0, new ItemStack(Items.STONE, 64));
		assertTrue(slot.mayPlace(new ItemStack(Items.DIRT, 5)));
	}

	@Test
	void aSlotOverItsLimitTakesNothingAndGivesNothingBack() {
		// The config lowered the limit below what the slot already holds (set directly: setItem would trim it)
		ClientChestContainer chest = new ClientChestContainer(27, 2);
		chest.getItems().set(0, new ItemStack(Items.STONE, 300));
		BigSlot slot = new BigSlot(chest, 0, 0, 0);
		ItemStack carried = new ItemStack(Items.STONE, 10);
		ItemStack rest = slot.safeInsert(carried, 10);
		assertEquals(10, rest.getCount());
		assertEquals(300, chest.getItem(0).getCount());
		// Exactly at the limit: nothing goes in either
		chest.setItem(0, new ItemStack(Items.STONE, 128));
		assertEquals(10, slot.safeInsert(new ItemStack(Items.STONE, 10), 10).getCount());
		assertEquals(128, chest.getItem(0).getCount());
	}

	@Test
	void bigSlotKeepsUnstackablesSingle() {
		ClientChestContainer chest = new ClientChestContainer(27, 64);
		BigSlot slot = new BigSlot(chest, 0, 0, 0);
		slot.safeInsert(new ItemStack(Items.DIAMOND_SWORD));
		ItemStack second = new ItemStack(Items.DIAMOND_SWORD);
		assertEquals(1, slot.safeInsert(second).getCount());
		assertEquals(1, chest.getItem(0).getCount());
		assertTrue(chest.getItem(1).isEmpty());
	}
}
