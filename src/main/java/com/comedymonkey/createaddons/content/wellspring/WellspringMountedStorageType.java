package com.comedymonkey.createaddons.content.wellspring;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Tells Create how to carry a Fluid Wellspring's tank on a contraption. */
public class WellspringMountedStorageType extends MountedFluidStorageType<WellspringMountedStorage> {
	public WellspringMountedStorageType() {
		super(WellspringMountedStorage.CODEC);
	}

	@Nullable
	@Override
	public WellspringMountedStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
		return be instanceof WellspringBlockEntity wellspring ? WellspringMountedStorage.of(wellspring) : null;
	}
}
