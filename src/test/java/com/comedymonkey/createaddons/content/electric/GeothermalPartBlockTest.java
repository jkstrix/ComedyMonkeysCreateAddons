package com.comedymonkey.createaddons.content.electric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

/** The turbine's casing and core blocks (and the controller's block states), and the casing's controller note. */
class GeothermalPartBlockTest {
	private static HolderLookup.Provider registries;

	@BeforeAll
	static void lookup() {
		registries = VanillaRegistries.createLookup();
	}

	private static BlockState casing() {
		return ModBlocks.GEOTHERMAL_CASING.get().defaultBlockState();
	}

	private static BlockState core() {
		return ModBlocks.GEOTHERMAL_CORE.get().defaultBlockState();
	}

	@Test
	void casingAndCoreAreTold() {
		assertTrue(ModBlocks.GEOTHERMAL_CASING.get().isCasing());
		assertFalse(ModBlocks.GEOTHERMAL_CORE.get().isCasing());
	}

	@Test
	void partsStartUnformed() {
		assertFalse(casing().getValue(GeothermalPartBlock.FORMED));
		assertFalse(core().getValue(GeothermalPartBlock.FORMED));
		BlockState controller = ModBlocks.GEOTHERMAL_CONTROLLER.get().defaultBlockState();
		assertFalse(controller.getValue(GeothermalControllerBlock.FORMED));
		assertFalse(controller.getValue(GeothermalControllerBlock.LIT));
		assertEquals(Direction.NORTH, controller.getValue(GeothermalControllerBlock.FACING));
	}

	@Test
	void controllerSharesTheFormedProperty() {
		// The controller's own FORMED is set by the same loop that sets the parts', so it must be the same property
		assertSame(GeothermalPartBlock.FORMED, GeothermalControllerBlock.FORMED);
	}

	@Test
	void isPartTellsCasingFromCore() {
		assertTrue(GeothermalPartBlock.isPart(casing(), true));
		assertFalse(GeothermalPartBlock.isPart(casing(), false));
		assertTrue(GeothermalPartBlock.isPart(core(), false));
		assertFalse(GeothermalPartBlock.isPart(core(), true));
	}

	@Test
	void isPartIgnoresTheFormedState() {
		assertTrue(GeothermalPartBlock.isPart(casing().setValue(GeothermalPartBlock.FORMED, true), true));
		assertTrue(GeothermalPartBlock.isPart(core().setValue(GeothermalPartBlock.FORMED, true), false));
	}

	@Test
	void otherBlocksAreNotParts() {
		for (boolean wantCasing : new boolean[] {true, false}) {
			assertFalse(GeothermalPartBlock.isPart(Blocks.STONE.defaultBlockState(), wantCasing));
			assertFalse(GeothermalPartBlock.isPart(Blocks.AIR.defaultBlockState(), wantCasing));
			assertFalse(GeothermalPartBlock.isPart(ModBlocks.GEOTHERMAL_CONTROLLER.get().defaultBlockState(), wantCasing));
		}
	}

	@Test
	void onlyCasingsHaveABlockEntity() {
		assertInstanceOf(GeothermalCasingBlockEntity.class, ModBlocks.GEOTHERMAL_CASING.get().newBlockEntity(BlockPos.ZERO, casing()));
		assertNull(ModBlocks.GEOTHERMAL_CORE.get().newBlockEntity(BlockPos.ZERO, core()));
	}

	@Test
	void casingRemembersItsController() {
		GeothermalCasingBlockEntity casing = new GeothermalCasingBlockEntity(BlockPos.ZERO, casing());
		assertNull(casing.getController());
		BlockPos controller = new BlockPos(-3, 70, 12);
		casing.setController(controller);
		CompoundTag tag = new CompoundTag();
		casing.saveAdditional(tag, registries);

		GeothermalCasingBlockEntity loaded = new GeothermalCasingBlockEntity(BlockPos.ZERO, casing());
		loaded.loadAdditional(tag, registries);
		assertEquals(controller, loaded.getController());
	}

	@Test
	void casingForgetsAClearedController() {
		GeothermalCasingBlockEntity casing = new GeothermalCasingBlockEntity(BlockPos.ZERO, casing());
		casing.setController(new BlockPos(1, 2, 3));
		casing.setController(null);
		CompoundTag tag = new CompoundTag();
		casing.saveAdditional(tag, registries);
		assertFalse(tag.contains("Controller"));

		GeothermalCasingBlockEntity loaded = new GeothermalCasingBlockEntity(BlockPos.ZERO, casing());
		loaded.setController(new BlockPos(9, 9, 9));
		loaded.loadAdditional(tag, registries);
		assertNull(loaded.getController());
	}

	@Test
	void casingWithoutALevelLeadsNowhere() {
		GeothermalCasingBlockEntity casing = new GeothermalCasingBlockEntity(BlockPos.ZERO, casing());
		casing.setController(new BlockPos(0, 0, 1));
		assertNull(casing.controller());
	}

	@Test
	void controllerRotatesAndMirrors() {
		BlockState north = ModBlocks.GEOTHERMAL_CONTROLLER.get().defaultBlockState();
		assertEquals(Direction.EAST, north.rotate(Rotation.CLOCKWISE_90).getValue(GeothermalControllerBlock.FACING));
		assertEquals(Direction.SOUTH, north.mirror(Mirror.LEFT_RIGHT).getValue(GeothermalControllerBlock.FACING));
		assertEquals(Direction.NORTH, north.mirror(Mirror.FRONT_BACK).getValue(GeothermalControllerBlock.FACING));
	}
}
