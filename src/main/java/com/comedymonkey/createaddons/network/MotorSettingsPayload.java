package com.comedymonkey.createaddons.network;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.electric.ElectricMotorBlockEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: new settings for the Electric Motor at {@code pos}, from its screen. */
public record MotorSettingsPayload(BlockPos pos, int speed, int stress, boolean reversed) implements CustomPacketPayload {
	public static final Type<MotorSettingsPayload> TYPE = new Type<>(ComedyMonkeysCreateAddons.asResource("motor_settings"));
	public static final StreamCodec<ByteBuf, MotorSettingsPayload> STREAM_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, MotorSettingsPayload::pos,
		ByteBufCodecs.VAR_INT, MotorSettingsPayload::speed,
		ByteBufCodecs.VAR_INT, MotorSettingsPayload::stress,
		ByteBufCodecs.BOOL, MotorSettingsPayload::reversed,
		MotorSettingsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(MotorSettingsPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Player player = context.player();
			// Close by, and allowed to change things (not a spectator or in adventure mode, nor in someone's claim or
			// spawn protection). The screen is client-only, so there is no open menu to check against.
			if (player.distanceToSqr(payload.pos().getCenter()) > 64 || !player.mayBuild()
				|| !player.level().mayInteract(player, payload.pos()))
				return;
			if (player.level().getBlockEntity(payload.pos()) instanceof ElectricMotorBlockEntity motor)
				motor.applySettings(payload.speed(), payload.stress(), payload.reversed());
		});
	}
}
