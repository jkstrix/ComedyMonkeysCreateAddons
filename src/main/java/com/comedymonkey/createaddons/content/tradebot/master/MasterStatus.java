package com.comedymonkey.createaddons.content.tradebot.master;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * What a Trade Master's station screen and goggles show (design/specs/trade-master.md P2, P3, P11; the screen as
 * previewed and approved in design/gui), as the server last saw it: the board chests and gauges he can see, what he's
 * doing and for which gauge, and every other gauge on his network: the ones he can't fill and why, the ones waiting or
 * being filled by another Trade Master, and the full ones, in that order.
 */
public record MasterStatus(int boards, int gauges, @Nullable Activity activity, ItemStack working, int level, int amount, int coming,
	List<Line> others) {
	/** The part of his trip he's on. */
	public enum Activity { FETCHING, SELLING, BUYING, DELIVERING, TIDYING }

	/** Where a gauge stands, in the list's order. */
	public enum State { CANT, WAITING, OTHER_MASTER, FULL }

	/** One of his other gauges: its item, how full, and its state (with why, if he can't fill it). */
	public record Line(ItemStack kind, int level, int amount, State state, @Nullable GaugePlanner.Reason reason) {}

	/** At most this many gauges are sent (a screen's worth many times over). */
	public static final int MAX_LINES = 256;
	public static final MasterStatus NONE = new MasterStatus(0, 0, null, ItemStack.EMPTY, 0, 0, 0, List.of());

	/** How many of his gauges he can't fill. */
	public int cantFill() {
		return (int) others.stream().filter(line -> line.state() == State.CANT).count();
	}

	public CompoundTag save(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("Boards", boards);
		tag.putInt("Gauges", gauges);
		if (activity != null)
			tag.putString("Activity", activity.name());
		if (!working.isEmpty()) {
			tag.put("Working", working.save(registries));
			tag.putInt("Level", level);
			tag.putInt("Amount", amount);
			tag.putInt("Coming", coming);
		}
		ListTag list = new ListTag();
		for (Line line : others) {
			CompoundTag entry = new CompoundTag();
			entry.put("Kind", line.kind().save(registries));
			entry.putInt("Level", line.level());
			entry.putInt("Amount", line.amount());
			entry.putString("State", line.state().name());
			if (line.reason() != null)
				entry.putString("Reason", line.reason().name());
			list.add(entry);
		}
		tag.put("Others", list);
		return tag;
	}

	public static MasterStatus load(CompoundTag tag, HolderLookup.Provider registries) {
		Activity activity = tag.contains("Activity") ? parse(Activity.class, tag.getString("Activity")) : null;
		ItemStack working = tag.contains("Working") ? ItemStack.parseOptional(registries, tag.getCompound("Working")) : ItemStack.EMPTY;
		List<Line> others = new ArrayList<>();
		for (Tag t : tag.getList("Others", Tag.TAG_COMPOUND)) {
			CompoundTag entry = (CompoundTag) t;
			State state = parse(State.class, entry.getString("State"));
			if (state == null)
				continue; // a state from another version: left out
			GaugePlanner.Reason reason = entry.contains("Reason") ? parse(GaugePlanner.Reason.class, entry.getString("Reason")) : null;
			others.add(new Line(ItemStack.parseOptional(registries, entry.getCompound("Kind")), entry.getInt("Level"), entry.getInt("Amount"), state, reason));
		}
		return new MasterStatus(tag.getInt("Boards"), tag.getInt("Gauges"), activity, working, tag.getInt("Level"), tag.getInt("Amount"),
			tag.getInt("Coming"), others);
	}

	@Nullable
	private static <E extends Enum<E>> E parse(Class<E> type, String name) {
		try {
			return Enum.valueOf(type, name);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
