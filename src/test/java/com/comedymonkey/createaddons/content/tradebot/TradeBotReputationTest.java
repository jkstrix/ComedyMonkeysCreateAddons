package com.comedymonkey.createaddons.content.tradebot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/** His standing with villagers he hasn't traded with for a week of his own time is forgotten. */
class TradeBotReputationTest {
	@Test
	void aVillagerNotTradedWithForAWeekIsForgotten() {
		UUID recent = UUID.randomUUID(), old = UUID.randomUUID(), undated = UUID.randomUUID();
		Map<UUID, Integer> reputation = new HashMap<>(Map.of(recent, 10, old, 20, undated, 6));
		Map<UUID, Long> lastTraded = new HashMap<>(Map.of(recent, 100_000L, old, 1_000L, UUID.randomUUID(), 5L));
		long now = 1_000L + TradeBotEntity.FORGET_VILLAGER_TICKS;
		TradeBotEntity.forget(reputation, lastTraded, now);
		assertFalse(reputation.containsKey(old), "a week since he traded with it");
		assertEquals(10, reputation.get(recent));
		assertEquals(6, reputation.get(undated), "an old save's villager starts its week now");
		assertEquals(now, lastTraded.get(undated));
		assertEquals(2, lastTraded.size(), "dates go with the reputation they're for");
		TradeBotEntity.forget(reputation, lastTraded, now + TradeBotEntity.FORGET_VILLAGER_TICKS - 1);
		assertTrue(reputation.containsKey(undated));
		TradeBotEntity.forget(reputation, lastTraded, now + TradeBotEntity.FORGET_VILLAGER_TICKS);
		assertFalse(reputation.containsKey(undated));
	}
}
