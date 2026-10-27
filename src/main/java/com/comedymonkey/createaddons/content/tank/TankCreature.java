package com.comedymonkey.createaddons.content.tank;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Squid;

/**
 * One of the creatures swimming in a Fish Tank's water (client only): it glides to a spot in the water, drifts a
 * while, picks another, turning gently as it goes, and stays under the surface. Squid pulse as they swim. Positions are
 * in the tank's model pixels, front facing north.
 */
public class TankCreature {
	private static final float MIN_X = 3, MAX_X = 13, MIN_Z = 4.5f, MAX_Z = 12;
	/**
	 * How tall a squid is drawn, in blocks (its model hangs 1.2 below its origin, tentacles and all), and so the room it
	 * needs at the renderer's scale: its tentacle tips clear the gravel, its mantle the surface.
	 */
	public static final float SQUID_HEIGHT = 2.06f;
	private static final float SQUID_BELOW = 4.4f, SQUID_ABOVE = 2.6f;
	private static final float SPEED = 0.09f;

	private final Entity entity;
	/** The lowest it swims, and how far under the surface it keeps (pixels). */
	private final float minY, headroom;
	private float x, y, z, prevX, prevY, prevZ, yaw, prevYaw;
	private float targetX, targetY, targetZ;
	private int wait;

	TankCreature(Entity entity, RandomSource random) {
		this.entity = entity;
		boolean squid = entity instanceof Squid;
		minY = squid ? 3.5f + SQUID_BELOW : 4;
		headroom = squid ? SQUID_ABOVE : 1.2f;
		x = prevX = targetX = Mth.lerp(random.nextFloat(), MIN_X, MAX_X);
		y = prevY = targetY = minY + random.nextFloat() * 4;
		z = prevZ = targetZ = Mth.lerp(random.nextFloat(), MIN_Z, MAX_Z);
		yaw = prevYaw = random.nextFloat() * 360;
		wait = random.nextInt(40);
		inWater(entity);
	}

	/** Fish flop about unless they know they're in water; there's no setter, so the flag is set directly. */
	private static void inWater(Entity entity) {
		try {
			java.lang.reflect.Field field = Entity.class.getDeclaredField("wasTouchingWater");
			field.setAccessible(true);
			field.setBoolean(entity, true);
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			// drawn flopping, then: harmless
		}
	}

	public Entity entity() {
		return entity;
	}

	/** {@code surface} is the water's top, in pixels. */
	void tick(RandomSource random, boolean running, float surface) {
		prevX = x;
		prevY = y;
		prevZ = z;
		prevYaw = yaw;
		entity.tickCount++;
		float top = Math.max(minY, surface - headroom);
		targetY = Math.min(targetY, top);
		float dx = targetX - x, dy = targetY - y, dz = targetZ - z, dist = Mth.sqrt(dx * dx + dy * dy + dz * dz);
		if (dist > 0.2f && wait <= 0) {
			float step = Math.min(SPEED * (running ? 1 : 0.5f), dist);
			x += dx / dist * step;
			y += dy / dist * step;
			z += dz / dist * step;
			if (dx * dx + dz * dz > 0.001f) {
				float facing = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90;
				yaw += Mth.clamp(Mth.wrapDegrees(facing - yaw), -6, 6);
			}
		} else if (wait > 0) {
			wait--;
		} else {
			targetX = Mth.lerp(random.nextFloat(), MIN_X, MAX_X);
			targetY = Mth.lerp(random.nextFloat(), minY, top);
			targetZ = Mth.lerp(random.nextFloat(), MIN_Z, MAX_Z);
			wait = 10 + random.nextInt(50);
		}
		y = Math.min(y, top);
		if (entity instanceof Squid squid) {
			// the tentacles' pulse, as a swimming squid's
			squid.oldTentacleAngle = squid.tentacleAngle;
			squid.tentacleAngle = Mth.abs(Mth.sin(entity.tickCount * 0.25f)) * Mth.PI * 0.25f;
		}
		if (entity instanceof LivingEntity living) {
			living.yBodyRotO = prevYaw;
			living.yBodyRot = yaw;
			living.yHeadRotO = prevYaw;
			living.yHeadRot = yaw;
			living.walkAnimation.update(0.6f, 0.4f);
		}
		entity.yRotO = prevYaw;
		entity.setYRot(yaw);
	}

	public float x(float partialTicks) {
		return Mth.lerp(partialTicks, prevX, x);
	}

	public float y(float partialTicks) {
		return Mth.lerp(partialTicks, prevY, y);
	}

	public float z(float partialTicks) {
		return Mth.lerp(partialTicks, prevZ, z);
	}

	public float yaw(float partialTicks) {
		return Mth.rotLerp(partialTicks, prevYaw, yaw);
	}
}
