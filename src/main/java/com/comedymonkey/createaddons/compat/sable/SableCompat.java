package com.comedymonkey.createaddons.compat.sable;

import net.neoforged.fml.ModList;

/**
 * Whether Sable (the physics engine under Create Aeronautics) is installed. Kept apart from {@link AeronauticsContent}
 * so checking it doesn't load the classes that need Sable.
 */
public final class SableCompat {
	public static final String SABLE_ID = "sable";
	public static final boolean LOADED = ModList.get().isLoaded(SABLE_ID);

	private SableCompat() {}
}
