package com.comedymonkey.createaddons.client.ponder;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.compat.sable.AeronauticsContent;
import com.comedymonkey.createaddons.compat.sable.SableCompat;
import com.comedymonkey.createaddons.compat.sable.client.AeronauticsScenes;
import com.comedymonkey.createaddons.content.cobblegen.GeneratorProduct;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModItems;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Registers the mod's Ponder scenes (press W while hovering an item to open them). */
public class CMPonderPlugin implements PonderPlugin {
	/** Create's "Fluid Manipulators" Ponder tag. */
	private static final ResourceLocation CREATE_FLUIDS_TAG = ResourceLocation.fromNamespaceAndPath("create", "fluids");
	/** Create's "Kinetic Blocks" (appliances) Ponder tag. */
	private static final ResourceLocation CREATE_KINETIC_APPLIANCES_TAG = ResourceLocation.fromNamespaceAndPath("create", "kinetic_appliances");
	/** Create's "Kinetic Sources" (generators) Ponder tag. */
	private static final ResourceLocation CREATE_KINETIC_SOURCES_TAG = ResourceLocation.fromNamespaceAndPath("create", "kinetic_sources");

	@Override
	public String getModId() {
		return ComedyMonkeysCreateAddons.MOD_ID;
	}

	@Override
	public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
		helper.forComponents(ModBlocks.WINDING_KEY.getId())
			.addStoryBoard("juggernaut_ritual", JuggernautScenes::ritual);
		helper.forComponents(ModBlocks.SPRING_ENGINE.getId())
			.addStoryBoard("spring_engine", JuggernautScenes::springEngine);
		helper.forComponents(ModBlocks.WELLSPRING.getId())
			.addStoryBoard("wellspring", WellspringScenes::wellspring);

		// The Fluid and Power Lines: the lines themselves, then the fittings
		helper.forComponents(ModItems.COPPER_FLUID_LINE.getId(), ModItems.BRASS_FLUID_LINE.getId(), ModItems.INTAKE.getId())
			.addStoryBoard("fluid_lines", LineScenes::fluidLines)
			.addStoryBoard("line_fittings", LineScenes::fittings);
		helper.forComponents(ModItems.VALVE.getId(), ModItems.FILTER.getId(), ModItems.METER.getId(), ModItems.SPLITTER.getId())
			.addStoryBoard("line_fittings", LineScenes::fittings);
		helper.forComponents(ModItems.COPPER_POWER_LINE.getId(), ModItems.BRASS_POWER_LINE.getId(), ModItems.LINE_POST.getId(), ModItems.BREAKER.getId())
			.addStoryBoard("power_lines", LineScenes::powerLines)
			.addStoryBoard("power_networks", LineScenes::powerNetworks);

		// Electric Lights: the basics, Lamp Posts, the Floodlight and Festoons (P17)
		var lights = new java.util.ArrayList<ResourceLocation>();
		ModItems.LIGHTS.values().forEach(l -> lights.add(l.getId()));
		lights.add(ModItems.LIGHT_STRIP.getId());
		lights.add(ModItems.EDGE_LIGHT.getId());
		lights.add(ModItems.LIGHT_SWITCH.getId());
		helper.forComponents(lights).addStoryBoard("lights", LightScenes::basics);
		helper.forComponents(ModItems.CAST_IRON_LAMP_POST.getId(), ModItems.STEEL_LAMP_POST.getId(),
				ModItems.LIGHTS.get(com.comedymonkey.createaddons.content.light.LightKind.LANTERN_HEAD).getId(), ModItems.LIGHTS.get(com.comedymonkey.createaddons.content.light.LightKind.COBRA_HEAD).getId(),
				ModItems.LIGHTS.get(com.comedymonkey.createaddons.content.light.LightKind.GLOBE_HEAD).getId(), ModItems.LIGHTS.get(com.comedymonkey.createaddons.content.light.LightKind.YARD_HEAD).getId())
			.addStoryBoard("lamp_posts", LightScenes::lampPosts);
		helper.forComponents(ModItems.LIGHTS.get(com.comedymonkey.createaddons.content.light.LightKind.FLOODLIGHT).getId(), ModItems.FESTOON_LIGHTS.getId())
			.addStoryBoard("floodlight_festoons", LightScenes::floodlightFestoons);
		// the Splitter fits both kinds of line: on a Power Line, its branch priority
		helper.forComponents(ModItems.SPLITTER.getId())
			.addStoryBoard("power_networks", LineScenes::powerNetworks);

		// One scene shared by the template and every tier
		helper.forComponents(
				ModItems.CHEST_UPGRADE_TEMPLATE.getId(),
				ModItems.COPPER_CHEST_UPGRADE.getId(),
				ModItems.IRON_CHEST_UPGRADE.getId(),
				ModItems.GOLD_CHEST_UPGRADE.getId(),
				ModItems.DIAMOND_CHEST_UPGRADE.getId(),
				ModItems.NETHERITE_CHEST_UPGRADE.getId(),
				ModItems.UPGRADED_CHEST.getId(),
				ModItems.UPGRADED_BARREL.getId())
			.addStoryBoard("chest_upgrades", ChestUpgradeScenes::upgrades);

		helper.forComponents(
				ModBlocks.PRESSURE_CRUCIBLE.getId(),
				ModItems.EMERALD_DUST.getId(),
				ModItems.NETHERITE_DUST.getId())
			.addStoryBoard("pressure_crucible", CrucibleScenes::crucible);

		// The Basalt and Blackstone ones get their own scene first, then the full tour they share with cobblestone
		helper.forComponents(ModBlocks.generators().stream()
				.filter(g -> g.get().product() == GeneratorProduct.BASALT || g.get().product() == GeneratorProduct.BLACKSTONE)
				.map(DeferredBlock::getId).toList())
			.addStoryBoard("stone_generators", NewBlockScenes::stoneGenerators);
		// Tuff has its own scene; every other generator gets the full cobblestone tour
		helper.forComponents(ModBlocks.generators().stream().filter(g -> g.get().product() != GeneratorProduct.TUFF)
				.map(DeferredBlock::getId).toList())
			.addStoryBoard("cobble_generator", CobbleGeneratorScenes::generator);

		helper.forComponents(ModBlocks.MAGMA_ENGINE.getId(), ModBlocks.SUPERCHARGED_MAGMA_ENGINE.getId())
			.addStoryBoard("magma_engine", NewBlockScenes::magmaEngine);
		helper.forComponents(ModBlocks.INCINERATOR.getId())
			.addStoryBoard("incinerator", NewBlockScenes::incinerator);
		helper.forComponents(ModBlocks.STRING_TANGLER.getId())
			.addStoryBoard("string_tangler", MachineScenes::tangler);
		helper.forComponents(ModBlocks.STONE_INFUSER.getId())
			.addStoryBoard("stone_infuser", MachineScenes::infuser);
		helper.forComponents(ModBlocks.TUFF_GENERATOR.getId(), ModBlocks.WIRELESS_TUFF_GENERATOR.getId())
			.addStoryBoard("tuff_generator", MachineScenes::tuff);
		helper.forComponents(ModItems.BATTERY.getId())
			.addStoryBoard("battery", BatteryScenes::battery)
			.addStoryBoard("industrial_battery", BatteryScenes::industrialBattery);
		helper.forComponents(ModBlocks.LAVA_GENERATOR.getId())
			.addStoryBoard("electric_power", ElectricScenes::lavaGenerator);
		helper.forComponents(ModBlocks.ELECTRIC_MOTOR.getId())
			.addStoryBoard("electric_power", ElectricScenes::electricMotor);
		helper.forComponents(ModBlocks.CLOCKWORK_DYNAMO.getId())
			.addStoryBoard("clockwork_dynamo", ElectricScenes::clockworkDynamo);
		helper.forComponents(ModBlocks.GEOTHERMAL_CONTROLLER.getId(), ModBlocks.GEOTHERMAL_CASING.getId(),
				ModBlocks.GEOTHERMAL_CORE.getId())
			.addStoryBoard("geothermal_turbine", ElectricScenes::geothermalTurbine);
		helper.forComponents(ModBlocks.AUTO_HARVESTER.getId())
			.addStoryBoard("auto_harvester", FarmScenes::harvester);
		helper.forComponents(ModBlocks.ANIMAL_PEN.getId(), ModBlocks.BRASS_ANIMAL_PEN.getId(), ModItems.CAPTURE_BOTTLE.getId())
			.addStoryBoard("animal_pen", FarmScenes::animalPen);
		// the Monster Cages: the dry ones and the jar, the Flooded ones, the Netherite one; then the Fish Tanks
		helper.forComponents(ModBlocks.MONSTER_CAGE.getId(), ModBlocks.BRASS_MONSTER_CAGE.getId(), ModItems.IRON_JAR.getId(),
				ModItems.MONSTER_CHOW.getId())
			.addStoryBoard("monster_cage", CageScenes::monsterCage);
		helper.forComponents(ModBlocks.FLOODED_MONSTER_CAGE.getId(), ModBlocks.BRASS_FLOODED_MONSTER_CAGE.getId())
			.addStoryBoard("flooded_monster_cage", CageScenes::floodedCage)
			.addStoryBoard("monster_cage", CageScenes::monsterCage);
		helper.forComponents(ModBlocks.NETHERITE_MONSTER_CAGE.getId(), ModItems.NETHERITE_JAR.getId())
			.addStoryBoard("netherite_monster_cage", CageScenes::containment);
		helper.forComponents(ModBlocks.FISH_TANK.getId(), ModBlocks.BRASS_FISH_TANK.getId(), ModItems.FISH_FOOD.getId())
			.addStoryBoard("fish_tank", CageScenes::fishTank);
		helper.forComponents(ModBlocks.TRADE_BOT.getId())
			.addStoryBoard("trade_bot", TradeBotScenes::station)
			.addStoryBoard("trade_bot_linking", TradeBotScenes::linking)
			.addStoryBoard("trade_bot_trading", TradeBotScenes::trading);
		// Helper Bot: driving a train, keeping shop
		helper.forComponents(ModBlocks.HELPER_BOT.getId())
			.addStoryBoard("helper_bot_driving", HelperBotScenes::driving)
			.addStoryBoard("helper_bot_shop", HelperBotScenes::shop);
		helper.forComponents(ModBlocks.CUCKOO_CLOCK.getId())
			.addStoryBoard("cuckoo_clock", ClockScenes::cuckooClock);
		if (SableCompat.LOADED)
			AeronauticsScenes.register(helper);
	}

	@Override
	public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
		helper.addToTag(CREATE_FLUIDS_TAG)
			.add(ModBlocks.WELLSPRING.getId())
			.add(ModBlocks.COPPER_FLUID_LINE.getId())
			.add(ModBlocks.BRASS_FLUID_LINE.getId());
		helper.addToTag(CREATE_KINETIC_APPLIANCES_TAG)
			.add(ModBlocks.CLOCKWORK_DYNAMO.getId())
			.add(ModBlocks.PRESSURE_CRUCIBLE.getId())
			.add(ModBlocks.ANDESITE_COBBLE_GENERATOR.getId())
			.add(ModBlocks.BRASS_COBBLE_GENERATOR.getId())
			.add(ModBlocks.ANDESITE_BASALT_GENERATOR.getId())
			.add(ModBlocks.BRASS_BASALT_GENERATOR.getId())
			.add(ModBlocks.ANDESITE_BLACKSTONE_GENERATOR.getId())
			.add(ModBlocks.BRASS_BLACKSTONE_GENERATOR.getId())
			.add(ModBlocks.INCINERATOR.getId())
			.add(ModBlocks.STRING_TANGLER.getId())
			.add(ModBlocks.STONE_INFUSER.getId())
			.add(ModBlocks.TUFF_GENERATOR.getId())
			.add(ModBlocks.AUTO_HARVESTER.getId())
			.add(ModBlocks.ANIMAL_PEN.getId())
			.add(ModBlocks.BRASS_ANIMAL_PEN.getId())
			.add(ModBlocks.MONSTER_CAGE.getId())
			.add(ModBlocks.BRASS_MONSTER_CAGE.getId())
			.add(ModBlocks.FLOODED_MONSTER_CAGE.getId())
			.add(ModBlocks.BRASS_FLOODED_MONSTER_CAGE.getId())
			.add(ModBlocks.NETHERITE_MONSTER_CAGE.getId())
			.add(ModBlocks.FISH_TANK.getId())
			.add(ModBlocks.BRASS_FISH_TANK.getId())
			.add(ModBlocks.CUCKOO_CLOCK.getId());
		helper.addToTag(CREATE_KINETIC_SOURCES_TAG)
			.add(ModBlocks.MAGMA_ENGINE.getId())
			.add(ModBlocks.SUPERCHARGED_MAGMA_ENGINE.getId())
			.add(ModBlocks.ELECTRIC_MOTOR.getId())
			.add(ModBlocks.COPPER_POWER_LINE.getId())
			.add(ModBlocks.BRASS_POWER_LINE.getId());
		if (SableCompat.LOADED)
			helper.addToTag(CREATE_KINETIC_APPLIANCES_TAG)
				.add(AeronauticsContent.COMPACT_GYROSCOPE.getId())
				.add(AeronauticsContent.TORQUE_ROTOR.getId());
	}
}
