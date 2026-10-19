package com.comedymonkey.createaddons.compat.sable;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModDisplaySources;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.comedymonkey.createaddons.registry.ModItems;
import com.simibubi.create.api.stress.BlockStressValues;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Blocks that act on Sable physics objects (Create Aeronautics ships). Only loaded, and so only registered, when Sable
 * is installed: their block entities implement Sable's interfaces.
 */
public final class AeronauticsContent {
	/** One-block Gyroscope: keeps a ship level. Converted from Gyroscope Stabilizers' Gyroscope. */
	public static final DeferredBlock<CompactGyroscopeBlock> COMPACT_GYROSCOPE = ModBlocks.BLOCKS.register(
		"compact_gyroscope", () -> new CompactGyroscopeBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.TERRACOTTA_YELLOW)
			.strength(3.0f, 6.0f)
			.sound(SoundType.COPPER)
			.requiresCorrectToolForDrops()
			.noOcclusion()
			.isRedstoneConductor((state, level, pos) -> false)
			.isSuffocating((state, level, pos) -> false)
			.isViewBlocking((state, level, pos) -> false)));

	/** Turns a ship about its shaft, with none of a propeller's sideways push. */
	public static final DeferredBlock<TorqueRotorBlock> TORQUE_ROTOR = ModBlocks.BLOCKS.register(
		"torque_rotor", () -> new TorqueRotorBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.STONE)
			.strength(2.5f, 6.0f)
			.sound(SoundType.METAL)
			.requiresCorrectToolForDrops()
			.noOcclusion()
			.isRedstoneConductor((state, level, pos) -> false)
			.isSuffocating((state, level, pos) -> false)
			.isViewBlocking((state, level, pos) -> false)));

	/** A cockpit panel of four dials, and readings for Create's Display Links. */
	public static final DeferredBlock<FlightInstrumentsBlock> FLIGHT_INSTRUMENTS = ModBlocks.BLOCKS.register(
		"flight_instruments", () -> new FlightInstrumentsBlock(BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.strength(2.5f, 6.0f)
			.sound(SoundType.METAL)
			.requiresCorrectToolForDrops()
			// A sloping desk, not a full cube
			.noOcclusion()));

	public static final DeferredItem<BlockItem> FLIGHT_INSTRUMENTS_ITEM = ModItems.ITEMS.registerSimpleBlockItem(FLIGHT_INSTRUMENTS);

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FlightInstrumentsBlockEntity>> FLIGHT_INSTRUMENTS_BE =
		ModBlockEntities.BLOCK_ENTITY_TYPES.register("flight_instruments",
			() -> BlockEntityType.Builder.of(FlightInstrumentsBlockEntity::new, FLIGHT_INSTRUMENTS.get()).build(null));

	// One Display Link source per reading
	public static final DeferredHolder<DisplaySource, FlightDisplaySources> FLIGHT_ALTITUDE =
		ModDisplaySources.SOURCES.register("flight_altitude", () -> new FlightDisplaySources(FlightDisplaySources.ALTITUDE));
	public static final DeferredHolder<DisplaySource, FlightDisplaySources> FLIGHT_AIRSPEED =
		ModDisplaySources.SOURCES.register("flight_airspeed", () -> new FlightDisplaySources(FlightDisplaySources.AIRSPEED));
	public static final DeferredHolder<DisplaySource, FlightDisplaySources> FLIGHT_CLIMB =
		ModDisplaySources.SOURCES.register("flight_climb", () -> new FlightDisplaySources(FlightDisplaySources.CLIMB));
	public static final DeferredHolder<DisplaySource, FlightDisplaySources> FLIGHT_HEADING =
		ModDisplaySources.SOURCES.register("flight_heading", () -> new FlightDisplaySources(FlightDisplaySources.HEADING));
	public static final DeferredHolder<DisplaySource, FlightDisplaySources> FLIGHT_ATTITUDE =
		ModDisplaySources.SOURCES.register("flight_attitude", () -> new FlightDisplaySources(FlightDisplaySources.ATTITUDE));

	public static final DeferredItem<BlockItem> COMPACT_GYROSCOPE_ITEM = ModItems.ITEMS.registerSimpleBlockItem(COMPACT_GYROSCOPE);
	public static final DeferredItem<BlockItem> TORQUE_ROTOR_ITEM = ModItems.ITEMS.registerSimpleBlockItem(TORQUE_ROTOR);

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CompactGyroscopeBlockEntity>> COMPACT_GYROSCOPE_BE =
		ModBlockEntities.BLOCK_ENTITY_TYPES.register("compact_gyroscope",
			() -> BlockEntityType.Builder.of(CompactGyroscopeBlockEntity::new, COMPACT_GYROSCOPE.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TorqueRotorBlockEntity>> TORQUE_ROTOR_BE =
		ModBlockEntities.BLOCK_ENTITY_TYPES.register("torque_rotor",
			() -> BlockEntityType.Builder.of(TorqueRotorBlockEntity::new, TORQUE_ROTOR.get()).build(null));

	/** Called from the mod constructor, before the registries fire, to add the entries above. */
	public static void init() {}

	public static void addToTab(CreativeModeTab.Output output) {
		output.accept(COMPACT_GYROSCOPE_ITEM.get());
		output.accept(TORQUE_ROTOR_ITEM.get());
		output.accept(FLIGHT_INSTRUMENTS_ITEM.get());
	}

	/** From common setup's main-thread work. */
	public static void commonSetup() {
		BlockStressValues.IMPACTS.register(COMPACT_GYROSCOPE.get(), CMConfig.SERVER::gyroStressImpact);
		BlockStressValues.IMPACTS.register(TORQUE_ROTOR.get(), CMConfig.SERVER::torqueRotorStressImpact);
		for (var source : java.util.List.of(FLIGHT_ALTITUDE, FLIGHT_AIRSPEED, FLIGHT_CLIMB, FLIGHT_HEADING, FLIGHT_ATTITUDE))
			DisplaySource.BY_BLOCK.add(FLIGHT_INSTRUMENTS.get(), source.get());
	}

	private AeronauticsContent() {}
}
