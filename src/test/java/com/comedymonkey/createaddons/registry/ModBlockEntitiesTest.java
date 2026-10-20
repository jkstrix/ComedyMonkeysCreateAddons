package com.comedymonkey.createaddons.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * A block entity type that doesn't list its block makes placing that block throw ("Invalid block entity state"), so
 * every block is checked against the type it makes.
 */
class ModBlockEntitiesTest {
	@Test
	void everyTypeIsRegisteredUnderTheMod() {
		for (DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<?>> entry : ModBlockEntities.BLOCK_ENTITY_TYPES.getEntries()) {
			assertEquals(ComedyMonkeysCreateAddons.MOD_ID, entry.getId().getNamespace());
			assertEquals(entry.getId(), BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entry.get()));
		}
	}

	@Test
	void createStyleBlocksAreAcceptedByTheirType() {
		int checked = 0;
		for (DeferredHolder<Block, ? extends Block> entry : ModBlocks.BLOCKS.getEntries())
			if (entry.get() instanceof IBE<?> ibe) {
				assertTrue(ibe.getBlockEntityType().isValid(entry.get().defaultBlockState()),
					entry.getId() + " is not a valid block for " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(ibe.getBlockEntityType()));
				checked++;
			}
		assertTrue(checked > 10, "only " + checked + " IBE blocks found");
	}

	@Test
	void otherBlocksAreAcceptedByTheirType() {
		Map<Block, BlockEntityType<?>> pairs = Map.of(
			ModBlocks.WELLSPRING.get(), ModBlockEntities.WELLSPRING.get(),
			ModBlocks.UPGRADED_CHEST.get(), ModBlockEntities.UPGRADED_CHEST.get(),
			ModBlocks.UPGRADED_BARREL.get(), ModBlockEntities.UPGRADED_BARREL.get(),
			ModBlocks.INCINERATOR.get(), ModBlockEntities.INCINERATOR.get(),
			ModBlocks.GEOTHERMAL_CASING.get(), ModBlockEntities.GEOTHERMAL_CASING.get());
		pairs.forEach((block, type) -> assertTrue(type.isValid(block.defaultBlockState()),
			BuiltInRegistries.BLOCK.getKey(block) + " is not valid for its type"));
	}

	@Test
	void bothPensAndBothEngines() {
		assertTrue(ModBlockEntities.ANIMAL_PEN.get().isValid(ModBlocks.ANIMAL_PEN.get().defaultBlockState()));
		assertTrue(ModBlockEntities.ANIMAL_PEN.get().isValid(ModBlocks.BRASS_ANIMAL_PEN.get().defaultBlockState()));
		assertTrue(ModBlockEntities.MAGMA_ENGINE.get().isValid(ModBlocks.MAGMA_ENGINE.get().defaultBlockState()));
		assertTrue(ModBlockEntities.MAGMA_ENGINE.get().isValid(ModBlocks.SUPERCHARGED_MAGMA_ENGINE.get().defaultBlockState()));
	}
}
