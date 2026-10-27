package com.comedymonkey.createaddons.client;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.DoubleSupplier;

import com.comedymonkey.createaddons.content.cage.MonsterCageBlockEntity;
import com.comedymonkey.createaddons.registry.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The Monster Cages' running loops (design/sound/monster-cage/monster-cage.md), from their client tick: the Netherite
 * cage's containment hum, whose volume follows the shield (full while it's up, cutting out with it through a countdown,
 * silent when the field is down), and the Flooded cages' bubbles while there's water. The one-shots are played by the
 * server.
 */
public final class MonsterCageSounds {
	private static final Map<MonsterCageBlockEntity, State> STATES = new WeakHashMap<>();

	private MonsterCageSounds() {}

	public static void forget() {
		STATES.clear();
	}

	private static final class State {
		Loop hum, bubbles;
	}

	public static void tick(MonsterCageBlockEntity be) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || be.getLevel() != mc.level)
			return;
		if (!STATES.containsKey(be))
			STATES.keySet().removeIf(BlockEntity::isRemoved);
		State s = STATES.computeIfAbsent(be, b -> new State());
		BlockPos pos = be.getBlockPos();
		if (be.isNetherite()) {
			SoundEvent hum = ModSounds.cage("field_hum");
			if (MachineLoop.ended(s.hum) && be.hasMonster() && (be.isFieldUp() || be.isWarning()) && Loop.canStart(hum, pos))
				mc.getSoundManager().play(s.hum = new Loop(be, hum,
					() -> MonsterCageRenderer.shieldOn(be, be.getLevel().getGameTime()) ? 1 : be.isWarning() ? 0.05 : 0));
		}
		if (be.isFlooded()) {
			SoundEvent bubbles = ModSounds.cage("bubbles_loop");
			if (MachineLoop.ended(s.bubbles) && be.getWater().getFluidAmount() > 0 && Loop.canStart(bubbles, pos))
				mc.getSoundManager().play(s.bubbles = new Loop(be, bubbles, () -> be.getWater().getFluidAmount() > 0 ? 1 : 0));
		}
	}

	/** A running loop: follows its volume (snapping, so the hum cuts out crisply), stops once silent or gone. */
	private static final class Loop extends MachineLoop {
		private final MonsterCageBlockEntity be;
		private final DoubleSupplier want;
		private int silent;

		Loop(MonsterCageBlockEntity be, SoundEvent sound, DoubleSupplier want) {
			super(sound);
			this.be = be;
			this.want = want;
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
			if (be.isRemoved() || !Hearing.inRange(getLocation(), 1, x, y, z, Hearing.LOOP_STOP_MARGIN)) {
				stop();
				return;
			}
			float target = (float) Math.max(0, Math.min(1, want.getAsDouble()));
			// the cut-outs are hard electrical cuts, with a short surge as the power comes back
			volume = target > volume + 0.5f ? Math.min(1, target * 1.3f) : Math.max(0.01f, target);
			silent = target == 0 ? silent + 1 : 0;
			if (silent > 20)
				stop();
		}

		@Override
		public boolean canStartSilent() {
			return true;
		}
	}
}
