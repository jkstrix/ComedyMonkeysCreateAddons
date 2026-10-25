package com.comedymonkey.createaddons.content.line;

import net.minecraft.util.StringRepresentable;

/** Where a Line Post block sits in its pole: alone, at the foot, part-way up, or at the head that carries the insulators. */
public enum PostRole implements StringRepresentable {
	SINGLE("single"),
	FOOT("foot"),
	POLE("pole"),
	HEAD("head");

	private final String name;

	PostRole(String name) {
		this.name = name;
	}

	/** It carries insulators (and branches): the top of the pole. */
	public boolean isHead() {
		return this == SINGLE || this == HEAD;
	}

	/** It stands on the surface (the wire beside it joins the pole here). */
	public boolean isFoot() {
		return this == SINGLE || this == FOOT;
	}

	public static PostRole of(boolean below, boolean above) {
		return below ? above ? POLE : HEAD : above ? FOOT : SINGLE;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
