package com.comedymonkey.createaddons.content.chest;

import com.comedymonkey.createaddons.registry.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;

/**
 * Item form of an upgraded barrel, for Carry On (which draws carried blocks through their item) and creative
 * pick-block. Not craftable or in the creative tab; the tier travels in the block-state component.
 */
public class UpgradedBarrelItem extends BlockItem {
	public UpgradedBarrelItem(UpgradedBarrelBlock block, Properties properties) {
		super(block, properties);
	}

	public static ItemStack of(ChestTier tier) {
		ItemStack stack = new ItemStack(ModItems.UPGRADED_BARREL.get());
		stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(UpgradedBarrelBlock.TIER, tier));
		return stack;
	}

	public static ChestTier tierOf(ItemStack stack) {
		BlockItemStateProperties properties = stack.get(DataComponents.BLOCK_STATE);
		ChestTier tier = properties == null ? null : properties.get(UpgradedBarrelBlock.TIER);
		return tier == null ? ChestTier.COPPER : tier;
	}

	/** "Copper Barrel", "Diamond Barrel"... */
	@Override
	public Component getName(ItemStack stack) {
		return tierOf(stack).barrelName();
	}
}
