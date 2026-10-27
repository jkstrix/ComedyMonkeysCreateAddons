package com.comedymonkey.createaddons.content.cage;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * One of the monsters pacing behind a Monster Cage's bars (client only), as the Animal Pens' animals wander their straw:
 * it walks to a spot, stands a while, picks another. A Wither hovers instead, drifting and turning to glare out. Positions
 * are in the cage's model pixels, front facing north.
 */
public class CageMonster {
	/** The floor its feet may walk on: behind the tray and the bars, clear of the bowl at the back. */
	private static final float MIN_X = 3.5f, MAX_X = 12.5f, MIN_Z = 4.5f, MAX_Z = 8.8f;
	private static final float SPEED = 0.16f;

	private final Entity entity;
	private final boolean hovers;
	private float x, z, prevX, prevZ, yaw, prevYaw, bob, prevBob;
	private float targetX, targetZ;
	private int wait;

	CageMonster(Entity entity, RandomSource random, boolean hovers) {
		this.entity = entity;
		this.hovers = hovers;
		x = prevX = targetX = hovers ? 8 : Mth.lerp(random.nextFloat(), MIN_X, MAX_X);
		z = prevZ = targetZ = hovers ? 6.5f : Mth.lerp(random.nextFloat(), MIN_Z, MAX_Z);
		yaw = prevYaw = hovers ? 180 : random.nextFloat() * 360;
		wait = random.nextInt(60);
	}

	public Entity entity() {
		return entity;
	}

	public boolean hovers() {
		return hovers;
	}

	void tick(RandomSource random, boolean running) {
		prevX = x;
		prevZ = z;
		prevYaw = yaw;
		prevBob = bob;
		entity.tickCount++;
		if (hovers) {
			// drifts side to side and bobs, always turning back to face the bars
			float t = entity.tickCount / 20f;
			x = 8 + Mth.sin(t * 0.35f) * 2.5f;
			z = 6.5f + Mth.sin(t * 0.21f) * 0.8f;
			bob = 0.4f + Mth.sin(t * 2.2f) * 0.3f;
			yaw = 180 + Mth.sin(t * 0.5f) * 25;
		} else {
			float dx = targetX - x, dz = targetZ - z, dist = Mth.sqrt(dx * dx + dz * dz);
			boolean walking = running && wait <= 0 && dist > 0.2f;
			if (walking) {
				float step = Math.min(SPEED, dist);
				x += dx / dist * step;
				z += dz / dist * step;
				float facing = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90;
				yaw += Mth.clamp(Mth.wrapDegrees(facing - yaw), -12, 12);
			} else if (wait > 0) {
				wait--;
			} else if (running) {
				targetX = Mth.lerp(random.nextFloat(), MIN_X, MAX_X);
				targetZ = Mth.lerp(random.nextFloat(), MIN_Z, MAX_Z);
				wait = 40 + random.nextInt(100);
			}
			if (entity instanceof LivingEntity living)
				living.walkAnimation.update(walking ? 0.6f : 0, 0.4f);
		}
		if (entity instanceof LivingEntity living) {
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

	/** How far above the floor, in pixels (a hovering Wither). */
	public float y(float partialTicks) {
		return Mth.lerp(partialTicks, prevBob, bob);
	}

	public float yaw(float partialTicks) {
		return Mth.rotLerp(partialTicks, prevYaw, yaw);
	}
}
