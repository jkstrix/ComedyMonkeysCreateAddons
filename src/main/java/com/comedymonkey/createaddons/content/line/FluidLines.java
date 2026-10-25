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
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * The Fluid Lines of one level, as networks: every piece of line joined to the next, with the tanks they deliver to,
 * the Intakes they draw from and the fittings on the way. Each tick a network with an Intake moves up to its tier's
 * rate, one fluid at a time, from its Intakes straight into its tanks: shared evenly unless a Splitter says otherwise,
 * past Filters only if it's their fluid, through Valves only as far as they're open. Nothing ever stays in the line, so
 * breaking a piece can't spill or duplicate anything. Networks are worked out when first needed and forgotten whenever a
 * piece of line, a joint, a fitting or its setting changes, or a chunk with line in it loads or unloads. Parts of a line
 * in unloaded chunks are left out until they load: their tanks get nothing meanwhile.
 */
public final class FluidLines {
	private static final Map<ResourceKey<Level>, FluidLines> BY_LEVEL = new HashMap<>();
	/**
	 * Ticks after fluid last ran through a piece that its sight slot still shows it moving (so a slow trickle, or a
	 * Splitter taking turns, doesn't flicker).
	 */
	private static final int SHOW_FOR = 10;
	/**
	 * Ticks an Intake's tank may stand empty and still count as a supply: the line drains a slow source (a Wellspring)
	 * dry every tick between its top-ups, which mustn't empty and refill the whole line each time.
	 */
	private static final int SUPPLY_FOR = 40;

	private final ServerLevel level;
	private final Map<BlockPos, Network> byPos = new HashMap<>();
	/** Line blocks with an Intake: the only places a network's work starts from. */
	private final Set<BlockPos> intakes = new LinkedHashSet<>();
	/** A piece that may be showing fluid no network gave it (loaded from the save, cut off): emptied unless one has it. */
	void recheck(BlockPos pos) {
		stale.add(pos.immutable());
	}

	/** Pieces a forgotten network was showing fluid in: emptied after the next tick unless a new network has them. */
	private final Set<BlockPos> stale = new java.util.HashSet<>();

	private FluidLines(ServerLevel level) {
		this.level = level;
	}

	public static FluidLines of(ServerLevel level) {
		FluidLines lines = BY_LEVEL.get(level.dimension());
		if (lines == null || lines.level != level) {
			lines = new FluidLines(level);
			BY_LEVEL.put(level.dimension(), lines);
		}
		return lines;
	}

	public static void clear() {
		BY_LEVEL.clear();
	}

	public static void onLevelTick(LevelTickEvent.Post event) {
		if (event.getLevel() instanceof ServerLevel server) {
			FluidLines lines = BY_LEVEL.get(server.dimension());
			if (lines != null && lines.level == server)
				lines.tick();
		}
	}

	/**
	 * A chunk unloading takes every network with a piece in it apart, so the rest is worked out again without it (and
	 * nothing flows across the gap). Block entities' own unload hook doesn't fire for an ordinary chunk unload.
	 */
	public static void onChunkUnload(net.neoforged.neoforge.event.level.ChunkEvent.Unload event) {
		if (!(event.getLevel() instanceof ServerLevel server) || !(event.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk chunk))
			return;
		FluidLines fluid = BY_LEVEL.get(server.dimension());
		PowerLines power = PowerLines.existing(server);
		for (net.minecraft.world.level.block.entity.BlockEntity be : chunk.getBlockEntities().values()) {
			if (fluid != null && be instanceof FluidLineBlockEntity)
				fluid.changed(be.getBlockPos());
			if (power != null && (be instanceof PowerLineBlockEntity || be instanceof LinePostBlockEntity))
				power.changed(be.getBlockPos());
		}
	}

	/** Something about the line at pos changed: its network, and any its neighbours were in, are worked out again. */
	public void changed(BlockPos pos) {
		forget(pos);
		for (Direction d : Direction.values())
			forget(pos.relative(d));
	}

	private void forget(BlockPos pos) {
		Network n = byPos.get(pos);
		if (n == null)
			return;
		for (BlockPos p : n.members)
			byPos.remove(p);
		// the network worked out next keeps what's still in the pieces; the rest is emptied after it has had its tick
		stale.addAll(n.applied.keySet());
		stale.addAll(n.draining.keySet());
		stale.addAll(n.holding);
		n.meters(level, true);
	}

	void addIntake(BlockPos pos) {
		intakes.add(pos.immutable());
	}

	private void tick() {
		long now = level.getGameTime();
		for (BlockPos pos : new ArrayList<>(intakes)) {
			if (!level.isLoaded(pos))
				continue;
			if (!(level.getBlockEntity(pos) instanceof FluidLineBlockEntity be) || be.intakeSide() == null) {
				intakes.remove(pos);
				continue;
			}
			Network n = byPos.get(pos);
			// a chunk it runs through went, or one at its edge came back: worked out again
			if (n != null && !n.chunksAsBuilt(level)) {
				forget(pos);
				n = null;
			}
			if (n == null)
				n = build(pos);
			if (n != null)
				n.tick(level, now);
		}
		// pieces no network has any more (cut off from every Intake): empty
		if (!stale.isEmpty()) {
			for (BlockPos p : stale)
				if (!byPos.containsKey(p) && level.isLoaded(p) && level.getBlockEntity(p) instanceof FluidLineBlockEntity be) {
					be.setFlow(FluidStack.EMPTY, 0, false, FluidLineBlockEntity.FULL, 0);
					be.setHolding(false);
				}
			stale.clear();
		}
	}

	/** The network the line at start belongs to, worked out by walking every joint. */
	@Nullable
	private Network build(BlockPos start) {
		if (!(level.getBlockState(start).getBlock() instanceof FluidLineBlock line))
			return null;
		Network n = new Network(line.tier());
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start.immutable());
		Set<BlockPos> seen = new LinkedHashSet<>(queue);
		while (!queue.isEmpty()) {
			BlockPos pos = queue.poll();
			BlockState state = level.getBlockState(pos);
			if (!(state.getBlock() instanceof FluidLineBlock))
				continue;
			n.members.add(pos);
			byPos.put(pos, n);
			LineBlockEntity be = LineBlockEntity.at(level, pos);
			if (be != null && be.fitting() != Fitting.NONE && be.fitting() != Fitting.INTAKE)
				n.fittings.put(pos, be);
			for (Direction d : Direction.values()) {
				BlockPos next = pos.relative(d);
				switch (FluidLineBlock.side(state, d)) {
					case LINE -> {
						if (!level.isLoaded(next))
							n.edges.add(net.minecraft.world.level.ChunkPos.asLong(next));
						else if (!seen.contains(next) && level.getBlockState(next).getBlock() instanceof FluidLineBlock
							&& FluidLineBlock.side(level.getBlockState(next), d.getOpposite()) == LineSide.LINE) {
							seen.add(next);
							queue.add(next);
						}
					}
					case TANK -> n.ends.add(new End(pos, d, cache(next, d)));
					case INTAKE -> n.intakes.add(new Intake(pos, d, cache(next, d), be));
					default -> {}
				}
			}
		}
		n.route(level);
		for (BlockPos p : n.members)
			n.chunks.add(net.minecraft.world.level.ChunkPos.asLong(p));
		return n;
	}

	private BlockCapabilityCache<IFluidHandler, Direction> cache(BlockPos target, Direction from) {
		return BlockCapabilityCache.create(Capabilities.FluidHandler.BLOCK, level, target, from.getOpposite());
	}

	/** A tank the line delivers into, through side {@code side} of the line block at {@code pos}. */
	record End(BlockPos pos, Direction side, BlockCapabilityCache<IFluidHandler, Direction> tank) {
		BlockPos target() {
			return pos.relative(side);
		}
	}

	/** An Intake on side {@code side} of the line block at {@code pos}, drawing from the block it faces. */
	record Intake(BlockPos pos, Direction side, BlockCapabilityCache<IFluidHandler, Direction> tank, @Nullable LineBlockEntity be) {
		FluidStack filter() {
			return be == null ? FluidStack.EMPTY : be.filter();
		}

		BlockPos target() {
			return pos.relative(side);
		}
	}

	/**
	 * The way from an Intake to one tank: its length and the fittings the fluid passes on the way, read as they are set
	 * each tick (a setting changing needs no new network).
	 */
	record Route(End end, int length, List<BlockPos> valves, List<LineBlockEntity> filters, @Nullable BlockPos splitter,
				 @Nullable LineBlockEntity splitterBe, List<BlockPos> meters, Map<BlockPos, LineBlockEntity> fittings, List<BlockPos> path) {
		boolean shut() {
			for (BlockPos v : valves)
				if (fittings.get(v).passes() <= 0)
					return true;
			return false;
		}

		/** A Filter set to another fluid holds it back (an empty one lets anything past). */
		boolean lets(FluidStack fluid) {
			for (LineBlockEntity f : filters)
				if (!f.filter().isEmpty() && !FluidStack.isSameFluidSameComponents(f.filter(), fluid))
					return false;
			return true;
		}

		SplitMode mode() {
			return splitterBe == null ? SplitMode.EVEN : splitterBe.splitMode();
		}
	}

	/** What a piece's sight slot was last told to show: the fluid (its hash), whether it slides, and its fill or drain. */
	record Shown(int fluid, boolean moving, int mode, long since) {}

	static final class Network {
		final LineTier tier;
		final Set<BlockPos> members = new LinkedHashSet<>();
		final List<End> ends = new ArrayList<>();
		final List<Intake> intakes = new ArrayList<>();
		/** The in-line fittings (everything but Intakes), by where they sit. */
		final Map<BlockPos, LineBlockEntity> fittings = new HashMap<>();
		final Map<Intake, List<Route>> routes = new HashMap<>();
		/** Per member, the sides the fluid leaves through (towards the tanks), for the sight slots. */
		final Map<BlockPos, Integer> downstream = new HashMap<>();
		final Map<BlockPos, Integer> turns = new HashMap<>();
		/** The chunks its pieces are in, and the unloaded ones a piece runs on into. */
		final Set<Long> chunks = new java.util.HashSet<>(), edges = new java.util.HashSet<>();
		long lastTick = -1;
		/** When fluid last ran through each piece (it's moving there), and what each piece stands full of. */
		final Map<BlockPos, Long> wetAt = new HashMap<>();
		Map<BlockPos, FluidStack> filled = Map.of();
		/** The Valves and Filters with fluid against them that they won't let past. */
		Set<BlockPos> holding = Set.of();
		/** What each piece's sight slot was last told to show (a fluid, still or moving), and what the fill was worked out from. */
		final Map<BlockPos, Shown> applied = new HashMap<>();
		int fillKey = 0;
		boolean filledOnce;
		/** Each piece's walking distance from the nearest Intake. */
		final Map<BlockPos, Integer> depth = new HashMap<>();
		/** Pieces that lost their fluid and are draining: what they still show, and the tick they're empty. */
		final Map<BlockPos, FluidStack> draining = new HashMap<>();
		final Map<BlockPos, Long> drainedAt = new HashMap<>();
		/** The tick each full piece filled (or will, as the fluid works its way along), and when the last one does. */
		final Map<BlockPos, Long> filledAt = new HashMap<>();
		/** Each Intake's last fluid on offer and when, so a tank drained dry between top-ups still counts (see SUPPLY_FOR). */
		final Map<BlockPos, FluidStack> offered = new HashMap<>();
		final Map<BlockPos, Long> offeredAt = new HashMap<>();
		long fillingUntil = Long.MIN_VALUE / 2;
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

		/** The way from each Intake to each tank, and which way the fluid runs through each piece. */
		void route(Level level) {
			Map<BlockPos, Integer> nearest = new HashMap<>();
			for (Intake in : intakes) {
				Map<BlockPos, BlockPos> parent = new HashMap<>();
				Map<BlockPos, Integer> dist = walk(level, in.pos, parent);
				dist.forEach((p, d) -> nearest.merge(p, d, Math::min));
				List<Route> list = new ArrayList<>();
				for (End end : ends) {
					if (!dist.containsKey(end.pos))
						continue;
					List<LineBlockEntity> filters = new ArrayList<>();
					List<BlockPos> valves = new ArrayList<>(), meters = new ArrayList<>();
					BlockPos splitter = null;
					LineBlockEntity splitterBe = null;
					List<BlockPos> path = new ArrayList<>();
					// walk back from the tank to the Intake; the first Splitter met is the one that governs this tank
					for (BlockPos p = end.pos; p != null; p = parent.get(p)) {
						path.add(p);
						LineBlockEntity f = fittings.get(p);
						if (f == null)
							continue;
						switch (f.fitting()) {
							case VALVE -> valves.add(p);
							case FILTER -> filters.add(f);
							case SPLITTER -> {
								if (splitter == null) {
									splitter = p;
									splitterBe = f;
								}
							}
							case METER -> meters.add(p);
							default -> {}
						}
					}
					list.add(new Route(end, dist.get(end.pos), valves, filters, splitter, splitterBe, meters, fittings, path));
				}
				list.sort((a, b) -> Integer.compare(a.length, b.length));
				routes.put(in, list);
			}
			for (BlockPos pos : members) {
				BlockState state = level.getBlockState(pos);
				int out = 0;
				Integer here = nearest.get(pos);
				for (Direction d : Direction.values()) {
					LineSide side = FluidLineBlock.side(state, d);
					Integer there = nearest.get(pos.relative(d));
					if (side == LineSide.TANK || (side == LineSide.LINE && here != null && there != null && there > here))
						out |= 1 << d.ordinal();
				}
				downstream.put(pos, out);
			}
			depth.putAll(nearest);
		}

		/** Walking distances through the joints from {@code from} to every member, with each one's way back. */
		private Map<BlockPos, Integer> walk(Level level, BlockPos from, Map<BlockPos, BlockPos> parent) {
			Map<BlockPos, Integer> dist = new HashMap<>();
			ArrayDeque<BlockPos> queue = new ArrayDeque<>();
			dist.put(from, 0);
			queue.add(from);
			while (!queue.isEmpty()) {
				BlockPos pos = queue.poll();
				BlockState state = level.getBlockState(pos);
				for (Direction d : Direction.values()) {
					if (FluidLineBlock.side(state, d) != LineSide.LINE)
						continue;
					BlockPos next = pos.relative(d);
					if (members.contains(next) && !dist.containsKey(next)) {
						dist.put(next, dist.get(pos) + 1);
						parent.put(next, pos);
						queue.add(next);
					}
				}
			}
			return dist;
		}

		void tick(ServerLevel level, long now) {
			if (lastTick == now)
				return;
			lastTick = now;
			int remaining = tier.fluidPerTick();
			FluidStack moved = FluidStack.EMPTY;
			Map<BlockPos, Integer> capLeft = new HashMap<>();
			for (Map.Entry<BlockPos, LineBlockEntity> f : fittings.entrySet())
				if (f.getValue().fitting() == Fitting.VALVE)
					capLeft.put(f.getKey(), f.getValue().passes());
			Map<BlockPos, Integer> metered = new HashMap<>();
			int count = intakes.size();
			// the Intakes take turns going first, so two on one line share it over time
			for (int k = 0; k < count && remaining > 0; k++) {
				Intake in = intakes.get((turn + k) % count);
				IFluidHandler source = in.tank.getCapability();
				if (source == null)
					continue;
				FluidStack filter = in.filter();
				FluidStack offer = filter.isEmpty() ? source.drain(remaining, FluidAction.SIMULATE)
					: source.drain(filter.copyWithAmount(remaining), FluidAction.SIMULATE);
				if (offer.isEmpty() || (!moved.isEmpty() && !FluidStack.isSameFluidSameComponents(offer, moved)))
					continue;
				int done = deliver(source, offer, in, capLeft, metered, now);
				if (done > 0) {
					remaining -= done;
					moved = offer;
				}
			}
			turn++;
			for (Map.Entry<BlockPos, LineBlockEntity> f : fittings.entrySet())
				if (f.getValue().fitting() == Fitting.METER && !f.getValue().isRemoved())
					f.getValue().readMeter(metered.getOrDefault(f.getKey(), 0));
			show(level, now);
		}

		/** Moves up to offer's amount from the Intake's tank into the tanks it reaches; returns how much moved. */
		private int deliver(IFluidHandler source, FluidStack offer, Intake in, Map<BlockPos, Integer> capLeft,
							Map<BlockPos, Integer> metered, long now) {
			List<Route> open = new ArrayList<>();
			List<IFluidHandler> tanks = new ArrayList<>();
			List<Sharing.Claim> claims = new ArrayList<>();
			Set<BlockPos> targets = new java.util.HashSet<>();
			for (Route r : routes.getOrDefault(in, List.of())) {
				// never back into the block it's drawn from
				if (r.end.target().equals(in.target()) || !r.lets(offer) || r.shut())
					continue;
				// the line is still filling on the way to this tank
				Long full = filledAt.get(r.end.pos);
				if (full == null || full + tier.drainTicksPerBlock() > now)
					continue;
				IFluidHandler tank = r.end.tank.getCapability();
				// never into the tank it draws from, even through another block of a multiblock tank; one share a tank
				if (tank == null || tank == source || tanks.contains(tank) || targets.contains(r.end.target()))
					continue;
				targets.add(r.end.target());
				int room = tank.fill(offer.copyWithAmount(offer.getAmount()), FluidAction.SIMULATE);
				open.add(r);
				tanks.add(tank);
				List<BlockPos> caps = new ArrayList<>();
				for (BlockPos v : r.valves)
					if (fittings.get(v).passes() < Integer.MAX_VALUE)
						caps.add(v);
				claims.add(new Sharing.Claim(room, r.length, r.splitter, r.mode(), caps));
			}
			if (open.isEmpty())
				return 0;
			int[] share = Sharing.allocate(offer.getAmount(), claims, capLeft, turns);
			int total = 0;
			for (int s : share)
				total += s;
			if (total <= 0)
				return 0;
			FluidStack drawn = source.drain(offer.copyWithAmount(total), FluidAction.EXECUTE);
			int left = drawn.getAmount();
			int[] given = new int[share.length];
			for (int i = 0; i < share.length && left > 0; i++) {
				int give = Math.min(share[i], left);
				if (give > 0) {
					given[i] = tanks.get(i).fill(drawn.copyWithAmount(give), FluidAction.EXECUTE);
					left -= given[i];
				}
			}
			// anything a tank turned down after all: to the others with room, else back where it came from
			for (int i = 0; i < share.length && left > 0; i++) {
				int more = tanks.get(i).fill(drawn.copyWithAmount(left), FluidAction.EXECUTE);
				given[i] += more;
				left -= more;
			}
			if (left > 0)
				left -= source.fill(drawn.copyWithAmount(left), FluidAction.EXECUTE);
			Sharing.spend(claims, given, capLeft);
			for (int i = 0; i < given.length; i++) {
				for (BlockPos m : open.get(i).meters)
					metered.merge(m, given[i], Integer::sum);
				if (given[i] > 0 && now % 5 == 0)
					for (BlockPos p : open.get(i).path)
						wetAt.put(p, now);
			}
			return drawn.getAmount() - left;
		}

		/**
		 * Shows each piece's sight slot as it is: full of the fluid that can reach it (sliding along where it ran lately,
		 * still where it's held up, as by a full tank), or empty past a shut Valve, a Filter set to another fluid, or with
		 * nothing to draw. Only the pieces whose slot changes are told.
		 */
		void show(ServerLevel level, long now) {
			List<FluidStack> offers = offers(now);
			int key = fillKey(offers);
			if (!filledOnce || key != fillKey) {
				// starts from what the pieces show now (full, or still draining), not from scratch
				Map<BlockPos, FluidStack> before = showing(level);
				filledOnce = true;
				fillKey = key;
				Set<BlockPos> held = new java.util.HashSet<>();
				filled = fill(level, offers, held);
				// their signal flags go up, or down
				for (Map.Entry<BlockPos, LineBlockEntity> f : fittings.entrySet())
					if ((f.getValue().fitting() == Fitting.VALVE || f.getValue().fitting() == Fitting.FILTER) && !f.getValue().isRemoved())
						f.getValue().setHolding(held.contains(f.getKey()));
				holding = held;
				drain(before, now);
				rise(before, now);
			}
			if (filled.isEmpty() && applied.isEmpty() && draining.isEmpty())
				return;
			int per = tier.drainTicksPerBlock();
			for (BlockPos pos : members) {
				FluidStack fluid = filled.getOrDefault(pos, FluidStack.EMPTY);
				int mode = FluidLineBlockEntity.FULL;
				long since = 0;
				if (!fluid.isEmpty()) {
					long at = filledAt.getOrDefault(pos, Long.MIN_VALUE / 2);
					// not there yet: the fluid is still working its way along; then it creeps through this block
					if (now < at)
						fluid = FluidStack.EMPTY;
					else if (now < at + per) {
						mode = FluidLineBlockEntity.FILLING;
						since = at;
					}
				}
				boolean moving = !fluid.isEmpty() && (now - wetAt.getOrDefault(pos, Long.MIN_VALUE / 2) <= SHOW_FOR || now <= fillingUntil);
				// the last of the fluid running out of a piece that's lost its supply, on its way to the ends
				if (fluid.isEmpty() && draining.containsKey(pos)) {
					long end = drainedAt.get(pos);
					if (now < end) {
						fluid = draining.get(pos);
						moving = true;
						// its last block's worth runs out of it
						if (now >= end - per) {
							mode = FluidLineBlockEntity.DRAINING;
							since = end - per;
						}
					} else {
						draining.remove(pos);
						drainedAt.remove(pos);
					}
				}
				Shown want = fluid.isEmpty() ? null : new Shown(FluidStack.hashFluidAndComponents(fluid), moving, mode, since);
				if (java.util.Objects.equals(want, applied.get(pos)))
					continue;
				if (want == null)
					applied.remove(pos);
				else
					applied.put(pos, want);
				if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof FluidLineBlockEntity be)
					be.setFlow(fluid, downstream.getOrDefault(pos, 0), moving, mode, since);
			}
		}

		/**
		 * The pieces that were full and no longer are (the source ran dry, a Valve shut, a Filter changed) drain from the
		 * end nearest the Intake outwards, a block every few ticks, rather than all emptying at once.
		 */
		private void drain(Map<BlockPos, FluidStack> before, long now) {
			int first = Integer.MAX_VALUE;
			List<BlockPos> lost = new ArrayList<>();
			for (Map.Entry<BlockPos, FluidStack> e : before.entrySet()) {
				FluidStack after = filled.get(e.getKey());
				if (after != null && FluidStack.isSameFluidSameComponents(after, e.getValue()))
					continue;
				lost.add(e.getKey());
				first = Math.min(first, depth.getOrDefault(e.getKey(), 0));
			}
			for (BlockPos pos : lost) {
				if (filled.containsKey(pos))
					continue; // another fluid fills it straight away
				draining.put(pos, before.get(pos));
				drainedAt.put(pos, now + (long) (depth.getOrDefault(pos, 0) - first + 1) * tier.drainTicksPerBlock());
			}
			// refilled while draining: full again
			draining.keySet().removeIf(filled::containsKey);
			drainedAt.keySet().retainAll(draining.keySet());
		}

		/** What its pieces show now (left by the network before it), as a starting point. */
		private Map<BlockPos, FluidStack> showing(Level level) {
			Map<BlockPos, FluidStack> out = new HashMap<>();
			for (BlockPos pos : members)
				if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof FluidLineBlockEntity be && !be.flowing().isEmpty())
					out.put(pos, be.flowing());
			return out;
		}

		/**
		 * The pieces newly full fill from the end nearest the Intake outwards, a block every few ticks; the ones already
		 * full of the same fluid stay full.
		 */
		private void rise(Map<BlockPos, FluidStack> before, long now) {
			int first = Integer.MAX_VALUE;
			List<BlockPos> rising = new ArrayList<>();
			for (Map.Entry<BlockPos, FluidStack> e : filled.entrySet()) {
				FluidStack was = before.get(e.getKey());
				if (was != null && FluidStack.isSameFluidSameComponents(was, e.getValue())) {
					filledAt.putIfAbsent(e.getKey(), Long.MIN_VALUE / 2);
					continue;
				}
				rising.add(e.getKey());
				first = Math.min(first, depth.getOrDefault(e.getKey(), 0));
			}
			for (BlockPos pos : rising) {
				long at = now + (long) (depth.getOrDefault(pos, 0) - first) * tier.drainTicksPerBlock();
				filledAt.put(pos, at);
				fillingUntil = Math.max(fillingUntil, at + tier.drainTicksPerBlock());
			}
			filledAt.keySet().retainAll(filled.keySet());
		}

		/** The fluid each Intake could draw now (empty when its tank has none, or none its filter takes). */
		private List<FluidStack> offers(long now) {
			List<FluidStack> out = new ArrayList<>(intakes.size());
			for (Intake in : intakes) {
				IFluidHandler source = in.tank.getCapability();
				FluidStack filter = in.filter();
				FluidStack offer = source == null ? FluidStack.EMPTY : filter.isEmpty() ? source.drain(1, FluidAction.SIMULATE)
					: source.drain(filter.copyWithAmount(1), FluidAction.SIMULATE);
				if (!offer.isEmpty()) {
					offered.put(in.pos, offer.copyWithAmount(1));
					offeredAt.put(in.pos, now);
				} else if (source != null && now - offeredAt.getOrDefault(in.pos, Long.MIN_VALUE / 2) <= SUPPLY_FOR)
					// drained dry for now: still the supply until it's been empty a while
					offer = offered.get(in.pos);
				out.add(offer);
			}
			return out;
		}

		/** What the fill depends on: the Intakes' fluids, the Valves shut and the Filters' settings. */
		private int fillKey(List<FluidStack> offers) {
			int key = 1;
			for (FluidStack f : offers)
				key = 31 * key + (f.isEmpty() ? 0 : FluidStack.hashFluidAndComponents(f));
			for (Map.Entry<BlockPos, LineBlockEntity> e : fittings.entrySet()) {
				LineBlockEntity f = e.getValue();
				if (f.fitting() == Fitting.VALVE)
					key = 31 * key + (f.passes() <= 0 ? 1 : 2);
				else if (f.fitting() == Fitting.FILTER)
					key = 31 * key + (f.filter().isEmpty() ? 3 : FluidStack.hashFluidAndComponents(f.filter()));
			}
			return key;
		}

		/**
		 * What each piece stands full of: whatever an Intake with fluid to give reaches, through the joints, short of a shut
		 * Valve or a Filter set to another fluid (those pieces and everything past them stay empty). Where two Intakes'
		 * fluids meet, the first one's fills the piece.
		 */
		private Map<BlockPos, FluidStack> fill(Level level, List<FluidStack> offers, Set<BlockPos> held) {
			Map<BlockPos, FluidStack> out = new HashMap<>();
			for (int i = 0; i < intakes.size(); i++) {
				FluidStack fluid = offers.get(i);
				BlockPos start = intakes.get(i).pos;
				if (fluid.isEmpty() || out.containsKey(start))
					continue;
				FluidStack one = fluid.copyWithAmount(1);
				ArrayDeque<BlockPos> queue = new ArrayDeque<>();
				out.put(start, one);
				queue.add(start);
				while (!queue.isEmpty()) {
					BlockPos pos = queue.poll();
					BlockState state = level.getBlockState(pos);
					for (Direction d : Direction.values()) {
						if (FluidLineBlock.side(state, d) != LineSide.LINE)
							continue;
						BlockPos next = pos.relative(d);
						if (!members.contains(next) || out.containsKey(next))
							continue;
						if (holdsBack(next, one))
							held.add(next);
						else {
							out.put(next, one);
							queue.add(next);
						}
					}
				}
			}
			return out;
		}

		/** A shut Valve, or a Filter set to another fluid, at pos. */
		private boolean holdsBack(BlockPos pos, FluidStack fluid) {
			LineBlockEntity f = fittings.get(pos);
			if (f == null)
				return false;
			return switch (f.fitting()) {
				case VALVE -> f.passes() <= 0;
				case FILTER -> !f.filter().isEmpty() && !FluidStack.isSameFluidSameComponents(f.filter(), fluid);
				default -> false;
			};
		}

		/** Zeroes the Meters when the network is forgotten (a new one will read them again). */
		void meters(ServerLevel level, boolean zero) {
			if (!zero)
				return;
			for (Map.Entry<BlockPos, LineBlockEntity> f : fittings.entrySet())
				if (f.getValue().fitting() == Fitting.METER && !f.getValue().isRemoved() && level.isLoaded(f.getKey()))
					f.getValue().readMeter(0);
		}
	}
}
