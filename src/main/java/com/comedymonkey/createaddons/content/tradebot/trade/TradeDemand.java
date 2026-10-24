package com.comedymonkey.createaddons.content.tradebot.trade;

import java.util.ArrayList;
import java.util.List;

import com.comedymonkey.createaddons.content.tradebot.work.ContainerWork;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * What this trip's buyers will take off him: for each offer he can sell into, its goods times the trades it has left.
 * He takes only that from his inputs, so goods nobody buys stay where they are (D22). Each item taken (or already
 * carried) counts against the first offer it suits.
 */
public final class TradeDemand implements ContainerWork.Wanted {
	private static final class Entry {
		final ItemCost goods;
		int left;

		Entry(ItemCost goods, int left) {
			this.goods = goods;
			this.left = left;
		}
	}

	private final List<Entry> entries = new ArrayList<>();

	/** A buyer's offer, at the price he'd pay now, for every trade it has left. */
	public void add(MerchantOffer offer, int reputation) {
		if (BotPricing.buys(offer))
			add(offer.getItemCostA(), BotPricing.cost(offer, reputation) * (offer.getMaxUses() - offer.getUses()));
	}

	public void add(ItemCost goods, int count) {
		if (count > 0)
			entries.add(new Entry(goods, count));
	}

	@Override
	public int howMany(ItemStack stack) {
		int total = 0;
		for (Entry entry : entries)
			if (entry.goods.test(stack))
				total += entry.left;
		return total;
	}

	@Override
	public void moved(ItemStack stack) {
		int count = stack.getCount();
		for (Entry entry : entries) {
			if (count <= 0)
				return;
			if (entry.goods.test(stack)) {
				int used = Math.min(count, entry.left);
				entry.left -= used;
				count -= used;
			}
		}
	}

	public boolean isEmpty() {
		return entries.stream().allMatch(entry -> entry.left <= 0);
	}
}
