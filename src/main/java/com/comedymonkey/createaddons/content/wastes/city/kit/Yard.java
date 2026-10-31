package com.comedymonkey.createaddons.content.wastes.city.kit;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.comedymonkey.createaddons.content.wastes.DerelictInsulatorBlock;
import com.comedymonkey.createaddons.content.wastes.ScarredPlateBlock;
import com.comedymonkey.createaddons.content.wastes.city.CityPlan;
import com.comedymonkey.createaddons.content.wastes.city.kit.Kit.Ctx;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Type 8, the Substation yard and its lattice mast, and the corner masts (design brief; D72, D73, D74): the district's
 * industrial set piece, the only place with warning colours. An 11 x 11 makers' yard (x, z 0..10, the gate in the front
 * fence at z 0) on gravel with polished cut deepslate pads under its equipment, a plinth-wall fence with andesite bars; the
 * Substation on its pad behind the one hazard band; the transformer (a void-alloy tank, Slotted Grille radiators down both
 * long sides, three Derelict Insulator bushings on its lid) with its arc scorch on the firewall behind the bushings; the cable
 * tray on wall posts; the lattice mast (four Create Metal Girder legs on a 3 x 3, girder rings, a cross-arm, an anti-climb
 * guard of andesite bars, each leg on a deepslate pad) with the approved riser climbing its front leg; the makers' control
 * hut (the smallest shed) with its window toward the Substation, a Broken Press in it and cable threaded in through the window.
 */
public final class Yard {
	private Yard() {}

	public static final int SIZE = 11;
	/** A lattice mast's legs' height (10-12 high with its insulator). */
	public static final int MAST_LEGS = 11;

	/** What the yard puts that the caller ties spans to: the Substation's lower block and the mast's insulator (local cells). */
	public record Built(Model model, Model hut, BlockPos substation, BlockPos mastInsulator) {}

	static BlockState girder(Direction.Axis axis) {
		return CityPlan.with(CityPlan.block("create:metal_girder", Blocks.IRON_BARS), "axis", axis.getName());
	}

	public static Built build(Ctx c) {
		Model md = new Model();
		Masonry m = c.m();
		BlockState pad = CityPlan.block("create:polished_cut_deepslate", Blocks.POLISHED_DEEPSLATE);
		BlockState padSlab = CityPlan.block("create:polished_cut_deepslate_slab", Blocks.POLISHED_DEEPSLATE_SLAB);
		// ground: gravel; pads under the Substation, the transformer, the mast's legs
		for (int x = 1; x < SIZE - 1; x++)
			for (int z = 1; z < SIZE - 1; z++)
				md.set(x, -1, z, Blocks.GRAVEL.defaultBlockState());
		for (int x = 1; x <= 3; x++)
			for (int z = 7; z <= 9; z++)
				md.set(x, -1, z, pad);
		for (int x = 5; x <= 9; x++)
			for (int z = 2; z <= 5; z++)
				md.set(x, -1, z, pad);
		for (int x = 5; x <= 9; x++) {
			md.set(x, 0, 1, Kit.slab(padSlab, false));
		}
		// the one hazard band across the yard in front of the Substation (muted safety yellow and black)
		for (int x = 1; x <= 9; x++)
			md.set(x, -1, 6, (x % 2 == 0 ? Blocks.YELLOW_TERRACOTTA : Blocks.BLACK_TERRACOTTA).defaultBlockState());
		// the fence: a plinth wall of polished cut deepslate (1 high), andesite bars on it (2 high), casing gate posts, the
		// gate in the front open
		for (int i = 0; i < SIZE; i++)
			for (int[] p : new int[][] { { i, 0 }, { i, SIZE - 1 }, { 0, i }, { SIZE - 1, i } }) {
				boolean gate = p[1] == 0 && p[0] >= 5 && p[0] <= 7;
				if (gate)
					continue;
				md.set(p[0], 0, p[1], pad);
				md.set(p[0], 1, p[1], Masonry.bars());
				md.set(p[0], 2, p[1], Masonry.bars());
			}
		for (int x : new int[] { 4, 8 })
			for (int y = 0; y <= 2; y++)
				md.set(x, y, 0, CityPlan.block("create:andesite_casing", Blocks.POLISHED_ANDESITE));
		// the Substation on its pad, facing the gate
		BlockPos sub = new BlockPos(2, 0, 8);
		md.special(Map.of(sub, ModWastes.SUBSTATION.get().defaultBlockState(), sub.above(), ModWastes.SUBSTATION.get().defaultBlockState()),
			site -> site.bound(() -> site.plan.substation(site.at(sub.getX(), 0, sub.getZ()), site.w(Direction.NORTH))));
		// the transformer: a tank 3 long x 2 high x 2 deep (x 6..8, z 3..4), grille radiators down both long sides (z 2, 5),
		// three insulator bushings on the lid (y 2, z 3), heat tint on its upper course under them; the firewall behind (x 9,
		// the yard's wall) with the arc scorch: soot fanning up from the bushings' height
		for (int x = 6; x <= 8; x++)
			for (int y = 0; y <= 1; y++) {
				md.set(x, y, 3, y == 1 && x == 7 ? Kit.scarred(ScarredPlateBlock.Scar.HEAT, Direction.NORTH) : Kit.plate());
				md.set(x, y, 4, Kit.plate());
				md.set(x, y, 2, Kit.grille(Direction.NORTH));
				md.set(x, y, 5, Kit.grille(Direction.SOUTH));
			}
		for (int x = 6; x <= 8; x++) {
			int bx = x;
			md.special(Map.of(new BlockPos(bx, 2, 3), ModWastes.DERELICT_INSULATOR.get().defaultBlockState()),
				site -> site.insulator(bx, 2, 3, DerelictInsulatorBlock.Mount.TOP, Direction.NORTH));
		}
		for (int z = 2; z <= 5; z++)
			for (int y = 0; y <= 3; y++)
				md.set(9, y, z, y >= 2 && z >= 3 && z <= 4 ? Masonry.soot() : y == 3 && (z == 2 || z == 5) && z % 2 == 0 ? Masonry.sootPlain() : m.wall());
		md.set(9, 4, 3, Kit.slab(m.dressSlab(), false));
		md.set(9, 4, 4, Kit.slab(m.dressSlab(), false));
		md.mark(Model.Cause.ARC, 7, 2, 3, 3);
		// the cable tray: void-alloy top slabs at head height on wall posts every 3, from the Substation to the transformer, the
		// cable on it
		int[][] tray = { { 3, 8 }, { 4, 8 }, { 4, 7 }, { 4, 6 }, { 4, 5 }, { 4, 4 }, { 5, 4 } };
		Map<BlockPos, BlockState> trayCable = new LinkedHashMap<>();
		for (int i = 0; i < tray.length; i++) {
			int x = tray[i][0], z = tray[i][1];
			md.set(x, 2, z, Kit.vaSlab(true));
			if (i % 3 == 0)
				for (int y = 0; y <= 1; y++)
					md.set(x, y, z, Kit.vaWall());
			trayCable.put(new BlockPos(x, 3, z), ModWastes.DERELICT_CABLE.get().defaultBlockState());
		}
		md.special(trayCable, site -> {
			for (BlockPos p : trayCable.keySet())
				site.cable(p.getX(), p.getY(), p.getZ(), Direction.DOWN);
		});
		// the lattice mast: legs at (6, 8), (8, 8), (6, 10)... kept inside the fence: legs x 6, 8, z 7, 9
		BlockPos mastIns = lattice(md, 6, 7, true, Direction.NORTH);
		// the ground cable from the Substation's front to the riser's foot (6, 0, 6)
		Map<BlockPos, BlockState> ground = new LinkedHashMap<>();
		for (int[] p : new int[][] { { 2, 7 }, { 2, 6 }, { 3, 6 }, { 3, 5 }, { 4, 5 }, { 5, 5 }, { 5, 6 } })
			ground.put(new BlockPos(p[0], 0, p[1]), ModWastes.DERELICT_CABLE.get().defaultBlockState());
		md.special(ground, site -> {
			for (BlockPos p : ground.keySet())
				site.cable(p.getX(), p.getY(), p.getZ(), Direction.DOWN);
		});
		// worn: one fence panel down (its bars gone, a heap where it fell)
		if (c.wear().atLeast(Wear.WORN))
			for (int x = 2; x <= 4; x++) {
				md.remove(x, 1, SIZE - 1);
				md.remove(x, 2, SIZE - 1);
				if (x == 3)
					md.set(x, 0, SIZE - 2, ModWastes.SCRAP_HEAP.get().defaultBlockState());
			}
		// the control hut: the smallest shed, 4 x 4 (x 1..4, z 1..4), its window toward the Substation, its door toward the
		// gate; drawn in its own frame (local x along its ridge from the gate end)
		Ctx hutCtx = new Ctx(m, c.wear().atMost(Wear.WORN), java.util.Set.of(), c.seed() + 5);
		Model hut = Shed.build(hutCtx, new Shed.Spec(4, 4, 2, Shed.Door.WALNUT, false, false));
		hut.set(2, 0, 2, ModWastes.BROKEN_PRESS.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
		return new Built(md, hut, sub, mastIns);
	}

	/**
	 * A lattice mast (D73: four girder legs on a 3 x 3) with its legs at (x0, z0), (x0+2, z0), (x0, z0+2), (x0+2, z0+2):
	 * legs {@link #MAST_LEGS} high on deepslate pads, girder rings every 3-4 blocks, a cross-arm (a horizontal girder 5 long)
	 * near the top, an anti-climb guard of andesite bars at y 2-3, the approved riser (D72) up the outer face of its
	 * {@code riserFront} leg toward {@code riserSide}, and a top-mounted insulator on that leg (D64's collar bites the
	 * girder). Returns the insulator's local cell.
	 */
	public static BlockPos lattice(Model md, int x0, int z0, boolean guard, Direction riserSide) {
		BlockState pad = CityPlan.block("create:polished_cut_deepslate", Blocks.POLISHED_DEEPSLATE);
		int[][] legs = { { x0, z0 }, { x0 + 2, z0 }, { x0, z0 + 2 }, { x0 + 2, z0 + 2 } };
		for (int[] l : legs) {
			md.set(l[0], -1, l[1], pad);
			for (int y = 0; y < MAST_LEGS; y++)
				md.set(l[0], y, l[1], girder(Direction.Axis.Y));
		}
		for (int y : new int[] { 4, 7 }) {
			md.set(x0 + 1, y, z0, girder(Direction.Axis.X));
			md.set(x0 + 1, y, z0 + 2, girder(Direction.Axis.X));
			md.set(x0, y, z0 + 1, girder(Direction.Axis.Z));
			md.set(x0 + 2, y, z0 + 1, girder(Direction.Axis.Z));
		}
		// the cross-arm through the middle, 5 long, one past the legs either side
		for (int x = x0 - 1; x <= x0 + 3; x++)
			md.set(x, MAST_LEGS - 2, z0 + 1, girder(Direction.Axis.X));
		if (guard)
			for (int y = 2; y <= 3; y++) {
				md.set(x0 + 1, y, z0, Masonry.bars());
				md.set(x0 + 1, y, z0 + 2, Masonry.bars());
				md.set(x0, y, z0 + 1, Masonry.bars());
				md.set(x0 + 2, y, z0 + 1, Masonry.bars());
			}
		// the riser up the front leg's face toward riserSide, the insulator on that leg's top
		int[] leg = riserSide == Direction.NORTH || riserSide == Direction.WEST ? legs[0] : legs[3];
		BlockPos foot = new BlockPos(leg[0] + riserSide.getStepX(), 0, leg[1] + riserSide.getStepZ());
		Map<BlockPos, BlockState> cells = new LinkedHashMap<>();
		for (int y = 0; y < MAST_LEGS; y++)
			cells.put(foot.above(y), ModWastes.DERELICT_CABLE.get().defaultBlockState());
		BlockPos ins = new BlockPos(leg[0], MAST_LEGS, leg[1]);
		cells.put(ins, ModWastes.DERELICT_INSULATOR.get().defaultBlockState());
		Direction girderSide = riserSide.getOpposite();
		md.special(cells, site -> {
			site.riser(foot.getX(), 0, foot.getZ(), girderSide, MAST_LEGS);
			site.insulator(ins.getX(), ins.getY(), ins.getZ(), DerelictInsulatorBlock.Mount.TOP, riserSide);
		});
		return ins;
	}

	/**
	 * The corner mast at a mock crossroads (catalogue; D72, D74): two streets 5 wide crossing (x 0..4 along z, z 0..4 along
	 * x, paving, scorched gutters), Derelict Cable along each street one in from its edge, the lattice mast on the
	 * crossroads' outer corner (legs x 6..8, z 6..8) with its riser on the leg facing the crossroads, both streets' cable lines
	 * turning into its foot. Returns the insulator's local cell.
	 */
	public static BlockPos cornerMast(Model md, int len) {
		for (int a = 0; a < len; a++)
			for (int w = 0; w <= 4; w++) {
				BlockState pv = w == 0 || w == 4 ? ModWastes.SCORCHED_END_STONE.get().defaultBlockState() : Kit.hash(a, w, 3, 9) % 5 == 0
					? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.POLISHED_ANDESITE.defaultBlockState();
				md.set(w, -1, a, pv);
				md.set(a, -1, w, pv);
			}
		BlockPos ins = lattice(md, 6, 6, true, Direction.NORTH);
		Map<BlockPos, BlockState> cable = new LinkedHashMap<>();
		BlockState cb = ModWastes.DERELICT_CABLE.get().defaultBlockState();
		for (int a = 0; a < len; a++) {
			cable.put(new BlockPos(a, 0, 3), cb);
			cable.put(new BlockPos(3, 0, a), cb);
		}
		// the turn into the riser's foot (6, 5)
		cable.put(new BlockPos(4, 0, 5), cb);
		cable.put(new BlockPos(5, 0, 5), cb);
		cable.put(new BlockPos(3, 0, 5), cb);
		md.special(cable, site -> {
			for (BlockPos p : cable.keySet())
				site.cable(p.getX(), p.getY(), p.getZ(), Direction.DOWN);
		});
		return ins;
	}

	/** The hut's frame inside the yard: its gable door toward the gate (yard +x), its back (window) toward the Substation. */
	public static Site hutSite(Site yard) {
		return yard.sub(4, 1, Direction.WEST, Direction.SOUTH);
	}

	/** The Substation's barrel-free yard has no loot; kept for symmetry with the other builders. */
	static List<BlockState> none() {
		return List.of(Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
	}
}
