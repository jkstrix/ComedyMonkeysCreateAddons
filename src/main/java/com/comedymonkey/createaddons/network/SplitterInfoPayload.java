package com.comedymonkey.createaddons.network;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.line.PowerLineBlockEntity;
import com.comedymonkey.createaddons.content.line.PowerLines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server to client, when a wrench opens a Power Line Splitter: its branches (which way, what's down each, the priority set
 * for it) and how its line stood at the last update, for the Splitter's screen.
 */
public record SplitterInfoPayload(BlockPos pos, List<Row> rows, int has, int wanted, int took, boolean known) implements CustomPacketPayload {
	public static final Type<SplitterInfoPayload> TYPE = new Type<>(ComedyMonkeysCreateAddons.asResource("splitter_info"));

	/** One branch: its direction, the machines and generators down it (named if all one kind), and its priority. */
	public record Row(Direction dir, int devices, @Nullable Component deviceName, int generators, @Nullable Component generatorName, int priority,
					  boolean loops) {}

	public static final StreamCodec<RegistryFriendlyByteBuf, SplitterInfoPayload> STREAM_CODEC = StreamCodec.of(SplitterInfoPayload::write, SplitterInfoPayload::read);

	public static void send(ServerLevel level, ServerPlayer player, PowerLineBlockEntity be) {
		PowerLines.SplitterView view = PowerLines.of(level).splitter(be);
		List<Row> rows = new ArrayList<>();
		for (PowerLines.Branch b : view.branches())
			rows.add(new Row(b.dir(), b.devices(), b.deviceName(), b.generators(), b.generatorName(), be.priority(b.dir()), b.loops()));
		PacketDistributor.sendToPlayer(player, new SplitterInfoPayload(be.getBlockPos(), rows, (int) Math.round(view.has()), (int) Math.round(view.wanted()),
			(int) Math.round(view.took()), view.known()));
	}

	private static void write(RegistryFriendlyByteBuf buf, SplitterInfoPayload p) {
		buf.writeBlockPos(p.pos);
		buf.writeVarInt(p.rows.size());
		for (Row r : p.rows) {
			buf.writeByte(r.dir.get3DDataValue());
			buf.writeVarInt(r.devices);
			name(buf, r.deviceName);
			buf.writeVarInt(r.generators);
			name(buf, r.generatorName);
			buf.writeByte(r.priority);
			buf.writeBoolean(r.loops);
		}
		buf.writeVarInt(p.has);
		buf.writeVarInt(p.wanted);
		buf.writeVarInt(p.took);
		buf.writeBoolean(p.known);
	}

	private static void name(RegistryFriendlyByteBuf buf, @Nullable Component c) {
		buf.writeBoolean(c != null);
		if (c != null)
			ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, c);
	}

	@Nullable
	private static Component name(RegistryFriendlyByteBuf buf) {
		return buf.readBoolean() ? ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf) : null;
	}

	private static SplitterInfoPayload read(RegistryFriendlyByteBuf buf) {
		BlockPos pos = buf.readBlockPos();
		int n = Math.min(6, buf.readVarInt());
		List<Row> rows = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			Direction dir = Direction.from3DDataValue(buf.readByte());
			int devices = buf.readVarInt();
			Component deviceName = name(buf);
			int generators = buf.readVarInt();
			Component generatorName = name(buf);
			int priority = buf.readByte();
			rows.add(new Row(dir, devices, deviceName, generators, generatorName, priority, buf.readBoolean()));
		}
		return new SplitterInfoPayload(pos, rows, buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handle(SplitterInfoPayload payload, IPayloadContext context) {
		context.enqueueWork(() -> com.comedymonkey.createaddons.client.SplitterScreen.open(payload));
	}
}
