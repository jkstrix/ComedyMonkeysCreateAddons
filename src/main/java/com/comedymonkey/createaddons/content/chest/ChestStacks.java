package com.comedymonkey.createaddons.content.chest;

import java.util.List;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * Oversized-stack helpers shared by the placed chest, its automation handler and its contraption storage.
 * <p>
 * Minecraft 1.21 refuses to save item stacks above 99, so stacks are written as a single item plus a separate
 * {@code Count}. Unstackable items (tools, armour...) are never multiplied.
 */
public final class ChestStacks {
	public static final int SLOTS = 27;

	private ChestStacks() {}

	public static int limit(ItemStack stack, int multiplier) {
		int max = stack.getMaxStackSize();
		return max <= 1 ? max : max * multiplier;
	}

	/**
	 * True if the stacks would fit a chest half with the given multiplier (1 = a plain vanilla chest) and slot count,
	 * after compacting them into the first slots.
	 */
	public static boolean fits(List<ItemStack> items, int multiplier, int slots) {
		int used = 0;
		for (ItemStack stack : items) {
			if (stack.isEmpty())
				continue;
			if (stack.getCount() > limit(stack, multiplier) || ++used > slots)
				return false;
		}
		return true;
	}

	/** The items moved into a list of a new size: positions kept when growing, compacted in order when shrinking. */
	public static NonNullList<ItemStack> resize(List<ItemStack> items, int size) {
		NonNullList<ItemStack> resized = NonNullList.withSize(size, ItemStack.EMPTY);
		if (size >= items.size()) {
			for (int i = 0; i < items.size(); i++)
				resized.set(i, items.get(i));
			return resized;
		}
		int next = 0;
		for (ItemStack stack : items)
			if (!stack.isEmpty() && next < size)
				resized.set(next++, stack);
		return resized;
	}

	/** Inserts into one slot up to the multiplied limit. Returns what did not fit. */
	public static ItemStack insert(NonNullList<ItemStack> items, int slot, ItemStack stack, boolean simulate,
								   int multiplier) {
		if (stack.isEmpty())
			return ItemStack.EMPTY;
		ItemStack existing = items.get(slot);
		if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack))
			return stack;

		int space = limit(stack, multiplier) - existing.getCount();
		if (space <= 0)
			return stack;
		int moved = Math.min(space, stack.getCount());
		if (!simulate) {
			if (existing.isEmpty())
				items.set(slot, stack.copyWithCount(moved));
			else
				existing.grow(moved);
		}
		return moved == stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - moved);
	}

	/** Extracts from one slot, never handing out more than a normal stack at once. */
	public static ItemStack extract(NonNullList<ItemStack> items, int slot, int amount, boolean simulate) {
		ItemStack existing = items.get(slot);
		if (existing.isEmpty() || amount <= 0)
			return ItemStack.EMPTY;
		int taken = Math.min(amount, Math.min(existing.getCount(), existing.getMaxStackSize()));
		if (simulate)
			return existing.copyWithCount(taken);
		ItemStack result = existing.split(taken);
		if (existing.isEmpty())
			items.set(slot, ItemStack.EMPTY);
		return result;
	}

	public static ListTag save(List<ItemStack> items, HolderLookup.Provider registries) {
		ListTag list = new ListTag();
		for (int i = 0; i < items.size(); i++) {
			ItemStack stack = items.get(i);
			if (stack.isEmpty())
				continue;
			CompoundTag entry = new CompoundTag();
			entry.putByte("Slot", (byte) i);
			entry.putInt("Count", stack.getCount());
			entry.put("Item", stack.copyWithCount(1).save(registries));
			list.add(entry);
		}
		return list;
	}

	public static void load(NonNullList<ItemStack> items, ListTag list, HolderLookup.Provider registries) {
		for (int i = 0; i < items.size(); i++)
			items.set(i, ItemStack.EMPTY);
		for (Tag tag : list) {
			CompoundTag entry = (CompoundTag) tag;
			int slot = entry.getByte("Slot") & 255;
			if (slot >= items.size())
				continue;
			ItemStack stack = ItemStack.parseOptional(registries, entry.getCompound("Item"));
			if (!stack.isEmpty())
				stack.setCount(Math.max(1, entry.getInt("Count")));
			items.set(slot, stack);
		}
	}

	public static NonNullList<ItemStack> copy(List<ItemStack> items) {
		NonNullList<ItemStack> copy = NonNullList.withSize(items.size(), ItemStack.EMPTY);
		for (int i = 0; i < items.size(); i++)
			copy.set(i, items.get(i).copy());
		return copy;
	}
}
