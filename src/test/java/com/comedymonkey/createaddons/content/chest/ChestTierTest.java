package com.comedymonkey.createaddons.content.chest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.testutil.TestConfig;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;

class ChestTierTest {
	@BeforeEach
	void loadConfig() {
		TestConfig.loadDefaults();
	}

	@AfterEach
	void resetConfig() {
		TestConfig.reset();
	}

	private static String key(Component component) {
		return assertInstanceOf(TranslatableContents.class, component.getContents()).getKey();
	}

	@Test
	void defaultMultipliers() {
		assertEquals(2, ChestTier.COPPER.multiplier());
		assertEquals(4, ChestTier.IRON.multiplier());
		assertEquals(8, ChestTier.GOLD.multiplier());
		assertEquals(8, ChestTier.DIAMOND.multiplier());
		assertEquals(16, ChestTier.NETHERITE.multiplier());
	}

	@Test
	void diamondFollowsTheGoldMultiplier() {
		TestConfig.set(CMConfig.SERVER.goldChestMultiplier, 3);
		assertEquals(3, ChestTier.GOLD.multiplier());
		assertEquals(3, ChestTier.DIAMOND.multiplier());
		assertEquals(16, ChestTier.NETHERITE.multiplier());
	}

	@Test
	void eachTierReadsItsOwnConfigValue() {
		TestConfig.set(CMConfig.SERVER.copperChestMultiplier, 5);
		TestConfig.set(CMConfig.SERVER.ironChestMultiplier, 6);
		TestConfig.set(CMConfig.SERVER.netheriteChestMultiplier, 64);
		assertEquals(5, ChestTier.COPPER.multiplier());
		assertEquals(6, ChestTier.IRON.multiplier());
		assertEquals(64, ChestTier.NETHERITE.multiplier());
	}

	@Test
	void slots() {
		assertEquals(27, ChestTier.COPPER.slots());
		assertEquals(27, ChestTier.IRON.slots());
		assertEquals(27, ChestTier.GOLD.slots());
		assertEquals(54, ChestTier.DIAMOND.slots());
		assertEquals(54, ChestTier.NETHERITE.slots());
	}

	/** The upgrade GUI assumes every tier above one that fits also fits (it compares ordinals). */
	@Test
	void defaultCapacityNeverShrinksGoingUp() {
		ChestTier[] tiers = ChestTier.values();
		for (int i = 1; i < tiers.length; i++) {
			ChestTier lower = tiers[i - 1];
			ChestTier higher = tiers[i];
			assertTrue(higher.multiplier() >= lower.multiplier(), higher + " multiplier");
			assertTrue(higher.slots() >= lower.slots(), higher + " slots");
			assertTrue(higher.multiplier() * higher.slots() > lower.multiplier() * lower.slots(), higher + " capacity");
		}
	}

	@Test
	void onlyNetheriteIsBlastProof() {
		for (ChestTier tier : ChestTier.values())
			assertEquals(tier == ChestTier.NETHERITE, tier.blastProof(), tier.name());
	}

	@Test
	void isAboveIsStrict() {
		assertTrue(ChestTier.IRON.isAbove(ChestTier.COPPER));
		assertTrue(ChestTier.NETHERITE.isAbove(ChestTier.DIAMOND));
		assertFalse(ChestTier.GOLD.isAbove(ChestTier.GOLD));
		assertFalse(ChestTier.COPPER.isAbove(ChestTier.NETHERITE));
	}

	@Test
	void eachTierHasItsOwnMatchingUpgradeItem() {
		Set<Item> seen = new HashSet<>();
		for (ChestTier tier : ChestTier.values()) {
			Item item = tier.upgradeItem();
			ChestUpgradeItem upgrade = assertInstanceOf(ChestUpgradeItem.class, item);
			assertEquals(tier, upgrade.getTier());
			assertTrue(seen.add(item), "duplicate upgrade item for " + tier);
		}
	}

	@Test
	void codecRoundTripsBySerializedName() {
		for (ChestTier tier : ChestTier.values()) {
			JsonElement json = ChestTier.CODEC.encodeStart(JsonOps.INSTANCE, tier).getOrThrow();
			assertEquals(tier.getSerializedName(), json.getAsString());
			assertEquals(tier, ChestTier.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
		}
		assertEquals("netherite", ChestTier.NETHERITE.getSerializedName());
	}

	@Test
	void names() {
		assertEquals("comedymonkeysaddons.chest.gold", key(ChestTier.GOLD.chestName()));
		assertEquals("comedymonkeysaddons.barrel.diamond", key(ChestTier.DIAMOND.barrelName()));
	}
}
