package com.comedymonkey.createaddons.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Only the nearest few of one kind of running loop play at once (the Fish Tanks' pumps, say). Every machine that wants
 * the loop offers itself each tick with its distance, and after the tick the nearest {@link #PER_KIND} are let play;
 * the rest fade out or don't start. A hundred tanks would otherwise hold two hundred of the sound engine's ~250
 * channels, starving everything else, for loops lost under the near ones anyway.
 */
final class NearestLoops {
	static final int PER_KIND = 4;
	/** A loop already playing counts as this much nearer (of the squared distance), so two at much the same distance don't take turns. */
	private static final double PLAYING_BIAS = 0.7;
	private static final List<NearestLoops> ALL = new ArrayList<>();

	private record Offer(Object owner, double distanceSqr) {}

	private final List<Offer> offers = new ArrayList<>();
	private final Set<Object> allowed = Collections.newSetFromMap(new IdentityHashMap<>());
	/** Ticks in a row nothing was offered: after a while the machines held here are let go. */
	private int idle;

	NearestLoops() {
		ALL.add(this);
	}

	/**
	 * {@code owner} (its machine) wants this loop this tick, {@code distanceSqr} from the listener; whether it may play,
	 * as the nearest were picked last tick.
	 */
	boolean offer(Object owner, double distanceSqr, boolean playing) {
		offers.add(new Offer(owner, playing ? distanceSqr * PLAYING_BIAS : distanceSqr));
		return allowed.contains(owner);
	}

	/** Whether {@code owner}'s loop may keep playing. */
	boolean allows(Object owner) {
		return allowed.contains(owner);
	}

	/**
	 * After the machines' ticks: the nearest of what was offered may play. A tick with no offers at all (the game
	 * paused, the world not ticking) keeps what was let play for a while, so loops don't drop out and fade back in.
	 */
	static void onClientTick(ClientTickEvent.Post event) {
		if (!Minecraft.getInstance().isPaused())
			rank();
	}

	/** Picks each kind's nearest from this tick's offers. */
	static void rank() {
		for (NearestLoops kind : ALL) {
			if (kind.offers.isEmpty()) {
				if (++kind.idle > 100)
					kind.allowed.clear();
				continue;
			}
			kind.idle = 0;
			kind.offers.sort(Comparator.comparingDouble(Offer::distanceSqr));
			kind.allowed.clear();
			for (int i = 0; i < kind.offers.size() && i < PER_KIND; i++)
				kind.allowed.add(kind.offers.get(i).owner());
			kind.offers.clear();
		}
	}

	/** As the world goes: the machines are let go. */
	static void forget() {
		for (NearestLoops kind : ALL) {
			kind.offers.clear();
			kind.allowed.clear();
		}
	}
}
