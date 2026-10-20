package com.comedymonkey.createaddons.network;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.QuickTransfer;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: a quick move in the screen the player has open (a button, or the mouse wheel over a slot). */
public record QuickTransferPayload(int containerId, QuickTransfer.Action action, int slot) implements CustomPacketPayload {
	public static final Type<QuickTransferPayload> TYPE = new Type<>(ComedyMonkeysCreateAddons.asResource("quick_transfer"));
	private static final StreamCodec<ByteBuf, QuickTransfer.Action> ACTION =
		ByteBufCodecs.VAR_INT.map(i -> QuickTransfer.Action.values()[Math.floorMod(i, QuickTransfer.Action.values().length)],
			QuickTransfer.Action::ordinal);
	public static final StreamCodec<ByteBuf, QuickTransferPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, QuickTransferPayload::containerId,
		ACTION, QuickTransferPayload::action,
		ByteBufCodecs.VAR_INT, QuickTransferPayload::slot,
		QuickTransferPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(QuickTransferPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> {
			Player player = context.player();
			// Only the screen the client meant, in case another has opened since
			if (player.containerMenu.containerId == payload.containerId())
				QuickTransfer.handle(player, player.containerMenu, payload.action(), payload.slot());
		});
	}
}
