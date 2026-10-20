package com.comedymonkey.createaddons.compat.sable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;

import org.joml.AxisAngle4d;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.testutil.TestConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Compact Gyroscope's controller maths. Its helpers are private, so they are reached by reflection. The physics
 * step itself needs a Sable sub-level and rigid body, so only the pure parts are covered here.
 */
class CompactGyroscopeBlockEntityTest {
	private static final double EPSILON = 1e-9;

	@BeforeEach
	void loadConfig() {
		TestConfig.loadDefaults();
	}

	@AfterEach
	void resetConfig() {
		TestConfig.reset();
	}

	// ---- clampingFactor(): damping may slow the spin to a stop but never reverse it

	@Test
	void changeAlongTheSpinIsNotDamping() throws Exception {
		assertEquals(0, clampingFactor(new Vector3d(1, 0, 0), new Vector3d(0.5, 0, 0)), EPSILON);
	}

	@Test
	void noSpinAndNoChangeGivesZeroNotNaN() throws Exception {
		double f = clampingFactor(new Vector3d(), new Vector3d());
		assertFalse(Double.isNaN(f));
		assertEquals(0, f, EPSILON);
	}

	@Test
	void changeAcrossTheSpinIsLeftAlone() throws Exception {
		assertEquals(1, clampingFactor(new Vector3d(2, 0, 0), new Vector3d(0, 3, 0)), EPSILON);
	}

	@Test
	void smallDampingIsBarelyScaled() throws Exception {
		// k = 0.2, v = 4: the linear branch, 1 - k / 2v
		assertEquals(1 - 0.2 / 8, clampingFactor(new Vector3d(2, 0, 0), new Vector3d(-0.1, 0, 0)), EPSILON);
	}

	@Test
	void largeDampingIsScaledExponentially() throws Exception {
		// k = 5, v = 1
		assertEquals((1 - Math.exp(-5)) / 5, clampingFactor(new Vector3d(1, 0, 0), new Vector3d(-5, 0, 0)), EPSILON);
	}

	@Test
	void dampingNeverReversesTheSpin() throws Exception {
		Vector3d spin = new Vector3d(0.3, -1.2, 0.7);
		double v = spin.lengthSquared();
		for (double scale = 1e-6; scale < 1e6; scale *= 1.7) {
			Vector3d change = new Vector3d(spin).mul(-scale);
			double f = clampingFactor(spin, change);
			assertTrue(f >= 0 && f <= 1, "factor " + f + " at scale " + scale);
			// The spin left along its own direction after the scaled change: v - f * k, where k = scale * v
			double remaining = v - f * scale * v;
			assertTrue(remaining >= -1e-12, "reversed at scale " + scale + ": " + remaining);
		}
	}

	@Test
	void theTwoBranchesMeetAtTheirBoundary() throws Exception {
		// 10k = v is where the linear branch hands over
		Vector3d spin = new Vector3d(1, 0, 0);
		double justBelow = clampingFactor(spin, new Vector3d(-0.0999999, 0, 0));
		double justAbove = clampingFactor(spin, new Vector3d(-0.1000001, 0, 0));
		assertEquals(justBelow, justAbove, 0.005);
	}

	@Test
	void tinySpinStaysFinite() throws Exception {
		double f = clampingFactor(new Vector3d(1e-6, 0, 0), new Vector3d(-1, 0, 0));
		assertTrue(Double.isFinite(f) && f >= 0 && f <= 1, "factor " + f);
	}

	// ---- tiltDegrees()

	@Test
	void levelShipHasNoTilt() throws Exception {
		assertEquals(0, tiltDegrees(new Quaterniond()), EPSILON);
		// Turning about the vertical isn't a tilt
		assertEquals(0, tiltDegrees(about(0, 1, 0, 123)), 1e-6);
	}

	@Test
	void pitchOrRollIsTheTilt() throws Exception {
		assertEquals(30, tiltDegrees(about(1, 0, 0, 30)), 1e-6);
		assertEquals(45, tiltDegrees(about(0, 0, 1, -45)), 1e-6);
		assertEquals(20, tiltDegrees(about(0, 1, 0, 70).mul(about(1, 0, 0, 20))), 1e-6);
	}

	@Test
	void upsideDownIsOneEighty() throws Exception {
		double tilt = tiltDegrees(about(1, 0, 0, 180));
		assertFalse(Double.isNaN(tilt));
		assertEquals(180, tilt, 1e-6);
	}

	// ---- bar(): the goggle bar is always 18 segments in three bands

	@Test
	void barSegmentsByBand() throws Exception {
		assertBar(0, 0, 0, 0, 18);
		assertBar(1, 6, 6, 6, 0);
		assertBar(0.5f, 6, 3, 0, 9);
		assertBar(0.3f, 5, 0, 0, 13);
		assertBar(0.75f, 6, 6, 2, 4);
	}

	@Test
	void barClampsOutOfRangeFractions() throws Exception {
		assertBar(2, 6, 6, 6, 0);
		assertBar(-1, 0, 0, 0, 18);
		assertBar(Float.NaN, 0, 0, 0, 18);
	}

	// ---- strengthFraction() and getFlywheelAngle(), on a real block entity

	@Test
	void strengthGrowsWithSpeedUpToTheReferenceRpm() throws Exception {
		CompactGyroscopeBlockEntity gyro = gyroscope();
		// Default reference: 256 RPM
		assertEquals(0, strength(gyro, 0), EPSILON);
		assertEquals(0.5, strength(gyro, 128), EPSILON);
		assertEquals(0.5, strength(gyro, -128), EPSILON);
		assertEquals(1, strength(gyro, 256), EPSILON);
		assertEquals(1, strength(gyro, -1024), EPSILON);
	}

	@Test
	void strengthFollowsTheConfiguredReferenceRpm() throws Exception {
		TestConfig.set(CMConfig.SERVER.gyroReferenceRpm, 64.0);
		CompactGyroscopeBlockEntity gyro = gyroscope();
		assertEquals(0.5, strength(gyro, 32), EPSILON);
		assertEquals(1, strength(gyro, 128), EPSILON);
	}

	@Test
	void flywheelAngleInterpolatesBetweenTicks() throws Exception {
		CompactGyroscopeBlockEntity gyro = gyroscope();
		assertEquals(105, flywheelDegrees(gyro, 100, 110, 0.5f), 1e-4);
		assertEquals(100, flywheelDegrees(gyro, 100, 110, 0), 1e-4);
		assertEquals(110, flywheelDegrees(gyro, 100, 110, 1), 1e-4);
	}

	@Test
	void flywheelAngleTakesTheShortWayAcrossTheWrap() throws Exception {
		CompactGyroscopeBlockEntity gyro = gyroscope();
		// Spinning forwards past 360
		assertEquals(0, normalise(flywheelDegrees(gyro, 350, 10, 0.5f)), 1e-4);
		assertEquals(355, normalise(flywheelDegrees(gyro, 350, 10, 0.25f)), 1e-4);
		// Spinning backwards past 0
		assertEquals(5, normalise(flywheelDegrees(gyro, 10, 350, 0.25f)), 1e-4);
		// Java's % leaves a reversed flywheel's angle negative
		assertEquals(0, normalise(flywheelDegrees(gyro, -355, -5, 0.5f)), 1e-4);
	}

	// ---- helpers

	private static double clampingFactor(Vector3dc velocity, Vector3dc change) throws Exception {
		Method m = CompactGyroscopeBlockEntity.class.getDeclaredMethod("clampingFactor", Vector3dc.class, Vector3dc.class);
		m.setAccessible(true);
		return (double) m.invoke(null, velocity, change);
	}

	/**
	 * A rotation of {@code degrees} about an axis. Built from an axis-angle because JOML 1.10.5's
	 * {@code Quaterniond.rotationX/Y/Z} produce wrong quaternions (e.g. rotationX(30°) gives w = 0).
	 */
	private static Quaterniond about(double x, double y, double z, double degrees) {
		return new Quaterniond(new AxisAngle4d(Math.toRadians(degrees), x, y, z));
	}

	private static double tiltDegrees(Quaterniondc orientation) throws Exception {
		Method m = CompactGyroscopeBlockEntity.class.getDeclaredMethod("tiltDegrees", Quaterniondc.class);
		m.setAccessible(true);
		return (double) m.invoke(null, orientation);
	}

	private static void assertBar(float fraction, int low, int mid, int high, int empty) throws Exception {
		Method m = CompactGyroscopeBlockEntity.class.getDeclaredMethod("bar", float.class, ChatFormatting.class,
			ChatFormatting.class, ChatFormatting.class);
		m.setAccessible(true);
		Component bar = (Component) m.invoke(null, fraction, ChatFormatting.GOLD, ChatFormatting.YELLOW, ChatFormatting.GREEN);
		assertEquals("|".repeat(18), bar.getString(), "fraction " + fraction);

		Map<ChatFormatting, Integer> counts = new EnumMap<>(ChatFormatting.class);
		for (Component part : bar.getSiblings()) {
			String text = part.getString();
			if (text.isEmpty())
				continue;
			TextColor colour = part.getStyle().getColor();
			for (ChatFormatting f : new ChatFormatting[] {ChatFormatting.GOLD, ChatFormatting.YELLOW, ChatFormatting.GREEN,
				ChatFormatting.DARK_GRAY})
				if (TextColor.fromLegacyFormat(f).equals(colour))
					counts.merge(f, text.length(), Integer::sum);
		}
		String at = "fraction " + fraction;
		assertEquals(low, counts.getOrDefault(ChatFormatting.GOLD, 0), at);
		assertEquals(mid, counts.getOrDefault(ChatFormatting.YELLOW, 0), at);
		assertEquals(high, counts.getOrDefault(ChatFormatting.GREEN, 0), at);
		assertEquals(empty, counts.getOrDefault(ChatFormatting.DARK_GRAY, 0), at);
	}

	/** A Compact Gyroscope block entity, not in any level. Only there when Sable is loaded. */
	private static CompactGyroscopeBlockEntity gyroscope() {
		Block block = BuiltInRegistries.BLOCK.get(ComedyMonkeysCreateAddons.asResource("compact_gyroscope"));
		assumeTrue(block != Blocks.AIR, "Sable isn't loaded, so the Compact Gyroscope isn't registered");
		return new CompactGyroscopeBlockEntity(BlockPos.ZERO, block.defaultBlockState());
	}

	private static double strength(CompactGyroscopeBlockEntity gyro, double rpm) throws Exception {
		gyro.effectiveSpeed.setValue(rpm);
		Method m = CompactGyroscopeBlockEntity.class.getDeclaredMethod("strengthFraction");
		m.setAccessible(true);
		return (double) m.invoke(gyro);
	}

	private static double flywheelDegrees(CompactGyroscopeBlockEntity gyro, float last, float now, float partialTicks)
		throws Exception {
		setFloat(gyro, "flywheelAngleLast", last);
		setFloat(gyro, "flywheelAngle", now);
		return Math.toDegrees(gyro.getFlywheelAngle(partialTicks));
	}

	private static void setFloat(CompactGyroscopeBlockEntity target, String name, float value) throws Exception {
		Field f = CompactGyroscopeBlockEntity.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setFloat(target, value);
	}

	private static double normalise(double degrees) {
		double d = ((degrees % 360) + 360) % 360;
		// 359.99999 and 0 are the same angle
		return d > 360 - 1e-4 ? 0 : d;
	}
}
