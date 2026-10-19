package com.comedymonkey.createaddons.compat.pipez;

import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A fill-only "tank" on a Portable Engine. A fluid is fuel if its bucket is: lava, or any modded fluid whose bucket
 * burns in a furnace. Each 1000 mB gives the engine the bucket's burn time, added straight onto the fuel it is
 * burning. It only tops the engine up to one bucket's worth, so it doesn't hoard fuel, and it never mixes into a
 * superheated (Blaze Cake) burn.
 */
class EngineFuelTank implements IFluidHandler {
	private final BlockEntity engine;

	EngineFuelTank(BlockEntity engine) {
		this.engine = engine;
	}

	/** Burn time, in ticks, of a full bucket of this fluid; 0 if it isn't fuel. */
	static int bucketBurnTime(FluidStack fluid) {
		if (fluid.isEmpty())
			return 0;
		var bucket = fluid.getFluid().getBucket();
		return new ItemStack(bucket).getBurnTime(RecipeType.SMELTING);
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		int perBucket = bucketBurnTime(resource);
		if (perBucket <= 0)
			return 0;
		try {
			int burnTime = (int) PipezEngineCompat.getBurnTime.invoke(engine);
			if ((boolean) PipezEngineCompat.isFuelInfinite.invoke(engine))
				return 0;
			if (burnTime > 0 && (boolean) PipezEngineCompat.isSuperHeated.invoke(engine))
				return 0;
			int room = perBucket - burnTime;
			if (room <= 0)
				return 0;
			// Round up so a nearly-full engine still takes the last drop it needs
			int accepted = Math.min(resource.getAmount(),
				(int) Math.ceil(room * (double) FluidType.BUCKET_VOLUME / perBucket));
			if (accepted <= 0)
				return 0;
			if (action.execute()) {
				if (burnTime <= 0)
					PipezEngineCompat.setSuperHeated.invoke(engine, false);
				PipezEngineCompat.setBurnTime.invoke(engine,
					burnTime + (int) ((long) accepted * perBucket / FluidType.BUCKET_VOLUME));
				if (engine instanceof SyncedBlockEntity synced)
					synced.notifyUpdate();
				ModAdvancements.awardNearby(engine.getLevel(), engine.getBlockPos(), ModAdvancements.PIPED_ENGINE_FUEL,
					new ItemStack(resource.getFluid().getBucket()), accepted);
			}
			return accepted;
		} catch (Throwable e) {
			return 0;
		}
	}

	@Override
	public int getTanks() {
		return 1;
	}

	@Override
	public FluidStack getFluidInTank(int tank) {
		return FluidStack.EMPTY;
	}

	@Override
	public int getTankCapacity(int tank) {
		return FluidType.BUCKET_VOLUME;
	}

	@Override
	public boolean isFluidValid(int tank, FluidStack stack) {
		return bucketBurnTime(stack) > 0;
	}

	@Override
	public FluidStack drain(FluidStack resource, FluidAction action) {
		return FluidStack.EMPTY;
	}

	@Override
	public FluidStack drain(int maxDrain, FluidAction action) {
		return FluidStack.EMPTY;
	}
}
