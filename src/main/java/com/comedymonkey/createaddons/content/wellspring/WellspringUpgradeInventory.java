package com.comedymonkey.createaddons.content.wellspring;

import com.comedymonkey.createaddons.config.CMConfig;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The Wellspring's upgrade slots: {@link #GENERATION_SLOTS} for Water/Lava Upgrades followed by
 * {@link #CAPACITY_SLOTS} for Capacity Upgrades. One item per slot. Slot rules live here so the GUI,
 * right-click installing and any future automation all agree.
 */
public class WellspringUpgradeInventory extends ItemStackHandler {
	public static final int GENERATION_SLOTS = 3;
	public static final int CAPACITY_SLOTS = 3;
	public static final int FIRST_CAPACITY_SLOT = GENERATION_SLOTS;

	private final WellspringBlockEntity be;

	WellspringUpgradeInventory(WellspringBlockEntity be) {
		super(GENERATION_SLOTS + CAPACITY_SLOTS);
		this.be = be;
	}

	public static boolean isCapacitySlot(int slot) {
		return slot >= FIRST_CAPACITY_SLOT;
	}

	/** Whether the slot is within the configured limit (slots beyond it can still be emptied). */
	public static boolean isUnlocked(int slot) {
		if (isCapacitySlot(slot))
			return slot - FIRST_CAPACITY_SLOT < CMConfig.SERVER.maxCapacityUpgrades();
		return slot < CMConfig.SERVER.maxUpgrades();
	}

	@Override
	public int getSlotLimit(int slot) {
		return 1;
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		if (!(stack.getItem() instanceof WellspringUpgradeItem item) || !isUnlocked(slot))
			return false;
		WellspringUpgrade upgrade = item.getUpgrade();

		if (isCapacitySlot(slot))
			return upgrade == WellspringUpgrade.CAPACITY;
		if (!upgrade.isGeneration())
			return false;

		// One kind of generation upgrade per wellspring, matching any fluid already inside
		for (int i = 0; i < GENERATION_SLOTS; i++) {
			if (i == slot)
				continue;
			ItemStack other = getStackInSlot(i);
			if (other.getItem() instanceof WellspringUpgradeItem otherItem && otherItem.getUpgrade() != upgrade)
				return false;
		}
		var fluid = be.getTank().getFluid();
		return fluid.isEmpty() || upgrade.source().matches(fluid);
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		// A Capacity Upgrade can only come out if the fluid still fits without it
		if (isCapacitySlot(slot) && !getStackInSlot(slot).isEmpty() && !be.canRemoveCapacityUpgrade())
			return ItemStack.EMPTY;
		return super.extractItem(slot, amount, simulate);
	}

	@Override
	protected void onContentsChanged(int slot) {
		be.onUpgradesChanged();
	}

	/** Empties every slot without firing change callbacks (see {@link WellspringBlockEntity#clearContent()}). */
	void clearQuietly() {
		stacks.clear();
	}

	/** Puts one of the stack into the first free slot that accepts it. Returns true if it went in. */
	boolean insertOne(ItemStack stack) {
		for (int slot = 0; slot < getSlots(); slot++) {
			if (!getStackInSlot(slot).isEmpty() || !isItemValid(slot, stack))
				continue;
			setStackInSlot(slot, stack.copyWithCount(1));
			return true;
		}
		return false;
	}
}
