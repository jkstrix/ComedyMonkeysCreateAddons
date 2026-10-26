package com.comedymonkey.createaddons.content.electric;

import java.util.List;

import com.comedymonkey.createaddons.content.SaveMark;

import net.minecraft.world.level.Level;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * The one store of a group of Batteries: a bank of touching plain Batteries, or an Industrial Battery. It lives only
 * while every member is loaded and nothing about the group has changed; each member's saved charge is its even share of
 * the store ({@link #shareOf}), so breaking, moving or unloading any one of them takes exactly its share. When the group
 * changes the store is dissolved: every member is given its share to keep, and the next to need it builds a new one.
 * <p>
 * A bank moves up to its rate a tick (the Battery's rate times its size); an Industrial Battery as much as its cables
 * carry. Asked without doing it (simulate), it answers for a second at that rate, the way the Power Lines ask: they plan
 * a second at once and hand it out a twentieth a tick, which the per-tick limit then lets through.
 */
public final class BatteryStore implements IEnergyStorage {
	/** Ticks the Power Lines plan at once. */
	private static final int SECOND = 20;

	final List<BatteryBlockEntity> members;
	final boolean tower;
	private final long capacity, perTick;
	private long energy;
	private boolean valid = true;
	/** This tick's moves, against the per-tick rate. */
	private long tick = Long.MIN_VALUE, inNow, outNow;
	/** Over the last second, for the goggles and the lights: FE a tick in and out. */
	private long rollStart = Long.MIN_VALUE, inSum, outSum;
	private float inRate, outRate;
	private long marked = Long.MIN_VALUE;
	/** The last tick its Batteries ran (a gap: they were away, and its record ran it meanwhile). */
	long ran = Long.MIN_VALUE;

	/** Each member's place in the order, for its share. */
	private final java.util.Map<BatteryBlockEntity, Integer> index = new java.util.IdentityHashMap<>();

	BatteryStore(List<BatteryBlockEntity> members, boolean tower, long capacity, long perTick) {
		this.members = members;
		this.tower = tower;
		this.capacity = capacity;
		this.perTick = perTick;
		for (int i = 0; i < members.size(); i++) {
			energy += members.get(i).charge;
			index.put(members.get(i), i);
		}
	}

	boolean valid() {
		return valid;
	}

	/** Every member keeps its share, and the store is gone. */
	void dissolve() {
		if (!valid)
			return;
		valid = false;
		for (int i = 0; i < members.size(); i++) {
			BatteryBlockEntity be = members.get(i);
			be.charge = share(i);
			if (be.store == this)
				be.store = null;
			SaveMark.mark(be);
		}
	}

	/** A member's share: the store split evenly, the odd FE to the first members (in position order). */
	int shareOf(BatteryBlockEntity be) {
		Integer i = index.get(be);
		return i == null ? be.charge : share(i);
	}

	boolean contains(BatteryBlockEntity be) {
		return index.containsKey(be);
	}

	private int share(int i) {
		long n = members.size();
		return (int) (energy / n + (i < energy % n ? 1 : 0));
	}

	long energy() {
		return energy;
	}

	long capacity() {
		return capacity;
	}

	/** How full it is, 0 to 1 (a bank re-formed from a bigger tower may hold more than it could take in). */
	float fill() {
		return capacity <= 0 ? 0 : Math.min(1, energy / (float) capacity);
	}

	/** FE a tick over the last second: in, and out. */
	float inRate() {
		return inRate;
	}

	float outRate() {
		return outRate;
	}

	/** What it did while away, on paper: taken on once as it starts (never below empty). */
	void resume(long delta) {
		energy = Math.max(0, energy + delta);
	}

	long perTick() {
		return perTick;
	}

	/** What was drawn from it and not used, back: past its capacity and rate, since it was its own. */
	int refund(int fe) {
		if (!valid || fe <= 0)
			return 0;
		energy += fe;
		outNow = Math.max(0, outNow - fe);
		changed();
		return fe;
	}

	/** Lightning and the like: in, past the rate, up to full (what doesn't fit is lost). */
	int add(int amount) {
		int added = (int) Math.max(0, Math.min(amount, capacity - energy));
		if (added > 0) {
			energy += added;
			changed();
		}
		return added;
	}

	/** Keeps the per-tick and per-second counts current. */
	void roll(long now) {
		if (tick != now) {
			tick = now;
			inNow = outNow = 0;
		}
		if (rollStart == Long.MIN_VALUE)
			rollStart = now;
		if (now - rollStart >= SECOND) {
			inRate = inSum / (float) (now - rollStart);
			outRate = outSum / (float) (now - rollStart);
			inSum = outSum = 0;
			rollStart = now;
		}
	}

	private long now() {
		Level level = members.get(0).getLevel();
		return level == null ? 0 : level.getGameTime();
	}

	@Override
	public int receiveEnergy(int max, boolean simulate) {
		if (!valid || max <= 0)
			return 0;
		roll(now());
		long limit = simulate ? perTick * SECOND : perTick - inNow;
		int got = (int) Math.max(0, Math.min(max, Math.min(capacity - energy, limit)));
		if (!simulate && got > 0) {
			energy += got;
			inNow += got;
			inSum += got;
			changed();
		}
		return got;
	}

	@Override
	public int extractEnergy(int max, boolean simulate) {
		if (!valid || max <= 0)
			return 0;
		roll(now());
		long limit = simulate ? perTick * SECOND : perTick - outNow;
		int got = (int) Math.max(0, Math.min(max, Math.min(energy, limit)));
		if (!simulate && got > 0) {
			energy -= got;
			outNow += got;
			outSum += got;
			changed();
		}
		return got;
	}

	/** The members' chunks need saving (once a tick is enough). */
	private void changed() {
		long now = now();
		if (marked == now)
			return;
		marked = now;
		for (BatteryBlockEntity be : members)
			SaveMark.mark(be);
	}

	@Override
	public int getEnergyStored() {
		return (int) Math.min(Integer.MAX_VALUE, energy);
	}

	/** Never less than it holds, so a bank holding more than it could take in reads as full. */
	@Override
	public int getMaxEnergyStored() {
		return (int) Math.min(Integer.MAX_VALUE, Math.max(capacity, energy));
	}

	@Override
	public boolean canExtract() {
		return valid;
	}

	@Override
	public boolean canReceive() {
		return valid;
	}
}
