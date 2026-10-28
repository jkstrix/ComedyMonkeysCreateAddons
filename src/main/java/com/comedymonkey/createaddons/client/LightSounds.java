package com.comedymonkey.createaddons.client;

import com.comedymonkey.createaddons.content.light.LightBlockEntity;
import com.comedymonkey.createaddons.registry.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * The Electric Lights' flicker and warm-up sounds (design/sound/electric-lights): played on the client as a light's
 * flicker or warm-up starts, and capped, so a whole street browning out or a yard warming up on one Breaker is a soft
 * crackle, not a roar: at most three of a kind at once, within 16 blocks of the player.
 */
public final class LightSounds {
	private static final int MAX_AT_ONCE = 3;
	private static final double RANGE = 16;
	/** When each of the last few started (game time), by kind: flicker, warm-up. */
	private static final long[][] STARTED = {free(), free()};

	private static long[] free() {
		long[] slots = new long[MAX_AT_ONCE];
		java.util.Arrays.fill(slots, Long.MIN_VALUE / 2);
		return slots;
	}

	private LightSounds() {}

	public static void cue(LightBlockEntity be, int cue) {
		if (cue == LightBlockEntity.CUE_LIT) {
			ComedyMonkeysCreateAddonsClient.revisualize(be);
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || be.getLevel() == null || mc.player.distanceToSqr(be.getBlockPos().getCenter()) > RANGE * RANGE)
			return;
		boolean flicker = cue == LightBlockEntity.CUE_FLICKER;
		long now = be.getLevel().getGameTime(), length = flicker ? LightBlockEntity.FLICKER_TICKS : LightBlockEntity.WARM_TICKS;
		long[] started = STARTED[flicker ? 0 : 1];
		// a free place: one whose sound has finished
		int free = -1;
		for (int i = 0; i < started.length; i++)
			if (now - started[i] >= length || started[i] > now + length) {
				free = i;
				break;
			}
		if (free < 0)
			return;
		started[free] = now;
		SoundEvent sound = ModSounds.light(flicker ? "flicker" : "warm_up");
		be.getLevel().playLocalSound(be.getBlockPos(), sound, SoundSource.BLOCKS, 1f, 0.95f + be.getLevel().random.nextFloat() * 0.1f, false);
	}
}
