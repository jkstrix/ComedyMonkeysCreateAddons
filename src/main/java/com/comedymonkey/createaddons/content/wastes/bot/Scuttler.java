package com.comedymonkey.createaddons.content.wastes.bot;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.tradebot.clip.ClipLibrary;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Scuttler (P18, P42; brief: "a tick with a camera for a face"): about half a block, 12 health, 5 a bite, fast; it
 * climbs walls like a spider and leaps. Asleep it's a curled lump of plates in the scrap (the wake clip's first frame);
 * when its district wakes it unrolls, lens lighting. Woken, its clips follow what it does (brief, Motion Set): it walks
 * (a tripod cycle in 0.45 s) and runs at a player (0.3 s), clings to walls facing up them, leaps; it freezes and crouches
 * when it first sees a player and stands hostile while it has one; a player it can't fight (creative, spectating) near
 * it makes it curious for a moment (D67); a hit flips it onto its back, legs paddling, until it rights itself with a hop
 * (at most once every 4 s, D67); a packmate's death turns every Scuttler near it to the body for a beat; it dies with its lens
 * cracking and its legs curling in. Standing with nothing to do it has its mannerisms, one every 4 to 8 s: wiping its
 * lens, sniffing the ground, chittering, stretching, picking at scrap (a spark), and huddling with a packmate beside it.
 * <p>
 * The Custodian calls fight Scuttlers out of the heart hall's wall vents (P35, D57): each tumbles out of its vent
 * ({@link #burstFromVent}); they drop nothing and crumble when it dies or resets.
 */
public class Scuttler extends CityBot {
	public static final String CLIPS = "/assets/comedymonkeysaddons/scuttler/clips.json";
	private static final EntityDataAccessor<Boolean> CLIMBING = SynchedEntityData.defineId(Scuttler.class, EntityDataSerializers.BOOLEAN);
	/** The wall it clings to (a horizontal direction's 2D value), or -1. */
	private static final EntityDataAccessor<Byte> CLIMB_WALL = SynchedEntityData.defineId(Scuttler.class, EntityDataSerializers.BYTE);

	/** P18, P42. */
	public static final float HEALTH = 12, DAMAGE = 5;
	/** The wake clip unrolls it over this long before it moves (brief: legs unfold one by one, 0.8 s, from 0.7 s in). */
	static final int WAKE_TICKS = 44;
	/** Seeing a player: the freeze, the lens snapping to them and the crouch (sees_player's keys at 0.2 and 0.45 s). */
	static final int SEES_TICKS = 12;
	/** Brief: a mannerism every 4 to 8 s. */
	static final int MANNERISM_MIN = 80, MANNERISM_SPREAD = 80;
	/** D60: a walking tripod cycle takes 0.45 s; the walk clip's is 0.3 s, so it plays at two thirds. Running: 0.3 s, as made. */
	public static final float WALK_SPEED = 0.3f / 0.45f;
	/** Hit: on its back until it has rolled upright with its hop (the flipped clip: over at once, righted 0.95-1.35 s). */
	public static final int FLIP_TICKS = 27;
	/** D67: a hit flips it at most once in 4 s (flip to flip); the hits between still hurt it (and knock it back). */
	public static final int FLIP_EVERY = 80;
	/** D67: a player it can't fight (creative, spectating) this near, in its sight, makes it curious. */
	public static final double CURIOUS_RANGE = 8;
	/**
	 * Provisional (choice: nothing gives it): curious this long, facing the player as they move (the clip springs into its
	 * pose and holds it), then idle...
	 */
	public static final int CURIOUS_TICKS = 60;
	/** ...and not curious again for this long after (provisional, a choice). */
	public static final int CURIOUS_COOLDOWN = 400;
	/** A packmate dies: the beat of grief (the grief clip's turn to the body, held 0.05-0.55 s), then hostile. */
	static final int GRIEF_TICKS = 11;
	/** Provisional (the brief says "every nearby Scuttler"): packmates this near see one die. */
	public static final double GRIEF_RANGE = 8;
	/** The grief clip turns its lens 40 degrees to its left: it faces so that look lands on the body. */
	static final float GRIEF_LOOK = 40;
	/** Provisional (the brief: "if two are close"): a packmate this near, standing idle, huddles with it. */
	public static final double HUDDLE_RANGE = 1.5;
	/** Called out of a wall vent (D57): its vent_burst clip plays out (out of the cavity, a somersault, righted, hostile). */
	public static final int VENT_TICKS = 48;
	/** pick_scrap: the second poke strikes a spark (the clip's second poke key). */
	static final float SPARK_AT = 1.0f;
	/** The mannerisms it has on its own (the huddle needs a packmate beside it). */
	static final List<String> MANNERISMS = List.of("wipe_lens", "sniff", "chitter", "stretch", "pick_scrap");

	/** Called out by the Custodian for its fight (P35). */
	private boolean fight;
	@Nullable
	private UUID custodian;
	private int busyTicks, mannerismTicks, nextMannerism = MANNERISM_MIN;
	@Nullable
	private LivingEntity lastTarget;
	/** A facing it holds while a clip needs it (a huddle, its grief), or NaN. */
	private float heldYaw = Float.NaN;
	private int heldTicks;
	private boolean sparked;
	/** Game time it was last flipped by a hit (D67: at most once every {@link #FLIP_EVERY}). */
	private long lastFlip = Long.MIN_VALUE / 2;
	/** Curious about a player it can't fight (D67): ticks left, ticks till it can be again, and who. */
	private int curiousTicks, curiousCooldown;
	@Nullable
	private Player curiousAbout;

	public Scuttler(EntityType<? extends Scuttler> type, Level level) {
		super(type, level);
		xpReward = 5; // P18
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, HEALTH)
			.add(Attributes.ATTACK_DAMAGE, DAMAGE)
			.add(Attributes.MOVEMENT_SPEED, 0.35); // "fast" (P18): a spider's 0.3 and a bit; see the progress notes
	}

	@Override
	public ClipLibrary clips() {
		return ClipLibrary.of(CLIPS);
	}

	@Override
	protected String dormantClip() {
		return "wake";
	}

	@Override
	protected String deathClip() {
		return "death"; // the lens cracks and goes out, the lid slams, the legs curl in (brief)
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CLIMBING, false);
		builder.define(CLIMB_WALL, (byte) -1);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4f) {
			@Override
			public void start() {
				super.start();
				playClip("leap"); // sound: the leap's spring
			}
		});
		goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, true));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
		goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	/** Climbs walls like a spider (P18). */
	@Override
	protected PathNavigation createNavigation(Level level) {
		return new WallClimberNavigation(this, level);
	}

	@Override
	public boolean onClimbable() {
		return entityData.get(CLIMBING);
	}

	/** The wall it's clinging to (its climb clip faces up it, the wall 6 px in front of it), or null. */
	@Nullable
	public Direction climbingWall() {
		byte d = entityData.get(CLIMB_WALL);
		return d < 0 || !onClimbable() ? null : Direction.from2DDataValue(d);
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide)
			return;
		boolean climbing = !dormant() && isAlive() && horizontalCollision;
		Direction wall = climbing ? wallAhead() : null;
		entityData.set(CLIMBING, climbing);
		entityData.set(CLIMB_WALL, (byte) (wall == null ? -1 : wall.get2DDataValue()));
		// a clip that needs it to face one way (a huddle, its grief) holds it there, whatever its look goals did
		if (!Float.isNaN(heldYaw)) {
			if (--heldTicks <= 0 || !isAlive())
				heldYaw = Float.NaN;
			else {
				setYRot(heldYaw);
				yBodyRot = yHeadRot = heldYaw;
			}
		}
	}

	/** Of the walls it's pressed against, the one most nearly ahead of it. */
	@Nullable
	private Direction wallAhead() {
		AABB box = getBoundingBox();
		Direction best = null;
		double bestDot = -2;
		Vec3 ahead = Vec3.directionFromRotation(0, getYRot());
		for (Direction d : Direction.Plane.HORIZONTAL) {
			if (level().noCollision(this, box.move(d.getStepX() * 0.05, 0, d.getStepZ() * 0.05)))
				continue;
			double dot = d.getStepX() * ahead.x + d.getStepZ() * ahead.z;
			if (dot > bestDot) {
				bestDot = dot;
				best = d;
			}
		}
		return best;
	}

	// ---- asleep and waking

	@Override
	protected void onWake() {
		playClip("wake"); // sound: unrolling, the lens lighting
		busyTicks = WAKE_TICKS;
	}

	/** The wake clip, the first sight of a player, on its back, a beat of grief, out of a vent: it stands still for them. */
	@Override
	protected boolean isImmobile() {
		return super.isImmobile() || busyTicks > 0;
	}

	@Override
	protected void cityTick() {
		super.cityTick();
		if (busyTicks > 0 && --busyTicks > 0)
			getNavigation().stop();
	}

	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		if (dormant())
			return;
		if (curiousCooldown > 0)
			curiousCooldown--;
		LivingEntity target = getTarget();
		// anything else it does ends its curiosity (a target, running, climbing, a hit)
		if (curiousTicks > 0 && (target != null || !playing("curious")))
			endCurious();
		// sees a player: freezes, the lens snaps to them and widens, it crouches (brief)
		if (target instanceof Player && target != lastTarget) {
			playClip("sees_player"); // sound: a chitter
			busyTicks = SEES_TICKS;
			lastTarget = target;
			heldYaw = Float.NaN;
			return;
		}
		lastTarget = target;
		if (playing("pick_scrap") && !sparked && clipElapsed() >= SPARK_AT) {
			sparked = true;
			spark();
		}
		if (playing("leap") && !clipDone())
			return;
		if (onClimbable()) {
			// clinging to a wall, facing up it
			if (!playing("climb"))
				playClip("climb");
			mannerismTicks = 0;
			return;
		}
		boolean moving = getDeltaMovement().horizontalDistanceSqr() > 1e-3 || getNavigation().isInProgress();
		if (moving) {
			// after a player it runs (0.3 s a cycle); otherwise it walks (0.45 s, D60)
			if (target != null) {
				if (!playing("run"))
					playClip("run");
			} else if (!playing("walk"))
				playClip("walk", WALK_SPEED, 0);
			mannerismTicks = 0;
			heldYaw = Float.NaN;
			return;
		}
		if (target != null) {
			// standing with a player in its sights: crouched, plates up, lens narrow (brief: Hostile)
			if (!playing("hostile") && (settled() || playing("run") || playing("walk") || playing("climb")))
				playClip("hostile");
			mannerismTicks = 0;
			return;
		}
		// D67: curious about a player it can't fight; it watches them, turning as they move, then goes back to idle
		if (curiousTicks > 0) {
			mannerismTicks = 0;
			if (--curiousTicks > 0 && curiousAbout != null && curiousOf(curiousAbout)) {
				hold(yawTo(curiousAbout.position()), 2);
				return;
			}
			endCurious();
			playClip("idle");
			return;
		}
		if (curiousCooldown <= 0 && playing("idle")) {
			Player p = curiousAbout();
			if (p != null) {
				beCurious(p);
				return;
			}
		}
		if (++mannerismTicks > nextMannerism && playing("idle")) {
			mannerismTicks = 0;
			nextMannerism = MANNERISM_MIN + random.nextInt(MANNERISM_SPREAD);
			mannerism();
		} else
			playIfFree("idle");
	}

	// ---- curious (D67)

	/** A player it can't fight (creative, spectating; a real one) within {@link #CURIOUS_RANGE} and in its sight. */
	boolean curiousOf(Player p) {
		return p.isAlive() && !p.isRemoved() && p.level() == level() && realPlayer(p) && (p.isCreative() || p.isSpectator())
			&& p.distanceToSqr(this) <= CURIOUS_RANGE * CURIOUS_RANGE && hasLineOfSight(p);
	}

	/** The nearest player it's curious about, or null. */
	@Nullable
	private Player curiousAbout() {
		Player best = null;
		double bestD = Double.MAX_VALUE;
		for (Player p : level().players())
			if (curiousOf(p) && p.distanceToSqr(this) < bestD) {
				bestD = p.distanceToSqr(this);
				best = p;
			}
		return best;
	}

	/** Curious (brief: aperture wide, lid up, head tilted, a front leg lifted, unsure), facing them. */
	private void beCurious(Player p) {
		curiousAbout = p;
		curiousTicks = CURIOUS_TICKS;
		mannerismTicks = 0;
		hold(yawTo(p.position()), 2);
		playClip("curious"); // sound: a soft rising chirr, the lens whirring wide
	}

	/** Its curiosity is over (played out, or something else came up): not again for {@link #CURIOUS_COOLDOWN}. */
	private void endCurious() {
		curiousTicks = 0;
		curiousAbout = null;
		curiousCooldown = CURIOUS_COOLDOWN;
		heldYaw = Float.NaN;
	}

	/** Curious about someone now (game tests). */
	public boolean curious() {
		return curiousTicks > 0;
	}

	/** Ticks till it can be curious again (game tests). */
	public int curiousCooldown() {
		return curiousCooldown;
	}

	/** The yaw that faces {@code at}. */
	private float yawTo(Vec3 at) {
		Vec3 d = at.subtract(position());
		return (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90;
	}

	/** Its clip has played out (or loops): something else can start. */
	private boolean settled() {
		return clip().mode() == com.comedymonkey.createaddons.content.tradebot.clip.Clip.Mode.LOOP || clipDone();
	}

	/** One of its mannerisms, at random; the huddle only with an idle packmate close beside it. */
	private void mannerism() {
		Scuttler mate = huddleMate();
		List<String> choices = new ArrayList<>(MANNERISMS);
		if (mate != null)
			choices.add("huddle");
		String pick = choices.get(random.nextInt(choices.size()));
		if (pick.equals("huddle")) {
			huddleWith(mate);
			return;
		}
		sparked = false;
		playClip(pick); // sound: a wipe, a sniff, a chitter, a creak, a spark
	}

	/** The nearest woken packmate standing idle within {@link #HUDDLE_RANGE}. */
	@Nullable
	private Scuttler huddleMate() {
		Scuttler best = null;
		double bestD = HUDDLE_RANGE * HUDDLE_RANGE;
		for (Scuttler s : level().getEntitiesOfClass(Scuttler.class, getBoundingBox().inflate(HUDDLE_RANGE), s -> s != this && s.isAlive()
			&& !s.dormant() && s.getTarget() == null && s.busyTicks <= 0 && s.playing("idle") && !s.onClimbable()))
			if (s.distanceToSqr(this) < bestD) {
				bestD = s.distanceToSqr(this);
				best = s;
			}
		return best;
	}

	/**
	 * Huddles with a packmate (brief: "they lean together, lenses touching"): both turn to face the same way, side by
	 * side, it with the packmate on its right (huddle_right), the packmate with it on its left (huddle_left).
	 */
	void huddleWith(Scuttler mate) {
		Vec3 d = mate.position().subtract(position());
		// the shared facing whose right (-cos, -sin of the yaw) points from it to the packmate
		float yaw = (float) (Mth.atan2(-d.z, -d.x) * Mth.RAD_TO_DEG);
		int ticks = Mth.ceil(clips().clip("huddle_right").length() * 20);
		hold(yaw, ticks);
		playClip("huddle_right"); // sound: a soft click of lenses
		mate.hold(yaw, ticks);
		mate.mannerismTicks = 0;
		mate.playClip("huddle_left");
	}

	private void hold(float yaw, int ticks) {
		heldYaw = yaw;
		heldTicks = ticks;
		setYRot(yaw);
		yBodyRot = yHeadRot = yaw;
		getNavigation().stop();
	}

	/** pick_scrap's spark: where its front left leg pokes the ground. */
	private void spark() {
		if (!(level() instanceof ServerLevel server))
			return;
		float r = getYRot() * Mth.DEG_TO_RAD;
		Vec3 ahead = new Vec3(-Mth.sin(r), 0, Mth.cos(r)), left = new Vec3(Mth.cos(r), 0, Mth.sin(r));
		Vec3 at = position().add(ahead.scale(0.45)).add(left.scale(0.3));
		server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 0.05, at.z, 3, 0.03, 0.02, 0.03, 0.05);
		// sound: a tick of metal on metal
	}

	// ---- hit, grief, the vent

	/**
	 * Gets hit (brief): flips onto its back, legs paddling, and rights itself with a hop before it goes on. D67: at most
	 * once every {@link #FLIP_EVERY} (4 s, flip to flip), so it can't be kept on its back; the hits between still hurt it.
	 */
	@Override
	public boolean hurt(DamageSource source, float amount) {
		boolean hurt = super.hurt(source, amount);
		if (hurt && !level().isClientSide && isAlive() && !dormant() && !playing("wake") && !playing("vent_burst")
			&& level().getGameTime() - lastFlip >= FLIP_EVERY) {
			lastFlip = level().getGameTime();
			playClip("flipped"); // sound: a clatter of plates
			busyTicks = FLIP_TICKS;
			heldYaw = Float.NaN;
		}
		return hurt;
	}

	/** Game time a hit last flipped it (game tests). */
	public long lastFlip() {
		return lastFlip;
	}

	/** A packmate's death: every woken Scuttler near it turns its lens to the body for a beat (brief). */
	@Override
	public void die(DamageSource source) {
		boolean already = dead;
		super.die(source);
		if (already || level().isClientSide)
			return;
		for (Scuttler s : level().getEntitiesOfClass(Scuttler.class, getBoundingBox().inflate(GRIEF_RANGE),
			s -> s != this && s.isAlive() && !s.dormant() && s.distanceToSqr(this) <= GRIEF_RANGE * GRIEF_RANGE))
			s.grieve(position());
	}

	/** Its lens turns to a fallen packmate's body for a beat, then it goes on, hostile (a look: no stat changes). */
	void grieve(Vec3 body) {
		if (playing("wake") || playing("vent_burst") || playing("flipped") && !clipDone())
			return;
		Vec3 d = body.subtract(position());
		float bearing = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90;
		hold(bearing + GRIEF_LOOK, Mth.ceil(clips().clip("grief").length() * 20));
		playClip("grief"); // sound: a low falling chirr
		busyTicks = GRIEF_TICKS;
	}

	/** Called out of a wall vent by the Custodian (D57): it tumbles out and rights itself before it runs. */
	public void burstFromVent() {
		playClip("vent_burst"); // sound: the hatch blowing, the tumble
		busyTicks = VENT_TICKS;
	}

	public boolean busy() {
		return busyTicks > 0;
	}

	// ---- the Custodian's fight Scuttlers (P35)

	/** Called out by the Custodian {@code by}: awake, and it drops nothing. */
	public void makeFightScuttler(Custodian by) {
		fight = true;
		custodian = by.getUUID();
		bind(by.cityOrigin(), by.district());
		playClip("idle");
	}

	public boolean isFightScuttler() {
		return fight;
	}

	@Nullable
	public UUID custodian() {
		return custodian;
	}

	/** Peaceful (D56): a fight Scuttler's fight is over (its Custodian resets), so it crumbles; the others sleep. */
	@Override
	protected void sleepForPeace() {
		if (fight)
			crumble();
		else
			super.sleepForPeace();
	}

	/** The Custodian died or reset: it falls to pieces, leaving nothing (P35). */
	public void crumble() {
		if (level() instanceof ServerLevel server)
			server.sendParticles(ParticleTypes.SMOKE, getX(), getY(0.5), getZ(), 12, 0.25, 0.2, 0.25, 0.02);
		// sound: plates clattering apart
		discard();
	}

	/** Fight Scuttlers drop nothing, not even experience (P35); the others drop by their loot table (P18). */
	@Override
	protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
		if (!fight)
			super.dropAllDeathLoot(level, source);
	}

	// ---- saving

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putBoolean("Fight", fight);
		if (custodian != null)
			tag.putUUID("Custodian", custodian);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		fight = tag.getBoolean("Fight");
		custodian = tag.hasUUID("Custodian") ? tag.getUUID("Custodian") : null;
	}
}
