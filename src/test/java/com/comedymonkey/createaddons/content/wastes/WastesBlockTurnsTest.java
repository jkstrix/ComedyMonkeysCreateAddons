package com.comedymonkey.createaddons.content.wastes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.content.wastes.StainedBrickDiagonalBlock.Corner;
import com.comedymonkey.createaddons.content.wastes.overgrowth.BreakrootBlock;
import com.comedymonkey.createaddons.content.wastes.overgrowth.BreakrootBurstBlock;
import com.comedymonkey.createaddons.content.wastes.overgrowth.MossCarpetBlock;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Wastes' oriented blocks turn and mirror as a contraption, a rotated schematic or a structure block moves them: the
 * diagonals' outlines land where the turned or mirrored outline would (so the arch springers swap on a mirror), the
 * breakroots, the burst and the moss carpet's drapes follow, a prop piece keeps its facing on a mirror. The diagonals stop
 * light at their full back faces and let it through their open half.
 */
@SuppressWarnings("deprecation") // exercises the blocks' own rotate and mirror
class WastesBlockTurnsTest {
	private static List<BlockState> diagonals() {
		var out = new java.util.ArrayList<BlockState>();
		out.addAll(ModWastes.STAINED_END_STONE_BRICK_DIAGONAL.get().getStateDefinition().getPossibleStates());
		out.addAll(ModWastes.CUT_DEEPSLATE_BRICK_DIAGONAL.get().getStateDefinition().getPossibleStates());
		return out;
	}

	private static VoxelShape shape(BlockState s) {
		return s.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
	}

	/** Each box of {@code s} moved by {@code f}. */
	private static VoxelShape map(VoxelShape s, java.util.function.UnaryOperator<AABB> f) {
		VoxelShape out = Shapes.empty();
		for (AABB b : s.toAabbs())
			out = Shapes.or(out, Shapes.create(f.apply(b)));
		return out;
	}

	/** A quarter turn clockwise from above: east goes south. */
	private static VoxelShape turned(VoxelShape s, Rotation r) {
		for (int i = 0; i < r.ordinal(); i++)
			s = map(s, b -> new AABB(1 - b.maxZ, b.minY, b.minX, 1 - b.minZ, b.maxY, b.maxX));
		return s;
	}

	private static VoxelShape mirrored(VoxelShape s, Mirror m) {
		return switch (m) {
			case LEFT_RIGHT -> map(s, b -> new AABB(b.minX, b.minY, 1 - b.maxZ, b.maxX, b.maxY, 1 - b.minZ));
			case FRONT_BACK -> map(s, b -> new AABB(1 - b.maxX, b.minY, b.minZ, 1 - b.minX, b.maxY, b.maxZ));
			default -> s;
		};
	}

	private static void assertSameShape(VoxelShape want, VoxelShape got, String what) {
		assertFalse(Shapes.joinIsNotEmpty(want, got, BooleanOp.NOT_SAME), what);
	}

	@Test
	void diagonalsTurnByTheirCornersQuarters() {
		assertEquals(Corner.SOUTH_WEST, Corner.SOUTH_EAST.turned(Rotation.CLOCKWISE_90));
		assertEquals(Corner.NORTH_EAST, Corner.SOUTH_EAST.turned(Rotation.COUNTERCLOCKWISE_90));
		for (BlockState s : diagonals())
			for (Rotation r : Rotation.values()) {
				BlockState t = s.rotate(r);
				assertEquals((s.getValue(StainedBrickDiagonalBlock.CORNER).quarters() + r.ordinal()) % 4, t.getValue(StainedBrickDiagonalBlock.CORNER).quarters());
				assertSameShape(turned(shape(s), r), shape(t), s + " turned " + r + " isn't its outline turned");
			}
	}

	@Test
	void diagonalsMirrorTheirCornerAndSwapTheArchSpringers() {
		BlockState arch = ModWastes.STAINED_END_STONE_BRICK_DIAGONAL.get().defaultBlockState()
			.setValue(StainedBrickDiagonalBlock.FORM, StainedBrickDiagonalBlock.Form.ARCH_NEG);
		BlockState m = arch.mirror(Mirror.LEFT_RIGHT);
		assertEquals(Corner.NORTH_EAST, m.getValue(StainedBrickDiagonalBlock.CORNER));
		assertEquals(StainedBrickDiagonalBlock.Form.ARCH_POS, m.getValue(StainedBrickDiagonalBlock.FORM));
		for (BlockState s : diagonals())
			for (Mirror r : Mirror.values()) {
				BlockState t = s.mirror(r);
				assertSameShape(mirrored(shape(s), r), shape(t), s + " mirrored " + r + " isn't its outline mirrored");
				assertSame(s, t.mirror(r), s + " mirrored twice isn't itself");
			}
	}

	@Test
	void diagonalsStopLightAtTheirBackAndLetItThroughTheirOpenHalf() {
		for (BlockState s : diagonals()) {
			assertTrue(s.canOcclude() && s.useShapeForLightOcclusion(), s + " doesn't stop light by its shape");
			Corner c = s.getValue(StainedBrickDiagonalBlock.CORNER);
			Direction backX = c.sx > 0 ? Direction.WEST : Direction.EAST, backZ = c.sz > 0 ? Direction.NORTH : Direction.SOUTH;
			for (Direction back : List.of(backX, backZ)) {
				VoxelShape face = LightEngine.getOcclusionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, s, back);
				// an arch springer's lower half is open on one side, through one of its backs; the rest is a full back
				boolean arch = s.hasProperty(StainedBrickDiagonalBlock.FORM) && s.getValue(StainedBrickDiagonalBlock.FORM) != StainedBrickDiagonalBlock.Form.FULL;
				VoxelShape solid = Shapes.getFaceShape(arch ? Block.box(0, 8, 0, 16, 16, 16) : Shapes.block(), back);
				assertFalse(Shapes.joinIsNotEmpty(solid, face, BooleanOp.ONLY_FIRST), s + " lets light through its back (" + back + ")");
				VoxelShape front = LightEngine.getOcclusionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, s, back.getOpposite());
				assertFalse(Shapes.faceShapeOccludes(front, Shapes.empty()), s + " stops light at its open front (" + back.getOpposite() + ")");
			}
			for (Direction v : List.of(Direction.UP, Direction.DOWN))
				assertFalse(Shapes.faceShapeOccludes(LightEngine.getOcclusionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, s, v), Shapes.empty()),
					s + " stops light through its open half (" + v + ")");
		}
	}

	@Test
	void breakrootsTurnTheirAxisAndKeepItInAMirror() {
		for (var b : List.of(ModWastes.BREAKROOT_KNUCKLE, ModWastes.BREAKROOT_RUN)) {
			BlockState x = b.get().defaultBlockState().setValue(BreakrootBlock.AXIS, Direction.Axis.X);
			assertEquals(Direction.Axis.Z, x.rotate(Rotation.CLOCKWISE_90).getValue(BreakrootBlock.AXIS));
			assertEquals(Direction.Axis.Z, x.rotate(Rotation.COUNTERCLOCKWISE_90).getValue(BreakrootBlock.AXIS));
			assertEquals(Direction.Axis.X, x.rotate(Rotation.CLOCKWISE_180).getValue(BreakrootBlock.AXIS));
			for (BlockState s : b.get().getStateDefinition().getPossibleStates()) {
				for (Mirror m : Mirror.values())
					assertSame(s, s.mirror(m));
				for (Rotation r : Rotation.values())
					assertSameShape(turned(shape(s), r), shape(s.rotate(r)), s + " turned " + r + " isn't its outline turned");
			}
		}
	}

	@Test
	void burstTurnsAndMirrorsWithItsWallAndStaysUp() {
		BlockState up = ModWastes.BREAKROOT_BURST.get().defaultBlockState().setValue(BreakrootBurstBlock.FACING, Direction.UP);
		for (Rotation r : Rotation.values())
			assertSame(up, up.rotate(r));
		for (Mirror m : Mirror.values())
			assertSame(up, up.mirror(m));
		for (Direction d : Direction.Plane.HORIZONTAL) {
			BlockState s = up.setValue(BreakrootBurstBlock.FACING, d);
			assertEquals(d.getClockWise(), s.rotate(Rotation.CLOCKWISE_90).getValue(BreakrootBurstBlock.FACING));
			assertEquals(d.getAxis() == Direction.Axis.Z ? d.getOpposite() : d, s.mirror(Mirror.LEFT_RIGHT).getValue(BreakrootBurstBlock.FACING));
			assertEquals(d.getAxis() == Direction.Axis.X ? d.getOpposite() : d, s.mirror(Mirror.FRONT_BACK).getValue(BreakrootBurstBlock.FACING));
			for (Rotation r : Rotation.values())
				assertSameShape(turned(shape(s), r), shape(s.rotate(r)), s + " turned " + r + " isn't its outline turned");
		}
	}

	@Test
	void mossCarpetsDrapesTurnAndMirror() {
		BlockState north = ModWastes.DUSK_MOSS_CARPET.get().defaultBlockState().setValue(MossCarpetBlock.DRAPES.get(Direction.NORTH), true);
		for (Rotation r : Rotation.values())
			for (Direction d : Direction.Plane.HORIZONTAL)
				assertEquals(r.rotate(Direction.NORTH) == d, north.rotate(r).getValue(MossCarpetBlock.DRAPES.get(d)), "turned " + r + ", drape " + d);
		BlockState ne = north.setValue(MossCarpetBlock.DRAPES.get(Direction.EAST), true);
		BlockState lr = ne.mirror(Mirror.LEFT_RIGHT), fb = ne.mirror(Mirror.FRONT_BACK);
		assertTrue(lr.getValue(MossCarpetBlock.DRAPES.get(Direction.SOUTH)) && lr.getValue(MossCarpetBlock.DRAPES.get(Direction.EAST))
			&& !lr.getValue(MossCarpetBlock.DRAPES.get(Direction.NORTH)) && !lr.getValue(MossCarpetBlock.DRAPES.get(Direction.WEST)));
		assertTrue(fb.getValue(MossCarpetBlock.DRAPES.get(Direction.NORTH)) && fb.getValue(MossCarpetBlock.DRAPES.get(Direction.WEST))
			&& !fb.getValue(MossCarpetBlock.DRAPES.get(Direction.SOUTH)) && !fb.getValue(MossCarpetBlock.DRAPES.get(Direction.EAST)));
	}

	@Test
	void propPiecesTurnButKeepTheirFacingInAMirror() {
		int seen = 0;
		for (var e : ModBlocks.BLOCKS.getEntries()) {
			Block b = e.get();
			if (!(b instanceof PropPieceBlock))
				continue;
			seen++;
			for (Direction d : Direction.Plane.HORIZONTAL) {
				BlockState s = b.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, d);
				for (Mirror m : Mirror.values())
					assertSame(s, s.mirror(m), e.getId() + " facing " + d + " spun in a " + m + " mirror");
				assertEquals(d.getClockWise(), s.rotate(Rotation.CLOCKWISE_90).getValue(HorizontalDirectionalBlock.FACING));
			}
		}
		assertTrue(seen > 0, "no prop pieces registered");
	}
}
