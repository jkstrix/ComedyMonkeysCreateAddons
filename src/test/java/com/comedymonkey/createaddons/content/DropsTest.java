package com.comedymonkey.createaddons.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** A culled horde's loot joined into full stacks (the cages' and tanks' cull, release and break). */
class DropsTest {
	private static int total(List<ItemStack> stacks, net.minecraft.world.item.Item item) {
		return stacks.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
	}

	@Test
	void manySmallRollsBecomeFullStacks() {
		List<ItemStack> rolls = new ArrayList<>();
		// a zombie horde's culls: 255 rolls of 0-2 rotten flesh, now and then an iron ingot
		for (int i = 0; i < 255; i++) {
			rolls.add(new ItemStack(Items.ROTTEN_FLESH, 1 + i % 2));
			if (i % 50 == 0)
				rolls.add(new ItemStack(Items.IRON_INGOT));
		}
		List<ItemStack> merged = Drops.merged(rolls);
		assertEquals(total(rolls, Items.ROTTEN_FLESH), total(merged, Items.ROTTEN_FLESH));
		assertEquals(total(rolls, Items.IRON_INGOT), total(merged, Items.IRON_INGOT));
		// 382 flesh is 5 full stacks and one of 62; the 6 ingots one stack
		assertEquals(7, merged.size());
		long notFull = merged.stream().filter(s -> s.is(Items.ROTTEN_FLESH) && s.getCount() < 64).count();
		assertEquals(1, notFull);
		for (ItemStack stack : merged)
			assertTrue(stack.getCount() <= stack.getMaxStackSize());
	}

	@Test
	void differentComponentsStayApart() {
		ItemStack named = new ItemStack(Items.BONE, 3);
		named.set(DataComponents.CUSTOM_NAME, Component.literal("Rex"));
		List<ItemStack> merged = Drops.merged(List.of(new ItemStack(Items.BONE, 2), named, new ItemStack(Items.BONE, 4), named.copy()));
		assertEquals(2, merged.size());
		assertEquals(6, merged.get(0).getCount());
		assertEquals(6, merged.get(1).getCount());
		assertTrue(merged.get(1).has(DataComponents.CUSTOM_NAME));
	}

	@Test
	void unstackablesStaySeparate() {
		List<ItemStack> merged = Drops.merged(List.of(new ItemStack(Items.TRIDENT), new ItemStack(Items.TRIDENT)));
		assertEquals(2, merged.size());
	}

	@Test
	void theRollsAreNotChangedAndEmptiesDropOut() {
		ItemStack roll = new ItemStack(Items.ROTTEN_FLESH, 2);
		List<ItemStack> merged = Drops.merged(List.of(roll, ItemStack.EMPTY, new ItemStack(Items.ROTTEN_FLESH, 2)));
		assertEquals(2, roll.getCount());
		assertEquals(1, merged.size());
		assertEquals(4, merged.get(0).getCount());
	}
}
