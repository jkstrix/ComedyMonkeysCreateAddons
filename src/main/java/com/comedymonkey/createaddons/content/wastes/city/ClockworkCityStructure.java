package com.comedymonkey.createaddons.content.wastes.city;

import java.util.Optional;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * A Clockwork Wastes city (design/specs/clockwork-wastes.md, D13, D21, P2, P3, P45-P47, P53, D66): placed by the structure
 * set {@code comedymonkeysaddons:clockwork_city} (random spread, spacing {@link CityLayout#SPACING} chunks (P45: 32, tuned for Elytra parity), separation 12, kept 6 chunks from
 * {@code minecraft:end_cities}), in the Clockwork Wastes ({@code #comedymonkeysaddons:has_structure/clockwork_city}) or,
 * as the P53 fallback when another mod hides the biome ({@code highlands}), in the vanilla End highlands.
 * <p>
 * <b>Off until the user approves the city (D66):</b> {@link CMConfig.Server#clockworkCities} ({@code cities} in the
 * server config's {@code clockworkWastes} section) is false by default, and while it is, this never places anything.
 * <p>
 * Island fit (P46): the heart needs End Stone under it, and at least {@code islandFit} (half) of its 9 x 9-chunk footprint
 * End Stone at the heart's height; short of that, the spot gets the outskirts only, with their own Elytra in the
 * resistance's cache (D69: so the Wastes keep Elytra parity at spacing 32). The rest stands on struts and bridges.
 */
public class ClockworkCityStructure extends Structure {
	public static final MapCodec<ClockworkCityStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(settingsCodec(i),
		Codec.BOOL.optionalFieldOf("highlands", false).forGetter(s -> s.highlands)).apply(i, ClockworkCityStructure::new));
	/** The city's footprint for island fit, in chunks each way from the heart's (9 x 9, D21). */
	public static final int FOOTPRINT = 4;

	final boolean highlands;

	public ClockworkCityStructure(StructureSettings settings, boolean highlands) {
		super(settings);
		this.highlands = highlands;
	}

	/** Where a city in this chunk would stand, and whether the island holds it (null: no ground under its heart). */
	public record Site(BlockPos origin, double fit, boolean fits) {}

	/** The site for a city whose heart is the middle of {@code chunk}, as world generation would judge it. */
	public static Optional<Site> site(ChunkGenerator generator, RandomState random, LevelHeightAccessor height, ChunkPos chunk, double share) {
		int x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
		int surface = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, height, random);
		// the heart needs End Stone under it (P3)
		if (surface <= height.getMinBuildHeight() + 2)
			return Optional.empty();
		double fit = islandFit(generator, random, height, x, z, surface);
		return Optional.of(new Site(new BlockPos(x, surface, z), fit, fit >= share));
	}

	/** The share of the 9 x 9-chunk footprint (one sample a chunk, at its middle) with End Stone at the heart's floor (P46). */
	public static double islandFit(ChunkGenerator generator, RandomState random, LevelHeightAccessor height, int x, int z, int surface) {
		int solid = 0, all = 0;
		for (int cx = -FOOTPRINT; cx <= FOOTPRINT; cx++)
			for (int cz = -FOOTPRINT; cz <= FOOTPRINT; cz++) {
				all++;
				var column = generator.getBaseColumn(x + cx * 16, z + cz * 16, height, random);
				if (!column.getBlock(surface - 1).isAir())
					solid++;
			}
		return solid / (double) all;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		if (!CMConfig.Server.get(CMConfig.SERVER.clockworkCities))
			return Optional.empty();
		if (highlands && !CMConfig.Server.get(CMConfig.SERVER.cityHighlandsFallback))
			return Optional.empty();
		double share = CMConfig.SERVER.cityIslandFit();
		Optional<Site> site = site(context.chunkGenerator(), context.randomState(), context.heightAccessor(), context.chunkPos(), share);
		if (site.isEmpty())
			return Optional.empty();
		Site s = site.get();
		long seed = context.seed() ^ context.chunkPos().toLong() * 0x2545F4914F6CDD1DL;
		return Optional.of(new GenerationStub(s.origin(), builder -> builder.addPiece(new ClockworkCityPiece(s.origin(), seed, !s.fits()))));
	}

	@Override
	public StructureType<?> type() {
		return ModStructures.CLOCKWORK_CITY.get();
	}
}
