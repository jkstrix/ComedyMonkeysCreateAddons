package com.comedymonkey.createaddons.content.wellspring;

import java.util.function.Supplier;

import com.comedymonkey.createaddons.registry.ModItems;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;

/** The kinds of upgrade a Fluid Wellspring accepts. */
public enum WellspringUpgrade implements StringRepresentable {
	/** Generates water. Shares the generation slots with {@link #DRIPSTONE}. */
	WATER("water", WellspringSource.WATER, ModItems.WATER_UPGRADE),
	/** Generates lava. Shares the generation slots with {@link #WATER}. */
	DRIPSTONE("dripstone", WellspringSource.LAVA, ModItems.DRIPSTONE_UPGRADE),
	/** Generates Create's honey. Shares the generation slots with the other fluids. */
	HONEY("honey", WellspringSource.HONEY, ModItems.HONEY_UPGRADE),
	/** Generates Create's chocolate. Shares the generation slots with the other fluids. */
	CHOCOLATE("chocolate", WellspringSource.CHOCOLATE, ModItems.CHOCOLATE_UPGRADE),
	/** Increases tank capacity. Has its own slots. */
	CAPACITY("capacity", WellspringSource.NONE, ModItems.CAPACITY_UPGRADE);

	private final String name;
	private final WellspringSource source;
	private final Supplier<? extends Item> item;

	WellspringUpgrade(String name, WellspringSource source, Supplier<? extends Item> item) {
		this.name = name;
		this.source = source;
		this.item = item;
	}

	/** The fluid this upgrade generates, or NONE for capacity upgrades. */
	public WellspringSource source() {
		return source;
	}

	public boolean isGeneration() {
		return source != WellspringSource.NONE;
	}

	public Item item() {
		return item.get();
	}

	/** The generation upgrade for a fluid, or null for none. */
	public static WellspringUpgrade forSource(WellspringSource source) {
		for (WellspringUpgrade upgrade : values())
			if (upgrade.source == source && source != WellspringSource.NONE)
				return upgrade;
		return null;
	}

	public static WellspringUpgrade byName(String name) {
		for (WellspringUpgrade upgrade : values())
			if (upgrade.name.equals(name))
				return upgrade;
		return null;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
