package com.comedymonkey.createaddons.content.tradebot.trade;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.content.VillagerSeats;
import com.comedymonkey.createaddons.content.tradebot.TradeBotEntity;
import com.comedymonkey.createaddons.content.tradebot.TradeBotStationBlockEntity;
import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.content.tradebot.work.BotWork;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * One trade with a seated villager, one per clip (D35): he grips the goods from his drawer, lobs them over, and on the
 * clip's trade beat the trade happens at the villager's real price and stock (D17), paid into his emerald pouch (D48),
 * with the XP vanilla would have dropped banked for Nuggets of Experience (D57). Until that beat he checks the villager
 * is still seated, still there and not trading with a player; if not, the trade is off and he keeps everything (D46).
 */
public class TradeWork implements BotWork {
	public static final String CLIP = "trade_villager", CLIP_OPEN = "trade_villager_open";
	/** The clip's trade beat: the package has landed in the villager's arms (its {@code villager_trade} cue). */
	public static final float TRADE_AT = 4.0f;

	private final TradeBotEntity bot;
	private final TradeSpot spot;
	private boolean traded, failed;

	public TradeWork(TradeBotEntity bot, TradeSpot spot) {
		this.bot = bot;
		this.spot = spot;
	}

	public AbstractVillager villager() {
		return spot.villager();
	}

	@Override
	public BlockPos standBlock() {
		return spot.standBlock();
	}

	@Override
	public Vec3 stand() {
		return spot.stand();
	}

	@Override
	public float yaw() {
		return spot.yaw();
	}

	@Override
	public void start() {
		traded = failed = false;
		bot.playClip(clipName());
	}

	@Override
	public boolean tick() {
		AbstractVillager villager = spot.villager();
		VillagerClaims.claim(villager.getUUID(), bot.getUUID(), bot.level().getGameTime());
		if (villager.isAlive())
			villager.getLookControl().setLookAt(bot, 30, 30);
		float t = bot.clipElapsed(0);
		if (!traded) {
			if (!stillThere()) {
				failed = true;
				return true;
			}
			if (t >= TRADE_AT) {
				if (!trade()) {
					failed = true;
					return true;
				}
				traded = true;
			}
		}
		Clip clip = bot.clip();
		return !clip.name().equals(clipName()) || clip.finished(t);
	}

	@Override
	public void abort() {
		VillagerClaims.release(spot.villager().getUUID(), bot.getUUID());
	}

	/** The trade went through on this clip. */
	public boolean traded() {
		return traded;
	}

	/** The trade was called off (the villager stood up, left, or a player started trading with it). */
	public boolean failed() {
		return failed;
	}

	/** The villager can still trade with him: alive, seated where it was, and no player at its trades (D46). */
	public boolean stillThere() {
		AbstractVillager villager = spot.villager();
		return villager.isAlive() && !villager.isRemoved() && VillagerSeats.sitting(villager) && !villager.isTrading() && spot.villagerInPlace();
	}

	public boolean inPlace() {
		return spot.villagerInPlace();
	}

	private String clipName() {
		return spot.open() ? CLIP_OPEN : CLIP;
	}

	private boolean trade() {
		AbstractVillager villager = spot.villager();
		MerchantOffer offer = offerFor(bot, villager);
		if (offer == null)
			return false;
		int price = BotPricing.cost(offer, bot.reputationWith(villager.getUUID()));
		ItemStackHandler load = bot.getLoad();
		int left = price;
		for (int slot = 0; slot < load.getSlots() && left > 0; slot++)
			if (offer.getItemCostA().test(load.getStackInSlot(slot)))
				left -= load.extractItem(slot, left, false).getCount();
		int xp = villager instanceof TradeBotCustomer customer ? customer.comedymonkeysaddons$botTrade(offer) : wanderingTrade(villager, offer);
		bot.addEarnings(offer.getResult().getCount(), xp);
		bot.tradedWith(villager.getUUID());
		bot.addCharge(-CMConfig.SERVER.tradeBotFePerTrade());
		villager.playSound(villager.getNotifyTradeSound(), 1, villager.getVoicePitch());
		if (bot.level().getBlockEntity(bot.getStation()) instanceof TradeBotStationBlockEntity station)
			station.countTrade();
		return true;
	}

	/** A wandering trader's bookkeeping, as vanilla's {@code notifyTrade} does it, with the orb's XP returned instead. */
	private static int wanderingTrade(AbstractVillager trader, MerchantOffer offer) {
		offer.increaseUses();
		return offer.shouldRewardExp() ? 3 + trader.getRandom().nextInt(4) : 0;
	}

	/** The first offer of the villager's he can sell into with what he's carrying, at his price; null if none. */
	@Nullable
	public static MerchantOffer offerFor(TradeBotEntity bot, AbstractVillager villager) {
		int reputation = bot.reputationWith(villager.getUUID());
		for (MerchantOffer offer : villager.getOffers())
			if (BotPricing.buys(offer) && carried(bot.getLoad(), offer) >= BotPricing.cost(offer, reputation))
				return offer;
		return null;
	}

	private static int carried(ItemStackHandler load, MerchantOffer offer) {
		int count = 0;
		for (int slot = 0; slot < load.getSlots(); slot++) {
			ItemStack stack = load.getStackInSlot(slot);
			if (offer.getItemCostA().test(stack))
				count += stack.getCount();
		}
		return count;
	}
}
