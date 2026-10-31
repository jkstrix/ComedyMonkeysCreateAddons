package com.comedymonkey.createaddons.content.wastes;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A wall vent's link to its city's heart (D57, D60). Sealed until the heart wakes (all four districts), then live; the
 * Custodian {@link #burst}s it once when it calls its Scuttlers, and it stays open until the Custodian resets, when it
 * {@link #reseal}s with a new Scuttler inside (D67). The burst's look (the seam
 * flaring, the hatch rattling and blowing flat, the vent preview's sequence) runs from {@link #burstAt}, synced.
 */
public class WallVentBlockEntity extends CityBlockEntity {
	/** The burst sequence (the vent preview's burst clip), seconds from its start: the seam flares to the core tone... */
	public static final float FLASH_FROM = 0.1f, FLASH_TO = 0.3f;
	/** ...the hatch rattles (something kicking inside)... */
	public static final float RATTLE_FROM = 0.15f, RATTLE_HARDER = 0.3f;
	/** ...and blows: slammed flat onto the floor in 0.1 s, bouncing twice; the seam and contact go out (spent). */
	public static final float BLOW = 0.45f, SLAMMED = 0.55f, BURST_LENGTH = 2.6f;

	private long burstAt = SubstationBlockEntity.NEVER;

	public WallVentBlockEntity(BlockPos pos, BlockState state) {
		super(ModWastes.WALL_VENT_BE.get(), pos, state);
	}

	public WallVentBlock.State ventState() {
		return WallVentBlock.state(getBlockState());
	}

	/** Its Scuttler waits behind its hatch: the heart is awake and it hasn't burst. */
	public boolean isLive() {
		return ventState() == WallVentBlock.State.LIVE;
	}

	public boolean isOpen() {
		return ventState() == WallVentBlock.State.OPEN;
	}

	/** Game time its burst started (NEVER: it hasn't, or it was already open when it loaded). */
	public long burstAt() {
		return burstAt;
	}

	/** The heart woke (or had woken while it was unloaded): sealed turns live. Spent stays spent. */
	@Override
	protected void recheck(boolean live, @Nullable BlockPos from) {
		CityRecord record = record();
		if (record == null || level == null || !link.bound() || ventState() != WallVentBlock.State.SEALED)
			return;
		if (record.awake(link.origin(), District.HEART)) {
			level.setBlock(worldPosition, getBlockState().setValue(WallVentBlock.STATE, WallVentBlock.State.LIVE), Block.UPDATE_ALL);
			sync();
		}
	}

	/** The Custodian calls (D57): it bursts, once, and is spent for good; true if it did. */
	public boolean burst() {
		if (level == null || level.isClientSide || !isLive())
			return false;
		burstAt = level.getGameTime();
		level.setBlock(worldPosition, getBlockState().setValue(WallVentBlock.STATE, WallVentBlock.State.OPEN), Block.UPDATE_ALL);
		sync();
		return true;
	}

	/**
	 * The Custodian reset (D67): a spent vent shuts again with a new Scuttler curled behind its hatch: sealed, then live
	 * at once while its heart is awake, so it can burst again in the next fight. True if it was spent.
	 */
	public boolean reseal() {
		if (level == null || level.isClientSide || !isOpen())
			return false;
		burstAt = SubstationBlockEntity.NEVER;
		level.setBlock(worldPosition, getBlockState().setValue(WallVentBlock.STATE, WallVentBlock.State.SEALED), Block.UPDATE_ALL);
		sync();
		recheck(true, null);
		return true;
	}

	@Override
	protected void writeLooks(CompoundTag tag) {
		tag.putLong("BurstAt", burstAt);
	}

	@Override
	protected void readLooks(CompoundTag tag) {
		burstAt = tag.contains("BurstAt") ? tag.getLong("BurstAt") : SubstationBlockEntity.NEVER;
	}
}
