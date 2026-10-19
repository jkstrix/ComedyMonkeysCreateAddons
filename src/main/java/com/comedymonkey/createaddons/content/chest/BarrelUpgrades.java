package com.comedymonkey.createaddons.content.chest;

import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.registry.ModBlocks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Turning barrels into upgraded barrels and back. Same rules as chests, without doubles. All server side. */
public final class BarrelUpgrades {
	private BarrelUpgrades() {}

	/** Vanilla barrel -> upgraded barrel of the given tier, keeping its items and facing. */
	static void upgradeVanillaBarrel(Level level, BlockPos pos, BlockState state, ChestTier tier, Player player,
									 ItemStack upgrade) {
		if (!(level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel))
			return;
		barrel.unpackLootTable(player);
		NonNullList<ItemStack> contents = NonNullList.withSize(ChestStacks.SLOTS, ItemStack.EMPTY);
		for (int i = 0; i < Math.min(barrel.getContainerSize(), ChestStacks.SLOTS); i++)
			contents.set(i, barrel.getItem(i).copy());
		// Empty the old barrel first so it drops nothing when replaced
		barrel.clearContent();

		level.setBlock(pos, ModBlocks.UPGRADED_BARREL.get().defaultBlockState()
			.setValue(UpgradedBarrelBlock.FACING, state.getValue(BarrelBlock.FACING))
			.setValue(UpgradedBarrelBlock.TIER, tier), 3);
		if (level.getBlockEntity(pos) instanceof UpgradedBarrelBlockEntity upgraded)
			upgraded.fill(contents);

		upgrade.consume(1, player);
		feedback(level, pos, player, tier);
	}

	/** Upgraded barrel -> higher tier. The previous upgrade goes back to the player. */
	static void upgradeTier(Level level, BlockPos pos, BlockState state, ChestTier tier, Player player,
							ItemStack upgrade) {
		ChestTier current = state.getValue(UpgradedBarrelBlock.TIER);
		if (!tier.isAbove(current)) {
			player.displayClientMessage(Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".chest.already_tier",
				current.barrelName()).withStyle(ChatFormatting.GOLD), true);
			return;
		}
		setTier(level, pos, tier);
		upgrade.consume(1, player);
		player.getInventory().placeItemBackInInventory(new ItemStack(current.upgradeItem()));
		feedback(level, pos, player, tier);
	}

	static void setTier(Level level, BlockPos pos, ChestTier tier) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof UpgradedBarrelBlock && state.getValue(UpgradedBarrelBlock.TIER) != tier)
			level.setBlock(pos, state.setValue(UpgradedBarrelBlock.TIER, tier), 3);
	}

	/** Upgraded barrel -> vanilla barrel. Callers make sure every stack fits a normal barrel first. */
	static void revertToVanilla(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof UpgradedBarrelBlock) || !(level.getBlockEntity(pos) instanceof UpgradedBarrelBlockEntity upgraded))
			return;
		NonNullList<ItemStack> contents = ChestStacks.resize(ChestStacks.copy(upgraded.getItems()), ChestStacks.SLOTS);
		upgraded.clearContent();

		level.setBlock(pos, Blocks.BARREL.defaultBlockState()
			.setValue(BarrelBlock.FACING, state.getValue(UpgradedBarrelBlock.FACING)), 3);
		if (level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel)
			for (int i = 0; i < contents.size(); i++)
				barrel.setItem(i, contents.get(i));
		level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 0.9f);
	}

	private static void feedback(Level level, BlockPos pos, Player player, ChestTier tier) {
		ModAdvancements.award(player, ModAdvancements.BARREL_UPGRADE, new ItemStack(tier.upgradeItem()), tier.ordinal());
		level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.6f, 1.2f);
		player.displayClientMessage(Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".chest.upgraded",
			tier.barrelName(), tier.multiplier()).withStyle(ChatFormatting.GOLD), true);
	}
}
