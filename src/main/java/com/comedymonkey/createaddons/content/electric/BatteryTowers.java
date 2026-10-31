package com.comedymonkey.createaddons.content.electric;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.advancement.ModAdvancements;
import com.comedymonkey.createaddons.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Forming and coming apart: Batteries filling a 3×3 footprint 3 to 9 high are one Industrial Battery.
 * <ul>
 * <li>A column forms from the bottom: 9 at most, and what's left over above forms its own tower if it's 3 or more.</li>
 * <li>Every full 3×3 forms; where full 3×3s overlap, the one at the lowest corner (west, then north) forms.</li>
 * <li>It faces the way of the Battery whose placing completed it; towers that grow or re-form keep their front.</li>
 * <li>Towers side by side stay apart. Stacked on one footprint they're worked out together, as one column.</li>
 * <li>Breaking a block re-forms the tallest run of 3 or more full layers left (the lower if two tie); the rest are plain.</li>
 * </ul>
 * Charge is never made or lost: forming sums the Batteries' shares, coming apart gives each block its share. The spans
 * on a tower's insulators stay up if it re-forms with the same master (the same bottom-front-middle block), and come down
 * otherwise, their Power Lines dropped there.
 */
public final class BatteryTowers {
	public static final int MIN = 3, MAX = 9;
	/** Block events the master's client side shows: a lightning strike (its param the top block struck), and forming. */
	public static final int EVENT_STRIKE = 1, EVENT_FORM = 2;

	public enum Sound {
		HUM("hum"), FORM("form"), UNFORM("unform"), STRIKE("strike");

		final String id;

		Sound(String id) {
			this.id = id;
		}
	}

	/** Spans taken off towers that came apart in this change, by master: put back on one that re-forms there, else taken down. */
	private static final java.util.Map<BlockPos, List<com.comedymonkey.createaddons.content.line.Span>> LOOSE = new java.util.LinkedHashMap<>();

	private BatteryTowers() {}

	/** The end of a change: spans no tower took back come down. */
	private static void finish(ServerLevel level) {
		for (var e : LOOSE.entrySet()) {
			BlockPos master = e.getKey();
			for (com.comedymonkey.createaddons.content.line.Span s : e.getValue()) {
				com.comedymonkey.createaddons.content.line.SpanHolder far =
					level.isLoaded(master.offset(s.offset())) ? com.comedymonkey.createaddons.content.line.SpanHolder.at(level, master.offset(s.offset())) : null;
				// pays back once: here if the post still held it; a post that isn't loaded finds out itself and pays
				if (far != null && far.removeSpan(s.reversed()) || s.owner())
					net.minecraft.world.level.block.Block.popResource(level, master, com.comedymonkey.createaddons.content.line.LinePostBlockEntity.wireFor(s));
			}
		}
		LOOSE.clear();
	}

	/** The middle of the tower's bottom layer, behind the master. */
	public static BlockPos center(BlockPos master, Direction facing) {
		return master.relative(facing.getOpposite());
	}

	/** The master of a tower whose bottom-layer centre is {@code center}: the middle of its front row. */
	static BlockPos masterOf(BlockPos center, Direction facing) {
		return center.relative(facing);
	}

	/** Every block of a tower, layer by layer. */
	public static List<BlockPos> blocks(BlockPos master, int height, Direction facing) {
		BlockPos c = center(master, facing);
		List<BlockPos> out = new ArrayList<>(height * 9);
		for (int y = 0; y < height; y++)
			for (int dz = -1; dz <= 1; dz++)
				for (int dx = -1; dx <= 1; dx++)
					out.add(c.offset(dx, y, dz));
		return out;
	}

	/** A tower: its footprint's west-north corner, bottom layer and height, and its front. */
	record Tower(int x0, int z0, int bottom, int height, Direction facing) {
		BlockPos center() {
			return new BlockPos(x0 + 1, bottom, z0 + 1);
		}

		BlockPos master() {
			return masterOf(center(), facing);
		}

		int top() {
			return bottom + height - 1;
		}
	}

	/** The tower a formed block is part of, as it sees it. */
	@Nullable
	static Tower towerOf(BatteryBlockEntity be) {
		if (be.master == null)
			return null;
		BlockPos c = center(be.master, be.towerFacing);
		return new Tower(c.getX() - 1, c.getZ() - 1, c.getY(), be.height, be.towerFacing);
	}

	// ---------- placing ----------

	/**
	 * A tick after a Battery is placed: a formed block put down from somewhere else (a contraption, Carry On) whose
	 * tower isn't here is plain again; then whatever it completes forms, with its front.
	 */
	static void placed(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof BatteryBlock) || !(level.getBlockEntity(pos) instanceof BatteryBlockEntity be))
			return;
		try {
			if (state.getValue(BatteryBlock.FORMED)) {
				// its tower stands as it was: nothing to do (a check after loading, or after something went missing)
				if (intact(level, be))
					return;
				// what's left of its tower is plain again, and so is it. Moved here (a contraption, Carry On), what was
				// left came apart when it went, and a tower standing there now may be a new one: only it goes plain
				boolean moved = be.formedAt != null && !pos.equals(be.formedAt);
				Tower t = towerOf(be);
				if (t != null && !moved)
					unform(level, t, null);
				if (be.master != null || level.getBlockState(pos).getValue(BatteryBlock.FORMED)) {
					be.dissolveStore();
					be.setTower(null, 0, state.getValue(BatteryBlock.FACING));
					be.forgetSpans();
					level.setBlock(pos, BatteryBlock.joins(level.getBlockState(pos).setValue(BatteryBlock.FORMED, false), level, pos), 3);
				}
				state = level.getBlockState(pos);
			}
			Direction facing = state.getValue(BatteryBlock.FACING);
			// each footprint it could complete, lowest corner first; once one forms it's taken
			for (int x0 = pos.getX() - 2; x0 <= pos.getX(); x0++)
				for (int z0 = pos.getZ() - 2; z0 <= pos.getZ(); z0++)
					if (column(level, x0, z0, pos.getY(), facing, null))
						return;
		} finally {
			finish(level);
		}
	}

	/**
	 * A formed block's tower stands as it formed: every block of it there (as far as it's loaded: it never loads a
	 * chunk to look), formed in this tower, where it formed (not moved here by a contraption).
	 */
	private static boolean intact(ServerLevel level, BatteryBlockEntity be) {
		// (formed before blocks noted where: taken as not moved)
		if (be.master == null || be.height < MIN || be.formedAt != null && !be.getBlockPos().equals(be.formedAt))
			return false;
		return stands(level, towerOf(be));
	}

	/** Every block of a tower that's loaded is formed in it, where it formed (it never loads a chunk to look). */
	private static boolean stands(ServerLevel level, Tower t) {
		for (BlockPos p : blocks(t.master(), t.height(), t.facing())) {
			if (!level.isLoaded(p))
				continue;
			if (!(level.getBlockEntity(p) instanceof BatteryBlockEntity o) || !BatteryBlock.isFormed(o.getBlockState()) || !in(o, t, p))
				return false;
		}
		return true;
	}

	/**
	 * A block at p is part of tower t: it remembers that very tower (master, height and front), and formed at p. A block
	 * remembering another tower on the same master (one that stood there before, or that it was carried off from) isn't.
	 */
	private static boolean in(BatteryBlockEntity be, Tower t, BlockPos p) {
		return t.equals(towerOf(be)) && (be.formedAt == null || p.equals(be.formedAt));
	}

	/** A layer of the footprint every block of which is a Battery, plain or in a tower on this same footprint. */
	private static boolean fillable(ServerLevel level, int x0, int z0, int y, @Nullable BlockPos without) {
		if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight())
			return false;
		for (int dz = 0; dz < 3; dz++)
			for (int dx = 0; dx < 3; dx++) {
				BlockPos p = new BlockPos(x0 + dx, y, z0 + dz);
				if (p.equals(without) || !level.isLoaded(p))
					return false;
				BlockState s = level.getBlockState(p);
				if (!(s.getBlock() instanceof BatteryBlock) || !(level.getBlockEntity(p) instanceof BatteryBlockEntity be) || be.isRemoved())
					return false;
				if (s.getValue(BatteryBlock.FORMED)) {
					Tower t = towerOf(be);
					if (t == null || t.x0() != x0 || t.z0() != z0)
						return false;
				}
			}
		return true;
	}

	/**
	 * The column of full layers on a footprint through layer y, worked out as towers from the bottom: those that are
	 * already right stay, the rest come apart and the new ones form. Returns whether anything formed.
	 */
	private static boolean column(ServerLevel level, int x0, int z0, int y, Direction facing, @Nullable BlockPos without) {
		if (!fillable(level, x0, z0, y, without))
			return false;
		int bottom = y, top = y;
		while (fillable(level, x0, z0, bottom - 1, without))
			bottom--;
		while (fillable(level, x0, z0, top + 1, without))
			top++;
		return arrange(level, x0, z0, bottom, top, facing);
	}

	/** Layers bottom to top of one footprint, as towers of 9 from the bottom (and the rest, if 3 or more). */
	private static boolean arrange(ServerLevel level, int x0, int z0, int bottom, int top, Direction facing) {
		Set<Tower> old = towersIn(level, x0, z0, bottom, top);
		List<Tower> plan = new ArrayList<>();
		for (int b = bottom; top - b + 1 >= MIN;) {
			int h = Math.min(MAX, top - b + 1);
			// a tower that grows or re-forms keeps the front it had
			Direction f = facing;
			for (Tower t : old)
				if (t.bottom() <= b + h - 1 && t.top() >= b) {
					f = t.facing();
					break;
				}
			plan.add(new Tower(x0, z0, b, h, f));
			b += h;
		}
		boolean formed = false;
		for (Tower t : old)
			if (!plan.contains(t))
				unform(level, t, null);
		// (one a block remembers that doesn't stand as it was, a block of it plain or remembering another, forms again)
		for (Tower t : plan)
			if (!old.contains(t) || !stands(level, t)) {
				form(level, t);
				formed = true;
			}
		return formed;
	}

	/** The towers standing on this footprint between two layers. */
	private static Set<Tower> towersIn(ServerLevel level, int x0, int z0, int bottom, int top) {
		Set<Tower> out = new LinkedHashSet<>();
		for (int y = bottom; y <= top; y++)
			if (level.isLoaded(new BlockPos(x0 + 1, y, z0 + 1)) && level.getBlockEntity(new BlockPos(x0 + 1, y, z0 + 1)) instanceof BatteryBlockEntity be
				&& be.isFormed()) {
				Tower t = towerOf(be);
				if (t != null)
					out.add(t);
			}
		return out;
	}

	private static void form(ServerLevel level, Tower t) {
		BlockPos master = t.master();
		List<BlockPos> blocks = blocks(master, t.height(), t.facing());
		// every Battery keeps its share of the bank it was in, then they're all one store
		for (BlockPos p : blocks)
			if (level.getBlockEntity(p) instanceof BatteryBlockEntity be)
				be.dissolveStore();
		for (BlockPos p : blocks)
			if (level.getBlockEntity(p) instanceof BatteryBlockEntity be) {
				be.setTower(master, t.height(), t.facing());
				BlockState s = level.getBlockState(p);
				level.setBlock(p, BatteryBlock.joins(s.setValue(BatteryBlock.FORMED, true), level, p), 3);
				be.sendData();
			}
		List<com.comedymonkey.createaddons.content.line.Span> back = LOOSE.remove(master);
		if (back != null && level.getBlockEntity(master) instanceof BatteryBlockEntity m)
			m.giveSpans(back);
		// a tower on this footprint that came apart with another master (one layer up or down): its cables move over
		for (BlockPos old : new ArrayList<>(LOOSE.keySet()))
			if (old.getX() == master.getX() && old.getZ() == master.getZ() && old.getY() >= t.bottom() - MAX && old.getY() <= t.top()
				&& level.getBlockEntity(master) instanceof BatteryBlockEntity m)
				moveSpans(level, old, m, LOOSE.remove(old));
		com.comedymonkey.createaddons.content.line.PowerLines.of(level).changed(master);
		level.blockEvent(master, level.getBlockState(master).getBlock(), EVENT_FORM, 0);
		play(level, Sound.FORM, middle(t));
		ModAdvancements.awardNearby(level, BlockPos.containing(middle(t)), ModAdvancements.INDUSTRIAL_BATTERY_FORMED, ItemStack.EMPTY, t.height());
	}

	/** Spans that were on a master at {@code from} go on {@code to}, the posts at their far ends told. */
	private static void moveSpans(ServerLevel level, BlockPos from, BatteryBlockEntity to, List<com.comedymonkey.createaddons.content.line.Span> spans) {
		List<com.comedymonkey.createaddons.content.line.Span> moved = new ArrayList<>();
		for (com.comedymonkey.createaddons.content.line.Span s : spans) {
			BlockPos far = from.offset(s.offset());
			com.comedymonkey.createaddons.content.line.SpanHolder post =
				level.isLoaded(far) ? com.comedymonkey.createaddons.content.line.SpanHolder.at(level, far) : null;
			if (post == null || !post.removeSpan(s.reversed())) {
				// the post isn't loaded or doesn't hold it now: it comes down (the post's end pays)
				LOOSE.computeIfAbsent(from, k -> new ArrayList<>()).add(s);
				continue;
			}
			com.comedymonkey.createaddons.content.line.Span m = new com.comedymonkey.createaddons.content.line.Span(s.from(), far.subtract(to.getBlockPos()),
				s.to(), s.tier(), s.dye(), s.owner(), s.wires());
			post.addSpan(m.reversed());
			moved.add(m);
		}
		if (!moved.isEmpty())
			to.giveSpans(moved);
	}

	/**
	 * Back to plain Batteries, each with its share ({@code going}: a block that's going, already given its share; its
	 * block entity may be out of the world already, and nothing here looks for one at its place, which would make a new one).
	 */
	private static void unform(ServerLevel level, Tower t, @Nullable BatteryBlockEntity going) {
		BlockPos master = t.master(), without = going == null ? null : going.getBlockPos();
		List<BlockPos> blocks = blocks(master, t.height(), t.facing());
		net.minecraft.world.level.block.entity.BlockEntity atMaster = master.equals(without) ? going
			: level.isLoaded(master) ? level.getBlockEntity(master) : null;
		// (only this very tower's blocks: one on the same master remembering another tower is left as it is)
		if (atMaster instanceof BatteryBlockEntity m && in(m, t, master)) {
			List<com.comedymonkey.createaddons.content.line.Span> off = m.takeSpans();
			if (!off.isEmpty())
				LOOSE.computeIfAbsent(master, k -> new ArrayList<>()).addAll(off);
		}
		com.comedymonkey.createaddons.content.line.PowerLines.of(level).changed(master);
		// (never loading a chunk: a block of it left unloaded checks its tower when it loads, and goes plain then)
		for (BlockPos p : blocks)
			if (!p.equals(without) && level.isLoaded(p) && level.getBlockEntity(p) instanceof BatteryBlockEntity be && be.isFormed() && in(be, t, p))
				be.dissolveStore();
		for (BlockPos p : blocks) {
			if (p.equals(without) || !level.isLoaded(p) || !(level.getBlockEntity(p) instanceof BatteryBlockEntity be) || !in(be, t, p))
				continue;
			BlockState s = level.getBlockState(p);
			be.setTower(null, 0, s.getValue(BatteryBlock.FACING));
			if (s.getBlock() instanceof BatteryBlock && s.getValue(BatteryBlock.FORMED))
				level.setBlock(p, s.setValue(BatteryBlock.FORMED, false), 3);
			be.sendData();
		}
		// bus bars now that they're plain (each saw its neighbours still formed when it changed)
		for (BlockPos p : blocks) {
			if (!level.isLoaded(p))
				continue;
			BlockState s = level.getBlockState(p);
			if (!p.equals(without) && s.getBlock() instanceof BatteryBlock) {
				BlockState joined = BatteryBlock.joins(s, level, p);
				if (joined != s)
					level.setBlock(p, joined, 2);
			}
		}
	}

	// ---------- breaking ----------

	/**
	 * A tower block whose block entity went before the block (a contraption, Carry On): its tower, already apart, and the
	 * spans it let go, until the block goes too and says how (only a contraption's is moved).
	 */
	private record Pending(Tower tower, java.util.Map<BlockPos, List<com.comedymonkey.createaddons.content.line.Span>> loose) {}

	private static final java.util.Map<GlobalPos, Pending> PENDING = new java.util.HashMap<>();

	/** The server stopped: nothing half done is kept for the next world (a singleplayer game keeps what's static). */
	public static void clear() {
		PENDING.clear();
		LOOSE.clear();
	}

	/**
	 * A Battery is going (broken, blown up, carried off): it keeps its share; its tower comes apart and re-forms if it
	 * can (only the switch-in is heard then). Carried off by a contraption, the rest is going too: nothing re-forms.
	 */
	static void removed(ServerLevel level, BlockPos pos, BlockState state, BatteryBlockEntity be, boolean moving) {
		Tower t = state.getBlock() instanceof BatteryBlock && state.getValue(BatteryBlock.FORMED) ? towerOf(be) : null;
		be.dissolveStore();
		if (t == null)
			return;
		try {
			unform(level, t, be);
			be.setTower(null, 0, state.getValue(BatteryBlock.FACING));
			if (!moving)
				reform(level, t, pos);
		} finally {
			finish(level);
		}
	}

	/**
	 * A Battery's block entity went before its block (a contraption or Carry On takes it that way): it keeps its share
	 * and its tower comes apart now; whether what's left re-forms waits for the block to go ({@link #blockGone}), which
	 * tells a contraption (the rest going too) from Carry On (one block picked up).
	 */
	static void entityRemoved(ServerLevel level, BlockPos pos, BlockState state, BatteryBlockEntity be) {
		Tower t = state.getBlock() instanceof BatteryBlock && state.getValue(BatteryBlock.FORMED) ? towerOf(be) : null;
		be.dissolveStore();
		if (t == null)
			return;
		unform(level, t, be);
		be.setTower(null, 0, state.getValue(BatteryBlock.FACING));
		GlobalPos at = GlobalPos.of(level.dimension(), pos.immutable());
		Pending before = PENDING.remove(at);
		if (before != null)
			before.loose().forEach((k, v) -> LOOSE.computeIfAbsent(k, x -> new ArrayList<>()).addAll(v));
		PENDING.put(at, new Pending(t, new java.util.LinkedHashMap<>(LOOSE)));
		LOOSE.clear();
		// the block goes straight after; if it never does, nothing re-forms and the spans come down, as when carried off
		net.minecraft.server.MinecraftServer server = level.getServer();
		server.tell(new net.minecraft.server.TickTask(server.getTickCount(), () -> blockGone(level, at.pos(), true)));
	}

	/** A Battery's block went after its block entity: what's left of its tower re-forms if it can, unless it was moved. */
	static void blockGone(ServerLevel level, BlockPos pos, boolean moving) {
		Pending p = PENDING.remove(GlobalPos.of(level.dimension(), pos));
		if (p == null)
			return;
		p.loose().forEach((k, v) -> LOOSE.computeIfAbsent(k, x -> new ArrayList<>()).addAll(v));
		try {
			if (!moving)
				reform(level, p.tower(), pos);
		} finally {
			finish(level);
		}
	}

	/** After a block of tower t at pos went: the tallest run of full layers left in it re-forms (the lower if two tie). */
	private static void reform(ServerLevel level, Tower t, BlockPos pos) {
		// the runs left below and above the broken layer, within this tower (one stacked on it stays as it is)
		int y = pos.getY();
		int[] below = run(level, t, y - 1, -1, pos), above = run(level, t, y + 1, 1, pos);
		int[] keep = above[1] - above[0] > below[1] - below[0] ? above : below;
		boolean reformed = keep[1] - keep[0] + 1 >= MIN && arrange(level, t.x0(), t.z0(), keep[0], keep[1], t.facing());
		if (!reformed)
			play(level, Sound.UNFORM, middle(t));
	}

	/** From layer y going one way, the full layers on the tower's footprint: {low, high} (empty if high < low). */
	private static int[] run(ServerLevel level, Tower t, int y, int step, BlockPos without) {
		int end = y;
		if (y < t.bottom() || y > t.top() || !fillable(level, t.x0(), t.z0(), y, without))
			return step < 0 ? new int[] {y + 1, y} : new int[] {y, y - 1};
		while (end + step >= t.bottom() && end + step <= t.top() && fillable(level, t.x0(), t.z0(), end + step, without))
			end += step;
		return step < 0 ? new int[] {end, y} : new int[] {y, end};
	}

	// ---------- sound ----------

	private static Vec3 middle(Tower t) {
		return Vec3.atCenterOf(t.center()).add(0, (t.height() - 1) / 2.0, 0);
	}

	static void play(ServerLevel level, Sound sound, BlockPos at) {
		play(level, sound, Vec3.atCenterOf(at));
	}

	private static void play(ServerLevel level, Sound sound, Vec3 at) {
		level.playSound(null, at.x, at.y, at.z, ModSounds.battery(sound.id), SoundSource.BLOCKS, 1,
			0.97f + level.random.nextFloat() * 0.06f);
	}
}
