package com.comedymonkey.createaddons.content.chest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.testutil.TestConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

class UpgradedChestMountedStorageTest {
	private static DynamicOps<Tag> ops;

	@BeforeAll
	static void setUpOps() {
		ops = VanillaRegistries.createLookup().createSerializationContext(NbtOps.INSTANCE);
	}

	@BeforeEach
	void loadConfig() {
		TestConfig.loadDefaults();
	}

	private static UpgradedChestMountedStorage mounted(ChestTier tier, NonNullList<ItemStack> items) {
		return new UpgradedChestMountedStorage(tier, items);
	}

	private static Codec<UpgradedChestMountedStorage> codec() {
		return UpgradedChestMountedStorage.CODEC.codec();
	}

	private static Tag encode(UpgradedChestMountedStorage storage) {
		return codec().encodeStart(ops, storage).getOrThrow();
	}

	private static UpgradedChestMountedStorage decode(Tag tag) {
		return codec().parse(ops, tag).getOrThrow();
	}

	@Test
	void codecRoundTripKeepsOversizedStacks() {
		NonNullList<ItemStack> items = NonNullList.withSize(54, ItemStack.EMPTY);
		items.set(0, new ItemStack(Items.STONE, 500));
		ItemStack named = new ItemStack(Items.DIAMOND, 300);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Shiny"));
		items.set(26, named);
		items.set(53, new ItemStack(Items.DIAMOND_SWORD));
		UpgradedChestMountedStorage storage = mounted(ChestTier.DIAMOND, items);

		UpgradedChestMountedStorage back = decode(encode(storage));
		assertEquals(54, back.getSlots());
		for (int i = 0; i < 54; i++)
			assertTrue(ItemStack.matches(items.get(i), back.getStackInSlot(i)), "slot " + i);
	}

	@Test
	void encodesTheTierAndOnlyFilledSlots() {
		NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
		items.set(4, new ItemStack(Items.STONE, 1000));
		CompoundTag tag = (CompoundTag) encode(mounted(ChestTier.GOLD, items));
		assertEquals("gold", tag.getString("tier"));
		ListTag entries = tag.getList("items", Tag.TAG_COMPOUND);
		assertEquals(1, entries.size());
		assertEquals(4, entries.getCompound(0).getInt("slot"));
		assertEquals(1000, entries.getCompound(0).getInt("count"));
	}

	@Test
	void decodesToTheTiersSlotCount() {
		UpgradedChestMountedStorage back = decode(encode(mounted(ChestTier.NETHERITE,
			NonNullList.withSize(54, ItemStack.EMPTY))));
		assertEquals(54, back.getSlots());
		back = decode(encode(mounted(ChestTier.COPPER, NonNullList.withSize(27, ItemStack.EMPTY))));
		assertEquals(27, back.getSlots());
	}

	@Test
	void depositsUseTheMultipliedLimit() {
		NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
		UpgradedChestMountedStorage storage = mounted(ChestTier.GOLD, items);
		assertEquals(512, storage.getSlotLimit(0));
		assertEquals(488, storage.insertItem(0, new ItemStack(Items.STONE, 1000), true).getCount());
		assertTrue(items.get(0).isEmpty());
		assertEquals(488, storage.insertItem(0, new ItemStack(Items.STONE, 1000), false).getCount());
		assertEquals(512, items.get(0).getCount());
	}

	@Test
	void withdrawalsHandOutNormalStacks() {
		NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
		items.set(0, new ItemStack(Items.STONE, 512));
		UpgradedChestMountedStorage storage = mounted(ChestTier.GOLD, items);
		assertEquals(64, storage.extractItem(0, 512, true).getCount());
		assertEquals(512, items.get(0).getCount());
		assertEquals(64, storage.extractItem(0, 512, false).getCount());
		assertEquals(448, items.get(0).getCount());
	}
}
