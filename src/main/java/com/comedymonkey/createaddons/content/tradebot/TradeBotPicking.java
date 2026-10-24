package com.comedymonkey.createaddons.content.tradebot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.config.CMConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Who is picking containers for which station, on the server (D61). While a player picks, their clicks on blocks
 * are taken over here: right-click cycles the container's link, left-click unlinks it, so chests never open and
 * nothing gets broken. Sneak-right-click or the station itself finishes; so do leaving his range, changing
 * dimension, three idle minutes or logging out. (Cancelling only on the client isn't enough: the click still
 * reaches the server.)
 */
public final class TradeBotPicking {
	private static final long TIMEOUT_TICKS = 20 * 60 * 3;

	private record Picker(BlockPos station, ResourceKey<Level> dimension, long lastUsed) {}

	private static final Map<UUID, Picker> PICKERS = new HashMap<>();

	private TradeBotPicking() {}

	public static void start(ServerPlayer player, BlockPos station) {
		PICKERS.put(player.getUUID(), new Picker(station.immutable(), player.level().dimension(), player.level().getGameTime()));
	}

	public static void stop(ServerPlayer player) {
		PICKERS.remove(player.getUUID());
	}

	public static void clear() {
		PICKERS.clear();
	}

	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		PICKERS.remove(event.getEntity().getUUID());
	}

	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		Picker picker = current(player);
		if (picker == null)
			return;
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.SUCCESS);
		if (event.getHand() != InteractionHand.MAIN_HAND)
			return; // the event comes once per hand; act on one
		touch(player, picker);
		if (player.isShiftKeyDown() || event.getPos().equals(picker.station())) {
			stop(player);
			player.displayClientMessage(Component.translatable("message.comedymonkeysaddons.trade_bot.picking_done"), true);
			return;
		}
		if (player.level().getBlockEntity(picker.station()) instanceof TradeBotStationBlockEntity station)
			station.pick(player, event.getPos());
	}

	public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		Picker picker = current(player);
		if (picker == null)
			return;
		event.setCanceled(true);
		if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START)
			return;
		touch(player, picker);
		if (player.level().getBlockEntity(picker.station()) instanceof TradeBotStationBlockEntity station
			&& station.roleOf(event.getPos()) != TradeBotLinks.Role.NONE)
			station.handle(player, com.comedymonkey.createaddons.network.TradeBotStationPayload.Action.UNLINK, event.getPos());
	}

	/** The player's picking, if it's still going (same dimension, in range, not idle too long, station still there). */
	@Nullable
	private static Picker current(ServerPlayer player) {
		Picker picker = PICKERS.get(player.getUUID());
		if (picker == null)
			return null;
		int range = CMConfig.SERVER.tradeBotRange() + 16;
		boolean valid = player.level().dimension() == picker.dimension()
			&& player.level().getGameTime() - picker.lastUsed() <= TIMEOUT_TICKS
			&& player.distanceToSqr(picker.station().getCenter()) <= (double) range * range
			&& player.level().getBlockEntity(picker.station()) instanceof TradeBotStationBlockEntity;
		if (!valid) {
			stop(player);
			return null;
		}
		return picker;
	}

	private static void touch(ServerPlayer player, Picker picker) {
		PICKERS.put(player.getUUID(), new Picker(picker.station(), picker.dimension(), player.level().getGameTime()));
	}
}
