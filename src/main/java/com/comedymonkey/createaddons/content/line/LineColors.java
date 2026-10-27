package com.comedymonkey.createaddons.content.line;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.DyeColor;

/**
 * A line is just its colour: undyed it's its tier's material (Create's copper, the generators' mid brass), dyed it's the
 * dye, muted to the house palette (vanilla's dyes with about 40% less colour, at mid value or below).
 */
public final class LineColors {
	/** By {@link DyeColor#getId()}: the same muted colours as the design showcase. */
	private static final int[] DYES = {
		0x888C8B, 0xA55D21, 0x873F81, 0x337B93, 0xA58E31, 0x618C27, 0x985A6C, 0x464A4C,
		0x6B6B68, 0x267676, 0x663282, 0x383D7A, 0x674B37, 0x546629, 0x7F312C, 0x353537,
	};

	private LineColors() {}

	public static int tier(LineTier tier) {
		return tier == LineTier.COPPER ? 0xB26247 : 0x8F7638;
	}

	public static int of(LineTier tier, @Nullable DyeColor dye) {
		return dye == null ? tier(tier) : DYES[dye.getId()];
	}

	/**
	 * A Fluid Line's hubs and flanges: a shade darker than the pipe. Undyed, its metal's ring shade (Create's copper, the
	 * generators' brass); dyed, the dye at 84% (the coupling rings and corner collars stay the metal).
	 */
	public static int joint(LineTier tier, @Nullable DyeColor dye) {
		if (dye == null)
			return tier == LineTier.COPPER ? 0x9A5038 : 0x7E6832;
		int c = DYES[dye.getId()];
		return (int) ((c >> 16 & 0xFF) * 0.84) << 16 | (int) ((c >> 8 & 0xFF) * 0.84) << 8 | (int) ((c & 0xFF) * 0.84);
	}
}
