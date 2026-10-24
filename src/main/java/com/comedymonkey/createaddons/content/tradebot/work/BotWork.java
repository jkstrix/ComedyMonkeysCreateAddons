package com.comedymonkey.createaddons.content.tradebot.work;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Something he rolls up to and does on a clip: a go at a container, or a trade with a villager. */
public interface BotWork {
	/** The block he stands on, the exact spot on it, and which way he faces there. */
	BlockPos standBlock();

	Vec3 stand();

	float yaw();

	/** He's on the spot: start the clip. */
	void start();

	/** Runs the clip's beats; true once it's over. */
	boolean tick();

	/** Interrupted: put back anything he'd started (a lid, a claim). */
	void abort();
}
