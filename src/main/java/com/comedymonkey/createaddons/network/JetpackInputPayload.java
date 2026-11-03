package com.comedymonkey.createaddons.network;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.jetpack.JetpackFlight;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server, whenever it changes: which Clockwork Jetpack controls the player is holding (Jump up, Sneak down,
 * the movement keys, Jump to boost in flight). The client moves the player; the server only needs this to spend the
 * right wind and keep the flight from being taken for cheating.
 */
public record JetpackInputPayload(int keys) implements CustomPacketPayload {
	public static final int UP = 1, DOWN = 2, MOVING = 4;

	public static final Type<JetpackInputPayload> TYPE = new Type<>(ComedyMonkeysCreateAddons.asResource("jetpack_input"));
	public static final StreamCodec<ByteBuf, JetpackInputPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(JetpackInputPayload::new, JetpackInputPayload::keys);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(JetpackInputPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player() instanceof ServerPlayer player)
				JetpackFlight.setInput(player, payload.keys() & (UP | DOWN | MOVING));
		});
	}
}
