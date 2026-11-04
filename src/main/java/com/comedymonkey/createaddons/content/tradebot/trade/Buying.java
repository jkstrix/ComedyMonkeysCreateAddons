package com.comedymonkey.createaddons.content.tradebot.trade;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.tradebot.TradeBotEntity;

import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The Trade Master buying for a gauge (design/specs/trade-master.md D17, D32, P6, P7): offers that give exactly the
 * gauge's item, never above their base price, paid from his emerald pouch and then from his load (emeralds, and a
 * second cost item such as a book), the goods into his load. He buys while the gauge still wants some; the last trade
 * may overshoot, which goes into the gauge's chest too (D30).
 */
public final class Buying implements Deal {
	private final ItemStack kind;
	private int wanted;

	/** Buying {@code kind} (item and components, as the gauge counts it) until {@code wanted} have been bought. */
	public Buying(ItemStack kind, int wanted) {
		this.kind = kind.copyWithCount(1);
		this.wanted = wanted;
	}

	/** How many the gauge still wants from this trip. */
	public int wanted() {
		return wanted;
	}

	@Override
	@Nullable
	public MerchantOffer next(TradeBotEntity bot, AbstractVillager villager) {
		if (wanted <= 0)
			return null;
		int reputation = bot.reputationWith(villager.getUUID());
		for (MerchantOffer offer : villager.getOffers())
			if (gives(offer, kind) && BotPricing.withinBase(offer, reputation) && canPay(bot, offer, reputation) && hasRoom(bot.getLoad(), offer.getResult()))
				return offer;
		return null;
	}

	@Override
	public void make(TradeBotEntity bot, AbstractVillager villager, MerchantOffer offer) {
		pay(bot, offer.getItemCostA(), BotPricing.cost(offer, bot.reputationWith(villager.getUUID())));
		offer.getItemCostB().ifPresent(b -> pay(bot, b, b.count()));
		ItemStack goods = offer.getResult().copy();
		ItemHandlerHelper.insertItemStacked(bot.getLoad(), goods, false);
		wanted -= goods.getCount();
	}

	/** An offer he can buy {@code kind} from: still in stock, giving exactly that (P7). */
	public static boolean gives(MerchantOffer offer, ItemStack kind) {
		return BotPricing.sells(offer) && ItemStack.isSameItemSameComponents(offer.getResult(), kind);
	}

	/** What he has to pay {@code cost} with: his load's, and his pouch's emeralds. */
	public static long has(TradeBotEntity bot, ItemCost cost) {
		long count = cost.item().value() == Items.EMERALD ? bot.getEmeralds() : 0;
		ItemStackHandler load = bot.getLoad();
		for (int slot = 0; slot < load.getSlots(); slot++)
			if (cost.test(load.getStackInSlot(slot)))
				count += load.getStackInSlot(slot).getCount();
		return count;
	}

	private static boolean canPay(TradeBotEntity bot, MerchantOffer offer, int reputation) {
		int a = BotPricing.cost(offer, reputation);
		Optional<ItemCost> b = offer.getItemCostB();
		if (b.isPresent() && b.get().item().value() == offer.getItemCostA().item().value())
			return has(bot, offer.getItemCostA()) >= a + b.get().count();
		return has(bot, offer.getItemCostA()) >= a && (b.isEmpty() || has(bot, b.get()) >= b.get().count());
	}

	/** Emeralds from his pouch first, then from his load. */
	private static void pay(TradeBotEntity bot, ItemCost cost, int count) {
		int left = count;
		if (cost.item().value() == Items.EMERALD) {
			int fromPouch = (int) Math.min(left, bot.getEmeralds());
			bot.addEarnings(-fromPouch, 0);
			left -= fromPouch;
		}
		ItemStackHandler load = bot.getLoad();
		for (int slot = 0; slot < load.getSlots() && left > 0; slot++)
			if (cost.test(load.getStackInSlot(slot)))
				left -= load.extractItem(slot, left, false).getCount();
	}

	private static boolean hasRoom(ItemStackHandler load, ItemStack goods) {
		return ItemHandlerHelper.insertItemStacked(load, goods.copy(), true).isEmpty();
	}
}
