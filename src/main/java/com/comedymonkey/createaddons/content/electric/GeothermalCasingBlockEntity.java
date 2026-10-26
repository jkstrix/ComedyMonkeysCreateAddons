package com.comedymonkey.createaddons.content.electric;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Remembers which controller this casing belongs to, so its capabilities lead there. */
public class GeothermalCasingBlockEntity extends BlockEntity implements com.comedymonkey.createaddons.content.line.PowerGrid.Generator, com.comedymonkey.createaddons.content.line.NetworkReading.Shown {
	@Nullable
	private BlockPos controller;

	public GeothermalCasingBlockEntity(BlockPos pos, BlockState state) {
		super(GeothermalPartBlock.casingType(), pos, state);
	}

	@Nullable
	public BlockPos getController() {
		return controller;
	}

	public void setController(@Nullable BlockPos controller) {
		this.controller = controller;
		setChanged();
	}

	/** The controller's block entity, if this casing is part of a formed turbine. */
	@Nullable
	public GeothermalControllerBlockEntity controller() {
		if (controller == null || level == null || !level.isLoaded(controller))
			return null;
		return level.getBlockEntity(controller) instanceof GeothermalControllerBlockEntity be && be.isFormed() ? be : null;
	}

	/**
	 * The turbine's lava, water and power connect only through the port flanges under its base: the bottom face of the
	 * bottom layer of casings. {@code null} (no side, e.g. a mod looking inside) is let through as well.
	 */
	/** A cable on a port flange draws on the turbine: recorded as its controller. */
	@Override
	public BlockPos generatorKey() {
		return controller != null ? controller : worldPosition;
	}

	/** A line's network figures, met at a port casing, go to the turbine. */
	@Override
	public void networkReading(int makes, int uses) {
		GeothermalControllerBlockEntity be = controller();
		if (be != null)
			be.networkReading(makes, uses);
	}

	/** Fluid a line brings a port casing goes to the turbine. */
	@Override
	public void fed(BlockPos source, net.minecraft.world.level.material.Fluid fluid, int amount) {
		GeothermalControllerBlockEntity be = controller();
		if (be != null)
			be.fed(source, fluid, amount);
	}

	@Nullable
	public GeothermalControllerBlockEntity port(@Nullable Direction side) {
		GeothermalControllerBlockEntity be = controller();
		if (be == null || side == null)
			return be;
		return side == Direction.DOWN && worldPosition.getY() == be.centre().getY() - 1 ? be : null;
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		if (controller != null)
			tag.put("Controller", NbtUtils.writeBlockPos(controller));
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		controller = NbtUtils.readBlockPos(tag, "Controller").orElse(null);
	}
}
