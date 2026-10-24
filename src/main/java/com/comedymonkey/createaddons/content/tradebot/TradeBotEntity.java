package com.comedymonkey.createaddons.content.tradebot;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipLibrary;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipPlayback;
import com.comedymonkey.createaddons.content.tradebot.clip.ClipRoot;
import com.comedymonkey.createaddons.registry.ModSounds;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Trade Bot: a clockwork robot who sells your goods to seated villagers. See design/specs/trade-bot.md for what he
 * does and design/plans/trade-bot.md for how it's built.
 * <p>
 * The server picks which clip plays and when ({@link #playClip}); clients pose him from the same clip data the
 * Blockbench preview uses. In {@link Clip.Root#PHYSICAL} clips (hops, stairs, docking) the server moves him along the
 * clip's root path from where the clip started, and clients draw him on that path.
 */
public class TradeBotEntity extends PathfinderMob {
	private static final EntityDataAccessor<Integer> CLIP = SynchedEntityData.defineId(TradeBotEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> CLIP_START = SynchedEntityData.defineId(TradeBotEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Vector3f> CLIP_ANCHOR = SynchedEntityData.defineId(TradeBotEntity.class, EntityDataSerializers.VECTOR3);
	private static final EntityDataAccessor<Float> CLIP_ANCHOR_YAW = SynchedEntityData.defineId(TradeBotEntity.class, EntityDataSerializers.FLOAT);

	public static final String IDLE = "idle";

	/** Client only: the cross-fade and sound cues. */
	public final ClipPlayback playback = new ClipPlayback();

	public TradeBotEntity(EntityType<? extends TradeBotEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 20)
			.add(Attributes.MOVEMENT_SPEED, 0.2)
			// he rolls over anything 4 px or lower; taller steps are hops (design/specs/trade-bot.md D4)
			.add(Attributes.STEP_HEIGHT, 0.25)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CLIP, ClipLibrary.get().id(ClipLibrary.get().clip(IDLE)));
		builder.define(CLIP_START, 0);
		builder.define(CLIP_ANCHOR, new Vector3f());
		builder.define(CLIP_ANCHOR_YAW, 0f);
	}

	// ---- clips

	/** Starts a clip now (server). A physical clip remembers where it started, so it can move him from there. */
	public void playClip(Clip clip) {
		entityData.set(CLIP, ClipLibrary.get().id(clip));
		entityData.set(CLIP_START, (int) level().getGameTime());
		if (clip.root() == Clip.Root.PHYSICAL) {
			entityData.set(CLIP_ANCHOR, position().toVector3f());
			entityData.set(CLIP_ANCHOR_YAW, getYRot());
		}
	}

	public void playClip(String name) {
		playClip(ClipLibrary.get().clip(name));
	}

	public Clip clip() {
		Clip clip = ClipLibrary.get().byId(entityData.get(CLIP));
		return clip != null ? clip : ClipLibrary.get().clip(IDLE);
	}

	/** The game tick the current clip started on. */
	public long clipStart() {
		// the tick is synced as an int; rebuild the full value from the current time
		long now = level().getGameTime();
		return now - (int) (now - entityData.get(CLIP_START));
	}

	/** Seconds the current clip has played, at {@code partialTick} into the current tick. */
	public float clipElapsed(float partialTick) {
		return (float) ((level().getGameTime() - clipStart() + partialTick) / 20);
	}

	/** Where a physical clip started, and which way he faced. */
	public Vec3 clipAnchor() {
		return new Vec3(entityData.get(CLIP_ANCHOR));
	}

	public float clipAnchorYaw() {
		return entityData.get(CLIP_ANCHOR_YAW);
	}

	/** His place and facing on the current physical clip's path, at {@code seconds} into it. */
	public Vec3 rootPosition(Clip clip, float seconds) {
		return ClipRoot.place(clipAnchor(), clipAnchorYaw(), ClipRoot.travel(clip, seconds, new Vector3f()));
	}

	public float rootYaw(Clip clip, float seconds) {
		return ClipRoot.facing(clipAnchorYaw(), ClipRoot.turned(clip, seconds));
	}

	@Override
	public void tick() {
		super.tick();
		// he has no separate head or body turn: everything faces where he faces
		yBodyRot = getYRot();
		yHeadRot = getYRot();
		if (level().isClientSide) {
			playback.observe(clip(), clipStart(), level().getGameTime());
			playback.tickCues(level().getGameTime(), this::playCue);
			return;
		}
		Clip clip = clip();
		float elapsed = clipElapsed(0);
		boolean physical = clip.root() == Clip.Root.PHYSICAL;
		noPhysics = physical && !clip.finished(elapsed);
		setNoGravity(noPhysics);
		// on the path for the whole clip, including its last frame (and held there by a hold clip)
		if (physical) {
			float t = clip.localTime(elapsed);
			setDeltaMovement(Vec3.ZERO);
			Vec3 at = rootPosition(clip, t);
			setPos(at.x, at.y, at.z);
			setYRot(rootYaw(clip, t));
		}
		if (clip.mode() == Clip.Mode.ONCE && clip.finished(elapsed))
			playClip(IDLE);
	}

	private void playCue(String event) {
		SoundEvent sound = ModSounds.TRADE_BOT.containsKey(event) ? ModSounds.tradeBot(event) : null;
		if (sound != null)
			level().playLocalSound(getX(), getY(), getZ(), sound, SoundSource.NEUTRAL, 1, 1, false);
	}

	// ---- life

	@Override
	public boolean requiresCustomPersistence() {
		return true;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public boolean canBeLeashed() {
		return false;
	}

	@Override
	protected BodyRotationControl createBodyControl() {
		// no lagging body turn: he faces where he rolls
		return new BodyRotationControl(this) {
			@Override
			public void clientTick() {}
		};
	}

	@Override
	@Nullable
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.tradeBot("bonk");
	}

	@Override
	@Nullable
	protected SoundEvent getDeathSound() {
		return null;
	}

	@Override
	public boolean isPushable() {
		// nothing shoves him off a hop or out of his station mid-move
		return clip().root() != Clip.Root.PHYSICAL && super.isPushable();
	}
}
