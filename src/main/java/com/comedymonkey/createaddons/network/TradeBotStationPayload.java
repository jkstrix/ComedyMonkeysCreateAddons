package com.comedymonkey.createaddons.network;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.tradebot.TradeBotStationBlockEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server: something done from Trade Bot's station screen (D61): start or stop picking containers (the
 * clicks themselves are handled on the server, see TradeBotPicking), unlink a container, or call him home.
 */
public record TradeBotStationPayload(BlockPos station, Action action, BlockPos target) implements CustomPacketPayload {
	public enum Action { START_PICKING, STOP_PICKING, UNLINK, CALL_HOME }

	public static final Type<TradeBotStationPayload> TYPE = new Type<>(ComedyMonkeysCreateAddons.asResource("trade_bot_station"));
	public static final StreamCodec<ByteBuf, TradeBotStationPayload> STREAM_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, TradeBotStationPayload::station,
		ByteBufCodecs.idMapper(i -> Action.values()[i], Action::ordinal), TradeBotStationPayload::action,
		BlockPos.STREAM_CODEC, TradeBotStationPayload::target,
		TradeBotStationPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(TradeBotStationPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player) || !player.mayBuild())
				return;
			if (player.level().getBlockEntity(payload.station()) instanceof TradeBotStationBlockEntity station)
				station.handle(player, payload.action(), payload.target());
		});
	}
}
