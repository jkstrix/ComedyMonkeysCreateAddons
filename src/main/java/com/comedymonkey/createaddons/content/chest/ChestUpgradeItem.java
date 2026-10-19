package com.comedymonkey.createaddons.content.chest;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Copper, Iron, Gold or Diamond Chest Upgrade. Handled in {@link #onItemUseFirst} so it runs before the chest's own
 * right-click (which would just open it):
 * <ul>
 *     <li>on a vanilla chest or barrel: turns it into an upgraded chest or barrel of this tier</li>
 *     <li>on an upgraded chest of a lower tier: upgrades it and hands back the old upgrade</li>
 *     <li>sneaking on an upgraded chest: opens its upgrade GUI</li>
 * </ul>
 */
public class ChestUpgradeItem extends Item {
	private final ChestTier tier;

	public ChestUpgradeItem(ChestTier tier, Properties properties) {
		super(properties);
		this.tier = tier;
	}

	public ChestTier getTier() {
		return tier;
	}

	@Override
	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);
		Player player = context.getPlayer();
		boolean vanillaChest = state.is(Blocks.CHEST);
		boolean vanillaBarrel = state.is(Blocks.BARREL);
		boolean upgraded = state.getBlock() instanceof TieredStorageBlock;
		if (player == null || (!vanillaChest && !vanillaBarrel && !upgraded))
			return InteractionResult.PASS;
		if (level.isClientSide)
			return InteractionResult.SUCCESS;

		if (upgraded && player.isShiftKeyDown()) {
			if (player instanceof ServerPlayer serverPlayer)
				ChestUpgrades.openUpgradeGui(serverPlayer, pos);
			return InteractionResult.SUCCESS;
		}

		if (vanillaChest)
			ChestUpgrades.upgradeVanillaChest(level, pos, state, tier, player, stack);
		else if (vanillaBarrel)
			BarrelUpgrades.upgradeVanillaBarrel(level, pos, state, tier, player, stack);
		else if (state.getBlock() instanceof UpgradedBarrelBlock)
			BarrelUpgrades.upgradeTier(level, pos, state, tier, player, stack);
		else
			ChestUpgrades.upgradeTier(level, pos, state, tier, player, stack);
		return InteractionResult.SUCCESS;
	}
}
