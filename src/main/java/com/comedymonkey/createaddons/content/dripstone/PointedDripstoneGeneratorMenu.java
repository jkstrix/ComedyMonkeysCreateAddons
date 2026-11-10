package com.comedymonkey.createaddons.content.dripstone;

import org.anti_ad.mc.ipn.api.IPNIgnore;

import com.comedymonkey.createaddons.content.MenuLayout;
import com.comedymonkey.createaddons.content.QuickTransferMenu;
import com.comedymonkey.createaddons.content.ScreenBlockEntities;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.comedymonkey.createaddons.registry.ModMenus;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Wrench screen of a Pointed Dripstone Generator, laid out as the Fish Tank's on the Animal Pens' artwork: the seed (four
 * Pointed Dripstone; take them out here), the growth and water gauges, the store (take only), and the player's inventory.
 */
/* Inventory Profiles Next's sorting would shuffle these machine slots. */
@IPNIgnore
public final class PointedDripstoneGeneratorMenu extends AbstractContainerMenu implements QuickTransferMenu {
	public static final int SEED_X = 8, SEED_Y = 18;
	public static final int OUTPUT_X = 80, OUTPUT_Y = 36;
	public static final int WIDTH = 176, HEIGHT = 124;
	private static final int SEED_SLOT = 0, OUTPUT_SLOT = 1;

	private final PointedDripstoneGeneratorBlockEntity be;
	private final ContainerLevelAccess access;
	private final int firstPlayer;

	public PointedDripstoneGeneratorMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
		this(containerId, inventory, find(inventory, extraData));
	}

	private static PointedDripstoneGeneratorBlockEntity find(Inventory inventory, RegistryFriendlyByteBuf extraData) {
		var pos = extraData.readBlockPos();
		Block here = inventory.player.level().getBlockState(pos).getBlock();
		Block block = here instanceof PointedDripstoneGeneratorBlock ? here : com.comedymonkey.createaddons.registry.ModBlocks.POINTED_DRIPSTONE_GENERATOR.get();
		return ScreenBlockEntities.find(inventory, pos, ModBlockEntities.POINTED_DRIPSTONE_GENERATOR.get(), block);
	}

	public PointedDripstoneGeneratorMenu(int containerId, Inventory inventory, PointedDripstoneGeneratorBlockEntity be) {
		super(ModMenus.POINTED_DRIPSTONE_GENERATOR.get(), containerId);
		this.be = be;
		this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
		addSlot(new SlotItemHandler(be.getSeed(), 0, SEED_X, SEED_Y) {
			/**
			 * A shift-click merging into a part-filled seed grows its stack in place, which the handler never hears of:
			 * set it again, so the block saves it and its clients see it.
			 */
			@Override
			public void setChanged() {
				super.setChanged();
				be.getSeed().setStackInSlot(0, getItem());
			}
		});
		addSlot(new SlotItemHandler(be.getOutputHandler(), 0, OUTPUT_X, OUTPUT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return false;
			}
		});
		firstPlayer = slots.size();
		MenuLayout.addPlayerInventory(this::addSlot, inventory, MenuLayout.inventoryLeft(WIDTH), MenuLayout.inventoryTop(HEIGHT));
	}

	public PointedDripstoneGeneratorBlockEntity getBlockEntity() {
		return be;
	}

	@Override
	public int firstPlayerSlot() {
		return firstPlayer;
	}

	/** Quick moves take what it has made into the player's inventory: never the seed. */
	@Override
	public boolean allowsQuickTransfer(Slot slot) {
		return slot.index == OUTPUT_SLOT || slot.index >= firstPlayer;
	}

	@Override
	public boolean allowsQuickTransferIn() {
		return false;
	}

	/** Shift-click: the seed and the store out to the inventory; Pointed Dripstone from the inventory into the seed. */
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
		} else if (!slots.get(SEED_SLOT).mayPlace(stack) || !moveItemStackTo(stack, SEED_SLOT, SEED_SLOT + 1, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty())
			slot.setByPlayer(ItemStack.EMPTY);
		else
			slot.set(stack);
		return original;
	}

	@Override
	public boolean stillValid(Player player) {
		return access.evaluate((level, pos) -> level.getBlockEntity(pos) == be
			&& player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64, true);
	}
}
