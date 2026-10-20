package com.comedymonkey.createaddons.content.harvester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.registry.ModItems;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

/**
 * Which vanilla items an Auto Harvester takes, and how long they grow. Item tags aren't bound in unit tests, so
 * these only cover what is decided by block class and registry name (not the plantable/blacklist tags or
 * #minecraft:saplings, which in game also makes azaleas trees).
 */
class HarvesterPlantsTest {
	private static PlantKind kind(ItemLike item) {
		return HarvesterPlants.kindOf(new ItemStack(item));
	}

	private static float factor(ItemLike item) {
		return HarvesterPlants.growthFactor(new ItemStack(item));
	}

	// ---------------------------------------------------------------- kindOf

	@Test
	void saplingsAreTrees() {
		for (ItemLike sapling : new ItemLike[] {Items.OAK_SAPLING, Items.SPRUCE_SAPLING, Items.BIRCH_SAPLING,
			Items.JUNGLE_SAPLING, Items.ACACIA_SAPLING, Items.DARK_OAK_SAPLING, Items.CHERRY_SAPLING,
			Items.MANGROVE_PROPAGULE})
			assertEquals(PlantKind.TREE, kind(sapling), sapling.asItem().toString());
	}

	@Test
	void seedsAndSmallPlantsAreCrops() {
		for (ItemLike crop : new ItemLike[] {Items.WHEAT_SEEDS, Items.CARROT, Items.POTATO, Items.BEETROOT_SEEDS,
			Items.MELON_SEEDS, Items.PUMPKIN_SEEDS, Items.TORCHFLOWER_SEEDS, Items.PITCHER_POD, Items.SWEET_BERRIES,
			Items.GLOW_BERRIES, Items.NETHER_WART, Items.COCOA_BEANS, Items.SUGAR_CANE, Items.CACTUS, Items.BAMBOO,
			Items.KELP, Items.DANDELION, Items.RED_MUSHROOM})
			assertEquals(PlantKind.CROP, kind(crop), crop.asItem().toString());
	}

	@Test
	void flowersSmallAndTallAreCrops() {
		for (ItemLike flower : new ItemLike[] {Items.POPPY, Items.CORNFLOWER, Items.WITHER_ROSE, Items.TORCHFLOWER,
			Items.SUNFLOWER, Items.LILAC, Items.ROSE_BUSH, Items.PEONY, Items.PINK_PETALS})
			assertEquals(PlantKind.CROP, kind(flower), flower.asItem().toString());
	}

	@Test
	void mushroomsAreCrops() {
		assertEquals(PlantKind.CROP, kind(Items.BROWN_MUSHROOM));
		assertEquals(PlantKind.CROP, kind(Items.RED_MUSHROOM));
	}

	/** Nether fungi grow huge fungi: their stems are the logs, their wart blocks the leaves. */
	@Test
	void netherFungiAreTrees() {
		assertEquals(PlantKind.TREE, kind(Items.CRIMSON_FUNGUS));
		assertEquals(PlantKind.TREE, kind(Items.WARPED_FUNGUS));
		assertSame(Blocks.CRIMSON_STEM, HarvesterPlants.woodOf(Blocks.CRIMSON_FUNGUS, "_log"));
		assertSame(Blocks.NETHER_WART_BLOCK, HarvesterPlants.woodOf(Blocks.CRIMSON_FUNGUS, "_leaves"));
		assertSame(Blocks.WARPED_STEM, HarvesterPlants.woodOf(Blocks.WARPED_FUNGUS, "_log"));
		assertSame(Blocks.WARPED_WART_BLOCK, HarvesterPlants.woodOf(Blocks.WARPED_FUNGUS, "_leaves"));
	}

	/** Bushes that give nothing worth growing are refused, though they're plants. */
	@Test
	void grassFernsAndTheLikeAreNotPlantable() {
		for (ItemLike weed : new ItemLike[] {Items.SHORT_GRASS, Items.TALL_GRASS, Items.FERN, Items.LARGE_FERN,
			Items.DEAD_BUSH, Items.CRIMSON_ROOTS, Items.WARPED_ROOTS, Items.NETHER_SPROUTS, Items.SEAGRASS, Items.LILY_PAD})
			assertEquals(PlantKind.NONE, kind(weed), weed.asItem().toString());
	}

	@Test
	void everythingElseIsNotPlantable() {
		for (ItemLike other : new ItemLike[] {Items.WHEAT, Items.STICK, Items.APPLE, Items.DIRT, Items.STONE,
			Items.OAK_LOG, Items.OAK_LEAVES, Items.CHORUS_FLOWER, Items.FARMLAND, Items.BONE_MEAL})
			assertEquals(PlantKind.NONE, kind(other), other.asItem().toString());
	}

	@Test
	void emptyIsNotPlantable() {
		assertEquals(PlantKind.NONE, HarvesterPlants.kindOf(ItemStack.EMPTY));
	}

	@Test
	void stackSizeDoesNotMatter() {
		assertEquals(PlantKind.CROP, HarvesterPlants.kindOf(new ItemStack(Items.WHEAT_SEEDS, 64)));
		assertEquals(PlantKind.TREE, HarvesterPlants.kindOf(new ItemStack(Items.OAK_SAPLING, 16)));
	}

	@Test
	void theMachineItselfIsNotPlantable() {
		assertEquals(PlantKind.NONE, kind(ModItems.AUTO_HARVESTER.get()));
	}

	// ---------------------------------------------------------------- woodOf

	@Test
	void saplingsFindTheirLogsAndLeaves() {
		assertSame(Blocks.OAK_LOG, HarvesterPlants.woodOf(Blocks.OAK_SAPLING, "_log"));
		assertSame(Blocks.OAK_LEAVES, HarvesterPlants.woodOf(Blocks.OAK_SAPLING, "_leaves"));
		assertSame(Blocks.DARK_OAK_LOG, HarvesterPlants.woodOf(Blocks.DARK_OAK_SAPLING, "_log"));
		assertSame(Blocks.CHERRY_LEAVES, HarvesterPlants.woodOf(Blocks.CHERRY_SAPLING, "_leaves"));
		assertSame(Blocks.JUNGLE_LOG, HarvesterPlants.woodOf(Blocks.JUNGLE_SAPLING, "_log"));
	}

	@Test
	void propagulesFindMangrove() {
		assertSame(Blocks.MANGROVE_LOG, HarvesterPlants.woodOf(Blocks.MANGROVE_PROPAGULE, "_log"));
		assertSame(Blocks.MANGROVE_LEAVES, HarvesterPlants.woodOf(Blocks.MANGROVE_PROPAGULE, "_leaves"));
	}

	@Test
	void azaleasGrowOakWithTheirOwnLeaves() {
		assertSame(Blocks.OAK_LOG, HarvesterPlants.woodOf(Blocks.AZALEA, "_log"));
		assertSame(Blocks.AZALEA_LEAVES, HarvesterPlants.woodOf(Blocks.AZALEA, "_leaves"));
		assertSame(Blocks.OAK_LOG, HarvesterPlants.woodOf(Blocks.FLOWERING_AZALEA, "_log"));
		assertSame(Blocks.FLOWERING_AZALEA_LEAVES, HarvesterPlants.woodOf(Blocks.FLOWERING_AZALEA, "_leaves"));
	}

	@Test
	void nonSaplingsHaveNoWood() {
		assertNull(HarvesterPlants.woodOf(Blocks.WHEAT, "_log"));
		assertNull(HarvesterPlants.woodOf(Blocks.STONE, "_log"));
		assertNull(HarvesterPlants.woodOf(Blocks.OAK_LOG, "_log"));
		// Named like a sapling, but there's no bamboo log, stem or wood
		assertNull(HarvesterPlants.woodOf(Blocks.BAMBOO_SAPLING, "_log"));
		assertNull(HarvesterPlants.woodOf(Blocks.BAMBOO_SAPLING, "_leaves"));
	}

	// ---------------------------------------------------------------- growthFactor

	@Test
	void wheatIsTheYardstick() {
		assertEquals(1f, factor(Items.WHEAT_SEEDS), 1e-6f);
		assertEquals(1f, factor(Items.CARROT), 1e-6f);
		assertEquals(1f, factor(Items.POTATO), 1e-6f);
	}

	@Test
	void slowerAndFasterCrops() {
		assertEquals(63f / 49f, factor(Items.BEETROOT_SEEDS), 1e-6f);
		assertEquals(56f / 49f, factor(Items.MELON_SEEDS), 1e-6f);
		assertEquals(56f / 49f, factor(Items.PUMPKIN_SEEDS), 1e-6f);
		assertEquals(14f / 49f, factor(Items.TORCHFLOWER_SEEDS), 1e-6f);
		assertEquals(15f / 49f, factor(Items.SWEET_BERRIES), 1e-6f);
		assertEquals(30f / 49f, factor(Items.NETHER_WART), 1e-6f);
		assertEquals(10f / 49f, factor(Items.COCOA_BEANS), 1e-6f);
		assertEquals(16f / 49f, factor(Items.SUGAR_CANE), 1e-6f);
		assertEquals(16f / 49f, factor(Items.CACTUS), 1e-6f);
	}

	@Test
	void theFastestAreHeldAtAFifth() {
		assertEquals(0.2f, factor(Items.BAMBOO), 1e-6f);
		assertEquals(0.2f, factor(Items.KELP), 1e-6f);
		assertEquals(0.2f, factor(Items.GLOW_BERRIES), 1e-6f);
	}

	@Test
	void alwaysWithinBounds() {
		for (ItemLike item : new ItemLike[] {Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.MELON_SEEDS, Items.BAMBOO,
			Items.KELP, Items.COCOA_BEANS, Items.DANDELION, Items.PITCHER_POD, Items.OAK_SAPLING, Items.STICK}) {
			float factor = factor(item);
			assertTrue(factor >= 0.2f && factor <= 2f, item.asItem() + " " + factor);
		}
	}

	@Test
	void plantsWithoutStagesAndNonBlocksAreNormalSpeed() {
		assertEquals(1f, factor(Items.DANDELION), 1e-6f);
		assertEquals(1f, factor(Items.OAK_SAPLING), 1e-6f);
		assertEquals(1f, factor(Items.STICK), 1e-6f);
		assertEquals(1f, HarvesterPlants.growthFactor(ItemStack.EMPTY), 1e-6f);
	}

	// ---------------------------------------------------------------- reflection guard

	@Test
	void findsTheStemsPrivateFruitField() throws ReflectiveOperationException {
		// Melon and pumpkin seeds harvest the fruit through this; without it they'd give the stem's own drops
		Field field = HarvesterPlants.class.getDeclaredField("STEM_FRUIT");
		field.setAccessible(true);
		assertNotNull(field.get(null));
	}
}
