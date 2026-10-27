package com.comedymonkey.createaddons.content;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * What pipes see of a machine's tank that only takes fluid in (a Fish Tank's or Flooded Monster Cage's water): they can
 * fill it and read it, but draw nothing back out.
 */
public record FillOnly(IFluidHandler tank) implements IFluidHandler {
	@Override
	public int getTanks() {
		return tank.getTanks();
	}

	@Override
	public FluidStack getFluidInTank(int index) {
		return tank.getFluidInTank(index);
	}

	@Override
	public int getTankCapacity(int index) {
		return tank.getTankCapacity(index);
	}

	@Override
	public boolean isFluidValid(int index, FluidStack stack) {
		return tank.isFluidValid(index, stack);
	}

	@Override
	public int fill(FluidStack resource, FluidAction action) {
		return tank.fill(resource, action);
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
