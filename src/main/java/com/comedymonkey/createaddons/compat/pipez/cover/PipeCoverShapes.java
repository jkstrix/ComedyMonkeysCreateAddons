package com.comedymonkey.createaddons.compat.pipez.cover;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModAttachments;

import de.maxhenkel.pipez.blocks.PipeBlock;
import de.maxhenkel.pipez.blocks.tileentity.PipeTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The boxes a cover is made of. A Pipez pipe is a 6 px core with 6 px arms out to each connected side and an 8 px
 * collar, 1 px deep, on each side it extracts from. The snug cover sits a pixel proud of all of that (the collar's
 * cover is 10 px and 2 px deep); the full cover is the whole block.
 */
public final class PipeCoverShapes {
	/** Snug covers' shapes, made once each: by connected sides (bits 0-5) and extracting ones (bits 6-11). */
	private static final AtomicReferenceArray<VoxelShape> SNUG_SHAPES = new AtomicReferenceArray<>(1 << 12);
	/** Bare pipes' block-state-only shapes (collision, support, occlusion...), which Pipez builds afresh every time. */
	private static final Map<BlockState, VoxelShape> PLAIN_SHAPES = new ConcurrentHashMap<>();

	private PipeCoverShapes() {}

	/**
	 * A box, and the sides of it that are never drawn because they're buried against another of its boxes. The ends
	 * against a neighbour are drawn and left to the game to cull: a neighbour's face may not cover them (a port in the
	 * middle of a machine's side), and then the cover would be open there.
	 */
	public record Box(AABB box, Set<Direction> hidden) {}

	@Nullable
	public static PipeCover cover(BlockGetter level, BlockPos pos) {
		return coverOf(level.getBlockEntity(pos));
	}

	@Nullable
	private static PipeCover coverOf(@Nullable BlockEntity be) {
		return be instanceof PipeTileEntity && be.hasData(ModAttachments.PIPE_COVER) ? be.getData(ModAttachments.PIPE_COVER) : null;
	}

	/**
	 * The covered pipe's outline and collision, or null to leave the pipe's own. Asked for every collision check and
	 * every look at the block, so each snug shape is built once and kept.
	 */
	@Nullable
	public static VoxelShape shapeOrNull(BlockState state, BlockGetter level, BlockPos pos) {
		BlockEntity be = level.getBlockEntity(pos);
		PipeCover cover = coverOf(be);
		if (cover == null)
			return null;
		if (cover.kind() == PipeCover.Kind.FULL)
			return Shapes.block();
		int connected = connected(state);
		return snugShape(connected, extracting(be, connected));
	}

	/** A snug cover's shape for these connected and extracting sides (bit {@code 1 << d.get3DDataValue()} each). */
	public static VoxelShape snugShape(int connected, int extracting) {
		int key = connected | (extracting & connected) << 6;
		VoxelShape shape = SNUG_SHAPES.get(key);
		if (shape == null) {
			shape = buildShape(PipeCover.Kind.SNUG, connected, extracting);
			SNUG_SHAPES.set(key, shape);
		}
		return shape;
	}

	/** A cover's shape built from its boxes, without the store. */
	static VoxelShape buildShape(PipeCover.Kind kind, int connected, int extracting) {
		if (kind == PipeCover.Kind.FULL)
			return Shapes.block();
		VoxelShape shape = Shapes.empty();
		for (Box box : boxes(kind, connected, extracting))
			shape = Shapes.or(shape, Shapes.create(box.box()));
		return shape.optimize();
	}

	/**
	 * A bare pipe's shape that depends only on its block state (Pipez's collision, support, interaction, occlusion and
	 * visual shapes all are), kept per state: Pipez builds it again on every call.
	 */
	public static VoxelShape plainShape(BlockState state) {
		VoxelShape shape = PLAIN_SHAPES.get(state);
		if (shape == null && state.getBlock() instanceof PipeBlock pipe) {
			shape = pipe.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, state, false);
			PLAIN_SHAPES.put(state, shape);
		}
		return shape != null ? shape : Shapes.block();
	}

	public static List<Box> boxes(PipeCover.Kind kind, BlockState state, BlockGetter level, BlockPos pos) {
		if (kind == PipeCover.Kind.FULL)
			return boxes(kind, 0, 0);
		int connected = connected(state);
		return boxes(kind, connected, extracting(level.getBlockEntity(pos), connected));
	}

	/** The cover's boxes for these connected and extracting sides (bit {@code 1 << d.get3DDataValue()} each). */
	public static List<Box> boxes(PipeCover.Kind kind, int connected, int extracting) {
		if (kind == PipeCover.Kind.FULL)
			return List.of(new Box(new AABB(0, 0, 0, 1, 1, 1), Set.of()));
		List<Box> boxes = new ArrayList<>();
		EnumSet<Direction> sides = EnumSet.noneOf(Direction.class);
		for (Direction d : Direction.values())
			if (has(connected, d))
				sides.add(d);
		boxes.add(new Box(px(4, 4, 4, 12, 12, 12), sides));
		for (Direction d : sides) {
			if (has(extracting, d)) {
				// The collar's cover, against the neighbour, then the arm on to the core
				boxes.add(new Box(along(d, 0, 2, 3, 13), EnumSet.noneOf(Direction.class)));
				boxes.add(new Box(along(d, 2, 4, 4, 12), EnumSet.of(d, d.getOpposite())));
			} else {
				boxes.add(new Box(along(d, 0, 4, 4, 12), EnumSet.of(d.getOpposite())));
			}
		}
		return boxes;
	}

	/** The sides a pipe is connected on, as a mask; none for anything that isn't a pipe. */
	static int connected(BlockState state) {
		int mask = 0;
		if (state.getBlock() instanceof PipeBlock pipe)
			for (Direction d : Direction.values())
				if (state.getValue(pipe.getProperty(d)))
					mask |= 1 << d.get3DDataValue();
		return mask;
	}

	/** Which of the connected sides the pipe's block entity extracts from, as a mask. */
	private static int extracting(@Nullable BlockEntity be, int connected) {
		int mask = 0;
		if (be instanceof PipeTileEntity tile)
			for (Direction d : Direction.values())
				if (has(connected, d) && tile.isExtracting(d))
					mask |= 1 << d.get3DDataValue();
		return mask;
	}

	private static boolean has(int mask, Direction d) {
		return (mask & 1 << d.get3DDataValue()) != 0;
	}

	private static AABB px(double x0, double y0, double z0, double x1, double y1, double z1) {
		return new AABB(x0 / 16, y0 / 16, z0 / 16, x1 / 16, y1 / 16, z1 / 16);
	}

	/**
	 * A box reaching from {@code near} to {@code far} pixels in from side {@code d}, and from {@code lo} to {@code hi}
	 * pixels across the other two axes.
	 */
	private static AABB along(Direction d, double near, double far, double lo, double hi) {
		boolean positive = d.getAxisDirection() == Direction.AxisDirection.POSITIVE;
		double a = positive ? 16 - far : near, b = positive ? 16 - near : far;
		return switch (d.getAxis()) {
			case X -> px(a, lo, lo, b, hi, hi);
			case Y -> px(lo, a, lo, hi, b, hi);
			case Z -> px(lo, lo, a, hi, hi, b);
		};
	}
}
