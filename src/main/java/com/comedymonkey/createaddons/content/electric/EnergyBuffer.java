package com.comedymonkey.createaddons.content.electric;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** A Forge Energy (FE) store that tells its owner when it changes, and can be filled or emptied from inside. */
public class EnergyBuffer extends EnergyStorage {
	private final Runnable onChanged;

	public EnergyBuffer(int capacity, int maxReceive, int maxExtract, Runnable onChanged) {
		super(capacity, maxReceive, maxExtract);
		this.onChanged = onChanged;
	}

	@Override
	public int receiveEnergy(int toReceive, boolean simulate) {
		int received = super.receiveEnergy(toReceive, simulate);
		if (received > 0 && !simulate)
			onChanged.run();
		return received;
	}

	@Override
	public int extractEnergy(int toExtract, boolean simulate) {
		int extracted = super.extractEnergy(toExtract, simulate);
		if (extracted > 0 && !simulate)
			onChanged.run();
		return extracted;
	}

	/** Adds what fits, ignoring the receive limit. */
	public int generate(int amount) {
		int added = Math.min(amount, capacity - energy);
		if (added > 0) {
			energy += added;
			onChanged.run();
		}
		return added;
	}

	/** Takes up to {@code amount}, ignoring the extract limit. */
	public int consume(int amount) {
		int taken = Math.min(amount, energy);
		if (taken > 0) {
			energy -= taken;
			onChanged.run();
		}
		return taken;
	}

	public void setEnergy(int amount) {
		energy = Math.max(0, Math.min(capacity, amount));
	}

	public boolean isFull() {
		return energy >= capacity;
	}

	/** Pushes energy into whatever next to {@code pos} takes it on side {@code side}; returns what was sent. */
	public int pushTo(Level level, BlockPos pos, Direction side, int limit) {
		if (energy <= 0)
			return 0;
		IEnergyStorage target = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(side), side.getOpposite());
		if (target == null || !target.canReceive())
			return 0;
		int sent = target.receiveEnergy(Math.min(energy, limit), false);
		return consume(sent);
	}

	/** A view others can only take from. */
	public IEnergyStorage extractOnly() {
		return new IEnergyStorage() {
			@Override
			public int receiveEnergy(int toReceive, boolean simulate) {
				return 0;
			}

			@Override
			public int extractEnergy(int toExtract, boolean simulate) {
				return EnergyBuffer.this.extractEnergy(toExtract, simulate);
			}

			@Override
			public int getEnergyStored() {
				return EnergyBuffer.this.getEnergyStored();
			}

			@Override
			public int getMaxEnergyStored() {
				return EnergyBuffer.this.getMaxEnergyStored();
			}

			@Override
			public boolean canExtract() {
				return true;
			}

			@Override
			public boolean canReceive() {
				return false;
			}
		};
	}
}
