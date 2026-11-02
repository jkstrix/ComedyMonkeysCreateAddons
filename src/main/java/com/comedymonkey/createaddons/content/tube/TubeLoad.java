package com.comedymonkey.createaddons.content.tube;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * One load in one tube: an item (a stack from a Brass Inlet) on its way to the Receiver. It came in through
 * {@link #from}, leaves through {@link #to}, and is {@link #progress} of the way across the block (0 at the face it came
 * in through, half at the middle, 1 at the face it leaves by). A load lives in exactly one tube, so nothing that
 * happens to a tube can lose or copy it (design/specs/clockwork-tubes.md D14, D20).
 */
public final class TubeLoad {
	public ItemStack stack;
	public Direction from;
	public Direction to;
	public float progress;
	/** The game time {@link #progress} was true at, and how far it moves a tick from then (for the client to glide it). */
	public long since;
	public float speed;

	public TubeLoad(ItemStack stack, Direction from, Direction to, float progress, long since, float speed) {
		this.stack = stack;
		this.from = from;
		this.to = to;
		this.progress = progress;
		this.since = since;
		this.speed = speed;
	}

	/** Where it is at {@code time} as the client sees it: gliding on from its last update, waiting at the far face. */
	public float progressAt(double time) {
		return (float) Math.max(0, Math.min(1, progress + (time - since) * speed));
	}

	public CompoundTag write(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.put("Item", stack.save(registries));
		tag.putByte("From", (byte) from.get3DDataValue());
		tag.putByte("To", (byte) to.get3DDataValue());
		tag.putFloat("Progress", progress);
		tag.putLong("Since", since);
		tag.putFloat("Speed", speed);
		return tag;
	}

	@Nullable
	public static TubeLoad read(CompoundTag tag, HolderLookup.Provider registries) {
		ItemStack stack = ItemStack.parseOptional(registries, tag.getCompound("Item"));
		if (stack.isEmpty())
			return null;
		return new TubeLoad(stack, Direction.from3DDataValue(tag.getByte("From")), Direction.from3DDataValue(tag.getByte("To")),
			tag.getFloat("Progress"), tag.getLong("Since"), tag.getFloat("Speed"));
	}
}
