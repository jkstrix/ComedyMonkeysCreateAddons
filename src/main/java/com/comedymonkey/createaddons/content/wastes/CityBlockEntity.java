package com.comedymonkey.createaddons.content.wastes;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A city block that reads its district's state from the {@link CityRecord} (P48): it joins its city's loaded blocks
 * when it loads, catches up with the record on its first scheduled tick (so a district that woke while this chunk was
 * unloaded opens or lights it as soon as it's back), and hears every later change while it stays loaded. Most of them
 * never tick: a change schedules one tick, in which {@link #recheck} looks at the record again.
 */
public abstract class CityBlockEntity extends BlockEntity implements CityLink.Member {
	protected final CityLink link = new CityLink();
	/** Where the last change started, waiting for the scheduled tick that acts on it (null: catching up on load). */
	@Nullable
	protected BlockPos changedFrom;
	/** A change is waiting for its tick (as opposed to the first look after loading). */
	protected boolean live;

	protected CityBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	@Override
	public CityLink cityLink() {
		return link;
	}

	@Nullable
	protected CityRecord record() {
		return level instanceof ServerLevel server ? CityRecord.of(server) : null;
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (level instanceof ServerLevel server) {
			CityRecord.track(this, server);
			// catch up with the record once the chunk is in (not now: blocks can't change while it loads)
			live = false;
			schedule(1);
		}
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		CityRecord.untrack(this);
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		CityRecord.untrack(this);
	}

	/** Its city changed while it was loaded: it looks at the record on its next tick (never while the record is still telling others). */
	@Override
	public void cityChanged(@Nullable BlockPos from) {
		changedFrom = from;
		live = true;
		schedule(1);
	}

	@Override
	public void catchUp() {
		schedule(1);
	}

	/** Its scheduled tick (the block forwards it): looks at the record again. */
	public void scheduledTick() {
		boolean wasLive = live;
		BlockPos from = changedFrom;
		live = false;
		changedFrom = null;
		recheck(wasLive, from);
	}

	/**
	 * Looks at its district in the record and follows it. {@code live}: something just changed while it was loaded (so
	 * it may animate: a lever slams, an iris opens); false when it's only catching up after loading.
	 */
	protected abstract void recheck(boolean live, @Nullable BlockPos from);

	protected void schedule(int delay) {
		if (level != null && !level.isClientSide && !isRemoved()
			&& !level.getBlockTicks().hasScheduledTick(worldPosition, getBlockState().getBlock()))
			level.scheduleTick(worldPosition, getBlockState().getBlock(), Math.max(1, delay));
	}

	protected void sync() {
		if (level != null && !level.isClientSide) {
			setChanged();
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	// ---------- saving and syncing ----------

	/** What it saves besides its link. */
	protected void writeState(CompoundTag tag, HolderLookup.Provider registries) {}

	protected void readState(CompoundTag tag, HolderLookup.Provider registries) {}

	/** What the client needs to draw it (sent on load and with each sync). */
	protected void writeLooks(CompoundTag tag) {}

	protected void readLooks(CompoundTag tag) {}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		link.write(tag);
		writeState(tag, registries);
		writeLooks(tag);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		link.read(tag);
		readState(tag, registries);
		readLooks(tag);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		writeLooks(tag);
		return tag;
	}

	@Override
	public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
		readLooks(tag);
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
		handleUpdateTag(pkt.getTag(), registries);
	}
}
