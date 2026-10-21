package com.comedymonkey.createaddons.content.clock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * When a villager last asked a Cuckoo Clock whether to restock: the game tick (which only goes forward) and the time
 * of day (which {@code /time set} can wind back, and which stands still without a daylight cycle). Kept on the
 * villager.
 */
public record ClockCheck(long gameTime, long dayTime) {
	/** A villager that has never asked. */
	public static final ClockCheck NEVER = new ClockCheck(Long.MIN_VALUE, Long.MIN_VALUE);

	public static final Codec<ClockCheck> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.LONG.fieldOf("game_time").forGetter(ClockCheck::gameTime),
		Codec.LONG.fieldOf("day_time").forGetter(ClockCheck::dayTime)
	).apply(i, ClockCheck::new));

	/**
	 * Has an 8:00 or 12:00 come round between this check and one at {@code gameTime} / {@code dayTime}? While time
	 * runs forwards, only if the day has passed one; frozen time never strikes. If {@code /time set} wound the day
	 * back, it goes by the game tick instead, so an hour that comes round again still counts.
	 */
	public boolean hourStruckBy(long gameTime, long dayTime) {
		if (this.equals(NEVER))
			return true;
		if (dayTime >= this.dayTime)
			return ClockTime.slot(dayTime) > ClockTime.slot(this.dayTime);
		return this.gameTime < gameTime - ClockTime.intoSlot(dayTime);
	}
}
