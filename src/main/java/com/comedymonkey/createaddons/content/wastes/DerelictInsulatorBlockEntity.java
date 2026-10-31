package com.comedymonkey.createaddons.content.wastes;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModWastes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A Derelict Insulator's spans (D61, D64): the sky-cables the city slung from it, each to another insulator or to a
 * Substation's tray, and the snapped ones hanging from it. The city links them ({@link #addSpan}, {@link #addSnapped});
 * players can't make them. Each span is kept by one end (its owner, which draws it); the far end only knows it's
 * there. Lit while its district is fed. When its district wakes while it's loaded, the wake bead runs out of the
 * Substation and hops span to span up to the crown, 2 s a span ({@link #BEAD_TICKS}), each span's line lighting in behind
 * it; the insulator's neck ring lights as the bead reaches its tie ({@link #hop}). Snapped spans never light. Broken, it
 * drops its spans as scrap ({@link #SCRAP_PER_BLOCKS} blocks of span a scrap), and so do the spans of anything that was
 * holding them at the other end.
 */
public class DerelictInsulatorBlockEntity extends CityBlockEntity {
	/** Ticks the wake bead takes along one span (D64: 2 s). */
	public static final int BEAD_TICKS = 40;
	/** Blocks of span per Clockwork Scrap when it comes down (the surface cable gives one in four). */
	public static final int SCRAP_PER_BLOCKS = 4;
	/** How far a removed anchor looks for insulators holding spans to it, in chunks. */
	private static final int ANCHOR_REACH_CHUNKS = 4;

	/**
	 * A span this insulator keeps: to {@code other} (an insulator or a Substation's lower half), or snapped (no other end:
	 * it hangs from here, heading {@code heading} degrees, 0 south, 90 west, as an entity's yaw). {@code stage}: the bead
	 * runs on it from {@code stage} x 2 s after the wake; {@code fromHere}: the bead starts at this end.
	 */
	public record Span(@Nullable BlockPos other, int stage, boolean fromHere, boolean snapped, float heading) {
		CompoundTag save() {
			CompoundTag tag = new CompoundTag();
			if (other != null)
				tag.put("Other", NbtUtils.writeBlockPos(other));
			tag.putInt("Stage", stage);
			tag.putBoolean("FromHere", fromHere);
			tag.putBoolean("Snapped", snapped);
			tag.putFloat("Heading", heading);
			return tag;
		}

		static Span load(CompoundTag tag) {
			return new Span(NbtUtils.readBlockPos(tag, "Other").orElse(null), tag.getInt("Stage"), tag.getBoolean("FromHere"),
				tag.getBoolean("Snapped"), tag.getFloat("Heading"));
		}
	}

	private final List<Span> spans = new ArrayList<>();
	/** Spans from here to the Substation, counted along the chain: the bead reaches this tie hop x 2 s after the wake. */
	private int hop;
	/** It has seen its district awake (so a later wake while loaded is news, and runs the bead). */
	private boolean sawAwake;
	/** Looks: its district is fed (its spans' lines lit). */
	private boolean fed;
	/** Looks: the game time its district woke while it was loaded (the bead's start); NEVER if not. */
	private long wakeAt = SubstationBlockEntity.NEVER;
	/** The tick its ring lights (the bead's arrival); MAX while not waiting. */
	private long ringAt = Long.MAX_VALUE;

	public DerelictInsulatorBlockEntity(BlockPos pos, BlockState state) {
		super(ModWastes.DERELICT_INSULATOR_BE.get(), pos, state);
	}

	public List<Span> spans() {
		return spans;
	}

	public int hop() {
		return hop;
	}

	public boolean fed() {
		return fed;
	}

	public long wakeAt() {
		return wakeAt;
	}

	// ---------------------------------------------------------------- the city's builder

	/** Hangs a span from here to {@code other} (an insulator or a Substation's lower half); the bead runs it at {@code stage}. */
	public void addSpan(BlockPos other, int stage, boolean beadFromHere) {
		spans.add(new Span(other.immutable(), stage, beadFromHere, false, 0));
		setChanged();
	}

	/** A snapped span hanging from here (D64: about 3 blocks, a curl, bare copper at the break; never lit). */
	public void addSnapped(float heading) {
		spans.add(new Span(null, 0, true, true, heading));
		setChanged();
	}

	/** Its place in its district's chain (spans from the Substation to here). */
	public void setHop(int hop) {
		this.hop = hop;
		setChanged();
	}

	// ---------------------------------------------------------------- where spans tie on

	/** Where a span ties on at {@code pos}: an insulator's tie knob, or the Substation's tray insulator; null if neither is there. */
	@Nullable
	public static Vec3 tie(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof DerelictInsulatorBlock)
			return Vec3.atLowerCornerOf(pos).add(DerelictInsulatorBlock.tie(state));
		if (state.getBlock() instanceof SubstationBlock && state.getValue(SubstationBlock.HALF) == DoubleBlockHalf.LOWER)
			return Vec3.atLowerCornerOf(pos).add(substationTie(state.getValue(SubstationBlock.FACING)));
		return null;
	}

	/**
	 * The Substation's tray tie (the east insulator of the two on its cable tray, in the north-facing model at 11, 34, 8
	 * px: the insulator preview's), turned with its facing as its blockstate turns the model.
	 */
	public static Vec3 substationTie(Direction facing) {
		double dx = 3 / 16.0, dz = 0;
		int turns = switch (facing) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
		for (int i = 0; i < turns; i++) {
			double t = dx;
			dx = -dz;
			dz = t;
		}
		return new Vec3(0.5 + dx, 34 / 16.0, 0.5 + dz);
	}

	public AABB renderBox() {
		AABB box = new AABB(worldPosition).inflate(4);
		if (level != null)
			for (Span s : spans)
				if (s.other() != null)
					box = box.minmax(new AABB(s.other()).inflate(2));
		return box;
	}

	// ---------------------------------------------------------------- the city's word

	@Override
	protected void recheck(boolean live, @Nullable BlockPos from) {
		CityRecord record = record();
		if (level == null || record == null)
			return;
		long now = level.getGameTime();
		boolean awake = link.bound() && record.awake(link.origin(), link.district());
		boolean want = link.bound() && record.fed(link.origin(), link.district(), now);
		boolean changed = false;
		if (awake && !sawAwake) {
			sawAwake = true;
			setChanged();
			// woke while it watched: the bead runs from the Substation now, reaching this tie hop spans later
			if (live) {
				wakeAt = now;
				ringAt = now + (long) hop * BEAD_TICKS;
				changed = true;
			}
		}
		if (want != fed) {
			fed = want;
			changed = true;
		}
		if (!want) {
			ringAt = Long.MAX_VALUE;
			showRing(false);
		} else if (ringAt != Long.MAX_VALUE && now < ringAt) {
			// the bead is still on its way to this tie
			schedule((int) (ringAt - now));
			showRing(false);
		} else {
			ringAt = Long.MAX_VALUE;
			showRing(true);
		}
		if (changed)
			sync();
	}

	private void showRing(boolean lit) {
		if (level == null || isRemoved())
			return;
		BlockState state = level.getBlockState(worldPosition);
		if (state.getBlock() instanceof DerelictInsulatorBlock && state.getValue(DerelictInsulatorBlock.LIT) != lit)
			level.setBlock(worldPosition, state.setValue(DerelictInsulatorBlock.LIT, lit), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
	}

	// ---------------------------------------------------------------- coming down

	/** Blocks of span between two ties, as scrap. */
	static int scrapFor(double length) {
		return Math.max(1, (int) Math.round(length / SCRAP_PER_BLOCKS));
	}

	/** It's being broken: its spans come down as scrap where it stood, and the far ends forget them. */
	void removed() {
		if (!(level instanceof ServerLevel server))
			return;
		int scrap = 0;
		Vec3 here = Vec3.atCenterOf(worldPosition);
		for (Span s : spans)
			scrap += s.snapped() ? 1 : scrapFor(here.distanceTo(Vec3.atCenterOf(s.other())));
		spans.clear();
		drop(server, worldPosition, scrap);
		anchorRemoved(server, worldPosition);
	}

	/**
	 * An anchor at {@code pos} (an insulator or a Substation) came down: every loaded insulator near it that kept a span to
	 * it lets that span fall, as scrap where the anchor stood.
	 */
	public static void anchorRemoved(ServerLevel level, BlockPos pos) {
		ChunkPos c = new ChunkPos(pos);
		int scrap = 0;
		for (int dx = -ANCHOR_REACH_CHUNKS; dx <= ANCHOR_REACH_CHUNKS; dx++)
			for (int dz = -ANCHOR_REACH_CHUNKS; dz <= ANCHOR_REACH_CHUNKS; dz++) {
				var chunk = level.getChunkSource().getChunkNow(c.x + dx, c.z + dz);
				if (chunk == null)
					continue;
				for (BlockEntity be : new ArrayList<>(chunk.getBlockEntities().values()))
					if (be instanceof DerelictInsulatorBlockEntity ins && !ins.worldPosition.equals(pos)) {
						boolean any = false;
						for (var it = ins.spans.iterator(); it.hasNext();) {
							Span s = it.next();
							if (pos.equals(s.other())) {
								scrap += scrapFor(Vec3.atCenterOf(ins.worldPosition).distanceTo(Vec3.atCenterOf(pos)));
								it.remove();
								any = true;
							}
						}
						if (any)
							ins.sync();
					}
			}
		drop(level, pos, scrap);
	}

	private static void drop(ServerLevel level, BlockPos pos, int scrap) {
		while (scrap > 0) {
			int n = Math.min(64, scrap);
			Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(ModWastes.CLOCKWORK_SCRAP.get(), n));
			scrap -= n;
		}
	}

	// ---------------------------------------------------------------- saving and syncing

	@Override
	protected void writeState(CompoundTag tag, HolderLookup.Provider registries) {
		tag.putInt("Hop", hop);
		tag.putBoolean("SawAwake", sawAwake);
		if (ringAt != Long.MAX_VALUE)
			tag.putLong("RingAt", ringAt);
	}

	@Override
	protected void readState(CompoundTag tag, HolderLookup.Provider registries) {
		hop = tag.getInt("Hop");
		sawAwake = tag.getBoolean("SawAwake");
		ringAt = tag.contains("RingAt") ? tag.getLong("RingAt") : Long.MAX_VALUE;
	}

	@Override
	protected void writeLooks(CompoundTag tag) {
		ListTag list = new ListTag();
		for (Span s : spans)
			list.add(s.save());
		tag.put("Spans", list);
		tag.putBoolean("Fed", fed);
		tag.putLong("WakeAt", wakeAt);
	}

	@Override
	protected void readLooks(CompoundTag tag) {
		spans.clear();
		for (Tag t : tag.getList("Spans", Tag.TAG_COMPOUND))
			spans.add(Span.load((CompoundTag) t));
		fed = tag.getBoolean("Fed");
		wakeAt = tag.contains("WakeAt") ? tag.getLong("WakeAt") : SubstationBlockEntity.NEVER;
	}
}
