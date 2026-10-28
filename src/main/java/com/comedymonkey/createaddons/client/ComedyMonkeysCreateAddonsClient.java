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
		// Light Strips: built from parts by how they join, the lit diffuser tinted by dye and brightness
		modBus.addListener(com.comedymonkey.createaddons.client.line.StripModel::registerBlockColors);
		modBus.addListener(com.comedymonkey.createaddons.client.line.StripModel::registerModels);
		modBus.addListener(com.comedymonkey.createaddons.client.line.StripModel::wrap);
		modBus.addListener(com.comedymonkey.createaddons.client.line.EdgeModel::registerModels);
		modBus.addListener(com.comedymonkey.createaddons.client.line.EdgeModel::wrap);
		modBus.addListener(com.comedymonkey.createaddons.client.line.LineClient::registerItemColors);
		modBus.addListener(com.comedymonkey.createaddons.client.line.LineClient::wrapModels);
		// Helper Bot's flat icon: his chest panels in his dye
		modBus.addListener(com.comedymonkey.createaddons.client.helperbot.HelperBotClient::registerItemColors);
		// Helper Bot's render types batch on their own buffers, drawn once the block entities are
		modBus.addListener(com.comedymonkey.createaddons.client.helperbot.HelperBotClient::registerRenderBuffers);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.comedymonkey.createaddons.client.helperbot.HelperBotClient::endBatches);
		// A formed Battery draws its tile of the Industrial Battery
		modBus.addListener(BatteryTowerModel::wrap);
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
		// The ground shakes under the Juggernaut's spring leap
		com.comedymonkey.createaddons.client.juggernaut.JuggernautShake.register();
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ComedyMonkeysCreateAddonsClient::forgetMachineSounds);
		// Development only: Flywheel on/off A/B screenshots on request
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(com.comedymonkey.createaddons.client.dev.ShotRunner::onClientTick);
		// The Geothermal Turbine's sounds run from its controller's client tick
		com.comedymonkey.createaddons.content.electric.GeothermalControllerBlockEntity.clientTicker = GeothermalTurbineSounds::tick;
		// The Lava Generator's and the Electric Motor's sounds run from their client ticks
		com.comedymonkey.createaddons.content.electric.LavaGeneratorBlockEntity.clientTicker = ElectricSounds::tickGenerator;
		// Flywheel: a line's visual made again when its fitting changes; a post's spans laid out again (and its partners')
		com.comedymonkey.createaddons.content.line.LineBlockEntity.clientRefitted = ComedyMonkeysCreateAddonsClient::revisualize;
		com.comedymonkey.createaddons.content.line.LinePostBlockEntity.clientSpansChanged = be -> {
			if (be.getLevel() == null || !dev.engine_room.flywheel.api.visualization.VisualizationManager.supportsVisualization(be.getLevel()))
				return;
			dev.engine_room.flywheel.lib.visualization.VisualizationHelper.queueUpdate(be);
			for (com.comedymonkey.createaddons.content.line.Span s : be.spans())
				if (!s.owner() && be.getLevel().getBlockEntity(be.getBlockPos().offset(s.offset())) instanceof com.comedymonkey.createaddons.content.line.LinePostBlockEntity owner)
					dev.engine_room.flywheel.lib.visualization.VisualizationHelper.queueUpdate(owner);
		};
		// an Industrial Battery's tower or spans changed: the posts its spans hang from lay them out again
		com.comedymonkey.createaddons.content.electric.BatteryBlockEntity.clientTowerChanged = be -> {
			if (be.getLevel() == null || !dev.engine_room.flywheel.api.visualization.VisualizationManager.supportsVisualization(be.getLevel()))
				return;
			for (com.comedymonkey.createaddons.content.line.Span s : be.spans())
				if (be.getLevel().getBlockEntity(be.getBlockPos().offset(s.offset())) instanceof com.comedymonkey.createaddons.content.line.LinePostBlockEntity post)
					dev.engine_room.flywheel.lib.visualization.VisualizationHelper.queueUpdate(post);
		};
		com.comedymonkey.createaddons.content.electric.ElectricMotorBlockEntity.clientTicker = ElectricSounds::tickMotor;
		// Electric Lights: flicker and warm-up sounds (capped nearby), and a lit light's visual made when it comes on
		com.comedymonkey.createaddons.content.light.LightBlockEntity.clientCue = LightSounds::cue;
		// The Industrial Battery's hum runs from its master's client tick
		com.comedymonkey.createaddons.content.electric.BatteryBlockEntity.clientTicker = ElectricSounds::tickBattery;
		// The Magma Engines' sounds and exhaust puffs run from their client ticks
		com.comedymonkey.createaddons.content.engine.MagmaEngineBlockEntity.clientTicker = MagmaEngineSounds::tick;
		// The Pressure Crucible's sounds run from its client tick
		com.comedymonkey.createaddons.content.crucible.PressureCrucibleBlockEntity.clientTicker = PressureCrucibleSounds::tick;
		// The Monster Cages' hum and bubbles run from their client ticks
		com.comedymonkey.createaddons.content.cage.MonsterCageBlockEntity.clientTicker = MonsterCageSounds::tick;
		// The Fish Tanks' pump and bubbles, and their creatures swimming, run from their client ticks
		com.comedymonkey.createaddons.content.tank.FishTankBlockEntity.clientTicker = FishTankSounds::tick;
		// Cages and tanks keep their window's creatures only near the camera (Ponder's and contraptions' always), only
		// the nearest few of their loops play, and only so many creatures are built a tick and drawn a frame
		WindowClient.install();
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ChestSortButton::onScreenInit);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(QuickTransferControls::onScreenInit);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(QuickTransferControls::onMouseScrolled);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(QuickTransferControls::onMousePressed);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(QuickTransferControls::onMouseDragged);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(QuickTransferControls::onMouseReleased);
		// Electric Lights cast light in their glow's colour where Veil can draw it (it comes with Sable): not with Iris (its
		// light pass fails there, shader pack or not), nor with Colorful Lighting (which colours their light itself)
		if (net.neoforged.fml.ModList.get().isLoaded("veil") && !net.neoforged.fml.ModList.get().isLoaded("iris")
			&& !net.neoforged.fml.ModList.get().isLoaded("colorful_lighting"))
			com.comedymonkey.createaddons.compat.veil.LightGlow.install(modBus);
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
			com.comedymonkey.createaddons.client.helperbot.HelperBotModels.forget();
		});
	}

	/** The harvester's plants read some of their models: read them again once they're rebaked. */
	private static void forgetModelReads(ModelEvent.BakingCompleted event) {
		RealPlants.forget();
		com.comedymonkey.createaddons.client.parts.PartModels.forget();
		com.comedymonkey.createaddons.client.line.PowerLineModel.forget();
		com.comedymonkey.createaddons.client.line.StripModel.forget();
		com.comedymonkey.createaddons.client.line.EdgeModel.forget();
		BatteryTowerModel.forget();
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
		MonsterCageSounds.forget();
		FishTankSounds.forget();
		NearestLoops.forget();
		Hearing.forget();
		BottledAnimalRenderer.forget();
		MonsterJarRenderer.forget();
	}

	private void registerScreens(RegisterMenuScreensEvent event) {
		event.register(ModMenus.WELLSPRING.get(), WellspringScreen::new);
		event.register(ModMenus.UPGRADED_CHEST.get(), UpgradedChestScreen::new);
		event.register(ModMenus.LARGE_UPGRADED_CHEST.get(), LargeUpgradedChestScreen::new);
		event.register(ModMenus.CHEST_UPGRADE.get(), ChestUpgradeScreen::new);
		event.register(ModMenus.AUTO_HARVESTER.get(), AutoHarvesterScreen::new);
		event.register(ModMenus.ANIMAL_PEN.get(), AnimalPenScreen::new);
		event.register(ModMenus.MONSTER_CAGE.get(), MonsterCageScreen::new);
		event.register(ModMenus.FISH_TANK.get(), FishTankScreen::new);
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
		event.registerBlockEntityRenderer(ModBlockEntities.MONSTER_CAGE.get(), MonsterCageRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.FISH_TANK.get(), FishTankRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.MAGMA_ENGINE.get(), MagmaEngineRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.ELECTRIC_MOTOR.get(), ElectricRenderers.Motor::new);
		event.registerBlockEntityRenderer(ModBlockEntities.WINDING_KEY.get(), com.comedymonkey.createaddons.client.juggernaut.WindingKeyRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.SPRING_ENGINE.get(), com.comedymonkey.createaddons.client.juggernaut.SpringEngineRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.LAVA_GENERATOR.get(), ElectricRenderers.Generator::new);
		event.registerBlockEntityRenderer(ModBlockEntities.BATTERY.get(), BatteryRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.LIGHT.get(), LightRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.GEOTHERMAL_CONTROLLER.get(), ElectricRenderers.Turbine::new);
		event.registerBlockEntityRenderer(ModBlockEntities.STRING_TANGLER.get(), MachineRenderers.Tangler::new);
		event.registerBlockEntityRenderer(ModBlockEntities.STONE_INFUSER.get(), MachineRenderers.Infuser::new);
		if (SableCompat.LOADED)
			AeronauticsRenderers.registerRenderers(event);
		event.registerEntityRenderer(ModEntities.TRADE_BOT.get(), TradeBotRenderer::new);
		event.registerEntityRenderer(ModEntities.CLOCKWORK_JUGGERNAUT.get(), com.comedymonkey.createaddons.client.juggernaut.JuggernautRenderer::new);
		event.registerEntityRenderer(ModEntities.THROWN_COG.get(), context -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(context, 2.5f, false));
		event.registerBlockEntityRenderer(ModBlockEntities.TRADE_BOT_STATION.get(), TradeBotStationRenderer::new);
		event.registerBlockEntityRenderer(ModBlockEntities.HELPER_BOT.get(), com.comedymonkey.createaddons.client.helperbot.HelperBotRenderer::new);
	}

	/** A block entity's Flywheel visual made again (it needs a different one now: a fitting went on, a chest opened). */
	static void revisualize(net.minecraft.world.level.block.entity.BlockEntity be) {
		if (be.getLevel() != null && dev.engine_room.flywheel.api.visualization.VisualizationManager.supportsVisualization(be.getLevel())) {
			var visuals = dev.engine_room.flywheel.api.visualization.VisualizationManager.get(be.getLevel()).blockEntities();
			visuals.queueRemove(be);
			visuals.queueAdd(be);
		}
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
		event.register(com.comedymonkey.createaddons.client.line.LinePostVisual.SEGMENT);
		PressureCrucibleRenderer.registerModels(event);
		CobbleGeneratorRenderer.registerModels(event);
		com.comedymonkey.createaddons.client.juggernaut.WindingKeyRenderer.registerModels(event);
		com.comedymonkey.createaddons.client.juggernaut.SpringEngineRenderer.registerModels(event);
		MachineRenderers.registerModels(event);
		AutoHarvesterRenderer.registerModels(event);
		event.register(AnimalPenRenderer.MILK);
		MonsterCageRenderer.registerModels(event);
		FishTankRenderer.registerModels(event);
		event.register(MonsterJarRenderer.IRON);
		event.register(MonsterJarRenderer.NETHERITE);
		CuckooClockRenderer.registerModels(event);
		MagmaEngineRenderer.registerModels(event);
		ElectricRenderers.registerModels(event);
		BatteryRenderer.registerModels(event);
		LightRenderer.registerModels(event);
		BatteryTowerModel.registerModels(event);
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
			private com.comedymonkey.createaddons.client.helperbot.HelperBotItemRenderer renderer;

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (renderer == null)
					renderer = new com.comedymonkey.createaddons.client.helperbot.HelperBotItemRenderer();
				return renderer;
			}
		}, ModItems.HELPER_BOT.get());
		event.registerItem(new IClientItemExtensions() {
			private BottledAnimalRenderer renderer;

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (renderer == null)
					renderer = new BottledAnimalRenderer();
				return renderer;
			}
		}, ModItems.BOTTLED_ANIMAL.get());
		// The jars draw their monster inside the glass
		event.registerItem(new IClientItemExtensions() {
			private MonsterJarRenderer renderer;

			@Override
			public BlockEntityWithoutLevelRenderer getCustomRenderer() {
				if (renderer == null)
					renderer = new MonsterJarRenderer();
				return renderer;
			}
		}, ModItems.IRON_JAR.get(), ModItems.NETHERITE_JAR.get());
	}

	private void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(TradeBotGeometry.BOT_LAYER, TradeBotGeometry::botLayer);
		event.registerLayerDefinition(com.comedymonkey.createaddons.client.juggernaut.JuggernautModel.LAYER,
			com.comedymonkey.createaddons.client.juggernaut.JuggernautModel::layer);
		event.registerLayerDefinition(com.comedymonkey.createaddons.client.helperbot.HelperBotModels.LAYER,
			com.comedymonkey.createaddons.client.helperbot.HelperBotModels::layer);
		event.registerLayerDefinition(TradeBotGeometry.STATION_LAYER, TradeBotGeometry::stationLayer);
		event.registerLayerDefinition(TradeBotGeometry.STATION_GLOW_LAYER, TradeBotGeometry::stationGlowLayer);
		// The goggles renderer is a Curios one, so its class can't even be loaded without Curios
		if (ModList.get().isLoaded(ComedyMonkeysCreateAddons.AERONAUTICS_ID)
			&& ModList.get().isLoaded(ComedyMonkeysCreateAddons.CURIOS_ID))
			AviatorGogglesCurioRenderer.registerLayers(event);
	}

	private void clientSetup(FMLClientSetupEvent event) {
		// The machines' moving parts, drawn by Flywheel when it can
		event.enqueueWork(com.comedymonkey.createaddons.client.parts.MachineVisuals::register);
		// Helper Bot's clips (a big file) are read now, in the background, rather than on the first frame he's seen
		com.comedymonkey.createaddons.client.helperbot.HelperBotRig.preload();
		// Upgraded chest item picks its tier's model (0 copper .. 0.3 diamond)
		event.enqueueWork(() -> ItemProperties.register(ModItems.UPGRADED_CHEST.get(),
			ComedyMonkeysCreateAddons.asResource("tier"),
			(stack, level, entity, seed) -> UpgradedChestItem.tierOf(stack).ordinal() / 10f));
		// a Battery's item lights its tubes to its charge, in quarters
		event.enqueueWork(() -> ItemProperties.register(ModItems.BATTERY.get(), ComedyMonkeysCreateAddons.asResource("charge"),
			(stack, level, entity, seed) -> com.comedymonkey.createaddons.content.electric.BatteryItem.fill(stack)));
		event.enqueueWork(() -> ItemProperties.register(ModItems.UPGRADED_BARREL.get(),
			ComedyMonkeysCreateAddons.asResource("tier"),
			(stack, level, entity, seed) -> UpgradedBarrelItem.tierOf(stack).ordinal() / 10f));

		if (!ModList.get().isLoaded(ComedyMonkeysCreateAddons.AERONAUTICS_ID)
			|| !ModList.get().isLoaded(ComedyMonkeysCreateAddons.CURIOS_ID))
			return;
		event.enqueueWork(AviatorGogglesCurioRenderer::register);
	}
}
