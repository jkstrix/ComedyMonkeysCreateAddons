package com.comedymonkey.createaddons.content.harvester;

import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModMenus;

import org.anti_ad.mc.ipn.api.IPNIgnore;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Wrench screen: the planter slot, the nine harvest slots (take only) and the player's inventory. */
/* Inventory Profiles Next's sorting would shuffle these machine slots (and can't handle the pen's big food stack). */
@IPNIgnore
public class AutoHarvesterMenu extends AbstractContainerMenu {
	public static final int PLANTER_X = 26, PLANTER_Y = 36;
	public static final int OUTPUT_X = 80, OUTPUT_Y = 18;
	public static final int INVENTORY_Y = 96, HOTBAR_Y = 154;
	private static final int PLANTER_SLOT = 0, FIRST_OUTPUT = 1, FIRST_PLAYER = 10;

	private final AutoHarvesterBlockEntity be;
	private final ContainerLevelAccess access;

	/** Client side: the block position comes from the open-menu packet. */
	public AutoHarvesterMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
		this(containerId, inventory, (AutoHarvesterBlockEntity) inventory.player.level().getBlockEntity(extraData.readBlockPos()));
	}

	public AutoHarvesterMenu(int containerId, Inventory inventory, AutoHarvesterBlockEntity be) {
		super(ModMenus.AUTO_HARVESTER.get(), containerId);
		this.be = be;
		this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

		addSlot(new SlotItemHandler(be.getPlanter(), 0, PLANTER_X, PLANTER_Y));
		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 3; col++)
				addSlot(new SlotItemHandler(be.getOutput(), col + row * 3, OUTPUT_X + col * 18, OUTPUT_Y + row * 18) {
					@Override
					public boolean mayPlace(ItemStack stack) {
						return false;
					}
				});

		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 9; col++)
				addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
		for (int col = 0; col < 9; col++)
			addSlot(new Slot(inventory, col, 8 + col * 18, HOTBAR_Y));
	}

	public AutoHarvesterBlockEntity getBlockEntity() {
		return be;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem())
			return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (index < FIRST_PLAYER) {
			// The plant or the harvest out to the player
			if (!moveItemStackTo(stack, FIRST_PLAYER, slots.size(), true))
				return ItemStack.EMPTY;
		} else if (!slots.get(PLANTER_SLOT).mayPlace(stack) || slots.get(PLANTER_SLOT).hasItem()
			|| !moveItemStackTo(stack, PLANTER_SLOT, FIRST_OUTPUT, false)) {
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
		return stillValid(access, player, ModBlocks.AUTO_HARVESTER.get());
	}
}
