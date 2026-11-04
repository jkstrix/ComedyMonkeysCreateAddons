package com.comedymonkey.createaddons.content.tradebot.master;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.content.VillagerSeats;
import com.comedymonkey.createaddons.content.tradebot.TradeBotEntity;
import com.comedymonkey.createaddons.content.tradebot.master.GaugePlanner.Plan;
import com.comedymonkey.createaddons.content.tradebot.master.TradeNetwork.Gauge;
import com.comedymonkey.createaddons.content.tradebot.master.TradeNetwork.Store;
import com.comedymonkey.createaddons.content.tradebot.move.TradeBotMotion.Route;
import com.comedymonkey.createaddons.content.tradebot.trade.Buying;
import com.comedymonkey.createaddons.content.tradebot.trade.Deal;
import com.comedymonkey.createaddons.content.tradebot.trade.TradeWork;
import com.comedymonkey.createaddons.content.tradebot.work.ContainerWork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A Trade Master's trip for one gauge, as {@link GaugePlanner} planned it (design/specs/trade-master.md, "Filling a
 * gauge"): he takes what the plan needs from his board chests (the item itself, payment, goods to sell), sells to raise
 * emeralds, buys, delivers into the gauge's chest (overshoot too, D30), then tidies up: what's left in his load back
 * where it came from, else into any of his board chests with room; spare emeralds to a chest with an emerald gauge,
 * else an input; Nuggets of Experience to a chest with a Nugget of Experience gauge, else he banks them (D30). With no
 * plan ({@link #putAway}) it's only the tidying up: home with something still in his load, he puts it away.
 */
public final class MasterErrand {
	public enum Stage { TAKE, SELL, BUY, DELIVER, TIDY, PUT_EMERALDS, PUT_NUGGETS, DONE }

	/** After a step: off to the next stop, asking again next tick (a route search is one a tick), or finished. */
	public enum Next { BUSY, LATER, DONE }

	private final TradeBotEntity bot;
	@Nullable
	private final Plan plan;
	private final TradeNetwork network;
	private Stage stage = Stage.TAKE;
	private final Deque<BlockPos> stops = new ArrayDeque<>();
	private final Map<BlockPos, KindCounts> takes = new LinkedHashMap<>();
	/** What he took from each chest, so what's left goes back there (D30). */
	private final Map<BlockPos, KindCounts> takenFrom = new LinkedHashMap<>();
	private final Deal selling;
	private final Buying buying;
	/** Villagers he's finished with this trip (traded out, called off, or out of reach). */
	private final Set<UUID> doneWith = new HashSet<>();
	private final Map<UUID, Set<Direction>> failedSides = new HashMap<>();
	private final Deque<BlockPos> tidy = new ArrayDeque<>();
	private boolean tidyStarted, tidyAnywhere, delivering;
	/**
	 * The visit he's on his way to, until it's done or he can't get there. Interrupted on the way (he stops to greet
	 * someone, glares, or hides from a monster, then carries on), he goes back to it rather than on to the next stop.
	 */
	@Nullable
	private Visit pending;

	private record Visit(BlockPos container, ContainerWork.Mode mode, ContainerWork.Wanted wanted) {}
	private int taken, trades, delivered, returned;
	/** Trades this trip that sold goods, and that bought for the gauge. */
	private int sold, bought;

	public MasterErrand(TradeBotEntity bot, @Nullable Plan plan, TradeNetwork network) {
		this.bot = bot;
		this.plan = plan;
		this.network = network;
		if (plan == null) {
			stage = Stage.TIDY;
			selling = Deal.selling(stack -> false);
			buying = new Buying(ItemStack.EMPTY, 0);
			return;
		}
		plan.takes().forEach((pos, counts) -> takes.put(pos, counts.copy()));
		stops.addAll(nearestFirst(new ArrayList<>(takes.keySet()), bot.position()));
		List<ItemStack> goods = plan.sellKinds();
		selling = Deal.selling(stack -> goods.stream().anyMatch(kind -> ItemStack.isSameItemSameComponents(kind, stack)));
		buying = new Buying(plan.gauge().kind(), plan.buy());
	}

	/** A trip just to put away what he came home with: any of his board chests with room, then his emeralds and nuggets. */
	public static MasterErrand putAway(TradeBotEntity bot, TradeNetwork network) {
		return new MasterErrand(bot, null, network);
	}

	@Nullable
	public Plan plan() {
		return plan;
	}

	/** The gauge he's filling; null on a trip to put things away. */
	@Nullable
	public Gauge gauge() {
		return plan == null ? null : plan.gauge();
	}

	public Stage stage() {
		return stage;
	}

	/** What's still on its way to the gauge (D24): the plan's promise until he's been to deliver. */
	public int promised() {
		return plan != null && (stage.ordinal() <= Stage.DELIVER.ordinal() || delivering) ? plan.promised() : 0;
	}

	/** The villagers this trip is planned around, while he still has trading to do (D54). */
	public List<UUID> villagersAhead() {
		if (plan == null || stage.ordinal() > Stage.BUY.ordinal())
			return List.of();
		List<UUID> ahead = new ArrayList<>();
		for (UUID id : plan.buyers())
			if (!doneWith.contains(id))
				ahead.add(id);
		for (UUID id : plan.sellers())
			if (!doneWith.contains(id) && !ahead.contains(id))
				ahead.add(id);
		return ahead;
	}

	public int taken() {
		return taken;
	}

	public int trades() {
		return trades;
	}

	public int delivered() {
		return delivered;
	}

	public int returned() {
		return returned;
	}

	/** The next stop, or {@link Next#DONE} once he's delivered and tidied up. */
	public Next next(ErrandHost host) {
		if (pending != null) {
			Visit interrupted = pending;
			pending = null;
			Next next = visit(host, interrupted.container(), interrupted.mode(), interrupted.wanted());
			if (next == Next.LATER)
				pending = interrupted;
			if (next != null)
				return next;
			delivering = false; // no way there any more: on with the rest
		}
		while (true) {
			switch (stage) {
				case TAKE -> {
					while (!stops.isEmpty() && !ContainerWork.full(bot.getLoad())) {
						BlockPos pos = stops.poll();
						KindCounts wanted = takes.get(pos);
						if (wanted == null || wanted.isEmpty())
							continue;
						Next next = visit(host, pos, ContainerWork.Mode.TAKE, wanted);
						if (next == Next.LATER)
							stops.addFirst(pos);
						if (next != null)
							return next;
					}
					stage = Stage.SELL;
				}
				case SELL -> {
					Next next = plan == null || plan.sellKinds().isEmpty() ? null : trade(host, plan.buyers(), selling);
					if (next != null)
						return next;
					stage = Stage.BUY;
				}
				case BUY -> {
					Next next = buying.wanted() <= 0 ? null : trade(host, plan.sellers(), buying);
					if (next != null)
						return next;
					stage = Stage.DELIVER;
				}
				case DELIVER -> {
					if (plan == null) {
						stage = Stage.TIDY;
						continue;
					}
					ItemStack kind = plan.gauge().kind();
					boolean emeralds = kind.is(Items.EMERALD);
					stage = Stage.TIDY;
					if (emeralds ? bot.getEmeralds() > 0 || carries(stack -> stack.is(Items.EMERALD)) : carries(same(kind))) {
						ContainerWork.Mode mode = emeralds ? ContainerWork.Mode.DELIVER_EMERALDS : ContainerWork.Mode.RETURN;
						Next next = visit(host, plan.gauge().container(), mode, ContainerWork.Wanted.all(same(kind)));
						if (next == Next.LATER)
							stage = Stage.DELIVER;
						delivering = next == Next.BUSY;
						if (next != null)
							return next;
					}
				}
				case TIDY -> {
					if (!tidyStarted) {
						tidyStarted = true;
						tidy.addAll(nearestFirst(new ArrayList<>(takenFrom.keySet()), bot.position()));
					}
					Next next = tidyStep(host);
					if (next != null)
						return next;
					stage = Stage.PUT_EMERALDS;
				}
				case PUT_EMERALDS -> {
					stage = Stage.PUT_NUGGETS;
					BlockPos home = emeraldHome();
					if (bot.getEmeralds() > 0 && home != null) {
						Next next = visit(host, home, ContainerWork.Mode.DELIVER_EMERALDS, stack -> 0);
						if (next == Next.LATER)
							stage = Stage.PUT_EMERALDS;
						if (next != null)
							return next;
					}
				}
				case PUT_NUGGETS -> {
					stage = Stage.DONE;
					BlockPos home = nuggetHome();
					if (bot.getXpBank() >= TradeBotEntity.XP_PER_NUGGET && home != null) {
						Next next = visit(host, home, ContainerWork.Mode.DELIVER_NUGGETS, stack -> 0);
						if (next == Next.LATER)
							stage = Stage.PUT_NUGGETS;
						if (next != null)
							return next;
					}
				}
				case DONE -> {
					return Next.DONE;
				}
			}
		}
	}

	/** A go at a container's done: what moved. */
	public void containerDone(ContainerWork work) {
		pending = null;
		switch (work.mode()) {
			case TAKE -> {
				taken += work.moved();
				KindCounts from = takenFrom.computeIfAbsent(work.visit().container(), pos -> new KindCounts());
				for (ItemStack stack : work.movedStacks())
					from.add(stack, stack.getCount());
			}
			case RETURN, DELIVER_EMERALDS -> {
				if (delivering) {
					delivered += work.moved();
					if (work.moved() > 0)
						credit(work.moved());
				} else
					returned += work.moved();
			}
			default -> returned += work.moved();
		}
		delivering = false;
	}

	/** He couldn't get to a container: it's skipped this trip (the gauge's chest too: what he bought is tidied away). */
	public void containerUnreachable() {
		pending = null;
		delivering = false;
	}

	/** A visit's trades are over; {@code again}: the villager would still trade, so he goes again rather than moving on. */
	public void tradeDone(TradeWork trade, boolean again) {
		trades += trade.trades();
		if (trade.deal() == buying)
			bought += trade.trades();
		else
			sold += trade.trades();
		if (!again)
			doneWith.add(trade.villager().getUUID());
	}

	/** The villager called it off, or wouldn't trade after all: someone else, and not this one again this trip (D46). */
	public void calledOff(UUID villager) {
		doneWith.add(villager);
	}

	/** He couldn't get to this side of a villager: another side next time it's tried. */
	public void sideFailed(UUID villager, Direction side) {
		failedSides.computeIfAbsent(villager, v -> new HashSet<>()).add(side);
	}

	/**
	 * He's filled a gauge (P14): his owner's advancements for it, for selling and buying for it in one trip ("Supply
	 * Chain"), and for an enchanted book ("Book Club").
	 */
	private void credit(int moved) {
		if (plan == null)
			return;
		bot.awardOwner(ModAdvancements.TRADE_MASTER_FILL, moved);
		if (sold > 0 && bought > 0)
			bot.awardOwner(ModAdvancements.TRADE_MASTER_SUPPLY_CHAIN, bought);
		if (bought > 0 && plan.gauge().kind().has(DataComponents.STORED_ENCHANTMENTS))
			bot.awardOwner(ModAdvancements.TRADE_MASTER_BOOK, bought);
	}

	// ---- steps

	@Nullable
	private Next visit(ErrandHost host, BlockPos container, ContainerWork.Mode mode, ContainerWork.Wanted wanted) {
		return switch (host.visit(container, mode, wanted)) {
			case PLANNED -> {
				pending = new Visit(container, mode, wanted);
				yield Next.BUSY;
			}
			case LATER -> Next.LATER;
			case NONE -> null;
		};
	}

	/** The nearest of {@code villagers} still to visit who'd make the deal now; null when there's none left. */
	@Nullable
	private Next trade(ErrandHost host, List<UUID> villagers, Deal deal) {
		if (!(bot.level() instanceof ServerLevel level))
			return null;
		List<AbstractVillager> ahead = new ArrayList<>();
		for (UUID id : villagers)
			if (!doneWith.contains(id) && level.getEntity(id) instanceof AbstractVillager villager && villager.isAlive()
				&& VillagerSeats.sitting(villager) && deal.next(bot, villager) != null)
				ahead.add(villager);
			else
				doneWith.add(id);
		ahead.sort(Comparator.comparingDouble(villager -> villager.distanceToSqr(bot)));
		for (AbstractVillager villager : ahead) {
			Route route = host.trade(villager, deal, failedSides.getOrDefault(villager.getUUID(), Set.of()));
			if (route == Route.PLANNED)
				return Next.BUSY;
			if (route == Route.LATER)
				return Next.LATER;
			doneWith.add(villager.getUUID());
		}
		return null;
	}

	/** Anything left over after its own chests: any of his board chests with room, inputs first. */
	private boolean queueAnywhere() {
		if (tidyAnywhere)
			return false;
		tidyAnywhere = true;
		List<BlockPos> inputs = new ArrayList<>(), outputs = new ArrayList<>();
		for (Store store : network.stores)
			(store.input() ? inputs : outputs).add(store.pos);
		tidy.addAll(nearestFirst(inputs, bot.position()));
		tidy.addAll(nearestFirst(outputs, bot.position()));
		return !tidy.isEmpty();
	}

	@Nullable
	private Next tidyStep(ErrandHost host) {
		while (!isEmpty(bot.getLoad())) {
			if (tidy.isEmpty() && !queueAnywhere())
				return null; // every chest is full: he keeps the rest for next time
			BlockPos pos = tidy.poll();
			if (pos == null)
				return null;
			Predicate<ItemStack> which = tidyAnywhere ? stack -> true : takenFromHere(pos);
			if (!carries(which))
				continue;
			Next next = visit(host, pos, ContainerWork.Mode.RETURN, ContainerWork.Wanted.all(which));
			if (next == Next.LATER)
				tidy.addFirst(pos);
			if (next != null)
				return next;
		}
		return null;
	}

	private Predicate<ItemStack> takenFromHere(BlockPos pos) {
		KindCounts from = takenFrom.getOrDefault(pos, new KindCounts());
		return stack -> from.get(stack) > 0;
	}

	/** Where spare emeralds go: the nearest chest with an emerald gauge, else the nearest input. */
	@Nullable
	private BlockPos emeraldHome() {
		BlockPos gauged = nearestWithGauge(stack -> stack.is(Items.EMERALD));
		if (gauged != null)
			return gauged;
		List<BlockPos> inputs = network.stores.stream().filter(Store::input).map(store -> store.pos).toList();
		return inputs.isEmpty() ? null : nearestFirst(new ArrayList<>(inputs), bot.position()).get(0);
	}

	/** Where Nuggets of Experience go: only a chest with a Nugget of Experience gauge (D30). */
	@Nullable
	private BlockPos nuggetHome() {
		var nugget = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "experience_nugget"));
		return nearestWithGauge(stack -> stack.is(nugget));
	}

	@Nullable
	private BlockPos nearestWithGauge(Predicate<ItemStack> kind) {
		List<BlockPos> found = new ArrayList<>();
		for (Gauge gauge : network.gauges())
			if (kind.test(gauge.kind()) && !found.contains(gauge.container()))
				found.add(gauge.container());
		return found.isEmpty() ? null : nearestFirst(found, bot.position()).get(0);
	}

	private boolean carries(Predicate<ItemStack> which) {
		ItemStackHandler load = bot.getLoad();
		for (int slot = 0; slot < load.getSlots(); slot++)
			if (!load.getStackInSlot(slot).isEmpty() && which.test(load.getStackInSlot(slot)))
				return true;
		return false;
	}

	private static Predicate<ItemStack> same(ItemStack kind) {
		return stack -> ItemStack.isSameItemSameComponents(stack, kind);
	}

	private static boolean isEmpty(ItemStackHandler load) {
		for (int slot = 0; slot < load.getSlots(); slot++)
			if (!load.getStackInSlot(slot).isEmpty())
				return false;
		return true;
	}

	/** Orders containers so each next one is the nearest to the last. */
	private static List<BlockPos> nearestFirst(List<BlockPos> containers, Vec3 from) {
		List<BlockPos> ordered = new ArrayList<>();
		Vec3 at = from;
		while (!containers.isEmpty()) {
			Vec3 here = at;
			BlockPos nearest = containers.stream().min(Comparator.comparingDouble(c -> c.getCenter().distanceToSqr(here))).orElseThrow();
			containers.remove(nearest);
			ordered.add(nearest);
			at = nearest.getCenter();
		}
		return ordered;
	}
}
