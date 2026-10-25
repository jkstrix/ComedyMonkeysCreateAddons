package com.comedymonkey.createaddons.client;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.compat.curios.client.AviatorGogglesCurioRenderer;
import com.comedymonkey.createaddons.client.ponder.CMPonderPlugin;
import com.comedymonkey.createaddons.compat.sable.SableCompat;
import com.comedymonkey.createaddons.compat.sable.client.AeronauticsRenderers;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.comedymonkey.createaddons.registry.ModItems;
import com.comedymonkey.createaddons.registry.ModMenus;
import com.comedymonkey.createaddons.content.chest.UpgradedBarrelItem;
import com.comedymonkey.createaddons.content.chest.UpgradedChestItem;
import net.minecraft.client.renderer.item.ItemProperties;

import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.minecraft.world.level.block.state.properties.ChestType;
import com.comedymonkey.createaddons.content.chest.ChestTier;
import com.comedymonkey.createaddons.client.tradebot.TradeBotGeometry;
import com.comedymonkey.createaddons.client.tradebot.TradeBotRenderer;
import com.comedymonkey.createaddons.client.tradebot.TradeBotStationRenderer;
import com.comedymonkey.createaddons.client.tradebot.TradeBotItemRenderer;
import com.comedymonkey.createaddons.client.tradebot.TradeBotLinkPicking;
import com.comedymonkey.createaddons.registry.ModEntities;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = ComedyMonkeysCreateAddons.MOD_ID, dist = Dist.CLIENT)
public class ComedyMonkeysCreateAddonsClient {
	public ComedyMonkeysCreateAddonsClient(IEventBus modBus, ModContainer container) {
		// Config button in NeoForge's own mod list as well (Create's menu finds the specs by itself)
		container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
		PonderIndex.addPlugin(new CMPonderPlugin());

		modBus.addListener(this::registerRenderers);
		modBus.addListener(this::registerLayers);
		modBus.addListener(this::clientSetup);
		modBus.addListener(this::registerScreens);
		modBus.addListener(this::registerModels);
		modBus.addListener(this::registerClientExtensions);
		// Fluid and Power Lines: tier and dye colours, encased lines drawn as their block, hidden lines through goggles
		modBus.addListener(com.comedymonkey.createaddons.client.line.LineClient::registerBlockColors);
		modBus.addListener(com.comedymonkey.createaddons.client.line.LineClient::registerItemColors);
		modBus.addListener(com.comedymonkey.createaddons.client.line.LineClient::wrapModels);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.comedymonkey.createaddons.client.line.LineClient::drawHiddenLines);
		// Cached sprites are looked up again once the block atlas is restitched, and what was read from models once
		// they're baked again
		modBus.addListener(BlockSprite::onStitched);
		modBus.addListener(ComedyMonkeysCreateAddonsClient::forgetModelReads);
		modBus.addListener(ComedyMonkeysCreateAddonsClient::registerReloadListeners);
		// Picking containers for Trade Bot in the world, from his station screen
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(TradeBotLinkPicking::onRightClickBlock);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(TradeBotLinkPicking::onLeftClickBlock);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(TradeBotLinkPicking::onClientTick);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.comedymonkey.createaddons.client.tradebot.TradeBotLoopSounds::onClientTick);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ComedyMonkeysCreateAddonsClient::tickClipClock);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ComedyMonkeysCreateAddonsClient::forgetMachineSounds);
		// Development only: Flywheel on/off A/B screenshots on request
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.comedymonkey.createaddons.client.dev.ShotRunner::onClientTick);
		// The Geothermal Turbine's sounds run from its controller's client tick
		com.comedymonkey.createaddons.content.electric.GeothermalControllerBlockEntity.clientTicker = GeothermalTurbineSounds::tick;
		// The Lava Generator's and the Electric Motor's sounds run from their client ticks
		com.comedymonkey.createaddons.content.electric.LavaGeneratorBlockEntity.clientTicker = ElectricSounds::tickGenerator;
		com.comedymonkey.createaddons.content.electric.ElectricMotorBlockEntity.clientTicker = ElectricSounds::tickMotor;
		// The Magma Engines' sounds and exhaust puffs run from their client ticks
		com.comedymonkey.createaddons.content.engine.MagmaEngineBlockEntity.clientTicker = MagmaEngineSounds::tick;
		// The Pressure Crucible's sounds run from its client tick
		com.comedymonkey.createaddons.content.crucible.PressureCrucibleBlockEntity.clientTicker = PressureCrucibleSounds::tick;
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ChestSortButton::onScreenInit);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(QuickTransferControls::onScreenInit);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(QuickTransferControls::onMouseScrolled);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(QuickTransferControls::onMousePressed);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(QuickTransferControls::onMouseDragged);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(QuickTransferControls::onMouseReleased);
		// Pipez Covers: covered pipes are drawn as their cover, tinted like its block
		if (net.neoforged.fml.ModList.get().isLoaded("pipez")) {
			modBus.addListener(com.comedymonkey.createaddons.compat.pipez.cover.client.CoveredPipeModel::wrapPipes);
			modBus.addListener(com.comedymonkey.createaddons.compat.pipez.cover.client.PipeCoverClient::registerColors);
		}
	}

	/** Trade Bot's clips run on a clock that keeps the server's real pace without jumping when it corrects the time. */
	private static void tickClipClock(net.neoforged.neoforge.client.event.ClientTickEvent.Pre event) {
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		if (mc.level != null && !mc.isPaused() && mc.level.tickRateManager().runsNormally())
			com.comedymonkey.createaddons.content.tradebot.clip.ClipClock.tick(mc.level, mc.level.getGameTime());
	}

	/** What's read from resources once and kept is read again after a resource reload. */
	private static void registerReloadListeners(net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) {
		event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> {
			Hearing.forget();
			TradeBotItemRenderer.forget();
		});
	}

	/** The harvester's plants read some of their models: read them again once they're rebaked. */
	private static void forgetModelReads(ModelEvent.BakingCompleted event) {
		RealPlants.forget();
		com.comedymonkey.createaddons.client.parts.PartModels.forget();
		com.comedymonkey.createaddons.client.line.PowerLineModel.forget();
	}

	/**
	 * The machines' sound states hold their block entities, and the bottled animals their entities, and so the world:
	 * let them go with it. The sounds' ranges are read again too, in case a resource pack changed them.
	 */
	private static void forgetMachineSounds(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
		if (!event.getLevel().isClientSide())
			return;
		GeothermalTurbineSounds.forget();
		ElectricSounds.forget();
		MagmaEngineSounds.forget();
		PressureCrucibleSounds.forget();
		Hearing.forget();
		BottledAnimalRenderer.forget();
	}

	private void registerScreens(RegisterMenuScreensEvent event) {
		event.register(ModMenus.WELLSPRING.get(), WellspringScreen::new);
		event.register(ModMenus.UPGRADED_CHEST.get(), UpgradedChestScreen::new);
		event.register(ModMenus.LARGE_UPGRADED_CHEST.get(), LargeUpgradedChestScreen::new);
		event.register(ModMenus.CHEST_UPGRADE.get(), ChestUpgradeScreen::new);
		event.register(ModMenus.AUTO_HARVESTER.get(), AutoHarvesterScreen::new);
		event.register(ModMenus.ANIMAL_PEN.get(), AnimalPenScreen::new);
	}

	private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(ModBlockEntities.WELLSPRING.get(), WellspringRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.FLUID_LINE.get(), FluidLineRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.POWER_LINE.get(), com.comedymonkey.createaddons.client.line.PowerLineRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.LINE_POST.get(), com.comedymonkey.createaddons.client.line.LinePostRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.UPGRADED_CHEST.get(), UpgradedChestRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.PRESSURE_CRUCIBLE.get(), PressureCrucibleRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.COBBLE_GENERATOR.get(), CobbleGeneratorRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.AUTO_HARVESTER.get(), AutoHarvesterRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.CUCKOO_CLOCK.get(), CuckooClockRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.ANIMAL_PEN.get(), AnimalPenRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.MAGMA_ENGINE.get(), MagmaEngineRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.ELECTRIC_MOTOR.get(), ElectricRenderers.Motor::new);
		event.registerBlockEntityRenderer(ModBlockEntities.LAVA_GENERATOR.get(), ElectricRenderers.Generator::new);
		event.registerBlockEntityRenderer(ModBlockEntities.GEOTHERMAL_CONTROLLER.get(), ElectricRenderers.Turbine::new);
		event.registerBlockEntityRenderer(ModBlockEntities.STRING_TANGLER.get(), MachineRenderers.Tangler::new);
		event.registerBlockEntityRenderer(ModBlockEntities.STONE_INFUSER.get(), MachineRenderers.Infuser::new);
		if (SableCompat.LOADED)
			AeronauticsRenderers.registerRenderers(event);
		event.registerEntityRenderer(ModEntities.TRADE_BOT.get(), TradeBotRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.TRADE_BOT_STATION.get(), TradeBotStationRenderer::new);
	}

	/** The upgraded chest lids and the crucible's parts aren't referenced by any blockstate, so they're loaded explicitly. */
	private void registerModels(ModelEvent.RegisterAdditional event) {
		for (ChestTier tier : ChestTier.values())
			for (ChestType type : ChestType.values())
				event.register(UpgradedChestRenderer.lidModel(tier, type));
		WellspringRenderer.registerModels(event);
		FluidLineRenderer.registerModels(event);
		com.comedymonkey.createaddons.client.line.PowerLineModel.registerModels(event);
		com.comedymonkey.createaddons.client.line.PowerLineRenderer.registerModels(event);
		PressureCrucibleRenderer.registerModels(event);
		CobbleGeneratorRenderer.registerModels(event);
		MachineRenderers.registerModels(event);
		AutoHarvesterRenderer.registerModels(event);
		event.register(AnimalPenRenderer.MILK);
		CuckooClockRenderer.registerModels(event);
		MagmaEngineRenderer.registerModels(event);
		ElectricRenderers.registerModels(event);
		if (SableCompat.LOADED)
			AeronauticsRenderers.registerModels(event);
	}

	/** A Bottled Animal draws its animal inside the jar. */
	private void registerClientExtensions(RegisterClientExtensionsEvent event) {
		event.registerItem(new IClientItemExtensions() {
			private TradeBotItemRenderer renderer;

			@Override
			public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (renderer == null)
					renderer = new TradeBotItemRenderer();
				return renderer;
			}
		}, ModItems.TRADE_BOT.get());
		event.registerItem(new IClientItemExtensions() {
			private BottledAnimalRenderer renderer;

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (renderer == null)
					renderer = new BottledAnimalRenderer();
				return renderer;
			}
		}, ModItems.BOTTLED_ANIMAL.get());
	}

	private void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(TradeBotGeometry.BOT_LAYER, TradeBotGeometry::botLayer);
		event.registerLayerDefinition(TradeBotGeometry.STATION_LAYER, TradeBotGeometry::stationLayer);
		// The goggles renderer is a Curios one, so its class can't even be loaded without Curios
		if (ModList.get().isLoaded(ComedyMonkeysCreateAddons.AERONAUTICS_ID)
			&& ModList.get().isLoaded(ComedyMonkeysCreateAddons.CURIOS_ID))
			AviatorGogglesCurioRenderer.registerLayers(event);
	}

	private void clientSetup(FMLClientSetupEvent event) {
		// The machines' moving parts, drawn by Flywheel when it can
		event.enqueueWork(com.comedymonkey.createaddons.client.parts.MachineVisuals::register);
		// Upgraded chest item picks its tier's model (0 copper .. 0.3 diamond)
		event.enqueueWork(() -> ItemProperties.register(ModItems.UPGRADED_CHEST.get(),
			ComedyMonkeysCreateAddons.asResource("tier"),
			(stack, level, entity, seed) -> UpgradedChestItem.tierOf(stack).ordinal() / 10f));
		event.enqueueWork(() -> ItemProperties.register(ModItems.UPGRADED_BARREL.get(),
			ComedyMonkeysCreateAddons.asResource("tier"),
			(stack, level, entity, seed) -> UpgradedBarrelItem.tierOf(stack).ordinal() / 10f));

		if (!ModList.get().isLoaded(ComedyMonkeysCreateAddons.AERONAUTICS_ID)
			|| !ModList.get().isLoaded(ComedyMonkeysCreateAddons.CURIOS_ID))
			return;
		event.enqueueWork(AviatorGogglesCurioRenderer::register);
	}
}
