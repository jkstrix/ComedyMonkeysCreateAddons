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
	/** Value: the tower's height. */
	public static final String INDUSTRIAL_BATTERY_FORMED = "industrial_battery_formed";
	public static final String MOTOR_FULL_POWER = "motor_full_power";
	/** An Electric Motor running off a Clockwork Dynamo's power. */
	public static final String DYNAMO_MOTOR = "dynamo_motor";
	/** Items delivered by a Clockwork Receiver. Value: how many. */
	public static final String TUBE_DELIVERY = "tube_delivery";
	public static final String HARVESTER_HARVEST = "harvester_harvest";
	public static final String PEN_CULL = "pen_cull";
	public static final String PEN_FULL = "pen_full";
	public static final String BRASS_PEN_FULL = "brass_pen_full";
	public static final String CAGE_MONSTER = "cage_monster";
	public static final String CAGE_WITHER = "cage_wither";
	/** A blue axolotl born in a Fish Tank (1 in 1,200). */
	public static final String TANK_BLUE_AXOLOTL = "tank_blue_axolotl";
	public static final String CLOCK_RESTOCK = "clock_restock";
	public static final String CLOCK_NOON = "clock_noon";
	public static final String CLOCK_MARKET = "clock_market";
	public static final String TRADE_BOT_TRADE = "trade_bot_trade";
	public static final String TRADE_BOT_LEVEL_UP = "trade_bot_level_up";
	public static final String TRADE_BOT_STACK = "trade_bot_stack";
	public static final String TRADE_BOT_NUGGETS = "trade_bot_nuggets";
	public static final String TRADE_BOT_SCARED = "trade_bot_scared";
	public static final String TRADE_BOT_WOUND_UP = "trade_bot_wound_up";
	public static final String TRADE_BOT_SOAKED = "trade_bot_soaked";
	public static final String INTAKE_FITTED = "intake_fitted";
	public static final String LINE_ENCASED = "line_encased";
	/** Value: the span's length in blocks. */
	public static final String SPAN_HUNG = "span_hung";
	/** Trades one Trade Bot makes in a day for his challenge, and emeralds one trip earns for his goal. */
	public static final int TRADE_BOT_DAY_TRADES = 64, TRADE_BOT_STACK_EMERALDS = 64;
	/** Villagers one Cuckoo Clock has to keep time for, for its challenge. */
	public static final int CLOCK_MARKET_VILLAGERS = 20;

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
		awardNearby(level, pos, action, stack, value, NEARBY);
	}

	/** Everyone within {@code range} blocks of {@code pos} gets the credit. */
	public static void awardNearby(Level level, BlockPos pos, String action, ItemStack stack, int value, int range) {
		if (level == null || level.isClientSide)
			return;
		for (Player player : level.players())
			if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= (double) range * range)
				award(player, action, stack, value);
	}
}
