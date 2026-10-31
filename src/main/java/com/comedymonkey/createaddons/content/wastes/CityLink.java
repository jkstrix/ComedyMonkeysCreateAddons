package com.comedymonkey.createaddons.content.wastes;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Which city a city block belongs to (design/specs/clockwork-wastes.md, P48): the city's origin (the key of its
 * {@link CityRecord}) and its district. Kept in the block entity's NBT, so a structure template can carry it and the
 * city's builder can set it after placing ({@link #bindAll}). A block with no city (one placed in creative) has no
 * origin: it reads as a district that never wakes.
 */
public final class CityLink {
	@Nullable
	private BlockPos origin;
	private District district = District.FOUNDRY;

	@Nullable
	public BlockPos origin() {
		return origin;
	}

	public District district() {
		return district;
	}

	public boolean bound() {
		return origin != null;
	}

	public void set(@Nullable BlockPos origin, District district) {
		this.origin = origin == null ? null : origin.immutable();
		this.district = district;
	}

	public void write(CompoundTag tag) {
		if (origin != null)
			tag.put("CityOrigin", NbtUtils.writeBlockPos(origin));
		tag.putString("District", district.getSerializedName());
	}

	public void read(CompoundTag tag) {
		origin = NbtUtils.readBlockPos(tag, "CityOrigin").orElse(null);
		String d = tag.getString("District");
		district = District.FOUNDRY;
		for (District x : District.values())
			if (x.getSerializedName().equals(d))
				district = x;
	}

	/** A city block's block entity: it belongs to a district of a city, and hears when that city's record changes. */
	public interface Member {
		CityLink cityLink();

		/**
		 * Its city's record changed while it was loaded (a district woke, was fed or went unfed, the heart burned, the
		 * Custodian woke or died): it may animate (a lever slams, an iris opens). {@code from}: where the change started (a
		 * Substation, the heart's reactor), so lamps can ripple out from it; null if nowhere in particular.
		 */
		void cityChanged(@Nullable BlockPos from);

		/** It looks at its city's record afresh, as it does on loading (no animation: nothing just happened). */
		default void catchUp() {
			cityChanged(null);
		}

		/** The heart surged (the Custodian's overload, D54): a heart lamp flares and flickers for about 3 s. */
		default void surged() {}

		/** Its link was set again: it hears its new city from now on, and catches up with it. */
		default void relinked() {
			BlockEntity be = (BlockEntity) this;
			if (be.getLevel() instanceof ServerLevel level) {
				CityRecord.track(this, level);
				be.setChanged();
				catchUp();
			}
		}
	}

	/** Binds one city block to a city's district (phase 3: after placing it, or from a structure processor). */
	public static boolean bind(Level level, BlockPos pos, BlockPos origin, District district) {
		if (!(level.getBlockEntity(pos) instanceof Member m))
			return false;
		m.cityLink().set(origin, district);
		m.relinked();
		return true;
	}

	/**
	 * Binds every city block in a box to a city's district: the city's builder calls this once per district piece
	 * after placing it. Returns how many it bound.
	 */
	public static int bindAll(Level level, BoundingBox box, BlockPos origin, District district) {
		int n = 0;
		for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()))
			if (level.isLoaded(p) && bind(level, p, origin, district))
				n++;
		return n;
	}
}
