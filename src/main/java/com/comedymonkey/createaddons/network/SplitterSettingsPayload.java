package com.comedymonkey.createaddons.network;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.line.Fitting;
import com.comedymonkey.createaddons.content.line.PowerLineBlockEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: a Power Line Splitter's priority for each branch (by direction: 0 first, 1 second, 2 last), from its screen. */
public record SplitterSettingsPayload(BlockPos pos, byte[] priorities) implements CustomPacketPayload {
	public static final Type<SplitterSettingsPayload> TYPE = new Type<>(ComedyMonkeysCreateAddons.asResource("splitter_settings"));
	public static final StreamCodec<ByteBuf, SplitterSettingsPayload> STREAM_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, SplitterSettingsPayload::pos,
		ByteBufCodecs.BYTE_ARRAY, SplitterSettingsPayload::priorities,
		SplitterSettingsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(SplitterSettingsPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Player player = context.player();
			// Close by, and allowed to change things, as for the Electric Motor's settings
			if (player.distanceToSqr(payload.pos().getCenter()) > 64 || !player.mayBuild()
				|| !player.level().mayInteract(player, payload.pos()))
				return;
			if (player.level().getBlockEntity(payload.pos()) instanceof PowerLineBlockEntity be && be.fitting() == Fitting.SPLITTER)
				be.setPriorities(payload.priorities());
		});
	}
}
