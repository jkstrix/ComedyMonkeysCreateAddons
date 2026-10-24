package com.comedymonkey.createaddons.content.tradebot.trade;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

import com.comedymonkey.createaddons.content.tradebot.work.ContainerWork;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * What this trip's buyers will take off him: for each offer he can sell into, its price times the trades it has left,
 * in whole lots only, as many as the goods there are can pay for ({@link #limit}). He takes only that from his inputs,
 * so goods nobody buys, and odd amounts short of a lot, stay where they are (D22). Each item taken (or already carried)
 * counts against the first offer it suits.
 */
public final class TradeDemand implements ContainerWork.Wanted {
	private static final class Entry {
		final ItemCost goods;
		final int price;
		int trades;
		int left;

		Entry(ItemCost goods, int price, int trades) {
			this.goods = goods;
			this.price = price;
			this.trades = trades;
			this.left = price * trades;
		}
	}

	private final List<Entry> entries = new ArrayList<>();

	/** A buyer's offer, at the price he'd pay now, for every trade it has left. */
	public void add(MerchantOffer offer, int reputation) {
		if (BotPricing.buys(offer))
			add(offer.getItemCostA(), BotPricing.cost(offer, reputation), offer.getMaxUses() - offer.getUses());
	}

	/** {@code trades} lots of {@code price} goods each. */
	public void add(ItemCost goods, int price, int trades) {
		if (price > 0 && trades > 0)
			entries.add(new Entry(goods, price, trades));
	}

	/**
	 * Only whole lots the goods there are can pay for: {@code available} counts the goods (in his inputs and load)
	 * suiting an offer; offers wanting the same item share them, first come first served.
	 */
	public void limit(ToIntFunction<ItemCost> available) {
		List<Entry> done = new ArrayList<>();
		for (Entry entry : entries) {
			int pool = available.applyAsInt(entry.goods);
			for (Entry before : done)
				if (before.goods.item().equals(entry.goods.item()))
					pool -= before.price * before.trades;
			entry.trades = Math.max(0, Math.min(entry.trades, pool / entry.price));
			entry.left = entry.price * entry.trades;
			done.add(entry);
		}
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
