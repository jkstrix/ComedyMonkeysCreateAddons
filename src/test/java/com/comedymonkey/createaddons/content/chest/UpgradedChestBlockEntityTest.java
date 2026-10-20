package com.comedymonkey.createaddons.content.chest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.testutil.TestConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

class UpgradedChestBlockEntityTest {
	private static HolderLookup.Provider registries;

	@BeforeAll
	static void setUpRegistries() {
		registries = VanillaRegistries.createLookup();
	}

	@BeforeEach
	void loadConfig() {
		TestConfig.loadDefaults();
	}

	private static BlockState state(ChestTier tier) {
		Block block = ModBlocks.UPGRADED_CHEST.get();
		return block.defaultBlockState().setValue(UpgradedChestBlock.TIER, tier);
	}

	private static UpgradedChestBlockEntity chest(ChestTier tier) {
		return ModBlockEntities.UPGRADED_CHEST.get().create(BlockPos.ZERO, state(tier));
	}

	private static ItemStack named(Item item, String name, int count) {
		ItemStack stack = new ItemStack(item, count);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
		return stack;
	}

	private static int total(UpgradedStorage storage, Item item) {
		int total = 0;
		for (ItemStack stack : storage.getItems())
			if (stack.is(item))
				total += stack.getCount();
		return total;
	}

	private static UpgradedChestBlockEntity reload(UpgradedChestBlockEntity chest, ChestTier tier) {
		CompoundTag tag = chest.saveWithoutMetadata(registries);
		UpgradedChestBlockEntity loaded = chest(tier);
		loaded.loadWithComponents(tag, registries);
		return loaded;
	}

	@Test
	void slotsFollowTheTier() {
		for (ChestTier tier : ChestTier.values()) {
			UpgradedStorage storage = chest(tier);
			assertEquals(tier, storage.getTier());
			assertEquals(tier.slots(), storage.getContainerSize(), tier.name());
		}
	}

	@Test
	void stackLimitsFollowTheTier() {
		UpgradedStorage gold = chest(ChestTier.GOLD);
		assertEquals(512, gold.getMaxStackSize());
		assertEquals(512, gold.getMaxStackSize(new ItemStack(Items.STONE)));
		assertEquals(128, gold.getMaxStackSize(new ItemStack(Items.ENDER_PEARL)));
		assertEquals(1, gold.getMaxStackSize(new ItemStack(Items.DIAMOND_SWORD)));
		assertEquals(1024, chest(ChestTier.NETHERITE).getMaxStackSize(new ItemStack(Items.STONE)));
	}

	@Test
	void saveAndLoadKeepOversizedStacks() {
		UpgradedChestBlockEntity chest = chest(ChestTier.GOLD);
		chest.getItems().set(0, new ItemStack(Items.STONE, 500));
		chest.getItems().set(26, named(Items.DIAMOND, "Shiny", 300));

		UpgradedStorage loaded = reload(chest, ChestTier.GOLD);
		assertEquals(27, loaded.getContainerSize());
		for (int i = 0; i < 27; i++)
			assertTrue(ItemStack.matches(chest.getItem(i), loaded.getItem(i)), "slot " + i);
	}

	@Test
	void saveAndLoadKeepAFullNetheriteChest() {
		UpgradedChestBlockEntity chest = chest(ChestTier.NETHERITE);
		for (int i = 0; i < 54; i++)
			chest.getItems().set(i, new ItemStack(Items.STONE, 1024));
		UpgradedStorage loaded = reload(chest, ChestTier.NETHERITE);
		assertEquals(54 * 1024, total(loaded, Items.STONE));
	}

	@Test
	void holdsUpgradeIsSavedAndDefaultsToTrue() {
		UpgradedChestBlockEntity chest = chest(ChestTier.IRON);
		assertTrue(chest.saveWithoutMetadata(registries).getBoolean("HoldsUpgrade"));

		CompoundTag without = chest.saveWithoutMetadata(registries);
		without.putBoolean("HoldsUpgrade", false);
		UpgradedChestBlockEntity notHolding = chest(ChestTier.IRON);
		notHolding.loadWithComponents(without, registries);
		assertFalse(notHolding.saveWithoutMetadata(registries).getBoolean("HoldsUpgrade"));

		// Data from before the flag existed: the chest holds its upgrade
		CompoundTag legacy = chest.saveWithoutMetadata(registries);
		legacy.remove("HoldsUpgrade");
		notHolding.loadWithComponents(legacy, registries);
		assertTrue(notHolding.saveWithoutMetadata(registries).getBoolean("HoldsUpgrade"));
	}

	@Test
	void downgradeCompactsIntoTheSmallerInventory() {
		UpgradedChestBlockEntity chest = chest(ChestTier.DIAMOND);
		NonNullList<ItemStack> items = chest.getItems();
		items.set(5, new ItemStack(Items.STONE, 500));
		items.set(30, new ItemStack(Items.DIRT, 2));
		items.set(53, new ItemStack(Items.DIAMOND, 1));

		chest.setBlockState(state(ChestTier.GOLD));
		UpgradedStorage gold = chest;
		assertEquals(27, gold.getContainerSize());
		assertTrue(gold.getItem(0).is(Items.STONE));
		assertEquals(500, gold.getItem(0).getCount());
		assertTrue(gold.getItem(1).is(Items.DIRT));
		assertTrue(gold.getItem(2).is(Items.DIAMOND));
	}

	@Test
	void upgradeToDiamondKeepsPositions() {
		UpgradedChestBlockEntity chest = chest(ChestTier.GOLD);
		chest.getItems().set(26, new ItemStack(Items.STONE, 300));
		chest.setBlockState(state(ChestTier.DIAMOND));
		assertEquals(54, chest.getContainerSize());
		assertEquals(300, chest.getItem(26).getCount());
	}

	@Test
	void fillResizesToTheTier() {
		UpgradedStorage diamond = chest(ChestTier.DIAMOND);
		NonNullList<ItemStack> contents = NonNullList.withSize(27, ItemStack.EMPTY);
		contents.set(26, new ItemStack(Items.STONE, 10));
		diamond.fill(contents);
		assertEquals(54, diamond.getContainerSize());
		assertEquals(10, diamond.getItem(26).getCount());
	}

	@Test
	void loadMakesRoomForSlotsPastTheTier() {
		// Data can arrive before the block state that matches it (e.g. moved by a physics mod)
		UpgradedChestBlockEntity diamond = chest(ChestTier.DIAMOND);
		diamond.getItems().set(40, new ItemStack(Items.STONE, 64));
		UpgradedStorage gold = reload(diamond, ChestTier.GOLD);
		assertEquals(54, gold.getContainerSize());
		assertEquals(64, gold.getItem(40).getCount());
	}

	@Test
	void contentsStayOutOfItemComponents() {
		// The vanilla container component can't hold stacks above 99
		UpgradedChestBlockEntity chest = chest(ChestTier.GOLD);
		chest.getItems().set(0, new ItemStack(Items.STONE, 500));
		assertFalse(chest.collectComponents().has(DataComponents.CONTAINER));
	}

	@Test
	void automationFillsPast64() {
		UpgradedStorage gold = chest(ChestTier.GOLD);
		UpgradedChestItemHandler handler = new UpgradedChestItemHandler(gold);
		assertEquals(488, handler.insertItem(0, new ItemStack(Items.STONE, 1000), false).getCount());
		assertEquals(512, gold.getItem(0).getCount());
		assertEquals(64, handler.extractItem(0, 1000, false).getCount());
		assertEquals(448, gold.getItem(0).getCount());
	}
}
