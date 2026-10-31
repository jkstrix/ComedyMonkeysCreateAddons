package com.comedymonkey.createaddons.content.wastes.city.kit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The building kit's plan checks (design brief, Builder Notes, "Tests over checks"), read off a building's {@link Model}:
 * the anti-monoblock rule (no one block makes up more than {@link #MAX_SHARE} of what's seen of a face), excluded blocks,
 * marks only near their causes, and blocks held up. Shared by the game tests and the progress doc's numbers.
 */
public final class ModelChecks {
	private ModelChecks() {}

	/**
	 * The anti-monoblock rule (provisional, for the user's word: progress doc, "The building kit"): of the blocks seen
	 * looking straight at one face of a building (each column's nearest block, the ground left out), no one block may make
	 * up more than this share. The brief's own proportions put a wall field at 55-80% of one surface; a whole face, with its
	 * plinth, piers, windows, dressings and roof, comes in under this.
	 */
	public static final double MAX_SHARE = 0.65;

	/** The share of the commonest block seen from each side, and which it is. */
	public record Share(Direction side, Block block, double share, int seen) {}

	public static List<Share> elevations(Model md) {
		Map<BlockPos, BlockState> blocks = md.blocks();
		int[] b = md.bounds();
		List<Share> out = new ArrayList<>();
		for (Direction side : Direction.Plane.HORIZONTAL) {
			Map<Block, Integer> count = new HashMap<>();
			int seen = 0;
			boolean alongX = side.getAxis() == Direction.Axis.Z;
			int u0 = alongX ? b[0] : b[2], u1 = alongX ? b[3] : b[5];
			int d0 = alongX ? b[2] : b[0], d1 = alongX ? b[5] : b[3];
			for (int u = u0; u <= u1; u++)
				for (int y = 0; y <= b[4]; y++) {
					// look in from the side: from its outside toward the other
					int step = side.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? 1 : -1;
					int start = step > 0 ? d0 : d1;
					for (int d = start; d >= d0 && d <= d1; d += step) {
						BlockState s = blocks.get(alongX ? new BlockPos(u, y, d) : new BlockPos(d, y, u));
						if (s != null) {
							count.merge(s.getBlock(), 1, Integer::sum);
							seen++;
							break;
						}
					}
				}
			Block top = Blocks.AIR;
			int n = 0;
			for (var e : count.entrySet())
				if (e.getValue() > n) {
					n = e.getValue();
					top = e.getKey();
				}
			out.add(new Share(side, top, seen == 0 ? 0 : n / (double) seen, seen));
		}
		return out;
	}

	/** Blocks the brief keeps out of the city's buildings (design brief, "Excluded, and why"; D73). */
	public static boolean excluded(BlockState s) {
		String id = BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString();
		return s.is(Blocks.IRON_BARS) || id.contains("mossy") || s.is(Blocks.END_STONE_BRICKS) || id.contains("limestone") || id.contains("calcite")
			|| id.contains("framed_glass") || s.is(Blocks.SMOOTH_STONE) || id.equals("create:brass_block") || s.is(Blocks.BRICKS);
	}

	/** Whether (x, y, z) lies within reach of a mark of one of the given causes (the larger of the axis distances). */
	public static boolean nearCause(Model md, int x, int y, int z, Model.Cause... causes) {
		for (Model.Mark m : md.marks)
			for (Model.Cause c : causes)
				if (m.cause() == c && Math.max(Math.abs(m.x() - x), Math.max(Math.abs(m.y() - y), Math.abs(m.z() - z))) <= m.reach() + 1)
					return true;
		return false;
	}

	/** Every cracked or spalled stone with no cause within reach, and every soot block likewise (each as a message). */
	public static List<String> marksWithoutCause(Model md) {
		List<String> out = new ArrayList<>();
		for (var e : md.blocks().entrySet()) {
			BlockPos p = e.getKey();
			BlockState s = e.getValue();
			if (Masonry.isSpalled(s) && !nearCause(md, p.getX(), p.getY(), p.getZ(), Model.Cause.CLAMP, Model.Cause.CUT, Model.Cause.STRIKE, Model.Cause.BREAK,
				Model.Cause.FALL))
				out.add("spalled " + s.getBlock() + " at " + p.toShortString() + " with no cause near");
			if (Masonry.isSoot(s) && !nearCause(md, p.getX(), p.getY(), p.getZ(), Model.Cause.STRIKE, Model.Cause.FIRE, Model.Cause.CHIMNEY, Model.Cause.FURNACE,
				Model.Cause.ARC, Model.Cause.FALL))
				out.add("soot " + s.getBlock() + " at " + p.toShortString() + " with no cause near");
			if (s.is(ModWastes.CRACKED_STAINED_END_STONE_BRICKS.get()) && !nearCause(md, p.getX(), p.getY(), p.getZ(), Model.Cause.values()))
				out.add("cracked brick at " + p.toShortString() + " with no cause near");
		}
		return out;
	}

	/** The scarred plates in a model. */
	public static int scars(Model md) {
		int n = 0;
		for (BlockState s : md.blocks().values())
			if (s.is(ModWastes.SCARRED_VOID_ALLOY_PLATE.get()))
				n++;
		return n;
	}

	/** Cells not held up (joined face to face, through the building, to the ground). */
	public static List<BlockPos> floating(Model md) {
		var held = md.held();
		List<BlockPos> out = new ArrayList<>();
		for (BlockPos p : md.blocks().keySet())
			if (!held.contains(p.asLong()))
				out.add(p);
		return out;
	}
}
