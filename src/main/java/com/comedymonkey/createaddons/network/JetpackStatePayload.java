package com.comedymonkey.createaddons.network;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.jetpack.JetpackState;
import com.comedymonkey.createaddons.registry.ModAttachments;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server to clients: a player's Clockwork Jetpack toggles, to them (their controls and gauge) and to everyone who can
 * see them (their wings).
 */
public record JetpackStatePayload(int entity, JetpackState state) implements CustomPacketPayload {
	public static final Type<JetpackStatePayload> TYPE = new Type<>(ComedyMonkeysCreateAddons.asResource("jetpack_state"));
	public static final StreamCodec<ByteBuf, JetpackStatePayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, JetpackStatePayload::entity,
		JetpackState.STREAM_CODEC, JetpackStatePayload::state,
		JetpackStatePayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(JetpackStatePayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Entity entity = context.player().level().getEntity(payload.entity());
			if (entity != null)
				entity.setData(ModAttachments.JETPACK, payload.state());
		});
	}
}
