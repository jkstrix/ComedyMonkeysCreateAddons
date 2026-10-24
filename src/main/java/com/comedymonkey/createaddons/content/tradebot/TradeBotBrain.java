package com.comedymonkey.createaddons.content.tradebot;

import java.util.ArrayDeque;
import java.util.ArrayList;
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
import org.joml.Vector3f;

import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipLibrary;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipRoot;
import com.comedymonkey.createaddons.content.tradebot.react.CornerFinder;
import com.comedymonkey.createaddons.content.tradebot.react.Reactions;
import com.comedymonkey.createaddons.content.tradebot.trade.Buyers;
import com.comedymonkey.createaddons.content.tradebot.trade.TradeDemand;
import com.comedymonkey.createaddons.content.tradebot.trade.TradeSpot;
import com.comedymonkey.createaddons.content.tradebot.trade.TradeWork;
import com.comedymonkey.createaddons.content.tradebot.trade.VillagerClaims;
import com.comedymonkey.createaddons.content.tradebot.work.BotWork;
import com.comedymonkey.createaddons.content.tradebot.work.ContainerVisit;
import com.comedymonkey.createaddons.content.tradebot.work.ContainerWork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

/**
 * What Trade Bot is doing (server): docked at home, leaving, out on a job, rolling home and docking. While docked he
 * checks every few seconds whether there's a trip worth making (D47, D52), and goes on his own. Wherever he is, he
 * heads home when his station's redstone pauses him, at night, in a thunderstorm, or when his charge only just covers
 * the way back (D3, D32, D38, D49). {@code /tradebot goto} sends him on test trips.
 */
public class TradeBotBrain {
	public static final String LEAVE = "action_comes_in";
	/** How long he waits at a trip's end before heading home (ticks). */
	private static final int WAIT_TICKS = 60;
	/** Homeward route failures before he's put back in his station anyway (so he can always be packed up). */
	private static final int MAX_HOME_FAILURES = 3;

	public enum Task {
		HOME, LEAVING, GOING, WAITING, APPROACHING, WORKING, SHRUGGING, WAITING_OUTPUT, RETURNING, DOCKING, STRANDED,
		/** A reaction clip (a player turning up, being hit, a good day's trading), then back to what he was doing. */
		REACTING,
		/** Afraid of a mob (D8): spots it, flees to a corner, hides, cowers till it's gone a while, recovers. */
		SCARED_SPOT, SCARED_FLEE, SCARED_HIDE, SCARED_COWER, SCARED_RECOVER,
		/** Run down (D24, D29) until he's wound up, then carries on. */
		WOUND_DOWN, WINDING_UP,
		/** On his spot, waiting for a player to finish trading with his villager (players come first, D46). */
		WAITING_TURN
	}
	/** Tasks a reaction or a fright may interrupt: travelling and waiting, never mid-container or mid-trade. */
	private static final java.util.EnumSet<Task> INTERRUPTIBLE = java.util.EnumSet.of(Task.GOING, Task.WAITING, Task.APPROACHING,
		Task.WAITING_OUTPUT, Task.RETURNING, Task.REACTING, Task.WAITING_TURN);
	/** How long he waits for a player to finish with his villager before shrugging and moving on (ticks). */
	private static final int TURN_WAIT_TICKS = 300;
	/** A player within this many blocks is noticed (D9), and greeted within this long if he's busy at the time. */
	private static final double SEES_PLAYER_RANGE = 6;
	private static final int GREET_WAIT_TICKS = 200;
	/** How far he flees to hide (blocks), how long the mob must be gone before he recovers, and how long he's calm after. */
	private static final int HIDE_RADIUS = 8, ALL_CLEAR_TICKS = 100, CALM_TICKS = 100, FLEE_TICKS = 200;
	/** How long he waits beside a full output before trying again (D22). */
	private static final int OUTPUT_RETRY_TICKS = 100;
	/** How often, docked, he checks for a trip worth making (ticks). */
	private static final int SCHEDULE_TICKS = 200;
	/** After a trading run that made no trade, how long he rests before looking again (ticks). */
	private static final int NO_TRADE_REST_TICKS = 1200;
	/** Extra fetch-and-trade rounds a trip may take for trades his trading opened up. */
	private static final int MAX_ROUNDS = 3;
	/** How long a villager he couldn't get to is left alone (ticks), so he doesn't set off for it over and over. */
	private static final int UNREACHABLE_TICKS = 2400;
	/** A trade called off (D46): he looks puzzled and shrugs. */
	private static final String SHRUG_CLIP = "shrug";

	private final TradeBotEntity bot;
	private Task task = Task.HOME;
	/** Where he's going, in order (a trip has one stop; a trading run, several). */
	private final Deque<BlockPos> goals = new ArrayDeque<>();
	/** Blocks he has stood on this trip, for checking a run in tests. */
	private final List<BlockPos> visited = new ArrayList<>();
	private boolean commanded;
	private boolean routed;
	private int ticks;
	private int homeFailures;
	/** The current job: the inputs in this trip's order, the output, and what he takes (milestone 5 puts trading in between). */
	@Nullable
	private Job job;
	@Nullable
	private BotWork work;
	/** Which input goes first next time, so they take turns (D47). */
	private int roundRobin;
	/** Villagers he couldn't get to, and the game time to try them again. */
	private final Map<UUID, Long> unreachable = new HashMap<>();
	private final Reactions reactions = new Reactions();
	/** What he was doing when a reaction, a fright or running down interrupted him, to carry on with after. */
	private Task resumeTask = Task.RETURNING;
	/** The mob he's afraid of: where it was last seen, and when. */
	@Nullable
	private Vec3 threat;
	private long threatSeen, calmUntil;
	/** A player who has just turned up, to greet once he's free. */
	@Nullable
	private UUID greet;
	private long greetUntil;

	private enum Phase { TAKING, TRADING, DELIVERING, RETURNING_ITEMS }

	private static final class Job {
		final List<BlockPos> inputs;
		final BlockPos output;
		ContainerWork.Wanted wanted;
		final boolean trading;
		Phase phase = Phase.TAKING;
		int input;
		int taken, trades, delivered, returned, emeralds;
		boolean cheered;
		/** Buyers he's finished with this round (sold out, or out of reach), and ones who called a trade off: those wait for another trip (D46). */
		final Set<UUID> done = new HashSet<>(), calledOff = new HashSet<>();
		/** Sides of a villager he couldn't get to this trip. */
		final Map<UUID, Set<Direction>> failedSides = new HashMap<>();
		/** What he took from each input, so unsold items go back where they came from (D62). */
		final Map<BlockPos, List<ItemStack>> takenFrom = new LinkedHashMap<>();
		/** Where unsold items still have to go: their own inputs first, then any input for what's left over. */
		final Deque<BlockPos> returns = new ArrayDeque<>();
		boolean leftoversQueued;
		/** Extra rounds of fetching and trading this trip, for trades his own trading opened up. */
		int rounds;

		Job(List<BlockPos> inputs, BlockPos output, ContainerWork.Wanted wanted, boolean trading) {
			this.inputs = inputs;
			this.output = output;
			this.wanted = wanted;
			this.trading = trading;
		}

		/** Stacks in his load that came from {@code input} (or, once leftovers are queued, anything). */
		Predicate<ItemStack> from(BlockPos input) {
			List<ItemStack> kinds = takenFrom.getOrDefault(input, List.of());
			return stack -> leftoversQueued || kinds.stream().anyMatch(k -> ItemStack.isSameItemSameComponents(k, stack));
		}
	}

	public TradeBotBrain(TradeBotEntity bot) {
		this.bot = bot;
	}

	public Task task() {
		return task;
	}

	/** The stops reached on this trip, in order. */
	public List<BlockPos> visited() {
		return List.copyOf(visited);
	}

	// ---- orders

	/** Sends him to stand on {@code target}, leaving his station first if he's docked. */
	public boolean goTo(BlockPos target) {
		return tour(List.of(target));
	}

	/** Visits each block in turn (a short wait at each), then goes home. */
	public boolean tour(List<BlockPos> stops) {
		if (stops.isEmpty())
			return false;
		goals.clear();
		goals.addAll(stops);
		visited.clear();
		job = null; // a trip, not the last job
		commanded = true;
		if (bot.isDocked())
			return leave();
		set(Task.GOING);
		return true;
	}

	/**
	 * A trading run, if there's one worth making (D47, D52): it's day, he isn't paused, the output has room, and either
	 * an input holds (or he carries) something a seated villager in range buys, or he has earnings to deliver; and his
	 * charge covers the round trip plus a trade (D49). From the inputs in turn he takes only what those villagers will
	 * buy (D22), sells it to them nearest-first, delivers his earnings and returns what didn't sell.
	 */
	public boolean trade() {
		if (!(bot.level().getBlockEntity(bot.getStation()) instanceof TradeBotStationBlockEntity station) || station.outputPosition() == null
			|| station.isPaused() || !bot.level().isDay() || bot.level().isThundering())
			return false;
		List<AbstractVillager> buyers = reachableBuyers();
		TradeDemand demand = demandFrom(buyers, station.inputPositions());
		boolean goods = sellable(buyers, station.inputPositions());
		if (!goods && !bot.hasEarnings() || !hasRoom(station.outputPosition()))
			return false;
		double far = goods ? buyers.stream().mapToDouble(bot::distanceTo).min().orElse(0)
			: bot.position().distanceTo(Vec3.atCenterOf(station.outputPosition()));
		if (bot.getCharge() < (far * 3 + 4) * CMConfig.SERVER.tradeBotFePerBlock() + CMConfig.SERVER.tradeBotFePerTrade())
			return false; // he stays home and charges
		return startJob(demand, goods, false);
	}

	/**
	 * A job without trading: from each of his station's inputs in turn, take what's {@code wanted} until he's full;
	 * deliver his earnings, emeralds and Nuggets of Experience, to the output, waiting beside it if it's full (D22, D48,
	 * D57); then, on the way home, put every unsold item back in the input it came from (D62).
	 */
	public boolean fetchAndDeliver(Predicate<ItemStack> wanted) {
		return startJob(ContainerWork.Wanted.all(wanted), false, true);
	}

	private boolean startJob(ContainerWork.Wanted wanted, boolean trading, boolean command) {
		if (!(bot.level().getBlockEntity(bot.getStation()) instanceof TradeBotStationBlockEntity station) || station.outputPosition() == null)
			return false;
		List<BlockPos> inputs = new ArrayList<>(station.inputPositions());
		if (!inputs.isEmpty())
			java.util.Collections.rotate(inputs, -(roundRobin++ % inputs.size()));
		job = new Job(inputs, station.outputPosition(), wanted, trading);
		debug("new job ({}trading), docked {}", trading ? "" : "not ", bot.isDocked());
		goals.clear();
		visited.clear();
		commanded = command;
		if (bot.isDocked())
			return leave();
		nextJobStep();
		return true;
	}

	/** Items taken and delivered, and trades made, on the current or last job. */
	public int taken() {
		return job == null ? 0 : job.taken;
	}

	public int trades() {
		return job == null ? 0 : job.trades;
	}

	public int delivered() {
		return job == null ? 0 : job.delivered;
	}

	public int returned() {
		return job == null ? 0 : job.returned;
	}

	/** Home to his station (wrench, redstone pause, packing, night...). */
	public void goHome() {
		if (task == Task.RETURNING || task == Task.DOCKING || task == Task.HOME && bot.isDocked())
			return;
		if (work != null) {
			work.abort();
			work = null;
		}
		VillagerClaims.releaseAll(bot.getUUID());
		bot.motion.stop();
		set(Task.RETURNING);
	}

	/** The next stop: inputs with something wanted, then buyers, then the output (if he has earnings), then returns; then home. */
	private void nextJobStep() {
		Job j = job;
		if (j == null) {
			set(Task.RETURNING);
			return;
		}
		if (j.phase == Phase.TAKING) {
			while (j.input < j.inputs.size() && !ContainerWork.full(bot.getLoad())) {
				BlockPos input = j.inputs.get(j.input);
				ContainerVisit visit = hasWanted(input, j.wanted) ? ContainerVisit.plan(bot, input) : null;
				if (visit != null && approach(visit, ContainerWork.Mode.TAKE, j.wanted))
					return;
				j.input++;
			}
			j.phase = j.trading ? Phase.TRADING : Phase.DELIVERING;
		}
		if (j.phase == Phase.TRADING) {
			// re-planned after each buyer, nearest first (D49)
			for (TradeSpot spot = nextBuyer(j); spot != null; spot = nextBuyer(j)) {
				UUID villager = spot.villager().getUUID();
				if (VillagerClaims.claim(villager, bot.getUUID(), bot.level().getGameTime()) && approach(new TradeWork(bot, spot, this::canAffordTrade)))
					return;
				VillagerClaims.release(villager, bot.getUUID());
				unreachable.put(villager, bot.level().getGameTime() + UNREACHABLE_TICKS);
				j.done.add(villager);
			}
			if (anotherRound(j)) {
				nextJobStep();
				return;
			}
			j.phase = Phase.DELIVERING;
		}
		if (j.phase == Phase.DELIVERING) {
			if (bot.hasEarnings()) {
				ContainerVisit visit = ContainerVisit.plan(bot, j.output);
				if (visit != null && approach(visit, ContainerWork.Mode.DELIVER_EARNINGS, stack -> 0))
					return;
			}
			j.phase = Phase.RETURNING_ITEMS;
			j.returns.addAll(nearestFirst(new ArrayList<>(j.takenFrom.keySet())));
		}
		while (!isEmpty(bot.getLoad())) {
			if (j.returns.isEmpty()) {
				if (j.leftoversQueued)
					break; // every input is full: he keeps the rest for next time
				// left over (their own input was full): any input with room
				j.leftoversQueued = true;
				j.returns.addAll(nearestFirst(new ArrayList<>(j.inputs)));
			}
			BlockPos input = j.returns.poll();
			if (!carries(j.from(input)))
				continue;
			ContainerVisit visit = ContainerVisit.plan(bot, input);
			if (visit != null && approach(visit, ContainerWork.Mode.RETURN, ContainerWork.Wanted.all(j.from(input))))
				return;
		}
		if (anotherRound(j)) {
			nextJobStep();
			return;
		}
		if (j.trading && j.trades > 0 && !j.cheered) {
			j.cheered = true; // a good day's trading (D9)
			resumeTask = Task.RETURNING;
			react("expr_happy", null);
			return;
		}
		set(Task.RETURNING);
	}

	/** The nearest buyer still to visit who buys something he carries, with a spot to trade from; null when there's none. */
	@Nullable
	private TradeSpot nextBuyer(Job j) {
		if (!canAffordTrade())
			return null;
		long now = bot.level().getGameTime();
		List<AbstractVillager> buyers = Buyers.near(bot, bot.getStation());
		buyers.removeIf(villager -> j.done.contains(villager.getUUID()) || j.calledOff.contains(villager.getUUID())
			|| unreachable.getOrDefault(villager.getUUID(), 0L) > now
			|| TradeWork.offerFor(bot, villager) == null);
		buyers.sort(java.util.Comparator.comparingDouble(villager -> villager.distanceToSqr(bot)));
		for (AbstractVillager villager : buyers) {
			TradeSpot spot = TradeSpot.plan(bot, villager);
			if (spot != null)
				return spot;
			j.done.add(villager.getUUID());
		}
		return null;
	}

	/** Seated buyers in range he can get to and trade with. */
	private List<AbstractVillager> reachableBuyers() {
		List<AbstractVillager> buyers = Buyers.near(bot, bot.getStation());
		long now = bot.level().getGameTime();
		buyers.removeIf(villager -> unreachable.getOrDefault(villager.getUUID(), 0L) > now || TradeSpot.plan(bot, villager) == null);
		return buyers;
	}

	/** What {@code buyers} will take, less what he already carries. */
	private TradeDemand demandFrom(List<AbstractVillager> buyers, List<BlockPos> inputs) {
		TradeDemand demand = Buyers.demand(bot, buyers);
		// whole lots only, of what's there to take: no taking 64 sticks for two lots of 35 and carrying 29 back
		List<net.neoforged.neoforge.items.IItemHandler> stores = stores(inputs);
		demand.limit(goods -> {
			int count = 0;
			for (var store : stores)
				for (int slot = 0; slot < store.getSlots(); slot++)
					if (goods.test(store.getStackInSlot(slot)))
						count += store.getStackInSlot(slot).getCount();
			return count;
		});
		for (int slot = 0; slot < bot.getLoad().getSlots(); slot++)
			demand.moved(bot.getLoad().getStackInSlot(slot)); // what he already carries counts first
		return demand;
	}

	/** His load and the inputs' inventories. */
	private List<net.neoforged.neoforge.items.IItemHandler> stores(List<BlockPos> inputs) {
		List<net.neoforged.neoforge.items.IItemHandler> stores = new ArrayList<>();
		stores.add(bot.getLoad());
		for (BlockPos input : inputs) {
			var handler = bot.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, input, null);
			if (handler != null)
				stores.add(handler);
		}
		return stores;
	}

	/**
	 * His trades opened new ones (a villager levelled up and now buys something else his inputs hold): rather than go
	 * home and straight back out, he fetches for them and trades again, this trip (D45). A few rounds at most.
	 */
	private boolean anotherRound(Job j) {
		if (!j.trading || j.rounds >= MAX_ROUNDS || !canAffordTrade() || shouldGoHome())
			return false;
		List<AbstractVillager> buyers = reachableBuyers();
		buyers.removeIf(villager -> j.calledOff.contains(villager.getUUID()));
		if (!sellable(buyers, j.inputs))
			return false;
		j.rounds++;
		j.wanted = demandFrom(buyers, j.inputs);
		j.input = 0;
		j.done.clear();
		j.leftoversQueued = false;
		j.returns.clear();
		j.phase = Phase.TAKING;
		debug("new trades opened up: round {}", j.rounds + 1);
		return true;
	}

	/** Some buyer's offer can be paid in full from his inputs and load together: a trip that will make a trade. */
	private boolean sellable(List<AbstractVillager> buyers, List<BlockPos> inputs) {
		List<net.neoforged.neoforge.items.IItemHandler> stores = stores(inputs);
		for (AbstractVillager villager : buyers)
			for (net.minecraft.world.item.trading.MerchantOffer offer : villager.getOffers()) {
				if (!com.comedymonkey.createaddons.content.tradebot.trade.BotPricing.buys(offer))
					continue;
				int have = 0;
				for (var store : stores)
					for (int slot = 0; slot < store.getSlots(); slot++)
						if (offer.getItemCostA().test(store.getStackInSlot(slot)))
							have += store.getStackInSlot(slot).getCount();
				if (have >= com.comedymonkey.createaddons.content.tradebot.trade.BotPricing.cost(offer, bot.reputationWith(villager.getUUID())))
					return true;
			}
		return false;
	}

	/** Enough charge for a trade and still the way home (D49). */
	private boolean canAffordTrade() {
		double home = bot.position().distanceTo(Vec3.atCenterOf(bot.getStation()));
		return bot.getCharge() - CMConfig.SERVER.tradeBotFePerTrade() > (home * 1.5 + 4) * CMConfig.SERVER.tradeBotFePerBlock();
	}

	private boolean hasRoom(BlockPos output) {
		var handler = bot.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, output, null);
		return handler != null && net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(handler, new ItemStack(Items.EMERALD), true).isEmpty();
	}

	private boolean approach(ContainerVisit visit, ContainerWork.Mode mode, ContainerWork.Wanted which) {
		if (!approach(new ContainerWork(bot, visit, mode, which)))
			return false;
		debug("{} {} at {} from the {} side, standing at {}", mode, visit.kind(), visit.container().toShortString(), visit.side(),
			visit.standBlock().toShortString());
		return true;
	}

	private boolean approach(BotWork next) {
		if (!bot.motion.moveTo(next.standBlock(), next.stand(), next.yaw()))
			return false;
		work = next;
		if (next instanceof TradeWork trade)
			debug("trading with {} at {}, standing at {}", trade.villager().getName().getString(), TradeSpot.seatOf(trade.villager()).toShortString(),
				next.standBlock().toShortString());
		set(Task.APPROACHING);
		return true;
	}

	private void debug(String message, Object... args) {
		if (com.comedymonkey.createaddons.content.tradebot.move.TradeBotMotion.debug) {
			Object[] all = new Object[args.length + 1];
			all[0] = bot.getId();
			System.arraycopy(args, 0, all, 1, args.length);
			com.mojang.logging.LogUtils.getLogger().info("[Trade Bot {}] " + message, all);
		}
	}

	/** Orders containers so each next one is the nearest to the last (from where he is now). */
	private List<BlockPos> nearestFirst(List<BlockPos> containers) {
		List<BlockPos> ordered = new ArrayList<>();
		net.minecraft.world.phys.Vec3 from = bot.position();
		while (!containers.isEmpty()) {
			net.minecraft.world.phys.Vec3 at = from;
			BlockPos nearest = containers.stream().min(java.util.Comparator.comparingDouble(c -> c.getCenter().distanceToSqr(at))).orElseThrow();
			containers.remove(nearest);
			ordered.add(nearest);
			from = nearest.getCenter();
		}
		return ordered;
	}

	private boolean carries(Predicate<ItemStack> which) {
		for (int slot = 0; slot < bot.getLoad().getSlots(); slot++)
			if (!bot.getLoad().getStackInSlot(slot).isEmpty() && which.test(bot.getLoad().getStackInSlot(slot)))
				return true;
		return false;
	}

	private boolean hasWanted(BlockPos pos, ContainerWork.Wanted wanted) {
		var handler = bot.level().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, null);
		if (handler == null)
			return false;
		for (int slot = 0; slot < handler.getSlots(); slot++)
			if (!handler.getStackInSlot(slot).isEmpty() && wanted.howMany(handler.getStackInSlot(slot)) > 0)
				return true;
		return false;
	}

	private static boolean isEmpty(net.neoforged.neoforge.items.IItemHandler handler) {
		for (int slot = 0; slot < handler.getSlots(); slot++)
			if (!handler.getStackInSlot(slot).isEmpty())
				return false;
		return true;
	}

	// ---- ticking

	public void tick() {
		ticks++;
		watch();
		BlockPos station = bot.getStation();
		if (task == Task.WOUND_DOWN)
			return;
		if (station == null || !(bot.level().getBlockState(station).getBlock() instanceof TradeBotStationBlock)) {
			if (task != Task.STRANDED) {
				bot.motion.stop();
				set(Task.STRANDED);
			}
			return;
		}
		if ((task == Task.GOING || task == Task.WAITING || task == Task.APPROACHING || task == Task.WORKING || task == Task.SHRUGGING
			|| task == Task.WAITING_OUTPUT || task == Task.WAITING_TURN) && shouldGoHome())
			goHome();
		switch (task) {
			case HOME -> {
				if (!bot.isDocked() && !bot.motion.isMoving())
					set(Task.RETURNING);
				else if (bot.isDocked() && ticks >= SCHEDULE_TICKS) {
					ticks = 0;
					trade();
				}
			}
			case LEAVING -> {
				Clip clip = bot.clip();
				if (!clip.name().equals(LEAVE) || clip.finished(bot.clipElapsed(0)))
					if (job != null)
						nextJobStep();
					else
						set(!goals.isEmpty() ? Task.GOING : Task.RETURNING);
			}
			case APPROACHING -> {
				BotWork w = work;
				if (w instanceof TradeWork trade && (!trade.available()
					|| !VillagerClaims.claim(trade.villager().getUUID(), bot.getUUID(), bot.level().getGameTime()))) {
					// the villager stood up, left, or another bot got there first: a shrug, and someone else
					debug("{} can't trade now (seated {}, in place {})", trade.villager().getName().getString(),
						com.comedymonkey.createaddons.content.VillagerSeats.sitting(trade.villager()), trade.inPlace());
					calledOff(trade);
				} else if (w == null)
					nextJobStep();
				else if (bot.motion.arrived()) {
					Vec3 stand = w.stand();
					bot.moveTo(stand.x, stand.y, stand.z, w.yaw(), 0);
					bot.setYRot(w.yaw());
					if (w instanceof TradeWork trade && trade.busy()) {
						// a player's trading with it: he waits his turn, checking his stock
						debug("waits for a player to finish with {}", trade.villager().getName().getString());
						bot.playClip("count_stock");
						set(Task.WAITING_TURN);
					} else {
						w.start();
						set(Task.WORKING);
					}
				} else if (bot.motion.failed()) {
					work = null;
					if (w instanceof TradeWork trade) {
						trade.abort();
						UUID villager = trade.villager().getUUID();
						// that side's no good: another side of the same villager, if there is one
						if (job != null) {
							Set<Direction> failed = job.failedSides.computeIfAbsent(villager, v -> new HashSet<>());
							failed.add(trade.side());
							TradeSpot other = TradeSpot.plan(bot, trade.villager(), failed);
							debug("couldn't reach {} from the {} side{}", trade.villager().getName().getString(), trade.side(),
								other != null ? ", trying the " + other.side() : "");
							if (other != null && VillagerClaims.claim(villager, bot.getUUID(), bot.level().getGameTime())
								&& approach(new TradeWork(bot, other, this::canAffordTrade)))
								return;
							job.done.add(villager);
						}
						unreachable.put(villager, bot.level().getGameTime() + UNREACHABLE_TICKS);
					} else if (job != null && job.phase == Phase.TAKING)
						job.input++;
					else if (job != null && job.phase == Phase.DELIVERING)
						job.phase = Phase.RETURNING_ITEMS; // can't reach the output: keep the earnings, return the items
					nextJobStep();
				}
			}
			case WORKING -> {
				BotWork w = work;
				if (w == null || w.tick()) {
					work = null;
					if (w instanceof TradeWork trade) {
						tradeDone(trade);
						return;
					}
					if (job != null && w instanceof ContainerWork c) {
						switch (c.mode()) {
							case TAKE -> {
								job.taken += c.moved();
								for (ItemStack stack : c.movedStacks())
									job.takenFrom.computeIfAbsent(c.visit().container(), at -> new ArrayList<>()).add(stack.copyWithCount(1));
								job.input++;
							}
							case DELIVER_EARNINGS -> {
								job.delivered += c.moved();
								job.emeralds += c.emeraldsDelivered();
								if (bot.hasEarnings()) { // the output's full: wait beside it, worried, and try again (D22)
									bot.playClip("expr_sad");
									set(Task.WAITING_OUTPUT);
									return;
								}
							}
							case RETURN -> job.returned += c.moved();
						}
					}
					nextJobStep();
				}
			}
			case WAITING_TURN -> {
				if (!(work instanceof TradeWork trade))
					nextJobStep();
				else {
					VillagerClaims.claim(trade.villager().getUUID(), bot.getUUID(), bot.level().getGameTime());
					if (!trade.available() || ticks >= TURN_WAIT_TICKS) {
						debug("gave up waiting for {}", trade.villager().getName().getString());
						calledOff(trade);
					} else if (!trade.busy()) {
						trade.start();
						set(Task.WORKING);
					}
				}
			}
			case SHRUGGING -> {
				Clip clip = bot.clip();
				if (!clip.name().equals(SHRUG_CLIP) || clip.finished(bot.clipElapsed(0)))
					nextJobStep();
			}
			case WAITING_OUTPUT -> {
				if (ticks >= OUTPUT_RETRY_TICKS)
					nextJobStep();
			}
			case GOING -> {
				if (!routed) {
					routed = true;
					if (goals.isEmpty() || !bot.motion.moveTo(goals.peek(), null, Float.NaN))
						set(Task.RETURNING);
				} else if (bot.motion.arrived()) {
					visited.add(goals.poll());
					set(Task.WAITING);
					bot.playClip("count_stock");
				} else if (bot.motion.failed())
					set(Task.RETURNING);
			}
			case WAITING -> {
				if (ticks >= WAIT_TICKS)
					set(goals.isEmpty() ? Task.RETURNING : Task.GOING);
			}
			case RETURNING -> returnHome(station);
			case DOCKING -> {
				Clip clip = bot.clip();
				if (!clip.name().equals(TradeBotEntity.DOCKED) || clip.finished(bot.clipElapsed(0))) {
					bot.dockAt(station, facing(station));
					commanded = false;
					work = null;
					set(Task.HOME);
					if (job != null && job.emeralds >= ModAdvancements.TRADE_BOT_STACK_EMERALDS)
						bot.awardOwner(ModAdvancements.TRADE_BOT_STACK, job.emeralds);
					if (job != null && job.trading && job.trades == 0)
						ticks = SCHEDULE_TICKS - NO_TRADE_REST_TICKS; // nothing sold: give it a minute before trying again
				}
			}
			case STRANDED -> set(Task.HOME);
			case REACTING -> {
				if (clipOver())
					resume();
			}
			case WINDING_UP -> {
				// wound up in his cradle: he hops out first, like any morning, then carries on
				if (clipOver() && (!onStation(station) || !leave()))
					resume();
			}
			case WOUND_DOWN -> {}
			case SCARED_SPOT -> {
				if (clipOver())
					flee();
			}
			case SCARED_FLEE -> {
				if (bot.motion.arrived() || bot.motion.failed() || ticks >= FLEE_TICKS) {
					bot.motion.halt();
					bot.playClip("scared_hide");
					set(Task.SCARED_HIDE);
				}
			}
			case SCARED_HIDE -> {
				if (ticks == 1)
					bot.awardOwner(ModAdvancements.TRADE_BOT_SCARED, 0);
				if (clipOver()) {
					bot.playClip("scared_cower");
					set(Task.SCARED_COWER);
				}
			}
			case SCARED_COWER -> {
				if (bot.level().getGameTime() - threatSeen >= ALL_CLEAR_TICKS) {
					bot.playClip("scared_recover");
					set(Task.SCARED_RECOVER);
				}
			}
			case SCARED_RECOVER -> {
				if (clipOver()) {
					bot.motion.setFleeing(false);
					threat = null;
					calmUntil = bot.level().getGameTime() + CALM_TICKS;
					resume();
				}
			}
		}
	}

	// ---- feeling alive (D8, D9, D24, D26)

	/**
	 * Every half second while he's awake (not asleep in his station or run down): mobs he can see (fright first), and
	 * players turning up. A player who turns up while he can't react (hopping out, at a container) is greeted as soon
	 * as he's on the ground and free, if they're still about.
	 */
	private void watch() {
		long now = bot.level().getGameTime();
		boolean awake = !bot.isDocked() && task != Task.WOUND_DOWN && task != Task.WINDING_UP;
		if (now % 10 != 0 || !awake)
			return;
		Monster mob = hostileInSight();
		boolean scared = task.name().startsWith("SCARED_");
		if (mob != null && scared) {
			threat = mob.position();
			threatSeen = now;
		} else if (mob != null && now >= calmUntil && interruptible())
			scare(mob);
		if (now % 20 != 0)
			return;
		for (Player player : bot.level().getEntitiesOfClass(Player.class, bot.getBoundingBox().inflate(SEES_PLAYER_RANGE),
			player -> !player.isSpectator() && player.distanceTo(bot) <= SEES_PLAYER_RANGE && bot.hasLineOfSight(player)))
			if (reactions.arrived(player.getUUID(), now) && greet == null) {
				greet = player.getUUID();
				greetUntil = now + GREET_WAIT_TICKS;
			}
		if (greet == null)
			return;
		Player player = bot.level().getPlayerByUUID(greet);
		if (player == null || now > greetUntil || player.distanceTo(bot) > SEES_PLAYER_RANGE + 2)
			greet = null;
		else if (interruptible() && reactions.tryPlay(Reactions.Kind.SEES_PLAYER, now)) {
			greet = null;
			react("sees_player", player);
		}
	}

	private boolean interruptible() {
		return INTERRUPTIBLE.contains(task) && bot.motion.canHalt();
	}

	/** The nearest monster he can see within his fright radius (monsters ignore him, but he doesn't know that, D26). */
	@Nullable
	private Monster hostileInSight() {
		int radius = CMConfig.SERVER.tradeBotHostileRadius();
		return bot.level().getEntitiesOfClass(Monster.class, bot.getBoundingBox().inflate(radius),
				mob -> mob.isAlive() && !mob.isInvisible() && mob.distanceTo(bot) <= radius && bot.hasLineOfSight(mob))
			.stream().min(java.util.Comparator.comparingDouble(mob -> mob.distanceToSqr(bot))).orElse(null);
	}

	/** He's been hit by a player: a cross look at them (D9). */
	public void hitBy(Player player) {
		if (interruptible() && reactions.tryPlay(Reactions.Kind.CROSS, bot.level().getGameTime()))
			react("expr_cross", player);
	}

	/** Stops where he is and plays a reaction (facing {@code at}, if given), then carries on with what he was doing. */
	private void react(String clip, @Nullable net.minecraft.world.entity.Entity at) {
		if (task != Task.REACTING)
			resumeTask = task;
		bot.motion.halt();
		if (at != null)
			bot.setYRot(com.comedymonkey.createaddons.content.tradebot.move.TradeBotMotion.yawTo(bot.position(), at.position()));
		bot.playClip(clip);
		debug("reacts: {}", clip);
		set(Task.REACTING);
	}

	/** A mob! He stops dead, stares at it and jumps (the five-stage routine, D8). */
	private void scare(Monster mob) {
		resumeTask = task == Task.REACTING ? resumeTask : task;
		threat = mob.position();
		threatSeen = bot.level().getGameTime();
		bot.motion.halt();
		bot.setYRot(com.comedymonkey.createaddons.content.tradebot.move.TradeBotMotion.yawTo(bot.position(), mob.position()));
		bot.playClip("scared_spot");
		debug("scared of {} at {}", mob.getName().getString(), mob.blockPosition().toShortString());
		set(Task.SCARED_SPOT);
	}

	/** Off to the best corner nearby, as fast as his wheel will go; nowhere to go, he hides where he is. */
	private void flee() {
		var level = bot.level();
		BlockPos corner = threat == null ? null : CornerFinder.find(bot.blockPosition(), threat, HIDE_RADIUS,
			pos -> level.isLoaded(pos) && WalkNodeEvaluator.getPathTypeStatic(bot, pos) == PathType.WALKABLE,
			pos -> level.isLoaded(pos) && !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty());
		bot.motion.setFleeing(true);
		if (corner != null && bot.motion.moveTo(corner, null, Float.NaN)) {
			debug("flees to {}", corner.toShortString());
			set(Task.SCARED_FLEE);
			return;
		}
		bot.playClip("scared_hide");
		set(Task.SCARED_HIDE);
	}

	/** Runs down: drops what he was doing (keeping everything he carries) and slumps where he is. */
	public void windDown() {
		if (task != Task.WOUND_DOWN && task != Task.WINDING_UP)
			resumeTask = task == Task.REACTING || task.name().startsWith("SCARED_") ? resumeTask : task;
		if (work != null) {
			work.abort();
			work = null;
		}
		VillagerClaims.releaseAll(bot.getUUID());
		bot.motion.halt();
		bot.motion.setFleeing(false);
		threat = null;
		debug("wound down ({})", bot.rundown());
		set(Task.WOUND_DOWN);
	}

	/** Being wound up again: once the clip's done he carries on. */
	public void windingUp() {
		set(Task.WINDING_UP);
	}

	/** Back to what he was doing before the interruption: his job, his trip, or home. */
	private void resume() {
		switch (resumeTask) {
			case APPROACHING, WORKING, SHRUGGING, WAITING_OUTPUT, WAITING_TURN -> {
				if (job != null)
					nextJobStep();
				else
					set(Task.RETURNING);
			}
			case GOING, WAITING -> set(goals.isEmpty() ? Task.RETURNING : Task.GOING);
			default -> set(Task.RETURNING);
		}
	}

	/** On his station's cradle (run down there, or put there). */
	private boolean onStation(BlockPos station) {
		return bot.position().distanceTo(TradeBotStationBlock.dockPoint(station, facing(station))) < 0.75;
	}

	private boolean clipOver() {
		Clip clip = bot.clip();
		return clip.finished(bot.clipElapsed(0));
	}

	/**
	 * A trade clip is over, with every trade he could make in it (D63). If the villager would still buy (it can't
	 * normally: the clip stops only when it won't, or his charge is short), he goes again; otherwise he's done with this
	 * villager. If it was called off, he shrugs and carries on with the next buyer, keeping everything (D46).
	 */
	private void tradeDone(TradeWork trade) {
		UUID villager = trade.villager().getUUID();
		if (trade.failed()) {
			debug("the trade with {} was called off", trade.villager().getName().getString());
			trade.abort();
			if (job != null)
				job.calledOff.add(villager);
			bot.playClip(SHRUG_CLIP);
			set(Task.SHRUGGING);
			return;
		}
		if (job != null)
			job.trades += trade.trades();
		debug("{} trades with {} in one clip", trade.trades(), trade.villager().getName().getString());
		if (trade.traded() && trade.stillThere() && canAffordTrade() && !shouldGoHome() && TradeWork.offerFor(bot, trade.villager()) != null) {
			work = trade;
			trade.start();
			return;
		}
		trade.abort();
		if (job != null)
			job.done.add(villager);
		nextJobStep();
	}

	/** A trade that's off before it started (D46): he shrugs where he is, keeps everything, and tries someone else. */
	private void calledOff(TradeWork trade) {
		trade.abort();
		work = null;
		if (job != null)
			job.calledOff.add(trade.villager().getUUID());
		if (!bot.motion.canHalt()) {
			bot.motion.stop();
			nextJobStep();
			return;
		}
		bot.motion.halt();
		bot.playClip(SHRUG_CLIP);
		set(Task.SHRUGGING);
	}

	private void returnHome(BlockPos station) {
		Direction facing = facing(station);
		Vec3 start = dockStart(station, facing);
		if (!routed) {
			routed = true;
			if (!bot.motion.moveTo(BlockPos.containing(start), start, facing.toYRot()))
				homeFailed(station, facing);
			return;
		}
		if (bot.motion.failed()) {
			routed = false;
			homeFailed(station, facing);
		} else if (bot.motion.arrived()) {
			// right on the spot, facing away from the station: back in and hop up into the cradle
			bot.moveTo(start.x, start.y, start.z, facing.toYRot(), 0);
			bot.setYRot(facing.toYRot());
			bot.playClip(ClipLibrary.get().clip(TradeBotEntity.DOCKED));
			set(Task.DOCKING);
		}
	}

	private void homeFailed(BlockPos station, Direction facing) {
		if (++homeFailures >= MAX_HOME_FAILURES) {
			bot.dockAt(station, facing);
			set(Task.HOME);
		} else
			ticks = -40; // try again in a couple of seconds
	}

	/** Hops out of the cradle forward (D3). If the second block in front is blocked, the hop is shortened to the first. */
	private boolean leave() {
		BlockPos station = bot.getStation();
		if (station == null)
			return false;
		Direction facing = facing(station);
		Clip clip = ClipLibrary.get().clip(LEAVE);
		Vec3 from = bot.position();
		Vector3f travel = ClipRoot.travel(clip, clip.length(), new Vector3f());
		Vec3 landing = ClipRoot.place(from, facing.toYRot(), travel);
		float scale = 1;
		if (!standable(BlockPos.containing(landing))) {
			Vec3 front = Vec3.atBottomCenterOf(station.relative(facing));
			if (!standable(station.relative(facing)))
				return false;
			scale = (float) (horizontal(front.subtract(from)) / Math.abs(travel.z));
		}
		bot.setYRot(facing.toYRot());
		bot.leaveDock();
		bot.playClip(clip, 1, scale);
		set(Task.LEAVING);
		return true;
	}

	private boolean standable(BlockPos pos) {
		return WalkNodeEvaluator.getPathTypeStatic(bot, pos) == PathType.WALKABLE;
	}

	/** Where docking starts: on the ground in front of the station, facing away from it (the docking clip backs in). */
	public static Vec3 dockStart(BlockPos station, Direction facing) {
		Clip dock = ClipLibrary.get().clip(TradeBotEntity.DOCKED);
		Vec3 travelled = ClipRoot.place(Vec3.ZERO, facing.toYRot(), ClipRoot.travel(dock, dock.length(), new Vector3f()));
		return TradeBotStationBlock.dockPoint(station, facing).subtract(travelled);
	}

	/** Home rules: paused by redstone, night or a thunderstorm (unless sent by command), or charge running out. */
	private boolean shouldGoHome() {
		if (bot.level().getBlockEntity(bot.getStation()) instanceof TradeBotStationBlockEntity station && station.isPaused())
			return true;
		if (!commanded && (!bot.level().isDay() || bot.level().isThundering()))
			return true;
		double home = bot.position().distanceTo(Vec3.atCenterOf(bot.getStation()));
		return bot.getCharge() <= (home * 1.5 + 4) * CMConfig.SERVER.tradeBotFePerBlock();
	}

	private Direction facing(BlockPos station) {
		BlockState state = bot.level().getBlockState(station);
		return state.getValue(TradeBotStationBlock.FACING);
	}

	private void set(Task task) {
		if (task != this.task)
			debug("{} -> {}{}", this.task, task, job != null ? " (job " + job.phase + ")" : "");
		this.task = task;
		ticks = 0;
		routed = false;
		if (task == Task.HOME)
			homeFailures = 0;
	}

	private static double horizontal(Vec3 v) {
		return Math.sqrt(v.x * v.x + v.z * v.z);
	}
}
