package com.comedymonkey.createaddons.content.wastes;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.content.line.PowerGrid;
import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * One of the Derelict Star Reactor's four ports (the bottom edge-middles): where a Power Line meets the reactor, recorded
 * as the reactor's controller (as the Geothermal Turbine's port casings are), so the line draws its spare power (P10).
 */
public class DerelictReactorPortBlockEntity extends BlockEntity implements PowerGrid.Generator {
	public DerelictReactorPortBlockEntity(BlockPos pos, BlockState state) {
		super(ModWastes.DERELICT_REACTOR_PORT_BE.get(), pos, state);
	}

	@Override
	public BlockPos generatorKey() {
		return DerelictStarReactorBlock.controller(getBlockState(), worldPosition);
	}

	/** The reactor's power, as a line sees it here; none once it's salvaged. */
	@Nullable
	public IEnergyStorage energy() {
		if (level == null || !level.isLoaded(generatorKey()))
			return null;
		return level.getBlockEntity(generatorKey()) instanceof DerelictStarReactorBlockEntity be && !be.isSalvaged() ? be.energyOut() : null;
	}
}
