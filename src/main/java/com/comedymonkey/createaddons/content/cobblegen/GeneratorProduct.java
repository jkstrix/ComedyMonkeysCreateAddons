package com.comedymonkey.createaddons.content.cobblegen;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * What a generator makes. The block's hardness sets the drill-paced cycle time, so basalt (1.25) and blackstone
 * (1.5) come a little quicker than cobblestone (2), and tuff (1.5) the same. Cobblestone and tuff form where water
 * meets lava; basalt and blackstone where lava meets blue ice.
 */
public enum GeneratorProduct {
	COBBLESTONE("cobble_generator", Blocks.COBBLESTONE, "block/cobblestone", false),
	BASALT("basalt_generator", Blocks.BASALT, "block/basalt_side", true),
	BLACKSTONE("blackstone_generator", Blocks.BLACKSTONE, "block/blackstone", true),
	/** Lava meets water over gravel, the volcanic ash it presses into tuff. */
	TUFF("tuff_generator", Blocks.TUFF, "block/tuff", false);

	private final String langKey;
	private final Block block;
	private final ResourceLocation sprite;
	private final boolean blueIce;

	GeneratorProduct(String langKey, Block block, String sprite, boolean blueIce) {
		this.langKey = langKey;
		this.block = block;
		this.sprite = ResourceLocation.withDefaultNamespace(sprite);
		this.blueIce = blueIce;
	}

	/** Goggles header key, under {@code gui.goggles.}. */
	public String langKey() {
		return langKey;
	}

	public Item item() {
		return block.asItem();
	}

	public float hardness() {
		return block.defaultDestroyTime();
	}

	/** The block texture shown forming in the chamber. */
	public ResourceLocation sprite() {
		return sprite;
	}

	/** Lava meets a block of blue ice, rather than poured water. */
	public boolean usesBlueIce() {
		return blueIce;
	}
}
