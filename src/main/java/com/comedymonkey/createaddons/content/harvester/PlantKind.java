package com.comedymonkey.createaddons.content.harvester;

import java.util.Locale;

import net.minecraft.util.StringRepresentable;

/** What an Auto Harvester has growing in it: decides the soil, the generic plant drawn and the tool that cuts it. */
public enum PlantKind implements StringRepresentable {
	/** Empty planter, bare dirt. */
	NONE,
	/** Seeds, crops and other small plants: farmland, cut with the harvester reel. */
	CROP,
	/** Saplings: dirt, felled with the saw. */
	TREE;

	@Override
	public String getSerializedName() {
		return name().toLowerCase(Locale.ROOT);
	}
}
