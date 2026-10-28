package com.comedymonkey.createaddons;

import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.compat.curios.AviatorGogglesCurios;
import com.comedymonkey.createaddons.compat.pipez.PipezBlazeBurnerCompat;
import com.comedymonkey.createaddons.compat.pipez.PipezEngineCompat;
import com.comedymonkey.createaddons.compat.sable.AeronauticsContent;
import com.comedymonkey.createaddons.compat.sable.SableCompat;
import com.comedymonkey.createaddons.content.clock.CuckooClocks;
import com.comedymonkey.createaddons.content.cobblegen.CobbleGeneratorBlock;
import com.comedymonkey.createaddons.content.engine.MagmaEngineBlock;
import com.comedymonkey.createaddons.content.MachineEnchantments;
import com.comedymonkey.createaddons.content.pen.AnimalCapture;
import com.comedymonkey.createaddons.content.incinerator.IncineratorBlock;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.comedymonkey.createaddons.content.chest.ChestEvents;
import com.comedymonkey.createaddons.content.chest.ChestUpgrades;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.network.ContraptionBlockSyncPayload;
import com.comedymonkey.createaddons.network.QuickTransferPayload;
import com.comedymonkey.createaddons.network.SortChestPayload;
import com.comedymonkey.createaddons.client.ClientContraptionSync;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModCreativeTabs;
import com.comedymonkey.createaddons.registry.ModDisplaySources;
import com.comedymonkey.createaddons.registry.ModItems;
import com.comedymonkey.createaddons.registry.ModMenus;
import com.comedymonkey.createaddons.registry.ModMountedStorage;
import com.comedymonkey.createaddons.registry.ModRecipes;
import com.comedymonkey.createaddons.registry.ModSounds;
import com.comedymonkey.createaddons.registry.ModEntities;
import com.comedymonkey.createaddons.registry.ModDataComponents;
import com.comedymonkey.createaddons.content.tradebot.TradeBotCommands;
import com.comedymonkey.createaddons.content.tradebot.TradeBotPicking;
import com.comedymonkey.createaddons.gametest.TradeBotGameTests;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.TooltipModifier;

import net.createmod.catnip.lang.FontHelper;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import com.comedymonkey.createaddons.compat.pipez.cover.PipeCoverEvents;
import com.comedymonkey.createaddons.compat.pipez.cover.client.PipeCoverClient;
import com.comedymonkey.createaddons.network.PipeCoverPayload;
import com.comedymonkey.createaddons.network.MotorSettingsPayload;
import com.comedymonkey.createaddons.network.TurbineSettingsPayload;
import com.comedymonkey.createaddons.network.SplitterInfoPayload;
import com.comedymonkey.createaddons.network.SplitterSettingsPayload;
import com.comedymonkey.createaddons.network.TradeBotStationPayload;
import com.comedymonkey.createaddons.content.electric.ElectricMotorBlock;
import com.comedymonkey.createaddons.content.electric.LavaGeneratorBlock;
import com.comedymonkey.createaddons.registry.ModAttachments;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@Mod(ComedyMonkeysCreateAddons.MOD_ID)
public class ComedyMonkeysCreateAddons {
	public static final String MOD_ID = "comedymonkeysaddons";
	public static final String AERONAUTICS_ID = "aeronautics";
	public static final String CURIOS_ID = "curios";

	public ComedyMonkeysCreateAddons(IEventBus modBus, ModContainer container) {
		// Adds its blocks to the registers below, so it has to come first
		if (SableCompat.LOADED)
			AeronauticsContent.init();
		ModBlocks.BLOCKS.register(modBus);
		ModItems.ITEMS.register(modBus);
		ModBlockEntities.BLOCK_ENTITY_TYPES.register(modBus);
		ModCreativeTabs.TABS.register(modBus);
		ModMenus.MENUS.register(modBus);
		ModMountedStorage.ITEM_STORAGE_TYPES.register(modBus);
		ModMountedStorage.FLUID_STORAGE_TYPES.register(modBus);
		ModRecipes.TYPES.register(modBus);
		ModRecipes.SERIALIZERS.register(modBus);
		ModDisplaySources.SOURCES.register(modBus);
		ModAdvancements.TRIGGERS.register(modBus);
		ModAttachments.ATTACHMENT_TYPES.register(modBus);
		ModSounds.SOUND_EVENTS.register(modBus);
		ModEntities.ENTITY_TYPES.register(modBus);
		com.comedymonkey.createaddons.registry.ModStructures.TYPES.register(modBus);
		com.comedymonkey.createaddons.registry.ModStructures.PIECES.register(modBus);
		ModDataComponents.DATA_COMPONENTS.register(modBus);
		modBus.addListener(ModEntities::registerAttributes);
		// Trade Bot's test courses (run with ./gradlew runGameTestServer, or /test in a dev world)
		modBus.addListener(TradeBotGameTests::register);
		modBus.addListener(com.comedymonkey.createaddons.gametest.StorageGameTests::register);
		modBus.addListener(com.comedymonkey.createaddons.gametest.MachineGameTests::register);
		modBus.addListener(com.comedymonkey.createaddons.gametest.LineGameTests::register);
		modBus.addListener(com.comedymonkey.createaddons.gametest.LightGameTests::register);
		modBus.addListener(com.comedymonkey.createaddons.gametest.BatteryGameTests::register);
		modBus.addListener(com.comedymonkey.createaddons.gametest.TankGameTests::register);
		modBus.addListener(com.comedymonkey.createaddons.gametest.CagePerformanceGameTests::register);
		modBus.addListener(com.comedymonkey.createaddons.gametest.CageFieldGameTests::register);
		modBus.addListener(com.comedymonkey.createaddons.gametest.HelperBotGameTests::register);
		modBus.addListener(com.comedymonkey.createaddons.gametest.JuggernautGameTests::register);
		if (SableCompat.LOADED)
			modBus.addListener(com.comedymonkey.createaddons.gametest.SableGameTests::register);
		NeoForge.EVENT_BUS.addListener(TradeBotCommands::register);
		NeoForge.EVENT_BUS.addListener(com.comedymonkey.createaddons.content.juggernaut.ClockworkJuggernaut::onMobGriefing);
		// Helper Bot's test courses (/helperbot testsetup), and the follow-up once Create has registered their stations
		// Helper Bot's test railway and shop: the command is a dev tool (testtrain go gives every train a schedule), so
		// not in releases; its queue runs everywhere, for the game tests
		// ...and the Electric Lights' test street (/lights testsetup), likewise a dev tool
		if (!net.neoforged.fml.loading.FMLEnvironment.production) {
			NeoForge.EVENT_BUS.addListener(com.comedymonkey.createaddons.content.helperbot.HelperBotTestSetup::register);
			NeoForge.EVENT_BUS.addListener(com.comedymonkey.createaddons.content.light.LightTestSetup::register);
		}
		NeoForge.EVENT_BUS.addListener(com.comedymonkey.createaddons.content.helperbot.HelperBotTestSetup::onLevelTick);
		NeoForge.EVENT_BUS.addListener(com.comedymonkey.createaddons.content.helperbot.HelperBotTestSetup::onServerStopped);
		// picking containers for Trade Bot: the server takes over the picker's clicks on blocks
		NeoForge.EVENT_BUS.addListener(TradeBotPicking::onRightClickBlock);
		NeoForge.EVENT_BUS.addListener(TradeBotPicking::onLeftClickBlock);
		NeoForge.EVENT_BUS.addListener(TradeBotPicking::onLogout);
		NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> TradeBotPicking.clear());
		NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> com.comedymonkey.createaddons.content.tradebot.trade.VillagerClaims.clear());

		// Both specs show up in Create's "Access configs of other mods" screen
		container.registerConfig(ModConfig.Type.SERVER, CMConfig.SERVER_SPEC);
		container.registerConfig(ModConfig.Type.CLIENT, CMConfig.CLIENT_SPEC);

		NeoForge.EVENT_BUS.addListener(ChestEvents::onRightClickBlock);
		NeoForge.EVENT_BUS.addListener(ChestEvents::onBlockBreak);
		// Bountiful and Fruitful books from Librarians
		NeoForge.EVENT_BUS.addListener(MachineEnchantments::addTrades);
		// Capture Bottles catch animals for the Animal Pens
		NeoForge.EVENT_BUS.addListener(AnimalCapture::onEntityInteract);
		// Iron and Netherite Jars catch monsters for the Monster Cages
		NeoForge.EVENT_BUS.addListener(com.comedymonkey.createaddons.content.cage.MonsterCapture::onEntityInteract);
		// fish follow Fish Food
		NeoForge.EVENT_BUS.addListener(com.comedymonkey.createaddons.content.tank.FishFood::onEntityJoin);
		NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> CuckooClocks.clear());
		NeoForge.EVENT_BUS.addListener(com.comedymonkey.createaddons.content.line.FluidLines::onLevelTick);
		NeoForge.EVENT_BUS.addListener(com.comedymonkey.createaddons.content.line.PowerLines::onLevelTick);
		NeoForge.EVENT_BUS.addListener(com.comedymonkey.createaddons.content.line.FluidLines::onChunkUnload);
		NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> com.comedymonkey.createaddons.content.line.PowerLines.clear());
		NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> com.comedymonkey.createaddons.content.line.FluidLines.clear());
		NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> ChestUpgrades.clearJustPlaced());
		if (ModList.get().isLoaded(PipezBlazeBurnerCompat.PIPEZ_ID)) {
			NeoForge.EVENT_BUS.addListener(PipeCoverEvents::onRightClickBlock);
			NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, PipeCoverEvents::onBlockBreak);
			NeoForge.EVENT_BUS.addListener(PipeCoverEvents::onChunkSent);
		}
		modBus.addListener(this::registerCapabilities);
		modBus.addListener(this::commonSetup);
		modBus.addListener(this::registerPayloads);

		// Aviator's Goggles in the Curios head slot: needs both mods, and neither is required
		if (ModList.get().isLoaded(AERONAUTICS_ID) && ModList.get().isLoaded(CURIOS_ID))
			AviatorGogglesCurios.init();
	}

	/** The face a cage's shaft goes into, which takes nothing else. */
	private static boolean isBack(net.minecraft.world.level.block.state.BlockState state, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
		var facing = net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
		return side != null && state.hasProperty(facing) && side == state.getValue(facing).getOpposite();
	}

	private void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.WELLSPRING.get(),
			(be, side) -> be.getTank());
		// Automation (hoppers, funnels, Pipez...) fills upgraded chests past 64 per slot, and a double chest as one
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.UPGRADED_CHEST.get(),
			(be, side) -> be.getAutomationHandler());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.UPGRADED_BARREL.get(),
			(be, side) -> be.getItemHandler());
		// Cobblestone out of the generators, from any side
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.COBBLE_GENERATOR.get(),
			(be, side) -> be.getOutputHandler());
		// The harvest out from any side; a seed or sapling in, while it's empty
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.AUTO_HARVESTER.get(),
			(be, side) -> be.getAutomationHandler());
		// Food in and drops out from any side; milk out to pipes
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.ANIMAL_PEN.get(),
			(be, side) -> be.getAutomationHandler());
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.ANIMAL_PEN.get(),
			(be, side) -> be.getMilkHandler());
		// Monster Chow in and drops out from any side; water (Flooded) and FE (Netherite) on every side but the back
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.MONSTER_CAGE.get(),
			(be, side) -> be.getAutomationHandler());
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.MONSTER_CAGE.get(),
			(be, side) -> be.isFlooded() && !isBack(be.getBlockState(), side) ? be.getWaterHandler() : null);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.MONSTER_CAGE.get(),
			(be, side) -> be.isNetherite() && !isBack(be.getBlockState(), side) ? be.getEnergy() : null);
		// Fish Food in and drops out from any side; water on every side but the back
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.FISH_TANK.get(),
			(be, side) -> be.getAutomationHandler());
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.FISH_TANK.get(),
			(be, side) -> !isBack(be.getBlockState(), side) ? be.getWaterHandler() : null);
		// Trade Bot's station takes FE from underneath only (D16)
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.TRADE_BOT_STATION.get(),
			(be, side) -> be.getEnergy(side));
		// Dust in from any side, results out from any side
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.PRESSURE_CRUCIBLE.get(),
			(be, side) -> be.getAutomationHandler());
			// Both ingredients in from any side, the result out
			event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.STRING_TANGLER.get(),
				(be, side) -> be.getAutomationHandler());
			event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.STONE_INFUSER.get(),
				(be, side) -> be.getAutomationHandler());
			// Anything goes in, nothing comes out
			event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.INCINERATOR.get(),
				(be, side) -> be.getItemHandler());
			event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.INCINERATOR.get(),
				(be, side) -> be.getFluidHandler());
			// Lava in on every side but the front, where the shaft is
			event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.MAGMA_ENGINE.get(),
				(be, side) -> side == null || MagmaEngineBlock.isInlet(be.getBlockState(), side) ? be.getTank() : null);

		// Electricity: the motor takes FE through its base (opposite the shaft); the generator takes lava at the back and
		// gives FE at the sides; the turbine takes lava and water and gives FE through its controller or any of its casings
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.ELECTRIC_MOTOR.get(),
			(be, side) -> side == null || ElectricMotorBlock.isInput(be.getBlockState(), side) ? be.getEnergy() : null);
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.LAVA_GENERATOR.get(),
			(be, side) -> side == null || side == LavaGeneratorBlock.inlet(be.getBlockState()) ? be.getFluidsOut() : null);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.LAVA_GENERATOR.get(),
			(be, side) -> side == null || LavaGeneratorBlock.isTerminal(be.getBlockState(), side) ? be.getEnergyOut() : null);
		// A Battery takes and gives on its four sides; an Industrial Battery only through its insulators, to the network
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.BATTERY.get(), (be, side) -> be.energy(side));
		// An Electric Light takes power from a line on any side, including one hidden in the block it's fixed to (D11)
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.LIGHT.get(), (be, side) -> be.energy());
		// A Light Strip takes power for its whole run, through whichever piece a line touches (P7)
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.LIGHT_STRIP.get(), (be, side) -> be.runEnergy());
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.EDGE_LIGHT.get(), (be, side) -> be.runEnergy());
		// The turbine connects only through the port flanges under its base (the bottom faces of its bottom casings);
		// the controller, in the middle of the front, offers nothing to a side
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.GEOTHERMAL_CONTROLLER.get(),
			(be, side) -> be.isFormed() && side == null ? be.getFluids() : null);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.GEOTHERMAL_CONTROLLER.get(),
			(be, side) -> be.isFormed() && side == null ? be.getEnergyOut() : null);
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.GEOTHERMAL_CASING.get(),
			(be, side) -> be.port(side) != null ? be.port(side).getFluids() : null);
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlockEntities.GEOTHERMAL_CASING.get(),
			(be, side) -> be.port(side) != null ? be.port(side).getEnergyOut() : null);

		// Optional: without Pipez nothing is registered and Blaze Burners behave exactly as in Create
		if (ModList.get().isLoaded(PipezBlazeBurnerCompat.PIPEZ_ID)) {
			PipezBlazeBurnerCompat.registerCapabilities(event);
			// Lava and other fuel fluids into Simulated's (Create Aeronautics') Portable Engines
			if (ModList.get().isLoaded(PipezEngineCompat.SIMULATED_ID))
				PipezEngineCompat.registerCapabilities(event);
		}
	}

	private void registerPayloads(RegisterPayloadHandlersEvent event) {
		// Only handled on the client; the handler class is not loaded on dedicated servers
		event.registrar("1").playToClient(ContraptionBlockSyncPayload.TYPE, ContraptionBlockSyncPayload.STREAM_CODEC,
			(payload, context) -> context.enqueueWork(() -> ClientContraptionSync.handle(payload)));
		// The Sort button on upgraded chests and barrels
		event.registrar("1").playToServer(SortChestPayload.TYPE, SortChestPayload.STREAM_CODEC, SortChestPayload::handle);
		event.registrar("1").playToServer(QuickTransferPayload.TYPE, QuickTransferPayload.STREAM_CODEC,
			QuickTransferPayload::handle);
		// The Electric Motor's settings screen
		event.registrar("1").playToServer(MotorSettingsPayload.TYPE, MotorSettingsPayload.STREAM_CODEC, MotorSettingsPayload::handle);
		event.registrar("1").playToServer(TurbineSettingsPayload.TYPE, TurbineSettingsPayload.STREAM_CODEC, TurbineSettingsPayload::handle);
		event.registrar("1").playToServer(SplitterSettingsPayload.TYPE, SplitterSettingsPayload.STREAM_CODEC, SplitterSettingsPayload::handle);
		event.registrar("1").playToClient(SplitterInfoPayload.TYPE, SplitterInfoPayload.STREAM_CODEC, SplitterInfoPayload::handle);
		// Helper Bot: what his train is doing that clients can't see (paused, a player driving, lost, derailed)
		event.registrar("1").playToClient(com.comedymonkey.createaddons.network.HelperBotStatusPayload.TYPE,
			com.comedymonkey.createaddons.network.HelperBotStatusPayload.STREAM_CODEC,
			(payload, context) -> context.enqueueWork(() -> com.comedymonkey.createaddons.client.helperbot.HelperBotClient.applyStatus(payload)));
		event.registrar("1").playToServer(TradeBotStationPayload.TYPE, TradeBotStationPayload.STREAM_CODEC, TradeBotStationPayload::handle);
		// Covers on Pipez pipes
		event.registrar("1").playToClient(PipeCoverPayload.TYPE, PipeCoverPayload.STREAM_CODEC,
			(payload, context) -> context.enqueueWork(() -> PipeCoverClient.handle(payload)));
	}

	private void commonSetup(FMLCommonSetupEvent event) {
		// a hanging lamp moves with the ceiling it hangs from, its cable and all, and goes back after it (Create contraptions)
		com.simibubi.create.api.contraption.BlockMovementChecks.registerAttachedCheck((state, world, pos, dir) -> {
			if (state.getBlock() instanceof com.comedymonkey.createaddons.content.light.HangingCableBlock)
				return com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult.of(dir.getAxis() == net.minecraft.core.Direction.Axis.Y);
			if (state.getBlock() instanceof com.comedymonkey.createaddons.content.light.HangingLampBlock && dir == net.minecraft.core.Direction.UP)
				return com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult.SUCCESS;
			return com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult.PASS;
		});
		com.simibubi.create.api.contraption.BlockMovementChecks.registerBrittleCheck(state ->
			state.getBlock() instanceof com.comedymonkey.createaddons.content.light.HangingCableBlock || state.getBlock() instanceof com.comedymonkey.createaddons.content.light.HangingLampBlock
				? com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult.SUCCESS
				: com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult.PASS);
		event.enqueueWork(() -> {
			// Create-style "Hold [Shift] for Summary" tooltips, read from the lang file
			registerCreateTooltip(ModBlocks.WELLSPRING.asItem());
			registerCreateTooltip(ModItems.COPPER_FLUID_LINE.get());
			registerCreateTooltip(ModItems.BRASS_FLUID_LINE.get());
			registerCreateTooltip(ModItems.INTAKE.get());
			registerCreateTooltip(ModItems.COPPER_POWER_LINE.get());
			registerCreateTooltip(ModItems.BRASS_POWER_LINE.get());
			registerCreateTooltip(ModItems.LINE_POST.get());
			registerCreateTooltip(ModItems.VALVE.get());
			registerCreateTooltip(ModItems.FILTER.get());
			registerCreateTooltip(ModItems.METER.get());
			registerCreateTooltip(ModItems.LIGHT_SWITCH.get());
			registerCreateTooltip(ModItems.LIGHT_STRIP.get());
			registerCreateTooltip(ModItems.EDGE_LIGHT.get());
			registerCreateTooltip(ModItems.CAST_IRON_LAMP_POST.get());
			registerCreateTooltip(ModItems.STEEL_LAMP_POST.get());
			registerCreateTooltip(ModItems.SPLITTER.get());
			registerCreateTooltip(ModItems.BREAKER.get());
			registerCreateTooltip(ModItems.UPGRADE_TEMPLATE.get());
			registerCreateTooltip(ModItems.DRIP_TEMPLATE.get());
			registerCreateTooltip(ModItems.WATER_UPGRADE.get());
			registerCreateTooltip(ModItems.DRIPSTONE_UPGRADE.get());
			registerCreateTooltip(ModItems.HONEY_UPGRADE.get());
			registerCreateTooltip(ModItems.CHOCOLATE_UPGRADE.get());
			registerCreateTooltip(ModItems.CAPACITY_UPGRADE.get());
			registerCreateTooltip(ModItems.CHEST_UPGRADE_TEMPLATE.get());
			registerCreateTooltip(ModItems.COPPER_CHEST_UPGRADE.get());
			registerCreateTooltip(ModItems.IRON_CHEST_UPGRADE.get());
			registerCreateTooltip(ModItems.GOLD_CHEST_UPGRADE.get());
			registerCreateTooltip(ModItems.DIAMOND_CHEST_UPGRADE.get());
			registerCreateTooltip(ModItems.NETHERITE_CHEST_UPGRADE.get());
			registerCreateTooltip(ModItems.PRESSURE_CRUCIBLE.get());
			registerCreateTooltip(ModItems.EMERALD_DUST.get());
			registerCreateTooltip(ModItems.NETHERITE_DUST.get());
			registerCreateTooltip(ModItems.QUARTZ_DUST.get());

			// Stress impact per RPM, read from the config each time Create asks
			BlockStressValues.IMPACTS.register(ModBlocks.PRESSURE_CRUCIBLE.get(), CMConfig.SERVER::crucibleStressImpact);
			BlockStressValues.IMPACTS.register(ModBlocks.WINDING_KEY.get(), CMConfig.SERVER::windingKeyStressImpact);
			registerCreateTooltip(ModItems.WINDING_KEY.get());
			registerCreateTooltip(ModItems.SPRING_ENGINE.get());
			// Every generator, by tier: the Brass ones make 16 at a time and load the network to match
			for (var generator : ModBlocks.generators()) {
				CobbleGeneratorBlock block = generator.get();
				BlockStressValues.IMPACTS.register(block, block.perCycle() > 1
					? CMConfig.SERVER::brassCobbleGenStressImpact : CMConfig.SERVER::andesiteCobbleGenStressImpact);
				registerCreateTooltip(block.asItem());
			}

			registerCreateTooltip(ModItems.AUTO_HARVESTER.get());
			registerCreateTooltip(ModItems.ANIMAL_PEN.get());
			registerCreateTooltip(ModItems.BRASS_ANIMAL_PEN.get());
			registerCreateTooltip(ModItems.CUCKOO_CLOCK.get());
			registerCreateTooltip(ModItems.TRADE_BOT.get());
			registerCreateTooltip(ModItems.HELPER_BOT.get());
			registerCreateTooltip(ModItems.FE_COIL.get());
			registerCreateTooltip(ModItems.BOTTLED_ANIMAL.get());
			registerCreateTooltip(ModItems.CAPTURE_BOTTLE.get());
			registerCreateTooltip(ModItems.MONSTER_CAGE.get());
			registerCreateTooltip(ModItems.BRASS_MONSTER_CAGE.get());
			registerCreateTooltip(ModItems.FLOODED_MONSTER_CAGE.get());
			registerCreateTooltip(ModItems.BRASS_FLOODED_MONSTER_CAGE.get());
			registerCreateTooltip(ModItems.NETHERITE_MONSTER_CAGE.get());
			registerCreateTooltip(ModItems.IRON_JAR.get());
			registerCreateTooltip(ModItems.NETHERITE_JAR.get());
			registerCreateTooltip(ModItems.MONSTER_CHOW.get());
			registerCreateTooltip(ModItems.FISH_TANK.get());
			registerCreateTooltip(ModItems.BRASS_FISH_TANK.get());
			registerCreateTooltip(ModItems.FISH_FOOD.get());
			registerCreateTooltip(ModItems.INCINERATOR.get());
			registerCreateTooltip(ModItems.STRING_TANGLER.get());
			registerCreateTooltip(ModItems.STONE_INFUSER.get());
			BlockStressValues.IMPACTS.register(ModBlocks.STRING_TANGLER.get(), CMConfig.SERVER::tanglerStressImpact);
			BlockStressValues.IMPACTS.register(ModBlocks.STONE_INFUSER.get(), CMConfig.SERVER::infuserStressImpact);
			registerCreateTooltip(ModItems.MAGMA_ENGINE.get());
			registerCreateTooltip(ModItems.SUPERCHARGED_MAGMA_ENGINE.get());
			registerCreateTooltip(ModItems.ELECTRIC_MOTOR.get());
			registerCreateTooltip(ModItems.LAVA_GENERATOR.get());
			registerCreateTooltip(ModItems.BATTERY.get());
			registerCreateTooltip(ModItems.GEOTHERMAL_CONTROLLER.get());
			registerCreateTooltip(ModItems.GEOTHERMAL_CASING.get());
			registerCreateTooltip(ModItems.GEOTHERMAL_CORE.get());
			registerCreateTooltip(ModItems.SNUG_PIPEZ_COVER.get());
			registerCreateTooltip(ModItems.FULL_PIPEZ_COVER.get());
			// Capacity per RPM, so 128 at 64 RPM is 8192 SU
			BlockStressValues.CAPACITIES.register(ModBlocks.MAGMA_ENGINE.get(), CMConfig.SERVER::magmaEngineCapacity);
			BlockStressValues.CAPACITIES.register(ModBlocks.SUPERCHARGED_MAGMA_ENGINE.get(),
				CMConfig.SERVER::superchargedMagmaEngineCapacity);
			// A lit Incinerator heats a boiler like a lit Blaze Burner; cold, it's no heat source at all
			BoilerHeater.REGISTRY.register(ModBlocks.INCINERATOR.get(),
				(level, pos, state) -> state.getValue(IncineratorBlock.LIT) ? 1 : BoilerHeater.NO_HEAT);
			if (SableCompat.LOADED) {
				AeronauticsContent.commonSetup();
				registerCreateTooltip(AeronauticsContent.COMPACT_GYROSCOPE_ITEM.get());
				registerCreateTooltip(AeronauticsContent.TORQUE_ROTOR_ITEM.get());
				registerCreateTooltip(AeronauticsContent.FLIGHT_INSTRUMENTS_ITEM.get());
			}

			ModDisplaySources.bindBlocks();
			// Dispensers catch and release with the jars and the Capture Bottle
			com.comedymonkey.createaddons.content.cage.CatcherDispensing.register();

			// Upgraded chests keep their oversized stacks, and Wellsprings keep generating, on Create contraptions
			ModMountedStorage.bindBlocks();
			// Helper Bot conducts trains through Create's conductor rules, and animates himself on contraptions
			com.comedymonkey.createaddons.content.helperbot.HelperBots.bindBlocks();
		});
	}

	private static void registerCreateTooltip(Item item) {
		TooltipModifier.REGISTRY.register(item, new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE));
	}

	public static ResourceLocation asResource(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
