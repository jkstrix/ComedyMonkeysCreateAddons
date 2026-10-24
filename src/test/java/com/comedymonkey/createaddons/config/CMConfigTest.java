package com.comedymonkey.createaddons.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.MachineEnchantments;
import com.comedymonkey.createaddons.content.chest.ChestTier;
import com.comedymonkey.createaddons.content.wellspring.WellspringSource;
import com.comedymonkey.createaddons.testutil.ModResourceFiles;
import com.comedymonkey.createaddons.testutil.TestConfig;
import com.google.gson.JsonObject;

import net.neoforged.neoforge.common.ModConfigSpec;

class CMConfigTest {
	private static final CMConfig.Server S = CMConfig.SERVER;

	@BeforeAll
	static void load() {
		TestConfig.loadDefaults();
		// In case an earlier test class left a value changed
		TestConfig.reset();
	}

	@AfterEach
	void reset() {
		TestConfig.reset();
	}

	/** Every setting a spec holds, from the public final fields of its holder. */
	private static List<ModConfigSpec.ConfigValue<?>> values(Object holder) throws IllegalAccessException {
		List<ModConfigSpec.ConfigValue<?>> values = new ArrayList<>();
		for (Field field : holder.getClass().getFields())
			if (!Modifier.isStatic(field.getModifiers()) && ModConfigSpec.ConfigValue.class.isAssignableFrom(field.getType()))
				values.add((ModConfigSpec.ConfigValue<?>) field.get(holder));
		return values;
	}

	private static void checkSpec(ModConfigSpec spec, Object holder) throws IllegalAccessException {
		List<ModConfigSpec.ConfigValue<?>> values = values(holder);
		assertFalse(values.isEmpty());
		Set<List<String>> paths = new HashSet<>();
		for (ModConfigSpec.ConfigValue<?> value : values) {
			List<String> path = value.getPath();
			assertTrue(paths.add(path), "two settings at " + path);
			ModConfigSpec.ValueSpec valueSpec = spec.getSpec().get(path);
			assertNotNull(valueSpec, "no spec for " + path);
			assertTrue(valueSpec.test(value.getDefault()), "default of " + path + " is outside its range");
			assertEquals(value.getDefault(), value.get(), "loaded value of " + path);
			assertNotNull(valueSpec.getTranslationKey(), "no translation key for " + path);
			assertNotNull(valueSpec.getComment(), "no comment for " + path);
		}
	}

	@Test
	void serverDefaultsAreInRangeAndLoad() throws IllegalAccessException {
		assertTrue(CMConfig.SERVER_SPEC.isLoaded());
		checkSpec(CMConfig.SERVER_SPEC, CMConfig.SERVER);
	}

	@Test
	void clientDefaultsAreInRangeAndLoad() throws IllegalAccessException {
		checkSpec(CMConfig.CLIENT_SPEC, CMConfig.CLIENT);
	}

	/** Missing names show up as raw keys in the config screens. */
	@Test
	void everySettingHasAName() throws IllegalAccessException {
		Path file = ModResourceFiles.find("assets", ComedyMonkeysCreateAddons.MOD_ID, "lang", "en_us.json");
		Assumptions.assumeTrue(file != null, "lang file not reachable");
		JsonObject lang = ModResourceFiles.readJson(file).getAsJsonObject();
		for (var pair : List.of(new Object[] {CMConfig.SERVER_SPEC, CMConfig.SERVER}, new Object[] {CMConfig.CLIENT_SPEC, CMConfig.CLIENT}))
			for (ModConfigSpec.ConfigValue<?> value : values(pair[1])) {
				ModConfigSpec.ValueSpec valueSpec = ((ModConfigSpec) pair[0]).getSpec().get(value.getPath());
				assertTrue(lang.has(valueSpec.getTranslationKey()), "no name for " + valueSpec.getTranslationKey());
			}
	}

	@Test
	void outOfRangeValuesAreRejected() {
		ModConfigSpec.ValueSpec breed = CMConfig.SERVER_SPEC.getSpec().get(S.penBreedSeconds.getPath());
		assertFalse(breed.test(0));
		assertFalse(breed.test(36001));
		assertTrue(breed.test(1));
	}

	// ---------------------------------------------------------------- Wellspring

	@Test
	void generationRisesWithEachUpgrade() {
		for (WellspringSource source : WellspringSource.values()) {
			if (source == WellspringSource.NONE)
				continue;
			int one = S.generationRate(source, 1), two = S.generationRate(source, 2), three = S.generationRate(source, 3);
			assertTrue(0 < one && one < two && two < three, source + ": " + one + ", " + two + ", " + three);
		}
	}

	@Test
	void generationRates() {
		assertEquals(0, S.generationRate(WellspringSource.WATER, 0));
		assertEquals(0, S.generationRate(WellspringSource.WATER, -1));
		assertEquals(100, S.generationRate(WellspringSource.WATER, 1));
		assertEquals(250, S.generationRate(WellspringSource.WATER, 2));
		assertEquals(500, S.generationRate(WellspringSource.WATER, 3));
		// More than the slots allow counts as the most
		assertEquals(500, S.generationRate(WellspringSource.WATER, 7));
		assertEquals(50, S.generationRate(WellspringSource.LAVA, 3));
		assertEquals(0, S.generationRate(WellspringSource.NONE, 3));
	}

	@Test
	void generationIsCappedAtTheUpgradeLimit() {
		TestConfig.set(S.maxUpgrades, 2);
		assertEquals(250, S.generationRate(WellspringSource.WATER, 3));
		assertEquals(2, S.maxUpgrades());
	}

	@Test
	void tankCapacityGrowsPerUpgradeUpToTheLimit() {
		assertEquals(8_000, S.tankCapacity(0));
		assertEquals(24_000, S.tankCapacity(1));
		assertEquals(56_000, S.tankCapacity(3));
		assertEquals(56_000, S.tankCapacity(10));
		TestConfig.set(S.maxCapacityUpgrades, 0);
		assertEquals(8_000, S.tankCapacity(3));
	}

	@Test
	void upgradeLimitsFitTheSlots() {
		assertTrue(S.maxUpgrades() <= CMConfig.MAX_UPGRADE_SLOTS);
		assertTrue(S.maxCapacityUpgrades() <= CMConfig.MAX_UPGRADE_SLOTS);
	}

	// ---------------------------------------------------------------- chests

	@Test
	void chestMultipliersRiseByTier() {
		assertEquals(2, S.chestMultiplier(ChestTier.COPPER));
		assertEquals(4, S.chestMultiplier(ChestTier.IRON));
		assertEquals(8, S.chestMultiplier(ChestTier.GOLD));
		// Diamond doubles the slots instead
		assertEquals(S.chestMultiplier(ChestTier.GOLD), S.chestMultiplier(ChestTier.DIAMOND));
		assertEquals(16, S.chestMultiplier(ChestTier.NETHERITE));
		TestConfig.set(S.goldChestMultiplier, 5);
		assertEquals(5, S.chestMultiplier(ChestTier.DIAMOND));
	}

	// ---------------------------------------------------------------- what the comments promise

	@Test
	void crucibleComment() {
		// "At 256 RPM the default of 12 needs 3072 stress units."
		assertEquals(3072, S.crucibleStressImpact() * 256, 1e-9);
	}

	@Test
	void magmaEngineComments() {
		// "At the default 64 RPM, 128 gives 8192 stress units."
		assertEquals(8192, S.magmaEngineCapacity() * S.magmaEngineSpeed(), 1e-9);
		// "At the default 128 RPM, 128 gives 16384 stress units."
		assertEquals(16384, S.superchargedMagmaEngineCapacity() * S.superchargedMagmaEngineSpeed(), 1e-9);
		// "twice a Magma Engine's speed and power, for twice the lava"
		assertEquals(2 * S.magmaEngineSpeed(), S.superchargedMagmaEngineSpeed());
		assertEquals(2 * S.magmaEngineLavaPerSecond(), S.superchargedMagmaEngineLavaPerSecond());
		assertEquals(2 * S.magmaEngineCapacity() * S.magmaEngineSpeed(),
			S.superchargedMagmaEngineCapacity() * S.superchargedMagmaEngineSpeed(), 1e-9);
	}

	@Test
	void electricityComments() {
		// "one Lava Generator run a motor at 16,384 SU"
		assertEquals(16_384, S.generatorFePerTick() / S.motorFePerStress(), 1e-9);
		// "one turbine run two and a half motors flat out"
		assertEquals(2.5, S.turbineFePerTick() / (S.motorMaxStress() * S.motorFePerStress()), 1e-9);
		// "ten times as efficient as the Lava Generator"
		double generator = S.generatorFePerTick() / (double) S.generatorLavaPerSecond();
		double turbine = S.turbineFePerTick() / (double) S.turbineLavaPerSecond();
		assertEquals(10, turbine / generator, 1e-9);
	}

	@Test
	void tradeBotComments() {
		// "At the default 4 FE a block that's 25,000 blocks of rolling."
		assertEquals(25_000, S.tradeBotFullCharge() / S.tradeBotFePerBlock());
	}

	@Test
	void animalPenDefaults() {
		assertTrue(CMConfig.Server.get(S.brassPenStress) > CMConfig.Server.get(S.penStress));
		// As long as vanilla: 300 s between breedings, 1200 s to grow up; eggs "vanilla is 300 to 600"
		assertEquals(300, CMConfig.Server.get(S.penBreedSeconds));
		assertEquals(1200, CMConfig.Server.get(S.penGrowSeconds));
		int egg = CMConfig.Server.get(S.penEggSeconds);
		assertTrue(egg >= 300 && egg <= 600);
	}

	@Test
	void villagersSitInSeatsByDefault() {
		assertTrue(S.villagersSitInSeats.getDefault());
		assertTrue(S.villagersSitInSeats());
		TestConfig.set(S.villagersSitInSeats, false);
		assertFalse(S.villagersSitInSeats());
	}

	@Test
	void fruitfulAndBountifulComments() {
		// "0.5 makes I 1.5x, II 2x, III 2.5x as fast"
		for (ModConfigSpec.DoubleValue bonus : List.of(S.penFruitfulBonus, S.harvesterBountifulBonus)) {
			assertEquals(1.0f, MachineEnchantments.speedMultiplier(0, bonus), 1e-6);
			assertEquals(1.5f, MachineEnchantments.speedMultiplier(1, bonus), 1e-6);
			assertEquals(2.0f, MachineEnchantments.speedMultiplier(2, bonus), 1e-6);
			assertEquals(2.5f, MachineEnchantments.speedMultiplier(3, bonus), 1e-6);
		}
	}

	@Test
	void overridesAreSeenAndReset() {
		TestConfig.set(S.penBreedSeconds, 77);
		assertEquals(77, CMConfig.Server.get(S.penBreedSeconds));
		TestConfig.reset();
		assertEquals(300, CMConfig.Server.get(S.penBreedSeconds));
	}
}
