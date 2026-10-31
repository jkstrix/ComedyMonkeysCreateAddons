package com.comedymonkey.createaddons.content.wastes;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Lift call plate's link to its district: it unlocks when the district wakes (P49, D24). Its lens lights while a call
 * from it waits for the deck (D62): {@link LiftBlockEntity#call} lights it, the deck's arrival puts it out.
 */
public class LiftCallPlateBlockEntity extends CityBlockEntity {
	/** Client: its lens went on or off (Flywheel makes its visual again). */
	public static java.util.function.Consumer<LiftCallPlateBlockEntity> clientChanged = be -> {};

	private boolean waiting;
	public LiftCallPlateBlockEntity(BlockPos pos, BlockState state) {
		super(ModWastes.LIFT_CALL_PLATE_BE.get(), pos, state);
	}

	public boolean isAwake() {
		return !getBlockState().getValue(LiftCallPlateBlock.LOCKED);
	}

	/** Its call waits for the deck: its lens is lit. */
	public boolean isCalling() {
		return waiting && isAwake();
	}

	void setWaiting(boolean waiting) {
		if (this.waiting == waiting)
			return;
		this.waiting = waiting;
		sync();
	}

	@Override
	protected void writeLooks(CompoundTag tag) {
		tag.putBoolean("Calling", waiting);
	}

	@Override
	protected void readLooks(CompoundTag tag) {
		boolean was = waiting;
		waiting = tag.getBoolean("Calling");
		if (was != waiting && level != null && level.isClientSide)
			clientChanged.accept(this);
	}

	@Override
	protected void recheck(boolean live, @Nullable BlockPos from) {
		CityRecord record = record();
		if (record == null || !link.bound() || level == null || isAwake())
			return;
		if (record.awake(link.origin(), link.district())) {
			level.setBlock(worldPosition, getBlockState().setValue(LiftCallPlateBlock.LOCKED, false), Block.UPDATE_ALL);
			sync();
		}
	}
}
