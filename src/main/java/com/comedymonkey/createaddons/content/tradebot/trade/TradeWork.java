package com.comedymonkey.createaddons.content.tradebot.trade;

import java.util.function.BooleanSupplier;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.advancement.ModAdvancements;
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
 * A visit's trades with a seated villager, all in one clip (D63, refining D35): he grips the goods from his drawer,
 * lobs them over, and on the clip's trade beat makes every trade he can pay for, one after another, at the villager's
 * real prices and stock (D17), paid into his emerald pouch (D48), with the XP vanilla would have dropped banked for
 * Nuggets of Experience (D57). He stops when the villager buys nothing more he carries, or his charge only covers a
 * trade and the way home. Until that beat he checks the villager is still seated, still there and not trading with a
 * player; if not, the visit is off and he keeps everything (D46).
 */
public class TradeWork implements BotWork {
	public static final String CLIP = "trade_villager", CLIP_OPEN = "trade_villager_open";
	/** The clip's trade beat: the package has landed in the villager's arms (its {@code villager_trade} cue). */
	public static final float TRADE_AT = 4.0f;

	private final TradeBotEntity bot;
	private final TradeSpot spot;
	/** Whether his charge allows one more trade (and still the way home). */
	private final BooleanSupplier affordable;
	private int trades;
	private boolean done, failed;

	public TradeWork(TradeBotEntity bot, TradeSpot spot, BooleanSupplier affordable) {
		this.bot = bot;
		this.spot = spot;
		this.affordable = affordable;
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
		trades = 0;
		done = failed = false;
		bot.playClip(clipName());
	}

	@Override
	public boolean tick() {
		AbstractVillager villager = spot.villager();
		VillagerClaims.claim(villager.getUUID(), bot.getUUID(), bot.level().getGameTime());
		if (villager.isAlive())
			villager.getLookControl().setLookAt(bot, 30, 30);
		float t = bot.clipElapsed(0);
		if (!done) {
			if (!stillThere()) {
				failed = true;
				return true;
			}
			if (t >= TRADE_AT) {
				while (affordable.getAsBoolean() && trade())
					trades++;
				done = true;
				if (trades == 0) {
					failed = true;
					return true;
				}
			}
		}
		Clip clip = bot.clip();
		return !clip.name().equals(clipName()) || clip.finished(t);
	}

	@Override
	public void abort() {
		VillagerClaims.release(spot.villager().getUUID(), bot.getUUID());
	}

	/** The visit's trades went through on this clip. */
	public boolean traded() {
		return trades > 0;
	}

	/** How many trades this clip made. */
	public int trades() {
		return trades;
	}

	/** The trade was called off (the villager stood up, left, or a player started trading with it). */
	public boolean failed() {
		return failed;
	}

	/** The villager can still trade with him: alive, seated where it was, and no player at its trades (D46). */
	public boolean stillThere() {
		return available() && !busy();
	}

	/** Alive and still seated where he planned to trade with it. */
	public boolean available() {
		AbstractVillager villager = spot.villager();
		return villager.isAlive() && !villager.isRemoved() && VillagerSeats.sitting(villager) && spot.villagerInPlace();
	}

	/** A player is at its trades: players always come first (D46). */
	public boolean busy() {
		return spot.villager().isTrading();
	}

	/** Which side of the villager he trades from. */
	public net.minecraft.core.Direction side() {
		return spot.side();
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
		if (com.comedymonkey.createaddons.content.tradebot.move.TradeBotMotion.debug)
			com.mojang.logging.LogUtils.getLogger().info("[Trade Bot {}] sold {} x{} to {} for {} emeralds, {} XP (uses {}/{})", bot.getId(),
				offer.getBaseCostA().getHoverName().getString(), price, villager.getName().getString(), offer.getResult().getCount(), xp, offer.getUses(),
				offer.getMaxUses());
		bot.addEarnings(offer.getResult().getCount(), xp);
		bot.tradedWith(villager.getUUID());
		bot.addCharge(-CMConfig.SERVER.tradeBotFePerTrade());
		villager.playSound(villager.getNotifyTradeSound(), 1, villager.getVoicePitch());
		int today = 0;
		if (bot.level().getBlockEntity(bot.getStation()) instanceof TradeBotStationBlockEntity station) {
			station.countTrade();
			today = station.tradesToday();
		}
		bot.awardOwner(ModAdvancements.TRADE_BOT_TRADE, today);
		if (villager instanceof TradeBotCustomer customer && customer.comedymonkeysaddons$levellingUp())
			bot.awardOwner(ModAdvancements.TRADE_BOT_LEVEL_UP, 0);
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
