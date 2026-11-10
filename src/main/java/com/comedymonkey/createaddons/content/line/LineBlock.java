package com.comedymonkey.createaddons.content.line;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A block a line fitting can clip onto: Fluid Lines and Power Lines. */
public interface LineBlock {
	/**
	 * Encased in a block (kept with the block entity's encasing). Light works from the block state alone, so this makes an
	 * encased line light like the solid block it looks like: it blocks light and sky, and shades its corners and its
	 * neighbours'.
	 */
	BooleanProperty ENCASED = BooleanProperty.create("encased");

	static boolean encased(BlockState state) {
		return state.getBlock() instanceof LineBlock && state.getValue(ENCASED);
	}

	/** An encased line hides its neighbours' faces and the light behind it; a bare one neither. */
	static VoxelShape occlusionShape(BlockState state) {
		return encased(state) ? Shapes.block() : Shapes.empty();
	}

	/** Ambient occlusion: a solid block's shade when encased, none when bare. */
	static float shadeBrightness(BlockState state) {
		return encased(state) ? 0.2f : 1.0f;
	}

	/** Whether this kind of line takes that fitting. */
	boolean takes(Fitting fitting);

	/** One end of the straight run through this block (an in-line fitting turns that way), or null if it isn't straight. */
	@Nullable
	Direction straightSide(Level level, BlockPos pos, BlockState state);

	/** Where that fitting would sit here (the way it turns), or null if it can't: by default, on a straight run. */
	@Nullable
	default Direction sideFor(Fitting fitting, Level level, BlockPos pos, BlockState state) {
		return straightSide(level, pos, state);
	}

	/** The line's own outline, encased or not (the goggles draw it through the encasing). */
	VoxelShape lineShape(BlockState state, BlockGetter level, BlockPos pos);
}
