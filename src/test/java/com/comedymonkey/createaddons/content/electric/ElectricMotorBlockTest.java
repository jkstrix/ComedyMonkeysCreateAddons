package com.comedymonkey.createaddons.content.electric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** The Electric Motor's shaft: one, out of its facing, turning about the facing's axis; power in through the base opposite. */
class ElectricMotorBlockTest {
	private static BlockState facing(Direction facing) {
		return ModBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(ElectricMotorBlock.FACING, facing);
	}

	@Test
	void startsUnpowered() {
		assertFalse(ModBlocks.ELECTRIC_MOTOR.get().defaultBlockState().getValue(ElectricMotorBlock.POWERED));
	}

	@Test
	void hasAShaftOnlyOutOfItsFacing() {
		ElectricMotorBlock block = ModBlocks.ELECTRIC_MOTOR.get();
		for (Direction facing : Direction.values()) {
			BlockState state = facing(facing);
			for (Direction face : Direction.values())
				assertEquals(face == facing, block.hasShaftTowards(null, BlockPos.ZERO, state, face),
					"facing " + facing + ", face " + face);
		}
	}

	@Test
	void takesPowerOnlyThroughItsBase() {
		for (Direction facing : Direction.values()) {
			BlockState state = facing(facing);
			for (Direction side : Direction.values())
				assertEquals(side == facing.getOpposite(), ElectricMotorBlock.isInput(state, side),
					"facing " + facing + ", side " + side);
		}
	}

	@Test
	void turnsAboutItsFacingsAxis() {
		ElectricMotorBlock block = ModBlocks.ELECTRIC_MOTOR.get();
		for (Direction facing : Direction.values())
			assertEquals(facing.getAxis(), block.getRotationAxis(facing(facing)));
	}
}
