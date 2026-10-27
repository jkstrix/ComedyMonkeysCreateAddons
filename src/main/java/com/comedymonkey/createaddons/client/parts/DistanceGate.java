package com.comedymonkey.createaddons.client.parts;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.compat.sable.SableCompat;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import dev.engine_room.flywheel.api.visual.DynamicVisual;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * How often an animated visual that Flywheel's own culling can't place is posed: Create's contraption actors (posed
 * every frame, with no frame context) and visuals embedded in a ship or a contraption (whose positions are the
 * embedding's, so the world's frustum and Flywheel's distance limiter say nothing about them). Each frame close by,
 * less often further out (the same bands as Flywheel's distance limiter), and not at all beyond render distance.
 */
public final class DistanceGate {
	/** Flywheel's BandedPrimeLimiter: one band per 2048 blocks² (about 45 blocks, then 64, 78...), 1 then the primes. */
	private static final int[] DIVISORS = { 1, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31 };

	private int frame;

	/** {@code seed} staggers visuals made in the same frame (a train's bots), so far ones don't all pose together. */
	public DistanceGate(int seed) {
		frame = Math.floorMod(seed, 31 * 29);
	}

	/** Every how many frames something this far (squared, in blocks) from the camera is posed. */
	public static int divisor(double distanceSq) {
		return DIVISORS[Mth.clamp(Mth.ceil(distanceSq) / 2048, 0, DIVISORS.length - 1)];
	}

	/** Once a frame: whether to pose it this frame, at this squared distance, with nothing seen beyond {@code maxSq}. */
	public boolean shouldUpdate(double distanceSq, double maxSq) {
		frame++;
		return distanceSq <= maxSq && frame % divisor(distanceSq) == 0;
	}

	/** Once a frame, for something at {@code at} in the world. */
	public boolean shouldUpdate(Vec3 at) {
		return shouldUpdate(at.distanceToSqr(camera()), renderDistanceSq());
	}

	/**
	 * For an embedded visual with Flywheel's frame context: whether to skip this frame, measured from where it really
	 * is ({@code at}, null when that can't be told: then it's posed every frame, as before).
	 */
	public static boolean skip(DynamicVisual.Context context, @Nullable Vec3 at) {
		if (at == null)
			return false;
		double distanceSq = at.distanceToSqr(context.camera().getPosition());
		return distanceSq > renderDistanceSq() || !context.limiter().shouldUpdate(distanceSq);
	}

	/** Squared blocks to the edge of render distance. */
	public static double renderDistanceSq() {
		double blocks = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0;
		return blocks * blocks;
	}

	private static Vec3 camera() {
		return Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
	}

	/**
	 * Where an embedded block entity really is. On a Sable ship its block sits in the ship's plot: moved out to the
	 * ship. On a Create contraption its level is the contraption's: placed by the contraption's entity ({@code owner},
	 * found once and kept by the caller). Null if neither can tell.
	 */
	@Nullable
	public static Vec3 worldPosition(BlockEntity be, @Nullable Entity owner) {
		BlockPos pos = be.getBlockPos();
		Vec3 centre = Vec3.atCenterOf(pos);
		if (owner instanceof AbstractContraptionEntity contraption)
			return contraption.toGlobalVector(centre, 1);
		ClientLevel level = Minecraft.getInstance().level;
		if (SableCompat.LOADED && be.getLevel() == level) {
			Vec3 out = com.comedymonkey.createaddons.compat.sable.client.SableSoundPositions.heardFrom(centre.x, centre.y, centre.z);
			// (not in the plots: it isn't on a ship, and its own position is right)
			return out;
		}
		return null;
	}

	/** The contraption whose level this block entity is drawn in (rendering threads only), or null. */
	@Nullable
	public static AbstractContraptionEntity contraptionOf(BlockEntity be) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null || be.getLevel() == level || be.getLevel() == null)
			return null;
		for (Entity entity : level.entitiesForRendering())
			if (entity instanceof AbstractContraptionEntity ace && ace.getContraption() != null
				&& ace.getContraption().getOrCreateClientContraptionLazy().getRenderLevel() == be.getLevel())
				return ace;
		return null;
	}
}
