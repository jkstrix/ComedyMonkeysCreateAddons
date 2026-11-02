package com.comedymonkey.createaddons.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.config.CMConfig;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.common.ModConfigSpec;

class ModDataComponentsTest {
	private static int read(int fe) {
		Codec<Integer> codec = ModDataComponents.BATTERY_CHARGE.get().codec();
		return codec.parse(JsonOps.INSTANCE, new JsonPrimitive(fe)).getOrThrow();
	}

	private static int receive(int fe) {
		ByteBuf buf = Unpooled.buffer();
		ByteBufCodecs.VAR_INT.encode(buf, fe);
		return ModDataComponents.BATTERY_CHARGE_STREAM_CODEC.decode(buf);
	}

	/** A Battery item's charge, saved or sent, is brought between none and the most a block can ever hold, not refused. */
	@Test
	void batteryChargeIsBroughtInRange() {
		DataComponentType<Integer> type = ModDataComponents.BATTERY_CHARGE.get();
		assertEquals(123_456, read(123_456));
		assertEquals(ModDataComponents.BATTERY_CHARGE_MAX, read(2_000_000_000));
		assertEquals(0, read(-5));
		assertEquals(123_456, receive(123_456));
		assertEquals(ModDataComponents.BATTERY_CHARGE_MAX, receive(2_000_000_000));
		assertEquals(0, receive(-5));
		// saved as it is
		assertEquals(new JsonPrimitive(123_456), type.codec().encodeStart(JsonOps.INSTANCE, 123_456).getOrThrow());
	}

	/** The ceiling is exactly the highest either capacity can be set to, so no legitimate charge is ever cut. */
	@Test
	void batteryChargeMaxIsTheHighestCapacity() {
		for (ModConfigSpec.IntValue value : new ModConfigSpec.IntValue[] {CMConfig.SERVER.batteryCapacity, CMConfig.SERVER.industrialBatteryCapacity}) {
			ModConfigSpec.ValueSpec spec = CMConfig.SERVER_SPEC.getSpec().get(value.getPath());
			assertTrue(spec.test(ModDataComponents.BATTERY_CHARGE_MAX), value.getPath() + " can't be set to the ceiling");
			assertFalse(spec.test(ModDataComponents.BATTERY_CHARGE_MAX + 1), value.getPath() + " can be set above the ceiling");
		}
	}
}
