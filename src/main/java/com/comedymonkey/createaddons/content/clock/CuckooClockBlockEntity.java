package com.comedymonkey.createaddons.content.clock;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.FixedStress;
import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Keeps time and puts on the show. While it turns (any speed, always the configured SU), villagers within reach
 * restock at 8:00 and 12:00 ({@link CuckooClocks}), and as each of those hours comes round it chimes, the doors open,
 * a carved villager rolls out onto the balcony with a little workstation (a random one from those within reach) and
 * the cuckoo calls the hour. The server plays the sounds; the renderer works out the moves from the show's start time.
 * <p>
 * Stopped, the hands, the sun and moon and the pendulum stop where they are, and there is no show.
 */
public class CuckooClockBlockEntity extends KineticBlockEntity {
	/** A show only starts this soon after its hour, so loading a clock later, or skipping time, doesn't start one late. */
	private static final int SHOW_WINDOW = 200;
	/** One swing of the pendulum, there and back. */
	public static final int SWING_TICKS = 40;
	/** How often the goggles recount the villagers in reach. */
	private static final int RECOUNT_TICKS = 20;
	// notes on the note block scale, 0-24 (F#3 to F#5 for the bell; 12 is the instrument's own pitch)
	private static final int[] CHIME = {10, 6, 8, 1};
	private static final int CALL_HIGH = 15, CALL_LOW = 11;

	/** The slot the clock last saw, so it notices the next one start. */
	private long lastSlot = Long.MIN_VALUE;
	/** The time it shows: kept up while running, held while stopped. */
	private long shownTime;
	private boolean wasRunning;
	/** Game time the current show started, or -1. */
	private long showStart = -1;
	private int showCalls = 8;
	/** The workstation the villager brings out this show, or null for none. */
	private @Nullable BlockState workstation;
	private int villagersInReach;
	/** Ponder: the scene runs its own clock, as a Ponder world's time doesn't move. */
	private boolean ponder;
	private long ponderTick;
	private float ponderTime, ponderRate;
	/** Game tick of the last count; starts a recount back so the first look counts at once. */
	private long countedAt = -RECOUNT_TICKS;

	public CuckooClockBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.CUCKOO_CLOCK.get(), pos, state);
	}

	// ---------------------------------------------------------------- running

	public boolean isRunning() {
		return getSpeed() != 0 && !isOverStressed();
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null || level.isClientSide) {
			if (ponder) {
				ponderTick++;
				ponderTime += ponderRate;
			}
			return;
		}

		keepStressFixed();
		boolean running = isRunning();
		CuckooClocks.setRunning(level, worldPosition, running);
		long dayTime = level.getDayTime();
		if (running)
			shownTime = dayTime;
		if (running != wasRunning) {
			wasRunning = running;
			setChanged();
			sendData();  // clients hold the hands where the server stopped them
		}

		long slot = ClockTime.slot(dayTime);
		if (slot != lastSlot) {
			if (running && slot == lastSlot + 1 && ClockTime.intoSlot(dayTime) < SHOW_WINDOW)
				startShow(slot);
			lastSlot = slot;
			setChanged();
		}
		if (showStart >= 0) {
			if (running)
				showTick((int) (level.getGameTime() - showStart));
			else
				endShow();  // stopped mid-show: the doors don't wait
		}
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if (level != null)
			CuckooClocks.setRunning(level, worldPosition, false);
	}

	private void startShow(long slot) {
		showStart = level.getGameTime();
		showCalls = ClockTime.hourOf(slot);
		workstation = pickWorkstation();
		sendData();
		if (showCalls == 12)
			ModAdvancements.awardNearby(level, worldPosition, ModAdvancements.CLOCK_NOON, ItemStack.EMPTY, 0);
		int r = CuckooClocks.radius();
		if (CuckooClocks.villagersReached(level, worldPosition, r) >= ModAdvancements.CLOCK_MARKET_VILLAGERS)
			ModAdvancements.awardNearby(level, worldPosition, ModAdvancements.CLOCK_MARKET, ItemStack.EMPTY, 0, r);
	}

	/** A random workstation within reach, in loaded chunks only (never loading one); none if there are none. */
	private @Nullable BlockState pickWorkstation() {
		if (!(level instanceof ServerLevel server))
			return null;
		int r = CuckooClocks.radius();
		PoiManager pois = server.getPoiManager();
		List<PoiRecord> sites = ChunkPos.rangeClosed(new ChunkPos(worldPosition), Math.floorDiv(r, 16) + 1)
			.filter(chunk -> server.hasChunk(chunk.x, chunk.z))
			.flatMap(chunk -> pois.getInChunk(type -> type.is(PoiTypeTags.ACQUIRABLE_JOB_SITE), chunk, PoiManager.Occupancy.ANY))
			.filter(poi -> CuckooClocks.reaches(worldPosition, poi.getPos().getCenter(), r))
			.toList();
		if (sites.isEmpty())
			return null;
		BlockState state = server.getBlockState(sites.get(server.random.nextInt(sites.size())).getPos());
		return PoiTypes.forState(state).isPresent() ? state : null;
	}

	/** The show's sounds, at {@code t} ticks in. */
	private void showTick(int t) {
		if (t < ClockTime.CHIME_NOTES * ClockTime.CHIME_GAP && t % ClockTime.CHIME_GAP == 0)
			note(SoundEvents.NOTE_BLOCK_BELL.value(), CHIME[t / ClockTime.CHIME_GAP], 1.2f);
		if (t == ClockTime.LOW_DOORS_OPEN || t == ClockTime.BIRD_DOORS_OPEN)
			play(SoundEvents.WOODEN_TRAPDOOR_OPEN, 0.25f, 1.7f);
		if (t == ClockTime.VILLAGER_OUT + ClockTime.VILLAGER_OUT_TICKS) {
			play(SoundEvents.VILLAGER_AMBIENT, 0.35f, 1.9f);
			SoundEvent work = workSound();
			if (work != null)
				play(work, 0.4f, 1.5f);
		}
		for (int i = 0; i < showCalls; i++) {
			if (t == ClockTime.callTick(i))
				note(SoundEvents.NOTE_BLOCK_FLUTE.value(), CALL_HIGH, 1);
			else if (t == ClockTime.callTick(i) + ClockTime.CALL_SECOND_NOTE)
				note(SoundEvents.NOTE_BLOCK_FLUTE.value(), CALL_LOW, 1);
		}
		int birdShut = ClockTime.callsEnd(showCalls) + ClockTime.BIRD_TICKS + ClockTime.DOOR_TICKS;
		int villagerShut = ClockTime.villagerIn(showCalls) + ClockTime.VILLAGER_OUT_TICKS + ClockTime.DOOR_TICKS;
		if (t == birdShut || t == villagerShut)
			play(SoundEvents.WOODEN_TRAPDOOR_CLOSE, 0.25f, 1.7f);
		if (t >= ClockTime.showLength(showCalls))
			endShow();
	}

	private void endShow() {
		showStart = -1;
		workstation = null;
		sendData();
	}

	/** The work sound of the profession whose workstation came out, as if the little villager were at it. */
	private @Nullable SoundEvent workSound() {
		if (workstation == null)
			return null;
		Holder<PoiType> poi = PoiTypes.forState(workstation).orElse(null);
		if (poi == null)
			return null;
		for (VillagerProfession profession : BuiltInRegistries.VILLAGER_PROFESSION)
			if (profession.heldJobSite().test(poi))
				return profession.workSound();
		return null;
	}

	private void note(SoundEvent sound, int note, float volume) {
		play(sound, volume, (float) Math.pow(2, (note - 12) / 12.0));
	}

	private void play(SoundEvent sound, float volume, float pitch) {
		level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, volume, pitch);
	}

	// ---------------------------------------------------------------- stress: fixed SU at any speed

	@Override
	public float calculateStressApplied() {
		float impact = FixedStress.impact(this, stressSU(), false);
		lastStressApplied = impact;
		return impact;
	}

	private static float stressSU() {
		return CMConfig.Server.get(CMConfig.SERVER.clockStress);
	}

	@Override
	public void onSpeedChanged(float previousSpeed) {
		super.onSpeedChanged(previousSpeed);
		if (level != null && !level.isClientSide)
			keepStressFixed();
	}

	/** Keeps the network's copy of our impact right for the current speed (see {@link FixedStress#keep}). */
	private void keepStressFixed() {
		if (FixedStress.keep(this, FixedStress.impact(this, stressSU(), false)))
			sendData();
	}

	// ---------------------------------------------------------------- what the renderer shows

	/** The time the hands and the sun and moon show: now while it runs, where it stopped otherwise. */
	public float getShownTime(float partialTicks) {
		if (ponder)
			return ponderTime + ponderRate * partialTicks;
		// only the time of day goes into the float, so the hands stay smooth however old the world
		if (level != null && isRunning())
			return ClockTime.timeOfDay(level.getDayTime()) + (level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT) ? partialTicks : 0);
		return ClockTime.timeOfDay(shownTime);
	}

	/** Ticks into the current show, or -1 when there's none. */
	public float getShowTime(float partialTicks) {
		if (level == null || showStart < 0)
			return -1;
		float t = (ponder ? ponderTick : level.getGameTime()) - showStart + partialTicks;
		return t >= 0 && t < ClockTime.showLength(showCalls) ? t : -1;
	}

	/**
	 * Ticks for the pendulum's swing: the game's, or the scene's own in Ponder, wrapped to a whole number of swings
	 * (each {@link #SWING_TICKS}) so the float stays precise.
	 */
	public float getSwingTicks(float partialTicks) {
		long ticks = ponder ? ponderTick : level == null ? 0 : level.getGameTime();
		return Math.floorMod(ticks, SWING_TICKS * 60L) + partialTicks;
	}

	/** Ponder only: show {@code dayTime}, moving on {@code rate} ticks of the day every tick (0 holds it). */
	public void setPonderTime(float dayTime, float rate) {
		ponder = true;
		ponderTime = dayTime;
		ponderRate = rate;
	}

	/** Ponder only: the show starts now, the cuckoo calling {@code calls} times, the villager bringing {@code station}. */
	public void startPonderShow(int calls, @Nullable BlockState station) {
		ponder = true;
		showStart = ponderTick;
		showCalls = calls;
		workstation = station;
	}

	public int getShowCalls() {
		return showCalls;
	}

	public @Nullable BlockState getWorkstation() {
		return workstation;
	}

	/** The pendulum and weights hang into the block below, the roof's finial just above. */
	@Override
	protected AABB createRenderBoundingBox() {
		return new AABB(worldPosition).expandTowards(0, -1, 0).inflate(0.25);
	}

	// ---------------------------------------------------------------- goggles

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		lang().translate("gui.goggles.cuckoo_clock").forGoggles(tooltip);
		long time = (long) getShownTime(0);
		lang().translate("gui.goggles.cuckoo_clock.time", clockText(ClockTime.hours(time), ClockTime.minutes(time)))
			.style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
		if (!isRunning())
			lang().translate("gui.goggles.cuckoo_clock.stopped").style(ChatFormatting.RED).forGoggles(tooltip, 1);
		else {
			long now = level.getDayTime();
			int until = ClockTime.untilNextSlot(now);
			String next = clockText(ClockTime.hourOf(ClockTime.slot(now + until)), 0);
			lang().translate("gui.goggles.cuckoo_clock.next", next, durationText(until)).style(ChatFormatting.AQUA)
				.forGoggles(tooltip, 1);
		}
		lang().translate("gui.goggles.cuckoo_clock.villagers", villagersInReach()).style(ChatFormatting.GRAY)
			.forGoggles(tooltip, 1);
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		return true;
	}

	/** Villagers this clock reaches, as this side sees them, recounted once a second. */
	private int villagersInReach() {
		if (level == null)
			return 0;
		if (level.getGameTime() - countedAt >= RECOUNT_TICKS) {
			countedAt = level.getGameTime();
			villagersInReach = CuckooClocks.villagersReached(level, worldPosition, CuckooClocks.radius());
		}
		return villagersInReach;
	}

	static String clockText(int hours, int minutes) {
		return String.format(Locale.ROOT, "%d:%02d", hours, minutes);
	}

	/** Game ticks as real minutes and seconds. */
	static String durationText(int ticks) {
		int seconds = (ticks + 19) / 20;
		return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
	}

	private static LangBuilder lang() {
		return new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID);
	}

	// ---------------------------------------------------------------- persistence

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		tag.putLong("LastSlot", lastSlot);
		tag.putLong("ShownTime", shownTime);
		if (clientPacket && showStart >= 0) {
			tag.putLong("ShowStart", showStart);
			tag.putInt("ShowCalls", showCalls);
			if (workstation != null)
				tag.put("Workstation", NbtUtils.writeBlockState(workstation));
		}
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		lastSlot = tag.contains("LastSlot") ? tag.getLong("LastSlot") : Long.MIN_VALUE;
		shownTime = tag.getLong("ShownTime");
		if (clientPacket) {
			showStart = tag.contains("ShowStart") ? tag.getLong("ShowStart") : -1;
			showCalls = tag.contains("ShowCalls") ? tag.getInt("ShowCalls") : 8;
			workstation = tag.contains("Workstation")
				? NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("Workstation")) : null;
		}
	}
}
