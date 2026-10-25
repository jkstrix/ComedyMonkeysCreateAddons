package com.comedymonkey.createaddons.content.line;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.DyeColor;

/**
 * A span of cable hung from insulator {@code from} of one Line Post head to insulator {@code to} of another, {@code
 * offset} away, that took {@code wires} Power Lines to hang (and gives them back when it comes down). Kept on both
 * posts; the {@code owner} end draws it, and alone refunds it if the far post goes missing. The offset is relative, so
 * a ship carrying both posts carries the span with it.
 */
public record Span(int from, BlockPos offset, int to, LineTier tier, @Nullable DyeColor dye, boolean owner, int wires) {
	/** The same span as the far end keeps it. */
	public Span reversed() {
		return new Span(to, offset.multiply(-1), from, tier, dye, !owner, wires);
	}

	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putInt("From", from);
		tag.put("Offset", NbtUtils.writeBlockPos(offset));
		tag.putInt("To", to);
		tag.putString("Tier", tier.getSerializedName());
		if (dye != null)
			tag.putInt("Dye", dye.getId());
		if (owner)
			tag.putBoolean("Owner", true);
		tag.putInt("Wires", wires);
		return tag;
	}

	public static Span load(CompoundTag tag) {
		BlockPos offset = NbtUtils.readBlockPos(tag, "Offset").orElse(BlockPos.ZERO);
		LineTier tier = "brass".equals(tag.getString("Tier")) ? LineTier.BRASS : LineTier.COPPER;
		int wires = tag.contains("Wires") ? tag.getInt("Wires") : LinePostBlockEntity.wireCount(Math.sqrt(offset.distSqr(BlockPos.ZERO)));
		return new Span(tag.getInt("From"), offset, tag.getInt("To"), tier, tag.contains("Dye") ? DyeColor.byId(tag.getInt("Dye")) : null,
			tag.getBoolean("Owner"), wires);
	}
}
