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
	/**
	 * The far hooks of strings a Schematicannon printed here that aren't printed yet: those strings wait for them (rather
	 * than come down, taking the hook with them). Once the far hook holds the string too it's an ordinary one.
	 */
	private final List<BlockPos> printed = new ArrayList<>();
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
			if (!level.isLoaded(s.other()))
				continue;
			if (level.getBlockEntity(s.other()) instanceof FestoonBlockEntity far && far.partners().contains(worldPosition)) {
				// both hooks hold it: an ordinary string at both now
				if (printed.remove(s.other()))
					setChanged();
				if (far.printed.remove(worldPosition))
					far.setChanged();
				continue;
			}
			// printed, and its far hook not printed yet (nothing there yet): it waits
			if (printed.contains(s.other()) && level.getBlockState(s.other()).isAir())
				continue;
			it.remove();
			printed.remove(s.other());
			changed = true;
			if (s.held() && s.paid())
				net.minecraft.world.level.block.Block.popResource(level, worldPosition,
					new net.minecraft.world.item.ItemStack(com.comedymonkey.createaddons.registry.ModItems.FESTOON_LIGHTS.get(), Festoons.cost(length(s))));
		}
		if (changed) {
			RunLightBlockEntity.runsChanged(level);
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
		writeSpans(tag, false);
	}

	/**
	 * Its strings; {@code print}: as a Schematicannon prints them, waiting for their far hooks, paid for (the holding
	 * hook's cost) at both ends as a hung string is. A schematic file can say anything, so only a string a player could
	 * have hung prints: to another hook in the schematic that holds it back (one end holding it), facing as that hook
	 * faces, in reach.
	 */
	private void writeSpans(CompoundTag tag, boolean print) {
		ListTag list = new ListTag();
		for (Span s : spans) {
			if (print && !printable(s))
				continue;
			CompoundTag t = new CompoundTag();
			// (from this hook: carried off with its far hook by a contraption, the string still joins them)
			t.putLong("Off", s.other().subtract(worldPosition).asLong());
			t.putInt("Facing", s.facing().get3DDataValue());
			t.putBoolean("Held", s.held());
			t.putBoolean("Paid", print || s.paid());
			if (print || printed.contains(s.other()))
				t.putBoolean("Printed", true);
			list.add(t);
		}
		tag.put("Spans", list);
	}

	private boolean printable(Span s) {
		if (level == null || s.other().equals(worldPosition) || length(s) > Festoons.reach())
			return false;
		if (!(level.getBlockEntity(s.other()) instanceof FestoonBlockEntity far)
			|| !(far.getBlockState().getBlock() instanceof FestoonHookBlock) || far.getBlockState().getValue(FestoonHookBlock.FACING) != s.facing())
			return false;
		for (Span back : far.spans)
			if (back.other().equals(worldPosition) && back.held() != s.held() && back.facing() == facing())
				return true;
		return false;
	}

	/** What a Schematicannon prints with it: its brightness and its strings (see FestoonHookBlock.getRequiredItems). */
	@Override
	public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
		super.writeSafe(tag, registries);
		writeSpans(tag, true);
	}

	/** Turned or mirrored with a schematic or a contraption: each string's far hook, and the way it faces, turn with it. */
	@Override
	public void transform(net.minecraft.world.level.block.entity.BlockEntity be, com.simibubi.create.content.contraptions.StructureTransform transform) {
		super.transform(be, transform);
		List<Span> turned = new ArrayList<>();
		List<BlockPos> turnedPrinted = new ArrayList<>();
		for (Span s : spans) {
			BlockPos other = worldPosition.offset(transform.applyWithoutOffset(s.other().subtract(worldPosition)));
			// the far hook faces as Create turns hooks: mirrored, and turned only about an upright axis (a hook faces level)
			Direction facing = transform.mirrorFacing(s.facing());
			if (transform.rotationAxis == null || transform.rotationAxis == Direction.Axis.Y)
				facing = transform.rotateFacing(facing);
			turned.add(new Span(other, facing.getAxis().isHorizontal() ? facing : s.facing(), s.held(), s.paid()));
			if (printed.contains(s.other()))
				turnedPrinted.add(other);
		}
		spans.clear();
		spans.addAll(turned);
		printed.clear();
		printed.addAll(turnedPrinted);
		setChanged();
	}

	/** The Festoon Lights the strings it holds took (what a Schematicannon spends on printing it). */
	public int heldCost() {
		int n = 0;
		for (Span s : spans)
			if (s.held())
				n += Festoons.cost(length(s));
		return n;
	}

	/** Whether a printed string here still waits for its far hook. */
	public boolean waitingForFarHook() {
		return !printed.isEmpty();
	}

	/** Client hook: the strings it holds changed (with Flywheel, its visual lays them out again). */
	public static java.util.function.Consumer<FestoonBlockEntity> clientSpansChanged = be -> {};

	private void readSpans(CompoundTag tag) {
		List<Span> before = level != null && level.isClientSide ? List.copyOf(spans) : null;
		spans.clear();
		printed.clear();
		for (Tag t : tag.getList("Spans", Tag.TAG_COMPOUND)) {
			CompoundTag c = (CompoundTag) t;
			BlockPos other = c.contains("Off") ? worldPosition.offset(BlockPos.of(c.getLong("Off"))) : BlockPos.of(c.getLong("To"));
			spans.add(new Span(other, Direction.from3DDataValue(c.getInt("Facing")), c.getBoolean("Held"), c.getBoolean("Paid")));
			if (c.getBoolean("Printed"))
				printed.add(other);
		}
		if (before != null && !before.equals(spans))
			clientSpansChanged.accept(this);
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
