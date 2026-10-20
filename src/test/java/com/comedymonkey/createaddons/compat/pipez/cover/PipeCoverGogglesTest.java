package com.comedymonkey.createaddons.compat.pipez.cover;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

/** Only the uncovered case: the covered tooltip lines are laid out with the client's font, which needs a running client. */
class PipeCoverGogglesTest {
	@Test
	void uncoveredPipesShowNothing() {
		List<Component> tooltip = new ArrayList<>();
		assertFalse(PipeCoverGoggles.addToTooltip(new FurnaceBlockEntity(BlockPos.ZERO, Blocks.FURNACE.defaultBlockState()), tooltip));
		assertTrue(tooltip.isEmpty());
	}
}
