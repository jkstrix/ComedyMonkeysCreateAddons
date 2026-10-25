package com.comedymonkey.createaddons.content.tradebot.trade;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;

/**
 * Villagers a bot couldn't get to, left alone for a while so he doesn't set off for them over and over. Each time a
 * route search finds no way at all to the same villager in the same seat, he leaves it twice as long (up to
 * {@link #MAX_STRIKES} doublings); being stuck on the way (behind a player or another bot, say) only ever costs the
 * first wait. A villager in a new seat, or one he gets to, starts afresh. Marks long past are forgotten.
 */
public final class Unreachable {
	/** How long a villager he couldn't get to is first left alone (ticks). */
	public static final int BASE_TICKS = 2400;
	static final int MAX_STRIKES = 3;
	/** How long after a mark runs out it's still remembered, for the next one to count as a repeat (ticks). */
	static final int FORGET_TICKS = 24000;

	/** {@code strikes}: doublings so far, -1 if there has always been a way (he was only stuck). */
	private record Mark(long until, BlockPos seat, int strikes) {}

	private final Map<UUID, Mark> marks = new HashMap<>();

	/**
	 * He couldn't get to {@code villager}, sitting at {@code seat}, at {@code now}: {@code noWay} if a route search
	 * found no way there, false if he was stuck on the way.
	 */
	public void mark(UUID villager, BlockPos seat, long now, boolean noWay) {
		prune(now);
		Mark before = marks.get(villager);
		int strikes = before != null && before.seat.equals(seat) ? before.strikes : -1;
		if (noWay)
			strikes = Math.min(strikes + 1, MAX_STRIKES);
		// strikes stays -1 while he's only ever been stuck on the way
		marks.put(villager, new Mark(now + ((long) BASE_TICKS << (noWay ? strikes : 0)), seat.immutable(), strikes));
	}

	/** Still being left alone at {@code now}. */
	public boolean blocked(UUID villager, long now) {
		Mark mark = marks.get(villager);
		return mark != null && mark.until > now;
	}

	/** He got to it after all: it starts afresh. */
	public void clear(UUID villager) {
		marks.remove(villager);
	}

	private void prune(long now) {
		marks.values().removeIf(mark -> now >= mark.until + FORGET_TICKS);
	}

	/** Villagers remembered (for tests). */
	int size() {
		return marks.size();
	}
}
