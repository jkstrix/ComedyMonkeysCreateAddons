package com.comedymonkey.createaddons.content.incinerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.testutil.ReflectionAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** The Incinerator's item and fluid sinks, its burn timer and its saved totals, on a block entity with no level. */
class IncineratorBlockEntityTest {
	private static final HolderLookup.Provider REGISTRIES = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

	private static IncineratorBlockEntity incinerator() {
		return new IncineratorBlockEntity(BlockPos.ZERO,
			BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("comedymonkeysaddons", "incinerator"))
				.defaultBlockState());
	}

	private static long itemsBurned(IncineratorBlockEntity be) {
		return (Long) ReflectionAccess.get(be, "itemsBurned");
	}

	private static long fluidBurned(IncineratorBlockEntity be) {
		return (Long) ReflectionAccess.get(be, "fluidBurned");
	}

	private static int burnTicks(IncineratorBlockEntity be) {
		return (Integer) ReflectionAccess.get(be, "burnTicks");
	}

	@Test
	void swallowsWholeStacks() {
		IncineratorBlockEntity be = incinerator();
		assertTrue(be.getItemHandler().insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false).isEmpty());
		assertTrue(be.getItemHandler().insertItem(0, new ItemStack(Items.DIRT, 200), false).isEmpty(), "even oversized stacks");
		assertEquals(264, itemsBurned(be));
	}

	@Test
	void simulatingBurnsNothing() {
		IncineratorBlockEntity be = incinerator();
		assertTrue(be.getItemHandler().insertItem(0, new ItemStack(Items.COBBLESTONE, 64), true).isEmpty());
		assertEquals(0, itemsBurned(be));
		assertEquals(0, burnTicks(be), "not lit by a simulation");
		assertEquals(1000, be.getFluidHandler().fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.SIMULATE));
		assertEquals(0, fluidBurned(be));
	}

	@Test
	void givesNothingBack() {
		IncineratorBlockEntity be = incinerator();
		be.getItemHandler().insertItem(0, new ItemStack(Items.DIAMOND, 3), false);
		assertTrue(be.getItemHandler().getStackInSlot(0).isEmpty());
		assertTrue(be.getItemHandler().extractItem(0, 64, false).isEmpty());
		be.getFluidHandler().fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE);
		assertTrue(be.getFluidHandler().getFluidInTank(0).isEmpty());
		assertTrue(be.getFluidHandler().drain(500, FluidAction.EXECUTE).isEmpty());
		assertTrue(be.getFluidHandler().drain(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE).isEmpty());
	}

	@Test
	void emptyOffersAreIgnored() {
		IncineratorBlockEntity be = incinerator();
		assertSame(ItemStack.EMPTY, be.getItemHandler().insertItem(0, ItemStack.EMPTY, false));
		assertEquals(0, be.getFluidHandler().fill(FluidStack.EMPTY, FluidAction.EXECUTE));
		assertEquals(0, burnTicks(be));
	}

	@Test
	void takesAnyFluidInAnyAmount() {
		IncineratorBlockEntity be = incinerator();
		assertEquals(Integer.MAX_VALUE, be.getFluidHandler().getTankCapacity(0));
		assertTrue(be.getFluidHandler().isFluidValid(0, new FluidStack(Fluids.LAVA, 1)));
		assertEquals(Integer.MAX_VALUE, be.getFluidHandler().fill(new FluidStack(Fluids.WATER, Integer.MAX_VALUE), FluidAction.EXECUTE));
		assertEquals(Integer.MAX_VALUE, be.getFluidHandler().fill(new FluidStack(Fluids.WATER, Integer.MAX_VALUE), FluidAction.EXECUTE));
		assertEquals(2L * Integer.MAX_VALUE, fluidBurned(be), "the total doesn't overflow");
	}

	@Test
	void burningLightsTheFireForFiveSeconds() {
		IncineratorBlockEntity be = incinerator();
		be.getItemHandler().insertItem(0, new ItemStack(Items.STICK), false);
		assertEquals(100, burnTicks(be));
		// The tick would sync the totals, which needs a level
		ReflectionAccess.set(be, "syncQueued", false);
		for (int i = 0; i < 99; i++)
			be.serverTick();
		assertEquals(1, burnTicks(be));
		be.serverTick();
		assertEquals(0, burnTicks(be));
		be.serverTick();
		assertEquals(0, burnTicks(be), "stays out");
	}

	@Test
	void eachBurnRestartsTheTimer() {
		IncineratorBlockEntity be = incinerator();
		be.getFluidHandler().fill(new FluidStack(Fluids.LAVA, 10), FluidAction.EXECUTE);
		ReflectionAccess.set(be, "syncQueued", false);
		for (int i = 0; i < 60; i++)
			be.serverTick();
		assertEquals(40, burnTicks(be));
		be.getFluidHandler().fill(new FluidStack(Fluids.LAVA, 10), FluidAction.EXECUTE);
		assertEquals(100, burnTicks(be));
	}

	@Test
	void saveAndLoadKeepTheTotals() {
		IncineratorBlockEntity be = incinerator();
		be.getItemHandler().insertItem(0, new ItemStack(Items.COBBLESTONE, 64), false);
		ReflectionAccess.set(be, "fluidBurned", 5_000_000_000L);

		CompoundTag tag = be.saveWithFullMetadata(REGISTRIES);
		IncineratorBlockEntity loaded = incinerator();
		loaded.loadWithComponents(tag, REGISTRIES);
		assertEquals(64, itemsBurned(loaded));
		assertEquals(5_000_000_000L, fluidBurned(loaded));
		assertEquals(100, burnTicks(loaded));
	}

	@Test
	void updateTagCarriesTheTotalsForTheGoggles() {
		IncineratorBlockEntity be = incinerator();
		be.getItemHandler().insertItem(0, new ItemStack(Items.COBBLESTONE, 12), false);
		be.getFluidHandler().fill(new FluidStack(Fluids.WATER, 3000), FluidAction.EXECUTE);
		IncineratorBlockEntity client = incinerator();
		client.loadWithComponents(be.getUpdateTag(REGISTRIES), REGISTRIES);
		assertEquals(12, itemsBurned(client));
		assertEquals(3000, fluidBurned(client));
	}
}
