package com.comedymonkey.createaddons.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.compat.pipez.cover.PipeCover;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;

class PipeCoverPayloadTest {
	@Test
	void typeId() {
		assertEquals(ComedyMonkeysCreateAddons.asResource("pipe_cover"), PipeCoverPayload.TYPE.id());
		assertSame(PipeCoverPayload.TYPE, new PipeCoverPayload(BlockPos.ZERO, Optional.empty()).type());
	}

	@Test
	void roundTripsACover() {
		PipeCoverPayload payload = new PipeCoverPayload(new BlockPos(-30_000_000 + 1, -64, 29_999_999),
			Optional.of(new PipeCover(PipeCover.Kind.FULL,
				Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP))));
		assertEquals(payload, roundTrip(payload));
	}

	@Test
	void roundTripsABareCover() {
		PipeCoverPayload payload = new PipeCoverPayload(new BlockPos(1, 2, 3),
			Optional.of(new PipeCover(PipeCover.Kind.SNUG, Blocks.AIR.defaultBlockState())));
		assertEquals(payload, roundTrip(payload));
	}

	@Test
	void roundTripsARemovedCover() {
		PipeCoverPayload payload = new PipeCoverPayload(new BlockPos(0, 319, 0), Optional.empty());
		assertEquals(payload, roundTrip(payload));
	}

	private static PipeCoverPayload roundTrip(PipeCoverPayload payload) {
		RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
			RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
		PipeCoverPayload.STREAM_CODEC.encode(buf, payload);
		PipeCoverPayload decoded = PipeCoverPayload.STREAM_CODEC.decode(buf);
		assertEquals(0, buf.readableBytes(), "trailing bytes");
		return decoded;
	}
}
