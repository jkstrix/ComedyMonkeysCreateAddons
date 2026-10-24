package com.comedymonkey.createaddons.content.tradebot.react;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cooldowns for Trade Bot's reactions, so a reaction stays special (D43): each kind waits its own time before it can
 * play again, and a player he has already greeted isn't greeted again until they've been away a minute.
 */
public final class Reactions {
	/** Reactions and how long each waits before it can play again (ticks). */
	public enum Kind {
		WALL_BUMP(200), SEES_PLAYER(400), CROSS(100), HAPPY(0), SCARED(100);

		final int cooldown;

		Kind(int cooldown) {
			this.cooldown = cooldown;
		}
	}

	/** A player not seen for this long (ticks) is greeted again when they come back. */
	public static final int AWAY_TICKS = 1200;

	private final Map<Kind, Long> last = new HashMap<>();
	private final Map<UUID, Long> seen = new HashMap<>();

	/** Whether {@code kind} may play at {@code now}; if so, it counts as played. */
	public boolean tryPlay(Kind kind, long now) {
		Long at = last.get(kind);
		if (at != null && now - at < kind.cooldown)
			return false;
		last.put(kind, now);
		return true;
	}

	/** A player is in sight at {@code now}: true if they've just turned up (never seen, or away a minute). */
	public boolean arrived(UUID player, long now) {
		Long at = seen.put(player, now);
		return at == null || now - at >= AWAY_TICKS;
	}
}
