package com.comedymonkey.createaddons.content.juggernaut;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.juggernaut.ruin.JuggernautRuinStructure;
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
import net.neoforged.neoforge.common.util.FakePlayerFactory;
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
public final class ClockworkJuggernaut extends Monster {
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
	/** Going nowhere this long with them out of his reach (walled in, pinned in a pit): he steams out and comes to them. */
	public static final int TRAPPED_TICKS = 200;
	/** He rethinks who to go after this often while chasing, as well as after every combo. */
	static final int RETHINK_TICKS = 120;
	/** How often a move comes from the next distance band over, and a combo opens with a cog though he could reach them. */
	static final float BLUR = 0.25f, SURPRISE_COG = 0.125f;
	/** By stage, how often a wind-up is a fake-out that turns into another move. */
	static final float[] FAKE_OUT = { 0.10f, 0.15f, 0.20f };
	/** Circled up close this long without getting to face them: he stomps the ring round him instead. */
	static final int CIRCLE_TICKS = 30;
	/** Degrees a tick he turns to face his target before a move. */
	static final float TURN = 9;
	/** Ticks between looks for where a coiling leap will land (it's redrawn every 2 ticks; the take-off looks afresh). */
	static final int COIL_AIM_TICKS = 4;

	private final ServerBossEvent bossBar = new ServerBossEvent(Component.translatable("entity.comedymonkeysaddons.clockwork_juggernaut"),
		BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);
	/** Client: what's playing, the cross-fade, and the cues already played. */
	public final ClipPlayback playback = new ClipPlayback();
	private int seenClip = -1, seenClipStart;
	private double localClipStart;

	// ---- server state (saved)
	private int phaseTicks, woundTicks, rewindTicks, aloneTicks, keyHits, graceTicks;
	private boolean feinted, jammed, scaled, overdrive;
	/** Found kneeling in a ruin and not yet woken: his first waking ends with him bursting up through its roof (D61). */
	private boolean ruin;
	/** The leap that breaks him out of his ruin, and where it comes down. */
	private boolean bursting;
	/** A hop over a wall in his way: the break-out's path (up, across, down), lower, and breaking nothing. */
	private boolean hopping;
	/** Where his ruin's hall stands, if he was found in one; its field reaches about 40 blocks round it. */
	@Nullable
	private BlockPos home;
	private int hopCooldown;
	private Vec3 burstTo = Vec3.ZERO;
	/** Extra players when he woke (D14): every hit is divided by this. */
	private float toughness = 1;
	/** When he last killed a player (not saved). */
	private int lastKill = -KILL_MEMORY, wipePending, trackedId = -1;
	/** Players he's killed this fight (D58): respawned, they don't keep him awake until they hit him again. */
	private final Set<UUID> fallen = new HashSet<>();
	/** What else has hurt him this fight (pets, golems, mobs): his blows and cog may hit them; bystanders never (D27). Not saved. */
	private final Set<UUID> attackers = new HashSet<>();
	/**
	 * Who's been hurting him lately, fading by half every 4 s; blows from behind count double. Whoever hurts him most
	 * draws him (the user: one player kiting him while the rest worked his key made him "easy to cheese").
	 */
	private final java.util.Map<UUID, Float> threat = new java.util.HashMap<>();
	/** He rethinks who to go after at the end of a combo, and every RETHINK_TICKS while chasing. */
	private boolean rethink;
	private int rethinkTicks;
	/** Struck from behind mid-move by someone he isn't after: he rounds on them once the move's done. */
	@Nullable
	private UUID roundOn;
	/** His last few moves, latest first: the ones he's just used are less likely (D46, loosened). */
	private final Move[] recent = new Move[3];
	/** When this move's wind-up turns out to be a fake-out into another move (clip seconds); 0 for an honest one. */
	private float fakeOutAt;
	/** The next combo opens with a cog, though he could reach them. */
	private boolean surpriseCog;
	/** The blow that killed him (not saved), and whether a player (or their pet) struck it and who: saved, so a reload in
	 * his death clip still drops the Mainspring (the loot table wants a player's kill). */
	@Nullable
	private DamageSource killingBlow;
	private boolean playerKill;
	@Nullable
	private UUID killer;
	/** His loot and XP have dropped (saved): never twice for one Juggernaut. */
	private boolean paidOut;
	/** Where he stood still last step: an outside shove (a Create contraption moves him by setting his position) is undone
	 * the next, as nothing may push him (D20, D56). Not saved. */
	@Nullable
	private Vec3 anchoredAt;
	/** Game tests: a hall or field placed by hand has no structure in the world, so this stands in for the ruin's pieces. */
	@Nullable
	private java.util.function.Predicate<BlockPos> testRuin;
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
	/** Where he was when he last got anywhere, and how long he's been going nowhere since (see {@link #unpin}). */
	@Nullable
	private Vec3 trappedAt;
	private int trappedTicks;
	private boolean tock, charging;
	@Nullable
	private LivingEntity grabbed;
	private Vec3 leapFrom = Vec3.ZERO, leapTo = Vec3.ZERO;
	private boolean leaping;
	/** Where the coiling leap aims, looked up every few ticks (it's ten body-sized collision checks), and ticks till the next. */
	@Nullable
	private Vec3 coilAim;
	private int coilAimTicks;
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
			.add(Attributes.STEP_HEIGHT, 3); // his big feet step over anything 3 high: a ruin's stubs and broken walls
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
		holdGround();
		serverStep();
		settle();
		// standing (not walking, charging, spinning after them or in the air): from here, anything that moves him is a shove
		anchoredAt = !leaping && !charging && zza == 0 && getDeltaMovement().horizontalDistanceSqr() < 1e-6 && !isPassenger()
			? position() : null;
	}

	/**
	 * D20, D56: nothing pushes him. Create's contraptions shove whatever they run into by setting its position (asking
	 * neither isPushable nor knockback resistance), or by throwing it (a train running it over, a slime-faced
	 * contraption's bounce), so a move he didn't make while he stood still is put back and any sideways speed he didn't
	 * give himself taken away, keeping his height and fall so he still falls. His own moves set their speed later in
	 * the step, so they're untouched.
	 */
	/**
	 * On a hillside a corner of his wide footprint can catch on the slope and hold the rest of him over thin air (the
	 * user: "he ends up floating if he is on a hill"). Standing with under a third of his footprint on ground (within 2
	 * blocks under his feet, so steps and slopes he can walk are ground), he slides off away from what's holding him up,
	 * and comes down.
	 */
	private void settle() {
		if (!onGround() || leaping || charging || noPhysics || zza != 0 || isPassenger())
			return;
		AABB box = getBoundingBox();
		int feet = Mth.floor(getY() - 0.5), held = 0, cells = 0, under = 0;
		double cx = 0, cz = 0;
		for (int x = Mth.floor(box.minX); x <= Mth.floor(box.maxX - 1e-3); x++)
			for (int z = Mth.floor(box.minZ); z <= Mth.floor(box.maxZ - 1e-3); z++) {
				cells++;
				boolean holding = solid(new BlockPos(x, feet, z));
				if (holding || solid(new BlockPos(x, feet - 1, z)))
					held++;
				if (holding) {
					cx += x + 0.5;
					cz += z + 0.5;
					under++;
				}
			}
		if (held * 3 >= cells || under == 0)
			return;
		Vec3 off = new Vec3(getX() - cx / under, 0, getZ() - cz / under);
		if (off.lengthSqr() > 1e-4)
			move(net.minecraft.world.entity.MoverType.SELF, off.normalize().scale(0.2));
	}

	private void holdGround() {
		if (anchoredAt == null || leaping)
			return;
		double dx = getX() - anchoredAt.x, dz = getZ() - anchoredAt.z;
		boolean shoved = dx * dx + dz * dz > 1e-6;
		if (shoved)
			setPos(anchoredAt.x, getY(), anchoredAt.z);
		if (getDeltaMovement().horizontalDistanceSqr() > 1e-8) {
			setDeltaMovement(0, getDeltaMovement().y, 0);
			shoved = true;
		}
		if (shoved)
			hurtMarked = true;
	}

	@Override
	public void teleportTo(double x, double y, double z) {
		super.teleportTo(x, y, z);
		anchoredAt = null; // a command (or a mod) sending him somewhere isn't a shove
	}

	@Override
	public boolean teleportTo(ServerLevel level, double x, double y, double z, Set<net.minecraft.world.entity.RelativeMovement> relative,
							  float yaw, float pitch) {
		boolean moved = super.teleportTo(level, x, y, z, relative, yaw, pitch);
		anchoredAt = null;
		return moved;
	}

	private void serverStep() {
		super.customServerAiStep();
		phaseTicks++;
		updateBossBar();
		if (level().getDifficulty() == Difficulty.PEACEFUL && phase() != Phase.DORMANT && phase() != Phase.DYING) {
			goDormant(); // D19: never fights in peaceful
			return;
		}
		if (overdrive && phase() != Phase.DYING)
			overdriveSteam();
		// what they've done to him fades, by half every 4 s
		if (tickCount % 20 == 0 && !threat.isEmpty()) {
			threat.replaceAll((p, t) -> t * 0.84f);
			threat.values().removeIf(t -> t < 0.5f);
		}
		if (tickCount % 20 == 0)
			lookForLegacyRuin();
		crumbleAhead();
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
				if (clipDone()) {
					if (ruin)
						breakOut();
					else
						startWound();
				}
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
		trappedAt = null; // a fresh wind: going nowhere is counted afresh
		trappedTicks = 0;
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
		attackers.clear();
		threat.clear();
		roundOn = null;
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
		if (unpin(target))
			return;
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
		if (hopCooldown > 0)
			hopCooldown--;
		if (move == null && stuckTicks >= HOP_AFTER && tryHop(target))
			return;
		if (restTicks > 0) {
			restTicks--;
			approach(target, d);
			return;
		}
		// a combo of 1 to stage + 1 moves, so there's no counting his rhythm; now and then it opens with a cog at range
		if (comboLeft <= 0) {
			comboLeft = 1 + random.nextInt(stage() + 1);
			surpriseCog = d >= CLOSE && random.nextFloat() < SURPRISE_COG;
		}
		// the move is chosen once and kept while he turns to it (re-rolling every tick would favour the moves that need no
		// facing); a new one only if they change distance band
		int band = d < CLOSE ? 0 : d < FAR ? 1 : 2;
		if (stuckTicks >= STUCK_TICKS || surpriseCog)
			planned = Move.COG;
		else if (planned == null || planned == Move.COG || band != plannedBand) {
			planned = pick(target, d, null);
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

	/**
	 * D46, loosened at the user's asking ("be a bit more random"): a move for the distance from this stage's set (D44),
	 * but one move in four from the next band over where it can still land; never the same twice in a row (nor
	 * {@code not}), and the two before that less likely.
	 */
	@Nullable
	private Move pick(LivingEntity target, double d, @Nullable Move not) {
		int band = d < CLOSE ? 0 : d < FAR ? 1 : 2;
		if (random.nextFloat() < BLUR) {
			java.util.Map<Move, Double> other = options(target, d, band == 1 ? (random.nextBoolean() ? 0 : 2) : 1, not);
			if (!other.isEmpty())
				return weighted(other);
		}
		java.util.Map<Move, Double> options = options(target, d, band, not);
		return options.isEmpty() ? null : weighted(options);
	}

	/** The moves of distance band {@code band} (0 close, 1 middle, 2 far) that can reach them at {@code d}, weighted. */
	private java.util.Map<Move, Double> options(LivingEntity target, double d, int band, @Nullable Move not) {
		java.util.Map<Move, Double> options = new java.util.EnumMap<>(Move.class);
		int stage = stage();
		if (band == 0) {
			if (d < 7.5)
				options.put(Move.SWING, 1.0);
			if (inFront(target, 50) && d < 7.5)
				options.put(Move.GRAB, 1.0);
			if (stage >= 3 && d < 10)
				options.put(Move.SPIN_START, 1.0);
		} else if (band == 1) {
			options.put(Move.CHARGE, 1.0);
			if (stage >= 2)
				options.put(Move.VENT, 1.0);
			// strafing out of reach of a charge: he jumps on them instead
			if (d >= 10 && strafing(target) && leapLanding(target, 1.5f) != null)
				options.put(Move.LEAP, 1.0);
		} else {
			options.put(Move.CHARGE, 1.0);
			// far off, he'd rather come down on them: twice the chance of a charge (and not from right on top of them)
			if (d >= 8 && leapLanding(target, 1.5f) != null)
				options.put(Move.LEAP, 2.0);
		}
		// a stomp only reaches 6 blocks: up close, or as they come in
		if (band < 2 && d < CLOSE + 3)
			options.put(Move.STOMP, 1.0);
		options.remove(lastMove);
		if (not != null)
			options.remove(not);
		if (lastMove == Move.SPIN_END)
			options.remove(Move.SPIN_START);
		// what he's just done is less likely
		if (recent[1] != null)
			options.computeIfPresent(recent[1], (m, w) -> w * 0.4);
		if (recent[2] != null)
			options.computeIfPresent(recent[2], (m, w) -> w * 0.7);
		return options;
	}

	private Move weighted(java.util.Map<Move, Double> options) {
		double total = options.values().stream().mapToDouble(Double::doubleValue).sum(), r = random.nextDouble() * total;
		for (var e : options.entrySet())
			if ((r -= e.getValue()) <= 0)
				return e.getKey();
		return options.keySet().iterator().next();
	}

	/**
	 * When a wind-up turns into another move (the user's fake-outs), in clip seconds: well before anything lands. Never
	 * the leap (its landing ring is a promise), the spin or the cog.
	 */
	private float fakeOutTime(Move m) {
		return switch (m) {
			case CHARGE -> 0.4f;
			case SWING -> 0.3f;
			case STOMP, STOMP_DOUBLE -> 0.45f;
			case GRAB -> 0.7f;
			case VENT -> 1.3f;
			default -> 0;
		};
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
		coilAim = null;
		coilAimTicks = 0; // a leap's coil aims at once
		fakeOutAt = random.nextFloat() < FAKE_OUT[Math.min(stage(), 3) - 1] ? fakeOutTime(next) : 0;
		surpriseCog = false; // it opens the combo, no more
		playClip(next.clip);
	}

	/**
	 * A fake-out: part-way through a wind-up he breaks off into another move, before anything of the first could land.
	 * The new move is the one the combo counts; it never fakes out in turn.
	 */
	private boolean fakeOut(@Nullable LivingEntity target) {
		if (fakeOutAt <= 0 || moveTime < fakeOutAt)
			return false;
		fakeOutAt = 0;
		if (target == null)
			return false;
		Move other = pick(target, horizontalDistance(target), move);
		// the vent's cone goes where he faces, and it doesn't turn him: only if he's already facing them
		if (other == null || other == Move.VENT && !inFront(target, 30))
			return false;
		startMove(other, target);
		fakeOutAt = 0;
		return true;
	}

	/** The next part of a move that comes in parts (the gear spin), or the end of the combo's move. */
	private void endMove() {
		grabbed = null;
		bursting = false;
		hopping = false;
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
			System.arraycopy(recent, 0, recent, 1, recent.length - 1);
			recent[0] = lastMove;
			comboLeft--;
			if (comboLeft <= 0) {
				surpriseCog = false;
				rethink = true; // a combo done: who next?
			}
			// relentless: a breath between moves, a short one between combos (shorter still in overdrive); how long, he
			// keeps you guessing
			restTicks = overdrive ? (comboLeft > 0 ? 4 : 8 + random.nextInt(10)) : comboLeft > 0 ? 4 + random.nextInt(11) : 10 + random.nextInt(41);
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
		if (fakeOut(target))
			return;
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
		if (t < 1.5f && (target != null || bursting || hopping)) {
			// where they're running to, looked up every few ticks (a landing check is ten checks of his whole box); the
			// take-off below looks afresh
			if (!bursting && !hopping && --coilAimTicks <= 0) {
				coilAim = leapLanding(target, (1.5f - t) / clipSpeed() + LEAP_FLIGHT);
				coilAimTicks = COIL_AIM_TICKS;
			}
			Vec3 aim = bursting || hopping ? burstTo : coilAim;
			turnToward(aim != null ? aim : target.position(), 6);
			if (aim != null && t > 0.3f && tickCount % 2 == 0)
				markLanding(aim);
		}
		if (leaping && tickCount % 2 == 0)
			markLanding(leapTo);
		if (at(1.5f)) {
			Vec3 landing = bursting ? stillClear(burstTo, BURST_RISE) : hopping ? (stillClear(burstTo, HOP_RISE) != null ? burstTo : target == null ? null
				: hopLanding(target)) : target == null ? null : leapLanding(target, LEAP_FLIGHT);
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
			if (bursting) {
				ruin = false;
				breakRoof();
			}
		}
		if (leaping) {
			float u = Mth.clamp((t - 1.55f) / 0.53f, 0, 1);
			Vec3 at = bursting || hopping ? burstPath(u) : leapFrom.lerp(leapTo, u).add(0, 6 * Mth.sin(u * Mth.PI), 0);
			setDeltaMovement(Vec3.ZERO);
			setPos(at.x, at.y, at.z);
			if (u >= 1) {
				boolean wasHop = hopping;
				leaping = false;
				bursting = false;
				hopping = false;
				noPhysics = false;
				setNoGravity(false);
				setPos(leapTo.x, leapTo.y, leapTo.z);
				playClip("leap", speed(), 2.1f); // back to his own pace for the recovery
				if (wasHop) {
					stuckTicks = 0;
					crumbleAround(2);
				} else {
					stomp(5, LEAP_DAMAGE);
					freezeAround(5); // the ground shakes (client, on the leap_land cue) and they're held still
				}
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
		return leapLanding(spot, true);
	}

	@Nullable
	private Vec3 leapLanding(Vec3 spot, boolean arc) {
		BlockPos feet = BlockPos.containing(spot.x, spot.y + 0.5, spot.z);
		for (int dy = 0; dy >= -4; dy--) {
			BlockPos below = feet.offset(0, dy - 1, 0);
			if (!level().getBlockState(below).getCollisionShape(level(), below).isEmpty()) {
				Vec3 land = new Vec3(spot.x, below.getY() + 1, spot.z);
				AABB box = getDimensions(getPose()).makeBoundingBox(land);
				// room to stand, ground under all of him (not a pillar top), no higher or lower than he could walk
				// back from (D56), and a clear arc there: he never leaps through a hill or a cave roof
				return level().noCollision(this, box) && Math.abs(land.y - getY()) <= 3 && footing(land) && (!arc || arcClear(position(), land))
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

	/**
	 * D61: woken in his ruin, he bursts up through its roof and comes down by whoever woke him (or out in front of the
	 * doorway): straight up, breaking the beams, chains and vines above him, across above the walls, and down.
	 */
	private void breakOut() {
		startWound();
		LivingEntity target = updateTarget();
		startMove(Move.LEAP, target);
		bursting = true;
		burstTo = burstLanding(target);
	}

	/**
	 * Where the break-out comes down: by whoever woke him, or failing that the nearest spot all round with room for him
	 * and ground under him (up to 26 blocks out, uphill or down), nearest to them first; or, hemmed in by the ruin's walls,
	 * back on his dais: through the roof regardless.
	 */
	private Vec3 burstLanding(@Nullable LivingEntity target) {
		Vec3 toward = target != null ? target.position() : position().add(forward().scale(16));
		Vec3 best = burstSpot(toward.x, toward.z);
		double bestD = best == null ? Double.MAX_VALUE : 0;
		for (int r = 10; r <= 26; r += 2)
			for (int k = 0; k < 24; k++) {
				double a = k * Math.PI * 2 / 24;
				Vec3 at = burstSpot(getX() + Math.cos(a) * r, getZ() + Math.sin(a) * r);
				if (at != null && at.distanceToSqr(toward) < bestD) {
					best = at;
					bestD = at.distanceToSqr(toward);
				}
			}
		return best != null ? best : position();
	}

	/** Standing room on the ground at (x, z), found from the top down, within 8 blocks of his height; or null. */
	@Nullable
	private Vec3 burstSpot(double x, double z) {
		int y = level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
		Vec3 land = new Vec3(x, y, z);
		return Math.abs(y - getY()) <= 8 && level().noCollision(this, getDimensions(getPose()).makeBoundingBox(land)) && footing(land) ? land : null;
	}

	/** How high above the higher end the break-out climbs (over the hall's walls with room to spare), and a hop (a ruin's walls). */
	private static final double BURST_RISE = 10, HOP_RISE = 7;
	/** Unable to reach them this long, he hops (before, at STUCK_TICKS, he gives up and throws a cog). */
	static final int HOP_AFTER = 40;

	/**
	 * He can't be pinned or trapped (the user's "so he's easy"): with them out of his reach and him going nowhere for
	 * {@link #TRAPPED_TICKS} (walled in, down a pit, across water he can't hop), he goes up in steam and comes out of it
	 * beside them. With nowhere by them to stand (up a pillar), he keeps to his cogs and looks again in a while.
	 */
	private boolean unpin(@Nullable LivingEntity target) {
		if (target == null || horizontalDistance(target) < CLOSE + 1 && Math.abs(target.getY() - getY()) <= 3) {
			trappedAt = null;
			trappedTicks = 0;
			return false;
		}
		// getting somewhere counts only by a way he can walk to them: pacing a trench floor under them doesn't
		if (trappedAt == null || trappedAt.distanceToSqr(position()) > 4) {
			trappedAt = position();
			if (stuckTicks == 0) {
				trappedTicks = 0;
				return false;
			}
		}
		if (++trappedTicks < TRAPPED_TICKS || move != null)
			return false;
		Vec3 to = besideThem(target);
		if (to == null) {
			trappedTicks = TRAPPED_TICKS / 2;
			return false;
		}
		steamOut();
		stopWalking();
		teleportTo(to.x, to.y, to.z);
		float yaw = (float) (Mth.atan2(target.getZ() - to.z, target.getX() - to.x) * Mth.RAD_TO_DEG) - 90;
		setYRot(yaw);
		setYBodyRot(yaw);
		setYHeadRot(yaw);
		steamOut();
		trappedAt = position();
		trappedTicks = stuckTicks = 0;
		planned = null;
		// out of the steam he takes a moment to find his feet before he strikes
		restTicks = Math.max(restTicks, 25);
		playClip("idle");
		return true;
	}

	/**
	 * A spot beside them with room for him, about their height, nearest first, from where he can see them, and a good
	 * deal nearer them than he is now, unless he's down a pit or up a ledge from them (else he'd only steam from one
	 * out-of-reach spot to the next).
	 */
	@Nullable
	private Vec3 besideThem(LivingEntity target) {
		double now = horizontalDistance(target);
		boolean level = Math.abs(target.getY() - getY()) <= 3;
		for (int r = 5; r <= 11 && (!level || r <= now - 3); r += 2)
			for (int k = 0; k < 16; k++) {
				double a = k * Math.PI * 2 / 16;
				double x = target.getX() + Math.cos(a) * r, z = target.getZ() + Math.sin(a) * r;
				for (int dy : new int[] { 0, 1, -1, 2, -2, 3, -3 }) {
					Vec3 at = new Vec3(x, Mth.floor(target.getY()) + dy, z);
					if (footing(at) && level().noCollision(this, getDimensions(getPose()).makeBoundingBox(at))
						&& level().clip(new net.minecraft.world.level.ClipContext(at.add(0, 3, 0), target.getEyePosition(),
						net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, this))
						.getType() == net.minecraft.world.phys.HitResult.Type.MISS)
						return at;
				}
			}
		return null;
	}

	/** A burst of steam where he stands, as he goes and as he comes. */
	private void steamOut() {
		sound("plates_burst", 0.7f);
		((ServerLevel) level()).sendParticles(ParticleTypes.CLOUD, getX(), getY() + 3.5, getZ(), 60, 2, 2, 2, 0.05);
	}

	/** Springs over whatever's in his way to come down by {@code target}, if there's a clear way over and somewhere to land. */
	private boolean tryHop(LivingEntity target) {
		if (hopCooldown > 0 || move != null)
			return false;
		hopCooldown = 60; // looked for at most every 3 s, found or not
		Vec3 landing = hopLanding(target);
		if (landing == null)
			return false;
		startMove(Move.LEAP, target);
		hopping = true;
		burstTo = landing;
		return true;
	}

	/** Where a hop comes down: by them, or the nearest spot to them with room for him, with a clear way up, over and down. */
	@Nullable
	private Vec3 hopLanding(LivingEntity target) {
		Vec3 best = null;
		double bestD = Double.MAX_VALUE;
		for (int r = 4; r <= 10; r += 2)
			for (int k = 0; k < 12; k++) {
				double a = k * Math.PI * 2 / 12;
				Vec3 at = burstSpot(target.getX() + Math.cos(a) * r, target.getZ() + Math.sin(a) * r);
				if (at == null || Math.abs(at.y - getY()) > 3 || horizontalDistanceTo(at) < 3)
					continue;
				double d = at.distanceToSqr(target.position());
				if (d < bestD && hopClear(at)) {
					best = at;
					bestD = d;
				}
			}
		return best;
	}

	private double horizontalDistanceTo(Vec3 at) {
		return Math.sqrt((at.x - getX()) * (at.x - getX()) + (at.z - getZ()) * (at.z - getZ()));
	}

	/** {@code to}, if he can still come down there by a clear way up, over and down; else null (a break-out lands back home). */
	@Nullable
	private Vec3 stillClear(Vec3 to, double rise) {
		if (bursting)
			breakRoof(); // his way up is through it
		boolean room = to.equals(position()) || level().noCollision(this, getDimensions(getPose()).makeBoundingBox(to));
		if (room && pathClear(to, rise))
			return to;
		return bursting ? position() : null;
	}

	/** Nothing solid in the hop's way: straight up, across at the top, and down onto {@code to}. */
	private boolean hopClear(Vec3 to) {
		return pathClear(to, HOP_RISE);
	}

	private boolean pathClear(Vec3 to, double rise) {
		Vec3 from = position();
		double top = Math.max(from.y, to.y) + rise;
		for (int i = 1; i < 12; i++) {
			float u = i / 12f;
			Vec3 at = u < 0.35f ? new Vec3(from.x, Mth.lerp(smooth(u / 0.35f), from.y, top), from.z)
				: u < 0.7f ? from.lerp(to, smooth((u - 0.35f) / 0.35f)).with(net.minecraft.core.Direction.Axis.Y, top)
				: new Vec3(to.x, Mth.lerp(smooth((u - 0.7f) / 0.3f), top, to.y), to.z);
			if (!level().noCollision(this, getDimensions(getPose()).makeBoundingBox(at).deflate(0.3)))
				return false;
		}
		return true;
	}

	/** The break-out's path: straight up, across at the top, then down onto the landing. */
	private Vec3 burstPath(float u) {
		double top = Math.max(leapFrom.y, leapTo.y) + (hopping ? HOP_RISE : BURST_RISE);
		if (u < 0.35f)
			return new Vec3(leapFrom.x, Mth.lerp(smooth(u / 0.35f), leapFrom.y, top), leapFrom.z);
		if (u < 0.7f) {
			double k = smooth((u - 0.35f) / 0.35f);
			return new Vec3(Mth.lerp(k, leapFrom.x, leapTo.x), top, Mth.lerp(k, leapFrom.z, leapTo.z));
		}
		return new Vec3(leapTo.x, Mth.lerp(smooth((u - 0.7f) / 0.3f), top, leapTo.y), leapTo.z);
	}

	private static double smooth(float f) {
		return f * f * (3 - 2 * f);
	}

	/** The one time he breaks blocks (D61): the ruin's roof beams, chains, vines and slabs straight above him. */
	private void breakRoof() {
		if (home == null || !home.closerToCenterThan(position(), 12))
			return; // only ever his own hall's roof
		AABB column = getBoundingBox().setMinY(getY() + 1).setMaxY(getY() + BURST_RISE + getBbHeight() + 8); // up past the climb, landing uphill or not
		for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(column.minX, column.minY, column.minZ),
			BlockPos.containing(column.maxX - 1e-3, column.maxY, column.maxZ - 1e-3)))
			if (level().getBlockState(p).is(BREAKS_OUT))
				level().destroyBlock(p, true, this);
		sound("plates_burst", 0.8f);
		sound("stage_roar", 1);
		((ServerLevel) level()).sendParticles(ParticleTypes.CLOUD, getX(), getY() + 7, getZ(), 30, 2, 1, 2, 0.1);
	}

	/** What his break-out may smash on the way up (D61). */
	public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> BREAKS_OUT = net.minecraft.tags.TagKey.create(
		net.minecraft.core.registries.Registries.BLOCK, com.comedymonkey.createaddons.ComedyMonkeysCreateAddons.asResource("juggernaut_breaks_out"));

	/** What of a Juggernaut Ruin crumbles under him (D62): its own building blocks, never the ground or its brass. */
	public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> RUIN_BLOCKS = net.minecraft.tags.TagKey.create(
		net.minecraft.core.registries.Registries.BLOCK, com.comedymonkey.createaddons.ComedyMonkeysCreateAddons.asResource("juggernaut_ruin_blocks"));

	/** How far from his hall his ruin reaches (its field's radius, and a little). What of it crumbles is asked of the
	 * structure itself ({@link #crumbles}), so a ruin {@code /place} put down (which keeps no record of the structure)
	 * stands firm, but for its own blocks. */
	static final double RUIN_REACH = 45;

	/** A Juggernaut Ruin's own blocks (RuinBlocks), which any Juggernaut crumbles wherever he comes up against them. */
	public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> CRUMBLES = net.minecraft.tags.TagKey.create(
		net.minecraft.core.registries.Registries.BLOCK, com.comedymonkey.createaddons.ComedyMonkeysCreateAddons.asResource("juggernaut_crumbles"));

	/**
	 * A block of a ruin, which crumbles as he walks into it or stomps by it (D62): the ruin's own blocks, wherever they
	 * are; or, in a ruin built before it had them, its brick where the ruin itself put it, not a player's wall of the same
	 * brick beside it. Never its chest, brass, casing or shafts.
	 */
	boolean crumbles(BlockPos p) {
		BlockState state = level().getBlockState(p);
		if (state.is(CRUMBLES))
			return true;
		return legacyRuinNear && home != null && home.closerToCenterThan(Vec3.atCenterOf(p), RUIN_REACH) && state.is(RUIN_BLOCKS) && partOfRuin(p);
	}

	/** Near a ruin generated before it had its own blocks (looked up once a second): only then is the old check asked. */
	private boolean legacyRuinNear;

	private void lookForLegacyRuin() {
		legacyRuinNear = home != null && home.closerToCenterThan(position(), RUIN_REACH)
			&& (testRuin != null || level() instanceof ServerLevel server && JuggernautRuinStructure.legacyNear(server, blockPosition()));
	}

	/** Built by a Juggernaut Ruin: in its hall, or where its field put a wall, pillar or heap. */
	private boolean partOfRuin(BlockPos p) {
		if (testRuin != null)
			return testRuin.test(p);
		return level() instanceof ServerLevel server && JuggernautRuinStructure.built(server, p);
	}

	/**
	 * He goes through a ruin's walls: walking or charging, whatever of it is just ahead of him; and whatever of it he's
	 * pushed up against, whichever way; above his feet either way.
	 */
	private void crumbleAhead() {
		if (leaping)
			return;
		if (zza > 0 || charging)
			crumble(getBoundingBox().move(forward().scale(0.9)).setMinY(getY() + 1));
		if (horizontalCollision)
			crumble(getBoundingBox().inflate(0.6, 0, 0.6).setMinY(getY() + 1));
	}

	/** His stomp brings down the ruin within {@code radius} blocks of his footprint, from his feet up to his knees. */
	private void crumbleAround(double radius) {
		crumble(getBoundingBox().inflate(radius, 0, radius).setMinY(getY() + 1).setMaxY(getY() + 3));
	}

	/**
	 * Brings down what of a ruin is in {@code box}, and the rest of each of its columns above that: a wall knocked out
	 * from under comes down whole, never left floating (no longer standing on the ground, an old ruin's would no longer
	 * count as its, and stay in his way). All of it is judged before any of it falls.
	 */
	private void crumble(AABB box) {
		java.util.Set<BlockPos> falls = new java.util.LinkedHashSet<>();
		for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ),
			BlockPos.containing(box.maxX - 1e-3, box.maxY - 1e-3, box.maxZ - 1e-3))) {
			if (falls.contains(p) || !crumbles(p))
				continue;
			BlockPos up = p.immutable();
			for (int i = 0; i < 12 && (i == 0 || crumbles(up)); i++, up = up.above())
				falls.add(up);
		}
		for (BlockPos p : falls)
			level().destroyBlock(p, false, this);
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
			DamageSource source = killingBlow;
			// a player's kill, and the vanilla memory of it gone (it isn't saved, and lasts 5 s): back to them, so the loot
			// table's killed_by_player passes. A reload lost the blow too: theirs again, if they're about
			if (playerKill && (lastHurtByPlayer == null || lastHurtByPlayerTime <= 0)) {
				Player by = killer != null ? level().getPlayerByUUID(killer) : null;
				if (by != null && source == null)
					source = damageSources().playerAttack(by);
				// gone (logged off): a stand-in no one gets the credit of, so the Mainspring still drops (D30)
				setLastHurtByPlayer(by != null ? by : FakePlayerFactory.getMinecraft((ServerLevel) level()));
				lastHurtByPlayerTime = 100;
			}
			setHealth(0);
			die(source != null ? source : damageSources().generic());
			// paid: a save before he's gone (the next tick) must not hand the kill out again
			playerKill = false;
			killer = null;
			killingBlow = null;
		}
	}

	/** His Mainspring, loot and XP come out once, whatever saves and reloads land around his death. */
	@Override
	protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
		if (paidOut)
			return;
		paidOut = true;
		super.dropAllDeathLoot(level, source);
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
		if (d > CLOSE - 1.5 && turnToward(target.position(), 5)) {
			if (safeAhead(forward(), 1)) {
				walkForward();
				return;
			}
			// a wall too tall to step, or a gap: he springs over it, to come down by them
			if (tryHop(target))
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
			for (int dy = 3; dy <= 6; dy++)
				if (solid(base.offset(0, dy, 0)) && !crumbles(base.offset(0, dy, 0)))
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

	/** Living things in a box he can hurt ({@link #fairGame}). */
	private List<LivingEntity> victims(AABB box) {
		return level().getEntitiesOfClass(LivingEntity.class, box, this::fairGame);
	}

	/**
	 * D27: what his blows and his cog may hurt: players not in creative or spectator, whatever he's targeting, and
	 * whatever has hurt him this fight (a pet, a golem). Villagers, golems, livestock and pets that never touched him are
	 * left be, however close.
	 */
	public boolean fairGame(LivingEntity e) {
		return e != this && e.isAlive() && !(e instanceof ClockworkJuggernaut)
			&& (e instanceof Player p ? fightable(p) : e == getTarget() || attackers.contains(e.getUUID()));
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
		crumbleAround(3);
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
		} else if (target instanceof Player && move == null) {
			// he never locks on to one (the user: that made him easy to cheese): whoever struck his back mid-move he rounds
			// on; and after every combo, and every few seconds of chasing, he goes for whoever's hurting him most
			Player round = roundOn == null ? null : level().getPlayerByUUID(roundOn);
			roundOn = null;
			if (round != null && fightable(round) && round.distanceTo(this) <= AGGRO) {
				target = round;
				rethink = false;
				rethinkTicks = 0;
			} else if (rethink || ++rethinkTicks >= RETHINK_TICKS) {
				rethink = false;
				rethinkTicks = 0;
				Player p = mostThreatening();
				if (p != null)
					target = p;
			}
		}
		if (target != getTarget()) {
			setTarget(target);
			planned = null; // a move for whoever he was after
			trappedAt = null;
			trappedTicks = 0;
		}
		return target;
	}

	/**
	 * Who he goes after next: what each player near him has dealt him lately counts most (twice over from behind), how
	 * near they are a little, and chance the rest, so it's never quite certain.
	 */
	@Nullable
	private Player mostThreatening() {
		return mostThreatening(level().players());
	}

	@Nullable
	private Player mostThreatening(List<? extends Player> players) {
		Player best = null;
		double bestScore = -1;
		for (Player p : players) {
			if (!fightable(p) || fallen.contains(p.getUUID()) || p.distanceToSqr(this) > AGGRO * AGGRO)
				continue;
			double score = threat.getOrDefault(p.getUUID(), 0f) + 6 * (1 - p.distanceTo(this) / AGGRO) + random.nextDouble() * 10;
			if (score > bestScore) {
				best = p;
				bestScore = score;
			}
		}
		return best;
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
		// whatever (but a player) strikes him is fair game for his blows from now on (D27)
		if (source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof Player) && attacker != this)
			attackers.add(attacker.getUUID());
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
		// who's hurting him draws him: blows from behind twice over, and he rounds on whoever lands one there mid-move
		if (hurt && source.getEntity() instanceof Player hitter) {
			boolean behind = fromBehind(source);
			threat.merge(hitter.getUUID(), amount * (behind ? 2 : 1), Float::sum);
			if (behind && move != null && hitter != getTarget())
				roundOn = hitter.getUUID();
		}
		if (hurt && open && !jammed && phase() == Phase.REWIND && clip().name().equals("rewind"))
			playClip("hit_open");
		else if (hurt && !open && phase() == Phase.WOUND && move == null)
			playClip("hit_wound");
		if (getHealth() <= floor + 0.01f) {
			if (stage() >= 3) {
				rememberKill(source);
				beginDying();
			} else
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

	/** The killing blow, and whether a player (or their pet) struck it and who, kept for the end of his death clip. */
	private void rememberKill(DamageSource source) {
		killingBlow = source;
		Player by = source.getEntity() instanceof Player p ? p : lastHurtByPlayer;
		playerKill = lastHurtByPlayerTime > 0 && by != null;
		killer = playerKill ? by.getUUID() : null;
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
	@SuppressWarnings("deprecation") // override-only API: overriding it is the intended use
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
	@SuppressWarnings("deprecation") // overriding is the intended use; callers go through EventHooks.finalizeMobSpawn
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
		SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data);
		// found in a ruin (D31): he stays kneeling until someone comes near (D32)
		if (reason == MobSpawnType.STRUCTURE) {
			ruin = true;
			home = blockPosition();
			goDormant();
			return out;
		}
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
		tag.putBoolean("Ruin", ruin);
		if (home != null)
			tag.putLong("Home", home.asLong());
		tag.putFloat("Toughness", toughness);
		tag.putInt("WipePending", wipePending);
		ListTag fallenTag = new ListTag();
		for (UUID id : fallen)
			fallenTag.add(NbtUtils.createUUID(id));
		tag.put("Fallen", fallenTag);
		tag.putInt("Rest", graceTicks);
		if (playerKill) {
			tag.putBoolean("PlayerKill", true);
			if (killer != null)
				tag.putUUID("Killer", killer);
		}
		if (paidOut)
			tag.putBoolean("PaidOut", true);
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
		ruin = tag.getBoolean("Ruin") && (phase == Phase.DORMANT || phase == Phase.STIRRING);
		// found in a ruin, he remembers it: placed from its template (which carries Ruin but no Home), where he kneels is home
		home = tag.contains("Home") ? BlockPos.of(tag.getLong("Home")) : ruin ? blockPosition() : null;
		toughness = tag.contains("Toughness") ? Math.max(1, tag.getFloat("Toughness")) : 1;
		fallen.clear();
		for (Tag id : tag.getList("Fallen", Tag.TAG_INT_ARRAY))
			fallen.add(NbtUtils.loadUUID(id));
		overdrive = tag.getBoolean("Overdrive") && phase != Phase.DORMANT && phase != Phase.STIRRING;
		// saved in his death clip: a player's kill is still theirs when he finishes dying (the blow itself is gone)
		playerKill = phase == Phase.DYING && tag.getBoolean("PlayerKill");
		killer = playerKill && tag.hasUUID("Killer") ? tag.getUUID("Killer") : null;
		killingBlow = null;
		paidOut = tag.getBoolean("PaidOut");
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
			case DYING -> {
				// saved after he died, before he was gone: he stays dead (and goes, next tick), never dies again
				if (getHealth() > 0 && !paidOut)
					beginDying();
				else
					setPhase(Phase.DYING);
			}
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

	/** Found kneeling in a ruin and not woken yet: he breaks out when he wakes (D61). */
	public boolean fromRuin() {
		return ruin;
	}

	/** Wakes him as a ruin one would be woken, to break out (game tests). */
	public void wakeFromRuin() {
		ruin = true;
		home = blockPosition();
		beginWaking("wake");
	}

	/** Springs at {@code target} now (game tests). */
	public void forceLeap(LivingEntity target) {
		setTarget(target);
		startMove(Move.LEAP, target);
	}

	/** Charges at {@code target} now (game tests). */
	public void forceCharge(LivingEntity target) {
		setTarget(target);
		startMove(Move.CHARGE, target);
	}

	/** Found in a ruin, kneeling at {@code home}, without breaking out when he wakes (game tests). */
	public void setRuinHome(BlockPos home) {
		this.home = home;
		lookForLegacyRuin();
	}

	/**
	 * Game tests: treat {@code box} as his ruin's pieces. A hall placed from its template by hand is no structure in the
	 * world, so the real check ({@link JuggernautRuinStructure#built}) can't see it.
	 */
	public void testRuinPieces(@Nullable net.minecraft.world.level.levelgen.structure.BoundingBox box) {
		testRuin = box == null ? null : box::isInside;
		lookForLegacyRuin();
	}

	/** Game tests: who of {@code players} he'd go after next. */
	@Nullable
	public Player nextTargetOf(List<? extends Player> players) {
		return mostThreatening(players);
	}

	/** Game tests: the move he'd pick against {@code target} now. */
	@Nullable
	public String pickFor(LivingEntity target) {
		Move m = pick(target, horizontalDistance(target), null);
		return m == null ? null : m.name();
	}

	/** Game tests: treat {@code field} as his ruin's field, asking it what it built as the real check would. */
	public void testRuinField(com.comedymonkey.createaddons.content.juggernaut.ruin.RuinField field) {
		testRuin = p -> field.built(level(), p);
		lookForLegacyRuin();
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
