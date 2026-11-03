package com.comedymonkey.createaddons.content.wastes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.advancement.ModAdvancements;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The Clockwork Wastes' wake records (design/specs/clockwork-wastes.md, P48): one per level, keyed by each city's origin.
 * For every city it holds which of the four districts are awake (woken for good by their Substation's breaker, D96),
 * whether a Substation was broken after waking (its district's own grid cut for good, P50), how long the heart's star
 * burns (P10), and whether the Custodian is awake or dead (D31, D35). A woken district stays lit with no power (D96).
 * <p>
 * City blocks never hold this state themselves: each reads its district's from here when it loads ({@link CityLink.Member}),
 * so a district half unloaded when it woke catches up without loading anything. While loaded, they hear every change
 * to their city at once. Other systems (the city's builder, the bots) use the same calls and can listen for changes
 * ({@link #listen}).
 * <p>
 * The API: {@link #of}, {@link #awake}, {@link #lit}, {@link #wake}, {@link #wakeAll}, {@link #wakeCustodian},
 * {@link #custodianDied}, {@link #vaultOpen}, {@link #doorOpen}, and {@link Listener}.
 */
public final class CityRecord extends SavedData {
	private static final String NAME = "comedymonkeysaddons_clockwork_cities";
	private static final int DISTRICTS = District.DISTRICTS.length;

	/** Something that wants to know when a city changes (the bots). */
	public interface Listener {
		/**
		 * A district woke for good: its locks open, its bots wake (within 5 s, P8). {@code breaker}: where it was woken from,
		 * its Substation's breaker (D97: the pack runs to it); null when the heart's star woke it (or all four at once).
		 */
		default void districtWoke(ServerLevel level, BlockPos origin, District district, @Nullable BlockPos breaker) {}

		/** All four districts are awake: the heart's doors open (P11); the Custodian waits for a player in its hall (D96). */
		default void heartWoke(ServerLevel level, BlockPos origin) {}

		/** The Custodian died, by any means (D35): the heart vault opens. */
		default void custodianDied(ServerLevel level, BlockPos origin) {}
	}

	private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();
	/** The loaded city blocks of each city, by level, to tell them of changes. Not saved: they join as they load. */
	private static final Map<ResourceKey<Level>, Map<BlockPos, Set<CityLink.Member>>> MEMBERS = new HashMap<>();
	/** Where each member was last filed, so it can be taken out again (its link can change). */
	private static final Map<CityLink.Member, BlockPos> FILED = new HashMap<>();

	/** Game tests only: cities whose heart vault follows the Peaceful rule, without making the whole server Peaceful. */
	public static final Set<BlockPos> PEACEFUL_FOR_TESTS = java.util.concurrent.ConcurrentHashMap.newKeySet();

	public static void listen(Listener listener) {
		LISTENERS.add(listener);
	}

	public static void unlisten(Listener listener) {
		LISTENERS.remove(listener);
	}

	/** One city's record. */
	static final class City {
		final BlockPos origin;
		final boolean[] awake = new boolean[DISTRICTS];
		/** Its Substation was broken after waking: its own grid is cut for good (the heart's star can still light it). */
		final boolean[] cut = new boolean[DISTRICTS];
		boolean custodianAwake, custodianDead;
		/** The game time the heart's star burns until (0: none). */
		long heartUntil;

		City(BlockPos origin) {
			this.origin = origin.immutable();
		}
	}

	private final Map<BlockPos, City> cities = new LinkedHashMap<>();

	private CityRecord() {}

	public static CityRecord of(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(CityRecord::new, CityRecord::load, null), NAME);
	}

	@Nullable
	private City get(@Nullable BlockPos origin) {
		return origin == null ? null : cities.get(origin);
	}

	private City make(BlockPos origin) {
		return cities.computeIfAbsent(origin.immutable(), City::new);
	}

	/** A city is built here (the city's builder calls this when it places one): it has a record from now on, all asleep. */
	public void add(BlockPos origin) {
		if (!cities.containsKey(origin)) {
			make(origin);
			setDirty();
		}
	}

	/** A city built again in the same place (the dev command): its record starts over, all asleep. */
	public void forget(BlockPos origin) {
		if (cities.remove(origin) != null)
			setDirty();
	}

	/** Cities world generation has placed, waiting for the server thread to record them ({@link #addLater}). */
	private static final java.util.Queue<Map.Entry<ResourceKey<Level>, BlockPos>> PLACED = new java.util.concurrent.ConcurrentLinkedQueue<>();

	/**
	 * World generation placed a city's heart here (from a worker thread): it's added to its level's record on the next
	 * server tick. Its blocks and bots don't wait for it (a district with no record is simply asleep).
	 */
	public static void addLater(ResourceKey<Level> dimension, BlockPos origin) {
		PLACED.add(Map.entry(dimension, origin.immutable()));
	}

	/** Every city this level has a record for. */
	public Set<BlockPos> cities() {
		return Collections.unmodifiableSet(cities.keySet());
	}

	// ---------- reading ----------

	/** A district is awake (the heart: all four are). A city with no record has nothing awake. */
	public boolean awake(@Nullable BlockPos origin, District district) {
		City c = get(origin);
		if (c == null)
			return false;
		if (district.isHeart())
			return allAwake(c);
		return c.awake[district.ordinal()];
	}

	public boolean allAwake(@Nullable BlockPos origin) {
		City c = get(origin);
		return c != null && allAwake(c);
	}

	private static boolean allAwake(City c) {
		for (boolean a : c.awake)
			if (!a)
				return false;
		return true;
	}

	/** The heart's star is burning (P10): it lights the whole city, a cut district too. */
	public boolean heartBurning(@Nullable BlockPos origin, long now) {
		City c = get(origin);
		return c != null && c.heartUntil > now;
	}

	/** Game time the heart's star burns until (0: none). */
	public long heartUntil(@Nullable BlockPos origin) {
		City c = get(origin);
		return c == null ? 0 : c.heartUntil;
	}

	/**
	 * A district's lamps are lit: it's awake, for good (D96), unless its Substation was broken (P50) and the heart's star
	 * isn't burning. The heart is lit while its star burns or all four districts are lit.
	 */
	public boolean lit(@Nullable BlockPos origin, District district, long now) {
		City c = get(origin);
		return c != null && lit(c, district, now);
	}

	private static boolean lit(City c, District district, long now) {
		boolean heart = c.heartUntil > now;
		if (district.isHeart()) {
			if (heart)
				return true;
			for (District d : District.DISTRICTS)
				if (!lit(c, d, now))
					return false;
			return true;
		}
		int i = district.ordinal();
		return c.awake[i] && (heart || !c.cut[i]);
	}

	public boolean custodianAwake(@Nullable BlockPos origin) {
		City c = get(origin);
		return c != null && c.custodianAwake && !c.custodianDead;
	}

	public boolean custodianDead(@Nullable BlockPos origin) {
		City c = get(origin);
		return c != null && c.custodianDead;
	}

	/** A Substation was broken after waking (P50): its district's own grid is cut for good. */
	public boolean cut(@Nullable BlockPos origin, District district) {
		City c = get(origin);
		return c != null && !district.isHeart() && c.cut[district.ordinal()];
	}

	/**
	 * A vault opens: a district's once it's awake (P11, P21); the heart's on the Custodian's death (D31, D35), or on
	 * Peaceful, where it never wakes, once all four districts are awake (P41).
	 */
	public boolean vaultOpen(ServerLevel level, @Nullable BlockPos origin, District district) {
		if (!district.isHeart())
			return awake(origin, district);
		boolean peaceful = level.getDifficulty() == Difficulty.PEACEFUL || origin != null && PEACEFUL_FOR_TESTS.contains(origin);
		return custodianDead(origin) || peaceful && allAwake(origin);
	}

	/** A locked door opens: a district's once it's awake; the heart's once all four are (P11, P49). */
	public boolean doorOpen(@Nullable BlockPos origin, District district) {
		return awake(origin, district);
	}

	// ---------- changing ----------

	/**
	 * Wakes a district for good (D96): its record says so from now on, its loaded blocks hear at once (lamps ripple on
	 * from {@code from}, locks open), and the action bar tells players within 64 blocks. The heart's spire pulses for it;
	 * waking the fourth wakes the heart instead. Nothing happens if it was already awake.
	 */
	public void wake(ServerLevel level, BlockPos origin, District district, @Nullable BlockPos from) {
		if (district.isHeart()) {
			wakeAll(level, origin, from);
			return;
		}
		City c = make(origin);
		if (c.awake[district.ordinal()])
			return;
		c.awake[district.ordinal()] = true;
		setDirty();
		woke(level, c, district, from, from);
		if (allAwake(c))
			heartWoke(level, c);
		else
			pulseSpire(level, c);
		tell(level, c, from);
	}

	/** Wakes all four districts at once (a Nether Star in the heart's reactor, P10). */
	public void wakeAll(ServerLevel level, BlockPos origin, @Nullable BlockPos from) {
		City c = make(origin);
		boolean any = false;
		for (District d : District.DISTRICTS)
			if (!c.awake[d.ordinal()]) {
				c.awake[d.ordinal()] = true;
				any = true;
				// no breaker: each district's bots wake where they lie (D97)
				woke(level, c, d, from, null);
			}
		if (!any)
			return;
		setDirty();
		heartWoke(level, c);
		tell(level, c, from);
	}

	/** A woken district's Substation was broken (P50): it stays awake, but its own grid is cut for good. */
	public void cut(ServerLevel level, BlockPos origin, District district) {
		if (district.isHeart())
			return;
		City c = make(origin);
		if (c.cut[district.ordinal()])
			return;
		c.cut[district.ordinal()] = true;
		setDirty();
		tell(level, c, null);
	}

	/** The heart's star burns until {@code until} (game time; 0 puts it out): it lights the whole city (P10). */
	public void burnHeart(ServerLevel level, BlockPos origin, long until, @Nullable BlockPos from) {
		City c = make(origin);
		if (c.heartUntil == until)
			return;
		c.heartUntil = until;
		setDirty();
		tell(level, c, from);
	}

	/** The Custodian woke: the heart's pylons can be harmed from now on (P33). */
	public void wakeCustodian(ServerLevel level, BlockPos origin) {
		City c = make(origin);
		if (c.custodianAwake || c.custodianDead)
			return;
		c.custodianAwake = true;
		setDirty();
		tell(level, c, null);
	}

	/** The Custodian went back to sleep (it reset, P39): its pylons can't be harmed until it wakes again. */
	public void custodianReset(ServerLevel level, BlockPos origin) {
		City c = get(origin);
		if (c == null || !c.custodianAwake || c.custodianDead)
			return;
		c.custodianAwake = false;
		setDirty();
		tell(level, c, null);
	}

	/** The Custodian died, by any means (D35): the heart vault opens for good. */
	public void custodianDied(ServerLevel level, BlockPos origin) {
		City c = make(origin);
		if (c.custodianDead)
			return;
		c.custodianDead = true;
		c.custodianAwake = false;
		setDirty();
		for (Listener l : LISTENERS)
			l.custodianDied(level, c.origin);
		tell(level, c, null);
	}

	/**
	 * The Custodian overloaded (P36, D54): every loaded heart lamp surges and flickers for about 3 s, then settles as it
	 * was. Nothing is saved (a lamp loaded later never saw it).
	 */
	public void surgeHeart(ServerLevel level, BlockPos origin) {
		for (CityLink.Member m : members(level, origin))
			if (m.cityLink().district().isHeart())
				m.surged();
	}

	/** D96: a district woke (not the fourth): the spire's loaded seams flash from its roof to its crown and go dark again. */
	private static void pulseSpire(ServerLevel level, City c) {
		for (CityLink.Member m : members(level, c.origin))
			if (m instanceof DerelictLightBlockEntity light && m.cityLink().district().isHeart())
				light.pulse();
	}

	private static void woke(ServerLevel level, City c, District d, @Nullable BlockPos from, @Nullable BlockPos breaker) {
		for (Listener l : LISTENERS)
			l.districtWoke(level, c.origin, d, breaker);
		// "The Foundry wakes…" for everyone near where it started (or near the city, if it woke from afar)
		BlockPos at = from != null ? from : c.origin;
		Component text = Component.translatable("comedymonkeysaddons.wastes.wakes", d.displayName());
		for (ServerPlayer p : level.players())
			if (p.blockPosition().distSqr(at) <= 64 * 64) {
				p.displayClientMessage(text, true);
				ModAdvancements.award(p, ModAdvancements.WASTES_LIGHTS_ON);
			}
	}

	private static void heartWoke(ServerLevel level, City c) {
		for (Listener l : LISTENERS)
			l.heartWoke(level, c.origin);
		for (ServerPlayer p : level.players())
			if (p.blockPosition().distSqr(c.origin) <= 160 * 160)
				ModAdvancements.award(p, ModAdvancements.WASTES_CITY_OF_LIGHTS);
	}

	/** Every loaded block of the city catches up with its record. */
	private static void tell(ServerLevel level, City c, @Nullable BlockPos from) {
		for (CityLink.Member m : members(level, c.origin))
			m.cityChanged(from);
	}

	// ---------- the loaded city blocks ----------

	/** The loaded blocks of a city, as a copy (they may leave while it's walked). */
	static List<CityLink.Member> members(ServerLevel level, @Nullable BlockPos origin) {
		Map<BlockPos, Set<CityLink.Member>> byCity = MEMBERS.get(level.dimension());
		Set<CityLink.Member> members = byCity == null || origin == null ? null : byCity.get(origin);
		return members == null ? List.of() : new ArrayList<>(members);
	}

	/** A city block loaded (or was bound again): it hears its city's changes until it's removed. */
	public static void track(CityLink.Member member, ServerLevel level) {
		untrack(member);
		BlockPos origin = member.cityLink().origin();
		if (origin == null)
			return;
		MEMBERS.computeIfAbsent(level.dimension(), k -> new HashMap<>()).computeIfAbsent(origin, k -> new LinkedHashSet<>()).add(member);
		FILED.put(member, origin);
	}

	public static void untrack(CityLink.Member member) {
		BlockPos was = FILED.remove(member);
		if (was == null)
			return;
		for (Map<BlockPos, Set<CityLink.Member>> byCity : MEMBERS.values()) {
			Set<CityLink.Member> set = byCity.get(was);
			if (set != null && set.remove(member) && set.isEmpty())
				byCity.remove(was);
		}
	}

	/** Server stopped: nothing is loaded any more. */
	public static void clear() {
		MEMBERS.clear();
		FILED.clear();
		// a city placed as the server stopped belongs to that world, never the next one opened
		PLACED.clear();
		com.comedymonkey.createaddons.content.wastes.city.ClockworkCityPiece.forget();
	}

	/**
	 * Every tick, the cities world generation placed are recorded; once a second, a heart star that burned out puts out
	 * what only it lit (the reactor may not be loaded to say so). Nothing is loaded to do it.
	 */
	public static void onLevelTick(LevelTickEvent.Post event) {
		if (!(event.getLevel() instanceof ServerLevel level))
			return;
		// cities world generation placed since (it runs off the server thread, so it only leaves word here)
		for (var it = PLACED.iterator(); it.hasNext();) {
			var placed = it.next();
			if (placed.getKey() == level.dimension()) {
				of(level).add(placed.getValue());
				it.remove();
			}
		}
		if (level.getGameTime() % 20 != 0)
			return;
		CityRecord record = level.getDataStorage().get(new SavedData.Factory<>(CityRecord::new, CityRecord::load, null), NAME);
		if (record == null)
			return;
		long now = level.getGameTime();
		for (City c : record.cities.values())
			if (c.heartUntil > 0 && c.heartUntil <= now) {
				c.heartUntil = 0;
				record.setDirty();
				tell(level, c, null);
			}
	}

	// ---------- saving ----------

	private static CityRecord load(CompoundTag tag, HolderLookup.Provider registries) {
		CityRecord r = new CityRecord();
		for (Tag t : tag.getList("Cities", Tag.TAG_COMPOUND)) {
			CompoundTag ct = (CompoundTag) t;
			City c = r.make(BlockPos.of(ct.getLong("Origin")));
			int awake = ct.getInt("Awake"), cut = ct.getInt("Cut");
			for (int i = 0; i < DISTRICTS; i++) {
				c.awake[i] = (awake >> i & 1) != 0;
				c.cut[i] = (cut >> i & 1) != 0;
			}
			c.custodianAwake = ct.getBoolean("CustodianAwake");
			c.custodianDead = ct.getBoolean("CustodianDead");
			c.heartUntil = ct.getLong("HeartUntil");
		}
		return r;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
		ListTag list = new ListTag();
		for (City c : cities.values()) {
			CompoundTag ct = new CompoundTag();
			ct.putLong("Origin", c.origin.asLong());
			int awake = 0, cut = 0;
			for (int i = 0; i < DISTRICTS; i++) {
				awake |= c.awake[i] ? 1 << i : 0;
				cut |= c.cut[i] ? 1 << i : 0;
			}
			ct.putInt("Awake", awake);
			ct.putInt("Cut", cut);
			ct.putBoolean("CustodianAwake", c.custodianAwake);
			ct.putBoolean("CustodianDead", c.custodianDead);
			ct.putLong("HeartUntil", c.heartUntil);
			list.add(ct);
		}
		tag.put("Cities", list);
		return tag;
	}
}
