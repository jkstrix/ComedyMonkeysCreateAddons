package com.comedymonkey.createaddons.content.tradebot.trade;

import java.util.List;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.VillagerSeats;
import com.comedymonkey.createaddons.content.tradebot.TradeBotEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Who Trade Bot can sell to (D7, D20): seated grown-up villagers and wandering traders within his range of the station,
 * with no player at their trades and no other bot on its way to them (D54). Only loaded ones are found, so a villager
 * in an unloaded chunk waits for another trip (D55).
 */
public final class Buyers {
	private Buyers() {}

	public static List<AbstractVillager> near(TradeBotEntity bot, BlockPos station) {
		int range = CMConfig.SERVER.tradeBotRange();
		Vec3 centre = Vec3.atCenterOf(station);
		long now = bot.level().getGameTime();
		return bot.level().getEntitiesOfClass(AbstractVillager.class, new AABB(station).inflate(range), villager ->
			villager.isAlive() && !villager.isBaby() && VillagerSeats.sitting(villager) && !villager.isTrading()
				&& villager.distanceToSqr(centre) <= range * range && !VillagerClaims.claimedByOther(villager.getUUID(), bot.getUUID(), now));
	}

	/** What {@code buyers} will take off him this trip, at his prices. */
	public static TradeDemand demand(TradeBotEntity bot, List<AbstractVillager> buyers) {
		TradeDemand demand = new TradeDemand();
		for (AbstractVillager villager : buyers)
			for (MerchantOffer offer : villager.getOffers())
				demand.add(offer, bot.reputationWith(villager.getUUID()));
		return demand;
	}
}
