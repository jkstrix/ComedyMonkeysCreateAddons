package com.comedymonkey.createaddons.registry;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.chest.UpgradedBarrelBlock;
import com.comedymonkey.createaddons.content.cobblegen.CobbleGeneratorBlock;
import com.comedymonkey.createaddons.content.cobblegen.GeneratorProduct;
import com.comedymonkey.createaddons.content.crucible.PressureCrucibleBlock;
import com.comedymonkey.createaddons.content.engine.MagmaEngineBlock;
import com.comedymonkey.createaddons.content.harvester.AutoHarvesterBlock;
import com.comedymonkey.createaddons.content.pen.AnimalPenBlock;
import com.comedymonkey.createaddons.content.electric.ElectricMotorBlock;
import com.comedymonkey.createaddons.content.electric.GeothermalControllerBlock;
import com.comedymonkey.createaddons.content.electric.GeothermalPartBlock;
import com.comedymonkey.createaddons.content.electric.LavaGeneratorBlock;
import com.comedymonkey.createaddons.content.incinerator.IncineratorBlock;
import com.comedymonkey.createaddons.content.machine.TwoInputMachineBlock;
import com.comedymonkey.createaddons.content.chest.UpgradedChestBlock;
import com.comedymonkey.createaddons.content.wellspring.WellspringBlock;
import com.comedymonkey.createaddons.content.wellspring.WellspringSource;

import java.util.List;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
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
	public static final DeferredBlock<LavaGeneratorBlock> LAVA_GENERATOR = BLOCKS.register("lava_generator",
		() -> new LavaGeneratorBlock(machine().lightLevel(state -> state.getValue(LavaGeneratorBlock.LIT) ? 9 : 0)));
	public static final DeferredBlock<GeothermalPartBlock> GEOTHERMAL_CASING = BLOCKS.register("geothermal_casing",
		() -> new GeothermalPartBlock(machine(), true));
	public static final DeferredBlock<GeothermalPartBlock> GEOTHERMAL_CORE = BLOCKS.register("geothermal_core",
		() -> new GeothermalPartBlock(machine(), false));
	public static final DeferredBlock<GeothermalControllerBlock> GEOTHERMAL_CONTROLLER = BLOCKS.register("geothermal_controller",
		() -> new GeothermalControllerBlock(machine().lightLevel(state -> state.getValue(GeothermalControllerBlock.LIT) ? 12 : 0)));

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
