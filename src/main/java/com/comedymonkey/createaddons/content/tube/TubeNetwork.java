package com.comedymonkey.createaddons.content.tube;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.config.CMConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Receiver's network as it last read it: its tubes nearest first, the way home from each (the side towards the
 * Receiver, along the shortest route), and the Inlets on it. Read again whenever tubes, Inlets or Receivers changed or
 * loaded somewhere ({@link TubeJoints#version}). A joint counts only when both its tubes say joined. It reads at most the
 * most tubes, nearest first (the rest aren't served, D30). Tubes in chunks that aren't loaded are left out until they
 * load; no chunk is ever loaded to read one (D17, D26).
 */
final class TubeNetwork {
	/**
	 * An Inlet on the network: the tube its back joins, and that tube's side it's on (where its loads come in). Both
	 * null for an Inlet right against the Receiver's input (a short run: its loads go straight into the Receiver).
	 */
	record Entry(BlockPos inlet, @Nullable BlockPos tube, @Nullable Direction side) {}

	final int version;
	/** The tube on the Receiver's input, or null when nothing is joined there. */
	@Nullable
	final BlockPos first;
	/** Nearest the Receiver first, so a load moving on always finds the tube ahead already moved this tick. */
	final List<BlockPos> tubes = new ArrayList<>();
	final Map<BlockPos, Direction> home = new HashMap<>();
	final List<Entry> inlets = new ArrayList<>();
	/** More tubes than it may serve: the farthest are left out. */
	boolean tooLong;
	/**
	 * Another Receiver's input is on these tubes (possible only where joints were made while part of the network wasn't
	 * loaded): it moves nothing until that's put right, so two Receivers never fight over the same loads.
	 */
	boolean shared;

	private TubeNetwork(int version, @Nullable BlockPos first) {
		this.version = version;
		this.first = first;
	}

	private static boolean open(BlockState tube, Direction side) {
		return tube.getBlock() instanceof ClockworkTubeBlock && tube.getValue(ClockworkTubeBlock.JOINTS.get(side)) == TubeJoint.OPEN;
	}

	static TubeNetwork read(Level level, BlockPos receiver, Direction input, int version) {
		BlockPos first = receiver.relative(input);
		if (!level.isLoaded(first))
			return new TubeNetwork(version, null);
		BlockState fs = level.getBlockState(first);
		// an Inlet right against its input, its back to it: a network of no tubes
		if (fs.getBlock() instanceof ClockworkInletBlock && fs.getValue(ClockworkInletBlock.FACING) == input) {
			TubeNetwork net = new TubeNetwork(version, null);
			net.inlets.add(new Entry(first.immutable(), null, null));
			return net;
		}
		if (!open(fs, input.getOpposite()))
			return new TubeNetwork(version, null);
		int max = CMConfig.SERVER.clockworkTubeMaxLength();
		TubeNetwork net = new TubeNetwork(version, first.immutable());
		net.home.put(net.first, input.getOpposite());
		net.tubes.add(net.first);
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(net.first);
		while (!queue.isEmpty()) {
			BlockPos p = queue.poll();
			BlockState ps = level.getBlockState(p);
			for (Direction side : Direction.values()) {
				if (!open(ps, side))
					continue;
				BlockPos n = p.relative(side);
				if (!level.isLoaded(n))
					continue;
				BlockState ns = level.getBlockState(n);
				if (ns.getBlock() instanceof ClockworkTubeBlock) {
					if (net.home.containsKey(n) || !open(ns, side.getOpposite()))
						continue;
					if (net.tubes.size() >= max) {
						net.tooLong = true;
						continue;
					}
					net.home.put(n.immutable(), side.getOpposite());
					net.tubes.add(n.immutable());
					queue.add(n.immutable());
				} else if (ns.getBlock() instanceof ClockworkInletBlock && ns.getValue(ClockworkInletBlock.FACING) == side) {
					net.inlets.add(new Entry(n.immutable(), p, side));
				} else if (ns.getBlock() instanceof ClockworkReceiverBlock && ns.getValue(ClockworkReceiverBlock.FACING) == side
					&& !n.equals(receiver)) {
					net.shared = true;
				}
			}
		}
		return net;
	}
}
