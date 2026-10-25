package com.comedymonkey.createaddons.client;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

import com.comedymonkey.createaddons.content.electric.ElectricMotorBlock;
import com.comedymonkey.createaddons.content.electric.ElectricMotorBlockEntity;
import com.comedymonkey.createaddons.content.electric.LavaGeneratorBlockEntity;
import com.comedymonkey.createaddons.registry.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * The Lava Generator's and the Electric Motor's sounds (design/sound/lava-generator/lava-generator.md and
 * design/sound/electric-motor/electric-motor.md), run from their client ticks. They run in a base all day, so what
 * plays while they run (the generator's whir and burn, Minecraft's own lava pop, the motor's hum) is quiet and carries
 * only 8 blocks; starting up and shutting down are loud. The motor's settings click and sparks are played elsewhere.
 */
public final class ElectricSounds {
	/** How long the generator ticks as it cools after going out (ticks), and how often on average. */
	private static final int COOL_TICKS = 200, COOL_CHANCE = 60;
	/** Ticks between lava pops on average (about 15 s). */
	private static final int POP_CHANCE = 300;
	private static final Map<LavaGeneratorBlockEntity, Generator> GENERATORS = new WeakHashMap<>();
	private static final Map<ElectricMotorBlockEntity, Motor> MOTORS = new WeakHashMap<>();

	private ElectricSounds() {}

	/** Forgets every generator and motor when the client's world goes: their block entities, and their loops, hold that world. */
	public static void forget() {
		GENERATORS.clear();
		MOTORS.clear();
	}

	private static final class Generator {
		boolean lit;
		int cooling;
		Loop whir, burn;
	}

	private static final class Motor {
		boolean powered;
		Loop hum;
	}

	public static void tickGenerator(LavaGeneratorBlockEntity be) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || be.getLevel() != mc.level)
			return;
		boolean lit = be.isLit();
		float speed = be.rotorSpeed();
		Generator s = GENERATORS.get(be);
		if (s == null) {
			// Already running when it came into view: pick up the loops without replaying its start
			s = new Generator();
			s.lit = lit;
			// the loops hold their block entity, so a weak key alone never lets go: drop the ones unloaded or broken
			GENERATORS.keySet().removeIf(BlockEntity::isRemoved);
			GENERATORS.put(be, s);
		}
		RandomSource random = be.getLevel().random;
		BlockPos pos = be.getBlockPos();
		if (lit && !s.lit) {
			play(pos, ModSounds.generator("ignite"), 1, 1);
			if (speed < 0.05f)
				play(pos, ModSounds.generator("spin_up"), 1, 1);
		}
		if (!lit && s.lit) {
			play(pos, ModSounds.generator("fire_out"), 1, 1);
			if (speed > 0.1f)
				play(pos, ModSounds.generator("spin_down"), 1, 1);
			s.cooling = COOL_TICKS;
		}
		s.lit = lit;

		// The loops follow the wheel and the fire, and fade out on their own
		if ((s.whir == null || s.whir.isStopped()) && speed > 0.03f)
			mc.getSoundManager().play(s.whir = new Loop(ModSounds.generator("whir"), pos, be::isRemoved,
				be::rotorSpeed, () -> 0.6 + 0.4 * be.rotorSpeed()));
		if ((s.burn == null || s.burn.isStopped()) && lit)
			mc.getSoundManager().play(s.burn = new Loop(ModSounds.generator("burn"), pos, be::isRemoved,
				() -> be.isLit() ? 1 : 0, () -> 1));

		if (lit) {
			if (random.nextInt(POP_CHANCE) == 0)
				play(pos, ModSounds.generator("lava_pop"), 0.4f, 0.85f + 0.3f * random.nextFloat());
		} else if (s.cooling > 0) {
			s.cooling--;
			if (random.nextInt(COOL_CHANCE) == 0)
				play(pos, ModSounds.generator("cool_tick"), 0.8f, 0.9f + 0.2f * random.nextFloat());
		}
	}

	public static void tickMotor(ElectricMotorBlockEntity be) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || be.getLevel() != mc.level)
			return;
		boolean powered = be.getBlockState().getValue(ElectricMotorBlock.POWERED);
		Motor s = MOTORS.get(be);
		if (s == null) {
			s = new Motor();
			s.powered = powered;
			MOTORS.keySet().removeIf(BlockEntity::isRemoved);
			MOTORS.put(be, s);
		}
		BlockPos pos = be.getBlockPos();
		if (powered != s.powered)
			play(pos, ModSounds.motor(powered ? "start" : "stop"), 1, 0.97f + 0.06f * be.getLevel().random.nextFloat());
		s.powered = powered;
		if ((s.hum == null || s.hum.isStopped()) && powered)
			mc.getSoundManager().play(s.hum = new Loop(ModSounds.motor("hum"), pos, be::isRemoved,
				() -> be.getBlockState().getValue(ElectricMotorBlock.POWERED) ? 1 : 0, () -> humPitch(be.getSpeedSetting())));
	}

	/** The hum rises with the set speed: about 0.7 at 16 RPM to 1.2 at 256 RPM. */
	static float humPitch(int rpm) {
		return Mth.clamp(0.7f + 0.125f * (Mth.log2(Math.max(rpm, 1)) - 4), 0.5f, 1.2f);
	}

	private static void play(BlockPos at, SoundEvent sound, float volume, float pitch) {
		Minecraft.getInstance().level.playLocalSound(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, sound,
			SoundSource.BLOCKS, volume, pitch, false);
	}

	/** A running loop: eases toward its volume and pitch, and stops once it has faded out or its block is gone. */
	private static final class Loop extends AbstractTickableSoundInstance {
		private final BooleanSupplier removed;
		private final DoubleSupplier want, wantPitch;
		private float level;

		Loop(SoundEvent sound, BlockPos at, BooleanSupplier removed, DoubleSupplier want, DoubleSupplier wantPitch) {
			super(sound, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
			this.removed = removed;
			this.want = want;
			this.wantPitch = wantPitch;
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
			if (removed.getAsBoolean()) {
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
