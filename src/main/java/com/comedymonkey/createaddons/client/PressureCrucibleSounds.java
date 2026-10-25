package com.comedymonkey.createaddons.client;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.DoubleSupplier;

import com.comedymonkey.createaddons.content.crucible.PressureCrucibleBlockEntity;
import com.comedymonkey.createaddons.registry.ModSounds;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * The Pressure Crucible's sounds (design/sound/pressure-crucible/pressure-crucible.md), run from its client tick: the
 * gears as a loop while the shaft turns (higher the faster it turns), a creak as the platen starts down on each press,
 * and the burner's fire, or soul fire, as a loop that follows the heat below. The crunch at the end of a press is played
 * by the server. It runs in a base all day, so the running sounds are quiet and carry only 8 blocks.
 */
public final class PressureCrucibleSounds {
	/** Presses closer together than this (ticks) don't creak, so fast ones don't chatter. */
	private static final int CREAK_GAP = 20;
	private static final Map<PressureCrucibleBlockEntity, State> STATES = new WeakHashMap<>();

	private PressureCrucibleSounds() {}

	/** Forgets every crucible when the client's world goes: their block entities, and their loops, hold that world. */
	public static void forget() {
		STATES.clear();
	}

	private static final class State {
		boolean processing;
		float progress;
		long lastCreak = Long.MIN_VALUE;
		Loop gears, fire, soulFire;
	}

	public static void tick(PressureCrucibleBlockEntity be) {
		Minecraft mc = Minecraft.getInstance();
		Level level = be.getLevel();
		if (mc.level == null || level != mc.level)
			return;
		// the loops hold their block entity, so a weak key alone never lets go: drop the ones unloaded or broken
		if (!STATES.containsKey(be))
			STATES.keySet().removeIf(BlockEntity::isRemoved);
		State s = STATES.computeIfAbsent(be, b -> {
			// Already pressing when it came into view: don't creak for a press that started out of sight
			State fresh = new State();
			fresh.processing = b.isProcessing();
			fresh.progress = b.getProgress();
			return fresh;
		});

		// A press starting: processing turning on, or the progress starting over with presses back to back
		float progress = be.getProgress();
		boolean started = be.isProcessing() && (!s.processing || progress < s.progress - 0.3f);
		s.processing = be.isProcessing();
		s.progress = progress;
		long now = level.getGameTime();
		if (started && now - s.lastCreak >= CREAK_GAP) {
			s.lastCreak = now;
			BlockPos pos = be.getBlockPos();
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.crucible("creak"),
				SoundSource.BLOCKS, 1, 0.94f + 0.12f * level.random.nextFloat(), false);
		}

		// The loops: started when they're wanted, then they follow the shaft and the heat, and fade out on their own
		if ((s.gears == null || s.gears.isStopped()) && be.getSpeed() != 0)
			mc.getSoundManager().play(s.gears = new Loop(be, ModSounds.crucible("whirr"),
				() -> be.getSpeed() != 0 ? 1 : 0, () -> gearPitch(be.getSpeed())));
		HeatLevel heat = be.getHeatLevel();
		if ((s.fire == null || s.fire.isStopped()) && fireVolume(heat) > 0)
			mc.getSoundManager().play(s.fire = new Loop(be, ModSounds.crucible("fire"), () -> fireVolume(be.getHeatLevel()), () -> 1));
		if ((s.soulFire == null || s.soulFire.isStopped()) && heat == HeatLevel.SEETHING)
			mc.getSoundManager().play(s.soulFire = new Loop(be, ModSounds.crucible("fire_soul"),
				() -> be.getHeatLevel() == HeatLevel.SEETHING ? 1 : 0, () -> 1));
	}

	/** 0.8 at 16 RPM up to 1.3 at 256 RPM, evenly per doubling. */
	private static double gearPitch(float speed) {
		float rpm = Mth.clamp(Math.abs(speed), 16, 256);
		return 0.8 + 0.5 * (Math.log(rpm / 16) / Math.log(16));
	}

	/** The plain fire: quiet while it smoulders, full when heated; the soul fire replaces it when superheated. */
	private static double fireVolume(HeatLevel heat) {
		return switch (heat) {
			case SMOULDERING, FADING -> 0.4;
			case KINDLED -> 1;
			default -> 0;
		};
	}

	/** A running loop: eases toward its volume and pitch, and stops once it has faded out or its crucible is gone. */
	private static final class Loop extends AbstractTickableSoundInstance {
		private final PressureCrucibleBlockEntity be;
		private final DoubleSupplier want, wantPitch;
		private float level;

		Loop(PressureCrucibleBlockEntity be, SoundEvent sound, DoubleSupplier want, DoubleSupplier wantPitch) {
			super(sound, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
			this.be = be;
			this.want = want;
			this.wantPitch = wantPitch;
			BlockPos at = be.getBlockPos();
			x = at.getX() + 0.5;
			y = at.getY() + 0.5;
			z = at.getZ() + 0.5;
			looping = true;
			delay = 0;
			volume = 0.01f;
			pitch = (float) wantPitch.getAsDouble();
		}

		@Override
		public void tick() {
			if (be.isRemoved()) {
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
