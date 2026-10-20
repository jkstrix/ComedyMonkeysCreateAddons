package com.comedymonkey.createaddons.content.pen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.registry.ModBlockEntities;
import com.comedymonkey.createaddons.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

class AnimalPenBlockTest {
	@Test
	void tiers() {
		assertFalse(ModBlocks.ANIMAL_PEN.get().isBrass());
		assertTrue(ModBlocks.BRASS_ANIMAL_PEN.get().isBrass());
	}

	@Test
	void facesNorthByDefault() {
		assertEquals(Direction.NORTH, ModBlocks.ANIMAL_PEN.get().defaultBlockState().getValue(AnimalPenBlock.FACING));
	}

	@Test
	void shaftGoesOnlyIntoTheBack() {
		AnimalPenBlock block = ModBlocks.ANIMAL_PEN.get();
		for (Direction front : Direction.Plane.HORIZONTAL) {
			BlockState state = block.defaultBlockState().setValue(AnimalPenBlock.FACING, front);
			assertEquals(front.getAxis(), block.getRotationAxis(state));
			for (Direction face : Direction.values())
				assertEquals(face == front.getOpposite(), block.hasShaftTowards(null, BlockPos.ZERO, state, face),
					front + " pen, " + face + " face");
		}
	}

	@Test
	void rotatesAndMirrorsItsFront() {
		BlockState north = ModBlocks.ANIMAL_PEN.get().defaultBlockState();
		assertEquals(Direction.EAST, north.rotate(Rotation.CLOCKWISE_90).getValue(AnimalPenBlock.FACING));
		assertEquals(Direction.SOUTH, north.rotate(Rotation.CLOCKWISE_180).getValue(AnimalPenBlock.FACING));
		assertEquals(Direction.SOUTH, north.mirror(Mirror.LEFT_RIGHT).getValue(AnimalPenBlock.FACING));
		assertEquals(Direction.NORTH, north.mirror(Mirror.FRONT_BACK).getValue(AnimalPenBlock.FACING));
	}

	@Test
	void givesAComparatorSignal() {
		assertTrue(ModBlocks.ANIMAL_PEN.get().defaultBlockState().hasAnalogOutputSignal());
		assertTrue(ModBlocks.BRASS_ANIMAL_PEN.get().defaultBlockState().hasAnalogOutputSignal());
	}

	@Test
	void bothTiersShareTheBlockEntity() {
		assertSame(ModBlockEntities.ANIMAL_PEN.get(), ModBlocks.ANIMAL_PEN.get().getBlockEntityType());
		assertSame(ModBlockEntities.ANIMAL_PEN.get(), ModBlocks.BRASS_ANIMAL_PEN.get().getBlockEntityType());
		assertSame(AnimalPenBlockEntity.class, ModBlocks.ANIMAL_PEN.get().getBlockEntityClass());
		assertTrue(ModBlockEntities.ANIMAL_PEN.get().isValid(ModBlocks.ANIMAL_PEN.get().defaultBlockState()));
		assertTrue(ModBlockEntities.ANIMAL_PEN.get().isValid(ModBlocks.BRASS_ANIMAL_PEN.get().defaultBlockState()));
	}
}
