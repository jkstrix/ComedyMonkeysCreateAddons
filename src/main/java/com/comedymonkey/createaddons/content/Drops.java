package com.comedymonkey.createaddons.content;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;

/** Loot rolled many times over (a whole horde culled at once): kept as few, full stacks rather than one per roll. */
public final class Drops {
	private Drops() {}

	/**
	 * The same items, equal stacks joined up to their stack size. A cage of 256 culled rolls hundreds of small stacks;
	 * joined, they go into the output, or out as item entities, a few at a time.
	 */
	public static List<ItemStack> merged(List<ItemStack> drops) {
		List<ItemStack> merged = new ArrayList<>();
		for (ItemStack drop : drops) {
			ItemStack left = drop.copy();
			for (int i = 0; i < merged.size() && !left.isEmpty(); i++) {
				ItemStack into = merged.get(i);
				if (!ItemStack.isSameItemSameComponents(into, left))
					continue;
				int moved = Math.min(left.getCount(), into.getMaxStackSize() - into.getCount());
				into.grow(moved);
				left.shrink(moved);
			}
			if (!left.isEmpty())
				merged.add(left);
		}
		return merged;
	}
}
