package com.comedymonkey.createaddons.content.wastes;

import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * A part of a Clockwork Wastes city (design/specs/clockwork-wastes.md, P4): the four districts round the heart, each
 * with its own Substation, vault, lamps and bots, and the heart itself (the Star Reactor hall, heart vault, heart doors
 * and the Custodian's pylons). City blocks remember which they belong to ({@link CityLink}).
 */
public enum District implements StringRepresentable {
	FOUNDRY, WORKSHOPS, RAIL_YARD, QUARTERS, HEART;

	/** The four that wake by their Substations (not the heart). */
	public static final District[] DISTRICTS = {FOUNDRY, WORKSHOPS, RAIL_YARD, QUARTERS};

	public boolean isHeart() {
		return this == HEART;
	}

	/** "The Foundry", "the Rail Yard"... as the action bar names it. */
	public Component displayName() {
		return Component.translatable("comedymonkeysaddons.wastes.district." + getSerializedName());
	}

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static District byId(int id) {
		return values()[Math.floorMod(id, values().length)];
	}
}
