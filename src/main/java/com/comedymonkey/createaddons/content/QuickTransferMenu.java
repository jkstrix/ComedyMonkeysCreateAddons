package com.comedymonkey.createaddons.content;

import net.minecraft.world.inventory.Slot;

/**
 * A menu whose screen gets the quick-move buttons and mouse-wheel moves (see {@link QuickTransfer}): its own slots
 * come first, the player's inventory after them.
 */
public interface QuickTransferMenu {
	/** Index of the first of the player's slots; everything before it belongs to the block. */
	int firstPlayerSlot();

	/** Whether quick moves may take from or put into this slot (some slots do more than hold items). */
	default boolean allowsQuickTransfer(Slot slot) {
		return true;
	}

	/**
	 * Whether quick moves may put the player's items into the block. Machines whose quick moves are only for taking
	 * their output say no: their screens show just the two take buttons, and the wheel only takes out.
	 */
	default boolean allowsQuickTransferIn() {
		return true;
	}
}
