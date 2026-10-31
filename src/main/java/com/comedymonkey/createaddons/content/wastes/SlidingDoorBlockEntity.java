package com.comedymonkey.createaddons.content.wastes;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Sliding Door's lock, on its controller. Once the {@link CityRecord} says its district is awake (the heart's: all
 * four), it opens for good: every part's {@link SlidingDoorBlock#OPEN} at once (nothing stops anyone from then on), and,
 * if it happened while loaded, the seam lights and the leaves slide into the walls over the next second and a half.
 */
public class SlidingDoorBlockEntity extends CityBlockEntity {
	/** Ticks from opening: the seam lights, then the leaves slide (0.5 s to 1.5 s, ease-out), then they're in the walls. */
	public static final int SLIDE_FROM = 10, SLIDE_TO = 30;

	private long openedAt = SubstationBlockEntity.NEVER;

	public SlidingDoorBlockEntity(BlockPos pos, BlockState state) {
		super(ModWastes.SLIDING_DOOR_BE.get(), pos, state);
	}

	public boolean isOpen() {
		return getBlockState().getValue(SlidingDoorBlock.OPEN);
	}

	public long openedAt() {
		return openedAt;
	}

	@Override
	protected void recheck(boolean live, @Nullable BlockPos from) {
		CityRecord record = record();
		if (record == null || isOpen() || !link.bound())
			return;
		boolean heart = getBlockState().getValue(SlidingDoorBlock.HEART);
		if (record.doorOpen(link.origin(), heart ? District.HEART : link.district()))
			open(live);
	}

	/** Opens for good, every part at once (animated when {@code live}). */
	public void open(boolean live) {
		if (level == null || isOpen())
			return;
		openedAt = live ? level.getGameTime() : SubstationBlockEntity.NEVER;
		BlockState state = getBlockState();
		Direction facing = state.getValue(SlidingDoorBlock.FACING);
		// the other parts first (each would otherwise see the controller change and find the door whole anyway)
		for (int c = 0; c < 2; c++)
			for (int r = 0; r < 3; r++) {
				BlockPos p = SlidingDoorBlock.part(worldPosition, facing, c, r);
				BlockState there = level.getBlockState(p);
				if (there.is(state.getBlock()) && !there.getValue(SlidingDoorBlock.OPEN))
					level.setBlock(p, there.setValue(SlidingDoorBlock.OPEN, true), Block.UPDATE_ALL);
			}
		sync();
	}

	@Override
	protected void writeLooks(CompoundTag tag) {
		tag.putLong("OpenedAt", openedAt);
	}

	@Override
	protected void readLooks(CompoundTag tag) {
		openedAt = tag.contains("OpenedAt") ? tag.getLong("OpenedAt") : SubstationBlockEntity.NEVER;
	}
}
