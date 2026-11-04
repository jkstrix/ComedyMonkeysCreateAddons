package com.comedymonkey.createaddons.content.tradebot.master;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Which Trade Master is filling which board gauge, and what he's promised it (design/specs/trade-master.md D24, P8):
 * first come, first served per gauge, so several on one network pick different gauges; the promise shows on Create's
 * gauge as on its way. A claim lapses unless it's renewed, so a Trade Master who's gone (unloaded, packed) never holds
 * one for long. Nothing is saved: after a restart he plans afresh.
 */
public final class GaugeClaims {
	/** How long a claim lasts unless renewed (ticks); he renews his every second while out on the job. */
	public static final long CLAIM_TICKS = 200;

	private record Claim(UUID bot, int promised, long until) {}

	private static final Map<ResourceKey<Level>, Map<FactoryPanelPosition, Claim>> CLAIMS = new ConcurrentHashMap<>();

	private GaugeClaims() {}

	/** Claims (or renews) the gauge for {@code bot}, promising it {@code promised} items; false if another holds it. */
	public static boolean claim(Level level, FactoryPanelPosition gauge, UUID bot, int promised, long now) {
		Map<FactoryPanelPosition, Claim> claims = CLAIMS.computeIfAbsent(level.dimension(), d -> new ConcurrentHashMap<>());
		Claim held = claims.get(gauge);
		if (held != null && !held.bot.equals(bot) && held.until > now)
			return false;
		claims.put(gauge, new Claim(bot, Math.max(0, promised), now + CLAIM_TICKS));
		return true;
	}

	/** Another Trade Master is filling this gauge. */
	public static boolean claimedByOther(Level level, FactoryPanelPosition gauge, UUID bot, long now) {
		Claim held = held(level, gauge, now);
		return held != null && !held.bot.equals(bot);
	}

	/** What's on its way to this gauge (0 if nobody's filling it). */
	public static int promised(Level level, FactoryPanelPosition gauge, long now) {
		Claim held = held(level, gauge, now);
		return held == null ? 0 : held.promised;
	}

	public static void release(Level level, FactoryPanelPosition gauge, UUID bot) {
		Map<FactoryPanelPosition, Claim> claims = CLAIMS.get(level.dimension());
		if (claims != null)
			claims.computeIfPresent(gauge, (g, held) -> held.bot.equals(bot) ? null : held);
	}

	/** Lets go of every gauge {@code bot} holds, in every level. */
	public static void releaseAll(UUID bot) {
		for (Map<FactoryPanelPosition, Claim> claims : CLAIMS.values())
			claims.values().removeIf(held -> held.bot.equals(bot));
	}

	private static Claim held(Level level, FactoryPanelPosition gauge, long now) {
		Map<FactoryPanelPosition, Claim> claims = CLAIMS.get(level.dimension());
		Claim held = claims == null ? null : claims.get(gauge);
		return held != null && held.until > now ? held : null;
	}
}
