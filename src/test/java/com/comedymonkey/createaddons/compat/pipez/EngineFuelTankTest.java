package com.comedymonkey.createaddons.compat.pipez;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * The Portable Engine's fluid fuel tank. Simulated isn't on the compile classpath and its engine needs a level, so the
 * method handles {@link EngineFuelTank} calls are pointed at a stand-in engine for these tests.
 */
class EngineFuelTankTest {
	/** A lava bucket's furnace burn time. */
	private static final int LAVA = 20000;
	/** Furnace fuel values come from a data map, which isn't loaded without a server; pin lava's for the tests. */
	private static final Consumer<FurnaceFuelBurnTimeEvent> LAVA_FUEL = event -> {
		if (event.getItemStack().is(Items.LAVA_BUCKET))
			event.setBurnTime(LAVA);
	};

	private MethodHandle oldGetBurnTime, oldSetBurnTime, oldIsSuperHeated, oldSetSuperHeated, oldIsFuelInfinite;
	private FakeEngine engine;
	private EngineFuelTank tank;

	/** Just the accessors {@link PipezEngineCompat} binds on Simulated's PortableEngineBlockEntity. */
	public static class FakeEngine extends BlockEntity {
		public int burnTime;
		public boolean superHeated;
		public boolean infinite;

		public FakeEngine() {
			super(BlockEntityType.FURNACE, BlockPos.ZERO, Blocks.FURNACE.defaultBlockState());
		}

		public int getCurrentBurnTime() {
			return burnTime;
		}

		public void setCurrentBurnTime(int burnTime) {
			this.burnTime = burnTime;
		}

		public boolean isSuperHeated() {
			return superHeated;
		}

		public void setSuperHeated(boolean superHeated) {
			this.superHeated = superHeated;
		}

		public boolean isCurrentFuelInfinite() {
			return infinite;
		}
	}

	@BeforeAll
	static void pinLavaBurnTime() {
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, FurnaceFuelBurnTimeEvent.class, LAVA_FUEL);
	}

	@AfterAll
	static void unpinLavaBurnTime() {
		NeoForge.EVENT_BUS.unregister(LAVA_FUEL);
	}

	@BeforeEach
	void bindFakeEngine() throws ReflectiveOperationException {
		oldGetBurnTime = PipezEngineCompat.getBurnTime;
		oldSetBurnTime = PipezEngineCompat.setBurnTime;
		oldIsSuperHeated = PipezEngineCompat.isSuperHeated;
		oldSetSuperHeated = PipezEngineCompat.setSuperHeated;
		oldIsFuelInfinite = PipezEngineCompat.isFuelInfinite;

		MethodHandles.Lookup lookup = MethodHandles.lookup();
		PipezEngineCompat.getBurnTime = lookup.findVirtual(FakeEngine.class, "getCurrentBurnTime", MethodType.methodType(int.class));
		PipezEngineCompat.setBurnTime = lookup.findVirtual(FakeEngine.class, "setCurrentBurnTime",
			MethodType.methodType(void.class, int.class));
		PipezEngineCompat.isSuperHeated = lookup.findVirtual(FakeEngine.class, "isSuperHeated", MethodType.methodType(boolean.class));
		PipezEngineCompat.setSuperHeated = lookup.findVirtual(FakeEngine.class, "setSuperHeated",
			MethodType.methodType(void.class, boolean.class));
		PipezEngineCompat.isFuelInfinite = lookup.findVirtual(FakeEngine.class, "isCurrentFuelInfinite",
			MethodType.methodType(boolean.class));

		engine = new FakeEngine();
		tank = new EngineFuelTank(engine);
	}

	@AfterEach
	void restoreHandles() {
		PipezEngineCompat.getBurnTime = oldGetBurnTime;
		PipezEngineCompat.setBurnTime = oldSetBurnTime;
		PipezEngineCompat.isSuperHeated = oldIsSuperHeated;
		PipezEngineCompat.setSuperHeated = oldSetSuperHeated;
		PipezEngineCompat.isFuelInfinite = oldIsFuelInfinite;
	}

	private static FluidStack lava(int amount) {
		return new FluidStack(Fluids.LAVA, amount);
	}

	// ---- What counts as fuel

	@Test
	void bucketBurnTime() {
		assertEquals(LAVA, EngineFuelTank.bucketBurnTime(lava(1)));
		assertEquals(0, EngineFuelTank.bucketBurnTime(new FluidStack(Fluids.WATER, 1000)));
		assertEquals(0, EngineFuelTank.bucketBurnTime(FluidStack.EMPTY));
	}

	@Test
	void onlyFuelIsValid() {
		assertTrue(tank.isFluidValid(0, lava(1000)));
		assertFalse(tank.isFluidValid(0, new FluidStack(Fluids.WATER, 1000)));
		assertFalse(tank.isFluidValid(0, FluidStack.EMPTY));
	}

	@Test
	void fillOnlyTank() {
		assertEquals(1, tank.getTanks());
		assertEquals(1000, tank.getTankCapacity(0));
		assertTrue(tank.getFluidInTank(0).isEmpty());
		engine.burnTime = 5000;
		assertTrue(tank.drain(1000, FluidAction.EXECUTE).isEmpty());
		assertTrue(tank.drain(lava(1000), FluidAction.EXECUTE).isEmpty());
		assertEquals(5000, engine.burnTime);
	}

	// ---- Filling

	@Test
	void simulateChangesNothing() {
		assertEquals(1000, tank.fill(lava(1000), FluidAction.SIMULATE));
		assertEquals(0, engine.burnTime);
	}

	@Test
	void anEmptyEngineTakesOneBucket() {
		assertEquals(1000, tank.fill(lava(1000), FluidAction.EXECUTE));
		assertEquals(LAVA, engine.burnTime);
	}

	@Test
	void neverTakesMoreThanOneBucketsWorth() {
		assertEquals(1000, tank.fill(lava(5000), FluidAction.SIMULATE));
		assertEquals(1000, tank.fill(lava(5000), FluidAction.EXECUTE));
		assertEquals(LAVA, engine.burnTime);
		assertEquals(0, tank.fill(lava(5000), FluidAction.EXECUTE));
		assertEquals(LAVA, engine.burnTime);
	}

	@Test
	void smallFillsAddProportionalBurnTime() {
		assertEquals(50, tank.fill(lava(50), FluidAction.EXECUTE));
		assertEquals(50 * LAVA / 1000, engine.burnTime);
		assertEquals(50, tank.fill(lava(50), FluidAction.EXECUTE));
		assertEquals(100 * LAVA / 1000, engine.burnTime);
	}

	@Test
	void aPartlyBurningEngineIsToppedUpToOneBucket() {
		engine.burnTime = 15000;
		// 5000 ticks of room is a quarter of a bucket
		assertEquals(250, tank.fill(lava(1000), FluidAction.SIMULATE));
		assertEquals(250, tank.fill(lava(1000), FluidAction.EXECUTE));
		assertEquals(LAVA, engine.burnTime);
	}

	@Test
	void aNearlyFullEngineStillTakesTheLastDrop() {
		engine.burnTime = LAVA - 10;
		// Half a millibucket's worth of room rounds up to one
		assertEquals(1, tank.fill(lava(1000), FluidAction.EXECUTE));
		assertEquals(LAVA - 10 + LAVA / 1000, engine.burnTime);
	}

	@Test
	void aFullOrOverfullEngineTakesNothing() {
		engine.burnTime = LAVA;
		assertEquals(0, tank.fill(lava(1000), FluidAction.EXECUTE));
		engine.burnTime = LAVA * 3;
		assertEquals(0, tank.fill(lava(1000), FluidAction.EXECUTE));
		assertEquals(LAVA * 3, engine.burnTime);
	}

	@Test
	void simulateAndExecuteAgree() {
		for (int burnTime : new int[] {0, 1, 999, 7777, 19_999, LAVA})
			for (int amount : new int[] {1, 7, 250, 1000, 4000}) {
				engine.burnTime = burnTime;
				int simulated = tank.fill(lava(amount), FluidAction.SIMULATE);
				assertEquals(burnTime, engine.burnTime, "simulate changed the engine");
				int filled = tank.fill(lava(amount), FluidAction.EXECUTE);
				assertEquals(simulated, filled, "burnTime " + burnTime + ", amount " + amount);
				assertTrue(filled <= amount);
				// Every millibucket taken is burnt: nothing vanishes beyond rounding of a fraction of a tick
				assertEquals(burnTime + (long) filled * LAVA / 1000, engine.burnTime);
			}
	}

	@Test
	void nonFuelIsRefused() {
		assertEquals(0, tank.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE));
		assertEquals(0, tank.fill(FluidStack.EMPTY, FluidAction.EXECUTE));
		assertEquals(0, engine.burnTime);
	}

	@Test
	void creativeInfiniteFuelIsLeftAlone() {
		engine.infinite = true;
		assertEquals(0, tank.fill(lava(1000), FluidAction.EXECUTE));
		assertEquals(0, engine.burnTime);
	}

	@Test
	void doesNotMixIntoASuperheatedBurn() {
		engine.burnTime = 100;
		engine.superHeated = true;
		assertEquals(0, tank.fill(lava(1000), FluidAction.EXECUTE));
		assertEquals(100, engine.burnTime);
		assertTrue(engine.superHeated);
	}

	@Test
	void aBurntOutSuperheatedEngineGoesBackToNormalFuel() {
		engine.superHeated = true;
		assertEquals(1000, tank.fill(lava(1000), FluidAction.SIMULATE));
		assertTrue(engine.superHeated, "simulate changed the engine");
		assertEquals(1000, tank.fill(lava(1000), FluidAction.EXECUTE));
		assertFalse(engine.superHeated);
		assertEquals(LAVA, engine.burnTime);
	}

	@Test
	void somethingThatIsntAnEngineTakesNothing() {
		// The handles are bound to the engine class; anything else fails the call, which is caught
		EngineFuelTank onFurnace = new EngineFuelTank(new FurnaceBlockEntity(BlockPos.ZERO, Blocks.FURNACE.defaultBlockState()));
		assertEquals(0, onFurnace.fill(lava(1000), FluidAction.EXECUTE));
	}
}
