package com.comedymonkey.createaddons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.comedymonkey.createaddons.content.clock.CuckooClocks;
import com.comedymonkey.createaddons.content.tradebot.trade.TradeBotCustomer;

import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Near a running Cuckoo Clock a villager restocks at 8:00 and 12:00 instead of vanilla's times (see
 * {@link CuckooClocks#shouldRestock}). Vanilla asks this when the villager starts work at its workstation, and calls
 * {@code restock()} on a yes, so prices move with each restock as normal; only vanilla's catching up on restocks it
 * missed overnight doesn't happen while a clock keeps the time. Each time the clock decides, vanilla's own counters
 * are kept current (today's restock count back to zero, the day it last checked), so if the villager walks out of
 * reach or the clock stops, vanilla carries on from a clean start. Away from a clock nothing changes.
 * <p>
 * It also lets Trade Bot trade with the villager ({@link TradeBotCustomer}): vanilla's trade bookkeeping without a
 * player, so no XP orb and no trade event for a player who isn't there.
 */
@Mixin(Villager.class)
public abstract class VillagerMixin implements TradeBotCustomer {
	@Shadow
	private int numberOfRestocksToday;
	@Shadow
	private long lastRestockCheckDayTime;
	@Shadow
	private int villagerXp;
	@Shadow
	private int updateMerchantTimer;
	@Shadow
	private boolean increaseProfessionLevelOnUpdate;

	@Shadow
	protected abstract boolean shouldIncreaseLevel();

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

	@Override
	public boolean comedymonkeysaddons$levellingUp() {
		return increaseProfessionLevelOnUpdate;
	}

	/** Vanilla's {@code notifyTrade} and {@code rewardTradeXp} for a trade with Trade Bot, minus the player parts. */
	@Override
	public int comedymonkeysaddons$botTrade(MerchantOffer offer) {
		Villager villager = (Villager) (Object) this;
		offer.increaseUses();
		int xp = 3 + villager.getRandom().nextInt(4);
		villagerXp += offer.getXp();
		if (shouldIncreaseLevel()) {
			updateMerchantTimer = 40;
			increaseProfessionLevelOnUpdate = true;
			xp += 5;
		}
		return offer.shouldRewardExp() ? xp : 0;
	}
}
