package com.comedymonkey.createaddons.content.tradebot.trade;

import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Added to villagers by {@code VillagerMixin}: a trade with Trade Bot, as vanilla does it for a player (the offer's
 * uses, the villager's XP and the level-up it may earn), minus the parts that need a player: no XP orb (the XP is
 * returned for him to bank as Nuggets of Experience, D57) and no trade event with a missing player.
 */
public interface TradeBotCustomer {
	/** Does the trade's bookkeeping; returns the XP vanilla would have dropped as an orb (0 if the offer gives none). */
	int comedymonkeysaddons$botTrade(MerchantOffer offer);

	/** Whether it's about to go up a level (from a trade just made). */
	boolean comedymonkeysaddons$levellingUp();
}
