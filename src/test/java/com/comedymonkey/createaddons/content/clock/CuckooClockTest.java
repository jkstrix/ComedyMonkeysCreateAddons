package com.comedymonkey.createaddons.content.clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.testutil.TestConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Cuckoo Clock block and its block entity: shaft, shapes, goggle readout, fixed stress, what it shows, and the
 * check it saves on villagers. Reach and the restock rule are in {@link CuckooClocksTest}.
 */
class CuckooClockTest {
	@BeforeEach
	void setUp() {
		TestConfig.loadDefaults();
	}

	private static BlockState facing(Direction facing) {
		return ModBlocks.CUCKOO_CLOCK.get().defaultBlockState().setValue(CuckooClockBlock.FACING, facing);
	}

	private static CuckooClockBlockEntity clock() {
		return new CuckooClockBlockEntity(BlockPos.ZERO, facing(Direction.NORTH));
	}

	@Test
	void takesAShaftOnlyInTheBack() {
		CuckooClockBlock block = ModBlocks.CUCKOO_CLOCK.get();
		for (Direction facing : Direction.Plane.HORIZONTAL) {
			for (Direction face : Direction.values())
				assertEquals(face == facing.getOpposite(), block.hasShaftTowards(null, BlockPos.ZERO, facing(facing), face),
					"facing " + facing + ", face " + face);
			assertEquals(facing.getAxis(), block.getRotationAxis(facing(facing)));
		}
		assertEquals(Direction.EAST, facing(Direction.NORTH).rotate(Rotation.CLOCKWISE_90).getValue(CuckooClockBlock.FACING));
	}

	@Test
	void shapesTurnWithTheModel() {
		// a box against the back wall, at the west side when facing north
		Map<Direction, VoxelShape> shapes = CuckooClockBlock.shapes(new double[][] {{0, 0, 12, 4, 16, 16}});
		assertBounds(shapes.get(Direction.NORTH), 0, 12, 4, 16);
		assertBounds(shapes.get(Direction.SOUTH), 12, 0, 16, 4);
		// y 90 takes north to east: the back wall is then west, and the model's west side north
		assertBounds(shapes.get(Direction.EAST), 0, 0, 4, 4);
		assertBounds(shapes.get(Direction.WEST), 12, 12, 16, 16);
	}

	private static void assertBounds(VoxelShape shape, double x1, double z1, double x2, double z2) {
		AABB b = shape.bounds();
		assertEquals(x1 / 16, b.minX, 1e-6);
		assertEquals(z1 / 16, b.minZ, 1e-6);
		assertEquals(x2 / 16, b.maxX, 1e-6);
		assertEquals(z2 / 16, b.maxZ, 1e-6);
	}

	@Test
	void countsVillagersOnTheFirstLook() throws Exception {
		var field = CuckooClockBlockEntity.class.getDeclaredField("countedAt");
		field.setAccessible(true);
		long countedAt = field.getLong(clock());
		assertTrue(0 - countedAt >= 20, "a world at tick 0 still counts at once (no overflow)");
	}

	@Test
	void handsStaySmoothInAVeryOldWorld() throws Exception {
		// a clock stopped at day 41,666 + 16123 ticks (dayTime about a billion): only the time of day reaches the float
		CuckooClockBlockEntity be = clock();
		var shown = CuckooClockBlockEntity.class.getDeclaredField("shownTime");
		shown.setAccessible(true);
		shown.setLong(be, 1_000_000_123L);
		assertEquals(16123f, be.getShownTime(0), 0f);
		assertEquals(0.5f, be.getSwingTicks(0.5f), 0f, "no level yet: tick 0");
	}

	@Test
	void ponderRunsItsOwnTime() {
		CuckooClockBlockEntity be = clock();
		be.setPonderTime(1000, 10);
		assertEquals(1005f, be.getShownTime(0.5f), 1e-3f);
		be.startPonderShow(12, Blocks.LECTERN.defaultBlockState());
		assertEquals(12, be.getShowCalls());
		assertEquals(Blocks.LECTERN.defaultBlockState(), be.getWorkstation());
	}

	@Test
	void clockChecksSaveAndLoad() {
		for (ClockCheck check : new ClockCheck[] {new ClockCheck(123_456_789L, -42L), ClockCheck.NEVER}) {
			var saved = ClockCheck.CODEC.encodeStart(NbtOps.INSTANCE, check).getOrThrow();
			assertEquals(check, ClockCheck.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow());
		}
	}

	@Test
	void gogglesText() {
		assertEquals("8:00", CuckooClockBlockEntity.clockText(8, 0));
		assertEquals("12:05", CuckooClockBlockEntity.clockText(12, 5));
		assertEquals("0:01", CuckooClockBlockEntity.durationText(20));
		assertEquals("3:20", CuckooClockBlockEntity.durationText(4000));
		assertEquals("0:01", CuckooClockBlockEntity.durationText(1), "never shows 0:00 before the hour");
	}

	@Test
	void stressIsTheSameAtAnySpeed() {
		CuckooClockBlockEntity be = clock();
		for (float speed : new float[] {1, 4, 32, 256}) {
			be.setSpeed(speed);
			assertEquals(1024f, be.calculateStressApplied() * speed, 1e-3f, "at " + speed + " RPM");
		}
		be.setSpeed(0);
		assertEquals(0, be.calculateStressApplied(), 1e-6f);
	}
}
