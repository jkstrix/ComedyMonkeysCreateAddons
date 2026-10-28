package com.comedymonkey.createaddons.content.light;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Which light a remote spot belongs to (LightSpotBlock). */
public class LightSpotBlockEntity extends BlockEntity {
	@Nullable
	private BlockPos owner;

	public LightSpotBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.LIGHT_SPOT.get(), pos, state);
	}

	public @Nullable BlockPos owner() {
		return owner;
	}

	void setOwner(BlockPos owner) {
		this.owner = owner.immutable();
		setChanged();
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		if (owner != null)
			tag.putLong("Owner", owner.asLong());
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		owner = tag.contains("Owner") ? BlockPos.of(tag.getLong("Owner")) : null;
	}
}
