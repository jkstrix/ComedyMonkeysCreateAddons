package com.comedymonkey.createaddons.network;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server to client: a block on a moving contraption changed, with its new block entity data. Create's own block
 * change packet only carries the block state, so without this clients would rebuild the block entity from the old
 * block's data (e.g. a vanilla chest's).
 */
public record ContraptionBlockSyncPayload(int entityId, BlockPos localPos, BlockState state, CompoundTag nbt)
	implements CustomPacketPayload {

	public static final Type<ContraptionBlockSyncPayload> TYPE =
		new Type<>(ComedyMonkeysCreateAddons.asResource("contraption_block_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ContraptionBlockSyncPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, ContraptionBlockSyncPayload::entityId,
		BlockPos.STREAM_CODEC, ContraptionBlockSyncPayload::localPos,
		ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), ContraptionBlockSyncPayload::state,
		ByteBufCodecs.COMPOUND_TAG, ContraptionBlockSyncPayload::nbt,
		ContraptionBlockSyncPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
