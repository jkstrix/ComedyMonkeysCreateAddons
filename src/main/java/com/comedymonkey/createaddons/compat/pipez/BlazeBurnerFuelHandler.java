package com.comedymonkey.createaddons.compat.pipez;

import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Insert-only, single-slot view of a Blaze Burner. Each insertion feeds one item through Create's own
 * {@link BlazeBurnerBlock#tryInsert}, the same path as hand-feeding, so Create (and any mod hooking it, such as
 * Create: Liquid Fuel) decides whether the fuel is accepted. A burner that is already stocked up refuses more,
 * so pipes never dump a whole stack into it.
 */
public class BlazeBurnerFuelHandler implements IItemHandler {
	private final BlazeBurnerBlockEntity burner;

	public BlazeBurnerFuelHandler(BlazeBurnerBlockEntity burner) {
		this.burner = burner;
	}

	@Override
	public int getSlots() {
		return 1;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
		if (stack.isEmpty() || !accepts(stack, simulate))
			return stack;
		return stack.copyWithCount(stack.getCount() - 1);
	}

	@Override
	public ItemStack extractItem(int slot, int amount, boolean simulate) {
		return ItemStack.EMPTY;
	}

	@Override
	public int getSlotLimit(int slot) {
		return 1;
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return !stack.isEmpty() && accepts(stack, true);
	}

	private boolean accepts(ItemStack stack, boolean simulate) {
		var level = burner.getLevel();
		if (level == null || level.isClientSide)
			return false;

		BlockState state = burner.getBlockState();
		// Empty burners (no blaze inside) cannot take fuel
		if (!state.hasProperty(BlazeBurnerBlock.HEAT_LEVEL)
			|| state.getValue(BlazeBurnerBlock.HEAT_LEVEL) == BlazeBurnerBlock.HeatLevel.NONE)
			return false;
		// Items that leave something behind (e.g. lava buckets) would lose the container in a pipe, so skip them
		if (stack.hasCraftingRemainingItem())
			return false;

		// doNotConsume = true: we hand back the rest of the stack ourselves
		boolean fed = BlazeBurnerBlock.tryInsert(state, level, burner.getBlockPos(), stack, true, false, simulate)
			.getResult() == InteractionResult.SUCCESS;
		if (fed && !simulate)
			ModAdvancements.awardNearby(level, burner.getBlockPos(), ModAdvancements.PIPED_BLAZE_BURNER, stack, 1);
		return fed;
	}
}
