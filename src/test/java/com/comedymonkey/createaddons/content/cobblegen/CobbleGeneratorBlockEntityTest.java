package com.comedymonkey.createaddons.content.cobblegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.testutil.ReflectionAccess;
import com.comedymonkey.createaddons.testutil.TestConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * A Cobblestone Generator with no level: its output buffer as automation sees it, the comparator and the saved
 * state.
 */
class CobbleGeneratorBlockEntityTest {
	private static final HolderLookup.Provider REGISTRIES = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

	@BeforeEach
	void config() {
		TestConfig.loadDefaults();
	}

	@AfterEach
	void restore() {
		TestConfig.reset();
	}

	private static CobbleGeneratorBlockEntity generator(String id) {
		BlockState state = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("comedymonkeysaddons", id))
			.defaultBlockState();
		return new CobbleGeneratorBlockEntity(BlockPos.ZERO, state);
	}

	private static void setStored(CobbleGeneratorBlockEntity be, int stored, boolean powered) {
		be.setPonderState(stored, powered);
	}

	private static int stored(CobbleGeneratorBlockEntity be) {
		return (Integer) ReflectionAccess.get(be, "stored");
	}

	private static IItemHandler output(CobbleGeneratorBlockEntity be) {
		return be.getOutputHandler();
	}

	@Test
	void productComesFromTheBlock() {
		assertEquals(GeneratorProduct.COBBLESTONE, generator("andesite_cobble_generator").product());
		assertEquals(GeneratorProduct.BASALT, generator("brass_basalt_generator").product());
		assertEquals(GeneratorProduct.BLACKSTONE, generator("andesite_blackstone_generator").product());
		assertEquals(GeneratorProduct.TUFF, generator("tuff_generator").product());
	}

	@Test
	void outputShowsAtMostAStack() {
		CobbleGeneratorBlockEntity be = generator("brass_cobble_generator");
		IItemHandler output = output(be);
		assertEquals(1, output.getSlots());
		assertTrue(output.getStackInSlot(0).isEmpty());

		setStored(be, 100, false);
		ItemStack shown = output.getStackInSlot(0);
		assertSame(Items.COBBLESTONE, shown.getItem());
		assertEquals(64, shown.getCount());
		assertEquals(64, output.getSlotLimit(0));
	}

	@Test
	void extractingTakesFromTheBuffer() {
		CobbleGeneratorBlockEntity be = generator("brass_cobble_generator");
		setStored(be, 100, false);
		IItemHandler output = output(be);

		ItemStack simulated = output.extractItem(0, 64, true);
		assertEquals(64, simulated.getCount());
		assertEquals(100, stored(be), "simulating takes nothing");

		assertEquals(64, output.extractItem(0, 64, false).getCount());
		assertEquals(36, stored(be));
		assertEquals(36, output.getStackInSlot(0).getCount());

		assertEquals(36, output.extractItem(0, 1000, false).getCount());
		assertEquals(0, stored(be));
		assertTrue(output.extractItem(0, 64, false).isEmpty());
	}

	@Test
	void extractHandsOutTheProduct() {
		CobbleGeneratorBlockEntity be = generator("andesite_basalt_generator");
		setStored(be, 5, false);
		ItemStack out = output(be).extractItem(0, 2, false);
		assertSame(Items.BASALT, out.getItem());
		assertEquals(2, out.getCount());
		assertEquals(3, stored(be));
	}

	@Test
	void nothingGoesIn() {
		CobbleGeneratorBlockEntity be = generator("andesite_cobble_generator");
		IItemHandler output = output(be);
		ItemStack cobble = new ItemStack(Items.COBBLESTONE, 10);
		assertSame(cobble, output.insertItem(0, cobble, false));
		assertEquals(10, cobble.getCount());
		assertFalse(output.isItemValid(0, cobble));
		assertEquals(0, stored(be));
	}

	@Test
	void idleWhenFullOrPowered() {
		CobbleGeneratorBlockEntity be = generator("andesite_cobble_generator");
		assertFalse(be.isIdle());
		setStored(be, 63, false);
		assertFalse(be.isFull());
		assertFalse(be.isIdle());
		setStored(be, 64, false);
		assertTrue(be.isFull());
		assertTrue(be.isIdle());
		setStored(be, 0, true);
		assertTrue(be.isIdle());
		assertTrue(be.isPowered());
		assertFalse(be.isRunning(), "not turning, so not running");
	}

	@Test
	void brassHoldsFourStacks() {
		CobbleGeneratorBlockEntity be = generator("brass_cobble_generator");
		setStored(be, 255, false);
		assertFalse(be.isFull());
		setStored(be, 256, false);
		assertTrue(be.isFull());
	}

	@Test
	void noStressWhileIdle() {
		CobbleGeneratorBlockEntity be = generator("andesite_cobble_generator");
		setStored(be, 64, false);
		assertEquals(0f, be.calculateStressApplied(), 1e-6);
		setStored(be, 0, true);
		assertEquals(0f, be.calculateStressApplied(), 1e-6);
	}

	@Test
	void comparatorFollowsTheBuffer() {
		CobbleGeneratorBlockEntity be = generator("andesite_cobble_generator");
		int[][] cases = {{0, 0}, {1, 1}, {32, 8}, {63, 14}, {64, 15}};
		for (int[] c : cases) {
			setStored(be, c[0], false);
			assertEquals(c[1], be.getComparatorOutput(), c[0] + " stored");
		}
	}

	@Test
	void formingFractionIsWholeWhenFull() {
		CobbleGeneratorBlockEntity be = generator("andesite_cobble_generator");
		assertEquals(0f, be.getFormingFraction(), 1e-6);
		setStored(be, 64, false);
		assertEquals(1f, be.getFormingFraction(), 1e-6);
	}

	@Test
	void clearContentEmptiesTheBuffer() {
		CobbleGeneratorBlockEntity be = generator("brass_cobble_generator");
		setStored(be, 200, false);
		be.clearContent();
		assertEquals(0, stored(be));
		assertTrue(output(be).getStackInSlot(0).isEmpty());
	}

	// ---------------------------------------------------------------- drill pacing

	/**
	 * Ticks the drill logic until a block is finished, and returns how many ticks that took. Finishing a block plays
	 * a sound, which needs a level: with none, that is a NullPointerException, right after the block was counted.
	 */
	private static int ticksToFinishOne(CobbleGeneratorBlockEntity be) {
		for (int tick = 1; tick <= 100_000; tick++) {
			try {
				ReflectionAccess.call(be, "work");
			} catch (NullPointerException finished) {
				return tick;
			}
		}
		throw new AssertionError("never finished a block");
	}

	private static CobbleGeneratorBlockEntity turning(String id, float rpm) {
		CobbleGeneratorBlockEntity be = generator(id);
		ReflectionAccess.set(be, "speed", rpm);
		return be;
	}

	@Test
	void drillPaceAtFullSpeed() {
		// 10 ticks per cobblestone at 256 RPM, as a Mechanical Drill mines it; basalt is softer
		assertEquals(10, ticksToFinishOne(turning("andesite_cobble_generator", 256)));
		assertEquals(5, ticksToFinishOne(turning("andesite_basalt_generator", 256)));
		assertEquals(10, ticksToFinishOne(turning("tuff_generator", -256)), "either direction");
	}

	@Test
	void eachFinishedCycleAddsPerCycle() {
		CobbleGeneratorBlockEntity andesite = turning("andesite_cobble_generator", 256);
		ticksToFinishOne(andesite);
		assertEquals(1, stored(andesite));
		CobbleGeneratorBlockEntity brass = turning("brass_cobble_generator", 256);
		ticksToFinishOne(brass);
		ticksToFinishOne(brass);
		assertEquals(32, stored(brass));
	}

	@Test
	void bufferNeverOverfills() {
		CobbleGeneratorBlockEntity brass = turning("brass_cobble_generator", 256);
		setStored(brass, 250, false);
		ticksToFinishOne(brass);
		assertEquals(256, stored(brass));
	}

	@Test
	void slowerShaftsTakeLonger() {
		int fast = ticksToFinishOne(turning("andesite_cobble_generator", 256));
		int slow = ticksToFinishOne(turning("andesite_cobble_generator", 32));
		assertTrue(slow > fast * 4, slow + " vs " + fast);
	}

	@Test
	void gogglesCycleTimeMatchesAtFullSpeed() {
		CobbleGeneratorBlockEntity be = turning("andesite_cobble_generator", 256);
		assertEquals(ticksToFinishOne(turning("andesite_cobble_generator", 256)), ReflectionAccess.call(be, "ticksPerCycle"));
	}

	@Test
	void gogglesCycleTimeMatchesTheRealPace() {
		for (float rpm : new float[] {16, 32, 64, 128, 200}) {
			CobbleGeneratorBlockEntity be = turning("andesite_cobble_generator", rpm);
			int real = ticksToFinishOne(turning("andesite_cobble_generator", rpm));
			assertEquals(real, ReflectionAccess.call(be, "ticksPerCycle"), rpm + " RPM");
		}
	}

	@Test
	void stoppedShaftShowsNoCycle() {
		assertEquals(0, ReflectionAccess.call(generator("andesite_cobble_generator"), "ticksPerCycle"));
	}

	@Test
	void saveAndLoad() {
		CobbleGeneratorBlockEntity be = generator("brass_blackstone_generator");
		setStored(be, 123, true);
		ReflectionAccess.set(be, "destroyProgress", 6);

		CompoundTag tag = be.saveWithFullMetadata(REGISTRIES);
		CobbleGeneratorBlockEntity loaded = generator("brass_blackstone_generator");
		loaded.loadWithComponents(tag, REGISTRIES);
		assertEquals(123, stored(loaded));
		assertEquals(6, ReflectionAccess.get(loaded, "destroyProgress"));
		assertTrue(loaded.isPowered());
	}
}
