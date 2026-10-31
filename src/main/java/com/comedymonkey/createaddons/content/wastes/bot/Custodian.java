package com.comedymonkey.createaddons.content.wastes.bot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipLibrary;
import com.comedymonkey.createaddons.content.wastes.CityRecord;
import com.comedymonkey.createaddons.content.wastes.CityVaultBlockEntity;
import com.comedymonkey.createaddons.content.wastes.ConduitPylonBlock;
import com.comedymonkey.createaddons.content.wastes.ConduitPylonBlockEntity;
import com.comedymonkey.createaddons.content.wastes.District;
import com.comedymonkey.createaddons.content.wastes.WallVentBlockEntity;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Custodian (D30, D31, D35, P32-P41; brief: "a robed priest on puppet strings"), the heart's guardian. It kneels
 * dark on its dais until the heart wakes (all four districts), then lights its lenses, rises and bows to the nearest
 * player before it fights. Four Conduit Pylons feed it: while any stands it takes no damage and mends 2 health a second
 * for each one standing (P34), arcs running from their crowns to the sockets on its back. Breaking a pylon turns it on
 * the breaker and calls two fight Scuttlers (eight a fight, P35); all four down, the shield drops. At half health it
 * overloads once (P36): 30% quicker, sparking, and one pylon re-lights. Its moves (P37): the slam, the shockwave ring,
 * the bolt volley and, overloaded, the chain arc; each lands at its clip's own impact time.
 * <p>
 * A broken pylon: wrath at the breaker, then it raises a hand and calls (call_scuttlers): at 0.8 s the two live wall vents
 * nearest that pylon burst (D57, D60), a Scuttler tumbling out of each. Unshielded, a hit makes it recoil; in lulls it
 * has its mannerisms (straightening a robe plate, mourning a broken pylon, counting on its palm, a glance at the vault),
 * the head turned to what it looks at by the code (D60). Its tether cables are its renderer's.
 * <p>
 * It never breaks a block and never leaves its hall (leashed to its dais). Left alone for 30 s (no player within 48
 * blocks), or with every fighter dead, it walks back to its dais, kneels, and everything resets: pylons, health, its
 * Scuttlers crumble (P39), the spent wall vents re-seal with new Scuttlers inside (D67). Its death, however it comes
 * (/kill too), opens the heart vault (D35); only a player's kill drops its own loot (P40). Nothing but a player can
 * hurt it (P38).
 */
public class Custodian extends CityBot {
	public static final String CLIPS = "/assets/comedymonkeysaddons/custodian/clips.json";

	public enum Phase {
		/** Kneeling dark on its dais. */
		DORMANT,
		/** The heart woke: lenses light top to bottom, it rises. */
		WAKING,
		/** The formal bow to the nearest player before it fights. */
		BOWING,
		/** Facing, walking, choosing its next move. */
		FIGHT,
		/** A move or a reaction under way (its clip). */
		MOVE,
		/** The fight's over (P39): walking back to its dais. */
		RETURNING,
		/** On its dais, kneeling; the pylons re-light one by one. */
		KNEELING,
		/** Its death clip: it kneels to the vault, its plates fall. */
		DYING
	}

	/** Its attacks (P37) and reactions (brief), each a clip; a move's damage lands at its clip's times. */
	enum Move {
		SLAM("slam", 1.15f), SHOCKWAVE("shockwave", 0.8f), VOLLEY("bolt_volley", 0.9f, 1.1f, 1.3f), CHAIN_ARC("chain_arc", 0.8f),
		/** A pylon broken: the vents burst as its raised hand snaps (the clip's key at 0.78 s). */
		CALL("call_scuttlers", 0.8f),
		WRATH("wrath"), SHIELD_DOWN("shield_down"), OVERLOAD("overload");

		final String clip;
		final float[] hits;

		Move(String clip, float... hits) {
			this.clip = clip;
			this.hits = hits;
		}
	}

	private static final EntityDataAccessor<Byte> PHASE = SynchedEntityData.defineId(Custodian.class, EntityDataSerializers.BYTE);
	private static final EntityDataAccessor<Boolean> SHIELDED = SynchedEntityData.defineId(Custodian.class, EntityDataSerializers.BOOLEAN);
	/** Tick count of the last hit its shield turned (the shell ripples). */
	private static final EntityDataAccessor<Integer> SHIELD_HIT = SynchedEntityData.defineId(Custodian.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> OVERLOADED = SynchedEntityData.defineId(Custodian.class, EntityDataSerializers.BOOLEAN);
	/** Game time (as an int) its shockwave ring started running out, or {@link #NO_RING}. */
	private static final EntityDataAccessor<Integer> RING_START = SynchedEntityData.defineId(Custodian.class, EntityDataSerializers.INT);
	/** Where the ring runs out from: the floor under it as it stamped. */
	private static final EntityDataAccessor<Vector3f> RING_AT = SynchedEntityData.defineId(Custodian.class, EntityDataSerializers.VECTOR3);
	/** Its dais and the way it faces kneeling there (its tether cables are laid out from them). */
	private static final EntityDataAccessor<Optional<BlockPos>> DAIS = SynchedEntityData.defineId(Custodian.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
	private static final EntityDataAccessor<Float> DAIS_YAW = SynchedEntityData.defineId(Custodian.class, EntityDataSerializers.FLOAT);
	/** What its head turns to in wrath (an entity's id, or -1), and in a mourn or a glance at the vault (a block). */
	private static final EntityDataAccessor<Integer> LOOK_ENTITY = SynchedEntityData.defineId(Custodian.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Optional<BlockPos>> LOOK_BLOCK = SynchedEntityData.defineId(Custodian.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
	@SuppressWarnings("unchecked")
	private static final EntityDataAccessor<Optional<BlockPos>>[] PYLON = new EntityDataAccessor[] {
		SynchedEntityData.defineId(Custodian.class, EntityDataSerializers.OPTIONAL_BLOCK_POS),
		SynchedEntityData.defineId(Custodian.class, EntityDataSerializers.OPTIONAL_BLOCK_POS),
		SynchedEntityData.defineId(Custodian.class, EntityDataSerializers.OPTIONAL_BLOCK_POS),
		SynchedEntityData.defineId(Custodian.class, EntityDataSerializers.OPTIONAL_BLOCK_POS) };

	// ---- the spec's numbers
	/** P38. */
	public static final float HEALTH = 400;
	/** P34: per standing pylon, a second. */
	public static final float MEND_PER_PYLON = 2;
	/** P37. */
	public static final float SLAM_DAMAGE = 20, SHOCKWAVE_DAMAGE = 12, BOLT_DAMAGE = 8, ARC_DAMAGE = 10;
	public static final double SLAM_REACH = 2, SHOCKWAVE_RADIUS = 8, VOLLEY_RANGE = 24, ARC_JUMP = 6;
	/** P38: +50% for each extra player within 32 blocks when it wakes. */
	public static final float EXTRA_PLAYER = 0.5f;
	public static final double PLAYERS_RANGE = 32;
	/** The boss bar within 32 blocks (task brief; P20's "brass" bar). */
	public static final double BAR_RANGE = 32;
	/** P39: no player within 48 blocks for 30 s. */
	public static final double ALONE_RANGE = 48;
	public static final int ALONE_TICKS = 600;
	/** It never leaves the hall: tethered this far from its dais (P38). */
	public static final double LEASH = 15;
	/** P36: 30% quicker overloaded. */
	public static final float OVERLOAD_SPEED = 1.3f;
	/** P35: two a broken pylon, eight a fight. */
	public static final int CALL = 2, CALL_MAX = 8;
	/** Its four pylons are within its hall: looked for this far round its dais. */
	static final int HALL = 16;
	/** D51: after a reset it wakes again when a player is this near and in its sight. */
	public static final double REWAKE_RANGE = 16;
	/** D56: walking home, this long without getting nearer and it appears on its dais. */
	public static final int STUCK_TICKS = 200;
	/** No shockwave ring running. */
	public static final int NO_RING = Integer.MIN_VALUE;

	// ---- tuning the spec doesn't give (see "Choices needing the user's agreement" in the progress notes)
	/** Ticks between moves. */
	static final int MOVE_GAP = 30;
	/** Degrees a tick it turns to face its target. */
	static final float TURN = 8;
	/** Targets a player this far off (the boss bar's range). */
	static final double AGGRO = 32;
	/**
	 * Blocks a tick the shockwave ring runs out at (D56). Provisional: nothing gives its speed; 0.5 (10 blocks a second)
	 * takes it the 8 blocks in 0.8 s, so it's spent by 1.6 s of the 2.4 s clip, as the robe plates fall back.
	 */
	public static final double RING_SPEED = 0.5;
	/**
	 * A called Scuttler appears this long after its vent starts to burst: the vent preview starts its vent_burst clip 0.3 s
	 * in, so it shoots out as the hatch blows at 0.45 s.
	 */
	public static final int VENT_DELAY = 6;
	/** It lands 1.25 blocks out from the vent's face (the vent preview, D59). */
	public static final double VENT_LANDING = 1.25;
	/** Provisional (the brief gives none for it): in a lull, a mannerism every 6 to 10 s, never the same twice running. */
	static final int MANNERISM_MIN = 120, MANNERISM_SPREAD = 80;
	/** Its mannerisms (brief); mourn needs a broken pylon to look at, glance_vault the heart vault. */
	static final List<String> MANNERISMS = List.of("straighten_plate", "mourn", "count", "glance_vault");
	/** A hit while walking: the recoil shows this long before its walk takes over again. */
	static final float HIT_HOLD = 0.5f;

	/** The overload's 30% on its walk. */
	private static final net.minecraft.resources.ResourceLocation OVERLOAD_ID = ComedyMonkeysCreateAddons.asResource("custodian_overload");

	private final ServerBossEvent bossBar = new ServerBossEvent(Component.translatable("entity.comedymonkeysaddons.custodian"),
		BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);

	// ---- saved
	@Nullable
	private BlockPos dais;
	private float daisYaw;
	private final List<BlockPos> pylons = new ArrayList<>();
	@Nullable
	private BlockPos vault;
	/** It has woken before (a reset leaves it kneeling until it's woken again). */
	private boolean woke;
	private float toughness = 1;
	private int called;
	private final Set<UUID> scuttlers = new HashSet<>();
	// ---- the fight (not saved: a reload mid-fight picks up fighting)
	private final boolean[] standing = new boolean[4];
	private boolean shieldWas, pylonsSeen;
	@Nullable
	private Move move;
	private int hitsDone, gap, alone, phaseTicks;
	private final Set<UUID> fighters = new HashSet<>(), fallen = new HashSet<>();
	@Nullable
	private LivingEntity wrathAt;
	private boolean wrathPending, shieldDownPending;
	/** Pylons broken that it hasn't called Scuttlers for yet (the call follows the wrath). */
	private final List<BlockPos> callFor = new ArrayList<>();
	/** Scuttlers on their way out of bursting vents: where, and when they come out. */
	private final List<VentCall> ventCalls = new ArrayList<>();
	@Nullable
	private UUID callBreaker;
	private int stillTicks, nextMannerism = MANNERISM_MIN;
	@Nullable
	private String lastMannerism;
	@Nullable
	private DamageSource killedBy;
	@Nullable
	private Player killer;
	/** Who its running ring has already hit (once each). */
	private final Set<UUID> ringHit = new HashSet<>();
	/** Walking home: the nearest it has got, and how long since it got nearer (D56). */
	private double bestHome = Double.MAX_VALUE;
	private int stuckTicks;

	public Custodian(EntityType<? extends Custodian> type, Level level) {
		super(type, level);
		xpReward = 100; // P40
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, HEALTH)
			.add(Attributes.MOVEMENT_SPEED, 0.18) // a measured walk (brief); see the progress notes
			.add(Attributes.ATTACK_DAMAGE, SLAM_DAMAGE)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1)
			.add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 1)
			.add(Attributes.FOLLOW_RANGE, ALONE_RANGE)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	public ClipLibrary clips() {
		return ClipLibrary.of(CLIPS);
	}

	@Override
	protected String dormantClip() {
		return "waking"; // its first frame: kneeling, head bowed, lenses dark
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(PHASE, (byte) Phase.DORMANT.ordinal());
		builder.define(SHIELDED, false);
		builder.define(SHIELD_HIT, -100);
		builder.define(OVERLOADED, false);
		builder.define(RING_START, NO_RING);
		builder.define(RING_AT, new Vector3f());
		builder.define(DAIS, Optional.empty());
		builder.define(DAIS_YAW, 0f);
		builder.define(LOOK_ENTITY, -1);
		builder.define(LOOK_BLOCK, Optional.empty());
		for (EntityDataAccessor<Optional<BlockPos>> p : PYLON)
			builder.define(p, Optional.empty());
	}

	// ======================================================================== state

	public Phase phase() {
		return Phase.values()[Mth.clamp(entityData.get(PHASE), 0, Phase.values().length - 1)];
	}

	private void setPhase(Phase phase) {
		entityData.set(PHASE, (byte) phase.ordinal());
		phaseTicks = 0;
	}

	/** Shielded: a pylon of its stands (P34). */
	public boolean shielded() {
		return entityData.get(SHIELDED);
	}

	/** Tick count its shield last turned a hit (the shell's ripple). */
	public int shieldHitAt() {
		return entityData.get(SHIELD_HIT);
	}

	public boolean overloaded() {
		return entityData.get(OVERLOADED);
	}

	/** Its pylons, as clients know them (for the arcs). */
	public List<BlockPos> pylonPositions() {
		List<BlockPos> out = new ArrayList<>();
		for (EntityDataAccessor<Optional<BlockPos>> p : PYLON)
			entityData.get(p).ifPresent(out::add);
		return out;
	}

	public float toughness() {
		return toughness;
	}

	public int scuttlersCalled() {
		return called;
	}

	@Nullable
	public BlockPos dais() {
		return dais;
	}

	/** Its dais as clients know it (for its cables). */
	@Nullable
	public BlockPos syncedDais() {
		return entityData.get(DAIS).orElse(null);
	}

	/** The way it faces kneeling on its dais. */
	public float daisYaw() {
		return entityData.get(DAIS_YAW);
	}

	/** What its head turns to in wrath: an entity (the breaker), or null. */
	@Nullable
	public Entity lookEntity() {
		int id = entityData.get(LOOK_ENTITY);
		return id < 0 ? null : level().getEntity(id);
	}

	/** What its head turns to in a mourn (a broken pylon) or a glance at the vault, or null. */
	@Nullable
	public BlockPos lookBlock() {
		return entityData.get(LOOK_BLOCK).orElse(null);
	}

	private float speed() {
		return overloaded() ? OVERLOAD_SPEED : 1;
	}

	@Override
	public void playClip(String name) {
		playClip(name, phase() == Phase.DYING ? 1 : speed(), 0);
	}

	// ======================================================================== the city's builder (phase 4) and waking

	/** Kneels it on its dais here, facing {@code yaw}: where it returns to after a fight (P39). */
	public void setDais(BlockPos pos, float yaw) {
		dais = pos.immutable();
		daisYaw = yaw;
		entityData.set(DAIS, Optional.of(dais));
		entityData.set(DAIS_YAW, yaw);
		setYRot(yaw);
		yBodyRot = yHeadRot = yaw;
	}

	/** Its four pylons (the city's builder knows them; otherwise it looks round its hall when it wakes). */
	public void setPylons(List<BlockPos> list) {
		pylons.clear();
		for (BlockPos p : list)
			if (pylons.size() < 4)
				pylons.add(p.immutable());
		syncPylons();
		pylonsSeen = true;
		for (int k = 0; k < pylons.size(); k++) {
			ConduitPylonBlockEntity be = ConduitPylonBlock.pylonAt(level(), pylons.get(k));
			standing[k] = be != null && be.isStanding();
		}
	}

	public List<BlockPos> pylons() {
		return List.copyOf(pylons);
	}

	/** The heart vault it kneels to when it dies (found round its hall; the builder may set it). */
	public void setVault(@Nullable BlockPos pos) {
		vault = pos == null ? null : pos.immutable();
	}

	private void syncPylons() {
		for (int k = 0; k < 4; k++)
			entityData.set(PYLON[k], k < pylons.size() ? Optional.of(pylons.get(k)) : Optional.empty());
	}

	/** Its pylons and the heart vault, bound to its city's heart, within its hall (loaded chunks only). */
	private void findHall() {
		if (!link.bound() || !pylons.isEmpty() && vault != null)
			return;
		BlockPos centre = dais != null ? dais : blockPosition();
		List<BlockPos> found = new ArrayList<>();
		BlockPos foundVault = null;
		for (int cx = (centre.getX() - HALL) >> 4; cx <= (centre.getX() + HALL) >> 4; cx++)
			for (int cz = (centre.getZ() - HALL) >> 4; cz <= (centre.getZ() + HALL) >> 4; cz++) {
				if (!level().hasChunk(cx, cz))
					continue;
				LevelChunk chunk = level().getChunk(cx, cz);
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					BlockPos p = be.getBlockPos();
					if (Math.abs(p.getX() - centre.getX()) > HALL || Math.abs(p.getZ() - centre.getZ()) > HALL || Math.abs(p.getY() - centre.getY()) > 8)
						continue;
					if (be instanceof ConduitPylonBlockEntity pylon && ours(pylon.cityLink().origin(), pylon.cityLink().district()))
						found.add(p.immutable());
					else if (be instanceof CityVaultBlockEntity v && v.isHeart() && ours(v.cityLink().origin(), District.HEART))
						foundVault = p.immutable();
				}
			}
		if (pylons.isEmpty()) {
			found.sort((a, b) -> Double.compare(a.distSqr(centre), b.distSqr(centre)));
			setPylons(found);
		}
		if (vault == null)
			vault = foundVault;
	}

	private boolean ours(@Nullable BlockPos origin, District district) {
		return origin != null && origin.equals(link.origin()) && district == District.HEART;
	}

	/**
	 * From an egg or a command (not the city): it has no city, so nothing would wake it; it kneels where it appeared
	 * and wakes at once (rises, bows, fights, unshielded: it has no pylons). Never on Peaceful.
	 */
	@Override
	public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
		net.minecraft.world.entity.MobSpawnType reason, @Nullable net.minecraft.world.entity.SpawnGroupData data) {
		net.minecraft.world.entity.SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data);
		if (!link.bound()) {
			setDais(blockPosition(), getYRot());
			makeDormant();
			wake();
		}
		return out;
	}

	/** The heart woke (all four districts, P11, D31): it wakes, unless it's dead or it's Peaceful (P41). */
	public void heartWoke() {
		wake();
	}

	@Override
	protected void catchUp() {
		// the heart woke while it was unloaded and it never has: it wakes now
		CityRecord record = record();
		if (dormant() && !woke && record != null && link.bound() && record.allAwake(link.origin()) && !record.custodianDead(link.origin()))
			wake();
	}

	/**
	 * D51: kneeling after a reset (it has woken before, the heart is still awake and it isn't dead), it wakes again,
	 * rising and bowing, when a fightable player comes within 16 blocks in its sight. Never on Peaceful.
	 */
	@Override
	protected void cityTick() {
		super.cityTick();
		if (!dormant() || !woke || phase() != Phase.DORMANT || tickCount % 10 != 0 || peaceful())
			return;
		CityRecord record = record();
		if (link.bound() && (record == null || !record.allAwake(link.origin()) || record.custodianDead(link.origin())))
			return;
		for (Player p : level().players())
			if (fightable(p) && p.distanceToSqr(this) <= REWAKE_RANGE * REWAKE_RANGE && hasLineOfSight(p)) {
				wake();
				return;
			}
	}

	/**
	 * D56: the world turned Peaceful mid-fight: it resets as it does when everyone leaves (walks home, kneels, every
	 * pylon re-lit, mended, its Scuttlers crumble) and sleeps. It wakes again as after any reset (D51).
	 */
	@Override
	protected void sleepForPeace() {
		switch (phase()) {
			case DORMANT, DYING, RETURNING, KNEELING -> {}
			default -> beginReset();
		}
	}

	@Override
	protected void onDormant() {
		if (dais == null && level() != null && !level().isClientSide)
			setDais(blockPosition(), getYRot());
		setPhase(Phase.DORMANT);
		bossBar.setVisible(false);
	}

	@Override
	protected void onWake() {
		woke = true;
		if (dais == null)
			setDais(blockPosition(), getYRot());
		findHall();
		// P38: +50% for each extra player within 32 blocks, as toughness (max health can't pass 1024)
		int players = level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(PLAYERS_RANGE), CityBot::fightable).size();
		toughness = 1 + EXTRA_PLAYER * Math.max(0, players - 1);
		CityRecord record = record();
		if (record != null && link.bound())
			record.wakeCustodian((ServerLevel) level(), link.origin()); // its pylons can be harmed from now (P33)
		setPhase(Phase.WAKING);
		playClip("waking"); // sound: lenses lighting, the rise
	}

	// ======================================================================== ticking

	@Override
	public void tick() {
		super.tick();
		yBodyRot = getYRot();
		yHeadRot = getYRot();
		if (!level().isClientSide)
			tickRing();
	}

	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		phaseTicks++;
		updateBossBar();
		if (dormant())
			return;
		if (phase() == Phase.DYING) {
			tickDying();
			return;
		}
		watchPylons();
		if (shielded() && tickCount % 20 == 0 && getHealth() < getMaxHealth())
			heal(MEND_PER_PYLON * standingCount());
		if (overloaded() && tickCount % 2 == 0)
			((ServerLevel) level()).sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(1.8), getZ(), 2, 0.5, 0.9, 0.5, 0.15);
		tickVentCalls();
		switch (phase()) {
			case WAKING -> {
				stopWalking();
				if (clipDone()) {
					setPhase(Phase.BOWING);
					Player p = nearestPlayer(AGGRO);
					if (p != null)
						faceNow(p.position());
					playClip("bow");
				}
			}
			case BOWING -> {
				stopWalking();
				if (clipDone())
					startFight();
			}
			case FIGHT -> tickFight();
			case MOVE -> tickMove();
			case RETURNING -> tickReturning();
			case KNEELING -> tickKneeling();
			default -> {}
		}
	}

	private void startFight() {
		setPhase(Phase.FIGHT);
		move = null;
		gap = MOVE_GAP;
		playClip("idle");
	}

	// ---- the pylons (P33-P36)

	private int standingCount() {
		int n = 0;
		for (int k = 0; k < pylons.size(); k++)
			if (standing[k])
				n++;
		return n;
	}

	/** Each tick: a pylon broken turns it on the breaker and calls Scuttlers; the last one down drops the shield. */
	private void watchPylons() {
		if (!pylonsSeen) {
			// loaded (or first seen): how they stand now is no news
			pylonsSeen = true;
			for (int k = 0; k < pylons.size(); k++) {
				ConduitPylonBlockEntity be = ConduitPylonBlock.pylonAt(level(), pylons.get(k));
				standing[k] = be != null && be.isStanding();
			}
			shieldWas = standingCount() > 0;
			entityData.set(SHIELDED, shieldWas);
			return;
		}
		for (int k = 0; k < pylons.size(); k++) {
			BlockPos at = pylons.get(k);
			if (!level().isLoaded(at))
				continue;
			ConduitPylonBlockEntity be = ConduitPylonBlock.pylonAt(level(), at);
			boolean now = be != null && be.isStanding();
			if (standing[k] && !now)
				pylonBroken(at, be != null ? be.lastHitter() : null);
			standing[k] = now;
		}
		boolean shield = standingCount() > 0;
		if (shield != shielded())
			entityData.set(SHIELDED, shield);
		if (shieldWas && !shield && phase() != Phase.KNEELING) {
			// all four down: the shield drops with a crack (P34)
			shieldDownPending = true;
			((ServerLevel) level()).sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(0.5), getZ(), 40, 0.8, 1.4, 0.8, 0.4);
			// sound: the shield's crack
		}
		shieldWas = shield;
	}

	private void pylonBroken(BlockPos at, @Nullable Player breaker) {
		if (phase() == Phase.RETURNING || phase() == Phase.KNEELING)
			return;
		// it turns on the breaker (P35); the wrath clip plays when it's free
		if (breaker != null && fightable(breaker)) {
			wrathAt = breaker;
			setTarget(breaker);
			fighters.add(breaker.getUUID());
		}
		// one wrath at a time: a pylon broken while it's already in one only adds to the call
		if (move != Move.WRATH)
			wrathPending = true;
		// it calls for this pylon once the wrath has played (call_scuttlers, the vents bursting at its 0.8 s)
		callFor.add(at.immutable());
		if (breaker != null && fightable(breaker))
			callBreaker = breaker.getUUID();
	}

	/** A Scuttler coming out of a bursting vent. */
	private record VentCall(BlockPos vent, long at, @Nullable UUID breaker) {}

	/**
	 * P35, D57, D60: two fight Scuttlers for a broken pylon, eight in a fight, each out of its own live wall vent, the two
	 * nearest the pylon; each vent bursts once and is left open until a reset re-seals it (D67). With too few live vents
	 * (one broken, or unloaded), the rest appear round the pylon's foot in smoke (D67).
	 */
	private void callScuttlers(BlockPos pylon, @Nullable UUID breaker) {
		ServerLevel level = (ServerLevel) level();
		List<WallVentBlockEntity> live = liveVents();
		live.sort((a, b) -> Double.compare(a.getBlockPos().distSqr(pylon), b.getBlockPos().distSqr(pylon)));
		for (int i = 0; i < CALL && called < CALL_MAX; i++) {
			called++;
			if (i < live.size()) {
				WallVentBlockEntity vent = live.get(i);
				vent.burst(); // sound: the hatch rattling, then blowing
				ventCalls.add(new VentCall(vent.getBlockPos(), level.getGameTime() + VENT_DELAY, breaker));
				continue;
			}
			Scuttler s = ModWastes.SCUTTLER.get().create(level);
			if (s == null)
				return;
			double a = random.nextDouble() * Mth.TWO_PI;
			s.moveTo(pylon.getX() + 0.5 + Math.cos(a) * 1.2, pylon.getY(), pylon.getZ() + 0.5 + Math.sin(a) * 1.2, random.nextFloat() * 360, 0);
			release(s, breaker);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, s.getX(), s.getY(0.5), s.getZ(), 6, 0.2, 0.2, 0.2, 0.02);
		}
	}

	/** Its city's live (unspent, heart awake) wall vents in its hall (loaded chunks only). */
	public List<WallVentBlockEntity> liveVents() {
		return vents(WallVentBlockEntity::isLive);
	}

	/**
	 * D67: a reset re-seals every spent wall vent of its hall (loaded) with a new Scuttler inside, live again (the heart is
	 * still awake); the number re-sealed.
	 */
	public int resealVents() {
		int n = 0;
		for (WallVentBlockEntity vent : vents(WallVentBlockEntity::isOpen))
			if (vent.reseal()) // sound: the hatch swinging up and latching
				n++;
		return n;
	}

	/** Its city's heart's wall vents in its hall (loaded chunks only) that pass {@code which}. */
	private List<WallVentBlockEntity> vents(java.util.function.Predicate<WallVentBlockEntity> which) {
		List<WallVentBlockEntity> out = new ArrayList<>();
		if (!link.bound())
			return out;
		BlockPos centre = dais != null ? dais : blockPosition();
		for (int cx = (centre.getX() - HALL) >> 4; cx <= (centre.getX() + HALL) >> 4; cx++)
			for (int cz = (centre.getZ() - HALL) >> 4; cz <= (centre.getZ() + HALL) >> 4; cz++) {
				if (!level().hasChunk(cx, cz))
					continue;
				for (BlockEntity be : level().getChunk(cx, cz).getBlockEntities().values()) {
					BlockPos p = be.getBlockPos();
					if (be instanceof WallVentBlockEntity vent && which.test(vent) && Math.abs(p.getX() - centre.getX()) <= HALL
						&& Math.abs(p.getZ() - centre.getZ()) <= HALL && Math.abs(p.getY() - centre.getY()) <= 8
						&& ours(vent.cityLink().origin(), vent.cityLink().district()))
						out.add(vent);
				}
			}
		return out;
	}

	/** The Scuttlers due out of their vents: each tumbles out (vent_burst), landing 1.25 blocks out from the vent's face. */
	private void tickVentCalls() {
		if (ventCalls.isEmpty())
			return;
		ServerLevel level = (ServerLevel) level();
		for (java.util.Iterator<VentCall> it = ventCalls.iterator(); it.hasNext();) {
			VentCall call = it.next();
			if (level.getGameTime() < call.at())
				continue;
			it.remove();
			net.minecraft.world.level.block.state.BlockState state = level.getBlockState(call.vent());
			if (!state.hasProperty(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING))
				continue;
			net.minecraft.core.Direction out = state.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
			Scuttler s = ModWastes.SCUTTLER.get().create(level);
			if (s == null)
				continue;
			Vec3 at = Vec3.atBottomCenterOf(call.vent()).add(out.getStepX() * (0.5 + VENT_LANDING), 0, out.getStepZ() * (0.5 + VENT_LANDING));
			s.moveTo(at.x, at.y, at.z, out.toYRot(), 0);
			s.setYHeadRot(out.toYRot());
			s.yBodyRot = out.toYRot();
			release(s, call.breaker());
			s.burstFromVent();
		}
	}

	/** A called Scuttler into the world, after the breaker. */
	private void release(Scuttler s, @Nullable UUID breaker) {
		s.makeFightScuttler(this);
		Player p = breaker == null ? null : level().getPlayerByUUID(breaker);
		if (p != null && fightable(p))
			s.setTarget(p);
		level().addFreshEntity(s);
		scuttlers.add(s.getUUID());
	}

	/** Its fight Scuttlers fall to pieces (it died or reset, P35). */
	private void crumbleScuttlers() {
		UUID me = getUUID();
		for (Scuttler s : ((ServerLevel) level()).getEntities(EntityTypeTest.forClass(Scuttler.class), s -> s.isFightScuttler() && me.equals(s.custodian())))
			s.crumble();
		scuttlers.clear();
		ventCalls.clear();
		callFor.clear();
	}

	// ---- the fight

	private void tickFight() {
		LivingEntity target = updateTarget();
		if (checkReset())
			return;
		if (wrathPending) {
			wrathPending = false;
			if (wrathAt != null && wrathAt.isAlive())
				faceNow(wrathAt.position());
			// D60: its head snaps to the breaker, and the code keeps it on them
			entityData.set(LOOK_ENTITY, wrathAt != null && wrathAt.isAlive() ? wrathAt.getId() : -1);
			startMove(Move.WRATH);
			return;
		}
		if (!callFor.isEmpty()) {
			// it turns to the breaker and raises a hand: the vents burst (brief: Calls Scuttlers)
			if (wrathAt != null && wrathAt.isAlive())
				faceNow(wrathAt.position());
			startMove(Move.CALL);
			return;
		}
		if (shieldDownPending) {
			shieldDownPending = false;
			startMove(Move.SHIELD_DOWN);
			return;
		}
		if (target == null) {
			stopWalking();
			lull();
			return;
		}
		if (gap > 0)
			gap--;
		double d = horizontalDistance(target);
		boolean facing = turnToward(target.position());
		if (gap <= 0 && facing) {
			Move next = choose(target, d);
			if (next != null) {
				startMove(next);
				return;
			}
		}
		if (walkToward(target, d))
			stillTicks = 0;
		else
			lull();
	}

	/**
	 * Standing in the fight with no move to make (no one in reach, or no one at all): idle, and now and then one of its
	 * mannerisms (brief: "in lulls, never twice in a row"). Provisional: a lull is any stretch of standing so.
	 */
	private void lull() {
		if (++stillTicks < nextMannerism || !playing("idle")) {
			playIfFree("idle");
			return;
		}
		stillTicks = 0;
		nextMannerism = MANNERISM_MIN + random.nextInt(MANNERISM_SPREAD);
		List<String> choices = new ArrayList<>();
		List<BlockPos> broken = new ArrayList<>();
		for (int k = 0; k < pylons.size(); k++)
			if (!standing[k])
				broken.add(pylons.get(k));
		for (String m : MANNERISMS)
			if (!m.equals(lastMannerism) && (!m.equals("mourn") || !broken.isEmpty()) && (!m.equals("glance_vault") || vault != null))
				choices.add(m);
		if (choices.isEmpty()) {
			playIfFree("idle");
			return;
		}
		String pick = choices.get(random.nextInt(choices.size()));
		lastMannerism = pick;
		// D60: the code turns its head to what it looks at
		if (pick.equals("mourn"))
			entityData.set(LOOK_BLOCK, Optional.of(broken.get(random.nextInt(broken.size()))));
		else if (pick.equals("glance_vault"))
			entityData.set(LOOK_BLOCK, Optional.of(vault));
		playClip(pick); // sound: a plate's scrape, a hum, a tick of fingertips
	}

	/** Which move for a target this far off (the spec gives the moves, not when each is used: see the progress notes). */
	@Nullable
	private Move choose(LivingEntity target, double d) {
		double front = getBbWidth() / 2;
		if (overloaded() && d <= VOLLEY_RANGE && hasLineOfSight(target) && random.nextInt(3) == 0)
			return Move.CHAIN_ARC;
		if (d <= front + SLAM_REACH)
			return Move.SLAM;
		if (d <= SHOCKWAVE_RADIUS && target.onGround())
			return Move.SHOCKWAVE;
		if (d <= VOLLEY_RANGE && hasLineOfSight(target))
			return Move.VOLLEY;
		return null;
	}

	private void startMove(Move m) {
		stopWalking();
		stillTicks = 0;
		move = m;
		hitsDone = 0;
		setPhase(Phase.MOVE);
		playClip(m.clip); // sound: each move's wind-up
	}

	private void tickMove() {
		stopWalking();
		LivingEntity target = getTarget();
		float t = clipElapsed();
		if (move == Move.VOLLEY && target != null)
			turnToward(target.position());
		if (move == Move.CHAIN_ARC && t > 0.4f && t < 0.8f && tickCount % 2 == 0)
			((ServerLevel) level()).sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(0.35), getZ(), 6, 0.6, 0.6, 0.6, 0.2);
		if (move == Move.CALL && target != null)
			turnToward(target.position());
		while (move != null && hitsDone < move.hits.length && t >= move.hits[hitsDone]) {
			land(move, target);
			hitsDone++;
		}
		if (clipDone()) {
			gap = MOVE_GAP;
			setPhase(Phase.FIGHT);
			move = null;
			playClip("idle");
		}
	}

	private void land(Move m, @Nullable LivingEntity target) {
		switch (m) {
			case SLAM -> slam();
			case SHOCKWAVE -> shockwave();
			case VOLLEY -> {
				if (target != null) {
					Vec3 fwd = forward(), right = new Vec3(-fwd.z, 0, fwd.x).scale(-1);
					Vec3 palm = position().add(fwd.scale(0.9)).add(right.scale(0.55)).add(0, 2.0, 0);
					StarlightBolt.fire(this, palm, target.getBoundingBox().getCenter(), BOLT_DAMAGE);
				}
			}
			case CHAIN_ARC -> chainArc();
			case CALL -> {
				UUID breaker = callBreaker;
				for (BlockPos pylon : List.copyOf(callFor))
					callScuttlers(pylon, breaker);
				callFor.clear();
			}
			default -> {}
		}
	}

	/** P37: 20 to whoever is within 2 blocks in front of it, as the forearms land. Never a block. */
	private void slam() {
		ServerLevel level = (ServerLevel) level();
		Vec3 at = position().add(forward().scale(getBbWidth() / 2 + 1.2));
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 0.1, at.z, 20, 0.6, 0.05, 0.6, 0.2);
		// sound: the slam's impact
		for (Player p : victims(SLAM_REACH + getBbWidth() / 2 + 0.5))
			if (horizontalDistance(p) <= getBbWidth() / 2 + SLAM_REACH && inFront(p, 60) && Math.abs(p.getY() - getY()) < 2.5)
				hit(p, SLAM_DAMAGE, forward().scale(0.6).add(0, 0.3, 0));
	}

	/**
	 * P37, D56: the stamp sends a starlight ring running out 8 blocks along the floor from its feet ({@link #RING_SPEED});
	 * it hits each player on the ground once, for 12, as its crest passes them. Jump it to dodge.
	 */
	private void shockwave() {
		ServerLevel level = (ServerLevel) level();
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 0.1, getZ(), 16, getBbWidth() / 2, 0.02, getBbWidth() / 2, 0.1);
		// sound: the stamp and the ring
		ringHit.clear();
		entityData.set(RING_AT, new Vector3f((float) getX(), (float) getY(), (float) getZ()));
		entityData.set(RING_START, (int) level.getGameTime());
	}

	/** The ring's crest this many ticks after the stamp: from its feet's edge outward. */
	public double ringRadius(double age) {
		return getBbWidth() / 2 + RING_SPEED * age;
	}

	/** How far the ring runs: 8 blocks beyond its feet. */
	public double ringEnd() {
		return getBbWidth() / 2 + SHOCKWAVE_RADIUS;
	}

	/** Ticks since its ring started (with the partial tick), or -1 with none running. */
	public double ringAge(float partialTick) {
		int start = entityData.get(RING_START);
		if (start == NO_RING)
			return -1;
		double age = (int) level().getGameTime() - start + partialTick;
		return age < 0 || ringRadius(age) > ringEnd() ? -1 : age;
	}

	/** Where its ring runs out from. */
	public Vec3 ringCentre() {
		Vector3f c = entityData.get(RING_AT);
		return new Vec3(c.x, c.y, c.z);
	}

	/** The running ring, a tick on: every fightable player on the ground its crest passed this tick takes the hit. */
	private void tickRing() {
		int start = entityData.get(RING_START);
		if (start == NO_RING)
			return;
		int age = (int) level().getGameTime() - start;
		double r0 = ringRadius(age - 1), r1 = Math.min(ringRadius(age), ringEnd());
		if (r0 >= ringEnd()) {
			entityData.set(RING_START, NO_RING);
			ringHit.clear();
			return;
		}
		Vec3 c = ringCentre();
		double reach = ringEnd() + 1;
		for (Player p : level().getEntitiesOfClass(Player.class, new AABB(c.x - reach, c.y - 2, c.z - reach, c.x + reach, c.y + 3, c.z + reach), CityBot::fightable)) {
			if (ringHit.contains(p.getUUID()))
				continue;
			double dx = p.getX() - c.x, dz = p.getZ() - c.z, d = Math.sqrt(dx * dx + dz * dz), half = p.getBbWidth() / 2;
			if (d + half < r0 || d - half > r1)
				continue; // the crest isn't at them this tick
			if (!p.getRootVehicle().onGround() || Math.abs(p.getY() - c.y) > 1.5)
				continue; // in the air (jumped it), or not on its floor
			ringHit.add(p.getUUID());
			Vec3 out = d < 1e-3 ? forward() : new Vec3(dx / d, 0, dz / d);
			hit(p, SHOCKWAVE_DAMAGE, out.scale(0.5).add(0, 0.6, 0));
		}
	}

	/** P37: 10 to the nearest player, and the arc jumps once more to a player within 6 blocks of them. */
	private void chainArc() {
		Player first = nearestPlayer(AGGRO);
		if (first == null)
			return;
		Vec3 from = position().add(0, 2.2, 0);
		arc(from, first.getBoundingBox().getCenter());
		hit(first, ARC_DAMAGE, Vec3.ZERO);
		Player second = null;
		double best = ARC_JUMP * ARC_JUMP;
		for (Player p : victims(AGGRO))
			if (p != first && p.distanceToSqr(first) <= best) {
				best = p.distanceToSqr(first);
				second = p;
			}
		if (second != null) {
			arc(first.getBoundingBox().getCenter(), second.getBoundingBox().getCenter());
			hit(second, ARC_DAMAGE, Vec3.ZERO);
		}
		// sound: the arc's crack
	}

	private void arc(Vec3 a, Vec3 b) {
		ServerLevel level = (ServerLevel) level();
		int n = (int) Math.ceil(a.distanceTo(b) * 3);
		for (int i = 0; i <= n; i++) {
			Vec3 p = a.lerp(b, i / (double) n);
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
		}
	}

	private void hit(Player victim, float damage, Vec3 push) {
		victim.stopRiding();
		victim.hurt(damageSources().mobAttack(this), damage);
		if (push.lengthSqr() > 0) {
			victim.push(push.x, push.y, push.z);
			victim.hurtMarked = true;
		}
	}

	/** Real players near it (P37's moves hit players). */
	private List<Player> victims(double radius) {
		return level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(radius, 3, radius), CityBot::fightable);
	}

	// ---- overload (P36)

	private void overload() {
		entityData.set(OVERLOADED, true);
		var speed = getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null && !speed.hasModifier(OVERLOAD_ID))
			speed.addTransientModifier(new AttributeModifier(OVERLOAD_ID, OVERLOAD_SPEED - 1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		// it re-lights one broken pylon: the shield's back until that one's broken again
		List<Integer> broken = new ArrayList<>();
		for (int k = 0; k < pylons.size(); k++)
			if (!standing[k])
				broken.add(k);
		if (!broken.isEmpty()) {
			int k = broken.get(random.nextInt(broken.size()));
			ConduitPylonBlockEntity be = ConduitPylonBlock.pylonAt(level(), pylons.get(k));
			if (be != null && be.relight())
				standing[k] = true;
		}
		// the heart's lamps surge and flicker for about 3 s, then settle (P36, D54)
		CityRecord record = record();
		if (record != null && link.bound())
			record.surgeHeart((ServerLevel) level(), link.origin());
		startMove(Move.OVERLOAD); // sound: the long electric howl
	}

	// ---- damage (P34, P38, D35)

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
		// /kill and the void end it (D35: the vault still opens); nothing else but a player can hurt it (P38)
		if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !fromPlayer(source))
			return super.hurt(source, amount);
		// D52: kneeling (asleep, or kneeling home after a reset) nothing touches it
		if (level().isClientSide || dormant() || phase() == Phase.KNEELING || isInvulnerableTo(source) || phase() == Phase.DYING
			|| !fromPlayer(source))
			return false;
		if (source.getEntity() instanceof Player p) {
			fighters.add(p.getUUID());
			fallen.remove(p.getUUID());
		}
		if (shielded()) {
			// the shell ripples; it doesn't flinch (brief)
			entityData.set(SHIELD_HIT, tickCount);
			Vec3 at = source.getSourcePosition() != null ? source.getSourcePosition() : position().add(0, 1.5, 0);
			((ServerLevel) level()).sendParticles(ParticleTypes.ELECTRIC_SPARK, (at.x + getX()) / 2, Math.max(getY() + 0.5, at.y), (at.z + getZ()) / 2, 4, 0.1, 0.1, 0.1, 0.1);
			// sound: the shield's ring
			return false;
		}
		float dealt = Math.min(amount / toughness, Math.max(0, getHealth() - 1));
		boolean hurt = super.hurt(source, dealt);
		if (getHealth() <= 1.01f && hurt)
			beginDying(source);
		else if (hurt && !overloaded() && getHealth() <= getMaxHealth() / 2 && phase() != Phase.DYING)
			overload();
		else if (hurt && phase() == Phase.FIGHT)
			// the struck side recoils, a plate rattles (brief; D67: the left-side clip once it's in); sound: a clang
			playClip(hitClip(source, position(), facing(yBodyRot), new Vec3(0, 1, 0)));
		return hurt;
	}

	private void beginDying(DamageSource source) {
		stopWalking();
		move = null;
		killedBy = source;
		killer = source.getEntity() instanceof Player p ? p : lastHurtByPlayer;
		setPhase(Phase.DYING);
		// it turns its back to the heart vault: the death clip turns it round to kneel to it (brief)
		if (vault != null) {
			Vec3 away = position().subtract(Vec3.atCenterOf(vault));
			setYRot((float) (Mth.atan2(away.z, away.x) * Mth.RAD_TO_DEG) - 90);
		}
		playClip("death", 1, 0); // sound: the lenses going out, the plates clattering
		crumbleScuttlers();
	}

	private void tickDying() {
		stopWalking();
		// a player's kill: keep their credit through the long death (vanilla forgets it after 5 s)
		if (killer != null) {
			lastHurtByPlayer = killer;
			lastHurtByPlayerTime = 100;
		}
		if (clipDone()) {
			DamageSource source = killedBy != null ? killedBy : damageSources().generic();
			setHealth(0);
			die(source);
		}
	}

	@Override
	public void die(DamageSource source) {
		boolean already = dead;
		super.die(source);
		if (already || level().isClientSide)
			return;
		crumbleScuttlers();
		// any death opens the heart vault (D35)
		CityRecord record = record();
		if (record != null && link.bound())
			record.custodianDied((ServerLevel) level(), link.origin());
		if (fromPlayer(source))
			for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(PLAYERS_RANGE), CityBot::fightable))
				ModAdvancements.award(p, ModAdvancements.WASTES_CUSTODIAN_DOWN);
	}

	/** P40: its own loot (and experience) only for a player's kill; /kill and the void drop nothing. */
	@Override
	protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
		if (fromPlayer(source))
			super.dropAllDeathLoot(level, source);
	}

	@Override
	protected void tickDeath() {
		// its own death clip has already played; now it's gone
		if (!level().isClientSide && !isRemoved()) {
			level().broadcastEntityEvent(this, (byte) 60);
			remove(RemovalReason.KILLED);
		}
	}

	@Override
	public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
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

	// ---- the reset (P39)

	/** Everyone gone for 30 s, or every fighter dead: back to its dais. True if it's going. */
	private boolean checkReset() {
		alone = nearestPlayer(ALONE_RANGE) == null ? alone + 1 : 0;
		for (UUID id : fighters) {
			Player p = level().getPlayerByUUID(id);
			if (p != null && p.isDeadOrDying())
				fallen.add(id);
		}
		if (alone >= ALONE_TICKS || !fighters.isEmpty() && fallen.containsAll(fighters)) {
			beginReset();
			return true;
		}
		return false;
	}

	/** Walks back to its dais to kneel (P39; the game tests call it). */
	public void beginReset() {
		if (phase() == Phase.DYING || dormant())
			return;
		move = null;
		wrathPending = shieldDownPending = false;
		callFor.clear();
		ventCalls.clear();
		setTarget(null);
		setPhase(Phase.RETURNING);
		bestHome = Double.MAX_VALUE;
		stuckTicks = 0;
		if (dais != null)
			getNavigation().moveTo(dais.getX() + 0.5, dais.getY(), dais.getZ() + 0.5, 1);
	}

	private void tickReturning() {
		BlockPos home = dais != null ? dais : blockPosition();
		Vec3 to = Vec3.atBottomCenterOf(home);
		if (position().multiply(1, 0, 1).distanceTo(to.multiply(1, 0, 1)) < 0.6 && Math.abs(getY() - to.y) < 1.5) {
			stopWalking();
			moveTo(to.x, to.y, to.z, daisYaw, 0);
			setYRot(daisYaw);
			setPhase(Phase.KNEELING);
			playClip("kneel", 1, 0); // sound: kneeling
			return;
		}
		// D56: no nearer for 10 s (walled in, a pit, a path gone): it appears on its dais
		double d = position().distanceTo(to);
		if (d < bestHome - 0.25) {
			bestHome = d;
			stuckTicks = 0;
		} else if (++stuckTicks >= STUCK_TICKS) {
			appearHome(to);
			return;
		}
		if (getNavigation().isDone())
			getNavigation().moveTo(to.x, to.y, to.z, 1);
		playIfFree("walk");
	}

	/** Stuck on its way home (D56): gone in a puff of smoke, and back on its dais in another. */
	private void appearHome(Vec3 to) {
		ServerLevel level = (ServerLevel) level();
		level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(0.5), getZ(), 24, 0.5, 1.2, 0.5, 0.02);
		stopWalking();
		moveTo(to.x, to.y, to.z, daisYaw, 0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, to.x, to.y + 1.5, to.z, 24, 0.5, 1.2, 0.5, 0.02);
		// sound: a hiss of steam, twice
		stuckTicks = 0;
		bestHome = 0;
	}

	private void tickKneeling() {
		stopWalking();
		// the pylons re-light one by one as it kneels (brief)
		float t = clipElapsed();
		for (int k = 0; k < pylons.size(); k++)
			if (t >= 0.6f + 0.5f * k && !standing[k]) {
				ConduitPylonBlockEntity be = ConduitPylonBlock.pylonAt(level(), pylons.get(k));
				if (be != null) {
					be.reset();
					standing[k] = true;
				}
			}
		if (clipDone())
			finishReset();
	}

	private void finishReset() {
		for (int k = 0; k < pylons.size(); k++) {
			ConduitPylonBlockEntity be = ConduitPylonBlock.pylonAt(level(), pylons.get(k));
			if (be != null) {
				be.reset();
				standing[k] = true;
			}
		}
		shieldWas = standingCount() > 0;
		entityData.set(SHIELDED, shieldWas);
		setHealth(getMaxHealth());
		crumbleScuttlers();
		// D67: every spent vent shuts again with a new Scuttler behind its hatch, ready for the next fight
		resealVents();
		// a new fight next time: Scuttlers to call and the overload again (see the progress notes)
		called = 0;
		entityData.set(OVERLOADED, false);
		var speed = getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null)
			speed.removeModifier(OVERLOAD_ID);
		fighters.clear();
		fallen.clear();
		alone = 0;
		CityRecord record = record();
		if (record != null && link.bound())
			record.custodianReset((ServerLevel) level(), link.origin()); // the vault stays locked
		makeDormant();
	}

	// ---- targets, facing, walking

	@Nullable
	private Player nearestPlayer(double range) {
		Player best = null;
		double bestD = range * range;
		for (Player p : level().players())
			if (fightable(p) && !fallen.contains(p.getUUID()) && p.distanceToSqr(this) < bestD) {
				best = p;
				bestD = p.distanceToSqr(this);
			}
		return best;
	}

	@Nullable
	private LivingEntity updateTarget() {
		LivingEntity target = getTarget();
		if (target != null && (!target.isAlive() || target.level() != level() || target.distanceTo(this) > ALONE_RANGE
			|| target instanceof Player p && !fightable(p)))
			target = null;
		if (target == null) {
			Player p = nearestPlayer(AGGRO);
			if (p != null) {
				target = p;
				fighters.add(p.getUUID());
			}
		}
		if (target != getTarget())
			setTarget(target);
		return target;
	}

	/** Walks toward the target (true), or stands where it is (false): already in reach, or as near as its leash lets it. */
	private boolean walkToward(LivingEntity target, double d) {
		if (d <= getBbWidth() / 2 + SLAM_REACH - 0.3) {
			stopWalking();
			return false;
		}
		Vec3 to = target.position();
		// leashed: never past LEASH blocks from its dais (P38)
		if (dais != null) {
			Vec3 home = Vec3.atBottomCenterOf(dais);
			Vec3 off = to.subtract(home).multiply(1, 0, 1);
			if (off.length() > LEASH)
				to = home.add(off.normalize().scale(LEASH)).add(0, to.y - home.y, 0);
		}
		if (position().distanceToSqr(to) < 1) {
			stopWalking();
			return false;
		}
		getNavigation().moveTo(to.x, to.y, to.z, 1);
		if (!playing("walk") && !(playingHit() && clipElapsed() < HIT_HOLD))
			playClip("walk", speed(), 0);
		return true;
	}

	private void stopWalking() {
		getNavigation().stop();
		setZza(0);
		setDeltaMovement(0, getDeltaMovement().y, 0);
	}

	/** Turns toward a point; true once it faces it (within 20 degrees). */
	private boolean turnToward(Vec3 at) {
		double dx = at.x - getX(), dz = at.z - getZ();
		if (dx * dx + dz * dz < 1e-4)
			return true;
		float want = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90;
		float diff = Mth.wrapDegrees(want - getYRot());
		setYRot(getYRot() + Mth.clamp(diff, -TURN * speed(), TURN * speed()));
		yBodyRot = yHeadRot = getYRot();
		return Math.abs(diff) < 20;
	}

	private void faceNow(Vec3 at) {
		double dx = at.x - getX(), dz = at.z - getZ();
		if (dx * dx + dz * dz > 1e-4)
			setYRot((float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90);
		yBodyRot = yHeadRot = getYRot();
	}

	Vec3 forward() {
		float r = getYRot() * Mth.DEG_TO_RAD;
		return new Vec3(-Mth.sin(r), 0, Mth.cos(r));
	}

	private double horizontalDistance(Entity e) {
		double dx = e.getX() - getX(), dz = e.getZ() - getZ();
		return Math.sqrt(dx * dx + dz * dz);
	}

	private boolean inFront(Entity e, double halfAngle) {
		Vec3 to = e.position().subtract(position()).multiply(1, 0, 1);
		return to.lengthSqr() < 1e-4 || to.normalize().dot(forward()) >= Math.cos(Math.toRadians(halfAngle));
	}

	/** World position of one of its back sockets, standing (for the arcs; the renderer has its own posed copy). */
	public Vec3 socket(int k) {
		// sockets 1-4 on its back (generator: x -2.5 / 2.5, y 30 / 27, z 3.5 px), Blockbench frame to the world
		double x = k % 2 == 0 ? -2.5 : 2.5, y = k < 2 ? 30 : 27;
		Vec3 local = new Vec3(-x / 16, y / 16, -4 / 16.0);
		return position().add(local.yRot(-getYRot() * Mth.DEG_TO_RAD));
	}

	// ---- the boss bar

	private void updateBossBar() {
		bossBar.setProgress(getHealth() / getMaxHealth());
		bossBar.setVisible(!dormant());
		if (tickCount % 20 != 0)
			return;
		for (ServerPlayer p : new ArrayList<>(bossBar.getPlayers()))
			if (p.distanceToSqr(this) > BAR_RANGE * BAR_RANGE || p.level() != level())
				bossBar.removePlayer(p);
		for (Player p : level().players())
			if (p instanceof ServerPlayer sp && p.distanceToSqr(this) <= BAR_RANGE * BAR_RANGE)
				bossBar.addPlayer(sp);
	}

	@Override
	public void remove(RemovalReason reason) {
		super.remove(reason);
		bossBar.removeAllPlayers();
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

	@Override
	public AABB getBoundingBoxForCulling() {
		if (ringAge(0) >= 0) {
			// its shockwave ring runs out 8 blocks along the floor
			Vec3 c = ringCentre();
			double r = ringEnd() + 0.5;
			return getBoundingBox().inflate(1.5, 0.5, 1.5).minmax(new AABB(c.x - r, c.y - 0.5, c.z - r, c.x + r, c.y + 1, c.z + r));
		}
		return getBoundingBox().inflate(1.5, 0.5, 1.5); // its arms in a slam, its plates flared; the arcs draw their own way
	}

	// ======================================================================== saving

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		if (dais != null)
			tag.put("Dais", NbtUtils.writeBlockPos(dais));
		tag.putFloat("DaisYaw", daisYaw);
		ListTag list = new ListTag();
		for (BlockPos p : pylons)
			list.add(LongTag.valueOf(p.asLong()));
		tag.put("Pylons", list);
		if (vault != null)
			tag.put("Vault", NbtUtils.writeBlockPos(vault));
		tag.putBoolean("Woke", woke);
		tag.putFloat("Toughness", toughness);
		tag.putInt("Called", called);
		tag.putBoolean("Overloaded", overloaded());
		tag.putString("Phase", phase().name());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		dais = NbtUtils.readBlockPos(tag, "Dais").orElse(null);
		daisYaw = tag.getFloat("DaisYaw");
		entityData.set(DAIS, Optional.ofNullable(dais));
		entityData.set(DAIS_YAW, daisYaw);
		pylons.clear();
		for (Tag t : tag.getList("Pylons", Tag.TAG_LONG))
			pylons.add(BlockPos.of(((LongTag) t).getAsLong()));
		syncPylons();
		pylonsSeen = false; // looked at again on its first tick: a pylon found down then isn't a new break
		vault = NbtUtils.readBlockPos(tag, "Vault").orElse(null);
		woke = tag.getBoolean("Woke");
		toughness = Math.max(1, tag.contains("Toughness") ? tag.getFloat("Toughness") : 1);
		called = tag.getInt("Called");
		entityData.set(OVERLOADED, tag.getBoolean("Overloaded"));
		if (overloaded()) {
			var speed = getAttribute(Attributes.MOVEMENT_SPEED);
			if (speed != null && !speed.hasModifier(OVERLOAD_ID))
				speed.addTransientModifier(new AttributeModifier(OVERLOAD_ID, OVERLOAD_SPEED - 1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}
		if (hasCustomName())
			bossBar.setName(getDisplayName());
		if (dormant())
			return;
		// moves and walks don't survive a reload: it fights on (or finishes dying, or walks home again)
		Phase was;
		try {
			was = Phase.valueOf(tag.getString("Phase"));
		} catch (IllegalArgumentException e) {
			was = Phase.FIGHT;
		}
		switch (was) {
			case DYING -> beginDying(damageSources().generic());
			case RETURNING, KNEELING -> beginReset();
			default -> startFight();
		}
	}

	// ======================================================================== test hooks

	/** Straight into the fight, skipping the waking and the bow (game tests). */
	public void forceFight() {
		if (dormant()) {
			wake();
			if (dormant())
				return;
		}
		startFight();
	}

	public boolean hasWoken() {
		return woke;
	}

	/** Its shockwave, now (game tests): the move as it plays in a fight, its ring at the clip's stamp. */
	public void testShockwave() {
		startMove(Move.SHOCKWAVE);
	}

	/** Its boss bar's colour (D58: yellow, the nearest to brass). */
	public BossEvent.BossBarColor bossBarColor() {
		return bossBar.getColor();
	}
}
