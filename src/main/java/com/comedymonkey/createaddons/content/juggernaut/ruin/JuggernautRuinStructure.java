package com.comedymonkey.createaddons.content.juggernaut.ruin;

import java.util.Optional;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModStructures;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * The Juggernaut Ruins (D31, D55): a circle of ruin about five chunks across. At the edge, the odd fallen block; walls
 * and pillars getting bigger and closer together towards the middle; and in the middle the hall where a dormant
 * Clockwork Juggernaut kneels. The End's ({@code end}) is the same in End Stone and purpur, half-sunk, with chorus.
 */
public class JuggernautRuinStructure extends Structure {
	public static final MapCodec<JuggernautRuinStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(settingsCodec(i),
		Codec.BOOL.optionalFieldOf("end", false).forGetter(s -> s.end)).apply(i, JuggernautRuinStructure::new));
	/** How far out from the hall the ruin reaches, in blocks. */
	static final int RADIUS = 40;

	final boolean end;

	public JuggernautRuinStructure(StructureSettings settings, boolean end) {
		super(settings);
		this.end = end;
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		if (!CMConfig.Server.get(end ? CMConfig.SERVER.juggernautEndRuins : CMConfig.SERVER.juggernautRuins))
			return Optional.empty();
		ChunkPos chunk = context.chunkPos();
		int x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
		int surface = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(),
			context.randomState());
		int floor = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(),
			context.randomState());
		// on land (not under water), and in the End on an island, not over the void
		if (surface != floor || surface <= context.heightAccessor().getMinBuildHeight() + 2)
			return Optional.empty();
		// fairly level ground under the hall: not a cliff edge, a ravine or over water at any corner
		int low = surface, high = surface;
		for (int[] c : new int[][] { { -8, -8 }, { 8, -8 }, { -8, 8 }, { 8, 8 } }) {
			int h = context.chunkGenerator().getFirstOccupiedHeight(x + c[0], z + c[1], Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(),
				context.randomState());
			int g = context.chunkGenerator().getFirstOccupiedHeight(x + c[0], z + c[1], Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(),
				context.randomState());
			if (h != g)
				return Optional.empty();
			low = Math.min(low, h);
			high = Math.max(high, h);
		}
		if (high - low > 6)
			return Optional.empty();
		BlockPos centre = new BlockPos(x, surface, z);
		return Optional.of(new GenerationStub(centre, builder -> {
			Rotation rotation = Rotation.getRandom(context.random());
			builder.addPiece(new RuinHall(context.structureTemplateManager(), centre, rotation, end));
			builder.addPiece(new RuinField(centre, RADIUS, context.random().nextLong(), end));
		}));
	}

	@Override
	public StructureType<?> type() {
		return ModStructures.JUGGERNAUT_RUIN.get();
	}
}
