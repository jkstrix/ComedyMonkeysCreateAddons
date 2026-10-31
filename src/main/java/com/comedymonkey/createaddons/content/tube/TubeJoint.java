package com.comedymonkey.createaddons.content.tube;

import net.minecraft.util.StringRepresentable;

/**
 * One side of a Clockwork Tube: nothing there, joined (its arm runs to the face), or capped (something it could join is
 * there, but the joint is shut by a wrench or refused: a second Receiver, or a network past its length).
 */
public enum TubeJoint implements StringRepresentable {
	NONE("none"),
	OPEN("open"),
	CAPPED("capped");

	private final String name;

	TubeJoint(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
