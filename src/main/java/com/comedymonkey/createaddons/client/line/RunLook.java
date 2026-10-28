package com.comedymonkey.createaddons.client.line;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.light.LightBlock;
import com.comedymonkey.createaddons.content.light.LightBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How a block of a run (Light Strip, Edge Light) shows its diffuser in its baked model: "lit", "dark" (the unlit glass),
 * "body" (none: a starving run's flicker is drawn frame by frame by RunFlickerRenderer), or only the diffuser, "glow" or
 * "glass" (what RunFlickerRenderer draws).
 */
final class RunLook {
	private RunLook() {}

	/** As it shows in the world. */
	static String mode(BlockAndTintGetter level, BlockPos pos, BlockState state) {
		if (state.getValue(LightBlock.LIGHT) == 0)
			return "dark";
		return level.getBlockEntity(pos) instanceof LightBlockEntity be && be.phase() == LightBlockEntity.Phase.FADING ? "body" : "lit";
	}

	/** The channel or trim the diffuser sits in is drawn. */
	static boolean body(String mode) {
		return mode.equals("lit") || mode.equals("dark") || mode.equals("body");
	}

	/** Which diffuser is drawn ("_glow" or "_glass"), or none. */
	static @Nullable String diffuser(String mode) {
		return switch (mode) {
			case "lit", "glow" -> "_glow";
			case "dark", "glass" -> "_glass";
			default -> null;
		};
	}
}
