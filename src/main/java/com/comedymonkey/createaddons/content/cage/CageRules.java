package com.comedymonkey.createaddons.content.cage;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.common.Tags;

/**
 * Which monsters the Monster Cages take, and what each new one costs (design/specs/monster-cage-and-fish-tank.md, D24,
 * D26, D52). Everything is by entity tag, so packs can move monsters between cages: {@code monster_cage/andesite} (the
 * common Overworld ones, 1 Monster Chow), {@code /brass} (Nether, End, ocean and pillager-type, 2), {@code /rare} (4),
 * {@code /netherite} (the Wither, its own cost and cage), {@code /flooded} (only in a Flooded cage) and
 * {@code /blacklist}. Any other monster (a modded one) counts as Brass. Bosses are never caged, except the Wither.
 */
public final class CageRules {
	public static final TagKey<EntityType<?>> ANDESITE = tag("andesite");
	public static final TagKey<EntityType<?>> BRASS = tag("brass");
	public static final TagKey<EntityType<?>> RARE = tag("rare");
	public static final TagKey<EntityType<?>> NETHERITE = tag("netherite");
	public static final TagKey<EntityType<?>> FLOODED = tag("flooded");
	public static final TagKey<EntityType<?>> BLACKLIST = tag("blacklist");

	/** How a monster is kept: by which cage, and for how much chow. */
	public enum Kind {
		COMMON, NETHER, RARE, WITHER
	}

	private CageRules() {}

	private static TagKey<EntityType<?>> tag(String name) {
		return TagKey.create(Registries.ENTITY_TYPE, ComedyMonkeysCreateAddons.asResource("monster_cage/" + name));
	}

	/** What kind of caged monster this type is, or null if no cage takes it. */
	@Nullable
	public static Kind kindOf(@Nullable EntityType<?> type) {
		if (type == null || type.is(BLACKLIST))
			return null;
		if (type.is(NETHERITE))
			return Kind.WITHER;
		if (type.is(Tags.EntityTypes.BOSSES))
			return null;
		if (type.is(ANDESITE))
			return Kind.COMMON;
		if (type.is(RARE))
			return Kind.RARE;
		return Kind.NETHER;
	}

	/** Whether this living entity is a monster some cage could hold (an animal that's also hostile, a hoglin, isn't). */
	public static boolean isCageable(Entity entity) {
		return entity instanceof Enemy && !(entity instanceof Animal) && entity.isAlive() && kindOf(entity.getType()) != null;
	}

	public static boolean isFlooded(EntityType<?> type) {
		return type.is(FLOODED);
	}

	/** Zombies and husks can go into a Flooded cage, where they turn into drowned (D56). */
	public static boolean drowns(EntityType<?> type) {
		return type == EntityType.ZOMBIE || type == EntityType.HUSK;
	}

	/** Whether a cage of this tier, dry or flooded, takes this monster. */
	public static boolean fits(CageTier tier, boolean flooded, EntityType<?> type) {
		Kind kind = kindOf(type);
		if (kind == null)
			return false;
		if (flooded && drowns(type))
			return tier != CageTier.NETHERITE;
		if (flooded != isFlooded(type))
			return false;
		return switch (tier) {
			case ANDESITE -> kind == Kind.COMMON;
			case BRASS -> kind != Kind.WITHER;
			case NETHERITE -> kind == Kind.WITHER;
		};
	}

	/** Monster Chow a new one of these costs. */
	public static int chowCost(EntityType<?> type) {
		Kind kind = kindOf(type);
		if (kind == null)
			return Integer.MAX_VALUE;
		return switch (kind) {
			case COMMON -> CMConfig.Server.get(CMConfig.SERVER.cageChowCommon);
			case NETHER -> CMConfig.Server.get(CMConfig.SERVER.cageChowNether);
			case RARE -> CMConfig.Server.get(CMConfig.SERVER.cageChowRare);
			case WITHER -> CMConfig.Server.get(CMConfig.SERVER.cageChowWither);
		};
	}
}
