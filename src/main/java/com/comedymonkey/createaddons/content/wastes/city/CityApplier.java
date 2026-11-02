package com.comedymonkey.createaddons.content.wastes.city;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.comedymonkey.createaddons.content.wastes.CityLink;
import com.comedymonkey.createaddons.content.wastes.DerelictInsulatorBlockEntity;
import com.comedymonkey.createaddons.content.wastes.District;
import com.comedymonkey.createaddons.content.wastes.VoidAlloyBond;
import com.comedymonkey.createaddons.content.wastes.bot.WastesBots;
import com.comedymonkey.createaddons.content.wastes.overgrowth.BreakrootBurstBlock;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Builds a {@link CityPlan}'s ops into a world: the whole of it into a live level ({@link Live}, the dev command), or a
 * chunk of it at a time while that chunk generates ({@link Gen}). The ground is read here, column by column, before
 * the city touches it: the core is levelled to the heart's floor (a dip filled with End Stone, a hill cut down), and
 * where there's no land near it (the island's edge, the void) the city stands on a void-alloy deck with trestles
 * under it (P3). Past the outskirts' levelling (the terrace's edge, a hill or dip beyond its limits) the ground eases
 * back into the island with scree on it ({@link CityEdge}, D91). Outskirts wrecks only go where there's ground at the
 * floor; bridge rails and a snapped bridge's hanging end only over the void. Bars, panes, fences and girders join up with what's beside them once the blocks are in
 * (on a chunk's edge in world generation, once the chunk beside it is built: {@link Gen#defer}).
 */
public abstract class CityApplier {
	/** Land this far under the floor still counts as the city's ground (filled up to it); deeper, the city is decked over it. */
	public static final int FILL_DEPTH = 6;
	/** In the outskirts, land this far below or above the floor is levelled to it; a little past that, part way ({@link CityEdge#SOFT}). */
	public static final int OUTSKIRT_BELOW = 8, OUTSKIRT_ABOVE = 12;
	/** How far down a trestle reaches looking for land before it ends in the void. */
	public static final int TRESTLE_REACH = 32;

	protected final BlockPos origin;
	protected final int floor;
	/** The plan's seed, for the edge's scree ({@link CityEdge#roll}). */
	protected final long seed;
	private final Map<Long, Integer> tops = new HashMap<>();
	/** Outskirts columns whose ground ended off the floor (eased back into the island, D91). */
	private final java.util.Set<Long> off = new java.util.HashSet<>();
	private final List<BlockPos> joining = new ArrayList<>();
	/** Each column's ground top as its ground op left it (a deck's too: the plan may pave it), for the overgrowth's {@link CityPlan.Op.Surface}. */
	private final Map<Long, Integer> surfaces = new HashMap<>();

	protected CityApplier(BlockPos origin, long seed) {
		this.origin = origin;
		this.floor = origin.getY() - 1;
		this.seed = seed;
	}

	/** The top solid block of the column as it was before the city (MIN_VALUE: none). */
	protected abstract int terrainTop(int x, int z);

	/**
	 * The island's ground at or below {@code y} in the column: the first {@code ground} block (or End Stone) down
	 * (MIN_VALUE: none), so chorus, an End city's spill or anything else standing on the island isn't taken for it.
	 */
	public static int groundBelow(BlockGetter level, int x, int y, int z, Block ground) {
		if (y == Integer.MIN_VALUE)
			return y;
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, y, z);
		for (int min = level.getMinBuildHeight(); y > min; y--) {
			BlockState s = level.getBlockState(p.setY(y));
			if (s.is(ground) || s.is(Blocks.END_STONE))
				return y;
		}
		return Integer.MIN_VALUE;
	}

	protected abstract void set(BlockPos pos, BlockState state);

	protected abstract BlockState get(BlockPos pos);

	protected abstract BlockEntity blockEntity(BlockPos pos);

	protected abstract boolean contains(BlockPos pos);

	protected abstract ServerLevelAccessor accessor();

	/** Binds a city block to its district. */
	protected abstract void bind(CityLink.Member member, BlockPos pos, District district);

	/** The block entity's data changed after the city's builder set it (spans, a hop). */
	protected void changed(BlockEntity be) {
		be.setChanged();
	}

	private int top(int x, int z) {
		return tops.computeIfAbsent(BlockPos.asLong(x, 0, z), k -> terrainTop(x, z));
	}

	public boolean hasGround(int x, int z) {
		int t = top(x, z);
		return t >= floor - OUTSKIRT_BELOW && t <= floor + OUTSKIRT_ABOVE && !off.contains(BlockPos.asLong(x, 0, z));
	}

	public boolean overVoid(int x, int z) {
		return top(x, z) < floor - OUTSKIRT_BELOW;
	}

	/** Builds these ops (in order), then joins up the bars, panes and girders it placed. */
	public void apply(List<CityPlan.Op> ops) {
		for (CityPlan.Op op : ops)
			if (contains(op.at()))
				apply(op);
		for (BlockPos p : joining) {
			if (defer(p))
				continue;
			BlockState s = get(p);
			BlockState joined = Block.updateFromNeighbourShapes(s, accessor(), p);
			if (joined != s)
				set(p, joined);
		}
		joining.clear();
	}

	private void apply(CityPlan.Op op) {
		switch (op) {
			case CityPlan.Op.Ground g -> ground(g);
			case CityPlan.Op.Put p -> put(p);
			case CityPlan.Op.Span s -> {
				if (blockEntity(s.at()) instanceof DerelictInsulatorBlockEntity be) {
					be.addSpan(s.other(), s.stage(), s.fromHere());
					changed(be);
				}
			}
			case CityPlan.Op.Snapped s -> {
				if (blockEntity(s.at()) instanceof DerelictInsulatorBlockEntity be) {
					be.addSnapped(s.heading());
					changed(be);
				}
			}
			case CityPlan.Op.Hop h -> {
				if (blockEntity(h.at()) instanceof DerelictInsulatorBlockEntity be) {
					be.setHop(h.hop());
					changed(be);
				}
			}
			case CityPlan.Op.Scuttler s -> WastesBots.placeScuttler(accessor(), s.pos(), origin, s.district());
			case CityPlan.Op.Sentry s -> WastesBots.placeSentry(accessor(), s.wall(), s.face(), origin, s.district());
			case CityPlan.Op.Custodian c -> WastesBots.placeCustodian(accessor(), c.at(), c.yaw(), origin, c.pylons(), c.vault());
			case CityPlan.Op.Surface s -> surface(s);
			case CityPlan.Op.Loot l -> {
				if (blockEntity(l.at()) instanceof RandomizableContainerBlockEntity c) {
					c.setLootTable(l.table(), l.seed());
					changed(c);
				}
			}
		}
	}

	/** Whether {@code p}'s joining waits for the blocks beside it (false: join it now). */
	protected boolean defer(BlockPos p) {
		return false;
	}

	private void put(CityPlan.Op.Put p) {
		int x = p.at().getX(), z = p.at().getZ();
		if (p.need() == CityPlan.ON_GROUND && !hasGround(x, z) || p.need() == CityPlan.OVER_VOID && !overVoid(x, z))
			return;
		// a void-alloy block's bond from where it stands (D75), as a player's or a command's placing gives it
		set(p.at(), VoidAlloyBond.at(p.state(), p.at()));
		if (joins(p.state()))
			joining.add(p.at());
		if (p.district() != null && blockEntity(p.at()) instanceof CityLink.Member m)
			bind(m, p.at(), p.district());
	}

	/**
	 * Blocks whose look depends on what's beside them (bars, panes, fences, walls, girders; and for the building kit, D71:
	 * stairs, whose inner and outer corners form from their neighbours, the Void-Alloy Band's flush joins and Create's pipes).
	 */
	private static boolean joins(BlockState s) {
		Block b = s.getBlock();
		return b instanceof CrossCollisionBlock || b instanceof WallBlock || b instanceof net.minecraft.world.level.block.StairBlock
			|| b instanceof com.comedymonkey.createaddons.content.wastes.VoidAlloyBandBlock || b instanceof net.minecraft.world.level.block.PipeBlock
			|| BuiltInRegistries.BLOCK.getKey(b).getPath().contains("girder");
	}

	/**
	* A plant on its column's ground (D92): on the top its ground op left (a deck's too, where the plan paved over it), if
	* that's sturdy, not void-alloy (a bare deck) and the block above is clear. A burst that cracks its brick cracks it only
	* here, as it grows. Only this column (and a burst's brick, in the same chunk) is read, so a chunk builds the same
	* whichever order the chunks come in.
	*/
	private void surface(CityPlan.Op.Surface s) {
		int x = s.at().getX(), z = s.at().getZ();
		Integer top = surfaces.get(BlockPos.asLong(x, 0, z));
		if (top == null || s.floorOnly() && top != floor)
			return;
		BlockPos ground = new BlockPos(x, top, z), above = ground.above();
		BlockState under = get(ground);
		if (!get(above).isAir() || !under.isFaceSturdy(accessor(), ground, Direction.UP) || BuiltInRegistries.BLOCK.getKey(under.getBlock()).getPath().contains("void_alloy"))
			return;
		set(above, s.state());
		if (s.cracks() && s.state().hasProperty(BreakrootBurstBlock.FACING)) {
			BlockPos brick = above.relative(s.state().getValue(BreakrootBurstBlock.FACING).getOpposite());
			if (contains(brick) && get(brick).is(ModWastes.STAINED_END_STONE_BRICKS.get()))
				set(brick, ModWastes.CRACKED_STAINED_END_STONE_BRICKS.get().defaultBlockState());
		}
	}

	/** One column of the city's ground. */
	private void ground(CityPlan.Op.Ground g) {
		int x = g.at().getX(), z = g.at().getZ();
		int t = top(x, z);
		// plain outskirts: levelled as far as the edge and the ground's height let it, eased back past that (D91); a bridge
		// heading into a hill too, a cutting through it as the ground beside it is (its deck only over the void or far below)
		if (!g.core() && (!g.deck() || t > floor + OUTSKIRT_ABOVE)) {
			if (t == Integer.MIN_VALUE)
				return;
			int d = t - floor;
			if (g.across() > 0 || CityEdge.inner(d) != 0) {
				blend(x, z, t, floor + CityEdge.top(d, g.across(), g.width()), g.across());
				return;
			}
		}
		boolean land = g.core() ? t >= floor - FILL_DEPTH : t >= floor - OUTSKIRT_BELOW && t <= floor + OUTSKIRT_ABOVE;
		if (land) {
			for (int y = t + 1; y < floor; y++)
				set(new BlockPos(x, y, z), Blocks.END_STONE.defaultBlockState());
			set(new BlockPos(x, floor, z), CityLayout.scorched(x, z));
			for (int y = floor + 1; y <= t; y++)
				set(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
			surfaces.put(BlockPos.asLong(x, 0, z), floor);
			return;
		}
		if (!g.deck())
			return;
		BlockPos deck = new BlockPos(x, floor, z);
		set(deck, VoidAlloyBond.at(ModWastes.VOID_ALLOY_PLATE.get().defaultBlockState(), deck));
		// the plan may pave over it (a street on a deck): its plants grow on the paving, never on the bare plate (surface)
		surfaces.put(BlockPos.asLong(x, 0, z), floor);
		if (g.trestle()) {
			int bottom = t == Integer.MIN_VALUE ? floor - TRESTLE_REACH : Math.max(t + 1, floor - TRESTLE_REACH);
			BlockState leg = ModWastes.VOID_ALLOY_PILLAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
			for (int y = floor - 1; y >= bottom; y--)
				set(new BlockPos(x, y, z), leg);
		}
	}

	/**
	 * A column levelled part way: cut down or filled up to {@code top}, its top easing from the city's scorched ground to the
	 * island's End Stone, scree where it was cut or filled (and on the cut's face or the fill's side, which the next column
	 * down shows), a loose stone or two on it; where nothing moved, a fan of debris below filled ground. Only this column's
	 * own ground decides it.
	 */
	private void blend(int x, int z, int t, int top, double across) {
		int d = t - floor, moved = Math.abs(top - t);
		// the city's scorched ground thins out across the band, and on ground the levelling only partly took
		double w = CityEdge.levelling(across) * (d == 0 ? 1 : 1 - Math.abs(CityEdge.inner(d) / d));
		BlockState stone = Blocks.END_STONE.defaultBlockState();
		if (top != floor)
			off.add(BlockPos.asLong(x, 0, z));
		if (moved == 0 && d != 0) {
			// the fan: below the band where it was filled, or below a dip filled part way
			int below = across >= 1 ? -d : -OUTSKIRT_BELOW - CityEdge.SOFT - d;
			double past = across >= 1 ? (across - 1) / CityEdge.FAN : (below - 1) / 4.0;
			double chance = CityEdge.fan(past, below) * (across >= 1 ? 1 : CityEdge.levelling(across));
			boolean loose = false;
			if (CityEdge.roll(seed, x, z, 1, chance)) {
				set(new BlockPos(x, t, z), CityEdge.screeBlock(seed, x, z));
				loose = CityEdge.roll(seed, x, z, 4, 0.3);
				if (loose)
					set(new BlockPos(x, t + 1, z), CityEdge.looseBlock(seed, x, z));
			}
			if (!loose)
				surface(x, z, t);
			return;
		}
		for (int y = t + 1; y < top; y++)
			set(new BlockPos(x, y, z), stone);
		for (int y = top + 1; y <= t; y++)
			set(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
		double scree = CityEdge.scree(d, across);
		BlockState surface = CityEdge.roll(seed, x, z, 1, scree) ? CityEdge.screeBlock(seed, x, z)
			: CityEdge.roll(seed, x, z, 2, w) ? CityLayout.scorched(x, z) : top > t ? stone : null;
		if (surface != null)
			set(new BlockPos(x, top, z), surface);
		if (moved > 0 && CityEdge.roll(seed, x, z, 3, scree))
			set(new BlockPos(x, top - 1, z), CityEdge.screeBlock(seed, z, x));
		if (CityEdge.roll(seed, x, z, 4, CityEdge.loose(d, across)))
			set(new BlockPos(x, top + 1, z), CityEdge.looseBlock(seed, x, z));
		else
			surface(x, z, top);
	}

	/**
	 * An eased column's top as the overgrowth's ground, only within the levelling's reach of the floor (ground eased back
	 * toward the city), never a lower island or a roof further off.
	 */
	private void surface(int x, int z, int top) {
		if (top - floor <= OUTSKIRT_ABOVE + CityEdge.SOFT && floor - top <= OUTSKIRT_BELOW + CityEdge.SOFT)
			surfaces.put(BlockPos.asLong(x, 0, z), top);
	}

	// ================================================================ into a live level (the dev command)

	/** The whole city into a loaded level at once. */
	public static class Live extends CityApplier {
		private final ServerLevel level;
		private final List<BlockEntity> touched = new ArrayList<>();

		/** With the edge's scree as seed 0 lays it (a tool's or a test's build). */
		public Live(ServerLevel level, BlockPos origin) {
			this(level, origin, 0);
		}

		public Live(ServerLevel level, BlockPos origin, long seed) {
			super(origin, seed);
			this.level = level;
		}

		@Override
		protected int terrainTop(int x, int z) {
			int h = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
			return h <= level.getMinBuildHeight() ? Integer.MIN_VALUE : h;
		}

		@Override
		protected void set(BlockPos pos, BlockState state) {
			level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
		}

		@Override
		protected BlockState get(BlockPos pos) {
			return level.getBlockState(pos);
		}

		@Override
		protected BlockEntity blockEntity(BlockPos pos) {
			return level.getBlockEntity(pos);
		}

		@Override
		protected boolean contains(BlockPos pos) {
			return true;
		}

		@Override
		protected ServerLevelAccessor accessor() {
			return level;
		}

		@Override
		protected void bind(CityLink.Member member, BlockPos pos, District district) {
			CityLink.bind(level, pos, origin, district);
		}

		@Override
		protected void changed(BlockEntity be) {
			be.setChanged();
			touched.add(be);
		}

		@Override
		public void apply(List<CityPlan.Op> ops) {
			super.apply(ops);
			for (BlockEntity be : touched)
				level.sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), Block.UPDATE_ALL);
			touched.clear();
		}
	}

	// ================================================================ a chunk of it while the chunk generates

	/** The ops of one chunk into the chunk being generated (a structure piece's postProcess). */
	public static class Gen extends CityApplier {
		private final WorldGenLevel level;
		private final BoundingBox box;
		private final Block ground;

		/** With seed 0's scree, on End Stone (a test's build). */
		public Gen(WorldGenLevel level, BoundingBox box, BlockPos origin) {
			this(level, box, origin, 0, Blocks.END_STONE);
		}

		/** {@code ground}: the island's own block (the noise's default block). */
		public Gen(WorldGenLevel level, BoundingBox box, BlockPos origin, long seed, Block ground) {
			super(origin, seed);
			this.level = level;
			this.box = box;
			this.ground = ground;
		}

		/** The island's ground: down from the column's top to its own first block. */
		@Override
		protected int terrainTop(int x, int z) {
			return groundBelow(level, x, surfaceTop(x, z), z, ground);
		}

		/** The column's top block of any kind (MIN_VALUE: none). */
		protected int surfaceTop(int x, int z) {
			int h = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
			return h <= level.getMinBuildHeight() ? Integer.MIN_VALUE : h;
		}

		/**
		 * A joiner on the chunk's edge waits for the chunk beside it, which may not be built yet: it's marked for the
		 * chunk's post-processing (as a vanilla structure's fences are), which joins it once every chunk round it is done.
		 */
		@Override
		protected boolean defer(BlockPos p) {
			int x = p.getX() & 15, z = p.getZ() & 15;
			return (x == 0 || x == 15 || z == 0 || z == 15) && box.isInside(p) && postProcessLater(p);
		}

		/** Marks {@code p} for its chunk's post-processing (false: a loaded chunk, which has none, so it joins now). */
		protected boolean postProcessLater(BlockPos p) {
			ChunkAccess chunk = level.getChunk(p);
			if (chunk.getPersistedStatus().getChunkType() != ChunkType.PROTOCHUNK)
				return false;
			chunk.markPosForPostprocessing(p);
			return true;
		}

		@Override
		protected void set(BlockPos pos, BlockState state) {
			// (no shape updates: world generation's region makes none, and a test's live level mustn't either)
			if (box.isInside(pos))
				level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
		}

		@Override
		protected BlockState get(BlockPos pos) {
			return level.getBlockState(pos);
		}

		@Override
		protected BlockEntity blockEntity(BlockPos pos) {
			return box.isInside(pos) ? level.getBlockEntity(pos) : null;
		}

		@Override
		protected boolean contains(BlockPos pos) {
			return box.isInside(pos.getX(), box.minY(), pos.getZ());
		}

		@Override
		protected ServerLevelAccessor accessor() {
			return level;
		}

		/** Not loaded yet: the link goes into its NBT, and it joins its city when its chunk loads (P48). */
		@Override
		protected void bind(CityLink.Member member, BlockPos pos, District district) {
			member.cityLink().set(origin, district);
			((BlockEntity) member).setChanged();
		}
	}
}
