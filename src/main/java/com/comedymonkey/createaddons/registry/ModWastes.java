package com.comedymonkey.createaddons.registry;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.comedymonkey.createaddons.content.wastes.AssemblyClampBlock;
import com.comedymonkey.createaddons.content.wastes.BrassPlaqueBlock;
import com.comedymonkey.createaddons.content.wastes.BrokenMakersWindowBlock;
import com.comedymonkey.createaddons.content.wastes.ClampBracketBlock;
import com.comedymonkey.createaddons.content.wastes.KeyMarkedStoneBlock;
import com.comedymonkey.createaddons.content.wastes.ScarredPlateBlock;
import com.comedymonkey.createaddons.content.wastes.SlottedGrilleBlock;
import com.comedymonkey.createaddons.content.wastes.VoidAlloyBandBlock;
import com.comedymonkey.createaddons.content.wastes.VoidAlloyPlateBlock;
import com.comedymonkey.createaddons.content.wastes.VoidAlloySlabBlock;
import com.comedymonkey.createaddons.content.wastes.ChargingAlcoveBlock;
import com.comedymonkey.createaddons.content.wastes.CityLocks;
import com.comedymonkey.createaddons.content.wastes.CityRecord;
import com.comedymonkey.createaddons.content.wastes.CityVaultBlock;
import com.comedymonkey.createaddons.content.wastes.CityVaultBlockEntity;
import com.comedymonkey.createaddons.content.wastes.ConduitPylonBlock;
import com.comedymonkey.createaddons.content.wastes.ConduitPylonBlockEntity;
import com.comedymonkey.createaddons.content.wastes.DerelictCableBlock;
import com.comedymonkey.createaddons.content.wastes.DerelictInsulatorBlock;
import com.comedymonkey.createaddons.content.wastes.DerelictInsulatorBlockEntity;
import com.comedymonkey.createaddons.content.wastes.DerelictLightBlockEntity;
import com.comedymonkey.createaddons.content.wastes.DerelictReactorPortBlockEntity;
import com.comedymonkey.createaddons.content.wastes.DerelictStarReactorBlock;
import com.comedymonkey.createaddons.content.wastes.DerelictStarReactorBlockEntity;
import com.comedymonkey.createaddons.content.wastes.DerelictStreetLampBlock;
import com.comedymonkey.createaddons.content.wastes.DerelictWallLampBlock;
import com.comedymonkey.createaddons.content.wastes.EndBiomes;
import com.comedymonkey.createaddons.content.wastes.LiftBlock;
import com.comedymonkey.createaddons.content.wastes.LiftBlockEntity;
import com.comedymonkey.createaddons.content.wastes.LiftCallPlateBlock;
import com.comedymonkey.createaddons.content.wastes.LiftCallPlateBlockEntity;
import com.comedymonkey.createaddons.content.wastes.LiftDeck;
import com.comedymonkey.createaddons.content.wastes.SlidingDoorBlock;
import com.comedymonkey.createaddons.content.wastes.SlidingDoorBlockEntity;
import com.comedymonkey.createaddons.content.wastes.StarReactorBlock;
import com.comedymonkey.createaddons.content.wastes.StarReactorBlockEntity;
import com.comedymonkey.createaddons.content.wastes.SubstationBlock;
import com.comedymonkey.createaddons.content.wastes.SubstationBlockEntity;
import com.comedymonkey.createaddons.content.wastes.SalvageBlock;
import com.comedymonkey.createaddons.content.wastes.ScrapHeapBlock;
import com.comedymonkey.createaddons.content.wastes.SeamedPlateBlock;
import com.comedymonkey.createaddons.content.wastes.ShellBlock;
import com.comedymonkey.createaddons.content.wastes.ThreePartBlock;
import com.comedymonkey.createaddons.content.wastes.CityVaultMenu;
import com.comedymonkey.createaddons.content.wastes.WallVentBlock;
import com.comedymonkey.createaddons.content.wastes.WallVentBlockEntity;
import com.comedymonkey.createaddons.content.wastes.bot.CityBot;
import com.comedymonkey.createaddons.content.wastes.bot.Custodian;
import com.comedymonkey.createaddons.content.wastes.bot.Scuttler;
import com.comedymonkey.createaddons.content.wastes.bot.Sentry;
import com.comedymonkey.createaddons.content.wastes.bot.StarlightBolt;
import com.comedymonkey.createaddons.content.wastes.bot.WastesBots;
import com.mojang.serialization.Codec;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.TooltipModifier;

import net.createmod.catnip.lang.FontHelper;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The Clockwork Wastes' blocks and items (design/specs/clockwork-wastes.md; progress in
 * design/specs/clockwork-wastes.progress.md), registered through {@link ModBlocks#BLOCKS} and {@link ModItems#ITEMS}.
 * Kept together so the later phases (powered city blocks, the city, the bots) add theirs here. Models, blockstates,
 * textures and loot come from design/models/clockwork-wastes.export.js.
 */
public final class ModWastes {
	// ---------------------------------------------------------------- the ground (the biome's top layer)
	public static final DeferredBlock<Block> SCORCHED_END_STONE = ground("scorched_end_stone");
	public static final DeferredBlock<Block> RUSTED_SCORCHED_END_STONE = ground("rusted_scorched_end_stone");
	public static final DeferredBlock<Block> LITTERED_SCORCHED_END_STONE = ground("littered_scorched_end_stone");

	// ---------------------------------------------------------------- the decor set (D37: drops itself)
	/** Its faces in the void-alloy bond (D75), from its position. */
	public static final DeferredBlock<VoidAlloyPlateBlock> VOID_ALLOY_PLATE = ModBlocks.BLOCKS.register("void_alloy_plate", () -> new VoidAlloyPlateBlock(voidAlloy()));
	/** Its seams light while its district is fed (a Derelict light, D37 and the brief). */
	public static final DeferredBlock<SeamedPlateBlock> SEAMED_VOID_ALLOY_PLATE = ModBlocks.BLOCKS.register("seamed_void_alloy_plate",
		() -> new SeamedPlateBlock(voidAlloy()));
	public static final DeferredBlock<RotatedPillarBlock> VOID_ALLOY_PILLAR = ModBlocks.BLOCKS.register("void_alloy_pillar",
		() -> new RotatedPillarBlock(voidAlloy().noOcclusion()));
	/** Its contact plate lights while its district is fed (a Derelict light). */
	public static final DeferredBlock<ChargingAlcoveBlock> CHARGING_ALCOVE = ModBlocks.BLOCKS.register("charging_alcove",
		() -> new ChargingAlcoveBlock(voidAlloy().noOcclusion(), new double[][][] {
			{ { 1, 0, 13, 15, 16, 16 }, { 1, 0, 4, 3, 16, 13 }, { 13, 0, 4, 15, 16, 13 }, { 3, 0, 5, 13, 2, 13 } },
			{ { 1, 0, 13, 15, 16, 16 }, { 1, 0, 4, 3, 16, 13 }, { 13, 0, 4, 15, 16, 13 } },
			{ { 1, 0, 13, 15, 15, 16 }, { 1, 0, 4, 3, 12, 13 }, { 13, 0, 4, 15, 12, 13 }, { 3, 8, 5, 13, 15, 13 } } }));
	public static final DeferredBlock<ThreePartBlock> MAKER_STATUE = ModBlocks.BLOCKS.register("maker_statue",
		() -> new ThreePartBlock(carved(), false, new double[][][] {
			{ { 1.5, 0, 1.5, 14.5, 16, 14.5 } }, { { 3, 0, 3, 13, 16, 13 } }, { { 4, 0, 4, 12, 13, 12 } } }));
	public static final DeferredBlock<ThreePartBlock> TOPPLED_MAKER_STATUE = ModBlocks.BLOCKS.register("toppled_maker_statue",
		() -> new ThreePartBlock(carved(), true, new double[][][] {
			{ { 0, 0, 1.5, 16, 13, 14.5 } }, { { 0, 0, 1.5, 16, 13, 14.5 } }, { { 0, 0, 2, 16, 8, 14 } } }));
	public static final DeferredBlock<ScrapHeapBlock> SCRAP_HEAP = ModBlocks.BLOCKS.register("scrap_heap",
		() -> new ScrapHeapBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).strength(1.5f, 3.0f)
			.sound(SoundType.CHAIN).requiresCorrectToolForDrops().noOcclusion()));

	// ---------------------------------------------------------------- the building block set (D71, D73, D75: decor, drops itself)
	public static final DeferredBlock<StairBlock> VOID_ALLOY_STAIRS = ModBlocks.BLOCKS.register("void_alloy_stairs",
		() -> new StairBlock(Blocks.STONE.defaultBlockState(), voidAlloy()));
	public static final DeferredBlock<VoidAlloySlabBlock> VOID_ALLOY_SLAB = ModBlocks.BLOCKS.register("void_alloy_slab",
		() -> new VoidAlloySlabBlock(voidAlloy()));
	public static final DeferredBlock<WallBlock> VOID_ALLOY_WALL = ModBlocks.BLOCKS.register("void_alloy_wall",
		() -> new WallBlock(voidAlloy().forceSolidOn()));
	/** Its dim pane lit while its district is fed. */
	public static final DeferredBlock<SlottedGrilleBlock> SLOTTED_GRILLE = ModBlocks.BLOCKS.register("slotted_grille",
		() -> new SlottedGrilleBlock(voidAlloy().noOcclusion()));
	/** Its seam lit (edge tone) while its district is fed. */
	public static final DeferredBlock<VoidAlloyBandBlock> VOID_ALLOY_BAND = ModBlocks.BLOCKS.register("void_alloy_band",
		() -> new VoidAlloyBandBlock(voidAlloy().noOcclusion()));
	public static final DeferredBlock<ClampBracketBlock> CLAMP_BRACKET = ModBlocks.BLOCKS.register("clamp_bracket",
		() -> new ClampBracketBlock(voidAlloy().noOcclusion()));
	/** A cracked one's crack lit (dim) while its district is fed. */
	public static final DeferredBlock<ScarredPlateBlock> SCARRED_VOID_ALLOY_PLATE = ModBlocks.BLOCKS.register("scarred_void_alloy_plate",
		() -> new ScarredPlateBlock(voidAlloy()));
	public static final DeferredBlock<Block> STAINED_END_STONE_BRICKS = ModBlocks.BLOCKS.register("stained_end_stone_bricks",
		() -> new Block(makersBrick()));
	public static final DeferredBlock<Block> CRACKED_STAINED_END_STONE_BRICKS = ModBlocks.BLOCKS.register("cracked_stained_end_stone_bricks",
		() -> new Block(makersBrick()));
	public static final DeferredBlock<StairBlock> STAINED_END_STONE_BRICK_STAIRS = ModBlocks.BLOCKS.register("stained_end_stone_brick_stairs",
		() -> new StairBlock(Blocks.END_STONE_BRICKS.defaultBlockState(), makersBrick()));
	public static final DeferredBlock<SlabBlock> STAINED_END_STONE_BRICK_SLAB = ModBlocks.BLOCKS.register("stained_end_stone_brick_slab",
		() -> new SlabBlock(makersBrick()));
	public static final DeferredBlock<WallBlock> STAINED_END_STONE_BRICK_WALL = ModBlocks.BLOCKS.register("stained_end_stone_brick_wall",
		() -> new WallBlock(makersBrick().forceSolidOn()));
	public static final DeferredBlock<KeyMarkedStoneBlock> KEY_MARKED_STONE = ModBlocks.BLOCKS.register("key_marked_stone",
		() -> new KeyMarkedStoneBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM)
			.strength(3.0f, 9.0f).requiresCorrectToolForDrops()));
	public static final DeferredBlock<BrokenMakersWindowBlock> BROKEN_MAKERS_WINDOW = ModBlocks.BLOCKS.register("broken_makers_window",
		() -> new BrokenMakersWindowBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5f, 6.0f).sound(SoundType.GLASS)
			.requiresCorrectToolForDrops().noOcclusion().isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false)));
	public static final DeferredBlock<Block> PATINA_RIB = ModBlocks.BLOCKS.register("patina_rib",
		() -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.WARPED_STEM).strength(3.0f, 6.0f).sound(SoundType.COPPER)
			.requiresCorrectToolForDrops()));

	/** The building block set (D75), in creative-tab order. */
	public static List<DeferredBlock<? extends Block>> buildingBlocks() {
		return List.of(VOID_ALLOY_STAIRS, VOID_ALLOY_SLAB, VOID_ALLOY_WALL, SLOTTED_GRILLE, VOID_ALLOY_BAND, CLAMP_BRACKET, SCARRED_VOID_ALLOY_PLATE,
			STAINED_END_STONE_BRICKS, CRACKED_STAINED_END_STONE_BRICKS, STAINED_END_STONE_BRICK_STAIRS, STAINED_END_STONE_BRICK_SLAB,
			STAINED_END_STONE_BRICK_WALL, KEY_MARKED_STONE, BROKEN_MAKERS_WINDOW, PATINA_RIB);
	}

	// ---------------------------------------------------------------- salvage (D26: wrench for parts, anything else for scrap)
	public static final DeferredBlock<SalvageBlock> BROKEN_PRESS = machine("broken_press", SalvageBlock::new);
	public static final DeferredBlock<SalvageBlock> BROKEN_MIXER = machine("broken_mixer", SalvageBlock::new);
	public static final DeferredBlock<SalvageBlock> BROKEN_DEPLOYER = machine("broken_deployer", SalvageBlock::new);
	public static final DeferredBlock<SalvageBlock> BROKEN_MECHANICAL_ARM = machine("broken_mechanical_arm",
		p -> new SalvageBlock(p, new double[][] { { 2, 0, 2, 14, 4, 14 }, { 5, 4, 5, 11, 16, 11 } }));
	public static final DeferredBlock<SalvageBlock> BROKEN_CRUSHING_WHEEL = machine("broken_crushing_wheel",
		p -> new SalvageBlock(p, new double[][] { { 0, 0, 2, 16, 8, 16 } }));
	public static final DeferredBlock<ShellBlock.Worker> WORKER_SHELL = ModBlocks.BLOCKS.register("worker_shell",
		() -> new ShellBlock.Worker(shell(MapColor.COLOR_BLACK)));
	public static final DeferredBlock<ShellBlock.Resistance> RESISTANCE_SHELL = ModBlocks.BLOCKS.register("resistance_shell",
		() -> new ShellBlock.Resistance(shell(MapColor.STONE)));
	/** The Workshops' assembly line (D72): a jig holding a Worker at one of five stages; salvaged like a shell. */
	public static final DeferredBlock<AssemblyClampBlock> ASSEMBLY_CLAMP = ModBlocks.BLOCKS.register("assembly_clamp",
		() -> new AssemblyClampBlock(shell(MapColor.COLOR_BLACK)));
	/** The makers' worn brass plaque (D72): decor, drops itself. */
	public static final DeferredBlock<BrassPlaqueBlock> BRASS_PLAQUE = ModBlocks.BLOCKS.register("brass_plaque",
		() -> new BrassPlaqueBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_YELLOW).strength(1.5f, 6.0f).sound(SoundType.METAL)
			.requiresCorrectToolForDrops().noOcclusion()));

	// ---------------------------------------------------------------- the powered city (phase 2: locks open as districts wake)
	public static final DeferredBlock<SubstationBlock> SUBSTATION = ModBlocks.BLOCKS.register("substation",
		() -> new SubstationBlock(lockable(MapColor.METAL, SubstationBlock.HARDNESS, SubstationBlock.RESISTANCE).sound(SoundType.NETHERITE_BLOCK).noOcclusion()));
	public static final DeferredBlock<CityVaultBlock> CITY_VAULT = ModBlocks.BLOCKS.register("city_vault",
		() -> new CityVaultBlock(lockable(MapColor.COLOR_BLACK, CityVaultBlock.HARDNESS, CityVaultBlock.RESISTANCE).sound(SoundType.NETHERITE_BLOCK).noOcclusion()));
	public static final DeferredBlock<SlidingDoorBlock> SLIDING_DOOR = ModBlocks.BLOCKS.register("sliding_door",
		() -> new SlidingDoorBlock(lockable(MapColor.COLOR_BLACK, SlidingDoorBlock.HARDNESS, SlidingDoorBlock.RESISTANCE).sound(SoundType.NETHERITE_BLOCK).noOcclusion()));
	public static final DeferredBlock<DerelictStreetLampBlock> DERELICT_STREET_LAMP = ModBlocks.BLOCKS.register("derelict_street_lamp",
		() -> new DerelictStreetLampBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5f, 6.0f).sound(SoundType.LANTERN)
			.requiresCorrectToolForDrops().noOcclusion().lightLevel(DerelictStreetLampBlock::lightLevel)));
	public static final DeferredBlock<DerelictWallLampBlock> DERELICT_WALL_LAMP = ModBlocks.BLOCKS.register("derelict_wall_lamp",
		() -> new DerelictWallLampBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f, 6.0f).sound(SoundType.LANTERN)
			.requiresCorrectToolForDrops().noOcclusion().lightLevel(DerelictWallLampBlock::lightLevel)));
	public static final DeferredBlock<DerelictCableBlock> DERELICT_CABLE = ModBlocks.BLOCKS.register("derelict_cable",
		() -> new DerelictCableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WARPED_STEM).strength(0.5f).sound(SoundType.COPPER)
			.noOcclusion().pushReaction(PushReaction.DESTROY)));
	public static final DeferredBlock<ConduitPylonBlock> CONDUIT_PYLON = ModBlocks.BLOCKS.register("conduit_pylon",
		() -> new ConduitPylonBlock(locked(MapColor.COLOR_BLACK).sound(SoundType.NETHERITE_BLOCK).noOcclusion()));
	public static final DeferredBlock<DerelictStarReactorBlock> DERELICT_STAR_REACTOR = ModBlocks.BLOCKS.register("derelict_star_reactor",
		() -> new DerelictStarReactorBlock(locked(MapColor.COLOR_BLACK).sound(SoundType.NETHERITE_BLOCK).noOcclusion()));
	/** The crafted one (P13): breaks like a Brass Casing and keeps its burn. */
	public static final DeferredBlock<StarReactorBlock> STAR_REACTOR = ModBlocks.BLOCKS.register("star_reactor",
		() -> new StarReactorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_YELLOW).strength(3.0f, 6.0f)
			.sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops().noOcclusion()));

	// ---------------------------------------------------------------- the lifts (P49, D40): begin
	/** A Lift: the drive at the head of a 2 x 2 shaft; its deck (an entity) rides between landings (P49). */
	public static final DeferredBlock<LiftBlock> LIFT = ModBlocks.BLOCKS.register("lift",
		() -> new LiftBlock(lockable(MapColor.COLOR_BLACK, LiftBlock.HARDNESS, LiftBlock.RESISTANCE).sound(SoundType.NETHERITE_BLOCK).noOcclusion()));
	/** A landing's call plate: calls the lift's deck to its level. */
	public static final DeferredBlock<LiftCallPlateBlock> LIFT_CALL_PLATE = ModBlocks.BLOCKS.register("lift_call_plate",
		() -> new LiftCallPlateBlock(lockable(MapColor.COLOR_BLACK, LiftCallPlateBlock.HARDNESS, LiftCallPlateBlock.RESISTANCE)
			.sound(SoundType.NETHERITE_BLOCK).noOcclusion().noCollission()));
	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LiftBlockEntity>> LIFT_BE =
		ModBlockEntities.BLOCK_ENTITY_TYPES.register("lift", () -> BlockEntityType.Builder.of(LiftBlockEntity::new, LIFT.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LiftCallPlateBlockEntity>> LIFT_CALL_PLATE_BE =
		ModBlockEntities.BLOCK_ENTITY_TYPES.register("lift_call_plate",
			() -> BlockEntityType.Builder.of(LiftCallPlateBlockEntity::new, LIFT_CALL_PLATE.get()).build(null));
	/** The deck that rides the shaft: never saved (its drive makes it), moved by its drive on both sides. */
	public static final DeferredHolder<EntityType<?>, EntityType<LiftDeck>> LIFT_DECK = ModEntities.ENTITY_TYPES.register("lift_deck",
		() -> EntityType.Builder.<LiftDeck>of(LiftDeck::new, MobCategory.MISC).sized(2f, 0.125f).noSave().noSummon().fireImmune()
			.clientTrackingRange(10).updateInterval(40).build("lift_deck"));
	// ---------------------------------------------------------------- the lifts: end

	// ---------------------------------------------------------------- the heart hall's wall vents (D57, D60): begin
	/** A wall vent: a curled Scuttler behind a louvred hatch, burst once by the Custodian's call. */
	public static final DeferredBlock<WallVentBlock> WALL_VENT = ModBlocks.BLOCKS.register("wall_vent",
		() -> new WallVentBlock(lockable(MapColor.COLOR_BLACK, WallVentBlock.HARDNESS, WallVentBlock.RESISTANCE).sound(SoundType.NETHERITE_BLOCK).noOcclusion()));
	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WallVentBlockEntity>> WALL_VENT_BE =
		ModBlockEntities.BLOCK_ENTITY_TYPES.register("wall_vent", () -> BlockEntityType.Builder.of(WallVentBlockEntity::new, WALL_VENT.get()).build(null));
	// ---------------------------------------------------------------- the wall vents: end

	// ---------------------------------------------------------------- the sky-cables (D61, D64): begin
	/** A Derelict Insulator: holds the city's sky-cable spans on its towers and masts; its neck ring lit while its district is fed. */
	public static final DeferredBlock<DerelictInsulatorBlock> DERELICT_INSULATOR = ModBlocks.BLOCKS.register("derelict_insulator",
		() -> new DerelictInsulatorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3.0f, 6.0f)
			.sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops().noOcclusion()));
	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DerelictInsulatorBlockEntity>> DERELICT_INSULATOR_BE =
		ModBlockEntities.BLOCK_ENTITY_TYPES.register("derelict_insulator",
			() -> BlockEntityType.Builder.of(DerelictInsulatorBlockEntity::new, DERELICT_INSULATOR.get()).build(null));
	// ---------------------------------------------------------------- the sky-cables: end

	// ---------------------------------------------------------------- the City Vault's screen (D46, D59 option A)
	/** The City Vault's menu: the Upgraded Chest's slots under its own void-alloy band. */
	public static final DeferredHolder<MenuType<?>, MenuType<CityVaultMenu>> CITY_VAULT_MENU =
		ModMenus.MENUS.register("city_vault", () -> IMenuTypeExtension.create(CityVaultMenu::new));

	/** The powered city's blocks (they read their district from the city's record), and the Star Reactor. */
	public static List<DeferredBlock<? extends Block>> cityBlocks() {
		return List.of(SUBSTATION, CITY_VAULT, SLIDING_DOOR, DERELICT_STREET_LAMP, DERELICT_WALL_LAMP, DERELICT_CABLE, CONDUIT_PYLON,
			DERELICT_STAR_REACTOR, STAR_REACTOR, LIFT, LIFT_CALL_PLATE, WALL_VENT, DERELICT_INSULATOR);
	}

	// ---------------------------------------------------------------- their block entities
	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SubstationBlockEntity>> SUBSTATION_BE =
		ModBlockEntities.BLOCK_ENTITY_TYPES.register("substation", () -> BlockEntityType.Builder.of(SubstationBlockEntity::new, SUBSTATION.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CityVaultBlockEntity>> CITY_VAULT_BE =
		ModBlockEntities.BLOCK_ENTITY_TYPES.register("city_vault", () -> BlockEntityType.Builder.of(CityVaultBlockEntity::new, CITY_VAULT.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SlidingDoorBlockEntity>> SLIDING_DOOR_BE =
		ModBlockEntities.BLOCK_ENTITY_TYPES.register("sliding_door", () -> BlockEntityType.Builder.of(SlidingDoorBlockEntity::new, SLIDING_DOOR.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DerelictLightBlockEntity>> DERELICT_LIGHT_BE =
		ModBlockEntities.BLOCK_ENTITY_TYPES.register("derelict_light", () -> BlockEntityType.Builder.of(DerelictLightBlockEntity::new,
			DERELICT_STREET_LAMP.get(), DERELICT_WALL_LAMP.get(), DERELICT_CABLE.get(), SEAMED_VOID_ALLOY_PLATE.get(), CHARGING_ALCOVE.get(),
			SLOTTED_GRILLE.get(), VOID_ALLOY_BAND.get(), SCARRED_VOID_ALLOY_PLATE.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConduitPylonBlockEntity>> CONDUIT_PYLON_BE =
		ModBlockEntities.BLOCK_ENTITY_TYPES.register("conduit_pylon", () -> BlockEntityType.Builder.of(ConduitPylonBlockEntity::new, CONDUIT_PYLON.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DerelictStarReactorBlockEntity>> DERELICT_STAR_REACTOR_BE =
		ModBlockEntities.BLOCK_ENTITY_TYPES.register("derelict_star_reactor",
			() -> BlockEntityType.Builder.of(DerelictStarReactorBlockEntity::new, DERELICT_STAR_REACTOR.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DerelictReactorPortBlockEntity>> DERELICT_REACTOR_PORT_BE =
		ModBlockEntities.BLOCK_ENTITY_TYPES.register("derelict_reactor_port",
			() -> BlockEntityType.Builder.of(DerelictReactorPortBlockEntity::new, DERELICT_STAR_REACTOR.get()).build(null));
	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StarReactorBlockEntity>> STAR_REACTOR_BE =
		ModBlockEntities.BLOCK_ENTITY_TYPES.register("star_reactor", () -> BlockEntityType.Builder.of(StarReactorBlockEntity::new, STAR_REACTOR.get()).build(null));

	// ---------------------------------------------------------------- data on items
	/** On a Star Reactor: the ticks of running its star has left (P14). */
	public static final Supplier<DataComponentType<Long>> STAR_BURN = ModDataComponents.DATA_COMPONENTS.registerComponentType("star_burn",
		builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
	/** On a Star Reactor: an Unknown Device is fitted (P44). */
	public static final Supplier<DataComponentType<Boolean>> REACTOR_OVERHAULED = ModDataComponents.DATA_COMPONENTS.registerComponentType("reactor_overhauled",
		builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

	/** Every salvage block, for the tests and the city. */
	public static List<DeferredBlock<? extends SalvageBlock>> salvage() {
		return List.of(BROKEN_PRESS, BROKEN_MIXER, BROKEN_DEPLOYER, BROKEN_MECHANICAL_ARM, BROKEN_CRUSHING_WHEEL, WORKER_SHELL, RESISTANCE_SHELL, ASSEMBLY_CLAMP);
	}

	/** Every block here, in creative-tab order (each has a BlockItem, so Carry On can carry it). */
	public static List<DeferredBlock<? extends Block>> blocks() {
		List<DeferredBlock<? extends Block>> all = new java.util.ArrayList<>(List.of(SCORCHED_END_STONE, RUSTED_SCORCHED_END_STONE, LITTERED_SCORCHED_END_STONE, VOID_ALLOY_PLATE,
			SEAMED_VOID_ALLOY_PLATE, VOID_ALLOY_PILLAR, CHARGING_ALCOVE, MAKER_STATUE, TOPPLED_MAKER_STATUE, SCRAP_HEAP, BRASS_PLAQUE,
			BROKEN_PRESS, BROKEN_MIXER, BROKEN_DEPLOYER, BROKEN_MECHANICAL_ARM, BROKEN_CRUSHING_WHEEL, WORKER_SHELL, RESISTANCE_SHELL, ASSEMBLY_CLAMP,
			SUBSTATION, CITY_VAULT, SLIDING_DOOR, DERELICT_STREET_LAMP, DERELICT_WALL_LAMP, DERELICT_CABLE, CONDUIT_PYLON,
			DERELICT_STAR_REACTOR, STAR_REACTOR, LIFT, LIFT_CALL_PLATE, WALL_VENT, DERELICT_INSULATOR));
		// the building block set after the Pillar (the void-alloy family together, then the makers' masonry)
		all.addAll(all.indexOf(VOID_ALLOY_PILLAR) + 1, buildingBlocks());
		return List.copyOf(all);
	}

	// ---------------------------------------------------------------- items
	/** The salvage material (P15): crushes into nuggets; the Star Reactor and the next End biomes use it. */
	public static final DeferredItem<Item> CLOCKWORK_SCRAP = ModItems.ITEMS.registerSimpleItem("clockwork_scrap");
	/** The rare intact tech of unknown make (P12): seven a city, from vaults and the Custodian. */
	public static final DeferredItem<Item> UNKNOWN_DEVICE = ModItems.ITEMS.registerSimpleItem("unknown_device",
		new Item.Properties().stacksTo(16).rarity(Rarity.RARE));
	public static final List<DeferredItem<BlockItem>> BLOCK_ITEMS = blocks().stream()
		.map(b -> ModItems.ITEMS.registerSimpleBlockItem(b)).toList();

	// ---------------------------------------------------------------- the bots (phase 3: P18, P19, P32-P42)
	/** About half a block (P18): the brief's 12 px across its legs, 8 px tall. Climbs walls, leaps. */
	public static final DeferredHolder<EntityType<?>, EntityType<Scuttler>> SCUTTLER = ModEntities.ENTITY_TYPES.register("scuttler",
		() -> EntityType.Builder.of(Scuttler::new, MobCategory.MONSTER).sized(0.75f, 0.5f).clientTrackingRange(8).build("scuttler"));
	/** One block (brief), fixed to a wall or floor (P19). */
	public static final DeferredHolder<EntityType<?>, EntityType<Sentry>> SENTRY = ModEntities.ENTITY_TYPES.register("sentry",
		() -> EntityType.Builder.of(Sentry::new, MobCategory.MONSTER).sized(1f, 1f).clientTrackingRange(8).build("sentry"));
	/** About 3 blocks tall (P20), 20 px across the shoulders and its forearms beside them (brief); fire can't touch it (P38). */
	public static final DeferredHolder<EntityType<?>, EntityType<Custodian>> CUSTODIAN = ModEntities.ENTITY_TYPES.register("custodian",
		() -> EntityType.Builder.of(Custodian::new, MobCategory.MONSTER).sized(1.4f, 3f).fireImmune().clientTrackingRange(10)
			.updateInterval(1).build("custodian"));
	/** The Sentries' and the Custodian's starlight bolt. */
	public static final DeferredHolder<EntityType<?>, EntityType<StarlightBolt>> STARLIGHT_BOLT = ModEntities.ENTITY_TYPES.register("starlight_bolt",
		() -> EntityType.Builder.<StarlightBolt>of(StarlightBolt::new, MobCategory.MISC).sized(0.25f, 0.25f).clientTrackingRange(6)
			.updateInterval(10).build("starlight_bolt"));

	/** Dev and creative only: the city places its bots (P6). Void-alloy with starlight spots. */
	public static final DeferredItem<DeferredSpawnEggItem> SCUTTLER_SPAWN_EGG = ModItems.ITEMS.register("scuttler_spawn_egg",
		() -> new DeferredSpawnEggItem(SCUTTLER, 0x1d1c25, 0x7f76a8, new Item.Properties()));
	public static final DeferredItem<DeferredSpawnEggItem> SENTRY_SPAWN_EGG = ModItems.ITEMS.register("sentry_spawn_egg",
		() -> new DeferredSpawnEggItem(SENTRY, 0x25232f, 0xb8b0d8, new Item.Properties()));
	public static final DeferredItem<DeferredSpawnEggItem> CUSTODIAN_SPAWN_EGG = ModItems.ITEMS.register("custodian_spawn_egg",
		() -> new DeferredSpawnEggItem(CUSTODIAN, 0x16151c, 0xb5452f, new Item.Properties()));

	private ModWastes() {}

	/** Loads the class, so everything above is in the registers before they're handed to the mod bus. */
	public static void init() {}

	/** Common setup, on the main thread: the End biome slots and the Create-style tooltips. */
	public static void commonSetup() {
		EndBiomes.register();
		for (Item item : List.of(CLOCKWORK_SCRAP.get(), UNKNOWN_DEVICE.get()))
			TooltipModifier.REGISTRY.register(item, new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE));
		for (var block : salvage())
			TooltipModifier.REGISTRY.register(block.asItem(), new ItemDescription.Modifier(block.asItem(), FontHelper.Palette.STANDARD_CREATE));
		for (var block : cityBlocks())
			TooltipModifier.REGISTRY.register(block.asItem(), new ItemDescription.Modifier(block.asItem(), FontHelper.Palette.STANDARD_CREATE));
	}

	/** The game-bus listeners: the city records' once-a-second look (a heart star burning out), and forgetting loaded blocks. */
	public static void registerEvents() {
		NeoForge.EVENT_BUS.addListener(CityRecord::onLevelTick);
		// the bots hear their cities (P8, P9), and never change a block (P37)
		WastesBots.register();
		NeoForge.EVENT_BUS.addListener(CityBot::onMobGriefing);
		NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> CityRecord.clear());
	}

	/**
	 * What the powered city's blocks offer: a Substation takes FE on any side of either half (a Battery beside it, a
	 * line at its back or underneath); a vault's items once it's open; stars into the reactors from any side (the
	 * Derelict one through any of its blocks); FE out of the Star Reactor's terminal, and of the Derelict one's ports.
	 */
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, SUBSTATION_BE.get(), (be, side) -> be.energy());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CITY_VAULT_BE.get(), (be, side) -> be.items());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, STAR_REACTOR_BE.get(), (be, side) -> be.items());
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, STAR_REACTOR_BE.get(),
			(be, side) -> side == null || side == be.getBlockState().getValue(StarReactorBlock.FACING) ? be.energyOut() : null);
		event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, be, side) -> {
			DerelictStarReactorBlockEntity controller = DerelictStarReactorBlock.controllerAt(level, pos);
			return controller == null || controller.isSalvaged() ? null : controller.items();
		}, DERELICT_STAR_REACTOR.get());
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DERELICT_REACTOR_PORT_BE.get(), (be, side) -> be.energy());
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, DERELICT_STAR_REACTOR_BE.get(),
			(be, side) -> side == null && !be.isSalvaged() ? be.energyOut() : null);
	}

	public static void addToTab(CreativeModeTab.Output output) {
		output.accept(CLOCKWORK_SCRAP.get());
		output.accept(UNKNOWN_DEVICE.get());
		BLOCK_ITEMS.forEach(item -> output.accept(item.get()));
		output.accept(SCUTTLER_SPAWN_EGG.get());
		output.accept(SENTRY_SPAWN_EGG.get());
		output.accept(CUSTODIAN_SPAWN_EGG.get());
	}

	/** The bots' attributes (mod bus). */
	public static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(SCUTTLER.get(), Scuttler.createAttributes().build());
		event.put(SENTRY.get(), Sentry.createAttributes().build());
		event.put(CUSTODIAN.get(), Custodian.createAttributes().build());
	}

	private static DeferredBlock<Block> ground(String id) {
		return ModBlocks.BLOCKS.register(id, () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.SAND)
			.instrument(NoteBlockInstrument.BASEDRUM).strength(3.0f, 9.0f).requiresCorrectToolForDrops()));
	}

	/** The makers' masonry (Stained End Stone Bricks): as vanilla's End Stone Bricks. */
	private static BlockBehaviour.Properties makersBrick() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM).strength(3.0f, 9.0f)
			.requiresCorrectToolForDrops();
	}

	private static BlockBehaviour.Properties voidAlloy() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(5.0f, 9.0f)
			.sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops();
	}

	/**
	 * A locked city block: bedrock's hardness and blast resistance, so contraptions and pistons pass it by; players break
	 * it at its own hardness once its lock opens (CityLocks).
	 */
	private static BlockBehaviour.Properties locked(MapColor color) {
		return BlockBehaviour.Properties.of().mapColor(color).strength(-1.0f, CityLocks.LOCKED_RESISTANCE).requiresCorrectToolForDrops()
			.pushReaction(PushReaction.BLOCK).isValidSpawn((state, level, pos, type) -> false);
	}

	/**
	 * A city block that locks until its district wakes (D42): its own hardness, which reads as bedrock's while its state
	 * is locked (CityLocks.Lockable), so contraptions and pistons pass it by until then and treat it as any block after.
	 */
	private static BlockBehaviour.Properties lockable(MapColor color, float hardness, float resistance) {
		return BlockBehaviour.Properties.of().mapColor(color).strength(hardness, resistance).requiresCorrectToolForDrops()
			.isValidSpawn((state, level, pos, type) -> false);
	}

	private static BlockBehaviour.Properties carved() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM)
			.strength(3.0f, 9.0f).requiresCorrectToolForDrops().noOcclusion();
	}

	private static BlockBehaviour.Properties shell(MapColor color) {
		return BlockBehaviour.Properties.of().mapColor(color).strength(2.0f, 6.0f).sound(SoundType.NETHERITE_BLOCK)
			.requiresCorrectToolForDrops().noOcclusion();
	}

	private static DeferredBlock<SalvageBlock> machine(String id, Function<BlockBehaviour.Properties, SalvageBlock> make) {
		return ModBlocks.BLOCKS.register(id, () -> make.apply(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
			.strength(3.0f, 6.0f).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
	}
}
