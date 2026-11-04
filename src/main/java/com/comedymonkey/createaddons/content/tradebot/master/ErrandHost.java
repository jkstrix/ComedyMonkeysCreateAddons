package com.comedymonkey.createaddons.content.tradebot.master;

import java.util.Set;

import com.comedymonkey.createaddons.content.tradebot.TradeBotEntity;
import com.comedymonkey.createaddons.content.tradebot.move.TradeBotMotion.Route;
import com.comedymonkey.createaddons.content.tradebot.trade.Deal;
import com.comedymonkey.createaddons.content.tradebot.work.ContainerWork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.npc.AbstractVillager;

/** What a {@link MasterErrand} asks of his brain: rolling to a container or a villager to do something there. */
public interface ErrandHost {
	TradeBotEntity bot();

	/** Off to a container for a go at it: {@code NONE} no way there, {@code LATER} ask again next tick, {@code PLANNED} on his way. */
	Route visit(BlockPos container, ContainerWork.Mode mode, ContainerWork.Wanted wanted);

	/**
	 * Off to trade with {@code villager} ({@code failed}: sides of it he couldn't get to this trip), claiming it first;
	 * {@code NONE} when another bot has it or there's no spot to trade from.
	 */
	Route trade(AbstractVillager villager, Deal deal, Set<Direction> failed);
}
