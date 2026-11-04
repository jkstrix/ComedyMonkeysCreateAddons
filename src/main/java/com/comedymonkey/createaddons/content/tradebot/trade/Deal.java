package com.comedymonkey.createaddons.content.tradebot.trade;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.tradebot.TradeBotEntity;

import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * What a visit's trades are: selling goods he carries for emeralds (Trade Bot's only kind), or buying (the Trade
 * Master's, {@link Buying}). {@link TradeWork} runs the clip and the bookkeeping either way.
 */
public interface Deal {
	/** Selling anything he carries that a villager buys: Trade Bot's trades. */
	Deal SELL_ANYTHING = selling(stack -> true);

	/** The offer he'd make next with this villager, with what he carries now; null when there's none. */
	@Nullable
	MerchantOffer next(TradeBotEntity bot, AbstractVillager villager);

	/** Pays for {@code offer} (at his price) and takes what it gives. */
	void make(TradeBotEntity bot, AbstractVillager villager, MerchantOffer offer);

	/** Selling only the goods {@code goods} accepts, for emeralds into his pouch (D48). */
	static Deal selling(Predicate<ItemStack> goods) {
		return new Deal() {
			@Override
			@Nullable
			public MerchantOffer next(TradeBotEntity bot, AbstractVillager villager) {
				int reputation = bot.reputationWith(villager.getUUID());
				for (MerchantOffer offer : villager.getOffers())
					if (BotPricing.buys(offer) && carried(bot.getLoad(), offer, goods) >= BotPricing.cost(offer, reputation))
						return offer;
				return null;
			}

			@Override
			public void make(TradeBotEntity bot, AbstractVillager villager, MerchantOffer offer) {
				int left = BotPricing.cost(offer, bot.reputationWith(villager.getUUID()));
				ItemStackHandler load = bot.getLoad();
				for (int slot = 0; slot < load.getSlots() && left > 0; slot++) {
					ItemStack stack = load.getStackInSlot(slot);
					if (offer.getItemCostA().test(stack) && goods.test(stack))
						left -= load.extractItem(slot, left, false).getCount();
				}
				bot.addEarnings(offer.getResult().getCount(), 0);
			}
		};
	}

	private static int carried(ItemStackHandler load, MerchantOffer offer, Predicate<ItemStack> goods) {
		int count = 0;
		for (int slot = 0; slot < load.getSlots(); slot++) {
			ItemStack stack = load.getStackInSlot(slot);
			if (offer.getItemCostA().test(stack) && goods.test(stack))
				count += stack.getCount();
		}
		return count;
	}
}
