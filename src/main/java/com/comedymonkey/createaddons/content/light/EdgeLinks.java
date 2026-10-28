package com.comedymonkey.createaddons.content.light;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix3f;
import org.joml.Vector3f;

import com.comedymonkey.createaddons.content.line.WireLink;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Edge Lights' edges (P6): a block of Edge Light holds trim along any of the 24 edges of its faces, an edge being the
 * face it lies against and the side of that face it runs along. An edge runs off each end along the next block's same
 * edge (joined), into the edge it meets in the same block on the same face (a mitre: the one with the lower side draws the
 * corner), or nowhere (capped). Joined edges are one run (EdgeLightBlockEntity); EdgeModel draws them from the same rules.
 * Parts are authored lying on the floor at the north edge, running along x; {@link #east} is where authored east goes.
 */
public final class EdgeLinks {
	private EdgeLinks() {}

	/** How an edge's end meets what's beyond it. */
	public enum End {
		/** Nothing: an end cap. */
		NONE,
		/** The same edge of the next block. */
		LINE,
		/** The edge it meets in this block, which this one draws the corner for. */
		CORNER,
		/** The edge it meets in this block, which draws the corner. */
		MET
	}

	public static int bit(Direction face, Direction side) {
		return 1 << (face.ordinal() * 4 + WireLink.across(face).indexOf(side));
	}

	public static boolean has(int edges, Direction face, Direction side) {
		return side.getAxis() != face.getAxis() && (edges & bit(face, side)) != 0;
	}

	/** Where the edge on {@code face} along {@code side} runs towards at its authored east end: face × side. */
	public static Direction east(Direction face, Direction side) {
		Vector3f f = new Vector3f(face.getStepX(), face.getStepY(), face.getStepZ());
		Vector3f s = new Vector3f(side.getStepX(), side.getStepY(), side.getStepZ());
		Vector3f e = f.cross(s);
		return Direction.getNearest(e.x, e.y, e.z);
	}

	/** The edges of the Edge Light at pos (none if there's none, or its chunk isn't loaded: it isn't loaded to look). */
	public static int edgesAt(BlockGetter level, BlockPos pos) {
		if (level instanceof LevelReader reader && !reader.hasChunkAt(pos))
			return 0;
		return level.getBlockEntity(pos) instanceof EdgeLightBlockEntity be ? be.edges() : 0;
	}

	/** How the edge on face along side, in a block with {@code edges}, meets what's beyond its end towards e. */
	public static End end(BlockGetter level, BlockPos pos, int edges, Direction face, Direction side, Direction e) {
		// an edge it meets in this block first: only one of the two may fill the corner, even where both run on beyond it
		if (has(edges, face, e))
			return side.ordinal() < e.ordinal() ? End.CORNER : End.MET;
		if (has(edgesAt(level, pos.relative(e)), face, side))
			return End.LINE;
		return End.NONE;
	}

	/** The Edge Light blocks this one's edges join to (along to the next block's same edge). */
	public static List<BlockPos> joined(BlockGetter level, BlockPos pos, int edges) {
		List<BlockPos> out = new ArrayList<>();
		for (Direction face : Direction.values())
			for (Direction side : WireLink.across(face)) {
				if (!has(edges, face, side))
					continue;
				Direction east = east(face, side);
				for (Direction e : new Direction[] {east, east.getOpposite()})
					if (has(edgesAt(level, pos.relative(e)), face, side))
						out.add(pos.relative(e));
			}
		return out;
	}

	/** The faces any of the edges lie against. */
	public static boolean onFace(int edges, Direction face) {
		for (Direction side : WireLink.across(face))
			if (has(edges, face, side))
				return true;
		return false;
	}

	/** The outline of the edges: each a 2.5 px square bar along its edge. */
	public static VoxelShape shape(int edges) {
		VoxelShape out = Shapes.empty();
		for (Direction face : Direction.values())
			for (Direction side : WireLink.across(face))
				if (has(edges, face, side))
					out = Shapes.or(out, bar(face, side));
		return out.optimize();
	}

	/** The authored bar (on the floor at the north edge, along x) turned onto face, along side. */
	private static VoxelShape bar(Direction face, Direction side) {
		Matrix3f m = rotation(face, side);
		Vector3f a = m.transform(new Vector3f(-0.5f, -0.5f, -0.5f)), b = m.transform(new Vector3f(0.5f, -0.5f + 2.5f / 16, -0.5f + 2.5f / 16));
		return Shapes.box(0.5 + Math.min(a.x, b.x), 0.5 + Math.min(a.y, b.y), 0.5 + Math.min(a.z, b.z),
			0.5 + Math.max(a.x, b.x), 0.5 + Math.max(a.y, b.y), 0.5 + Math.max(a.z, b.z));
	}

	/** The turn taking the floor (down) to face and north to side, as PowerLineModel turns wires' parts. */
	public static Matrix3f rotation(Direction face, Direction side) {
		Vector3f f = new Vector3f(face.getStepX(), face.getStepY(), face.getStepZ());
		Vector3f r = new Vector3f(side.getStepX(), side.getStepY(), side.getStepZ());
		Vector3f x = new Vector3f(f).cross(r);
		return new Matrix3f(x.x, x.y, x.z, -f.x, -f.y, -f.z, -r.x, -r.y, -r.z);
	}
}
