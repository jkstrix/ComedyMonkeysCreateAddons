package com.comedymonkey.createaddons.content.wastes.bot;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.comedymonkey.createaddons.content.tradebot.clip.ClipLibrary;
import com.comedymonkey.createaddons.content.wastes.CityRecord;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Sentry (P19, P42, P9; brief: "a flower with eyes"): a turret fixed to a wall or floor, its mount on one face of a
 * block. It never moves (no push, no knockback, no fall). Woken and fed, it opens and watches; a player in its sight
 * within 24 blocks gets a starlight bolt every 2 s, 7 a bolt. While its district is unfed it folds up and stops (P9),
 * and opens again when the power's back. Asleep (its district not woken) it's a closed bud.
 * <p>
 * It aims with its yaw ring and pitch knuckle: the server turns its aim at up to 120°/s (synced), clients follow with a
 * spring that overshoots a few degrees and settles (brief: "turns smoothly with a slight overshoot").
 * <p>
 * Its clips (brief, Motion Set): watching (the slow sweep, lenses dim and rippling) with a mannerism every 6 to 10 s
 * (a barrel check, a petal twitch, a blink round the ring, a tilt to listen); a target seen snaps its petals open and
 * its lenses to it; aiming holds them narrowed between shots; firing; a hit's flinch; the slow fold; and its death,
 * petals splaying and drooping, lenses going out in a spiral.
 */
public class Sentry extends CityBot {
	public static final String CLIPS = "/assets/comedymonkeysaddons/sentry/clips.json";
	private static final EntityDataAccessor<Direction> FACE = SynchedEntityData.defineId(Sentry.class, EntityDataSerializers.DIRECTION);
	private static final EntityDataAccessor<Float> AIM_YAW = SynchedEntityData.defineId(Sentry.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> AIM_PITCH = SynchedEntityData.defineId(Sentry.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Boolean> FOLDED = SynchedEntityData.defineId(Sentry.class, EntityDataSerializers.BOOLEAN);

	/** P19, P42. */
	public static final float HEALTH = 30, BOLT_DAMAGE = 7;
	public static final double RANGE = 24;
	/** A bolt every 2 s (P19). */
	public static final int FIRE_EVERY = 40;
	/** The fire clip's shot: after the lenses narrow and the bore gathers light (0.6 s), at its recoil key (0.7 s). */
	static final float SHOT_AT = 0.7f;
	/** Brief: yaw up to 120°/s; pitch ±60°. */
	static final float TURN = 6, PITCH_LIMIT = 60;
	/** Aimed near enough to fire (the spec doesn't say; see the progress notes). */
	public static final float AIMED = 10;
	/** Where the head turns, along the mount's normal (the model's head at 14 px), and how far out the barrel's tip is. */
	static final double HEAD = 14 / 16.0, BARREL = 8.5 / 16.0;
	/** Brief: a mannerism every 6 to 10 s while it watches. */
	static final int MANNERISM_MIN = 120, MANNERISM_SPREAD = 80;
	/** Its mannerisms (brief), each from the watching pose. */
	static final java.util.List<String> MANNERISMS = java.util.List.of("barrel_check", "petal_twitch", "blink", "listen");
	/** Watching sweeps its yaw ring through centre at 0 s and 3 s of its 6 s loop: a mannerism starts there, so nothing jumps. */
	static final float SWEEP_HALF = 3, SWEEP_SLACK = 0.1f;
	/** A target seen: the petals have snapped open (the target clip's 0.25 s) before it may fire. */
	static final float SNAPPED = 0.3f;

	@Nullable
	private BlockPos anchor;
	private boolean fed = true;
	private int cooldown, shots;
	/** The shot of the fire clip under way: fires when the clip reaches it. */
	private boolean shotPending;
	@Nullable
	private LivingEntity aimedAt, lastTarget;
	private int mannerismTicks, nextMannerism = MANNERISM_MIN;

	/** Client: the spring following the server's aim. */
	public float shownYaw, shownPitch, yawSpeed, pitchSpeed;
	private double lastSpring = Double.NaN;

	public Sentry(EntityType<? extends Sentry> type, Level level) {
		super(type, level);
		xpReward = 10; // P19
		setNoGravity(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, HEALTH)
			.add(Attributes.MOVEMENT_SPEED, 0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1)
			.add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 1)
			.add(Attributes.FOLLOW_RANGE, RANGE);
	}

	@Override
	public ClipLibrary clips() {
		return ClipLibrary.of(CLIPS);
	}

	@Override
	protected String dormantClip() {
		return "unfold"; // its first frame: the closed bud, lenses dark
	}

	@Override
	protected String deathClip() {
		return "death"; // petals splay open and droop, the lenses go out in a spiral, the barrel sags (brief)
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(FACE, Direction.UP);
		builder.define(AIM_YAW, 0f);
		builder.define(AIM_PITCH, 0f);
		builder.define(FOLDED, false);
	}

	// ======================================================================== the mount

	/**
	 * Fixes it to {@code face} of the block at {@code wall} (UP: on the floor; a side: on that wall). It sits in the block
	 * in front of that face.
	 */
	public void mount(BlockPos wall, Direction face) {
		if (face == Direction.DOWN)
			face = Direction.UP; // floors and walls only (P19)
		entityData.set(FACE, face);
		anchor = wall.relative(face).immutable();
		snap();
	}

	/** The face of its block it's mounted on: UP on a floor, a side on a wall. */
	public Direction face() {
		return entityData.get(FACE);
	}

	/** The block it sits in. */
	public BlockPos anchor() {
		return anchor != null ? anchor : blockPosition();
	}

	private void snap() {
		BlockPos a = anchor();
		setPos(a.getX() + 0.5, a.getY(), a.getZ() + 0.5);
		setDeltaMovement(Vec3.ZERO);
		setYRot(0);
		yBodyRot = yHeadRot = 0;
	}

	/** Its frame: the model's front, up and its yaw ring's turning side, in the world (shared with the renderer). */
	public static Vector3f[] frame(Direction face) {
		Quaternionf r = face.getRotation();
		return new Vector3f[] { r.transform(new Vector3f(0, 0, 1)), r.transform(new Vector3f(0, 1, 0)), r.transform(new Vector3f(1, 0, 0)) };
	}

	/** Where its head turns, in the world. */
	public Vec3 headPos() {
		Vector3f up = frame(face())[1];
		Vec3 base = face() == Direction.UP ? position() : position().add(0, 0.5, 0).subtract(up.x * 0.5, up.y * 0.5, up.z * 0.5);
		return base.add(up.x * HEAD, up.y * HEAD, up.z * HEAD);
	}

	/** The yaw and pitch (degrees, its own frame) that point its barrel at {@code at}. */
	float[] aimAt(Vec3 at) {
		Vector3f[] f = frame(face());
		Vec3 d = at.subtract(headPos()).normalize();
		double front = d.x * f[0].x + d.y * f[0].y + d.z * f[0].z, up = d.x * f[1].x + d.y * f[1].y + d.z * f[1].z,
			side = d.x * f[2].x + d.y * f[2].y + d.z * f[2].z;
		float yaw = (float) Math.toDegrees(Math.atan2(side, front));
		float pitch = (float) Math.toDegrees(Math.asin(Mth.clamp(up, -1, 1)));
		return new float[] { yaw, Mth.clamp(pitch, -PITCH_LIMIT, PITCH_LIMIT) };
	}

	/** Its barrel's line in the world for an aim. */
	Vec3 barrelDir(float yaw, float pitch) {
		Vector3f[] f = frame(face());
		double cy = Math.cos(Math.toRadians(yaw)), sy = Math.sin(Math.toRadians(yaw)), cp = Math.cos(Math.toRadians(pitch)), sp = Math.sin(Math.toRadians(pitch));
		return new Vec3(f[0].x * cy * cp + f[2].x * sy * cp + f[1].x * sp, f[0].y * cy * cp + f[2].y * sy * cp + f[1].y * sp,
			f[0].z * cy * cp + f[2].z * sy * cp + f[1].z * sp);
	}

	public float aimYaw() {
		return entityData.get(AIM_YAW);
	}

	public float aimPitch() {
		return entityData.get(AIM_PITCH);
	}

	/** Folded up (unfed, P9) or asleep: dark and still. */
	public boolean folded() {
		return dormant() || entityData.get(FOLDED);
	}

	/** From an egg or a command (not the city): mounted on the floor where it appeared. */
	@Override
	public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
		net.minecraft.world.entity.MobSpawnType reason, @Nullable net.minecraft.world.entity.SpawnGroupData data) {
		if (anchor == null)
			mount(blockPosition().below(), Direction.UP);
		return super.finalizeSpawn(level, difficulty, reason, data);
	}

	// ======================================================================== power

	public boolean fed() {
		return fed;
	}

	/** Its district's feed changed (P9): unfed it folds and stops; fed again it opens. */
	public void setFed(boolean fed) {
		this.fed = fed;
		if (dormant() || level().isClientSide)
			return;
		if (!fed && !entityData.get(FOLDED)) {
			entityData.set(FOLDED, true);
			shotPending = false;
			aimedAt = null;
			setTarget(null);
			playClip("fold"); // sound: the slow fold, lenses going out
		} else if (fed && entityData.get(FOLDED)) {
			entityData.set(FOLDED, false);
			playClip("unfold"); // sound: the unfold
		}
	}

	/** Its district's feed from the record; one with no city runs as fed (an egg's, for testing). */
	private boolean fedNow() {
		CityRecord record = record();
		if (record == null || !link.bound())
			return true;
		return record.fed(link.origin(), link.district(), level().getGameTime());
	}

	@Override
	protected void catchUp() {
		super.catchUp();
		if (!dormant())
			setFed(fedNow());
	}

	@Override
	protected void onWake() {
		entityData.set(FOLDED, false);
		fed = true;
		playClip("unfold"); // sound: the unfold
		setFed(fedNow());
	}

	@Override
	protected void onDormant() {
		entityData.set(FOLDED, false);
	}

	// ======================================================================== watching and firing

	@Override
	public void tick() {
		if (!level().isClientSide)
			snap();
		super.tick();
	}

	/** Folded (unfed) it stops: no aim, no fire. */
	@Override
	protected boolean isImmobile() {
		return super.isImmobile() || entityData.get(FOLDED);
	}

	@Override
	protected void cityTick() {
		super.cityTick();
		if (entityData.get(FOLDED) && getTarget() != null)
			setTarget(null);
	}

	/** It thinks for itself (no goals): watch, aim, fire. */
	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		if (playing("unfold") && !clipDone())
			return;
		LivingEntity target = updateTarget();
		if (cooldown > 0)
			cooldown--;
		float wantYaw = 0, wantPitch = 0;
		if (target != null) {
			float[] want = aimAt(target.getBoundingBox().getCenter());
			wantYaw = want[0];
			wantPitch = want[1];
		}
		float yaw = aimYaw() + Mth.clamp(Mth.wrapDegrees(wantYaw - aimYaw()), -TURN, TURN);
		float pitch = aimPitch() + Mth.clamp(wantPitch - aimPitch(), -TURN, TURN);
		entityData.set(AIM_YAW, yaw);
		entityData.set(AIM_PITCH, pitch);
		if (shotPending && clipElapsed() >= SHOT_AT) {
			shotPending = false;
			LivingEntity at = aimedAt != null && aimedAt.isAlive() ? aimedAt : target;
			Vec3 tip = headPos().add(barrelDir(yaw, pitch).scale(BARREL));
			Vec3 to = at != null ? at.getBoundingBox().getCenter() : tip.add(barrelDir(yaw, pitch));
			StarlightBolt.fire(this, tip, to, BOLT_DAMAGE);
			shots++;
		}
		if (target == null) {
			lastTarget = null;
			if (playing("fire") && !clipDone() || playingHit() && !clipDone())
				return;
			// watching; now and then, as its sweep passes centre, a mannerism
			float t = playing("watching") ? clipLocalTime() % SWEEP_HALF : -1;
			if (++mannerismTicks > nextMannerism && t >= 0 && (t < SWEEP_SLACK || t > SWEEP_HALF - SWEEP_SLACK)) {
				mannerismTicks = 0;
				nextMannerism = MANNERISM_MIN + random.nextInt(MANNERISM_SPREAD);
				playClip(MANNERISMS.get(random.nextInt(MANNERISMS.size()))); // sound: a click, a whir, a tick
			} else
				playIfFree("watching");
			return;
		}
		mannerismTicks = 0;
		if (target != lastTarget) {
			// target seen: the petals snap open, every lens lights and swivels to it together (brief)
			lastTarget = target;
			if (!shotPending)
				playClip("target"); // sound: the lenses turning together
		}
		if (playing("target") && clipElapsed() < SNAPPED || playingHit() && !clipDone())
			return;
		boolean aimed = Math.abs(Mth.wrapDegrees(wantYaw - yaw)) < AIMED && Math.abs(wantPitch - pitch) < AIMED;
		if (cooldown <= 0 && aimed && !shotPending) {
			cooldown = FIRE_EVERY;
			shotPending = true;
			aimedAt = target;
			playClip("fire"); // sound: the bore gathering light
		} else if ((!playing("fire") || clipDone()) && (!playing("target") || clipDone()) && !playing("aim"))
			playClip("aim"); // between shots: lenses narrowed on the target, petals twitching tight (brief: Aiming)
	}

	/**
	 * Hit (brief): the struck side's petals flinch and its lenses blink out for 0.3 s; never cutting a shot short. The side
	 * is its head's, as its yaw ring has turned it (D67: the left-side clip once it's in, {@link CityBot#LEFT_HIT_CLIPS}).
	 */
	@Override
	public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
		boolean hurt = super.hurt(source, amount);
		if (hurt && !level().isClientSide && isAlive() && !folded() && !(playing("fire") && !clipDone()) && !(playing("unfold") && !clipDone()))
			playClip(hitClip(source, headPos(), headFacing(), headUp())); // sound: a clang, the lenses' blink
		return hurt;
	}

	/** The way its head faces, its yaw ring's turn in its mount's plane (its pitch left out). */
	public Vec3 headFacing() {
		return barrelDir(aimYaw(), 0);
	}

	/** Its head's up: its mount's normal. */
	public Vec3 headUp() {
		Vector3f up = frame(face())[1];
		return new Vec3(up.x, up.y, up.z);
	}

	/** A player it can see within 24 blocks (P19); it keeps one while it can still see them. */
	@Nullable
	private LivingEntity updateTarget() {
		LivingEntity target = getTarget();
		if (target != null && (!target.isAlive() || target.level() != level() || !inSight(target) || target instanceof Player p && !fightable(p)))
			target = null;
		if (target == null && tickCount % 10 == 0) {
			double best = RANGE * RANGE;
			for (Player p : level().players())
				if (fightable(p) && p.distanceToSqr(headPos()) < best && inSight(p)) {
					best = p.distanceToSqr(headPos());
					target = p;
				}
		}
		if (target != getTarget())
			setTarget(target); // sound: the lenses turning together
		return target;
	}

	/** Within 24 blocks, in its line of sight from the head, and not behind its own wall or floor. */
	public boolean inSight(Entity e) {
		Vec3 from = headPos(), to = e.getBoundingBox().getCenter();
		if (from.distanceToSqr(to) > RANGE * RANGE)
			return false;
		Vector3f up = frame(face())[1];
		if ((to.x - from.x) * up.x + (to.y - from.y) * up.y + (to.z - from.z) * up.z < -0.5)
			return false;
		return level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
	}

	/** Bolts it has fired since it loaded (game tests). */
	public int shots() {
		return shots;
	}

	// ======================================================================== client: the aim's spring

	/** Follows the server's aim with an underdamped spring: a few degrees past, then settled (brief). Once a frame. */
	public void springAim(double now) {
		double dt = Double.isNaN(lastSpring) ? 0 : Math.min(5, now - lastSpring) / 20;
		lastSpring = now;
		if (folded()) {
			shownYaw = shownPitch = yawSpeed = pitchSpeed = 0;
			return;
		}
		// about 2.5 Hz, damped so a turn overshoots a few degrees
		float k = 250, c = 22;
		for (double left = dt; left > 1e-6; left -= 0.01) {
			float h = (float) Math.min(0.01, left);
			yawSpeed += (k * Mth.wrapDegrees(aimYaw() - shownYaw) - c * yawSpeed) * h;
			pitchSpeed += (k * (aimPitch() - shownPitch) - c * pitchSpeed) * h;
			shownYaw += yawSpeed * h;
			shownPitch += pitchSpeed * h;
		}
	}

	// ======================================================================== never moves

	@Override
	public void move(MoverType type, Vec3 delta) {
		if (type == MoverType.SELF || type == MoverType.PLAYER || type == MoverType.PISTON || type == MoverType.SHULKER || type == MoverType.SHULKER_BOX)
			return;
		super.move(type, Vec3.ZERO);
	}

	@Override
	public void knockback(double strength, double x, double z) {}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public void push(double x, double y, double z) {}

	@Override
	protected void pushEntities() {}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	protected boolean canRide(Entity vehicle) {
		return false;
	}

	@Override
	public boolean causeFallDamage(float distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
		return false;
	}

	// ======================================================================== saving

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putString("Face", face().getSerializedName());
		if (anchor != null)
			tag.putLong("Anchor", anchor.asLong());
		tag.putBoolean("Folded", entityData.get(FOLDED));
		tag.putBoolean("Fed", fed);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		Direction face = Direction.byName(tag.getString("Face"));
		entityData.set(FACE, face == null || face == Direction.DOWN ? Direction.UP : face);
		anchor = tag.contains("Anchor") ? BlockPos.of(tag.getLong("Anchor")) : null;
		fed = !tag.contains("Fed") || tag.getBoolean("Fed");
		entityData.set(FOLDED, tag.getBoolean("Folded"));
		if (!dormant())
			playClip(entityData.get(FOLDED) ? "fold" : "watching", 1, entityData.get(FOLDED) ? 3 : 0);
	}
}
