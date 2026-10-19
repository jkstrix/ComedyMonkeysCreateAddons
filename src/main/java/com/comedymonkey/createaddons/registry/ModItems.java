package com.comedymonkey.createaddons.registry;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.chest.ChestTier;
import com.comedymonkey.createaddons.content.chest.ChestUpgradeItem;
import com.comedymonkey.createaddons.content.chest.UpgradedBarrelItem;
import com.comedymonkey.createaddons.content.chest.UpgradedChestItem;
import com.comedymonkey.createaddons.content.wellspring.WellspringUpgrade;
import com.comedymonkey.createaddons.content.wellspring.WellspringUpgradeItem;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ComedyMonkeysCreateAddons.MOD_ID);

	public static final DeferredItem<BlockItem> WELLSPRING = ITEMS.registerSimpleBlockItem(ModBlocks.WELLSPRING);

	/** Pick-block / carried form of an upgraded chest (tier in its block-state component). */
	public static final DeferredItem<UpgradedChestItem> UPGRADED_CHEST = ITEMS.register("upgraded_chest",
		() -> new UpgradedChestItem(ModBlocks.UPGRADED_CHEST.get(), new Item.Properties()));

	/** Creative tab icon only: ComedyMonkey's head, from their Minecraft skin. Not obtainable. */
	public static final DeferredItem<Item> COMEDYMONKEY_HEAD = ITEMS.registerSimpleItem("comedymonkey_head");

	/** Pick-block / carried form of an upgraded barrel (tier in its block-state component). */
	public static final DeferredItem<UpgradedBarrelItem> UPGRADED_BARREL = ITEMS.register("upgraded_barrel",
		() -> new UpgradedBarrelItem(ModBlocks.UPGRADED_BARREL.get(), new Item.Properties()));

	public static final DeferredItem<BlockItem> PRESSURE_CRUCIBLE = ITEMS.registerSimpleBlockItem(ModBlocks.PRESSURE_CRUCIBLE);

	public static final DeferredItem<BlockItem> ANDESITE_COBBLE_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.ANDESITE_COBBLE_GENERATOR);
	public static final DeferredItem<BlockItem> BRASS_COBBLE_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.BRASS_COBBLE_GENERATOR);
	public static final DeferredItem<BlockItem> WIRELESS_ANDESITE_COBBLE_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.WIRELESS_ANDESITE_COBBLE_GENERATOR);
	public static final DeferredItem<BlockItem> WIRELESS_BRASS_COBBLE_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.WIRELESS_BRASS_COBBLE_GENERATOR);
	public static final DeferredItem<BlockItem> ANDESITE_BASALT_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.ANDESITE_BASALT_GENERATOR);
	public static final DeferredItem<BlockItem> BRASS_BASALT_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.BRASS_BASALT_GENERATOR);
	public static final DeferredItem<BlockItem> WIRELESS_ANDESITE_BASALT_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.WIRELESS_ANDESITE_BASALT_GENERATOR);
	public static final DeferredItem<BlockItem> WIRELESS_BRASS_BASALT_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.WIRELESS_BRASS_BASALT_GENERATOR);
	public static final DeferredItem<BlockItem> ANDESITE_BLACKSTONE_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.ANDESITE_BLACKSTONE_GENERATOR);
	public static final DeferredItem<BlockItem> BRASS_BLACKSTONE_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.BRASS_BLACKSTONE_GENERATOR);
	public static final DeferredItem<BlockItem> WIRELESS_ANDESITE_BLACKSTONE_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.WIRELESS_ANDESITE_BLACKSTONE_GENERATOR);
	public static final DeferredItem<BlockItem> WIRELESS_BRASS_BLACKSTONE_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.WIRELESS_BRASS_BLACKSTONE_GENERATOR);

	public static final DeferredItem<BlockItem> INCINERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.INCINERATOR);
	public static final DeferredItem<BlockItem> STRING_TANGLER = ITEMS.registerSimpleBlockItem(ModBlocks.STRING_TANGLER);
	public static final DeferredItem<BlockItem> STONE_INFUSER = ITEMS.registerSimpleBlockItem(ModBlocks.STONE_INFUSER);
	public static final DeferredItem<BlockItem> TUFF_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.TUFF_GENERATOR);
	public static final DeferredItem<BlockItem> WIRELESS_TUFF_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.WIRELESS_TUFF_GENERATOR);
	public static final DeferredItem<BlockItem> MAGMA_ENGINE = ITEMS.registerSimpleBlockItem(ModBlocks.MAGMA_ENGINE);

	/** Small chance from crushing cobblestone; nine are pressed into an Emerald. */
	public static final DeferredItem<Item> EMERALD_DUST = ITEMS.registerSimpleItem("emerald_dust");

	/** Small chance from crushing netherrack; nine are pressed into Netherite Scrap. */
	public static final DeferredItem<Item> NETHERITE_DUST = ITEMS.registerSimpleItem("netherite_dust",
		new Item.Properties().fireResistant().rarity(Rarity.UNCOMMON));

	/** Small chance from crushing netherrack or blackstone; pressed into Nether Quartz. */
	public static final DeferredItem<Item> QUARTZ_DUST = ITEMS.registerSimpleItem("quartz_dust");

	/** Common base for every Wellspring upgrade. */
	public static final DeferredItem<Item> UPGRADE_TEMPLATE = ITEMS.registerSimpleItem("upgrade_template");

	/** Upgrade Template with two dripstones; a spout fills it with water or lava to make the Wellspring upgrades. */
	public static final DeferredItem<Item> DRIP_TEMPLATE = ITEMS.registerSimpleItem("drip_template",
		new Item.Properties().stacksTo(16));

	public static final DeferredItem<WellspringUpgradeItem> WATER_UPGRADE = ITEMS.register("water_upgrade",
		() -> new WellspringUpgradeItem(WellspringUpgrade.WATER, new Item.Properties().stacksTo(16)));

	public static final DeferredItem<WellspringUpgradeItem> DRIPSTONE_UPGRADE = ITEMS.register("dripstone_upgrade",
		() -> new WellspringUpgradeItem(WellspringUpgrade.DRIPSTONE, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

	public static final DeferredItem<WellspringUpgradeItem> HONEY_UPGRADE = ITEMS.register("honey_upgrade",
		() -> new WellspringUpgradeItem(WellspringUpgrade.HONEY, new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

	public static final DeferredItem<WellspringUpgradeItem> CHOCOLATE_UPGRADE = ITEMS.register("chocolate_upgrade",
		() -> new WellspringUpgradeItem(WellspringUpgrade.CHOCOLATE, new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

	public static final DeferredItem<WellspringUpgradeItem> CAPACITY_UPGRADE = ITEMS.register("capacity_upgrade",
		() -> new WellspringUpgradeItem(WellspringUpgrade.CAPACITY, new Item.Properties().stacksTo(16)));

	// Chest upgrades: Upgrade Template -> Chest Upgrade Template -> Copper -> Iron -> Gold -> Diamond
	public static final DeferredItem<Item> CHEST_UPGRADE_TEMPLATE = ITEMS.registerSimpleItem("chest_upgrade_template");

	public static final DeferredItem<ChestUpgradeItem> COPPER_CHEST_UPGRADE = ITEMS.register("copper_chest_upgrade",
		() -> new ChestUpgradeItem(ChestTier.COPPER, new Item.Properties().stacksTo(16)));

	public static final DeferredItem<ChestUpgradeItem> IRON_CHEST_UPGRADE = ITEMS.register("iron_chest_upgrade",
		() -> new ChestUpgradeItem(ChestTier.IRON, new Item.Properties().stacksTo(16)));

	public static final DeferredItem<ChestUpgradeItem> GOLD_CHEST_UPGRADE = ITEMS.register("gold_chest_upgrade",
		() -> new ChestUpgradeItem(ChestTier.GOLD, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

	public static final DeferredItem<ChestUpgradeItem> DIAMOND_CHEST_UPGRADE = ITEMS.register("diamond_chest_upgrade",
		() -> new ChestUpgradeItem(ChestTier.DIAMOND, new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

	/** Smithed from a Diamond Chest Upgrade with a netherite ingot; survives fire and lava like netherite gear. */
	public static final DeferredItem<ChestUpgradeItem> NETHERITE_CHEST_UPGRADE = ITEMS.register("netherite_chest_upgrade",
		() -> new ChestUpgradeItem(ChestTier.NETHERITE, new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant()));

	// Transitional items for the sequenced assembly recipes
	public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_UPGRADE_TEMPLATE = ITEMS.register(
		"incomplete_upgrade_template", () -> new SequencedAssemblyItem(new Item.Properties().stacksTo(1)));

	public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_WATER_UPGRADE = ITEMS.register(
		"incomplete_water_upgrade", () -> new SequencedAssemblyItem(new Item.Properties().stacksTo(1)));

	public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_DRIPSTONE_UPGRADE = ITEMS.register(
		"incomplete_dripstone_upgrade", () -> new SequencedAssemblyItem(new Item.Properties().stacksTo(1)));

	public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_CAPACITY_UPGRADE = ITEMS.register(
		"incomplete_capacity_upgrade", () -> new SequencedAssemblyItem(new Item.Properties().stacksTo(1)));

	public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_DRIP_TEMPLATE = incomplete("drip_template");
	public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_HONEY_UPGRADE = incomplete("honey_upgrade");
	public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_CHOCOLATE_UPGRADE = incomplete("chocolate_upgrade");
	public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_CHEST_UPGRADE_TEMPLATE = incomplete("chest_upgrade_template");
	public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_COPPER_CHEST_UPGRADE = incomplete("copper_chest_upgrade");
	public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_IRON_CHEST_UPGRADE = incomplete("iron_chest_upgrade");
	public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_GOLD_CHEST_UPGRADE = incomplete("gold_chest_upgrade");
	public static final DeferredItem<SequencedAssemblyItem> INCOMPLETE_DIAMOND_CHEST_UPGRADE = incomplete("diamond_chest_upgrade");

	private static DeferredItem<SequencedAssemblyItem> incomplete(String name) {
		return ITEMS.register("incomplete_" + name, () -> new SequencedAssemblyItem(new Item.Properties().stacksTo(1)));
	}

	private ModItems() {}
}
