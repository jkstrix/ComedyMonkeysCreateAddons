package com.comedymonkey.createaddons.content.tradebot.trade;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * What Trade Bot pays and which offers he sells into. Prices are the villager's real ones (D17): the base cost plus
 * demand, as vanilla works them out, less a discount from his own reputation with that villager the way a player's
 * reputation discounts theirs (D34). His reputation grows by vanilla's trading gossip step and stops at its cap (D42).
 */
public final class BotPricing {
	/** Vanilla's trading gossip: +2 per trade, capped at 25, never negative. */
	public static final int REPUTATION_PER_TRADE = 2;
	public static final int MAX_REPUTATION = 25;

	private BotPricing() {}

	/** How many of the offer's goods he gives for one trade, with {@code reputation} (at least 1, at most a stack). */
	public static int cost(MerchantOffer offer, int reputation) {
		ItemStack base = offer.getBaseCostA();
		int count = base.getCount();
		int demand = Math.max(0, Mth.floor((float) (count * offer.getDemand()) * offer.getPriceMultiplier()));
		int discount = Mth.floor(Math.max(0, reputation) * offer.getPriceMultiplier());
		return Mth.clamp(count + demand - discount, 1, base.getMaxStackSize());
	}

	/** An offer he can sell into: one kind of goods for emeralds, still in stock. */
	public static boolean buys(MerchantOffer offer) {
		return !offer.isOutOfStock() && offer.getResult().is(Items.EMERALD) && offer.getItemCostB().isEmpty()
			&& !offer.getBaseCostA().is(Items.EMERALD);
	}

	/** His reputation after one more trade. */
	public static int raise(int reputation) {
		return Math.min(MAX_REPUTATION, Math.max(0, reputation) + REPUTATION_PER_TRADE);
	}
}
