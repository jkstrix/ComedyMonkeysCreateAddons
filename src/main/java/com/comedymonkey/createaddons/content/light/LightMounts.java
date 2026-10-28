package com.comedymonkey.createaddons.content.light;

import java.util.ArrayList;
import java.util.List;

import com.comedymonkey.createaddons.content.line.PowerLineBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Where lights are fixed, for the Power Lines that feed them: a wire lying on the same surface as a light runs flat to
 * its foot (WireLink.LAMP), and a wire lying on any face of the block a light is fixed to feeds it through the block
 * (WireLink.BEHIND), so the cable can run hidden behind a wall or above a ceiling.
 */
public final class LightMounts {
	private LightMounts() {}

	/** The light (Light Strip, Edge Light) at pos, in {@code state}, is fixed to the block at {@code support}. */
	public static boolean fixedTo(BlockGetter level, BlockState state, BlockPos pos, BlockPos support) {
		if (state.getBlock() instanceof FestoonHookBlock)
			return support.equals(pos.relative(state.getValue(FestoonHookBlock.FACING).getOpposite()));
		if (state.getBlock() instanceof HangingLampBlock)
			return support.equals(HangingLampBlock.ceiling(level, state, pos));
		if (state.getBlock() instanceof EdgeLightBlock) {
			int edges = EdgeLinks.edgesAt(level, pos);
			for (Direction face : Direction.values())
				if (EdgeLinks.onFace(edges, face) && pos.relative(face).equals(support))
					return true;
			return false;
		}
		if (state.getBlock() instanceof LightStripBlock) {
			for (Direction face : Direction.values())
				if (LightStripBlock.hasFace(state, face) && pos.relative(face).equals(support))
					return true;
			return false;
		}
		return support.equals(LightBlock.support(state, pos));
	}

	/** The lights fixed to the block at {@code support}, on its faces other than the one towards {@code except}; loaded only. */
	public static List<BlockPos> fixedAround(LevelReader level, BlockPos support, BlockPos except) {
		List<BlockPos> out = new ArrayList<>();
		// (only a solid block carries a feed through it)
		if (!level.getBlockState(support).isCollisionShapeFullBlock(level, support))
			return out;
		for (Direction d : Direction.values()) {
			BlockPos p = support.relative(d);
			if (p.equals(except) || !level.hasChunkAt(p))
				continue;
			BlockState there = level.getBlockState(p);
			// a hanging lamp's cable: the lamp at its foot is fixed to this block
			if (there.getBlock() instanceof HangingCableBlock) {
				BlockPos q = HangingCableBlock.foot(level, p);
				if (q != null && fixedTo(level, level.getBlockState(q), q, support))
					out.add(q);
				continue;
			}
			if (fixedTo(level, there, p, support))
				out.add(p);
		}
		return out;
	}

	/** A light at pos was placed or taken away: the wires round each block it's fixed to work their links out again. */
	static void relinkBehind(Level level, BlockState state, BlockPos pos) {
		if (level.isClientSide)
			return;
		// a hanging lamp is fixed to the ceiling at the top of its cable, not to a neighbour
		if (state.getBlock() instanceof HangingLampBlock) {
			BlockPos ceiling = HangingLampBlock.ceiling(level, state, pos);
			if (ceiling != null)
				PowerLineBlock.refreshAround(level, ceiling);
			return;
		}
		for (Direction d : Direction.values()) {
			BlockPos support = pos.relative(d);
			if (fixedTo(level, state, pos, support))
			{
				PowerLineBlock.refreshAround(level, support);
				if (level instanceof net.minecraft.server.level.ServerLevel server
					&& level.getBlockState(support).getBlock() instanceof com.comedymonkey.createaddons.content.line.LinePostBlock)
					com.comedymonkey.createaddons.content.line.PowerLines.of(server).changed(support);
			}
		}
	}
}
