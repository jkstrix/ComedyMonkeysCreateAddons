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
		// Picking containers for Trade Bot in the world, from his station screen
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(TradeBotLinkPicking::onRightClickBlock);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(TradeBotLinkPicking::onLeftClickBlock);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(TradeBotLinkPicking::onClientTick);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.comedymonkey.createaddons.client.tradebot.TradeBotLoopSounds::onClientTick);
		// The Geothermal Turbine's sounds run from its controller's client tick
		com.comedymonkey.createaddons.content.electric.GeothermalControllerBlockEntity.clientTicker = GeothermalTurbineSounds::tick;
		// The Lava Generator's and the Electric Motor's sounds run from their client ticks
		com.comedymonkey.createaddons.content.electric.LavaGeneratorBlockEntity.clientTicker = ElectricSounds::tickGenerator;
		com.comedymonkey.createaddons.content.electric.ElectricMotorBlockEntity.clientTicker = ElectricSounds::tickMotor;
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ChestSortButton::onScreenInit);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(QuickTransferControls::onScreenInit);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(QuickTransferControls::onMouseScrolled);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(QuickTransferControls::onMousePressed);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(QuickTransferControls::onMouseDragged);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(QuickTransferControls::onMouseReleased);
		if (MagmaEngineSimModels.LOADED)
			modBus.addListener(MagmaEngineSimModels::modifyBakingResult);
		// Pipez Covers: covered pipes are drawn as their cover, tinted like its block
		if (net.neoforged.fml.ModList.get().isLoaded("pipez")) {
			modBus.addListener(com.comedymonkey.createaddons.compat.pipez.cover.client.CoveredPipeModel::wrapPipes);
			modBus.addListener(com.comedymonkey.createaddons.compat.pipez.cover.client.PipeCoverClient::registerColors);
		}
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

	/** The upgraded chest lids and the crucible ram aren't referenced by any blockstate, so they're loaded explicitly. */
	private void registerModels(ModelEvent.RegisterAdditional event) {
		for (ChestTier tier : ChestTier.values())
			for (ChestType type : ChestType.values())
				event.register(UpgradedChestRenderer.lidModel(tier, type));
		event.register(PressureCrucibleRenderer.RAM);
		event.register(PressureCrucibleRenderer.ROD);
		event.register(PressureCrucibleRenderer.SHAFT);
		event.register(CobbleGeneratorRenderer.SHAFT);
		AutoHarvesterRenderer.registerModels(event);
		event.register(AnimalPenRenderer.MILK);
		CuckooClockRenderer.registerModels(event);
		event.register(MagmaEngineRenderer.HEAD);
		event.register(MagmaEngineRenderer.SUPERCHARGED_HEAD);
		ElectricRenderers.registerModels(event);
		if (MagmaEngineSimModels.LOADED)
			MagmaEngineSimModels.registerModels(event);
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
