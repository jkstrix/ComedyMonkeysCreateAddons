package com.comedymonkey.createaddons.client;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.DoubleSupplier;

import com.comedymonkey.createaddons.content.electric.GeothermalControllerBlockEntity;
import com.comedymonkey.createaddons.registry.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * The Geothermal Turbine's sounds (design/sound/geothermal-turbine/geothermal-turbine.md), run from the controller's
 * client tick: the turbine whine and the firebox roar as loops following the rotor and the fire, the one-shots for its
 * changes of state (firing up, stalling, spinning down, settling), steam chuffs that quicken with the load, the odd
 * groan from the boiler, and ticking as it cools. Forming and coming apart are played by the server.
 */
public final class GeothermalTurbineSounds {
	/** Ticks between steam chuffs at no load and flat out. */
	private static final int CHUFF_SLOW = 24, CHUFF_FAST = 12;
	/** How long it ticks as it cools after it stops (ticks), and how often on average. */
	private static final int COOL_TICKS = 1200, COOL_CHANCE = 70;
	private static final Map<GeothermalControllerBlockEntity, State> STATES = new WeakHashMap<>();

	private GeothermalTurbineSounds() {}

	/** Forgets every turbine when the client's world goes: their block entities, and their loops, hold that world. */
	public static void forget() {
		STATES.clear();
	}

	private static final class State {
		boolean running, stalled, spinning;
		int chuff, groan, cooling;
		Loop whine, fire;
	}

	public static void tick(GeothermalControllerBlockEntity be) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || be.getLevel() != mc.level)
			return;
		boolean formed = be.isFormed(), running = formed && be.isRunning(), stalled = be.isStalled();
		float speed = be.rotorSpeed();
		State s = STATES.get(be);
		if (s == null) {
			// Already running when it came into view: pick up the loops without replaying its start
			s = new State();
			s.running = running;
			s.stalled = stalled;
			s.spinning = speed > 0.01f;
			s.groan = 160 + be.getLevel().random.nextInt(140);
			// the loops hold their block entity, so a weak key alone never lets go: drop the ones unloaded or broken
			STATES.keySet().removeIf(BlockEntity::isRemoved);
			STATES.put(be, s);
		}
		RandomSource random = be.getLevel().random;
		BlockPos centre = be.centre();

		if (running && !s.running) {
			play(centre, "relay_clack", 0.8f, 1);
			play(centre, "ignite", 1, 1);
			play(centre, "lamp_on", 0.7f, 1);
			if (speed < 0.05f)
				play(centre, "spin_up", 0.8f, 1);
			s.chuff = CHUFF_SLOW;
			s.groan = 160 + random.nextInt(140);
		}
		if (!running && s.running) {
			if (speed > 0.1f)
				play(centre, "spin_down", 0.8f, 1);
			s.cooling = COOL_TICKS;
		}
		if (stalled && !s.stalled) {
			play(centre, "relay_clack", 0.8f, 0.95f);
			play(centre, "stall", 0.9f, 1);
		}
		if (speed < 0.01f && s.spinning)
			play(centre, "settle", 0.7f, 0.95f + 0.1f * random.nextFloat());
		s.running = running;
		s.stalled = stalled;
		s.spinning = speed > 0.01f;

		// The loops: started when they're wanted, then they follow the rotor and the fire, and fade out on their own.
		// Kept low (halved after play-testing): they run the whole time it's on, under the one-shots. Out of earshot
		// they stop, and start again (without the start-up sounds) on coming back
		if (MachineLoop.ended(s.whine) && speed > 0.03f && Loop.canStart(centre, "turbine_whine"))
			mc.getSoundManager().play(s.whine = new Loop(be, "turbine_whine", () -> 0.3 * be.rotorSpeed(), () -> 0.6 + 0.4 * be.rotorSpeed()));
		if (MachineLoop.ended(s.fire) && running && Loop.canStart(centre, "firebox_roar"))
			mc.getSoundManager().play(s.fire = new Loop(be, "firebox_roar", () -> be.isFormed() && be.isRunning() ? 0.25 : 0, () -> 1));

		if (running) {
			if (--s.chuff <= 0) {
				play(centre, "steam_chuff", 0.5f, 0.94f + 0.12f * random.nextFloat());
				s.chuff = Math.round(CHUFF_SLOW - (CHUFF_SLOW - CHUFF_FAST) * be.load());
			}
			// Now and then, 8 to 15 seconds apart
			if (--s.groan <= 0) {
				play(centre, "boiler_groan", 0.45f, 0.92f + 0.16f * random.nextFloat());
				s.groan = 160 + random.nextInt(140);
			}
		} else if (s.cooling > 0) {
			s.cooling--;
			if (random.nextInt(COOL_CHANCE) == 0)
				play(centre, "cool_tick", 0.3f, 0.9f + 0.2f * random.nextFloat());
		}
	}

	/** A one-shot from the turbine's middle, if anyone's in earshot. */
	private static void play(BlockPos at, String sound, float volume, float pitch) {
		SoundEvent event = ModSounds.turbine(sound);
		double x = at.getX() + 0.5, y = at.getY() + 0.5, z = at.getZ() + 0.5;
		if (Hearing.inRange(event, volume, x, y, z, Hearing.ONE_SHOT_MARGIN))
			Minecraft.getInstance().level.playLocalSound(x, y, z, event, SoundSource.BLOCKS, volume, pitch, false);
	}

	/** One of the turbine's loops: eases toward its volume and pitch, and stops once it has faded out. */
	private static final class Loop extends MachineLoop {
		private final GeothermalControllerBlockEntity be;
		private final DoubleSupplier want, wantPitch;
		private float level;

		Loop(GeothermalControllerBlockEntity be, String sound, DoubleSupplier want, DoubleSupplier wantPitch) {
			super(ModSounds.turbine(sound));
			this.be = be;
			this.want = want;
			this.wantPitch = wantPitch;
			BlockPos centre = be.centre();
			x = centre.getX() + 0.5;
			y = centre.getY() + 0.5;
			z = centre.getZ() + 0.5;
			looping = true;
			delay = 0;
			volume = 0.01f;
		}

		/** Whether a loop is in earshot of the turbine's middle, to start it. */
		static boolean canStart(BlockPos centre, String sound) {
			return Hearing.inRange(ModSounds.turbine(sound), 1, centre.getX() + 0.5, centre.getY() + 0.5, centre.getZ() + 0.5,
				Hearing.LOOP_START_MARGIN);
		}

		@Override
		public void tick() {
			if (be.isRemoved() || !Hearing.inRange(getLocation(), 1, x, y, z, Hearing.LOOP_STOP_MARGIN)) {
				stop();
				return;
			}
			float target = (float) Math.max(0, Math.min(1, want.getAsDouble()));
			level += (target - level) * 0.15f;
			if (target == 0 && level < 0.02f) {
				stop();
				return;
			}
			volume = Math.max(0.01f, level);
			pitch = (float) wantPitch.getAsDouble();
		}

		@Override
		public boolean canStartSilent() {
			return true;
		}
	}
}
