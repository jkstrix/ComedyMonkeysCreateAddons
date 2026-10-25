package com.comedymonkey.createaddons.content.line;

import java.util.function.Supplier;

import com.comedymonkey.createaddons.registry.ModItems;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;

/**
 * The parts that clip onto a line, one per block. The Intake goes on the side facing a tank; the rest go on a straight
 * piece, round the line. Each is a real part you can see and wrench off, never a slot in a menu.
 */
public enum Fitting implements StringRepresentable {
	NONE("none", false, false, () -> null),
	/** Fluid: draws from the tank it faces, the only part that moves fluid. Has a filter. */
	INTAKE("intake", true, false, ModItems.INTAKE),
	/** Fluid: shuts on a redstone signal; its wheel throttles the flow in 10% steps. */
	VALVE("valve", true, false, ModItems.VALVE),
	/** Fluid: lets one fluid past and nothing else. */
	FILTER("filter", true, false, ModItems.FILTER),
	/** Fluid or power: a gauge reading the flow through it; comparators read it too. */
	METER("meter", true, true, ModItems.METER),
	/** Fluid or power: how what passes it is shared between the ends beyond it. */
	SPLITTER("splitter", true, true, ModItems.SPLITTER),
	/** Power: a switch that cuts the line, with a lever or by redstone. */
	BREAKER("breaker", false, true, ModItems.BREAKER);

	private final String name;
	private final boolean fluid, power;
	private final Supplier<? extends Item> item;

	Fitting(String name, boolean fluid, boolean power, Supplier<? extends Item> item) {
		this.name = name;
		this.fluid = fluid;
		this.power = power;
		this.item = item;
	}

	public boolean fitsFluidLines() {
		return fluid;
	}

	public boolean fitsPowerLines() {
		return power;
	}

	public Item item() {
		return item.get();
	}

	public static Fitting byName(String name) {
		for (Fitting f : values())
			if (f.name.equals(name))
				return f;
		return NONE;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
