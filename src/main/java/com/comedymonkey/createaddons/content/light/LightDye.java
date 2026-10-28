package com.comedymonkey.createaddons.content.light;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;

/**
 * The dye an Electric Light's glow is coloured with, or none (D5, D13). It's part of the block state, not only its block
 * entity, so a coloured-light mod can give each colour its own light (Colorful Lighting reads emitters by state:
 * assets/comedymonkeysaddons/light/emitters.json), and re-dyeing a light re-lights the world around it at once.
 */
public enum LightDye implements StringRepresentable {
	NONE(null),
	WHITE(DyeColor.WHITE), ORANGE(DyeColor.ORANGE), MAGENTA(DyeColor.MAGENTA), LIGHT_BLUE(DyeColor.LIGHT_BLUE),
	YELLOW(DyeColor.YELLOW), LIME(DyeColor.LIME), PINK(DyeColor.PINK), GRAY(DyeColor.GRAY),
	LIGHT_GRAY(DyeColor.LIGHT_GRAY), CYAN(DyeColor.CYAN), PURPLE(DyeColor.PURPLE), BLUE(DyeColor.BLUE),
	BROWN(DyeColor.BROWN), GREEN(DyeColor.GREEN), RED(DyeColor.RED), BLACK(DyeColor.BLACK);

	@Nullable
	private final DyeColor dye;

	LightDye(@Nullable DyeColor dye) {
		this.dye = dye;
	}

	@Nullable
	public DyeColor dye() {
		return dye;
	}

	public static LightDye of(@Nullable DyeColor dye) {
		return dye == null ? NONE : values()[dye.getId() + 1];
	}

	@Override
	public String getSerializedName() {
		return dye == null ? "none" : dye.getSerializedName();
	}
}
