package com.comedymonkey.createaddons.content.wellspring;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/** Which fluid a wellspring is set up for. Also used as a blockstate property for the indicator lamps. */
public enum WellspringSource implements StringRepresentable {
	NONE("none"),
	WATER("water"),
	LAVA("lava"),
	/** Create's honey: meets lava as limestone. */
	HONEY("honey"),
	/** Create's chocolate: meets lava as scoria. */
	CHOCOLATE("chocolate");

	private final String name;

	WellspringSource(String name) {
		this.name = name;
	}

	public Fluid fluid() {
		return switch (this) {
			case WATER -> Fluids.WATER;
			case LAVA -> Fluids.LAVA;
			case HONEY -> create("honey");
			case CHOCOLATE -> create("chocolate");
			case NONE -> Fluids.EMPTY;
		};
	}

	private static Fluid create(String id) {
		return BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("create", id));
	}

	public boolean matches(FluidStack stack) {
		return this != NONE && !stack.isEmpty() && stack.getFluid().isSame(fluid());
	}

	public static WellspringSource of(FluidStack stack) {
		for (WellspringSource source : values())
			if (source.matches(stack))
				return source;
		return NONE;
	}

	public static WellspringSource byName(String name) {
		for (WellspringSource source : values())
			if (source.name.equals(name))
				return source;
		return NONE;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
