package com.comedymonkey.createaddons.content.wastes.city.kit;

import java.util.LinkedHashMap;
import java.util.Map;

import com.comedymonkey.createaddons.content.wastes.city.CityPlan;
import com.comedymonkey.createaddons.content.wastes.city.kit.Kit.Ctx;
import com.comedymonkey.createaddons.content.wastes.city.kit.Kit.Face;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * Type 4, the long workshop shed (Workshops, lot B; design brief): a saw blade along the skyline. Walls 6 on the full
 * grammar (tall 1 x 3 side windows a bay), a north-light sawtooth roof to 10: teeth across the long axis every 4 blocks,
 * each a 1:1 slate slope rising 3 to a glazed face 3 high facing the heart (local -x). The loading door at the street
 * gable (4 x 4, a walnut beam over it, Create train trapdoors as a half-raised roller door: the one makers' opening the
 * uprising never needed to cut), its plaque beside it. Inside: walnut benches with Broken Mechanical Arms and Deployers,
 * the makers' line shaft (Create Shafts along each truss line, a cogwheel over each bench). The uprising: a lined cut in
 * the back long side, charging alcoves against the inner piers at the far end, cable threaded through the clerestory.
 */
public final class Workshop {
	private Workshop() {}

	/** 29 long (7 bays of 4), 9 wide (two side by side fit lot B beside the corner mast), walls to y 6. */
	public static final int L = 29, D = 9, E = 6;

	public static Model build(Ctx c) {
		Model md = new Model();
		Masonry m = c.m();
		Face front = Kit.front(L, D), back = Kit.back(L, D), east = Kit.east(L, D), west = Kit.west(L, D);
		// walls: plinth (y 0), field, lintel band at y 5, top course y 6
		for (Face f : new Face[] { front, back, east, west })
			for (int t = 0; t < f.n(); t++)
				for (int y = 1; y <= E; y++)
					f.set(md, t, y, 0, y == 5 ? m.band() : m.wall());
		int dz = D / 2 - 1; // the loading door: z dz..dz+3 in the west gable
		for (Face f : new Face[] { front, back, east, west })
			Kit.plinth(md, m, f, 1, f == west ? t -> t >= dz && t <= dz + 3 : t -> false);
		// piers every 4 on the long sides (one under each tooth's valley), the corners an L
		for (Face f : new Face[] { front, back })
			for (int t = 4; t < f.n() - 1; t += 4) {
				f.set(md, t, 0, 1, m.plinth());
				Kit.pier(md, m, f, t, 1, E - 1, 1);
			}
		for (int[] cn : new int[][] { { 0, 0 }, { L - 1, 0 }, { L - 1, D - 1 }, { 0, D - 1 } }) {
			Direction ox = cn[0] == 0 ? Direction.WEST : Direction.EAST, oz = cn[1] == 0 ? Direction.NORTH : Direction.SOUTH;
			Kit.cornerPier(md, m, cn[0], cn[1], ox, oz, 0, 0);
			Kit.cornerPier(md, m, cn[0], cn[1], ox, oz, 1, E - 1);
		}
		// tall side windows 1 x 3 a bay (y 2-4) with sills; the small-tuff panels either side (fixed places)
		for (Face f : new Face[] { front, back })
			for (int t = 2; t < f.n() - 1; t += 4) {
				Kit.window(md, m, f, t, 2, 3);
				f.set(md, t - 1, 3, 0, m.second());
				f.set(md, t + 1, 3, 0, m.second());
			}
		Kit.window(md, m, east, 2, 2, 3);
		Kit.window(md, m, east, D - 3, 2, 3);
		for (Face f : new Face[] { front, back })
			Kit.corbels(md, m, f, E, false, 0, f.n() - 1);
		// ---- the sawtooth: for each tooth k (x 4k .. 4k+3), the glazed face at x 4k looking -x (y 7-9), slate stairs at
		// x 4k+1 (y 9), 4k+2 (y 8), 4k+3 (y 7) rising toward it, a slate slab on the glazing's top; the barge walnut
		BlockState glassX = Masonry.window().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_AXIS, Direction.Axis.X);
		for (int k = 0; 4 * k < L - 1; k++) {
			int gx = 4 * k;
			for (int z = -1; z <= D; z++) {
				boolean edge = z == -1 || z == D;
				for (int y = E + 1; y <= E + 3; y++)
					md.set(gx, y, z, edge ? (y == E + 1 ? Kit.stairs(m.dressStairs(), Direction.EAST, false) : m.second()) : glassX);
				md.set(gx, E + 4, z, Kit.slab(Masonry.slateSlab(), false));
				for (int i = 1; i <= 3 && gx + i <= L - 1; i++) {
					int y = E + 4 - i;
					md.set(gx + i, y, z, Kit.stairs(edge ? Masonry.walnutStairs() : Masonry.slateStairs(), Direction.WEST, false));
					// the tooth's end closed under its slope in the wall line (small tuff: the gable infill's fixed place)
					if (z == 0 || z == D - 1)
						for (int yy = E + 1; yy < y; yy++)
							md.set(gx + i, yy, z, m.second());
				}
			}
		}
		// the last tooth's glazing over the outer gable wall: the gable closes the end under the last slope
		for (int z = 0; z < D; z++)
			md.set(L - 1, E + 1, z, m.second());
		// the trusses: walnut slab tie beams along each valley line (x 4k), the teeth's trusses bearing on the piers
		for (int x = 4; x < L - 1; x += 4)
			for (int z = 1; z <= D - 2; z++)
				md.set(x, E, z, Kit.slab(Masonry.walnutSlab(), true));
		// ---- inside: benches along the front with machines on them; the line shaft along the truss lines with a cogwheel over each bench
		for (int x = 2; x < L - 2; x += 4) {
			md.set(x, 0, 2, Masonry.walnutPlanks());
			md.set(x + 1, 0, 2, Kit.slab(Masonry.walnutSlab(), true));
			md.set(x, 1, 2, ((x / 4) % 2 == 0 ? ModWastes.BROKEN_MECHANICAL_ARM : ModWastes.BROKEN_DEPLOYER).get().defaultBlockState()
				.setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
			md.set(x, 0, D - 3, Masonry.walnutPlanks());
			md.set(x + 1, 0, D - 3, Masonry.walnutPlanks());
		}
		BlockState shaft = CityPlan.with(CityPlan.block("create:shaft", Blocks.CHAIN), "axis", "x");
		BlockState cog = CityPlan.with(CityPlan.block("create:cogwheel", Blocks.CHAIN), "axis", "x");
		for (int x = 1; x <= L - 2; x++)
			md.set(x, E - 2, 3, (x - 2) % 4 == 0 ? cog : shaft);
		for (int x = 4; x < L - 1; x += 4)
			md.set(x, E - 1, 3, Blocks.CHAIN.defaultBlockState());
		// ---- the loading door in the west gable: 4 x 4, its walnut beam, train trapdoors as a half-raised roller door
		for (int t = dz; t <= dz + 3; t++) {
			for (int y = 0; y <= 3; y++)
				west.remove(md, t, y, 0);
			west.remove(md, t, 0, 1);
			west.set(md, t, 4, 0, Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
			BlockState panel = CityPlan.block("create:train_trapdoor", Blocks.DARK_OAK_TRAPDOOR);
			panel = CityPlan.with(CityPlan.with(CityPlan.with(panel, "facing", "east"), "open", "true"), "half", "bottom");
			west.set(md, t, 3, 0, panel);
			west.set(md, t, 2, 0, panel);
		}
		for (int y = 0; y <= E - 1; y++) {
			west.set(md, dz - 1, y, 1, y == 0 ? m.plinth() : m.pier());
			west.set(md, dz + 4, y, 1, y == 0 ? m.plinth() : m.pier());
		}
		Shed.plaque(md, west.x(dz - 2, 1), west.z(dz - 2, 1), Direction.WEST, c.wear().atLeast(Wear.DAMAGED));
		// ---- the uprising: a lined cut in the back (onto the gantry lot), alcoves at the far end, cable through the clerestory
		Shed.linedCut(md, m, back, 9, 1);
		for (int x : new int[] { L - 6, L - 10 })
			md.special(Map.of(new BlockPos(x, 0, D - 2), ModWastes.CHARGING_ALCOVE.get().defaultBlockState(), new BlockPos(x, 1, D - 2),
				ModWastes.CHARGING_ALCOVE.get().defaultBlockState(), new BlockPos(x, 2, D - 2), ModWastes.CHARGING_ALCOVE.get().defaultBlockState()),
				site -> site.alcove(x, 0, D - 2, Direction.NORTH, false));
		Map<BlockPos, BlockState> cable = new LinkedHashMap<>();
		for (int x = 1; x <= 15; x++)
			cable.put(new BlockPos(x, E + 1, 4), ModWastes.DERELICT_CABLE.get().defaultBlockState());
		md.special(cable, site -> {
			for (BlockPos p : cable.keySet())
				site.cable(p.getX(), p.getY(), p.getZ(), Direction.DOWN);
		});
		for (int x = 4; x <= 12; x += 4)
			md.set(x, E + 1, 4, Kit.slab(Masonry.walnutSlab(), false));
		damage(md, c, front);
		return md;
	}

	/**
	 * Worn: the outward glazing broken in the outer tooth. Damaged: "a shed roof collapsed onto the line": the last two teeth
	 * at the outer end down on the benches (slates and a broken truss across them, glazing gone, the line shaft fallen in two
	 * lengths), the rubble inside (it fell in). Ruined: the outer half's walls to sill height.
	 */
	static void damage(Model md, Ctx c, Face front) {
		if (c.wear() == Wear.INTACT)
			return;
		int lastTooth = 4 * ((L - 2) / 4);
		if (c.wear() == Wear.WORN) {
			for (int z = 0; z < D; z++)
				for (int y = E + 1; y <= E + 3; y++)
					if (z % 3 != 1)
						md.remove(lastTooth, y, z);
			md.mark(Model.Cause.STRIKE, lastTooth, E + 2, D / 2, 2);
			md.strikes++;
			return;
		}
		if (c.wear() == Wear.DAMAGED) {
			int x0 = lastTooth - 4;
			for (int x = x0; x <= L; x++)
				for (int z = -1; z <= D; z++)
					for (int y = E + 1; y <= E + 4; y++)
						md.remove(x, y, z);
			for (int x = x0; x <= L - 2; x++)
				for (int z = 1; z <= D - 2; z++)
					md.remove(x, E, z);
			// the shaft fallen in two lengths, slates and a broken truss across the benches (it fell in)
			for (int x = x0; x <= L - 2; x++) {
				md.remove(x, E - 2, 3);
				md.remove(x, E - 1, 3);
			}
			for (int x = x0 + 1; x <= x0 + 3; x++)
				md.set(x, 1, 3, CityPlan.with(CityPlan.block("create:shaft", Blocks.CHAIN), "axis", "x"));
			for (int x = x0 + 5; x <= x0 + 7 && x < L - 1; x++)
				md.set(x, 0, 4, CityPlan.with(CityPlan.block("create:shaft", Blocks.CHAIN), "axis", "x"));
			for (int x = x0; x < L - 1; x++)
				for (int z = 1; z < D - 1; z++)
					if ((x * 3 + z) % 4 == 0)
						md.setIfEmpty(x, 0, z, Kit.slab(Masonry.slateSlab(), false));
					else if ((x + z * 5) % 7 == 0)
						md.setIfEmpty(x, 1, z, Kit.stairs(Masonry.slateStairs(), Direction.from2DDataValue((x + z) % 4), false));
			for (int z = 2; z <= D - 3; z++)
				md.set(x0 + 2, 1, z, Kit.slab(Masonry.walnutSlab(), (z % 2) == 0));
			md.set(x0 + 2, 2, 4, Masonry.walnutFence());
			md.mark(Model.Cause.FALL, x0 + 2, 0, D / 2, 4);
			md.settle();
			return;
		}
		// ruined: the outer half's walls to sill height, the roof gone over it, rubble in and round it
		int half = L / 2;
		for (var en : md.blocks().entrySet()) {
			BlockPos p = en.getKey();
			int x = p.getX(), y = p.getY(), z = p.getZ();
			if (x >= half && y >= 2 || x >= half - 4 && y > E)
				md.remove(x, y, z);
		}
		for (int y = E + 1; y < 20; y++)
			for (int x = half - 4; x < half + 4; x++)
				for (int z = -2; z <= D + 1; z++)
					md.remove(x, y, z);
		md.settle();
		for (int x = half; x < L - 1; x += 3)
			md.mark(Model.Cause.BREAK, x, 1, 0, 1);
		Kit.rubbleCone(md, c.m(), front, 0, L / 2 - 2, 40, c.seed());
	}

	static BlockState trapdoor(Direction facing) {
		return Blocks.DARK_OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, facing).setValue(TrapDoorBlock.HALF, Half.TOP);
	}
}
