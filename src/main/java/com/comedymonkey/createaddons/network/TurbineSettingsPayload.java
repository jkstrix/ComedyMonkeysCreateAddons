package com.comedymonkey.createaddons.network;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.electric.GeothermalControllerBlockEntity;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: a new power level (tenths of full output) for the Geothermal Turbine controller at {@code pos}, from its screen. */
public record TurbineSettingsPayload(BlockPos pos, int tenths) implements CustomPacketPayload {
	public static final Type<TurbineSettingsPayload> TYPE = new Type<>(ComedyMonkeysCreateAddons.asResource("turbine_settings"));
	public static final StreamCodec<ByteBuf, TurbineSettingsPayload> STREAM_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, TurbineSettingsPayload::pos,
		ByteBufCodecs.VAR_INT, TurbineSettingsPayload::tenths,
		TurbineSettingsPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(TurbineSettingsPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Player player = context.player();
			// Close by, and allowed to change things, as for the Electric Motor's settings
			if (player.distanceToSqr(payload.pos().getCenter()) > 64 || !player.mayBuild()
				|| !player.level().mayInteract(player, payload.pos()))
				return;
			if (player.level().getBlockEntity(payload.pos()) instanceof GeothermalControllerBlockEntity turbine)
				turbine.applyOutput(payload.tenths());
		});
	}
}
