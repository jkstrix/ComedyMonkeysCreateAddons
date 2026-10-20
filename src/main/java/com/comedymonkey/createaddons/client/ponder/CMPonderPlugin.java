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
		helper.forComponents(ModBlocks.WELLSPRING.getId())
			.addStoryBoard("wellspring", WellspringScenes::wellspring);

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
		helper.forComponents(ModBlocks.LAVA_GENERATOR.getId())
			.addStoryBoard("electric_power", ElectricScenes::lavaGenerator);
		helper.forComponents(ModBlocks.ELECTRIC_MOTOR.getId())
			.addStoryBoard("electric_power", ElectricScenes::electricMotor);
		helper.forComponents(ModBlocks.GEOTHERMAL_CONTROLLER.getId(), ModBlocks.GEOTHERMAL_CASING.getId(),
				ModBlocks.GEOTHERMAL_CORE.getId())
			.addStoryBoard("geothermal_turbine", ElectricScenes::geothermalTurbine);
		if (SableCompat.LOADED)
			AeronauticsScenes.register(helper);
	}

	@Override
	public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
		helper.addToTag(CREATE_FLUIDS_TAG)
			.add(ModBlocks.WELLSPRING.getId());
		helper.addToTag(CREATE_KINETIC_APPLIANCES_TAG)
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
			.add(ModBlocks.TUFF_GENERATOR.getId());
		helper.addToTag(CREATE_KINETIC_SOURCES_TAG)
			.add(ModBlocks.MAGMA_ENGINE.getId())
			.add(ModBlocks.SUPERCHARGED_MAGMA_ENGINE.getId())
			.add(ModBlocks.ELECTRIC_MOTOR.getId());
		if (SableCompat.LOADED)
			helper.addToTag(CREATE_KINETIC_APPLIANCES_TAG)
				.add(AeronauticsContent.COMPACT_GYROSCOPE.getId())
				.add(AeronauticsContent.TORQUE_ROTOR.getId());
	}
}
