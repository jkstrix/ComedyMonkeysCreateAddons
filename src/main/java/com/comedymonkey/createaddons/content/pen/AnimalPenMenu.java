package com.comedymonkey.createaddons.content.pen;

import com.comedymonkey.createaddons.content.MenuLayout;
import com.comedymonkey.createaddons.content.QuickTransferMenu;
import com.comedymonkey.createaddons.content.ScreenBlockEntities;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
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

/**
 * Wrench screen of an Animal Pen: the captured animal's bottle (take it out to empty the pen), what the pen has made
 * (take only; three columns, six on the Brass pen), the trough's food in a row underneath, and the player's inventory.
 */
/* Inventory Profiles Next's sorting would shuffle these machine slots. */
@IPNIgnore
public class AnimalPenMenu extends AbstractContainerMenu implements QuickTransferMenu {
	public static final int ANIMAL_X = 8, ANIMAL_Y = 18;
	public static final int OUTPUT_X = 62, OUTPUT_Y = 18;
	public static final int TROUGH_X = 8, TROUGH_Y = 80;
	/** The machine panel (the player's inventory panel sits under it). */
	public static final int WIDTH = 176, HEIGHT = 124;
	private static final int ANIMAL_SLOT = 0, FIRST_FOOD = 1;

	private final AnimalPenBlockEntity be;
	private final ContainerLevelAccess access;
	private final int firstOutput;
	private final int firstPlayer;

	/** Client side: the block position comes from the open-menu packet. */
	public AnimalPenMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
		this(containerId, inventory, ScreenBlockEntities.find(inventory, extraData.readBlockPos(),
			ModBlockEntities.ANIMAL_PEN.get(), ModBlocks.ANIMAL_PEN.get()));
	}

	public AnimalPenMenu(int containerId, Inventory inventory, AnimalPenBlockEntity be) {
		super(ModMenus.ANIMAL_PEN.get(), containerId);
		this.be = be;
		this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

		addSlot(new SlotItemHandler(be.getCaptiveSlot(), 0, ANIMAL_X, ANIMAL_Y));
		var food = be.getFood();
		for (int i = 0; i < food.getSlots(); i++)
			addSlot(new SlotItemHandler(food, i, TROUGH_X + i * 18, TROUGH_Y));
		firstOutput = slots.size();
		int columns = columns(be);
		var output = be.getOutput();
		for (int i = 0; i < output.getSlots(); i++)
			addSlot(new SlotItemHandler(output, i, OUTPUT_X + (i % columns) * 18, OUTPUT_Y + (i / columns) * 18) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return false;
				}
			});
		firstPlayer = slots.size();
		MenuLayout.addPlayerInventory(this::addSlot, inventory, MenuLayout.inventoryLeft(WIDTH), MenuLayout.inventoryTop(HEIGHT));
	}

	/** Output columns: three, or six on the Brass pen. */
	public static int columns(AnimalPenBlockEntity be) {
		return be.getOutput().getSlots() > 9 ? 6 : 3;
	}

	public AnimalPenBlockEntity getBlockEntity() {
		return be;
	}

	@Override
	public int firstPlayerSlot() {
		return firstPlayer;
	}

	/** Quick moves only take what the pen has made: never the bottle (that releases the herd) or the trough's food. */
	@Override
	public boolean allowsQuickTransfer(Slot slot) {
		return slot.index >= firstOutput;
	}

	@Override
	public boolean allowsQuickTransferIn() {
		return false;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem())
			return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (index < firstPlayer) {
			if (!moveItemStackTo(stack, firstPlayer, slots.size(), true))
				return ItemStack.EMPTY;
		} else if (slots.get(ANIMAL_SLOT).mayPlace(stack) && !slots.get(ANIMAL_SLOT).hasItem()) {
			if (!moveItemStackTo(stack, ANIMAL_SLOT, FIRST_FOOD, false))
				return ItemStack.EMPTY;
		} else if (!slots.get(FIRST_FOOD).mayPlace(stack) || !moveItemStackTo(stack, FIRST_FOOD, firstOutput, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty())
			slot.setByPlayer(ItemStack.EMPTY);
		else if (index == ANIMAL_SLOT)
			slot.setChanged(); // setting the bottle slot would release and re-catch the herd
		else
			// Through the handler: a SlotItemHandler's setChanged() tells nobody, so a partly moved stack wasn't saved
			slot.set(stack);
		return original;
	}

	@Override
	public boolean stillValid(Player player) {
		return access.evaluate((level, pos) -> level.getBlockEntity(pos) == be
			&& player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64, true);
	}
}
