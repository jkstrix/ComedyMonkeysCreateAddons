package com.comedymonkey.createaddons.content.light;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.comedymonkey.createaddons.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * A block of Light Strip: a light like any other ({@link LightBlockEntity}), and a piece of a run (P7). A Power Line
 * touching any piece feeds the whole run through it, and only the run: the power it takes is shared out among the
 * pieces, and the network's word on whether its circuit reaches any power (switched off) goes to all of them. The run is
 * every strip joined to this one (StripLinks), worked out again after any strip is added or taken away.
 */
public class LightStripBlockEntity extends LightBlockEntity {
	/** The most pieces a run is followed through (a run longer than this lights only its first this many from a feed). */
	public static final int MAX_RUN = 1024;
	/** Bumped whenever a strip is added or taken away anywhere: runs worked out before it are out of date. */
	private static int changes;

	private List<BlockPos> run = List.of();
	private int runAt = -1;
	/** Where in the run the next delivery starts, so a short supply is shared round rather than always to the same pieces. */
	private int next;
	/**
	 * How many pieces of the run the network last fed it through: each says it has only its share of the run's room, so a
	 * run touching a line in several places isn't counted (and drawn for) several times over.
	 */
	private int feeds = 1;
	/** The run's loaded pieces, worked out once a tick (the network asks several times a tick). */
	private List<LightStripBlockEntity> pieces = List.of();
	private long piecesAt = Long.MIN_VALUE;
	private int piecesRun = -1;

	private final IEnergyStorage runEnergy = new IEnergyStorage() {
		@Override
		public int receiveEnergy(int max, boolean simulate) {
			List<LightStripBlockEntity> pieces = run();
			int took = 0, n = pieces.size(), start = n == 0 ? 0 : Math.floorMod(next, n);
			for (int i = 0; i < n && took < max; i++)
				took += pieces.get((start + i) % n).receive(max - took, simulate);
			if (!simulate)
				next = start + 1;
			return took;
		}

		@Override
		public int extractEnergy(int max, boolean simulate) {
			return 0;
		}

		@Override
		public int getEnergyStored() {
			long sum = 0;
			for (LightStripBlockEntity piece : run())
				sum += piece.energy().getEnergyStored();
			return (int) Math.min(Integer.MAX_VALUE, sum / Math.max(1, feeds));
		}

		@Override
		public int getMaxEnergyStored() {
			long sum = 0;
			for (LightStripBlockEntity piece : run())
				sum += piece.energy().getMaxEnergyStored();
			return (int) Math.min(Integer.MAX_VALUE, sum / Math.max(1, feeds));
		}

		@Override
		public boolean canExtract() {
			return false;
		}

		@Override
		public boolean canReceive() {
			return true;
		}
	};

	public LightStripBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.LIGHT_STRIP.get(), pos, state);
	}

	/** Strips were added or taken away: every run is worked out again when next asked for. */
	static void runsChanged() {
		changes++;
	}

	/** What a Power Line touching this piece feeds: the whole run. */
	public IEnergyStorage runEnergy() {
		return runEnergy;
	}

	/** The run is one lamp to the network: its word comes once for the whole run (merged across every feed). */
	@Override
	public Object lampGroup() {
		BlockPos min = worldPosition;
		for (LightStripBlockEntity piece : run())
			if (piece.worldPosition.compareTo(min) < 0)
				min = piece.worldPosition;
		return min;
	}

	@Override
	public void supplied(boolean source, Object network) {
		supplied(source, network, 1);
	}

	/** The network's word reaches every piece of the run, each deciding with what else it has heard (P3). */
	@Override
	public void supplied(boolean source, Object network, int feeds) {
		for (LightStripBlockEntity piece : run()) {
			piece.feeds = Math.max(1, feeds);
			piece.heard(source, network);
		}
	}

	/** Loaded or unloaded, the runs around it change: they're worked out again. */
	@Override
	public void onLoad() {
		super.onLoad();
		runsChanged();
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		runsChanged();
	}

	private void heard(boolean source, Object network) {
		super.supplied(source, network);
	}

	/** Every loaded piece of the run this one is in, this one first. */
	public List<LightStripBlockEntity> run() {
		if (level == null)
			return List.of(this);
		if (runAt != changes) {
			run = follow();
			runAt = changes;
		}
		long now = level.getGameTime();
		if (piecesAt == now && piecesRun == runAt)
			return pieces;
		List<LightStripBlockEntity> out = new ArrayList<>(run.size());
		for (BlockPos p : run)
			if (level.isLoaded(p) && level.getBlockEntity(p) instanceof LightStripBlockEntity piece && !piece.isRemoved())
				out.add(piece);
		if (out.isEmpty() || out.get(0) != this)
			out.add(0, this);
		pieces = out;
		piecesAt = now;
		piecesRun = runAt;
		return out;
	}

	/** Every strip joined to this one, through loaded chunks, up to MAX_RUN of them. */
	private List<BlockPos> follow() {
		List<BlockPos> out = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> walk = new ArrayDeque<>();
		walk.add(worldPosition);
		seen.add(worldPosition);
		while (!walk.isEmpty() && out.size() < MAX_RUN) {
			BlockPos p = walk.poll();
			out.add(p);
			BlockState state = level.getBlockState(p);
			for (BlockPos q : StripLinks.joined(level, p, state))
				if (level.isLoaded(q) && seen.add(q.immutable()))
					walk.add(q.immutable());
		}
		return out;
	}
}
