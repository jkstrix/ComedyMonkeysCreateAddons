package com.comedymonkey.createaddons.content.chest;

import com.comedymonkey.createaddons.content.MenuLayout;
import com.comedymonkey.createaddons.content.QuickTransferMenu;
import com.comedymonkey.createaddons.registry.ModMenus;

import org.anti_ad.mc.ipn.api.IPNIgnore;

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
 * Inventory Profiles Next sorts by simulating clicks and assumes vanilla stack sizes, which oversized stacks break;
 * the chest has its own Sort button (see ChestSorting). IPN still put its sort buttons on the chest side with
 * @IPNPlayerSideOnly, so, like the single chest, it's told to ignore this menu outright.
 */
@IPNIgnore
public class LargeUpgradedChestMenu extends AbstractContainerMenu implements UpgradedChestView, QuickTransferMenu {
	public static final int COLUMNS = 12;
	public static final int ROWS = 9;
	public static final int SLOTS = COLUMNS * ROWS;
	public static final int CHEST_X = 8;
	public static final int CHEST_Y = 18;
	/** The chest panel (the player's inventory panel sits under it, centred). */
	public static final int WIDTH = CHEST_X * 2 + COLUMNS * 18 - 2;
	public static final int HEIGHT = CHEST_Y + ROWS * 18 + 4;

	private final Container container;
	/** Client side only (for the screen's colour): the chest's tier. */
	private final ChestTier tier;

	public LargeUpgradedChestMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
		this(containerId, inventory, new UpgradedChestMenu.ClientChestContainer(SLOTS, extraData.readVarInt()),
			ChestTier.values()[extraData.readVarInt()]);
	}

	public LargeUpgradedChestMenu(int containerId, Inventory inventory, Container container, ChestTier tier) {
		super(ModMenus.LARGE_UPGRADED_CHEST.get(), containerId);
		checkContainerSize(container, SLOTS);
		this.container = container;
		this.tier = tier;
		container.startOpen(inventory.player);

		for (int row = 0; row < ROWS; row++)
			for (int col = 0; col < COLUMNS; col++)
				addSlot(new UpgradedChestMenu.BigSlot(container, col + row * COLUMNS, CHEST_X + col * 18, CHEST_Y + row * 18));
		MenuLayout.addPlayerInventory(this::addSlot, inventory, MenuLayout.inventoryLeft(WIDTH), MenuLayout.inventoryTop(HEIGHT));
	}

	public ChestTier getTier() {
		return tier;
	}

	@Override
	public int firstPlayerSlot() {
		return SLOTS;
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
