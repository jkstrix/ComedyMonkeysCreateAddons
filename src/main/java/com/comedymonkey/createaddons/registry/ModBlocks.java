package com.comedymonkey.createaddons.registry;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.chest.UpgradedBarrelBlock;
import com.comedymonkey.createaddons.content.clock.CuckooClockBlock;
import com.comedymonkey.createaddons.content.clock.CuckooClockWeightsBlock;
import com.comedymonkey.createaddons.content.cobblegen.CobbleGeneratorBlock;
import com.comedymonkey.createaddons.content.cobblegen.GeneratorProduct;
import com.comedymonkey.createaddons.content.crucible.PressureCrucibleBlock;
import com.comedymonkey.createaddons.content.engine.MagmaEngineBlock;
import com.comedymonkey.createaddons.content.harvester.AutoHarvesterBlock;
import com.comedymonkey.createaddons.content.pen.AnimalPenBlock;
import com.comedymonkey.createaddons.content.cage.CageTier;
import com.comedymonkey.createaddons.content.cage.MonsterCageBlock;
import com.comedymonkey.createaddons.content.electric.ElectricMotorBlock;
import com.comedymonkey.createaddons.content.electric.GeothermalControllerBlock;
import com.comedymonkey.createaddons.content.electric.GeothermalPartBlock;
import com.comedymonkey.createaddons.content.electric.BatteryBlock;
import com.comedymonkey.createaddons.content.electric.LavaGeneratorBlock;
import com.comedymonkey.createaddons.content.incinerator.IncineratorBlock;
import com.comedymonkey.createaddons.content.line.FluidLineBlock;
import com.comedymonkey.createaddons.content.line.LinePostBlock;
import com.comedymonkey.createaddons.content.line.PowerLineBlock;
import com.comedymonkey.createaddons.content.line.LineTier;
import com.comedymonkey.createaddons.content.machine.TwoInputMachineBlock;
import com.comedymonkey.createaddons.content.chest.UpgradedChestBlock;
import com.comedymonkey.createaddons.content.wellspring.WellspringBlock;
import com.comedymonkey.createaddons.content.wellspring.WellspringSource;

import java.util.List;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import com.comedymonkey.createaddons.content.tradebot.TradeBotStationBlock;
import com.comedymonkey.createaddons.content.helperbot.HelperBotBlock;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ComedyMonkeysCreateAddons.MOD_ID);

	public static final DeferredBlock<WellspringBlock> WELLSPRING = BLOCKS.register("wellspring",
		() -> new WellspringBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_ORANGE)
			.strength(3.0f, 6.0f)
			.sound(SoundType.COPPER)
			.requiresCorrectToolForDrops()
			.noOcclusion()
			.isRedstoneConductor((state, level, pos) -> false)
			.isSuffocating((state, level, pos) -> false)
			.isViewBlocking((state, level, pos) -> false)
			.lightLevel(state -> state.getValue(WellspringBlock.SOURCE) == WellspringSource.LAVA ? 9 : 0)));

	/** Never placed directly: made by using a chest upgrade on a vanilla chest. */
	public static final DeferredBlock<UpgradedChestBlock> UPGRADED_CHEST = BLOCKS.register("upgraded_chest",
		() -> new UpgradedChestBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.strength(2.5f)
			.sound(SoundType.WOOD)
			.noOcclusion()));

	/** Never placed directly: made by using a chest upgrade on a vanilla barrel. */
	public static final DeferredBlock<UpgradedBarrelBlock> UPGRADED_BARREL = BLOCKS.register("upgraded_barrel",
		() -> new UpgradedBarrelBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.strength(2.5f)
			.sound(SoundType.WOOD)));

	public static final DeferredBlock<PressureCrucibleBlock> PRESSURE_CRUCIBLE = BLOCKS.register("pressure_crucible",
		() -> new PressureCrucibleBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.TERRACOTTA_YELLOW)
			.strength(3.5f, 8.0f)
			.sound(SoundType.NETHERITE_BLOCK)
			.requiresCorrectToolForDrops()
			.noOcclusion()
			.isRedstoneConductor((state, level, pos) -> false)));

	/** 1 cobblestone per drill-paced cycle, holds a stack. */
	public static final DeferredBlock<CobbleGeneratorBlock> ANDESITE_COBBLE_GENERATOR = BLOCKS.register("andesite_cobble_generator",
		() -> new CobbleGeneratorBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.strength(2.5f, 6.0f)
			.sound(SoundType.STONE)
			.noOcclusion()
			.isViewBlocking((state, level, pos) -> false)
			.requiresCorrectToolForDrops()
			// The lava in the window glows
			.lightLevel(state -> 7), GeneratorProduct.COBBLESTONE, 1, 64, false));

	/** 16 cobblestone per cycle, holds four stacks. */
	public static final DeferredBlock<CobbleGeneratorBlock> BRASS_COBBLE_GENERATOR = BLOCKS.register("brass_cobble_generator",
		() -> new CobbleGeneratorBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.TERRACOTTA_YELLOW)
			.strength(3.0f, 6.0f)
			.sound(SoundType.NETHERITE_BLOCK)
			.noOcclusion()
			.isViewBlocking((state, level, pos) -> false)
			.requiresCorrectToolForDrops()
			// The lava in the window glows
			.lightLevel(state -> 7), GeneratorProduct.COBBLESTONE, 16, 256, false));

	/** The Andesite generator crafted with a Redstone Link: an antenna on top switches it off wirelessly. */
	public static final DeferredBlock<CobbleGeneratorBlock> WIRELESS_ANDESITE_COBBLE_GENERATOR = BLOCKS.register(
		"wireless_andesite_cobble_generator", () -> new CobbleGeneratorBlock(
			BlockBehaviour.Properties.ofFullCopy(ANDESITE_COBBLE_GENERATOR.get()), GeneratorProduct.COBBLESTONE, 1, 64, true));

	/** The Brass generator crafted with a Redstone Link. */
	public static final DeferredBlock<CobbleGeneratorBlock> WIRELESS_BRASS_COBBLE_GENERATOR = BLOCKS.register(
		"wireless_brass_cobble_generator", () -> new CobbleGeneratorBlock(
			BlockBehaviour.Properties.ofFullCopy(BRASS_COBBLE_GENERATOR.get()), GeneratorProduct.COBBLESTONE, 16, 256, true));

	// Basalt and Blackstone Generators: the same four tiers, with lava meeting blue ice
	public static final DeferredBlock<CobbleGeneratorBlock> ANDESITE_BASALT_GENERATOR = andesiteGenerator("andesite_basalt_generator", GeneratorProduct.BASALT, false);
	public static final DeferredBlock<CobbleGeneratorBlock> BRASS_BASALT_GENERATOR = brassGenerator("brass_basalt_generator", GeneratorProduct.BASALT, false);
	public static final DeferredBlock<CobbleGeneratorBlock> WIRELESS_ANDESITE_BASALT_GENERATOR = andesiteGenerator("wireless_andesite_basalt_generator", GeneratorProduct.BASALT, true);
	public static final DeferredBlock<CobbleGeneratorBlock> WIRELESS_BRASS_BASALT_GENERATOR = brassGenerator("wireless_brass_basalt_generator", GeneratorProduct.BASALT, true);
	public static final DeferredBlock<CobbleGeneratorBlock> ANDESITE_BLACKSTONE_GENERATOR = andesiteGenerator("andesite_blackstone_generator", GeneratorProduct.BLACKSTONE, false);
	public static final DeferredBlock<CobbleGeneratorBlock> BRASS_BLACKSTONE_GENERATOR = brassGenerator("brass_blackstone_generator", GeneratorProduct.BLACKSTONE, false);
	public static final DeferredBlock<CobbleGeneratorBlock> WIRELESS_ANDESITE_BLACKSTONE_GENERATOR = andesiteGenerator("wireless_andesite_blackstone_generator", GeneratorProduct.BLACKSTONE, true);
	public static final DeferredBlock<CobbleGeneratorBlock> WIRELESS_BRASS_BLACKSTONE_GENERATOR = brassGenerator("wireless_brass_blackstone_generator", GeneratorProduct.BLACKSTONE, true);

	/** Like the Andesite Cobblestone Generator: one block per cycle, holds a stack. */
	private static DeferredBlock<CobbleGeneratorBlock> andesiteGenerator(String id, GeneratorProduct product, boolean wireless) {
		return BLOCKS.register(id, () -> new CobbleGeneratorBlock(
			BlockBehaviour.Properties.ofFullCopy(ANDESITE_COBBLE_GENERATOR.get()), product, 1, 64, wireless));
	}

	/** Like the Brass Cobblestone Generator: sixteen per cycle, holds four stacks. */
	private static DeferredBlock<CobbleGeneratorBlock> brassGenerator(String id, GeneratorProduct product, boolean wireless) {
		return BLOCKS.register(id, () -> new CobbleGeneratorBlock(
			BlockBehaviour.Properties.ofFullCopy(BRASS_COBBLE_GENERATOR.get()), product, 16, 256, wireless));
	}

	// Tuff Generators: the Andesite kind and its wireless version
	public static final DeferredBlock<CobbleGeneratorBlock> TUFF_GENERATOR = andesiteGenerator("tuff_generator", GeneratorProduct.TUFF, false);
	public static final DeferredBlock<CobbleGeneratorBlock> WIRELESS_TUFF_GENERATOR = andesiteGenerator("wireless_tuff_generator", GeneratorProduct.TUFF, true);

	/** Every generator: Andesite, Brass and their wireless versions of each product, and the two Tuff ones. */
	public static List<DeferredBlock<CobbleGeneratorBlock>> generators() {
		return List.of(ANDESITE_COBBLE_GENERATOR, BRASS_COBBLE_GENERATOR, WIRELESS_ANDESITE_COBBLE_GENERATOR,
			WIRELESS_BRASS_COBBLE_GENERATOR, ANDESITE_BASALT_GENERATOR, BRASS_BASALT_GENERATOR,
			WIRELESS_ANDESITE_BASALT_GENERATOR, WIRELESS_BRASS_BASALT_GENERATOR, ANDESITE_BLACKSTONE_GENERATOR,
			BRASS_BLACKSTONE_GENERATOR, WIRELESS_ANDESITE_BLACKSTONE_GENERATOR, WIRELESS_BRASS_BLACKSTONE_GENERATOR,
			TUFF_GENERATOR, WIRELESS_TUFF_GENERATOR);
	}

	/** Grows a crop or sapling behind glass and harvests it, at a fixed pace and stress. */
	public static final DeferredBlock<AutoHarvesterBlock> AUTO_HARVESTER = BLOCKS.register("auto_harvester",
		() -> new AutoHarvesterBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.strength(2.5f, 6.0f)
			.sound(SoundType.STONE)
			.noOcclusion()
			.isViewBlocking((state, level, pos) -> false)
			.requiresCorrectToolForDrops()));

	/** Keeps one captured animal and breeds it up, at a fixed stress. */
	public static final DeferredBlock<AnimalPenBlock> ANIMAL_PEN = BLOCKS.register("animal_pen",
		() -> new AnimalPenBlock(BlockBehaviour.Properties.ofFullCopy(AUTO_HARVESTER.get()), false));
	/** Holds four times as many animals. */
	public static final DeferredBlock<AnimalPenBlock> BRASS_ANIMAL_PEN = BLOCKS.register("brass_animal_pen",
		() -> new AnimalPenBlock(BlockBehaviour.Properties.ofFullCopy(BRASS_COBBLE_GENERATOR.get()).lightLevel(state -> 0), true));

	/** Keeps one jarred monster and brings in more of it, each eating Monster Chow. */
	public static final DeferredBlock<MonsterCageBlock> MONSTER_CAGE = BLOCKS.register("monster_cage",
		() -> new MonsterCageBlock(BlockBehaviour.Properties.ofFullCopy(AUTO_HARVESTER.get()), CageTier.ANDESITE, false));
	public static final DeferredBlock<MonsterCageBlock> BRASS_MONSTER_CAGE = BLOCKS.register("brass_monster_cage",
		() -> new MonsterCageBlock(BlockBehaviour.Properties.ofFullCopy(BRASS_COBBLE_GENERATOR.get()).lightLevel(state -> 0),
			CageTier.BRASS, false));
	/** For drowned and guardians: water piped in. */
	public static final DeferredBlock<MonsterCageBlock> FLOODED_MONSTER_CAGE = BLOCKS.register("flooded_monster_cage",
		() -> new MonsterCageBlock(BlockBehaviour.Properties.ofFullCopy(AUTO_HARVESTER.get()), CageTier.ANDESITE, true));
	public static final DeferredBlock<MonsterCageBlock> BRASS_FLOODED_MONSTER_CAGE = BLOCKS.register("brass_flooded_monster_cage",
		() -> new MonsterCageBlock(BlockBehaviour.Properties.ofFullCopy(BRASS_COBBLE_GENERATOR.get()).lightLevel(state -> 0),
			CageTier.BRASS, true));
	/** Breeds one kind of fish, squid or axolotl, underwater. */
	public static final DeferredBlock<com.comedymonkey.createaddons.content.tank.FishTankBlock> FISH_TANK = BLOCKS.register("fish_tank",
		() -> new com.comedymonkey.createaddons.content.tank.FishTankBlock(BlockBehaviour.Properties.ofFullCopy(AUTO_HARVESTER.get()).sound(SoundType.GLASS), false));
	public static final DeferredBlock<com.comedymonkey.createaddons.content.tank.FishTankBlock> BRASS_FISH_TANK = BLOCKS.register("brass_fish_tank",
		() -> new com.comedymonkey.createaddons.content.tank.FishTankBlock(BlockBehaviour.Properties.ofFullCopy(BRASS_COBBLE_GENERATOR.get()).sound(SoundType.GLASS), true));
	/** Withers in a containment field on FE; blast-proof. */
	public static final DeferredBlock<MonsterCageBlock> NETHERITE_MONSTER_CAGE = BLOCKS.register("netherite_monster_cage",
		() -> new MonsterCageBlock(BlockBehaviour.Properties.ofFullCopy(BRASS_COBBLE_GENERATOR.get()).lightLevel(state -> 0)
			.strength(5f, 1200f).sound(SoundType.NETHERITE_BLOCK), CageTier.NETHERITE, false));

	/** A wall clock two blocks tall that sets nearby villagers' restock times to 8:00 and 12:00, at a fixed stress. */
	public static final DeferredBlock<CuckooClockBlock> CUCKOO_CLOCK = BLOCKS.register("cuckoo_clock",
		() -> new CuckooClockBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_BROWN)
			.strength(2.0f, 3.0f)
			.sound(SoundType.WOOD)
			.noOcclusion()
			.isViewBlocking((state, level, pos) -> false)));
	/** The clock's bottom block, where the pendulum and weights hang; placed and removed with it. */
	public static final DeferredBlock<CuckooClockWeightsBlock> CUCKOO_CLOCK_WEIGHTS = BLOCKS.register("cuckoo_clock_weights",
		() -> new CuckooClockWeightsBlock(BlockBehaviour.Properties.ofFullCopy(CUCKOO_CLOCK.get())
			.noLootTable()
			.randomTicks()
			.pushReaction(PushReaction.BLOCK)));

	/** Tangles string and slime into cobwebs, behind copper-framed glass. */
	public static final DeferredBlock<TwoInputMachineBlock> STRING_TANGLER = BLOCKS.register("string_tangler",
		() -> new TwoInputMachineBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_ORANGE)
			.strength(2.5f, 6.0f)
			.sound(SoundType.COPPER)
			.requiresCorrectToolForDrops()
			.noOcclusion()
			.isViewBlocking((state, level, pos) -> false)
			.isSuffocating((state, level, pos) -> false), () -> ModBlockEntities.STRING_TANGLER.get()));

	/** Infuses cobblestone with crushed ore to make Create's ore-bearing stones. */
	public static final DeferredBlock<TwoInputMachineBlock> STONE_INFUSER = BLOCKS.register("stone_infuser",
		() -> new TwoInputMachineBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.strength(3.0f, 6.0f)
			.sound(SoundType.STONE)
			.requiresCorrectToolForDrops()
			.noOcclusion()
			.isViewBlocking((state, level, pos) -> false), () -> ModBlockEntities.STONE_INFUSER.get()));

	/** Destroys every item and fluid put into it; heats a boiler while it burns. */
	public static final DeferredBlock<IncineratorBlock> INCINERATOR = BLOCKS.register("incinerator",
		() -> new IncineratorBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.strength(3.5f, 6.0f)
			.sound(SoundType.STONE)
			.requiresCorrectToolForDrops()
			// the firebox mouth is recessed into the front, so it must not hide its neighbours' faces or go dark inside
			.noOcclusion()
			.lightLevel(state -> state.getValue(IncineratorBlock.LIT) ? 13 : 0)));

	/** Lava-burning engine: four Portable Engines' worth of power from one block. */
	public static final DeferredBlock<MagmaEngineBlock> MAGMA_ENGINE = BLOCKS.register("magma_engine",
		() -> new MagmaEngineBlock(magmaEngineProperties(), false));
	/** Twice the Magma Engine's speed, power and lava. */
	public static final DeferredBlock<MagmaEngineBlock> SUPERCHARGED_MAGMA_ENGINE = BLOCKS.register("supercharged_magma_engine",
		() -> new MagmaEngineBlock(magmaEngineProperties(), true));

	private static BlockBehaviour.Properties magmaEngineProperties() {
		return BlockBehaviour.Properties.of()
			.mapColor(MapColor.TERRACOTTA_YELLOW)
			.strength(3.5f, 8.0f)
			.sound(SoundType.NETHERITE_BLOCK)
			.requiresCorrectToolForDrops()
			// Its body sits a little inside the block, so it mustn't hide its neighbours' faces
			.noOcclusion()
			.lightLevel(state -> state.getValue(MagmaEngineBlock.LIT) ? 10 : 0);
	}

	// ---- Fluid Lines: slim pump-free pipes in two tiers that never join each other
	public static final DeferredBlock<FluidLineBlock> COPPER_FLUID_LINE = BLOCKS.register("copper_fluid_line",
		() -> new FluidLineBlock(LineTier.COPPER, lineProperties(MapColor.COLOR_ORANGE)));
	public static final DeferredBlock<FluidLineBlock> BRASS_FLUID_LINE = BLOCKS.register("brass_fluid_line",
		() -> new FluidLineBlock(LineTier.BRASS, lineProperties(MapColor.TERRACOTTA_YELLOW)));

	// ---- Power Lines: thin cable on any face, and wooden Line Posts to hang it between
	public static final DeferredBlock<PowerLineBlock> COPPER_POWER_LINE = BLOCKS.register("copper_power_line",
		() -> new PowerLineBlock(LineTier.COPPER, wireProperties(MapColor.COLOR_ORANGE)));
	public static final DeferredBlock<PowerLineBlock> BRASS_POWER_LINE = BLOCKS.register("brass_power_line",
		() -> new PowerLineBlock(LineTier.BRASS, wireProperties(MapColor.TERRACOTTA_YELLOW)));
	public static final DeferredBlock<LinePostBlock> LINE_POST = BLOCKS.register("line_post",
		() -> new LinePostBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.WOOD)
			.strength(1.5f, 3.0f)
			.sound(SoundType.WOOD)
			.noOcclusion()
			.isSuffocating((state, level, pos) -> false)
			.isViewBlocking((state, level, pos) -> false)));

	private static BlockBehaviour.Properties wireProperties(MapColor color) {
		return BlockBehaviour.Properties.of()
			.mapColor(color)
			.strength(0.3f)
			.sound(SoundType.WOOL)
			.noOcclusion()
			.noCollission()
			.isRedstoneConductor((state, level, pos) -> false)
			.isSuffocating((state, level, pos) -> false)
			.isViewBlocking((state, level, pos) -> false);
	}

	private static BlockBehaviour.Properties lineProperties(MapColor color) {
		return BlockBehaviour.Properties.of()
			.mapColor(color)
			.strength(1.0f, 3.0f)
			.sound(SoundType.COPPER)
			// a thin pipe through the block: its neighbours' faces show round it
			.noOcclusion()
			.isRedstoneConductor((state, level, pos) -> false)
			.isSuffocating((state, level, pos) -> false)
			.isViewBlocking((state, level, pos) -> false);
	}

	/** Not an item: just the look of a Pipez Cover with no block on it yet. */
	public static final DeferredBlock<Block> PIPE_COVER_BLANK = BLOCKS.register("pipe_cover_blank",
		() -> new Block(BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(1.5f)
			.sound(SoundType.COPPER)
			.noLootTable()));

	// ---- Electricity
	public static final DeferredBlock<ElectricMotorBlock> ELECTRIC_MOTOR = BLOCKS.register("electric_motor",
		() -> new ElectricMotorBlock(machine().lightLevel(state -> state.getValue(ElectricMotorBlock.POWERED) ? 6 : 0)));
	/** The Clockwork Juggernaut's ritual key (D16-D18): fitted to a T of Brass Blocks and wound with a shaft. */
	public static final DeferredBlock<com.comedymonkey.createaddons.content.juggernaut.WindingKeyBlock> WINDING_KEY = BLOCKS.register("winding_key",
		() -> new com.comedymonkey.createaddons.content.juggernaut.WindingKeyBlock(machine()));
	/** The Mainspring's use (D34-D39): a two-block engine that stores rotation, wound in behind and let out in front. */
	public static final DeferredBlock<com.comedymonkey.createaddons.content.juggernaut.SpringEngineBlock> SPRING_ENGINE = BLOCKS.register("spring_engine",
		() -> new com.comedymonkey.createaddons.content.juggernaut.SpringEngineBlock(machine()));
	public static final DeferredBlock<LavaGeneratorBlock> LAVA_GENERATOR = BLOCKS.register("lava_generator",
		() -> new LavaGeneratorBlock(machine().lightLevel(state -> state.getValue(LavaGeneratorBlock.LIT) ? 9
			: state.getValue(LavaGeneratorBlock.FUELLED) ? 4 : 0)));
	public static final DeferredBlock<GeothermalPartBlock> GEOTHERMAL_CASING = BLOCKS.register("geothermal_casing",
		() -> new GeothermalPartBlock(machine(), true));
	public static final DeferredBlock<GeothermalPartBlock> GEOTHERMAL_CORE = BLOCKS.register("geothermal_core",
		() -> new GeothermalPartBlock(machine(), false));
	public static final DeferredBlock<GeothermalControllerBlock> GEOTHERMAL_CONTROLLER = BLOCKS.register("geothermal_controller",
		() -> new GeothermalControllerBlock(machine().lightLevel(state -> state.getValue(GeothermalControllerBlock.LIT) ? 12 : 0)));

	/** Battery: the plain metal block sounds (it's silent in use); blown up, it always drops with its charge (D21). */
	public static final DeferredBlock<BatteryBlock> BATTERY = BLOCKS.register("battery",
		() -> new BatteryBlock(machine().sound(SoundType.METAL)));

	/** Trade Bot's winding station: unbreakable except by its owner's wrench (D51), which packs it up with him. */
	/** Helper Bot: drives trains and keeps shop like a Blaze Burner (design/specs/helper-bot.md). Pickaxe, andesite-casing hardness (D23). */
	public static final DeferredBlock<HelperBotBlock> HELPER_BOT = BLOCKS.register("helper_bot",
		() -> new HelperBotBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.strength(1.5f, 6.0f)
			.sound(SoundType.NETHERITE_BLOCK)
			.requiresCorrectToolForDrops()
			.noOcclusion()
			.isRedstoneConductor((state, level, pos) -> false)
			.isSuffocating((state, level, pos) -> false)
			.isViewBlocking((state, level, pos) -> false)));

	public static final DeferredBlock<TradeBotStationBlock> TRADE_BOT = BLOCKS.register("trade_bot",
		() -> new TradeBotStationBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(-1.0f, 3600000.0f)
			.sound(SoundType.NETHERITE_BLOCK)
			.noOcclusion()
			.noLootTable()
			.pushReaction(PushReaction.BLOCK)
			.lightLevel(state -> state.getValue(TradeBotStationBlock.GLOW) ? 4 : 0)));

	// ---- Electric Lights (design/specs/electric-lights.md): break fast with a pickaxe (P30), never pushed (they hold a block entity)
	public static final java.util.Map<com.comedymonkey.createaddons.content.light.LightKind, DeferredBlock<com.comedymonkey.createaddons.content.light.LightBlock>> LIGHTS =
		registerLights();

	/** Electric Lights: the Light Strip, on any face of a block, joining its neighbours like a Power Line. */
	public static final DeferredBlock<com.comedymonkey.createaddons.content.light.LightStripBlock> LIGHT_STRIP = BLOCKS.register("light_strip",
		() -> new com.comedymonkey.createaddons.content.light.LightStripBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(0.5f, 1.0f)
			.sound(SoundType.COPPER)
			.noOcclusion()
			.noCollission()
			.pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)
			.lightLevel(com.comedymonkey.createaddons.content.light.LightStripBlock::lightLevel)));

	/** Electric Lights: the Edge Light, trim along the edges of a block's faces, several edges in one block. */
	public static final DeferredBlock<com.comedymonkey.createaddons.content.light.EdgeLightBlock> EDGE_LIGHT = BLOCKS.register("edge_light",
		() -> new com.comedymonkey.createaddons.content.light.EdgeLightBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(0.5f, 1.0f)
			.sound(SoundType.COPPER)
			.noOcclusion()
			.noCollission()
			.dynamicShape()
			.pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)
			.lightLevel(com.comedymonkey.createaddons.content.light.EdgeLightBlock::lightLevel)));

	/** A street head can hang on a Lamp Post's arm (LampHeadBlock); every other light is a plain LightBlock. */
	private static com.comedymonkey.createaddons.content.light.LightBlock lightBlock(com.comedymonkey.createaddons.content.light.LightKind kind, boolean head,
																				   BlockBehaviour.Properties properties) {
		return head ? new com.comedymonkey.createaddons.content.light.LampHeadBlock(kind, properties)
			: new com.comedymonkey.createaddons.content.light.LightBlock(kind, properties);
	}

	/** Electric Lights: the Lamp Posts, a pole of cast iron or steel for street lamps. */
	public static final DeferredBlock<com.comedymonkey.createaddons.content.light.LampPostBlock> CAST_IRON_LAMP_POST = lampPost("cast_iron_lamp_post",
		com.comedymonkey.createaddons.content.light.ArmFinish.IRON);
	public static final DeferredBlock<com.comedymonkey.createaddons.content.light.LampPostBlock> STEEL_LAMP_POST = lampPost("steel_lamp_post",
		com.comedymonkey.createaddons.content.light.ArmFinish.STEEL);

	private static DeferredBlock<com.comedymonkey.createaddons.content.light.LampPostBlock> lampPost(String id, com.comedymonkey.createaddons.content.light.ArmFinish finish) {
		return BLOCKS.register(id, () -> new com.comedymonkey.createaddons.content.light.LampPostBlock(finish, BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(2.0f, 6.0f)
			.sound(SoundType.METAL)
			.requiresCorrectToolForDrops()
			.noOcclusion()
			.isSuffocating((state, level, pos) -> false)
			.isViewBlocking((state, level, pos) -> false)));
	}

	private static java.util.Map<com.comedymonkey.createaddons.content.light.LightKind, DeferredBlock<com.comedymonkey.createaddons.content.light.LightBlock>> registerLights() {
		var out = new java.util.EnumMap<com.comedymonkey.createaddons.content.light.LightKind, DeferredBlock<com.comedymonkey.createaddons.content.light.LightBlock>>(
			com.comedymonkey.createaddons.content.light.LightKind.class);
		for (var kind : com.comedymonkey.createaddons.content.light.LightKind.values()) {
			boolean floor = kind == com.comedymonkey.createaddons.content.light.LightKind.FLOOR_LIGHT;
			boolean small = kind == com.comedymonkey.createaddons.content.light.LightKind.INDICATOR;
			boolean head = kind == com.comedymonkey.createaddons.content.light.LightKind.LANTERN_HEAD
				|| kind == com.comedymonkey.createaddons.content.light.LightKind.GLOBE_HEAD
				|| kind == com.comedymonkey.createaddons.content.light.LightKind.COBRA_HEAD
				|| kind == com.comedymonkey.createaddons.content.light.LightKind.YARD_HEAD;
			out.put(kind, BLOCKS.register(kind.id(), () -> lightBlock(kind, head, BlockBehaviour.Properties.of()
				.mapColor(MapColor.METAL)
				.strength(floor ? 3.0f : 1.0f, floor ? 6.0f : 3.0f)
				.sound(floor ? SoundType.METAL : small ? SoundType.COPPER : SoundType.LANTERN)
				.requiresCorrectToolForDrops()
				.noOcclusion()
				.lightLevel(state -> com.comedymonkey.createaddons.content.light.LightBlock.lightLevel(kind, state)))));
		}
		return java.util.Collections.unmodifiableMap(out);
	}

	private static BlockBehaviour.Properties machine() {
		return BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(3.5f, 8.0f)
			.sound(SoundType.NETHERITE_BLOCK)
			.requiresCorrectToolForDrops()
			.noOcclusion();
	}

	private ModBlocks() {}
}
