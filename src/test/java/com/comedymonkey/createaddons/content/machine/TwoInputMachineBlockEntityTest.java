package com.comedymonkey.createaddons.content.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.testutil.ModRecipeFiles;
import com.comedymonkey.createaddons.testutil.ReflectionAccess;
import com.comedymonkey.createaddons.testutil.TestConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * A String Tangler or Stone Infuser with no level: what automation may take, the comparator, progress and saving.
 * Which slot an item goes in depends on the level's recipes, so it isn't covered here.
 */
class TwoInputMachineBlockEntityTest {
	private static final int MAIN = 0;
	private static final int EXTRA = 1;
	private static final int OUTPUT = 2;

	private TwoInputMachineBlockEntity be;

	@BeforeEach
	void setUp() {
		TestConfig.loadDefaults();
		be = machine("stone_infuser", MachineKind.INFUSING);
	}

	@AfterEach
	void restore() {
		TestConfig.reset();
	}

	private static TwoInputMachineBlockEntity machine(String id, MachineKind kind) {
		ResourceLocation key = ResourceLocation.fromNamespaceAndPath("comedymonkeysaddons", id);
		BlockState state = BuiltInRegistries.BLOCK.get(key).defaultBlockState();
		BlockEntityType<?> type = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(key);
		return new TwoInputMachineBlockEntity(type, BlockPos.ZERO, state, kind);
	}

	private static ItemStack stack(TwoInputMachineBlockEntity be, int slot) {
		return be.getStack(slot);
	}

	private static IItemHandler automation(TwoInputMachineBlockEntity be) {
		return be.getAutomationHandler();
	}

	private static void ponder(TwoInputMachineBlockEntity be, ItemStack main, ItemStack extra, ItemStack output,
		ItemStack making, int time) {
		be.setPonderState(main, extra, output, making, time);
	}

	private float progress(float partialTicks) {
		return be.getProgress(partialTicks);
	}

	@Test
	void blockEntityTypesAreRegistered() {
		assertSame(TwoInputMachineBlockEntity.class, machine("string_tangler", MachineKind.TANGLING).getClass());
		assertEquals(MachineKind.TANGLING, machine("string_tangler", MachineKind.TANGLING).getKind());
		assertEquals(MachineKind.INFUSING, be.getKind());
	}

	@Test
	void automationTakesOnlyTheResult() {
		ponder(be, new ItemStack(Items.COBBLESTONE, 5), new ItemStack(Items.IRON_NUGGET, 9), new ItemStack(Items.STONE, 3),
			ItemStack.EMPTY, 0);
		IItemHandler automation = automation(be);
		assertEquals(3, automation.getSlots());
		assertTrue(automation.extractItem(MAIN, 64, false).isEmpty());
		assertTrue(automation.extractItem(EXTRA, 64, false).isEmpty());
		assertEquals(5, stack(be, MAIN).getCount());
		assertEquals(9, stack(be, EXTRA).getCount());

		ItemStack out = automation.extractItem(OUTPUT, 2, false);
		assertSame(Items.STONE, out.getItem());
		assertEquals(2, out.getCount());
		assertEquals(1, stack(be, OUTPUT).getCount());
	}

	@Test
	void nothingGoesIntoTheOutput() {
		ItemStack stone = new ItemStack(Items.STONE, 4);
		assertSame(stone, automation(be).insertItem(OUTPUT, stone, false));
		assertFalse(automation(be).isItemValid(OUTPUT, stone));
		assertTrue(stack(be, OUTPUT).isEmpty());
	}

	@Test
	void noRecipeUntilBothSlotsHoldSomething() {
		assertNull(be.getRecipe());
		ponder(be, new ItemStack(Items.COBBLESTONE), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, 0);
		assertNull(be.getRecipe());
	}

	@Test
	void comparatorFollowsTheOutput() {
		assertEquals(0, be.getComparatorOutput());
		ponder(be, new ItemStack(Items.COBBLESTONE, 64), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, 0);
		assertEquals(0, be.getComparatorOutput(), "inputs don't count");
		ponder(be, ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.COBWEB, 32), ItemStack.EMPTY, 0);
		assertEquals(8, be.getComparatorOutput());
		ponder(be, ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.COBWEB, 64), ItemStack.EMPTY, 0);
		assertEquals(15, be.getComparatorOutput());
	}

	@Test
	void speedScalesLikeAMillstone() {
		assertEquals(1f, be.processingSpeed(), 1e-6);
		ReflectionAccess.set(be, "speed", 128f);
		assertEquals(8f, be.processingSpeed(), 1e-6);
		ReflectionAccess.set(be, "speed", -256f);
		assertEquals(16f, be.processingSpeed(), 1e-6);
	}

	@Test
	void progressAndWhatIsBeingMade() {
		ItemStack making = new ItemStack(Items.COBWEB);
		ponder(be, new ItemStack(Items.STRING, 4), new ItemStack(Items.SLIME_BALL), ItemStack.EMPTY, making, 160);
		assertTrue(be.isProcessing());
		assertSame(making, be.getMaking());
		assertEquals(0f, progress(0), 1e-6);
		ReflectionAccess.set(be, "timer", 40f);
		assertEquals(0.75f, progress(0), 1e-6);
		ReflectionAccess.set(be, "speed", 64f);
		// Half a tick at 4x is two of the 160 ticks
		assertEquals(0.7625f, progress(0.5f), 1e-6);

		ponder(be, new ItemStack(Items.STRING, 4), new ItemStack(Items.SLIME_BALL), ItemStack.EMPTY, ItemStack.EMPTY, 160);
		assertFalse(be.isProcessing());
		assertEquals(0f, progress(0), 1e-6);
	}

	@Test
	void clearContentEmptiesEverySlot() {
		ponder(be, new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.GOLD_NUGGET, 18), new ItemStack(Items.STONE, 2),
			new ItemStack(Items.STONE), 200);
		be.clearContent();
		for (int slot : new int[] {MAIN, EXTRA, OUTPUT})
			assertTrue(stack(be, slot).isEmpty(), "slot " + slot);
		assertFalse(be.isProcessing());
	}

	@Test
	void saveAndLoad() {
		ponder(be, new ItemStack(Items.COBBLESTONE, 7), new ItemStack(Items.IRON_NUGGET, 27), new ItemStack(Items.ANDESITE, 3),
			new ItemStack(Items.DIORITE), 200);
		ReflectionAccess.set(be, "timer", 55.25f);

		CompoundTag tag = be.saveWithFullMetadata(ModRecipeFiles.REGISTRIES);
		TwoInputMachineBlockEntity loaded = machine("stone_infuser", MachineKind.INFUSING);
		loaded.loadWithComponents(tag, ModRecipeFiles.REGISTRIES);

		assertTrue(ItemStack.matches(new ItemStack(Items.COBBLESTONE, 7), stack(loaded, MAIN)));
		assertTrue(ItemStack.matches(new ItemStack(Items.IRON_NUGGET, 27), stack(loaded, EXTRA)));
		assertTrue(ItemStack.matches(new ItemStack(Items.ANDESITE, 3), stack(loaded, OUTPUT)));
		assertTrue(ItemStack.matches(new ItemStack(Items.DIORITE), loaded.getMaking()));
		assertEquals(55.25f, (Float) ReflectionAccess.get(loaded, "timer"), 1e-6);
		assertEquals(200, ReflectionAccess.get(loaded, "maxTime"));
		assertTrue(loaded.isProcessing());
	}

	@Test
	void nothingBeingMadeIsNotSaved() {
		CompoundTag tag = be.saveWithFullMetadata(ModRecipeFiles.REGISTRIES);
		assertFalse(tag.contains("Making"));
		TwoInputMachineBlockEntity loaded = machine("stone_infuser", MachineKind.INFUSING);
		loaded.loadWithComponents(tag, ModRecipeFiles.REGISTRIES);
		assertTrue(loaded.getMaking().isEmpty());
	}
}
