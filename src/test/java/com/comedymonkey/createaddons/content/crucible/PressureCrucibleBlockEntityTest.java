package com.comedymonkey.createaddons.content.crucible;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.testutil.ModRecipeFiles;
import com.comedymonkey.createaddons.testutil.ModResourceFiles;
import com.comedymonkey.createaddons.testutil.ReflectionAccess;
import com.comedymonkey.createaddons.testutil.TestConfig;
import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A Pressure Crucible with no level: what automation may take, the comparator, press speed and progress, and saving.
 */
class PressureCrucibleBlockEntityTest {
	private static final int INPUT = 0;
	private static final int OUTPUT = 1;

	private PressureCrucibleBlockEntity be;

	@BeforeEach
	void setUp() {
		TestConfig.loadDefaults();
		be = crucible();
	}

	@AfterEach
	void restore() {
		TestConfig.reset();
	}

	private static PressureCrucibleBlockEntity crucible() {
		BlockState state = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("comedymonkeysaddons", "pressure_crucible"))
			.defaultBlockState();
		return new PressureCrucibleBlockEntity(BlockPos.ZERO, state);
	}

	private static ItemStackHandler inventory(PressureCrucibleBlockEntity be) {
		return be.getInventory();
	}

	private static IItemHandler automation(PressureCrucibleBlockEntity be) {
		return be.getAutomationHandler();
	}

	private void setSpeed(float speed) {
		ReflectionAccess.set(be, "speed", speed);
	}

	@Test
	void slotConstants() {
		assertEquals(INPUT, PressureCrucibleBlockEntity.INPUT);
		assertEquals(OUTPUT, PressureCrucibleBlockEntity.OUTPUT);
		assertEquals(2, inventory(be).getSlots());
	}

	@Test
	void automationTakesOnlyResults() {
		inventory(be).setStackInSlot(INPUT, new ItemStack(Items.COAL, 20));
		inventory(be).setStackInSlot(OUTPUT, new ItemStack(Items.DIAMOND, 3));
		IItemHandler automation = automation(be);

		assertTrue(automation.extractItem(INPUT, 64, false).isEmpty(), "the dust stays in");
		assertEquals(20, inventory(be).getStackInSlot(INPUT).getCount());

		ItemStack out = automation.extractItem(OUTPUT, 64, false);
		assertSame(Items.DIAMOND, out.getItem());
		assertEquals(3, out.getCount());
		assertTrue(inventory(be).getStackInSlot(OUTPUT).isEmpty());
	}

	@Test
	void nothingGoesIntoTheOutput() {
		ItemStack diamonds = new ItemStack(Items.DIAMOND, 5);
		assertEquals(5, automation(be).insertItem(OUTPUT, diamonds, false).getCount());
		assertFalse(automation(be).isItemValid(OUTPUT, diamonds));
		assertTrue(inventory(be).getStackInSlot(OUTPUT).isEmpty());
	}

	@Test
	void noRecipeWithoutContents() {
		assertNull(be.getRecipe());
		assertEquals("EMPTY", be.getStatus(null).name());
	}

	@Test
	void comparatorFollowsTheInput() {
		assertEquals(0, be.getComparatorOutput());
		inventory(be).setStackInSlot(INPUT, new ItemStack(Items.COAL, 32));
		assertEquals(8, be.getComparatorOutput());
		inventory(be).setStackInSlot(INPUT, new ItemStack(Items.COAL, 64));
		assertEquals(15, be.getComparatorOutput());
	}

	@Test
	void pressSpeedScalesLikeAMillstone() {
		assertEquals(1f, be.processingSpeed(), 1e-6);
		setSpeed(16);
		assertEquals(1f, be.processingSpeed(), 1e-6);
		setSpeed(8);
		assertEquals(1f, be.processingSpeed(), 1e-6, "never slower than 1x");
		setSpeed(256);
		assertEquals(16f, be.processingSpeed(), 1e-6);
		setSpeed(-64);
		assertEquals(4f, be.processingSpeed(), 1e-6, "either direction");
	}

	@Test
	void progressRunsFromZeroToOne() {
		be.setPonderState(new ItemStack(Items.SAND, 4), ItemStack.EMPTY, true, 200);
		assertTrue(be.isProcessing());
		assertEquals(0f, be.getProgress(), 1e-6);
		ReflectionAccess.set(be, "timer", 50f);
		assertEquals(0.75f, be.getProgress(), 1e-6);
		ReflectionAccess.set(be, "timer", -3f);
		assertEquals(1f, be.getProgress(), 1e-6, "clamped");

		be.setPonderState(new ItemStack(Items.SAND, 4), ItemStack.EMPTY, false, 200);
		assertEquals(0f, be.getProgress(), 1e-6, "no progress while idle");
	}

	@Test
	void progressLooksAheadWithinTheTickWhileTurning() {
		be.setPonderState(new ItemStack(Items.SAND, 4), ItemStack.EMPTY, true, 100);
		setSpeed(32);
		// Half a tick at 2x is one tick of work out of 100
		assertEquals(0.01f, be.getProgress(0.5f), 1e-6);
	}

	@Test
	void clearContentEmptiesBothSlots() {
		be.setPonderState(new ItemStack(Items.COAL, 64), new ItemStack(Items.DIAMOND), true, 3200);
		be.clearContent();
		assertTrue(inventory(be).getStackInSlot(INPUT).isEmpty());
		assertTrue(inventory(be).getStackInSlot(OUTPUT).isEmpty());
		assertFalse(be.isProcessing());
	}

	@Test
	void saveAndLoad() {
		be.setPonderState(new ItemStack(Items.QUARTZ, 12), new ItemStack(Items.AMETHYST_SHARD, 6), true, 800);
		ReflectionAccess.set(be, "timer", 123.5f);

		CompoundTag tag = be.saveWithFullMetadata(ModRecipeFiles.REGISTRIES);
		PressureCrucibleBlockEntity loaded = crucible();
		loaded.loadWithComponents(tag, ModRecipeFiles.REGISTRIES);

		assertTrue(ItemStack.matches(new ItemStack(Items.QUARTZ, 12), inventory(loaded).getStackInSlot(INPUT)));
		assertTrue(ItemStack.matches(new ItemStack(Items.AMETHYST_SHARD, 6), inventory(loaded).getStackInSlot(OUTPUT)));
		assertEquals(123.5f, (Float) ReflectionAccess.get(loaded, "timer"), 1e-6);
		assertEquals(800, ReflectionAccess.get(loaded, "maxTime"));
		assertTrue(loaded.isProcessing());
	}

	@Test
	void everyStatusShownIsTranslated() {
		Path lang = ModResourceFiles.find("assets", "comedymonkeysaddons", "lang", "en_us.json");
		assumeTrue(lang != null, "en_us.json not found");
		JsonObject json = ModResourceFiles.readJson(lang).getAsJsonObject();
		for (PressureCrucibleBlockEntity.Status status : PressureCrucibleBlockEntity.Status.values()) {
			String name = status.name();
			// EMPTY shows nothing and WORKING shows the progress instead
			if (name.equals("EMPTY") || name.equals("WORKING"))
				continue;
			String key = "comedymonkeysaddons.gui.goggles.crucible.status." + name.toLowerCase(Locale.ROOT);
			assertTrue(json.has(key), key);
		}
	}
}
