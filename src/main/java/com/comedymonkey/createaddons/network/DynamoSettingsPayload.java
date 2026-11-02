package com.comedymonkey.createaddons.network;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.electric.ClockworkDynamoBlockEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: a new stress setting for the Clockwork Dynamo at {@code pos}, from its screen. */
public record DynamoSettingsPayload(BlockPos pos, int stress) implements CustomPacketPayload {
	public static final Type<DynamoSettingsPayload> TYPE = new Type<>(ComedyMonkeysCreateAddons.asResource("dynamo_settings"));
	public static final StreamCodec<ByteBuf, DynamoSettingsPayload> STREAM_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, DynamoSettingsPayload::pos,
		ByteBufCodecs.VAR_INT, DynamoSettingsPayload::stress,
		DynamoSettingsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(DynamoSettingsPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Player player = context.player();
			// As the motor's: close by, and allowed to change things here
			if (player.distanceToSqr(payload.pos().getCenter()) > 64 || !player.mayBuild()
				|| !player.level().mayInteract(player, payload.pos()))
				return;
			if (player.level().getBlockEntity(payload.pos()) instanceof ClockworkDynamoBlockEntity dynamo)
				dynamo.applySettings(payload.stress());
		});
	}
}
