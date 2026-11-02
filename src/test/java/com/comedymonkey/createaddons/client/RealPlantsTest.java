package com.comedymonkey.createaddons.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.client.parts.PartSink;
import com.comedymonkey.createaddons.content.harvester.AutoHarvesterBlock;
import com.comedymonkey.createaddons.content.harvester.AutoHarvesterBlockEntity;
import com.comedymonkey.createaddons.content.harvester.PlantKind;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.testutil.ReflectionAccess;
import com.comedymonkey.createaddons.testutil.TestConfig;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * FLY-05: whether the Auto Harvester's real plant can be drawn is asked without drawing (so the generic plant can be a
 * part model, posed on Flywheel's worker threads), and the generic plant is one of its parts exactly when it can't.
 */
class RealPlantsTest {
	@BeforeAll
	static void setUp() {
		TestConfig.loadDefaults();
	}

	private static boolean can(ItemLike item, PlantKind kind, float growth) {
		return RealPlants.canRender(new ItemStack(item), kind, growth);
	}

	@Test
	void realPlantsCanBeDrawn() {
		for (float growth : new float[] {0, 0.5f, 1}) {
			assertTrue(can(Items.WHEAT_SEEDS, PlantKind.CROP, growth));
			assertTrue(can(Items.SUGAR_CANE, PlantKind.CROP, growth));
			assertTrue(can(Items.BAMBOO, PlantKind.CROP, growth));
			assertTrue(can(Items.OAK_SAPLING, PlantKind.TREE, growth));
			// two blocks high: answered without reading the top half's models (there's no game client here)
			assertTrue(can(Items.SUNFLOWER, PlantKind.CROP, growth));
		}
	}

	@Test
	void whatIsNotABlockOrHasNoModelIsTheGenericPlant() {
		for (float growth : new float[] {0, 0.5f, 1}) {
			assertFalse(can(Items.STICK, PlantKind.CROP, growth));
			assertFalse(can(Items.STICK, PlantKind.TREE, growth));
			// a chest is drawn by its renderer, not a block model
			assertFalse(can(Items.CHEST, PlantKind.CROP, growth));
		}
	}

	/** A "sapling" with no wood to match is a sapling while young, then the generic tree. */
	@Test
	void aTreeWithNoWoodIsGenericOnceGrown() {
		assertTrue(can(Items.DIRT, PlantKind.TREE, 0.1f));
		assertFalse(can(Items.DIRT, PlantKind.TREE, 0.9f));
	}

	private static List<ModelResourceLocation> parts(ItemLike plant, PlantKind kind) {
		AutoHarvesterBlockEntity be = new AutoHarvesterBlockEntity(BlockPos.ZERO,
			ModBlocks.AUTO_HARVESTER.get().defaultBlockState().setValue(AutoHarvesterBlock.PLANT, kind));
		ReflectionAccess.set(be, "plant", new ItemStack(plant));
		List<ModelResourceLocation> drawn = new ArrayList<>();
		PartSink sink = (model, ms, light, type, rgb) -> drawn.add(model);
		// (the parts inside: the shaft end needs a game client)
		AutoHarvesterRenderer.inside(be, 0, new PoseStack(), sink, 0);
		return drawn;
	}

	@Test
	void theGenericPlantIsAPartOnlyWhenTheRealOneCantBeDrawn() {
		assertTrue(parts(Items.STICK, PlantKind.CROP).contains(AutoHarvesterRenderer.CROP));
		assertTrue(parts(Items.STICK, PlantKind.TREE).contains(AutoHarvesterRenderer.TREE));
		List<ModelResourceLocation> wheat = parts(Items.WHEAT_SEEDS, PlantKind.CROP);
		assertFalse(wheat.contains(AutoHarvesterRenderer.CROP));
		assertFalse(wheat.contains(AutoHarvesterRenderer.TREE));
		// nothing planted: no plant at all, just the head
		List<ModelResourceLocation> none = parts(Items.STICK, PlantKind.NONE);
		assertFalse(none.contains(AutoHarvesterRenderer.CROP));
		assertEquals(none.size() + 1, parts(Items.STICK, PlantKind.CROP).size());
	}
}
