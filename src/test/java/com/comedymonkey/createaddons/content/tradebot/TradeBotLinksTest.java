package com.comedymonkey.createaddons.content.tradebot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.content.tradebot.TradeBotLinks.Role;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.testutil.TestConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.component.ItemContainerContents;

/** Picking containers (D61): takes from, then delivers to, then unlinked; four inputs at most, one output. */
class TradeBotLinksTest {
	@BeforeEach
	void config() {
		TestConfig.loadDefaults();
	}

	@Test
	void secondClickMakesTheOutputOnlyIfThereIsNone() {
		assertEquals(Role.TAKES, TradeBotLinks.nextRole(Role.NONE, false, 0, 4, false));
		assertEquals(Role.DELIVERS, TradeBotLinks.nextRole(Role.TAKES, false, 1, 4, false), "no output yet: it becomes the output");
		assertEquals(Role.NONE, TradeBotLinks.nextRole(Role.TAKES, false, 1, 4, true), "there's an output: it unlinks");
		assertEquals(Role.NONE, TradeBotLinks.nextRole(Role.DELIVERS, false, 0, 4, true));
	}

	@Test
	void beltsGoStraightToDeliversAndAFullStationTakesNoMore() {
		assertEquals(Role.DELIVERS, TradeBotLinks.nextRole(Role.NONE, true, 0, 4, true), "belts only take deliveries");
		assertEquals(Role.DELIVERS, TradeBotLinks.nextRole(Role.NONE, false, 4, 4, false), "four inputs, no output");
		assertEquals(Role.NONE, TradeBotLinks.nextRole(Role.NONE, false, 4, 4, true), "four inputs and an output");
	}

	@Test
	void theStationKeepsOneOutputAndRelativeLinks() {
		BlockPos at = new BlockPos(100, 64, -40);
		TradeBotStationBlockEntity station = new TradeBotStationBlockEntity(at, ModBlocks.TRADE_BOT.get().defaultBlockState());
		station.claim(UUID.randomUUID(), UUID.randomUUID(), new TradeBotData(0, 0, 0, Map.of(), ItemContainerContents.EMPTY, List.of(), Optional.empty()));
		BlockPos chest = at.offset(3, 0, 1), barrel = at.offset(-2, 1, 0), depot = at.offset(0, 0, 4);
		station.setRole(chest, Role.TAKES);
		station.setRole(barrel, Role.TAKES);
		station.setRole(depot, Role.DELIVERS);
		assertEquals(List.of(chest, barrel), station.inputPositions());
		assertEquals(depot, station.outputPosition());
		station.setRole(barrel, Role.DELIVERS); // a new output replaces the old one, and stops being an input
		assertEquals(List.of(chest), station.inputPositions());
		assertEquals(barrel, station.outputPosition());
		assertEquals(Role.NONE, station.roleOf(depot));
		station.setRole(barrel, Role.NONE);
		assertNull(station.outputPosition());
	}

	@Test
	void rangeIsMeasuredFromTheStation() {
		BlockPos station = BlockPos.ZERO;
		assertEquals(true, TradeBotLinks.inRange(station, new BlockPos(40, 0, 40)));
		assertEquals(false, TradeBotLinks.inRange(station, new BlockPos(60, 0, 60)));
	}
}
