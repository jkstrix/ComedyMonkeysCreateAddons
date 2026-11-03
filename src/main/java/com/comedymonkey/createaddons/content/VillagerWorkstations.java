package com.comedymonkey.createaddons.content;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Villagers' workstations, through Engineer's Goggles and a wrench. Wearing goggles, a villager shows the workstation it
 * has claimed (and its trades, on Shift), and a workstation shows the villager who claimed it (see
 * {@code VillagerGoggles}). Sneak-right-click a villager with a wrench and it gives its workstation up: vanilla then
 * takes the profession from one that never traded, and one that has looks for another of its kind. For
 * {@link #IGNORE_TICKS} it won't take back the one it gave up ({@code AcquirePoiMixin}), so it can't walk straight back
 * to it; any other villager can. Seated villagers (a Create seat, on the ground or on a contraption) work the same.
 */
public final class VillagerWorkstations {
	/** How long a villager leaves alone the workstation it was made to give up. */
	public static final int IGNORE_TICKS = 20 * 120;
	/** The server answers goggles about villagers and workstations this close to the player. */
	public static final double VIEW_RANGE = 16;
	/** How far from a workstation its villager is looked for (vanilla claims them from 48 blocks; villagers wander). */
	private static final int OWNER_RANGE = 64;

	private record Ignored(GlobalPos site, long until) {}

	/** Each villager made to give up a workstation, and until when it leaves that one alone. Kept for the server's life. */
	private static final Map<UUID, Ignored> IGNORED = new HashMap<>();

	private VillagerWorkstations() {}

	/** Whether villagers can take this block as a workstation. */
	public static boolean isWorkstation(BlockState state) {
		return PoiTypes.forState(state).filter(VillagerProfession.ALL_ACQUIRABLE_JOBS).isPresent();
	}

	/** The workstation {@code villager} holds, or has claimed and is on its way to; null if neither. */
	@Nullable
	public static GlobalPos station(Villager villager) {
		Brain<Villager> brain = villager.getBrain();
		return brain.getMemory(MemoryModuleType.JOB_SITE).or(() -> brain.getMemory(MemoryModuleType.POTENTIAL_JOB_SITE)).orElse(null);
	}

	/** Whether {@code villager} has only claimed its workstation so far, and is still on its way to it. */
	public static boolean heading(Villager villager) {
		return villager.getBrain().getMemory(MemoryModuleType.JOB_SITE).isEmpty() && station(villager) != null;
	}

	/** The villager holding (or on its way to) the workstation at {@code pos}: one that holds it before one on its way. */
	@Nullable
	public static Villager owner(ServerLevel level, BlockPos pos) {
		GlobalPos site = GlobalPos.of(level.dimension(), pos);
		Villager heading = null;
		for (Villager villager : level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(OWNER_RANGE), v -> site.equals(station(v)))) {
			if (!heading(villager))
				return villager;
			heading = villager;
		}
		return heading;
	}

	/** Whether someone has claimed the workstation at {@code pos}, seen or not (its one ticket is taken). */
	public static boolean claimed(ServerLevel level, BlockPos pos) {
		return level.getPoiManager().getInRange(VillagerProfession.ALL_ACQUIRABLE_JOBS, pos, 0, PoiManager.Occupancy.IS_OCCUPIED).findAny().isPresent();
	}

	/** Sneak-right-click a villager with a wrench: it gives its workstation up. A plain right-click still trades. */
	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		if (!(event.getTarget() instanceof Villager villager) || !event.getEntity().isShiftKeyDown() || !Wrenches.isWrench(event.getItemStack()))
			return;
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
		if (event.getLevel().isClientSide)
			return;
		GlobalPos site = unassign(villager);
		Component message = site == null
			? Component.translatable("message.comedymonkeysaddons.villager.no_workstation", villager.getDisplayName())
			: Component.translatable("message.comedymonkeysaddons.villager.unassigned", villager.getDisplayName(), blockName(villager.getServer(), site));
		event.getEntity().displayClientMessage(message, true);
	}

	private static Component blockName(@Nullable MinecraftServer server, GlobalPos site) {
		ServerLevel level = server == null ? null : server.getLevel(site.dimension());
		return level != null && level.isLoaded(site.pos()) ? level.getBlockState(site.pos()).getBlock().getName()
			: Component.translatable("message.comedymonkeysaddons.villager.workstation");
	}

	/**
	 * Has {@code villager} give up the workstation it holds (or is on its way to), and leave that one alone for
	 * {@link #IGNORE_TICKS}. Vanilla does the rest: no workstation, no profession for a villager that never traded.
	 * Returns the workstation given up, or null if it had none.
	 */
	@Nullable
	public static GlobalPos unassign(Villager villager) {
		GlobalPos site = station(villager);
		if (site == null)
			return null;
		MinecraftServer server = villager.getServer();
		ServerLevel siteLevel = server == null ? null : server.getLevel(site.dimension());
		// (only a workstation still standing holds a ticket to give back)
		if (siteLevel != null && siteLevel.getPoiManager().getType(site.pos()).isPresent())
			siteLevel.getPoiManager().release(site.pos());
		Brain<Villager> brain = villager.getBrain();
		brain.eraseMemory(MemoryModuleType.JOB_SITE);
		brain.eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
		long now = villager.level().getGameTime();
		IGNORED.values().removeIf(ignored -> ignored.until <= now);
		IGNORED.put(villager.getUUID(), new Ignored(site, now + IGNORE_TICKS));
		return site;
	}

	/** Whether {@code mob} is leaving alone the workstation at {@code pos} because it was made to give it up. */
	public static boolean ignores(Entity mob, BlockPos pos) {
		if (IGNORED.isEmpty())
			return false;
		Ignored ignored = IGNORED.get(mob.getUUID());
		return ignored != null && ignored.until > mob.level().getGameTime() && ignored.site.dimension() == mob.level().dimension()
			&& ignored.site.pos().equals(pos);
	}

	/** How many more ticks {@code villager} leaves its old workstation alone; 0 once it's free to take it again. */
	public static int ignoringFor(Villager villager) {
		Ignored ignored = IGNORED.get(villager.getUUID());
		return ignored == null ? 0 : (int) Math.max(0, ignored.until - villager.level().getGameTime());
	}

	/** Forgets every ignored workstation, when the server stops. */
	public static void clear() {
		IGNORED.clear();
	}
}
