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
 * Fluid Line between them stops at the unloaded chunk). When the generator runs again it takes on how far its record
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

	/** A block that makes power for a line (or the part of one a cable meets, recorded as the machine it belongs to). */
	public interface Generator {
		/** Where it's recorded: itself, or the machine it's part of (a turbine's port casing: its controller). */
		default BlockPos generatorKey() {
			return ((BlockEntity) this).getBlockPos();
		}

		/** A fresh record of its kind, or null if this is only a part of one. */
		@Nullable
		default Paper newPaper() {
			return null;
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

		public void set(double amount, int cap, double use) {
			this.amount = amount;
			this.cap = cap;
			this.use = use;
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
		long storeCap;
		double store, seenStore;
		final Fuel[] fuels;
		/** The touching generators it shares its store with (a row of Lava Generators). */
		List<BlockPos> row = List.of();
		/** As of the tick {@code at}; the generator last wrote it at {@code seenAt}. */
		long at, seenAt;

		public Paper(String kind, int fuels) {
			this.kind = kind;
			this.fuels = new Fuel[fuels];
			for (int i = 0; i < fuels; i++)
				this.fuels[i] = new Fuel();
		}

		public void set(int rate, double store, long storeCap) {
			this.rate = rate;
			this.store = store;
			this.storeCap = storeCap;
		}

		public Fuel fuel(int i) {
			return fuels[i];
		}

		public void row(List<BlockPos> row) {
			this.row = row;
		}

		/** mB a tick a fuel is brought from sources that aren't running (one that is runs for real). */
		private double income(Fuel f, ServerLevel level) {
			double in = 0;
			for (Map.Entry<BlockPos, Double> e : f.income.entrySet())
				if (!running(level, e.getKey()))
					in += e.getValue();
			return in;
		}

		/** FE a tick it's making now: flat out, or (with a fuel run dry) what that fuel's income keeps going; none if it can't run. */
		double making(ServerLevel level) {
			if (rate <= 0)
				return 0;
			double run = 1;
			for (Fuel f : fuels)
				if (f.amount <= 1e-9)
					run = Math.min(run, f.use <= 0 ? 1 : Math.min(1, income(f, level) / f.use));
			return rate * run;
		}

		/**
		 * Worked forward to the end of the tick {@code now}, flat out: each fuel burns and is brought in, the output
		 * fills the store and what doesn't fit is lost. In steps, since running a fuel dry changes the rate.
		 */
		void advance(long now, ServerLevel level) {
			double left = now - at;
			at = Math.max(at, now);
			if (left <= 0)
				return;
			double[] in = new double[fuels.length];
			for (int i = 0; i < fuels.length; i++)
				in[i] = income(fuels[i], level);
			for (int step = 0; step < 8 && left > 1e-9; step++) {
				// how much of full output it makes as its fuels stand: all, or (with one run dry) what that one is brought
				double run = rate <= 0 ? 0 : 1;
				for (int i = 0; i < fuels.length; i++)
					if (fuels[i].amount <= 1e-9)
						run = Math.min(run, fuels[i].use <= 0 ? 1 : Math.min(1, in[i] / fuels[i].use));
				double until = left;
				for (int i = 0; i < fuels.length; i++) {
					double change = in[i] - fuels[i].use * run;
					if (change < 0 && fuels[i].amount > 1e-9)
						until = Math.min(until, fuels[i].amount / -change);
				}
				for (int i = 0; i < fuels.length; i++)
					fuels[i].amount = Math.max(0, Math.min(fuels[i].cap, fuels[i].amount + (in[i] - fuels[i].use * run) * until));
				store = Math.min(storeCap, store + rate * run * until);
				left -= until;
			}
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

	public static PowerGrid of(ServerLevel level) {
		PowerGrid grid = level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(PowerGrid::new, PowerGrid::load, null), NAME);
		if (!grid.recordsLoaded) {
			grid.recordsLoaded = true;
			grid.records = level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Records::new, Records::load, null), NAME + "_generators");
			grid.generators = grid.records.generators;
			for (Paper p : grid.generators.values())
				grid.rowMembers.addAll(p.row);
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

		static Records load(CompoundTag tag, HolderLookup.Provider registries) {
			Records r = new Records();
			for (Tag t : tag.getList("Generators", Tag.TAG_COMPOUND)) {
				CompoundTag c = (CompoundTag) t;
				ListTag fl = c.getList("Fuels", Tag.TAG_COMPOUND);
				Paper p = new Paper(c.getString("Kind"), fl.size());
				p.set(c.getInt("Rate"), c.getDouble("Store"), c.getLong("StoreCap"));
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
		Paper fresh = g.newPaper();
		// a record of some other kind of generator that stood here: not this one's
		if (fresh == null || !fresh.kind.equals(p.kind) || fresh.fuels.length != p.fuels.length) {
			generators.remove(key);
			setDirty();
			return;
		}
		p.advance(now - 1, level);
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
		for (Map.Entry<BlockPos, Paper> e : generators.entrySet()) {
			if (running(level, e.getKey()))
				continue;
			Paper p = e.getValue();
			// a source that's loaded and no Wellspring now (broken) stops counting
			for (Fuel f : p.fuels)
				if (f.income.keySet().removeIf(src -> level.isLoaded(src)
					&& !(level.getBlockEntity(src) instanceof com.comedymonkey.createaddons.content.wellspring.WellspringBlockEntity)))
					records.setDirty();
			if (now - p.at >= 20) {
				p.advance(now, level);
				records.setDirty();
			}
		}
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
		boolean refresh = p == null || Math.floorMod(key.hashCode() + now, 20) == 0;
		if (p == null) {
			p = g.newPaper();
			if (p == null)
				return;
			generators.put(key.immutable(), p);
		}
		g.record(p, refresh);
		if (refresh)
			rowMembers.addAll(p.row);
		p.seenStore = p.store;
		for (Fuel f : p.fuels)
			f.seen = f.amount;
		p.at = p.seenAt = now;
		if (refresh)
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

	/** A record as something the network fills and draws, at the store's rate a tick (a second's worth when only asked). */
	IEnergyStorage paperStore(StorePaper p, ServerLevel level) {
		if (p.view == null)
			p.view = new RefundingStorage() {
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
					double limit = simulate ? p.perTick * 20.0 : p.perTick - p.inNow;
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
					double limit = simulate ? p.perTick * 20.0 : p.perTick - p.outNow;
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

	/** An energy store that takes refunds too. */
	public interface RefundingStorage extends IEnergyStorage, Refund {}

	/** Takes up to max FE from a record's store this tick (or only says what it would). */
	int draw(Paper p, int max, long now, boolean simulate, ServerLevel level) {
		p.advance(now, level);
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
