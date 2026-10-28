package com.comedymonkey.createaddons.content.juggernaut;

import com.comedymonkey.createaddons.registry.ModEntities;
import com.comedymonkey.createaddons.registry.ModSounds;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The heavy cog the Juggernaut hurls at a target he can't reach (D22): 8 damage, a knock back, and it breaks on what it
 * hits. It never breaks or changes a block, and drops nothing.
 */
public class ThrownCog extends ThrowableItemProjectile {
	/** About 6 s of flight before it gives up. */
	private static final int LIFETIME = 120;
	/** Create's large cogwheel, drawn as it flies. */
	private static final ResourceLocation LARGE_COGWHEEL = ResourceLocation.fromNamespaceAndPath("create", "large_cogwheel");

	public ThrownCog(EntityType<? extends ThrownCog> type, Level level) {
		super(type, level);
	}

	/** Thrown from his right hand toward {@code target}, leading a little for the arc. */
	public static void throwAt(ClockworkJuggernaut thrower, LivingEntity target) {
		ThrownCog cog = new ThrownCog(ModEntities.THROWN_COG.get(), thrower.level());
		cog.setOwner(thrower);
		Vec3 fwd = thrower.forward();
		Vec3 hand = thrower.position().add(fwd.scale(2.5)).add(fwd.z * 2.5, 4.5, -fwd.x * 2.5);
		cog.setPos(hand.x, hand.y, hand.z);
		double dx = target.getX() - hand.x, dz = target.getZ() - hand.z;
		double dy = target.getY(0.5) - hand.y;
		double d = Math.sqrt(dx * dx + dz * dz);
		cog.shoot(dx, dy + d * 0.12, dz, 1.4f, 2);
		thrower.level().addFreshEntity(cog);
	}

	@Override
	protected Item getDefaultItem() {
		return BuiltInRegistries.ITEM.get(LARGE_COGWHEEL);
	}


	@Override
	public void tick() {
		super.tick();
		if (!level().isClientSide && tickCount > LIFETIME)
			discard();
		if (!level().isClientSide && tickCount % 20 == 1)
			sound("cog_fly");
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		Entity victim = hit.getEntity();
		if (victim instanceof ClockworkJuggernaut)
			return;
		Entity owner = getOwner();
		victim.hurt(damageSources().mobProjectile(this, owner instanceof LivingEntity l ? l : null), ClockworkJuggernaut.COG_DAMAGE);
		Vec3 push = getDeltaMovement().multiply(1, 0, 1).normalize().scale(0.8);
		victim.push(push.x, 0.3, push.z);
		victim.hurtMarked = true;
	}

	/** Living things only: it flies past item frames, paintings and end crystals, and out of his own hand. */
	@Override
	protected boolean canHitEntity(Entity target) {
		return super.canHitEntity(target) && target instanceof LivingEntity && !(target instanceof ClockworkJuggernaut);
	}

	/** D2: a block it hits isn't told (no target block pulses, no bell rings); it just breaks on it. */
	@Override
	protected void onHitBlock(BlockHitResult result) {
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);
		if (level() instanceof ServerLevel server) {
			server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, getItem()), getX(), getY(), getZ(), 16, 0.3, 0.3, 0.3, 0.15);
			sound("cog_hit");
			discard();
		}
	}

	private void sound(String id) {
		SoundEvent sound = ModSounds.juggernaut(id);
		if (sound != null)
			level().playSound(null, getX(), getY(), getZ(), sound, SoundSource.HOSTILE, 1, 1);
	}

	@Override
	protected double getDefaultGravity() {
		return 0.03;
	}
}
