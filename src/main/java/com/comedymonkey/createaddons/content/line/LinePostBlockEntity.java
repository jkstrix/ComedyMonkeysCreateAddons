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
public class LinePostBlockEntity extends BlockEntity {
	/** Spans an insulator can hold. */
	public static final int SPANS_PER_INSULATOR = 2;
	/** Blocks of span one Power Line item hangs. */
	public static final int BLOCKS_PER_WIRE = 4;

	private final List<Span> spans = new ArrayList<>();

	public LinePostBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.LINE_POST.get(), pos, state);
	}

	public List<Span> spans() {
		return spans;
	}

	public List<Span> spansAt(int insulator) {
		List<Span> out = new ArrayList<>();
		for (Span s : spans)
			if (s.from() == insulator)
				out.add(s);
		return out;
	}

	/** The tier of the cable tied to an insulator, or null if none is. */
	@Nullable
	public LineTier tierAt(int insulator) {
		for (Span s : spans)
			if (s.from() == insulator)
				return s.tier();
		return null;
	}

	/** The colour of cable on an insulator (null: plain, or none tied yet; see {@link #tierAt}). */
	@Nullable
	public net.minecraft.world.item.DyeColor dyeAt(int insulator) {
		for (Span s : spans)
			if (s.from() == insulator)
				return s.dye();
		return null;
	}

	/** Where a span ties onto an insulator, in the world. */
	public Vec3 tie(int insulator) {
		return LinePostBlock.tie(getBlockState(), worldPosition, insulator);
	}

	public void addSpan(Span span) {
		spans.add(span);
		changed();
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
		changed();
		boolean farHeld = false;
		BlockPos farPos = worldPosition.offset(s.offset());
		if (level != null && level.isLoaded(farPos) && level.getBlockEntity(farPos) instanceof LinePostBlockEntity far) {
			farHeld = far.spans.remove(s.reversed());
			far.changed();
		}
		// a span pays back once: here when the far end still held it (and now doesn't), or when this end owns it (the far
		// end, gone or unloaded, then drops it without paying when it finds out)
		return farHeld || s.owner() ? wireFor(s) : ItemStack.EMPTY;
	}

	public double length(Span s) {
		if (level == null)
			return 0;
		BlockState farState = level.getBlockState(worldPosition.offset(s.offset()));
		return tie(s.from()).distanceTo(LinePostBlock.tie(farState, worldPosition.offset(s.offset()), s.to()));
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
	 * Spans whose far post is gone (moved away on a ship, say) come down. Only the owning end drops their wire, so a
	 * span pays back once however its two ends find out.
	 */
	public void dropBrokenSpans() {
		if (level == null || level.isClientSide)
			return;
		for (Span s : new ArrayList<>(spans)) {
			BlockPos far = worldPosition.offset(s.offset());
			if (!level.isLoaded(far))
				continue;
			if (!(level.getBlockEntity(far) instanceof LinePostBlockEntity post) || !post.spans.contains(s.reversed())) {
				spans.remove(s);
				if (s.owner())
					Block.popResource(level, worldPosition, wireFor(s));
				changed();
			}
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
		ListTag list = new ListTag();
		for (Span s : spans)
			list.add(s.save());
		tag.put("Spans", list);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
		super.loadAdditional(tag, registries);
		spans.clear();
		for (Tag t : tag.getList("Spans", Tag.TAG_COMPOUND))
			spans.add(Span.load((CompoundTag) t));
		if (level != null && level.isClientSide)
			clientSpansChanged.accept(this);
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
			if (s.owner())
				box = box.minmax(new AABB(worldPosition.offset(s.offset())).inflate(1));
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
