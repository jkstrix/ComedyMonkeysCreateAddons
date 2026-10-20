package com.comedymonkey.createaddons.advancement;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The mod's advancement trigger, and helpers to fire it. The advancements themselves are data, in
 * data/comedymonkeysaddons/advancement/.
 */
public final class ModAdvancements {
	public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
		DeferredRegister.create(Registries.TRIGGER_TYPE, ComedyMonkeysCreateAddons.MOD_ID);

	public static final DeferredHolder<CriterionTrigger<?>, CMActionTrigger> ACTION = TRIGGERS.register("action", CMActionTrigger::new);

	// Action names used by the advancement files
	public static final String WELLSPRING_UPGRADE = "wellspring_upgrade";
	public static final String CHEST_UPGRADE = "chest_upgrade";
	public static final String BARREL_UPGRADE = "barrel_upgrade";
	public static final String CHEST_EXTENDED = "chest_extended";
	public static final String CRUCIBLE_PRESS = "crucible_press";
	public static final String PIPED_BLAZE_BURNER = "piped_blaze_burner";
	public static final String PIPED_ENGINE_FUEL = "piped_engine_fuel";
	public static final String TURBINE_FORMED = "turbine_formed";
	public static final String MOTOR_FULL_POWER = "motor_full_power";

	/** How close a player has to be to machinery working on its own to earn its advancements. */
	public static final int NEARBY = 16;

	private ModAdvancements() {}

	public static void award(Player player, String action, ItemStack stack, int value) {
		if (player instanceof ServerPlayer serverPlayer)
			ACTION.get().trigger(serverPlayer, action, stack, value);
	}

	public static void award(Player player, String action) {
		award(player, action, ItemStack.EMPTY, 0);
	}

	/** For things automation does: everyone within {@link #NEARBY} blocks gets the credit. */
	public static void awardNearby(Level level, BlockPos pos, String action, ItemStack stack, int value) {
		if (level == null || level.isClientSide)
			return;
		for (Player player : level.players())
			if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= NEARBY * NEARBY)
				award(player, action, stack, value);
	}
}
