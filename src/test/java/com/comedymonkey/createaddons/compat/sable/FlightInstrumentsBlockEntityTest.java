package com.comedymonkey.createaddons.compat.sable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Map;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Compass and heading maths of the Flight Instruments, off a ship (no level). */
class FlightInstrumentsBlockEntityTest {
	private static final double EPSILON = 1e-9;

	// ---- Readings.compassPoint()

	@Test
	void compassPointsAtTheirCentres() {
		String[] points = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
		for (int i = 0; i < points.length; i++)
			assertEquals(points[i], compassPoint(i * 45), "heading " + i * 45);
	}

	@Test
	void compassPointBoundariesRoundToTheNearestPoint() {
		assertEquals("N", compassPoint(22.4));
		assertEquals("NE", compassPoint(22.5));
		assertEquals("NW", compassPoint(337.4));
		// Just short of a full turn is north again, not off the end of the table
		assertEquals("N", compassPoint(337.5));
		assertEquals("N", compassPoint(359.99));
	}

	@Test
	void compassPointCopesWithHeadingsOutsideATurn() {
		assertEquals("N", compassPoint(360));
		assertEquals("E", compassPoint(450));
		assertEquals("W", compassPoint(-90));
	}

	// ---- headingOf()

	@Test
	void headingIsCompassStyle() throws Exception {
		assertEquals(0, headingOf(new Vector3d(0, 0, -1)), EPSILON);
		assertEquals(90, headingOf(new Vector3d(1, 0, 0)), EPSILON);
		assertEquals(180, headingOf(new Vector3d(0, 0, 1)), EPSILON);
		assertEquals(270, headingOf(new Vector3d(-1, 0, 0)), EPSILON);
		assertEquals(45, headingOf(new Vector3d(1, 0, -1)), EPSILON);
		assertEquals(315, headingOf(new Vector3d(-1, 0, -1)), EPSILON);
	}

	@Test
	void headingIgnoresLengthAndClimb() throws Exception {
		assertEquals(90, headingOf(new Vector3d(5, 0, 0)), EPSILON);
		assertEquals(90, headingOf(new Vector3d(1, 3, 0)), EPSILON);
	}

	@Test
	void headingIsAlwaysWithinOneTurn() throws Exception {
		for (int deg = -720; deg <= 720; deg += 5) {
			double a = Math.toRadians(deg);
			double heading = headingOf(new Vector3d(Math.sin(a), 0, -Math.cos(a)));
			assertTrue(heading >= 0 && heading < 360, deg + " gave " + heading);
			assertEquals(0, angleBetween(heading, deg), 1e-6, "at " + deg);
		}
	}

	// ---- read() off a ship

	@Test
	void offAShipItReadsWhereTheBlockStands() {
		BlockPos pos = new BlockPos(10, 72, -4);
		FlightInstrumentsBlockEntity.Readings r = FlightInstrumentsBlockEntity.read(null, pos, Direction.NORTH);
		assertEquals(72.5, r.altitude(), EPSILON);
		assertEquals(0, r.airspeed(), EPSILON);
		assertEquals(0, r.climb(), EPSILON);
		assertEquals(0, r.pitch(), EPSILON);
		assertEquals(0, r.roll(), EPSILON);
		assertFalse(r.onShip());
	}

	@Test
	void forwardIsAwayFromThePilotIntoThePanel() {
		// The panel faces the pilot, so the pilot (and the ship's forward) look the other way
		Map<Direction, Double> expected = Map.of(
			Direction.NORTH, 180.0, Direction.SOUTH, 0.0, Direction.EAST, 270.0, Direction.WEST, 90.0);
		for (var entry : expected.entrySet())
			assertEquals(entry.getValue(), FlightInstrumentsBlockEntity.read(null, BlockPos.ZERO, entry.getKey()).heading(),
				EPSILON, entry.getKey().toString());
	}

	// ---- helpers

	private static String compassPoint(double heading) {
		return new FlightInstrumentsBlockEntity.Readings(0, 0, 0, heading, 0, 0, false).compassPoint();
	}

	/** headingOf() is private, so it is reached by reflection. */
	private static double headingOf(Vector3d direction) throws Exception {
		Method m = FlightInstrumentsBlockEntity.class.getDeclaredMethod("headingOf", Vector3d.class);
		m.setAccessible(true);
		return (double) m.invoke(null, direction);
	}

	/** Smallest difference between two angles in degrees, 0 to 180. */
	private static double angleBetween(double a, double b) {
		double d = ((a - b) % 360 + 360) % 360;
		return Math.min(d, 360 - d);
	}
}
