package com.comedymonkey.createaddons.content.wellspring;

import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Upgrade GUI: three generation slots, three capacity slots and the player's inventory. */
public class WellspringMenu extends AbstractContainerMenu {
	public static final int SLOT_X = 8;
	public static final int GENERATION_Y = 28;
	public static final int CAPACITY_Y = 60;
	public static final int INVENTORY_Y = 94;
	public static final int HOTBAR_Y = 152;

	private static final int UPGRADE_SLOT_COUNT = WellspringUpgradeInventory.GENERATION_SLOTS
		+ WellspringUpgradeInventory.CAPACITY_SLOTS;

	private final WellspringBlockEntity be;
	private final ContainerLevelAccess access;

	/** Client side: the block position comes from the open-menu packet. */
	public WellspringMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
		this(containerId, inventory, (WellspringBlockEntity) inventory.player.level()
			.getBlockEntity(extraData.readBlockPos()));
	}

	public WellspringMenu(int containerId, Inventory inventory, WellspringBlockEntity be) {
		super(ModMenus.WELLSPRING.get(), containerId);
		this.be = be;
		this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

		WellspringUpgradeInventory upgrades = be.getUpgrades();
		for (int i = 0; i < WellspringUpgradeInventory.GENERATION_SLOTS; i++)
			addSlot(new UpgradeSlot(upgrades, i, SLOT_X + i * 18, GENERATION_Y));
		for (int i = 0; i < WellspringUpgradeInventory.CAPACITY_SLOTS; i++)
			addSlot(new UpgradeSlot(upgrades, WellspringUpgradeInventory.FIRST_CAPACITY_SLOT + i, SLOT_X + i * 18,
				CAPACITY_Y));

		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 9; col++)
				addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
		for (int col = 0; col < 9; col++)
			addSlot(new Slot(inventory, col, 8 + col * 18, HOTBAR_Y));
	}

	public WellspringBlockEntity getBlockEntity() {
		return be;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem())
			return ItemStack.EMPTY;

		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (index < UPGRADE_SLOT_COUNT) {
			if (!moveItemStackTo(stack, UPGRADE_SLOT_COUNT, slots.size(), true))
				return ItemStack.EMPTY;
		} else if (!moveItemStackTo(stack, 0, UPGRADE_SLOT_COUNT, false)) {
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
		return stillValid(access, player, ModBlocks.WELLSPRING.get());
	}

	/** Hidden beyond the configured slot limit, unless it still holds an upgrade that needs taking out. */
	public static class UpgradeSlot extends SlotItemHandler {
		public UpgradeSlot(WellspringUpgradeInventory handler, int index, int x, int y) {
			super(handler, index, x, y);
		}

		public boolean isUnlocked() {
			return WellspringUpgradeInventory.isUnlocked(getSlotIndex());
		}

		@Override
		public boolean isActive() {
			return isUnlocked() || hasItem();
		}
	}
}
