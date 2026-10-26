package com.comedymonkey.createaddons.content.line;

/**
 * A joint between two pieces of line, as the wrench left it. Pieces of the same tier and colour join by themselves;
 * two colours (plain is one) never join.
 */
public enum Joint {
	/** As placed: joined if the tier and colour match. */
	AUTO,
	/** Cut with a wrench: never joined. */
	CUT,
	/** Wrenched together: brass and copper Power Lines at a Breaker. */
	JOINED,
	/** A Power Line's way through the solid block on this side, drilled with a wrench, to the wire behind it. */
	DRILLED
}
