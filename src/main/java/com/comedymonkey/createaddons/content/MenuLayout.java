package com.comedymonkey.createaddons.content;

import java.util.function.Consumer;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Where the shared GUI kit puts things (must match the textures): a machine panel with a header band at the top, and
 * the player's inventory as its own panel underneath, {@link #GAP} pixels below it.
 */
public final class MenuLayout {
	/** Height of the header band (with the title on it), including its bottom edge. */
	public static final int HEADER = 14;
	public static final int GAP = 4;
	public static final int INVENTORY_WIDTH = 176;
	public static final int INVENTORY_HEIGHT = 96;
	/** Inside the inventory panel: the label, the first row of slots and the hotbar. */
	public static final int INVENTORY_LABEL_Y = 4;
	public static final int INVENTORY_ROWS_Y = 15;
	private static final int HOTBAR_Y = 73;

	private MenuLayout() {}

	/** Top of the inventory panel under a machine panel of the given height. */
	public static int inventoryTop(int machineHeight) {
		return machineHeight + GAP;
	}

	/** Left edge of the inventory panel, centred under a machine panel of the given width. */
	public static int inventoryLeft(int machineWidth) {
		return (machineWidth - INVENTORY_WIDTH) / 2;
	}

	/** Full height of a screen: the machine panel, the gap and the inventory panel. */
	public static int screenHeight(int machineHeight) {
		return inventoryTop(machineHeight) + INVENTORY_HEIGHT;
	}

	/** Adds the player's 27 inventory slots and 9 hotbar slots, in vanilla's order, inside the inventory panel. */
	public static void addPlayerInventory(Consumer<Slot> addSlot, Inventory inventory, int panelLeft, int panelTop) {
		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 9; col++)
				addSlot.accept(new Slot(inventory, col + row * 9 + 9, panelLeft + 8 + col * 18, panelTop + INVENTORY_ROWS_Y + row * 18));
		for (int col = 0; col < 9; col++)
			addSlot.accept(new Slot(inventory, col, panelLeft + 8 + col * 18, panelTop + HOTBAR_Y));
	}
}
