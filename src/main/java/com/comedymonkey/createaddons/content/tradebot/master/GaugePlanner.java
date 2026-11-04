package com.comedymonkey.createaddons.content.tradebot.master;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.tradebot.master.TradeNetwork.Gauge;
import com.comedymonkey.createaddons.content.tradebot.master.TradeNetwork.Store;
import com.comedymonkey.createaddons.content.tradebot.trade.BotPricing;
import com.comedymonkey.createaddons.content.tradebot.trade.Buying;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Which gauge the Trade Master fills next, and how (design/specs/trade-master.md, "Picking a job" and "Filling a
 * gauge"). Pure planning: it works on a {@link TradeNetwork} as read and the traders he could reach, so the rules can
 * be tested away from the world.
 * <ul>
 * <li>Unmet gauges only, the emptiest first by share of its amount (D15, D24, P4); none another Trade Master has
 * claimed (P8).</li>
 * <li>Courier first: the item from his other board chests, above their own gauges (D35, D36).</li>
 * <li>Then buying the rest from seated sellers, never above base price, the cheapest per item first, then the nearest
 * (D32, P4), a second cost item (a book) from his chests too (P6).</li>
 * <li>Paid from his pouch, then emeralds in his chests above any emerald gauge (D36), then by selling what his chests
 * hold the most of, above their gauges (D22, P5). An emerald gauge is filled the same way, without buying.</li>
 * </ul>
 * Nothing is ever planned out of a chest below a gauge's amount (D23, D36). Gauges it can't fill get a reason (P3).
 */
public final class GaugePlanner {
	/** A seated villager or wandering trader he can get to: how far from his station, his reputation with it, its offers. */
	public record Trader(UUID id, double distance, int reputation, List<MerchantOffer> offers) {}

	/** Why a gauge isn't being filled (P3). */
	public enum Reason { OUT_OF_RANGE, NOBODY_SELLS, CANT_PAY }

	public record Problem(Gauge gauge, Reason reason) {}

	/**
	 * A trip for one gauge: what to take from which chests (the item itself, payment, goods to sell), the goods to
	 * sell and to whom, how many to buy and from whom, and what that promises the gauge (D24).
	 */
	public record Plan(Gauge gauge, Map<BlockPos, KindCounts> takes, int courier, List<ItemStack> sellKinds, List<UUID> buyers,
		int buy, List<UUID> sellers, int fromPouch, int promised) {}

	public record Result(@Nullable Plan plan, List<Problem> problems) {}

	/** Room left for what he takes and buys: a few slots stay free for payment and odd stacks. */
	private static final int SPARE_SLOTS = 3;

	private GaugePlanner() {}

	/**
	 * The next trip: the emptiest unmet gauge he can do something for. {@code claimed} tells gauges another Trade Master
	 * is filling; {@code pouch} is his emeralds; {@code freeSlots} the empty stacks in his load.
	 */
	public static Result plan(TradeNetwork network, List<Trader> traders, long pouch, int freeSlots, Predicate<FactoryPanelPosition> claimed) {
		List<Problem> problems = new ArrayList<>();
		for (Gauge gauge : network.outOfRange)
			problems.add(new Problem(gauge, Reason.OUT_OF_RANGE));
		Plan plan = null;
		for (Gauge gauge : unmetEmptiestFirst(network)) {
			if (claimed.test(gauge.panel()))
				continue;
			Reason[] why = new Reason[1];
			Plan candidate = planFor(network, gauge, traders, pouch, freeSlots, why);
			if (candidate != null && plan == null)
				plan = candidate;
			else if (candidate == null && why[0] != null)
				problems.add(new Problem(gauge, why[0]));
		}
		return new Result(plan, problems);
	}

	/** The gauges below their amount, the emptiest first by share of its amount (P4). */
	public static List<Gauge> unmetEmptiestFirst(TradeNetwork network) {
		List<Gauge> unmet = new ArrayList<>();
		for (Gauge gauge : network.gauges())
			if (level(network, gauge) < gauge.amount())
				unmet.add(gauge);
		unmet.sort(Comparator.comparingDouble(gauge -> (double) level(network, gauge) / gauge.amount()));
		return unmet;
	}

	private static int level(TradeNetwork network, Gauge gauge) {
		Store store = network.store(gauge.container());
		return store == null ? 0 : store.count(gauge.kind());
	}

	/** The trip for {@code gauge}, or null with {@code why[0]} set when there's nothing he can do for it now. */
	@Nullable
	static Plan planFor(TradeNetwork read, Gauge gauge, List<Trader> traders, long pouch, int freeSlots, Reason[] why) {
		TradeNetwork network = read.copy();
		ItemStack kind = gauge.kind();
		boolean emeralds = kind.is(Items.EMERALD);
		Map<BlockPos, KindCounts> takes = new LinkedHashMap<>();
		int shortfall = gauge.amount() - level(network, gauge);
		int room = Math.max(0, freeSlots - SPARE_SLOTS) * kind.getMaxStackSize();

		// 1. courier: what his other board chests hold above their own gauges (D35, D36)
		int courier = takeSpare(network, gauge.container(), kind, Math.min(shortfall, room), takes);
		int rest = shortfall - courier;
		room -= courier;

		// 2. emeralds he'll have: his pouch, then his chests' above any emerald gauge, then what selling could raise
		long budget = pouch + spare(network, gauge.container(), new ItemStack(Items.EMERALD)) + raisable(network, traders, kind);
		int fromPouch = 0;
		int bought = 0;
		Set<UUID> sellers = new LinkedHashSet<>();
		long emeraldsNeeded;
		List<ItemStack> payments = new ArrayList<>();
		if (emeralds) {
			// an emerald gauge: his pouch first, then selling (D22)
			fromPouch = (int) Math.min(pouch, rest);
			emeraldsNeeded = rest - fromPouch;
		} else {
			// 3. buying the rest, never above base price, the cheapest per item first, then the nearest (D32, P4)
			List<Candidate> candidates = candidates(traders, kind);
			if (candidates.isEmpty() && courier == 0) {
				why[0] = Reason.NOBODY_SELLS;
				return null;
			}
			int toBuy = Math.min(rest, room);
			long spent = 0;
			for (Candidate c : candidates) {
				if (bought >= toBuy)
					break;
				int perTrade = c.offer.getResult().getCount();
				int trades = Math.min(c.offer.getMaxUses() - c.offer.getUses(), ceil(toBuy - bought, perTrade));
				// paid for: emeralds within the budget, other cost items from his chests (P6)
				trades = Math.min(trades, (int) Math.min(Integer.MAX_VALUE, (budget - spent) / Math.max(1, c.emeraldsPerTrade())));
				for (ItemCost cost : c.itemCosts())
					trades = Math.min(trades, spare(network, null, stack(cost)) / cost.count());
				if (trades <= 0)
					continue;
				for (ItemCost cost : c.itemCosts()) {
					takeSpare(network, null, stack(cost), trades * cost.count(), takes);
					payments.add(stack(cost));
				}
				spent += (long) trades * c.emeraldsPerTrade();
				bought += trades * perTrade;
				sellers.add(c.trader.id());
			}
			if (bought == 0 && courier == 0) {
				why[0] = Reason.CANT_PAY;
				return null;
			}
			emeraldsNeeded = spent;
		}

		// 4. paying: the pouch, then emeralds above any emerald gauge, then selling what his chests hold the most of (D22, D36, P5)
		long need = emeraldsNeeded - (emeralds ? 0 : Math.min(pouch, emeraldsNeeded));
		if (!emeralds)
			fromPouch = (int) Math.min(pouch, emeraldsNeeded);
		long fromChests = takeSpare(network, emeralds ? gauge.container() : null, new ItemStack(Items.EMERALD), (int) Math.min(Integer.MAX_VALUE, need), takes);
		need -= fromChests;
		List<ItemStack> sellKinds = new ArrayList<>();
		Set<UUID> buyers = new LinkedHashSet<>();
		long raised = 0;
		if (need > 0) {
			List<ItemStack> keep = new ArrayList<>(payments);
			keep.add(kind);
			raised = sell(network, traders, keep, need, takes, sellKinds, buyers);
		}
		// what the gauge can expect from this trip (D24): an emerald gauge gets every emerald he raises
		int promised = emeralds ? (int) Math.min(shortfall, courier + fromPouch + fromChests + raised) : courier + bought;
		if (promised <= 0 && sellKinds.isEmpty() && takes.isEmpty()) {
			why[0] = emeralds ? Reason.CANT_PAY : Reason.NOBODY_SELLS;
			return null;
		}
		return new Plan(gauge, takes, courier, sellKinds, List.copyOf(buyers), bought, List.copyOf(sellers), fromPouch, promised);
	}

	/** A trader's offer giving the gauge's item, at its price for him now. */
	private record Candidate(Trader trader, MerchantOffer offer, int price) {
		/** What one trade costs in emeralds (cost A, and cost B if that's emeralds too). */
		int emeraldsPerTrade() {
			int emeralds = isEmerald(offer.getItemCostA()) ? price : 0;
			Optional<ItemCost> b = offer.getItemCostB();
			return emeralds + (b.isPresent() && isEmerald(b.get()) ? b.get().count() : 0);
		}

		/** The costs paid in other items (the book for an enchanted book, P6), cost A at his price. */
		List<ItemCost> itemCosts() {
			List<ItemCost> costs = new ArrayList<>();
			if (!isEmerald(offer.getItemCostA()))
				costs.add(new ItemCost(offer.getItemCostA().item(), price, offer.getItemCostA().components()));
			offer.getItemCostB().filter(b -> !isEmerald(b)).ifPresent(costs::add);
			return costs;
		}

		double perItem() {
			return (double) (price + offer.getItemCostB().map(ItemCost::count).orElse(0)) / offer.getResult().getCount();
		}
	}

	/** Every in-stock offer giving {@code kind}, never above base price (D32): the cheapest per item first, then the nearest (P4). */
	private static List<Candidate> candidates(List<Trader> traders, ItemStack kind) {
		List<Candidate> candidates = new ArrayList<>();
		for (Trader trader : traders)
			for (MerchantOffer offer : trader.offers())
				if (Buying.gives(offer, kind) && BotPricing.withinBase(offer, trader.reputation()))
					candidates.add(new Candidate(trader, offer, BotPricing.cost(offer, trader.reputation())));
		candidates.sort(Comparator.comparingDouble(Candidate::perItem).thenComparingDouble(c -> c.trader.distance()));
		return candidates;
	}

	/**
	 * Sells for {@code need} emeralds: what his chests hold the most of first (P5), never {@code keep}'s kinds (the
	 * gauge's item, payment), to the nearest buyers, in whole lots.
	 */
	private static long sell(TradeNetwork network, List<Trader> traders, List<ItemStack> keep, long need, Map<BlockPos, KindCounts> takes,
		List<ItemStack> sellKinds, Set<UUID> buyers) {
		long raised = 0;
		for (ItemStack goods : mostPlentifulFirst(network, keep)) {
			if (raised >= need)
				break;
			for (Trader trader : nearestFirst(traders))
				for (MerchantOffer offer : trader.offers()) {
					if (raised >= need || !BotPricing.buys(offer) || !offer.getItemCostA().test(goods))
						continue;
					int price = BotPricing.cost(offer, trader.reputation());
					int perLot = offer.getResult().getCount();
					int lots = Math.min(offer.getMaxUses() - offer.getUses(), spare(network, null, goods) / price);
					lots = (int) Math.min(lots, ceil(need - raised, perLot));
					if (lots <= 0)
						continue;
					takeSpare(network, null, goods, lots * price, takes);
					raised += (long) lots * perLot;
					buyers.add(trader.id());
					if (sellKinds.stream().noneMatch(k -> ItemStack.isSameItemSameComponents(k, goods)))
						sellKinds.add(goods);
				}
		}
		return raised;
	}

	/** The most emeralds selling could raise, from everything above its gauges but the gauge's own item. */
	private static long raisable(TradeNetwork read, List<Trader> traders, ItemStack kind) {
		return sell(read.copy(), traders, List.of(kind), Long.MAX_VALUE / 2, new LinkedHashMap<>(), new ArrayList<>(), new LinkedHashSet<>());
	}

	/** The kinds his chests hold above their gauges, the most plentiful first (P5), without emeralds or {@code keep}'s. */
	public static List<ItemStack> mostPlentifulFirst(TradeNetwork network, List<ItemStack> keep) {
		KindCounts spare = new KindCounts();
		for (Store store : network.stores)
			for (ItemStack kind : store.kinds())
				if (!kind.is(Items.EMERALD) && keep.stream().noneMatch(k -> ItemStack.isSameItemSameComponents(k, kind)))
					spare.add(kind, store.spare(kind));
		List<ItemStack> kinds = new ArrayList<>(spare.kinds());
		kinds.sort(Comparator.comparingInt(spare::get).reversed());
		return kinds;
	}

	/** The spare {@code kind} in his chests (all but {@code except}'s). */
	private static int spare(TradeNetwork network, @Nullable BlockPos except, ItemStack kind) {
		int spare = 0;
		for (Store store : network.stores)
			if (!store.pos.equals(except))
				spare += store.spare(kind);
		return spare;
	}

	/** Plans taking up to {@code count} spare {@code kind} from his chests (not {@code except}), reserving it; returns how many. */
	private static int takeSpare(TradeNetwork network, @Nullable BlockPos except, ItemStack kind, int count, Map<BlockPos, KindCounts> takes) {
		int taken = 0;
		for (Store store : network.stores) {
			if (taken >= count)
				break;
			if (store.pos.equals(except))
				continue;
			int n = Math.min(store.spare(kind), count - taken);
			if (n <= 0)
				continue;
			store.reserve(kind, n);
			takes.computeIfAbsent(store.pos, p -> new KindCounts()).add(kind, n);
			taken += n;
		}
		return taken;
	}

	private static List<Trader> nearestFirst(List<Trader> traders) {
		List<Trader> sorted = new ArrayList<>(traders);
		sorted.sort(Comparator.comparingDouble(Trader::distance));
		return sorted;
	}

	private static boolean isEmerald(ItemCost cost) {
		return cost.item().value() == Items.EMERALD;
	}

	private static ItemStack stack(ItemCost cost) {
		return cost.itemStack().copyWithCount(1);
	}

	private static int ceil(long amount, int per) {
		return (int) Math.min(Integer.MAX_VALUE, (amount + per - 1) / per);
	}
}
