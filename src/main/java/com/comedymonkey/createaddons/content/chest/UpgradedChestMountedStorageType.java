package com.comedymonkey.createaddons.content.chest;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Tells Create how to carry an upgraded chest's or barrel's contents on a contraption. */
public class UpgradedChestMountedStorageType extends MountedItemStorageType<UpgradedChestMountedStorage> {
	public UpgradedChestMountedStorageType() {
		super(UpgradedChestMountedStorage.CODEC);
	}

	@Nullable
	@Override
	public UpgradedChestMountedStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
		return be instanceof UpgradedStorage storage ? UpgradedChestMountedStorage.of(storage) : null;
	}
}
