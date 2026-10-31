package com.comedymonkey.createaddons.content.wastes.city.kit;

import java.util.Map;

import com.comedymonkey.createaddons.content.wastes.ScarredPlateBlock;
import com.comedymonkey.createaddons.content.wastes.city.CityPlan;
import com.comedymonkey.createaddons.content.wastes.city.kit.Kit.Ctx;
import com.comedymonkey.createaddons.content.wastes.city.kit.Kit.Face;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Type 2, the brick furnace shed with its chimney and blast furnace (Foundry, lot B; design brief): a heavy hall with a
 * stepped monitor roof, a tall square stack at its heart end and a stepped-in furnace stack at its outer end, the Foundry's
 * skyline reading as industrial teeth. The hall's long side toward the casting floor (its front, z 0) is a row of arched
 * furnace mouths between piers under a corbel table; walls 2 thick on a two-course plinth with a stair water table.
 * <ul>
 * <li>The chimney (x -7..-3): a 5 x 5 plinth 2 high, a 3 x 3 shaft to 21 with its flue, a stair set-off at a third, hoops
 * every 5, a corbelled cap, its top three courses sooted (smoke), an andesite ladder up its outer face.</li>
 * <li>The blast furnace (x l+1..l+5): a 5 x 5 stack of cut scoria to y 6, battered in by a stair course to 3 x 3, hooped every
 * 3 in weathered iron, a bustle pipe of Create fluid pipe round it at y 3 with four downcomers (the tuyeres), a tap hole on
 * the casting floor side under a soot fan, a walnut charging deck on its top reached by an andesite ladder. Square: a
 * vessel, not an engine.</li>
 * <li>The launder (the join): a void-alloy pouring channel from the tap hole, slabs on wall legs stepping down a half block a
 * block, clamped to the furnace's bottom hoop by two Clamp Brackets, its first blocks heat-tinted scarred plates.</li>
 * </ul>
 */
public final class FurnaceShed {
	private FurnaceShed() {}

	/**
	 * The hall: 16 long (three mouths, pitch 5: the brief's 17-19 with four mouths doesn't fit lot B with its chimney and
	 * furnace; progress doc), 14 deep, walls to y 7. The toppled shaft lies {@link #FALLEN} blocks (across one street).
	 */
	public static final int L = 16, D = 14, E = 7, FALLEN = 4;

	public static Model build(Ctx c) {
		Model md = new Model();
		Masonry m = c.m();
		Face front = Kit.front(L, D), back = Kit.back(L, D), east = Kit.east(L, D), west = Kit.west(L, D);
		Face[] all = { front, back, east, west };
		// ---- walls, 2 thick: the outer layer (d 0) and the inner (d -1)
		for (Face f : all)
			for (int t = 0; t < f.n(); t++)
				for (int y = 2; y <= E; y++)
					for (int d = 0; d >= -1; d--)
						if (!(d == -1 && (t == 0 || t == f.n() - 1)))
							f.set(md, t, y, d, y == E - 1 ? m.band() : m.wall());
		for (Face f : all)
			for (int t = 0; t < f.n(); t++)
				for (int y = 0; y <= 1; y++)
					f.set(md, t, y, -1, m.plinth());
		for (Face f : all)
			Kit.plinth(md, m, f, 2, t -> false);
		// ---- the front: arched furnace mouths (2 x 3, recessed, a block arch over each and a hood 1 proud), piers between
		for (int k = 0; k < (L - 1) / 5; k++) {
			int t0 = 5 * k + 2; // the mouth's columns t0, t0 + 1
			for (int y = 0; y <= 2; y++)
				for (int d = 1; d >= -1; d--) {
					front.remove(md, t0, y, d);
					front.remove(md, t0 + 1, y, d);
				}
			front.remove(md, t0, 3, 1);
			front.remove(md, t0 + 1, 3, 1);
			front.set(md, t0, 3, 0, Kit.stairs(m.plinthStairs(), front.along().getOpposite(), true));
			front.set(md, t0 + 1, 3, 0, Kit.stairs(m.plinthStairs(), front.along(), true));
			front.set(md, t0, 4, 1, Kit.slab(m.plinthSlab(), true));
			front.set(md, t0 + 1, 4, 1, Kit.slab(m.plinthSlab(), true));
			// the spandrels either side of the arch: small scoria (the lighter infill's fixed place)
			for (int y = 4; y <= 5; y++) {
				front.set(md, t0 - 1, y, 0, m.second());
				front.set(md, t0 + 2, y, 0, m.second());
			}
			md.mark(Model.Cause.FURNACE, front.x(t0, 0), 1, front.z(t0, 0), 3);
		}
		for (int t = 0; t < front.n(); t += 5)
			for (int y = 3; y <= E - 1; y++)
				front.set(md, t, y, 1, m.pier());
		// the other faces' piers every 5 (back) and the gables' corners
		for (int t = 0; t < back.n(); t += 5)
			for (int y = 3; y <= E - 1; y++)
				back.set(md, t, y, 1, m.pier());
		for (Face f : new Face[] { east, west })
			for (int t : new int[] { 0, f.n() - 1 })
				for (int y = 3; y <= E - 1; y++)
					f.set(md, t, y, 1, m.pier());
		// windows high on the back and gables (the hall's light), 1 x 2 with sills
		for (int t = 2; t < back.n() - 1; t += 5) {
			Kit.window(md, m, back, t + 1, 4, 2);
			back.set(md, t + 1, 4, -1, Masonry.window().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_AXIS, Direction.Axis.Z));
			back.set(md, t + 1, 5, -1, Masonry.window().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_AXIS, Direction.Axis.Z));
		}
		// the corbel table under the eaves (corbels on alternate blocks, slabs between)
		for (Face f : all)
			Kit.corbels(md, m, f, E, true, 1, f.n() - 2);
		// ---- the roof: a 1:2 slate pitch from eaves 2 out (the corbel table is 1 proud), the monitor along the ridge
		int zE = -2, zF = D + 1;
		int mz0 = D / 2 - 3, mz1 = D / 2 + 2; // the monitor's sides
		for (int x = -1; x <= L; x++)
			for (int z = zE; z <= zF; z++) {
				int h = Kit.pitchHalf(z, zE, zF, E);
				boolean gable = x == 0 || x == L - 1, barge = x == -1 || x == L;
				if (z > mz0 && z < mz1)
					continue; // open under the monitor
				if (gable && z >= 0 && z <= D - 1) {
					Kit.gableFill(md, x, z, h, E + 1, m.second(), m.dressSlab());
					md.set(x, h / 2, z, h % 2 == 1 ? m.second() : Kit.slab(Masonry.slateSlab(), false));
					continue;
				}
				Kit.pitchSlab(md, x, z, h, barge ? Masonry.walnutSlab() : Masonry.slateSlab());
				if (!barge && (z == 0 || z == D - 1 || z == 1 || z == D - 2) && h % 2 == 1)
					md.set(x, h / 2, z, Masonry.slateFull());
			}
		// the monitor: louvred sides 2 high (walnut trapdoors) on the slate, end walls of small scoria, its own low slate roof
		int sy = Kit.pitchHalf(mz0, zE, zF, E) / 2 + 1;
		for (int x = 0; x <= L - 1; x++) {
			for (int y = sy; y <= sy + 1; y++) {
				boolean end = x == 0 || x == L - 1;
				md.set(x, y, mz0, end ? m.second() : Shed.louvre(Direction.SOUTH));
				md.set(x, y, mz1, end ? m.second() : Shed.louvre(Direction.NORTH));
				if (end)
					for (int z = mz0 + 1; z < mz1; z++)
						md.set(x, y, z, m.second());
			}
			if (x == 0 || x == L - 1)
				for (int z = mz0 + 1; z < mz1; z++)
					md.set(x, sy - 1, z, m.second());
		}
		for (int x = -1; x <= L; x++) {
			boolean barge = x == -1 || x == L;
			BlockState sl = barge ? Masonry.walnutSlab() : Masonry.slateSlab();
			for (int z = mz0 - 1; z <= mz1 + 1; z++)
				md.set(x, sy + 2, z, Kit.slab(sl, true));
			for (int z = mz0 + 1; z < mz1; z++) {
				boolean ridge = z == D / 2 - 1 || z == D / 2;
				md.set(x, sy + 3, z, Kit.slab(sl, ridge));
				if (ridge && !barge)
					md.set(x, sy + 4, z, Masonry.ridge());
			}
		}
		// ---- inside: hearths along the back wall (blast furnaces set in a scoria bench under small arches), crucibles
		for (int k = 0; k < (L - 1) / 5; k++) {
			int x = 2 + 5 * k;
			md.set(x, 0, D - 3, Blocks.BLAST_FURNACE.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
			md.set(x + 1, 0, D - 3, m.wall());
			md.set(x - 1, 0, D - 3, m.wall());
			md.set(x, 1, D - 3, Kit.stairs(m.dressStairs(), Direction.SOUTH, true));
			md.set(x, 0, D / 2, Blocks.CAULDRON.defaultBlockState());
		}
		md.set(5, 0, 4, ModWastes.BROKEN_PRESS.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
		md.set(10, 0, 4, ModWastes.BROKEN_MIXER.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
		// ---- the plaque beside the main door: the door in the heart-side gable, a walnut door 1 x 2 between piers
		int dz = D / 2;
		for (int y = 0; y <= 1; y++)
			for (int d = 2; d >= -1; d--)
				west.remove(md, dz, y, d);
		west.remove(md, dz, 2, 1);
		BlockState door = Blocks.DARK_OAK_DOOR.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.FACING, Direction.EAST);
		md.set(0, 0, dz, door.setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER));
		md.set(0, 1, dz, door.setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
		Shed.plaque(md, -2, dz - 2, Direction.WEST, c.wear().atLeast(Wear.DAMAGED));
		// ---- the chimney at the heart end, the blast furnace at the outer end
		chimney(md, m, -7, D / 2 - 2, 21, c.wear() != Wear.RUINED || true);
		blastFurnace(md, m, c, L + 3, D / 2 - 2);
		// ---- the state
		damage(md, c, front);
		return md;
	}

	/**
	 * A chimney with its 5 x 5 plinth at (x0..x0+4, z0..z0+4): the shaft 3 x 3 to y {@code top}, its flue open, a stair
	 * set-off at a third, hoops every 5, the corbelled cap, its top three courses soot (ragged at the lower edge), an andesite
	 * ladder up its west face. Returns nothing; marks its top as the soot's cause.
	 */
	static void chimney(Model md, Masonry m, int x0, int z0, int top, boolean ladder) {
		for (int x = x0; x <= x0 + 4; x++)
			for (int z = z0; z <= z0 + 4; z++)
				for (int y = 0; y <= 1; y++)
					md.set(x, y, z, m.plinth());
		int cx = x0 + 2, cz = z0 + 2, setOff = 2 + (top - 2) / 3;
		for (int y = 2; y <= top; y++)
			for (int x = cx - 1; x <= cx + 1; x++)
				for (int z = cz - 1; z <= cz + 1; z++) {
					if (x == cx && z == cz)
						continue;
					boolean hoop = y % 5 == 0 && y < top - 3;
					md.set(x, y, z, hoop ? Masonry.hoop() : m.wall());
				}
		// the plinth's top stepped in (a stair course) and the set-off a third up: stairs 1 proud, sloping in
		ring(md, cx, cz, 2, 2, (x, z, f) -> Kit.stairs(m.plinthStairs(), f, false));
		ring(md, cx, cz, 2, setOff, (x, z, f) -> Kit.stairs(m.wall() == m.second() ? m.dressStairs() : m.dressStairs(), f, false));
		// the corbelled cap: upside-down stairs 1 proud, a band of polished cut scorchia over them
		ring(md, cx, cz, 2, top - 1, (x, z, f) -> Kit.stairs(m.dressStairs(), f, true));
		ring(md, cx, cz, 2, top, (x, z, f) -> m.plinth());
		for (int x = cx - 1; x <= cx + 1; x++)
			for (int z = cz - 1; z <= cz + 1; z++)
				if (!(x == cx && z == cz))
					md.set(x, top, z, m.plinth());
		// soot in the top three courses of the shaft (smoke, not war), ragged at the lower edge
		for (int y = top - 3; y <= top - 1; y++)
			for (int x = cx - 1; x <= cx + 1; x++)
				for (int z = cz - 1; z <= cz + 1; z++)
					if (!(x == cx && z == cz) && (y > top - 3 || Kit.hash(x, y, z, 31) % 2 == 0) && md.get(x, y, z) != null
						&& !(md.get(x, y, z).getBlock() instanceof net.minecraft.world.level.block.StairBlock))
						md.set(x, y, z, y == top - 1 ? Masonry.sootPlain() : Masonry.soot());
		md.mark(Model.Cause.CHIMNEY, cx, top, cz, 4);
		if (ladder) {
			BlockState l = CityPlan.with(CityPlan.block("create:andesite_ladder", Blocks.LADDER), "facing", "west");
			for (int y = 2; y <= top - 3; y++)
				if (y != setOff)
					md.set(cx - 2, y, cz, l);
		}
	}

	interface Ring {
		BlockState at(int x, int z, Direction in);
	}

	/** A ring r out from (cx, cz) at y: each cell its block, given which way is in (corners face along the ring). */
	static void ring(Model md, int cx, int cz, int r, int y, Ring b) {
		for (int x = cx - r; x <= cx + r; x++)
			for (int z = cz - r; z <= cz + r; z++) {
				boolean ex = Math.abs(x - cx) == r, ez = Math.abs(z - cz) == r;
				if (!ex && !ez)
					continue;
				Direction in;
				if (ex && ez)
					in = x < cx && z < cz ? Direction.EAST : x > cx && z < cz ? Direction.SOUTH : x > cx ? Direction.WEST : Direction.NORTH;
				else if (ez)
					in = z < cz ? Direction.SOUTH : Direction.NORTH;
				else
					in = x < cx ? Direction.EAST : Direction.WEST;
				md.set(x, y, z, b.at(x, z, in));
			}
	}

	/**
	 * The blast furnace at (x0..x0+4, z0..z0+4): 5 x 5 to y 6, a stair batter to 3 x 3, hooped every 3, to y 17; the bustle
	 * pipe and its tuyeres; the tap hole toward the casting floor (north) with its soot fan; the charging deck on top on
	 * weathered-iron brackets with a ladder; the void-alloy launder from the tap hole, clamped to the bottom hoop.
	 */
	static void blastFurnace(Model md, Masonry m, Ctx c, int x0, int z0) {
		int cx = x0 + 2, cz = z0 + 2, top = 17;
		for (int y = 0; y <= 6; y++)
			for (int x = x0; x <= x0 + 4; x++)
				for (int z = z0; z <= z0 + 4; z++)
					md.set(x, y, z, y == 3 || y == 6 ? Masonry.hoop() : y == 0 ? m.plinth() : m.wall());
		ring(md, cx, cz, 2, 7, (x, z, f) -> Kit.stairs(m.dressStairs(), f, false));
		for (int y = 7; y <= top; y++)
			for (int x = cx - 1; x <= cx + 1; x++)
				for (int z = cz - 1; z <= cz + 1; z++)
					md.set(x, y, z, y % 3 == 0 ? Masonry.hoop() : x == cx && z == cz && y > 8 ? m.second() : m.wall());
		// the bustle pipe at y 4 round the stack (on the weathered-iron hoop clamps at y 3), four downcomers into the base
		BlockState pipe = CityPlan.block("create:fluid_pipe", Blocks.CHAIN);
		for (int x = x0 - 1; x <= x0 + 5; x++)
			for (int z = z0 - 1; z <= z0 + 5; z++)
				if (x == x0 - 1 || x == x0 + 5 || z == z0 - 1 || z == z0 + 5)
					md.set(x, 4, z, pipe);
		for (int[] dc : new int[][] { { x0, z0 - 1 }, { x0 + 5, cz + 1 }, { cx - 1, z0 + 5 }, { x0 - 1, cz - 1 } }) {
			md.set(dc[0], 3, dc[1], pipe);
			md.set(dc[0], 2, dc[1], pipe);
		}
		for (int[] cl : new int[][] { { x0 - 1, z0 + 1 }, { x0 + 5, z0 + 3 }, { x0 + 3, z0 + 5 } })
			md.set(cl[0], 3, cl[1], Masonry.hoop());
		// the tap hole at y 2 on the casting floor side (raised from the brief's y 0 so the launder can stand on its legs:
		// progress doc, "The building kit"); the soot fan above it (heat)
		md.remove(cx, 2, z0);
		md.remove(cx, 2, z0 + 1);
		Face tf = new Face(x0 + 4, z0, Direction.NORTH, 5);
		md.mark(Model.Cause.FURNACE, cx, 2, z0, 4);
		Kit.sootFan(md, tf, 2, 3, 1, 2, 3, c.seed() + 7, Model.Cause.FURNACE);
		// the launder: from the tap north, heat-tinted scarred plates first, then slabs a half block lower a block on wall legs,
		// clamped to the bottom hoop either side of the tap by two Clamp Brackets
		md.set(cx, 1, z0 - 1, Kit.scarred(ScarredPlateBlock.Scar.HEAT, Direction.WEST));
		md.set(cx, 0, z0 - 1, Kit.vaWall());
		md.set(cx, 1, z0 - 2, Kit.scarred(ScarredPlateBlock.Scar.HEAT, Direction.EAST));
		md.set(cx, 1, z0 - 3, Kit.vaSlab(true));
		md.set(cx, 1, z0 - 4, Kit.vaSlab(false));
		md.set(cx, 0, z0 - 4, Kit.vaWall());
		md.set(cx, 0, z0 - 5, Kit.vaSlab(true));
		md.set(cx, 0, z0 - 6, Kit.vaSlab(true));
		md.set(cx, 0, z0 - 7, Kit.vaSlab(false));
		md.set(cx, 0, z0 - 8, Kit.vaSlab(false));
		md.set(cx, 0, z0 - 2, Kit.vaWall());
		// the moulds at its end: void-alloy plates with a limb's groove (as the casting floor's)
		for (int x = cx - 1; x <= cx + 1; x++)
			for (int z = z0 - 11; z <= z0 - 9; z++)
				if (!(x == cx && z != z0 - 11))
					md.set(x, 0, z, Kit.plate());
		md.set(cx - 1, 3, z0 - 1, Kit.clampWall(Direction.NORTH));
		md.set(cx + 1, 3, z0 - 1, Kit.clampWall(Direction.NORTH));
		md.mark(Model.Cause.CLAMP, cx - 1, 3, z0, 1);
		md.mark(Model.Cause.CLAMP, cx + 1, 3, z0, 1);
		// the charging deck on top (walnut slabs on weathered-iron brackets) and its ladder up the east face
		for (int x = cx - 2; x <= cx + 2; x++)
			for (int z = cz - 2; z <= cz + 2; z++)
				if (Math.abs(x - cx) == 2 || Math.abs(z - cz) == 2)
					md.set(x, top, z, Kit.slab(Masonry.walnutSlab(), true));
		for (int[] br : new int[][] { { cx - 2, cz }, { cx + 2, cz }, { cx, cz - 2 }, { cx, cz + 2 } })
			md.set(br[0], top - 1, br[1], Masonry.hoop());
		md.set(cx, top + 1, cz, Kit.slab(Masonry.walnutSlab(), false));
		BlockState l = CityPlan.with(CityPlan.block("create:andesite_ladder", Blocks.LADDER), "facing", "east");
		for (int y = 8; y < top; y++)
			md.set(cx + 2, y, cz, l);
		md.mark(Model.Cause.CHIMNEY, cx, top, cz, 3);
	}

	/**
	 * The states: worn, soot over the tap hole and the chimney top only (built with them); damaged, the scorch fan up the
	 * wall from the outer furnace mouth (full soot 2 round it, half to 4, a quarter to 6, up onto the monitor, its louvres
	 * burnt out over that bay) and the second chimney toppled across the street (its stump broken on a stepped diagonal, the
	 * shaft in segments with gaps where it broke landing, its cap at the far end, the stones it hit cracked); ruined, the
	 * hall's outer end down, the furnace stack standing.
	 */
	static void damage(Model md, Ctx c, Face front) {
		if (!c.wear().atLeast(Wear.DAMAGED))
			return;
		Masonry m = c.m();
		if (c.wear() == Wear.DAMAGED) {
			// the scorch fan from the outer mouth (the front runs west from x L-1, so the outer mouth is t 2-3)
			int t0 = 2;
			Kit.sootFan(md, front, t0, 3, 2, 4, 6, c.seed() + 3, Model.Cause.FIRE);
			// the monitor's louvres over that bay burnt out
			int x0 = front.x(t0 + 1, 0), x1 = front.x(t0 - 2, 0);
			for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
				for (int y = 0; y < 30; y++)
					for (int z = 0; z < D; z++)
						if (md.get(x, y, z) != null && md.get(x, y, z).getBlock() instanceof net.minecraft.world.level.block.TrapDoorBlock)
							md.remove(x, y, z);
			// the second chimney, toppled toward the heart (-x) across the street: its stump to 6 on a stepped diagonal
			int sx = -7, sz = D / 2 + 4;
			chimney(md, m, sx, sz, 21, false);
			int cx = sx + 2, cz = sz + 2;
			for (int x = cx - 2; x <= cx + 2; x++)
				for (int z = cz - 2; z <= cz + 2; z++)
					for (int y = 2; y <= 22; y++) {
						int keep = 8 - (x - (cx - 1)) - (z > cz ? 1 : 0);
						if (y > keep) {
							md.remove(x, y, z);
							if (y == keep + 1 && md.get(x, keep, z) != null && Math.abs(x - cx) <= 1 && Math.abs(z - cz) <= 1)
								md.mark(Model.Cause.BREAK, x, keep, z, 1);
						}
					}
			for (int x = cx - 1; x <= cx + 1; x++)
				for (int z = cz - 1; z <= cz + 1; z++) {
					int keep = 8 - (x - (cx - 1)) - (z > cz ? 1 : 0);
					if (md.get(x, keep, z) != null && (x + z) % 2 == 0)
						md.set(x, keep, z, m.spalled());
				}
			// the fallen shaft lying west, in segments with 1-block gaps, its cap at the far end
			int len = FALLEN;
			for (int i = 0; i < len; i++) {
				if (i == 2)
					continue;
				int x = sx - 1 - i;
				for (int z = cz - 1; z <= cz + 1; z++)
					for (int y = 0; y <= 2; y++) {
						if (z == cz && y == 1)
							continue;
						md.set(x, y, z, i % 5 == 2 && y != 1 ? Masonry.hoop() : m.wall());
					}
			}
			int capX = sx - 1 - len;
			for (int z = cz - 1; z <= cz + 1; z++)
				for (int y = 0; y <= 2; y++)
					if (!(z == cz && y == 1))
						md.set(capX, y, z, y == 2 ? Masonry.soot() : m.plinth());
			// the stones it hit, cracked (the street's paving in the city; its own landing blocks here)
			for (int i : new int[] { 0, 3 }) {
				md.mark(Model.Cause.FALL, sx - 1 - i, 0, cz, 1);
				md.set(sx - 1 - i, 0, cz - 1, m.spalled());
			}
			return;
		}
		// ruined: the hall's outer end down on a stepped diagonal, its roof and monitor gone, the furnace standing
		for (var en : md.blocks().entrySet()) {
			BlockPos p = en.getKey();
			int x = p.getX(), y = p.getY(), z = p.getZ();
			if (x < -1 || x > L || y <= 1 || z < -2 || z > D + 1)
				continue;
			int top = x < 8 ? E : Math.max(2, E - (x - 7));
			if (y > top || y > E)
				md.remove(x, y, z);
		}
		md.settle();
		Kit.rubbleCone(md, m, Kit.east(L, D), 2, D - 3, 30, c.seed());
		for (int x = 8; x < L - 1; x += 2)
			md.mark(Model.Cause.BREAK, x, Math.max(2, E - (x - 7)), 0, 1);
	}

	/** Its interior's floor pieces placed whole (none: the hearths and machines are blocks). */
	static Map<BlockPos, BlockState> none() {
		return Map.of();
	}
}
