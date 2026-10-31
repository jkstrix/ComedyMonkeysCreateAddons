package com.comedymonkey.createaddons.content.chest;

import com.comedymonkey.createaddons.content.MenuLayout;
import com.comedymonkey.createaddons.content.QuickTransferMenu;
import com.comedymonkey.createaddons.registry.ModMenus;

import org.anti_ad.mc.ipn.api.IPNIgnore;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Standard 3 or 6-row chest screen, but with slots that hold oversized stacks. Taking from a slot never puts more
 * than a normal stack on the cursor or in the hotbar, so oversized stacks can't leak into the player's inventory.
 * The 108-slot diamond double chest uses {@link LargeUpgradedChestMenu} instead.
 */
/*
 * Inventory Profiles Next sorts by simulating clicks and assumes vanilla stack sizes, which oversized stacks break;
 * the chest has its own Sort button (see ChestSorting). IPN files any ChestMenu subclass as a vanilla chest and then
 * skips @IPNPlayerSideOnly, so it's told to ignore this menu outright, which drops its chest-side buttons.
 */
@IPNIgnore
public class UpgradedChestMenu extends ChestMenu implements UpgradedChestView, QuickTransferMenu {
	public static final int WIDTH = 176;
	private final int chestSlots;
	/** Client side only (for the screen's colour): the chest's tier. */
	private final ChestTier tier;

	/** Client side: multiplier, tier and row count come from the open-menu packet, so slot limits match the server. */
	public UpgradedChestMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
		this(containerId, inventory, extraData.readVarInt(), ChestTier.values()[extraData.readVarInt()], extraData.readVarInt());
	}

	private UpgradedChestMenu(int containerId, Inventory inventory, int multiplier, ChestTier tier, int rows) {
		this(containerId, inventory, new ClientChestContainer(rows * 9, multiplier), rows, tier);
	}

	/** {@code rows} is 3 or 6. */
	public UpgradedChestMenu(int containerId, Inventory inventory, Container container, int rows, ChestTier tier) {
		this(ModMenus.UPGRADED_CHEST.get(), containerId, inventory, container, rows, tier);
	}

	/** The same slots under a menu type of its own (the City Vault's, with its own screen). */
	protected UpgradedChestMenu(net.minecraft.world.inventory.MenuType<?> type, int containerId, Inventory inventory, Container container, int rows,
								ChestTier tier) {
		super(type, containerId, inventory, container, rows);
		this.tier = tier;
		chestSlots = rows * 9;
		for (int i = 0; i < chestSlots; i++) {
			Slot vanilla = slots.get(i);
			Slot big = new BigSlot(container, vanilla.getContainerSlot(), vanilla.x, vanilla.y);
			big.index = i;
			slots.set(i, big);
		}
		// The player's slots, moved into the inventory panel under the chest
		List<Slot> player = new ArrayList<>();
		MenuLayout.addPlayerInventory(player::add, inventory, MenuLayout.inventoryLeft(WIDTH),
			MenuLayout.inventoryTop(height(rows)));
		for (int i = 0; i < player.size(); i++) {
			Slot slot = player.get(i);
			slot.index = chestSlots + i;
			slots.set(chestSlots + i, slot);
		}
	}

	/** Height of the chest panel for 3 or 6 rows. */
	public static int height(int rows) {
		return 22 + rows * 18;
	}

	public ChestTier getTier() {
		return tier;
	}

	@Override
	public int firstPlayerSlot() {
		return chestSlots;
	}

	@Override
	public Container getChestContainer() {
		return getContainer();
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player player) {
		if (!handleOversizedSwap(this, slotId, button, clickType, player, chestSlots))
			super.clicked(slotId, button, clickType, player);
	}

	/**
	 * Number-key swaps would otherwise move a whole oversized stack into the hotbar. Moves at most a normal stack,
	 * and only into an empty hotbar slot. Returns true if it handled the click.
	 */
	static boolean handleOversizedSwap(AbstractContainerMenu menu, int slotId, int button, ClickType clickType,
									   Player player, int chestSlots) {
		if (clickType != ClickType.SWAP || slotId < 0 || slotId >= chestSlots)
			return false;
		// Only the hotbar and offhand, as vanilla allows (anything else is a modified client)
		if (!Inventory.isHotbarSlot(button) && button != Inventory.SLOT_OFFHAND)
			return true;
		Slot slot = menu.slots.get(slotId);
		ItemStack inSlot = slot.getItem();
		if (inSlot.getCount() <= inSlot.getMaxStackSize())
			return false;
		Inventory inventory = player.getInventory();
		if (inventory.getItem(button).isEmpty() && slot.mayPickup(player)) {
			int amount = inSlot.getMaxStackSize();
			inventory.setItem(button, slot.safeTake(amount, amount, player));
		}
		return true;
	}

	/** A chest slot whose limit comes from the container's per-item limit rather than the item's own. */
	public static class BigSlot extends Slot {
		public BigSlot(Container container, int slot, int x, int y) {
			super(container, slot, x, y);
		}

		@Override
		public int getMaxStackSize() {
			return container.getMaxStackSize();
		}

		@Override
		public int getMaxStackSize(ItemStack stack) {
			return container.getMaxStackSize(stack);
		}

		@Override
		public ItemStack remove(int amount) {
			return super.remove(Math.min(amount, getItem().getMaxStackSize()));
		}

		/**
		 * A different item can't go onto an oversized stack: vanilla would swap them, putting the whole oversized stack
		 * on the cursor (and from there into a dropped item that can't be saved).
		 */
		@Override
		public boolean mayPlace(ItemStack stack) {
			ItemStack inSlot = getItem();
			return super.mayPlace(stack)
				&& (inSlot.getCount() <= inSlot.getMaxStackSize() || ItemStack.isSameItemSameComponents(inSlot, stack));
		}

		/**
		 * A slot already at or over its limit (the config can lower it) takes nothing more. Vanilla would work out a
		 * negative amount to add and move the excess onto the cursor instead.
		 */
		@Override
		public ItemStack safeInsert(ItemStack stack, int increment) {
			if (!stack.isEmpty() && getItem().getCount() >= getMaxStackSize(stack))
				return stack;
			return super.safeInsert(stack, increment);
		}
	}

	/** Client copy of the chest: same limits as the real one so click prediction agrees with the server. */
	static class ClientChestContainer extends SimpleContainer {
		private final int multiplier;

		ClientChestContainer(int size, int multiplier) {
			super(size);
			this.multiplier = multiplier;
		}

		@Override
		public int getMaxStackSize() {
			return 64 * multiplier;
		}

		@Override
		public int getMaxStackSize(ItemStack stack) {
			return ChestStacks.limit(stack, multiplier);
		}
	}
}
