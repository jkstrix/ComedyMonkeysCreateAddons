package com.comedymonkey.createaddons.content.chest;

import java.util.function.IntSupplier;

import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Both halves of an upgraded double chest as one 54-slot container. Vanilla's CompoundContainer falls back to the
 * item's own stack limit, so this restores the multiplied one.
 */
public class BigCompoundContainer extends CompoundContainer {
	private final IntSupplier multiplier;

	public BigCompoundContainer(Container first, Container second, IntSupplier multiplier) {
		super(first, second);
		this.multiplier = multiplier;
	}

	@Override
	public int getMaxStackSize() {
		return 64 * multiplier.getAsInt();
	}

	@Override
	public int getMaxStackSize(ItemStack stack) {
		return ChestStacks.limit(stack, multiplier.getAsInt());
	}
}
