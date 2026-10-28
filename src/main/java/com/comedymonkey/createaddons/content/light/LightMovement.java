package com.comedymonkey.createaddons.content.light;

import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How Create's contraptions move the Electric Lights (design/specs/electric-lights.md, P11): each light goes with the
 * block it's fixed to (a hanging lamp with its cable and ceiling, a Festoon hook with its wall or post), and is taken off
 * first and put back last, after that block. On the move they keep their glow and light nothing; put back, they wake and
 * look for power again.
 */
public final class LightMovement {
	private LightMovement() {}

	public static void register() {
		BlockMovementChecks.registerAttachedCheck(LightMovement::attached);
		BlockMovementChecks.registerBrittleCheck(state -> light(state) ? CheckResult.SUCCESS : CheckResult.PASS);
	}

	private static boolean light(BlockState state) {
		Block b = state.getBlock();
		return b instanceof LightBlock || b instanceof LightStripBlock || b instanceof EdgeLightBlock || b instanceof FestoonHookBlock
			|| b instanceof HangingCableBlock;
	}

	/** The light at pos is fixed to the block the way {@code dir} is. */
	static CheckResult attached(BlockState state, Level level, BlockPos pos, Direction dir) {
		Block b = state.getBlock();
		if (b instanceof HangingCableBlock)
			return CheckResult.of(dir.getAxis() == Direction.Axis.Y);
		if (b instanceof HangingLampBlock && state.getValue(HangingLampBlock.HUNG))
			return dir == Direction.UP ? CheckResult.SUCCESS : CheckResult.PASS;
		if (b instanceof LightBlock)
			return pos.relative(dir).equals(LightBlock.support(state, pos)) ? CheckResult.SUCCESS : CheckResult.PASS;
		if (b instanceof LightStripBlock)
			return LightStripBlock.hasFace(state, dir) ? CheckResult.SUCCESS : CheckResult.PASS;
		if (b instanceof EdgeLightBlock)
			return EdgeLinks.onFace(EdgeLinks.edgesAt(level, pos), dir) ? CheckResult.SUCCESS : CheckResult.PASS;
		if (b instanceof FestoonHookBlock)
			return dir == state.getValue(FestoonHookBlock.FACING).getOpposite() ? CheckResult.SUCCESS : CheckResult.PASS;
		return CheckResult.PASS;
	}
}
