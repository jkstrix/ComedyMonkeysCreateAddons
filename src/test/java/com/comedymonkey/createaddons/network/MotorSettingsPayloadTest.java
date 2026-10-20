package com.comedymonkey.createaddons.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;

/** The motor settings packet survives the wire unchanged, whatever a client puts in it (the server clamps). */
class MotorSettingsPayloadTest {
	private static MotorSettingsPayload roundTrip(MotorSettingsPayload payload) {
		ByteBuf buf = Unpooled.buffer();
		try {
			MotorSettingsPayload.STREAM_CODEC.encode(buf, payload);
			MotorSettingsPayload decoded = MotorSettingsPayload.STREAM_CODEC.decode(buf);
			assertFalse(buf.isReadable(), "bytes left over");
			return decoded;
		} finally {
			buf.release();
		}
	}

	@Test
	void typicalSettingsRoundTrip() {
		MotorSettingsPayload payload = new MotorSettingsPayload(new BlockPos(120, 64, -340), 128, 16384, true);
		assertEquals(payload, roundTrip(payload));
	}

	@Test
	void outOfRangeValuesArriveAsSent() {
		for (MotorSettingsPayload payload : new MotorSettingsPayload[] {
			new MotorSettingsPayload(BlockPos.ZERO, 0, 0, false),
			new MotorSettingsPayload(BlockPos.ZERO, -1, -256, false),
			new MotorSettingsPayload(BlockPos.ZERO, Integer.MAX_VALUE, Integer.MIN_VALUE, true),
			new MotorSettingsPayload(BlockPos.ZERO, Integer.MIN_VALUE, Integer.MAX_VALUE, false)})
			assertEquals(payload, roundTrip(payload));
	}

	@Test
	void farPositionsRoundTrip() {
		for (BlockPos pos : new BlockPos[] {new BlockPos(29_999_999, 319, -29_999_999), new BlockPos(-29_999_999, -64, 29_999_999)}) {
			MotorSettingsPayload payload = new MotorSettingsPayload(pos, 64, 256, false);
			assertEquals(payload, roundTrip(payload));
		}
	}

	@Test
	void hasItsOwnType() {
		assertEquals(ComedyMonkeysCreateAddons.asResource("motor_settings"),
			new MotorSettingsPayload(BlockPos.ZERO, 1, 256, false).type().id());
		assertEquals(MotorSettingsPayload.TYPE, new MotorSettingsPayload(BlockPos.ZERO, 1, 256, false).type());
	}
}
