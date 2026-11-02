package com.comedymonkey.createaddons.content.wastes.city.kit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.comedymonkey.createaddons.content.wastes.PropPieceBlock;
import com.comedymonkey.createaddons.content.wastes.WindingTowerPieces;
import com.comedymonkey.createaddons.content.wastes.city.CityPlan;
import com.comedymonkey.createaddons.content.wastes.city.kit.Kit.Ctx;
import com.comedymonkey.createaddons.content.wastes.city.kit.Kit.Face;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * D79 (2): a wrecked winding tower, one a city (design/lore/the-three-peoples.md, I and V: over every town stood the
 * winding towers, tall brick drums whose great mainsprings ran motion out along the shafts to every press and loom; on the
 * night of the uprising they were wrecked, their mainsprings let fly and the shafts that fed the Clockwork districts fell
 * still). A makers' building in the Quarters' masonry (the brief's grammar): an octagonal drum of Stained End Stone Brick, 11
 * across and 2 thick, on a two-course cut deepslate plinth with a stair water table and a ground step; a deepslate pier in
 * each of the octagon's eight corners; string courses at y 7 and 13 and a wall plate at 19; a window a stage on each axis
 * flat with its sill and head; a corbel table at y 20 under what's left of its slate cone; a walnut door and its plaque in
 * the front; inside, a ledge course on corbels where the spring's frame sat (D80).
 * <p>
 * The wreck: the roof caved in (the eaves course and a ragged edge of slates left on the corbels, two rafters fallen in
 * as angled beams from the west rim (D81: the Fallen Rafter prop, {@link WindingTowerPieces}), slates and timber on the
 * floor); the great mainspring sprung off its arbor and out through the west wall,
 * torn open round it (D80: bigger than the strip, each wall layer its own ragged opening, bricks bowed out over it, spalled
 * stone round it, the stones it threw on the ground), peeling off the wall in an arc down to the stones, and standing off
 * the wall as a sprung spiral on its edge, its last turn sagging along the ground (D81, D82: the Sprung Mainspring prop,
 * {@link WindingTowerPieces}, its pieces laid by {@link #props}); the barrel's great wheel and its bevel still on the
 * arbor; the drive shaft down to the floor gearbox snapped, and the two line shafts out through the plinth snapped with it.
 * Local x 0..10 across the
 * front (the outer end at the high x), z 0..10 back from the front (z 0), the spring on the west (heart) side. The ring adds
 * the war: worn, a strike on an outward flat; damaged, a second strike and the top broken on an irregular slant down to the
 * outer end (D80: bites and stubs, the inner layer broken lower than the outer), a heap of brick, slab and timber under it;
 * ruined, a stub.
 */
public final class WindingTower {
	private WindingTower() {}

	/** Across (x and z 0..10), the middle, the wall plate's course, the arbor's height, the inner ledge's course. */
	public static final int N = 11, C = 5, TOP = 19, AY = 10, LEDGE = AY - 2;
	/** The spring's two courses of width (z), the arbor's run (z) between the wall's inner faces. */
	public static final int RZ0 = 3, RZ1 = 4;
	/** The height the strip goes out through the west wall at. */
	static final int OUT_Y = AY - 1;

	public static boolean inDrum(int x, int z) {
		int dx = Math.abs(x - C), dz = Math.abs(z - C);
		return dx <= 5 && dz <= 5 && dx + dz <= 7;
	}

	public static boolean inHollow(int x, int z) {
		int dx = Math.abs(x - C), dz = Math.abs(z - C);
		return dx <= 3 && dz <= 3 && dx + dz <= 5;
	}

	static boolean isWall(int x, int z) {
		return inDrum(x, z) && !inHollow(x, z);
	}

	/** A wall cell with the outside beside it (the outer layer). */
	static boolean outer(int x, int z) {
		if (!isWall(x, z))
			return false;
		for (Direction d : Direction.Plane.HORIZONTAL)
			if (!inDrum(x + d.getStepX(), z + d.getStepZ()))
				return true;
		return false;
	}

	/** The eight corner notches (where a flat meets a stepped diagonal): the piers stand in them. */
	static final int[][] PIERS = { { 2, 0 }, { 8, 0 }, { 10, 2 }, { 10, 8 }, { 8, 10 }, { 2, 10 }, { 0, 8 }, { 0, 2 } };

	static boolean pier(int x, int z) {
		for (int[] p : PIERS)
			if (p[0] == x && p[1] == z)
				return true;
		return false;
	}

	static BlockState shaft(Direction.Axis a) {
		return CityPlan.with(CityPlan.block("create:shaft", Blocks.CHAIN), "axis", a.getName());
	}

	static BlockState cog(Direction.Axis a) {
		return CityPlan.with(CityPlan.block("create:cogwheel", Blocks.CHAIN), "axis", a.getName());
	}

	static BlockState largeCog(Direction.Axis a) {
		return CityPlan.with(CityPlan.block("create:large_cogwheel", Blocks.CHAIN), "axis", a.getName());
	}

	/** Whether a block is one of the winding tower's prop pieces (the Sprung Mainspring's or a Fallen Rafter). */
	public static boolean isProp(BlockState s) {
		return s.getBlock() instanceof PropPieceBlock;
	}

	/** Whether a block is one of the Sprung Mainspring's pieces. */
	public static boolean isSprung(BlockState s) {
		for (int i = 0; i < WindingTowerPieces.SPRING_PIECES; i++)
			if (s.is(ModWastes.WINDING_TOWER_PIECES.get(i).get()))
				return true;
		return false;
	}

	/** A prop piece facing north, as the tower lays it in its own frame. */
	static BlockState piece(int i) {
		return ModWastes.WINDING_TOWER_PIECES.get(i).get().defaultBlockState();
	}

	/** Whether a cell is one the sprung spring's strip passes through. */
	static boolean sprung(Set<Long> cells, int x, int y, int z) {
		return cells.contains(Model.key(x, y, z));
	}

	/** The cells the spring's strip passes through as the ring lays it (the fallen peel's in the ruined ring only). */
	static Set<Long> sprungCells(Wear wear) {
		Set<Long> out = new HashSet<>();
		int[] c = WindingTowerPieces.SPRING_CELLS;
		for (int i = 0; i < c.length; i += 4)
			if (!fallen(c[i + 3]) || wear == Wear.RUINED)
				out.add(Model.key(c[i], c[i + 1], c[i + 2]));
		return out;
	}

	/** Whether a laying ({@link WindingTowerPieces#LAYS}) is the ruined ring's fallen peel's. */
	static boolean fallen(int lay) {
		return lay >= WindingTowerPieces.SPRING_LAYS && lay < WindingTowerPieces.SPRING_LAYS + WindingTowerPieces.FALLEN_LAYS;
	}

	/** Every cell the tower's Sprung Mainspring pieces reach in a built model (the prop's whole strip, for the checks). */
	public static List<BlockPos> sprungReach(Model md) {
		List<BlockPos> out = new ArrayList<>();
		for (var e : md.blocks().entrySet())
			if (isSprung(e.getValue()))
				out.addAll(PropPieceBlock.reach(e.getKey(), e.getValue()));
		return out;
	}

	public static Model build(Ctx c) {
		Model md = new Model();
		Masonry m = c.m();
		Face front = new Face(7, 0, Direction.NORTH, 5), east = new Face(10, 7, Direction.EAST, 5), back = new Face(3, 10, Direction.SOUTH, 5),
			west = new Face(0, 3, Direction.WEST, 5);
		// ---- the drum: 2 thick, its plinth courses in the wall, the string courses and the wall plate in the outer layer
		for (int x = 0; x < N; x++)
			for (int z = 0; z < N; z++) {
				if (!isWall(x, z))
					continue;
				boolean out = outer(x, z);
				for (int y = 0; y <= TOP; y++)
					md.set(x, y, z, y <= 1 ? m.plinth() : out && (y == 7 || y == 13 || y == TOP) ? m.band() : m.wall());
			}
		// ---- the heavy plinth round it: two courses 1 proud, a stair water table, a ground step 2 proud
		for (int x = 0; x < N; x++)
			for (int z = 0; z < N; z++) {
				if (!outer(x, z))
					continue;
				for (Direction d : Direction.Plane.HORIZONTAL) {
					int ox = x + d.getStepX(), oz = z + d.getStepZ();
					if (inDrum(ox, oz))
						continue;
					md.set(ox, 0, oz, m.plinth());
					md.set(ox, 1, oz, m.plinth());
					md.setIfEmpty(ox, 2, oz, Kit.stairs(m.plinthStairs(), d.getOpposite(), false));
					int sx = ox + d.getStepX(), sz = oz + d.getStepZ();
					if (!md.has(sx, 0, sz))
						md.set(sx, 0, sz, Kit.stairs(m.plinthStairs(), d.getOpposite(), false));
				}
			}
		// ---- the piers in the eight corner notches, from the water table to the wall plate
		for (int[] p : PIERS) {
			md.set(p[0], 0, p[1], m.plinth());
			md.set(p[0], 1, p[1], m.plinth());
			for (int y = 2; y <= TOP; y++)
				md.set(p[0], y, p[1], m.pier());
		}
		// ---- a window a stage on each axis flat (1 x 2, its sill 1 proud, a dressed head), through both layers
		for (Face f : new Face[] { front, east, back, west })
			for (int y0 : new int[] { 4, 10, 16 }) {
				if (f == front && y0 == 4)
					continue; // the door's stage
				Kit.window(md, m, f, 2, y0, 2);
				f.set(md, 2, y0 + 2, 0, m.band());
				for (int y = y0; y < y0 + 2; y++)
					f.remove(md, 2, y, -1);
			}
		// ---- the door: a walnut door in the front, the plinth cut for it, a dressed head; the plaque beside it
		BlockState door = Blocks.DARK_OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, front.in());
		front.set(md, 2, 0, 0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
		front.set(md, 2, 1, 0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
		front.remove(md, 2, 0, 1);
		front.remove(md, 2, 1, 1);
		front.set(md, 2, 2, 0, m.band());
		for (int y = 0; y <= 1; y++)
			front.remove(md, 2, y, -1);
		Shed.plaque(md, front.x(3, 2), front.z(3, 2), Direction.NORTH, c.wear().atLeast(Wear.DAMAGED));
		// ---- the corbel table at y 20: upside-down dressing stairs round the outer ring, slabs between on the flats
		List<int[]> ring = new ArrayList<>();
		for (int x = -1; x <= N; x++)
			for (int z = -1; z <= N; z++) {
				if (inDrum(x, z))
					continue;
				Direction toWall = null;
				for (Direction d : Direction.Plane.HORIZONTAL)
					if (isWall(x + d.getStepX(), z + d.getStepZ()))
						toWall = d;
				if (toWall == null)
					continue;
				ring.add(new int[] { x, z, toWall.get2DDataValue() });
				boolean flat = Math.abs(x - C) <= 2 || Math.abs(z - C) <= 2;
				md.set(x, TOP + 1, z, flat && (x + z) % 2 == 1 ? Kit.slab(m.dressSlab(), true) : Kit.stairs(m.dressStairs(), toWall, true));
			}
		// ---- inside: the ledge the spring's frame sat on, and its corbels
		innerRelief(md, m);
		// ---- the wreck: the roof caved in, the spring let fly, the shafts snapped
		roof(md, ring, c.seed());
		spring(md, m, c.seed(), c.wear());
		drive(md);
		// ---- the war, by the ring
		damage(md, c, front, east);
		// ---- the props (their pieces stand as they fell, whatever the ring took from the walls)
		props(md, c.wear());
		return md;
	}

	/**
	 * D80: the drum's inner face carries a ledge course at {@link #LEDGE} all round (upside-down dressing stairs, their
	 * backs to the wall), on a corbel under every other one: where the frame of the spring's barrel sat.
	 */
	static void innerRelief(Model md, Masonry m) {
		for (int x = 0; x < N; x++)
			for (int z = 0; z < N; z++) {
				if (!inHollow(x, z))
					continue;
				Direction toWall = null;
				for (Direction d : Direction.Plane.HORIZONTAL)
					if (toWall == null && isWall(x + d.getStepX(), z + d.getStepZ()))
						toWall = d;
				if (toWall == null)
					continue;
				md.set(x, LEDGE, z, Kit.stairs(m.dressStairs(), toWall, true));
				if ((x + z) % 2 == 0)
					md.set(x, LEDGE - 1, z, Kit.stairs(m.dressStairs(), toWall, true));
			}
	}

	/**
	 * What's left of the slate cone: its eaves course on the corbels all round but where it tore away over the spring, a
	 * second and third course on parts of the ring (the edge ragged where the cone broke from them, by the angle round the
	 * drum, not by chance), the rest of it on the floor (the two rafters fallen in from the west rim are props: {@link #props}).
	 */
	static void roof(Model md, List<int[]> ring, long seed) {
		BlockState slate = Masonry.slateStairs(), brick = ModWastes.STAINED_END_STONE_BRICKS.get().defaultBlockState();
		for (int[] r : ring) {
			int x = r[0], z = r[1];
			Direction in = Direction.from2DDataValue(r[2]);
			double a = Math.atan2(z - C, x - C);
			// the eaves course: gone over the west side, where the spring went out and the cone first gave
			if (Math.cos(a) > -0.85)
				md.set(x, TOP + 2, z, Kit.stairs(slate, in, false));
		}
		// the next two courses, on the outer and inner wall layers, kept on arcs of the ring (the cone tore away elsewhere)
		for (int x = 0; x < N; x++)
			for (int z = 0; z < N; z++) {
				if (!isWall(x, z))
					continue;
				double a = Math.toDegrees(Math.atan2(z - C, x - C));
				Direction in = Math.abs(x - C) >= Math.abs(z - C) ? (x > C ? Direction.WEST : Direction.EAST) : (z > C ? Direction.NORTH : Direction.SOUTH);
				boolean course2 = a > -150 && a < 60, course3 = a > -110 && a < 10;
				// the wall's top under the eaves (the cone's seat), then each course a block in and a block up
				if (outer(x, z)) {
					md.set(x, TOP + 1, z, brick);
					if (course2) {
						md.set(x, TOP + 2, z, Masonry.slateFull());
						md.set(x, TOP + 3, z, Kit.stairs(slate, in, false));
					}
				} else if (course3) {
					md.set(x, TOP + 1, z, brick);
					md.set(x, TOP + 2, z, brick);
					md.set(x, TOP + 3, z, Masonry.slateFull());
					md.set(x, TOP + 4, z, Kit.stairs(slate, in, false));
				}
			}
		// the rest on the floor: slates and timber in a heap over the front half of the floor
		int i = 0;
		for (int x = 2; x <= 8; x++)
			for (int z = 2; z <= 5; z++) {
				if (!inHollow(x, z) || z == RZ0 && x == C)
					continue;
				int h = Kit.hash(x, 0, z, seed + 3);
				BlockState s = h % 5 == 0 ? Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, (h & 8) == 0 ? Direction.Axis.X : Direction.Axis.Z)
					: h % 5 == 1 ? Kit.slab(Masonry.walnutSlab(), false)
					: h % 5 == 2 ? Kit.stairs(slate, Direction.from2DDataValue(h % 4), false) : Kit.slab(Masonry.slateSlab(), false);
				md.setIfEmpty(x, 0, z, s);
				if (h % 7 == 0 && i++ < 4)
					md.setIfEmpty(x, 1, z, Kit.slab(Masonry.slateSlab(), false));
			}
	}

	/**
	 * The mainspring: on its arbor (a shaft across the drum at y {@link #AY}), its great wheel on it; the strip's turn left
	 * round it, out west through the wall at y {@link #OUT_Y}, the run peeling off the wall, and the sprung spiral standing off
	 * it (the Sprung Mainspring's pieces, laid by {@link #props}). The hole (D80): torn, bigger than the strip, the outer layer's opening wider and taller than
	 * the inner's and each column's edge its own, bricks bowed out round the lip, the stone round it spalled; the stones it
	 * threw lie at the foot.
	 */
	static void spring(Model md, Masonry m, long seed, Wear wear) {
		// the arbor, its great wheel on it (the barrel's)
		for (int z = 2; z <= 8; z++)
			md.set(C, AY, z, shaft(Direction.Axis.Z));
		md.set(C, AY, 6, largeCog(Direction.Axis.Z));
		Set<Long> cells = sprungCells(wear);
		// the hole: each layer's opening round the strip (z 3, 4 at OUT_Y), the inner layer's (x 1) a block smaller each way
		// than the outer's (x 0, and the sill line outside it at x -1), each column's top and bottom its own
		List<BlockState> thrown = new ArrayList<>();
		// the hole's cells (whatever stood in them), each a mark: the cracked stone lies within two of one
		Set<Long> hole = new HashSet<>();
		for (int x = 1; x >= -1; x--)
			for (int z = 0; z <= 7; z++) {
				int off = Math.abs(2 * z - (RZ0 + RZ1)); // 1 at the strip, 3 a block off, 5, 7
				int half = x == 1 ? (off <= 1 ? 2 : off <= 3 ? 1 : -1) : (off <= 1 ? 3 : off <= 3 ? 2 : off <= 5 ? 1 : -1);
				if (half < 0)
					continue;
				int h = Kit.hash(x, 0, z, seed + 11);
				int y0 = OUT_Y - half - h % 2, y1 = OUT_Y + half + (h >> 3) % 2 + (x < 1 && off <= 3 ? 1 : 0);
				for (int y = y0; y <= y1; y++) {
					if (x >= 0) {
						hole.add(Model.key(x, y, z));
						md.mark(Model.Cause.SPRUNG, x, y, z, 1);
					}
					BlockState s = md.get(x, y, z);
					if (s == null)
						continue;
					if (x >= 0)
						thrown.add(s);
					md.remove(x, y, z);
				}
			}
		// the lip: bricks bowed out of the outer layer round the opening (a lip stair over it, a cracked brick at its sides;
		// D81: none pushed out on its sill, where they stacked into a stair), only where the wall behind still stands
		BlockState lip = Kit.stairs(ModWastes.STAINED_END_STONE_BRICK_STAIRS.get().defaultBlockState(), Direction.EAST, true);
		for (int z = 1; z <= 7; z++)
			for (int y = OUT_Y - 5; y <= OUT_Y + 6; y++) {
				if (!md.has(0, y, z) || md.has(-1, y, z) || sprung(cells, -1, y, z) || Kit.hash(0, y, z, seed + 13) % 2 != 0)
					continue;
				boolean below = !md.has(0, y - 1, z) && y - 1 > OUT_Y - 5,
					side = hole.contains(Model.key(0, y, z - 1)) && z - 1 >= 2 || hole.contains(Model.key(0, y, z + 1)) && z + 1 <= 6;
				if (below)
					md.set(-1, y, z, lip);
				else if (side)
					md.set(-1, y, z, m.spalled());
			}
		// the stone round it spalled, two blocks out on the outer layer and one on the inner
		for (int x = 0; x <= 1; x++)
			for (int z = -1; z <= 8; z++)
				for (int y = OUT_Y - 5; y <= OUT_Y + 6; y++) {
					BlockState s = md.get(x, y, z);
					if (s == null || !s.is(ModWastes.STAINED_END_STONE_BRICKS.get()) || Kit.hash(x, y, z, seed) % 3 == 0)
						continue;
					int reach = x == 0 ? 2 : 1;
					boolean near = false;
					for (int dz = -reach; dz <= reach && !near; dz++)
						for (int dy = -reach; dy <= reach && !near; dy++)
							near = hole.contains(Model.key(x, y + dy, z + dz)) && y + dy > 1 && z + dz >= 1 && z + dz <= 7 && Math.abs(y + dy - OUT_Y) <= 4;
					if (near)
						md.set(x, y, z, m.spalled());
				}
		// the ledge and corbels inside where the wall behind them went with the hole
		for (int z = 1; z <= 7; z++)
			for (int y = LEDGE - 1; y <= LEDGE; y++) {
				BlockState s = md.get(2, y, z);
				if (s != null && s.getBlock() instanceof StairBlock && !md.has(1, y, z))
					md.remove(2, y, z);
			}
		// the stones it threw, on the ground at the foot (a few; the rest broke up): first a pile where the peeling run comes down
		// and the spiral's sag ends (D81: where the strip changes its angle, hidden), then scattered clear of the strip
		md.mark(Model.Cause.FALL, -6, 0, C, 4);
		BlockState slab = Kit.slab(ModWastes.STAINED_END_STONE_BRICK_SLAB.get().defaultBlockState(), false);
		md.set(-8, 0, RZ0, m.wall());
		md.set(-8, 0, RZ1, m.spalled());
		md.set(-7, 0, RZ0, m.wall());
		md.set(-7, 0, RZ1, slab);
		// (the ruined ring's fallen peel lies across the pile's top: no slab on it there)
		if (wear != Wear.RUINED)
			md.set(-8, 1, RZ0, slab);
		int put = 5, want = Math.max(6, thrown.size() / 3);
		// (no farther out than the pile: past x -8 is the city's street)
		for (int x = -5; x >= -8 && put < want; x--)
			for (int z = 0; z <= 9 && put < want; z++) {
				int h = Kit.hash(x, 0, z, seed + 7);
				if (h % 3 == 0 || md.has(x, 0, z) || sprung(cells, x, 0, z))
					continue;
				md.set(x, 0, z, h % 3 == 1 ? slab : h % 2 == 0 ? m.spalled() : m.wall());
				put++;
			}
	}

	/**
	 * The props (D80-D87, {@link WindingTowerPieces}): the Sprung Mainspring's pieces and the two Fallen Rafters, each a block
	 * whose model runs on through the blocks round its own (its reach; the kit's checks count a prop standing in all of it,
	 * so a prop on the ground at one cell holds all its pieces). Each is laid at its own block; a piece whose block is already
	 * taken isn't laid. The ruined ring keeps the spiral on the ground and the peel's lower end, fallen with the wall across
	 * the stones; its other pieces and its rafters came down with the walls. The pieces can be turned but not mirrored: a
	 * mirrored site fails loudly.
	 */
	static void props(Model md, Wear wear) {
		boolean ruined = wear == Wear.RUINED;
		layProps(md, 0, WindingTowerPieces.SPRING_LAYS, id -> !ruined || id.contains("_spiral_"));
		if (ruined)
			layProps(md, WindingTowerPieces.SPRING_LAYS, WindingTowerPieces.SPRING_LAYS + WindingTowerPieces.FALLEN_LAYS, id -> true);
		else
			layProps(md, WindingTowerPieces.SPRING_LAYS + WindingTowerPieces.FALLEN_LAYS, WindingTowerPieces.LAYS.length, id -> true);
	}

	/** Lays {@link WindingTowerPieces#LAYS} from..to whose piece ids {@code keep} keeps, as one city piece. */
	static void layProps(Model md, int from, int to, Predicate<String> keep) {
		Map<BlockPos, BlockState> holds = new LinkedHashMap<>();
		for (int i = from; i < to; i++) {
			WindingTowerPieces.Lay l = WindingTowerPieces.LAYS[i];
			if (keep.test(WindingTowerPieces.PIECES[l.piece()].id()) && !md.has(l.x(), l.y(), l.z()))
				holds.put(new BlockPos(l.x(), l.y(), l.z()), piece(l.piece()));
		}
		md.special(holds, site -> {
			if (site.mirrored())
				throw new IllegalStateException("the winding tower's props can be turned but not mirrored: lay the tower in an unmirrored site");
			holds.forEach((p, state) -> site.put(p.getX(), p.getY(), p.getZ(), state));
		});
	}

	/**
	 * The drive: under the great wheel a bevel (a large cogwheel lying flat) turned the drive shaft down to the floor gearbox,
	 * which sent the motion out through the plinth on both sides as the town's line shafts. All snapped: the drive shaft broken
	 * a third of the way up (its top hanging from the bevel), each line shaft broken a block from the gearbox, the pieces on
	 * the floor.
	 */
	static void drive(Model md) {
		md.set(C, AY - 1, 7, largeCog(Direction.Axis.Y));
		for (int y = 1; y <= AY - 2; y++)
			if (y != 5 && y != 6)
				md.set(C, y, 7, shaft(Direction.Axis.Y));
		md.set(C, 0, 7, CityPlan.with(CityPlan.block("create:gearbox", Blocks.CHAIN), "axis", "z"));
		for (int x = 0; x < N; x++)
			if (x != C && x != 3 && x != 7)
				md.set(x, 0, 7, shaft(Direction.Axis.X));
		md.set(6, 0, 5, shaft(Direction.Axis.Z));
		md.set(3, 0, 6, shaft(Direction.Axis.X));
		md.set(7, 0, 8, shaft(Direction.Axis.Z));
	}

	/**
	 * Where the damaged ring's break leaves a column's top (D80): high on the heart side (x 0..5, whole), falling about 1.3
	 * a block to the outer end; the piers standing proud of it as stubs, bites out of the outer layer and the odd stub of it,
	 * the inner layer broken one or three lower than the outer, the ring outside a block under the wall's.
	 */
	static int breakTop(int x, int z, long seed) {
		if (x <= C)
			return 99;
		int t = TOP + 1 - (int) Math.round((x - 4) * 1.3), h = Kit.hash(x, 0, z, seed + 21);
		if (pier(x, z))
			return t + 2 - h % 2;
		if (isWall(x, z) && !outer(x, z))
			return t - 1 - (h % 3 == 0 ? 2 : 0);
		if (!inDrum(x, z))
			return t - 1;
		return h % 5 == 0 ? t - 3 : h % 7 == 1 ? t + 1 : t;
	}

	/**
	 * The war on it, by its ring. Worn: a strike on an outward flat. Damaged: a second strike, and the top broken
	 * ({@link #breakTop}), its broken tops cracked, a heap of what fell under the outer flat. Ruined: a stub, walls on a slant
	 * from 9 at the heart side down to the plinth, the spring's loops left on the ground.
	 */
	static void damage(Model md, Ctx c, Face front, Face east) {
		Masonry m = c.m();
		if (c.wear() == Wear.INTACT)
			return;
		Face struck = c.outward(Direction.EAST) ? east : front;
		if (c.wear() == Wear.WORN || c.wear() == Wear.DAMAGED)
			Kit.strike(md, m, struck, 0, 14, 2, 2, c.seed());
		if (c.wear() == Wear.WORN)
			return;
		boolean damaged = c.wear() == Wear.DAMAGED;
		int fallen = 0;
		for (var en : md.blocks().entrySet()) {
			BlockPos p = en.getKey();
			if (p.getY() <= 1 || p.getX() < -1 || p.getX() > N || p.getZ() < -1 || p.getZ() > N)
				continue;
			int top = damaged ? breakTop(p.getX(), p.getZ(), c.seed()) : Math.max(1, 9 - p.getX());
			if (p.getY() > top) {
				md.remove(p.getX(), p.getY(), p.getZ());
				fallen++;
			}
		}
		// the break line's tops cracked, every other one (its cause)
		for (int x = 0; x < N; x++)
			for (int z = 0; z < N; z++) {
				int t = damaged ? breakTop(x, z, c.seed()) : Math.max(1, 9 - x);
				if (t > TOP || !isWall(x, z))
					continue;
				BlockState s = md.get(x, t, z);
				if (s != null && s.is(m.wall().getBlock()) && (x + z) % 2 == 0)
					md.set(x, t, z, m.spalled());
				md.mark(Model.Cause.BREAK, x, t, z, 1);
			}
		md.settle();
		if (damaged && c.outward(Direction.NORTH))
			Kit.strike(md, m, front, 3, 8, 2, 2, c.seed() + 1);
		heap(md, m, fallen, c.seed() + 2);
	}

	/**
	 * D80: the heap under the outer flat where the top came down, of what it was made of (brick, cracked brick, brick and
	 * walnut slabs, walnut timber), never the plinth's stairs: highest against the plinth, falling away, a slab or a beam on
	 * top; about a third of what fell (the rest broke up), at most 30. It lies over the plinth's ground step, never in it.
	 */
	static void heap(Model md, Masonry m, int fallen, long seed) {
		int want = Math.max(6, Math.min(30, fallen / 3)), put = 0;
		md.mark(Model.Cause.FALL, N + 2, 0, C, 4);
		BlockState brickSlab = Kit.slab(ModWastes.STAINED_END_STONE_BRICK_SLAB.get().defaultBlockState(), false), plank = Kit.slab(Masonry.walnutSlab(), false);
		for (int y = 0; y < 3 && put < want; y++)
			for (int x = N + 1; x <= N + 4 && put < want; x++)
				for (int z = 1; z <= 9 && put < want; z++) {
					double d = Math.hypot((x - N - 0.5) / 3.2, (z - C) / 4.0) + Kit.hash(x, 0, z, seed) % 5 / 20.0;
					int h = d < 0.55 ? 3 : d < 0.85 ? 2 : d < 1.05 ? 1 : 0;
					if (y >= h)
						continue;
					int k = Kit.hash(x, y, z, seed + 1);
					BlockState s = y == h - 1 && k % 3 != 0 ? (k % 2 == 0 ? brickSlab : plank)
						: k % 6 == 0 ? Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, k % 4 < 2 ? Direction.Axis.X : Direction.Axis.Z)
						: k % 4 == 1 ? m.spalled() : m.wall();
					if (md.has(x, y, z))
						continue;
					md.set(x, y, z, s);
					put++;
				}
	}

	/** The highest block of the tower (for the skyline check: it never stands up to the heart's spire). */
	public static int height(Model md) {
		return md.bounds()[4];
	}
}
