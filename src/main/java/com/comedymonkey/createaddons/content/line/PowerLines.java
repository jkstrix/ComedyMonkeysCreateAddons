package com.comedymonkey.createaddons.content.line;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The Power Lines of one level, as networks: wires joined to wires, round corners, up Line Posts and along the spans
 * between their insulators, with the machines at their ends. Each tick a network moves up to its tier's rate from
 * whatever gives power (a generator, a battery) to whatever takes it (a motor), shared evenly unless a Splitter says
 * otherwise, and not past an open Breaker. A battery is charged by generators but never by another battery, so power
 * doesn't slosh between them. Like the Fluid Lines, networks are worked out when needed and forgotten on any change;
 * parts in unloaded chunks are left out until they load.
 */
public final class PowerLines {
	private static final Map<ResourceKey<Level>, PowerLines> BY_LEVEL = new HashMap<>();

	private final ServerLevel level;
	private final Map<Node, Network> byNode = new HashMap<>();
	/** Wire blocks that meet a machine: where the work starts. */
	private final Set<BlockPos> terminals = new LinkedHashSet<>();

	/** A wire block (insulator -1) or one insulator on a Line Post head. */
	public record Node(BlockPos pos, int insulator) {}

	private PowerLines(ServerLevel level) {
		this.level = level;
	}

	public static PowerLines of(ServerLevel level) {
		PowerLines lines = BY_LEVEL.get(level.dimension());
		if (lines == null || lines.level != level) {
			lines = new PowerLines(level);
			BY_LEVEL.put(level.dimension(), lines);
		}
		return lines;
	}

	/** This level's Power Lines, if any were made yet. */
	@Nullable
	static PowerLines existing(ServerLevel level) {
		PowerLines lines = BY_LEVEL.get(level.dimension());
		return lines != null && lines.level == level ? lines : null;
	}

	public static void clear() {
		BY_LEVEL.clear();
	}

	public static void onLevelTick(LevelTickEvent.Post event) {
		if (event.getLevel() instanceof ServerLevel server) {
			PowerLines lines = BY_LEVEL.get(server.dimension());
			if (lines != null && lines.level == server)
				lines.tick();
		}
	}

	/** Something at pos changed: every network it or the blocks round it (corners too) were in is worked out again. */
	public void changed(BlockPos pos) {
		for (int dx = -1; dx <= 1; dx++)
			for (int dy = -1; dy <= 1; dy++)
				for (int dz = -1; dz <= 1; dz++) {
					BlockPos p = pos.offset(dx, dy, dz);
					for (int i = -1; i <= 4; i++)
						forget(new Node(p, i));
				}
	}

	private void forget(Node node) {
		Network n = byNode.get(node);
		if (n == null)
			return;
		for (Node m : n.nodes)
			byNode.remove(m);
		n.zeroMeters(level);
	}

	void addTerminal(BlockPos pos) {
		terminals.add(pos.immutable());
	}

	private void tick() {
		long now = level.getGameTime();
		for (BlockPos pos : new ArrayList<>(terminals)) {
			if (!level.isLoaded(pos))
				continue;
			if (!(level.getBlockEntity(pos) instanceof PowerLineBlockEntity be) || !be.hasTerminal()) {
				terminals.remove(pos);
				continue;
			}
			Node node = new Node(pos, -1);
			Network n = byNode.get(node);
			// a chunk it runs through went, or one at its edge came back: worked out again
			if (n != null && !n.chunksAsBuilt(level)) {
				forget(node);
				n = null;
			}
			if (n == null)
				n = build(node);
			if (n != null)
				n.tick(now);
		}
	}

	// ---------- walking the network ----------

	@Nullable
	private Network build(Node start) {
		if (!(level.getBlockState(start.pos).getBlock() instanceof PowerLineBlock line))
			return null;
		Network n = new Network(line.tier());
		ArrayDeque<Node> queue = new ArrayDeque<>();
		queue.add(start);
		n.nodes.add(start);
		while (!queue.isEmpty()) {
			Node node = queue.poll();
			byNode.put(node, n);
			List<Node> next = new ArrayList<>();
			visit(node, n, next);
			n.links.put(node, next);
			for (Node m : next)
				if (n.nodes.add(m))
					queue.add(m);
		}
		n.route();
		for (Node m : n.nodes)
			n.chunks.add(net.minecraft.world.level.ChunkPos.asLong(m.pos));
		return n;
	}

	/** The nodes joined to this one, and (into the network) the machines it meets and its fitting. */
	private void visit(Node node, Network n, List<Node> next) {
		if (node.insulator < 0) {
			BlockState state = level.getBlockState(node.pos);
			if (!(level.getBlockEntity(node.pos) instanceof PowerLineBlockEntity be) || be.tier() != n.tier)
				return;
			if (be.fitting() != Fitting.NONE)
				n.fittings.put(node, be);
			for (Direction face : Direction.values()) {
				if (!PowerLineBlock.hasFace(state, face))
					continue;
				if ((be.links(face) & WireLink.BASE) != 0)
					n.ends.add(new End(node, node.pos.relative(face), cache(node.pos.relative(face), face.getOpposite())));
				// up (or down) through the block into a machine across it
				if ((be.links(face) & WireLink.RISE) != 0)
					n.ends.add(new End(node, node.pos.relative(face.getOpposite()), cache(node.pos.relative(face.getOpposite()), face)));
				// through the wall it lies on, to the wire behind
				if (WireLink.hole(be.links(face)) != 0)
					wire(node.pos.relative(face, 2), next, n);
				for (Direction d : WireLink.across(face)) {
					BlockPos to = node.pos.relative(d);
					switch (be.link(face, d)) {
						case LINE -> wire(to, next, n);
						case THROUGH -> wire(to.relative(d), next, n);
						case OUTSIDE -> wire(to.relative(face), next, n);
						case POST -> {
							BlockPos head = level.isLoaded(to) ? LinePostBlock.head(level, to) : null;
							if (head != null)
								next.add(new Node(head, 0));
							else
								n.edges.add(net.minecraft.world.level.ChunkPos.asLong(to));
						}
						case TERMINAL -> n.ends.add(new End(node, to, cache(to, d.getOpposite())));
						default -> {}
					}
				}
			}
			return;
		}
		if (!(level.getBlockEntity(node.pos) instanceof LinePostBlockEntity post) || !LinePostBlock.hasInsulator(post.getBlockState(), node.insulator))
			return;
		for (Span s : post.spansAt(node.insulator)) {
			BlockPos far = node.pos.offset(s.offset());
			if (!level.isLoaded(far))
				n.edges.add(net.minecraft.world.level.ChunkPos.asLong(far));
			else if (s.tier() == n.tier && LinePostBlock.hasInsulator(level.getBlockState(far), s.to()))
				next.add(new Node(far, s.to()));
		}
		// the top insulator: down the pole to the wires running under its foot
		if (node.insulator == 0) {
			BlockPos foot = LinePostBlock.foot(level, node.pos);
			if (foot == null)
				return;
			Direction facing = level.getBlockState(foot).getValue(LinePostBlock.FACING);
			for (Direction d : Direction.values()) {
				if (d.getAxis() == facing.getAxis())
					continue;
				BlockPos w = foot.relative(d);
				if (level.isLoaded(w) && level.getBlockEntity(w) instanceof PowerLineBlockEntity be && be.tier() == n.tier
					&& be.link(facing.getOpposite(), d.getOpposite()) == WireLink.POST)
					next.add(new Node(w, -1));
			}
		}
	}

	private void wire(BlockPos pos, List<Node> next, Network n) {
		if (!level.isLoaded(pos))
			n.edges.add(net.minecraft.world.level.ChunkPos.asLong(pos));
		else if (level.getBlockState(pos).getBlock() instanceof PowerLineBlock)
			next.add(new Node(pos, -1));
	}

	private BlockCapabilityCache<IEnergyStorage, Direction> cache(BlockPos target, Direction side) {
		return BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, level, target, side);
	}

	/** A machine on the line: the block {@code target}, met from the wire at {@code node}. */
	record End(Node node, BlockPos target, BlockCapabilityCache<IEnergyStorage, Direction> energy) {}

	/** The way from one machine to another: its length and the fittings on the way, read as they are set each tick. */
	record Route(End to, int length, List<LineBlockEntity> breakers, @Nullable BlockPos splitter, @Nullable LineBlockEntity splitterBe,
				 List<Node> meters) {
		boolean shut() {
			for (LineBlockEntity b : breakers)
				if (b.passes() <= 0)
					return true;
			return false;
		}

		SplitMode mode() {
			return splitterBe == null ? SplitMode.EVEN : splitterBe.splitMode();
		}
	}

	final class Network {
		final LineTier tier;
		final Set<Node> nodes = new LinkedHashSet<>();
		final Map<Node, List<Node>> links = new HashMap<>();
		final List<End> ends = new ArrayList<>();
		final Map<Node, LineBlockEntity> fittings = new HashMap<>();
		final Map<End, List<Route>> routes = new HashMap<>();
		final Map<BlockPos, Integer> turns = new HashMap<>();
		/** The chunks its pieces are in, and the unloaded ones a piece runs on into. */
		final Set<Long> chunks = new java.util.HashSet<>(), edges = new java.util.HashSet<>();
		long lastTick = -1;
		int turn;

		Network(LineTier tier) {
			this.tier = tier;
		}

		/** Every chunk it runs through is still there, and none at its edge has come back. */
		boolean chunksAsBuilt(ServerLevel level) {
			for (long c : chunks)
				if (!level.hasChunk(net.minecraft.world.level.ChunkPos.getX(c), net.minecraft.world.level.ChunkPos.getZ(c)))
					return false;
			for (long c : edges)
				if (level.hasChunk(net.minecraft.world.level.ChunkPos.getX(c), net.minecraft.world.level.ChunkPos.getZ(c)))
					return false;
			return true;
		}

		/** From every machine, the way to every other, walking the joints. */
		void route() {
			for (End from : ends) {
				Map<Node, Node> parent = new HashMap<>();
				Map<Node, Integer> dist = new HashMap<>();
				ArrayDeque<Node> queue = new ArrayDeque<>();
				dist.put(from.node, 0);
				queue.add(from.node);
				while (!queue.isEmpty()) {
					Node node = queue.poll();
					for (Node m : links.getOrDefault(node, List.of()))
						if (!dist.containsKey(m)) {
							dist.put(m, dist.get(node) + 1);
							parent.put(m, node);
							queue.add(m);
						}
				}
				List<Route> list = new ArrayList<>();
				for (End to : ends) {
					if (to == from || to.target.equals(from.target) || !dist.containsKey(to.node))
						continue;
					List<LineBlockEntity> breakers = new ArrayList<>();
					BlockPos splitter = null;
					LineBlockEntity splitterBe = null;
					List<Node> meters = new ArrayList<>();
					for (Node p = to.node; p != null; p = parent.get(p)) {
						LineBlockEntity f = fittings.get(p);
						if (f == null)
							continue;
						switch (f.fitting()) {
							case BREAKER -> breakers.add(f);
							case SPLITTER -> {
								if (splitter == null) {
									splitter = p.pos;
									splitterBe = f;
								}
							}
							case METER -> meters.add(p);
							default -> {}
						}
					}
					list.add(new Route(to, dist.get(to.node), breakers, splitter, splitterBe, meters));
				}
				routes.put(from, list);
			}
		}

		void tick(long now) {
			if (lastTick == now)
				return;
			lastTick = now;
			int remaining = tier.powerPerTick();
			Map<Node, Integer> metered = new HashMap<>();
			int count = ends.size();
			for (int k = 0; k < count && remaining > 0; k++) {
				End from = ends.get((turn + k) % count);
				IEnergyStorage source = from.energy.getCapability();
				if (source == null || !source.canExtract())
					continue;
				int offer = source.extractEnergy(remaining, true);
				if (offer <= 0)
					continue;
				boolean store = source.canReceive();
				int moved = deliver(source, offer, from, false, metered);
				// what pure takers left, batteries may have, but only from a generator
				if (!store && moved < offer)
					moved += deliver(source, offer - moved, from, true, metered);
				remaining -= moved;
			}
			turn++;
			for (Map.Entry<Node, LineBlockEntity> f : fittings.entrySet())
				if (f.getValue().fitting() == Fitting.METER && !f.getValue().isRemoved())
					f.getValue().readMeter(metered.getOrDefault(f.getKey(), 0));
		}

		/** Moves up to {@code offer} FE from source to the takers (batteries only when {@code stores}); returns how much. */
		private int deliver(IEnergyStorage source, int offer, End from, boolean stores, Map<Node, Integer> metered) {
			List<Route> open = new ArrayList<>();
			List<IEnergyStorage> sinks = new ArrayList<>();
			List<Sharing.Claim> claims = new ArrayList<>();
			Set<BlockPos> targets = new java.util.HashSet<>();
			for (Route r : routes.getOrDefault(from, List.of())) {
				if (r.to.target.equals(from.target) || r.shut())
					continue;
				IEnergyStorage sink = r.to.energy.getCapability();
				// one share a machine, however many wires meet it
				if (sink == null || sink == source || !sink.canReceive() || sink.canExtract() != stores || sinks.contains(sink)
					|| targets.contains(r.to.target))
					continue;
				targets.add(r.to.target);
				int room = sink.receiveEnergy(offer, true);
				if (room <= 0)
					continue;
				open.add(r);
				sinks.add(sink);
				claims.add(new Sharing.Claim(room, r.length, r.splitter, r.mode(), List.of()));
			}
			if (open.isEmpty())
				return 0;
			int[] share = Sharing.allocate(offer, claims, new HashMap<>(), turns);
			int total = 0;
			for (int s : share)
				total += s;
			if (total <= 0)
				return 0;
			int drawn = source.extractEnergy(total, false);
			int left = drawn;
			for (int i = 0; i < share.length && left > 0; i++) {
				int give = Math.min(share[i], left);
				if (give <= 0)
					continue;
				int took = sinks.get(i).receiveEnergy(give, false);
				left -= took;
				for (Node m : open.get(i).meters)
					metered.merge(m, took, Integer::sum);
			}
			// anything turned down after all goes back where it came from, if it can take it
			if (left > 0 && source.canReceive())
				left -= source.receiveEnergy(left, false);
			return drawn - left;
		}

		void zeroMeters(ServerLevel level) {
			for (Map.Entry<Node, LineBlockEntity> f : fittings.entrySet())
				if (f.getValue().fitting() == Fitting.METER && !f.getValue().isRemoved() && level.isLoaded(f.getKey().pos))
					f.getValue().readMeter(0);
		}
	}
}
