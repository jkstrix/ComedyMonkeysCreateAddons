package com.comedymonkey.createaddons.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.content.light.FestoonBlockEntity;
import com.comedymonkey.createaddons.content.light.Festoons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * FLY-02: the Festoon strings' rods, laid out once and shared by the renderer and the Flywheel visual: the same pieces,
 * sag and bulbs the renderer always drew, and the rod shape stretched onto each.
 */
class FestoonGeometryTest {
	private static final double EPS = 1e-9;
	private static final BlockPos HOOK = new BlockPos(0, 64, 0), FAR = new BlockPos(10, 64, 0);

	private static FestoonGeometry.Layout eastToWest(boolean held) {
		return FestoonGeometry.layOut(HOOK, Direction.EAST, List.of(new FestoonBlockEntity.Span(FAR, Direction.WEST, held, true)));
	}

	private static void assertVec(Vec3 want, Vec3 got, String what) {
		assertTrue(want.distanceTo(got) < 1e-5, what + ": want " + want + ", got " + got);
	}

	@Test
	void segmentsAreThreeABlockAndAtLeastEight() {
		assertEquals(8, FestoonGeometry.segments(0.5));
		assertEquals(8, FestoonGeometry.segments(8 / 3.0));
		assertEquals(9, FestoonGeometry.segments(2.7));
		assertEquals(33, FestoonGeometry.segments(10.875));
	}

	/** A 10-block string east to west: 10.875 blocks from eye to eye, 33 pieces of flex and 5 bulbs on sockets. */
	@Test
	void aKnownString() {
		FestoonGeometry.Layout layout = eastToWest(true);
		Vec3 origin = Vec3.atLowerCornerOf(HOOK);
		Vec3 a = Festoons.eye(HOOK, Direction.EAST), b = Festoons.eye(FAR, Direction.WEST);
		assertVec(new Vec3(1 / 16.0, 64.75, 0.5), a, "near eye");
		assertVec(new Vec3(10 + 15 / 16.0, 64.75, 0.5), b, "far eye");
		assertEquals(10.875, a.distanceTo(b), EPS);
		assertEquals(33 + 5 * 2, layout.rods());
		// the flex: from eye to eye, joined up, along the sag
		assertVec(a.subtract(origin), layout.a(0), "start");
		assertVec(b.subtract(origin), layout.b(32), "end");
		for (int i = 0; i < 33; i++) {
			assertEquals(FestoonGeometry.Part.FLEX, layout.parts()[i]);
			assertVec(Festoons.along(a, b, i / 33.0).subtract(origin), layout.a(i), "piece " + i);
			if (i > 0)
				assertVec(layout.b(i - 1), layout.a(i), "join " + i);
		}
		// the middle sags a twelfth of the length
		assertEquals(64.75 - 10.875 / 12 - 64, Festoons.along(a, b, 0.5).subtract(origin).y, EPS);
		// each bulb: its socket hangs from the string, its glass from the socket
		double[] bulbs = Festoons.bulbs(10.875);
		assertEquals(5, bulbs.length);
		for (int k = 0; k < 5; k++) {
			int socket = 33 + 2 * k, glass = socket + 1;
			Vec3 at = Festoons.along(a, b, bulbs[k]).subtract(origin);
			assertEquals(FestoonGeometry.Part.SOCKET, layout.parts()[socket]);
			assertEquals(FestoonGeometry.Part.BULB, layout.parts()[glass]);
			assertVec(at, layout.a(socket), "socket top " + k);
			assertVec(at.subtract(0, 1.25 / 16, 0), layout.b(socket), "neck " + k);
			assertVec(at.subtract(0, 1.25 / 16, 0), layout.a(glass), "glass top " + k);
			assertVec(at.subtract(0, 3.75 / 16, 0), layout.b(glass), "glass bottom " + k);
			// the socket and the glass take their light from where the bulb hangs
			long cell = BlockPos.containing(at.add(origin)).asLong();
			assertEquals(cell, layout.cells()[layout.cell()[socket]]);
			assertEquals(cell, layout.cells()[layout.cell()[glass]]);
		}
		// a piece of flex takes its light from where its middle is; each block looked up once
		long mid = BlockPos.containing(layout.a(16).add(layout.b(16)).scale(0.5).add(origin)).asLong();
		assertEquals(mid, layout.cells()[layout.cell()[16]]);
		assertEquals(layout.cells().length, java.util.Arrays.stream(layout.cells()).distinct().count());
	}

	/** The far hook draws a string it doesn't hold. */
	@Test
	void onlyHeldStrings() {
		assertEquals(0, eastToWest(false).rods());
	}

	/** Each rod's matrix puts the rod shape's ends on its ends, and makes it as thick as its part. */
	@Test
	void theRodShapeFitsEachRod() {
		FestoonGeometry.Layout layout = eastToWest(true);
		for (int i : new int[] {0, 16, 32, 33, 34}) {
			Matrix4f m = layout.matrix(i, 0, 0, 0);
			Vec3 a = layout.a(i), b = layout.b(i);
			Vector3f start = m.transformPosition(new Vector3f(0, 0, 0)), end = m.transformPosition(new Vector3f(1, 0, 0));
			assertVec(a, new Vec3(start.x, start.y, start.z), "rod " + i + " start");
			assertVec(b, new Vec3(end.x, end.y, end.z), "rod " + i + " end");
			float half = switch (layout.parts()[i]) {
				case FLEX -> FestoonGeometry.FLEX;
				case SOCKET -> FestoonGeometry.SOCKET;
				case BULB -> FestoonGeometry.BULB;
			};
			Vector3f side = m.transformDirection(new Vector3f(0, 1, 0)), up = m.transformDirection(new Vector3f(0, 0, 1)),
				along = m.transformDirection(new Vector3f(1, 0, 0));
			assertEquals(half, side.length(), 1e-6);
			assertEquals(half, up.length(), 1e-6);
			assertEquals(0, side.dot(up), 1e-7);
			assertEquals(0, side.dot(along), 1e-7);
			assertEquals(0, up.dot(along), 1e-7);
		}
		// moved to where the visual draws from
		Vector3f moved = layout.matrix(0, 5, -2, 7).transformPosition(new Vector3f(0, 0, 0));
		assertVec(layout.a(0).add(5, -2, 7), new Vec3(moved.x, moved.y, moved.z), "moved");
	}

	/** The rod shape: 6 faces of 4 corners, each corner on its face, every face showing the middle of the texture. */
	@Test
	void theRodShape() {
		float[] rod = FestoonGeometry.ROD;
		assertEquals(FestoonGeometry.ROD_VERTICES * FestoonGeometry.ROD_STRIDE, rod.length);
		for (int k = 0; k < FestoonGeometry.ROD_VERTICES; k++) {
			int p = k * FestoonGeometry.ROD_STRIDE;
			float x = rod[p], y = rod[p + 1], z = rod[p + 2], nx = rod[p + 3], ny = rod[p + 4], nz = rod[p + 5];
			assertEquals(1, nx * nx + ny * ny + nz * nz, 1e-6);
			// on the face its normal points out of: x at 0 or 1, y and z at -1 or 1
			if (nx != 0)
				assertEquals(nx > 0 ? 1 : 0, x);
			if (ny != 0)
				assertEquals(ny, y);
			if (nz != 0)
				assertEquals(nz, z);
			assertTrue(rod[p + 6] == FestoonGeometry.U0 || rod[p + 6] == FestoonGeometry.U1);
			assertTrue(rod[p + 7] == FestoonGeometry.V0 || rod[p + 7] == FestoonGeometry.V1);
		}
	}

	/**
	 * The rod shape stretched by a rod's frame (as the renderer draws it, and as the visual's matrix does) gives the very
	 * corners, normals and texture corners of the square rod the renderer drew before, face by face.
	 */
	@Test
	void sameCornersAsTheOldRod() {
		FestoonGeometry.Layout layout = FestoonGeometry.layOut(HOOK, Direction.EAST, List.of(
			new FestoonBlockEntity.Span(FAR, Direction.WEST, true, true),
			// steep: its pieces near the ends are near upright, which turns the rod's sides another way
			new FestoonBlockEntity.Span(new BlockPos(1, 54, 0), Direction.WEST, true, true)));
		float[] rod = FestoonGeometry.ROD;
		boolean upright = false;
		for (int i = 0; i < layout.rods(); i++) {
			Vec3 a = layout.a(i), b = layout.b(i);
			float half = switch (layout.parts()[i]) {
				case FLEX -> FestoonGeometry.FLEX;
				case SOCKET -> FestoonGeometry.SOCKET;
				case BULB -> FestoonGeometry.BULB;
			};
			double[][] old = oldRod(a, b, half);
			upright |= Math.abs(b.subtract(a).normalize().y) >= 0.9;
			Matrix4f m = layout.matrix(i, 0, 0, 0);
			for (int k = 0; k < FestoonGeometry.ROD_VERTICES; k++) {
				int p = k * FestoonGeometry.ROD_STRIDE;
				Vector3f at = m.transformPosition(new Vector3f(rod[p], rod[p + 1], rod[p + 2]));
				assertVec(new Vec3(old[k][0], old[k][1], old[k][2]), new Vec3(at.x, at.y, at.z), "rod " + i + " corner " + k);
				Vector3f n = m.normal(new org.joml.Matrix3f()).transform(new Vector3f(rod[p + 3], rod[p + 4], rod[p + 5])).normalize();
				assertVec(new Vec3(old[k][3], old[k][4], old[k][5]), new Vec3(n.x, n.y, n.z), "rod " + i + " normal " + k);
				// and as the renderer draws it
				float[] c = new float[6];
				FestoonGeometry.corner(layout.frame(), i, k, c);
				assertVec(new Vec3(old[k][0], old[k][1], old[k][2]), new Vec3(c[0], c[1], c[2]), "rod " + i + " drawn corner " + k);
				assertVec(new Vec3(old[k][3], old[k][4], old[k][5]), new Vec3(c[3], c[4], c[5]), "rod " + i + " drawn normal " + k);
				assertEquals(old[k][6], rod[p + 6], 1e-6);
				assertEquals(old[k][7], rod[p + 7], 1e-6);
			}
		}
		assertTrue(upright, "no near-upright piece tried");
	}

	/** The renderer's square rod as it was before (FestoonRenderer.beam): each corner's x, y, z, normal, u, v. */
	private static double[][] oldRod(Vec3 a, Vec3 b, float half) {
		Vec3 d = b.subtract(a).normalize();
		Vec3 u = d.cross(Math.abs(d.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).normalize().scale(half);
		Vec3 v = d.cross(u).normalize().scale(half);
		Vec3[] ring = {u.add(v), u.subtract(v), u.scale(-1).subtract(v), u.scale(-1).add(v)};
		java.util.List<double[]> out = new java.util.ArrayList<>();
		for (int i = 0; i < 4; i++) {
			Vec3 s0 = ring[i], s1 = ring[(i + 1) % 4], n = s0.add(s1).normalize();
			quad(out, a.add(s0), b.add(s0), b.add(s1), a.add(s1), n);
		}
		quad(out, a.add(ring[3]), a.add(ring[2]), a.add(ring[1]), a.add(ring[0]), d.scale(-1));
		quad(out, b.add(ring[0]), b.add(ring[1]), b.add(ring[2]), b.add(ring[3]), d);
		return out.toArray(double[][]::new);
	}

	private static void quad(java.util.List<double[]> out, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 n) {
		out.add(new double[] {p0.x, p0.y, p0.z, n.x, n.y, n.z, 0.4f, 0.2f});
		out.add(new double[] {p1.x, p1.y, p1.z, n.x, n.y, n.z, 0.6f, 0.2f});
		out.add(new double[] {p2.x, p2.y, p2.z, n.x, n.y, n.z, 0.6f, 0.8f});
		out.add(new double[] {p3.x, p3.y, p3.z, n.x, n.y, n.z, 0.4f, 0.8f});
	}
}
