package com.comedymonkey.createaddons.network;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.chest.ChestSorting;
import com.comedymonkey.createaddons.content.chest.UpgradedChestView;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: sort the upgraded chest or barrel the player has open. */
public record SortChestPayload() implements CustomPacketPayload {
	public static final Type<SortChestPayload> TYPE = new Type<>(ComedyMonkeysCreateAddons.asResource("sort_chest"));
	public static final StreamCodec<ByteBuf, SortChestPayload> STREAM_CODEC = StreamCodec.unit(new SortChestPayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(SortChestPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Player player = context.player();
			// Not for spectators: vanilla ignores their clicks in a container too
			if (!player.isSpectator() && player.containerMenu instanceof UpgradedChestView view
				&& player.containerMenu.stillValid(player)) {
				ChestSorting.sort(view.getChestContainer());
				player.containerMenu.broadcastChanges();
			}
		});
	}
}
