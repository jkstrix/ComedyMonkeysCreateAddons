package com.comedymonkey.createaddons.content.line;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Direction;

/**
 * How a wire lying on one face of a block runs off towards one edge of that face. Several of these per face, packed
 * three bits a direction (see {@link #pack}), describe the wire's whole shape.
 */
public enum WireLink {
	NONE,
	/** On along the same face of the next block. */
	LINE,
	/** Up the next face of the same block (an inside corner). */
	INSIDE,
	/** Over the edge, down the far side of the block it lies on (an outside corner). */
	OUTSIDE,
	/** Into a machine that gives or takes power: up its face to the middle and into a socket there. */
	TERMINAL,
	/** Under the foot of a Line Post. */
	POST,
	/** Into the solid block at that edge, through a drilled hole, to the wire behind it (a floor wire into a wall). */
	THROUGH;

	public boolean connected() {
		return this != NONE;
	}

	/** The four directions across face f, in a fixed order (the bit positions). */
	public static List<Direction> across(Direction face) {
		List<Direction> out = new ArrayList<>(4);
		for (Direction d : Direction.values())
			if (d.getAxis() != face.getAxis())
				out.add(d);
		return out;
	}

	public static WireLink get(int packed, Direction face, Direction d) {
		int i = across(face).indexOf(d);
		return i < 0 ? NONE : values()[(packed >> (i * 3)) & 7];
	}

	public static int with(int packed, Direction face, Direction d, WireLink link) {
		int i = across(face).indexOf(d);
		if (i < 0)
			return packed;
		return (packed & ~(7 << (i * 3))) | (link.ordinal() << (i * 3));
	}

	/** Set on a face's packed links when the block the wire lies on itself gives or takes power. */
	public static final int BASE = 1 << 12;

	/**
	 * Set on a floor or ceiling wire's packed links when a machine stands across the block, on the far side of a block of
	 * air (a raised motor over a floor wire): the cable goes straight up from the middle into the middle of its face.
	 * The bits: 0-11 the four links (3 each), 12 BASE, 13-15 the hole, 16 RISE.
	 */
	public static final int RISE = 1 << 16;

	private static final int HOLE_SHIFT = 13;

	/**
	 * Where a wire lying on a wall goes into it, through a drilled hole to the wire behind: 0 nowhere, 1 the middle of
	 * the face, 2 + i at its edge towards {@code across(face).get(i)} (the side of a floor wire it meets).
	 */
	public static int hole(int packed) {
		return (packed >> HOLE_SHIFT) & 7;
	}

	/** The edge the hole is at, or null for the middle (or no hole). */
	public static Direction holeEdge(int packed, Direction face) {
		int v = hole(packed);
		return v >= 2 ? across(face).get(v - 2) : null;
	}

	/** A hole in the middle of the face ({@code edge} null) or at an edge. */
	public static int withHole(int packed, Direction face, Direction edge) {
		int v = edge == null ? 1 : 2 + across(face).indexOf(edge);
		return (packed & ~(7 << HOLE_SHIFT)) | (v << HOLE_SHIFT);
	}

	public static int holeBits(int packed) {
		return packed & (7 << HOLE_SHIFT);
	}
}
