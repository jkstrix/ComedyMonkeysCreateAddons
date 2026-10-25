package com.comedymonkey.createaddons.content.tradebot.trade;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * What his load and his inputs hold, read once for a whole round of planning: each kind of item (item and
 * components) with how many there are in all. Asking how many goods suit an offer then looks at a few kinds, not
 * every slot of every container once per offer.
 */
public final class InputSnapshot {
	private final List<ItemStack> kinds = new ArrayList<>();
	private final List<Integer> counts = new ArrayList<>();

	private InputSnapshot() {}

	/** Everything in {@code stores}, by kind. */
	public static InputSnapshot of(List<? extends IItemHandler> stores) {
		Map<Key, Integer> byKind = new LinkedHashMap<>();
		for (IItemHandler store : stores)
			for (int slot = 0; slot < store.getSlots(); slot++) {
				ItemStack stack = store.getStackInSlot(slot);
				if (!stack.isEmpty())
					byKind.merge(new Key(stack), stack.getCount(), Integer::sum);
			}
		InputSnapshot snapshot = new InputSnapshot();
		byKind.forEach((key, count) -> {
			snapshot.kinds.add(key.stack.copyWithCount(1));
			snapshot.counts.add(count);
		});
		return snapshot;
	}

	/** How many items there are that would pay for {@code goods}. */
	public int count(ItemCost goods) {
		int count = 0;
		for (int i = 0; i < kinds.size(); i++)
			if (goods.test(kinds.get(i)))
				count += counts.get(i);
		return count;
	}

	/** Whether any of it would pay for {@code goods} at all. */
	public boolean has(ItemCost goods) {
		for (ItemStack kind : kinds)
			if (goods.test(kind))
				return true;
		return false;
	}

	/** A stack as a map key: the same item with the same components. */
	private record Key(ItemStack stack) {
		@Override
		public boolean equals(Object other) {
			return other instanceof Key key && ItemStack.isSameItemSameComponents(stack, key.stack);
		}

		@Override
		public int hashCode() {
			return ItemStack.hashItemAndComponents(stack);
		}
	}
}
