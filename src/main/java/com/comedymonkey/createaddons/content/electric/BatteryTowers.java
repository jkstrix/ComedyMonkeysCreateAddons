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
		if (state.getValue(BatteryBlock.FORMED)) {
			BatteryBlockEntity m = be.masterEntity();
			if (m != null && be.tile() != null && java.util.Objects.equals(towerOf(m), towerOf(be)))
				return;
			be.setTower(null, 0, state.getValue(BatteryBlock.FACING));
			be.forgetSpans();
			level.setBlock(pos, BatteryBlock.joins(state.setValue(BatteryBlock.FORMED, false), level, pos), 3);
			state = level.getBlockState(pos);
		}
		Direction facing = state.getValue(BatteryBlock.FACING);
		// each footprint it could complete, lowest corner first; once one forms it's taken
		try {
			for (int x0 = pos.getX() - 2; x0 <= pos.getX(); x0++)
				for (int z0 = pos.getZ() - 2; z0 <= pos.getZ(); z0++)
					if (column(level, x0, z0, pos.getY(), facing, null))
						return;
		} finally {
			finish(level);
		}
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
		for (Tower t : plan)
			if (!old.contains(t)) {
				form(level, t);
				formed = true;
			}
		return formed;
	}

	/** The towers standing on this footprint between two layers. */
	private static Set<Tower> towersIn(ServerLevel level, int x0, int z0, int bottom, int top) {
		Set<Tower> out = new LinkedHashSet<>();
		for (int y = bottom; y <= top; y++)
			if (level.getBlockEntity(new BlockPos(x0 + 1, y, z0 + 1)) instanceof BatteryBlockEntity be && be.isFormed()) {
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
		com.comedymonkey.createaddons.content.line.PowerLines.of(level).changed(master);
		level.blockEvent(master, level.getBlockState(master).getBlock(), EVENT_FORM, 0);
		play(level, Sound.FORM, middle(t));
		ModAdvancements.awardNearby(level, BlockPos.containing(middle(t)), ModAdvancements.INDUSTRIAL_BATTERY_FORMED, ItemStack.EMPTY, t.height());
	}

	/** Back to plain Batteries, each with its share ({@code without}: a block that's going, already given its share). */
	private static void unform(ServerLevel level, Tower t, @Nullable BlockPos without) {
		BlockPos master = t.master();
		List<BlockPos> blocks = blocks(master, t.height(), t.facing());
		if (level.getBlockEntity(master) instanceof BatteryBlockEntity m) {
			List<com.comedymonkey.createaddons.content.line.Span> off = m.takeSpans();
			if (!off.isEmpty())
				LOOSE.computeIfAbsent(master, k -> new ArrayList<>()).addAll(off);
		}
		com.comedymonkey.createaddons.content.line.PowerLines.of(level).changed(master);
		for (BlockPos p : blocks)
			if (level.getBlockEntity(p) instanceof BatteryBlockEntity be && be.isFormed() && master.equals(be.master))
				be.dissolveStore();
		for (BlockPos p : blocks) {
			if (p.equals(without) || !(level.getBlockEntity(p) instanceof BatteryBlockEntity be) || !master.equals(be.master))
				continue;
			BlockState s = level.getBlockState(p);
			be.setTower(null, 0, s.getValue(BatteryBlock.FACING));
			if (s.getBlock() instanceof BatteryBlock && s.getValue(BatteryBlock.FORMED))
				level.setBlock(p, s.setValue(BatteryBlock.FORMED, false), 3);
			be.sendData();
		}
		// bus bars now that they're plain (each saw its neighbours still formed when it changed)
		for (BlockPos p : blocks) {
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
	 * A Battery is going (broken, blown up, carried off): it keeps its share; its tower comes apart and re-forms if it
	 * can (only the switch-in is heard then). Carried off by a contraption, the rest is going too: nothing re-forms.
	 */
	static void removed(ServerLevel level, BlockPos pos, BlockState state, BatteryBlockEntity be, boolean moving) {
		Tower t = state.getValue(BatteryBlock.FORMED) ? towerOf(be) : null;
		be.dissolveStore();
		if (t == null)
			return;
		try {
			unform(level, t, pos);
			be.setTower(null, 0, state.getValue(BatteryBlock.FACING));
			if (moving)
				return;
			// the runs left below and above the broken layer (with any tower stacked on the same footprint)
			int y = pos.getY();
			int[] below = run(level, t, y - 1, -1, pos), above = run(level, t, y + 1, 1, pos);
			int[] keep = above[1] - above[0] > below[1] - below[0] ? above : below;
			boolean reformed = keep[1] - keep[0] + 1 >= MIN && arrange(level, t.x0(), t.z0(), keep[0], keep[1], t.facing());
			if (!reformed)
				play(level, Sound.UNFORM, middle(t));
		} finally {
			finish(level);
		}
	}

	/** From layer y going one way, the full layers on the tower's footprint: {low, high} (empty if high < low). */
	private static int[] run(ServerLevel level, Tower t, int y, int step, BlockPos without) {
		int end = y;
		if (!fillable(level, t.x0(), t.z0(), y, without))
			return step < 0 ? new int[] {y + 1, y} : new int[] {y, y - 1};
		while (fillable(level, t.x0(), t.z0(), end + step, without))
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
