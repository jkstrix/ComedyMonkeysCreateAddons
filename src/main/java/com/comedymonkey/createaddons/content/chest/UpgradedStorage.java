package com.comedymonkey.createaddons.content.chest;

import java.util.List;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** An upgraded chest or barrel: a container whose stack size and slot count come from its tier. */
public interface UpgradedStorage extends Container {
	ChestTier getTier();

	default int multiplier() {
		return getTier().multiplier();
	}

	NonNullList<ItemStack> getItems();

	/** Replaces the contents (moved in from a vanilla container or back from a contraption). */
	void fill(List<ItemStack> contents);

	/** Whether the player has this storage open, on its own or as half of a double chest. */
	boolean isOpenIn(Player player);

	/**
	 * Closes the chest screens showing this storage. Called before a tier change that removes slots: a screen opened
	 * earlier would keep the extra slots, and the next time the server read them it would crash.
	 */
	static void closeScreens(Level level, UpgradedStorage storage) {
		for (Player player : List.copyOf(level.players()))
			if (storage.isOpenIn(player))
				player.closeContainer();
	}
}
