package com.comedymonkey.createaddons.content.wastes;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * A Conduit Pylon's coil (P32, P33, P36), in its bottom block. Each hit snaps a fin (six to break it, config), but only
 * from a player's hand or projectile, never a fake player (a Deployer) or a contraption, and only while its city's
 * Custodian is awake. Broken, its crown slumps and it stops arcing; the Custodian's overload can re-light it.
 * <p>
 * The API phase 4 (the Custodian's fight) uses: {@link #hit}, {@link #relight}, {@link #isStanding}, {@link #arcPoint}
 * and {@link #reset} (the fight reset re-lights every pylon, P39). Breaking one tells the {@link CityRecord}'s listeners
 * nothing by itself; the fight reads {@link #isStanding} (and may poll it once a second).
 */
public class ConduitPylonBlockEntity extends CityBlockEntity {
	/** Ticks between hits that count: a flurry of clicks is one hit. */
	public static final int HIT_COOLDOWN = 8;

	private int hits;
	private boolean relit;
	private long lastHitAt = SubstationBlockEntity.NEVER, brokenAt = SubstationBlockEntity.NEVER, relitAt = SubstationBlockEntity.NEVER;
	/** Who struck it last (not saved): the Custodian turns on whoever breaks it (P35). */
	@Nullable
	private Player lastHitter;
	/** On the client: whether its city's Custodian is awake (it can be harmed). */
	private boolean harmable;

	public ConduitPylonBlockEntity(BlockPos pos, BlockState state) {
		super(ModWastes.CONDUIT_PYLON_BE.get(), pos, state);
	}

	public static int maxHits() {
		return CMConfig.SERVER.pylonHits();
	}

	public int hits() {
		return hits;
	}

	public boolean isRelit() {
		return relit;
	}

	public long lastHitAt() {
		return lastHitAt;
	}

	/** The player whose hit counted last (it broke it, once it's down); null after a reload. */
	@Nullable
	public Player lastHitter() {
		return lastHitter;
	}

	public long brokenAt() {
		return brokenAt;
	}

	public long relitAt() {
		return relitAt;
	}

	/** Its coil holds: it shields the Custodian (P34). */
	public boolean isStanding() {
		return hits < maxHits();
	}

	/** Two hits or fewer left (or re-lit): it sparks and flickers. */
	public boolean isFailing() {
		return relit || maxHits() - hits <= 2;
	}

	/** Fins left of six, as the hits count down (one snaps a hit at the default six). */
	public int finsLeft() {
		return relit || !isStanding() ? 0 : Math.max(0, 6 - (int) Math.round(hits * 6.0 / maxHits()));
	}

	/** Where its arcs leave from: the core in its crown (world coordinates). */
	public Vec3 arcPoint() {
		return new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 44 / 16.0, worldPosition.getZ() + 0.5);
	}

	/** Whether it can be harmed now: its city's Custodian is awake (P33). */
	public boolean harmable() {
		if (level == null || level.isClientSide)
			return harmable;
		CityRecord record = record();
		return record != null && link.bound() && record.custodianAwake(link.origin());
	}

	/**
	 * A hit from {@code source} (a player, or a projectile's shooter): counts if it can be harmed, the source is a real
	 * player (no Deployer's fake player, nothing a contraption or dispenser threw, no stray skeleton's arrow), and it's
	 * been {@link #HIT_COOLDOWN} ticks since the last. Returns whether it counted.
	 */
	public boolean hit(@Nullable Entity source) {
		if (level == null || level.isClientSide || !isStanding())
			return false;
		if (!(source instanceof Player) || source instanceof FakePlayer || !harmable())
			return false;
		long now = level.getGameTime();
		if (now - lastHitAt < HIT_COOLDOWN)
			return false;
		lastHitAt = now;
		lastHitter = (Player) source;
		hits++;
		if (level instanceof ServerLevel server) {
			Vec3 at = arcPoint();
			server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, isStanding() ? 8 : 24, 0.2, 0.2, 0.2, 0.3);
		}
		if (!isStanding()) {
			brokenAt = now;
			relit = false;
			// P56: "Short Circuit", for breaking a pylon
			if (source instanceof ServerPlayer player)
				ModAdvancements.award(player, ModAdvancements.WASTES_SHORT_CIRCUIT);
		}
		sync();
		return true;
	}

	/**
	 * The Custodian's overload (P36): a broken pylon pulls itself up, its core blazing, and needs the full six hits again
	 * (D43). Its fins stay snapped (they tumbled off); it keeps the failing look until it breaks again.
	 */
	public boolean relight() {
		if (level == null || isStanding())
			return false;
		relit = true;
		hits = 0;
		relitAt = level.getGameTime();
		sync();
		return true;
	}

	/** The fight reset (P39): whole again, every fin back. */
	public void reset() {
		hits = 0;
		relit = false;
		brokenAt = relitAt = lastHitAt = SubstationBlockEntity.NEVER;
		sync();
	}

	/** Its city's Custodian woke or slept: the client learns whether it can be harmed. */
	@Override
	protected void recheck(boolean live, @Nullable BlockPos from) {
		boolean now = harmable();
		if (now != harmable) {
			harmable = now;
			sync();
		}
	}

	@Override
	protected void writeLooks(CompoundTag tag) {
		tag.putInt("Hits", hits);
		tag.putBoolean("Relit", relit);
		tag.putLong("LastHitAt", lastHitAt);
		tag.putLong("BrokenAt", brokenAt);
		tag.putLong("RelitAt", relitAt);
		tag.putBoolean("Harmable", harmable);
	}

	@Override
	protected void readLooks(CompoundTag tag) {
		hits = tag.getInt("Hits");
		relit = tag.getBoolean("Relit");
		lastHitAt = tag.contains("LastHitAt") ? tag.getLong("LastHitAt") : SubstationBlockEntity.NEVER;
		brokenAt = tag.contains("BrokenAt") ? tag.getLong("BrokenAt") : SubstationBlockEntity.NEVER;
		relitAt = tag.contains("RelitAt") ? tag.getLong("RelitAt") : SubstationBlockEntity.NEVER;
		harmable = tag.getBoolean("Harmable");
	}
}
