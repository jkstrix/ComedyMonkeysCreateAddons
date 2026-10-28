package com.comedymonkey.createaddons.content.line;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.line.PowerGrid.End;
import com.comedymonkey.createaddons.content.line.PowerGrid.Node;
import com.comedymonkey.createaddons.content.line.PowerGrid.Piece;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The Power Lines of one level, as networks: wires joined to wires, round corners and through walls, up Line Posts and
 * along the spans between poles, with the machines at their ends. What's joined to what is the level's {@link PowerGrid},
 * read from the world only when a piece changes; blocks in unloaded chunks don't change, so it holds for them too.
 * <p>
 * Once a second at most, and only while a device on it is live (a machine whose chunk is running and wants power;
 * generators never wake it), a network sets a second of what its devices want against what its generators give, with
 * no walking of the cables: generators whose own chunk is running give from their store, the rest on paper from their
 * record, as if they stood beside the devices. A line carries up to its tier's rate. Brass feeds copper where they meet
 * on a pole or at a wrenched Breaker, never the other way. If there isn't enough, devices share it evenly, unless a
 * Splitter ranks their branches. What the generators give beyond that charges batteries; when it's short, batteries
 * give. An open Breaker cuts the line (read each update, so switching it never rebuilds the network).
 */
public final class PowerLines {
	private static final Map<ResourceKey<Level>, PowerLines> BY_LEVEL = new HashMap<>();
	/** Ticks between a network's updates: each moves this many ticks' worth at once. */
	static final int INTERVAL = 20;

	private final ServerLevel level;
	private final PowerGrid grid;
	private final Map<Node, Network> byNode = new HashMap<>();
	/** Wire blocks that meet a machine, where the work starts, with the tick their network last updated. */
	private final Map<BlockPos, Long> terminals = new LinkedHashMap<>();
	/** The same, by the tick of the second each looks on, so a tick only goes through its twentieth. */
	@SuppressWarnings("unchecked")
	private final Set<BlockPos>[] phases = new Set[INTERVAL];
	/** What machines used, kept across a rebuild of their network (so a full one stays full): store, given, when. */
	private final Map<BlockPos, double[]> carriedUse = new HashMap<>();
	/** The wires of networks that were delivering when they were taken apart: worked out again and planned at once. */
	private final Set<BlockPos> replan = new LinkedHashSet<>();
	/** Networks with a plan for this second: handed out a twentieth each tick. */
	private final Set<Network> flowing = new LinkedHashSet<>();
	/** Where something changed, or loaded, since the last tick: read once, at the start of the next. */
	private final Set<BlockPos> changedAt = new LinkedHashSet<>(), loadedAt = new LinkedHashSet<>();
	/** Blocks of shared stores worked out anew since the last tick (not while a network is being built: next tick). */
	private final Set<BlockPos> storesChanged = new LinkedHashSet<>();

	private PowerLines(ServerLevel level) {
		this.level = level;
		this.grid = PowerGrid.of(level);
	}

	public static PowerLines of(ServerLevel level) {
		PowerLines lines = BY_LEVEL.get(level.dimension());
		if (lines == null || lines.level != level) {
			lines = new PowerLines(level);
			BY_LEVEL.put(level.dimension(), lines);
		}
		return lines;
	}

	public static void clear() {
		BY_LEVEL.clear();
		PowerGrid.AWAY.clear();
	}

	public static void onLevelTick(LevelTickEvent.Post event) {
		if (event.getLevel() instanceof ServerLevel server) {
			PowerLines lines = BY_LEVEL.get(server.dimension());
			if (lines != null && lines.level == server)
				lines.tick();
		}
	}

	/**
	 * A light at a line's end: told at each update whether its circuit reaches any generator or battery at all, so it
	 * can go out at once when it's switched off, rather than flicker as it does when it starves.
	 */
	public interface Lamp {
		/** {@code network}: which network says so (a light can touch several). */
		void supplied(boolean source, Object network);

		/**
		 * Lights that are one lamp to the network (a Light Strip run): the network's word is merged across all of them and
		 * given once, with how many of them it feeds. By default each light is its own.
		 */
		default Object lampGroup() {
			return this;
		}

		/** The word for a group: {@code feeds} of its lights take power from this network. */
		default void supplied(boolean source, Object network, int feeds) {
			supplied(source, network);
		}
	}

	/** Something at pos changed: it and the blocks round it (corners, and through a wall) are read again. */
	public void changed(BlockPos pos) {
		changedAt.add(pos.immutable());
	}

	/** Game tests only: the network a piece is in is worked out again (without reading the piece again). */
	void rebuildAt(Node node) {
		forget(node);
	}

	/** A shared store was worked out anew from these blocks: the networks meeting them are worked out again next tick. */
	public void storeChanged(java.util.Collection<BlockPos> blocks) {
		storesChanged.addAll(blocks);
	}

	/** A piece at pos loaded: read only if the grid hasn't got it (it can't have changed while it was unloaded). */
	public void loaded(BlockPos pos) {
		loadedAt.add(pos.immutable());
	}

	void addTerminal(BlockPos pos) {
		BlockPos p = pos.immutable();
		if (terminals.putIfAbsent(p, -1L) == null)
			phase(p).add(p);
	}

	private Set<BlockPos> phase(BlockPos pos) {
		int i = Math.floorMod(-pos.hashCode(), INTERVAL);
		if (phases[i] == null)
			phases[i] = new LinkedHashSet<>();
		return phases[i];
	}

	private void tick() {
		readChanges();
		long now = level.getGameTime();
		// generators away worked forward about once a second; and use carried over a rebuild, kept only a few seconds
		if (now % INTERVAL == 0) {
			grid.tickAway(level, now);
			carriedUse.values().removeIf(u -> now - u[2] > 5 * INTERVAL);
		}
		// a network taken apart while it was delivering doesn't wait for its turn in the second
		if (!replan.isEmpty()) {
			// each network once, however many of its wires asked (a second plan in the tick would see no use); any
			// asked for while these were worked out wait for the next tick
			List<BlockPos> asked = new ArrayList<>(replan);
			replan.removeAll(asked);
			Set<Network> planned = new HashSet<>();
			for (BlockPos pos : asked) {
				Node node = new Node(pos, Node.WIRE);
				Network n = byNode.get(node);
				if (n == null && level.isLoaded(pos) && grid.piece(node) != null)
					n = build(node);
				if (n != null && planned.add(n))
					n.tick(now, true);
			}
		}
		Set<BlockPos> due = phases[(int) Math.floorMod(now, INTERVAL)];
		for (BlockPos pos : due == null ? List.<BlockPos>of() : new ArrayList<>(due)) {
			// each machine's wire looks once a second, spread over the second (its machines are checked one by one)
			if (!level.isLoaded(pos))
				continue;
			if (!(level.getBlockEntity(pos) instanceof PowerLineBlockEntity be) || !be.hasTerminal()) {
				terminals.remove(pos);
				due.remove(pos);
				continue;
			}
			Node node = new Node(pos, Node.WIRE);
			Network n = byNode.get(node);
			if (n == null)
				n = build(node);
			if (n != null)
				n.tick(now);
		}
		for (Network n : new ArrayList<>(flowing))
			n.flow(now);
	}

	// ---------- what a Splitter's screen shows ----------

	/** One branch of a Splitter: the machines down it, and the generators (with their name if they're all one kind). */
	public record Branch(Direction dir, int devices, @Nullable Component deviceName, int generators, @Nullable Component generatorName, boolean loops) {}

	/**
	 * A Splitter's branches, and how its line stood at the last update (FE a tick): what it had to give, what its devices
	 * wanted, and what they took; {@code known} is false until the network has updated since it was last worked out.
	 */
	public record SplitterView(List<Branch> branches, double has, double wanted, double took, boolean known) {}

	public SplitterView splitter(PowerLineBlockEntity be) {
		readChanges();
		Node node = new Node(be.getBlockPos(), Node.WIRE);
		Piece piece = grid.piece(node);
		Network n = byNode.get(node);
		if (n == null)
			n = build(node);
		List<Branch> out = new ArrayList<>();
		// what each branch reaches without passing the Splitter: two that share any of it are one ring round
		Map<Direction, Set<Node>> reach = new HashMap<>();
		if (piece != null && n != null)
			for (PowerGrid.Link l : piece.links)
				if (l.via() != null)
					reach.computeIfAbsent(l.via(), k -> new HashSet<>()).addAll(behind(node, l.to(), n.nodes));
		for (Direction d : be.branches()) {
			List<End> ends = new ArrayList<>();
			if (piece != null && n != null) {
				for (End e : piece.ends)
					if (e.side().getOpposite() == d)
						ends.add(e);
				for (Node m : reach.getOrDefault(d, Set.of()))
					ends.addAll(grid.piece(m).ends);
			}
			boolean loops = false;
			for (Map.Entry<Direction, Set<Node>> other : reach.entrySet())
				if (other.getKey() != d && !java.util.Collections.disjoint(other.getValue(), reach.getOrDefault(d, Set.of())))
					loops = true;
			// one count a machine, however many wires meet it (a turbine's port casings: the turbine)
			Set<BlockPos> seen = new HashSet<>();
			int devices = 0, generators = 0;
			List<Component> deviceNames = new ArrayList<>(), generatorNames = new ArrayList<>();
			for (End e : ends) {
				BlockPos t = e.target();
				BlockPos key = generatorKeyAt(t);
				if (key != null) {
					if (seen.add(key)) {
						generators++;
						generatorNames.add(level.isLoaded(key) ? level.getBlockState(key).getBlock().getName() : null);
					}
					continue;
				}
				if (!seen.add(t))
					continue;
				IEnergyStorage cap = level.isLoaded(t) ? level.getCapability(Capabilities.EnergyStorage.BLOCK, t, e.side()) : null;
				if (cap != null && cap.canExtract() && !cap.canReceive()) {
					generators++;
					generatorNames.add(level.getBlockState(t).getBlock().getName());
				} else {
					devices++;
					deviceNames.add(level.isLoaded(t) ? level.getBlockState(t).getBlock().getName() : null);
				}
			}
			out.add(new Branch(d, devices, oneName(deviceNames), generators, oneName(generatorNames), loops));
		}
		if (n == null || n.lastCircuit == null)
			return new SplitterView(out, 0, 0, 0, false);
		Integer seg = n.segOf.get(node);
		int c = seg == null ? -1 : n.lastCircuit[seg];
		if (c < 0 || c >= n.lastSupply.length)
			return new SplitterView(out, 0, 0, 0, true);
		return new SplitterView(out, n.lastSupply[c], n.lastDemand[c], n.lastTook[c], true);
	}

	/** The name they all share, or null if they're not all one kind (or one isn't known). */
	@Nullable
	private static Component oneName(List<Component> names) {
		if (names.isEmpty() || names.contains(null))
			return null;
		for (Component c : names)
			if (!c.getString().equals(names.get(0).getString()))
				return null;
		return names.get(0);
	}

	/** The pieces of a network reached from {@code from} without going through {@code splitter}. */
	private Set<Node> behind(Node splitter, Node from, Set<Node> within) {
		Set<Node> seen = new HashSet<>();
		if (!within.contains(from))
			return seen;
		ArrayDeque<Node> queue = new ArrayDeque<>();
		seen.add(from);
		queue.add(from);
		while (!queue.isEmpty())
			for (Node o : grid.neighbours(queue.poll()))
				if (!o.equals(splitter) && seen.add(o))
					queue.add(o);
		return seen;
	}

	/** The generator a line meets at block t, by its key: from the block if it's loaded, else from where it was met. */
	@Nullable
	private BlockPos generatorKeyAt(BlockPos t) {
		if (level.isLoaded(t)) {
			if (level.getBlockEntity(t) instanceof PowerGrid.Generator g)
				return g.generatorKey();
			if (grid.keyAt(t) != null)
				grid.dropFace(t);
			return null;
		}
		return grid.keyAt(t);
	}

	// ---------- reading the pieces ----------

	private void readChanges() {
		if (!storesChanged.isEmpty()) {
			Set<Node> meeting = new HashSet<>();
			for (BlockPos b : storesChanged)
				meeting.addAll(grid.endsAt(b));
			storesChanged.clear();
			for (Node n : meeting)
				forget(n);
		}
		if (changedAt.isEmpty() && loadedAt.isEmpty())
			return;
		// every block to read, each once: what changed, the blocks round it and through a wall beside it
		Set<BlockPos> force = new LinkedHashSet<>();
		for (BlockPos pos : changedAt) {
			for (int dx = -1; dx <= 1; dx++)
				for (int dy = -1; dy <= 1; dy++)
					for (int dz = -1; dz <= 1; dz++)
						force.add(pos.offset(dx, dy, dz));
			for (Direction d : Direction.values())
				force.add(pos.relative(d, 2));
		}
		changedAt.clear();
		Set<BlockPos> lazy = new LinkedHashSet<>(loadedAt);
		loadedAt.clear();
		lazy.removeAll(force);
		Set<Node> touched = new HashSet<>();
		Set<BlockPos> read = new HashSet<>();
		for (BlockPos pos : force)
			read(pos, touched, true, read);
		for (BlockPos pos : lazy)
			read(pos, touched, false, read);
		for (Node n : touched)
			forget(n);
	}

	/** Reads what's at p into the grid (when {@code force}, or the grid hasn't got it), noting the pieces touched. */
	private void read(BlockPos p, Set<Node> touched, boolean force, Set<BlockPos> read) {
		if (!level.isLoaded(p) || (force && !read.add(p)))
			return;
		BlockState state = level.getBlockState(p);
		Node wire = new Node(p, Node.WIRE);
		if (state.getBlock() instanceof PowerLineBlock && level.getBlockEntity(p) instanceof PowerLineBlockEntity be) {
			if (force || grid.piece(wire) == null || grid.piece(wire).stale)
				set(wire, readWire(be), touched);
		} else if (grid.piece(wire) != null) {
			set(wire, null, touched);
		}
		BlockPos head = state.getBlock() instanceof LinePostBlock ? LinePostBlock.head(level, p) : towerAt(p);
		Node pole = new Node(p, Node.POLE);
		if (!p.equals(head) && grid.piece(pole) != null)
			set(pole, null, touched);
		if (head != null) {
			Node h = new Node(head, Node.POLE);
			if (force || grid.piece(h) == null || grid.piece(h).stale) {
				if (!(state.getBlock() instanceof LinePostBlock)) {
					set(h, readTower(head), touched);
					return;
				}
				set(h, readPole(head), touched);
				// and the wires at its foot, which join it
				BlockPos foot = LinePostBlock.foot(level, head);
				if (force && foot != null)
					for (Direction d : Direction.values())
						if (level.isLoaded(foot.relative(d)) && !(level.getBlockState(foot.relative(d)).getBlock() instanceof LinePostBlock))
							read(foot.relative(d), touched, true, read);
			}
		}
	}

	private void set(Node node, @Nullable Piece piece, Set<Node> touched) {
		touched.add(node);
		touched.addAll(grid.neighbours(node));
		touched.addAll(grid.listing(node));
		grid.put(node, piece);
		touched.addAll(grid.neighbours(node));
		if (piece != null)
			for (PowerGrid.Link l : piece.links)
				touched.add(l.to());
	}

	/** Where power at pos goes in: a hanging lamp's cable carries it down to the lamp. */
	private BlockPos device(BlockPos pos) {
		if (level.isLoaded(pos) && level.getBlockState(pos).getBlock() instanceof com.comedymonkey.createaddons.content.light.HangingCableBlock) {
			BlockPos lamp = com.comedymonkey.createaddons.content.light.HangingCableBlock.foot(level, pos);
			if (lamp != null)
				return lamp;
		}
		return pos;
	}

	private Piece readWire(PowerLineBlockEntity be) {
		LineTier tier = be.tier();
		DyeColor dye = be.dye();
		Piece p = new Piece(tier, be.fitting(), !be.fitting().cuts() || be.passes() > 0);
		p.dye = dye;
		if (be.fitting() == Fitting.SPLITTER)
			p.priority = be.priorities();
		BlockPos pos = be.getBlockPos();
		BlockState state = be.getBlockState();
		for (Direction face : Direction.values()) {
			if (!PowerLineBlock.hasFace(state, face))
				continue;
			int links = be.links(face);
			if ((links & WireLink.BASE) != 0)
				p.ends.add(new End(device(pos.relative(face)), face.getOpposite()));
			// up (or down) through the block into a machine across it
			if ((links & WireLink.RISE) != 0)
				p.ends.add(new End(device(pos.relative(face.getOpposite())), face));
			// through the block it lies on, into the lights fixed to its other faces
			if ((links & WireLink.BEHIND) != 0) {
				BlockPos support = pos.relative(face);
				for (BlockPos light : com.comedymonkey.createaddons.content.light.LightMounts.fixedAround(level, support, pos))
					p.ends.add(new End(light, Direction.getNearest(support.getX() - light.getX(), support.getY() - light.getY(), support.getZ() - light.getZ())));
			}
			// through the wall it lies on, to the wire behind
			if (WireLink.hole(links) != 0)
				link(p, new Node(pos.relative(face, 2), Node.WIRE), tier, dye, face);
			for (Direction d : WireLink.across(face)) {
				BlockPos to = pos.relative(d);
				switch (be.link(face, d)) {
					case LINE -> link(p, new Node(to, Node.WIRE), tier, dye, d);
					case THROUGH -> link(p, new Node(to.relative(d), Node.WIRE), tier, dye, d);
					case OUTSIDE -> link(p, new Node(to.relative(face), Node.WIRE), tier, dye, d);
					case POST -> {
						BlockPos head = level.isLoaded(to) ? LinePostBlock.head(level, to) : null;
						if (head != null)
							link(p, new Node(head, Node.POLE), tier, dye, d);
					}
					case TERMINAL, LAMP -> p.ends.add(new End(device(to), d.getOpposite()));
					default -> {}
				}
			}
		}
		return p;
	}

	/** A whole pole: the spans tied to its head, and the wires run to its foot. */
	private Piece readPole(BlockPos head) {
		Piece p = new Piece(null, Fitting.NONE, true);
		if (level.getBlockEntity(head) instanceof LinePostBlockEntity post)
			for (Span s : post.spans())
				link(p, new Node(head.offset(s.offset()), Node.POLE), s.tier(), s.dye(), null);
		// lamps fixed to the head (a lamp head on top, heads on a Lamp Post's arms): fed by the pole
		for (Direction d : Direction.values()) {
			BlockPos lamp = head.relative(d);
			if (level.isLoaded(lamp) && com.comedymonkey.createaddons.content.light.LightMounts.fixedTo(level, level.getBlockState(lamp), lamp, head))
				p.ends.add(new End(lamp, d.getOpposite()));
		}
		BlockPos foot = LinePostBlock.foot(level, head);
		if (foot == null)
			return p;
		Direction facing = level.getBlockState(foot).getValue(LinePostBlock.FACING);
		for (Direction d : Direction.values()) {
			if (d.getAxis() == facing.getAxis())
				continue;
			BlockPos w = foot.relative(d);
			if (level.isLoaded(w) && level.getBlockEntity(w) instanceof PowerLineBlockEntity be
				&& be.link(facing.getOpposite(), d.getOpposite()) == WireLink.POST)
				link(p, new Node(w, Node.WIRE), be.tier(), be.dye(), null);
		}
		return p;
	}

	/** The master of the Industrial Battery a block is part of (a junction like a pole's), if it's loaded. */
	@Nullable
	private BlockPos towerAt(BlockPos p) {
		SpanHolder h = level.getBlockEntity(p) instanceof SpanHolder.Part part ? part.spanHolder() : null;
		return h != null && h.isBattery() ? h.holderPos() : null;
	}

	/** An Industrial Battery: the spans on its insulators, joined like a pole's, and its store at the junction. */
	private Piece readTower(BlockPos master) {
		Piece p = new Piece(null, Fitting.NONE, true);
		SpanHolder h = SpanHolder.at(level, master);
		if (h != null)
			for (Span s : h.spans())
				link(p, new Node(master.offset(s.offset()), Node.POLE), s.tier(), s.dye(), null);
		p.ends.add(new End(master, Direction.UP));
		return p;
	}

	private static void link(Piece p, Node to, LineTier tier, @Nullable DyeColor dye, @Nullable Direction via) {
		PowerGrid.Link l = new PowerGrid.Link(to, tier, dye, via);
		if (!p.links.contains(l))
			p.links.add(l);
	}

	// ---------- networks ----------

	private void forget(Node node) {
		Network n = byNode.get(node);
		if (n == null)
			return;
		for (Node m : n.nodes)
			byNode.remove(m);
		if (flowing.remove(n))
			replan.addAll(n.wires);
		long now = level.getGameTime();
		n.use.forEach((pos, u) -> carriedUse.put(pos, new double[] {u[0], u[1], now}));
		n.zeroMeters();
	}

	/**
	 * The other pieces that meet a generator this one meets. Each generator it meets is noted where the line meets it
	 * (so it's found without loading it once it's on paper, and the lines on its other faces find this one).
	 */
	private Set<Node> throughGenerators(Node node) {
		Piece p = grid.piece(node);
		if (p == null || p.ends.isEmpty())
			return Set.of();
		Set<Node> out = new HashSet<>();
		for (End e : p.ends) {
			BlockPos t = e.target();
			if (level.isLoaded(t) && level.getBlockEntity(t) instanceof PowerGrid.Generator g)
				grid.face(t, g.generatorKey());
			BlockPos key = generatorKeyAt(t);
			if (key != null)
				out.addAll(grid.meeting(key));
		}
		out.remove(node);
		return out;
	}

	/** The other pieces that meet a store this one meets (another Battery of its bank, say). Loaded stores only. */
	private Set<Node> throughStores(Node node) {
		Piece p = grid.piece(node);
		if (p == null || p.ends.isEmpty())
			return Set.of();
		Set<Node> out = new HashSet<>();
		for (End e : p.ends) {
			BlockPos t = e.target();
			if (PowerGrid.running(level, t)) {
				if (level.getBlockEntity(t) instanceof PowerGrid.Store s)
					for (BlockPos b : s.storeBlocks())
						out.addAll(grid.endsAt(b));
				continue;
			}
			// away: through where lines met it when it last ran
			BlockPos key = grid.storeKeyFor(t);
			if (key != null)
				for (BlockPos f : grid.storeFacesOf(key))
					out.addAll(grid.endsAt(f));
		}
		out.remove(node);
		return out;
	}

	/**
	 * The shared store a line meets at t, from its record, if it's on paper: all of it away, or (an Industrial Battery,
	 * which runs only whole) not all of it running. Looked up again each tick: once it runs again its record is taken
	 * back, and gives nothing more.
	 */
	private boolean paperStore(BlockPos t, int c, Device d, Map<BlockPos, StoreAt> stores) {
		BlockPos rk = grid.storeKeyFor(t);
		PowerGrid.StorePaper rec = rk == null ? null : grid.storePaper(rk);
		if (rec == null || !onPaper(rk, rec))
			return false;
		StoreAt s = stores.computeIfAbsent(rk, k -> new StoreAt(() -> {
			PowerGrid.StorePaper r = grid.storePaper(k);
			return r != null && onPaper(k, r) ? grid.paperStore(r, level) : null;
		}, d.key()));
		if (!s.circuits.contains(c))
			s.circuits.add(c);
		return true;
	}

	private boolean onPaper(BlockPos key, PowerGrid.StorePaper r) {
		if (grid.allAway(r, level))
			return true;
		return r.whole() && !(PowerGrid.running(level, key) && level.getBlockEntity(key) instanceof PowerGrid.Store s && s.storeKey() != null);
	}

	/** A shared store's key at block t, if it's one (loaded). */
	@Nullable
	private BlockPos storeKeyAt(BlockPos t) {
		return level.getBlockEntity(t) instanceof PowerGrid.Store s ? s.storeKey() : null;
	}

	/** A shared store on a network this second: how to reach it, the circuits that meet it, and its Splitter rank. */
	private static final class StoreAt {
		final java.util.function.Supplier<IEnergyStorage> energy;
		final int[] key;
		final List<Integer> circuits = new ArrayList<>();

		StoreAt(java.util.function.Supplier<IEnergyStorage> energy, int[] key) {
			this.energy = energy;
			this.key = key;
		}
	}

	/** One tier of line at a node: a wire's own, or one of the tiers of cable tied to a pole. */
	private record Sub(Node node, LineTier tier, @Nullable DyeColor dye) {}

	/**
	 * Everything joined to start, sorted into segments (each one tier; every Breaker a segment of its own, switched in
	 * each update), the feeds from brass to copper, and the machines with their Splitter priority.
	 */
	@Nullable
	private Network build(Node start) {
		if (grid.piece(start) == null)
			return null;
		Network n = new Network();
		ArrayDeque<Node> queue = new ArrayDeque<>();
		queue.add(start);
		n.nodes.add(start);
		while (!queue.isEmpty()) {
			Node q = queue.poll();
			for (Node m : grid.neighbours(q))
				if (n.nodes.add(m))
					queue.add(m);
			// lines that meet one generator, or one store (a bank of Batteries), are one network, whatever their tier and
			// colour: through it
			for (Node m : throughGenerators(q))
				if (n.nodes.add(m))
					queue.add(m);
			for (Node m : throughStores(q))
				if (n.nodes.add(m))
					queue.add(m);
		}
		for (Node m : n.nodes) {
			Network old = byNode.get(m);
			if (old != null && old != n)
				forget(m);
		}
		for (Node m : n.nodes)
			byNode.put(m, n);
		// segments: same-tier pieces joined, not through a Breaker
		Map<Sub, Integer> seg = new HashMap<>();
		List<LineTier> tiers = new ArrayList<>();
		for (Node m : n.nodes)
			for (Sub first : subsAt(m)) {
				LineTier t = first.tier();
				DyeColor dye = first.dye();
				if (seg.containsKey(first))
					continue;
				int id = tiers.size();
				tiers.add(t);
				seg.put(first, id);
				if (grid.piece(m).fitting.cuts())
					continue;
				ArrayDeque<Sub> walk = new ArrayDeque<>();
				walk.add(first);
				while (!walk.isEmpty()) {
					Sub s = walk.poll();
					for (Node o : grid.neighbours(s.node())) {
						Piece op = grid.piece(o);
						// a cable of this tier and colour between them (there may be others beside it). A piece from a save
						// made before lines had colours (not read again yet, its chunk not loaded since) joins any colour,
						// and a wire of it takes the colour of the line it's on
						boolean anyColour = grid.piece(s.node()).stale || op.stale;
						boolean joined = false;
						for (PowerGrid.Link l : grid.linksTo(s.node(), o))
							if (l.tier() == t && (anyColour || l.dye() == dye))
								joined = true;
						if (!joined || (op.tier != null && (op.tier != t || !op.stale && op.dye != dye)) || op.fitting.cuts())
							continue;
						if (op.stale && op.tier != null && op.dye == null)
							op.dye = dye;
						Sub next = new Sub(o, t, dye);
						if (seg.putIfAbsent(next, id) == null)
							walk.add(next);
					}
				}
			}
		n.segTier = tiers.toArray(new LineTier[0]);
		Map<Node, int[]> keys = n.rankSplitters();
		for (Node m : n.nodes) {
			Piece p = grid.piece(m);
			// brass feeds copper where both are tied to one pole
			if (m.isPole()) {
				// brass feeds copper of its own colour
				for (Sub sub : subsAt(m)) {
					if (sub.tier() != LineTier.BRASS)
						continue;
					Integer brass = seg.get(sub), copper = seg.get(new Sub(m, LineTier.COPPER, sub.dye()));
					if (brass != null && copper != null)
						n.feeds.add(new Feed(brass, copper, keys.get(m), null));
				}
				// an Industrial Battery's store, at its junction: on every tier and colour tied to it (one store, shared
				// between them when the plan is made)
				for (End e : p.ends) {
					BlockCapabilityCache<IEnergyStorage, Direction> cache = null;
					for (Sub sub : subsAt(m)) {
						Integer s = seg.get(sub);
						if (s == null)
							continue;
						if (cache == null)
							cache = BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, level, e.target(), e.side());
						n.ends.add(new Device(e, s, cache, keys.get(m)));
					}
				}
				continue;
			}
			int s = seg.get(new Sub(m, p.tier, p.dye));
			n.segOf.put(m, s);
			// a Breaker or a Light Switch: joins the segments either side while it's on
			if (p.fitting.cuts()) {
				n.breakers.add(m);
				for (Node o : grid.neighbours(m)) {
					Piece op = grid.piece(o);
					if (o.isPole()) {
						Integer pole = seg.get(new Sub(o, p.tier, p.dye));
						if (pole != null)
							n.switches.add(new int[] {s, pole, n.breakers.size() - 1});
						continue;
					}
					if (op.tier == null)
						continue;
					Integer other = seg.get(new Sub(o, op.tier, op.dye));
					if (other == null)
						continue;
					if (op.tier == p.tier)
						n.switches.add(new int[] {s, other, n.breakers.size() - 1});
					// a wrenched joint to the other tier: brass feeds copper through it, while it's on
					else if (p.tier == LineTier.BRASS)
						n.feeds.add(new Feed(s, other, keys.get(o), m));
					else
						n.feeds.add(new Feed(other, s, keys.get(m), m));
				}
			}
			if (p.fitting == Fitting.METER)
				n.meters.put(m, s);
			for (End e : p.ends) {
				int[] key = keys.get(m);
				// a machine on a Splitter's own block, on one of its branches
				if (p.fitting == Fitting.SPLITTER && p.priority != null && n.splitterIndex.containsKey(m)) {
					key = key.clone();
					key[n.splitterIndex.get(m)] = p.priority[e.side().getOpposite().ordinal()];
				}
				n.ends.add(new Device(e, s, BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, level, e.target(), e.side()), key));
			}
		}
		boolean onlyGenerators = true;
		for (Device d : n.ends)
			if (generatorKeyAt(d.end().target()) == null)
				onlyGenerators = false;
		n.onlyGenerators = onlyGenerators;
		// keep the once-a-second rhythm across a rebuild: from when its machines' network last updated
		for (Node m : n.nodes)
			if (!m.isPole() && !grid.piece(m).ends.isEmpty()) {
				n.wires.add(m.pos());
				Long last = terminals.get(m.pos());
				if (last != null)
					n.lastTick = Math.max(n.lastTick, last);
			}
		return n;
	}

	/** The tiers and colours of line at a piece: a wire's own, or each tier and colour of cable tied to a pole. */
	private List<Sub> subsAt(Node m) {
		Piece p = grid.piece(m);
		if (p.tier != null)
			return List.of(new Sub(m, p.tier, p.dye));
		List<Sub> out = new ArrayList<>();
		for (Node o : grid.neighbours(m))
			for (PowerGrid.Link l : grid.linksTo(m, o)) {
				Sub sub = new Sub(m, l.tier(), l.dye());
				if (!out.contains(sub))
					out.add(sub);
			}
		return out;
	}

	/** A machine on the line, in segment {@code seg}, ranked by the Splitters it's behind ({@code key}). */
	private record Device(End end, int seg, BlockCapabilityCache<IEnergyStorage, Direction> energy, int[] key) {}

	/**
	 * Brass segment {@code from} feeds copper segment {@code to}, ranked among the brass circuit's devices by {@code key};
	 * through the Breaker at {@code breaker} (only while it's on), or a pole (null).
	 */
	private record Feed(int from, int to, int[] key, @Nullable Node breaker) {}

	/** Something giving power this second: a running block (looked up each tick), or a generator on paper. */
	private record Source(int circuit, @Nullable java.util.function.Supplier<IEnergyStorage> energy, @Nullable PowerGrid.Paper paper, int offer, boolean store,
						  @Nullable BlockPos generator) {}

	/** Something taking power this second: what it wants of the second (room to fill, and its use). */
	private record Taker(int circuit, java.util.function.Supplier<IEnergyStorage> energy, BlockPos pos, int want, boolean store, int[] key) {}

	/** A network's plan for the next second, handed out a twentieth each tick. */
	private static final class Schedule {
		final int count;
		final double[] fed;
		final List<Source> sources;
		final double[] budget;
		final List<Taker> takers;
		final double[] perTick, carry;
		final List<Feed> feeds;
		final double[] grant;
		final int[] circuitOf;
		/** What each circuit's sources give a tick in all (fixed for the second). */
		final double[] budgetOf;
		/** Each circuit's takers (indexes), machines and batteries apart. */
		final int[][] devicesOf, storesOf;
		/** Over the second: what flowed through each circuit, and what its machines took. */
		final double[] flowed, devices;
		/** A tick's working (kept, so a tick makes nothing new). */
		final double[] need, tickDevices, own, asked, pool, passed, took;
		final int[] accept;
		final IEnergyStorage[] taker;

		Schedule(int count, double[] fed, List<Source> sources, double[] budget, List<Taker> takers, double[] perTick, double[] carry,
				 List<Feed> feeds, double[] grant, int[] circuitOf) {
			this.count = count;
			this.fed = fed;
			this.sources = sources;
			this.budget = budget;
			this.takers = takers;
			this.perTick = perTick;
			this.carry = carry;
			this.feeds = feeds;
			this.grant = grant;
			this.circuitOf = circuitOf;
			budgetOf = new double[count];
			for (int i = 0; i < sources.size(); i++)
				budgetOf[sources.get(i).circuit()] += budget[i];
			devicesOf = new int[count][];
			storesOf = new int[count][];
			for (int c = 0; c < count; c++) {
				List<Integer> d = new ArrayList<>(), st = new ArrayList<>();
				for (int i = 0; i < takers.size(); i++)
					if (takers.get(i).circuit() == c)
						(takers.get(i).store() ? st : d).add(i);
				devicesOf[c] = d.stream().mapToInt(Integer::intValue).toArray();
				storesOf[c] = st.stream().mapToInt(Integer::intValue).toArray();
			}
			flowed = new double[count];
			devices = new double[count];
			need = new double[count];
			tickDevices = new double[count];
			own = new double[count];
			asked = new double[count];
			pool = new double[count];
			passed = new double[feeds.size()];
			took = new double[sources.size()];
			accept = new int[takers.size()];
			taker = new IEnergyStorage[takers.size()];
		}
	}

	/** Splitter priorities compared: the first Splitter's level decides, then the next's (nearest the generators first). */
	private static int compareKeys(int[] a, int[] b) {
		for (int i = 0; i < Math.min(a.length, b.length); i++)
			if (a[i] != b[i])
				return Integer.compare(a[i], b[i]);
		return 0;
	}

	/**
	 * Shares {@code total} between claims, as the fraction of what each wants it gets: the first priority level gets all it
	 * wants first, then the next; the claims on one level share evenly.
	 */
	private static double[] allocate(double total, double[] wants, int[][] keys) {
		int n = wants.length;
		Integer[] order = new Integer[n];
		for (int i = 0; i < n; i++)
			order[i] = i;
		java.util.Arrays.sort(order, (x, y) -> compareKeys(keys[x], keys[y]));
		double[] share = new double[n];
		double left = total;
		for (int i = 0; i < n;) {
			int j = i;
			while (j < n && compareKeys(keys[order[j]], keys[order[i]]) == 0)
				j++;
			// within the level: the smallest wants filled first, each of the rest an equal part of what's left
			Integer[] level = java.util.Arrays.copyOfRange(order, i, j);
			java.util.Arrays.sort(level, (x, y) -> Double.compare(wants[x], wants[y]));
			for (int k = 0; k < level.length; k++) {
				int t = level[k];
				double part = Math.max(0, left) / (level.length - k), got = Math.min(wants[t], part);
				share[t] = wants[t] <= 0 ? 0 : got / wants[t];
				left -= got;
			}
			i = j;
		}
		return share;
	}

	final class Network {
		final Set<Node> nodes = new LinkedHashSet<>();
		LineTier[] segTier;
		final Map<Node, Integer> segOf = new HashMap<>();
		final List<Device> ends = new ArrayList<>();
		final List<Feed> feeds = new ArrayList<>();
		final List<Node> breakers = new ArrayList<>();
		/** Segment, segment, Breaker (index into breakers): joined while that Breaker is on. */
		final List<int[]> switches = new ArrayList<>();
		final Map<Node, Integer> meters = new HashMap<>();
		final Map<Node, Integer> splitterIndex = new HashMap<>();
		/** Its wire blocks that meet a machine (its terminals). */
		final List<BlockPos> wires = new ArrayList<>();
		/** Every machine on it is a generator: it never has anything to do. */
		boolean onlyGenerators;
		/** At the last update: each segment's circuit, and per circuit a tick's worth of supply, demand and what was taken. */
		@Nullable
		int[] lastCircuit;
		double[] lastSupply = new double[0], lastDemand = new double[0], lastTook = new double[0];
		/** What its generators are making, FE a tick (flat out, less any out of fuel), at the last update. */
		double lastMakes;
		long lastTick = -1;
		/** The plan for this second, if anything on it is live. */
		@Nullable
		Schedule plan;
		/** Each machine's store at the last update, and what it was given since: what it uses in a second. */
		final Map<BlockPos, double[]> use = new HashMap<>();
		/** The live generators' stores, looked up each tick. */
		final Map<BlockPos, BlockCapabilityCache<IEnergyStorage, Direction>> generatorCaches = new HashMap<>();

		/**
		 * Every piece's priority behind the Splitters: walking out from the generators (or from anywhere, if it has none),
		 * each piece takes the one before it's, and passing a Splitter sets that Splitter's level for the branch taken.
		 * Splitters are numbered as they're reached, so one nearer the generators decides first.
		 */
		Map<Node, int[]> rankSplitters() {
			List<Node> roots = new ArrayList<>();
			for (Node m : nodes)
				for (End e : grid.piece(m).ends)
					if (generatorKeyAt(e.target()) != null) {
						roots.add(m);
						break;
					}
			if (roots.isEmpty())
				roots.add(nodes.iterator().next());
			// the walk, and the Splitters in the order it reaches them
			Map<Node, Node> parent = new HashMap<>();
			List<Node> order = new ArrayList<>();
			ArrayDeque<Node> queue = new ArrayDeque<>(roots);
			Set<Node> seen = new HashSet<>(roots);
			while (!queue.isEmpty()) {
				Node m = queue.poll();
				order.add(m);
				Piece p = grid.piece(m);
				if (p.fitting == Fitting.SPLITTER && p.priority != null)
					splitterIndex.put(m, splitterIndex.size());
				for (Node o : grid.neighbours(m))
					if (seen.add(o)) {
						parent.put(o, m);
						queue.add(o);
					}
			}
			int count = splitterIndex.size();
			int[] even = new int[count];
			java.util.Arrays.fill(even, 1);
			Map<Node, int[]> keys = new HashMap<>();
			for (Node m : order) {
				Node from = parent.get(m);
				if (from == null) {
					keys.put(m, even);
					continue;
				}
				int[] key = keys.get(from);
				Integer k = splitterIndex.get(from);
				if (k != null) {
					// through a Splitter: the level of the branch this piece is on
					Direction via = null;
					for (PowerGrid.Link l : grid.piece(from).links)
						if (l.to().equals(m))
							via = l.via();
					key = key.clone();
					key[k] = via == null ? 1 : grid.piece(from).priority[via.ordinal()];
				}
				keys.put(m, key);
			}
			return keys;
		}

		/**
		 * The once-a-second update: who's live, what each wants, what each gives, and the plan for the next second, handed
		 * out a twentieth at a time by {@link #flow} each tick (so stores stay level: a motor with enough coming in sits
		 * full, a generator making more than is used stays topped up).
		 */
		void tick(long now) {
			tick(now, false);
		}

		/** {@code now}: planned straight away (it was just worked out again, and was delivering), else once a second. */
		void tick(long now, boolean now_) {
			if (!now_ && lastTick >= 0 && now - lastTick < INTERVAL)
				return;
			lastTick = now;
			// nothing on it but generators: nothing to do (generators never wake a network), unless one has been
			// replaced by a machine since (without its wire changing)
			if (onlyGenerators) {
				for (Device d : ends)
					if (generatorKeyAt(d.end().target()) == null) {
						onlyGenerators = false;
						break;
					}
				if (onlyGenerators)
					return;
			}
			for (BlockPos w : wires)
				terminals.computeIfPresent(w, (k, v) -> now);
			// circuits this update: segments joined through the Breakers that are on
			boolean[] on = new boolean[breakers.size()];
			for (int i = 0; i < on.length; i++) {
				Node b = breakers.get(i);
				Piece p = grid.piece(b);
				if (level.isLoaded(b.pos()) && level.getBlockEntity(b.pos()) instanceof LineBlockEntity be && (be.passes() > 0) != p.closed) {
					p.closed = be.passes() > 0;
					grid.setDirty();
				}
				on[i] = p.closed;
			}
			int[] circuitOf = circuits(on);
			int count = 0;
			for (int c : circuitOf)
				count = Math.max(count, c + 1);
			LineTier[] tiers = new LineTier[count];
			for (int s = 0; s < circuitOf.length; s++)
				tiers[circuitOf[s]] = segTier[s];
			Map<Long, Feed> pairs = new LinkedHashMap<>();
			for (Feed f : feeds) {
				if (f.breaker() != null && !grid.piece(f.breaker()).closed)
					continue;
				int from = circuitOf[f.from()], to = circuitOf[f.to()];
				if (from != to)
					pairs.putIfAbsent(((long) from << 32) | to, new Feed(from, to, f.key(), null));
			}
			List<Feed> active = new ArrayList<>(pairs.values());

			List<Source> sources = new ArrayList<>();
			List<Taker> takers = new ArrayList<>();
			double[] gen = new double[count], stored = new double[count], want = new double[count], room = new double[count];
			boolean[] canGive = new boolean[count];
			// one share a machine, however many wires meet it: by its block (a generator by its key, a store by its own)
			Set<BlockPos> seen = new HashSet<>();
			Map<BlockPos, StoreAt> stores = new LinkedHashMap<>();
			Map<BlockPos, double[]> nextUse = new HashMap<>();
			boolean live = false;
			double makes = 0;
			// the generators, each with every circuit it meets (one with a brass line and a copper line serves both)
			Map<BlockPos, List<Integer>> generators = new LinkedHashMap<>();
			for (Device d : ends) {
				BlockPos t = d.end().target();
				int c = circuitOf[d.seg()], rate = tiers[c].powerPerTick() * INTERVAL;
				BlockPos key = generatorKeyAt(t);
				if (key != null) {
					List<Integer> cs = generators.computeIfAbsent(key.immutable(), k -> new ArrayList<>());
					if (!cs.contains(c))
						cs.add(c);
					continue;
				}
				if (!PowerGrid.running(level, t)) {
					// a shared store all of which is away: on paper, from its record
					paperStore(t, c, d, stores);
					continue;
				}
				// a store shared between blocks or circuits: once, below
				BlockPos sk = storeKeyAt(t);
				// an Industrial Battery whose blocks aren't all loaded can't be worked out: on paper meanwhile
				if (sk == null && level.getBlockEntity(t) instanceof PowerGrid.Store && paperStore(t, c, d, stores))
					continue;
				if (sk != null) {
					grid.storeFace(t, sk);
					StoreAt s = stores.computeIfAbsent(sk.immutable(), k -> new StoreAt(d.energy()::getCapability, d.key()));
					if (!s.circuits.contains(c))
						s.circuits.add(c);
					continue;
				}
				if (!seen.add(t))
					continue;
				IEnergyStorage e = d.energy().getCapability();
				if (e == null)
					continue;
				boolean in = e.canReceive(), out = e.canExtract();
				// something that can give (another mod's battery or generator, even empty or idle) is a source for the lights
				if (out)
					canGive[c] = true;
				if (out) {
					int offer = e.extractEnergy(rate, true);
					if (offer > 0) {
						sources.add(new Source(c, d.energy()::getCapability, null, offer, in, null));
						(in ? stored : gen)[c] += offer;
						// another mod's generator: what it could give this second, as a rate
						if (!in)
							makes += offer / (double) INTERVAL;
					}
				}
				if (in) {
					// a second's worth: room to fill, and (for a machine) what it used in the last second
					int roomNow = e.receiveEnergy(rate, true);
					double used = 0;
					double[] last = use.get(t);
					if (last == null)
						last = carriedUse.remove(t);
					if (last != null && !out)
						used = Math.max(0, last[1] + last[0] - e.getEnergyStored());
					nextUse.put(t.immutable(), new double[] {e.getEnergyStored(), 0});
					int w = (int) Math.min(rate, roomNow + used);
					if (w > 0) {
						takers.add(new Taker(c, d.energy()::getCapability, t.immutable(), w, out, d.key()));
						(out ? room : want)[c] += w;
						live = true;
					}
				}
			}
			// each shared store: what it can give goes to its circuits by what their machines want, its room by which of them
			// have generators (each up to that circuit's rate)
			boolean[] generating = new boolean[count];
			for (List<Integer> cs : generators.values())
				for (int c : cs)
					generating[c] = true;
			double[] haveGen = new double[count];
			for (int c = 0; c < count; c++)
				haveGen[c] = generating[c] || gen[c] > 0 ? 1 : 0;
			for (Map.Entry<BlockPos, StoreAt> st : stores.entrySet()) {
				StoreAt s = st.getValue();
				IEnergyStorage e = s.energy.get();
				if (e == null)
					continue;
				int all = rate(tiers, s.circuits);
				int offer = e.canExtract() ? e.extractEnergy(all, true) : 0, roomNow = e.canReceive() ? e.receiveEnergy(all, true) : 0;
				double[] byNeed = parts(s.circuits, want), byGen = parts(s.circuits, haveGen);
				for (int i = 0; i < s.circuits.size(); i++) {
					int c = s.circuits.get(i), cap = tiers[c].powerPerTick() * INTERVAL;
					int o = (int) Math.min(cap, Math.floor(offer * byNeed[i])), w = (int) Math.min(cap, Math.floor(roomNow * byGen[i]));
					if (o > 0) {
						sources.add(new Source(c, s.energy, null, o, true, null));
						stored[c] += o;
					}
					if (w > 0) {
						takers.add(new Taker(c, s.energy, st.getKey(), w, true, s.key));
						room[c] += w;
						live = true;
					}
				}
			}
			// what each circuit would take of a generator: its own machines and batteries, and for brass, the copper it feeds
			double[] weight = new double[count];
			int[] feeding = new int[count];
			for (int c = 0; c < count; c++)
				weight[c] = want[c] + room[c];
			for (Feed f : active)
				feeding[f.to()]++;
			for (Feed f : active)
				weight[f.from()] += (want[f.to()] + room[f.to()]) / feeding[f.to()];
			for (Map.Entry<BlockPos, List<Integer>> g : generators.entrySet()) {
				BlockPos key = g.getKey();
				List<Integer> cs = g.getValue();
				if (!seen.add(key))
					continue;
				double[] part = parts(cs, weight);
				int rate = rate(tiers, cs);
				// live or on paper by the generator's own block, not the one the line meets
				if (PowerGrid.running(level, key)) {
					if (!(level.getBlockEntity(key) instanceof PowerGrid.Generator)) {
						grid.forget(key);
						continue;
					}
					BlockCapabilityCache<IEnergyStorage, Direction> cache = generatorCaches.computeIfAbsent(key,
						k -> BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, level, k, null));
					IEnergyStorage e = cache.getCapability();
					int offer = e == null || !e.canExtract() ? 0 : e.extractEnergy(rate, true);
					PowerGrid.Paper rec = grid.paper(key);
					makes += rec != null ? rec.making(level) : offer / (double) INTERVAL;
					share(sources, gen, tiers, cs, part, offer, cache::getCapability, null, key);
					continue;
				}
				// on paper, with the touching generators it shares its store with (one running gives as itself, from its own
				// entry); each to its circuits and any of its own lines' too
				for (BlockPos k : grid.row(key)) {
					PowerGrid.Paper p = grid.paper(k);
					if (p == null || PowerGrid.running(level, k))
						continue;
					if (!k.equals(key) && !seen.add(k))
						continue;
					List<Integer> kc = cs;
					List<Integer> own = k.equals(key) ? null : generators.get(k);
					if (own != null) {
						kc = new ArrayList<>(cs);
						for (int c : own)
							if (!kc.contains(c))
								kc.add(c);
					}
					makes += p.making(level);
					share(sources, gen, tiers, kc, kc == cs ? part : parts(kc, weight), grid.draw(p, kc == cs ? rate : rate(tiers, kc), now, true, level),
						null, p, k.immutable());
				}
			}
			// lights learn whether their circuit reaches any power at all: none (a Breaker or switch open, or no generator)
			// and they go out at once; some, but too little, and they starve and flicker out
			boolean[] reaches = new boolean[count];
			for (int c = 0; c < count; c++)
				reaches[c] = generating[c] || canGive[c] || gen[c] > 0 || stored[c] > 0;
			for (Source s : sources)
				reaches[s.circuit()] = true;
			for (StoreAt s : stores.values())
				for (int c : s.circuits)
					reaches[c] = true;
			for (boolean grew = true; grew;) {
				grew = false;
				for (Feed f : active)
					if (reaches[f.from()] && !reaches[f.to()]) {
						reaches[f.to()] = true;
						grew = true;
					}
			}
			// once per light, reached if any of its circuits on this network is (one touching two circuits isn't switched off)
			Map<BlockPos, Boolean> lamps = new LinkedHashMap<>();
			for (Device d : ends)
				lamps.merge(d.end().target(), reaches[circuitOf[d.seg()]], Boolean::logicalOr);
			// ...and once per group of lights that are one lamp (a strip run fed in several places, say either side of a
			// switch, stays lit if any of its feeds reaches power)
			Map<Object, Lamp> groups = new LinkedHashMap<>();
			Map<Object, Boolean> groupReached = new HashMap<>();
			Map<Object, Integer> groupFeeds = new HashMap<>();
			for (Map.Entry<BlockPos, Boolean> l : lamps.entrySet())
				if (level.isLoaded(l.getKey()) && level.getBlockEntity(l.getKey()) instanceof Lamp lamp) {
					Object g = lamp.lampGroup();
					groups.putIfAbsent(g, lamp);
					groupReached.merge(g, l.getValue(), Boolean::logicalOr);
					groupFeeds.merge(g, 1, Integer::sum);
				}
			for (Map.Entry<Object, Lamp> g : groups.entrySet())
				g.getValue().supplied(groupReached.get(g.getKey()), this, groupFeeds.get(g.getKey()));
			use.clear();
			use.putAll(nextUse);
			lastMakes = makes;
			lastCircuit = circuitOf;
			lastSupply = new double[count];
			lastDemand = new double[count];
			lastTook = new double[count];
			if (!live) {
				flowing.remove(this);
				plan = null;
				publish(0);
				return;
			}
			Plan plan = plan(tiers, active, gen, stored, want, room, takers);
			for (int c = 0; c < count; c++) {
				lastSupply[c] = plan.supply[c] / INTERVAL;
				lastDemand[c] = plan.demand[c] / INTERVAL;
			}
			// the next second, a twentieth a tick: each source's part of what its circuit gives, each taker's share
			double[] budget = new double[sources.size()];
			for (int i = 0; i < sources.size(); i++) {
				Source s = sources.get(i);
				int c = s.circuit();
				double from = s.store() ? plan.fromStores[c] : plan.fromGenerators[c], all = s.store() ? stored[c] : gen[c];
				budget[i] = all <= 0 ? 0 : Math.min(s.offer(), from * s.offer() / all) / INTERVAL;
			}
			double[] perTick = new double[takers.size()];
			for (int i = 0; i < takers.size(); i++)
				perTick[i] = takers.get(i).want() * (takers.get(i).store() ? 1 : plan.share[i]) / INTERVAL;
			double[] grant = new double[active.size()];
			for (int i = 0; i < active.size(); i++)
				grant[i] = plan.grant[i] / INTERVAL;
			this.plan = new Schedule(count, plan.fed, sources, budget, takers, perTick, new double[takers.size()], active, grant, circuitOf);
			flowing.add(this);
		}

		/**
		 * Each tick while it has a plan: a twentieth of the second's plan, as much as the takers will have this tick, drawn
		 * from its sources in proportion and nothing more (no simulating the network, no walking it).
		 */
		void flow(long now) {
			Schedule f = plan;
			if (f == null)
				return;
			// a second's plan, and no longer: one not renewed (its machines' wires stopped running) stops
			if (now - lastTick >= INTERVAL + 2) {
				plan = null;
				flowing.remove(this);
				return;
			}
			int count = f.count;
			double[] need = f.need, devices = f.tickDevices, budget = f.budgetOf, own = f.own, passed = f.passed, asked = f.asked, pool = f.pool,
				took = f.took;
			int[] accept = f.accept;
			IEnergyStorage[] taker = f.taker;
			java.util.Arrays.fill(need, 0);
			java.util.Arrays.fill(devices, 0);
			java.util.Arrays.fill(own, 0);
			java.util.Arrays.fill(passed, 0);
			java.util.Arrays.fill(asked, 0);
			java.util.Arrays.fill(pool, 0);
			java.util.Arrays.fill(took, 0);
			for (int i = 0; i < accept.length; i++) {
				Taker t = f.takers.get(i);
				IEnergyStorage e = t.energy().get();
				taker[i] = e;
				accept[i] = 0;
				if (e == null)
					continue;
				double due = f.perTick[i] + f.carry[i];
				int whole = (int) Math.floor(due);
				f.carry[i] = Math.min(1, due - whole);
				accept[i] = whole <= 0 ? 0 : e.receiveEnergy(whole, true);
				need[t.circuit()] += accept[i];
				if (!t.store())
					devices[t.circuit()] += accept[i];
			}
			// copper fed by brass: what its own sources can't give, asked of the brass, up to what was planned
			for (int c = 0; c < count; c++) {
				if (f.fed[c] <= 0)
					continue;
				own[c] = Math.min(need[c], budget[c]);
				double rest = need[c] - own[c];
				for (int i = 0; i < f.feeds.size() && rest > 0; i++)
					if (f.feeds.get(i).to() == c) {
						passed[i] = Math.min(rest, f.grant[i]);
						rest -= passed[i];
						asked[f.feeds.get(i).from()] += passed[i];
					}
			}
			for (int c = 0; c < count; c++)
				if (f.fed[c] <= 0)
					own[c] = Math.min(need[c] + asked[c], budget[c]);
			// draw it, each source its part
			for (int i = 0; i < f.sources.size(); i++) {
				Source s = f.sources.get(i);
				int c = s.circuit();
				if (budget[c] <= 0 || own[c] <= 0)
					continue;
				int take = (int) Math.round(own[c] * f.budget[i] / budget[c]);
				if (take <= 0)
					continue;
				// a generator on paper that's running again gives for real from now (its record is its own again), and one
				// running that has gone away gives on paper only from the next plan
				if (s.generator() != null && (s.paper() != null) == PowerGrid.running(level, s.generator()))
					continue;
				if (s.paper() != null) {
					took[i] = grid.draw(s.paper(), take, now, false, level);
				} else {
					IEnergyStorage e = s.energy().get();
					took[i] = e == null ? 0 : e.extractEnergy(take, false);
				}
				pool[c] += took[i];
			}
			// brass passes the copper its part first if it's short, in proportion; then each circuit gives its takers
			for (int c = 0; c < count; c++) {
				if (f.fed[c] > 0)
					continue;
				double want = need[c] + asked[c], scale = want <= 0 ? 0 : Math.min(1, pool[c] / want);
				for (int i = 0; i < f.feeds.size(); i++)
					if (f.feeds.get(i).from() == c && passed[i] > 0) {
						double give = passed[i] * scale;
						pool[c] -= give;
						pool[f.feeds.get(i).to()] += give;
					}
			}
			for (int c = 0; c < count; c++)
				f.flowed[c] += give(f, f.devicesOf[c], c, pool) + give(f, f.storesOf[c], c, pool);
			// anything left (a taker that turned it down after all, rounding) goes back where it came from
			for (int i = 0; i < f.sources.size(); i++) {
				Source s = f.sources.get(i);
				int c = s.circuit();
				if (pool[c] < 1 || took[i] <= 0)
					continue;
				double back = Math.min(pool[c], took[i]);
				if (s.paper() != null) {
					pool[c] -= grid.giveBack(s.paper(), back);
				} else {
					IEnergyStorage e = s.energy().get();
					// a store takes back its own, past what it could take in (a bank holding more than it can)
					if (e instanceof PowerGrid.Refund r)
						pool[c] -= r.refund((int) back);
					else if (e != null && e.canReceive())
						pool[c] -= e.receiveEnergy((int) back, false);
				}
			}
			for (int c = 0; c < count; c++)
				f.devices[c] += devices[c];
			// once a second, the Meters, the read-out and the goggles' figures
			if (now - lastTick == INTERVAL - 1) {
				double uses = 0;
				for (int c = 0; c < count && c < lastTook.length; c++) {
					lastTook[c] = f.devices[c] / INTERVAL;
					uses += lastTook[c];
				}
				publish(uses);
				for (Map.Entry<Node, Integer> m : meters.entrySet())
					if (level.isLoaded(m.getKey().pos()) && level.getBlockEntity(m.getKey().pos()) instanceof LineBlockEntity be && be.fitting() == Fitting.METER)
						be.readMeter((int) (f.flowed[f.circuitOf[m.getValue()]] / INTERVAL));
				java.util.Arrays.fill(f.flowed, 0);
				java.util.Arrays.fill(f.devices, 0);
			}
		}

		/** Gives these takers of circuit c what they'll have this tick, scaled to what's in the pool. */
		private double give(Schedule f, int[] which, int c, double[] pool) {
			int[] accept = f.accept;
			IEnergyStorage[] taker = f.taker;
			double total = 0;
			for (int i : which)
				total += accept[i];
			if (total <= 0 || pool[c] < 1)
				return 0;
			double scale = Math.min(1, pool[c] / total), gave = 0;
			for (int i : which) {
				Taker t = f.takers.get(i);
				if (taker[i] == null || accept[i] <= 0)
					continue;
				int amount = (int) Math.min(pool[c], Math.floor(accept[i] * scale));
				if (amount <= 0)
					continue;
				int took = taker[i].receiveEnergy(amount, false);
				pool[c] -= took;
				gave += took;
				double[] u = use.get(t.pos());
				if (u != null)
					u[1] += took;
			}
			return gave;
		}

		/** How a generator's offer is shared between the circuits it meets: by what each would take (evenly if none would). */
		private static double[] parts(List<Integer> cs, double[] weight) {
			double[] part = new double[cs.size()];
			double all = 0;
			for (int i = 0; i < part.length; i++)
				all += part[i] = weight[cs.get(i)];
			for (int i = 0; i < part.length; i++)
				part[i] = all <= 0 ? 1.0 / part.length : part[i] / all;
			return part;
		}

		/** What the circuits a generator meets can carry from it in a second, together. */
		private static int rate(LineTier[] tiers, List<Integer> cs) {
			int rate = 0;
			for (int c : cs)
				rate += tiers[c].powerPerTick() * INTERVAL;
			return rate;
		}

		/** A generator's offer as a source on each circuit it meets, each its part (up to that circuit's rate). */
		private static void share(List<Source> sources, double[] gen, LineTier[] tiers, List<Integer> cs, double[] part, int offer,
								  @Nullable java.util.function.Supplier<IEnergyStorage> energy, @Nullable PowerGrid.Paper paper, BlockPos key) {
			for (int i = 0; i < cs.size(); i++) {
				int c = cs.get(i), o = (int) Math.min((long) tiers[c].powerPerTick() * INTERVAL, (long) Math.floor(offer * part[i]));
				if (o > 0) {
					sources.add(new Source(c, energy, paper, o, false, key));
					gen[c] += o;
				}
			}
		}

		/** Tells the machines on it, and the wires that meet them, what it makes and uses (for their goggles). */
		private void publish(double uses) {
			int makes = (int) Math.round(lastMakes), using = (int) Math.round(uses);
			for (Device d : ends) {
				BlockPos t = d.end().target();
				if (level.isLoaded(t) && level.getBlockEntity(t) instanceof NetworkReading.Shown s)
					s.networkReading(makes, using);
			}
			for (BlockPos w : wires)
				if (level.isLoaded(w) && level.getBlockEntity(w) instanceof NetworkReading.Shown s)
					s.networkReading(makes, using);
		}

		/** Each segment's circuit: segments joined through the Breakers that are on. */
		private int[] circuits(boolean[] on) {
			int n = segTier.length;
			int[] root = new int[n];
			for (int i = 0; i < n; i++)
				root[i] = i;
			for (int[] sw : switches)
				if (on[sw[2]]) {
					int a = find(root, sw[0]), b = find(root, sw[1]);
					if (a != b)
						root[a] = b;
				}
			int[] out = new int[n];
			Map<Integer, Integer> ids = new HashMap<>();
			for (int i = 0; i < n; i++)
				out[i] = ids.computeIfAbsent(find(root, i), k -> ids.size());
			return out;
		}

		private static int find(int[] root, int i) {
			while (root[i] != i)
				i = root[i] = root[root[i]];
			return i;
		}

		/** The share of what each taker wants it gets, and per circuit what it draws and passes on. */
		private final class Plan {
			final double[] share, fromGenerators, fromStores, fed, supply, demand;
			/** What each feed passes from brass to copper. */
			final double[] grant;

			Plan(int count, int takers, int feeds) {
				share = new double[takers];
				supply = new double[count];
				demand = new double[count];
				fromGenerators = new double[count];
				fromStores = new double[count];
				fed = new double[count];
				grant = new double[feeds];
			}
		}

		private Plan plan(LineTier[] tiers, List<Feed> feeds, double[] gen, double[] stored, double[] want, double[] room, List<Taker> takers) {
			int count = tiers.length;
			Plan plan = new Plan(count, takers.size(), feeds.size());
			double[] cap = new double[count], need = new double[count], asked = new double[count], received = new double[count],
				spare = new double[count];
			for (int c = 0; c < count; c++)
				cap[c] = (double) tiers[c].powerPerTick() * INTERVAL;
			for (Feed f : feeds)
				plan.fed[f.to()]++;
			// a copper circuit asks the brass feeding it for what its own generators can't give (its devices, then its
			// batteries), up to its rate; at first an even part of it from each brass circuit feeding it
			for (int c = 0; c < count; c++)
				if (plan.fed[c] > 0)
					need[c] = Math.max(0, Math.min(want[c] + room[c], cap[c]) - gen[c]);
			for (Feed f : feeds)
				asked[f.from()] += need[f.to()] / plan.fed[f.to()];
			// brass (and copper fed by nothing) first
			for (int c = 0; c < count; c++) {
				if (plan.fed[c] > 0)
					continue;
				double demand = want[c] + asked[c];
				boolean short_ = gen[c] < demand;
				double supply = Math.min(cap[c], gen[c] + (short_ ? stored[c] : 0));
				double deliver = Math.min(supply, demand);
				plan.supply[c] = supply;
				plan.demand[c] = want[c];
				// what it can give, shared by priority between its devices and the copper it feeds
				List<Integer> mine = new ArrayList<>(), out = new ArrayList<>();
				for (int i = 0; i < takers.size(); i++)
					if (takers.get(i).circuit() == c && !takers.get(i).store())
						mine.add(i);
				for (int i = 0; i < feeds.size(); i++)
					if (feeds.get(i).from() == c)
						out.add(i);
				double[] wants = new double[mine.size() + out.size()];
				int[][] keys = new int[wants.length][];
				for (int k = 0; k < mine.size(); k++) {
					wants[k] = takers.get(mine.get(k)).want();
					keys[k] = takers.get(mine.get(k)).key();
				}
				for (int k = 0; k < out.size(); k++) {
					Feed f = feeds.get(out.get(k));
					wants[mine.size() + k] = need[f.to()] / plan.fed[f.to()];
					keys[mine.size() + k] = f.key();
				}
				double[] got = allocate(deliver, wants, keys);
				for (int k = 0; k < mine.size(); k++)
					plan.share[mine.get(k)] = got[k];
				for (int k = 0; k < out.size(); k++) {
					int i = out.get(k);
					plan.grant[i] = got[mine.size() + k] * wants[mine.size() + k];
					received[feeds.get(i).to()] += plan.grant[i];
				}
				double fromGen = Math.min(gen[c], deliver);
				plan.fromGenerators[c] = fromGen;
				plan.fromStores[c] = deliver - fromGen;
				spare[c] = short_ ? 0 : Math.max(0, Math.min(cap[c], gen[c]) - deliver);
			}
			// a copper circuit one brass circuit couldn't serve asks the others for the rest, from what they have spare
			for (int c = 0; c < count; c++) {
				if (plan.fed[c] <= 0)
					continue;
				double rest = need[c] - received[c];
				for (int i = 0; i < feeds.size() && rest > 1e-6; i++) {
					Feed f = feeds.get(i);
					if (f.to() != c || spare[f.from()] <= 0)
						continue;
					double more = Math.min(rest, spare[f.from()]);
					spare[f.from()] -= more;
					plan.grant[i] += more;
					plan.fromGenerators[f.from()] += more;
					received[c] += more;
					rest -= more;
				}
			}
			// what brass generators still have spare charges its batteries, as far as the line's rate allows
			for (int c = 0; c < count; c++)
				if (plan.fed[c] <= 0)
					plan.fromGenerators[c] += Math.max(0, Math.min(room[c], spare[c]));
			// then the copper the brass feeds: its own generators and what it was passed
			for (int c = 0; c < count; c++) {
				if (plan.fed[c] <= 0)
					continue;
				double in = received[c];
				boolean short_ = gen[c] + in < want[c];
				double supply = Math.min(cap[c], gen[c] + in + (short_ ? stored[c] : 0));
				double deliver = Math.min(supply, want[c]);
				plan.supply[c] = supply;
				plan.demand[c] = want[c];
				List<Integer> mine = new ArrayList<>();
				for (int i = 0; i < takers.size(); i++)
					if (takers.get(i).circuit() == c && !takers.get(i).store())
						mine.add(i);
				double[] wants = new double[mine.size()];
				int[][] keys = new int[mine.size()][];
				for (int k = 0; k < mine.size(); k++) {
					wants[k] = takers.get(mine.get(k)).want();
					keys[k] = takers.get(mine.get(k)).key();
				}
				double[] got = allocate(deliver, wants, keys);
				for (int k = 0; k < mine.size(); k++)
					plan.share[mine.get(k)] = got[k];
				double usedIn = Math.min(in, deliver), own = deliver - usedIn;
				double fromGen = Math.min(gen[c], own);
				plan.fromStores[c] = own - fromGen;
				// batteries take what it was passed and didn't use, then what its own generators have spare
				double charge = Math.max(0, Math.min(room[c], cap[c] - deliver));
				double fromIn = Math.min(charge, in - usedIn);
				plan.fromGenerators[c] = fromGen + (short_ ? 0 : Math.max(0, Math.min(charge - fromIn, gen[c] - fromGen)));
			}
			return plan;
		}

		void zeroMeters() {
			for (Node m : meters.keySet())
				if (level.isLoaded(m.pos()) && level.getBlockEntity(m.pos()) instanceof LineBlockEntity be && be.fitting() == Fitting.METER)
					be.readMeter(0);
		}
	}
}
