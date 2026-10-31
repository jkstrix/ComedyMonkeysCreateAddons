package com.comedymonkey.createaddons.content.wastes.city;

import java.util.LinkedHashMap;
import java.util.Map;

import com.comedymonkey.createaddons.content.wastes.CityRecord;
import com.comedymonkey.createaddons.registry.ModStructures;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The whole of a Clockwork Wastes city as one structure piece: as each chunk it covers generates, that chunk's share of
 * the city's {@link CityPlan} is built into it ({@link CityApplier.Gen}). The plan is the same every time (it depends on
 * the origin and seed only), and the last few are kept, so a city's 150-odd chunks don't each draw it afresh. The chunk
 * holding the heart records the city ({@link CityRecord#addLater}).
 */
public class ClockworkCityPiece extends StructurePiece {
	private static final int DOWN = CityApplier.TRESTLE_REACH + 2, UP = CityLayout.CROWN_Y + 4;
	private static final Map<String, CityPlan> PLANS = new LinkedHashMap<>(8, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, CityPlan> eldest) {
			return size() > 4;
		}
	};

	private final BlockPos origin;
	private final long seed;
	private final boolean outskirtsOnly;

	public ClockworkCityPiece(BlockPos origin, long seed, boolean outskirtsOnly) {
		super(ModStructures.CLOCKWORK_CITY_PIECE.get(), 0, box(origin));
		this.origin = origin.immutable();
		this.seed = seed;
		this.outskirtsOnly = outskirtsOnly;
	}

	public ClockworkCityPiece(CompoundTag tag) {
		super(ModStructures.CLOCKWORK_CITY_PIECE.get(), tag);
		this.origin = BlockPos.of(tag.getLong("Origin"));
		this.seed = tag.getLong("Seed");
		this.outskirtsOnly = tag.getBoolean("Outskirts");
	}

	private static BoundingBox box(BlockPos o) {
		int r = CityLayout.OUT;
		return new BoundingBox(o.getX() - r, o.getY() - DOWN, o.getZ() - r, o.getX() + r, o.getY() + UP, o.getZ() + r);
	}

	/** The city's plan, from the cache or drawn afresh. */
	public static CityPlan plan(BlockPos origin, long seed, boolean outskirtsOnly) {
		String key = origin.asLong() + ":" + seed + ":" + outskirtsOnly;
		synchronized (PLANS) {
			CityPlan plan = PLANS.get(key);
			if (plan == null) {
				plan = CityPlan.of(origin, seed, outskirtsOnly);
				plan.opsIn(new ChunkPos(origin));
				PLANS.put(key, plan);
			}
			return plan;
		}
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putLong("Origin", origin.asLong());
		tag.putLong("Seed", seed);
		tag.putBoolean("Outskirts", outskirtsOnly);
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box,
							ChunkPos chunk, BlockPos pivot) {
		CityPlan plan = plan(origin, seed, outskirtsOnly);
		new CityApplier.Gen(level, box, origin).apply(plan.opsIn(chunk));
		if (!outskirtsOnly && chunk.equals(new ChunkPos(origin)))
			CityRecord.addLater(level.getLevel().dimension(), origin);
	}
}
