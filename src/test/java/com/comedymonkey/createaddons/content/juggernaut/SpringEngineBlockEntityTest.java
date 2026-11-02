package com.comedymonkey.createaddons.content.juggernaut;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModDataComponents;
import com.comedymonkey.createaddons.registry.ModItems;
import com.comedymonkey.createaddons.testutil.TestConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * JUG-02: a Spring Engine holding more than its capacity (wound full, then the capacity lowered in the config) reads as
 * full, never over: its fill, comparator, tooltip and the frames the renderer picks all stay in range.
 */
class SpringEngineBlockEntityTest {
	private static HolderLookup.Provider registries;

	@BeforeAll
	static void lookup() {
		registries = VanillaRegistries.createLookup();
	}

	@BeforeEach
	void config() {
		TestConfig.loadDefaults();
	}

	@AfterEach
	void restore() {
		TestConfig.reset();
	}

	static SpringEngineBlockEntity engine(SpringEngineBlock.Part part) {
		BlockState state = ModBlocks.SPRING_ENGINE.get().defaultBlockState().setValue(SpringEngineBlock.PART, part);
		return new SpringEngineBlockEntity(BlockPos.ZERO, state);
	}

	private static void halveCapacity() {
		TestConfig.set(CMConfig.SERVER.springEngineCapacity, CMConfig.SERVER.springEngineCapacity.getDefault() / 2);
	}

	/** Wound full, then the capacity halved: still full, not 200%. */
	@Test
	void overCapacityReadsAsFull() {
		SpringEngineBlockEntity engine = engine(SpringEngineBlock.Part.OUTPUT);
		engine.setStored(SpringEngineBlockEntity.capacity());
		halveCapacity();
		assertEquals(1f, engine.fill(), 1e-6);
		assertEquals(15, engine.comparatorLevel());
	}

	/** Saved full, loaded with half the capacity: it holds exactly the new full, and shows full. */
	@Test
	void overCapacityIsClampedOnLoad() {
		CompoundTag tag = new CompoundTag();
		tag.putLong("Stored", SpringEngineBlockEntity.capacity());
		tag.putFloat("Fill", 2f);
		halveCapacity();
		SpringEngineBlockEntity output = engine(SpringEngineBlock.Part.OUTPUT);
		output.loadWithComponents(tag, registries);
		assertEquals(SpringEngineBlockEntity.capacity(), output.stored());
		assertEquals(1f, output.fill(), 1e-6);
		assertEquals(1f, output.shownFill(), 1e-6);
		assertEquals(15, output.comparatorLevel());
		// the winding end only knows what it was sent
		SpringEngineBlockEntity winding = engine(SpringEngineBlock.Part.WINDING);
		winding.loadWithComponents(tag, registries);
		assertEquals(1f, winding.fill(), 1e-6);
		assertEquals(1f, winding.shownFill(), 1e-6);
	}

	/** The item's tooltip and bar: full, not 200%. */
	@Test
	void anOverfullItemReadsAsFull() {
		ItemStack stack = new ItemStack(ModItems.SPRING_ENGINE.get());
		stack.set(ModDataComponents.SPRING_WIND.get(), SpringEngineBlockEntity.capacity());
		halveCapacity();
		assertEquals(1f, SpringEngineItem.fill(stack), 1e-6);
		assertEquals(13, ModItems.SPRING_ENGINE.get().getBarWidth(stack));
	}

	/** Ponder's showFill can't push it past full either. */
	@Test
	void showFillStaysInRange() {
		SpringEngineBlockEntity winding = engine(SpringEngineBlock.Part.WINDING);
		winding.showFill(3f);
		assertEquals(1f, winding.shownFill(), 1e-6);
		winding.showFill(-1f);
		assertEquals(0f, winding.shownFill(), 1e-6);
	}
}
