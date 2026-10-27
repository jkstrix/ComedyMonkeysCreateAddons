package com.comedymonkey.createaddons.network;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: what a driving Helper Bot's train is doing that clients can't see for themselves (spec, Technical
 * Notes): paused, driven by a player, lost (no path), derailed. Sent when it changes, and every few seconds so players
 * who arrive later catch up. {@code flags} is a set of the {@code HelperBotMovement} status bits.
 */
public record HelperBotStatusPayload(int entityId, BlockPos localPos, byte flags) implements CustomPacketPayload {
	public static final Type<HelperBotStatusPayload> TYPE = new Type<>(ComedyMonkeysCreateAddons.asResource("helper_bot_status"));

	public static final StreamCodec<RegistryFriendlyByteBuf, HelperBotStatusPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, HelperBotStatusPayload::entityId,
		BlockPos.STREAM_CODEC, HelperBotStatusPayload::localPos,
		ByteBufCodecs.BYTE, HelperBotStatusPayload::flags,
		HelperBotStatusPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
