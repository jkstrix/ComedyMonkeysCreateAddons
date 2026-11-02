package com.comedymonkey.createaddons.gametest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.wastes.CityRecord;
import com.comedymonkey.createaddons.content.wastes.city.CityApplier;
import com.comedymonkey.createaddons.content.wastes.city.CityEdge;
import com.comedymonkey.createaddons.content.wastes.city.CityLayout;
import com.comedymonkey.createaddons.content.wastes.city.CityPlan;
import com.comedymonkey.createaddons.content.wastes.city.ClockworkCityPiece;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The city as world generation builds it, a chunk at a time ({@link CityApplier.Gen} on a live level, its surface read
 * from the live heightmap): a bridge into a hill is cut through as the ground beside it; joiners on a chunk's edge wait
 * for its post-processing and come out the same whichever order the chunks come in; the ground is the island's End
 * Stone, not what stands on it; the overgrowth stays on ground near the floor; {@code /place} builds the whole city at
 * once. A real {@code WorldGenRegion} can't be set up in a game test, so post-processing is played here as
 * {@code LevelChunk.postProcessGeneration} plays it.
 */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WastesWorldgenGameTests {
	private static final String COURSE = "trade_bot_course";

	private WastesWorldgenGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(WastesWorldgenGameTests.class);
	}

	private static void check(boolean ok, String message) {
		if (!ok)
			throw new GameTestAssertException(message);
	}

	/** A city's origin far from everything, in a chunk's middle. */
	private static BlockPos far(ServerLevel level, int x, int y, int z) {
		return new BlockPos(x - level.random.nextInt(256) * 1024, y, z + level.random.nextInt(256) * 1024);
	}

	private static BoundingBox box(ServerLevel level, ChunkPos c) {
		return new BoundingBox(c.getMinBlockX(), level.getMinBuildHeight(), c.getMinBlockZ(), c.getMaxBlockX(), level.getMaxBuildHeight() - 1,
			c.getMaxBlockZ());
	}

	/**
	 * World generation's applier for chunk {@code c}, reading the live surface (a loaded chunk keeps no world-generation
	 * heightmaps); joiners it leaves for post-processing go in {@code deferred}.
	 */
	private static CityApplier.Gen gen(ServerLevel level, ChunkPos c, BlockPos o, @Nullable List<BlockPos> deferred) {
		return new CityApplier.Gen(level, box(level, c), o, 0, Blocks.END_STONE) {
			@Override
			protected int surfaceTop(int x, int z) {
				int h = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
				return h <= level.getMinBuildHeight() ? Integer.MIN_VALUE : h;
			}

			@Override
			protected boolean postProcessLater(BlockPos p) {
				if (deferred == null)
					return false;
				deferred.add(p.immutable());
				return true;
			}
		};
	}

	private static void fill(ServerLevel level, List<ChunkPos> chunks, int y0, int y1, BlockState s) {
		for (ChunkPos c : chunks)
			for (BlockPos p : BlockPos.betweenClosed(c.getMinBlockX(), y0, c.getMinBlockZ(), c.getMaxBlockX(), y1, c.getMaxBlockZ()))
				level.setBlock(p, s, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
	}

	private static void force(ServerLevel level, List<ChunkPos> chunks, boolean on) {
		for (ChunkPos c : chunks)
			level.setChunkForced(c.x, c.z, on);
	}

	/** The column's ground top: its highest block that isn't air or a loose stone. */
	private static int groundTop(ServerLevel level, int x, int z, int from, int to) {
		for (int y = from; y >= to; y--) {
			BlockState s = level.getBlockState(new BlockPos(x, y, z));
			if (!s.isAir() && !(s.getBlock() instanceof SlabBlock))
				return y;
		}
		return Integer.MIN_VALUE;
	}

	// ================================================================ a bridge into a hill

	/**
	 * A boulevard's bridge heading into a hill 20 above the floor, built a chunk at a time: it's cut down as the ground
	 * beside it is (no 5-wide ridge left standing), and no deck plate is buried in it.
	 */
	@GameTest(template = COURSE, timeoutTicks = 400, batch = "wastes_worldgen")
	public static void aBridgeIntoAHillIsCutThrough(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos o = far(level, -80_008, 290, 80_008);
		int floor = o.getY() - 1, hill = 20;
		CityPlan plan = CityPlan.of(o, 3, false);
		List<ChunkPos> chunks = new ArrayList<>();
		// the east boulevard past the core (|x| > 70) out to the bridge's end and a little more
		for (int cx = (o.getX() + CityLayout.CORE + 2) >> 4; cx <= (o.getX() + CityLayout.OUT + 4) >> 4; cx++)
			for (int cz = (o.getZ() - 8) >> 4; cz <= (o.getZ() + 8) >> 4; cz++)
				chunks.add(new ChunkPos(cx, cz));
		force(level, chunks, true);
		try {
			fill(level, chunks, floor - 4, floor + hill, Blocks.END_STONE.defaultBlockState());
			fill(level, chunks, floor + hill + 1, floor + hill + 4, Blocks.AIR.defaultBlockState());
			for (ChunkPos c : chunks) {
				List<CityPlan.Op> ground = new ArrayList<>();
				for (CityPlan.Op op : plan.opsIn(c))
					if (op instanceof CityPlan.Op.Ground)
						ground.add(op);
				gen(level, c, o, null).apply(ground);
			}
			int expect = floor + (int) Math.round(CityEdge.inner(hill)), bridges = 0;
			for (int x = o.getX() + CityLayout.CORE + 2; x <= o.getX() + CityLayout.OUT; x++)
				for (int z = o.getZ() - 6; z <= o.getZ() + 6; z++) {
					int top = groundTop(level, x, z, floor + hill + 4, floor - 4);
					check(top == expect, "the ground at " + (x - o.getX()) + ", " + (z - o.getZ()) + " is " + (top - floor) + " above the floor, not "
						+ (expect - floor));
					BlockState atFloor = level.getBlockState(new BlockPos(x, floor, z));
					check(!atFloor.is(ModWastes.VOID_ALLOY_PLATE.get()), "a deck plate buried in the hill at " + (x - o.getX()) + ", " + (z - o.getZ()));
					if (Math.abs(z - o.getZ()) <= 2)
						bridges++;
				}
			check(bridges > 50, "only " + bridges + " bridge columns checked");
		} finally {
			force(level, chunks, false);
		}
		helper.succeed();
	}

	// ================================================================ joins at chunk seams

	/** Every joiner the test's little plan puts: bars, panes, a wall, stairs, a girder and a band across chunk seams. */
	private static CityPlan seamPlan(BlockPos o) {
		BlockState girder = CityPlan.with(CityPlan.block("create:metal_girder", Blocks.IRON_BARS), "axis", "x");
		BlockState band = CityPlan.with(ModWastes.VOID_ALLOY_BAND.get().defaultBlockState(), "axis", "x");
		return CityPlan.drawn(o, 0, p -> {
			// along x across the seam between x 7 and 8 (the origin is the first chunk's middle)
			for (int x = 2; x <= 13; x++) {
				p.put(x, 1, 0, Blocks.IRON_BARS.defaultBlockState());
				p.put(x, 2, 0, Blocks.GLASS_PANE.defaultBlockState());
				p.put(x, 3, 0, Blocks.COBBLESTONE_WALL.defaultBlockState());
				p.put(x, 5, 0, girder);
				p.put(x, 6, 0, band);
			}
			// along z across the seam between z 7 and 8
			for (int z = 2; z <= 13; z++)
				p.put(0, 1, z, Blocks.IRON_BARS.defaultBlockState());
			// a stair's corner made by the stair across the seam
			p.put(7, 4, 0, Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
			p.put(8, 4, 0, Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
		});
	}

	private static Map<BlockPos, BlockState> snapshot(ServerLevel level, BlockPos o) {
		Map<BlockPos, BlockState> out = new HashMap<>();
		for (BlockPos p : BlockPos.betweenClosed(o.offset(-1, 0, -1), o.offset(15, 7, 15)))
			out.put(p.immutable(), level.getBlockState(p));
		return out;
	}

	/** The chunks in {@code order}, then their post-processing as a chunk's is when every chunk round it is done. */
	private static Map<BlockPos, BlockState> buildSeams(ServerLevel level, BlockPos o, CityPlan plan, List<ChunkPos> order, List<ChunkPos> chunks,
														List<BlockPos> deferred) {
		fill(level, chunks, o.getY(), o.getY() + 8, Blocks.AIR.defaultBlockState());
		for (ChunkPos c : order)
			gen(level, c, o, deferred).apply(plan.opsIn(c));
		for (BlockPos p : deferred) {
			BlockState s = level.getBlockState(p);
			level.setBlock(p, Block.updateFromNeighbourShapes(s, level, p), Block.UPDATE_INVISIBLE | Block.UPDATE_KNOWN_SHAPE);
		}
		return snapshot(level, o);
	}

	/**
	 * Bars, panes, a wall, stairs, a girder and a band across chunk seams, built a chunk at a time in two orders: those on
	 * a chunk's edge are left for post-processing (only those), and after it they're block for block as the whole plan
	 * built at once joins them, whichever order the chunks came in.
	 */
	@GameTest(template = COURSE, timeoutTicks = 200, batch = "wastes_worldgen")
	public static void chunkSeamsJoinWhicheverOrder(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos o = far(level, -96_008, 200, 96_008);
		ChunkPos a = new ChunkPos(o);
		List<ChunkPos> chunks = List.of(a, new ChunkPos(a.x + 1, a.z), new ChunkPos(a.x, a.z + 1), new ChunkPos(a.x + 1, a.z + 1));
		CityPlan plan = seamPlan(o);
		force(level, chunks, true);
		try {
			List<BlockPos> deferred = new ArrayList<>();
			Map<BlockPos, BlockState> forward = buildSeams(level, o, plan, chunks, chunks, deferred);
			check(!deferred.isEmpty(), "nothing on a chunk's edge was left for post-processing");
			for (BlockPos p : deferred) {
				int x = p.getX() & 15, z = p.getZ() & 15;
				check(x == 0 || x == 15 || z == 0 || z == 15, "a joiner inside its chunk was left for post-processing: " + p.subtract(o));
			}
			Map<BlockPos, BlockState> back = buildSeams(level, o, plan, chunks.reversed(), chunks, new ArrayList<>());
			fill(level, chunks, o.getY(), o.getY() + 8, Blocks.AIR.defaultBlockState());
			new CityApplier.Live(level, o).apply(plan.ops());
			Map<BlockPos, BlockState> whole = snapshot(level, o);
			int differ = 0, differWhole = 0;
			for (Map.Entry<BlockPos, BlockState> e : forward.entrySet()) {
				if (back.get(e.getKey()) != e.getValue())
					differ++;
				if (whole.get(e.getKey()) != e.getValue())
					differWhole++;
			}
			check(differ == 0, differ + " blocks differ built in the other chunk order");
			check(differWhole == 0, differWhole + " blocks differ from the city built whole");
			// and they did join across the seams
			BlockState west = forward.get(o.offset(7, 1, 0)), east = forward.get(o.offset(8, 1, 0)), north = forward.get(o.offset(0, 1, 7));
			check(west.getValue(CrossCollisionBlock.EAST) && east.getValue(CrossCollisionBlock.WEST) && north.getValue(CrossCollisionBlock.SOUTH),
				"the bars don't join across the seams: " + west + ", " + east + ", " + north);
			check(forward.get(o.offset(7, 4, 0)).getValue(StairBlock.SHAPE) != StairsShape.STRAIGHT, "the stair at the seam didn't turn its corner");
		} finally {
			force(level, chunks, false);
		}
		helper.succeed();
	}

	// ================================================================ the island's ground, not what stands on it

	/**
	 * The ground is the island's End Stone, not chorus or an End city's purpur standing on it: an outskirts column over a
	 * lower island with purpur reaching past the floor gets no scorched floor (it's over a drop), and a core column whose
	 * dip has chorus in it is filled up to the floor.
	 */
	@GameTest(template = COURSE, timeoutTicks = 100, batch = "wastes_worldgen")
	public static void theGroundIsTheIslandNotWhatStandsOnIt(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos o = far(level, -112_008, 200, 112_008);
		int floor = o.getY() - 1;
		ChunkPos c = new ChunkPos(o);
		List<ChunkPos> chunks = List.of(c);
		force(level, chunks, true);
		try {
			fill(level, chunks, floor - 40, floor + 10, Blocks.AIR.defaultBlockState());
			BlockPos spill = o.offset(2, 0, 0), dip = o.offset(-2, 0, 0);
			// a lower island 30 under the floor, an End city's purpur on it up past the floor
			level.setBlock(spill.atY(floor - 30), Blocks.END_STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
			for (int y = floor - 29; y <= floor + 2; y++)
				level.setBlock(spill.atY(y), Blocks.PURPUR_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
			// a dip 3 under the floor, chorus growing out of it
			for (int y = floor - 8; y <= floor - 3; y++)
				level.setBlock(dip.atY(y), Blocks.END_STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
			for (int y = floor - 2; y <= floor + 3; y++)
				level.setBlock(dip.atY(y), Blocks.CHORUS_PLANT.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
			check(CityApplier.groundBelow(level, spill.getX(), floor + 2, spill.getZ(), Blocks.END_STONE) == floor - 30, "the spill's ground isn't the island under it");
			check(CityApplier.groundBelow(level, dip.getX(), floor + 3, dip.getZ(), Blocks.END_STONE) == floor - 3, "the chorus counts as ground");
			gen(level, c, o, null).apply(List.of(new CityPlan.Op.Ground(spill, false, false, false, -1, 8),
				new CityPlan.Op.Ground(dip, true, false, true, 0, 0)));
			check(level.getBlockState(spill.atY(floor)).is(Blocks.PURPUR_BLOCK), "the purpur was taken for ground: " + level.getBlockState(spill.atY(floor)));
			check(level.getBlockState(dip.atY(floor - 1)).is(Blocks.END_STONE) && level.getBlockState(dip.atY(floor - 2)).is(Blocks.END_STONE),
				"the dip under the chorus wasn't filled");
		} finally {
			force(level, chunks, false);
		}
		helper.succeed();
	}

	// ================================================================ the overgrowth's ground

	/**
	 * The overgrowth grows on ground eased back toward the city, never on a lower island far under the floor or on top
	 * of ground far above it, both inside the reach.
	 */
	@GameTest(template = COURSE, timeoutTicks = 100, batch = "wastes_worldgen")
	public static void theOvergrowthStaysNearTheFloor(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos o = far(level, -128_008, 200, 128_008);
		int floor = o.getY() - 1;
		ChunkPos c = new ChunkPos(o);
		List<ChunkPos> chunks = List.of(c);
		BlockState plant = Blocks.DEAD_BUSH.defaultBlockState();
		force(level, chunks, true);
		try {
			fill(level, chunks, floor - 48, floor + 44, Blocks.AIR.defaultBlockState());
			List<CityPlan.Op> ops = new ArrayList<>();
			List<BlockPos> low = new ArrayList<>(), high = new ArrayList<>(), near = new ArrayList<>();
			for (int i = 0; i < 6; i++) {
				low.add(o.offset(-7 + 2 * i, 0, -6));
				high.add(o.offset(-7 + 2 * i, 0, 0));
				near.add(o.offset(-7 + 2 * i, 0, 6));
			}
			for (BlockPos p : low)
				fillColumn(level, p, floor - 44, floor - 40);
			for (BlockPos p : high)
				fillColumn(level, p, floor - 4, floor + 40);
			for (BlockPos p : near)
				fillColumn(level, p, floor - 14, floor - 10);
			for (List<BlockPos> set : List.of(low, high, near))
				for (BlockPos p : set)
					ops.add(new CityPlan.Op.Ground(p, false, false, false, -1, 8));
			for (List<BlockPos> set : List.of(low, high, near))
				for (BlockPos p : set)
					ops.add(new CityPlan.Op.Surface(p, plant, false));
			gen(level, c, o, null).apply(ops);
			int grown = 0;
			for (BlockPos p : low)
				check(!level.getBlockState(p.atY(floor - 39)).is(Blocks.DEAD_BUSH), "a plant on the lower island at " + p.subtract(o));
			for (BlockPos p : high)
				check(!level.getBlockState(p.atY(floor + 41)).is(Blocks.DEAD_BUSH), "a plant on the ground high above at " + p.subtract(o));
			for (BlockPos p : near)
				for (int y = floor - 12; y <= floor; y++)
					if (level.getBlockState(p.atY(y)).is(Blocks.DEAD_BUSH))
						grown++;
			check(grown > 0, "no plant on the ground eased back toward the floor");
		} finally {
			force(level, chunks, false);
		}
		helper.succeed();
	}

	private static void fillColumn(ServerLevel level, BlockPos p, int y0, int y1) {
		for (int y = y0; y <= y1; y++)
			level.setBlock(p.atY(y), Blocks.END_STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
	}

	// ================================================================ /place structure

	/**
	 * {@code /place structure} on a live level: the piece builds the whole site from its heart's chunk at once (as the dev
	 * command does) and nothing from its other chunks; an outskirts-only site's cache stands on the one column with ground,
	 * with its loot table, and no record is made for it.
	 */
	@GameTest(template = COURSE, timeoutTicks = 200, batch = "wastes_worldgen")
	public static void placeBuildsTheWholeSiteAtOnce(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos o = far(level, -144_008, 290, 144_008);
		List<ChunkPos> chunks = new ArrayList<>();
		ChunkPos heart = new ChunkPos(o);
		int r = (CityEdge.REACH >> 4) + 1;
		for (int dx = -r; dx <= r; dx++)
			for (int dz = -r; dz <= r; dz++)
				chunks.add(new ChunkPos(heart.x + dx, heart.z + dz));
		force(level, chunks, true);
		try {
			level.setBlock(o.below(), Blocks.END_STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
			ClockworkCityPiece piece = new ClockworkCityPiece(o, 7, true);
			ChunkPos other = new ChunkPos(heart.x + 1, heart.z);
			piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random, box(level, other), other, o);
			check(level.getBlockState(o).isAir(), "a chunk other than the heart's built the site");
			piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random, box(level, heart), heart, o);
			check(level.getBlockState(o).is(Blocks.BARREL), "no cache at the site's middle: " + level.getBlockState(o));
			check(level.getBlockEntity(o) instanceof RandomizableContainerBlockEntity be && CityLayout.RESISTANCE_CACHE_LOOT.equals(be.getLootTable()),
				"the cache has no loot table");
			check(!CityRecord.of(level).cities().contains(o), "an outskirts-only site has a city record");
		} finally {
			force(level, chunks, false);
		}
		helper.succeed();
	}
}
