package com.comedymonkey.createaddons.client;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

import com.comedymonkey.createaddons.content.tank.FishTankBlockEntity;
import com.comedymonkey.createaddons.registry.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The Fish Tanks' running loops (design/sound/fish-tank/fish-tank.md), from their client tick: the air pump while the
 * shaft turns, its pitch following the speed a little, and the bubbles while it pumps air into water. The one-shots are
 * played by the server. A loop holds its tank weakly, so the map's weak keys can let a removed tank go.
 */
public final class FishTankSounds {
	private static final Map<FishTankBlockEntity, State> STATES = new WeakHashMap<>();
	/** Only the nearest few tanks' pumps and bubbles play: a wall of tanks would take every sound channel. */
	private static final NearestLoops PUMPS = new NearestLoops(), BUBBLES = new NearestLoops();

	private FishTankSounds() {}

	public static void forget() {
		STATES.clear();
	}

	private static final class State {
		Loop pump, bubbles;
	}

	private static boolean pumping(FishTankBlockEntity be) {
		return be.getSpeed() != 0 && !be.isOverStressed();
	}

	private static boolean bubbling(FishTankBlockEntity be) {
		return pumping(be) && be.getWater().getFluidAmount() > 0;
	}

	public static void tick(FishTankBlockEntity be) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || be.getLevel() != mc.level)
			return;
		if (!STATES.containsKey(be))
			STATES.keySet().removeIf(BlockEntity::isRemoved);
		State s = STATES.computeIfAbsent(be, b -> new State());
		// worked out once this tick, by the block entity, for its window too
		double distanceSqr = be.getViewDistanceSqr();
		SoundEvent pump = ModSounds.tank("pump_loop");
		if (pumping(be) && wanted(s.pump, pump, PUMPS, be, distanceSqr))
			mc.getSoundManager().play(s.pump = new Loop(be, pump, FishTankSounds::pumping, true, PUMPS));
		SoundEvent bubbles = ModSounds.tank("bubbles_loop");
		if (bubbling(be) && wanted(s.bubbles, bubbles, BUBBLES, be, distanceSqr))
			mc.getSoundManager().play(s.bubbles = new Loop(be, bubbles, FishTankSounds::bubbling, false, BUBBLES));
	}

	/**
	 * For a tank whose loop should run: offers it among the nearest (if it's in earshot, a playing one a little further
	 * out), and whether a new one should start.
	 */
	private static boolean wanted(Loop loop, SoundEvent sound, NearestLoops kind, FishTankBlockEntity be, double distanceSqr) {
		boolean playing = !MachineLoop.ended(loop);
		if (!Hearing.inRange(sound, 1, distanceSqr, playing ? Hearing.LOOP_STOP_MARGIN : Hearing.LOOP_START_MARGIN))
			return false;
		return kind.offer(be, distanceSqr, playing) && !playing;
	}

	/** A running loop: fades in and out with {@code on} (and out when nearer tanks take its place), stops once silent or gone. */
	private static final class Loop extends MachineLoop {
		private final WeakReference<FishTankBlockEntity> tank;
		private final Predicate<FishTankBlockEntity> on;
		private final boolean pitched;
		private final NearestLoops kind;

		Loop(FishTankBlockEntity be, SoundEvent sound, Predicate<FishTankBlockEntity> on, boolean pitched, NearestLoops kind) {
			super(sound);
			this.tank = new WeakReference<>(be);
			this.on = on;
			this.pitched = pitched;
			this.kind = kind;
			BlockPos at = be.getBlockPos();
			x = at.getX() + 0.5;
			y = at.getY() + 0.5;
			z = at.getZ() + 0.5;
			looping = true;
			delay = 0;
			volume = 0.01f;
		}

		@Override
		public void tick() {
			FishTankBlockEntity be = tank.get();
			if (be == null || be.isRemoved() || !Hearing.inRange(getLocation(), 1, x, y, z, Hearing.LOOP_STOP_MARGIN)) {
				stop();
				return;
			}
			boolean running = on.test(be) && kind.allows(be);
			volume = running ? Math.min(1, volume + 0.1f) : volume - 0.1f;
			if (pitched)
				pitch = Mth.clampedMap(Math.abs(be.getSpeed()), 16, 256, 0.9f, 1.1f);
			if (volume <= 0.01f && !running)
				stop();
		}

		@Override
		public boolean canStartSilent() {
			return true;
		}
	}
}
