package com.comedymonkey.createaddons.advancement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.registry.ModItems;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

class CMActionTriggerTest {
	private static RegistryOps<JsonElement> ops;

	@BeforeAll
	static void setUp() {
		ops = VanillaRegistries.createLookup().createSerializationContext(JsonOps.INSTANCE);
	}

	private static CMActionTrigger.Instance instance(String action, Optional<ItemPredicate> item, Optional<Integer> min) {
		return new CMActionTrigger.Instance(Optional.empty(), action, item, min);
	}

	private static CMActionTrigger.Instance decode(JsonElement json) {
		return CMActionTrigger.Instance.CODEC.parse(ops, json).getOrThrow();
	}

	private static JsonElement encode(CMActionTrigger.Instance instance) {
		return CMActionTrigger.Instance.CODEC.encodeStart(ops, instance).getOrThrow();
	}

	@Test
	void triggerUsesTheInstanceCodec() {
		assertSame(CMActionTrigger.Instance.CODEC, new CMActionTrigger().codec());
	}

	// ---------------------------------------------------------------- matching

	@Test
	void matchesOnlyItsAction() {
		CMActionTrigger.Instance cull = instance(ModAdvancements.PEN_CULL, Optional.empty(), Optional.empty());
		assertTrue(cull.matches(ModAdvancements.PEN_CULL, ItemStack.EMPTY, 0));
		assertFalse(cull.matches(ModAdvancements.PEN_FULL, ItemStack.EMPTY, 0));
		assertFalse(cull.matches("", ItemStack.EMPTY, 0));
	}

	@Test
	void minValueIsInclusive() {
		CMActionTrigger.Instance three = instance(ModAdvancements.WELLSPRING_UPGRADE, Optional.empty(), Optional.of(3));
		assertFalse(three.matches(ModAdvancements.WELLSPRING_UPGRADE, ItemStack.EMPTY, 2));
		assertTrue(three.matches(ModAdvancements.WELLSPRING_UPGRADE, ItemStack.EMPTY, 3));
		assertTrue(three.matches(ModAdvancements.WELLSPRING_UPGRADE, ItemStack.EMPTY, 4));
	}

	@Test
	void itemMustMatchWhenGiven() {
		ItemPredicate water = ItemPredicate.Builder.item().of(ModItems.WATER_UPGRADE.get()).build();
		CMActionTrigger.Instance upgrade = instance(ModAdvancements.WELLSPRING_UPGRADE, Optional.of(water), Optional.empty());
		assertTrue(upgrade.matches(ModAdvancements.WELLSPRING_UPGRADE, new ItemStack(ModItems.WATER_UPGRADE.get()), 1));
		assertFalse(upgrade.matches(ModAdvancements.WELLSPRING_UPGRADE, new ItemStack(ModItems.HONEY_UPGRADE.get()), 1));
		assertFalse(upgrade.matches(ModAdvancements.WELLSPRING_UPGRADE, ItemStack.EMPTY, 1));
	}

	// ---------------------------------------------------------------- the codec

	@Test
	void readsTheDocumentedExample() {
		CMActionTrigger.Instance example = decode(JsonParser.parseString(
			"{\"action\": \"wellspring_upgrade\", \"item\": {\"items\": \"comedymonkeysaddons:water_upgrade\"}, \"min_value\": 3}"));
		assertEquals("wellspring_upgrade", example.action());
		assertTrue(example.item().isPresent());
		assertEquals(Optional.of(3), example.minValue());
		assertTrue(example.player().isEmpty());
		assertTrue(example.matches("wellspring_upgrade", new ItemStack(ModItems.WATER_UPGRADE.get()), 3));
		assertFalse(example.matches("wellspring_upgrade", new ItemStack(ModItems.WATER_UPGRADE.get()), 2));
		assertFalse(example.matches("wellspring_upgrade", new ItemStack(Items.WATER_BUCKET), 3));
	}

	@Test
	void onlyTheActionIsRequired() {
		CMActionTrigger.Instance bare = decode(JsonParser.parseString("{\"action\": \"pen_full\"}"));
		assertEquals("pen_full", bare.action());
		assertTrue(bare.item().isEmpty());
		assertTrue(bare.minValue().isEmpty());

		JsonObject json = encode(bare).getAsJsonObject();
		assertEquals(1, json.size());
		assertEquals("pen_full", json.get("action").getAsString());
	}

	@Test
	void anActionMustBeNamed() {
		assertTrue(CMActionTrigger.Instance.CODEC.parse(ops, JsonParser.parseString("{\"min_value\": 3}")).error().isPresent());
	}

	@Test
	void roundTrips() {
		ItemPredicate chest = ItemPredicate.Builder.item().of(ModItems.COPPER_CHEST_UPGRADE.get(), ModItems.IRON_CHEST_UPGRADE.get()).build();
		CMActionTrigger.Instance original = instance(ModAdvancements.CHEST_UPGRADE, Optional.of(chest), Optional.of(2));
		JsonElement json = encode(original);
		CMActionTrigger.Instance read = decode(json);
		assertEquals(json, encode(read));
		assertEquals(original.action(), read.action());
		assertEquals(original.minValue(), read.minValue());
		assertTrue(read.matches(ModAdvancements.CHEST_UPGRADE, new ItemStack(ModItems.IRON_CHEST_UPGRADE.get()), 2));
		assertFalse(read.matches(ModAdvancements.CHEST_UPGRADE, new ItemStack(ModItems.GOLD_CHEST_UPGRADE.get()), 2));
	}
}
