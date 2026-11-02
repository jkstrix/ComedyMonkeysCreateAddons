package com.comedymonkey.createaddons.content.wastes.city;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;

import com.comedymonkey.createaddons.content.wastes.CityRecord;
import com.comedymonkey.createaddons.registry.ModStructures;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The whole of a Clockwork Wastes city as one structure piece: as each chunk it covers generates, that chunk's share of
 * the city's {@link CityPlan} is built into it ({@link CityApplier.Gen}). The plan is the same every time (it depends on
 * the origin and seed only); each city's is drawn once, by the first chunk to want it, while its other chunks wait on
 * that draw (and other cities' chunks don't), and the last few are kept, so a city's 200-odd chunks don't each draw it
 * afresh. The chunk holding the heart records the city ({@link CityRecord#addLater}).
 * <p>
 * {@code /place structure} (a live level, every chunk of it loaded) builds it whole at once instead, as
 * {@code /wastes buildcity} does ({@link CityApplier.Live}).
 */
public final class ClockworkCityPiece extends StructurePiece {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final int DOWN = CityApplier.TRESTLE_REACH + 2, UP = CityLayout.CROWN_Y + 4;
	/** Plans kept: enough for the few cities world generation works on at once. */
	private static final int KEPT = 6;

	private record Key(long origin, long seed, boolean outskirtsOnly) {}

	private static final ConcurrentHashMap<Key, CompletableFuture<CityPlan>> PLANS = new ConcurrentHashMap<>();
	/** The plans kept, oldest first. */
	private static final ConcurrentLinkedQueue<Key> KEPT_ORDER = new ConcurrentLinkedQueue<>();
	/** Cities whose plan couldn't be drawn (built as nothing, never drawn again), and cities started on an older plan. */
	private static final Set<Key> FAILED = ConcurrentHashMap.newKeySet(), OLDER = ConcurrentHashMap.newKeySet();

	private final BlockPos origin;
	private final long seed;
	private final boolean outskirtsOnly;
	/** The {@link CityPlan#VERSION} the city was started with (0: before it was saved). */
	private final int version;

	public ClockworkCityPiece(BlockPos origin, long seed, boolean outskirtsOnly) {
		super(ModStructures.CLOCKWORK_CITY_PIECE.get(), 0, box(origin));
		this.origin = origin.immutable();
		this.seed = seed;
		this.outskirtsOnly = outskirtsOnly;
		this.version = CityPlan.VERSION;
	}

	public ClockworkCityPiece(CompoundTag tag) {
		super(ModStructures.CLOCKWORK_CITY_PIECE.get(), tag);
		this.origin = BlockPos.of(tag.getLong("Origin"));
		this.seed = tag.getLong("Seed");
		this.outskirtsOnly = tag.getBoolean("Outskirts");
		this.version = tag.getInt("PlanVersion");
	}

	private static BoundingBox box(BlockPos o) {
		int r = CityEdge.REACH;
		return new BoundingBox(o.getX() - r, o.getY() - DOWN, o.getZ() - r, o.getX() + r, o.getY() + UP, o.getZ() + r);
	}

	/** The city's plan, from the cache or drawn afresh (each city's once, whichever threads want it). */
	public static CityPlan plan(BlockPos origin, long seed, boolean outskirtsOnly) {
		Key key = new Key(origin.asLong(), seed, outskirtsOnly);
		CompletableFuture<CityPlan> mine = new CompletableFuture<>();
		CompletableFuture<CityPlan> plan = PLANS.computeIfAbsent(key, k -> mine);
		if (plan == mine) {
			// drawn here, outside the map's lock: other chunks of this city wait on it, other cities' don't
			try {
				mine.complete(draw(key, origin, seed, outskirtsOnly));
			} catch (Throwable t) {
				// (an Error: the waiting chunks fail with it, and the next to want it tries again)
				PLANS.remove(key, mine);
				mine.completeExceptionally(t);
				throw t;
			}
			KEPT_ORDER.add(key);
			while (KEPT_ORDER.size() > KEPT) {
				Key old = KEPT_ORDER.poll();
				if (old != null)
					PLANS.remove(old);
			}
		}
		return plan.join();
	}

	/**
	 * The plan for world generation: without the kit's whole building models (only the plan tests read them). A plan that
	 * throws leaves the city out (logged once), rather than every one of its chunks failing.
	 */
	private static CityPlan draw(Key key, BlockPos origin, long seed, boolean outskirtsOnly) {
		if (FAILED.contains(key))
			return new CityPlan(origin, seed, outskirtsOnly);
		long start = System.nanoTime();
		try {
			CityPlan plan = CityPlan.of(origin, seed, outskirtsOnly);
			plan.buildings.clear();
			plan.opsIn(new ChunkPos(origin));
			LOGGER.debug("Drew the Clockwork city at {} (seed {}, outskirts only: {}): {} ops in {} ms", origin, seed, outskirtsOnly, plan.ops().size(),
				(System.nanoTime() - start) / 1_000_000);
			return plan;
		} catch (RuntimeException e) {
			if (FAILED.add(key))
				LOGGER.error("Couldn't draw the Clockwork city at {} (seed {}); its chunks generate without it", origin, seed, e);
			return new CityPlan(origin, seed, outskirtsOnly);
		}
	}

	/** Server stopped: no plan or note is kept for the next world. */
	public static void forget() {
		PLANS.clear();
		KEPT_ORDER.clear();
		FAILED.clear();
		OLDER.clear();
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putLong("Origin", origin.asLong());
		tag.putLong("Seed", seed);
		tag.putBoolean("Outskirts", outskirtsOnly);
		tag.putInt("PlanVersion", version);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box,
							ChunkPos chunk, BlockPos pivot) {
		if (level instanceof ServerLevel live) {
			// /place structure: every chunk is loaded, so the whole city at once from the heart's chunk
			if (chunk.equals(new ChunkPos(origin)))
				placeLive(live, ground(generator));
			return;
		}
		// a start saved part-way through generation by an older build goes on with this build's plan (unreleased, D66)
		if (version != CityPlan.VERSION && OLDER.add(new Key(origin.asLong(), seed, outskirtsOnly)))
			LOGGER.warn("The Clockwork city at {} was started on an older build (plan {}, now {}); it goes on with this build's plan", origin, version,
				CityPlan.VERSION);
		CityPlan plan = plan(origin, seed, outskirtsOnly);
		new CityApplier.Gen(level, box, origin, seed, ground(generator)).apply(plan.opsIn(chunk));
		if (!outskirtsOnly && chunk.equals(new ChunkPos(origin)))
			CityRecord.addLater(level.getLevel().dimension(), origin);
	}

	/** The whole city into a live level, bound and recorded, as the dev command builds it, on the island's own ground. */
	private void placeLive(ServerLevel level, Block ground) {
		if (!outskirtsOnly)
			CityRecord.of(level).add(origin);
		new CityApplier.Live(level, origin, seed) {
			@Override
			protected int terrainTop(int x, int z) {
				return groundBelow(level, x, super.terrainTop(x, z), z, ground);
			}
		}.apply(plan(origin, seed, outskirtsOnly).ops());
	}

	/** The island's own block: the noise's default block (End Stone). */
	private static Block ground(ChunkGenerator generator) {
		return generator instanceof NoiseBasedChunkGenerator noise ? noise.generatorSettings().value().defaultBlock().getBlock() : Blocks.END_STONE;
	}
}
