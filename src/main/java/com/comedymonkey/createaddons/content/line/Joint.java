package com.comedymonkey.createaddons.content.line;

/**
 * A joint between two pieces of line, as the wrench left it. Pieces of the same tier and colour join by themselves;
 * different colours (undyed counts as a colour) only join when wrenched together.
 */
public enum Joint {
	/** As placed: joined if the tier and colour match. */
	AUTO,
	/** Cut with a wrench: never joined. */
	CUT,
	/** Wrenched together: joined though the colours differ. */
	JOINED,
	/** A Power Line's way through the solid block on this side, drilled with a wrench, to the wire behind it. */
	DRILLED
}
