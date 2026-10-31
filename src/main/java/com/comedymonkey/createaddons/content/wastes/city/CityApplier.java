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
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.WallBlock;
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
 * under it (P3). Outskirts wrecks only go where there's ground; bridge rails and a snapped bridge's hanging end only
 * over the void. Bars, panes, fences and girders join up with what's beside them once the blocks are in.
 */
public abstract class CityApplier {
	/** Land this far under the floor still counts as the city's ground (filled up to it); deeper, the city is decked over it. */
	public static final int FILL_DEPTH = 6;
	/** In the outskirts, land this far below or above the floor is levelled to it; anything else is void or cliff. */
	public static final int OUTSKIRT_BELOW = 8, OUTSKIRT_ABOVE = 12;
	/** How far down a trestle reaches looking for land before it ends in the void. */
	public static final int TRESTLE_REACH = 32;

	protected final BlockPos origin;
	protected final int floor;
	private final Map<Long, Integer> tops = new HashMap<>();
	private final List<BlockPos> joining = new ArrayList<>();

	protected CityApplier(BlockPos origin) {
		this.origin = origin;
		this.floor = origin.getY() - 1;
	}

	/** The top solid block of the column as it was before the city (MIN_VALUE: none). */
	protected abstract int terrainTop(int x, int z);

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
		return t >= floor - OUTSKIRT_BELOW && t <= floor + OUTSKIRT_ABOVE;
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
			case CityPlan.Op.Loot l -> {
				if (blockEntity(l.at()) instanceof RandomizableContainerBlockEntity c) {
					c.setLootTable(l.table(), l.seed());
					changed(c);
				}
			}
		}
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

	/** One column of the city's ground. */
	private void ground(CityPlan.Op.Ground g) {
		int x = g.at().getX(), z = g.at().getZ();
		int t = top(x, z);
		boolean land = g.core() ? t >= floor - FILL_DEPTH : t >= floor - OUTSKIRT_BELOW && t <= floor + OUTSKIRT_ABOVE;
		if (land) {
			for (int y = t + 1; y < floor; y++)
				set(new BlockPos(x, y, z), Blocks.END_STONE.defaultBlockState());
			set(new BlockPos(x, floor, z), CityLayout.scorched(x, z));
			for (int y = floor + 1; y <= t; y++)
				set(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
			return;
		}
		if (!g.deck())
			return;
		BlockPos deck = new BlockPos(x, floor, z);
		set(deck, VoidAlloyBond.at(ModWastes.VOID_ALLOY_PLATE.get().defaultBlockState(), deck));
		if (g.trestle()) {
			int bottom = t == Integer.MIN_VALUE ? floor - TRESTLE_REACH : Math.max(t + 1, floor - TRESTLE_REACH);
			BlockState leg = ModWastes.VOID_ALLOY_PILLAR.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
			for (int y = floor - 1; y >= bottom; y--)
				set(new BlockPos(x, y, z), leg);
		}
	}

	// ================================================================ into a live level (the dev command)

	/** The whole city into a loaded level at once. */
	public static class Live extends CityApplier {
		private final ServerLevel level;
		private final List<BlockEntity> touched = new ArrayList<>();

		public Live(ServerLevel level, BlockPos origin) {
			super(origin);
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

		public Gen(WorldGenLevel level, BoundingBox box, BlockPos origin) {
			super(origin);
			this.level = level;
			this.box = box;
		}

		@Override
		protected int terrainTop(int x, int z) {
			int h = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
			return h <= level.getMinBuildHeight() ? Integer.MIN_VALUE : h;
		}

		@Override
		protected void set(BlockPos pos, BlockState state) {
			if (box.isInside(pos))
				level.setBlock(pos, state, Block.UPDATE_CLIENTS);
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
