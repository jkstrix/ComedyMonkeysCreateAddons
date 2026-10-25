package com.comedymonkey.createaddons.client;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * A machine's running loop. The sound engine can let one go without stopping it (no free channel, the master volume
 * at 0, F3+T, /stopsound), and one it never took doesn't tick, so it would never say it had stopped; once it's had a
 * moment to start, a loop the sound manager isn't playing counts as ended too, so the machine starts another.
 */
abstract class MachineLoop extends AbstractTickableSoundInstance {
	/** How long a newly queued loop has to start playing before it counts as dropped (ticks). */
	private static final int START_GRACE_TICKS = 20;
	private final long queuedAt;

	MachineLoop(SoundEvent sound) {
		super(sound, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
		queuedAt = now();
	}

	/** Whether a machine needs a new one of this loop: it has none, it stopped, or the sound engine let it go. */
	static boolean ended(@Nullable MachineLoop loop) {
		if (loop == null || loop.isStopped())
			return true;
		if (now() - loop.queuedAt > START_GRACE_TICKS && !Minecraft.getInstance().getSoundManager().isActive(loop)) {
			loop.stop();
			return true;
		}
		return false;
	}

	private static long now() {
		Minecraft mc = Minecraft.getInstance();
		return mc.level == null ? 0 : mc.level.getGameTime();
	}
}
