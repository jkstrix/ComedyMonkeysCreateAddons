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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

class UpgradedBarrelBlockEntityTest {
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
		Block block = ModBlocks.UPGRADED_BARREL.get();
		return block.defaultBlockState().setValue(UpgradedBarrelBlock.TIER, tier);
	}

	private static UpgradedBarrelBlockEntity barrel(ChestTier tier) {
		return ModBlockEntities.UPGRADED_BARREL.get().create(BlockPos.ZERO, state(tier));
	}

	@Test
	void slotsAndLimitsFollowTheTier() {
		for (ChestTier tier : ChestTier.values()) {
			UpgradedStorage storage = barrel(tier);
			assertEquals(tier.slots(), storage.getContainerSize(), tier.name());
			assertEquals(64 * tier.multiplier(), storage.getMaxStackSize(new ItemStack(Items.STONE)), tier.name());
		}
	}

	@Test
	void saveAndLoadKeepOversizedStacks() {
		UpgradedBarrelBlockEntity barrel = barrel(ChestTier.NETHERITE);
		barrel.getItems().set(0, new ItemStack(Items.STONE, 1024));
		barrel.getItems().set(53, new ItemStack(Items.ENDER_PEARL, 256));

		CompoundTag tag = barrel.saveWithoutMetadata(registries);
		UpgradedBarrelBlockEntity loaded = barrel(ChestTier.NETHERITE);
		loaded.loadWithComponents(tag, registries);
		for (int i = 0; i < 54; i++)
			assertTrue(ItemStack.matches(barrel.getItem(i), loaded.getItem(i)), "slot " + i);
	}

	@Test
	void downgradeCompactsIntoTheSmallerInventory() {
		UpgradedBarrelBlockEntity barrel = barrel(ChestTier.NETHERITE);
		NonNullList<ItemStack> items = barrel.getItems();
		items.set(10, new ItemStack(Items.STONE, 100));
		items.set(50, new ItemStack(Items.DIRT, 100));

		barrel.setBlockState(state(ChestTier.IRON));
		UpgradedStorage iron = barrel;
		assertEquals(27, iron.getContainerSize());
		assertTrue(iron.getItem(0).is(Items.STONE));
		assertTrue(iron.getItem(1).is(Items.DIRT));
		assertEquals(100, iron.getItem(1).getCount());
	}

	@Test
	void contentsStayOutOfItemComponents() {
		UpgradedBarrelBlockEntity barrel = barrel(ChestTier.GOLD);
		barrel.getItems().set(0, new ItemStack(Items.STONE, 500));
		assertFalse(barrel.collectComponents().has(DataComponents.CONTAINER));
	}
}
