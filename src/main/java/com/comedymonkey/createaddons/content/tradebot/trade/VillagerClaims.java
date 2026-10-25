package com.comedymonkey.createaddons.content.tradebot.trade;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Which bot is on its way to, or trading with, which villager (D54): first come, first served, so a second bot picks
 * another buyer. A claim lapses if its bot stops renewing it (unloaded, removed), so a villager is never held for good.
 * Kept for the server's life, like the Cuckoo Clocks.
 */
public final class VillagerClaims {
	/** How long a claim lasts without being renewed (ticks); a working bot renews it every tick. */
	public static final int HOLD_TICKS = 200;

	private record Claim(UUID bot, long until) {}

	private static final Map<UUID, Claim> CLAIMS = new HashMap<>();
	/** When lapsed claims were last cleared out. */
	private static long lastPrune = Long.MIN_VALUE / 2;

	private VillagerClaims() {}

	/** Claims or renews {@code villager} for {@code bot}; false if another bot holds it. */
	public static boolean claim(UUID villager, UUID bot, long now) {
		prune(now);
		if (claimedByOther(villager, bot, now))
			return false;
		CLAIMS.put(villager, new Claim(bot, now + HOLD_TICKS));
		return true;
	}

	public static boolean claimedByOther(UUID villager, UUID bot, long now) {
		Claim claim = CLAIMS.get(villager);
		return claim != null && !claim.bot.equals(bot) && claim.until > now;
	}

	public static void release(UUID villager, UUID bot) {
		Claim claim = CLAIMS.get(villager);
		if (claim != null && claim.bot.equals(bot))
			CLAIMS.remove(villager);
	}

	/** Everything {@code bot} holds (he went home, was packed or removed). */
	public static void releaseAll(UUID bot) {
		CLAIMS.values().removeIf(claim -> claim.bot.equals(bot));
	}

	/** Forgets every claim, when the server stops. */
	public static void clear() {
		CLAIMS.clear();
		lastPrune = Long.MIN_VALUE / 2;
	}

	/** Clears out lapsed claims (they count for nothing anyway), at most once every {@link #HOLD_TICKS}. */
	private static void prune(long now) {
		if (now >= lastPrune && now - lastPrune < HOLD_TICKS)
			return;
		lastPrune = now;
		CLAIMS.values().removeIf(claim -> claim.until <= now);
	}

	/** Claims held, lapsed or not (for tests). */
	static int size() {
		return CLAIMS.size();
	}
}
