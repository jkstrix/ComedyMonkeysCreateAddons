package com.comedymonkey.createaddons.content.line;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A Line Post block: on the head of a pole, the spans hung from its insulators (the top one, 0, and one on each
 * branch, 1 to 4 for north, east, south and west in the post's own frame). Each insulator is its own connection.
 */
public class LinePostBlockEntity extends BlockEntity implements SpanHolder, com.simibubi.create.api.schematic.nbt.PartialSafeNBT,
	com.simibubi.create.api.contraption.transformable.TransformableBlockEntity {
	/** Spans an insulator can hold. */
	public static final int SPANS_PER_INSULATOR = 2;
	/** Blocks of span one Power Line item hangs. */
	public static final int BLOCKS_PER_WIRE = 4;

	private final List<Span> spans = new ArrayList<>();
	/**
	 * Spans a Schematicannon printed here whose far post isn't printed yet: they wait for it (rather than come down as a
	 * span to nothing would). Once the far post holds them too they're ordinary spans.
	 */
	private final List<Span> printed = new ArrayList<>();

	public LinePostBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.LINE_POST.get(), pos, state);
	}

	@Override
	public List<Span> spans() {
		return spans;
	}

	@Override
	public void confirmPrinted(Span s) {
		if (printed.remove(s))
			setChanged();
	}

	/** Whether a printed span here still waits for its far post (see {@link #printed}). */
	public boolean waitingForFarPost() {
		return !printed.isEmpty();
	}

	@Override
	public BlockPos holderPos() {
		return worldPosition;
	}

	@Override
	public Vec3 tie(int insulator) {
		return LinePostBlock.tie(getBlockState(), worldPosition, insulator);
	}

	@Override
	public boolean hasInsulator(int insulator) {
		return LinePostBlock.hasInsulator(getBlockState(), insulator);
	}

	@Override
	public int nearestInsulator(Vec3 at) {
		return LinePostBlock.nearestInsulator(getBlockState(), worldPosition, at);
	}

	@Override
	public void addSpan(Span span) {
		spans.add(span);
		changed();
	}

	@Override
	public boolean removeSpan(Span span) {
		boolean held = spans.remove(span);
		printed.remove(span);
		if (held)
			changed();
		return held;
	}

	/** Takes down every span on an insulator (here and at its far end); returns the Power Lines they hung. */
	public List<ItemStack> takeDown(int insulator) {
		List<ItemStack> refund = new ArrayList<>();
		for (Span s : spansAt(insulator))
			refund.add(takeDown(s));
		return refund;
	}

	/** Takes down every span (as the post goes); returns the Power Lines they hung. */
	public List<ItemStack> takeDownAll() {
		List<ItemStack> refund = new ArrayList<>();
		for (Span s : new ArrayList<>(spans))
			refund.add(takeDown(s));
		return refund;
	}

	private ItemStack takeDown(Span s) {
		spans.remove(s);
		printed.remove(s);
		changed();
		boolean farHeld = false;
		BlockPos farPos = worldPosition.offset(s.offset());
		SpanHolder far = level != null && level.isLoaded(farPos) ? SpanHolder.at(level, farPos) : null;
		if (far != null)
			farHeld = far.removeSpan(s.reversed());
		// a span pays back once: here when the far end still held it (and now doesn't), or when this end owns it (the far
		// end, gone or unloaded, then drops it without paying when it finds out)
		return farHeld || s.owner() ? wireFor(s) : ItemStack.EMPTY;
	}

	public double length(Span s) {
		if (level == null)
			return 0;
		Vec3 far = SpanHolder.farTie(level, worldPosition.offset(s.offset()), s.to());
		return far == null ? 0 : tie(s.from()).distanceTo(far);
	}

	/** The Power Lines a span of this length takes: one for every four blocks, rounded up. */
	public static int wireCount(double length) {
		return Math.max(1, Mth.ceil(length / BLOCKS_PER_WIRE));
	}

	/** The Power Lines a span hung with: given back when it comes down. */
	public static ItemStack wireFor(Span s) {
		ItemStack stack = new ItemStack(s.tier() == LineTier.BRASS ? com.comedymonkey.createaddons.registry.ModItems.BRASS_POWER_LINE.get()
			: com.comedymonkey.createaddons.registry.ModItems.COPPER_POWER_LINE.get(), Math.max(1, s.wires()));
		if (s.dye() != null)
			stack.set(com.comedymonkey.createaddons.registry.ModDataComponents.LINE_DYE.get(), s.dye());
		return stack;
	}

	/**
	 * Spans whose far end is gone (a post moved away on a ship, say, or a battery that came apart) come down. Only the
	 * owning end drops their wire, so a span pays back once however its two ends find out.
	 */
	@Override
	public void dropBrokenSpans() {
		if (level == null || level.isClientSide)
			return;
		for (Span s : new ArrayList<>(spans)) {
			BlockPos far = worldPosition.offset(s.offset());
			if (!level.isLoaded(far))
				continue;
			SpanHolder holder = SpanHolder.at(level, far);
			// printed, and its far end not printed yet (nothing there, or a post or battery not finished yet): it waits
			if (holder == null && printed.contains(s) && SpanHolder.mayStillBePrinted(level, far))
				continue;
			if (holder != null && holder.spans().contains(s.reversed())) {
				// both ends hold it: it's an ordinary span at both now
				confirmPrinted(s);
				holder.confirmPrinted(s.reversed());
				continue;
			}
			spans.remove(s);
			printed.remove(s);
			if (s.owner())
				Block.popResource(level, worldPosition, wireFor(s));
			changed();
		}
	}

	private void changed() {
		setChanged();
		if (level != null && !level.isClientSide) {
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
			if (level instanceof ServerLevel server)
				PowerLines.of(server).changed(worldPosition);
		}
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (level != null && level.isClientSide)
			clientSpansChanged.accept(this);
		if (level instanceof ServerLevel server) {
			PowerLines.of(server).loaded(worldPosition);
			// a tick later, once the far posts have loaded too
			server.getServer().execute(this::dropBrokenSpans);
		}
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.saveAdditional(tag, registries);
		tag.put("Spans", saveSpans(spans, false));
	}

	private ListTag saveSpans(List<Span> list, boolean allPrinted) {
		ListTag out = new ListTag();
		for (Span s : list) {
			CompoundTag t = s.save();
			if (allPrinted || printed.contains(s))
				t.putBoolean("Printed", true);
			out.add(t);
		}
		return out;
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		// printed over a post that's already here (a Schematicannon replacing blocks): its own spans come down first,
		// paid back as if it were broken
		if (tag.getBoolean("Print") && level instanceof ServerLevel && !spans.isEmpty())
			for (ItemStack wire : takeDownAll())
				Block.popResource(level, worldPosition, wire);
		spans.clear();
		printed.clear();
		for (Tag t : tag.getList("Spans", Tag.TAG_COMPOUND)) {
			Span s = Span.load((CompoundTag) t);
			spans.add(s);
			if (((CompoundTag) t).getBoolean("Printed"))
				printed.add(s);
		}
		if (level != null && level.isClientSide)
			clientSpansChanged.accept(this);
		// printed (or set by a command) on a post already in the world: its network takes the spans in
		if (level instanceof ServerLevel server)
			PowerLines.of(server).changed(worldPosition);
	}

	// ---------- schematics and contraptions ----------

	/**
	 * What a Schematicannon prints with it: its spans to other posts and to Industrial Batteries (paid for by the end that
	 * draws them: see LinePostBlock.getRequiredItems), each marked to wait for its far end.
	 */
	@Override
	public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
		tag.putBoolean("Print", true);
		tag.put("Spans", saveSpans(level == null ? List.of() : SpanPrinting.printable(level, worldPosition, this), true));
	}

	/**
	 * Turned or mirrored with a schematic or a contraption: each span's way to its far post turns with it, and so do the
	 * branch insulators of the posts (an upright post's branches are on world sides, which turn).
	 */
	@Override
	public void transform(BlockEntity be, com.simibubi.create.content.contraptions.StructureTransform transform) {
		Direction facing = getBlockState().getBlock() instanceof LinePostBlock ? getBlockState().getValue(LinePostBlock.FACING) : Direction.UP;
		List<Span> turned = new ArrayList<>(), turnedPrinted = new ArrayList<>();
		for (Span s : spans) {
			BlockPos offset = transform.applyWithoutOffset(s.offset());
			// the far post's facing, where it's known (it's been turned already, or will be the same way as this one); an
			// Industrial Battery's insulators go round with its front, and mirrored, its left ones are its right ones
			BlockState farState = SpanHolder.stateIfLoaded(level, worldPosition.offset(offset));
			int to;
			if (farState != null && farState.getBlock() instanceof com.comedymonkey.createaddons.content.electric.BatteryBlock)
				to = transform.mirror != null && transform.mirror != net.minecraft.world.level.block.Mirror.NONE ? s.to() ^ 1 : s.to();
			else
				to = turnInsulator(s.to(), farState != null && farState.getBlock() instanceof LinePostBlock ? farState.getValue(LinePostBlock.FACING) : facing, transform);
			Span t = new Span(turnInsulator(s.from(), facing, transform), offset, to, s.tier(), s.dye(), s.owner(), s.wires());
			turned.add(t);
			if (printed.contains(s))
				turnedPrinted.add(t);
		}
		spans.clear();
		spans.addAll(turned);
		printed.clear();
		printed.addAll(turnedPrinted);
		setChanged();
	}

	/**
	 * A post's insulator number after the turn, for a post facing {@code after} once turned (0, the top, stays 0): the
	 * branch it's on goes where the turn takes the post's branches (the block's own rotate and mirror, as Create turns
	 * it), so a span stays on its crossarm whichever way the post stands.
	 */
	public static int turnInsulator(int insulator, Direction after, com.simibubi.create.content.contraptions.StructureTransform transform) {
		if (insulator < 1 || insulator > 4)
			return insulator;
		BlockState post = com.comedymonkey.createaddons.registry.ModBlocks.LINE_POST.get().defaultBlockState();
		for (net.minecraft.world.level.block.state.properties.BooleanProperty b : LinePostBlock.BRANCHES.values())
			post = post.setValue(b, false);
		for (Direction before : Direction.values()) {
			BlockState was = post.setValue(LinePostBlock.FACING, before);
			if (transform.apply(was).getValue(LinePostBlock.FACING) != after)
				continue;
			BlockState turned = transform.apply(was.setValue(LinePostBlock.BRANCHES.get(branchSide(insulator)), true));
			for (int i = 1; i <= 4; i++)
				if (turned.getValue(LinePostBlock.BRANCHES.get(branchSide(i))))
					return i;
		}
		return insulator;
	}

	/** The insulator number of a branch side in the post's own frame (see {@link #branchSide}). */
	static int insulatorOn(Direction local) {
		return switch (local) {
			case NORTH -> 1;
			case EAST -> 2;
			case SOUTH -> 3;
			default -> 4;
		};
	}

	/** Client: unloading, so the spans hung to it from other posts go too (as the renderer draws them only to a post that's there). */
	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		if (level != null && level.isClientSide)
			clientSpansChanged.accept(this);
	}

	/** Client: its spans changed, or it came into view (the posts its spans hang to lay them out again); set by the client setup. */
	public static java.util.function.Consumer<LinePostBlockEntity> clientSpansChanged = be -> {};

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		saveAdditional(tag, registries);
		return tag;
	}

	@Override
	public Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/** Big enough to hold every span this post draws, so they don't vanish when the post itself is off screen. */
	public AABB renderBox() {
		AABB box = new AABB(worldPosition).inflate(1);
		for (Span s : spans)
			if (s.owner()) {
				box = box.minmax(new AABB(worldPosition.offset(s.offset())).inflate(1));
				// a battery's insulators stand well above its foot
				Vec3 far = level == null ? null : SpanHolder.farTie(level, worldPosition.offset(s.offset()), s.to());
				if (far != null)
					box = box.minmax(new AABB(far, far).inflate(1));
			}
		return box;
	}

	/** The branch sides in this post's own frame, by insulator number. */
	public static Direction branchSide(int insulator) {
		return switch (insulator) {
			case 1 -> Direction.NORTH;
			case 2 -> Direction.EAST;
			case 3 -> Direction.SOUTH;
			default -> Direction.WEST;
		};
	}
}
