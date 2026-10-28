package com.comedymonkey.createaddons.content.light;

import java.util.ArrayList;
import java.util.List;

import com.comedymonkey.createaddons.content.line.WireLink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How Light Strips join (P6): a strip on one face of a block runs off each edge of that face to a strip up the next face
 * of the same block (an inside corner), along the same face of the next block, or over the edge down the far side of the
 * block it lies on (an outside corner), as Power Line wires do. What joins is drawn joined (StripModel) and is one run that
 * shares its power (LightStripBlockEntity), so both use this.
 */
public final class StripLinks {
	private StripLinks() {}

	/** A strip lies on face f of the block at pos. */
	public static boolean has(BlockGetter level, BlockPos pos, Direction face) {
		// (a chunk that isn't loaded is never loaded to look: no strip there yet)
		if (level instanceof net.minecraft.world.level.LevelReader reader && !reader.hasChunkAt(pos))
			return false;
		BlockState state = level.getBlockState(pos);
		return LightStripBlock.hasFace(state, face);
	}

	/** How the strip on face f of {@code state} at pos runs off towards d: inside corner first, then on, then over. */
	public static WireLink link(BlockGetter level, BlockPos pos, BlockState state, Direction face, Direction d) {
		if (LightStripBlock.hasFace(state, d))
			return WireLink.INSIDE;
		if (has(level, pos.relative(d), face))
			return WireLink.LINE;
		if (has(level, pos.relative(d).relative(face), d.getOpposite()))
			return WireLink.OUTSIDE;
		return WireLink.NONE;
	}

	/** The strip on face f's links towards each edge, packed as WireLink packs them. */
	public static int links(BlockGetter level, BlockPos pos, BlockState state, Direction face) {
		int packed = 0;
		for (Direction d : WireLink.across(face))
			packed = WireLink.with(packed, face, d, link(level, pos, state, face, d));
		return packed;
	}

	/** The strip blocks the strips of this block join to (its neighbours along and over the edges). */
	public static List<BlockPos> joined(BlockGetter level, BlockPos pos, BlockState state) {
		List<BlockPos> out = new ArrayList<>();
		for (Direction face : Direction.values()) {
			if (!LightStripBlock.hasFace(state, face))
				continue;
			for (Direction d : WireLink.across(face))
				switch (link(level, pos, state, face, d)) {
					case LINE -> out.add(pos.relative(d));
					case OUTSIDE -> out.add(pos.relative(d).relative(face));
					default -> {}
				}
		}
		return out;
	}
}
