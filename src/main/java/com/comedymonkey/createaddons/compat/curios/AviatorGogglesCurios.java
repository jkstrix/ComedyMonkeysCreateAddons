package com.comedymonkey.createaddons.compat.curios;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.simibubi.create.content.equipment.goggles.GogglesItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Makes Create Aeronautics' Aviator's Goggles count as "wearing goggles" while they sit in any Curios slot.
 * The item is put into the {@code curios:head} slot by the item tag in data/curios/tags/item/head.json.
 * Create's own Curios compat only recognises Create's Engineer's Goggles, and Aeronautics only checks the
 * vanilla helmet slot, so this predicate fills the gap for the overlay and anything else that asks Create.
 */
public final class AviatorGogglesCurios {
	public static final ResourceLocation GOGGLES_ID =
		ResourceLocation.fromNamespaceAndPath(ComedyMonkeysCreateAddons.AERONAUTICS_ID, "aviators_goggles");

	/** Resolved lazily, so there is no compile- or class-load-time dependency on Aeronautics. */
	public static final DeferredHolder<Item, Item> AVIATORS_GOGGLES = DeferredHolder.create(Registries.ITEM, GOGGLES_ID);

	private AviatorGogglesCurios() {}

	public static void init() {
		GogglesItem.addIsWearingPredicate(player -> AVIATORS_GOGGLES.isBound() && CuriosApi.getCuriosInventory(player)
			.map(inventory -> inventory.isEquipped(AviatorGogglesCurios::isAviatorsGoggles))
			.orElse(false));
	}

	public static boolean isAviatorsGoggles(ItemStack stack) {
		return AVIATORS_GOGGLES.isBound() && stack.is(AVIATORS_GOGGLES.get());
	}
}
