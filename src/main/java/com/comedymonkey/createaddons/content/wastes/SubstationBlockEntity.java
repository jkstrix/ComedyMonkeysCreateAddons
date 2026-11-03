package com.comedymonkey.createaddons.content.wastes;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.registry.ModWastes;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

/**
 * A Substation's breaker (D96, P8, P50). Dormant, a player holding use on it hauls its lever over: holding use repeats
 * the use every 4 ticks, so a haul is held while its uses keep coming ({@link #LET_GO_TICKS}), each player's apart. After
 * {@link #HOLD_TICKS} held, the breaker clunks home and its district wakes for good in the {@link CityRecord} (the lamps
 * ripple on, the locks open, the bots unfold); let go sooner and the lever springs back. No power is needed, and woken
 * it needs none: the district stays lit. Its logic runs on the lower half; the upper half only shows the goggles.
 * <p>
 * Asleep it leads players in: as one passes, the district's lamps nearest it sputter, farthest first ({@link #beckon}).
 * Its sparks and starlight are the block's ({@link SubstationBlock#animateTick}) and the renderer's.
 */
public class SubstationBlockEntity extends CityBlockEntity implements IHaveGoggleInformation {
	/** Column steps: the column climbs in ten as the lever is hauled. */
	public static final int STEPS = 10;
	/** Game time it woke while loaded (the lever's clunk); far in the past if it didn't or long ago. */
	public static final long NEVER = Long.MIN_VALUE / 2;
	/** D96: ticks of holding use that haul the breaker over (provisional: about 3 s). */
	public static final int HOLD_TICKS = 60;
	/** A haul whose use hasn't come again for this long was let go (held, it comes every 4 ticks). */
	public static final int LET_GO_TICKS = 7;
	/** Let go, the lever springs back this many times faster than it was hauled. */
	public static final float SPRING = 4;
	/** Between a player's uses, the lever is shown going on at most this far past the last (it waits for the next). */
	static final int AHEAD = 5;
	/** D96 (provisional): a player within this reach and not yet at it makes its nearest lamps sputter, every so often. */
	static final double BECKON_RANGE = 40, BECKON_NEAR = 6;
	/** ...the nearest this many of its district's lamps within this reach of it, one after another this far apart. */
	static final int BECKON_LAMPS = 6, BECKON_REACH = 28, BECKON_STEP = 6, BECKON_GAP = 200;

	private boolean woken;
	private long wokeAt = NEVER;
	/** It woke by a hand on its breaker: the lever was already hauled over, only its last clunk is left. */
	private boolean thrown;
	/** The hauls under way, by player (not saved: a haul is a moment). */
	private final Map<UUID, Haul> hauls = new HashMap<>();
	/** Looks: how far the lever was hauled when last told (ticks of {@link #HOLD_TICKS}), when, and whether a hand is on it. */
	private float haulShown;
	private long haulShownAt = NEVER;
	private boolean hauling;
	private long beckonedAt = NEVER;

	/** One player's haul: when it started (as if from the lever's place then) and their last use. */
	private static final class Haul {
		final long start;
		long lastUse;

		Haul(long start, long now) {
			this.start = start;
			this.lastUse = now;
		}
	}

	public SubstationBlockEntity(BlockPos pos, BlockState state) {
		super(ModWastes.SUBSTATION_BE.get(), pos, state);
	}

	private boolean isUpper() {
		return getBlockState().getValue(SubstationBlock.HALF) == DoubleBlockHalf.UPPER;
	}

	/** The lower half, which does the work (this one, or the one below). */
	@Nullable
	private SubstationBlockEntity lower() {
		if (!isUpper())
			return this;
		return level != null && level.getBlockEntity(worldPosition.below()) instanceof SubstationBlockEntity be && !be.isUpper() ? be : null;
	}

	public boolean isWoken() {
		return woken;
	}

	public long wokeAt() {
		return wokeAt;
	}

	/** It woke by a hand on its breaker (not by its city's record: the heart's star, or loading after). */
	public boolean thrown() {
		return thrown;
	}

	/** Players hauling it now (the server's count). */
	public int haulers() {
		return hauls.size();
	}

	// ---------- the haul (D96)

	/**
	 * A player's use on it, held or new (the block forwards it, server side): their haul goes on, or starts from where the
	 * lever is (springing back, it's caught). False if there's nothing to haul (it's awake).
	 */
	public boolean haul(Player player) {
		if (level == null || level.isClientSide || woken || isUpper())
			return false;
		long now = level.getGameTime();
		Haul h = hauls.get(player.getUUID());
		if (h == null || now - h.lastUse > LET_GO_TICKS) {
			h = new Haul(now - (long) Math.floor(shown(now, 0)), now);
			hauls.put(player.getUUID(), h);
		}
		h.lastUse = now;
		if (level instanceof ServerLevel server) {
			// sparks off the jaw as it's hauled, more as it nears home (sound: the ratchet's grind and crackle)
			Vec3 at = leverPoint();
			int n = 2 + (int) (4 * Math.min(1, (now - h.start) / (float) HOLD_TICKS));
			server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, n, 0.12, 0.15, 0.12, 0.12);
		}
		showHaul(now);
		return true;
	}

	/** Every tick on the server's lower half: hauls let go spring back; one held long enough throws the breaker. */
	void tick() {
		if (level == null || level.isClientSide)
			return;
		long now = level.getGameTime();
		if (!hauls.isEmpty()) {
			boolean letGo = false, home = false;
			for (var it = hauls.values().iterator(); it.hasNext();) {
				Haul h = it.next();
				if (now - h.lastUse > LET_GO_TICKS) {
					it.remove();
					letGo = true;
				} else if (now - h.start >= HOLD_TICKS)
					home = true;
			}
			if (home) {
				hauls.clear();
				thrown = true;
				// sound: the breaker's clunk (D96, the sound pass)
				wake(now, true);
				return;
			}
			if (letGo)
				showHaul(now);
		}
		if (!woken && Math.floorMod(now + worldPosition.hashCode(), 20) == 0)
			beckon(now);
	}

	/** The hauls changed: clients are told how far the lever is now, from which they carry it on. */
	private void showHaul(long now) {
		float at = shown(now, 0);
		long furthest = -1;
		for (Haul h : hauls.values())
			furthest = Math.max(furthest, now - h.start);
		haulShown = furthest >= 0 ? Math.min(HOLD_TICKS, furthest) : at;
		haulShownAt = now;
		hauling = !hauls.isEmpty();
		sync();
	}

	/**
	 * How far the lever is hauled, in ticks of {@link #HOLD_TICKS}, at game time {@code now} plus {@code partial}: going on
	 * with a hand on it (a little past the last word, until the next), springing back without. Full once woken.
	 */
	public float shown(long now, float partial) {
		if (woken)
			return HOLD_TICKS;
		if (haulShownAt == NEVER)
			return 0;
		float t = (now - haulShownAt) + partial;
		if (hauling)
			return Math.min(HOLD_TICKS, haulShown + Math.min(t, AHEAD));
		return Math.max(0, haulShown - t * SPRING);
	}

	/** A hand is on its lever now (as last told). */
	public boolean hauled() {
		return hauling;
	}

	/** Where the lever's jaw is, for its sparks: by its hinge on the cabinet's east side as built (north), turned with it. */
	private Vec3 leverPoint() {
		Direction facing = getBlockState().getValue(SubstationBlock.FACING);
		double dx = 8.5 / 16, dz = 0;
		int turns = switch (facing) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
		for (int i = 0; i < turns; i++) {
			double t = dx;
			dx = -dz;
			dz = t;
		}
		return new Vec3(worldPosition.getX() + 0.5 + dx, worldPosition.getY() + 1.25, worldPosition.getZ() + 0.5 + dz);
	}

	/**
	 * D96: while a player passes (in reach, not yet at it), the district's lamps nearest it sputter one after another,
	 * the farthest first, leading in; every {@link #BECKON_GAP} ticks at most.
	 */
	private void beckon(long now) {
		if (!link.bound() || !(level instanceof ServerLevel server) || now - beckonedAt < BECKON_GAP || !hauls.isEmpty())
			return;
		Vec3 here = Vec3.atCenterOf(worldPosition);
		boolean passing = false;
		for (ServerPlayer p : server.players()) {
			double d = p.distanceToSqr(here);
			if (!p.isSpectator() && d <= BECKON_RANGE * BECKON_RANGE && d > BECKON_NEAR * BECKON_NEAR)
				passing = true;
		}
		if (!passing)
			return;
		beckonedAt = now;
		List<DerelictLightBlockEntity> lamps = CityRecord.members(server, link.origin()).stream()
			.filter(m -> m instanceof DerelictLightBlockEntity l && l.isLamp() && m.cityLink().district() == link.district()
				&& l.getBlockPos().distSqr(worldPosition) <= BECKON_REACH * BECKON_REACH)
			.map(m -> (DerelictLightBlockEntity) m)
			.sorted(Comparator.comparingDouble(l -> l.getBlockPos().distSqr(worldPosition)))
			.limit(BECKON_LAMPS).toList();
		for (int i = 0; i < lamps.size(); i++)
			lamps.get(lamps.size() - 1 - i).beckon(i * BECKON_STEP);
	}

	/**
	 * Wakes: for good, its district with it. {@code live}: it happened here and now (the lever clunks home); false when
	 * it only catches up with the record (a heart star woke the city while it was unloaded).
	 */
	private void wake(long now, boolean live) {
		if (woken)
			return;
		woken = true;
		wokeAt = live ? now : NEVER;
		hauling = false;
		hauls.clear();
		unlock();
		if (level instanceof ServerLevel server) {
			CityRecord record = record();
			if (record != null && link.bound())
				record.wake(server, link.origin(), link.district(), worldPosition);
			else if (live) {
				// a Substation of no city still wakes itself, with the same word to players near
				Component text = Component.translatable("comedymonkeysaddons.wastes.wakes", link.district().displayName());
				for (ServerPlayer p : server.players())
					if (p.blockPosition().distSqr(worldPosition) <= 64 * 64)
						p.displayClientMessage(text, true);
			}
		}
		sync();
		level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
	}

	/** Both halves become ordinary blocks. */
	private void unlock() {
		if (level == null)
			return;
		BlockState state = getBlockState();
		if (state.getValue(SubstationBlock.LOCKED))
			level.setBlock(worldPosition, state.setValue(SubstationBlock.LOCKED, false), Block.UPDATE_ALL);
		BlockState up = level.getBlockState(worldPosition.above());
		if (up.is(state.getBlock()) && up.getValue(SubstationBlock.LOCKED))
			level.setBlock(worldPosition.above(), up.setValue(SubstationBlock.LOCKED, false), Block.UPDATE_ALL);
	}

	/** The comparator: 0 dormant, 15 awake. */
	public int comparator() {
		return woken ? 15 : 0;
	}

	/** Its district woke some other way (the heart's star) or was already awake when it loaded: it wakes to match. */
	@Override
	protected void recheck(boolean live, @Nullable BlockPos from) {
		if (isUpper() || level == null)
			return;
		CityRecord record = record();
		if (record == null || !link.bound())
			return;
		if (record.awake(link.origin(), link.district()))
			wake(level.getGameTime(), live);
		else if (woken && level instanceof ServerLevel server)
			// woken while its city had no record yet (bound after waking): the record learns it now
			record.wake(server, link.origin(), link.district(), worldPosition);
	}

	/** Broken (P50): a woken one's district stays awake, its own grid cut for good. */
	void removed() {
		if (level instanceof ServerLevel server && woken && link.bound())
			CityRecord.of(server).cut(server, link.origin(), link.district());
	}

	// ---------- saving ----------

	@Override
	protected void writeLooks(CompoundTag tag) {
		tag.putBoolean("Woken", woken);
		tag.putLong("WokeAt", wokeAt);
		tag.putBoolean("Thrown", thrown);
		tag.putFloat("Haul", haulShown);
		tag.putLong("HaulAt", haulShownAt);
		tag.putBoolean("Hauling", hauling);
	}

	@Override
	protected void readLooks(CompoundTag tag) {
		woken = tag.getBoolean("Woken");
		wokeAt = tag.contains("WokeAt") ? tag.getLong("WokeAt") : NEVER;
		thrown = tag.getBoolean("Thrown");
		haulShown = tag.getFloat("Haul");
		haulShownAt = tag.contains("HaulAt") ? tag.getLong("HaulAt") : NEVER;
		// a haul is never under way when it loads (it's let go by then)
		hauling = tag.getBoolean("Hauling") && level != null && level.isClientSide;
	}

	// ---------- goggles ----------

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean sneaking) {
		SubstationBlockEntity be = lower();
		if (be == null)
			return false;
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.substation").forGoggles(tooltip);
		if (!be.woken) {
			new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.substation.dormant").style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
			new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.substation.throw").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
			return true;
		}
		new LangBuilder(ComedyMonkeysCreateAddons.MOD_ID).translate("gui.goggles.substation.awake").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
		return true;
	}
}
