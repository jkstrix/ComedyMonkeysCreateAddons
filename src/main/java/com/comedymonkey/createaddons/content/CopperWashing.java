package com.comedymonkey.createaddons.content;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.WeatheringCopper;

/**
 * Bulk washing (a fan through water) ages copper a stage a pass, as rain would in time: copper to exposed, to weathered,
 * to oxidized, and there it stays. Anything the game counts as weathering copper, from any mod (Create's shingles and
 * tiles among them); waxed copper never weathers, so the wax keeps it as it is. A washing recipe for the same item
 * comes first.
 */
public final class CopperWashing {
	private CopperWashing() {}

	/** Every copper item that washing takes a stage on, with the item it becomes (for recipe viewers), by registry order. */
	public static java.util.List<java.util.Map.Entry<net.minecraft.world.item.Item, net.minecraft.world.item.Item>> stages() {
		java.util.List<java.util.Map.Entry<net.minecraft.world.item.Item, net.minecraft.world.item.Item>> out = new java.util.ArrayList<>();
		for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
			ItemStack next = washed(new ItemStack(item));
			if (next != null)
				out.add(java.util.Map.entry(item, next.getItem()));
		}
		return out;
	}

	/** The whole stack a stage on, or null if it isn't copper that can weather any further. */
	@Nullable
	public static ItemStack washed(ItemStack stack) {
		if (!(stack.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof WeatheringCopper copper))
			return null;
		return copper.getNext(item.getBlock().defaultBlockState())
			.map(next -> next.getBlock().asItem())
			.filter(next -> next != stack.getItem() && next != net.minecraft.world.item.Items.AIR)
			.map(next -> stack.transmuteCopy(next, stack.getCount()))
			.orElse(null);
	}
}
