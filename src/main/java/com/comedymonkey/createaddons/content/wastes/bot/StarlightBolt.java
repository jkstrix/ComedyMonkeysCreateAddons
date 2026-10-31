package com.comedymonkey.createaddons.content.wastes.bot;

import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A starlight bolt: what a Sentry fires (7, P19, P42) and the Custodian's volley throws (8 each, P37). It flies straight
 * (no drop), hurts the first living thing it meets that isn't one of the city's bots, and fizzles on a block without
 * changing it (P37). Gone once it has flown its range (24 blocks).
 */
public class StarlightBolt extends ThrowableProjectile {
	/** Blocks a tick (the spec doesn't say; see the progress notes). */
	public static final float SPEED = 1.5f;
	/** P19, P37: 24 blocks. */
	public static final double RANGE = 24;

	private float damage = 7;

	public StarlightBolt(EntityType<? extends StarlightBolt> type, Level level) {
		super(type, level);
	}

	/** A bolt from {@code from} at {@code at}'s middle, for {@code damage}. */
	public static StarlightBolt fire(LivingEntity shooter, Vec3 from, Vec3 at, float damage) {
		StarlightBolt bolt = new StarlightBolt(ModWastes.STARLIGHT_BOLT.get(), shooter.level());
		bolt.setOwner(shooter);
		bolt.damage = damage;
		bolt.setPos(from.x, from.y, from.z);
		Vec3 d = at.subtract(from);
		bolt.shoot(d.x, d.y, d.z, SPEED, 0);
		shooter.level().addFreshEntity(bolt);
		// sound: the Sentry's bolt / the Custodian's volley (P28)
		return bolt;
	}

	public float damage() {
		return damage;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {}

	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide && tickCount * SPEED > RANGE)
			discard();
	}

	@Override
	protected double getDefaultGravity() {
		return 0;
	}

	@Override
	protected boolean canHitEntity(Entity target) {
		return super.canHitEntity(target) && target instanceof LivingEntity && !(target instanceof CityBot);
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		Entity owner = getOwner();
		hit.getEntity().hurt(damageSources().mobProjectile(this, owner instanceof LivingEntity l ? l : null), damage);
	}

	/** P37: a block it hits isn't told (no button pressed, no bell rung). */
	@Override
	protected void onHitBlock(BlockHitResult result) {}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);
		if (level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 6, 0.1, 0.1, 0.1, 0.1);
			// sound: the bolt's hit
			discard();
		}
	}

	@Override
	public boolean isOnFire() {
		return false;
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putFloat("Damage", damage);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		damage = tag.contains("Damage") ? tag.getFloat("Damage") : 7;
	}
}
