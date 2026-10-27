package com.comedymonkey.createaddons.content.tank;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.pen.AnimalCapture;
import com.comedymonkey.createaddons.content.pen.BottledAnimalItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * What lives in a Fish Tank (design/specs/monster-cage-and-fish-tank.md, Part 2, F8, F20, F2): fish (cod, salmon,
 * tropical fish, pufferfish, and modded fish), squid and glow squid, and axolotls. Tadpoles and dolphins are refused
 * ({@code fish_tank_blacklist}). The Capture Bottle catches them all; the Animal Pens no longer take them.
 */
public final class TankRules {
	public static final TagKey<EntityType<?>> BLACKLIST =
		TagKey.create(Registries.ENTITY_TYPE, ComedyMonkeysCreateAddons.asResource("fish_tank_blacklist"));

	/** Whether each kind of entity is a water creature (by its class, which never changes). */
	private static final Map<EntityType<?>, Boolean> WATER_KINDS = new ConcurrentHashMap<>();

	private TankRules() {}

	public static boolean isTankCreature(@Nullable Entity entity) {
		return isWaterKind(entity) && !entity.getType().is(BLACKLIST);
	}

	private static boolean isWaterKind(@Nullable Entity entity) {
		return entity instanceof AbstractFish || entity instanceof Squid || entity instanceof Axolotl;
	}

	/**
	 * Whether a Bottled Animal holds a tank creature (so a pen refuses it and a tank takes it). Asked on every slot check
	 * and quick move, so what each kind of creature is gets worked out once; the tags are still read each time, as a
	 * datapack reload can change them.
	 */
	public static boolean livesInTank(ItemStack bottle, @Nullable Level level) {
		if (level == null || !(bottle.getItem() instanceof BottledAnimalItem))
			return false;
		EntityType<?> type = BottledAnimalItem.typeOf(bottle);
		if (type == null || type.is(AnimalCapture.BLACKLIST) || type.is(BLACKLIST))
			return false;
		return WATER_KINDS.computeIfAbsent(type, t -> isWaterKind(t.create(level)));
	}
}
