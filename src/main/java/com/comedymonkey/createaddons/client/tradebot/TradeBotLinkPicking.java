package com.comedymonkey.createaddons.client.tradebot;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.tradebot.TradeBotLinks;
import com.comedymonkey.createaddons.content.tradebot.TradeBotStationBlockEntity;
import com.comedymonkey.createaddons.network.TradeBotStationPayload;

import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Picking containers for Trade Bot in the world (D61), started from his station screen's "Select containers": with
 * any hand, right-click a container to cycle it through takes from (cyan) and delivers to (amber) to unlinked, or
 * left-click to unlink it. The server takes those clicks over and does the linking (TradeBotPicking); here they're
 * cancelled too, so nothing opens or breaks even for a moment, and the links are outlined. Sneak-right-click, or
 * right-click the station, to finish; walking well out of his range or three idle minutes finish it too.
 */
public final class TradeBotLinkPicking {
	private static final int TIMEOUT_TICKS = 20 * 60 * 3;
	private static final Object STATION_KEY = new Object();

	@Nullable
	private static BlockPos station;
	private static int idleTicks;

	private TradeBotLinkPicking() {}

	public static void start(BlockPos pos) {
		station = pos.immutable();
		idleTicks = 0;
		PacketDistributor.sendToServer(new TradeBotStationPayload(station, TradeBotStationPayload.Action.START_PICKING, station));
		hint();
	}

	public static boolean active() {
		return station != null;
	}

	private static void finish() {
		station = null;
		Minecraft mc = Minecraft.getInstance();
		mc.gui.setOverlayMessage(Component.translatable("message.comedymonkeysaddons.trade_bot.picking_done"), false);
	}

	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (station == null || !event.getLevel().isClientSide)
			return;
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.SUCCESS);
		if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND)
			return;
		idleTicks = 0;
		// the server links it (or finishes, for sneak-right-click and the station); here it just finishes too
		if (event.getEntity().isShiftKeyDown() || event.getPos().equals(station))
			finish();
	}

	/** Left-click unlinks (and never breaks the container by accident while picking). */
	public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
		if (station == null || !event.getLevel().isClientSide)
			return;
		event.setCanceled(true);
		idleTicks = 0;
	}

	public static void onClientTick(ClientTickEvent.Post event) {
		if (station == null)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.getConnection() == null || mc.level == null) {
			station = null; // disconnected: there's no server to tell
			return;
		}
		TradeBotStationBlockEntity be = station();
		int range = CMConfig.SERVER.tradeBotRange() + 16;
		if (be == null || mc.player == null || mc.player.distanceToSqr(station.getCenter()) > (double) range * range || ++idleTicks > TIMEOUT_TICKS) {
			PacketDistributor.sendToServer(new TradeBotStationPayload(station, TradeBotStationPayload.Action.STOP_PICKING, station));
			station = null;
			return;
		}
		Outliner outliner = Outliner.getInstance();
		outliner.showAABB(STATION_KEY, new AABB(station)).colored(0xE6DEC8).lineWidth(1 / 32f);
		for (BlockPos input : be.inputPositions())
			outliner.showAABB(input, new AABB(input)).colored(TradeBotLinks.TAKES_COLOUR).lineWidth(1 / 16f);
		BlockPos output = be.outputPosition();
		if (output != null)
			outliner.showAABB(output, new AABB(output)).colored(TradeBotLinks.DELIVERS_COLOUR).lineWidth(1 / 16f);
		if (mc.level.getGameTime() % 30 == 0)
			hint();
	}

	@Nullable
	private static TradeBotStationBlockEntity station() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level != null && station != null && mc.level.getBlockEntity(station) instanceof TradeBotStationBlockEntity be ? be : null;
	}

	private static void hint() {
		Minecraft.getInstance().gui.setOverlayMessage(Component.translatable("message.comedymonkeysaddons.trade_bot.picking"), false);
	}
}
