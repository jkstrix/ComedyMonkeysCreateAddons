package com.comedymonkey.createaddons.content.line;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;

/**
 * How one tick's flow from a source is shared between the ends it can reach, for fluid and power lines alike. Ends
 * share evenly unless a Splitter upstream of them says otherwise (nearest first, or one at a time in turn); a Valve or
 * Breaker part-way caps what passes it, shared by every end beyond it.
 */
public final class Sharing {
	private Sharing() {}

	/**
	 * One end's claim: how much it can take, how far it is, the Splitter that governs it (the nearest one upstream, or
	 * none) and the throttled blocks on its way.
	 */
	public record Claim(int room, int length, @Nullable BlockPos splitter, SplitMode mode, List<BlockPos> caps) {}

	/**
	 * Shares {@code amount} between the claims. {@code capLeft} holds what each throttled block can still pass this
	 * tick (and is used up); {@code turns} remembers each round-robin Splitter's turn.
	 */
	public static int[] allocate(int amount, List<Claim> claims, Map<BlockPos, Integer> capLeft, Map<BlockPos, Integer> turns) {
		int n = claims.size();
		int[] room = new int[n];
		for (int i = 0; i < n; i++) {
			int r = Math.max(0, claims.get(i).room);
			for (BlockPos cap : claims.get(i).caps)
				r = Math.min(r, capLeft.getOrDefault(cap, Integer.MAX_VALUE));
			room[i] = r;
		}
		// the ends under each Splitter, by mode; ends with no Splitter share evenly
		Map<BlockPos, List<Integer>> groups = new LinkedHashMap<>();
		List<Integer> even = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			Claim c = claims.get(i);
			if (c.splitter == null || c.mode == SplitMode.EVEN)
				even.add(i);
			else
				groups.computeIfAbsent(c.splitter, k -> new ArrayList<>()).add(i);
		}
		List<Integer> robinPicks = new ArrayList<>();
		Map<BlockPos, List<Integer>> nearest = new HashMap<>();
		for (Map.Entry<BlockPos, List<Integer>> g : groups.entrySet()) {
			List<Integer> members = g.getValue();
			if (claims.get(members.get(0)).mode == SplitMode.ROUND_ROBIN) {
				List<Integer> open = members.stream().filter(i -> room[i] > 0).toList();
				if (!open.isEmpty()) {
					int turn = turns.getOrDefault(g.getKey(), 0);
					robinPicks.add(open.get(Math.floorMod(turn, open.size())));
					turns.put(g.getKey(), turn + 1);
				}
			} else {
				List<Integer> sorted = new ArrayList<>(members);
				sorted.sort(Comparator.comparingInt(i -> claims.get(i).length));
				nearest.put(g.getKey(), sorted);
			}
		}
		int[] out = new int[n];
		Map<BlockPos, Integer> used = new HashMap<>();
		int pool = amount;
		// share between everyone taking part, round after round: what an end can't take, or a throttle won't pass, goes
		// to the others, and the nearest-first groups pass what's left on to their next end
		for (int round = 0; round <= 2 * n + 2 && pool > 0; round++) {
			List<Integer> active = new ArrayList<>();
			for (int i : even)
				if (headroom(i, claims, room, out, capLeft, used) > 0)
					active.add(i);
			for (int i : robinPicks)
				if (headroom(i, claims, room, out, capLeft, used) > 0)
					active.add(i);
			for (List<Integer> sorted : nearest.values())
				for (int i : sorted)
					if (headroom(i, claims, room, out, capLeft, used) > 0) {
						active.add(i);
						break;
					}
			if (active.isEmpty())
				break;
			int[] left = new int[active.size()];
			for (int k = 0; k < left.length; k++)
				left[k] = headroom(active.get(k), claims, room, out, capLeft, used);
			int[] given = share(pool, left);
			// a throttle shared by several ends this round: their shares scaled down to what it still passes
			Set<BlockPos> throttles = new LinkedHashSet<>();
			for (int k : active)
				throttles.addAll(claims.get(k).caps);
			for (BlockPos cap : throttles) {
				int passes = capLeft.getOrDefault(cap, Integer.MAX_VALUE) - used.getOrDefault(cap, 0);
				int sum = 0;
				for (int k = 0; k < given.length; k++)
					if (claims.get(active.get(k)).caps.contains(cap))
						sum += given[k];
				if (sum > passes) {
					double f = passes / (double) sum;
					for (int k = 0; k < given.length; k++)
						if (claims.get(active.get(k)).caps.contains(cap))
							given[k] = (int) Math.floor(given[k] * f);
					// what rounding down left over, a unit at a time to the ends that can still take it (else many ends
					// behind a narrow throttle would each round to nothing)
					int rest = passes;
					for (int k = 0; k < given.length; k++)
						if (claims.get(active.get(k)).caps.contains(cap))
							rest -= given[k];
					for (int k = 0; k < given.length && rest > 0; k++)
						if (claims.get(active.get(k)).caps.contains(cap) && given[k] < left[k]) {
							int add = Math.min(rest, Math.min(left[k] - given[k], spare(active.get(k), cap, claims, active, given, capLeft, used)));
							given[k] += add;
							rest -= add;
						}
				}
			}
			int total = 0;
			for (int k = 0; k < given.length; k++) {
				out[active.get(k)] += given[k];
				total += given[k];
				for (BlockPos cap : claims.get(active.get(k)).caps)
					used.merge(cap, given[k], Integer::sum);
			}
			if (total <= 0)
				break;
			pool -= total;
		}
		return out;
	}

	/** What end i's other throttles (not {@code except}) still pass this round, after what's been given past them. */
	private static int spare(int i, BlockPos except, List<Claim> claims, List<Integer> active, int[] given, Map<BlockPos, Integer> capLeft,
							 Map<BlockPos, Integer> used) {
		int r = Integer.MAX_VALUE;
		for (BlockPos cap : claims.get(i).caps) {
			if (cap.equals(except))
				continue;
			int round = 0;
			for (int k = 0; k < given.length; k++)
				if (claims.get(active.get(k)).caps.contains(cap))
					round += given[k];
			r = Math.min(r, capLeft.getOrDefault(cap, Integer.MAX_VALUE) - used.getOrDefault(cap, 0) - round);
		}
		return Math.max(0, r);
	}

	/** What end i can still take: its own room, and what every throttle on its way still passes. */
	private static int headroom(int i, List<Claim> claims, int[] room, int[] out, Map<BlockPos, Integer> capLeft, Map<BlockPos, Integer> used) {
		int r = room[i] - out[i];
		for (BlockPos cap : claims.get(i).caps)
			r = Math.min(r, capLeft.getOrDefault(cap, Integer.MAX_VALUE) - used.getOrDefault(cap, 0));
		return Math.max(0, r);
	}

	/** Uses up the throttles' allowance for what actually moved. */
	public static void spend(List<Claim> claims, int[] moved, Map<BlockPos, Integer> capLeft) {
		for (int i = 0; i < claims.size(); i++) {
			int m = moved[i];
			for (BlockPos cap : claims.get(i).caps)
				capLeft.computeIfPresent(cap, (k, v) -> Math.max(0, v - m));
		}
	}

	/**
	 * Shares {@code amount} evenly between ends with room for {@code room[i]} each: the smallest take what they can,
	 * the rest is split among the others. Integer shares; whatever can't be split evenly goes to the roomiest.
	 */
	public static int[] share(int amount, int[] room) {
		int n = room.length;
		int[] out = new int[n];
		Integer[] order = new Integer[n];
		for (int i = 0; i < n; i++)
			order[i] = i;
		Arrays.sort(order, Comparator.comparingInt(i -> room[i]));
		int left = amount;
		for (int k = 0; k < n; k++) {
			int i = order[k];
			int give = Math.min(Math.max(0, room[i]), left / (n - k));
			out[i] = give;
			left -= give;
		}
		for (int k = n - 1; k >= 0 && left > 0; k--) {
			int i = order[k];
			int extra = Math.min(room[i] - out[i], left);
			if (extra > 0) {
				out[i] += extra;
				left -= extra;
			}
		}
		return out;
	}
}
