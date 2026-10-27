package com.comedymonkey.createaddons.client;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.compat.sable.SableCompat;
import com.comedymonkey.createaddons.compat.sable.client.SableSoundPositions;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Whether the listener is in earshot of a machine's sound. The machines play their sounds from their client ticks, and
 * the sound engine gives every sound a channel however far off it is, so a big base would run out of channels; out of
 * earshot they aren't played at all. A sound's range is its attenuation distance (from sounds.json), longer when it's
 * played louder than 1, exactly as the sound engine works it out.
 */
public final class Hearing {
	/** One-shots: a little past their range, as the listener may walk into it while one plays. */
	public static final double ONE_SHOT_MARGIN = 2;
	/** Loops start this far past their range, so they've faded in before they can be heard... */
	static final double LOOP_START_MARGIN = 4;
	/** ...and stop a little further out, so walking along the edge doesn't start and stop them over and over. */
	static final double LOOP_STOP_MARGIN = 6;
	/** Minecraft's range for a sound that doesn't give one. */
	private static final float DEFAULT_RANGE = 16;
	private static final Map<ResourceLocation, Float> RANGES = new HashMap<>();

	private Hearing() {}

	/** Forgets the ranges, which a resource pack can change: on a resource reload, and as the world goes. */
	static void forget() {
		RANGES.clear();
	}

	/** Whether a sound played at {@code volume} from (x, y, z) reaches the listener, give or take {@code margin}. */
	public static boolean inRange(SoundEvent sound, float volume, double x, double y, double z, double margin) {
		return inRange(sound.getLocation(), volume, x, y, z, margin);
	}

	/**
	 * The same, for a sound by its id (a playing sound instance's {@code getLocation()}). A block on an Aeronautics
	 * ship is measured from where the ship is, as Sable plays it there; one that can't be placed is always in range.
	 */
	static boolean inRange(ResourceLocation sound, float volume, double x, double y, double z, double margin) {
		double range = Math.max(volume, 1) * range(sound) + margin;
		if (SableCompat.LOADED) {
			Vec3 heard = SableSoundPositions.heardFrom(x, y, z);
			if (heard == null)
				return true;
			return listener().distanceToSqr(heard) <= range * range;
		}
		return listener().distanceToSqr(x, y, z) <= range * range;
	}

	/**
	 * Squared distance from the listener (the camera) to (x, y, z), a block on a ship measured from where the ship is.
	 * Endless for one that can't be placed: for ranking loops and building display creatures it's furthest of all (a
	 * one-shot's {@link #inRange} still plays it).
	 */
	static double distanceSqr(double x, double y, double z) {
		if (SableCompat.LOADED)
			return distanceSqr(SableSoundPositions.heardFrom(x, y, z), listener());
		return listener().distanceToSqr(x, y, z);
	}

	/** From the listener to where a sound is heard from, endless when that can't be told. */
	static double distanceSqr(@Nullable Vec3 heard, Vec3 listener) {
		return heard == null ? Double.POSITIVE_INFINITY : listener.distanceToSqr(heard);
	}

	/** {@link #inRange} for a distance already worked out with {@link #distanceSqr}. */
	static boolean inRange(SoundEvent sound, float volume, double distanceSqr, double margin) {
		double range = Math.max(volume, 1) * range(sound.getLocation()) + margin;
		return distanceSqr <= range * range;
	}

	/** The sound's attenuation distance in blocks; all the variants of each of our sounds share one. */
	private static float range(ResourceLocation sound) {
		return RANGES.computeIfAbsent(sound, id -> {
			WeighedSoundEvents events = Minecraft.getInstance().getSoundManager().getSoundEvent(id);
			if (events == null)
				return DEFAULT_RANGE;
			Sound picked = events.getSound(RandomSource.create(0));
			return picked == null ? DEFAULT_RANGE : (float) picked.getAttenuationDistance();
		});
	}

	/** Where sounds are heard from: the camera, as the sound engine has it. */
	private static Vec3 listener() {
		Minecraft mc = Minecraft.getInstance();
		Camera camera = mc.gameRenderer.getMainCamera();
		if (camera.isInitialized() || mc.player == null)
			return camera.getPosition();
		return mc.player.getEyePosition();
	}
}
