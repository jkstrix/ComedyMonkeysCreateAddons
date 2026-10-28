package com.comedymonkey.createaddons.content.light;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.DyeColor;

/**
 * The Electric Lights' colours (design/specs/electric-lights.md, D13): a lens's, undyed or by its dye, muted to the house
 * palette, and the light it casts where it can be coloured (Colorful Lighting, or Veil): a dyed lens's hue, full and
 * stronger, so it reads over vanilla's white block light; an undyed one's warm white, as it is.
 */
public final class LightColours {
	/** Undyed: a warm incandescent glow. */
	public static final int WARM = 0xD99A4A;
	/** A sodium lamp's colour as it starts to warm up. */
	public static final int SODIUM = 0x8A2F22;
	/** The dyes' lens colours, muted and lifted for lit glass (the design's set), by {@link DyeColor#getId()}. */
	private static final int[] DYES = {
		0xD6D3C8, 0xD38A4C, 0xB979AD, 0x7FA9BF, 0xD2B964, 0x9CBA66, 0xCF97A5, 0x8C8F91,
		0xB0AFA8, 0x5FA6A6, 0x9270B4, 0x6E7CBD, 0xA7845F, 0x86A058, 0xC0665A, 0x5C5C62,
	};
	/** Black casts no light, so a black lens casts a deep ultraviolet. */
	private static final int BLACK_LIGHT = 0x5B3AA8;
	/** The green lens is olive, which casts the same as lime: green casts from a cooler green. */
	private static final int GREEN_LIGHT = 0x5E9E6A;
	/** How much stronger a dyed light's cast colour is than its lens (1 keeps the lens's own; undyed light keeps it). */
	private static final float SATURATE = 4.0f;

	private LightColours() {}

	/** A lit lens's colour, before its brightness step darkens it. */
	public static int lens(@Nullable DyeColor dye) {
		return dye == null ? WARM : DYES[dye.getId()];
	}

	/** The colour of the light it casts, as 0–1 red, green, blue: the lens's hue, at full (and strengthened if dyed). */
	public static float[] cast(@Nullable DyeColor dye, int lens) {
		float saturate = dye == null ? 1 : SATURATE;
		int c = dye == DyeColor.BLACK ? BLACK_LIGHT : dye == DyeColor.GREEN ? GREEN_LIGHT : lens;
		float[] rgb = {(c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f};
		float l = 0.2126f * rgb[0] + 0.7152f * rgb[1] + 0.0722f * rgb[2];
		for (int i = 0; i < 3; i++)
			rgb[i] = Math.max(0, l + (rgb[i] - l) * saturate);
		float max = Math.max(rgb[0], Math.max(rgb[1], rgb[2]));
		if (max <= 0)
			return new float[] {1, 1, 1};
		for (int i = 0; i < 3; i++)
			rgb[i] /= max;
		return rgb;
	}

	/** The colour a lit light casts, undyed or by its dye, as "#RRGGBB" (the coloured-light mods' emitters). */
	public static String castHex(@Nullable DyeColor dye) {
		float[] c = cast(dye, lens(dye));
		return String.format(java.util.Locale.ROOT, "#%02X%02X%02X", Math.round(c[0] * 255), Math.round(c[1] * 255), Math.round(c[2] * 255));
	}
}
