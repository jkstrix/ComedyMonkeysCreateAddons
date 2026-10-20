package com.comedymonkey.createaddons.content.wellspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.testutil.ModResourceFiles;
import com.google.gson.JsonObject;

import net.minecraft.world.item.Item;

/** The upgrade kinds: what they generate, their items and their saved names. */
class WellspringUpgradeTest {
	@Test
	void eachItemCarriesItsOwnUpgrade() {
		for (WellspringUpgrade upgrade : WellspringUpgrade.values()) {
			Item item = upgrade.item();
			WellspringUpgradeItem upgradeItem = assertInstanceOf(WellspringUpgradeItem.class, item, upgrade.name());
			assertSame(upgrade, upgradeItem.getUpgrade(), upgrade.name());
		}
	}

	@Test
	void itemsAreDistinct() {
		Set<Item> items = new HashSet<>();
		for (WellspringUpgrade upgrade : WellspringUpgrade.values())
			assertTrue(items.add(upgrade.item()), "shared item for " + upgrade.name());
	}

	@Test
	void sources() {
		assertEquals(WellspringSource.WATER, WellspringUpgrade.WATER.source());
		assertEquals(WellspringSource.LAVA, WellspringUpgrade.DRIPSTONE.source());
		assertEquals(WellspringSource.HONEY, WellspringUpgrade.HONEY.source());
		assertEquals(WellspringSource.CHOCOLATE, WellspringUpgrade.CHOCOLATE.source());
		assertEquals(WellspringSource.NONE, WellspringUpgrade.CAPACITY.source());
	}

	@Test
	void onlyCapacityIsNotGeneration() {
		for (WellspringUpgrade upgrade : WellspringUpgrade.values())
			assertEquals(upgrade != WellspringUpgrade.CAPACITY, upgrade.isGeneration(), upgrade.name());
	}

	@Test
	void forSourceFindsTheGenerator() {
		for (WellspringUpgrade upgrade : WellspringUpgrade.values())
			if (upgrade.isGeneration())
				assertSame(upgrade, WellspringUpgrade.forSource(upgrade.source()));
		// Every real fluid has a generator
		for (WellspringSource source : WellspringSource.values())
			if (source != WellspringSource.NONE)
				assertEquals(source, WellspringUpgrade.forSource(source).source());
	}

	@Test
	void forNoneIsNull() {
		// Must not hand back the Capacity Upgrade, which also has source NONE
		assertNull(WellspringUpgrade.forSource(WellspringSource.NONE));
	}

	@Test
	void namesRoundTrip() {
		Set<String> names = new HashSet<>();
		for (WellspringUpgrade upgrade : WellspringUpgrade.values()) {
			assertTrue(names.add(upgrade.getSerializedName()));
			assertSame(upgrade, WellspringUpgrade.byName(upgrade.getSerializedName()));
		}
		// The legacy save format stored these exact names
		assertSame(WellspringUpgrade.WATER, WellspringUpgrade.byName("water"));
		assertSame(WellspringUpgrade.DRIPSTONE, WellspringUpgrade.byName("dripstone"));
		assertSame(WellspringUpgrade.CAPACITY, WellspringUpgrade.byName("capacity"));
	}

	@Test
	void gogglesNameEveryUpgradeType() {
		Path lang = ModResourceFiles.find("assets", "comedymonkeysaddons", "lang", "en_us.json");
		assumeTrue(lang != null, "en_us.json not found");
		JsonObject json = ModResourceFiles.readJson(lang).getAsJsonObject();
		for (WellspringUpgrade upgrade : WellspringUpgrade.values()) {
			String key = "comedymonkeysaddons.gui.goggles.upgrade_type." + upgrade.getSerializedName();
			assertTrue(json.has(key), key);
		}
	}

	@Test
	void unknownNameIsNull() {
		assertNull(WellspringUpgrade.byName("lava"));
		assertNull(WellspringUpgrade.byName(""));
	}
}
