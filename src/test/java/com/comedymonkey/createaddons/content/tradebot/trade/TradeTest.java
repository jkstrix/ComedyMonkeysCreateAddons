package com.comedymonkey.createaddons.content.tradebot.trade;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

/** Trade Bot's prices, the offers he sells into, what he takes for a trip, and who's claimed which villager. */
class TradeTest {
	/** goods × count for 1 emerald, like a farmer's 20 wheat. */
	private static MerchantOffer sell(ItemCost goods, int uses, int maxUses, int demand) {
		return new MerchantOffer(goods, Optional.empty(), new ItemStack(Items.EMERALD), uses, maxUses, 2, 0.05f, demand);
	}

	@AfterEach
	void forgetClaims() {
		VillagerClaims.clear();
	}

	@Test
	void pricesAreVanillasWithHisOwnReputationsDiscount() {
		MerchantOffer wheat = sell(new ItemCost(Items.WHEAT, 20), 0, 16, 0);
		assertEquals(20, BotPricing.cost(wheat, 0));
		assertEquals(19, BotPricing.cost(wheat, 20), "20 reputation × 0.05 takes one off");
		assertEquals(19, BotPricing.cost(wheat, 39), "the discount is rounded down");
		assertEquals(20, BotPricing.cost(wheat, -10), "reputation is never negative (D42)");
		// demand raises it the way vanilla does: floor(count × demand × multiplier)
		assertEquals(20 + 5, BotPricing.cost(sell(new ItemCost(Items.WHEAT, 20), 0, 16, 5), 0));
		assertEquals(wheat.getCostA().getCount(), BotPricing.cost(wheat, 0), "no reputation: what a stranger pays");
	}

	@Test
	void aPriceIsAtLeastOneAndAtMostAStack() {
		MerchantOffer cheap = new MerchantOffer(new ItemCost(Items.WHEAT, 1), Optional.empty(), new ItemStack(Items.EMERALD), 0, 16, 2, 1f, 0);
		assertEquals(1, BotPricing.cost(cheap, BotPricing.MAX_REPUTATION));
		MerchantOffer dear = new MerchantOffer(new ItemCost(Items.WHEAT, 60), Optional.empty(), new ItemStack(Items.EMERALD), 0, 16, 2, 0.2f, 10);
		assertEquals(64, BotPricing.cost(dear, 0));
	}

	@Test
	void heOnlySellsOneKindOfGoodsForEmeraldsInStock() {
		assertTrue(BotPricing.buys(sell(new ItemCost(Items.WHEAT, 20), 0, 16, 0)));
		assertFalse(BotPricing.buys(sell(new ItemCost(Items.WHEAT, 20), 16, 16, 0)), "out of stock");
		assertFalse(BotPricing.buys(new MerchantOffer(new ItemCost(Items.EMERALD, 1), Optional.empty(), new ItemStack(Items.BREAD, 6), 0, 16, 2, 0.05f, 0)),
			"the villager selling to him");
		assertFalse(BotPricing.buys(new MerchantOffer(new ItemCost(Items.BOOK, 1), Optional.of(new ItemCost(Items.WHEAT, 2)),
			new ItemStack(Items.EMERALD), 0, 16, 2, 0.05f, 0)), "two kinds of goods");
		assertFalse(BotPricing.buys(new MerchantOffer(new ItemCost(Items.WHEAT, 20), Optional.empty(), new ItemStack(Items.BREAD), 0, 16, 2, 0.05f, 0)),
			"paid in something other than emeralds");
	}

	@Test
	void reputationGrowsByTwoAndStopsAtTwentyFive() {
		int reputation = 0;
		for (int i = 0; i < 12; i++)
			reputation = BotPricing.raise(reputation);
		assertEquals(24, reputation);
		assertEquals(25, BotPricing.raise(reputation));
		assertEquals(25, BotPricing.raise(25));
		assertEquals(2, BotPricing.raise(-4), "never negative");
	}

	@Test
	void heTakesOnlyWhatTheBuyersWillTake() {
		TradeDemand demand = new TradeDemand();
		demand.add(sell(new ItemCost(Items.WHEAT, 20), 13, 16, 0), 0); // three trades left: 60 wheat
		demand.add(sell(new ItemCost(Items.PAPER, 24), 16, 16, 0), 0); // sold out: nothing
		demand.add(sell(new ItemCost(Items.WHEAT, 20), 15, 16, 0), 0); // a second farmer, one trade left: 20 more
		assertEquals(80, demand.howMany(new ItemStack(Items.WHEAT, 64)));
		assertEquals(0, demand.howMany(new ItemStack(Items.PAPER, 64)));
		assertEquals(0, demand.howMany(new ItemStack(Items.COBBLESTONE, 64)), "nobody buys it: it stays in the input (D22)");
		demand.moved(new ItemStack(Items.WHEAT, 64));
		assertEquals(16, demand.howMany(new ItemStack(Items.WHEAT, 64)));
		demand.moved(new ItemStack(Items.WHEAT, 16));
		assertEquals(0, demand.howMany(new ItemStack(Items.WHEAT, 64)));
		assertTrue(demand.isEmpty());
	}

	@Test
	void firstComeFirstServedAndClaimsLapse() {
		UUID villager = UUID.randomUUID(), first = UUID.randomUUID(), second = UUID.randomUUID();
		assertTrue(VillagerClaims.claim(villager, first, 100));
		assertFalse(VillagerClaims.claim(villager, second, 101), "the second bot picks another buyer (D54)");
		assertTrue(VillagerClaims.claim(villager, first, 150), "renewed by its own bot");
		assertTrue(VillagerClaims.claimedByOther(villager, second, 150 + VillagerClaims.HOLD_TICKS - 1));
		assertFalse(VillagerClaims.claimedByOther(villager, second, 150 + VillagerClaims.HOLD_TICKS), "not renewed: it lapses");
		VillagerClaims.release(villager, second);
		assertTrue(VillagerClaims.claimedByOther(villager, second, 160), "only its own bot releases it");
		VillagerClaims.releaseAll(first);
		assertTrue(VillagerClaims.claim(villager, second, 160));
	}
}
