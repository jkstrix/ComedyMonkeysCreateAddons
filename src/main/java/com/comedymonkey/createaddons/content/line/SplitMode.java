package com.comedymonkey.createaddons.content.line;

/** How a Splitter shares what passes it between the ends beyond it. A line with no Splitter shares evenly. */
public enum SplitMode {
	/** Every end that can take more gets the same. */
	EVEN,
	/** The nearest end is filled first, then the next. */
	NEAREST,
	/** One end at a time, in turn, each tick. */
	ROUND_ROBIN;

	public SplitMode next() {
		return values()[(ordinal() + 1) % values().length];
	}

	public String key() {
		return name().toLowerCase(java.util.Locale.ROOT);
	}
}
