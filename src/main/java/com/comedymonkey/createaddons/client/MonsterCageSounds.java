package com.comedymonkey.createaddons.client;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.ToDoubleFunction;

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
 * server. A loop holds its cage weakly, so the map's weak keys can let a removed cage go.
 */
public final class MonsterCageSounds {
	private static final Map<MonsterCageBlockEntity, State> STATES = new WeakHashMap<>();
	/** Only the nearest few cages' hums and bubbles play: a wall of cages would take every sound channel. */
	private static final NearestLoops HUMS = new NearestLoops(), BUBBLES = new NearestLoops();

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
		// worked out once this tick, by the block entity, for its window too
		double distanceSqr = be.getViewDistanceSqr();
		if (be.isNetherite()) {
			SoundEvent hum = ModSounds.cage("field_hum");
			if (be.hasMonster() && (be.isFieldUp() || be.isWarning()) && wanted(s.hum, hum, HUMS, be, distanceSqr))
				mc.getSoundManager().play(s.hum = new Loop(be, hum, MonsterCageSounds::humVolume, HUMS));
		}
		if (be.isFlooded()) {
			SoundEvent bubbles = ModSounds.cage("bubbles_loop");
			if (be.getWater().getFluidAmount() > 0 && wanted(s.bubbles, bubbles, BUBBLES, be, distanceSqr))
				mc.getSoundManager().play(s.bubbles = new Loop(be, bubbles, b -> b.getWater().getFluidAmount() > 0 ? 1 : 0, BUBBLES));
		}
	}

	/**
	 * For a cage whose loop should run: offers it among the nearest (if it's in earshot, a playing one a little further
	 * out), and whether a new one should start.
	 */
	private static boolean wanted(Loop loop, SoundEvent sound, NearestLoops kind, MonsterCageBlockEntity be, double distanceSqr) {
		boolean playing = !MachineLoop.ended(loop);
		if (!Hearing.inRange(sound, 1, distanceSqr, playing ? Hearing.LOOP_STOP_MARGIN : Hearing.LOOP_START_MARGIN))
			return false;
		return kind.offer(be, distanceSqr, playing) && !playing;
	}

	/** Full while the shield shows, a murmur through a countdown's drop-outs, silent when the field is down. */
	private static double humVolume(MonsterCageBlockEntity be) {
		return be.getLevel() != null && MonsterCageRenderer.shieldOn(be, be.getLevel().getGameTime()) ? 1 : be.isWarning() ? 0.05 : 0;
	}

	/**
	 * A running loop: follows its volume (snapping, so the hum cuts out crisply), fades out when nearer cages take its
	 * place, stops once silent or gone.
	 */
	private static final class Loop extends MachineLoop {
		private final WeakReference<MonsterCageBlockEntity> cage;
		private final ToDoubleFunction<MonsterCageBlockEntity> want;
		private final NearestLoops kind;
		private int silent;

		Loop(MonsterCageBlockEntity be, SoundEvent sound, ToDoubleFunction<MonsterCageBlockEntity> want, NearestLoops kind) {
			super(sound);
			this.cage = new WeakReference<>(be);
			this.want = want;
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
			MonsterCageBlockEntity be = cage.get();
			if (be == null || be.isRemoved() || !Hearing.inRange(getLocation(), 1, x, y, z, Hearing.LOOP_STOP_MARGIN)) {
				stop();
				return;
			}
			float target = (float) Math.max(0, Math.min(1, want.applyAsDouble(be)));
			if (target > 0 && !kind.allows(be)) {
				// a nearer cage has its place: out gently, not with the hum's hard cut (kept for the power going)
				volume -= 0.1f;
				if (volume <= 0.01f)
					stop();
				return;
			}
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
