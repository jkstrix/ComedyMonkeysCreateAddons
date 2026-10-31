package com.comedymonkey.createaddons.content.wastes.city.kit;

import java.util.LinkedHashMap;
import java.util.Map;

import com.comedymonkey.createaddons.content.wastes.city.kit.Kit.Ctx;
import com.comedymonkey.createaddons.content.wastes.city.kit.Kit.Face;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Type 3, the makers' terrace with its doorways bricked up (Quarters, lot B; design brief): a row of small houses, each 5
 * wide between shared party walls, two storeys at human scale under steep 1:1 slate roofs, a party-wall chimney stack on
 * every party wall marching along the ridge; the most human-scaled thing in the city. Local x along the row (the outer end
 * house at the high x), z back from the front (z 0). Every front door and ground-floor window infilled flush with hasty
 * rubble by the makers themselves (D73), a mismatched timber lintel over each; a plaque by each door; Derelict Cable along
 * the row at the upper windows, held off the wall by a Clamp Bracket on each party-wall pier.
 */
public final class Terrace {
	private Terrace() {}

	/** A house's width (party wall to party wall), the row's depth (even: the ridge centres, the brief's 7 made 6), the walls' top. */
	public static final int HOUSE = 5, D = 6, E = 6;

	public static Model build(Ctx c, int houses) {
		Model md = new Model();
		Masonry m = c.m();
		int l = houses * HOUSE + 1;
		Face front = Kit.front(l, D), back = Kit.back(l, D), east = Kit.east(l, D), west = Kit.west(l, D);
		// ---- walls: plinth course (y 0), ground storey (1-3), the storey line (4: the lintel band), upper storey (5-6)
		for (Face f : new Face[] { front, back, east, west })
			for (int t = 0; t < f.n(); t++) {
				f.set(md, t, 0, 0, m.plinth());
				for (int y = 1; y <= E; y++)
					f.set(md, t, y, 0, y == 4 ? m.band() : m.wall());
			}
		// party walls inside
		for (int h = 1; h < houses; h++)
			for (int z = 1; z < D - 1; z++)
				for (int y = 0; y <= E; y++)
					md.set(h * HOUSE, y, z, y == 0 ? m.plinth() : m.wall());
		// ---- the party-wall piers on the front (1 proud) and the end piers; the back's piers
		for (int h = 0; h <= houses; h++) {
			int x = h * HOUSE;
			for (int y = 0; y <= E - 1; y++) {
				md.set(x, y, -1, y == 0 ? m.plinth() : m.pier());
				md.set(x, y, D, y == 0 ? m.plinth() : m.pier());
			}
		}
		for (int[] cn : new int[][] { { 0, 0 }, { l - 1, 0 }, { l - 1, D - 1 }, { 0, D - 1 } }) {
			int ox = cn[0] == 0 ? -1 : 1, oz = cn[1] == 0 ? -1 : 1;
			for (int y = 0; y <= E - 1; y++) {
				md.set(cn[0] + ox, y, cn[1], y == 0 ? m.plinth() : m.pier());
				md.set(cn[0] + ox, y, cn[1] + oz, y == 0 ? m.plinth() : m.pier());
			}
		}
		// ---- each house: a door (bricked up), a ground-floor window (bricked up), two upper windows, a doorstep, its plaque
		BlockState winZ = Masonry.window().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_AXIS, Direction.Axis.Z);
		boolean askewOuter = c.wear().atLeast(Wear.DAMAGED);
		for (int h = 0; h < houses; h++) {
			int x0 = h * HOUSE;
			int door = x0 + 1, plaque = x0 + 2, win = x0 + 3;
			// the bricked-up door: hasty rubble flush with the wall, a spalled brick or two, no coursing, a mismatched lintel
			md.set(door, 1, 0, Masonry.rubble());
			md.set(door, 2, 0, h % 2 == 0 ? m.spalled() : Masonry.rubble());
			md.set(door, 3, 0, Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
			md.set(door, 0, -1, Kit.stairs(m.plinthStairs(), Direction.SOUTH, false));
			md.mark(Model.Cause.CUT, door, 2, 0, 0);
			// the ground-floor window, bricked up the same way (no sill: they didn't trouble)
			md.set(win, 2, 0, Masonry.rubble());
			md.set(win, 3, 0, h % 2 == 1 ? m.spalled() : Masonry.rubble());
			md.mark(Model.Cause.CUT, win, 3, 0, 0);
			// two upper windows (1 x 1) under the eaves, their sills on the storey line
			for (int ux : new int[] { x0 + 1, x0 + 3 }) {
				md.set(ux, 5, 0, winZ);
				md.set(ux, 4, -1, Kit.stairs(m.dressStairs(), Direction.SOUTH, true));
			}
			// windows at the back too (the yards behind)
			md.set(x0 + 2, 2, D - 1, winZ);
			md.set(x0 + 2, 3, D - 1, winZ);
			md.set(x0 + 3, 5, D - 1, winZ);
			// the plaque: on the wall block beside the door, 1 up (askew on the outer houses from the damaged ring)
			Shed.plaque(md, plaque, -1, Direction.NORTH, askewOuter && h >= houses - 2);
			// inside: the walnut floor at the storey line, a fireplace on the party wall at both storeys, a walnut stair
			for (int x = x0 + 1; x <= x0 + 4; x++)
				for (int z = 1; z <= D - 2; z++)
					if (!(x == x0 + 4 && z >= D - 3))
						md.set(x, 4, z, Kit.slab(Masonry.walnutSlab(), true));
			{
				for (int y : new int[] { 1, 5 }) {
					BlockState hearth = CityPlanBlocks.scoriaBricks();
					md.set(x0 + 1, y, 2, hearth);
					md.set(x0 + 1, y, 3, hearth);
					md.set(x0 + 1, y + 1, 2, Kit.stairs(CityPlanBlocks.scoriaStairs(), Direction.WEST, true));
					md.set(x0 + 1, y + 1, 3, Kit.stairs(CityPlanBlocks.scoriaStairs(), Direction.WEST, true));
					md.set(x0, y, 2, Masonry.soot());
					md.set(x0, y + 1, 2, Masonry.soot());
					md.mark(Model.Cause.CHIMNEY, x0, y, 2, 3);
				}
			}
			md.set(x0 + 4, 1, D - 4, Kit.stairs(Masonry.walnutStairs(), Direction.SOUTH, false));
			md.set(x0 + 4, 2, D - 3, Kit.stairs(Masonry.walnutStairs(), Direction.SOUTH, false));
			md.set(x0 + 4, 3, D - 2, Kit.stairs(Masonry.walnutStairs(), Direction.SOUTH, false));
		}
		// ---- the corbel course, eaves 1 over, a 1:1 slate roof (stairs) to a ridge with its copper roll
		for (Face f : new Face[] { front, back })
			Kit.corbels(md, m, f, E, false, 0, f.n() - 1);
		for (int x = -1; x <= l; x++) {
			boolean barge = x == -1 || x == l, gable = x == 0 || x == l - 1;
			for (int z = -1; z <= D; z++) {
				int rise = Math.min(z + 1, D - z);
				int y = E + 1 + rise;
				Direction up = z + 1 <= D - z ? Direction.SOUTH : Direction.NORTH;
				BlockState st = Kit.stairs(barge ? Masonry.walnutStairs() : Masonry.slateStairs(), up, false);
				if (gable && z >= 0 && z <= D - 1) {
					for (int yy = E + 1; yy < y; yy++)
						md.set(x, yy, z, m.second());
					md.set(x, y, z, st);
					continue;
				}
				md.set(x, y, z, st);
				if (!barge && (z == 0 || z == D - 1) && x > 0 && x < l - 1) {
					md.set(x, y - 1, z, Masonry.slateFull());
					md.set(x, y - 2, z, m.wall());
				}
			}
			int ry = E + 1 + Math.min(D / 2 - 1 + 1, D - (D / 2 - 1)) + 1;
			if (!barge) {
				md.set(x, ry, D / 2 - 1, Masonry.ridge());
				md.set(x, ry, D / 2, Masonry.ridge());
			}
		}
		int ridgeY = E + 1 + Math.min(D / 2 - 1 + 1, D - (D / 2 - 1));
		// the party-wall chimney stacks (1 x 2 on the ridge, 2 above it), their tops sooted
		for (int h = 1; h < houses; h++) {
			int x = h * HOUSE;
			for (int z = D / 2 - 1; z <= D / 2; z++) {
				for (int y = E + 1; y <= ridgeY + 2; y++)
					md.set(x, y, z, y >= ridgeY + 1 ? (y == ridgeY + 2 || Kit.hash(x, y, z, 5) % 2 == 0 ? Masonry.soot() : m.wall()) : m.wall());
				md.set(x, ridgeY + 3, z, Kit.slab(m.dressSlab(), false));
			}
			md.mark(Model.Cause.CHIMNEY, x, ridgeY + 3, D / 2, 3);
		}
		// ---- the uprising: cable along the row at the upper windows, a Clamp Bracket on each party-wall pier holding it off
		Map<BlockPos, BlockState> cable = new LinkedHashMap<>();
		BlockState cb = ModWastes.DERELICT_CABLE.get().defaultBlockState();
		for (int x = 0; x < l; x++)
			if (x % HOUSE != 0)
				cable.put(new BlockPos(x, 5, -1), cb);
		md.special(cable, site -> {
			for (BlockPos p : cable.keySet())
				site.cable(p.getX(), p.getY(), p.getZ(), Direction.SOUTH);
		});
		for (int h = 0; h <= houses; h++)
			md.set(h * HOUSE, 5, -2, Kit.clampWall(Direction.NORTH));
		// ---- the state
		damage(md, c, houses, l, front, east);
		return md;
	}

	/**
	 * Worn: a strike on the outer end house, soot from one upper window. Damaged: the outer end house's front wall down in
	 * the street (a rubble cone), its floor line, both fireplaces on the party wall and the stair showing; the neighbour's
	 * roof holed. Ruined: only the party walls and chimney stacks standing, like a row of headstones.
	 */
	static void damage(Model md, Ctx c, int houses, int l, Face front, Face east) {
		Masonry m = c.m();
		if (c.wear() == Wear.INTACT)
			return;
		int outer = (houses - 1) * HOUSE;
		if (c.wear() == Wear.WORN) {
			if (c.outward(Direction.EAST))
				Kit.strike(md, m, east, 2, 2, 2, 2, c.seed());
			int wx = outer - HOUSE + 3;
			md.remove(wx, 5, 0);
			Kit.sootFan(md, front, l - 1 - wx, 6, 1, 1, 2, c.seed(), Model.Cause.FIRE);
			return;
		}
		if (c.wear() == Wear.DAMAGED) {
			// the outer house's front wall into the street, on a stepped line down from its party wall
			int fallen = 0;
			for (int x = outer + 1; x <= l - 1; x++)
				for (int y = 0; y <= E + 6; y++)
					for (int z = -2; z <= 0; z++) {
						int keep = x == l - 1 ? 2 : Math.max(0, 3 - (x - outer));
						if (y > keep && md.get(x, y, z) != null) {
							md.remove(x, y, z);
							fallen++;
						}
					}
			// its roof's front slope fell in with it
			for (int x = outer + 1; x <= l; x++)
				for (int z = -2; z <= 2; z++)
					for (int y = E + 1; y <= E + 6; y++)
						md.remove(x, y, z);
			md.settle();
			Face f = new Face(l - 1, 0, Direction.NORTH, HOUSE);
			Kit.rubbleCone(md, m, f, 0, HOUSE - 1, fallen, c.seed());
			for (int x = outer + 1; x < l - 1; x++)
				md.mark(Model.Cause.BREAK, x, 2, 0, 1);
			// the neighbour's roof holed (slates gone over three, a rafter left)
			int nx = outer - HOUSE + 2;
			for (int x = nx; x <= nx + 2; x++)
				for (int z = 0; z <= 1; z++)
					md.remove(x, E + 1 + Math.min(z + 1, D - z), z);
			md.set(nx + 1, E + 1 + 1, 0, Kit.stairs(Masonry.walnutStairs(), Direction.SOUTH, false));
			md.set(nx, 5, 2, Kit.slab(Masonry.slateSlab(), false));
			Kit.strike(md, m, east, 1, 4, 2, 2, c.seed() + 1);
			return;
		}
		// ruined: everything but the party walls (and the end walls' piers) and the chimney stacks
		int fallen = 0;
		for (var en : md.blocks().entrySet()) {
			BlockPos p = en.getKey();
			int x = p.getX(), y = p.getY(), z = p.getZ();
			if (y <= 0)
				continue;
			boolean party = x % HOUSE == 0 && x >= 0 && x <= l - 1 && z >= -1 && z <= D;
			int top = party ? 99 : (x + z) % 3 == 0 ? 2 : 1;
			if (y > top) {
				md.remove(x, y, z);
				fallen++;
			}
		}
		// the stacks' roofs gone, so each party wall's gable steps down on a stepped line from its stack
		for (int h = 0; h <= houses; h++) {
			int x = h * HOUSE;
			for (int z = -2; z <= D + 1; z++)
				for (int y = E + 1; y <= E + 12; y++) {
					BlockState s = md.get(x, y, z);
					if (s != null && (s.getBlock() instanceof net.minecraft.world.level.block.StairBlock || s.getBlock() instanceof net.minecraft.world.level.block.SlabBlock))
						md.remove(x, y, z);
				}
			md.mark(Model.Cause.BREAK, x, E, 0, 1);
		}
		md.settle();
		Kit.rubbleCone(md, m, front, 0, front.n() - 1, Math.min(fallen, 60), c.seed());
	}

	/** The fireplaces' scoria brick (the Foundry's firebrick, carried to the Quarters for hearths). */
	static final class CityPlanBlocks {
		static BlockState scoriaBricks() {
			return Masonry.foundry().wall();
		}

		static BlockState scoriaStairs() {
			return Masonry.foundry().dressStairs();
		}
	}
}
