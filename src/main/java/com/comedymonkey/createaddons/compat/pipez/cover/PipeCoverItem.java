package com.comedymonkey.createaddons.compat.pipez.cover;

import net.minecraft.world.item.Item;

/** A Pipez Cover, snug or full. Fitted by right-clicking a Pipez pipe; see {@link PipeCoverEvents}. */
public class PipeCoverItem extends Item {
	private final PipeCover.Kind kind;

	public PipeCoverItem(Properties properties, PipeCover.Kind kind) {
		super(properties);
		this.kind = kind;
	}

	public PipeCover.Kind kind() {
		return kind;
	}
}
