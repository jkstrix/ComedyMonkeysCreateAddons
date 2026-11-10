package com.comedymonkey.createaddons.advancement;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
	/** A Trade Master delivered into a gauge's chest (design/specs/trade-master.md P14). Value: how many. */
	public static final String TRADE_MASTER_FILL = "trade_master_fill";
	/** He sold surplus and bought what a gauge wanted with it, in one trip. */
	public static final String TRADE_MASTER_SUPPLY_CHAIN = "trade_master_supply_chain";
	/** He bought an enchanted book for a gauge. */
	public static final String TRADE_MASTER_BOOK = "trade_master_book";
	/** Every gauge on his boards full at once. Value: how many gauges. */
	public static final String TRADE_MASTER_STOCKED = "trade_master_stocked";
	/** A Jetpack Winding Station wound a jetpack full ("Wound and Ready"). */
	public static final String JETPACK_WOUND = "jetpack_wound";
	/** The jetpack's engine lifted its wearer ("Liftoff"). */
	public static final String JETPACK_FLIGHT = "jetpack_flight";
	/** Its wings spread ("Clockwork Wings"). */
	public static final String JETPACK_WINGS = "jetpack_wings";
	/** Its hover held its wearer in the air ("Station Keeping"). */
	public static final String JETPACK_HOVER = "jetpack_hover";
	/** It ran out of wind in mid-air ("What Goes Up"). */
	public static final String JETPACK_RUN_DOWN = "jetpack_run_down";
	/** Worn with Overwound. Value: the level ("Overwound" asks for III). */
	public static final String JETPACK_OVERWOUND = "jetpack_overwound";
	/** A Netherite Clockwork Jetpack running in lava ("The Floor Is Lava"). */
	public static final String JETPACK_LAVA = "jetpack_lava";
	/** Gauges that have to be full at once for "Fully Stocked". */
	public static final int TRADE_MASTER_STOCKED_GAUGES = 8;
	public static final String INTAKE_FITTED = "intake_fitted";
	public static final String LINE_ENCASED = "line_encased";
	/** A Pointed Dripstone Generator knocked off a Pointed Dripstone it grew ("Biggest Drip"). */
	public static final String DRIPSTONE_GROWN = "dripstone_grown";
	/** Value: the span's length in blocks. */
	public static final String SPAN_HUNG = "span_hung";
	/** A Clockwork Juggernaut woken by the ritual ("Wound Up", D41). */
	public static final String JUGGERNAUT_SUMMONED = "juggernaut_summoned";
	/** A Spring Engine wound full ("Tightly Wound", D41). */
	public static final String SPRING_ENGINE_FULL = "spring_engine_full";
	/** How close a player has to be to the ritual to see him wake (as the ritual's own messages). */
	public static final int SUMMON_NEARBY = 32;
	/** Trades one Trade Bot makes in a day for his challenge, and emeralds one trip earns for his goal. */
	public static final int TRADE_BOT_DAY_TRADES = 64, TRADE_BOT_STACK_EMERALDS = 64;
	/** Villagers one Cuckoo Clock has to keep time for, for its challenge. */
	public static final int CLOCK_MARKET_VILLAGERS = 20;

	/** How close a player has to be to machinery working on its own to earn its advancements. */
	public static final int NEARBY = 16;

	/**
	 * Game tests: told of every {@link #awardNearby} (their mock players aren't in the level, so who'd get it can't be
	 * seen any other way). Empty in play.
	 */
	public static final List<NearbyAward> TEST_LISTENERS = new CopyOnWriteArrayList<>();

	@FunctionalInterface
	public interface NearbyAward {
		void awarded(Level level, BlockPos pos, String action, int range);
	}

	/**
	 * Game tests: told of every award to a player, and of every award to a bot's owner, online or not (a test's owner
	 * never is). Empty in play.
	 */
	public static final List<Award> TEST_AWARDS = new CopyOnWriteArrayList<>();

	@FunctionalInterface
	public interface Award {
		void awarded(java.util.UUID player, String action, int value);
	}

	/** For game tests: a bot earned its owner {@code action} (whether or not they're online to get it). */
	public static void ownerAwarded(java.util.UUID owner, String action, int value) {
		for (Award listener : TEST_AWARDS)
			listener.awarded(owner, action, value);
	}

	private ModAdvancements() {}

	public static void award(Player player, String action, ItemStack stack, int value) {
		if (player != null)
			for (Award listener : TEST_AWARDS)
				listener.awarded(player.getUUID(), action, value);
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
		for (NearbyAward listener : TEST_LISTENERS)
			listener.awarded(level, pos, action, range);
		for (Player player : level.players())
			if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= (double) range * range)
				award(player, action, stack, value);
	}
}
