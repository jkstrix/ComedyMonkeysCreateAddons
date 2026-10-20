package com.comedymonkey.createaddons.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

class ContraptionBlockSyncPayloadTest {
	@Test
	void typeId() {
		assertEquals(ComedyMonkeysCreateAddons.asResource("contraption_block_sync"), ContraptionBlockSyncPayload.TYPE.id());
		assertSame(ContraptionBlockSyncPayload.TYPE,
			new ContraptionBlockSyncPayload(0, BlockPos.ZERO, Blocks.AIR.defaultBlockState(), new CompoundTag()).type());
	}

	@Test
	void roundTripsStateAndBlockEntityData() {
		BlockState chest = Blocks.CHEST.defaultBlockState()
			.setValue(ChestBlock.FACING, Direction.EAST)
			.setValue(ChestBlock.TYPE, ChestType.LEFT);
		CompoundTag nbt = new CompoundTag();
		nbt.putString("id", "comedymonkeysaddons:upgraded_chest");
		nbt.putBoolean("HoldsUpgrade", true);
		ListTag items = new ListTag();
		items.add(StringTag.valueOf("x"));
		nbt.put("Items", items);

		ContraptionBlockSyncPayload payload = new ContraptionBlockSyncPayload(123456, new BlockPos(-2, 0, 5), chest, nbt);
		assertEquals(payload, roundTrip(payload));
	}

	@Test
	void roundTripsEmptyDataAndExtremeIds() {
		for (int id : new int[] {0, -1, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
			ContraptionBlockSyncPayload payload = new ContraptionBlockSyncPayload(id, new BlockPos(-100, -64, 100),
				Blocks.BARREL.defaultBlockState(), new CompoundTag());
			assertEquals(payload, roundTrip(payload));
		}
	}

	private static ContraptionBlockSyncPayload roundTrip(ContraptionBlockSyncPayload payload) {
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
			RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
		ContraptionBlockSyncPayload.STREAM_CODEC.encode(buf, payload);
		ContraptionBlockSyncPayload decoded = ContraptionBlockSyncPayload.STREAM_CODEC.decode(buf);
		assertEquals(0, buf.readableBytes(), "trailing bytes");
		return decoded;
	}
}
