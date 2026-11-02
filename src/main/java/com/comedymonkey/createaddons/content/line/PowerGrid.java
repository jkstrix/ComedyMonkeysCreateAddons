package com.comedymonkey.createaddons.content.line;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * What a level knows about its Power Lines, kept with the level so it holds while chunks come and go: every piece of
 * line (a wire block, or a whole pole), what it's joined to and the machines it meets, and the records of the generators
 * the lines meet. A piece is read from the world only when it changes (or loads and isn't known yet), never from an
 * unloaded chunk; blocks don't change there, so what was read holds.
 * <p>
 * A generator's record is how it runs "on paper" while its chunk isn't running: its output, its store, its fuels and what
 * the Wellsprings feeding them bring in, worked forward as it would have run (always flat out while it has fuel). Fuel
 * from a Wellspring counts on paper only while that Wellspring isn't running either (one that is runs for real, and the
 * Fluid Line between them stops at the unloaded chunk), and comes out of that Wellspring's own record ({@link
 * SpringPaper}): never more than it holds and makes. When the generator runs again it takes on how far its record
 * moved while it was away, so nothing else that reached it meanwhile is lost or made twice.
 */
public final class PowerGrid extends SavedData {
	private static final String NAME = "comedymonkeysaddons_power_grid";

	/** A wire block (insulator {@link #WIRE}), or a whole pole ({@link #POLE}, at its head: every insulator joins). */
	public record Node(BlockPos pos, int insulator) {
		public static final int WIRE = -1, POLE = 0;

		boolean isPole() {
			return insulator == POLE;
		}
	}

	/**
	 * A join to another piece, by a cable of this tier and colour (null: plain), leaving a wire block {@code via} this
	 * side (null on a pole). On a pole, only cables of one tier and colour join each other (brass feeding copper of its
	 * colour).
	 */
	record Link(Node to, LineTier tier, @Nullable net.minecraft.world.item.DyeColor dye, @Nullable Direction via) {}

	/** A machine a piece meets: the block, and the side of it the wire meets. */
	record End(BlockPos target, Direction side) {}

	/** One piece as last read. */
	static final class Piece {
		/** Its tier; a pole has none (each cable tied to it has its own). */
		@Nullable
		final LineTier tier;
		/** A wire's colour (null: plain, or a pole). */
		@Nullable
		net.minecraft.world.item.DyeColor dye;
		/** Read from a save made before pieces had colours: read again from the world once it's loaded. */
		boolean stale;
		final Fitting fitting;
		/** A Breaker's state as last seen (read again whenever its chunk is loaded). */
		boolean closed;
		/** A Splitter's priority for the branch each way (by direction): 0 first, 1 second, 2 last; null if not one. */
		@Nullable
		byte[] priority;
		final List<Link> links = new ArrayList<>();
		final List<End> ends = new ArrayList<>();

		Piece(@Nullable LineTier tier, Fitting fitting, boolean closed) {
			this.tier = tier;
			this.fitting = fitting;
			this.closed = closed;
		}
	}

	/**
	 * A store several blocks share (a bank of Batteries, an Industrial Battery): lines meeting any of its blocks are one
	 * network through it, as through a generator, and it's one store in that network's plan, shared by every circuit
	 * (tier and colour) that meets it.
	 */
	public interface Store {
		/** The same for every block of it while it holds (null: it can't be worked out now). */
		@Nullable
		BlockPos storeKey();

		/** Every block of it a line can meet. */
		java.util.Collection<BlockPos> storeBlocks();
	}

	/** A store that takes back what was drawn from it and not used, past its capacity and rate (it was its own). */
	public interface Refund {
		int refund(int fe);
	}

	/**
	 * A store the Power Lines plan a second of at once: what it could take in or give over the next second at its rate.
	 * Its simulate answers stay for this tick only, as every other mod's cable expects.
	 */
	public interface Planned {
		int planReceive(int max);

		int planExtract(int max);
	}

	/** What a second's plan can take from e: a {@link Planned} store's second, else what it says it would give. */
	static int planExtract(IEnergyStorage e, int max) {
		return e instanceof Planned p ? p.planExtract(max) : e.extractEnergy(max, true);
	}

	/** What a second's plan can put into e: a {@link Planned} store's second, else what it says it would take. */
	static int planReceive(IEnergyStorage e, int max) {
		return e instanceof Planned p ? p.planReceive(max) : e.receiveEnergy(max, true);
	}

	/**
	 * A shared store's record, for running it on paper while none of it is running: what it holds, what it can hold, its
	 * rate a tick, and its blocks. While it runs it writes its state here every tick; once all of it is away the network
	 * fills and draws the record instead, and when it runs again it takes on how far the record moved, once.
	 */
	public static final class StorePaper {
		double energy, seen;
		long cap, perTick;
		List<BlockPos> members = List.of();
		/** It runs only whole (an Industrial Battery): while any of it isn't loaded, it's on paper. */
		boolean whole;
		/** This tick's moves, against its rate (not saved). */
		private long tick = Long.MIN_VALUE;
		private double inNow, outNow;
		@Nullable
		private IEnergyStorage view;

		public double energy() {
			return energy;
		}

		public boolean whole() {
			return whole;
		}
	}

	/**
	 * A machine that draws power and keeps drawing it on paper while it's away (a Clockwork Wastes Substation's upkeep,
	 * design/specs/clockwork-wastes.md, P24, D48): what the network has sent it while it was away and it hasn't used yet,
	 * how much that can be, and what it draws a tick. While it runs it notes itself here once a second
	 * ({@link #noteConsumer}); away, a network still running delivers into this record, and when none is running at all
	 * it's drawn through the records of the generators and stores it reaches ({@code PowerLines.drawAway}). Back, it takes
	 * what's left ({@link #consumerBack}).
	 */
	public static final class ConsumerPaper {
		double energy;
		long cap, perTick;
		@Nullable
		private IEnergyStorage view;

		public double energy() {
			return energy;
		}

		public long perTick() {
			return perTick;
		}
	}

	/** A block that makes power for a line (or the part of one a cable meets, recorded as the machine it belongs to). */
	public interface Generator {
		/** Where it's recorded: itself, or the machine it's part of (a turbine's port casing: its controller). */
		default BlockPos generatorKey() {
			return ((BlockEntity) this).getBlockPos();
		}

		/** The kind of record it keeps, or null if this is only a part of one. */
		@Nullable
		default String paperKind() {
			return null;
		}

		/** How many fuels its record keeps. */
		default int paperFuels() {
			return 0;
		}

		/** A fresh record of its kind, or null if this is only a part of one. */
		@Nullable
		default Paper newPaper() {
			String kind = paperKind();
			return kind == null ? null : new Paper(kind, paperFuels());
		}

		/** Writes its state as it is now into its record; its fuels' income only when {@code refresh}. */
		default void record(Paper p, boolean refresh) {}

		/** Running again: takes on how far its record's store and fuels moved while it was away. */
		default void resume(double storeChange, double[] fuelChange) {}

		/** A Fluid Line brought it this much of a fluid from the Wellspring (or other block) at {@code source}. */
		default void fed(BlockPos source, Fluid fluid, int amount) {}
	}

	/** One fuel a generator burns: what it holds and can hold (mB), what it burns a tick flat out, and its income. */
	public static final class Fuel {
		double amount, seen, use;
		int cap;
		/** mB a tick each source brings it, as last seen running. */
		final Map<BlockPos, Double> income = new LinkedHashMap<>();
		/** The fluid it burns (null in a record from before it was kept: any Wellspring's fluid counts). */
		@Nullable
		Fluid fluid;

		public void set(double amount, int cap, double use) {
			this.amount = amount;
			this.cap = cap;
			this.use = use;
		}

		public void set(double amount, int cap, double use, Fluid fluid) {
			set(amount, cap, use);
			this.fluid = fluid;
		}

		public Map<BlockPos, Double> income() {
			return income;
		}
	}

	/**
	 * What Fluid Lines bring a generator, from each source, as a rate in mB a tick averaged over a few seconds: its fuel
	 * income for its record. Kept by the generator, not saved (it builds up again within seconds of running).
	 */
	public static final class Feeds {
		private final int fuels;
		private final Map<BlockPos, double[]> got = new HashMap<>(), rate = new HashMap<>();
		private long since = -1;

		public Feeds(int fuels) {
			this.fuels = fuels;
		}

		public void add(BlockPos source, int fuel, int amount) {
			if (fuel >= 0 && fuel < fuels && amount > 0)
				got.computeIfAbsent(source.immutable(), k -> new double[fuels])[fuel] += amount;
		}

		/** Now and then (about once a second): the rates brought up to date with what came since. */
		public void update(long now) {
			double ticks = since < 0 ? 0 : now - since;
			since = now;
			if (ticks <= 0) {
				got.clear();
				return;
			}
			Set<BlockPos> all = new HashSet<>(rate.keySet());
			all.addAll(got.keySet());
			for (BlockPos s : all) {
				double[] g = got.get(s), r = rate.computeIfAbsent(s, k -> new double[fuels]);
				boolean any = false;
				for (int i = 0; i < fuels; i++) {
					r[i] = r[i] * 0.5 + (g == null ? 0 : g[i] / ticks) * 0.5;
					if (r[i] < 0.005)
						r[i] = 0;
					else
						any = true;
				}
				if (!any)
					rate.remove(s);
			}
			got.clear();
		}

		/** Sets a record's fuel's income to the rates from each source still there (a loaded block that's no Wellspring now isn't). */
		public void into(Fuel f, int fuel, net.minecraft.world.level.Level level) {
			rate.keySet().removeIf(src -> level.isLoaded(src)
				&& !(level.getBlockEntity(src) instanceof com.comedymonkey.createaddons.content.wellspring.WellspringBlockEntity));
			f.income.clear();
			for (Map.Entry<BlockPos, double[]> e : rate.entrySet())
				if (e.getValue()[fuel] > 0)
					f.income.put(e.getKey(), e.getValue()[fuel]);
		}
	}

	/** A generator's record: its output flat out (FE a tick), its store, its fuels and the generators it touches. */
	public static final class Paper {
		final String kind;
		int rate;
		/**
		 * A Clockwork Dynamo's free sources (water wheels, windmills): the share of its output each one gives, by where it
		 * is; null for every other generator. While it runs it makes its full {@link #rate}; while it's away a source counts
		 * only if it isn't running itself (it's away too, so nothing else uses it: the Wellspring rule) and this is the
		 * record that saw it last (a source re-routed to another dynamo counts once).
		 */
		@Nullable
		Map<BlockPos, Double> freeShares;
		long storeCap;
		double store, seenStore;
		final Fuel[] fuels;
		/** The touching generators it shares its store with (a row of Lava Generators). */
		List<BlockPos> row = List.of();
		/** As of the tick {@code at}; the generator last wrote it at {@code seenAt}. */
		long at, seenAt;
		/** Its away rate as worked out in the tick {@code rateAt} (asked several times a tick; not saved). */
		private long rateAt = Long.MIN_VALUE;
		private int rateNow;

		public Paper(String kind, int fuels) {
			this.kind = kind;
			this.fuels = new Fuel[fuels];
			for (int i = 0; i < fuels; i++)
				this.fuels[i] = new Fuel();
		}

		public void set(int rate, double store, long storeCap) {
			if (rate != this.rate)
				rateAt = Long.MIN_VALUE;
			this.rate = rate;
			this.store = store;
			this.storeCap = storeCap;
		}

		/** Only the free sources' shares count while it's away (see {@link #freeShares}); a map nothing else changes. */
		public void away(Map<BlockPos, Double> shares) {
			if (shares != freeShares)
				rateAt = Long.MIN_VALUE;
			this.freeShares = shares;
		}

		/**
		 * FE a tick flat out while away: its full rate, or (with free sources) its rate times the share of the sources that
		 * count now, never more than all of it. Without a level (tests) every source counts. Worked out once a tick.
		 */
		int paperRate(@Nullable ServerLevel level, @Nullable PowerGrid grid) {
			if (freeShares == null)
				return rate;
			if (level == null || grid == null) {
				double share = 0;
				for (double s : freeShares.values())
					share += s;
				return (int) Math.floor(rate * Math.min(1, share));
			}
			long now = level.getGameTime();
			if (rateAt == now)
				return rateNow;
			double share = 0;
			for (Map.Entry<BlockPos, Double> e : freeShares.entrySet())
				if (!running(level, e.getKey()) && grid.lastToSee(e.getKey()) == this)
					share += e.getValue();
			rateNow = (int) Math.floor(rate * Math.min(1, share));
			rateAt = now;
			return rateNow;
		}

		public Fuel fuel(int i) {
			return fuels[i];
		}

		public void row(List<BlockPos> row) {
			this.row = row;
		}

		/**
		 * mB a tick a fuel is brought now from Wellsprings that aren't running (one that is runs for real), each as far as
		 * its own record can give it: its recorded income while the Wellspring holds some, else its part of what that
		 * Wellspring makes. A Wellspring with no record gives nothing on paper. Without a grid (tests) as recorded.
		 */
		private double income(Fuel f, @Nullable ServerLevel level, @Nullable PowerGrid grid) {
			double in = 0;
			for (Map.Entry<BlockPos, Double> e : f.income.entrySet()) {
				if (level == null || grid == null) {
					in += e.getValue();
					continue;
				}
				if (running(level, e.getKey()))
					continue;
				SpringPaper s = grid.springFor(e.getKey(), f);
				if (s != null)
					in += s.givesNow(this, e.getValue(), level.getGameTime());
			}
			return in;
		}

		/**
		 * FE a tick it's making now: flat out, or (with a fuel run dry) what that fuel's income keeps going; none if it
		 * can't run. Flat out is its away rate while it's {@code away}, its full rate while it runs.
		 */
		double making(@Nullable ServerLevel level, @Nullable PowerGrid grid, boolean away) {
			int flatOut = away ? paperRate(level, grid) : rate;
			if (flatOut <= 0)
				return 0;
			double run = 1;
			for (Fuel f : fuels)
				if (f.amount <= 1e-9)
					run = Math.min(run, f.use <= 0 ? 1 : Math.min(1, income(f, level, grid) / f.use));
			return flatOut * run;
		}

		/**
		 * Worked forward to the end of the tick {@code now}, flat out: each fuel burns and is brought in, the output
		 * fills the store and what doesn't fit is lost. In steps, since running a fuel dry changes the rate. What a fuel
		 * takes in from a Wellspring away too comes out of that Wellspring's record (never more than it holds and makes,
		 * shared with the other generators it feeds); without a grid (tests) its income is as recorded.
		 */
		void advance(long now, @Nullable ServerLevel level, @Nullable PowerGrid grid) {
			double left = now - at;
			at = Math.max(at, now);
			if (left <= 0)
				return;
			int n = fuels.length;
			double[] in = new double[n], took = new double[n];
			// per fuel, the Wellsprings it draws on this time and the rate each gives
			SpringPaper[][] from = null;
			double[][] gives = null;
			for (int i = 0; i < n; i++) {
				Fuel f = fuels[i];
				if (f.income.isEmpty())
					continue;
				if (level == null || grid == null) {
					in[i] = income(f, null, null);
					continue;
				}
				if (from == null) {
					from = new SpringPaper[n][];
					gives = new double[n][];
				}
				from[i] = new SpringPaper[f.income.size()];
				gives[i] = new double[f.income.size()];
				int j = 0;
				for (Map.Entry<BlockPos, Double> e : f.income.entrySet()) {
					SpringPaper s = running(level, e.getKey()) ? null : grid.springFor(e.getKey(), f);
					if (s != null) {
						double r = Math.min(e.getValue(), s.offer(this, e.getValue(), left, now) / left);
						from[i][j] = s;
						gives[i][j] = r;
						in[i] += r;
					}
					j++;
				}
			}
			// it's away (or catching up on the time it was): its away rate
			int flatOut = paperRate(level, grid);
			for (int step = 0; step < 8 && left > 1e-9; step++) {
				// how much of full output it makes as its fuels stand: all, or (with one run dry) what that one is brought
				double run = flatOut <= 0 ? 0 : 1;
				for (int i = 0; i < n; i++)
					if (fuels[i].amount <= 1e-9)
						run = Math.min(run, fuels[i].use <= 0 ? 1 : Math.min(1, in[i] / fuels[i].use));
				double until = left;
				for (int i = 0; i < n; i++) {
					double change = in[i] - fuels[i].use * run;
					if (change < 0 && fuels[i].amount > 1e-9)
						until = Math.min(until, fuels[i].amount / -change);
				}
				for (int i = 0; i < n; i++) {
					double next = fuels[i].amount + (in[i] - fuels[i].use * run) * until;
					// what it was brought, less what a full tank turned away
					took[i] += Math.max(0, in[i] * until - Math.max(0, next - fuels[i].cap));
					fuels[i].amount = Math.max(0, Math.min(fuels[i].cap, next));
				}
				store = Math.min(storeCap, store + flatOut * run * until);
				left -= until;
			}
			// each Wellspring gives its part of what the fuel took in
			if (from != null) {
				boolean drew = false;
				for (int i = 0; i < n; i++)
					if (from[i] != null && in[i] > 0)
						for (int j = 0; j < from[i].length; j++)
							if (from[i][j] != null && gives[i][j] > 0) {
								from[i][j].take(took[i] * gives[i][j] / in[i]);
								from[i][j].settle(level);
								drew = true;
							}
				if (drew)
					grid.records.setDirty();
			}
		}
	}

	/**
	 * A Wellspring that feeds a generator, for running it on paper: what it holds and can hold (mB), what it makes a tick
	 * and of which fluid. While it runs it writes its state here every tick; while it's away, the generators it feeds on
	 * paper draw their fuel from this record (never more than it holds and makes, shared between them), and when it runs
	 * again its tank takes on how far the record went down, once. Never up: a Wellspring makes nothing on paper beyond
	 * what was drawn from it.
	 * <p>
	 * What else really takes from its tank meanwhile (a Fluid Line still running in its loaded chunk) comes off the
	 * record too ({@link PowerGrid#springDrained}), so the record never holds more than the tank. And while its chunk is
	 * loaded though not running, what paper draws from its stock comes out of the real tank at once (so the line can't
	 * take it again), a whole mB at a time.
	 */
	public static final class SpringPaper {
		final BlockPos pos;
		double amount, seen;
		int cap;
		/** mB a tick its generation upgrades make. */
		double rate;
		@Nullable
		Fluid fluid;
		/** Its making counted up to the tick {@code at}; the Wellspring last wrote it at {@code seenAt}. */
		long at, seenAt;
		/**
		 * What it made on paper and nothing has drawn yet (drawn first: owed by no tank), and what paper drew from its
		 * stock that its tank hasn't given up yet.
		 */
		double made, owed;
		/** Not saved: the records drawing on it lately, each with its income from it and the tick it last asked. */
		private final Map<Paper, double[]> takers = new java.util.IdentityHashMap<>();
		/** Not saved: all paper has drawn from it (game tests). */
		double given;

		SpringPaper(BlockPos pos) {
			this.pos = pos.immutable();
		}

		/** Its making counted up to the tick now (up to full). */
		void roll(long now) {
			if (now <= at)
				return;
			if (amount < cap) {
				double next = Math.min(cap, amount + rate * (now - at));
				made += next - amount;
				amount = next;
			}
			at = now;
		}

		/** Who's drawing on it now: this record's part, by income, of all those that asked in the last two seconds. */
		private double share(Paper who, double income, long now) {
			double[] mine = takers.computeIfAbsent(who, k -> new double[2]);
			mine[0] = income;
			mine[1] = now;
			double all = 0;
			java.util.Iterator<double[]> it = takers.values().iterator();
			while (it.hasNext()) {
				double[] t = it.next();
				if (now - t[1] > 40)
					it.remove();
				else
					all += t[0];
			}
			return all <= 0 ? 1 : Math.min(1, income / all);
		}

		/** mB it can give one record over the next {@code ticks}, at most that record's income (taken with {@link #take}). */
		double offer(Paper who, double income, double ticks, long now) {
			roll(now);
			return Math.max(0, Math.min(income * ticks, amount * share(who, income, now)));
		}

		/**
		 * mB a tick it gives one record as it stands: its income while its part of what it holds lasts a second, else its
		 * part of what it makes.
		 */
		double givesNow(Paper who, double income, long now) {
			roll(now);
			double share = share(who, income, now);
			return amount * share >= income * 20 ? income : Math.min(income, rate * share);
		}

		void take(double mb) {
			double t = Math.max(0, Math.min(mb, amount));
			amount -= t;
			given += t;
			double fromMade = Math.min(t, made);
			made -= fromMade;
			owed += t - fromMade;
		}

		/** Its chunk is loaded (though not running): what paper drew from its stock leaves its tank now, in whole mB. */
		void settle(@Nullable ServerLevel level) {
			if (owed < 1 || level == null || !level.isLoaded(pos)
				|| !(level.getBlockEntity(pos) instanceof com.comedymonkey.createaddons.content.wellspring.WellspringBlockEntity w))
				return;
			int n = (int) Math.floor(owed);
			owed -= n;
			seen = Math.max(0, seen - w.drawnOnPaper(n));
		}

		public double amount() {
			return amount;
		}
	}

	/**
	 * Whether a generator's own block can give out its power or fuel now: not while its chunk isn't running (it's on
	 * paper then, and its record is what runs it; anything else taking from it would spend the same twice).
	 */
	public static boolean givesHere(net.minecraft.world.level.Level level, BlockPos pos) {
		return !(level instanceof ServerLevel server) || running(server, pos);
	}

	/** A block is running: loaded, and its chunk ticking. Never loads a chunk to ask. */
	static boolean running(ServerLevel level, BlockPos pos) {
		return !away(level, pos) && level.isLoaded(pos) && level.shouldTickBlocksAt(pos);
	}

	/**
	 * Game tests only: generators treated as away (on paper) without unloading their chunks, until the tick each maps to
	 * (so a test that fails half way leaves nothing behind for long). See {@link PowerGridTesting}.
	 */
	static final Map<BlockPos, Long> AWAY = new HashMap<>();

	/** Game tests only: a generator marked away (it doesn't run, as if its chunk weren't). */
	public static boolean away(net.minecraft.world.level.Level level, BlockPos pos) {
		if (AWAY.isEmpty())
			return false;
		Long until = AWAY.get(pos);
		if (until == null)
			return false;
		if (level.getGameTime() < until)
			return true;
		AWAY.remove(pos);
		return false;
	}

	final Map<Node, Piece> pieces = new HashMap<>();
	/** Who links to each piece (the other way round from its own links). Not saved: worked out from the pieces. */
	private final Map<Node, Set<Node>> linkedFrom = new HashMap<>();
	/** How many pieces' ends meet each block (a face is dropped when none does). Not saved: worked out from the pieces. */
	private final Map<BlockPos, Integer> endRefs = new HashMap<>();
	/** The pieces whose ends meet each block. Not saved: worked out from the pieces. */
	private final Map<BlockPos, Set<Node>> endsAt = new HashMap<>();
	/** The generators' records, saved on their own (they change every second; the pieces hardly ever). */
	private Records records = new Records();
	private Map<BlockPos, Paper> generators = records.generators;
	/** Where a line meets each generator (the block a wire plugs into: itself, or a turbine's port casing), to its key. */
	private final Map<BlockPos, BlockPos> faces = new HashMap<>();
	/** How many faces each generator has: only generators a line meets keep a record (and those in a row with one). */
	private final Map<BlockPos, Integer> faceCount = new HashMap<>();
	/** Each generator's faces (the other way round from {@link #faces}). Not saved: worked out from the faces. */
	private final Map<BlockPos, Set<BlockPos>> facesOf = new HashMap<>();
	/** Generators in a recorded generator's row (they share its store, so they keep a record too). Not saved. */
	private final Set<BlockPos> rowMembers = new HashSet<>();
	/** Each block of a recorded store, to the store's key. Not saved: worked out from the records. */
	private final Map<BlockPos, BlockPos> storeMembers = new HashMap<>();
	/** Where lines meet each store, by its key (the other way round from the store faces). Not saved. */
	private final Map<BlockPos, Set<BlockPos>> storeFacesByKey = new HashMap<>();
	/**
	 * Each free source a dynamo's record counts, to the generator that last saw it (see {@link #lastToSee}). Not saved:
	 * found again the slow way when it doesn't hold.
	 */
	private final Map<BlockPos, BlockPos> lastSeer = new HashMap<>();
	/** The Wellsprings some generator's record has an income from: they keep a record too. Not saved. */
	private final Set<BlockPos> springsWanted = new HashSet<>();

	private static final SavedData.Factory<PowerGrid> FACTORY = new SavedData.Factory<>(PowerGrid::new, PowerGrid::load, null);
	private static final SavedData.Factory<Records> RECORDS = new SavedData.Factory<>(Records::new, Records::load, null);

	public static PowerGrid of(ServerLevel level) {
		PowerGrid grid = level.getDataStorage().computeIfAbsent(FACTORY, NAME);
		if (!grid.recordsLoaded) {
			grid.recordsLoaded = true;
			grid.records = level.getDataStorage().computeIfAbsent(RECORDS, NAME + "_generators");
			grid.generators = grid.records.generators;
			for (Paper p : grid.generators.values()) {
				grid.rowMembers.addAll(p.row);
				for (Fuel f : p.fuels)
					grid.springsWanted.addAll(f.income.keySet());
			}
			for (Map.Entry<BlockPos, StorePaper> e : grid.records.stores.entrySet())
				for (BlockPos m : e.getValue().members)
					grid.storeMembers.put(m, e.getKey());
			for (Map.Entry<BlockPos, BlockPos> e : grid.records.storeFaces.entrySet())
				grid.storeFacesByKey.computeIfAbsent(e.getValue(), k -> new HashSet<>()).add(e.getKey());
		}
		return grid;
	}

	private boolean recordsLoaded;

	/** The generators' records (their own saved data). */
	Records records() {
		return records;
	}

	/** The generators' records, as their own saved data. */
	static final class Records extends SavedData {
		final Map<BlockPos, Paper> generators = new HashMap<>();
		/** Shared stores' records by their key, and where lines meet them (a block of one, to its key). */
		final Map<BlockPos, StorePaper> stores = new HashMap<>();
		final Map<BlockPos, BlockPos> storeFaces = new HashMap<>();
		/** Consumers that run on paper while away, by their block. */
		final Map<BlockPos, ConsumerPaper> consumers = new HashMap<>();
		/** The records of Wellsprings feeding generators, by where they are. */
		final Map<BlockPos, SpringPaper> springs = new HashMap<>();

		@Nullable
		private static Fluid fluid(CompoundTag c) {
			if (!c.contains("Fluid", Tag.TAG_STRING))
				return null;
			net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(c.getString("Fluid"));
			Fluid f = id == null ? null : net.minecraft.core.registries.BuiltInRegistries.FLUID.get(id);
			return f == null || f == net.minecraft.world.level.material.Fluids.EMPTY ? null : f;
		}

		private static void putFluid(CompoundTag c, @Nullable Fluid f) {
			if (f != null && f != net.minecraft.world.level.material.Fluids.EMPTY)
				c.putString("Fluid", net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(f).toString());
		}

		static Records load(CompoundTag tag, HolderLookup.Provider registries) {
			Records r = new Records();
			for (Tag t : tag.getList("Generators", Tag.TAG_COMPOUND)) {
				CompoundTag c = (CompoundTag) t;
				ListTag fl = c.getList("Fuels", Tag.TAG_COMPOUND);
				Paper p = new Paper(c.getString("Kind"), fl.size());
				p.set(c.getInt("Rate"), c.getDouble("Store"), c.getLong("StoreCap"));
				if (c.contains("FreeShares")) {
					Map<BlockPos, Double> shares = new LinkedHashMap<>();
					for (Tag s : c.getList("FreeShares", Tag.TAG_COMPOUND))
						shares.put(BlockPos.of(((CompoundTag) s).getLong("Pos")), ((CompoundTag) s).getDouble("Share"));
					p.freeShares = Map.copyOf(shares);
				}
				p.seenStore = c.getDouble("SeenStore");
				p.at = c.getLong("At");
				p.seenAt = c.getLong("SeenAt");
				List<BlockPos> row = new ArrayList<>();
				for (long rp : c.getLongArray("Row"))
					row.add(BlockPos.of(rp));
				p.row = row;
				for (int i = 0; i < fl.size(); i++) {
					CompoundTag f = fl.getCompound(i);
					Fuel fuel = p.fuels[i];
					fuel.set(f.getDouble("Amount"), f.getInt("Cap"), f.getDouble("Use"));
					fuel.seen = f.getDouble("Seen");
					fuel.fluid = fluid(f);
					for (Tag it : f.getList("Income", Tag.TAG_COMPOUND))
						fuel.income.put(BlockPos.of(((CompoundTag) it).getLong("From")), ((CompoundTag) it).getDouble("Rate"));
				}
				r.generators.put(BlockPos.of(c.getLong("Pos")), p);
			}
			for (Tag t : tag.getList("Stores", Tag.TAG_COMPOUND)) {
				CompoundTag c = (CompoundTag) t;
				StorePaper p = new StorePaper();
				p.energy = c.getDouble("Energy");
				p.seen = c.getDouble("Seen");
				p.cap = c.getLong("Cap");
				p.perTick = c.getLong("PerTick");
				p.whole = c.getBoolean("Whole");
				List<BlockPos> members = new ArrayList<>();
				for (long mp : c.getLongArray("Members"))
					members.add(BlockPos.of(mp));
				p.members = members;
				r.stores.put(BlockPos.of(c.getLong("Pos")), p);
			}
			for (Tag t : tag.getList("StoreFaces", Tag.TAG_COMPOUND))
				r.storeFaces.put(BlockPos.of(((CompoundTag) t).getLong("Face")), BlockPos.of(((CompoundTag) t).getLong("Key")));
			for (Tag t : tag.getList("Consumers", Tag.TAG_COMPOUND)) {
				CompoundTag c = (CompoundTag) t;
				ConsumerPaper p = new ConsumerPaper();
				p.energy = c.getDouble("Energy");
				p.cap = c.getLong("Cap");
				p.perTick = c.getLong("PerTick");
				r.consumers.put(BlockPos.of(c.getLong("Pos")), p);
			}
			for (Tag t : tag.getList("Springs", Tag.TAG_COMPOUND)) {
				CompoundTag c = (CompoundTag) t;
				SpringPaper s = new SpringPaper(BlockPos.of(c.getLong("Pos")));
				s.amount = c.getDouble("Amount");
				s.seen = c.getDouble("Seen");
				s.cap = c.getInt("Cap");
				s.rate = c.getDouble("Rate");
				s.fluid = fluid(c);
				s.at = c.getLong("At");
				s.seenAt = c.getLong("SeenAt");
				s.made = c.getDouble("Made");
				s.owed = c.getDouble("Owed");
				r.springs.put(s.pos, s);
			}
			return r;
		}

		@Override
		public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
			ListTag gens = new ListTag();
			for (Map.Entry<BlockPos, Paper> e : generators.entrySet()) {
				Paper p = e.getValue();
				CompoundTag c = new CompoundTag();
				c.putLong("Pos", e.getKey().asLong());
				c.putString("Kind", p.kind);
				c.putInt("Rate", p.rate);
				if (p.freeShares != null) {
					ListTag shares = new ListTag();
					for (Map.Entry<BlockPos, Double> s : p.freeShares.entrySet()) {
						CompoundTag sc = new CompoundTag();
						sc.putLong("Pos", s.getKey().asLong());
						sc.putDouble("Share", s.getValue());
						shares.add(sc);
					}
					c.put("FreeShares", shares);
				}
				c.putDouble("Store", p.store);
				c.putDouble("SeenStore", p.seenStore);
				c.putLong("StoreCap", p.storeCap);
				c.putLong("At", p.at);
				c.putLong("SeenAt", p.seenAt);
				c.put("Row", new LongArrayTag(p.row.stream().mapToLong(BlockPos::asLong).toArray()));
				ListTag fl = new ListTag();
				for (Fuel f : p.fuels) {
					CompoundTag fc = new CompoundTag();
					fc.putDouble("Amount", f.amount);
					fc.putDouble("Seen", f.seen);
					fc.putInt("Cap", f.cap);
					fc.putDouble("Use", f.use);
					putFluid(fc, f.fluid);
					ListTag in = new ListTag();
					for (Map.Entry<BlockPos, Double> i : f.income.entrySet()) {
						CompoundTag ic = new CompoundTag();
						ic.putLong("From", i.getKey().asLong());
						ic.putDouble("Rate", i.getValue());
						in.add(ic);
					}
					fc.put("Income", in);
					fl.add(fc);
				}
				c.put("Fuels", fl);
				gens.add(c);
			}
			tag.put("Generators", gens);
			ListTag stores = new ListTag();
			for (Map.Entry<BlockPos, StorePaper> e : this.stores.entrySet()) {
				StorePaper p = e.getValue();
				CompoundTag c = new CompoundTag();
				c.putLong("Pos", e.getKey().asLong());
				c.putDouble("Energy", p.energy);
				c.putDouble("Seen", p.seen);
				c.putLong("Cap", p.cap);
				c.putLong("PerTick", p.perTick);
				if (p.whole)
					c.putBoolean("Whole", true);
				c.put("Members", new LongArrayTag(p.members.stream().mapToLong(BlockPos::asLong).toArray()));
				stores.add(c);
			}
			tag.put("Stores", stores);
			ListTag faces = new ListTag();
			for (Map.Entry<BlockPos, BlockPos> e : storeFaces.entrySet()) {
				CompoundTag c = new CompoundTag();
				c.putLong("Face", e.getKey().asLong());
				c.putLong("Key", e.getValue().asLong());
				faces.add(c);
			}
			tag.put("StoreFaces", faces);
			ListTag consumerList = new ListTag();
			for (Map.Entry<BlockPos, ConsumerPaper> e : consumers.entrySet()) {
				CompoundTag c = new CompoundTag();
				c.putLong("Pos", e.getKey().asLong());
				c.putDouble("Energy", e.getValue().energy);
				c.putLong("Cap", e.getValue().cap);
				c.putLong("PerTick", e.getValue().perTick);
				consumerList.add(c);
			}
			tag.put("Consumers", consumerList);
			ListTag springList = new ListTag();
			for (Map.Entry<BlockPos, SpringPaper> e : springs.entrySet()) {
				SpringPaper s = e.getValue();
				CompoundTag c = new CompoundTag();
				c.putLong("Pos", e.getKey().asLong());
				c.putDouble("Amount", s.amount);
				c.putDouble("Seen", s.seen);
				c.putInt("Cap", s.cap);
				c.putDouble("Rate", s.rate);
				putFluid(c, s.fluid);
				c.putLong("At", s.at);
				c.putLong("SeenAt", s.seenAt);
				c.putDouble("Made", s.made);
				c.putDouble("Owed", s.owed);
				springList.add(c);
			}
			tag.put("Springs", springList);
			return tag;
		}
	}

	// ---------- pieces ----------

	@Nullable
	Piece piece(Node node) {
		return pieces.get(node);
	}

	/** Sets the piece read at node (null: there's none now). */
	void put(Node node, @Nullable Piece piece) {
		Piece old = piece == null ? pieces.remove(node) : pieces.put(node, piece);
		if (piece != null)
			for (End e : piece.ends)
				endRefs.merge(e.target(), 1, Integer::sum);
		if (old != null) {
			for (Link l : old.links)
				unlink(node, l.to());
			for (End e : old.ends)
				if (endRefs.merge(e.target(), -1, Integer::sum) <= 0) {
					endRefs.remove(e.target());
					dropFace(e.target());
					dropStoreFace(e.target());
				}
		}
		if (piece != null)
			for (Link l : piece.links)
				linkedFrom.computeIfAbsent(l.to(), k -> new HashSet<>()).add(node);
		if (old != null)
			for (End e : old.ends) {
				Set<Node> s = endsAt.get(e.target());
				if (s != null && s.remove(node) && s.isEmpty())
					endsAt.remove(e.target());
			}
		if (piece != null)
			for (End e : piece.ends)
				endsAt.computeIfAbsent(e.target(), k -> new HashSet<>()).add(node);
		if (old != null || piece != null)
			setDirty();
	}

	/** The pieces whose ends meet a block. */
	Set<Node> endsAt(BlockPos pos) {
		return endsAt.getOrDefault(pos, Set.of());
	}

	/** The pieces that meet the generator recorded at key, on any of its faces (through it, they're one network). */
	Set<Node> meeting(BlockPos key) {
		Set<Node> out = new HashSet<>();
		for (BlockPos f : facesOf.getOrDefault(key, Set.of()))
			out.addAll(endsAt.getOrDefault(f, Set.of()));
		return out;
	}

	private void unlink(Node from, Node to) {
		Set<Node> s = linkedFrom.get(to);
		if (s != null && s.remove(from) && s.isEmpty())
			linkedFrom.remove(to);
	}

	/** The pieces joined to this one: both ends list the join (so one cut at either end is cut). */
	List<Node> neighbours(Node node) {
		List<Node> out = new ArrayList<>();
		Piece p = pieces.get(node);
		if (p == null)
			return out;
		Set<Node> back = linkedFrom.getOrDefault(node, Set.of());
		for (Link l : p.links)
			if (back.contains(l.to()) && !out.contains(l.to()))
				out.add(l.to());
		return out;
	}

	/** The pieces that list a join to this one, whether or not it lists them back (whose networks a change touches). */
	Set<Node> listing(Node node) {
		return linkedFrom.getOrDefault(node, Set.of());
	}

	/** The tier of the cable joining a and b, from a's link (each end's link carries its own tier). */
	@Nullable
	LineTier linkTier(Node a, Node b) {
		Link l = linkTo(a, b);
		return l == null ? null : l.tier();
	}

	/** Every link from a to b: spans of several tiers and colours can run between one pair of poles. */
	List<Link> linksTo(Node a, Node b) {
		Piece pa = pieces.get(a);
		if (pa == null)
			return List.of();
		List<Link> out = new ArrayList<>(1);
		for (Link l : pa.links)
			if (l.to().equals(b))
				out.add(l);
		return out;
	}

	/** a's link to b, if it has one. */
	@Nullable
	Link linkTo(Node a, Node b) {
		Piece pa = pieces.get(a);
		if (pa != null)
			for (Link l : pa.links)
				if (l.to().equals(b))
					return l;
		return null;
	}

	// ---------- generators ----------

	/** A generator's chunk is running: if it was on paper meanwhile, it takes on how far its record moved. */
	public void back(Generator g, ServerLevel level) {
		BlockPos key = g.generatorKey();
		Paper p = generators.get(key);
		long now = level.getGameTime();
		if (p == null || p.seenAt >= now - 1 || away(level, key))
			return;
		String kind = g.paperKind();
		// a record of some other kind of generator that stood here: not this one's
		if (kind == null || !kind.equals(p.kind) || g.paperFuels() != p.fuels.length) {
			generators.remove(key);
			setDirty();
			return;
		}
		p.advance(now - 1, level, this);
		double[] fuel = new double[p.fuels.length];
		for (int i = 0; i < fuel.length; i++)
			fuel[i] = p.fuels[i].amount - p.fuels[i].seen;
		g.resume(p.store - p.seenStore, fuel);
		// taken on once: from here the record starts again from where it stands
		p.seenStore = p.store;
		for (Fuel f : p.fuels)
			f.seen = f.amount;
		p.seenAt = now - 1;
		if (!needsRecord(key))
			generators.remove(key);
		records.setDirty();
	}

	/** Whether a generator keeps a record: a line meets it, or it's in a row with one a line meets. */
	private boolean needsRecord(BlockPos key) {
		return faceCount.containsKey(key) || rowMembers.contains(key);
	}

	/** Records of generators that are away worked forward (about once a second), so a fuel source that comes back with them still counted for the time away. */
	void tickAway(ServerLevel level, long now) {
		// the Wellsprings the records draw on keep theirs; one no record draws on any more goes once nothing is owed, and
		// one that's loaded and no Wellspring now (broken, carried off) goes at once
		springsWanted.clear();
		for (Paper p : generators.values())
			for (Fuel f : p.fuels)
				springsWanted.addAll(f.income.keySet());
		if (records.springs.entrySet().removeIf(e -> level.isLoaded(e.getKey())
			&& !(level.getBlockEntity(e.getKey()) instanceof com.comedymonkey.createaddons.content.wellspring.WellspringBlockEntity)
			|| !springsWanted.contains(e.getKey()) && Math.abs(e.getValue().amount - e.getValue().seen) < 0.5))
			records.setDirty();
		for (Map.Entry<BlockPos, Paper> e : generators.entrySet()) {
			if (running(level, e.getKey()))
				continue;
			Paper p = e.getValue();
			// a source that's loaded and no Wellspring now (broken) stops counting
			for (Fuel f : p.fuels)
				if (f.income.keySet().removeIf(src -> level.isLoaded(src)
					&& !(level.getBlockEntity(src) instanceof com.comedymonkey.createaddons.content.wellspring.WellspringBlockEntity)))
					records.setDirty();
			// ...and a dynamo's free source that's loaded and no free source now (broken, replaced)
			if (p.freeShares != null && p.freeShares.keySet().stream().anyMatch(src -> !stillFree(level, src))) {
				Map<BlockPos, Double> kept = new LinkedHashMap<>();
				p.freeShares.forEach((src, share) -> {
					if (stillFree(level, src))
						kept.put(src, share);
				});
				p.freeShares = Map.copyOf(kept);
				records.setDirty();
			}
			if (now - p.at >= 20) {
				p.advance(now, level, this);
				records.setDirty();
			}
		}
	}

	/** A free source counts while it's unloaded (nothing can change it) or still a free source's block. Never loads a chunk. */
	private static boolean stillFree(ServerLevel level, BlockPos src) {
		return !level.isLoaded(src) || level.getBlockState(src).is(com.comedymonkey.createaddons.content.electric.ClockworkDynamoBlockEntity.FREE_SOURCES);
	}

	/**
	 * Of the records that count a free source, the one whose generator saw it last (it's in no record: null). Kept as it
	 * goes ({@link #note}); looked for through every record only when that no longer holds (it dropped the source, or
	 * went).
	 */
	@Nullable
	Paper lastToSee(BlockPos source) {
		BlockPos k = lastSeer.get(source);
		Paper known = k == null ? null : generators.get(k);
		if (known != null && known.freeShares != null && known.freeShares.containsKey(source))
			return known;
		Paper last = null;
		BlockPos lastKey = null;
		for (Map.Entry<BlockPos, Paper> e : generators.entrySet()) {
			Paper p = e.getValue();
			if (p.freeShares != null && p.freeShares.containsKey(source) && (last == null || p.seenAt > last.seenAt)) {
				last = p;
				lastKey = e.getKey();
			}
		}
		if (lastKey == null)
			lastSeer.remove(source);
		else
			lastSeer.put(source.immutable(), lastKey);
		return last;
	}

	/** A generator has run this tick: its record as it now stands (only if a line meets it). */
	public void note(Generator g, ServerLevel level) {
		BlockPos key = g.generatorKey();
		if (away(level, key))
			return;
		if (!needsRecord(key)) {
			if (generators.remove(key) != null)
				records.setDirty();
			return;
		}
		Paper p = generators.get(key);
		long now = level.getGameTime();
		// a record of some other kind of generator that stood here (replaced within the tick): not this one's
		String kind = g.paperKind();
		if (kind == null)
			return;
		if (p != null && (!kind.equals(p.kind) || g.paperFuels() != p.fuels.length)) {
			generators.remove(key);
			p = null;
		}
		boolean refresh = p == null || Math.floorMod(key.hashCode() + now, 20) == 0;
		if (p == null) {
			p = g.newPaper();
			if (p == null)
				return;
			generators.put(key.immutable(), p);
		}
		g.record(p, refresh);
		if (refresh) {
			rowMembers.addAll(p.row);
			for (Fuel f : p.fuels)
				springsWanted.addAll(f.income.keySet());
		}
		p.seenStore = p.store;
		for (Fuel f : p.fuels)
			f.seen = f.amount;
		p.at = p.seenAt = now;
		// it has seen its free sources last now
		if (p.freeShares != null)
			for (BlockPos s : p.freeShares.keySet())
				if (!key.equals(lastSeer.get(s)))
					lastSeer.put(s, key.immutable());
		if (refresh)
			records.setDirty();
	}

	// ---------- Wellsprings ----------

	/** The record of the Wellspring at pos, if it has one and it holds (or makes) this fuel's fluid. */
	@Nullable
	SpringPaper springFor(BlockPos pos, Fuel f) {
		SpringPaper s = records.springs.get(pos);
		if (s == null || f.fluid != null && s.fluid != null && !s.fluid.isSame(f.fluid))
			return null;
		return s;
	}

	/** Game tests: the record of the Wellspring at pos. */
	@Nullable
	SpringPaper springPaper(BlockPos pos) {
		return records.springs.get(pos);
	}

	/**
	 * A Fluid Line took mb from the Wellspring at pos for real: its record goes down by as much (running or not), so
	 * generators on paper never draw on fluid that's gone, and the drop taken on when it runs again is only theirs.
	 */
	void springDrained(BlockPos pos, int mb) {
		SpringPaper s = records.springs.get(pos);
		if (s == null || mb <= 0)
			return;
		s.amount = Math.max(0, s.amount - mb);
		s.seen = Math.max(0, s.seen - mb);
	}

	/**
	 * A Wellspring has run this tick. Back from being away, its tank first takes on how far its record went down while
	 * it was (what generators on paper drew from it, less what it made meanwhile; never up). Then, if a generator's
	 * record draws on it, its record is written as it now stands.
	 */
	public void spring(com.comedymonkey.createaddons.content.wellspring.WellspringBlockEntity w, ServerLevel level) {
		BlockPos pos = w.getBlockPos();
		SpringPaper s = records.springs.get(pos);
		long now = level.getGameTime();
		if (s == null) {
			if (!springsWanted.contains(pos))
				return;
			s = new SpringPaper(pos);
			records.springs.put(s.pos, s);
			records.setDirty();
		} else if (s.seenAt < now - 1) {
			s.roll(now - 1);
			double down = s.seen - s.amount;
			// rounded up: it never ends up with more than the record left it
			if (down > 1e-6)
				w.drawnOnPaper((int) Math.ceil(down - 1e-6));
			records.setDirty();
		}
		net.neoforged.neoforge.fluids.FluidStack held = w.getTank().getFluid();
		Fluid makes = w.getGenerationSource().fluid();
		s.amount = s.seen = w.getTank().getFluidAmount();
		s.made = s.owed = 0;
		s.cap = w.getTank().getCapacity();
		s.rate = w.getGenerationRate() / 20.0;
		s.fluid = !held.isEmpty() ? held.getFluid() : makes == net.minecraft.world.level.material.Fluids.EMPTY ? null : makes;
		s.at = s.seenAt = now;
		if (Math.floorMod(pos.hashCode() + now, 20) == 0)
			records.setDirty();
	}

	/** A line meets the generator recorded at key at the block face. */
	void face(BlockPos face, BlockPos key) {
		BlockPos old = faces.put(face.immutable(), key.immutable());
		if (key.equals(old))
			return;
		if (old != null) {
			faceCount.computeIfPresent(old, (k, n) -> n <= 1 ? null : n - 1);
			unface(old, face);
		}
		faceCount.merge(key.immutable(), 1, Integer::sum);
		facesOf.computeIfAbsent(key.immutable(), k -> new HashSet<>()).add(face.immutable());
		setDirty();
	}

	private void unface(BlockPos key, BlockPos face) {
		Set<BlockPos> s = facesOf.get(key);
		if (s != null && s.remove(face) && s.isEmpty())
			facesOf.remove(key);
	}

	@Nullable
	BlockPos keyAt(BlockPos face) {
		return faces.get(face);
	}

	/** No line meets a generator at this block any more (or it's no generator now). */
	void dropFace(BlockPos face) {
		BlockPos key = faces.remove(face);
		if (key == null)
			return;
		faceCount.computeIfPresent(key, (k, n) -> n <= 1 ? null : n - 1);
		unface(key, face);
		if (!needsRecord(key))
			generators.remove(key);
		setDirty();
		records.setDirty();
	}

	@Nullable
	Paper paper(BlockPos key) {
		return generators.get(key);
	}

	/** The generator at key and every one its row of touching generators reaches (they share their stores). */
	List<BlockPos> row(BlockPos key) {
		List<BlockPos> out = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		seen.add(key);
		queue.add(key);
		while (!queue.isEmpty()) {
			BlockPos k = queue.poll();
			out.add(k);
			Paper p = generators.get(k);
			if (p != null)
				for (BlockPos o : p.row)
					if (seen.add(o) && out.size() < 64)
						queue.add(o);
		}
		return out;
	}

	// ---------- shared stores ----------

	/** A line meets the shared store keyed key at block face. */
	void storeFace(BlockPos face, BlockPos key) {
		BlockPos old = records.storeFaces.put(face.immutable(), key.immutable());
		if (key.equals(old))
			return;
		if (old != null)
			unStoreFace(old, face);
		storeFacesByKey.computeIfAbsent(key.immutable(), k -> new HashSet<>()).add(face.immutable());
		records.setDirty();
	}

	private void unStoreFace(BlockPos key, BlockPos face) {
		Set<BlockPos> s = storeFacesByKey.get(key);
		if (s != null && s.remove(face) && s.isEmpty())
			storeFacesByKey.remove(key);
	}

	/** The key of the shared store a line meets at block face, as last seen running. */
	@Nullable
	BlockPos storeKeyFor(BlockPos face) {
		return records.storeFaces.get(face);
	}

	/** Every block where a line meets the store keyed key. */
	Set<BlockPos> storeFacesOf(BlockPos key) {
		return storeFacesByKey.getOrDefault(key, Set.of());
	}

	/** No line meets a store at this block now: once none meets it anywhere, its record goes (unless it's owed). */
	private void dropStoreFace(BlockPos face) {
		BlockPos key = records.storeFaces.remove(face);
		if (key == null)
			return;
		unStoreFace(key, face);
		StorePaper p = records.stores.get(key);
		// a record that moved on paper is kept until the store takes it back, line or no line
		if (!storeFacesByKey.containsKey(key) && (p == null || Math.abs(p.energy - p.seen) < 0.5))
			removeStore(key);
		records.setDirty();
	}

	@Nullable
	public StorePaper storePaper(BlockPos key) {
		return records.stores.get(key);
	}

	private void removeStore(BlockPos key) {
		StorePaper p = records.stores.remove(key);
		if (p != null) {
			for (BlockPos m : p.members)
				storeMembers.remove(m, key);
			records.setDirty();
		}
	}

	/**
	 * A shared store has run this tick: its record as it stands, if a line meets any block of it ({@code members} is
	 * read only when {@code refresh}, or the record is new). Records of other stores its blocks were in go.
	 */
	public void noteStore(BlockPos key, java.util.function.Supplier<List<BlockPos>> members, long energy, long cap, long perTick, boolean whole,
						  boolean refresh) {
		StorePaper p = records.stores.get(key);
		// a new record only on a refresh (once a second): a store no line meets isn't looked at every tick
		if (p == null && !refresh)
			return;
		if (p == null || refresh) {
			List<BlockPos> list = members.get();
			boolean met = false;
			for (BlockPos m : list)
				if (records.storeFaces.containsKey(m)) {
					met = true;
					break;
				}
			if (!met) {
				removeStore(key);
				return;
			}
			if (p == null) {
				p = new StorePaper();
				records.stores.put(key.immutable(), p);
			}
			for (BlockPos m : p.members)
				storeMembers.remove(m, key);
			p.members = List.copyOf(list);
			for (BlockPos m : list) {
				BlockPos old = storeMembers.put(m.immutable(), key.immutable());
				if (old != null && !old.equals(key))
					removeStore(old);
			}
			records.setDirty();
		}
		p.energy = p.seen = energy;
		p.cap = cap;
		p.perTick = perTick;
		p.whole = whole;
	}

	/**
	 * A shared store made of these blocks is running again (or was worked out anew): how far the records its blocks were
	 * in moved while it was away, taken on once (the records go; it writes a fresh one as it runs).
	 */
	public long storeBack(java.util.Collection<BlockPos> members) {
		Set<BlockPos> keys = new HashSet<>(), here = new HashSet<>(members);
		for (BlockPos m : members) {
			BlockPos k = storeMembers.get(m);
			if (k != null)
				keys.add(k);
		}
		double delta = 0;
		for (BlockPos k : keys) {
			StorePaper p = records.stores.get(k);
			if (p == null)
				continue;
			List<BlockPos> back = new ArrayList<>(), still = new ArrayList<>();
			for (BlockPos m : p.members)
				(here.contains(m) ? back : still).add(m);
			if (still.isEmpty()) {
				delta += p.energy - p.seen;
				removeStore(k);
				continue;
			}
			// only some of it is back: they take their part, and the record goes on for the rest, re-keyed if its key came back
			double part = back.size() / (double) p.members.size();
			delta += (p.energy - p.seen) * part;
			p.energy *= 1 - part;
			p.seen *= 1 - part;
			p.cap = Math.round(p.cap * (1 - part));
			p.perTick = Math.max(1, Math.round(p.perTick * (1 - part)));
			for (BlockPos m : back)
				storeMembers.remove(m, k);
			p.members = List.copyOf(still);
			if (here.contains(k))
				rekey(k, still.get(0));
			records.setDirty();
		}
		return Math.round(delta);
	}

	/** A record's key changes (its old key's block came back on its own): its members and the faces at them follow. */
	private void rekey(BlockPos from, BlockPos to) {
		StorePaper p = records.stores.remove(from);
		if (p == null)
			return;
		records.stores.put(to.immutable(), p);
		Set<BlockPos> members = new HashSet<>(p.members);
		for (BlockPos m : p.members)
			storeMembers.put(m, to.immutable());
		for (BlockPos face : new ArrayList<>(storeFacesOf(from)))
			if (members.contains(face)) {
				records.storeFaces.put(face, to.immutable());
				unStoreFace(from, face);
				storeFacesByKey.computeIfAbsent(to.immutable(), k -> new HashSet<>()).add(face);
			}
	}

	/** Every block of a store's record is away: it runs on paper. */
	boolean allAway(StorePaper p, ServerLevel level) {
		for (BlockPos m : p.members)
			if (running(level, m))
				return false;
		return !p.members.isEmpty();
	}

	/**
	 * A record as something the network fills and draws, at the store's rate a tick (simulate answers for this tick too;
	 * the network's plan asks for a second's worth through {@link Planned}).
	 */
	IEnergyStorage paperStore(StorePaper p, ServerLevel level) {
		if (p.view == null)
			p.view = new RefundingStorage() {
				@Override
				public int planReceive(int max) {
					return (int) Math.max(0, Math.min(max, Math.min(Math.floor(p.cap - p.energy), p.perTick * 20.0)));
				}

				@Override
				public int planExtract(int max) {
					return (int) Math.max(0, Math.min(max, Math.min(Math.floor(p.energy), p.perTick * 20.0)));
				}

				@Override
				public int refund(int fe) {
					if (fe <= 0)
						return 0;
					roll();
					p.energy += fe;
					p.outNow = Math.max(0, p.outNow - fe);
					records.setDirty();
					return fe;
				}

				private void roll() {
					long now = level.getGameTime();
					if (p.tick != now) {
						p.tick = now;
						p.inNow = p.outNow = 0;
					}
				}

				@Override
				public int receiveEnergy(int max, boolean simulate) {
					roll();
					double limit = p.perTick - p.inNow;
					int got = (int) Math.max(0, Math.min(max, Math.min(Math.floor(p.cap - p.energy), limit)));
					if (!simulate && got > 0) {
						p.energy += got;
						p.inNow += got;
						records.setDirty();
					}
					return got;
				}

				@Override
				public int extractEnergy(int max, boolean simulate) {
					roll();
					double limit = p.perTick - p.outNow;
					int got = (int) Math.max(0, Math.min(max, Math.min(Math.floor(p.energy), limit)));
					if (!simulate && got > 0) {
						p.energy -= got;
						p.outNow += got;
						records.setDirty();
					}
					return got;
				}

				@Override
				public int getEnergyStored() {
					return (int) Math.min(Integer.MAX_VALUE, p.energy);
				}

				@Override
				public int getMaxEnergyStored() {
					return (int) Math.min(Integer.MAX_VALUE, Math.max(p.cap, p.energy));
				}

				@Override
				public boolean canExtract() {
					return true;
				}

				@Override
				public boolean canReceive() {
					return true;
				}
			};
		return p.view;
	}

	/** An energy store that takes refunds too, and is planned a second at once. */
	public interface RefundingStorage extends IEnergyStorage, Refund, Planned {}

	// ---------- consumers on paper ----------

	/**
	 * A consumer has run this second: its draw a tick and how much it can hold of what comes while it's away. It keeps a
	 * record only while a line meets it (with none, nothing reaches it while it's away).
	 */
	public void noteConsumer(BlockPos pos, long perTick, long cap) {
		ConsumerPaper p = records.consumers.get(pos);
		if (endsAt(pos).isEmpty()) {
			if (p != null && p.energy <= 0) {
				records.consumers.remove(pos);
				records.setDirty();
			}
			return;
		}
		if (p == null) {
			p = new ConsumerPaper();
			records.consumers.put(pos.immutable(), p);
		}
		if (p.perTick != perTick || p.cap != cap) {
			p.perTick = perTick;
			p.cap = cap;
			records.setDirty();
		}
	}

	/** A consumer is running again: what it was sent while away and didn't use, taken on once. */
	public double consumerBack(BlockPos pos) {
		ConsumerPaper p = records.consumers.get(pos);
		if (p == null || p.energy <= 0)
			return 0;
		double back = p.energy;
		p.energy = 0;
		records.setDirty();
		return back;
	}

	/** The block isn't that consumer any more (it was broken). */
	public void forgetConsumer(BlockPos pos) {
		if (records.consumers.remove(pos) != null)
			records.setDirty();
	}

	@Nullable
	public ConsumerPaper consumer(BlockPos pos) {
		return records.consumers.get(pos);
	}

	/** A consumer's record as something a running network fills (up to what it can hold). */
	IEnergyStorage consumerStore(ConsumerPaper p) {
		if (p.view == null)
			p.view = new IEnergyStorage() {
				@Override
				public int receiveEnergy(int max, boolean simulate) {
					int got = (int) Math.max(0, Math.min(max, Math.floor(p.cap - p.energy)));
					if (!simulate && got > 0) {
						p.energy += got;
						records.setDirty();
					}
					return got;
				}

				@Override
				public int extractEnergy(int max, boolean simulate) {
					return 0;
				}

				@Override
				public int getEnergyStored() {
					return (int) Math.min(Integer.MAX_VALUE, p.energy);
				}

				@Override
				public int getMaxEnergyStored() {
					return (int) Math.min(Integer.MAX_VALUE, p.cap);
				}

				@Override
				public boolean canExtract() {
					return false;
				}

				@Override
				public boolean canReceive() {
					return true;
				}
			};
		return p.view;
	}

	/** Uses up to {@code fe} of what a consumer's record holds; returns how much. */
	double spend(ConsumerPaper p, double fe) {
		double took = Math.max(0, Math.min(fe, p.energy));
		if (took > 0) {
			p.energy -= took;
			records.setDirty();
		}
		return took;
	}

	/** What a draw on paper brought, into a consumer's record. */
	void bring(ConsumerPaper p, double fe) {
		if (fe > 0) {
			p.energy += fe;
			records.setDirty();
		}
	}

	/** Takes up to max FE (at most a second at its rate) straight from the record of a shared store that's on paper. */
	int drawStore(StorePaper p, int max) {
		int got = (int) Math.max(0, Math.min(max, Math.min(Math.floor(p.energy), p.perTick * 20.0)));
		if (got > 0) {
			p.energy -= got;
			records.setDirty();
		}
		return got;
	}

	/** Takes up to max FE from a record's store this tick (or only says what it would). */
	int draw(Paper p, int max, long now, boolean simulate, ServerLevel level) {
		p.advance(now, level, this);
		int take = (int) Math.max(0, Math.min(max, Math.floor(p.store)));
		if (take > 0 && !simulate) {
			p.store -= take;
			records.setDirty();
		}
		return take;
	}

	/** Gives back energy drawn from a record and not used, up to what its store holds; returns how much it took. */
	double giveBack(Paper p, double fe) {
		double took = Math.max(0, Math.min(fe, p.storeCap - p.store));
		if (took > 0) {
			p.store += took;
			records.setDirty();
		}
		return took;
	}

	/** The block isn't a generator any more. */
	public void forget(BlockPos key) {
		if (generators.remove(key) != null)
			records.setDirty();
		boolean changed = faces.values().removeIf(key::equals);
		changed |= faceCount.remove(key) != null;
		facesOf.remove(key);
		rowMembers.remove(key);
		if (changed)
			setDirty();
	}

	// ---------- saving ----------

	/** 2: pieces and links have colours. */
	private static final int VERSION = 2;

	static PowerGrid load(CompoundTag tag, HolderLookup.Provider registries) {
		PowerGrid g = new PowerGrid();
		boolean old = tag.getInt("Version") < VERSION;
		for (Tag t : tag.getList("Pieces", Tag.TAG_COMPOUND)) {
			CompoundTag c = (CompoundTag) t;
			Node node = node(c);
			String tier = c.getString("Tier");
			Piece p = new Piece(tier.isEmpty() ? null : tier(tier), fitting(c), c.getBoolean("Closed"));
			p.dye = dye(c);
			// still waiting to be read again since colours came in (kept across saves until its chunk loads)
			p.stale = old || c.getBoolean("Stale");
			if (c.contains("Priority"))
				p.priority = c.getByteArray("Priority");
			for (Tag lt : c.getList("Links", Tag.TAG_COMPOUND)) {
				CompoundTag l = (CompoundTag) lt;
				int via = l.contains("Via") ? l.getByte("Via") : -1;
				p.links.add(new Link(node(l), tier(l.getString("Tier")), dye(l), via < 0 ? null : Direction.from3DDataValue(via)));
			}
			for (Tag et : c.getList("Ends", Tag.TAG_COMPOUND))
				p.ends.add(new End(BlockPos.of(((CompoundTag) et).getLong("Pos")), Direction.from3DDataValue(((CompoundTag) et).getInt("Side"))));
			g.put(node, p);
		}
		for (Tag t : tag.getList("Faces", Tag.TAG_COMPOUND))
			g.face(BlockPos.of(((CompoundTag) t).getLong("Face")), BlockPos.of(((CompoundTag) t).getLong("Key")));
		return g;
	}

	/** Saved by name (older saves: by its place in the list). */
	private static Fitting fitting(CompoundTag c) {
		if (c.contains("Fitting", Tag.TAG_STRING))
			for (Fitting f : Fitting.values())
				if (f.name().equals(c.getString("Fitting")))
					return f;
		return Fitting.values()[Math.clamp(c.getInt("Fitting"), 0, Fitting.values().length - 1)];
	}

	@Nullable
	private static net.minecraft.world.item.DyeColor dye(CompoundTag c) {
		return c.contains("Dye") ? net.minecraft.world.item.DyeColor.byId(c.getByte("Dye")) : null;
	}

	private static LineTier tier(String name) {
		return "brass".equals(name) ? LineTier.BRASS : LineTier.COPPER;
	}

	private static Node node(CompoundTag c) {
		return new Node(BlockPos.of(c.getLong("Pos")), c.getByte("Insulator"));
	}

	private static CompoundTag save(Node n, CompoundTag c) {
		c.putLong("Pos", n.pos().asLong());
		c.putByte("Insulator", (byte) n.insulator());
		return c;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag pieceList = new ListTag();
		for (Map.Entry<Node, Piece> e : pieces.entrySet()) {
			Piece p = e.getValue();
			CompoundTag c = save(e.getKey(), new CompoundTag());
			if (p.tier != null)
				c.putString("Tier", p.tier.getSerializedName());
			if (p.dye != null)
				c.putByte("Dye", (byte) p.dye.getId());
			c.putString("Fitting", p.fitting.name());
			c.putBoolean("Closed", p.closed);
			if (p.stale)
				c.putBoolean("Stale", true);
			if (p.priority != null)
				c.putByteArray("Priority", p.priority);
			ListTag links = new ListTag();
			for (Link l : p.links) {
				CompoundTag lc = save(l.to(), new CompoundTag());
				lc.putString("Tier", l.tier().getSerializedName());
				if (l.dye() != null)
					lc.putByte("Dye", (byte) l.dye().getId());
				if (l.via() != null)
					lc.putByte("Via", (byte) l.via().get3DDataValue());
				links.add(lc);
			}
			c.put("Links", links);
			ListTag ends = new ListTag();
			for (End en : p.ends) {
				CompoundTag ec = new CompoundTag();
				ec.putLong("Pos", en.target().asLong());
				ec.putInt("Side", en.side().get3DDataValue());
				ends.add(ec);
			}
			c.put("Ends", ends);
			pieceList.add(c);
		}
		tag.put("Pieces", pieceList);
		tag.putInt("Version", VERSION);
		ListTag faceList = new ListTag();
		for (Map.Entry<BlockPos, BlockPos> e : faces.entrySet()) {
			CompoundTag c = new CompoundTag();
			c.putLong("Face", e.getKey().asLong());
			c.putLong("Key", e.getValue().asLong());
			faceList.add(c);
		}
		tag.put("Faces", faceList);
		return tag;
	}
}
