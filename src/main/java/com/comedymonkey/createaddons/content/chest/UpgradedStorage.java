package com.comedymonkey.createaddons.content.chest;

import java.util.List;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** An upgraded chest or barrel: a container whose stack size and slot count come from its tier. */
public interface UpgradedStorage extends Container {
	ChestTier getTier();

	default int multiplier() {
		return getTier().multiplier();
	}

	NonNullList<ItemStack> getItems();

	/** Replaces the contents (moved in from a vanilla container or back from a contraption). */
	void fill(List<ItemStack> contents);
}
