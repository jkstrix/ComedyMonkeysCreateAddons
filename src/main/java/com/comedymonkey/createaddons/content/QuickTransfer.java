package com.comedymonkey.createaddons.content;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Quick moves between a block's slots and the player's inventory, done on the server: everything (or everything that
 * matches what's already on the other side) at once, or one item at a time with the mouse wheel. Moving everything is
 * the menu's own shift-click, so each menu's rules about what goes where still hold; single items only move once a
 * slot that will take them has been found, so nothing can be lost on the way.
 */
public final class QuickTransfer {
	public enum Action {
		/** Every stack in the player's inventory into the block. */
		ALL_IN,
		/** Stacks in the player's inventory that the block already holds some of. */
		MATCHING_IN,
		/** Every stack in the block into the player's inventory. */
		ALL_OUT,
		/** Stacks in the block that the player already holds some of. */
		MATCHING_OUT,
		/** One item from the given slot to the other side. */
		ONE_AWAY,
		/** One item like the given slot's, from the other side into it. */
		ONE_HERE
	}

	private QuickTransfer() {}

	public static void handle(Player player, AbstractContainerMenu menu, Action action, int slot) {
		if (!(menu instanceof QuickTransferMenu quick) || !menu.stillValid(player))
			return;
		int first = quick.firstPlayerSlot();
		int size = menu.slots.size();
		if (!quick.allowsQuickTransferIn() && putsIn(action, slot < first))
			return;
		switch (action) {
			case ALL_IN -> moveAll(player, menu, quick, first, size, 0, first, false);
			case MATCHING_IN -> moveAll(player, menu, quick, first, size, 0, first, true);
			case ALL_OUT -> moveAll(player, menu, quick, 0, first, first, size, false);
			case MATCHING_OUT -> moveAll(player, menu, quick, 0, first, first, size, true);
			case ONE_AWAY, ONE_HERE -> {
				if (slot < 0 || slot >= size)
					return;
				boolean blockSide = slot < first;
				int from = blockSide ? first : 0, to = blockSide ? size : first;
				if (action == Action.ONE_AWAY)
					moveOne(player, quick, menu.slots.get(slot), menu, from, to);
				else
					pullOne(player, quick, menu.slots.get(slot), menu, from, to);
			}
		}
		menu.broadcastChanges();
	}

	/** Whether the action would move the player's items into the block. */
	private static boolean putsIn(Action action, boolean blockSlot) {
		return switch (action) {
			case ALL_IN, MATCHING_IN -> true;
			case ALL_OUT, MATCHING_OUT -> false;
			// One away from the player's slot, or one pulled into the block's slot
			case ONE_AWAY -> !blockSlot;
			case ONE_HERE -> blockSlot;
		};
	}

	/** Shift-clicks every stack in [from, to), or only those the other side [otherFrom, otherTo) already holds. */
	private static void moveAll(Player player, AbstractContainerMenu menu, QuickTransferMenu quick, int from, int to,
								int otherFrom, int otherTo, boolean matchingOnly) {
		for (int i = from; i < to; i++) {
			Slot slot = menu.slots.get(i);
			if (!slot.hasItem() || !quick.allowsQuickTransfer(slot))
				continue;
			if (matchingOnly && !holds(menu, quick, otherFrom, otherTo, slot.getItem()))
				continue;
			menu.clicked(i, 0, ClickType.QUICK_MOVE, player);
		}
	}

	private static boolean holds(AbstractContainerMenu menu, QuickTransferMenu quick, int from, int to, ItemStack like) {
		for (int i = from; i < to; i++) {
			Slot slot = menu.slots.get(i);
			if (quick.allowsQuickTransfer(slot) && ItemStack.isSameItemSameComponents(slot.getItem(), like))
				return true;
		}
		return false;
	}

	/** One item from {@code source} into the first slot in [from, to) that takes it (topping up a stack first). */
	private static void moveOne(Player player, QuickTransferMenu quick, Slot source, AbstractContainerMenu menu, int from,
								int to) {
		if (!source.hasItem() || !quick.allowsQuickTransfer(source) || !source.mayPickup(player))
			return;
		ItemStack one = source.getItem().copyWithCount(1);
		Slot target = findRoom(menu, quick, from, to, one);
		if (target == null)
			return;
		ItemStack taken = source.safeTake(1, 1, player);
		if (!taken.isEmpty())
			keep(player, target.safeInsert(taken, 1));
	}

	/** One item like {@code target}'s, taken from a slot in [from, to), onto {@code target}. */
	private static void pullOne(Player player, QuickTransferMenu quick, Slot target, AbstractContainerMenu menu, int from,
								int to) {
		ItemStack like = target.getItem();
		if (like.isEmpty() || !quick.allowsQuickTransfer(target) || !target.mayPlace(like)
			|| like.getCount() >= target.getMaxStackSize(like))
			return;
		for (int i = from; i < to; i++) {
			Slot source = menu.slots.get(i);
			if (source != target && quick.allowsQuickTransfer(source) && source.mayPickup(player)
				&& ItemStack.isSameItemSameComponents(source.getItem(), like)) {
				ItemStack taken = source.safeTake(1, 1, player);
				if (!taken.isEmpty())
					keep(player, target.safeInsert(taken, 1));
				return;
			}
		}
	}

	/** A slot in [from, to) with room for {@code one}: a matching stack that isn't full, else an empty slot. */
	@Nullable
	private static Slot findRoom(AbstractContainerMenu menu, QuickTransferMenu quick, int from, int to, ItemStack one) {
		Slot empty = null;
		for (int i = from; i < to; i++) {
			Slot slot = menu.slots.get(i);
			if (!quick.allowsQuickTransfer(slot) || !slot.mayPlace(one))
				continue;
			ItemStack in = slot.getItem();
			if (in.isEmpty()) {
				if (empty == null)
					empty = slot;
			} else if (ItemStack.isSameItemSameComponents(in, one) && in.getCount() < slot.getMaxStackSize(one)) {
				return slot;
			}
		}
		return empty;
	}

	/** Anything a slot turned down after all goes to the player rather than vanishing. */
	private static void keep(Player player, ItemStack left) {
		if (!left.isEmpty())
			player.getInventory().placeItemBackInInventory(left);
	}
}
