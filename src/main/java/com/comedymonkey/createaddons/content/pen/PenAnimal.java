package com.comedymonkey.createaddons.content.pen;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * One of the animals walking about in an Animal Pen's window (client only). It wanders the straw in front of the
 * trough: walks to a spot, stands a while, picks another. Positions are in the pen's model pixels, front facing north.
 */
public class PenAnimal {
	/** The straw it may walk on: in front of the trough, inside the walls. */
	private static final float MIN_X = 3.5f, MAX_X = 12.5f, MIN_Z = 2.5f, MAX_Z = 9.5f;
	/** Walking speed in pixels a tick: about a real animal's pace at the size it's drawn. */
	private static final float SPEED = 0.18f;

	private final Entity entity;
	private float x, z, prevX, prevZ, yaw, prevYaw;
	private float targetX, targetZ;
	private int wait;
	private boolean baby;

	PenAnimal(Entity entity, RandomSource random) {
		this.entity = entity;
		x = prevX = targetX = Mth.lerp(random.nextFloat(), MIN_X, MAX_X);
		z = prevZ = targetZ = Mth.lerp(random.nextFloat(), MIN_Z, MAX_Z);
		yaw = prevYaw = random.nextFloat() * 360;
		wait = random.nextInt(60);
	}

	public Entity entity() {
		return entity;
	}

	void setBaby(boolean baby) {
		if (this.baby == baby)
			return;
		this.baby = baby;
		if (entity instanceof AgeableMob mob)
			mob.setAge(baby ? -24000 : 0);
	}

	/** Moves on a step; standing still while the pen isn't turning. */
	void tick(RandomSource random, boolean running) {
		prevX = x;
		prevZ = z;
		prevYaw = yaw;
		entity.tickCount++;
		float dx = targetX - x, dz = targetZ - z, dist = Mth.sqrt(dx * dx + dz * dz);
		boolean walking = running && wait <= 0 && dist > 0.2f;
		if (walking) {
			float step = Math.min(SPEED, dist);
			x += dx / dist * step;
			z += dz / dist * step;
			// Turn towards where it's going, a little at a time
			float facing = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90;
			yaw += Mth.clamp(Mth.wrapDegrees(facing - yaw), -12, 12);
		} else if (wait > 0) {
			wait--;
		} else if (running) {
			targetX = Mth.lerp(random.nextFloat(), MIN_X, MAX_X);
			targetZ = Mth.lerp(random.nextFloat(), MIN_Z, MAX_Z);
			wait = 40 + random.nextInt(100);
		}
		if (entity instanceof LivingEntity living) {
			living.walkAnimation.update(walking ? 0.6f : 0, 0.4f);
			living.yBodyRotO = prevYaw;
			living.yBodyRot = yaw;
			living.yHeadRotO = prevYaw;
			living.yHeadRot = yaw;
		}
		entity.yRotO = prevYaw;
		entity.setYRot(yaw);
	}

	public float x(float partialTicks) {
		return Mth.lerp(partialTicks, prevX, x);
	}

	public float z(float partialTicks) {
		return Mth.lerp(partialTicks, prevZ, z);
	}

	public float yaw(float partialTicks) {
		return Mth.rotLerp(partialTicks, prevYaw, yaw);
	}
}
