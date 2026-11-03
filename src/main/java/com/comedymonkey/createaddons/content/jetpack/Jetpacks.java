package com.comedymonkey.createaddons.content.jetpack;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.compat.curios.JetpackCurios;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.MachineEnchantments;
import com.comedymonkey.createaddons.registry.ModDataComponents;
import com.simibubi.create.content.equipment.armor.BacktankItem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * The Clockwork Jetpack's wind and where it's worn (jetpack spec D12, D18, D21, D34). Wind is counted in ticks of
 * thrust: a full wind is {@code seconds_per_wind} seconds of it, a third more for each level of Overwound. Hover and
 * flight use it at the slower rates their own settings give.
 */
public final class Jetpacks {
	/** Create's Copper Backtank holds 900 air; a full base wind fills as a backtank's 900 would (spec D12, D40). */
	public static final int BACKTANK_AIR = 900;

	private Jetpacks() {}

	public static boolean isJetpack(ItemStack stack) {
		return stack.getItem() instanceof ClockworkJetpackItem;
	}

	/**
	 * The Clockwork Jetpack a player has on: the Jetpack slot first, then the chest (D18, D23). Empty if neither.
	 * The stack is the worn one itself, so writing its wind writes the jetpack's.
	 */
	public static ItemStack worn(LivingEntity entity) {
		if (ModList.get().isLoaded(ComedyMonkeysCreateAddons.CURIOS_ID)) {
			ItemStack curio = JetpackCurios.findJetpack(entity);
			if (!curio.isEmpty())
				return curio;
		}
		ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
		return isJetpack(chest) ? chest : ItemStack.EMPTY;
	}

	/** A Create backtank or Create Jetpack's jetpack: never worn with a Clockwork Jetpack (D23). */
	public static boolean isBacktank(ItemStack stack) {
		return stack.getItem() instanceof BacktankItem || BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("create_jetpack");
	}

	/** Takes off the jetpack {@link #worn} finds, from where it found it. Empty if none was on. */
	public static ItemStack takeOff(LivingEntity entity) {
		if (ModList.get().isLoaded(ComedyMonkeysCreateAddons.CURIOS_ID)) {
			ItemStack curio = JetpackCurios.takeJetpack(entity);
			if (!curio.isEmpty())
				return curio;
		}
		ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
		if (!isJetpack(chest))
			return ItemStack.EMPTY;
		entity.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
		return chest;
	}

	/**
	 * Puts a jetpack on: the Jetpack slot with Curios, or the chest without (D18), only if that's free. The stack is used
	 * up when it goes on; false and untouched if there was nowhere for it.
	 */
	public static boolean putOn(Player player, ItemStack jetpack) {
		if (ModList.get().isLoaded(ComedyMonkeysCreateAddons.CURIOS_ID))
			return JetpackCurios.tryEquip(player, jetpack);
		if (!player.getItemBySlot(EquipmentSlot.CHEST).isEmpty())
			return false;
		player.setItemSlot(EquipmentSlot.CHEST, jetpack.split(1));
		return true;
	}

	public static int overwound(ItemStack stack) {
		return MachineEnchantments.levelIn(stack.getTagEnchantments(), MachineEnchantments.OVERWOUND);
	}

	/** A full wind with no Overwound, in ticks of thrust. */
	public static int baseWind() {
		return Math.max(1, CMConfig.Server.get(CMConfig.SERVER.jetpackSecondsPerWind) * 20);
	}

	/** What this jetpack holds when full: a third more for each level of Overwound (D34). */
	public static int maxWind(ItemStack stack) {
		return maxWind(overwound(stack));
	}

	/** A full wind at this level of Overwound. */
	public static int maxWind(int overwound) {
		return baseWind() * (3 + Math.max(0, overwound)) / 3;
	}

	/** The wind in it, never more than full (the config or its Overwound may have changed since). */
	public static int wind(ItemStack stack) {
		return Mth.clamp(stack.getOrDefault(ModDataComponents.JETPACK_WIND.get(), 0), 0, maxWind(stack));
	}

	public static void setWind(ItemStack stack, int wind) {
		int clamped = Mth.clamp(wind, 0, maxWind(stack));
		if (clamped != stack.getOrDefault(ModDataComponents.JETPACK_WIND.get(), 0))
			stack.set(ModDataComponents.JETPACK_WIND.get(), clamped);
	}

	/** How wound, 0..1. */
	public static float fill(ItemStack stack) {
		return (float) wind(stack) / maxWind(stack);
	}

	public static boolean full(ItemStack stack) {
		return wind(stack) >= maxWind(stack);
	}

	/** Ticks of thrust a tick of hovering uses (450 s of thrust or 900 s of hover a wind: half). */
	public static double hoverRate() {
		return (double) CMConfig.Server.get(CMConfig.SERVER.jetpackSecondsPerWind) / Math.max(1, CMConfig.Server.get(CMConfig.SERVER.jetpackSecondsPerWindHover));
	}

	/** Ticks of thrust a tick of boosting in flight mode uses; gliding uses none (D16). */
	public static double flightRate() {
		return (double) CMConfig.Server.get(CMConfig.SERVER.jetpackSecondsPerWind) / Math.max(1, CMConfig.Server.get(CMConfig.SERVER.jetpackSecondsPerWindFlight));
	}

	/** Wind a Winding Station adds in one step of a backtank's fill (air added that step), scaled to this jetpack. */
	public static int windPerAir() {
		return Math.max(1, baseWind() / BACKTANK_AIR);
	}
}
