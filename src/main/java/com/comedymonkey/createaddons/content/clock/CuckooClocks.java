package com.comedymonkey.createaddons.content.clock;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The running Cuckoo Clocks in each level (server side), and the restock rule they bring: a villager within reach
 * of one ({@link #reaches}) restocks at 8:00 and 12:00 instead of vanilla's twice-a-day-whenever. It still has to get
 * to its workstation to do it, so it restocks on its first visit after the hour. Villagers out of range keep the
 * vanilla rule.
 */
public final class CuckooClocks {
	private static final Map<ResourceKey<Level>, Set<BlockPos>> RUNNING = new HashMap<>();

	private CuckooClocks() {}

	/** Called by each clock every tick, so a clock that stops or is unloaded drops out at once. */
	static void setRunning(Level level, BlockPos pos, boolean running) {
		if (level.isClientSide)
			return;
		if (running)
			RUNNING.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(pos.immutable());
		else {
			Set<BlockPos> clocks = RUNNING.get(level.dimension());
			if (clocks != null)
				clocks.remove(pos);
		}
	}

	/** Forgets every clock, when the server stops. */
	public static void clear() {
		RUNNING.clear();
	}

	public static int radius() {
		return CMConfig.Server.get(CMConfig.SERVER.clockRadius);
	}

	/** Is a running clock within reach of {@code at}? */
	public static boolean inRange(Level level, Vec3 at) {
		Set<BlockPos> clocks = RUNNING.get(level.dimension());
		return clocks != null && anyReaches(clocks, at, radius());
	}

	/** Does any of {@code clocks} reach {@code at}? */
	static boolean anyReaches(Collection<BlockPos> clocks, Vec3 at, int r) {
		for (BlockPos clock : clocks)
			if (reaches(clock, at, r))
				return true;
		return false;
	}

	/**
	 * A clock reaches a cylinder: {@code r} blocks across the ground and {@code r} up and down, so a big trading hall
	 * is covered from a clock on one wall whatever its floors.
	 */
	public static boolean reaches(BlockPos clock, Vec3 at, int r) {
		Vec3 c = clock.getCenter();
		double dx = at.x - c.x, dz = at.z - c.z, reach = r + 0.5;
		return dx * dx + dz * dz <= reach * reach && Math.abs(at.y - c.y) <= reach;
	}

	/**
	 * The clock's answer to {@code Villager.shouldRestock()}, or null to leave it to vanilla (no running clock near).
	 * The first time the villager asks after an 8:00 or 12:00 it restocks, if any trade has been used; after that,
	 * not until the next one.
	 */
	public static Boolean shouldRestock(Villager villager) {
		Level level = villager.level();
		if (level.isClientSide)
			return null;
		boolean inReach = inRange(level, villager.position());
		// Out of reach it's vanilla's call; don't give the villager (and its save) a check it doesn't need
		if (!inReach)
			return null;
		ClockCheck last = villager.getData(ModAttachments.CLOCK_RESTOCK_CHECK);
		ClockCheck now = new ClockCheck(level.getGameTime(), level.getDayTime());
		Boolean answer = decide(inReach, last, now, anyTradeUsed(villager));
		if (answer == null)
			return null;
		villager.setData(ModAttachments.CLOCK_RESTOCK_CHECK, now);
		if (answer)
			// whoever runs the hall earns it, anywhere the clock reaches
			ModAdvancements.awardNearby(level, villager.blockPosition(), ModAdvancements.CLOCK_RESTOCK, ItemStack.EMPTY, 0,
				radius());
		return answer;
	}

	/**
	 * The restock rule on its own: null (vanilla's call) out of reach; otherwise a restock if an hour has struck since
	 * the last check and a trade has been used.
	 */
	static Boolean decide(boolean inReach, ClockCheck last, ClockCheck now, boolean anyTradeUsed) {
		if (!inReach)
			return null;
		return last.hourStruckBy(now.gameTime(), now.dayTime()) && anyTradeUsed;
	}

	private static boolean anyTradeUsed(Villager villager) {
		for (MerchantOffer offer : villager.getOffers())
			if (offer.needsRestock())
				return true;
		return false;
	}

	/** How many villagers that can trade a clock at {@code pos} reaches. */
	public static int villagersReached(Level level, BlockPos pos, int r) {
		return level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(r + 1),
			v -> canTrade(v.getVillagerData().getProfession(), v.isBaby()) && reaches(pos, v.position(), r)).size();
	}

	/** Only grown villagers with a trade have stock to keep: not babies, the unemployed or nitwits. */
	static boolean canTrade(VillagerProfession profession, boolean baby) {
		return !baby && profession != VillagerProfession.NONE && profession != VillagerProfession.NITWIT;
	}
}
