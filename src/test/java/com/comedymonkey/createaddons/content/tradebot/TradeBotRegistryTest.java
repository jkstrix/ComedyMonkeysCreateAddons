package com.comedymonkey.createaddons.content.tradebot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/** Bots removed for good: remembered while their stations may still be packed, and forgotten once they're gone. */
class TradeBotRegistryTest {
	@Test
	void aGoneBotIsRememberedTillItsStationGoes() {
		TradeBotRegistry registry = new TradeBotRegistry();
		UUID bot = UUID.randomUUID(), other = UUID.randomUUID();
		registry.markGone(bot);
		registry.markGone(other);
		assertTrue(registry.isGone(bot), "his station can still be packed on its own");
		registry.remove(bot);
		assertFalse(registry.isGone(bot), "his station was packed: nothing left to remember");
		assertTrue(registry.isGone(other));
	}
}
