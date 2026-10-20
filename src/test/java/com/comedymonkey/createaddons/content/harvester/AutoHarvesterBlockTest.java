package com.comedymonkey.createaddons.content.harvester;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/** The Auto Harvester block: empty planter by default, a shaft only in the back, and its facing under rotation. */
class AutoHarvesterBlockTest {
	private static AutoHarvesterBlock block() {
		return ModBlocks.AUTO_HARVESTER.get();
	}

	private static BlockState facing(Direction facing) {
		return block().defaultBlockState().setValue(AutoHarvesterBlock.FACING, facing);
	}

	@Test
	void startsEmptyFacingNorth() {
		BlockState state = block().defaultBlockState();
		assertEquals(PlantKind.NONE, state.getValue(AutoHarvesterBlock.PLANT));
		assertEquals(Direction.NORTH, state.getValue(AutoHarvesterBlock.FACING));
	}

	@Test
	void takesAShaftOnlyAtTheBack() {
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			BlockState state = facing(facing);
			for (Direction face : Direction.values())
				assertEquals(face == facing.getOpposite(), block().hasShaftTowards(null, BlockPos.ZERO, state, face),
					"facing " + facing + ", face " + face);
			assertEquals(facing.getAxis(), block().getRotationAxis(state));
		}
	}

	@Test
	void rotatesAndMirrorsKeepingThePlant() {
		BlockState state = facing(Direction.NORTH).setValue(AutoHarvesterBlock.PLANT, PlantKind.TREE);
		BlockState rotated = state.rotate(Rotation.CLOCKWISE_90);
		assertEquals(Direction.EAST, rotated.getValue(AutoHarvesterBlock.FACING));
		assertEquals(PlantKind.TREE, rotated.getValue(AutoHarvesterBlock.PLANT));
		assertEquals(Direction.SOUTH, state.mirror(Mirror.LEFT_RIGHT).getValue(AutoHarvesterBlock.FACING));
		assertEquals(Direction.WEST, facing(Direction.EAST).mirror(Mirror.FRONT_BACK).getValue(AutoHarvesterBlock.FACING));
	}
}
