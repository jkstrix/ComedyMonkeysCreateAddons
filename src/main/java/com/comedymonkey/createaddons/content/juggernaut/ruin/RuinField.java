package com.comedymonkey.createaddons.content.juggernaut.ruin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.comedymonkey.createaddons.registry.ModStructures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The ruin round the hall (D31, the user's "lots of little broken structures that get larger as you get into the
 * middle"): a circle about five chunks across. Out at the edge, the odd fallen block or a slab; further in, stubs of wall;
 * then broken walls and pillar stumps with vines on them; and nearest the hall, tall ragged corners and heaps of rubble,
 * with bits of brass and shafting among them. Old paving shows through the ground more and more towards the middle.
 * Every block sits on the ground where it lands, so the ruin follows the land. All of it comes from the structure's seed,
 * so every chunk agrees on what goes where.
 */
public class RuinField extends StructurePiece {
	/** The hall's own square, which the field leaves alone (half its width, plus a block). */
	private static final int HALL = 8;
	private static final int ELEMENTS = 110;

	private final BlockPos centre;
	private final int radius;
	private final long seed;
	private final boolean end;
	/** Worked out once, before any chunk is placed: several worldgen threads place the same piece at once. */
	private final List<Column> columns;

	/** A column of ruin: blocks of {@code kind} from {@code bottom} to below {@code top}, counted from the ground up. */
	private record Column(int x, int z, int bottom, int top, Kind kind, Direction vine) {}

	private enum Kind { WALL, PILLAR, RUBBLE, SLAB, BRASS, CASING, SHAFT, CHORUS }

	public RuinField(BlockPos centre, int radius, long seed, boolean end) {
		super(ModStructures.RUIN_FIELD.get(), 0, new BoundingBox(centre.getX() - radius, centre.getY() - 32, centre.getZ() - radius,
			centre.getX() + radius, centre.getY() + 32, centre.getZ() + radius));
		this.centre = centre;
		this.radius = radius;
		this.seed = seed;
		this.end = end;
		this.columns = layOut();
	}

	public RuinField(CompoundTag tag) {
		super(ModStructures.RUIN_FIELD.get(), tag);
		this.centre = new BlockPos(tag.getInt("CX"), tag.getInt("CY"), tag.getInt("CZ"));
		this.radius = tag.getInt("R");
		this.seed = tag.getLong("Seed");
		this.end = tag.getBoolean("End");
		this.columns = layOut();
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putInt("CX", centre.getX());
		tag.putInt("CY", centre.getY());
		tag.putInt("CZ", centre.getZ());
		tag.putInt("R", radius);
		tag.putLong("Seed", seed);
		tag.putBoolean("End", end);
	}

	// ---- what goes where (from the seed)

	private List<Column> layOut() {
		List<Column> out = new ArrayList<>();
		Random r = new Random(seed);
		for (int i = 0; i < ELEMENTS; i++) {
			// spread evenly out from the hall to the edge: as the rings get bigger, the same number spread thinner
			double t = 0.24 + 0.76 * r.nextDouble(), a = r.nextDouble() * Math.PI * 2;
			int x = (int) Math.round(Math.cos(a) * t * radius), z = (int) Math.round(Math.sin(a) * t * radius);
			Direction along = r.nextBoolean() ? Direction.EAST : Direction.SOUTH;
			if (t > 0.8) {
				// the odd fallen block, or a slab, or two lying together
				Kind kind = r.nextInt(4) == 0 ? Kind.SLAB : Kind.RUBBLE;
				out.add(new Column(x, z, 0, 1, kind, null));
				if (r.nextInt(3) == 0)
					out.add(new Column(x + along.getStepX(), z + along.getStepZ(), 0, 1, Kind.RUBBLE, null));
			} else if (t > 0.6) {
				// a stub of wall
				wall(out, r, x, z, along, 2 + r.nextInt(2), 1 + r.nextInt(2));
			} else if (t > 0.42) {
				// a broken wall, or a pillar stump
				if (r.nextInt(3) == 0)
					out.add(new Column(x, z, 0, 3 + r.nextInt(3), Kind.PILLAR, null));
				else
					wall(out, r, x, z, along, 3 + r.nextInt(3), 2 + r.nextInt(3));
			} else {
				// nearest the hall: tall ragged corners and heaps of rubble
				int h = 3 + r.nextInt(4);
				wall(out, r, x, z, along, 4 + r.nextInt(3), h);
				Direction across = along == Direction.EAST ? Direction.SOUTH : Direction.EAST;
				if (r.nextBoolean())
					across = across.getOpposite();
				wall(out, r, x, z, across, 3 + r.nextInt(3), h - 1);
				out.add(new Column(x, z, 0, h + 1, Kind.PILLAR, null));
				heap(out, r, x + across.getStepX() * 2 + along.getStepX() * 2, z + across.getStepZ() * 2 + along.getStepZ() * 2);
			}
			// the clockwork that was, here and there among the inner ruins
			if (t < 0.55 && r.nextInt(5) == 0) {
				int dx = r.nextInt(5) - 2, dz = r.nextInt(5) - 2;
				Kind bit = switch (r.nextInt(3)) {
					case 0 -> Kind.BRASS;
					case 1 -> Kind.CASING;
					default -> Kind.SHAFT;
				};
				out.add(new Column(x + dx, z + dz, bit == Kind.SHAFT ? 0 : -1, bit == Kind.SHAFT ? 1 : 0, bit, null));
			}
			// in the End, chorus where the Overworld has vines
			if (end && t < 0.8 && r.nextInt(3) == 0)
				out.add(new Column(x + r.nextInt(3) - 1, z + r.nextInt(3) - 1, 0, 2 + r.nextInt(3), Kind.CHORUS, null));
		}
		return List.copyOf(out);
	}

	/** A run of wall {@code length} long, about {@code height} high with a ragged top, and vines down one face. */
	private void wall(List<Column> out, Random r, int x, int z, Direction along, int length, int height) {
		Direction face = r.nextBoolean() ? along.getClockWise() : along.getCounterClockWise();
		for (int k = 0; k < length; k++) {
			int top = Math.max(1, height - r.nextInt(3));
			// broken through: now and then a gap in the run
			if (k > 0 && k < length - 1 && r.nextInt(6) == 0)
				continue;
			boolean vine = !end && top >= 3 && r.nextInt(3) == 0;
			out.add(new Column(x + along.getStepX() * k, z + along.getStepZ() * k, 0, top, Kind.WALL, vine ? face : null));
		}
	}

	private void heap(List<Column> out, Random r, int x, int z) {
		for (int dx = -1; dx <= 1; dx++)
			for (int dz = -1; dz <= 1; dz++)
				if (r.nextInt(3) > 0)
					out.add(new Column(x + dx, z + dz, 0, dx == 0 && dz == 0 ? 2 : 1, Kind.RUBBLE, null));
	}

	// ---- placing it, a chunk at a time

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
							BoundingBox chunkBox, ChunkPos chunk, BlockPos pivot) {
		// the ground in this chunk, read once up front
		int[] ground = new int[256];
		for (int i = 0; i < 256; i++) {
			int x = chunk.getMinBlockX() + (i & 15), z = chunk.getMinBlockZ() + (i >> 4);
			ground[i] = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
			// placed into a finished world (/place), the worldgen heightmap is gone: the live one serves
			if (ground[i] <= level.getMinBuildHeight())
				ground[i] = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
		}
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		// old paving through the ground, thicker towards the middle
		for (int i = 0; i < 256; i++) {
			int x = chunk.getMinBlockX() + (i & 15), z = chunk.getMinBlockZ() + (i >> 4);
			double t = distance(x, z);
			if (t > 1 || inHall(x, z) || !groundOk(level, p, x, ground[i], z))
				continue;
			if (chance(x, z, 1) < 0.5 * (1 - t) * (1 - t))
				level.setBlock(p.set(x, ground[i] - 1, z), paving(x, z), 2);
			else if (!end && chance(x, z, 2) < 0.06 * (1 - t) && level.getBlockState(p.set(x, ground[i], z)).isAir())
				level.setBlock(p, Blocks.MOSS_CARPET.defaultBlockState(), 2);
		}
		for (Column c : columns) {
			int x = centre.getX() + c.x, z = centre.getZ() + c.z;
			if (!inChunk(chunk, x, z) || inHall(x, z) || distance(x, z) > 1)
				continue;
			int g = ground[(x - chunk.getMinBlockX()) + ((z - chunk.getMinBlockZ()) << 4)];
			if (!groundOk(level, p, x, g, z))
				continue;
			if (c.kind == Kind.CHORUS)
				level.setBlock(p.set(x, g - 1, z), Blocks.END_STONE.defaultBlockState(), 2);
			for (int y = c.bottom; y < c.top; y++)
				level.setBlock(p.set(x, g + y, z), block(c, x, g + y, z, y == c.top - 1), 2);
			// vines down the wall's face, from just under its top
			if (c.vine != null) {
				int vx = x + c.vine.getStepX(), vz = z + c.vine.getStepZ();
				if (inChunk(chunk, vx, vz))
					for (int y = c.top - 2; y >= Math.max(1, c.top - 4); y--) {
						if (!level.getBlockState(p.set(vx, g + y, vz)).isAir())
							break;
						level.setBlock(p, Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(c.vine.getOpposite()), true), 2);
					}
			}
		}
	}

	private BlockState block(Column c, int x, int y, int z, boolean top) {
		double n = chance(x, z, 3 + y);
		return switch (c.kind) {
			case WALL -> brick(n);
			case PILLAR -> (end ? Blocks.PURPUR_PILLAR : Blocks.POLISHED_DEEPSLATE).defaultBlockState()
				.trySetValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
			case RUBBLE -> end ? (n < 0.5 ? Blocks.END_STONE : Blocks.END_STONE_BRICKS).defaultBlockState()
				: (n < 0.45 ? Blocks.COBBLED_DEEPSLATE : n < 0.7 ? Blocks.CRACKED_DEEPSLATE_BRICKS : n < 0.85 ? Blocks.GRAVEL
					: Blocks.MOSSY_COBBLESTONE).defaultBlockState();
			case SLAB -> (end ? Blocks.END_STONE_BRICK_SLAB : Blocks.DEEPSLATE_TILE_SLAB).defaultBlockState();
			case BRASS -> create("brass_block").defaultBlockState();
			case CASING -> create("andesite_casing").defaultBlockState();
			case SHAFT -> create("shaft").defaultBlockState().trySetValue(RotatedPillarBlock.AXIS, n < 0.5 ? Direction.Axis.X : Direction.Axis.Z);
			case CHORUS -> top ? Blocks.CHORUS_FLOWER.defaultBlockState().setValue(ChorusFlowerBlock.AGE, ChorusFlowerBlock.DEAD_AGE)
				: Blocks.CHORUS_PLANT.defaultBlockState().setValue(BlockStateProperties.DOWN, true).setValue(BlockStateProperties.UP, true);
		};
	}

	private BlockState brick(double n) {
		if (end)
			return (n < 0.7 ? Blocks.END_STONE_BRICKS : Blocks.END_STONE).defaultBlockState();
		return (n < 0.45 ? Blocks.DEEPSLATE_BRICKS : n < 0.7 ? Blocks.CRACKED_DEEPSLATE_BRICKS : n < 0.85 ? Blocks.DEEPSLATE_TILES
			: Blocks.COBBLED_DEEPSLATE).defaultBlockState();
	}

	private BlockState paving(int x, int z) {
		double n = chance(x, z, 7);
		if (end)
			return (n < 0.6 ? Blocks.END_STONE_BRICKS : Blocks.END_STONE).defaultBlockState();
		return (n < 0.4 ? Blocks.DEEPSLATE_TILES : n < 0.65 ? Blocks.CRACKED_DEEPSLATE_TILES : n < 0.85 ? Blocks.COBBLED_DEEPSLATE
			: Blocks.MOSS_BLOCK).defaultBlockState();
	}

	private static Block create(String id) {
		return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", id));
	}

	/** Solid, dry ground to stand on (not water, not a tree, not the End's void). */
	private boolean groundOk(WorldGenLevel level, BlockPos.MutableBlockPos p, int x, int g, int z) {
		if (g <= level.getMinBuildHeight() + 1)
			return false;
		BlockState below = level.getBlockState(p.set(x, g - 1, z));
		return below.getFluidState().isEmpty() && below.isSolid() && !below.is(BlockTags.LEAVES) && !below.is(BlockTags.LOGS);
	}

	/** 0 at the hall, 1 at the ruin's edge. */
	private double distance(int x, int z) {
		double dx = x - centre.getX(), dz = z - centre.getZ();
		return Math.sqrt(dx * dx + dz * dz) / radius;
	}

	private boolean inHall(int x, int z) {
		return Math.abs(x - centre.getX()) <= HALL && Math.abs(z - centre.getZ()) <= HALL;
	}

	private static boolean inChunk(ChunkPos chunk, int x, int z) {
		return x >= chunk.getMinBlockX() && x <= chunk.getMaxBlockX() && z >= chunk.getMinBlockZ() && z <= chunk.getMaxBlockZ();
	}

	/** A number 0..1 fixed for this ruin, column and purpose. */
	private double chance(int x, int z, int salt) {
		long h = seed ^ (x * 341873128712L) ^ (z * 132897987541L) ^ (salt * 0x9E3779B97F4A7C15L);
		h = (h ^ (h >>> 33)) * 0xff51afd7ed558ccdL;
		h = (h ^ (h >>> 33)) * 0xc4ceb9fe1a85ec53L;
		return ((h ^ (h >>> 33)) >>> 11) / (double) (1L << 53);
	}
}
