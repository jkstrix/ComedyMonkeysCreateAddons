package com.comedymonkey.createaddons.content.pen;

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
 * Wrench screen of an Animal Pen: the captured animal's bottle (take it out to empty the pen), the trough's food,
 * what the pen has made (take only; three columns, six on the Brass pen) and the player's inventory.
 */
/* Inventory Profiles Next's sorting would shuffle these machine slots (and can't handle the pen's big food stack). */
@IPNIgnore
public class AnimalPenMenu extends AbstractContainerMenu {
	public static final int ANIMAL_X = 10, ANIMAL_Y = 20, FOOD_X = 10, FOOD_Y = 54;
	public static final int OUTPUT_X = 62, OUTPUT_Y = 18;
	public static final int INVENTORY_Y = 106, HOTBAR_Y = 164;
	private static final int ANIMAL_SLOT = 0, FOOD_SLOT = 1, FIRST_OUTPUT = 2;

	private final AnimalPenBlockEntity be;
	private final ContainerLevelAccess access;
	private final int firstPlayer;

	/** Client side: the block position comes from the open-menu packet. */
	public AnimalPenMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
		this(containerId, inventory, (AnimalPenBlockEntity) inventory.player.level().getBlockEntity(extraData.readBlockPos()));
	}

	public AnimalPenMenu(int containerId, Inventory inventory, AnimalPenBlockEntity be) {
		super(ModMenus.ANIMAL_PEN.get(), containerId);
		this.be = be;
		this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());

		addSlot(new SlotItemHandler(be.getCaptiveSlot(), 0, ANIMAL_X, ANIMAL_Y));
		addSlot(new SlotItemHandler(be.getFood(), 0, FOOD_X, FOOD_Y));
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

		for (int row = 0; row < 3; row++)
			for (int col = 0; col < 9; col++)
				addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
		for (int col = 0; col < 9; col++)
			addSlot(new Slot(inventory, col, 8 + col * 18, HOTBAR_Y));
	}

	/** Output columns: three, or six on the Brass pen. */
	public static int columns(AnimalPenBlockEntity be) {
		return be.getOutput().getSlots() > 9 ? 6 : 3;
	}

	public AnimalPenBlockEntity getBlockEntity() {
		return be;
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
			if (!moveItemStackTo(stack, ANIMAL_SLOT, FOOD_SLOT, false))
				return ItemStack.EMPTY;
		} else if (!slots.get(FOOD_SLOT).mayPlace(stack) || !moveItemStackTo(stack, FOOD_SLOT, FIRST_OUTPUT, false)) {
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
		return access.evaluate((level, pos) -> level.getBlockEntity(pos) == be
			&& player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64, true);
	}
}
