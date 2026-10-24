package com.comedymonkey.createaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.comedymonkey.createaddons.content.VillagerSeats;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;

import net.minecraft.world.entity.Entity;

/**
 * Lowers a villager into a Create Seat (see {@link VillagerSeats}). Create adds this offset when it places a seat's
 * passenger, both for a seat in the world and for one on a moving contraption.
 */
@Mixin(SeatEntity.class)
public abstract class SeatEntityMixin {
	@Inject(method = "getCustomEntitySeatOffset", at = @At("HEAD"), cancellable = true, require = 1)
	private static void comedymonkeysaddons$villagerSits(Entity entity, CallbackInfoReturnable<Double> cir) {
		if (VillagerSeats.sits(entity))
			cir.setReturnValue(VillagerSeats.SEAT_OFFSET);
	}
}
