package com.comedymonkey.createaddons.content.light;

/**
 * A block lit by a {@link LightBlockEntity}: what it draws at each brightness step, and whether it warms up. The light it
 * casts follows from its block state (LIGHT), so each block works that out itself.
 */
public interface LitBlock {
	/** FE a tick at a brightness step. */
	double draw(int step);

	/** It warms up like a sodium lamp (P4). */
	default boolean warms() {
		return false;
	}
}
