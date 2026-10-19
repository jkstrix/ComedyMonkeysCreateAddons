package com.comedymonkey.createaddons.content.chest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Sorts an upgraded chest or barrel on the server: identical items are merged up to the slot's real limit (which can
 * be far above 64) and laid out in order of item id. Inventory sorting mods work by simulating clicks and assume
 * vanilla stack sizes, which oversized stacks break, so upgraded storage sorts itself.
 */
public final class ChestSorting {
	private ChestSorting() {}

	public static void sort(Container container) {
		// Total up each distinct item (same item and components)
		List<ItemStack> kinds = new ArrayList<>();
		List<Integer> totals = new ArrayList<>();
		int size = container.getContainerSize();
		for (int slot = 0; slot < size; slot++) {
			ItemStack stack = container.getItem(slot);
			if (stack.isEmpty())
				continue;
			int found = -1;
			for (int i = 0; i < kinds.size(); i++)
				if (ItemStack.isSameItemSameComponents(kinds.get(i), stack)) {
					found = i;
					break;
				}
			if (found < 0) {
				kinds.add(stack.copyWithCount(1));
				totals.add(stack.getCount());
			} else {
				totals.set(found, totals.get(found) + stack.getCount());
			}
		}

		List<Integer> order = new ArrayList<>();
		for (int i = 0; i < kinds.size(); i++)
			order.add(i);
		order.sort(Comparator.comparing((Integer i) -> BuiltInRegistries.ITEM.getKey(kinds.get(i).getItem()).toString())
			.thenComparing(i -> kinds.get(i).getHoverName().getString()));

		// Lay them out; merging can only use fewer slots, but don't touch anything if somehow it wouldn't fit
		List<ItemStack> sorted = new ArrayList<>();
		for (int i : order) {
			ItemStack kind = kinds.get(i);
			int limit = Math.max(1, container.getMaxStackSize(kind));
			int left = totals.get(i);
			while (left > 0) {
				int n = Math.min(left, limit);
				sorted.add(kind.copyWithCount(n));
				left -= n;
			}
		}
		if (sorted.size() > size)
			return;
		for (int slot = 0; slot < size; slot++)
			container.setItem(slot, slot < sorted.size() ? sorted.get(slot) : ItemStack.EMPTY);
		container.setChanged();
	}
}
