package com.comedymonkey.createaddons.compat.pipez.cover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.registry.ModAttachments;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;

import de.maxhenkel.pipez.blocks.tileentity.PipeTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The cover's boxes for each set of connected and extracting sides. Pipez's item pipe is looked up in the registry and
 * its sides set through block state property names.
 */
class PipeCoverShapesTest {
	private static final ResourceLocation ITEM_PIPE = ResourceLocation.fromNamespaceAndPath("pipez", "item_pipe");
	private static final BlockPos POS = new BlockPos(3, 64, -7);

	// ---- FULL

	@Test
	void fullCoverIsOneWholeBlockWithNoHiddenFaces() {
		List<PipeCoverShapes.Box> boxes = PipeCoverShapes.boxes(PipeCover.Kind.FULL, pipe(), EmptyBlockGetter.INSTANCE, POS);
		assertEquals(1, boxes.size());
		assertEquals(new AABB(0, 0, 0, 1, 1, 1), boxes.get(0).box());
		assertTrue(boxes.get(0).hidden().isEmpty());
	}

	// ---- SNUG, no block entity (so nothing extracts)

	@Test
	void unconnectedSnugCoverIsJustTheCore() {
		List<PipeCoverShapes.Box> boxes = PipeCoverShapes.boxes(PipeCover.Kind.SNUG, pipe(), EmptyBlockGetter.INSTANCE, POS);
		assertEquals(1, boxes.size());
		assertEquals(px(4, 4, 4, 12, 12, 12), boxes.get(0).box());
		assertTrue(boxes.get(0).hidden().isEmpty());
	}

	@Test
	void nonPipeStateIsJustTheCore() {
		List<PipeCoverShapes.Box> boxes = PipeCoverShapes.boxes(PipeCover.Kind.SNUG, Blocks.STONE.defaultBlockState(),
			EmptyBlockGetter.INSTANCE, POS);
		assertEquals(1, boxes.size());
		assertEquals(px(4, 4, 4, 12, 12, 12), boxes.get(0).box());
	}

	@Test
	void eachConnectedSideGetsAnArmOutToTheBlockEdge() {
		for (Direction d : Direction.values()) {
			List<PipeCoverShapes.Box> boxes = PipeCoverShapes.boxes(PipeCover.Kind.SNUG, pipe(d), EmptyBlockGetter.INSTANCE, POS);
			assertEquals(2, boxes.size(), d.toString());
			// The core hides its face where the arm joins
			assertEquals(Set.of(d), boxes.get(0).hidden(), d.toString());
			PipeCoverShapes.Box arm = boxes.get(1);
			assertEquals(arm(d, 0, 4, 4, 12), arm.box(), d.toString());
			// Only its inner end, in the core, is hidden: the outer end is drawn, so it's closed against a neighbour
			// whose face doesn't cover it (a port in the middle of a machine's side)
			assertEquals(EnumSet.of(d.getOpposite()), arm.hidden(), d.toString());
		}
	}

	@Test
	void armsAreExplicitForNorthAndUp() {
		List<PipeCoverShapes.Box> boxes = PipeCoverShapes.boxes(PipeCover.Kind.SNUG, pipe(Direction.NORTH, Direction.UP),
			EmptyBlockGetter.INSTANCE, POS);
		assertEquals(3, boxes.size());
		assertEquals(EnumSet.of(Direction.NORTH, Direction.UP), boxes.get(0).hidden());
		// EnumSet order: UP before NORTH
		assertEquals(px(4, 12, 4, 12, 16, 12), boxes.get(1).box());
		assertEquals(px(4, 4, 0, 12, 12, 4), boxes.get(2).box());
	}

	@Test
	void fullyConnectedSnugCoverStaysInsideTheBlockAndTouchesEverySide() {
		List<PipeCoverShapes.Box> boxes = PipeCoverShapes.boxes(PipeCover.Kind.SNUG, pipe(Direction.values()),
			EmptyBlockGetter.INSTANCE, POS);
		assertEquals(7, boxes.size());
		AABB unit = new AABB(0, 0, 0, 1, 1, 1);
		for (PipeCoverShapes.Box box : boxes) {
			assertTrue(contains(unit, box.box()), box.toString());
			assertTrue(box.box().getXsize() > 0 && box.box().getYsize() > 0 && box.box().getZsize() > 0, box.toString());
		}
		assertEquals(EnumSet.allOf(Direction.class), boxes.get(0).hidden());
	}

	// ---- SNUG with Pipez's block entity: extracting sides get a collar

	@Test
	void extractingSideGetsACollarAndAShorterArm() {
		BlockState state = pipe(Direction.EAST, Direction.DOWN);
		PipeTileEntity pipe = pipeEntity(state);
		setExtracting(pipe, Direction.EAST, true);
		BlockGetter level = new OneBlock(POS, state, pipe);

		List<PipeCoverShapes.Box> boxes = PipeCoverShapes.boxes(PipeCover.Kind.SNUG, state, level, POS);
		assertEquals(4, boxes.size());
		assertEquals(EnumSet.of(Direction.EAST, Direction.DOWN), boxes.get(0).hidden());
		// DOWN (first in enum order) is a plain arm
		assertEquals(px(4, 0, 4, 12, 4, 12), boxes.get(1).box());
		// EAST: a 10 px collar 2 px deep against the neighbour, then the arm on to the core
		assertEquals(px(14, 3, 3, 16, 13, 13), boxes.get(2).box());
		assertTrue(boxes.get(2).hidden().isEmpty());
		assertEquals(px(12, 4, 4, 14, 12, 12), boxes.get(3).box());
		assertEquals(EnumSet.of(Direction.EAST, Direction.WEST), boxes.get(3).hidden());
	}

	@Test
	void collarOnANegativeSideStartsAtZero() {
		BlockState state = pipe(Direction.NORTH);
		PipeTileEntity pipe = pipeEntity(state);
		setExtracting(pipe, Direction.NORTH, true);

		List<PipeCoverShapes.Box> boxes = PipeCoverShapes.boxes(PipeCover.Kind.SNUG, state, new OneBlock(POS, state, pipe), POS);
		assertEquals(3, boxes.size());
		assertEquals(px(3, 3, 0, 13, 13, 2), boxes.get(1).box());
		assertEquals(px(4, 4, 2, 12, 12, 4), boxes.get(2).box());
	}

	@Test
	void extractingAnUnconnectedSideAddsNothing() {
		BlockState state = pipe();
		PipeTileEntity pipe = pipeEntity(state);
		setExtracting(pipe, Direction.SOUTH, true);
		assertEquals(1, PipeCoverShapes.boxes(PipeCover.Kind.SNUG, state, new OneBlock(POS, state, pipe), POS).size());
	}

	// ---- cover() and shapeOrNull()

	@Test
	void noBlockEntityMeansNoCover() {
		assertNull(PipeCoverShapes.cover(EmptyBlockGetter.INSTANCE, POS));
		assertNull(PipeCoverShapes.shapeOrNull(pipe(), EmptyBlockGetter.INSTANCE, POS));
	}

	@Test
	void pipeWithoutCoverDataMeansNoCover() {
		BlockState state = pipe();
		OneBlock level = new OneBlock(POS, state, pipeEntity(state));
		assertNull(PipeCoverShapes.cover(level, POS));
		assertNull(PipeCoverShapes.shapeOrNull(state, level, POS));
	}

	@Test
	void coverDataOnSomethingOtherThanAPipeIsIgnored() {
		BlockState furnace = Blocks.FURNACE.defaultBlockState();
		BlockEntity be = new FurnaceBlockEntity(POS, furnace);
		be.setData(ModAttachments.PIPE_COVER, new PipeCover(PipeCover.Kind.FULL, Blocks.STONE.defaultBlockState()));
		assertNull(PipeCoverShapes.cover(new OneBlock(POS, furnace, be), POS));
	}

	@Test
	void coverIsReadFromThePipe() {
		BlockState state = pipe();
		PipeTileEntity pipe = pipeEntity(state);
		PipeCover cover = new PipeCover(PipeCover.Kind.SNUG, Blocks.BRICKS.defaultBlockState());
		pipe.setData(ModAttachments.PIPE_COVER, cover);
		assertEquals(cover, PipeCoverShapes.cover(new OneBlock(POS, state, pipe), POS));
	}

	@Test
	void fullCoverShapeIsTheWholeBlock() {
		BlockState state = pipe(Direction.NORTH, Direction.SOUTH);
		OneBlock level = covered(state, PipeCover.Kind.FULL);
		assertSameShape(Shapes.block(), PipeCoverShapes.shapeOrNull(state, level, POS));
	}

	@Test
	void snugCoverShapeIsTheUnionOfItsBoxes() {
		BlockState state = pipe(Direction.NORTH, Direction.SOUTH);
		OneBlock level = covered(state, PipeCover.Kind.SNUG);
		// A straight run: one 8 px bar right through the block
		assertSameShape(Shapes.create(px(4, 4, 0, 12, 12, 16)), PipeCoverShapes.shapeOrNull(state, level, POS));
	}

	@Test
	void snugCoverShapeWithoutConnectionsIsTheCore() {
		BlockState state = pipe();
		assertSameShape(Shapes.create(px(4, 4, 4, 12, 12, 12)),
			PipeCoverShapes.shapeOrNull(state, covered(state, PipeCover.Kind.SNUG), POS));
	}

	// ---- Through Pipez's own block, when the mixins are applied

	@Test
	void coveredPipeTakesItsCoversShape() {
		assumeMixinsApplied();
		BlockState state = pipe(Direction.EAST);
		OneBlock level = covered(state, PipeCover.Kind.FULL);
		assertSameShape(Shapes.block(), state.getShape(level, POS));
		assertSameShape(Shapes.block(), state.getCollisionShape(level, POS, net.minecraft.world.phys.shapes.CollisionContext.empty()));
		assertSameShape(Shapes.block(), state.getBlockSupportShape(level, POS));
	}

	@Test
	void uncoveredPipeKeepsPipezsShape() {
		assumeMixinsApplied();
		BlockState state = pipe(Direction.EAST);
		VoxelShape shape = state.getShape(EmptyBlockGetter.INSTANCE, POS);
		assertNotNull(shape);
		assertFalse(Shapes.joinIsNotEmpty(shape, Shapes.create(px(0, 0, 0, 2, 2, 2)), BooleanOp.AND),
			"a bare pipe doesn't fill the block's corners");
	}

	@Test
	void coverGivesThePipeAReasonToKeepItsBlockEntity() {
		assumeMixinsApplied();
		BlockState state = pipe();
		PipeTileEntity pipe = pipeEntity(state);
		assertFalse(pipe.hasReasonToStay());
		pipe.setData(ModAttachments.PIPE_COVER, new PipeCover(PipeCover.Kind.SNUG, Blocks.AIR.defaultBlockState()));
		assertTrue(pipe.hasReasonToStay());
	}

	@Test
	void fullCoverIsSturdyForTorches() {
		assumeMixinsApplied();
		BlockState state = pipe(Direction.EAST);
		OneBlock level = covered(state, PipeCover.Kind.FULL);
		assertTrue(state.isFaceSturdy(level, POS, Direction.UP));
	}

	// ---- helpers

	/** An item pipe connected on the given sides, without block entity data. */
	private static BlockState pipe(Direction... connected) {
		Block block = BuiltInRegistries.BLOCK.get(ITEM_PIPE);
		assumeTrue(block != Blocks.AIR, "Pipez isn't loaded");
		BlockState state = block.defaultBlockState();
		Set<Direction> sides = connected.length == 0 ? EnumSet.noneOf(Direction.class) : EnumSet.of(connected[0], connected);
		for (Direction d : Direction.values())
			state = state.setValue(bool(state, d.getSerializedName()), sides.contains(d));
		return state;
	}

	/** Pipez's block entity for this pipe; the state is switched to one that has data. */
	private static PipeTileEntity pipeEntity(BlockState state) {
		BlockState withData = state.setValue(bool(state, "has_data"), true);
		BlockEntity be = ((EntityBlock) withData.getBlock()).newBlockEntity(POS, withData);
		assertNotNull(be, "Pipez made no block entity");
		return (PipeTileEntity) be;
	}

	private static OneBlock covered(BlockState state, PipeCover.Kind kind) {
		PipeTileEntity pipe = pipeEntity(state);
		pipe.setData(ModAttachments.PIPE_COVER, new PipeCover(kind, Blocks.STONE.defaultBlockState()));
		return new OneBlock(POS, state, pipe);
	}

	private static void setExtracting(PipeTileEntity pipe, Direction side, boolean extracting) {
		pipe.setExtracting(side, extracting);
		assertTrue(pipe.isExtracting(side) == extracting);
	}

	private static BooleanProperty bool(BlockState state, String name) {
		Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
		assertNotNull(property, "no property " + name);
		return (BooleanProperty) property;
	}

	/** The mixins add Create's goggle interface to Pipez's block entity; use that to tell whether they ran. */
	private static void assumeMixinsApplied() {
		assumeTrue(pipeEntity(pipe()) instanceof IHaveGoggleInformation, "mixins aren't applied in this environment");
	}

	private static AABB px(double x0, double y0, double z0, double x1, double y1, double z1) {
		return new AABB(x0 / 16, y0 / 16, z0 / 16, x1 / 16, y1 / 16, z1 / 16);
	}

	/** The expected arm box from side {@code d}, worked out independently of {@link PipeCoverShapes}. */
	private static AABB arm(Direction d, double near, double far, double lo, double hi) {
		double a, b;
		if (d.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
			a = 16 - far;
			b = 16 - near;
		} else {
			a = near;
			b = far;
		}
		return switch (d.getAxis()) {
			case X -> px(a, lo, lo, b, hi, hi);
			case Y -> px(lo, a, lo, hi, b, hi);
			case Z -> px(lo, lo, a, hi, hi, b);
		};
	}

	private static boolean contains(AABB outer, AABB inner) {
		return inner.minX >= outer.minX && inner.minY >= outer.minY && inner.minZ >= outer.minZ
			&& inner.maxX <= outer.maxX && inner.maxY <= outer.maxY && inner.maxZ <= outer.maxZ;
	}

	private static void assertSameShape(VoxelShape expected, @Nullable VoxelShape actual) {
		assertNotNull(actual);
		assertFalse(Shapes.joinIsNotEmpty(expected, actual, BooleanOp.NOT_SAME),
			() -> "expected " + expected.toAabbs() + " but was " + actual.toAabbs());
	}

	/** A world of one block (and its block entity) in air. */
	private record OneBlock(BlockPos pos, BlockState state, @Nullable BlockEntity be) implements BlockGetter {
		@Nullable
		@Override
		public BlockEntity getBlockEntity(BlockPos at) {
			return at.equals(pos) ? be : null;
		}

		@Override
		public BlockState getBlockState(BlockPos at) {
			return at.equals(pos) ? state : Blocks.AIR.defaultBlockState();
		}

		@Override
		public FluidState getFluidState(BlockPos at) {
			return getBlockState(at).getFluidState();
		}

		@Override
		public int getHeight() {
			return 384;
		}

		@Override
		public int getMinBuildHeight() {
			return -64;
		}
	}
}
