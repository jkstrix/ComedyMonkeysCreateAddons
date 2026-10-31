package com.comedymonkey.createaddons.content.wastes.bot;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipClock;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipLibrary;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipPlayback;
import com.comedymonkey.createaddons.content.wastes.CityLink;
import com.comedymonkey.createaddons.content.wastes.CityRecord;
import com.comedymonkey.createaddons.content.wastes.District;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;

/**
 * A bot of a Clockwork Wastes city (design/specs/clockwork-wastes.md; looks and motion in
 * design/briefs/clockwork-wastes-bots.md): the Scuttler, the Sentry and the Custodian. It belongs to a district of a
 * city ({@link CityLink}, saved with it) and sleeps until its {@link CityRecord} says that district woke (P8); the
 * record's listener ({@link WastesBots}) tells the loaded ones at once, and one loaded later catches up on its first
 * tick, as the city's blocks do (P48). On Peaceful it never wakes (P22). Placed only by the city, never spawned
 * naturally (P6), persistent, and it never despawns (P24).
 * <p>
 * Like the Clockwork Juggernaut, the server picks the clip and when it started; clients pose the model from it (the
 * clips are the Blockbench previews', exported by design/models/clockwork-wastes-bots.game-export.js).
 */
public abstract class CityBot extends Monster {
	private static final EntityDataAccessor<Integer> CLIP = SynchedEntityData.defineId(CityBot.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> CLIP_START = SynchedEntityData.defineId(CityBot.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> CLIP_SPEED = SynchedEntityData.defineId(CityBot.class, EntityDataSerializers.FLOAT);
	/** Ticks into the clip it was started at. */
	private static final EntityDataAccessor<Integer> CLIP_OFFSET = SynchedEntityData.defineId(CityBot.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DORMANT = SynchedEntityData.defineId(CityBot.class, EntityDataSerializers.BOOLEAN);

	/** P8: a woken district's bots stir within 5 s, each at its own moment. */
	public static final int WAKE_SPREAD = 100;

	/** Its city and district (no city: placed by an egg or a command; it reads as a district that never wakes). */
	protected final CityLink link = new CityLink();
	/** Client: what's playing and the cross-fade. */
	public final ClipPlayback playback = new ClipPlayback();
	private int seenClip = -1, seenClipStart;
	private double localClipStart;
	/** Game time it stirs at (its district woke), or -1. */
	private long wakeAt = -1;
	/** It has looked at its city's record since it loaded. */
	private boolean caughtUp;
	/** Peaceful sent it to sleep (D56): it wakes again once the difficulty rises. */
	private boolean peaceSlept;

	protected CityBot(EntityType<? extends CityBot> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	/** Its clips (read once from the mod jar, the same on both sides). */
	public abstract ClipLibrary clips();

	/** The clip it shows asleep (held at its first frame). */
	protected abstract String dormantClip();

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CLIP, 0);
		builder.define(CLIP_START, 0);
		builder.define(CLIP_SPEED, 1f);
		builder.define(CLIP_OFFSET, 0);
		builder.define(DORMANT, false);
	}

	// ======================================================================== its city

	/** Binds it to a district of a city (the city's builder, after placing it). */
	public void bind(@Nullable BlockPos origin, District district) {
		link.set(origin, district);
		caughtUp = false;
	}

	@Nullable
	public BlockPos cityOrigin() {
		return link.origin();
	}

	public District district() {
		return link.district();
	}

	@Nullable
	protected CityRecord record() {
		return level() instanceof ServerLevel server ? CityRecord.of(server) : null;
	}

	/** Its district is awake in its city's record. */
	protected boolean districtAwake() {
		CityRecord record = record();
		return record != null && link.bound() && record.awake(link.origin(), link.district());
	}

	/** P22, P41: on Peaceful bots never wake (the game tests stand a city in for Peaceful without making the server so). */
	protected boolean peaceful() {
		return level().getDifficulty() == Difficulty.PEACEFUL || link.bound() && CityRecord.PEACEFUL_FOR_TESTS.contains(link.origin());
	}

	// ======================================================================== asleep and waking

	public boolean dormant() {
		return entityData.get(DORMANT);
	}

	/** Asleep until its district wakes: the city places its bots so. */
	public void makeDormant() {
		entityData.set(DORMANT, true);
		wakeAt = -1;
		setTarget(null);
		getNavigation().stop();
		playClip(dormantClip(), 0, 0);
		onDormant();
	}

	/** It stirs in {@code delay} ticks (its district woke), unless it's already awake or it's Peaceful then. */
	public void wakeIn(int delay) {
		if (dormant() && (wakeAt < 0 || level().getGameTime() + delay < wakeAt))
			wakeAt = level().getGameTime() + Math.max(0, delay);
	}

	/** Game time it's due to stir at, or -1. */
	public long wakeAt() {
		return wakeAt;
	}

	/** Wakes it now (P8; the game tests). Nothing on Peaceful. */
	public void wake() {
		wakeAt = -1;
		if (!dormant() || peaceful())
			return;
		peaceSlept = false;
		entityData.set(DORMANT, false);
		onWake();
	}

	protected void onDormant() {}

	protected void onWake() {}

	/** Loaded (or bound) again: what it missed. A district that woke while it was away wakes it within 5 s. */
	protected void catchUp() {
		if (dormant() && districtAwake())
			wakeIn(random.nextInt(WAKE_SPREAD));
	}

	/**
	 * Server, every tick (asleep too): its city's record looked at once after loading, a stir that's due, and Peaceful
	 * (D56): awake when the world turns Peaceful, it goes back to sleep; asleep for that reason, it wakes again once the
	 * difficulty rises.
	 */
	protected void cityTick() {
		if (!caughtUp) {
			caughtUp = true;
			catchUp();
		}
		boolean peaceful = peaceful();
		if (!dormant() && peaceful)
			sleepForPeace();
		else if (dormant() && peaceSlept && !peaceful) {
			peaceSlept = false;
			wakeAfterPeace();
		}
		if (wakeAt >= 0 && level().getGameTime() >= wakeAt)
			wake();
	}

	/** The world turned Peaceful while it was awake (D56): it sleeps (and wakes again when the difficulty rises). */
	protected void sleepForPeace() {
		makeDormant();
		peaceSlept = true;
	}

	/** The difficulty rose again after Peaceful put it to sleep (D56): it stirs within 5 s, as its district waking does. */
	protected void wakeAfterPeace() {
		if (!link.bound() || districtAwake())
			wakeIn(random.nextInt(WAKE_SPREAD));
	}

	/** Peaceful put it to sleep, and it hasn't woken since (D56). */
	public boolean sleptForPeace() {
		return peaceSlept;
	}

	/** Asleep, it doesn't think: no goals, no target, no steps (vanilla skips its AI while it's immobile). */
	@Override
	protected boolean isImmobile() {
		return super.isImmobile() || dormant();
	}

	// ======================================================================== clips

	public void playClip(String name) {
		playClip(name, 1, 0);
	}

	/** Starts a clip now, {@code offset} seconds in, at {@code speed} (0 holds it still at the offset). */
	public void playClip(String name, float speed, float offset) {
		Clip clip = clips().clip(name);
		int lead = speed > 0 ? Mth.floor(offset * 20 / speed) : 0;
		entityData.set(CLIP, clips().id(clip));
		entityData.set(CLIP_START, (int) (level().getGameTime() - lead));
		entityData.set(CLIP_SPEED, speed);
		entityData.set(CLIP_OFFSET, lead);
	}

	public Clip clip() {
		Clip clip = clips().byId(entityData.get(CLIP));
		return clip != null ? clip : clips().all().get(0);
	}

	public float clipSpeed() {
		return entityData.get(CLIP_SPEED);
	}

	/** Seconds the current clip has played (server time). */
	public float clipElapsed() {
		long now = level().getGameTime();
		long start = now - (int) (now - entityData.get(CLIP_START));
		return (now - start) / 20f * clipSpeed();
	}

	protected boolean clipDone() {
		return clip().finished(clipElapsed());
	}

	protected boolean playing(String name) {
		return clip().name().equals(name);
	}

	/** Plays {@code name} unless it's already on, or a clip that plays once hasn't finished. */
	protected void playIfFree(String name) {
		playIfFree(name, 1);
	}

	/** The same at {@code speed}. */
	protected void playIfFree(String name, float speed) {
		if (!playing(name) && (clip().mode() == Clip.Mode.LOOP || clipDone()))
			playClip(name, speed, 0);
	}

	/** Seconds into the clip it's playing now, wrapped for a loop (where its pose is). */
	public float clipLocalTime() {
		return clip().localTime(clipElapsed());
	}

	/** The time its clips play at, in ticks: on clients the steady {@link ClipClock}, as Trade Bot's and the Juggernaut's do. */
	public double clipNow(float partialTick) {
		return level().isClientSide ? ClipClock.now(partialTick) : level().getGameTime() + partialTick;
	}

	/** When the current clip started, on the {@link #clipNow} clock. */
	public double clipStartTime() {
		int id = entityData.get(CLIP), start = entityData.get(CLIP_START);
		if (!level().isClientSide)
			return level().getGameTime() - (int) (level().getGameTime() - start);
		if (id != seenClip || start != seenClipStart) {
			// how far into it the server already is when we first see it; a clip that changes while we watch starts now
			int lead = seenClip == -1 ? Math.max(0, (int) (level().getGameTime() - start)) : entityData.get(CLIP_OFFSET);
			seenClip = id;
			seenClipStart = start;
			localClipStart = ClipClock.now(0) - lead;
		}
		return localClipStart;
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide)
			playback.observe(clip(), clipStartTime(), clipSpeed(), clipNow(0));
		else if (!isDeadOrDying()) {
			// (dying, it plays its death clip out: nothing wakes it or puts it to sleep)
			cityTick();
			if (dormant() && getTarget() != null)
				setTarget(null);
		}
		// sound: the clips' cues play here once the bots have sounds (design/sound/clockwork-wastes-bots/<bot>.cues.json)
	}

	// ======================================================================== dying

	/**
	 * Its own death clip (brief: a Scuttler's lens cracks and its legs curl in, a Sentry's petals splay and droop), played
	 * as it dies instead of vanilla's topple; it's gone, in vanilla's puff, when the clip ends. Null: vanilla's death.
	 */
	@Nullable
	protected String deathClip() {
		return null;
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (!level().isClientSide && deathClip() != null && isDeadOrDying())
			playClip(deathClip(), 1, 0); // sound: its death
	}

	@Override
	protected void tickDeath() {
		String clip = deathClip();
		if (clip == null) {
			super.tickDeath();
			return;
		}
		if (++deathTime >= Mth.ceil(clips().clip(clip).length() * 20) && !level().isClientSide && !isRemoved()) {
			level().broadcastEntityEvent(this, (byte) 60);
			remove(RemovalReason.KILLED);
		}
	}

	// ======================================================================== players and damage

	/** Game tests only: stand-in players (fake players put straight into the world) that count as real ones. */
	public static final java.util.Set<java.util.UUID> STAND_INS_FOR_TESTS = java.util.concurrent.ConcurrentHashMap.newKeySet();

	/** A real player in the fight: alive, not creative or spectating, not a Deployer. */
	public static boolean fightable(Player p) {
		return p.isAlive() && !p.isCreative() && !p.isSpectator() && (!(p instanceof FakePlayer) || STAND_INS_FOR_TESTS.contains(p.getUUID()));
	}

	/**
	 * D52: asleep, nothing touches it (a district's bots are fought after it wakes) and a hit doesn't wake it; only
	 * /kill and the void still end it.
	 */
	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		if (dormant() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))
			return true;
		return super.isInvulnerableTo(source);
	}

	/** A real player (not a Deployer or other fake one; the game tests' stand-ins count as real), in any game mode. */
	public static boolean realPlayer(Player p) {
		return !(p instanceof FakePlayer) || STAND_INS_FOR_TESTS.contains(p.getUUID());
	}

	// ======================================================================== hits from either side (D67)

	/**
	 * D67: the left-side hit clips ({@value #HIT_LEFT}: the Sentry's and the Custodian's right-side {@value #HIT} mirrored,
	 * design/models/clockwork-wastes-bots.gen.js) are previewed in Blockbench and wait for the user's approval; they aren't
	 * exported to the game yet. Until they are, every hit plays the right-side clip whichever side it came from. Turn this
	 * on once {@value #HIT_LEFT} is in the Sentry's and the Custodian's clips.
	 */
	public static final boolean LEFT_HIT_CLIPS = true;
	public static final String HIT = "hit", HIT_LEFT = "hit_left";

	/** The side of a bot a hit lands on. */
	public enum Side {
		LEFT, RIGHT
	}

	/** The side the last hit it reacted to came from (null: none yet). */
	@Nullable
	private Side lastStruck;

	/**
	 * The side of a part at {@code at} facing {@code forward} (with {@code up} its up) that a hit from {@code from} lands
	 * on: its right is forward x up (yaw 0, facing south: west). Dead ahead or behind counts as its right.
	 */
	public static Side struckSide(Vec3 at, Vec3 forward, Vec3 up, Vec3 from) {
		return from.subtract(at).dot(forward.cross(up)) < 0 ? Side.LEFT : Side.RIGHT;
	}

	/** The way a yaw (degrees, Minecraft's) faces, level. */
	public static Vec3 facing(float yaw) {
		float r = yaw * Mth.DEG_TO_RAD;
		return new Vec3(-Mth.sin(r), 0, Mth.cos(r));
	}

	/** The hit clip for a side: the left-side one only once those clips are in ({@code leftClips}); otherwise the one there is. */
	public static String hitClip(Side side, boolean leftClips) {
		return leftClips && side == Side.LEFT ? HIT_LEFT : HIT;
	}

	/** Where a hit came from: the projectile, or whoever struck it; null if neither (it counts as its right). */
	@Nullable
	public static Vec3 hitFrom(DamageSource source) {
		Vec3 p = source.getSourcePosition();
		if (p == null && source.getEntity() != null)
			p = source.getEntity().position();
		return p;
	}

	/**
	 * The hit clip for {@code source} landing on its part at {@code at}, facing {@code forward} (up {@code up}): the side it
	 * struck is noted ({@link #lastStruck}), and the clip follows {@link #LEFT_HIT_CLIPS}.
	 */
	protected String hitClip(DamageSource source, Vec3 at, Vec3 forward, Vec3 up) {
		Vec3 from = hitFrom(source);
		lastStruck = from == null ? Side.RIGHT : struckSide(at, forward, up, from);
		return hitClip(lastStruck, LEFT_HIT_CLIPS);
	}

	/** The side the last hit it recoiled from came from (game tests), or null. */
	@Nullable
	public Side lastStruck() {
		return lastStruck;
	}

	/** Recoiling from a hit, either side's clip. */
	protected boolean playingHit() {
		return playing(HIT) || playing(HIT_LEFT);
	}

	/** A real player, their projectile or their pet: what can hurt the Custodian (D35, P38). */
	public static boolean fromPlayer(DamageSource source) {
		Entity attacker = source.getEntity();
		if (attacker instanceof Player p)
			return !(p instanceof FakePlayer);
		return attacker instanceof OwnableEntity pet && pet.getOwner() instanceof Player owner && !(owner instanceof FakePlayer);
	}

	// ======================================================================== never despawns, never griefs

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	/** P22: on Peaceful they stay, asleep (a fixed set, D22). */
	@Override
	public boolean shouldDespawnInPeaceful() {
		return false;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	public PushReaction getPistonPushReaction() {
		return PushReaction.IGNORE;
	}

	/** P37: they never change a block (no trampled farmland or turtle eggs either). */
	public static void onMobGriefing(EntityMobGriefingEvent event) {
		if (event.getEntity() instanceof CityBot)
			event.setCanGrief(false);
	}

	// ======================================================================== saving

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		link.write(tag);
		tag.putBoolean("Dormant", dormant());
		tag.putLong("WakeAt", wakeAt);
		tag.putBoolean("PeaceSlept", peaceSlept);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		link.read(tag);
		caughtUp = false;
		if (tag.getBoolean("Dormant"))
			makeDormant();
		wakeAt = tag.contains("WakeAt") ? tag.getLong("WakeAt") : -1;
		peaceSlept = tag.getBoolean("PeaceSlept");
	}
}
