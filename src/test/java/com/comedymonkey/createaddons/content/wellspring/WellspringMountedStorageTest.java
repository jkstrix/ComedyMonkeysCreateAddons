package com.comedymonkey.createaddons.content.wellspring;

import static com.comedymonkey.createaddons.content.wellspring.WellspringTestSupport.REGISTRIES;
import static com.comedymonkey.createaddons.content.wellspring.WellspringTestSupport.fill;
import static com.comedymonkey.createaddons.content.wellspring.WellspringTestSupport.install;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.testutil.ReflectionAccess;
import com.comedymonkey.createaddons.testutil.TestConfig;
import com.mojang.serialization.DynamicOps;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** A Wellspring's tank while it rides a contraption: generation, the fluid rule, saving and writing back. */
class WellspringMountedStorageTest {
	@BeforeEach
	void config() {
		TestConfig.loadDefaults();
	}

	@AfterEach
	void restore() {
		TestConfig.reset();
	}

	private static WellspringMountedStorage storage(int capacity, FluidStack fluid, WellspringSource source, int generators,
		int progress) {
		return new WellspringMountedStorage(capacity, fluid, source, generators, progress);
	}

	private static void generate(WellspringMountedStorage storage, int ticks) {
		for (int i = 0; i < ticks; i++)
			storage.generate();
	}

	@Test
	void generatesAtTheBlockRate() {
		WellspringMountedStorage storage = storage(8000, new FluidStack(Fluids.WATER, 100), WellspringSource.WATER, 1, 0);
		generate(storage, 20);
		assertEquals(200, storage.getFluidInTank(0).getAmount());

		WellspringMountedStorage lava = storage(8000, FluidStack.EMPTY, WellspringSource.LAVA, 3, 0);
		generate(lava, 20);
		assertEquals(50, lava.getFluidInTank(0).getAmount());
		assertTrue(lava.getFluidInTank(0).is(Fluids.LAVA));
	}

	@Test
	void doesNotGenerateWithoutUpgradesOrWhenFull() {
		WellspringMountedStorage idle = storage(8000, FluidStack.EMPTY, WellspringSource.NONE, 0, 0);
		generate(idle, 40);
		assertTrue(idle.getFluidInTank(0).isEmpty());

		WellspringMountedStorage full = storage(1000, new FluidStack(Fluids.WATER, 1000), WellspringSource.WATER, 3, 13);
		generate(full, 1);
		assertEquals(1000, full.getFluidInTank(0).getAmount());
		assertEquals(0, ReflectionAccess.get(full, "progress"));
	}

	@Test
	void keepsTheBlocksFluidRule() {
		WellspringMountedStorage water = storage(8000, FluidStack.EMPTY, WellspringSource.WATER, 1, 0);
		assertEquals(0, water.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE));
		assertFalse(water.isFluidValid(0, new FluidStack(Fluids.LAVA, 1)));
		assertEquals(1000, water.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE));

		WellspringMountedStorage any = storage(8000, FluidStack.EMPTY, WellspringSource.NONE, 0, 0);
		assertEquals(1000, any.fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE));
	}

	@Test
	void drainsLikeATank() {
		WellspringMountedStorage storage = storage(8000, new FluidStack(Fluids.WATER, 3000), WellspringSource.WATER, 1, 0);
		assertEquals(8000, storage.getTankCapacity(0));
		assertEquals(1000, storage.drain(1000, FluidAction.EXECUTE).getAmount());
		assertEquals(2000, storage.getFluidInTank(0).getAmount());
		assertTrue(storage.drain(new FluidStack(Fluids.LAVA, 500), FluidAction.EXECUTE).isEmpty());
	}

	@Test
	void changesMarkItForSyncing() {
		WellspringMountedStorage storage = storage(8000, FluidStack.EMPTY, WellspringSource.WATER, 1, 0);
		assertFalse(storage.isDirty());
		storage.fill(new FluidStack(Fluids.WATER, 10), FluidAction.EXECUTE);
		assertTrue(storage.isDirty());
		storage.markClean();
		assertFalse(storage.isDirty());
	}

	@Test
	void constructorCopiesTheFluid() {
		FluidStack fluid = new FluidStack(Fluids.WATER, 500);
		WellspringMountedStorage storage = storage(8000, fluid, WellspringSource.WATER, 1, 0);
		fluid.setAmount(1);
		assertEquals(500, storage.getFluidInTank(0).getAmount());
	}

	@Test
	void codecRoundTrip() {
		DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, REGISTRIES);
		WellspringMountedStorage storage = storage(24000, new FluidStack(Fluids.LAVA, 12345), WellspringSource.LAVA, 2, 17);
		Tag tag = WellspringMountedStorage.CODEC.codec().encodeStart(ops, storage).getOrThrow();
		WellspringMountedStorage decoded = WellspringMountedStorage.CODEC.codec().parse(ops, tag).getOrThrow();

		assertEquals(24000, decoded.getTankCapacity(0));
		assertEquals(12345, decoded.getFluidInTank(0).getAmount());
		assertTrue(decoded.getFluidInTank(0).is(Fluids.LAVA));
		assertEquals(WellspringSource.LAVA, ReflectionAccess.get(decoded, "source"));
		assertEquals(2, ReflectionAccess.get(decoded, "generators"));
		assertEquals(17, ReflectionAccess.get(decoded, "progress"));
		assertEquals(tag, WellspringMountedStorage.CODEC.codec().encodeStart(ops, decoded).getOrThrow());
	}

	@Test
	void codecRoundTripWhenEmpty() {
		DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, REGISTRIES);
		WellspringMountedStorage storage = storage(8000, FluidStack.EMPTY, WellspringSource.NONE, 0, 0);
		Tag tag = WellspringMountedStorage.CODEC.codec().encodeStart(ops, storage).getOrThrow();
		WellspringMountedStorage decoded = WellspringMountedStorage.CODEC.codec().parse(ops, tag).getOrThrow();
		assertTrue(decoded.getFluidInTank(0).isEmpty());
		assertEquals(WellspringSource.NONE, ReflectionAccess.get(decoded, "source"));
	}

	@Test
	void mountCopiesTheBlockEntity() {
		WellspringBlockEntity be = WellspringTestSupport.create();
		install(be, 0, WellspringUpgrade.WATER);
		install(be, 1, WellspringUpgrade.WATER);
		fill(be, new FluidStack(Fluids.WATER, 4321));
		be.setGenerationProgress(9);

		WellspringMountedStorage storage = WellspringMountedStorage.of(be);
		assertEquals(8000, storage.getTankCapacity(0));
		assertEquals(4321, storage.getFluidInTank(0).getAmount());
		assertEquals(WellspringSource.WATER, ReflectionAccess.get(storage, "source"));
		assertEquals(2, ReflectionAccess.get(storage, "generators"));
		assertEquals(9, ReflectionAccess.get(storage, "progress"));

		// Generating on the contraption doesn't touch the original
		generate(storage, 20);
		assertEquals(4321, be.getTank().getFluidAmount());
	}

	@Test
	void unmountWritesTheTankBack() {
		WellspringMountedStorage storage = storage(24000, new FluidStack(Fluids.WATER, 20000), WellspringSource.WATER, 1, 7);
		WellspringBlockEntity placed = WellspringTestSupport.create();
		storage.unmount(null, null, BlockPos.ZERO, placed);
		assertEquals(24000, placed.getTank().getCapacity(), "keeps room for everything carried back");
		assertEquals(20000, placed.getTank().getFluidAmount());
		assertTrue(placed.getTank().getFluid().is(Fluids.WATER));
		assertEquals(7, placed.getGenerationProgress());
	}

	@Test
	void unmountNeverShrinksTheTank() {
		WellspringMountedStorage storage = storage(8000, new FluidStack(Fluids.LAVA, 100), WellspringSource.LAVA, 1, 0);
		WellspringBlockEntity placed = WellspringTestSupport.create();
		placed.getTank().setCapacity(56000);
		storage.unmount(null, null, BlockPos.ZERO, placed);
		assertEquals(56000, placed.getTank().getCapacity());
		assertEquals(100, placed.getTank().getFluidAmount());
	}

	@Test
	void unmountCopiesRatherThanSharesTheFluid() {
		WellspringMountedStorage storage = storage(8000, new FluidStack(Fluids.WATER, 1000), WellspringSource.WATER, 1, 0);
		WellspringBlockEntity placed = WellspringTestSupport.create();
		storage.unmount(null, null, BlockPos.ZERO, placed);
		storage.drain(1000, IFluidHandler.FluidAction.EXECUTE);
		assertEquals(1000, placed.getTank().getFluidAmount());
	}
}
