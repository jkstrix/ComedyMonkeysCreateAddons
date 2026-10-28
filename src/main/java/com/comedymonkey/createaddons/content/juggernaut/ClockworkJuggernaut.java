package com.comedymonkey.createaddons.content.juggernaut;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipClock;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipLibrary;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipPlayback;
import com.comedymonkey.createaddons.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import org.joml.Vector3f;

/**
 * The Clockwork Juggernaut (design/specs/clockwork-juggernaut.md; look and motion in design/briefs/clockwork-juggernaut.md).
 * A 7-block war automaton that fights in a rhythm: wound, he's armoured and attacks in combos; his spring runs down, he
 * stops to rewind with the key on his back open, and that's when he's hurt. Every move is a clip from the Blockbench
 * preview (exported by design/models/clockwork-juggernaut.game-export.js), and its damage lands at the clip's own
 * impact times, so what you see is what hits you. He never breaks or changes a block (D2).
 * <p>
 * The server picks the clip and when it started; clients pose him from it and play its sound cues, as for Trade Bot.
 */
public class ClockworkJuggernaut extends Monster {
	public static final String CLIPS = "/assets/comedymonkeysaddons/clockwork_juggernaut/clips.json";

	/** Where he is in his fight (synced, so clients can show it). */
	public enum Phase {
		/** Kneeling asleep (a ruin, or left alone): mends; a player coming close stirs him (D29, D32). */
		DORMANT,
		/** The 5 s warning: his slit lights and the ticking starts; back off beyond 32 blocks and he settles (D32). */
		STIRRING,
		/** Getting up (or unfolding from the effigy); untouchable. */
		WAKING,
		/** Armoured and fighting. */
		WOUND,
		/** A fake run-down that ends in a lunge (D47). */
		FEINT,
		/** His spring giving out: four slowing ticks. Still armoured. */
		RUN_DOWN,
		/** Standing still, key spinning: takes full damage (D3, D14). */
		REWIND,
		/** Straightening up, wound again (or jolted back by lightning, D25). */
		REWIND_END,
		/** Losing a stage's plates (D43); untouchable. */
		SHEDDING,
		/** His last wind-down; the Mainspring bursts out at the end. */
		DYING
	}

	/** His attacks (D21, D44, D45), each a clip with damage at its impact times. */
	enum Move {
		CHARGE("charge"), SWING("swing"), STOMP("stomp"), STOMP_DOUBLE("stomp_double"), GRAB("grab_throw"), GRAB_BLOCKED("grab_blocked"),
		LEAP("leap"), VENT("steam_vent"), SPIN_START("gear_spin_start"), SPIN("gear_spin"), SPIN_END("gear_spin_end"), COG("cog_throw");

		final String clip;

		Move(String clip) {
			this.clip = clip;
		}
	}

	private static final EntityDataAccessor<Integer> CLIP = SynchedEntityData.defineId(ClockworkJuggernaut.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> CLIP_START = SynchedEntityData.defineId(ClockworkJuggernaut.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> CLIP_SPEED = SynchedEntityData.defineId(ClockworkJuggernaut.class, EntityDataSerializers.FLOAT);
	/** Ticks into the clip it was started at (the shield clang starts part-way in). */
	private static final EntityDataAccessor<Integer> CLIP_OFFSET = SynchedEntityData.defineId(ClockworkJuggernaut.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Byte> PHASE = SynchedEntityData.defineId(ClockworkJuggernaut.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Byte> STAGE = SynchedEntityData.defineId(ClockworkJuggernaut.class, EntityDataSerializers.BYTE);

	// ---- tuning (spec numbers; the rest in the server config's clockworkJuggernaut section)
	/** D46: centre distances for close, middle and far moves, and the most a move picks a target from. */
	static final double CLOSE = 6, FAR = 14, AGGRO = 48, LOSE = 64;
	/** Running down is the last 4 s of each wind (D43); a feint's real run-down follows 5 s later (D47). */
	static final int RUN_DOWN_TICKS = 80, FEINT_DELAY = 100;
	/** D48: three hits on the key from behind in one rewind jam it, for 3 s more, once per rewind. */
	static final int JAM_HITS = 3, JAM_TICKS = 60;
	static final float KEY_BONUS = 1.5f;
	/** D29: alone for 60 s, he sits down dormant and mends. */
	static final int ALONE_TICKS = 1200;
	static final float MEND_PER_SECOND = 2;
	/** The heartbeat: one tick every 0.75 s while he's wound, a little quicker in stage 3. */
	static final int HEARTBEAT = 15, HEARTBEAT_STAGE_3 = 13;
	/** Stage 3 plays everything a little faster (brief: motion feel). */
	static final float STAGE_3_SPEED = 1.15f;
	/** D45 / D21 damage (vanilla difficulty scaling applies on top, as for any mob). */
	static final float CHARGE_DAMAGE = 34, SWING_DAMAGE = 22, STOMP_DAMAGE = 14, GRAB_DAMAGE = 17, LEAP_DAMAGE = 12, VENT_DAMAGE = 10,
		SPIN_DAMAGE = 12, LUNGE_DAMAGE = 22, COG_DAMAGE = 19;
	/** He killed a player and nobody's left near: he sleeps this long, untouchable, so they can fetch their things. */
	static final int WIPE_SLEEP_TICKS = 3600, WIPE_DELAY = 40, KILL_MEMORY = 1200;
	/** Overdrive (stage 3, at 20% health): no more winding down, every hit lands in full, and he's quicker. */
	static final float OVERDRIVE_SHARE = 0.2f, OVERDRIVE_SPEED = 1.3f;
	/** How far ahead of a running target he aims his leap, at most. */
	static final double LEAD_MAX = 8;
	/** The leap clip's airborne stretch (seconds of clip), and how long he actually hangs in the air for it. */
	static final float LEAP_AIR = 0.58f, LEAP_FLIGHT = 1.3f;
	/** How long a leap's landing holds them still. */
	static final int FREEZE_TICKS = 25;
	/** The freeze is Slowness at this level (speed to nothing); the client knows it by it and keeps the view from zooming. */
	public static final int FREEZE_AMPLIFIER = 6;
	/** He can't find a way to his target for this long: he throws a cog instead (D22). */
	static final int STUCK_TICKS = 100;
	/** Circled up close this long without getting to face them: he stomps the ring round him instead. */
	static final int CIRCLE_TICKS = 30;
	/** Degrees a tick he turns to face his target before a move. */
	static final float TURN = 9;

	private final ServerBossEvent bossBar = new ServerBossEvent(Component.translatable("entity.comedymonkeysaddons.clockwork_juggernaut"),
		BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);
	/** Client: what's playing, the cross-fade, and the cues already played. */
	public final ClipPlayback playback = new ClipPlayback();
	private int seenClip = -1, seenClipStart;
	private double localClipStart;

	// ---- server state (saved)
	private int phaseTicks, woundTicks, rewindTicks, aloneTicks, keyHits, graceTicks;
	private boolean feinted, jammed, scaled, overdrive;
	/** Extra players when he woke (D14): every hit is divided by this. */
	private float toughness = 1;
	/** When he last killed a player (not saved). */
	private int lastKill = -KILL_MEMORY, wipePending, trackedId = -1;
	/** Players he's killed this fight (D58): respawned, they don't keep him awake until they hit him again. */
	private final Set<UUID> fallen = new HashSet<>();
	/** His target's ground speed, blocks a tick, smoothed: where they're running, for the leap. */
	private Vec3 targetVelocity = Vec3.ZERO;
	@Nullable
	private Vec3 lastTargetPos;
	// ---- the move in progress (not saved: a reload finishes it early)
	@Nullable
	private Move move, lastMove;
	/** The move he's turning to make, chosen once (not re-rolled every tick he turns), and the distance band it was for. */
	@Nullable
	private Move planned;
	private int plannedBand, movesStarted;
	private float moveTime, prevMoveTime;
	private int comboLeft, restTicks, stuckTicks, circling, heartbeat, mannerismTicks;
	private boolean tock, charging;
	@Nullable
	private LivingEntity grabbed;
	private Vec3 leapFrom = Vec3.ZERO, leapTo = Vec3.ZERO;
	private boolean leaping;
	private final Set<UUID> hitThisMove = new HashSet<>();

	public ClockworkJuggernaut(EntityType<? extends ClockworkJuggernaut> type, Level level) {
		super(type, level);
		xpReward = 500; // ten times the Wither's: the dragon's repeat-kill reward (tuned in play, 2026-10-28)
		moveControl = new MoveControl(this) {
			@Override
			public void tick() {
				// he steers himself (see walkToward); the vanilla control would stop him every tick
			}
		};
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 1000)
			.add(Attributes.MOVEMENT_SPEED, 0.055)
			.add(Attributes.ATTACK_DAMAGE, SWING_DAMAGE)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1)
			.add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 1) // nobody blasts him off a cliff (D56)
			.add(Attributes.FOLLOW_RANGE, LOSE)
			.add(Attributes.STEP_HEIGHT, 2);
	}

	public static ClipLibrary clips() {
		return ClipLibrary.of(CLIPS);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CLIP, 0);
		builder.define(CLIP_START, 0);
		builder.define(CLIP_SPEED, 1f);
		builder.define(CLIP_OFFSET, 0);
		builder.define(PHASE, (byte) Phase.WOUND.ordinal());
		builder.define(STAGE, (byte) 1);
	}

	// ======================================================================== state

	public Phase phase() {
		return Phase.values()[Mth.clamp(entityData.get(PHASE), 0, Phase.values().length - 1)];
	}

	public int stage() {
		return entityData.get(STAGE);
	}

	/** Open to full damage: rewinding, with the key exposed (D3). */
	public boolean isOpen() {
		return phase() == Phase.REWIND;
	}

	private void setPhase(Phase phase) {
		entityData.set(PHASE, (byte) phase.ordinal());
		phaseTicks = 0;
	}

	private float speed() {
		return overdrive ? OVERDRIVE_SPEED : stage() >= 3 ? STAGE_3_SPEED : 1;
	}

	/** Under 20% in his last stage: fighting for his life (no winding down, full damage taken). */
	public boolean overdrive() {
		return overdrive;
	}

	/** Asleep after a wipe, untouchable, while the dead fetch their things. */
	public boolean resting() {
		return phase() == Phase.DORMANT && graceTicks > 0;
	}

	static double woundDamage(int stage) {
		return CMConfig.Server.perStage(CMConfig.SERVER.juggernautWoundDamage, stage);
	}

	private int woundLength() {
		return (int) (CMConfig.Server.perStage(CMConfig.SERVER.juggernautWoundSeconds, stage()) * 20);
	}

	private int rewindLength() {
		return (int) (CMConfig.Server.perStage(CMConfig.SERVER.juggernautRewindSeconds, stage()) * 20) + (jammed ? JAM_TICKS : 0);
	}

	/** The health his current stage can't drop below until its plates are shed (the last stage: 1, his death). */
	public float stageFloor() {
		return switch (stage()) {
			case 1 -> getMaxHealth() * 2 / 3f;
			case 2 -> getMaxHealth() / 3f;
			default -> 1;
		};
	}

	// ======================================================================== clips

	public void playClip(String name) {
		playClip(name, phase() == Phase.DYING ? 1 : speed(), 0);
	}

	/** Starts a clip now, {@code offset} seconds in (server). */
	public void playClip(String name, float speed, float offset) {
		Clip clip = clips().clip(name);
		int lead = Mth.floor(offset * 20 / speed);
		entityData.set(CLIP, clips().id(clip));
		entityData.set(CLIP_START, (int) (level().getGameTime() - lead));
		entityData.set(CLIP_SPEED, speed);
		entityData.set(CLIP_OFFSET, lead);
	}

	public Clip clip() {
		Clip clip = clips().byId(entityData.get(CLIP));
		return clip != null ? clip : clips().clip("idle");
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

	/** The time his clips play at, in ticks: on clients the steady {@link ClipClock}, as Trade Bot's do. */
	public double clipNow(float partialTick) {
		return level().isClientSide ? ClipClock.now(partialTick) : level().getGameTime() + partialTick;
	}

	/** When the current clip started, on the {@link #clipNow} clock. */
	public double clipStartTime() {
		int id = entityData.get(CLIP), start = entityData.get(CLIP_START);
		if (!level().isClientSide)
			return level().getGameTime() - (int) (level().getGameTime() - start);
		if (id != seenClip || start != seenClipStart) {
			// how far into it the server already is when we first see him; a clip that changes while we watch starts now
			// (at the offset it was started at), as the client's world time drifts ahead of a lagging server
			int lead = seenClip == -1 ? Math.max(0, (int) (level().getGameTime() - start)) : entityData.get(CLIP_OFFSET);
			seenClip = id;
			seenClipStart = start;
			localClipStart = ClipClock.now(0) - lead;
		}
		return localClipStart;
	}

	private boolean clipDone() {
		return clip().finished(clipElapsed());
	}

	// ======================================================================== ticking

	@Override
	public void tick() {
		super.tick();
		yBodyRot = getYRot();
		yHeadRot = getYRot();
		if (level().isClientSide) {
			playback.observe(clip(), clipStartTime(), clipSpeed(), clipNow(0));
			playback.tickCues(clipNow(0), this::playCue);
		}
	}

	/** Client: shakes the camera for a player near a landing (set by the client setup; the server never calls it). */
	public static Consumer<Vec3> landShake = at -> {};

	private void playCue(String event) {
		if (event.equals("leap_land"))
			landShake.accept(position());
		SoundEvent sound = ModSounds.juggernaut(event);
		if (sound != null)
			level().playLocalSound(getX(), getY() + 3, getZ(), sound, SoundSource.HOSTILE, 1, 1, false);
	}

	/** A sound everyone near hears, from the server (the heartbeat, glancing hits). */
	void sound(String id, float pitch) {
		SoundEvent sound = ModSounds.juggernaut(id);
		if (sound != null)
			level().playSound(null, getX(), getY() + 3, getZ(), sound, SoundSource.HOSTILE, 1, pitch);
	}

	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		phaseTicks++;
		updateBossBar();
		if (level().getDifficulty() == Difficulty.PEACEFUL && phase() != Phase.DORMANT && phase() != Phase.DYING) {
			goDormant(); // D19: never fights in peaceful
			return;
		}
		if (overdrive && phase() != Phase.DYING)
			overdriveSteam();
		// a beat after the wipe (the death screen), he kneels; mid-shed or mid-wake he finishes that first
		if (wipePending > 0 && --wipePending == 0) {
			Phase p = phase();
			if (anyoneStanding()) {
				// someone came in (or back) and fought on in the meantime: no rest
			} else if (p == Phase.SHEDDING || p == Phase.WAKING)
				wipePending = 1;
			else if (p != Phase.DORMANT && p != Phase.DYING) {
				sleepAfterWipe();
				return;
			}
		}
		switch (phase()) {
			case DORMANT -> tickDormant();
			case STIRRING -> tickStirring();
			case WAKING -> {
				heartbeat();
				if (clipDone())
					startWound();
			}
			case WOUND -> tickWound();
			case FEINT -> tickFeint();
			case RUN_DOWN -> {
				stopWalking();
				if (clipDone()) {
					setPhase(Phase.REWIND);
					rewindTicks = 0;
					playClip("rewind");
				}
			}
			case REWIND -> tickRewind();
			case REWIND_END -> {
				stopWalking();
				if (clipDone())
					startWound();
			}
			case SHEDDING -> {
				stopWalking();
				if (clipDone()) {
					entityData.set(STAGE, (byte) (stage() + 1));
					startWound();
				}
			}
			case DYING -> tickDying();
		}
	}

	private void startWound() {
		setPhase(Phase.WOUND);
		woundTicks = 0;
		feinted = false;
		jammed = false;
		keyHits = 0;
		endMove();
		comboLeft = 0;
		restTicks = 20;
		playClip("idle");
	}

	// ---- dormant, stirring, waking

	private void goDormant() {
		endMove();
		stopWalking();
		setPhase(Phase.DORMANT);
		setTarget(null);
		scaled = false; // whoever wakes him next sets his health (D14)
		overdrive = false; // he mends out of it; a hit under 20% starts it again
		graceTicks = 0;
		wipePending = 0;
		fallen.clear(); // the fight's over
		playClip("dormant");
	}

	/** Everyone he fought is dead or gone: he kneels, untouchable, long enough for them to come back for their things. */
	private void sleepAfterWipe() {
		goDormant();
		graceTicks = WIPE_SLEEP_TICKS;
		sound("glow_out", 1);
	}

	private void tickDormant() {
		stopWalking();
		// he mends, but plates he shed stay shed: only up to the top of his stage
		float ceiling = getMaxHealth() * (4 - stage()) / 3f;
		if (tickCount % 10 == 0 && getHealth() < ceiling)
			setHealth(Math.min(ceiling, getHealth() + MEND_PER_SECOND / 2));
		if (graceTicks > 0) {
			graceTicks--; // sleeping through it: they can walk right up to fetch their things
			return;
		}
		if (level().getDifficulty() == Difficulty.PEACEFUL || tickCount % 10 != 0)
			return;
		Player near = nearestPlayer(16, true);
		if (near != null) {
			setPhase(Phase.STIRRING);
			setTarget(near);
			sound("glow_on", 1);
		}
	}

	private void tickStirring() {
		stopWalking();
		heartbeat();
		if (tickCount % 10 == 0 && nearestPlayer(32, false) == null) {
			setPhase(Phase.DORMANT); // backed off: he settles again
			return;
		}
		if (phaseTicks >= 100)
			beginWaking("wake");
	}

	/**
	 * D14: +50% health for each extra player. Vanilla caps max health at 1024, so the extra comes as toughness instead:
	 * every hit is divided by it, which is the same fight as that much more health. The boss bar reads the same.
	 */
	public void scaleForPlayers(int players) {
		toughness = (float) (1 + CMConfig.Server.get(CMConfig.SERVER.juggernautExtraPlayerHealth) * Math.max(0, players - 1));
		float share = getHealth() / getMaxHealth(); // a summoned one is whole; a mending one keeps his wounds
		getAttribute(Attributes.MAX_HEALTH).setBaseValue(CMConfig.Server.get(CMConfig.SERVER.juggernautHealth));
		setHealth(getMaxHealth() * share);
	}

	/** Gets up (a dormant one) or unfolds (summoned); his health scales with the players around him then (D14). */
	void beginWaking(String clip) {
		graceTicks = 0; // a hit ends his rest after a wipe early: they're ready
		endMove();
		stopWalking();
		if (!scaled) {
			scaled = true;
			int players = level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(32), ClockworkJuggernaut::fightable).size();
			scaleForPlayers(players);
		}
		setPhase(Phase.WAKING);
		playClip(clip);
	}

	// ---- wound: walking, combos, moves

	private void tickWound() {
		heartbeat();
		LivingEntity target = updateTarget();
		trackTarget(target);
		// his spring only runs down in a fight: alone, he stands wound
		if (target != null || move != null)
			woundTicks++;
		if (move != null) {
			tickMove(target);
			return;
		}
		// the spring gives out (D43), unless a feint comes first (D47); in overdrive it never does
		if (!overdrive && woundTicks >= woundLength() - RUN_DOWN_TICKS) {
			stopWalking();
			double feintChance = stage() >= 3 ? 0.4 : stage() == 2 ? 0.25 : 0;
			if (!feinted && random.nextDouble() < feintChance) {
				feinted = true;
				setPhase(Phase.FEINT);
				playClip("feint");
				hitThisMove.clear();
			} else {
				setPhase(Phase.RUN_DOWN);
				playClip("run_down", 1, 0);
			}
			return;
		}
		if (target == null) {
			stopWalking();
			// he killed someone and nobody's left in reach: he sleeps it off, untouchable, while they fetch their things
			boolean wiped = tickCount - lastKill < KILL_MEMORY;
			aloneTicks = nearestPlayer(wiped ? AGGRO : LOSE, false) == null ? aloneTicks + 1 : 0;
			if (wiped && aloneTicks >= WIPE_DELAY) {
				aloneTicks = 0;
				sleepAfterWipe();
				return;
			}
			if (aloneTicks >= ALONE_TICKS) {
				aloneTicks = 0;
				goDormant();
				return;
			}
			idleMannerisms();
			return;
		}
		aloneTicks = 0;
		double d = horizontalDistance(target);
		boolean reachable = canReach(target);
		stuckTicks = reachable ? 0 : stuckTicks + 1;
		if (restTicks > 0) {
			restTicks--;
			approach(target, d);
			return;
		}
		if (comboLeft <= 0)
			comboLeft = switch (stage()) {
				case 1 -> 2;
				case 2 -> 2 + random.nextInt(2);
				default -> 3;
			};
		// the move is chosen once and kept while he turns to it (re-rolling every tick would favour the moves that need no
		// facing); a new one only if they change distance band
		int band = d < CLOSE ? 0 : d < FAR ? 1 : 2;
		if (stuckTicks >= STUCK_TICKS)
			planned = Move.COG;
		else if (planned == null || planned == Move.COG || band != plannedBand) {
			planned = pick(target, d);
			plannedBand = band;
		}
		Move next = planned;
		if (next == null) {
			approach(target, d);
			return;
		}
		// close and circling him, so he can't come round to face them: the stomp ring needs no facing; if he just stomped,
		// he whirls round twice as fast instead (never the same move twice, D46)
		boolean circled = d < CLOSE + 2 && circling >= CIRCLE_TICKS;
		if (circled && lastMove != Move.STOMP)
			next = Move.STOMP;
		// face the target first, except for what reaches all round him (the stomp, the spin) and the cog
		// (the leap turns him through its coil)
		boolean allRound = next == Move.STOMP || next == Move.SPIN_START || next == Move.COG || next == Move.LEAP;
		if (!turnToward(target.position(), circled ? TURN * 2 : TURN) && !allRound) {
			circling = d < CLOSE + 2 ? circling + 1 : 0;
			stopWalking();
			playIfNot("idle");
			return;
		}
		startMove(next, target);
	}

	/** D46: a move for the distance, never the same twice in a row, from this stage's set (D44). */
	@Nullable
	private Move pick(LivingEntity target, double d) {
		List<Move> options = new ArrayList<>();
		int stage = stage();
		if (d < CLOSE) {
			options.add(Move.SWING);
			options.add(Move.STOMP);
			if (inFront(target, 50))
				options.add(Move.GRAB);
			if (stage >= 3)
				options.add(Move.SPIN_START);
		} else if (d < FAR) {
			options.add(Move.CHARGE);
			options.add(Move.STOMP);
			if (stage >= 2)
				options.add(Move.VENT);
			// strafing out of reach of a charge: he jumps on them instead
			if (d >= 10 && strafing(target) && leapLanding(target, 1.5f) != null)
				options.add(Move.LEAP);
		} else {
			options.add(Move.CHARGE);
			// far off, he'd rather come down on them: twice the chance of a charge
			if (leapLanding(target, 1.5f) != null) {
				options.add(Move.LEAP);
				options.add(Move.LEAP);
			}
		}
		// a stomp only reaches 6 blocks: in the middle band it's for when they come in
		if (d >= CLOSE + 2)
			options.remove(Move.STOMP);
		options.removeIf(m -> m == lastMove);
		if (lastMove == Move.SPIN_END)
			options.remove(Move.SPIN_START);
		if (options.isEmpty())
			return null;
		return options.get(random.nextInt(options.size()));
	}

	private void startMove(Move next, LivingEntity target) {
		stopWalking();
		if (next == Move.STOMP && stage() >= 3)
			next = Move.STOMP_DOUBLE;
		move = next;
		moveTime = prevMoveTime = 0;
		circling = 0;
		planned = null;
		movesStarted++;
		hitThisMove.clear();
		charging = false;
		leaping = false;
		grabbed = null;
		playClip(next.clip);
	}

	/** The next part of a move that comes in parts (the gear spin), or the end of the combo's move. */
	private void endMove() {
		grabbed = null;
		charging = false;
		if (leaping) {
			// cut off mid-air (overdrive, a wipe, peaceful): he comes down where he was going, a spot known to be clear,
			// never somewhere along the arc (which may be inside a hill)
			setPos(leapTo.x, leapTo.y, leapTo.z);
			noPhysics = false;
			setNoGravity(false);
			leaping = false;
		}
		if (move != null) {
			lastMove = move == Move.STOMP_DOUBLE ? Move.STOMP : move == Move.GRAB_BLOCKED ? Move.GRAB : move;
			comboLeft--;
			// relentless: a breath between moves, a short one between combos (shorter still in overdrive)
			restTicks = overdrive ? (comboLeft > 0 ? 4 : 8 + random.nextInt(10)) : comboLeft > 0 ? 6 : 16 + random.nextInt(20);
			// after a leap, a frozen player gets a moment of their legs back before the next move
			if (lastMove == Move.LEAP)
				restTicks = Math.max(restTicks, FREEZE_TICKS);
		}
		planned = null;
		move = null;
	}

	/** Did the move's clock just pass {@code time} (seconds into its clip)? */
	private boolean at(float time) {
		return prevMoveTime < time && moveTime >= time;
	}

	private void tickMove(@Nullable LivingEntity target) {
		prevMoveTime = moveTime;
		moveTime += clipSpeed() / 20f;
		float t = moveTime;
		Vec3 fwd = forward();
		switch (move) {
			case CHARGE -> {
				if (t < 0.55f && target != null)
					turnToward(target.position(), 4);
				// the run: from the lunge until the skid, unless a ledge or a wall stops him first (D56)
				boolean run = t >= 0.55f && t < 1.7f;
				if (run && t < 0.6f)
					charging = true;
				if (!run || !safeAhead(fwd, 1.5))
					charging = false;
				if (charging) {
					setDeltaMovement(fwd.x * 0.55, getDeltaMovement().y, fwd.z * 0.55);
					for (LivingEntity victim : victims(getBoundingBox().inflate(0.5, 0, 0.5).move(fwd.scale(1.5))))
						hit(victim, CHARGE_DAMAGE, fwd.scale(2.0).add(0, 0.6, 0));
				} else
					stopWalking();
			}
			case SWING -> {
				// the arm follows them through the wind-up: a late sidestep doesn't dodge it
				if (t < 0.4f && target != null)
					turnToward(target.position(), 5);
				if (at(0.5f))
					for (LivingEntity victim : around(6.5, 80))
						hit(victim, SWING_DAMAGE, away(victim).scale(1.4).add(0, 0.4, 0));
			}
			case STOMP, STOMP_DOUBLE -> {
				if (at(0.73f))
					stomp(6, STOMP_DAMAGE);
				if (move == Move.STOMP_DOUBLE && at(1.27f))
					stomp(6, STOMP_DAMAGE);
			}
			case GRAB -> tickGrab(target, t, fwd);
			case GRAB_BLOCKED -> {
			}
			case LEAP -> tickLeap(target, t);
			case VENT -> tickVent(t, fwd);
			case SPIN_START -> {
				if (t < 1.2f && target != null)
					turnToward(target.position(), 3);
				if (clipDone()) {
					move = Move.SPIN;
					moveTime = prevMoveTime = 0;
					playClip("gear_spin");
					return;
				}
			}
			case SPIN -> {
				tickSpin(target);
				if (moveTime >= 4) {
					move = Move.SPIN_END;
					moveTime = prevMoveTime = 0;
					stopWalking();
					playClip("gear_spin_end");
				}
				return;
			}
			case SPIN_END -> stopWalking();
			case COG -> {
				if (t < 0.8f && target != null)
					turnToward(target.position(), 6);
				if (at(0.87f) && target != null)
					ThrownCog.throwAt(this, target);
			}
		}
		if (move != null && move != Move.SPIN && clipDone())
			endMove();
	}

	private void tickGrab(@Nullable LivingEntity target, float t, Vec3 fwd) {
		if (t < 1.0f && target != null)
			turnToward(target.position(), 4);
		if (at(1.15f) && target != null && horizontalDistance(target) < 7.5 && inFront(target, 55)) {
			// a raised shield turns the grab into a clang on nothing (D45)
			if (target.isBlocking() && target.isDamageSourceBlocked(damageSources().mobAttack(this))) {
				move = Move.GRAB_BLOCKED;
				playClip("grab_blocked", speed(), 0.5f);
				target.push(fwd.x * 0.6, 0.2, fwd.z * 0.6);
				target.hurtMarked = true;
				return;
			}
			grabbed = target;
			grabbed.stopRiding(); // plucked out of the boat or the saddle
		}
		if (grabbed != null && t >= 1.2f && t < 1.95f) {
			if (!grabbed.isAlive()) {
				grabbed = null;
				return;
			}
			// held in the claw, then lifted overhead for the throw
			double lift = 4.5 + 2.5 * Mth.clamp((t - 1.3f) / 0.45f, 0, 1);
			double reach = 3.2 - 2.0 * Mth.clamp((t - 1.3f) / 0.45f, 0, 1);
			Vec3 claw = position().add(fwd.scale(reach)).add(0, lift - grabbed.getBbHeight() / 2, 0);
			grabbed.teleportTo(claw.x, claw.y, claw.z);
			grabbed.setDeltaMovement(Vec3.ZERO);
			grabbed.fallDistance = 0;
		}
		if (grabbed != null && at(1.95f)) {
			LivingEntity thrown = grabbed;
			grabbed = null;
			thrown.hurt(damageSources().mobAttack(this), GRAB_DAMAGE);
			thrown.setDeltaMovement(fwd.x * 1.9, 0.7, fwd.z * 1.9);
			thrown.hurtMarked = true;
		}
	}

	private void tickLeap(@Nullable LivingEntity target, float t) {
		// he aims where they're running to, and marks it (frost-blue ring on the ground) so it can be dodged
		if (t < 1.5f && target != null) {
			Vec3 aim = leapLanding(target, (1.5f - t) / clipSpeed() + LEAP_FLIGHT);
			turnToward(aim != null ? aim : target.position(), 6);
			if (aim != null && t > 0.3f && tickCount % 2 == 0)
				markLanding(aim);
		}
		if (leaping && tickCount % 2 == 0)
			markLanding(leapTo);
		if (at(1.5f)) {
			Vec3 landing = target == null ? null : leapLanding(target, LEAP_FLIGHT);
			if (landing == null) {
				// nowhere to come down (they ran under cover, or died): he straightens up instead, no landing
				endMove();
				playClip("idle");
				return;
			}
			leapFrom = position();
			leapTo = landing;
			// squared up to where he'll come down as he leaves the ground: in the air he can't turn, and he flies the way
			// he faces (not off at an angle to where the coil had him aimed)
			turnToward(landing, 360);
			leaping = true;
			noPhysics = true;
			setNoGravity(true);
			// a long hang in the air, so the landing can be dodged: the clip's airborne stretch plays slowly (and so does
			// the move's clock, which follows the clip), so he still touches down on its landing frame and sound
			playClip("leap", LEAP_AIR / LEAP_FLIGHT, 1.52f);
		}
		if (leaping) {
			float u = Mth.clamp((t - 1.55f) / 0.53f, 0, 1);
			Vec3 at = leapFrom.lerp(leapTo, u).add(0, 6 * Mth.sin(u * Mth.PI), 0);
			setDeltaMovement(Vec3.ZERO);
			setPos(at.x, at.y, at.z);
			if (u >= 1) {
				leaping = false;
				noPhysics = false;
				setNoGravity(false);
				setPos(leapTo.x, leapTo.y, leapTo.z);
				playClip("leap", speed(), 2.1f); // back to his own pace for the recovery
				stomp(5, LEAP_DAMAGE);
				freezeAround(5); // the ground shakes (client, on the leap_land cue) and they're held still
			}
		}
	}

	/**
	 * Where a spring leap at {@code target} lands, {@code seconds} before he comes down: where they'll be by then
	 * if they keep running (up to 8 blocks on), or else where they stand; null if neither has ground and room for him
	 * (D45, D56).
	 */
	@Nullable
	Vec3 leapLanding(LivingEntity target, float seconds) {
		Vec3 lead = targetVelocity.scale(Math.max(0, seconds) * 20);
		if (lead.length() > LEAD_MAX)
			lead = lead.normalize().scale(LEAD_MAX);
		Vec3 ahead = leapLanding(target.position().add(lead));
		return ahead != null ? ahead : leapLanding(target.position());
	}

	@Nullable
	private Vec3 leapLanding(Vec3 spot) {
		BlockPos feet = BlockPos.containing(spot.x, spot.y + 0.5, spot.z);
		for (int dy = 0; dy >= -4; dy--) {
			BlockPos below = feet.offset(0, dy - 1, 0);
			if (!level().getBlockState(below).getCollisionShape(level(), below).isEmpty()) {
				Vec3 land = new Vec3(spot.x, below.getY() + 1, spot.z);
				AABB box = getDimensions(getPose()).makeBoundingBox(land);
				// room to stand, ground under all of him (not a pillar top), no higher or lower than he could walk
				// back from (D56), and a clear arc there: he never leaps through a hill or a cave roof
				return level().noCollision(this, box) && Math.abs(land.y - getY()) <= 3 && footing(land) && arcClear(position(), land)
					? land : null;
			}
		}
		return null;
	}

	/** Ground within 3 blocks under each corner of his footprint at {@code land}. */
	private boolean footing(Vec3 land) {
		double r = getBbWidth() / 2 - 0.5;
		for (double[] c : new double[][] { { -r, -r }, { r, -r }, { -r, r }, { r, r } }) {
			BlockPos base = BlockPos.containing(land.x + c[0], land.y + 0.5, land.z + c[1]);
			boolean ground = false;
			for (int dy = -1; dy >= -3 && !ground; dy--)
				ground = solid(base.offset(0, dy, 0));
			if (!ground)
				return false;
		}
		return true;
	}

	/** Nothing solid anywhere along the leap's arc (as tickLeap flies it) for a body his size. */
	private boolean arcClear(Vec3 from, Vec3 to) {
		for (int i = 1; i < 10; i++) {
			float u = i / 10f;
			Vec3 at = from.lerp(to, u).add(0, 6 * Mth.sin(u * Mth.PI), 0);
			if (!level().noCollision(this, getDimensions(getPose()).makeBoundingBox(at).deflate(0.3)))
				return false;
		}
		return true;
	}

	/** Follows his target's ground speed (players' own motion isn't known to the server, so from their positions). */
	private void trackTarget(@Nullable LivingEntity target) {
		if (target == null) {
			lastTargetPos = null;
			targetVelocity = Vec3.ZERO;
			return;
		}
		Vec3 pos = target.position();
		if (target.getId() != trackedId) { // a new target: what the last one was doing says nothing
			trackedId = target.getId();
			lastTargetPos = null;
			targetVelocity = Vec3.ZERO;
		}
		if (lastTargetPos != null && pos.distanceToSqr(lastTargetPos) < 4) // a teleport isn't running
			targetVelocity = targetVelocity.scale(0.7).add(pos.subtract(lastTargetPos).multiply(1, 0, 1).scale(0.3));
		lastTargetPos = pos;
	}

	/** Running across his line of sight rather than at or away from him. */
	private boolean strafing(LivingEntity target) {
		Vec3 to = target.position().subtract(position()).multiply(1, 0, 1);
		if (to.lengthSqr() < 1e-4)
			return false;
		Vec3 across = new Vec3(-to.z, 0, to.x).normalize();
		return Math.abs(targetVelocity.dot(across)) > 0.12; // about a walk, sideways
	}

	/** A frost-blue ring on the ground where he'll come down, the size of the landing's stomp. */
	private void markLanding(Vec3 at) {
		ServerLevel server = (ServerLevel) level();
		for (int i = 0; i < 20; i++) {
			double a = i * Mth.TWO_PI / 20;
			server.sendParticles(RUNE_DUST, at.x + Math.cos(a) * 5, at.y + 0.15, at.z + Math.sin(a) * 5, 1, 0.05, 0, 0.05, 0);
		}
		server.sendParticles(RUNE_DUST, at.x, at.y + 0.15, at.z, 3, 0.4, 0, 0.4, 0);
	}

	private static final DustParticleOptions RUNE_DUST = new DustParticleOptions(new Vector3f(0.31f, 0.56f, 0.68f), 1.6f);

	/** Down to his last 20%: no more winding down, every hit lands in full, quicker; steam pours out of him. */
	private void beginOverdrive() {
		overdrive = true;
		endMove();
		comboLeft = 0;
		stopWalking();
		// a jolt, like the lightning's, then straight back in
		setPhase(Phase.REWIND_END);
		playClip("lightning", 1, 0);
		sound("stage_roar", 1.1f);
		ServerLevel server = (ServerLevel) level();
		server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 4, getZ(), 40, 1.5, 1.5, 1.5, 0.15);
	}

	private void overdriveSteam() {
		ServerLevel server = (ServerLevel) level();
		Vec3 fwd = forward();
		if (tickCount % 2 == 0) {
			// from his back (the key's housing), his shoulders and his beard
			Vec3 back = position().subtract(fwd.scale(1.8)).add(0, 4.5, 0);
			server.sendParticles(ParticleTypes.CLOUD, back.x, back.y, back.z, 1, 0.4, 0.3, 0.4, 0.03);
			double side = random.nextBoolean() ? 2.6 : -2.6;
			Vec3 shoulder = position().add(fwd.z * side, 5.6, -fwd.x * side);
			server.sendParticles(ParticleTypes.CLOUD, shoulder.x, shoulder.y, shoulder.z, 1, 0.2, 0.1, 0.2, 0.02);
		}
		if (tickCount % 30 == 0)
			sound(random.nextBoolean() ? "steam_puff" : "steam_wisp", 0.9f + random.nextFloat() * 0.2f);
	}

	/** Called on whoever gets the credit when anything dies, players included (ServerPlayer.die never calls killedEntity). */
	@Override
	public void awardKillScore(Entity killed, int score, DamageSource source) {
		super.awardKillScore(killed, score, source);
		if (killed instanceof Player dead) {
			lastKill = tickCount;
			fallen.add(dead.getUUID());
			// that was the last one standing near him: he sleeps, wherever they respawn (D58)
			if (!anyoneStanding())
				wipePending = WIPE_DELAY;
		}
	}

	/**
	 * Someone still fighting within 48 blocks: a player there who hasn't fallen to him this fight (a respawned one
	 * doesn't count until they hit him again).
	 */
	private boolean anyoneStanding() {
		for (Player p : level().players())
			if (fightable(p) && p.distanceToSqr(this) <= AGGRO * AGGRO && !fallen.contains(p.getUUID()))
				return true;
		return false;
	}

	/** The spring leap's landing: everyone it catches on the ground is frozen still for a moment (frost on the screen). */
	private void freezeAround(double radius) {
		for (LivingEntity victim : victims(getBoundingBox().inflate(radius, 2, radius))) {
			if (horizontalDistance(victim) > radius + getBbWidth() / 2 || !victim.getRootVehicle().onGround() || victim.getY() > getY() + 1.5)
				continue;
			victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FREEZE_TICKS, FREEZE_AMPLIFIER, false, false, true), this);
			victim.setTicksFrozen(Math.max(victim.getTicksFrozen(), victim.getTicksRequiredToFreeze() - 10)); // the frost, not its damage
			victim.setDeltaMovement(0, Math.min(0, victim.getDeltaMovement().y), 0);
			victim.hurtMarked = true;
		}
	}

	private void tickVent(float t, Vec3 fwd) {
		ServerLevel server = (ServerLevel) level();
		Vec3 beard = position().add(fwd.scale(1.6)).add(0, 4.2, 0);
		if (t > 0.3f && t < 1.9f && tickCount % 3 == 0)
			server.sendParticles(ParticleTypes.CLOUD, beard.x, beard.y, beard.z, 1, 0.3, 0.2, 0.3, 0.01);
		if (t >= 2.0f && t < 2.9f) {
			// a 9-block cone of steam from his beard (D45)
			for (int i = 0; i < 6; i++) {
				double r = random.nextDouble() * 9, spread = r * 0.45;
				Vec3 p = beard.add(fwd.scale(r)).add((random.nextDouble() - 0.5) * spread, (random.nextDouble() - 0.7) * spread * 0.6,
					(random.nextDouble() - 0.5) * spread);
				server.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.02);
			}
			for (LivingEntity victim : victims(new AABB(beard, beard).inflate(10))) {
				Vec3 to = victim.getBoundingBox().getCenter().subtract(beard);
				double along = to.dot(fwd);
				if (along < 0 || along > 9 || to.subtract(fwd.scale(along)).length() > 1 + along * 0.45)
					continue;
				if (hit(victim, VENT_DAMAGE, fwd.scale(1.0).add(0, 0.3, 0))) {
					victim.igniteForSeconds(4);
					victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40), this);
				}
			}
		}
	}

	private void tickSpin(@Nullable LivingEntity target) {
		// like a governor: pulls in anyone within 10 blocks, slower than a sprint, and drifts after his target (D45)
		for (LivingEntity victim : victims(getBoundingBox().inflate(10))) {
			Vec3 pull = position().subtract(victim.position()).multiply(1, 0, 1);
			double d = pull.length();
			if (d > 10 || d < 0.1)
				continue;
			victim.push(pull.x / d * 0.06, 0, pull.z / d * 0.06);
			victim.hurtMarked = true;
			if (d < 5.5 && (tickCount + victim.getId()) % 10 == 0) {
				hitThisMove.remove(victim.getUUID());
				hit(victim, SPIN_DAMAGE, pull.scale(-1 / d).add(0, 0.3, 0));
			}
		}
		if (target != null) {
			Vec3 to = target.position().subtract(position()).multiply(1, 0, 1);
			if (to.length() > 1 && safeAhead(to.normalize(), 1))
				setDeltaMovement(to.normalize().scale(0.06).add(0, getDeltaMovement().y, 0));
		}
	}

	private void tickFeint() {
		stopWalking();
		heartbeat();
		float t = clipElapsed();
		// a sharp snap, then a lunge at anyone within 6 blocks in front (D47)
		if (t >= 1.2f && t < 1.3f)
			for (LivingEntity victim : around(7, 70))
				hit(victim, LUNGE_DAMAGE, away(victim).scale(1.2).add(0, 0.3, 0));
		if (clipDone()) {
			setPhase(Phase.WOUND);
			woundTicks = woundLength() - RUN_DOWN_TICKS - FEINT_DELAY;
			restTicks = 10;
			playClip("idle");
		}
	}

	private void tickRewind() {
		stopWalking();
		rewindTicks++;
		// a hit reaction or a jam plays from the rewind pose, then he goes back to spinning the key
		if (!clip().name().equals("rewind") && clipDone())
			playClip("rewind");
		if (rewindTicks >= rewindLength()) {
			setPhase(Phase.REWIND_END);
			playClip("rewind_end");
		}
	}

	private void tickDying() {
		stopWalking();
		if (clipElapsed() >= 3.9f) {
			DamageSource source = getLastDamageSource() != null ? getLastDamageSource() : damageSources().generic();
			setHealth(0);
			die(source);
		}
	}

	@Override
	protected void tickDeath() {
		// his own death clip has already played (DYING); now he's gone, in a puff
		if (!level().isClientSide && !isRemoved()) {
			level().broadcastEntityEvent(this, (byte) 60);
			remove(RemovalReason.KILLED);
		}
	}

	// ---- walking and facing

	private void approach(LivingEntity target, double d) {
		if (d > CLOSE - 1.5 && turnToward(target.position(), 5) && safeAhead(forward(), 1)) {
			walkForward();
			return;
		}
		stopWalking();
		if (d <= CLOSE - 1.5)
			turnToward(target.position(), 5);
		playIfNot("idle");
	}

	private void walkForward() {
		setSpeed((float) getAttributeValue(Attributes.MOVEMENT_SPEED));
		zza = 1;
		if (!clip().name().equals("walk"))
			playClip("walk", speed() * 1.4f, 0);
	}

	private void stopWalking() {
		zza = 0;
		setSpeed(0);
		if (!charging && !leaping)
			setDeltaMovement(0, getDeltaMovement().y, 0);
	}

	private void playIfNot(String clip) {
		if (!clip().name().equals(clip) && (clip().mode() == Clip.Mode.LOOP || clipDone()))
			playClip(clip);
	}

	private void idleMannerisms() {
		if (++mannerismTicks < 200 + random.nextInt(100) || !clip().name().equals("idle")) {
			playIfNot("idle");
			return;
		}
		mannerismTicks = 0;
		playClip(List.of("scan", "knuckles", "sigh").get(random.nextInt(3)));
	}

	/** Turns toward a point at up to {@code degrees} a tick; true once he faces it (within 15°). */
	private boolean turnToward(Vec3 at, float degrees) {
		double dx = at.x - getX(), dz = at.z - getZ();
		if (dx * dx + dz * dz < 1e-4)
			return true;
		float want = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90;
		float diff = Mth.wrapDegrees(want - getYRot());
		setYRot(getYRot() + Mth.clamp(diff, -degrees, degrees));
		yBodyRot = yHeadRot = getYRot();
		return Math.abs(diff) < 15;
	}

	Vec3 forward() {
		float r = getYRot() * Mth.DEG_TO_RAD;
		return new Vec3(-Mth.sin(r), 0, Mth.cos(r));
	}

	/**
	 * Is the ground {@code ahead} blocks past his front edge walkable: no drop deeper than 3 (D56) and no wall taller than
	 * he can step (2)?
	 */
	boolean safeAhead(Vec3 dir, double ahead) {
		double reach = getBbWidth() / 2 + ahead;
		for (double side : new double[] { -2, 0, 2 }) {
			Vec3 p = position().add(dir.scale(reach)).add(-dir.z * side, 0, dir.x * side);
			BlockPos base = BlockPos.containing(p.x, getY() + 0.5, p.z);
			// ground at his feet's level or up to 3 below (a block at foot level is a step up)
			boolean ground = false;
			for (int dy = 0; dy >= -4 && !ground; dy--)
				ground = solid(base.offset(0, dy, 0));
			if (!ground) // water over a drop is still a drop
				return false;
			for (int dy = 2; dy <= 5; dy++)
				if (solid(base.offset(0, dy, 0)))
					return false;
		}
		return true;
	}

	private boolean solid(BlockPos pos) {
		BlockState state = level().getBlockState(pos);
		return !state.getCollisionShape(level(), pos).isEmpty();
	}

	/** Can he walk up to them: a straight walkable line, and not far above or below him? */
	private boolean canReach(LivingEntity target) {
		if (Math.abs(target.getY() - getY()) > 5)
			return false;
		double d = horizontalDistance(target);
		if (d < CLOSE)
			return true;
		Vec3 dir = target.position().subtract(position()).multiply(1, 0, 1).normalize();
		return safeAhead(dir, 1) && safeAhead(dir, Math.min(d - getBbWidth() / 2, 6));
	}

	// ---- hitting

	/**
	 * Living things in a box he can hurt: players not in creative or spectator, their pets (which can hurt him), and
	 * whatever he's targeting.
	 */
	private List<LivingEntity> victims(AABB box) {
		return level().getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e.isAlive() && !(e instanceof ClockworkJuggernaut)
			&& (e instanceof Player p ? fightable(p) : e == getTarget() || e instanceof OwnableEntity pet && pet.getOwner() instanceof Player));
	}

	/** Victims within {@code radius} of his centre and {@code halfAngle} degrees of where he faces. */
	private List<LivingEntity> around(double radius, double halfAngle) {
		List<LivingEntity> out = new ArrayList<>();
		for (LivingEntity e : victims(getBoundingBox().inflate(radius)))
			if (horizontalDistance(e) <= radius && inFront(e, halfAngle) && Math.abs(e.getY() - getY()) < 6)
				out.add(e);
		return out;
	}

	/** The stomp ring (D21, D45): everyone on the ground within the radius is thrown up; jump it to dodge. */
	private void stomp(double radius, float damage) {
		ServerLevel server = (ServerLevel) level();
		BlockState ground = level().getBlockState(blockPosition().below());
		ParticleOptions dust = ground.isAir() ? ParticleTypes.POOF : new BlockParticleOption(ParticleTypes.BLOCK, ground);
		for (int ring = 1; ring <= 3; ring++)
			for (int i = 0; i < 24; i++) {
				double a = i * Mth.TWO_PI / 24, r = radius * ring / 3;
				server.sendParticles(dust, getX() + Math.cos(a) * r, getY() + 0.1, getZ() + Math.sin(a) * r, 2, 0.2, 0.05, 0.2, 0.05);
			}
		for (LivingEntity victim : victims(getBoundingBox().inflate(radius, 2, radius))) {
			// riding doesn't dodge it: what you ride is on the ground
			if (horizontalDistance(victim) > radius + getBbWidth() / 2 || !victim.getRootVehicle().onGround() || victim.getY() > getY() + 1.5)
				continue;
			hitThisMove.remove(victim.getUUID());
			hit(victim, damage, away(victim).scale(0.6).add(0, 0.9, 0));
		}
	}

	/** Damages and throws a victim, once per move; true if it landed. */
	private boolean hit(LivingEntity victim, float damage, Vec3 push) {
		if (!hitThisMove.add(victim.getUUID()))
			return false;
		victim.stopRiding(); // knocked out of the saddle, so the push lands
		boolean landed = victim.hurt(damageSources().mobAttack(this), damage);
		victim.push(push.x, push.y, push.z);
		victim.hurtMarked = true;
		return landed;
	}

	private Vec3 away(Entity e) {
		Vec3 v = e.position().subtract(position()).multiply(1, 0, 1);
		return v.lengthSqr() < 1e-4 ? forward() : v.normalize();
	}

	public double horizontalDistance(Entity e) {
		double dx = e.getX() - getX(), dz = e.getZ() - getZ();
		return Math.sqrt(dx * dx + dz * dz);
	}

	/** Is {@code e} within {@code halfAngle} degrees of where he faces? */
	public boolean inFront(Entity e, double halfAngle) {
		Vec3 to = e.position().subtract(position()).multiply(1, 0, 1);
		if (to.lengthSqr() < 1e-4)
			return true;
		return to.normalize().dot(forward()) >= Math.cos(Math.toRadians(halfAngle));
	}

	// ======================================================================== targets and players

	static boolean fightable(Player p) {
		return p.isAlive() && !p.isCreative() && !p.isSpectator() && !(p instanceof FakePlayer);
	}

	@Nullable
	private Player nearestPlayer(double range, boolean mustSee) {
		Player best = null;
		double bestD = range * range;
		for (Player p : level().players()) {
			if (!fightable(p) || fallen.contains(p.getUUID())) // respawned after he killed them: left be until they hit him
				continue;
			double d = p.distanceToSqr(this);
			if (d < bestD && (!mustSee || hasLineOfSight(p))) {
				best = p;
				bestD = d;
			}
		}
		return best;
	}

	/** D27: players, and whatever hits him; a target is dropped beyond 64 blocks. */
	@Nullable
	private LivingEntity updateTarget() {
		LivingEntity target = getTarget();
		if (target != null && (!target.isAlive() || target.level() != level() || target.distanceTo(this) > LOSE
			|| target instanceof Player p && !fightable(p)))
			target = null;
		if (target == null || !(target instanceof Player) && tickCount % 20 == 0) {
			Player p = nearestPlayer(AGGRO, false);
			if (p != null)
				target = p;
		}
		if (target != getTarget())
			setTarget(target);
		return target;
	}

	// ======================================================================== damage (D14, D23, D26, D50, D51)

	/** Only real players, their projectiles and their pets can hurt him; nothing Create does can (D51). */
	static boolean fromPlayer(DamageSource source) {
		Entity attacker = source.getEntity();
		if (attacker instanceof Player p)
			return !(p instanceof FakePlayer);
		return attacker instanceof OwnableEntity pet && pet.getOwner() instanceof Player owner && !(owner instanceof FakePlayer);
	}

	/** Struck from behind (where the key is): within 70° of straight behind him. */
	boolean fromBehind(DamageSource source) {
		Vec3 from = source.getSourcePosition();
		if (from == null)
			return false;
		Vec3 to = from.subtract(position()).multiply(1, 0, 1);
		return to.lengthSqr() > 1e-4 && to.normalize().dot(forward()) < -Math.cos(Math.toRadians(70));
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))
			return false;
		return source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_DROWNING) || source.is(DamageTypeTags.IS_FIRE)
			|| source.is(DamageTypeTags.IS_FREEZING) || source.is(DamageTypeTags.IS_EXPLOSION) && !fromPlayer(source)
			|| source == damageSources().inWall() || source == damageSources().cramming() || super.isInvulnerableTo(source);
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		// /kill and the void (no Mainspring without a player: the loot table wants one); a player's "true damage" weapon
		// still goes through his rules below
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !fromPlayer(source))
			return super.hurt(source, amount);
		// thorns would land in the middle of his own move; only blows count
		if (level().isClientSide || isInvulnerableTo(source) || phase() == Phase.DYING || source.is(DamageTypes.THORNS))
			return false;
		if (!fromPlayer(source)) {
			// a golem or a stray mob: no damage, but he'll deal with it if nobody else is about
			if (source.getEntity() instanceof LivingEntity attacker && attacker != this && getTarget() == null && phase() == Phase.WOUND)
				setTarget(attacker);
			return false;
		}
		if (source.getEntity() instanceof Player hitter)
			fallen.remove(hitter.getUUID()); // back in the fight (D58)
		else if (source.getEntity() instanceof LivingEntity pet && phase() == Phase.WOUND && !(getTarget() instanceof Player))
			setTarget(pet); // a pet biting him gets dealt with (D27)
		if (phase() == Phase.DORMANT || phase() == Phase.STIRRING) {
			// a player's hit wakes him, even resting after a wipe; a pet's doesn't (D19: never in peaceful)
			if (source.getEntity() instanceof Player && level().getDifficulty() != Difficulty.PEACEFUL)
				beginWaking("wake");
			return false;
		}
		if (phase() == Phase.WAKING || phase() == Phase.SHEDDING) {
			sound("plate_ring", 1);
			return false;
		}
		boolean open = isOpen();
		// wound, arrows and tridents glance off (D23); in overdrive everything lands
		if (!open && !overdrive && source.getDirectEntity() instanceof Projectile) {
			sound("plate_ring", 1.1f);
			return false;
		}
		float factor = open || overdrive ? 1 : (float) woundDamage(stage());
		boolean onKey = open && fromBehind(source);
		if (onKey)
			factor *= KEY_BONUS;
		float floor = stageFloor();
		float dealt = Math.min(amount * factor / toughness, Math.max(0, getHealth() - floor));
		boolean hurt = super.hurt(source, dealt);
		// only real blows on the key count toward a jam: not snowballs, not hits his armour's flinch turned away
		if (hurt && onKey && amount >= 1)
			keyHit();
		if (hurt && open && !jammed && phase() == Phase.REWIND && clip().name().equals("rewind"))
			playClip("hit_open");
		else if (hurt && !open && phase() == Phase.WOUND && move == null)
			playClip("hit_wound");
		if (getHealth() <= floor + 0.01f) {
			if (stage() >= 3)
				beginDying();
			else
				beginShedding();
		} else if (hurt && stage() >= 3 && !overdrive && getHealth() <= getMaxHealth() * OVERDRIVE_SHARE)
			beginOverdrive();
		return hurt;
	}

	private void keyHit() {
		if (jammed || ++keyHits < JAM_HITS)
			return;
		jammed = true; // D48: three on the key in one rewind jam it, 3 s more, once
		playClip("key_jam");
		ServerLevel server = (ServerLevel) level();
		Vec3 key = position().subtract(forward().scale(1.5)).add(0, 5, 0);
		server.sendParticles(ParticleTypes.ELECTRIC_SPARK, key.x, key.y, key.z, 20, 0.5, 0.5, 0.5, 0.2);
	}

	private void beginShedding() {
		endMove();
		comboLeft = 0;
		stopWalking();
		setPhase(Phase.SHEDDING);
		playClip("shed_plates_" + (stage() + 1), 1, 0);
	}

	private void beginDying() {
		endMove();
		stopWalking();
		setHealth(1);
		setPhase(Phase.DYING);
		playClip("death", 1, 0);
	}

	@Override
	public void thunderHit(ServerLevel level, LightningBolt bolt) {
		// D25: lightning rewinds him at once, skipping his open window
		if (phase() == Phase.RUN_DOWN || phase() == Phase.REWIND) {
			setPhase(Phase.REWIND_END);
			playClip("lightning");
		}
	}

	// his sounds come from his clips' cues and the heartbeat, not the vanilla mob sounds

	@Override
	@Nullable
	protected SoundEvent getHurtSound(DamageSource source) {
		return null;
	}

	@Override
	@Nullable
	protected SoundEvent getDeathSound() {
		return null;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
	}

	@Override
	public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
		return false;
	}

	/** D26: in water he sinks and walks the bottom (and drowning can't hurt him: isInvulnerableTo). */
	@Override
	public boolean isAffectedByFluids() {
		return false;
	}

	@Override
	public boolean canBeAffected(MobEffectInstance effect) {
		return effect.getEffect() != MobEffects.POISON && effect.getEffect() != MobEffects.WITHER && super.canBeAffected(effect);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public PushReaction getPistonPushReaction() {
		return PushReaction.IGNORE;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		return getBoundingBox().inflate(2.5, 1, 2.5); // his arms reach past his box in a swing, a charge or a grab
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean shouldDespawnInPeaceful() {
		return false;
	}

	@Override
	protected boolean canRide(Entity vehicle) {
		return false;
	}

	@Override
	public boolean canChangeDimensions(Level from, Level to) {
		return false;
	}

	// ======================================================================== the heartbeat and the boss bar

	private void heartbeat() {
		if (++heartbeat < (stage() >= 3 ? HEARTBEAT_STAGE_3 : HEARTBEAT))
			return;
		heartbeat = 0;
		tock = !tock;
		sound(tock ? "tock" : "tick", 1);
	}

	private void updateBossBar() {
		bossBar.setProgress(getHealth() / getMaxHealth());
		boolean awake = phase() != Phase.DORMANT;
		bossBar.setVisible(awake);
		// the marker while he rewinds (D28): the bar turns rune-blue while he's open
		bossBar.setColor(overdrive ? BossEvent.BossBarColor.RED : isOpen() ? BossEvent.BossBarColor.BLUE : BossEvent.BossBarColor.YELLOW);
		if (tickCount % 20 != 0)
			return;
		for (ServerPlayer p : new ArrayList<>(bossBar.getPlayers()))
			if (p.distanceToSqr(this) > LOSE * LOSE || p.level() != level())
				bossBar.removePlayer(p);
		for (Player p : level().players())
			if (p instanceof ServerPlayer sp && p.distanceToSqr(this) <= LOSE * LOSE)
				bossBar.addPlayer(sp);
	}

	@Override
	public void remove(RemovalReason reason) {
		super.remove(reason);
		bossBar.removeAllPlayers(); // dead or unloaded: no bar left behind for players 48-64 blocks off
	}

	/** D2: he never changes a block, so he never tramples farmland or turtle eggs (mob griefing). */
	public static void onMobGriefing(EntityMobGriefingEvent event) {
		if (event.getEntity() instanceof ClockworkJuggernaut)
			event.setCanGrief(false);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bossBar.removePlayer(player);
	}

	@Override
	public void setCustomName(@Nullable Component name) {
		super.setCustomName(name);
		bossBar.setName(getDisplayName());
	}

	// ======================================================================== spawning and saving

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
		SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data);
		// summoned (the ritual, an egg, the summon command): he unfolds and salutes
		beginWaking("wake_ritual");
		return out;
	}

	/** Kneeling asleep, for a ruin (and for tests): wakes when a player comes near (D32). */
	public void makeDormant() {
		goDormant();
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putString("Phase", phase().name());
		tag.putByte("Stage", (byte) stage());
		tag.putInt("WoundTicks", woundTicks);
		tag.putInt("RewindTicks", rewindTicks);
		tag.putBoolean("Feinted", feinted);
		tag.putBoolean("Jammed", jammed);
		tag.putInt("KeyHits", keyHits);
		tag.putBoolean("Scaled", scaled);
		tag.putBoolean("Overdrive", overdrive);
		tag.putFloat("Toughness", toughness);
		tag.putInt("WipePending", wipePending);
		ListTag fallenTag = new ListTag();
		for (UUID id : fallen)
			fallenTag.add(NbtUtils.createUUID(id));
		tag.put("Fallen", fallenTag);
		tag.putInt("Rest", graceTicks);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		entityData.set(STAGE, (byte) Mth.clamp(tag.getByte("Stage"), 1, 3));
		Phase phase;
		try {
			phase = Phase.valueOf(tag.getString("Phase"));
		} catch (IllegalArgumentException e) {
			phase = Phase.WOUND;
		}
		woundTicks = tag.getInt("WoundTicks");
		rewindTicks = tag.getInt("RewindTicks");
		feinted = tag.getBoolean("Feinted");
		jammed = tag.getBoolean("Jammed");
		keyHits = tag.getInt("KeyHits");
		scaled = tag.getBoolean("Scaled");
		toughness = tag.contains("Toughness") ? Math.max(1, tag.getFloat("Toughness")) : 1;
		fallen.clear();
		for (Tag id : tag.getList("Fallen", Tag.TAG_INT_ARRAY))
			fallen.add(NbtUtils.loadUUID(id));
		overdrive = tag.getBoolean("Overdrive") && phase != Phase.DORMANT && phase != Phase.STIRRING;
		if (hasCustomName())
			bossBar.setName(getDisplayName());
		// saved mid-leap: the arc's no-gravity was saved with him, but the leap wasn't
		setNoGravity(false);
		noPhysics = false;
		// moves, waking and shedding don't survive a reload: he picks up wound (or asleep, or rewinding) again
		if (phase == Phase.SHEDDING)
			entityData.set(STAGE, (byte) Math.min(3, stage() + 1)); // the plates are off
		switch (phase) {
			case DORMANT, STIRRING -> {
				goDormant();
				graceTicks = tag.getInt("Rest");
			}
			case REWIND -> {
				setPhase(Phase.REWIND);
				playClip("rewind");
			}
			case DYING -> beginDying();
			default -> {
				int wound = woundTicks;
				startWound();
				woundTicks = wound;
			}
		}
		if (phase != Phase.DORMANT && phase != Phase.STIRRING)
			wipePending = tag.getInt("WipePending"); // saved in the beat after a wipe: he still kneels
	}

	// ======================================================================== test hooks

	/** Puts him straight into a phase (game tests). */
	public void forcePhase(Phase phase) {
		endMove();
		switch (phase) {
			case REWIND -> {
				setPhase(Phase.REWIND);
				rewindTicks = 0;
				playClip("rewind");
			}
			case DORMANT -> goDormant();
			default -> startWound();
		}
	}

	/** Skips to a stage, its plates already shed (game tests). */
	public void forceStage(int stage) {
		entityData.set(STAGE, (byte) Mth.clamp(stage, 1, 3));
	}

	/** Springs at {@code target} now (game tests). */
	public void forceLeap(LivingEntity target) {
		setTarget(target);
		startMove(Move.LEAP, target);
	}

	/** How many moves he has started (game tests). */
	public int movesStarted() {
		return movesStarted;
	}

	public boolean jammed() {
		return jammed;
	}

	public int rewindTicks() {
		return rewindTicks;
	}
}
