package com.comedymonkey.createaddons.content;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.StackRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.StrictNbtStackRequirement;

import net.minecraft.world.item.ItemStack;

/**
 * What a Schematicannon spends on printing a block, with equal items as one charge. The cannon checks each charge it's
 * given against its stock on its own before it takes any, so two charges of the same item would both pass on one stack
 * (and it would print the block having taken only the first).
 */
public final class PrintCost {
	private final List<StackRequirement> charges = new ArrayList<>();

	/** Any item like this one (its components aside), {@code stack.getCount()} of them. */
	public PrintCost add(ItemStack stack) {
		return add(stack, false);
	}

	/** Items exactly like this one, components and all. */
	public PrintCost addExact(ItemStack stack) {
		return add(stack, true);
	}

	private PrintCost add(ItemStack stack, boolean exact) {
		if (stack.isEmpty())
			return this;
		for (int i = 0; i < charges.size(); i++) {
			StackRequirement r = charges.get(i);
			if ((r instanceof StrictNbtStackRequirement) == exact && r.getClass() == (exact ? StrictNbtStackRequirement.class : StackRequirement.class)
				&& ItemStack.isSameItemSameComponents(r.stack, stack)) {
				charges.set(i, charge(r.stack.copyWithCount(r.stack.getCount() + stack.getCount()), exact));
				return this;
			}
		}
		charges.add(charge(stack.copy(), exact));
		return this;
	}

	private static StackRequirement charge(ItemStack stack, boolean exact) {
		return exact ? new StrictNbtStackRequirement(stack, ItemRequirement.ItemUseType.CONSUME)
			: new StackRequirement(stack, ItemRequirement.ItemUseType.CONSUME);
	}

	public ItemRequirement build() {
		return charges.isEmpty() ? ItemRequirement.NONE : new ItemRequirement(new ArrayList<>(charges));
	}
}
