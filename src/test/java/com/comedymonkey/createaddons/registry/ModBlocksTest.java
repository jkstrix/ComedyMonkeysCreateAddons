package com.comedymonkey.createaddons.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.cobblegen.CobbleGeneratorBlock;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;

class ModBlocksTest {
	@Test
	void everyBlockIsRegisteredUnderTheMod() {
		assertFalse(ModBlocks.BLOCKS.getEntries().isEmpty());
		for (DeferredHolder<Block, ? extends Block> entry : ModBlocks.BLOCKS.getEntries()) {
			assertEquals(ComedyMonkeysCreateAddons.MOD_ID, entry.getId().getNamespace());
			assertSame(entry.get(), BuiltInRegistries.BLOCK.get(entry.getId()));
		}
	}

	/**
	 * Every block but the cover's blank look and the Cuckoo Clock's weights has an item of its own name (Carry On and
	 * pick-block need one). The weights come and go with the clock: pick-block on them gives the clock, and Carry On
	 * is kept off them by its blacklist tag.
	 */
	@Test
	void everyBlockHasItsItem() {
		for (DeferredHolder<Block, ? extends Block> entry : ModBlocks.BLOCKS.getEntries()) {
			Item item = entry.get().asItem();
			if (entry.getId().equals(ModBlocks.PIPE_COVER_BLANK.getId())) {
				assertSame(Items.AIR, item, "the blank cover is not an item");
				continue;
			}
			if (entry.getId().equals(ModBlocks.CUCKOO_CLOCK_WEIGHTS.getId())) {
				assertSame(Items.AIR, item, "the clock's weights are not an item");
				continue;
			}
			if (entry.getId().equals(ModBlocks.HANGING_CABLE.getId())) {
				assertSame(Items.AIR, item, "a hanging lamp's cable is made by lowering it, not an item");
				continue;
			}
			assertTrue(item instanceof BlockItem, entry.getId() + " has no block item");
			assertSame(entry.get(), ((BlockItem) item).getBlock());
			assertEquals(entry.getId(), BuiltInRegistries.ITEM.getKey(item));
		}
	}

	/** The generators' block entity type is built from this list, so a generator missing here can't be placed. */
	@Test
	void generatorsListsEveryGenerator() {
		Set<Block> listed = new HashSet<>();
		for (DeferredBlock<CobbleGeneratorBlock> generator : ModBlocks.generators())
			assertTrue(listed.add(generator.get()), generator.getId() + " listed twice");
		for (DeferredHolder<Block, ? extends Block> entry : ModBlocks.BLOCKS.getEntries())
			if (entry.get() instanceof CobbleGeneratorBlock)
				assertTrue(listed.contains(entry.get()), entry.getId() + " missing from generators()");
	}

	@Test
	void penTiers() {
		assertFalse(ModBlocks.ANIMAL_PEN.get().isBrass());
		assertTrue(ModBlocks.BRASS_ANIMAL_PEN.get().isBrass());
		// The Brass pen is built like the Brass generator, but its window doesn't glow
		assertEquals(0, ModBlocks.BRASS_ANIMAL_PEN.get().defaultBlockState().getLightEmission());
	}
}
