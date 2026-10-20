package com.comedymonkey.createaddons.content.chest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

class UpgradedChestItemHandlerTest {
	/** A chest with a fixed multiplier (no config needed) that counts its setChanged calls. */
	private static final class FakeChest implements UpgradedStorage {
		private NonNullList<ItemStack> items;
		private final int multiplier;
		int changes;

		FakeChest(int size, int multiplier) {
			this.items = NonNullList.withSize(size, ItemStack.EMPTY);
			this.multiplier = multiplier;
		}

		@Override
		public ChestTier getTier() {
			return ChestTier.GOLD;
		}

		@Override
		public int multiplier() {
			return multiplier;
		}

		@Override
		public NonNullList<ItemStack> getItems() {
			return items;
		}

		@Override
		public void fill(List<ItemStack> contents) {
			items = ChestStacks.resize(contents, items.size());
		}

		@Override
		public boolean isOpenIn(Player player) {
			return false;
		}

		@Override
		public int getContainerSize() {
			return items.size();
		}

		@Override
		public boolean isEmpty() {
			return items.stream().allMatch(ItemStack::isEmpty);
		}

		@Override
		public ItemStack getItem(int slot) {
			return items.get(slot);
		}

		@Override
		public ItemStack removeItem(int slot, int amount) {
			return ContainerHelper.removeItem(items, slot, amount);
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot) {
			return ContainerHelper.takeItem(items, slot);
		}

		@Override
		public void setItem(int slot, ItemStack stack) {
			items.set(slot, stack);
		}

		@Override
		public int getMaxStackSize() {
			return 64 * multiplier;
		}

		@Override
		public int getMaxStackSize(ItemStack stack) {
			return ChestStacks.limit(stack, multiplier);
		}

		@Override
		public void setChanged() {
			changes++;
		}

		@Override
		public boolean stillValid(Player player) {
			return true;
		}

		@Override
		public void clearContent() {
			items.clear();
		}
	}

	@Test
	void reportsTheChestsSlotsAndLimit() {
		FakeChest chest = new FakeChest(54, 8);
		UpgradedChestItemHandler handler = new UpgradedChestItemHandler(chest);
		assertEquals(54, handler.getSlots());
		assertEquals(512, handler.getSlotLimit(0));
		assertTrue(handler.isItemValid(0, new ItemStack(Items.STONE)));
	}

	@Test
	void insertFillsPast64AndMarksTheChestChanged() {
		FakeChest chest = new FakeChest(27, 8);
		UpgradedChestItemHandler handler = new UpgradedChestItemHandler(chest);
		ItemStack rest = handler.insertItem(0, new ItemStack(Items.STONE, 1000), false);
		assertEquals(488, rest.getCount());
		assertEquals(512, chest.getItem(0).getCount());
		assertEquals(1, chest.changes);
	}

	@Test
	void simulatedInsertChangesNothing() {
		FakeChest chest = new FakeChest(27, 8);
		UpgradedChestItemHandler handler = new UpgradedChestItemHandler(chest);
		ItemStack simulated = handler.insertItem(0, new ItemStack(Items.STONE, 1000), true);
		assertEquals(488, simulated.getCount());
		assertTrue(chest.getItem(0).isEmpty());
		assertEquals(0, chest.changes);
	}

	@Test
	void refusedInsertDoesNotMarkTheChestChanged() {
		FakeChest chest = new FakeChest(27, 8);
		chest.setItem(0, new ItemStack(Items.DIRT));
		UpgradedChestItemHandler handler = new UpgradedChestItemHandler(chest);
		ItemStack input = new ItemStack(Items.STONE, 5);
		assertSame(input, handler.insertItem(0, input, false));
		assertEquals(0, chest.changes);
	}

	@Test
	void extractHandsOutNormalStacks() {
		FakeChest chest = new FakeChest(27, 8);
		chest.setItem(3, new ItemStack(Items.STONE, 500));
		UpgradedChestItemHandler handler = new UpgradedChestItemHandler(chest);
		assertEquals(64, handler.extractItem(3, 500, true).getCount());
		assertEquals(500, chest.getItem(3).getCount());
		assertEquals(0, chest.changes);

		assertEquals(64, handler.extractItem(3, 500, false).getCount());
		assertEquals(436, chest.getItem(3).getCount());
		assertEquals(1, chest.changes);
	}

	@Test
	void extractFromAnEmptySlotDoesNotMarkTheChestChanged() {
		FakeChest chest = new FakeChest(27, 8);
		UpgradedChestItemHandler handler = new UpgradedChestItemHandler(chest);
		assertTrue(handler.extractItem(0, 64, false).isEmpty());
		assertEquals(0, chest.changes);
	}

	@Test
	void automationRoundTripConservesItems() {
		FakeChest chest = new FakeChest(27, 4);
		UpgradedChestItemHandler handler = new UpgradedChestItemHandler(chest);
		int inserted = 0;
		for (int slot = 0; slot < handler.getSlots(); slot++) {
			ItemStack rest = handler.insertItem(slot, new ItemStack(Items.STONE, 300), false);
			inserted += 300 - rest.getCount();
		}
		assertEquals(27 * 256, inserted);

		int extracted = 0;
		for (int slot = 0; slot < handler.getSlots(); slot++)
			while (!handler.getStackInSlot(slot).isEmpty())
				extracted += handler.extractItem(slot, 64, false).getCount();
		assertEquals(inserted, extracted);
		assertTrue(chest.isEmpty());
	}

	@Test
	void setStackInSlotWritesThroughToTheChest() {
		FakeChest chest = new FakeChest(27, 8);
		UpgradedChestItemHandler handler = new UpgradedChestItemHandler(chest);
		ItemStack stack = new ItemStack(Items.STONE, 400);
		handler.setStackInSlot(5, stack);
		assertSame(stack, chest.getItem(5));
		assertSame(stack, handler.getStackInSlot(5));
	}
}
