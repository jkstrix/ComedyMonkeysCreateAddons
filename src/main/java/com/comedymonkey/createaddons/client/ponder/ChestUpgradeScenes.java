package com.comedymonkey.createaddons.client.ponder;

import com.comedymonkey.createaddons.content.chest.ChestTier;
import com.comedymonkey.createaddons.content.chest.UpgradedBarrelBlock;
import com.comedymonkey.createaddons.content.chest.UpgradedChestBlock;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModItems;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;

/**
 * Ponder scene shared by the Chest Upgrade Template and all four chest upgrades. As with the Wellspring scene, the
 * visible text comes from {@code comedymonkeysaddons.ponder.chest_upgrades.*} in en_us.json, numbered in the
 * order the texts appear below.
 */
public final class ChestUpgradeScenes {
	private ChestUpgradeScenes() {}

	public static void upgrades(SceneBuilder scene, SceneBuildingUtil util) {
		scene.title("chest_upgrades", "Increasing Chest and Barrel Capacity with Chest Upgrades");
		scene.configureBasePlate(0, 0, 5);
		scene.showBasePlate();
		scene.idle(5);

		BlockPos chest = util.grid().at(1, 1, 2);
		BlockPos secondHalf = util.grid().at(2, 1, 2);
		BlockPos barrel = util.grid().at(4, 1, 2);
		Vec3 top = util.vector().topOf(chest);
		Vec3 front = util.vector().blockSurface(chest, Direction.SOUTH);

		// The second half's spot is shown (empty) now so it appears when placed later
		scene.world().showSection(util.select().fromTo(chest, secondHalf), Direction.DOWN);
		scene.idle(15);

		// text_1
		scene.overlay().showText(70)
			.text("Chest upgrades let a chest hold more items in each slot")
			.pointAt(front)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(80);

		// Copper: vanilla chest -> upgraded chest
		scene.overlay().showControls(top, Pointing.DOWN, 40)
			.rightClick()
			.withItem(new ItemStack(ModItems.COPPER_CHEST_UPGRADE.get()));
		scene.idle(10);
		scene.world().setBlock(chest, ModBlocks.UPGRADED_CHEST.get().defaultBlockState()
			.setValue(UpgradedChestBlock.FACING, Direction.SOUTH)
			.setValue(UpgradedChestBlock.TIER, ChestTier.COPPER), true);
		// text_2
		scene.overlay().showText(90)
			.text("Right-click a chest with a Copper Chest Upgrade. It keeps its items and holds twice as many per slot")
			.pointAt(top)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		// Iron: replaces the copper upgrade, which is handed back
		upgradeChestTo(scene, chest, top, ChestTier.IRON, ModItems.IRON_CHEST_UPGRADE.get());
		// text_3
		scene.overlay().showText(90)
			.text("A higher tier replaces the current upgrade, which is handed back. Each tier doubles the stack size again")
			.pointAt(top)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		upgradeChestTo(scene, chest, top, ChestTier.GOLD, ModItems.GOLD_CHEST_UPGRADE.get());
		scene.idle(20);
		upgradeChestTo(scene, chest, top, ChestTier.DIAMOND, ModItems.DIAMOND_CHEST_UPGRADE.get());
		// text_4
		scene.overlay().showText(90)
			.text("Copper x2, Iron x4 and Gold x8 per slot. Diamond keeps x8 but doubles the number of slots")
			.pointAt(front)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		// Extend to a double chest: sneak + right-click the side with a chest
		Vec3 side = util.vector().blockSurface(chest, Direction.EAST);
		scene.overlay().showControls(side, Pointing.RIGHT, 40)
			.rightClick()
			.whileSneaking()
			.withItem(new ItemStack(Items.CHEST));
		scene.idle(10);
		// South-facing chest extended east: the original half is RIGHT, the new one LEFT (vanilla pairing rule)
		scene.world().modifyBlock(chest, state -> state.setValue(UpgradedChestBlock.TYPE, ChestType.RIGHT), false);
		scene.world().setBlock(secondHalf, ModBlocks.UPGRADED_CHEST.get().defaultBlockState()
			.setValue(UpgradedChestBlock.FACING, Direction.SOUTH)
			.setValue(UpgradedChestBlock.TIER, ChestTier.DIAMOND)
			.setValue(UpgradedChestBlock.TYPE, ChestType.LEFT), true);
		// text_5
		scene.overlay().showText(90)
			.text("Sneak and right-click the side of an upgraded chest while holding a chest to extend it into a double chest. One upgrade covers both halves")
			.pointAt(util.vector().topOf(secondHalf))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		// Upgrade GUI
		scene.overlay().showControls(top, Pointing.DOWN, 50)
			.rightClick()
			.withItem(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("create", "wrench"))));
		scene.idle(10);
		// text_6
		scene.overlay().showText(90)
			.text("Right-click with a Wrench to open the upgrade screen. Taking the upgrade out turns it back into a normal chest")
			.pointAt(top)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		// Barrels
		scene.world().showSection(util.select().position(barrel), Direction.DOWN);
		scene.idle(15);
		Vec3 barrelTop = util.vector().topOf(barrel);
		scene.overlay().showControls(barrelTop, Pointing.DOWN, 40)
			.rightClick()
			.withItem(new ItemStack(ModItems.COPPER_CHEST_UPGRADE.get()));
		scene.idle(10);
		scene.world().setBlock(barrel, ModBlocks.UPGRADED_BARREL.get().defaultBlockState()
			.setValue(UpgradedBarrelBlock.FACING, Direction.UP)
			.setValue(UpgradedBarrelBlock.TIER, ChestTier.COPPER), true);
		// text_7
		scene.overlay().showText(90)
			.text("Barrels take the same upgrades, with the same tiers and rules")
			.pointAt(barrelTop)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(50);
		for (ChestTier tier : new ChestTier[] { ChestTier.IRON, ChestTier.GOLD, ChestTier.DIAMOND }) {
			scene.overlay().showControls(barrelTop, Pointing.DOWN, 15)
				.rightClick()
				.withItem(new ItemStack(tier.upgradeItem()));
			scene.idle(8);
			scene.world().modifyBlock(barrel, state -> state.setValue(UpgradedBarrelBlock.TIER, tier), true);
			scene.idle(12);
		}
		scene.idle(40);

		// text_8
		scene.overlay().showText(80)
			.text("Hoppers, funnels and pipes fill upgraded chests and barrels all the way to their larger limit")
			.pointAt(util.vector().blockSurface(chest, Direction.WEST))
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(90);

		// text_9
		scene.overlay().showText(90)
			.text("They keep their contents on Create contraptions and Aeronautics physics objects, and can be picked up with Carry On")
			.pointAt(front)
			.placeNearTarget()
			.attachKeyFrame();
		scene.idle(100);

		scene.markAsFinished();
	}

	private static void upgradeChestTo(SceneBuilder scene, BlockPos chest, Vec3 top, ChestTier tier, Item upgrade) {
		scene.overlay().showControls(top, Pointing.DOWN, 30)
			.rightClick()
			.withItem(new ItemStack(upgrade));
		scene.idle(10);
		scene.world().modifyBlock(chest, state -> state.setValue(UpgradedChestBlock.TIER, tier), true);
	}
}
