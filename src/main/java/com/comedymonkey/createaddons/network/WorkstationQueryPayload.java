package com.comedymonkey.createaddons.network;

import java.util.Optional;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.VillagerWorkstations;
import com.simibubi.create.content.equipment.goggles.GogglesItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server, from a player wearing Engineer's Goggles: what's the workstation of the villager {@code villager}
 * (an entity id), or, when that's -1, who holds the workstation at {@code pos}? Answered with a
 * {@link WorkstationViewPayload}; the client asks again every half second while it looks.
 */
public record WorkstationQueryPayload(int villager, BlockPos pos) implements CustomPacketPayload {
	public static final Type<WorkstationQueryPayload> TYPE = new Type<>(ComedyMonkeysCreateAddons.asResource("workstation_query"));
	public static final StreamCodec<FriendlyByteBuf, WorkstationQueryPayload> STREAM_CODEC = StreamCodec.of(
		(buf, p) -> {
			buf.writeVarInt(p.villager + 1);
			buf.writeBlockPos(p.pos);
		},
		buf -> new WorkstationQueryPayload(buf.readVarInt() - 1, buf.readBlockPos()));

	public static WorkstationQueryPayload ofVillager(int id) {
		return new WorkstationQueryPayload(id, BlockPos.ZERO);
	}

	public static WorkstationQueryPayload ofStation(BlockPos pos) {
		return new WorkstationQueryPayload(-1, pos);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(WorkstationQueryPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player) || player.isSpectator() || !GogglesItem.isWearingGoggles(player))
				return;
			ServerLevel level = player.serverLevel();
			double range = VillagerWorkstations.VIEW_RANGE * VillagerWorkstations.VIEW_RANGE;
			if (payload.villager >= 0) {
				if (!(level.getEntity(payload.villager) instanceof net.minecraft.world.entity.npc.Villager villager) || villager.distanceToSqr(player) > range)
					return;
				GlobalPos site = VillagerWorkstations.station(villager);
				Optional<BlockPos> station = site != null && site.dimension() == level.dimension() ? Optional.of(site.pos()) : Optional.empty();
				PacketDistributor.sendToPlayer(player, new WorkstationViewPayload(true, villager.getId(), station, site != null,
					VillagerWorkstations.heading(villager), VillagerWorkstations.ignoringFor(villager), villager.getOffers()));
				return;
			}
			BlockPos pos = payload.pos;
			if (!level.isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > range || !VillagerWorkstations.isWorkstation(level.getBlockState(pos)))
				return;
			var owner = VillagerWorkstations.owner(level, pos);
			PacketDistributor.sendToPlayer(player, new WorkstationViewPayload(false, owner == null ? -1 : owner.getId(), Optional.of(pos),
				owner != null || VillagerWorkstations.claimed(level, pos), owner != null && VillagerWorkstations.heading(owner), 0, new MerchantOffers()));
		});
	}
}
