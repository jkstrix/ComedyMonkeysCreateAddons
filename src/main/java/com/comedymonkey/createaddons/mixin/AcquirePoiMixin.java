package com.comedymonkey.createaddons.mixin;

import java.util.Set;
import java.util.stream.Collectors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.comedymonkey.createaddons.content.VillagerWorkstations;
import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.AcquirePoi;
import net.minecraft.world.entity.ai.village.poi.PoiType;

/**
 * A villager made to give up its workstation with a wrench leaves that one alone for a while
 * ({@link VillagerWorkstations#ignores}): vanilla picks the nearest free workstations it can walk to, and here that one
 * drops out of the villager's picks. Every workstation a villager takes, unemployed or not, goes through here.
 */
@Mixin(AcquirePoi.class)
public abstract class AcquirePoiMixin {
	// (the call sits in a lambda of create(), the only caller in the class)
	@ModifyArg(method = "*", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/world/entity/ai/behavior/AcquirePoi;findPathToPois(Lnet/minecraft/world/entity/Mob;Ljava/util/Set;)Lnet/minecraft/world/level/pathfinder/Path;"),
		index = 1, require = 1)
	private static Set<Pair<Holder<PoiType>, BlockPos>> comedymonkeysaddons$skipIgnored(Mob mob, Set<Pair<Holder<PoiType>, BlockPos>> pois) {
		return pois.stream().anyMatch(poi -> VillagerWorkstations.ignores(mob, poi.getSecond()))
			? pois.stream().filter(poi -> !VillagerWorkstations.ignores(mob, poi.getSecond())).collect(Collectors.toSet())
			: pois;
	}
}
