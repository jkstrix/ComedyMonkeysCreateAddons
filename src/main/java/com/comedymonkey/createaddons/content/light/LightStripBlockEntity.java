package com.comedymonkey.createaddons.content.light;

import java.util.List;

import com.comedymonkey.createaddons.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** A block of Light Strip: a piece of a run of strips joined as StripLinks joins them. */
public class LightStripBlockEntity extends RunLightBlockEntity {
	public LightStripBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.LIGHT_STRIP.get(), pos, state);
	}

	@Override
	protected List<BlockPos> joined(BlockPos p, BlockState state) {
		return StripLinks.joined(level, p, state);
	}
}
