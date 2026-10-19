package com.comedymonkey.createaddons.content.chest;

import com.comedymonkey.createaddons.registry.ModMenus;

import org.anti_ad.mc.ipn.api.IPNPlayerSideOnly;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The 108-slot diamond double chest: 12 columns by 9 rows, with the player inventory centred underneath. */
/*
 * Inventory Profiles Next sorts by simulating clicks and assumes vanilla stack sizes, which oversized stacks break,
 * so it only manages the player's side here; the chest has its own Sort button (see ChestSorting).
 */
@IPNPlayerSideOnly
public class LargeUpgradedChestMenu extends AbstractContainerMenu implements UpgradedChestView {
	public static final int COLUMNS = 12;
	public static final int ROWS = 9;
	public static final int SLOTS = COLUMNS * ROWS;
	public static final int CHEST_X = 8;
	public static final int CHEST_Y = 18;
	public static final int INVENTORY_X = CHEST_X + (COLUMNS - 9) * 9;
	public static final int INVENTORY_Y = CHEST_Y + ROWS * 18 + 14;
	public static final int HOTBAR_Y = INVENTORY_Y + 58;

	private final Container container;

	public LargeUpgradedChestMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
		this(containerId, inventory, new UpgradedChestMenu.ClientChestContainer(SLOTS, extraData.readVarInt()));
	}

	public LargeUpgradedChestMenu(int containerId, Inventory inventory, Container container) {
		super(ModMenus.LARGE_UPGRADED_CHEST.get(), containerId);
		checkContainerSize(container, SLOTS);
		this.container = container;
		container.startOpen(inventory.player);

		for (int row = 0; row < ROWS; row++)
			for (int col = 0; col < COLUMNS; col++)
				addSlot(new UpgradedChestMenu.BigSlot(container, col + row * COLUMNS, CHEST_X + col * 18, CHEST_Y + row * 18));
		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 9; col++)
				addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
		for (int col = 0; col < 9; col++)
			addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, HOTBAR_Y));
	}

	@Override
	public Container getChestContainer() {
		return container;
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player player) {
		if (!UpgradedChestMenu.handleOversizedSwap(this, slotId, button, clickType, player, SLOTS))
			super.clicked(slotId, button, clickType, player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem())
			return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (index < SLOTS) {
			if (!moveItemStackTo(stack, SLOTS, slots.size(), true))
				return ItemStack.EMPTY;
		} else if (!moveItemStackTo(stack, 0, SLOTS, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty())
			slot.setByPlayer(ItemStack.EMPTY);
		else
			slot.setChanged();
		return original;
	}

	@Override
	public boolean stillValid(Player player) {
		return container.stillValid(player);
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		container.stopOpen(player);
	}
}
