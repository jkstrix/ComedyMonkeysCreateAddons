package com.comedymonkey.createaddons.content.light;

import java.util.ArrayList;
import java.util.List;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Festoon hook (Festoons): a piece of a run, joined to the hooks at the far ends of its strings. The strings it holds
 * (hung from it first) it draws, draws power for (1 FE/t per 2 blocks of string at full, config) and lights with spots of
 * light along them while it's lit.
 */
public class FestoonBlockEntity extends RunLightBlockEntity {
	/**
	 * A string from this hook to the hook at {@code other} (facing out of its wall so); {@code held}: this hook holds it;
	 * {@code paid}: hung with Festoon Lights (not free in creative), so it gives them back.
	 */
	public record Span(BlockPos other, Direction facing, boolean held, boolean paid) {}

	private final List<Span> spans = new ArrayList<>();
	/** The spots of light it put along the strings it holds. */
	private final List<BlockPos> spots = new ArrayList<>();

	public FestoonBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.FESTOON.get(), pos, state);
	}

	@Override
	protected List<BlockPos> joined(BlockPos p, BlockState state) {
		return level != null && level.getBlockEntity(p) instanceof FestoonBlockEntity hook ? hook.partners() : List.of();
	}

	public List<Span> spans() {
		return spans;
	}

	public List<BlockPos> partners() {
		return spans.stream().map(Span::other).toList();
	}

	void addSpan(Span span) {
		spans.add(span);
		changed();
	}

	void removeSpan(BlockPos other) {
		if (spans.removeIf(s -> s.other().equals(other)))
			changed();
	}

	void clearSpans() {
		spans.clear();
		if (level != null && !level.isClientSide)
			for (BlockPos p : spots)
				LightSpotBlock.remove(level, p, worldPosition);
		spots.clear();
	}

	/** Its strings changed: its spots follow (while it's lit), and the clients see it. */
	private void changed() {
		if (level == null || level.isClientSide)
			return;
		shown(phase() == Phase.LIT);
		setChanged();
		level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
	}

	private Direction facing() {
		return getBlockState().getValue(FestoonHookBlock.FACING);
	}

	private double length(Span s) {
		return Festoons.eye(worldPosition, facing()).distanceTo(Festoons.eye(s.other(), s.facing()));
	}

	/** Its strings' draw at its brightness step (P2: 1 FE/t per 2 blocks of string at full), half of each at either end. */
	@Override
	public double draw() {
		double blocks = 0;
		for (Span s : spans)
			blocks += length(s) / 2;
		return blocks * CMConfig.SERVER.lightFestoonFePerTick() * LightKind.STEPS[step()];
	}

	/**
	 * Its strings whose far hook is gone (taken down while this one was unloaded) come down too, one it held giving back
	 * its Festoon Lights here. False if it has none left: it goes.
	 */
	boolean check() {
		if (level == null || level.isClientSide)
			return true;
		boolean changed = false;
		for (java.util.Iterator<Span> it = spans.iterator(); it.hasNext();) {
			Span s = it.next();
			if (!level.isLoaded(s.other())
				|| level.getBlockEntity(s.other()) instanceof FestoonBlockEntity far && far.partners().contains(worldPosition))
				continue;
			it.remove();
			changed = true;
			if (s.held() && s.paid())
				net.minecraft.world.level.block.Block.popResource(level, worldPosition,
					new net.minecraft.world.item.ItemStack(com.comedymonkey.createaddons.registry.ModItems.FESTOON_LIGHTS.get(), Festoons.cost(length(s))));
		}
		if (changed) {
			RunLightBlockEntity.runsChanged();
			changed();
		}
		return !spans.isEmpty();
	}

	/** Lit: a spot of light every 4 blocks along each string it holds, at its brightness and colour; dark: none. */
	@Override
	protected void shown(boolean lit) {
		if (level == null || level.isClientSide || isRemoved())
			return;
		List<BlockPos> want = new ArrayList<>();
		if (lit)
			for (Span s : spans)
				if (s.held())
					want.addAll(Festoons.spotCells(worldPosition, facing(), s.other(), s.facing()));
		for (BlockPos p : spots)
			if (!want.contains(p))
				LightSpotBlock.remove(level, p, worldPosition);
		int light = Math.max(1, (int) Math.round(Festoons.LIGHT * LightKind.STEPS[step()]));
		List<BlockPos> placed = new ArrayList<>();
		for (BlockPos p : want)
			if (LightSpotBlock.place(level, p, worldPosition, light, dye()))
				placed.add(p);
		if (!placed.equals(spots)) {
			spots.clear();
			spots.addAll(placed);
			setChanged();
		}
	}

	@Override
	public boolean ownsSpot(BlockPos p) {
		return spots.contains(p);
	}

	// ---------- saving and syncing ----------

	private void writeSpans(CompoundTag tag) {
		ListTag list = new ListTag();
		for (Span s : spans) {
			CompoundTag t = new CompoundTag();
			// (from this hook: carried off with its far hook by a contraption, the string still joins them)
			t.putLong("Off", s.other().subtract(worldPosition).asLong());
			t.putInt("Facing", s.facing().get3DDataValue());
			t.putBoolean("Held", s.held());
			t.putBoolean("Paid", s.paid());
			list.add(t);
		}
		tag.put("Spans", list);
	}

	private void readSpans(CompoundTag tag) {
		spans.clear();
		for (Tag t : tag.getList("Spans", Tag.TAG_COMPOUND)) {
			CompoundTag c = (CompoundTag) t;
			BlockPos other = c.contains("Off") ? worldPosition.offset(BlockPos.of(c.getLong("Off"))) : BlockPos.of(c.getLong("To"));
			spans.add(new Span(other, Direction.from3DDataValue(c.getInt("Facing")), c.getBoolean("Held"), c.getBoolean("Paid")));
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		writeSpans(tag);
		tag.putLongArray("Spots", spots.stream().mapToLong(BlockPos::asLong).toArray());
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		readSpans(tag);
		spots.clear();
		for (long p : tag.getLongArray("Spots"))
			spots.add(BlockPos.of(p));
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = super.getUpdateTag(registries);
		writeSpans(tag);
		return tag;
	}

	@Override
	public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
		super.handleUpdateTag(tag, registries);
		readSpans(tag);
	}
}
