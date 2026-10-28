package com.comedymonkey.createaddons.content.light;

import net.minecraft.util.StringRepresentable;

/** The finish of a Lamp Post, and of the arm a lamp head hangs on (none: the head stands on its own). */
public enum ArmFinish implements StringRepresentable {
	NONE("none"),
	IRON("iron"),
	STEEL("steel");

	private final String name;

	ArmFinish(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
