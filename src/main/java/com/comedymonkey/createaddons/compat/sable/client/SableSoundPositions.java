package com.comedymonkey.createaddons.compat.sable.client;

import org.jetbrains.annotations.Nullable;

import dev.ryanhcode.sable.Sable;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Where a sound played from a block on a ship is really heard from. A ship's blocks are kept in a plot far from where
 * the ship is, and Sable moves their sounds out to the ship as the sound engine plays them; this does the same, so a
 * machine on a ship is in earshot as it's heard. Only loaded with Sable.
 */
public final class SableSoundPositions {
	private SableSoundPositions() {}

	/**
	 * The point in the world a sound at (x, y, z) is heard from: moved out to its ship if it's on one. Null if it's in
	 * the ships' plots but not on a ship the client knows of, so it can't be placed.
	 */
	@Nullable
	public static Vec3 heardFrom(double x, double y, double z) {
		Minecraft mc = Minecraft.getInstance();
		Vec3 at = new Vec3(x, y, z);
		if (mc.level == null)
			return at;
		try {
			if (Sable.HELPER.getContaining(mc.level, at) != null)
				return Sable.HELPER.projectOutOfSubLevel(mc.level, at);
			return Sable.HELPER.isInPlotGrid(mc.level, Mth.floor(x) >> 4, Mth.floor(z) >> 4) ? null : at;
		} catch (RuntimeException e) {
			return null;
		}
	}
}
