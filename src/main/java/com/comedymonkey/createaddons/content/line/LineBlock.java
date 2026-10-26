package com.comedymonkey.createaddons.content.line;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A block a line fitting can clip onto: Fluid Lines and Power Lines. */
public interface LineBlock {
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
