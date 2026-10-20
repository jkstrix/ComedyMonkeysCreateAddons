package com.comedymonkey.createaddons.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.pen.BottledAnimalItem;
import com.comedymonkey.createaddons.testutil.ModResourceFiles;
import com.google.gson.JsonObject;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

class ModItemsTest {
	@Test
	void everyItemIsRegisteredUnderTheMod() {
		for (DeferredHolder<Item, ? extends Item> entry : ModItems.ITEMS.getEntries()) {
			assertEquals(ComedyMonkeysCreateAddons.MOD_ID, entry.getId().getNamespace());
			assertSame(entry.get(), BuiltInRegistries.ITEM.get(entry.getId()));
		}
	}

	@Test
	void captureBottlesStackButFilledOnesDont() {
		assertEquals(64, new ItemStack(ModItems.CAPTURE_BOTTLE.get()).getMaxStackSize());
		assertEquals(1, new ItemStack(ModItems.BOTTLED_ANIMAL.get()).getMaxStackSize());
		assertInstanceOf(BottledAnimalItem.class, ModItems.BOTTLED_ANIMAL.get());
	}

	@Test
	void penItemsPlaceTheirPens() {
		assertSame(ModBlocks.ANIMAL_PEN.get(), assertInstanceOf(BlockItem.class, ModItems.ANIMAL_PEN.get()).getBlock());
		assertSame(ModBlocks.BRASS_ANIMAL_PEN.get(), assertInstanceOf(BlockItem.class, ModItems.BRASS_ANIMAL_PEN.get()).getBlock());
	}

	@Test
	void netheriteThingsSurviveLava() {
		assertTrue(new ItemStack(ModItems.NETHERITE_DUST.get()).has(net.minecraft.core.component.DataComponents.FIRE_RESISTANT));
		assertTrue(new ItemStack(ModItems.NETHERITE_CHEST_UPGRADE.get()).has(net.minecraft.core.component.DataComponents.FIRE_RESISTANT));
	}

	/** Every item has an English name (missing ones show as raw keys). */
	@Test
	void everyItemHasAName() {
		Path file = ModResourceFiles.find("assets", ComedyMonkeysCreateAddons.MOD_ID, "lang", "en_us.json");
		Assumptions.assumeTrue(file != null, "lang file not reachable");
		JsonObject lang = ModResourceFiles.readJson(file).getAsJsonObject();
		for (DeferredHolder<Item, ? extends Item> entry : ModItems.ITEMS.getEntries()) {
			String key = entry.get().getDescriptionId();
			assertTrue(lang.has(key), "no name for " + key);
		}
	}
}
