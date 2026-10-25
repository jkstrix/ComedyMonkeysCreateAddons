package com.comedymonkey.createaddons.content.line;

import net.minecraft.util.StringRepresentable;

/** What a side of a Fluid Line block joins: nothing, the next piece of line, a tank, or a tank through an Intake. */
public enum LineSide implements StringRepresentable {
	NONE("none"),
	LINE("line"),
	/** A block holding fluid: the line delivers into it. */
	TANK("tank"),
	/** An Intake is fitted on this side: the line draws from the block it faces. */
	INTAKE("intake");

	private final String name;

	LineSide(String name) {
		this.name = name;
	}

	public boolean connected() {
		return this != NONE;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
