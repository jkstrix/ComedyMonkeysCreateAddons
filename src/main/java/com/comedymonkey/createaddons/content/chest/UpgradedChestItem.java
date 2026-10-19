package com.comedymonkey.createaddons.content.chest;

import com.comedymonkey.createaddons.registry.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;

/**
 * Item form of an upgraded chest. Not craftable or in the creative tab: it exists so the chest has something to show
 * when carried (Carry On draws carried blocks through their item) and so creative pick-block (with Ctrl, including
 * contents) works. The tier travels in the item's block-state component.
 */
public class UpgradedChestItem extends BlockItem {
	public UpgradedChestItem(UpgradedChestBlock block, Properties properties) {
		super(block, properties);
	}

	public static ItemStack of(ChestTier tier) {
		ItemStack stack = new ItemStack(ModItems.UPGRADED_CHEST.get());
		stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(UpgradedChestBlock.TIER, tier));
		return stack;
	}

	public static ChestTier tierOf(ItemStack stack) {
		BlockItemStateProperties properties = stack.get(DataComponents.BLOCK_STATE);
		ChestTier tier = properties == null ? null : properties.get(UpgradedChestBlock.TIER);
		return tier == null ? ChestTier.COPPER : tier;
	}

	/** "Copper Chest", "Diamond Chest"... */
	@Override
	public Component getName(ItemStack stack) {
		return tierOf(stack).chestName();
	}
}
