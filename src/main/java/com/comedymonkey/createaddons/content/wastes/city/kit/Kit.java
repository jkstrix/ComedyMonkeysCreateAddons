package com.comedymonkey.createaddons.content.wastes.city.kit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.comedymonkey.createaddons.content.wastes.ClampBracketBlock;
import com.comedymonkey.createaddons.content.wastes.ScarredPlateBlock;
import com.comedymonkey.createaddons.content.wastes.SlottedGrilleBlock;
import com.comedymonkey.createaddons.content.wastes.VoidAlloyBandBlock;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The building kit's shared grammar (design/briefs/clockwork-wastes-buildings.md, "Secondary Forms"): the pieces every
 * building is built from, so the same rules hold on every building of a layer. Makers': plinth and ground step, piers,
 * window bays with sills, lintel band, corbel course, eaves, slab-pitch roof, truss. The uprising's: plate fields,
 * pillar corners, bands every 6-7, grille runs, chamfered tops, clamps. The war's: strikes, soot fans, rubble cones,
 * stepped diagonal breaks. Every mark is placed against a recorded cause ({@link Model.Mark}).
 */
public final class Kit {
	private Kit() {}

	/** A building's own facts the pieces need: its stone, its state, which local faces look outward, a seed for hashes. */
	public record Ctx(Masonry m, Wear wear, Set<Direction> outward, long seed) {
		public boolean outward(Direction side) {
			return outward.contains(side);
		}
	}

	// ================================================================ block states

	public static BlockState stairs(BlockState stairs, Direction facing, boolean top) {
		return stairs.setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
	}

	public static BlockState slab(BlockState slab, boolean top) {
		return slab.setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
	}

	public static BlockState plate() {
		return ModWastes.VOID_ALLOY_PLATE.get().defaultBlockState();
	}

	public static BlockState seamed() {
		return ModWastes.SEAMED_VOID_ALLOY_PLATE.get().defaultBlockState();
	}

	public static BlockState pillar(Direction.Axis axis) {
		return ModWastes.VOID_ALLOY_PILLAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
	}

	public static BlockState pillar() {
		return pillar(Direction.Axis.Y);
	}

	public static BlockState band(Direction.Axis axis) {
		return ModWastes.VOID_ALLOY_BAND.get().defaultBlockState().setValue(VoidAlloyBandBlock.AXIS, axis);
	}

	public static BlockState band() {
		return band(Direction.Axis.Y);
	}

	/** An upright grille looking out along {@code look}'s axis. */
	public static BlockState grille(Direction look) {
		return grille(look.getAxis() == Direction.Axis.X ? SlottedGrilleBlock.Orientation.X : SlottedGrilleBlock.Orientation.Z);
	}

	public static BlockState grille(SlottedGrilleBlock.Orientation o) {
		return ModWastes.SLOTTED_GRILLE.get().defaultBlockState().setValue(SlottedGrilleBlock.AXIS, o);
	}

	public static BlockState vaStairs(Direction facing, boolean top) {
		return stairs(ModWastes.VOID_ALLOY_STAIRS.get().defaultBlockState(), facing, top);
	}

	public static BlockState vaSlab(boolean top) {
		return slab(ModWastes.VOID_ALLOY_SLAB.get().defaultBlockState(), top);
	}

	public static BlockState vaWall() {
		return ModWastes.VOID_ALLOY_WALL.get().defaultBlockState();
	}

	/** A wall strap on the face of the block behind it (toward {@code facing}'s opposite), looking out {@code facing}. */
	public static BlockState clampWall(Direction facing) {
		return ModWastes.CLAMP_BRACKET.get().defaultBlockState().setValue(ClampBracketBlock.FACING, facing).setValue(ClampBracketBlock.MOUNT, ClampBracketBlock.Mount.WALL);
	}

	/** A saddle over the top of the block below, its jaws along {@code along}'s axis. */
	public static BlockState clampSaddle(Direction along) {
		return ModWastes.CLAMP_BRACKET.get().defaultBlockState().setValue(ClampBracketBlock.FACING, along).setValue(ClampBracketBlock.MOUNT, ClampBracketBlock.Mount.SADDLE);
	}

	public static BlockState scarred(ScarredPlateBlock.Scar scar, Direction face) {
		return ModWastes.SCARRED_VOID_ALLOY_PLATE.get().defaultBlockState().setValue(ScarredPlateBlock.FACING, face).setValue(ScarredPlateBlock.SCAR, scar);
	}

	public static boolean isVoidAlloy(BlockState s) {
		var b = s.getBlock();
		return b == ModWastes.VOID_ALLOY_PLATE.get() || b == ModWastes.SEAMED_VOID_ALLOY_PLATE.get() || b == ModWastes.VOID_ALLOY_PILLAR.get()
			|| b == ModWastes.VOID_ALLOY_BAND.get() || b == ModWastes.SLOTTED_GRILLE.get() || b == ModWastes.VOID_ALLOY_STAIRS.get()
			|| b == ModWastes.VOID_ALLOY_SLAB.get() || b == ModWastes.VOID_ALLOY_WALL.get() || b == ModWastes.SCARRED_VOID_ALLOY_PLATE.get();
	}

	/** A deterministic hash of a cell (only ever used to break up a band's edge, never to place a mark outside one). */
	public static int hash(int x, int y, int z, long seed) {
		long h = x * 73856093L ^ y * 19349663L ^ z * 83492791L ^ seed * 0x9E3779B97F4A7C15L;
		h ^= h >>> 29;
		h *= 0xBF58476D1CE4E5B9L;
		h ^= h >>> 32;
		return (int) (h & 0x7fffffff);
	}

	// ================================================================ faces

	/**
	 * One wall face of a rectangular building, read as a viewer outside it reads it: {@code t} runs left to right along it
	 * (from its left end, {@code (x0, z0)}), {@code d} out from the wall plane (d 1 is one proud, d -1 one in).
	 */
	public record Face(int x0, int z0, Direction out, int n) {
		public Direction along() {
			return out.getCounterClockWise();
		}

		public int x(int t, int d) {
			return x0 + along().getStepX() * t + out.getStepX() * d;
		}

		public int z(int t, int d) {
			return z0 + along().getStepZ() * t + out.getStepZ() * d;
		}

		public void set(Model m, int t, int y, int d, BlockState s) {
			m.set(x(t, d), y, z(t, d), s);
		}

		public BlockState get(Model m, int t, int y, int d) {
			return m.get(x(t, d), y, z(t, d));
		}

		public void remove(Model m, int t, int y, int d) {
			m.remove(x(t, d), y, z(t, d));
		}

		/** Toward the wall from outside (proud stairs face this way: their high side against the wall). */
		public Direction in() {
			return out.getOpposite();
		}
	}

	/** The four faces of the box x 0..l-1, z 0..d-1: front (z 0, looking north), east, back, west. */
	public static Face[] faces(int l, int d) {
		return new Face[] { new Face(l - 1, 0, Direction.NORTH, l), new Face(l - 1, d - 1, Direction.EAST, d), new Face(0, d - 1, Direction.SOUTH, l),
			new Face(0, 0, Direction.WEST, d) };
	}

	public static Face front(int l, int d) {
		return faces(l, d)[0];
	}

	public static Face east(int l, int d) {
		return faces(l, d)[1];
	}

	public static Face back(int l, int d) {
		return faces(l, d)[2];
	}

	public static Face west(int l, int d) {
		return faces(l, d)[3];
	}

	// ================================================================ the makers' pieces

	/**
	 * The plinth: its course(s) in the wall plane ({@code courses}, y 0 up) and a ground step of the plinth stone's stairs 1
	 * proud along its foot (right way up, the step outward). Heavy buildings (two courses) have the courses 1 proud too and
	 * a stair water table on top, the step 2 proud. Skips t where {@code skip} says (doors).
	 */
	public static void plinth(Model md, Masonry m, Face f, int courses, java.util.function.IntPredicate skip) {
		boolean heavy = courses > 1;
		for (int t = 0; t < f.n(); t++) {
			if (skip.test(t))
				continue;
			for (int y = 0; y < courses; y++) {
				f.set(md, t, y, 0, m.plinth());
				if (heavy)
					f.set(md, t, y, 1, m.plinth());
			}
			if (heavy) {
				f.set(md, t, courses, 1, stairs(m.plinthStairs(), f.in(), false));
				f.set(md, t, 0, 2, stairs(m.plinthStairs(), f.in(), false));
			} else
				f.set(md, t, 0, 1, stairs(m.plinthStairs(), f.in(), false));
		}
	}

	/** A pier (the dressing pillar, 1 proud) at t from y0 to y1, on a plinth-stone base where the ground step runs. */
	public static void pier(Model md, Masonry m, Face f, int t, int y0, int y1, int proud) {
		for (int y = y0; y <= y1; y++)
			f.set(md, t, y, proud, m.pier());
	}

	/**
	 * The corner pier where two faces meet (the L of the two face piers and the block between them), from y0 to y1, its
	 * foot on a plinth block.
	 */
	public static void cornerPier(Model md, Masonry m, int x, int z, Direction out1, Direction out2, int y0, int y1) {
		for (int y = y0; y <= y1; y++) {
			md.set(x + out1.getStepX(), y, z + out1.getStepZ(), m.pier());
			md.set(x + out2.getStepX(), y, z + out2.getStepZ(), m.pier());
			md.set(x + out1.getStepX() + out2.getStepX(), y, z + out1.getStepZ() + out2.getStepZ(), m.pier());
		}
	}

	/** A window 1 wide, from y0 up {@code h}, its sill (an upside-down dressing stair, 1 proud) under it. */
	public static void window(Model md, Masonry m, Face f, int t, int y0, int h) {
		BlockState w = Masonry.window().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_AXIS,
			f.out().getAxis() == Direction.Axis.Z ? Direction.Axis.Z : Direction.Axis.X);
		for (int y = y0; y < y0 + h; y++)
			f.set(md, t, y, 0, w);
		f.set(md, t, y0 - 1, 1, stairs(m.dressStairs(), f.in(), true));
	}

	/** A corbel course along a face at y: upside-down dressing stairs 1 proud; a corbel table if {@code table} (slabs between). */
	public static void corbels(Model md, Masonry m, Face f, int y, boolean table, int from, int to) {
		for (int t = from; t <= to; t++)
			f.set(md, t, y, 1, table && t % 2 == 1 ? slab(m.dressSlab(), true) : stairs(m.dressStairs(), f.in(), true));
	}

	/**
	 * A gable roof's height at depth z, in half blocks (2y + 1 for a top slab at y, 2y for a bottom slab at y): eaves at
	 * {@code zEave} (a top slab at y {@code e}), rising a half block a block (1:2) to the ridge, falling to the far eave at
	 * {@code zFar}.
	 */
	public static int pitchHalf(int z, int zEave, int zFar, int e) {
		return 2 * e + 1 + Math.min(z - zEave, zFar - z);
	}

	/** The slab at a half-block height: a top slab at y = h / 2 when h is odd, a bottom one when even. */
	public static void pitchSlab(Model md, int x, int z, int h, BlockState slab) {
		md.set(x, h / 2, z, slab(slab, h % 2 == 1));
	}

	/** Infill under a pitch at a gable from y0 up to its slab's underside (a slab of {@code half} where the slab is a top one). */
	public static void gableFill(Model md, int x, int z, int h, int y0, BlockState full, BlockState halfSlab) {
		int top = h / 2;
		for (int y = y0; y < top; y++)
			md.set(x, y, z, full);
		if (h % 2 == 1)
			md.set(x, top, z, slab(halfSlab, false));
	}

	// ================================================================ the uprising's pieces

	/**
	 * A chamfered top (design brief: "every top steps in with void-alloy stairs"): a ring of stairs sloping in round the
	 * rectangle x0..x1, z0..z1 at y, the corners outer stairs (each faces along the ring to its neighbour, so the outer
	 * shape forms), the inside filled with {@code inside} (null: left open).
	 */
	public static void chamfer(Model md, int x0, int z0, int x1, int z1, int y, BlockState inside) {
		for (int x = x0; x <= x1; x++)
			for (int z = z0; z <= z1; z++) {
				boolean ex = x == x0 || x == x1, ez = z == z0 || z == z1;
				Direction f;
				if (ex && ez)
					f = x == x0 && z == z0 ? Direction.EAST : x == x1 && z == z0 ? Direction.SOUTH : x == x1 ? Direction.WEST : Direction.NORTH;
				else if (ez)
					f = z == z0 ? Direction.SOUTH : Direction.NORTH;
				else if (ex)
					f = x == x0 ? Direction.EAST : Direction.WEST;
				else {
					if (inside != null)
						md.set(x, y, z, inside);
					continue;
				}
				md.set(x, y, z, vaStairs(f, false));
			}
	}

	// ================================================================ the war's marks

	/**
	 * A soot fan above {@code (sx, sy)} on a face (heat rises): the inner band (to {@code r1}) all soot, the middle band (to
	 * {@code r2}) about half, the fringe (to {@code r3}) a quarter, by distance from the source with the fan widening as it
	 * rises; a hash only breaks up a band's edge. Only blocks already there turn (full blocks to soot bricks, stairs and slabs
	 * to soot stairs and slabs, cracked near the source); void-alloy takes heat tint instead (a heat scar facing out) where
	 * {@code heatOnVoidAlloy}. Depth d 0 (the wall plane) and 1 (proud parts: piers, sills, corbels).
	 */
	public static void sootFan(Model md, Face f, int st, int sy, int r1, int r2, int r3, long seed, Model.Cause cause) {
		md.mark(cause, f.x(st, 0), sy, f.z(st, 0), r3);
		for (int dy = -1; dy <= r3; dy++)
			for (int dt = -r3; dt <= r3; dt++) {
				double dist = Math.sqrt(dt * dt * 1.0 + dy * dy * 0.45);
				// a fan: narrow at the source, widening as it rises
				if (Math.abs(dt) > 1 + dy || dist > r3)
					continue;
				int t = st + dt, y = sy + dy;
				if (t < 0 || t >= f.n() || y < 0)
					continue;
				int band = dist <= r1 ? 0 : dist <= r2 ? 1 : 2;
				int h = hash(f.x(t, 0), y, f.z(t, 0), seed);
				if (band == 1 && h % 2 != 0 || band == 2 && h % 4 != 0)
					continue;
				for (int d = 0; d <= 1; d++) {
					BlockState s = f.get(md, t, y, d);
					if (s == null)
						continue;
					BlockState to = sooted(s, band == 0 && dist <= 1.2);
					if (to != null)
						f.set(md, t, y, d, to);
				}
			}
	}

	/** A block's sooted form, or null if it doesn't take soot (windows, void-alloy, wood). */
	public static BlockState sooted(BlockState s, boolean cracked) {
		if (isVoidAlloy(s) || s.is(ModWastes.BROKEN_MAKERS_WINDOW.get()) || Masonry.isSoot(s))
			return null;
		if (s.getBlock() instanceof StairBlock)
			return Masonry.sootStairs().setValue(StairBlock.FACING, s.getValue(StairBlock.FACING)).setValue(StairBlock.HALF, s.getValue(StairBlock.HALF));
		if (s.getBlock() instanceof SlabBlock)
			return Masonry.sootSlab().setValue(SlabBlock.TYPE, s.getValue(SlabBlock.TYPE));
		if (!s.isCollisionShapeFullBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO))
			return null;
		if (s.getBlock() instanceof RotatedPillarBlock || s.getBlock().getDescriptionId().contains("pillar") || s.getBlock().getDescriptionId().contains("casing"))
			return Masonry.sootPlain();
		return cracked ? Masonry.sootCracked() : Masonry.soot();
	}

	/**
	 * A strike on a makers' face (a war bolt or shell from outside): a hole {@code w} x {@code h} with its lower left at
	 * (t, y), a ring of spalled stone round it (the masonry's cracked or spalled stone, 1 block), a soot fan above it, the
	 * knocked-out blocks on the floor inside (it came from outside).
	 */
	public static void strike(Model md, Masonry m, Face f, int t, int y, int w, int h, long seed) {
		md.strikes++;
		md.mark(Model.Cause.STRIKE, f.x(t, 0), y, f.z(t, 0), Math.max(w, h) + 1);
		List<BlockState> out = new ArrayList<>();
		for (int dt = 0; dt < w; dt++)
			for (int dy = 0; dy < h; dy++) {
				for (int d = 1; d >= 0; d--) {
					BlockState s = f.get(md, t + dt, y + dy, d);
					if (s != null) {
						out.add(s);
						f.remove(md, t + dt, y + dy, d);
					}
				}
			}
		// the ring: what's left of the face round the hole, spalled
		for (int dt = -1; dt <= w; dt++)
			for (int dy = -1; dy <= h; dy++) {
				if (dt >= 0 && dt < w && dy >= 0 && dy < h)
					continue;
				BlockState s = f.get(md, t + dt, y + dy, 0);
				if (s != null && s.isCollisionShapeFullBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO) && !isVoidAlloy(s)
					&& !(s.getBlock() instanceof RotatedPillarBlock) && (dt + dy + t) % 3 != 0)
					f.set(md, t + dt, y + dy, 0, m.spalled());
			}
		// soot above it (heat rises)
		sootFan(md, f, t + w / 2, y + h, 1, 2, 4, seed, Model.Cause.STRIKE);
		// the knocked-out blocks inside, on the floor (a few of them; the rest broke up)
		int k = 0;
		for (int i = 0; i < out.size() && k < 3; i += 2, k++)
			md.setIfEmpty(f.x(t + Math.min(k, w - 1), -1 - (k % 2)), 0, f.z(t + Math.min(k, w - 1), -1 - (k % 2)), rubbleForm(out.get(i), m, k));
	}

	/** A block as it lies fallen: full blocks as they are, every other one as the masonry's slab or stair (the cone's slope). */
	public static BlockState rubbleForm(BlockState s, Masonry m, int i) {
		if (isVoidAlloy(s))
			return i % 3 == 1 ? vaSlab(false) : plate();
		if (s.getBlock() instanceof StairBlock || s.getBlock() instanceof SlabBlock)
			return slab(m.dressSlab(), false);
		return i % 3 == 0 ? slab(m.dressSlab(), false) : i % 3 == 1 ? stairs(m.dressStairs(), Direction.from2DDataValue(i % 4), false) : s;
	}

	/**
	 * A rubble cone at the foot of a fallen wall, on the side it fell (toward {@code f.out()}): about a third of the fallen
	 * blocks (the rest crumbled), from its own stone, a slope of the masonry's slabs and stairs falling away from the
	 * middle; never another building's stone. {@code t0..t1} along the face it fell from.
	 */
	public static void rubbleCone(Model md, Masonry m, Face f, int t0, int t1, int fallen, long seed) {
		int want = Math.max(2, fallen / 3), put = 0;
		int mid = (t0 + t1) / 2, half = Math.max(1, (t1 - t0) / 2);
		// the cone's own cause: the wall that came down on this side (its broken stones are cracked where they fell)
		md.mark(Model.Cause.FALL, f.x(mid, 2), 0, f.z(mid, 2), half + 3);
		for (int ring = 0; ring < 3 && put < want; ring++)
			for (int t = t0; t <= t1 && put < want; t++) {
				int fromMid = Math.abs(t - mid);
				int height = ring == 0 ? (fromMid <= half / 2 ? 2 : 1) : ring == 1 ? (fromMid <= half / 3 ? 1 : 0) : 0;
				int d = ring + 1;
				for (int y = 0; y < height && put < want; y++, put++) {
					BlockState s = y == height - 1 ? (hash(t, y, d, seed) % 2 == 0 ? slab(m.dressSlab(), false) : stairs(m.dressStairs(), f.in(), false))
						: hash(t, y, d, seed) % 3 == 0 ? m.spalled() : m.wall();
					f.set(md, t, y, d, s);
				}
				if (height == 0 && hash(t, ring, d, seed) % 3 == 0 && put < want) {
					f.set(md, t, 0, d, slab(m.dressSlab(), false));
					put++;
				}
			}
	}
}
