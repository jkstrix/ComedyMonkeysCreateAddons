package com.comedymonkey.createaddons.gametest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.wastes.city.CityApplier;
import com.comedymonkey.createaddons.content.wastes.city.CityEdge;
import com.comedymonkey.createaddons.content.wastes.city.CityLayout;
import com.comedymonkey.createaddons.content.wastes.city.CityPlan;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The city's edge (D91, {@link CityEdge}): the outskirts stand on the levelled terrace; the terrace's edge wobbles; built
 * a chunk at a time on rolling ground, the ground eases back over the band with no step over 2, scree lies where it was
 * cut and where it was filled and fans out below, and the chunks build the same whichever order they come in.
 */
@GameTestHolder(ComedyMonkeysCreateAddons.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WastesEdgeGameTests {
	private static final String COURSE = "trade_bot_course";

	private WastesEdgeGameTests() {}

	public static void register(RegisterGameTestsEvent event) {
		event.register(WastesEdgeGameTests.class);
	}

	private static void check(boolean ok, String message) {
		if (!ok)
			throw new GameTestAssertException(message);
	}

	/**
	 * Everything the outskirts put on the ground inside the old square footprint stands where the terrace is levelled (so
	 * no wreck, siege line or war-frame is lost to the blend), every ground column lies within {@link CityEdge#REACH}, and
	 * each says where it lies across the edge as the plan's edge does.
	 */
	@GameTest(template = COURSE, timeoutTicks = 100, batch = "wastes_edge")
	public static void theOutskirtsStandOnTheTerrace(GameTestHelper helper) {
		for (long seed : new long[] { 0, 1, 7, 12345 })
			for (boolean outskirtsOnly : new boolean[] { false, true }) {
				BlockPos o = new BlockPos(8, 70, 8);
				CityPlan plan = CityPlan.of(o, seed, outskirtsOnly);
				CityEdge edge = new CityEdge(plan);
				int ground = 0;
				for (CityPlan.Op op : plan.ops()) {
					int x = op.at().getX() - o.getX(), z = op.at().getZ() - o.getZ();
					if (op instanceof CityPlan.Op.Ground g) {
						ground++;
						check(Math.abs(x) <= CityEdge.REACH && Math.abs(z) <= CityEdge.REACH, "ground past the reach at " + x + ", " + z);
						check(g.core() ? g.across() == 0 : Math.abs(g.across() - edge.across(x, z)) < 1e-4, "ground at " + x + ", " + z + " is " + g.across());
					} else if (op instanceof CityPlan.Op.Put p && p.need() == CityPlan.ON_GROUND && Math.abs(x) <= CityLayout.OUT
						&& Math.abs(z) <= CityLayout.OUT)
						check(edge.across(x, z) <= 0, p.state() + " at " + x + ", " + z + " stands on the blend (seed " + seed + ")");
				}
				check(ground > 30_000, "only " + ground + " ground columns");
			}
		helper.succeed();
	}

	/** Rolling made-up ground, {@code d} above the floor: hills to cut and dips to fill, up to 8 either way. */
	private static int rolling(int dx, int dz) {
		return (int) Math.round(6 * Math.sin(dz / 11.0) + 2 * Math.cos(dx / 9.0));
	}

	private static final int BELOW = 12, ABOVE = 20;

	/** The made-up island in these chunks, nothing on it. */
	private static void island(ServerLevel level, BlockPos o, List<ChunkPos> chunks) {
		int floor = o.getY() - 1;
		for (ChunkPos c : chunks)
			for (int x = c.getMinBlockX(); x <= c.getMaxBlockX(); x++)
				for (int z = c.getMinBlockZ(); z <= c.getMaxBlockZ(); z++) {
					int top = floor + rolling(x - o.getX(), z - o.getZ());
					for (int y = floor - BELOW; y <= floor + ABOVE; y++)
						level.setBlock(new BlockPos(x, y, z), y <= top ? Blocks.END_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(),
							Block.UPDATE_CLIENTS);
				}
	}

	/** The plan's ground in these chunks, a chunk at a time as world generation builds it. */
	private static void build(ServerLevel level, BlockPos o, CityPlan plan, List<ChunkPos> chunks) {
		for (ChunkPos c : chunks) {
			BoundingBox box = new BoundingBox(c.getMinBlockX(), level.getMinBuildHeight(), c.getMinBlockZ(), c.getMaxBlockX(),
				level.getMaxBuildHeight() - 1, c.getMaxBlockZ());
			List<CityPlan.Op> ground = new ArrayList<>();
			for (CityPlan.Op op : plan.opsIn(c))
				if (op instanceof CityPlan.Op.Ground)
					ground.add(op);
			// the live level's own surface (a loaded chunk keeps no world-generation heightmaps)
			new CityApplier.Gen(level, box, o) {
				@Override
				protected int terrainTop(int x, int z) {
					int h = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
					return h <= level.getMinBuildHeight() ? Integer.MIN_VALUE : h;
				}
			}.apply(ground);
		}
	}

	private static boolean isGround(BlockState s) {
		return s.is(Blocks.END_STONE) || s.is(ModWastes.SCORCHED_END_STONE.get()) || s.is(ModWastes.RUSTED_SCORCHED_END_STONE.get())
			|| s.is(ModWastes.LITTERED_SCORCHED_END_STONE.get()) || s.is(ModWastes.CRACKED_STAINED_END_STONE_BRICKS.get()) || s.is(Blocks.COBBLED_DEEPSLATE);
	}

	/** Scree only the edge lays: stained brick rubble, the makers' rubble, a loose stone. */
	private static boolean isScree(BlockState s) {
		return s.is(ModWastes.CRACKED_STAINED_END_STONE_BRICKS.get()) || s.is(Blocks.COBBLED_DEEPSLATE) || s.getBlock() instanceof SlabBlock;
	}

	/** The column's ground (from the floor), loose stones aside. */
	private static int groundAt(ServerLevel level, int floor, int x, int z) {
		for (int y = floor + ABOVE; y >= floor - BELOW; y--)
			if (isGround(level.getBlockState(new BlockPos(x, y, z))))
				return y - floor;
		return Integer.MIN_VALUE;
	}

	/**
	 * Built a chunk at a time on rolling ground (hills and dips either side of the floor) across the terrace's east edge: the
	 * ground is the floor inside, eases back over the band with no step over 2 a block (no cliffs in it), is the island's
	 * own past it; scree lies on cut and filled ground in the band and fans out below the fill past it; built again in the
	 * other chunk order it comes out block for block the same.
	 */
	@GameTest(template = COURSE, timeoutTicks = 400, batch = "wastes_edge")
	public static void theEdgeEasesBackWithScree(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos o = new BlockPos(48_008 + level.random.nextInt(256) * 1024, 300, -48_008 - level.random.nextInt(256) * 1024);
		int floor = o.getY() - 1;
		CityPlan plan = CityPlan.of(o, 0, false);
		Map<Long, CityPlan.Op.Ground> ops = new HashMap<>();
		for (CityPlan.Op op : plan.ops())
			if (op instanceof CityPlan.Op.Ground g)
				ops.put(BlockPos.asLong(g.at().getX(), 0, g.at().getZ()), g);
		List<ChunkPos> chunks = new ArrayList<>();
		for (int cx = (o.getX() + 76) >> 4; cx <= (o.getX() + CityEdge.REACH) >> 4; cx++)
			for (int cz = (o.getZ() - 40) >> 4; cz <= (o.getZ() + 40) >> 4; cz++)
				chunks.add(new ChunkPos(cx, cz));
		for (ChunkPos c : chunks)
			level.setChunkForced(c.x, c.z, true);
		try {
			island(level, o, chunks);
			build(level, o, plan, chunks);
			int x0 = chunks.getFirst().getMinBlockX(), x1 = chunks.getLast().getMaxBlockX();
			int z0 = chunks.getFirst().getMinBlockZ(), z1 = chunks.getLast().getMaxBlockZ();
			int band = 0, cutScree = 0, fillScree = 0, fan = 0, eased = 0;
			int edgeLo = Integer.MAX_VALUE, edgeHi = Integer.MIN_VALUE;
			for (int z = z0; z <= z1; z++) {
				int lastFull = Integer.MIN_VALUE;
				for (int x = x0; x <= x1; x++) {
					CityPlan.Op.Ground g = ops.get(BlockPos.asLong(x, 0, z));
					double across = g == null ? 99 : g.across();
					int d = rolling(x - o.getX(), z - o.getZ()), here = groundAt(level, floor, x, z);
					if (across <= 0) {
						lastFull = x;
						check(here == 0, "levelled ground at " + (x - o.getX()) + ", " + (z - o.getZ()) + " is " + here + " off the floor");
						continue;
					}
					BlockState top = level.getBlockState(new BlockPos(x, floor + here, z)), above = level.getBlockState(new BlockPos(x, floor + here + 1, z));
					boolean scree = isScree(top) || isScree(above);
					if (across < 1) {
						band++;
						if (Math.abs(here) < Math.abs(d))
							eased++;
						if (scree && d > 0)
							cutScree++;
						if (scree && d < 0)
							fillScree++;
						// no cliffs in the band: each neighbour within 2
						for (int[] n : new int[][] { { x + 1, z }, { x, z + 1 }, { x - 1, z }, { x, z - 1 } })
							if (n[0] >= x0 && n[0] <= x1 && n[1] >= z0 && n[1] <= z1) {
								int step = Math.abs(groundAt(level, floor, n[0], n[1]) - here);
								check(step <= 2, "a " + step + "-block step in the band at " + (x - o.getX()) + ", " + (z - o.getZ()));
							}
					} else if (scree && d < 0 && here == d)
						fan++;
					else if (g == null)
						check(here == d, "the island's own ground was touched at " + (x - o.getX()) + ", " + (z - o.getZ()));
				}
				if (Math.abs(z - o.getZ()) <= 40) {
					edgeLo = Math.min(edgeLo, lastFull);
					edgeHi = Math.max(edgeHi, lastFull);
				}
			}
			check(band > 300, "only " + band + " columns in the band");
			check(eased > band / 2, "only " + eased + " of " + band + " band columns eased toward the floor");
			check(cutScree >= 5 && fillScree >= 5, "scree on " + cutScree + " cut and " + fillScree + " filled columns in the band");
			check(fan >= 3, "debris on only " + fan + " columns below the fill");
			check(edgeHi - edgeLo >= 2, "the terrace's edge runs straight (" + edgeLo + " to " + edgeHi + ")");
			// again in the other order: the same, block for block
			Map<BlockPos, BlockState> first = new HashMap<>();
			for (ChunkPos c : chunks)
				for (BlockPos p : BlockPos.betweenClosed(c.getMinBlockX(), floor - BELOW, c.getMinBlockZ(), c.getMaxBlockX(), floor + ABOVE, c.getMaxBlockZ()))
					first.put(p.immutable(), level.getBlockState(p));
			island(level, o, chunks);
			build(level, o, plan, chunks.reversed());
			int differ = 0;
			for (Map.Entry<BlockPos, BlockState> e : first.entrySet())
				if (level.getBlockState(e.getKey()) != e.getValue())
					differ++;
			check(differ == 0, differ + " blocks differ built in the other chunk order");
		} finally {
			for (ChunkPos c : chunks)
				level.setChunkForced(c.x, c.z, false);
		}
		helper.succeed();
	}
}
