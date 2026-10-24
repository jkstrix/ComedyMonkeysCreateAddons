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
public class GeothermalCasingBlockEntity extends BlockEntity {
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
