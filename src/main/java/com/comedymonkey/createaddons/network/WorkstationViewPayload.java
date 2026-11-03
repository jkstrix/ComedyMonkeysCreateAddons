package com.comedymonkey.createaddons.network;

import java.util.Optional;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Server to client, the goggles' answer to a {@link WorkstationQueryPayload}. About a villager ({@code aboutVillager}):
 * its id, its workstation if it has one in this dimension ({@code claimed} is true for one elsewhere too), whether it's
 * only on its way there, how long it leaves alone the one it was made to give up, and its trades. About a workstation:
 * its position, the id of the villager holding it (-1 if none is in sight), whether anyone has claimed it, and whether
 * that villager is only on its way.
 */
public record WorkstationViewPayload(boolean aboutVillager, int villager, Optional<BlockPos> station, boolean claimed, boolean heading,
									 int ignoringFor, MerchantOffers offers) implements CustomPacketPayload {
	public static final Type<WorkstationViewPayload> TYPE = new Type<>(ComedyMonkeysCreateAddons.asResource("workstation_view"));

	public static final StreamCodec<RegistryFriendlyByteBuf, WorkstationViewPayload> STREAM_CODEC = StreamCodec.of(
		(buf, p) -> {
			buf.writeBoolean(p.aboutVillager);
			buf.writeVarInt(p.villager + 1);
			ByteBufCodecs.optional(BlockPos.STREAM_CODEC).encode(buf, p.station);
			buf.writeBoolean(p.claimed);
			buf.writeBoolean(p.heading);
			buf.writeVarInt(p.ignoringFor);
			MerchantOffers.STREAM_CODEC.encode(buf, p.offers);
		},
		buf -> new WorkstationViewPayload(buf.readBoolean(), buf.readVarInt() - 1, ByteBufCodecs.optional(BlockPos.STREAM_CODEC).decode(buf),
			buf.readBoolean(), buf.readBoolean(), buf.readVarInt(), MerchantOffers.STREAM_CODEC.decode(buf)));

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
