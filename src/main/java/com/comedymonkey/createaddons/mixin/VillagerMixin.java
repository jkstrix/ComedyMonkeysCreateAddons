package com.comedymonkey.createaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.comedymonkey.createaddons.content.clock.CuckooClocks;

import net.minecraft.world.entity.npc.Villager;

/**
 * Near a running Cuckoo Clock a villager restocks at 8:00 and 12:00 instead of vanilla's times (see
 * {@link CuckooClocks#shouldRestock}). Vanilla asks this when the villager starts work at its workstation, and calls
 * {@code restock()} on a yes, so prices move with each restock as normal; only vanilla's catching up on restocks it
 * missed overnight doesn't happen while a clock keeps the time. Each time the clock decides, vanilla's own counters
 * are kept current (today's restock count back to zero, the day it last checked), so if the villager walks out of
 * reach or the clock stops, vanilla carries on from a clean start. Away from a clock nothing changes.
 */
@Mixin(Villager.class)
public abstract class VillagerMixin {
	@Shadow
	private int numberOfRestocksToday;
	@Shadow
	private long lastRestockCheckDayTime;

	@Inject(method = "shouldRestock", at = @At("HEAD"), cancellable = true, require = 1)
	private void comedymonkeysaddons$clockRestock(CallbackInfoReturnable<Boolean> cir) {
		Villager villager = (Villager) (Object) this;
		Boolean clock = CuckooClocks.shouldRestock(villager);
		if (clock == null)
			return;
		numberOfRestocksToday = 0;
		lastRestockCheckDayTime = villager.level().getDayTime();
		cir.setReturnValue(clock);
	}
}
