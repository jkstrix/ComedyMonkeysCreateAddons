package com.comedymonkey.createaddons;

import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.compat.curios.AviatorGogglesCurios;
import com.comedymonkey.createaddons.compat.pipez.PipezBlazeBurnerCompat;
import com.comedymonkey.createaddons.compat.pipez.PipezEngineCompat;
import com.comedymonkey.createaddons.compat.sable.AeronauticsContent;
import com.comedymonkey.createaddons.compat.sable.SableCompat;
import com.comedymonkey.createaddons.content.cobblegen.CobbleGeneratorBlock;
import com.comedymonkey.createaddons.content.engine.MagmaEngineBlock;
import com.comedymonkey.createaddons.content.incinerator.IncineratorBlock;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.comedymonkey.createaddons.content.chest.ChestEvents;
import net.neoforged.neoforge.common.NeoForge;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.network.ContraptionBlockSyncPayload;
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
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.TooltipModifier;

import net.createmod.catnip.lang.FontHelper;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import com.comedymonkey.createaddons.compat.pipez.cover.PipeCoverEvents;
import com.comedymonkey.createaddons.compat.pipez.cover.client.PipeCoverClient;
import com.comedymonkey.createaddons.network.PipeCoverPayload;
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

		// Both specs show up in Create's "Access configs of other mods" screen
		container.registerConfig(ModConfig.Type.SERVER, CMConfig.SERVER_SPEC);
		container.registerConfig(ModConfig.Type.CLIENT, CMConfig.CLIENT_SPEC);

		NeoForge.EVENT_BUS.addListener(ChestEvents::onRightClickBlock);
		NeoForge.EVENT_BUS.addListener(ChestEvents::onBlockBreak);
		if (ModList.get().isLoaded(PipezBlazeBurnerCompat.PIPEZ_ID)) {
			NeoForge.EVENT_BUS.addListener(PipeCoverEvents::onRightClickBlock);
			NeoForge.EVENT_BUS.addListener(PipeCoverEvents::onBlockBreak);
			NeoForge.EVENT_BUS.addListener(PipeCoverEvents::onChunkSent);
		}
		modBus.addListener(this::registerCapabilities);
		modBus.addListener(this::commonSetup);
		modBus.addListener(this::registerPayloads);

		if (ModList.get().isLoaded(AERONAUTICS_ID))
			AviatorGogglesCurios.init();
	}

	private void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.WELLSPRING.get(),
			(be, side) -> be.getTank());
		// Automation (hoppers, funnels, Pipez...) fills upgraded chests past 64 per slot
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.UPGRADED_CHEST.get(),
			(be, side) -> be.getItemHandler());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.UPGRADED_BARREL.get(),
			(be, side) -> be.getItemHandler());
		// Cobblestone out of the generators, from any side
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.COBBLE_GENERATOR.get(),
			(be, side) -> be.getOutputHandler());
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
			// Lava in through the back or the bottom only
			event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.MAGMA_ENGINE.get(),
				(be, side) -> side == null || side == Direction.DOWN
					|| side == be.getBlockState().getValue(MagmaEngineBlock.FACING).getOpposite() ? be.getTank() : null);

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
		// Covers on Pipez pipes
		event.registrar("1").playToClient(PipeCoverPayload.TYPE, PipeCoverPayload.STREAM_CODEC,
			(payload, context) -> context.enqueueWork(() -> PipeCoverClient.handle(payload)));
	}

	private void commonSetup(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> {
			// Create-style "Hold [Shift] for Summary" tooltips, read from the lang file
			registerCreateTooltip(ModBlocks.WELLSPRING.asItem());
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
			// Every generator, by tier: the Brass ones make 16 at a time and load the network to match
			for (var generator : ModBlocks.generators()) {
				CobbleGeneratorBlock block = generator.get();
				BlockStressValues.IMPACTS.register(block, block.perCycle() > 1
					? CMConfig.SERVER::brassCobbleGenStressImpact : CMConfig.SERVER::andesiteCobbleGenStressImpact);
				registerCreateTooltip(block.asItem());
			}

			registerCreateTooltip(ModItems.INCINERATOR.get());
			registerCreateTooltip(ModItems.STRING_TANGLER.get());
			registerCreateTooltip(ModItems.STONE_INFUSER.get());
			BlockStressValues.IMPACTS.register(ModBlocks.STRING_TANGLER.get(), CMConfig.SERVER::tanglerStressImpact);
			BlockStressValues.IMPACTS.register(ModBlocks.STONE_INFUSER.get(), CMConfig.SERVER::infuserStressImpact);
			registerCreateTooltip(ModItems.MAGMA_ENGINE.get());
			registerCreateTooltip(ModItems.SUPERCHARGED_MAGMA_ENGINE.get());
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
			}

			ModDisplaySources.bindBlocks();

			// Upgraded chests keep their oversized stacks, and Wellsprings keep generating, on Create contraptions
			ModMountedStorage.bindBlocks();
		});
	}

	private static void registerCreateTooltip(Item item) {
		TooltipModifier.REGISTRY.register(item, new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE));
	}

	public static ResourceLocation asResource(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
