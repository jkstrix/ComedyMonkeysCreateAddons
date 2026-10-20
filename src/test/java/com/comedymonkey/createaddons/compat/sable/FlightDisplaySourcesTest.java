package com.comedymonkey.createaddons.compat.sable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

/** The text each Display Link source writes. */
class FlightDisplaySourcesTest {
	private static final String DEG = "°";

	@Test
	void altitudeInWholeMetres() {
		assertEquals("124 m", format(FlightDisplaySources.ALTITUDE, readings(123.6, 0, 0, 0, 0, 0)));
		assertEquals("-6 m", format(FlightDisplaySources.ALTITUDE, readings(-5.6, 0, 0, 0, 0, 0)));
		assertEquals("0 m", format(FlightDisplaySources.ALTITUDE, readings(0, 0, 0, 0, 0, 0)));
	}

	@Test
	void airspeedToOneDecimal() {
		assertEquals("12.3 m/s", format(FlightDisplaySources.AIRSPEED, readings(0, 12.34, 0, 0, 0, 0)));
		assertEquals("0.0 m/s", format(FlightDisplaySources.AIRSPEED, readings(0, 0, 0, 0, 0, 0)));
	}

	@Test
	void climbIsSigned() {
		assertEquals("+1.5 m/s", format(FlightDisplaySources.CLIMB, readings(0, 0, 1.5, 0, 0, 0)));
		assertEquals("-2.0 m/s", format(FlightDisplaySources.CLIMB, readings(0, 0, -2, 0, 0, 0)));
		assertEquals("+0.0 m/s", format(FlightDisplaySources.CLIMB, readings(0, 0, 0, 0, 0, 0)));
	}

	@Test
	void headingIsThreeDigitsAndACompassPoint() {
		assertEquals("005" + DEG + " N", format(FlightDisplaySources.HEADING, readings(0, 0, 0, 5, 0, 0)));
		assertEquals("090" + DEG + " E", format(FlightDisplaySources.HEADING, readings(0, 0, 0, 90, 0, 0)));
		assertEquals("225" + DEG + " SW", format(FlightDisplaySources.HEADING, readings(0, 0, 0, 225, 0, 0)));
		assertEquals("315" + DEG + " NW", format(FlightDisplaySources.HEADING, readings(0, 0, 0, 315.2, 0, 0)));
	}

	@Test
	void headingJustShortOfNorthReadsZero() {
		assertEquals("000" + DEG + " N", format(FlightDisplaySources.HEADING, readings(0, 0, 0, 359.7, 0, 0)));
	}

	@Test
	void attitudeIsSignedPitchAndRoll() {
		assertEquals("P +10" + DEG + " R -5" + DEG, format(FlightDisplaySources.ATTITUDE, readings(0, 0, 0, 0, 10, -5)));
		assertEquals("P -45" + DEG + " R +30" + DEG, format(FlightDisplaySources.ATTITUDE, readings(0, 0, 0, 0, -45.2, 29.6)));
	}

	private static FlightInstrumentsBlockEntity.Readings readings(double altitude, double airspeed, double climb,
		double heading, double pitch, double roll) {
		return new FlightInstrumentsBlockEntity.Readings(altitude, airspeed, climb, heading, pitch, roll, true);
	}

	private static String format(Function<FlightInstrumentsBlockEntity.Readings, String> reading,
		FlightInstrumentsBlockEntity.Readings readings) {
		return reading.apply(readings);
	}
}
