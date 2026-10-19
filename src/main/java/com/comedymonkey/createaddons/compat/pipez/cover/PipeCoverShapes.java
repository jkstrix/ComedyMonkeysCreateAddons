package com.comedymonkey.createaddons.compat.pipez.cover;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModAttachments;

import de.maxhenkel.pipez.blocks.PipeBlock;
import de.maxhenkel.pipez.blocks.tileentity.PipeTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
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
	private PipeCoverShapes() {}

	/** A box, and the sides of it that are never drawn because they're buried against another box or a neighbour. */
	public record Box(AABB box, Set<Direction> hidden) {}

	@Nullable
	public static PipeCover cover(BlockGetter level, BlockPos pos) {
		BlockEntity be = level.getBlockEntity(pos);
		return be instanceof PipeTileEntity && be.hasData(ModAttachments.PIPE_COVER) ? be.getData(ModAttachments.PIPE_COVER) : null;
	}

	/** The covered pipe's outline and collision, or null to leave the pipe's own. */
	@Nullable
	public static VoxelShape shapeOrNull(BlockState state, BlockGetter level, BlockPos pos) {
		PipeCover cover = cover(level, pos);
		if (cover == null)
			return null;
		if (cover.kind() == PipeCover.Kind.FULL)
			return Shapes.block();
		VoxelShape shape = Shapes.empty();
		for (Box box : boxes(cover.kind(), state, level, pos))
			shape = Shapes.or(shape, Shapes.create(box.box()));
		return shape.optimize();
	}

	public static List<Box> boxes(PipeCover.Kind kind, BlockState state, BlockGetter level, BlockPos pos) {
		if (kind == PipeCover.Kind.FULL)
			return List.of(new Box(new AABB(0, 0, 0, 1, 1, 1), Set.of()));
		List<Box> boxes = new ArrayList<>();
		EnumSet<Direction> connected = EnumSet.noneOf(Direction.class);
		BlockEntity be = level.getBlockEntity(pos);
		if (state.getBlock() instanceof PipeBlock pipe)
			for (Direction d : Direction.values())
				if (state.getValue(pipe.getProperty(d)))
					connected.add(d);
		boxes.add(new Box(px(4, 4, 4, 12, 12, 12), connected));
		for (Direction d : connected) {
			boolean extracting = be instanceof PipeTileEntity tile && tile.isExtracting(d);
			if (extracting) {
				// The collar's cover, against the neighbour, then the arm on to the core
				boxes.add(new Box(along(d, 0, 2, 3, 13), EnumSet.of(d)));
				boxes.add(new Box(along(d, 2, 4, 4, 12), EnumSet.of(d, d.getOpposite())));
			} else {
				boxes.add(new Box(along(d, 0, 4, 4, 12), EnumSet.of(d, d.getOpposite())));
			}
		}
		return boxes;
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
