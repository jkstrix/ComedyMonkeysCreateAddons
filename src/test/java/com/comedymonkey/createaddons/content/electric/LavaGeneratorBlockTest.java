package com.comedymonkey.createaddons.content.electric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.registry.ModBlocks;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/** Which faces of the Lava Generator take lava and give power, and how it turns. */
class LavaGeneratorBlockTest {
	private static BlockState facing(Direction facing) {
		return ModBlocks.LAVA_GENERATOR.get().defaultBlockState().setValue(LavaGeneratorBlock.FACING, facing);
	}

	@Test
	void startsFacingNorthUnlitAndEmpty() {
		BlockState state = ModBlocks.LAVA_GENERATOR.get().defaultBlockState();
		assertEquals(Direction.NORTH, state.getValue(LavaGeneratorBlock.FACING));
		assertFalse(state.getValue(LavaGeneratorBlock.LIT));
		assertFalse(state.getValue(LavaGeneratorBlock.FUELLED));
	}

	@Test
	void theLavaShowsWheneverThereIsSomeAndGlowsBrighterWhileItBurns() {
		BlockState empty = ModBlocks.LAVA_GENERATOR.get().defaultBlockState();
		BlockState full = empty.setValue(LavaGeneratorBlock.FUELLED, true);
		BlockState burning = full.setValue(LavaGeneratorBlock.LIT, true);
		assertEquals(0, empty.getLightEmission());
		assertTrue(full.getLightEmission() > 0);
		assertTrue(burning.getLightEmission() > full.getLightEmission());
	}

	@Test
	void lavaGoesInAtTheBack() {
		for (Direction facing : Direction.Plane.HORIZONTAL)
			assertEquals(facing.getOpposite(), LavaGeneratorBlock.inlet(facing(facing)));
	}

	@Test
	void powerComesOutOfBothSidesOnly() {
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			BlockState state = facing(facing);
			assertTrue(LavaGeneratorBlock.isTerminal(state, facing.getClockWise()));
			assertTrue(LavaGeneratorBlock.isTerminal(state, facing.getCounterClockWise()));
			assertFalse(LavaGeneratorBlock.isTerminal(state, facing));
			assertFalse(LavaGeneratorBlock.isTerminal(state, facing.getOpposite()));
			assertFalse(LavaGeneratorBlock.isTerminal(state, Direction.UP));
			assertFalse(LavaGeneratorBlock.isTerminal(state, Direction.DOWN));
		}
	}

	@Test
	void noSideIsNotATerminal() {
		assertFalse(LavaGeneratorBlock.isTerminal(facing(Direction.NORTH), null));
	}

	@Test
	void theInletIsNeverATerminal() {
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			BlockState state = facing(facing);
			assertFalse(LavaGeneratorBlock.isTerminal(state, LavaGeneratorBlock.inlet(state)));
		}
	}

	@Test
	void rotatesAndMirrorsItsFacing() {
		assertEquals(Direction.EAST, facing(Direction.NORTH).rotate(Rotation.CLOCKWISE_90).getValue(LavaGeneratorBlock.FACING));
		assertEquals(Direction.SOUTH, facing(Direction.NORTH).rotate(Rotation.CLOCKWISE_180).getValue(LavaGeneratorBlock.FACING));
		assertEquals(Direction.WEST, facing(Direction.NORTH).rotate(Rotation.COUNTERCLOCKWISE_90).getValue(LavaGeneratorBlock.FACING));
		assertEquals(Direction.SOUTH, facing(Direction.NORTH).mirror(Mirror.LEFT_RIGHT).getValue(LavaGeneratorBlock.FACING));
		assertEquals(Direction.NORTH, facing(Direction.NORTH).mirror(Mirror.FRONT_BACK).getValue(LavaGeneratorBlock.FACING));
		assertEquals(Direction.WEST, facing(Direction.EAST).mirror(Mirror.FRONT_BACK).getValue(LavaGeneratorBlock.FACING));
	}
}
