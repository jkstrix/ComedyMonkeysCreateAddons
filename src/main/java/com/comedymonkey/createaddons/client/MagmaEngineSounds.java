package com.comedymonkey.createaddons.client;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.DoubleSupplier;

import org.joml.Vector3f;

import com.comedymonkey.createaddons.content.engine.MagmaEngineBlock;
import com.comedymonkey.createaddons.content.engine.MagmaEngineBlockEntity;
import com.comedymonkey.createaddons.registry.ModSounds;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Magma Engines' sounds and exhaust (design/sound/magma-engine/magma-engine.md), run from their client ticks: a chug
 * and a puff of smoke from that cylinder's exhaust each time a piston reaches the top of its stroke (from the same
 * shaft angle the renderer uses, so they keep time with the pistons), the firebox and the supercharger's whine as loops,
 * Minecraft's own lava pop now and then, a loud start and sputter-out, and ticking as it cools. They run in a base all
 * day, so the running sounds are quiet and carry only 8 blocks.
 */
public final class MagmaEngineSounds {
	/** How long it ticks as it cools after stopping (ticks), and how often on average. */
	private static final int COOL_TICKS = 200, COOL_CHANCE = 60;
	/** Ticks between lava pops on average (about 15 s). */
	private static final int POP_CHANCE = 300;
	/** The exhaust mouths, in the model (pixels, shaft to the north): on the front of each cylinder head. */
	private static final float EXHAUST_X = 2.27f, EXHAUST_Y = 13.73f, EXHAUST_Z = 3.6f;
	private static final Map<MagmaEngineBlockEntity, State> STATES = new WeakHashMap<>();

	private MagmaEngineSounds() {}

	/** Forgets every engine when the client's world goes: their block entities, and their loops, hold that world. */
	public static void forget() {
		STATES.clear();
	}

	private static final class State {
		boolean lit;
		/** Which half of the turn the crank was in last tick: a change is a piston at the top of its stroke. */
		int half = -1;
		int cooling;
		Loop firebox, blower;
	}

	public static void tick(MagmaEngineBlockEntity be) {
		Minecraft mc = Minecraft.getInstance();
		Level level = be.getLevel();
		if (mc.level == null || level != mc.level)
			return;
		Direction facing = be.getBlockState().getValue(MagmaEngineBlock.FACING);
		boolean lit = be.getBlockState().getValue(MagmaEngineBlock.LIT);
		boolean sup = be.isSupercharged();
		State s = STATES.get(be);
		if (s == null) {
			// Already running when it came into view: pick up the loops without replaying its start
			s = new State();
			s.lit = lit;
			// the loops hold their block entity, so a weak key alone never lets go: drop the ones unloaded or broken
			STATES.keySet().removeIf(BlockEntity::isRemoved);
			STATES.put(be, s);
		}
		RandomSource random = level.random;
		BlockPos pos = be.getBlockPos();
		if (lit && !s.lit)
			play(pos, ModSounds.engine("start"), 1, 0.97f + 0.06f * random.nextFloat());
		if (!lit && s.lit) {
			play(pos, ModSounds.engine("stop"), 1, 0.97f + 0.06f * random.nextFloat());
			s.cooling = COOL_TICKS;
		}
		s.lit = lit;

		// A piston at the top of its stroke: the left at half a turn, the right at a whole turn
		if (be.getSpeed() != 0) {
			float degrees = KineticBlockEntityRenderer.getAngleForBe(be, pos, facing.getAxis()) * Mth.RAD_TO_DEG;
			int half = Mth.floor(Mth.positiveModulo(degrees, 360) / 180);
			if (s.half >= 0 && half != s.half) {
				play(pos, ModSounds.engine("chug"), 1, (sup ? 1.1f : 1) * (0.95f + 0.1f * random.nextFloat()));
				puff(level, pos, facing, half == 1, random);
			}
			s.half = half;
		} else {
			s.half = -1;
		}

		// The loops: started when they're wanted, then they follow the fire and the shaft, and fade out on their own.
		// Out of earshot they stop, and start again (without the start sound) on coming back
		if (MachineLoop.ended(s.firebox) && lit && Loop.canStart(ModSounds.engine("firebox"), pos))
			mc.getSoundManager().play(s.firebox = new Loop(be, ModSounds.engine("firebox"),
				() -> be.getBlockState().getValue(MagmaEngineBlock.LIT) ? 1 : 0, () -> 1));
		if (sup && MachineLoop.ended(s.blower) && be.getSpeed() != 0 && Loop.canStart(ModSounds.engine("blower"), pos))
			mc.getSoundManager().play(s.blower = new Loop(be, ModSounds.engine("blower"),
				() -> Math.min(1, Math.abs(be.getSpeed()) / 128f), () -> 0.8 + 0.2 * Math.min(1, Math.abs(be.getSpeed()) / 128f)));

		if (lit) {
			if (random.nextInt(POP_CHANCE) == 0)
				play(pos, ModSounds.engine("lava_pop"), 0.2f, 0.85f + 0.3f * random.nextFloat());
		} else if (s.cooling > 0) {
			s.cooling--;
			if (random.nextInt(COOL_CHANCE) == 0)
				play(pos, ModSounds.engine("cool_tick"), 0.8f, 0.9f + 0.2f * random.nextFloat());
		}
	}

	/** A puff of smoke from one cylinder's exhaust, blown forward and a little out. */
	private static void puff(Level level, BlockPos pos, Direction facing, boolean left, RandomSource random) {
		float x = left ? EXHAUST_X : 16 - EXHAUST_X;
		Vec3 at = MagmaEngineBlock.modelPoint(facing, x, EXHAUST_Y, EXHAUST_Z);
		Vector3f push = MagmaEngineBlock.modelToWorld(facing).transformDirection(new Vector3f(left ? -0.01f : 0.01f, 0.012f, -0.03f));
		level.addParticle(random.nextInt(4) == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
			pos.getX() + at.x, pos.getY() + at.y, pos.getZ() + at.z, push.x, push.y + 0.01, push.z);
	}

	/** A one-shot from the engine, if anyone's in earshot. */
	private static void play(BlockPos at, SoundEvent sound, float volume, float pitch) {
		double x = at.getX() + 0.5, y = at.getY() + 0.5, z = at.getZ() + 0.5;
		if (Hearing.inRange(sound, volume, x, y, z, Hearing.ONE_SHOT_MARGIN))
			Minecraft.getInstance().level.playLocalSound(x, y, z, sound, SoundSource.BLOCKS, volume, pitch, false);
	}

	/** A running loop: eases toward its volume and pitch, and stops once it has faded out or its engine is gone. */
	private static final class Loop extends MachineLoop {
		private final MagmaEngineBlockEntity be;
		private final DoubleSupplier want, wantPitch;
		private float level;

		Loop(MagmaEngineBlockEntity be, SoundEvent sound, DoubleSupplier want, DoubleSupplier wantPitch) {
			super(sound);
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

		/** Whether a loop from this engine is in earshot, to start it. */
		static boolean canStart(SoundEvent sound, BlockPos at) {
			return Hearing.inRange(sound, 1, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, Hearing.LOOP_START_MARGIN);
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
