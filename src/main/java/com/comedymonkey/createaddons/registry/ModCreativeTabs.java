package com.comedymonkey.createaddons.registry;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.compat.sable.AeronauticsContent;
import com.comedymonkey.createaddons.compat.sable.SableCompat;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
	public static final DeferredRegister<CreativeModeTab> TABS =
		DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ComedyMonkeysCreateAddons.MOD_ID);

	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
		() -> CreativeModeTab.builder()
			.title(Component.translatable("itemGroup." + ComedyMonkeysCreateAddons.MOD_ID))
			.icon(() -> new ItemStack(ModItems.COMEDYMONKEY_HEAD.get()))
			.displayItems((params, output) -> {
				output.accept(ModItems.WELLSPRING.get());
				output.accept(ModItems.UPGRADE_TEMPLATE.get());
				output.accept(ModItems.DRIP_TEMPLATE.get());
				output.accept(ModItems.WATER_UPGRADE.get());
				output.accept(ModItems.DRIPSTONE_UPGRADE.get());
				output.accept(ModItems.HONEY_UPGRADE.get());
				output.accept(ModItems.CHOCOLATE_UPGRADE.get());
				output.accept(ModItems.CAPACITY_UPGRADE.get());
				output.accept(ModItems.CHEST_UPGRADE_TEMPLATE.get());
				output.accept(ModItems.COPPER_CHEST_UPGRADE.get());
				output.accept(ModItems.IRON_CHEST_UPGRADE.get());
				output.accept(ModItems.GOLD_CHEST_UPGRADE.get());
				output.accept(ModItems.DIAMOND_CHEST_UPGRADE.get());
				output.accept(ModItems.NETHERITE_CHEST_UPGRADE.get());
				output.accept(ModItems.ANDESITE_COBBLE_GENERATOR.get());
				output.accept(ModItems.WIRELESS_ANDESITE_COBBLE_GENERATOR.get());
				output.accept(ModItems.BRASS_COBBLE_GENERATOR.get());
				output.accept(ModItems.WIRELESS_BRASS_COBBLE_GENERATOR.get());
				output.accept(ModItems.ANDESITE_BASALT_GENERATOR.get());
				output.accept(ModItems.WIRELESS_ANDESITE_BASALT_GENERATOR.get());
				output.accept(ModItems.BRASS_BASALT_GENERATOR.get());
				output.accept(ModItems.WIRELESS_BRASS_BASALT_GENERATOR.get());
				output.accept(ModItems.ANDESITE_BLACKSTONE_GENERATOR.get());
				output.accept(ModItems.WIRELESS_ANDESITE_BLACKSTONE_GENERATOR.get());
				output.accept(ModItems.BRASS_BLACKSTONE_GENERATOR.get());
				output.accept(ModItems.WIRELESS_BRASS_BLACKSTONE_GENERATOR.get());
				output.accept(ModItems.TUFF_GENERATOR.get());
				output.accept(ModItems.WIRELESS_TUFF_GENERATOR.get());
				output.accept(ModItems.AUTO_HARVESTER.get());
				output.accept(ModItems.ANIMAL_PEN.get());
				output.accept(ModItems.BRASS_ANIMAL_PEN.get());
				output.accept(ModItems.CAPTURE_BOTTLE.get());
				output.accept(ModItems.MONSTER_CAGE.get());
				output.accept(ModItems.BRASS_MONSTER_CAGE.get());
				output.accept(ModItems.FLOODED_MONSTER_CAGE.get());
				output.accept(ModItems.BRASS_FLOODED_MONSTER_CAGE.get());
				output.accept(ModItems.NETHERITE_MONSTER_CAGE.get());
				output.accept(ModItems.IRON_JAR.get());
				output.accept(ModItems.NETHERITE_JAR.get());
				output.accept(ModItems.MONSTER_CHOW.get());
				output.accept(ModItems.CUCKOO_CLOCK.get());
				output.accept(ModItems.TRADE_BOT.get());
				output.accept(ModItems.STRING_TANGLER.get());
				output.accept(ModItems.STONE_INFUSER.get());
				output.accept(ModItems.PRESSURE_CRUCIBLE.get());
				output.accept(ModItems.MAGMA_ENGINE.get());
				output.accept(ModItems.SUPERCHARGED_MAGMA_ENGINE.get());
				output.accept(ModItems.LAVA_GENERATOR.get());
				output.accept(ModItems.BATTERY.get());
				output.accept(ModItems.ELECTRIC_MOTOR.get());
				output.accept(ModItems.FE_COIL.get());
				output.accept(ModItems.GEOTHERMAL_CONTROLLER.get());
				output.accept(ModItems.GEOTHERMAL_CASING.get());
				output.accept(ModItems.GEOTHERMAL_CORE.get());
				// the Pipez Covers are retired (Fluid and Power Lines replace them): placed ones keep working, no new ones
				output.accept(ModItems.INCINERATOR.get());
				output.accept(ModItems.COPPER_FLUID_LINE.get());
				output.accept(ModItems.BRASS_FLUID_LINE.get());
				output.accept(ModItems.COPPER_POWER_LINE.get());
				output.accept(ModItems.BRASS_POWER_LINE.get());
				output.accept(ModItems.LINE_POST.get());
				output.accept(ModItems.INTAKE.get());
				output.accept(ModItems.VALVE.get());
				output.accept(ModItems.FILTER.get());
				output.accept(ModItems.METER.get());
				output.accept(ModItems.SPLITTER.get());
				output.accept(ModItems.BREAKER.get());
				if (SableCompat.LOADED)
					AeronauticsContent.addToTab(output);
				output.accept(ModItems.EMERALD_DUST.get());
				output.accept(ModItems.NETHERITE_DUST.get());
				output.accept(ModItems.QUARTZ_DUST.get());
			})
			.build());

	private ModCreativeTabs() {}
}
