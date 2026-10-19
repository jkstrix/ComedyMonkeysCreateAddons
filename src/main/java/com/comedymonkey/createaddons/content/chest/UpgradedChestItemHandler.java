package com.comedymonkey.createaddons.content.chest;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * Automation view of an upgraded chest or barrel (hoppers, Create funnels and arms, Pipez...). Unlike NeoForge's default
 * container wrapper it fills slots past 64, up to the chest's multiplied limit.
 */
public class UpgradedChestItemHandler implements IItemHandlerModifiable {
	private final UpgradedStorage chest;

	public UpgradedChestItemHandler(UpgradedStorage chest) {
		this.chest = chest;
	}

	@Override
	public int getSlots() {
		return chest.getContainerSize();
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return chest.getItem(slot);
	}

	@Override
	public void setStackInSlot(int slot, ItemStack stack) {
		chest.setItem(slot, stack);
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		ItemStack rest = ChestStacks.insert(chest.getItems(), slot, stack, simulate, chest.multiplier());
		if (!simulate && rest.getCount() != stack.getCount())
			chest.setChanged();
		return rest;
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		ItemStack taken = ChestStacks.extract(chest.getItems(), slot, amount, simulate);
		if (!simulate && !taken.isEmpty())
			chest.setChanged();
		return taken;
	}

	@Override
	public int getSlotLimit(int slot) {
		return chest.getMaxStackSize();
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return true;
	}
}
