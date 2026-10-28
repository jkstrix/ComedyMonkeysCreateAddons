package com.comedymonkey.createaddons.registry;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.clock.CuckooClockBlockEntity;
import com.comedymonkey.createaddons.content.chest.UpgradedBarrelBlockEntity;
import com.comedymonkey.createaddons.content.chest.UpgradedChestBlockEntity;
import com.comedymonkey.createaddons.content.cobblegen.CobbleGeneratorBlockEntity;
import com.comedymonkey.createaddons.content.crucible.PressureCrucibleBlockEntity;
import com.comedymonkey.createaddons.content.engine.MagmaEngineBlockEntity;
import com.comedymonkey.createaddons.content.harvester.AutoHarvesterBlockEntity;
import com.comedymonkey.createaddons.content.pen.AnimalPenBlockEntity;
import com.comedymonkey.createaddons.content.electric.ElectricMotorBlockEntity;
import com.comedymonkey.createaddons.content.electric.GeothermalCasingBlockEntity;
import com.comedymonkey.createaddons.content.electric.GeothermalControllerBlockEntity;
import com.comedymonkey.createaddons.content.electric.BatteryBlockEntity;
import com.comedymonkey.createaddons.content.electric.LavaGeneratorBlockEntity;
import com.comedymonkey.createaddons.content.incinerator.IncineratorBlockEntity;
import com.comedymonkey.createaddons.content.machine.MachineKind;
import com.comedymonkey.createaddons.content.machine.TwoInputMachineBlockEntity;
import com.comedymonkey.createaddons.content.wellspring.WellspringBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import com.comedymonkey.createaddons.content.tradebot.TradeBotStationBlockEntity;
import com.comedymonkey.createaddons.content.line.FluidLineBlockEntity;
import com.comedymonkey.createaddons.content.line.LinePostBlockEntity;
import com.comedymonkey.createaddons.content.line.PowerLineBlockEntity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
		DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ComedyMonkeysCreateAddons.MOD_ID);

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WellspringBlockEntity>> WELLSPRING =
		BLOCK_ENTITY_TYPES.register("wellspring",
			() -> BlockEntityType.Builder.of(WellspringBlockEntity::new, ModBlocks.WELLSPRING.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<UpgradedChestBlockEntity>> UPGRADED_CHEST =
		BLOCK_ENTITY_TYPES.register("upgraded_chest",
			() -> BlockEntityType.Builder.of(UpgradedChestBlockEntity::new, ModBlocks.UPGRADED_CHEST.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<UpgradedBarrelBlockEntity>> UPGRADED_BARREL =
		BLOCK_ENTITY_TYPES.register("upgraded_barrel",
			() -> BlockEntityType.Builder.of(UpgradedBarrelBlockEntity::new, ModBlocks.UPGRADED_BARREL.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PressureCrucibleBlockEntity>> PRESSURE_CRUCIBLE =
		BLOCK_ENTITY_TYPES.register("pressure_crucible",
			() -> BlockEntityType.Builder.of(PressureCrucibleBlockEntity::new, ModBlocks.PRESSURE_CRUCIBLE.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CobbleGeneratorBlockEntity>> COBBLE_GENERATOR =
		BLOCK_ENTITY_TYPES.register("cobble_generator",
			() -> BlockEntityType.Builder.of(CobbleGeneratorBlockEntity::new,
				ModBlocks.generators().stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AutoHarvesterBlockEntity>> AUTO_HARVESTER =
		BLOCK_ENTITY_TYPES.register("auto_harvester",
			() -> BlockEntityType.Builder.of(AutoHarvesterBlockEntity::new, ModBlocks.AUTO_HARVESTER.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CuckooClockBlockEntity>> CUCKOO_CLOCK =
		BLOCK_ENTITY_TYPES.register("cuckoo_clock",
			() -> BlockEntityType.Builder.of(CuckooClockBlockEntity::new, ModBlocks.CUCKOO_CLOCK.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.comedymonkey.createaddons.content.helperbot.HelperBotBlockEntity>> HELPER_BOT =
		BLOCK_ENTITY_TYPES.register("helper_bot",
			() -> BlockEntityType.Builder.of(com.comedymonkey.createaddons.content.helperbot.HelperBotBlockEntity::new, ModBlocks.HELPER_BOT.get()).build(null));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TradeBotStationBlockEntity>> TRADE_BOT_STATION =
		BLOCK_ENTITY_TYPES.register("trade_bot",
			() -> BlockEntityType.Builder.of(TradeBotStationBlockEntity::new, ModBlocks.TRADE_BOT.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AnimalPenBlockEntity>> ANIMAL_PEN =
		BLOCK_ENTITY_TYPES.register("animal_pen",
			() -> BlockEntityType.Builder.of(AnimalPenBlockEntity::new, ModBlocks.ANIMAL_PEN.get(),
				ModBlocks.BRASS_ANIMAL_PEN.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.comedymonkey.createaddons.content.cage.MonsterCageBlockEntity>> MONSTER_CAGE =
		BLOCK_ENTITY_TYPES.register("monster_cage",
			() -> BlockEntityType.Builder.of(com.comedymonkey.createaddons.content.cage.MonsterCageBlockEntity::new,
				ModBlocks.MONSTER_CAGE.get(), ModBlocks.BRASS_MONSTER_CAGE.get(), ModBlocks.FLOODED_MONSTER_CAGE.get(),
				ModBlocks.BRASS_FLOODED_MONSTER_CAGE.get(), ModBlocks.NETHERITE_MONSTER_CAGE.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.comedymonkey.createaddons.content.tank.FishTankBlockEntity>> FISH_TANK =
		BLOCK_ENTITY_TYPES.register("fish_tank",
			() -> BlockEntityType.Builder.of(com.comedymonkey.createaddons.content.tank.FishTankBlockEntity::new, ModBlocks.FISH_TANK.get(), ModBlocks.BRASS_FISH_TANK.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IncineratorBlockEntity>> INCINERATOR =
		BLOCK_ENTITY_TYPES.register("incinerator",
			() -> BlockEntityType.Builder.of(IncineratorBlockEntity::new, ModBlocks.INCINERATOR.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidLineBlockEntity>> FLUID_LINE =
		BLOCK_ENTITY_TYPES.register("fluid_line",
			() -> BlockEntityType.Builder.of(FluidLineBlockEntity::new, ModBlocks.COPPER_FLUID_LINE.get(),
				ModBlocks.BRASS_FLUID_LINE.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerLineBlockEntity>> POWER_LINE =
		BLOCK_ENTITY_TYPES.register("power_line",
			() -> BlockEntityType.Builder.of(PowerLineBlockEntity::new, ModBlocks.COPPER_POWER_LINE.get(),
				ModBlocks.BRASS_POWER_LINE.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LinePostBlockEntity>> LINE_POST =
		BLOCK_ENTITY_TYPES.register("line_post",
			() -> BlockEntityType.Builder.of(LinePostBlockEntity::new, ModBlocks.LINE_POST.get(), ModBlocks.CAST_IRON_LAMP_POST.get(),
				ModBlocks.STEEL_LAMP_POST.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MagmaEngineBlockEntity>> MAGMA_ENGINE =
		BLOCK_ENTITY_TYPES.register("magma_engine",
			() -> BlockEntityType.Builder.of(MagmaEngineBlockEntity::new, ModBlocks.MAGMA_ENGINE.get(),
				ModBlocks.SUPERCHARGED_MAGMA_ENGINE.get()).build(null));

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TwoInputMachineBlockEntity>> STRING_TANGLER =
		machine("string_tangler", MachineKind.TANGLING, () -> ModBlocks.STRING_TANGLER.get());

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TwoInputMachineBlockEntity>> STONE_INFUSER =
		machine("stone_infuser", MachineKind.INFUSING, () -> ModBlocks.STONE_INFUSER.get());

	@SuppressWarnings("DataFlowIssue")
	private static DeferredHolder<BlockEntityType<?>, BlockEntityType<TwoInputMachineBlockEntity>> machine(
			String id, MachineKind kind, java.util.function.Supplier<Block> block) {
		var holder = new java.util.concurrent.atomic.AtomicReference<DeferredHolder<BlockEntityType<?>, BlockEntityType<TwoInputMachineBlockEntity>>>();
		holder.set(BLOCK_ENTITY_TYPES.register(id, () -> BlockEntityType.Builder.<TwoInputMachineBlockEntity>of(
			(pos, state) -> new TwoInputMachineBlockEntity(holder.get().get(), pos, state, kind), block.get()).build(null)));
		return holder.get();
	}

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricMotorBlockEntity>> ELECTRIC_MOTOR =
		BLOCK_ENTITY_TYPES.register("electric_motor",
			() -> BlockEntityType.Builder.of(ElectricMotorBlockEntity::new, ModBlocks.ELECTRIC_MOTOR.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.comedymonkey.createaddons.content.juggernaut.WindingKeyBlockEntity>> WINDING_KEY =
		BLOCK_ENTITY_TYPES.register("winding_key",
			() -> BlockEntityType.Builder.of(com.comedymonkey.createaddons.content.juggernaut.WindingKeyBlockEntity::new, ModBlocks.WINDING_KEY.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.comedymonkey.createaddons.content.juggernaut.SpringEngineBlockEntity>> SPRING_ENGINE =
		BLOCK_ENTITY_TYPES.register("spring_engine",
			() -> BlockEntityType.Builder.of(com.comedymonkey.createaddons.content.juggernaut.SpringEngineBlockEntity::new, ModBlocks.SPRING_ENGINE.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LavaGeneratorBlockEntity>> LAVA_GENERATOR =
		BLOCK_ENTITY_TYPES.register("lava_generator",
			() -> BlockEntityType.Builder.of(LavaGeneratorBlockEntity::new, ModBlocks.LAVA_GENERATOR.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.comedymonkey.createaddons.content.light.LightBlockEntity>> LIGHT =
		BLOCK_ENTITY_TYPES.register("light",
			() -> BlockEntityType.Builder.of(com.comedymonkey.createaddons.content.light.LightBlockEntity::new,
				ModBlocks.LIGHTS.values().stream().map(net.neoforged.neoforge.registries.DeferredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.comedymonkey.createaddons.content.light.FestoonBlockEntity>> FESTOON =
		BLOCK_ENTITY_TYPES.register("festoon",
			() -> BlockEntityType.Builder.of(com.comedymonkey.createaddons.content.light.FestoonBlockEntity::new, ModBlocks.FESTOON_HOOK.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.comedymonkey.createaddons.content.light.LightSpotBlockEntity>> LIGHT_SPOT =
		BLOCK_ENTITY_TYPES.register("light_spot",
			() -> BlockEntityType.Builder.of(com.comedymonkey.createaddons.content.light.LightSpotBlockEntity::new, ModBlocks.LIGHT_SPOT.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.comedymonkey.createaddons.content.light.LightStripBlockEntity>> LIGHT_STRIP =
		BLOCK_ENTITY_TYPES.register("light_strip",
			() -> BlockEntityType.Builder.of(com.comedymonkey.createaddons.content.light.LightStripBlockEntity::new, ModBlocks.LIGHT_STRIP.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.comedymonkey.createaddons.content.light.EdgeLightBlockEntity>> EDGE_LIGHT =
		BLOCK_ENTITY_TYPES.register("edge_light",
			() -> BlockEntityType.Builder.of(com.comedymonkey.createaddons.content.light.EdgeLightBlockEntity::new, ModBlocks.EDGE_LIGHT.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryBlockEntity>> BATTERY =
		BLOCK_ENTITY_TYPES.register("battery",
			() -> BlockEntityType.Builder.of(BatteryBlockEntity::new, ModBlocks.BATTERY.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeothermalCasingBlockEntity>> GEOTHERMAL_CASING =
		BLOCK_ENTITY_TYPES.register("geothermal_casing",
			() -> BlockEntityType.Builder.of(GeothermalCasingBlockEntity::new, ModBlocks.GEOTHERMAL_CASING.get()).build(null));

	@SuppressWarnings("DataFlowIssue")
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeothermalControllerBlockEntity>> GEOTHERMAL_CONTROLLER =
		BLOCK_ENTITY_TYPES.register("geothermal_controller",
			() -> BlockEntityType.Builder.of(GeothermalControllerBlockEntity::new, ModBlocks.GEOTHERMAL_CONTROLLER.get()).build(null));

	private ModBlockEntities() {}
}
