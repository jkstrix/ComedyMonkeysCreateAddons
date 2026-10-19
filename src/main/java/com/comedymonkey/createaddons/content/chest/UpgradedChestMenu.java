package com.comedymonkey.createaddons.content.chest;

import com.comedymonkey.createaddons.registry.ModMenus;

import org.anti_ad.mc.ipn.api.IPNIgnore;

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
public class UpgradedChestMenu extends ChestMenu implements UpgradedChestView {
	private final int chestSlots;

	/** Client side: multiplier and row count come from the open-menu packet, so slot limits match the server. */
	public UpgradedChestMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
		this(containerId, inventory, extraData.readVarInt(), extraData.readVarInt());
	}

	private UpgradedChestMenu(int containerId, Inventory inventory, int multiplier, int rows) {
		this(containerId, inventory, new ClientChestContainer(rows * 9, multiplier), rows);
	}

	/** {@code rows} is 3 or 6. */
	public UpgradedChestMenu(int containerId, Inventory inventory, Container container, int rows) {
		super(ModMenus.UPGRADED_CHEST.get(), containerId, inventory, container, rows);
		chestSlots = rows * 9;
		for (int i = 0; i < chestSlots; i++) {
			Slot vanilla = slots.get(i);
			Slot big = new BigSlot(container, vanilla.getContainerSlot(), vanilla.x, vanilla.y);
			big.index = i;
			slots.set(i, big);
		}
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
