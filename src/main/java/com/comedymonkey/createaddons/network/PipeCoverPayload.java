package com.comedymonkey.createaddons.network;

import java.util.Optional;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.compat.pipez.cover.PipeCover;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: the cover on the Pipez pipe at {@code pos}, or none. Pipez's own sync doesn't carry our data, so
 * covers are sent when a chunk is sent and whenever one changes.
 */
public record PipeCoverPayload(BlockPos pos, Optional<PipeCover> cover) implements CustomPacketPayload {
	public static final Type<PipeCoverPayload> TYPE = new Type<>(ComedyMonkeysCreateAddons.asResource("pipe_cover"));
	public static final StreamCodec<ByteBuf, PipeCoverPayload> STREAM_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, PipeCoverPayload::pos,
		ByteBufCodecs.optional(PipeCover.STREAM_CODEC), PipeCoverPayload::cover,
		PipeCoverPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
