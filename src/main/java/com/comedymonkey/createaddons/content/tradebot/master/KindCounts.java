package com.comedymonkey.createaddons.content.tradebot.master;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.comedymonkey.createaddons.content.tradebot.work.ContainerWork;

import net.minecraft.world.item.ItemStack;

/**
 * Counts of items by kind (item and components: exact, as Create's gauges match, design/specs/trade-master.md P7).
 * As a {@link ContainerWork.Wanted} it's what he takes from a container: each kind up to its count, counted down as
 * it moves.
 */
public final class KindCounts implements ContainerWork.Wanted {
	private final Map<Key, Integer> counts = new LinkedHashMap<>();

	public static KindCounts of(List<ItemStack> stacks) {
		KindCounts counts = new KindCounts();
		for (ItemStack stack : stacks)
			counts.add(stack, stack.getCount());
		return counts;
	}

	public void add(ItemStack kind, int count) {
		if (!kind.isEmpty() && count != 0)
			counts.merge(new Key(kind.copyWithCount(1)), count, Integer::sum);
	}

	public int get(ItemStack kind) {
		return kind.isEmpty() ? 0 : counts.getOrDefault(new Key(kind), 0);
	}

	/** Every kind with a count above 0, one item each. */
	public List<ItemStack> kinds() {
		return counts.entrySet().stream().filter(e -> e.getValue() > 0).map(e -> e.getKey().stack).toList();
	}

	public boolean isEmpty() {
		return counts.values().stream().allMatch(count -> count <= 0);
	}

	public KindCounts copy() {
		KindCounts copy = new KindCounts();
		copy.counts.putAll(counts);
		return copy;
	}

	@Override
	public int howMany(ItemStack stack) {
		return Math.max(0, get(stack));
	}

	@Override
	public void moved(ItemStack stack) {
		add(stack, -stack.getCount());
	}

	@Override
	public String toString() {
		StringBuilder out = new StringBuilder("{");
		counts.forEach((key, count) -> out.append(out.length() > 1 ? ", " : "").append(count).append(' ').append(key.stack.getItem()));
		return out.append('}').toString();
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
