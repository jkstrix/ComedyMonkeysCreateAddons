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

	private static int heat(IncineratorBlockEntity be) {
		return (Integer) ReflectionAccess.get(be, "heat");
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
		assertEquals(0, heat(be), "not warmed by a simulation");
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
		assertEquals(0, heat(be));
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
	void eachThingAddsAPointOfHeatUpToTen() {
		IncineratorBlockEntity be = incinerator();
		be.getItemHandler().insertItem(0, new ItemStack(Items.STICK, 3), false);
		assertEquals(3, heat(be));
		be.getItemHandler().insertItem(0, new ItemStack(Items.COBBLESTONE, 7), false);
		assertEquals(IncineratorBlockEntity.FULL_HEAT, heat(be));
		be.getItemHandler().insertItem(0, new ItemStack(Items.DIRT, 1000), false);
		assertEquals(IncineratorBlockEntity.FULL_HEAT, heat(be), "no hotter than full");
	}

	@Test
	void aTenthOfABucketIsOneThingWaterToo() {
		IncineratorBlockEntity be = incinerator();
		be.getFluidHandler().fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE);
		assertEquals(IncineratorBlockEntity.FULL_HEAT, heat(be));
		// a little at a time adds up too: 60 + 60 mB is one thing, with 20 mB toward the next
		IncineratorBlockEntity drip = incinerator();
		drip.getFluidHandler().fill(new FluidStack(Fluids.LAVA, 60), FluidAction.EXECUTE);
		assertEquals(0, heat(drip));
		drip.getFluidHandler().fill(new FluidStack(Fluids.LAVA, 60), FluidAction.EXECUTE);
		assertEquals(1, heat(drip));
		assertEquals(20, (Integer) ReflectionAccess.get(drip, "fluidToward"));
	}

	@Test
	void losesAPointASecondUntilCold() {
		IncineratorBlockEntity be = incinerator();
		be.getItemHandler().insertItem(0, new ItemStack(Items.COBBLESTONE, 10), false);
		// The tick would sync the heat, which needs a level
		ReflectionAccess.set(be, "syncQueued", false);
		for (int i = 0; i < 19; i++)
			be.serverTick();
		assertEquals(10, heat(be));
		be.serverTick();
		assertEquals(9, heat(be), "a point a second");
		for (int i = 0; i < 180; i++)
			be.serverTick();
		assertEquals(0, heat(be), "cold ten seconds after the last thing");
		be.serverTick();
		assertEquals(0, heat(be), "stays cold");
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
		assertEquals(IncineratorBlockEntity.FULL_HEAT, heat(loaded));
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
