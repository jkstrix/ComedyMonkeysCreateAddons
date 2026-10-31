package com.comedymonkey.createaddons.content.line;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.registry.ModBlocks;
import com.simibubi.create.content.contraptions.StructureTransform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;

/**
 * A Power Line turned by a contraption or a rotated schematic: its Splitter's priorities turn with its branches, and the
 * block it's encased in turns with it (on a block entity with no level).
 */
class SplitterTurnTest {
	private static PowerLineBlockEntity wire() {
		return new PowerLineBlockEntity(BlockPos.ZERO, ModBlocks.COPPER_POWER_LINE.get().defaultBlockState());
	}

	private static StructureTransform turn(Rotation rotation, Mirror mirror) {
		return new StructureTransform(BlockPos.ZERO, Direction.Axis.Y, rotation, mirror);
	}

	@Test
	void prioritiesTurnWithTheBranches() {
		PowerLineBlockEntity be = wire();
		// North first, east last, the rest second
		be.setPriorities(new byte[] {1, 1, 0, 1, 1, 2});
		be.transform(be, turn(Rotation.CLOCKWISE_90, Mirror.NONE));
		assertEquals(0, be.priority(Direction.EAST), "the first branch (north) should now run east");
		assertEquals(2, be.priority(Direction.SOUTH), "the last branch (east) should now run south");
		assertEquals(1, be.priority(Direction.NORTH));
		assertEquals(1, be.priority(Direction.WEST));
		assertEquals(1, be.priority(Direction.UP));
		assertEquals(1, be.priority(Direction.DOWN));
	}

	@Test
	void prioritiesMirror() {
		PowerLineBlockEntity be = wire();
		be.setPriorities(new byte[] {1, 1, 1, 1, 1, 0});
		// mirrored front to back, east and west swap
		be.transform(be, turn(Rotation.NONE, Mirror.FRONT_BACK));
		assertEquals(0, be.priority(Direction.WEST));
		assertEquals(1, be.priority(Direction.EAST));
	}

	@Test
	void encasingTurnsWithTheLine() {
		PowerLineBlockEntity be = wire();
		be.setEncasing(Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
		be.transform(be, turn(Rotation.CLOCKWISE_90, Mirror.NONE));
		assertEquals(Direction.Axis.Z, be.encasing().getValue(RotatedPillarBlock.AXIS));
	}
}
