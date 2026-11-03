package com.comedymonkey.createaddons.content.jetpack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.juggernaut.SpringEngineBlockEntity;
import com.comedymonkey.createaddons.registry.ModDataComponents;
import com.comedymonkey.createaddons.registry.ModItems;
import com.comedymonkey.createaddons.testutil.TestConfig;
import com.mojang.serialization.JsonOps;

import net.minecraft.world.item.ItemStack;

/**
 * The Clockwork Jetpack's numbers (jetpack spec): Create Jetpack's options and defaults except hover descend (D4, D5),
 * its wind and Overwound (D12, D34-D36), its toggles (D8) and the Winding Station's backtank fill rule (D40).
 */
class JetpacksTest {
	@BeforeEach
	void config() {
		TestConfig.loadDefaults();
	}

	@AfterEach
	void restore() {
		TestConfig.reset();
	}

	/** D4, D5: Create Jetpack's defaults, with hover holding height. */
	@Test
	void createJetpackDefaultsWithHoverDescendZero() {
		assertEquals(450, CMConfig.SERVER.jetpackSecondsPerWind.getDefault());
		assertEquals(900, CMConfig.SERVER.jetpackSecondsPerWindHover.getDefault());
		assertEquals(900, CMConfig.SERVER.jetpackSecondsPerWindFlight.getDefault());
		assertEquals(0.02, CMConfig.SERVER.jetpackHorizontalSpeed.getDefault());
		assertEquals(0.4, CMConfig.SERVER.jetpackVerticalSpeed.getDefault());
		assertEquals(0.6, CMConfig.SERVER.jetpackAcceleration.getDefault());
		assertEquals(0.0, CMConfig.SERVER.jetpackHoverDescend.getDefault());
		assertEquals(1.8, CMConfig.SERVER.jetpackSwimModifier.getDefault());
		assertEquals(1.25, CMConfig.SERVER.jetpackElytraBoost.getDefault());
		assertEquals(1.4, CMConfig.SERVER.jetpackFlightSpeedModifier.getDefault());
		assertEquals(-1, CMConfig.SERVER.jetpackHeightAboveGroundLimit.getDefault());
		assertTrue(CMConfig.SERVER.jetpackEnchantmentsIsBlacklist.getDefault());
		assertTrue(CMConfig.CLIENT.jetpackOverlay.getDefault());
		assertEquals(6, CMConfig.CLIENT.jetpackOverlayX.getDefault());
		assertEquals(6, CMConfig.CLIENT.jetpackOverlayY.getDefault());
	}

	/** D16 (user, 2026-11-03): boosting flies at 1.4 x 1.25 an Elytra's shallow glide, about 14 blocks a second. */
	@Test
	void boostIsAbout14BlocksASecond() {
		assertEquals(14, JetpackFlight.boostSpeed() * 20, 1e-6);
	}

	/** D12, D16: a full wind is 450 s of thrust, 900 of hover or of boost in flight mode (gliding is free). */
	@Test
	void windRates() {
		assertEquals(450 * 20, Jetpacks.baseWind());
		assertEquals(0.5, Jetpacks.hoverRate(), 1e-9);
		assertEquals(0.5, Jetpacks.flightRate(), 1e-9);
		assertEquals(10, Jetpacks.windPerAir()); // a full base wind fills as a backtank's 900 air
	}

	/** D34: Overwound I-III gives a third more a level: twice as much at III. The Spring Engine's likewise (D36). */
	@Test
	void overwoundThirdMoreALevel() {
		int base = Jetpacks.maxWind(0);
		assertEquals(base * 4 / 3, Jetpacks.maxWind(1));
		assertEquals(base * 5 / 3, Jetpacks.maxWind(2));
		assertEquals(base * 2, Jetpacks.maxWind(3));
		assertEquals(SpringEngineBlockEntity.capacity() * 2, SpringEngineBlockEntity.capacity(3));
		assertEquals(SpringEngineBlockEntity.capacity(), SpringEngineBlockEntity.capacity(0));
	}

	/** Wind is kept in range: never under empty, never over full, even after the config lowers full. */
	@Test
	void windStaysInRange() {
		ItemStack jetpack = new ItemStack(ModItems.CLOCKWORK_JETPACK.get());
		assertEquals(0, Jetpacks.wind(jetpack));
		Jetpacks.setWind(jetpack, -50);
		assertEquals(0, Jetpacks.wind(jetpack));
		Jetpacks.setWind(jetpack, Integer.MAX_VALUE);
		assertTrue(Jetpacks.full(jetpack));
		assertEquals(1f, Jetpacks.fill(jetpack), 1e-6);
		TestConfig.set(CMConfig.SERVER.jetpackSecondsPerWind, 225);
		assertEquals(1f, Jetpacks.fill(jetpack), 1e-6);
		assertEquals(225 * 20, Jetpacks.wind(jetpack));
		assertFalse(Jetpacks.isJetpack(new ItemStack(ModItems.MAINSPRING.get())));
		assertTrue(Jetpacks.isJetpack(new ItemStack(ModItems.NETHERITE_CLOCKWORK_JETPACK.get())));
	}

	/** The smithing table carries the wind over to the Netherite jetpack as a component (D19). */
	@Test
	void windIsAComponent() {
		ItemStack jetpack = new ItemStack(ModItems.CLOCKWORK_JETPACK.get());
		Jetpacks.setWind(jetpack, 1234);
		ItemStack netherite = jetpack.transmuteCopy(ModItems.NETHERITE_CLOCKWORK_JETPACK.get(), 1);
		assertEquals(1234, netherite.get(ModDataComponents.JETPACK_WIND.get()));
	}

	/** D8: engine on, hover off, wings folded by default; saved and read back. */
	@Test
	void togglesDefaultAndRoundTrip() {
		assertTrue(JetpackState.DEFAULT.engine());
		assertFalse(JetpackState.DEFAULT.hover());
		assertFalse(JetpackState.DEFAULT.wings());
		JetpackState state = new JetpackState(false, true, true);
		assertEquals(state, JetpackState.CODEC.parse(JsonOps.INSTANCE, JetpackState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow()).getOrThrow());
		assertEquals(JetpackState.DEFAULT, JetpackState.CODEC.parse(JsonOps.INSTANCE, new com.google.gson.JsonObject()).getOrThrow());
	}

	/** D40: a Copper Backtank's fill: a full base wind in 9 s at 256 RPM, about ten and a half minutes on a Hand Crank. */
	@Test
	void stationFillsAsABacktank() {
		assertEquals(5, JetpackWindingStationBlockEntity.airPerStep(256));
		assertEquals(0, JetpackWindingStationBlockEntity.waitAfterStep(256));
		assertEquals(1, JetpackWindingStationBlockEntity.airPerStep(32));
		assertEquals(13, JetpackWindingStationBlockEntity.waitAfterStep(32));
		assertEquals(5, JetpackWindingStationBlockEntity.airPerStep(-256)); // either direction
		int fast = Jetpacks.baseWind() / (5 * Jetpacks.windPerAir()) * 1;
		assertEquals(180, fast); // ticks
		int crank = Jetpacks.baseWind() / (1 * Jetpacks.windPerAir()) * 14;
		assertEquals(12_600, crank); // ticks: 10.5 minutes
	}
}
