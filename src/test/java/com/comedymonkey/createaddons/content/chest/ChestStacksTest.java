package com.comedymonkey.createaddons.content.chest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

class ChestStacksTest {
	private static HolderLookup.Provider registries;

	@BeforeAll
	static void setUpRegistries() {
		registries = VanillaRegistries.createLookup();
	}

	private static NonNullList<ItemStack> slots(int size) {
		return NonNullList.withSize(size, ItemStack.EMPTY);
	}

	private static ItemStack named(Item item, String name, int count) {
		ItemStack stack = new ItemStack(item, count);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
		return stack;
	}

	private static int total(List<ItemStack> items, Item item) {
		int total = 0;
		for (ItemStack stack : items)
			if (stack.is(item))
				total += stack.getCount();
		return total;
	}

	// ---------------------------------------------------------------- limit

	@Test
	void limitMultipliesTheItemsOwnStackSize() {
		assertEquals(512, ChestStacks.limit(new ItemStack(Items.STONE), 8));
		assertEquals(128, ChestStacks.limit(new ItemStack(Items.ENDER_PEARL), 8));
		assertEquals(64, ChestStacks.limit(new ItemStack(Items.STONE), 1));
	}

	@Test
	void limitNeverMultipliesUnstackables() {
		assertEquals(1, ChestStacks.limit(new ItemStack(Items.DIAMOND_SWORD), 64));
	}

	@Test
	void limitHandlesTheLargestComponentStackSizeAndMultiplier() {
		ItemStack stack = new ItemStack(Items.STONE);
		stack.set(DataComponents.MAX_STACK_SIZE, 99);
		assertEquals(99 * 64, ChestStacks.limit(stack, 64));
	}

	// ---------------------------------------------------------------- fits

	@Test
	void fitsAcceptsEmptyAndFullVanillaChests() {
		assertTrue(ChestStacks.fits(slots(27), 1, 27));
		List<ItemStack> full = new ArrayList<>();
		for (int i = 0; i < 27; i++)
			full.add(new ItemStack(Items.STONE, 64));
		assertTrue(ChestStacks.fits(full, 1, 27));
	}

	@Test
	void fitsRejectsMoreStacksThanSlots() {
		List<ItemStack> items = new ArrayList<>();
		for (int i = 0; i < 28; i++)
			items.add(new ItemStack(Items.STONE, 1));
		assertFalse(ChestStacks.fits(items, 1, 27));
		assertTrue(ChestStacks.fits(items, 1, 28));
	}

	@Test
	void fitsRejectsAStackAboveTheLimit() {
		List<ItemStack> items = List.of(new ItemStack(Items.STONE, 65));
		assertFalse(ChestStacks.fits(items, 1, 27));
		assertTrue(ChestStacks.fits(items, 2, 27));
		assertFalse(ChestStacks.fits(List.of(new ItemStack(Items.STONE, 129)), 2, 27));
	}

	@Test
	void fitsRejectsMultipliedUnstackables() {
		assertFalse(ChestStacks.fits(List.of(new ItemStack(Items.DIAMOND_SWORD, 2)), 64, 27));
	}

	@Test
	void fitsIgnoresEmptySlotsWhenCountingScatteredStacks() {
		// 27 stacks spread over a 54-slot (diamond) inventory still fit 27 slots once compacted
		NonNullList<ItemStack> items = slots(54);
		for (int i = 0; i < 27; i++)
			items.set(i * 2, new ItemStack(Items.STONE, 64));
		assertTrue(ChestStacks.fits(items, 1, 27));
		items.set(53, new ItemStack(Items.DIRT));
		assertFalse(ChestStacks.fits(items, 1, 27));
	}

	// ---------------------------------------------------------------- resize

	@Test
	void resizeKeepsPositionsWhenGrowing() {
		NonNullList<ItemStack> items = slots(27);
		items.set(3, new ItemStack(Items.STONE, 100));
		items.set(26, new ItemStack(Items.DIRT, 5));
		NonNullList<ItemStack> resized = ChestStacks.resize(items, 54);
		assertEquals(54, resized.size());
		assertEquals(100, resized.get(3).getCount());
		assertTrue(resized.get(26).is(Items.DIRT));
		for (int i = 27; i < 54; i++)
			assertTrue(resized.get(i).isEmpty());
	}

	@Test
	void resizeKeepsPositionsAtTheSameSize() {
		NonNullList<ItemStack> items = slots(27);
		items.set(20, new ItemStack(Items.STONE));
		assertTrue(ChestStacks.resize(items, 27).get(20).is(Items.STONE));
	}

	@Test
	void resizeCompactsInOrderWhenShrinking() {
		NonNullList<ItemStack> items = slots(54);
		items.set(5, new ItemStack(Items.STONE, 300));
		items.set(30, new ItemStack(Items.DIRT, 2));
		items.set(53, new ItemStack(Items.DIAMOND, 1));
		NonNullList<ItemStack> resized = ChestStacks.resize(items, 27);
		assertEquals(27, resized.size());
		assertTrue(resized.get(0).is(Items.STONE));
		assertEquals(300, resized.get(0).getCount());
		assertTrue(resized.get(1).is(Items.DIRT));
		assertTrue(resized.get(2).is(Items.DIAMOND));
		for (int i = 3; i < 27; i++)
			assertTrue(resized.get(i).isEmpty());
	}

	@Test
	void resizeShrinkingLosesNothingWhenTheStacksFit() {
		NonNullList<ItemStack> items = slots(54);
		for (int i = 0; i < 27; i++)
			items.set(53 - i * 2, new ItemStack(Items.STONE, 500));
		assertTrue(ChestStacks.fits(items, 8, 27));
		NonNullList<ItemStack> resized = ChestStacks.resize(items, 27);
		assertEquals(total(items, Items.STONE), total(resized, Items.STONE));
	}

	// ---------------------------------------------------------------- insert

	@Test
	void insertFillsAnEmptySlotUpToTheMultipliedLimit() {
		NonNullList<ItemStack> items = slots(1);
		ItemStack rest = ChestStacks.insert(items, 0, new ItemStack(Items.STONE, 600), false, 8);
		assertEquals(512, items.get(0).getCount());
		assertEquals(88, rest.getCount());
		assertTrue(rest.is(Items.STONE));
	}

	@Test
	void insertTopsUpAMatchingStack() {
		NonNullList<ItemStack> items = slots(1);
		items.set(0, new ItemStack(Items.STONE, 500));
		ItemStack rest = ChestStacks.insert(items, 0, new ItemStack(Items.STONE, 64), false, 8);
		assertEquals(512, items.get(0).getCount());
		assertEquals(52, rest.getCount());
	}

	@Test
	void insertReturnsEmptyWhenEverythingFits() {
		NonNullList<ItemStack> items = slots(1);
		assertTrue(ChestStacks.insert(items, 0, new ItemStack(Items.STONE, 64), false, 2).isEmpty());
		assertTrue(ChestStacks.insert(items, 0, new ItemStack(Items.STONE, 64), false, 2).isEmpty());
		assertEquals(128, items.get(0).getCount());
	}

	@Test
	void insertNeverMutatesTheStackPassedIn() {
		NonNullList<ItemStack> items = slots(1);
		ItemStack input = new ItemStack(Items.STONE, 600);
		ChestStacks.insert(items, 0, input, false, 8);
		assertEquals(600, input.getCount());
		assertNotSame(input, items.get(0));
	}

	@Test
	void insertRefusesADifferentItem() {
		NonNullList<ItemStack> items = slots(1);
		items.set(0, new ItemStack(Items.DIRT, 1));
		ItemStack input = new ItemStack(Items.STONE, 10);
		assertSame(input, ChestStacks.insert(items, 0, input, false, 8));
		assertEquals(1, items.get(0).getCount());
	}

	@Test
	void insertRefusesTheSameItemWithDifferentComponents() {
		NonNullList<ItemStack> items = slots(1);
		items.set(0, new ItemStack(Items.DIAMOND, 1));
		ItemStack rest = ChestStacks.insert(items, 0, named(Items.DIAMOND, "Shiny", 5), false, 8);
		assertEquals(5, rest.getCount());
		assertEquals(1, items.get(0).getCount());
	}

	@Test
	void insertIntoAFullSlotReturnsTheStack() {
		NonNullList<ItemStack> items = slots(1);
		items.set(0, new ItemStack(Items.STONE, 128));
		assertEquals(10, ChestStacks.insert(items, 0, new ItemStack(Items.STONE, 10), false, 2).getCount());
		assertEquals(128, items.get(0).getCount());
	}

	@Test
	void insertLeavesAnOverLimitSlotAlone() {
		// e.g. after the server lowered the multiplier
		NonNullList<ItemStack> items = slots(1);
		items.set(0, new ItemStack(Items.STONE, 600));
		assertEquals(5, ChestStacks.insert(items, 0, new ItemStack(Items.STONE, 5), false, 8).getCount());
		assertEquals(600, items.get(0).getCount());
	}

	@Test
	void insertOnlyPutsOneUnstackableInASlot() {
		NonNullList<ItemStack> items = slots(1);
		ItemStack rest = ChestStacks.insert(items, 0, new ItemStack(Items.DIAMOND_SWORD), false, 64);
		assertTrue(rest.isEmpty());
		assertEquals(1, ChestStacks.insert(items, 0, new ItemStack(Items.DIAMOND_SWORD), false, 64).getCount());
		assertEquals(1, items.get(0).getCount());
	}

	@Test
	void insertOfAnEmptyStackDoesNothing() {
		NonNullList<ItemStack> items = slots(1);
		assertTrue(ChestStacks.insert(items, 0, ItemStack.EMPTY, false, 8).isEmpty());
		assertTrue(items.get(0).isEmpty());
	}

	@Test
	void simulatedInsertChangesNothingAndPredictsTheRealOne() {
		NonNullList<ItemStack> items = slots(2);
		items.set(1, new ItemStack(Items.STONE, 400));
		ItemStack simulatedEmpty = ChestStacks.insert(items, 0, new ItemStack(Items.STONE, 600), true, 8);
		ItemStack simulatedPartial = ChestStacks.insert(items, 1, new ItemStack(Items.STONE, 600), true, 8);
		assertTrue(items.get(0).isEmpty());
		assertEquals(400, items.get(1).getCount());

		assertEquals(simulatedEmpty.getCount(), ChestStacks.insert(items, 0, new ItemStack(Items.STONE, 600), false, 8).getCount());
		assertEquals(simulatedPartial.getCount(), ChestStacks.insert(items, 1, new ItemStack(Items.STONE, 600), false, 8).getCount());
	}

	@Test
	void insertConservesItems() {
		NonNullList<ItemStack> items = slots(1);
		ItemStack rest = ChestStacks.insert(items, 0, new ItemStack(Items.STONE, 777), false, 4);
		assertEquals(777, items.get(0).getCount() + rest.getCount());
	}

	// ---------------------------------------------------------------- extract

	@Test
	void extractNeverHandsOutMoreThanANormalStack() {
		NonNullList<ItemStack> items = slots(1);
		items.set(0, new ItemStack(Items.STONE, 1000));
		ItemStack taken = ChestStacks.extract(items, 0, 1000, false);
		assertEquals(64, taken.getCount());
		assertEquals(936, items.get(0).getCount());
	}

	@Test
	void extractCapsAtTheItemsOwnStackSize() {
		NonNullList<ItemStack> items = slots(1);
		items.set(0, new ItemStack(Items.ENDER_PEARL, 100));
		assertEquals(16, ChestStacks.extract(items, 0, 64, false).getCount());
	}

	@Test
	void extractTakesLessWhenAskedForLess() {
		NonNullList<ItemStack> items = slots(1);
		items.set(0, new ItemStack(Items.STONE, 1000));
		assertEquals(10, ChestStacks.extract(items, 0, 10, false).getCount());
		assertEquals(990, items.get(0).getCount());
	}

	@Test
	void extractEmptiesTheSlotWhenDrained() {
		NonNullList<ItemStack> items = slots(1);
		items.set(0, new ItemStack(Items.STONE, 20));
		assertEquals(20, ChestStacks.extract(items, 0, 64, false).getCount());
		assertTrue(items.get(0).isEmpty());
	}

	@Test
	void extractOfNothingReturnsEmpty() {
		NonNullList<ItemStack> items = slots(1);
		assertTrue(ChestStacks.extract(items, 0, 64, false).isEmpty());
		items.set(0, new ItemStack(Items.STONE, 20));
		assertTrue(ChestStacks.extract(items, 0, 0, false).isEmpty());
		assertTrue(ChestStacks.extract(items, 0, -5, false).isEmpty());
		assertEquals(20, items.get(0).getCount());
	}

	@Test
	void simulatedExtractChangesNothingAndPredictsTheRealOne() {
		NonNullList<ItemStack> items = slots(1);
		items.set(0, named(Items.DIAMOND, "Shiny", 300));
		ItemStack simulated = ChestStacks.extract(items, 0, 100, true);
		assertEquals(300, items.get(0).getCount());
		ItemStack real = ChestStacks.extract(items, 0, 100, false);
		assertTrue(ItemStack.matches(simulated, real));
		assertEquals("Shiny", real.getHoverName().getString());
	}

	@Test
	void repeatedExtractsReturnExactlyWhatWasStored() {
		NonNullList<ItemStack> items = slots(1);
		items.set(0, new ItemStack(Items.STONE, 1000));
		int taken = 0;
		for (int i = 0; i < 100 && !items.get(0).isEmpty(); i++)
			taken += ChestStacks.extract(items, 0, 64, false).getCount();
		assertEquals(1000, taken);
		assertTrue(items.get(0).isEmpty());
	}

	// ---------------------------------------------------------------- save / load

	@Test
	void saveAndLoadKeepOversizedStacksAndComponents() {
		NonNullList<ItemStack> items = slots(54);
		items.set(0, new ItemStack(Items.DIAMOND_SWORD));
		items.set(3, new ItemStack(Items.STONE, 1000));
		items.set(53, named(Items.DIAMOND, "Shiny", 5000));

		ListTag saved = ChestStacks.save(items, registries);
		NonNullList<ItemStack> loaded = slots(54);
		ChestStacks.load(loaded, saved, registries);

		for (int i = 0; i < 54; i++)
			assertTrue(ItemStack.matches(items.get(i), loaded.get(i)), "slot " + i);
	}

	@Test
	void saveWritesOnlyFilledSlotsWithVanillaSafeItems() {
		NonNullList<ItemStack> items = slots(27);
		items.set(7, new ItemStack(Items.STONE, 1000));
		ListTag saved = ChestStacks.save(items, registries);
		assertEquals(1, saved.size());
		CompoundTag entry = saved.getCompound(0);
		assertEquals(7, entry.getByte("Slot"));
		assertEquals(1000, entry.getInt("Count"));
		// The nested stack must stay within what vanilla can encode (1..99)
		ItemStack item = ItemStack.parseOptional(registries, entry.getCompound("Item"));
		assertTrue(item.is(Items.STONE));
		assertEquals(1, item.getCount());
	}

	@Test
	void loadClearsWhatWasThereBefore() {
		NonNullList<ItemStack> items = slots(27);
		items.set(0, new ItemStack(Items.DIRT, 5));
		ChestStacks.load(items, new ListTag(), registries);
		assertTrue(items.get(0).isEmpty());
	}

	@Test
	void loadIgnoresSlotsPastTheEnd() {
		NonNullList<ItemStack> big = slots(54);
		big.set(40, new ItemStack(Items.STONE, 10));
		big.set(2, new ItemStack(Items.DIRT, 3));
		NonNullList<ItemStack> small = slots(27);
		ChestStacks.load(small, ChestStacks.save(big, registries), registries);
		assertEquals(3, small.get(2).getCount());
		assertEquals(0, total(small, Items.STONE));
	}

	@Test
	void loadTreatsAMissingCountAsOne() {
		CompoundTag entry = new CompoundTag();
		entry.putByte("Slot", (byte) 0);
		entry.put("Item", new ItemStack(Items.STONE).save(registries));
		ListTag list = new ListTag();
		list.add(entry);
		NonNullList<ItemStack> items = slots(1);
		ChestStacks.load(items, list, registries);
		assertEquals(1, items.get(0).getCount());
	}

	// ---------------------------------------------------------------- copy

	@Test
	void copyIsDeep() {
		NonNullList<ItemStack> items = slots(3);
		items.set(1, new ItemStack(Items.STONE, 300));
		NonNullList<ItemStack> copy = ChestStacks.copy(items);
		assertEquals(3, copy.size());
		assertNotSame(items.get(1), copy.get(1));
		copy.get(1).shrink(100);
		assertEquals(300, items.get(1).getCount());
	}
}
