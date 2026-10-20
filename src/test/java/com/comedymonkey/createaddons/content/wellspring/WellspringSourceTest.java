package com.comedymonkey.createaddons.content.wellspring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/** Which fluid each source stands for, and how fluids are recognised. */
class WellspringSourceTest {
	private static void assumeCreateFluid(String id) {
		assumeTrue(BuiltInRegistries.FLUID.containsKey(ResourceLocation.fromNamespaceAndPath("create", id)),
			"Create's " + id + " is not registered");
	}

	@Test
	void vanillaFluids() {
		assertSame(Fluids.WATER, WellspringSource.WATER.fluid());
		assertSame(Fluids.LAVA, WellspringSource.LAVA.fluid());
		assertSame(Fluids.EMPTY, WellspringSource.NONE.fluid());
	}

	@Test
	void createFluids() {
		assumeCreateFluid("honey");
		assumeCreateFluid("chocolate");
		assertNotEquals(Fluids.EMPTY, WellspringSource.HONEY.fluid());
		assertNotEquals(Fluids.EMPTY, WellspringSource.CHOCOLATE.fluid());
		assertEquals(WellspringSource.HONEY, WellspringSource.of(new FluidStack(WellspringSource.HONEY.fluid(), 1000)));
		assertEquals(WellspringSource.CHOCOLATE,
			WellspringSource.of(new FluidStack(WellspringSource.CHOCOLATE.fluid(), 1000)));
	}

	@Test
	void matchesOnlyItsOwnFluid() {
		FluidStack water = new FluidStack(Fluids.WATER, 1000);
		FluidStack lava = new FluidStack(Fluids.LAVA, 1000);
		assertTrue(WellspringSource.WATER.matches(water));
		assertFalse(WellspringSource.WATER.matches(lava));
		assertTrue(WellspringSource.LAVA.matches(lava));
		assertFalse(WellspringSource.LAVA.matches(water));
	}

	@Test
	void flowingFluidCountsAsTheSame() {
		assertTrue(WellspringSource.WATER.matches(new FluidStack(Fluids.FLOWING_WATER, 1)));
		assertTrue(WellspringSource.LAVA.matches(new FluidStack(Fluids.FLOWING_LAVA, 1)));
	}

	@Test
	void nothingMatchesEmpty() {
		for (WellspringSource source : WellspringSource.values())
			assertFalse(source.matches(FluidStack.EMPTY), source.name());
		// NONE stands for "no fluid", never for an actual fluid
		assertFalse(WellspringSource.NONE.matches(new FluidStack(Fluids.WATER, 1000)));
	}

	@Test
	void ofFindsTheSource() {
		assertEquals(WellspringSource.WATER, WellspringSource.of(new FluidStack(Fluids.WATER, 1)));
		assertEquals(WellspringSource.LAVA, WellspringSource.of(new FluidStack(Fluids.LAVA, 250)));
		assertEquals(WellspringSource.NONE, WellspringSource.of(FluidStack.EMPTY));
	}

	@Test
	void ofIgnoresFluidsItCannotMake() {
		assumeCreateFluid("tea");
		var tea = BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("create", "tea"));
		assertEquals(WellspringSource.NONE, WellspringSource.of(new FluidStack(tea, 1000)));
	}

	@Test
	void namesRoundTrip() {
		Set<String> names = new HashSet<>();
		for (WellspringSource source : WellspringSource.values()) {
			String name = source.getSerializedName();
			assertEquals(name.toLowerCase(Locale.ROOT), name, "blockstate values must be lower case");
			assertTrue(names.add(name), "duplicate name " + name);
			assertEquals(source, WellspringSource.byName(name));
		}
	}

	@Test
	void unknownNameIsNone() {
		assertEquals(WellspringSource.NONE, WellspringSource.byName("milk"));
		assertEquals(WellspringSource.NONE, WellspringSource.byName(""));
	}
}
