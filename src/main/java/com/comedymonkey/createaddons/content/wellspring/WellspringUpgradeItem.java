package com.comedymonkey.createaddons.content.wellspring;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;

/**
 * Water Upgrade (water), Lava Upgrade (lava) or Capacity Upgrade (bigger tank). Right-click a Fluid Wellspring
 * to install it, or sneak + right-click to open the upgrade GUI; the interaction lives in {@link WellspringBlock}.
 */
public class WellspringUpgradeItem extends Item {
	private final WellspringUpgrade upgrade;

	public WellspringUpgradeItem(WellspringUpgrade upgrade, Properties properties) {
		super(properties);
		this.upgrade = upgrade;
	}

	public WellspringUpgrade getUpgrade() {
		return upgrade;
	}

	/** Lets sneak + right-click on a Wellspring reach the block (to open its GUI) instead of being skipped. */
	@Override
	public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
		return level.getBlockState(pos).getBlock() instanceof WellspringBlock;
	}
}
