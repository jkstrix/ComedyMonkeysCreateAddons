package com.comedymonkey.createaddons.content.wastes;

import com.comedymonkey.createaddons.content.chest.ChestTier;
import com.comedymonkey.createaddons.content.chest.UpgradedChestMenu;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;

/**
 * A City Vault opened (D46): the Upgraded Chest's slots, quick moves and inventory panel, under a menu type of its own so
 * its screen ({@code CityVaultScreen}) draws the void-alloy band the user chose (D59, option A) instead of a chest tier's.
 */
public class CityVaultMenu extends UpgradedChestMenu {
	/** Client side: the row count comes from the open-menu packet. */
	public CityVaultMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
		this(containerId, inventory, new SimpleContainer(extraData.readVarInt() * 9));
	}

	public CityVaultMenu(int containerId, Inventory inventory, Container container) {
		// its tier only picks a chest's band colour, which the vault's screen doesn't use; its stacks are ordinary (x1)
		super(ModWastes.CITY_VAULT_MENU.get(), containerId, inventory, container, container.getContainerSize() / 9, ChestTier.COPPER);
	}
}
