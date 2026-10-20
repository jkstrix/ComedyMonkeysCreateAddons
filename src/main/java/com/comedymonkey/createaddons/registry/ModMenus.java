package com.comedymonkey.createaddons.registry;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.chest.ChestUpgradeMenu;
import com.comedymonkey.createaddons.content.chest.LargeUpgradedChestMenu;
import com.comedymonkey.createaddons.content.chest.UpgradedChestMenu;
import com.comedymonkey.createaddons.content.harvester.AutoHarvesterMenu;
import com.comedymonkey.createaddons.content.wellspring.WellspringMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
	public static final DeferredRegister<MenuType<?>> MENUS =
		DeferredRegister.create(Registries.MENU, ComedyMonkeysCreateAddons.MOD_ID);

	public static final DeferredHolder<MenuType<?>, MenuType<WellspringMenu>> WELLSPRING =
		MENUS.register("wellspring", () -> IMenuTypeExtension.create(WellspringMenu::new));

	public static final DeferredHolder<MenuType<?>, MenuType<UpgradedChestMenu>> UPGRADED_CHEST =
		MENUS.register("upgraded_chest", () -> IMenuTypeExtension.create(UpgradedChestMenu::new));

	public static final DeferredHolder<MenuType<?>, MenuType<LargeUpgradedChestMenu>> LARGE_UPGRADED_CHEST =
		MENUS.register("large_upgraded_chest", () -> IMenuTypeExtension.create(LargeUpgradedChestMenu::new));

	public static final DeferredHolder<MenuType<?>, MenuType<ChestUpgradeMenu>> CHEST_UPGRADE =
		MENUS.register("chest_upgrade", () -> IMenuTypeExtension.create(ChestUpgradeMenu::new));

	public static final DeferredHolder<MenuType<?>, MenuType<AutoHarvesterMenu>> AUTO_HARVESTER =
		MENUS.register("auto_harvester", () -> IMenuTypeExtension.create(AutoHarvesterMenu::new));

	private ModMenus() {}
}
