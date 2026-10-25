package com.comedymonkey.createaddons.content.line;

import com.comedymonkey.createaddons.config.CMConfig;

import net.minecraft.util.StringRepresentable;

/** The two tiers of line. They never join each other; brass carries ten times as much. */
public enum LineTier implements StringRepresentable {
	COPPER("copper"),
	BRASS("brass");

	private final String name;

	LineTier(String name) {
		this.name = name;
	}

	/** Millibuckets a Fluid Line of this tier moves each tick, shared by every Intake on it. */
	public int fluidPerTick() {
		return this == COPPER ? CMConfig.SERVER.fluidLineCopperMbPerTick() : CMConfig.SERVER.fluidLineBrassMbPerTick();
	}

	/** FE a Power Line of this tier moves each tick, shared by everything that gives power on it. */
	public int powerPerTick() {
		return this == COPPER ? CMConfig.SERVER.powerLineCopperFePerTick() : CMConfig.SERVER.powerLineBrassFePerTick();
	}

	/** The longest span this tier's cable can hang, in blocks. */
	public int spanReach() {
		return this == COPPER ? CMConfig.SERVER.spanReachCopper() : CMConfig.SERVER.spanReachBrass();
	}

	/** Ticks a Fluid Line takes to fill, or drain, each block: unhurried on copper, quicker on brass. */
	public int drainTicksPerBlock() {
		return this == COPPER ? 8 : 3;
	}

	/** How fast the fluid slides along the sight slot, in pixels a second: slow on copper, quick on brass. */
	public float slotSpeed() {
		return this == COPPER ? 3f : 12f;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
