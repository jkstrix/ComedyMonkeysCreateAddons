package com.comedymonkey.createaddons.registry;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.chest.ChestTier;
import com.comedymonkey.createaddons.content.chest.ChestUpgradeItem;
import com.comedymonkey.createaddons.content.chest.UpgradedBarrelItem;
import com.comedymonkey.createaddons.content.chest.UpgradedChestItem;
import com.comedymonkey.createaddons.content.line.Fitting;
import com.comedymonkey.createaddons.content.line.FittingItem;
import com.comedymonkey.createaddons.content.line.IntakeItem;
import com.comedymonkey.createaddons.content.line.LineTier;
import com.comedymonkey.createaddons.content.line.LineItem;
import com.comedymonkey.createaddons.content.line.PowerLineItem;
import com.comedymonkey.createaddons.content.pen.BottledAnimalItem;
import com.comedymonkey.createaddons.content.wellspring.WellspringUpgrade;
import com.comedymonkey.createaddons.content.wellspring.WellspringUpgradeItem;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.comedymonkey.createaddons.compat.pipez.cover.PipeCover;
import com.comedymonkey.createaddons.compat.pipez.cover.PipeCoverItem;
import com.comedymonkey.createaddons.content.tradebot.TradeBotItem;

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
	public static final DeferredItem<BlockItem> AUTO_HARVESTER = ITEMS.registerSimpleBlockItem(ModBlocks.AUTO_HARVESTER);
	public static final DeferredItem<BlockItem> ANIMAL_PEN = ITEMS.registerSimpleBlockItem(ModBlocks.ANIMAL_PEN);
	public static final DeferredItem<BlockItem> BRASS_ANIMAL_PEN = ITEMS.registerSimpleBlockItem(ModBlocks.BRASS_ANIMAL_PEN);
	public static final DeferredItem<BlockItem> MONSTER_CAGE = ITEMS.registerSimpleBlockItem(ModBlocks.MONSTER_CAGE);
	public static final DeferredItem<BlockItem> BRASS_MONSTER_CAGE = ITEMS.registerSimpleBlockItem(ModBlocks.BRASS_MONSTER_CAGE);
	public static final DeferredItem<BlockItem> FLOODED_MONSTER_CAGE = ITEMS.registerSimpleBlockItem(ModBlocks.FLOODED_MONSTER_CAGE);
	public static final DeferredItem<BlockItem> BRASS_FLOODED_MONSTER_CAGE = ITEMS.registerSimpleBlockItem(ModBlocks.BRASS_FLOODED_MONSTER_CAGE);
	public static final DeferredItem<BlockItem> NETHERITE_MONSTER_CAGE = ITEMS.registerSimpleBlockItem(ModBlocks.NETHERITE_MONSTER_CAGE,
		new Item.Properties().fireResistant());
	/** Catches a monster for the cages (not the Wither); stacks to 16 empty, 1 filled. */
	public static final DeferredItem<com.comedymonkey.createaddons.content.cage.MonsterJarItem> IRON_JAR = ITEMS.register("iron_jar",
		() -> new com.comedymonkey.createaddons.content.cage.MonsterJarItem(new Item.Properties().stacksTo(16), false));
	/** Catches the Wither, for the Netherite cage only. Fireproof. */
	public static final DeferredItem<com.comedymonkey.createaddons.content.cage.MonsterJarItem> NETHERITE_JAR = ITEMS.register("netherite_jar",
		() -> new com.comedymonkey.createaddons.content.cage.MonsterJarItem(new Item.Properties().stacksTo(16).fireResistant(), true));
	/** What the cages' monsters eat. Edible, badly. */
	public static final DeferredItem<com.comedymonkey.createaddons.content.cage.MonsterChowItem> MONSTER_CHOW = ITEMS.register("monster_chow",
		() -> new com.comedymonkey.createaddons.content.cage.MonsterChowItem(new Item.Properties()));
	public static final DeferredItem<BlockItem> CUCKOO_CLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.CUCKOO_CLOCK);
	/** Trade Bot on his station, as one item (D27). */
	public static final DeferredItem<TradeBotItem> TRADE_BOT = ITEMS.register("trade_bot",
		() -> new TradeBotItem(ModBlocks.TRADE_BOT.get(), new Item.Properties().stacksTo(1)));
	/** Catches an animal for the Animal Pens; given back when the animal is let out. Stacks while empty. */
	public static final DeferredItem<Item> CAPTURE_BOTTLE = ITEMS.registerSimpleItem("capture_bottle");
	/** An animal caught in a Capture Bottle, for the Animal Pens. One a stack: each holds its own animal. */
	public static final DeferredItem<BottledAnimalItem> BOTTLED_ANIMAL = ITEMS.register("bottled_animal",
		() -> new BottledAnimalItem(new Item.Properties().stacksTo(1)));

	public static final DeferredItem<BlockItem> INCINERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.INCINERATOR);
	public static final DeferredItem<LineItem> COPPER_FLUID_LINE = ITEMS.register("copper_fluid_line",
		() -> new LineItem(ModBlocks.COPPER_FLUID_LINE.get(), new Item.Properties()));
	public static final DeferredItem<LineItem> BRASS_FLUID_LINE = ITEMS.register("brass_fluid_line",
		() -> new LineItem(ModBlocks.BRASS_FLUID_LINE.get(), new Item.Properties()));
	/** Clipped onto a Fluid Line's side facing a tank, it draws the tank's fluid into the line. */
	public static final DeferredItem<IntakeItem> INTAKE = ITEMS.register("intake", () -> new IntakeItem(new Item.Properties()));
	/** Placed, a wire on the face; on a Line Post's insulators, a hanging span. */
	public static final DeferredItem<PowerLineItem> COPPER_POWER_LINE = ITEMS.register("copper_power_line",
		() -> new PowerLineItem(ModBlocks.COPPER_POWER_LINE.get(), LineTier.COPPER, new Item.Properties()));
	public static final DeferredItem<PowerLineItem> BRASS_POWER_LINE = ITEMS.register("brass_power_line",
		() -> new PowerLineItem(ModBlocks.BRASS_POWER_LINE.get(), LineTier.BRASS, new Item.Properties()));
	public static final DeferredItem<BlockItem> LINE_POST = ITEMS.registerSimpleBlockItem(ModBlocks.LINE_POST);
	// the fittings that clip round a straight piece of line
	public static final DeferredItem<FittingItem> VALVE = ITEMS.register("valve", () -> new FittingItem(Fitting.VALVE, new Item.Properties()));
	public static final DeferredItem<FittingItem> FILTER = ITEMS.register("filter", () -> new FittingItem(Fitting.FILTER, new Item.Properties()));
	public static final DeferredItem<FittingItem> METER = ITEMS.register("meter", () -> new FittingItem(Fitting.METER, new Item.Properties()));
	public static final DeferredItem<FittingItem> SPLITTER = ITEMS.register("splitter", () -> new FittingItem(Fitting.SPLITTER, new Item.Properties()));
	public static final DeferredItem<FittingItem> BREAKER = ITEMS.register("breaker", () -> new FittingItem(Fitting.BREAKER, new Item.Properties()));
	public static final DeferredItem<BlockItem> STRING_TANGLER = ITEMS.registerSimpleBlockItem(ModBlocks.STRING_TANGLER);
	public static final DeferredItem<BlockItem> STONE_INFUSER = ITEMS.registerSimpleBlockItem(ModBlocks.STONE_INFUSER);
	public static final DeferredItem<BlockItem> TUFF_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.TUFF_GENERATOR);
	public static final DeferredItem<BlockItem> WIRELESS_TUFF_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.WIRELESS_TUFF_GENERATOR);
	public static final DeferredItem<BlockItem> MAGMA_ENGINE = ITEMS.registerSimpleBlockItem(ModBlocks.MAGMA_ENGINE);
	public static final DeferredItem<BlockItem> SUPERCHARGED_MAGMA_ENGINE =
		ITEMS.registerSimpleBlockItem(ModBlocks.SUPERCHARGED_MAGMA_ENGINE);
	public static final DeferredItem<BlockItem> ELECTRIC_MOTOR = ITEMS.registerSimpleBlockItem(ModBlocks.ELECTRIC_MOTOR);
	public static final DeferredItem<BlockItem> LAVA_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.LAVA_GENERATOR);
	public static final DeferredItem<com.comedymonkey.createaddons.content.electric.BatteryItem> BATTERY = ITEMS.register("battery",
		() -> new com.comedymonkey.createaddons.content.electric.BatteryItem(ModBlocks.BATTERY.get(), new Item.Properties()));
	public static final DeferredItem<BlockItem> GEOTHERMAL_CASING = ITEMS.registerSimpleBlockItem(ModBlocks.GEOTHERMAL_CASING);
	public static final DeferredItem<BlockItem> GEOTHERMAL_CORE = ITEMS.registerSimpleBlockItem(ModBlocks.GEOTHERMAL_CORE);
	public static final DeferredItem<BlockItem> GEOTHERMAL_CONTROLLER =
		ITEMS.registerSimpleBlockItem(ModBlocks.GEOTHERMAL_CONTROLLER);

	/** Small chance from crushing cobblestone; nine are pressed into an Emerald. */
	public static final DeferredItem<Item> EMERALD_DUST = ITEMS.registerSimpleItem("emerald_dust");
	/** Trade Bot's FE part (D56), for later electric machines too. */
	public static final DeferredItem<Item> FE_COIL = ITEMS.registerSimpleItem("fe_coil");
	/** Pipez Covers: copycat-style covers for Pipez pipes (they only do anything with Pipez installed). */
	public static final DeferredItem<PipeCoverItem> SNUG_PIPEZ_COVER = ITEMS.registerItem("snug_pipez_cover",
		p -> new PipeCoverItem(p, PipeCover.Kind.SNUG));
	public static final DeferredItem<PipeCoverItem> FULL_PIPEZ_COVER = ITEMS.registerItem("full_pipez_cover",
		p -> new PipeCoverItem(p, PipeCover.Kind.FULL));

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
