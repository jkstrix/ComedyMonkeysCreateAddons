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
import com.comedymonkey.createaddons.content.wastes.city.CityRoads;
import com.comedymonkey.createaddons.content.wastes.city.kit.Wear;
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

	/**
	 * The roads' wear (CityRoads): tidy at the heart (no potholes there), more worn through the worn ring and more again
	 * through the damaged one, more near what broke them than away from it; nothing stands over a pothole or a sunk sett,
	 * and a pothole only goes in on land; most of every ring's roads still paving, so the streets still read.
	 */
	@GameTest(template = COURSE, timeoutTicks = 200, batch = "wastes_edge")
	public static void theRoadsWearByTheirRings(GameTestHelper helper) {
		double[] worn = new double[3], all = new double[3], paving = new double[3];
		double near = 0, nearWorn = 0, far = 0, farWorn = 0;
		int verges = 0, endStoneVerges = 0;
		for (long seed : new long[] { 0, 1, 12345 })
			for (BlockPos o : new BlockPos[] { new BlockPos(1000, 70, -2000), new BlockPos(2584, 64, 1136) }) {
				CityPlan plan = CityPlan.of(o, seed, false);
				Map<BlockPos, CityPlan.Op.Put> last = new HashMap<>();
				java.util.Set<Long> causes = new java.util.HashSet<>();
				// the city as the wear left it (the overgrowth comes after, its vines hanging on the walls beside)
				for (CityPlan.Op op : plan.ops().subList(0, Math.min(plan.ops().size(), plan.overgrowthFrom)))
					if (op instanceof CityPlan.Op.Put p) {
						int y = p.at().getY() - o.getY();
						if (y >= -2 && y <= 0)
							last.put(p.at(), p);
						if (y >= -1 && y <= 6 && CityRoads.cause(p.state()) > 0)
							causes.add(BlockPos.asLong(p.at().getX(), 0, p.at().getZ()));
					}
				String at = " (seed " + seed + " at " + o.toShortString() + ")";
				for (CityRoads.Paved c : plan.roads.paved()) {
					int dx = c.at().getX() - o.getX(), dz = c.at().getZ() - o.getZ(), ring = Wear.ring(dx, dz);
					BlockState f = last.get(c.at()).state();
					CityPlan.Op.Put upOp = last.get(c.at().above());
					if (c.kind() == CityRoads.Kind.VERGE) {
						verges++;
						if (f.is(Blocks.END_STONE))
							endStoneVerges++;
						continue;
					}
					// (a cell the city built over since isn't road any more)
					if (!CityRoads.isPaving(f) && !f.isAir() && !scorched(f) && !f.is(Blocks.GRAVEL))
						continue;
					if (f.isAir() || f.getBlock() instanceof SlabBlock)
						check(upOp == null, upOp == null ? "" : upOp.state() + " stands over a broken sett at " + dx + ", " + dz + at);
					if (f.isAir())
						check(last.get(c.at()).need() == CityPlan.ON_LAND && ring > Wear.INTACT_TO, "a pothole at " + dx + ", " + dz + at);
					boolean broken = f != c.state() || upOp != null && rubble(upOp.state());
					int band = ring <= Wear.INTACT_TO ? 0 : ring <= Wear.WORN_TO ? 1 : 2;
					all[band]++;
					if (broken)
						worn[band]++;
					if (CityRoads.isPaving(f))
						paving[band]++;
					if (band == 2) {
						int d = nearest(causes, c.at(), 8);
						if (d <= 3) {
							near++;
							if (broken)
								nearWorn++;
						} else if (d > 8) {
							far++;
							if (broken)
								farWorn++;
						}
					}
				}
			}
		String share = " (heart " + worn[0] / all[0] + ", worn " + worn[1] / all[1] + ", damaged " + worn[2] / all[2] + ")";
		com.mojang.logging.LogUtils.getLogger().info("The roads' wear{}; still paving {}, {}, {}; near a cause {} worn, away {}; End Stone verges {} of {}", share,
			paving[0] / all[0], paving[1] / all[1], paving[2] / all[2], nearWorn / near, farWorn / far, endStoneVerges, verges);
		check(worn[0] / all[0] < 0.12, "the heart's plaza is too worn" + share);
		check(worn[0] / all[0] < worn[1] / all[1] && worn[1] / all[1] < worn[2] / all[2], "the roads' wear doesn't follow the rings" + share);
		check(worn[2] / all[2] > 0.35, "the damaged ring's roads are hardly worn" + share);
		check(near > 50 && far > 50 && nearWorn / near > farWorn / far + 0.1, "the wear doesn't follow its causes: " + nearWorn + " of " + near
			+ " near one worn, " + farWorn + " of " + far + " away from any");
		for (int b = 0; b < 3; b++)
			check(paving[b] > 0.8 * all[b], "only " + paving[b] + " of " + all[b] + " road cells in ring " + b + " are still paving");
		check(endStoneVerges > 0 && endStoneVerges < verges / 3, endStoneVerges + " of " + verges + " road verges are End Stone");
		helper.succeed();
	}

	private static boolean scorched(BlockState s) {
		return s.is(ModWastes.SCORCHED_END_STONE.get()) || s.is(ModWastes.RUSTED_SCORCHED_END_STONE.get()) || s.is(ModWastes.LITTERED_SCORCHED_END_STONE.get());
	}

	/** What the wear leaves lying on a road: a heaved sett, broken stone, rubble, a spill of gravel. */
	private static boolean rubble(BlockState s) {
		return s.getBlock() instanceof SlabBlock || s.getBlock() instanceof net.minecraft.world.level.block.StairBlock || s.is(Blocks.COBBLED_DEEPSLATE)
			|| s.is(Blocks.GRAVEL);
	}

	/** The distance (the larger of x and z) from {@code at} to the nearest of {@code columns}, up to {@code reach} (past it: reach + 1). */
	private static int nearest(java.util.Set<Long> columns, BlockPos at, int reach) {
		for (int r = 0; r <= reach; r++)
			for (int x = -r; x <= r; x++)
				for (int z = -r; z <= r; z++)
					if ((Math.abs(x) == r || Math.abs(z) == r) && columns.contains(BlockPos.asLong(at.getX() + x, 0, at.getZ() + z)))
						return r;
		return reach + 1;
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
			int band = 0, cutScree = 0, fillScree = 0, fan = 0, eased = 0, levelled = 0, endStone = 0;
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
						levelled++;
						if (level.getBlockState(new BlockPos(x, floor, z)).is(Blocks.END_STONE))
							endStone++;
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
			// the outskirts' scorched ground has its patches of End Stone (CityGround)
			check(endStone > levelled / 12 && endStone < levelled / 2, endStone + " of " + levelled + " levelled columns are End Stone");
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
