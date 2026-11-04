package com.comedymonkey.createaddons.content.tradebot.master;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.content.tradebot.master.GaugePlanner.Plan;
import com.comedymonkey.createaddons.content.tradebot.master.GaugePlanner.Reason;
import com.comedymonkey.createaddons.content.tradebot.master.GaugePlanner.Result;
import com.comedymonkey.createaddons.content.tradebot.master.GaugePlanner.Trader;
import com.comedymonkey.createaddons.content.tradebot.master.TradeNetwork.Gauge;
import com.comedymonkey.createaddons.content.tradebot.master.TradeNetwork.Store;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

/** The Trade Master's choice of gauge and his plan for it (design/specs/trade-master.md, "Filling a gauge"). */
class GaugePlannerTest {
	private static final BlockPos INPUT = new BlockPos(0, 0, 0), BOOKS = new BlockPos(2, 0, 0), EMERALDS = new BlockPos(4, 0, 0),
		OTHER = new BlockPos(6, 0, 0);

	@Test
	void theEmptiestGaugeByShareGoesFirst() {
		// 10 of 64 bookshelves is 16% full; 1 of 16 glass is 6%
		TradeNetwork network = network(store(BOOKS, List.of(gauge(BOOKS, Items.BOOKSHELF, 64, 0)), stack(Items.BOOKSHELF, 10)),
			store(OTHER, List.of(gauge(OTHER, Items.GLASS, 16, 0)), stack(Items.GLASS, 1)));
		List<Gauge> order = GaugePlanner.unmetEmptiestFirst(network);
		assertEquals(Items.GLASS, order.get(0).kind().getItem());
		assertEquals(Items.BOOKSHELF, order.get(1).kind().getItem());
	}

	@Test
	void hisOwnStockComesFirstAndNeverFromBelowAGauge() {
		// the input's 30 bookshelves, and 5 above the other output's own gauge of 20 (it holds 25)
		TradeNetwork network = network(store(INPUT, List.of(), stack(Items.BOOKSHELF, 30)),
			store(BOOKS, List.of(gauge(BOOKS, Items.BOOKSHELF, 64, 0)), stack(Items.BOOKSHELF, 10)),
			store(OTHER, List.of(gauge(OTHER, Items.BOOKSHELF, 20, 1)), stack(Items.BOOKSHELF, 25)));
		Plan plan = GaugePlanner.plan(network, List.of(), 0, 27, p -> false).plan();
		assertNotNull(plan);
		assertEquals(BOOKS, plan.gauge().container());
		assertEquals(35, plan.courier());
		assertEquals(30, plan.takes().get(INPUT).get(new ItemStack(Items.BOOKSHELF)));
		assertEquals(5, plan.takes().get(OTHER).get(new ItemStack(Items.BOOKSHELF)), "only what's above its gauge");
		assertEquals(35, plan.promised());
	}

	@Test
	void heNeverPaysAboveTheBasePrice() {
		TradeNetwork network = network(store(INPUT, List.of(), stack(Items.EMERALD, 64)),
			store(BOOKS, List.of(gauge(BOOKS, Items.BOOKSHELF, 16, 0))));
		// demand has pushed the price from 9 to 13 emeralds
		MerchantOffer dear = new MerchantOffer(new ItemCost(Items.EMERALD, 9), Optional.empty(), new ItemStack(Items.BOOKSHELF), 0, 12, 1, 0.05f, 9);
		Result result = GaugePlanner.plan(network, List.of(trader(1, dear)), 0, 27, p -> false);
		assertNull(result.plan());
		assertEquals(Reason.NOBODY_SELLS, result.problems().get(0).reason());
		// back at base price after a restock, he buys
		MerchantOffer restocked = new MerchantOffer(new ItemCost(Items.EMERALD, 9), Optional.empty(), new ItemStack(Items.BOOKSHELF), 0, 12, 1, 0.05f, 0);
		Plan plan = GaugePlanner.plan(network, List.of(trader(1, restocked)), 0, 27, p -> false).plan();
		assertNotNull(plan);
		assertEquals(7, plan.buy(), "64 emeralds buy 7 at 9 each");
		assertEquals(63, plan.takes().get(INPUT).get(new ItemStack(Items.EMERALD)));
	}

	@Test
	void heBuysTheCheapestFirstAndPaysFromPouchChestsThenTheMostPlentifulGoods() {
		// 64 glass wanted; one seller gives 4 for an emerald, another 2 for an emerald
		TradeNetwork network = network(
			store(INPUT, List.of(), stack(Items.EMERALD, 10), stack(Items.WHEAT, 64), stack(Items.WHEAT, 64), stack(Items.WHEAT, 64),
				stack(Items.COBBLESTONE, 64), stack(Items.COBBLESTONE, 64), stack(Items.COBBLESTONE, 64), stack(Items.COBBLESTONE, 64)),
			store(BOOKS, List.of(gauge(BOOKS, Items.GLASS, 64, 0))),
			// an emerald gauge of 8 holding 12: 4 spare
			store(EMERALDS, List.of(gauge(EMERALDS, Items.EMERALD, 8, 0)), stack(Items.EMERALD, 12)));
		Trader cheap = trader(5, new MerchantOffer(new ItemCost(Items.EMERALD, 1), Optional.empty(), new ItemStack(Items.GLASS, 4), 0, 16, 1, 0.05f, 0));
		Trader dear = trader(1, new MerchantOffer(new ItemCost(Items.EMERALD, 1), Optional.empty(), new ItemStack(Items.GLASS, 2), 0, 16, 1, 0.05f, 0));
		Trader farmer = trader(2, new MerchantOffer(new ItemCost(Items.WHEAT, 20), Optional.empty(), new ItemStack(Items.EMERALD), 0, 16, 1, 0.05f, 0));
		Trader mason = trader(3, new MerchantOffer(new ItemCost(Items.COBBLESTONE, 20), Optional.empty(), new ItemStack(Items.EMERALD), 0, 16, 1, 0.05f, 0));
		Plan plan = GaugePlanner.plan(network, List.of(cheap, dear, farmer, mason), 1, 27, p -> false).plan();
		assertNotNull(plan);
		assertEquals(64, plan.buy());
		assertEquals(List.of(cheap.id()), plan.sellers(), "the cheaper glass first; that's enough");
		// 16 emeralds: 1 from his pouch, 10 + 4 from his chests (never below the emerald gauge), 1 by selling cobblestone
		assertEquals(1, plan.fromPouch());
		assertEquals(10, plan.takes().get(INPUT).get(new ItemStack(Items.EMERALD)));
		assertEquals(4, plan.takes().get(EMERALDS).get(new ItemStack(Items.EMERALD)));
		assertEquals(List.of(new ItemStack(Items.COBBLESTONE).getItem()), plan.sellKinds().stream().map(ItemStack::getItem).toList(),
			"the most plentiful goods are sold first");
		assertEquals(20, plan.takes().get(INPUT).get(new ItemStack(Items.COBBLESTONE)));
		assertEquals(0, plan.takes().get(INPUT).get(new ItemStack(Items.WHEAT)));
		assertEquals(List.of(mason.id()), plan.buyers());
	}

	@Test
	void aSecondCostItemComesFromHisChestsOrTheTradeIsSkipped() {
		ItemStack book = new ItemStack(Items.BOOK);
		MerchantOffer bound = new MerchantOffer(new ItemCost(Items.EMERALD, 5), Optional.of(new ItemCost(Items.BOOK, 1)),
			new ItemStack(Items.WRITABLE_BOOK), 0, 12, 1, 0.05f, 0);
		TradeNetwork noBooks = network(store(INPUT, List.of(), stack(Items.EMERALD, 64)), store(BOOKS, List.of(gauge(BOOKS, Items.WRITABLE_BOOK, 3, 0))));
		Result without = GaugePlanner.plan(noBooks, List.of(trader(1, bound)), 0, 27, p -> false);
		assertNull(without.plan());
		assertEquals(Reason.CANT_PAY, without.problems().get(0).reason());
		TradeNetwork twoBooks = network(store(INPUT, List.of(), stack(Items.EMERALD, 64), book.copyWithCount(2)),
			store(BOOKS, List.of(gauge(BOOKS, Items.WRITABLE_BOOK, 3, 0))));
		Plan plan = GaugePlanner.plan(twoBooks, List.of(trader(1, bound)), 0, 27, p -> false).plan();
		assertNotNull(plan);
		assertEquals(2, plan.buy(), "two books, two trades");
		assertEquals(2, plan.takes().get(INPUT).get(book));
		assertEquals(10, plan.takes().get(INPUT).get(new ItemStack(Items.EMERALD)));
	}

	@Test
	void anEmeraldGaugeIsFilledBySellingAndClaimedGaugesAreLeftAlone() {
		TradeNetwork network = network(store(INPUT, List.of(), stack(Items.WHEAT, 64)),
			store(EMERALDS, List.of(gauge(EMERALDS, Items.EMERALD, 5, 0)), stack(Items.EMERALD, 2)),
			store(BOOKS, List.of(gauge(BOOKS, Items.BOOKSHELF, 64, 0))));
		Trader farmer = trader(2, new MerchantOffer(new ItemCost(Items.WHEAT, 20), Optional.empty(), new ItemStack(Items.EMERALD), 0, 16, 1, 0.05f, 0));
		// the bookshelves are emptier, but another Trade Master has them
		FactoryPanelPosition books = new FactoryPanelPosition(BOOKS, PanelSlot.values()[0]);
		Result result = GaugePlanner.plan(network, List.of(farmer), 0, 27, books::equals);
		Plan plan = result.plan();
		assertNotNull(plan);
		assertEquals(EMERALDS, plan.gauge().container());
		assertEquals(0, plan.buy());
		assertEquals(60, plan.takes().get(INPUT).get(new ItemStack(Items.WHEAT)), "3 lots for the 3 emeralds short");
		assertEquals(3, plan.promised());
		assertTrue(result.problems().stream().noneMatch(p -> p.gauge().container().equals(BOOKS)), "a claimed gauge isn't his problem");
	}

	@Test
	void gaugesOutOfRangeAreListed() {
		Gauge far = gauge(new BlockPos(200, 0, 0), Items.GLASS, 16, 0);
		Result result = GaugePlanner.plan(new TradeNetwork(List.of(), List.of(far)), List.of(), 0, 27, p -> false);
		assertNull(result.plan());
		assertEquals(Reason.OUT_OF_RANGE, result.problems().get(0).reason());
	}

	private static TradeNetwork network(Store... stores) {
		return new TradeNetwork(List.of(stores), List.of());
	}

	private static Store store(BlockPos pos, List<Gauge> gauges, ItemStack... contents) {
		return new Store(pos, gauges, List.of(contents));
	}

	private static Gauge gauge(BlockPos container, Item kind, int amount, int slot) {
		return new Gauge(new FactoryPanelPosition(container, PanelSlot.values()[slot]), container, new ItemStack(kind), amount);
	}

	private static ItemStack stack(Item item, int count) {
		return new ItemStack(item, count);
	}

	private static Trader trader(double distance, MerchantOffer offer) {
		return new Trader(UUID.randomUUID(), distance, 0, List.of(offer));
	}
}
