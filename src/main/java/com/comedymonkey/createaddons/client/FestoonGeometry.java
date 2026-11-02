package com.comedymonkey.createaddons.client;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;

import com.comedymonkey.createaddons.content.light.FestoonBlockEntity;
import com.comedymonkey.createaddons.content.light.Festoons;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Where a Festoon hook's strings go: every square rod of them (the flex, a segment at a time, and each bulb's socket
 * and glass), worked out once from its spans. {@link FestoonRenderer} draws the rods and {@link FestoonVisual} lays them
 * out as Flywheel instances, both from this, so the two can't drift apart. Each rod is one rod shape ({@link #ROD})
 * stretched into place by its frame.
 */
public final class FestoonGeometry {
	/** The flex's half thickness and the bulbs' size (blocks). */
	public static final float FLEX = 0.3f / 16, SOCKET = 0.5f / 16, BULB = 1.25f / 16;
	/** A bulb hangs below the string: its socket, then its glass. */
	static final double SOCKET_LENGTH = 1.25 / 16, BULB_LENGTH = 2.5 / 16;
	/** The part of each texture every face of a rod shows (fractions of the sprite). */
	static final float U0 = 0.4f, U1 = 0.6f, V0 = 0.2f, V1 = 0.8f;

	/** What a rod is: a bit of flex, a bulb's socket, or its glass. */
	public enum Part { FLEX, SOCKET, BULB }

	/**
	 * The rod shape, along x from 0 to 1, y and z from -1 to 1 (its frame scales those to the rod's half thickness):
	 * its four sides and two ends, 4 corners each, each corner x, y, z, its face's normal, and its u, v (fractions of
	 * the sprite). The faces, their corners' order and the texture on them are the renderer's square rod as it always was.
	 */
	static final float[] ROD;
	static final int ROD_STRIDE = 8, ROD_VERTICES = 24;
	static {
		// the ring of edges round the rod, in y, z: (u + v, u - v, -u - v, -u + v)
		float[][] ring = {{1, 1}, {1, -1}, {-1, -1}, {-1, 1}};
		float[] out = new float[ROD_VERTICES * ROD_STRIDE];
		int k = 0;
		for (int i = 0; i < 4; i++) {
			float[] s0 = ring[i], s1 = ring[(i + 1) % 4];
			float ny = Math.signum(s0[0] + s1[0]), nz = Math.signum(s0[1] + s1[1]);
			k = corner(out, k, 0, s0, 0, ny, nz, U0, V0);
			k = corner(out, k, 1, s0, 0, ny, nz, U1, V0);
			k = corner(out, k, 1, s1, 0, ny, nz, U1, V1);
			k = corner(out, k, 0, s1, 0, ny, nz, U0, V1);
		}
		k = corner(out, k, 0, ring[3], -1, 0, 0, U0, V0);
		k = corner(out, k, 0, ring[2], -1, 0, 0, U1, V0);
		k = corner(out, k, 0, ring[1], -1, 0, 0, U1, V1);
		k = corner(out, k, 0, ring[0], -1, 0, 0, U0, V1);
		k = corner(out, k, 1, ring[0], 1, 0, 0, U0, V0);
		k = corner(out, k, 1, ring[1], 1, 0, 0, U1, V0);
		k = corner(out, k, 1, ring[2], 1, 0, 0, U1, V1);
		corner(out, k, 1, ring[3], 1, 0, 0, U0, V1);
		ROD = out;
	}

	private static int corner(float[] out, int k, float x, float[] yz, float nx, float ny, float nz, float u, float v) {
		out[k++] = x;
		out[k++] = yz[0];
		out[k++] = yz[1];
		out[k++] = nx;
		out[k++] = ny;
		out[k++] = nz;
		out[k++] = u;
		out[k++] = v;
		return k;
	}

	/**
	 * A hook's rods. Rod {@code i}'s frame is {@code frame[12i..12i+11]}: the way along it (a to b, full length), its
	 * two sides (each as long as its half thickness) and its start a, in blocks from the hook's corner. It takes its
	 * light from the block {@code cells[cell[i]]}.
	 */
	public record Layout(Part[] parts, double[] frame, int[] cell, long[] cells) {
		public int rods() {
			return parts.length;
		}

		/** Rod {@code i}'s start or end (from the hook's corner). */
		public Vec3 a(int i) {
			return new Vec3(frame[12 * i + 9], frame[12 * i + 10], frame[12 * i + 11]);
		}

		public Vec3 b(int i) {
			return a(i).add(frame[12 * i], frame[12 * i + 1], frame[12 * i + 2]);
		}

		/** Rod {@code i}'s frame as a matrix, moved by {@code (x, y, z)}: the rod shape onto the rod. */
		public Matrix4f matrix(int i, float x, float y, float z) {
			int f = 12 * i;
			return new Matrix4f(
				(float) frame[f], (float) frame[f + 1], (float) frame[f + 2], 0,
				(float) frame[f + 3], (float) frame[f + 4], (float) frame[f + 5], 0,
				(float) frame[f + 6], (float) frame[f + 7], (float) frame[f + 8], 0,
				(float) frame[f + 9] + x, (float) frame[f + 10] + y, (float) frame[f + 11] + z, 1);
		}
	}

	private FestoonGeometry() {}

	/**
	 * Corner {@code k} of the rod shape on rod {@code i} of these frames: its x, y, z (from the hook's corner) and its
	 * normal, into {@code out} (the renderer's way of drawing a rod; the visual's matrix does the same on the GPU).
	 */
	static void corner(double[] frame, int i, int k, float[] out) {
		int f = 12 * i, p = k * ROD_STRIDE;
		double dx = frame[f], dy = frame[f + 1], dz = frame[f + 2];
		double ux = frame[f + 3], uy = frame[f + 4], uz = frame[f + 5];
		double vx = frame[f + 6], vy = frame[f + 7], vz = frame[f + 8];
		float x = ROD[p], y = ROD[p + 1], z = ROD[p + 2];
		out[0] = (float) (frame[f + 9] + x * dx + y * ux + z * vx);
		out[1] = (float) (frame[f + 10] + x * dy + y * uy + z * vy);
		out[2] = (float) (frame[f + 11] + x * dz + y * uz + z * vz);
		// the normal goes along or across it: the frame's way made unit length
		double nx = ROD[p + 3] / Math.sqrt(dx * dx + dy * dy + dz * dz), ny = ROD[p + 4] / Math.sqrt(ux * ux + uy * uy + uz * uz),
			nz = ROD[p + 5] / Math.sqrt(vx * vx + vy * vy + vz * vz);
		out[3] = (float) (nx * dx + ny * ux + nz * vx);
		out[4] = (float) (nx * dy + ny * uy + nz * vy);
		out[5] = (float) (nx * dz + ny * uz + nz * vz);
	}

	/** How many straight pieces a string this long is drawn in. */
	public static int segments(double length) {
		return Math.max(8, (int) Math.ceil(length * 3));
	}

	/** The rods of the strings a hook at {@code pos}, facing so, holds (those it doesn't hold, the far hook draws). */
	public static Layout layOut(BlockPos pos, Direction facing, List<FestoonBlockEntity.Span> spans) {
		List<Part> parts = new ArrayList<>();
		List<double[]> frames = new ArrayList<>();
		List<Integer> cellOf = new ArrayList<>();
		LongArrayList cells = new LongArrayList();
		Vec3 origin = Vec3.atLowerCornerOf(pos);
		for (FestoonBlockEntity.Span span : spans) {
			if (!span.held())
				continue;
			Vec3 a = Festoons.eye(pos, facing), b = Festoons.eye(span.other(), span.facing());
			double length = a.distanceTo(b);
			int segments = segments(length);
			for (int i = 0; i < segments; i++) {
				Vec3 p0 = Festoons.along(a, b, (double) i / segments), p1 = Festoons.along(a, b, (double) (i + 1) / segments);
				add(parts, frames, cellOf, cells, Part.FLEX, p0.subtract(origin), p1.subtract(origin), FLEX, p0.add(p1).scale(0.5));
			}
			for (double t : Festoons.bulbs(length)) {
				Vec3 at = Festoons.along(a, b, t);
				Vec3 top = at.subtract(origin), neck = top.subtract(0, SOCKET_LENGTH, 0), base = neck.subtract(0, BULB_LENGTH, 0);
				add(parts, frames, cellOf, cells, Part.SOCKET, top, neck, SOCKET, at);
				add(parts, frames, cellOf, cells, Part.BULB, neck, base, BULB, at);
			}
		}
		double[] frame = new double[12 * frames.size()];
		for (int i = 0; i < frames.size(); i++)
			System.arraycopy(frames.get(i), 0, frame, 12 * i, 12);
		return new Layout(parts.toArray(Part[]::new), frame, cellOf.stream().mapToInt(Integer::intValue).toArray(), cells.toLongArray());
	}

	private static void add(List<Part> parts, List<double[]> frames, List<Integer> cellOf, LongArrayList cells, Part part, Vec3 a,
							Vec3 b, float half, Vec3 lightFrom) {
		long cell = BlockPos.containing(lightFrom).asLong();
		int index = cells.indexOf(cell);
		if (index < 0) {
			index = cells.size();
			cells.add(cell);
		}
		parts.add(part);
		frames.add(frame(a, b, half));
		cellOf.add(index);
	}

	/**
	 * A rod's frame from a to b, {@code half} its half thickness: the way along, its two sides (square to it and to each
	 * other, the first level unless the rod is near upright), and a.
	 */
	static double[] frame(Vec3 a, Vec3 b, float half) {
		Vec3 along = b.subtract(a), d = along.normalize();
		Vec3 u = d.cross(Math.abs(d.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).normalize().scale(half);
		Vec3 v = d.cross(u).normalize().scale(half);
		return new double[] {along.x, along.y, along.z, u.x, u.y, u.z, v.x, v.y, v.z, a.x, a.y, a.z};
	}
}
