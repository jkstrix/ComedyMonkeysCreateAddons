package com.comedymonkey.createaddons.content.tradebot.move;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.content.tradebot.TradeBotBrain;
import com.comedymonkey.createaddons.content.tradebot.TradeBotStationBlock;
import com.comedymonkey.createaddons.content.tradebot.move.TradeBotMotion.Step;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

class TradeBotMotionTest {
	@Test
	void smallRisesRollAndTallerOnesHop() {
		assertNull(TradeBotMotion.classify(0, false, false));
		assertNull(TradeBotMotion.classify(0.125, false, false), "a carpet or snow layer");
		assertNull(TradeBotMotion.classify(-0.25, false, false));
		assertEquals(Step.SLAB_UP, TradeBotMotion.classify(0.5, false, false));
		assertEquals(Step.BLOCK_UP, TradeBotMotion.classify(1, false, false));
		assertEquals(Step.STAIRS_UP, TradeBotMotion.classify(1, true, false));
		assertEquals(Step.SLAB_DOWN, TradeBotMotion.classify(-0.5, false, false));
		assertEquals(Step.BLOCK_DOWN, TradeBotMotion.classify(-1, false, false));
		assertEquals(Step.STAIRS_DOWN, TradeBotMotion.classify(-1, false, true));
	}

	@Test
	void yawFacesThePoint() {
		assertEquals(0, TradeBotMotion.yawTo(Vec3.ZERO, new Vec3(0, 0, 1)), 1e-4, "south");
		assertEquals(90, TradeBotMotion.yawTo(Vec3.ZERO, new Vec3(-1, 0, 0)), 1e-4, "west");
		assertEquals(-90, TradeBotMotion.yawTo(Vec3.ZERO, new Vec3(1, 0, 0)), 1e-4, "east");
		assertEquals(180, Math.abs(TradeBotMotion.yawTo(Vec3.ZERO, new Vec3(0, 0, -1))), 1e-4, "north");
	}

	@Test
	void dockingStartsOnTheGroundInFrontOfTheStation() {
		// the docking clip backs him 18 px into the cradle and up the 2 px it sits above the ground
		Vec3 dock = TradeBotStationBlock.dockPoint(BlockPos.ZERO, Direction.NORTH);
		Vec3 start = TradeBotBrain.dockStart(BlockPos.ZERO, Direction.NORTH);
		assertEquals(0, start.y, 1e-4, "on the ground");
		assertEquals(dock.z - 18 / 16.0, start.z, 1e-4, "18 px out in front (north)");
		assertEquals(dock.x, start.x, 1e-4);
	}
}
