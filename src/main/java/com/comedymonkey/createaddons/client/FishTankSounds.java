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
		BlockPos pos = be.getBlockPos();
		SoundEvent pump = ModSounds.tank("pump_loop");
		if (MachineLoop.ended(s.pump) && pumping(be) && Loop.canStart(pump, pos))
			mc.getSoundManager().play(s.pump = new Loop(be, pump, FishTankSounds::pumping, true));
		SoundEvent bubbles = ModSounds.tank("bubbles_loop");
		if (MachineLoop.ended(s.bubbles) && bubbling(be) && Loop.canStart(bubbles, pos))
			mc.getSoundManager().play(s.bubbles = new Loop(be, bubbles, FishTankSounds::bubbling, false));
	}

	/** A running loop: fades in and out with {@code on}, stops once silent or gone. */
	private static final class Loop extends MachineLoop {
		private final WeakReference<FishTankBlockEntity> tank;
		private final Predicate<FishTankBlockEntity> on;
		private final boolean pitched;

		Loop(FishTankBlockEntity be, SoundEvent sound, Predicate<FishTankBlockEntity> on, boolean pitched) {
			super(sound);
			this.tank = new WeakReference<>(be);
			this.on = on;
			this.pitched = pitched;
			BlockPos at = be.getBlockPos();
			x = at.getX() + 0.5;
			y = at.getY() + 0.5;
			z = at.getZ() + 0.5;
			looping = true;
			delay = 0;
			volume = 0.01f;
		}

		static boolean canStart(SoundEvent sound, BlockPos at) {
			return Hearing.inRange(sound, 1, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, Hearing.LOOP_START_MARGIN);
		}

		@Override
		public void tick() {
			FishTankBlockEntity be = tank.get();
			if (be == null || be.isRemoved() || !Hearing.inRange(getLocation(), 1, x, y, z, Hearing.LOOP_STOP_MARGIN)) {
				stop();
				return;
			}
			boolean running = on.test(be);
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
