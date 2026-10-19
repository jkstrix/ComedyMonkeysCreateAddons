package com.comedymonkey.createaddons.content.chest;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModItems;
import com.mojang.serialization.Codec;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;

/** Chest upgrade tiers, lowest first. Each one doubles the stack size of the one before (configurable). */
public enum ChestTier implements StringRepresentable {
	COPPER("copper"),
	IRON("iron"),
	GOLD("gold"),
	DIAMOND("diamond"),
	NETHERITE("netherite");

	public static final Codec<ChestTier> CODEC = StringRepresentable.fromEnum(ChestTier::values);

	private final String name;

	ChestTier(String name) {
		this.name = name;
	}

	public int multiplier() {
		return CMConfig.SERVER.chestMultiplier(this);
	}

	/**
	 * Slots per chest half. Diamond keeps Gold's stack size and doubles the slots; Netherite keeps the doubled slots
	 * and raises the stack size again.
	 */
	public int slots() {
		return this == DIAMOND || this == NETHERITE ? ChestStacks.SLOTS * 2 : ChestStacks.SLOTS;
	}

	/** Netherite storage shrugs off explosions, like netherite blocks. */
	public boolean blastProof() {
		return this == NETHERITE;
	}

	public boolean isAbove(ChestTier other) {
		return ordinal() > other.ordinal();
	}

	public Item upgradeItem() {
		return switch (this) {
			case COPPER -> ModItems.COPPER_CHEST_UPGRADE.get();
			case IRON -> ModItems.IRON_CHEST_UPGRADE.get();
			case GOLD -> ModItems.GOLD_CHEST_UPGRADE.get();
			case DIAMOND -> ModItems.DIAMOND_CHEST_UPGRADE.get();
			case NETHERITE -> ModItems.NETHERITE_CHEST_UPGRADE.get();
		};
	}

	/** "Copper Barrel", "Iron Barrel"... */
	public Component barrelName() {
		return Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".barrel." + name);
	}

	/** "Copper Chest", "Iron Chest"... */
	public Component chestName() {
		return Component.translatable(ComedyMonkeysCreateAddons.MOD_ID + ".chest." + name);
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
