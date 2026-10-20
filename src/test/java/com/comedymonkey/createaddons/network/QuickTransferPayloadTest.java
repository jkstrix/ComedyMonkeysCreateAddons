package com.comedymonkey.createaddons.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.QuickTransfer.Action;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class QuickTransferPayloadTest {
	@Test
	void typeId() {
		assertEquals(ComedyMonkeysCreateAddons.asResource("quick_transfer"), QuickTransferPayload.TYPE.id());
	}

	@Test
	void roundTripsEveryAction() {
		for (Action action : Action.values()) {
			QuickTransferPayload payload = new QuickTransferPayload(7, action, action == Action.ONE_AWAY ? 53 : -1);
			ByteBuf buf = Unpooled.buffer();
			QuickTransferPayload.STREAM_CODEC.encode(buf, payload);
			assertEquals(payload, QuickTransferPayload.STREAM_CODEC.decode(buf));
			assertEquals(0, buf.readableBytes());
		}
	}

	@Test
	void anUnknownActionFromAModifiedClientIsStillAnAction() {
		ByteBuf buf = Unpooled.buffer();
		buf.writeByte(3); // container id
		buf.writeByte(100); // no such action
		buf.writeByte(0); // slot
		assertSame(Action.class, QuickTransferPayload.STREAM_CODEC.decode(buf).action().getDeclaringClass());
	}
}
