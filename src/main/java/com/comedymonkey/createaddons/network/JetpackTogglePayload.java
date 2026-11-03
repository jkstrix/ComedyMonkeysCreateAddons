package com.comedymonkey.createaddons.network;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.jetpack.JetpackFlight;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ByIdMap;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: a Clockwork Jetpack key was pressed: G engine, H hover, J wings (jetpack spec D8, D14). */
public record JetpackTogglePayload(Toggle toggle) implements CustomPacketPayload {
	public enum Toggle { ENGINE, HOVER, WINGS }

	public static final Type<JetpackTogglePayload> TYPE = new Type<>(ComedyMonkeysCreateAddons.asResource("jetpack_toggle"));
	public static final StreamCodec<ByteBuf, JetpackTogglePayload> STREAM_CODEC = ByteBufCodecs.idMapper(ByIdMap.continuous(Toggle::ordinal, Toggle.values(), ByIdMap.OutOfBoundsStrategy.ZERO), Toggle::ordinal)
		.map(JetpackTogglePayload::new, JetpackTogglePayload::toggle);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(JetpackTogglePayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player() instanceof ServerPlayer player)
				JetpackFlight.toggle(player, payload.toggle());
		});
	}
}
