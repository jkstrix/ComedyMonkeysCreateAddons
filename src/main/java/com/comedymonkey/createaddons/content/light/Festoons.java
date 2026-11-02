package com.comedymonkey.createaddons.content.light;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;

/**
 * Festoon Lights (design/specs/electric-lights.md, D20, P13, P14): a string of bulbs hung between two hooks, each on a
 * wall face or the side of a post, up to 16 blocks apart (config), using one Festoon Lights item per 4 blocks of string
 * (rounded up, given back when it comes down). The hook it was hung from first holds the string (FestoonBlockEntity): it
 * draws it, draws its power and puts a light-10 spot every 4 blocks along it. Hooks joined by strings are one run, fed
 * wherever a hook is fed (through its wall, or by its post's head) and powering only themselves, so a festoon never joins
 * two networks.
 */
public final class Festoons {
	/** Blocks of string a Festoon Lights item covers. */
	public static final int BLOCKS_PER_ITEM = 4;
	/** A bulb hangs every so many blocks along the string; a spot of light every 4 (P13). */
	public static final double BULB_EVERY = 2, SPOT_EVERY = 4;
	/** A festoon's light at full brightness (P1). */
	public static final int LIGHT = 10;

	public enum Result {
		HUNG,
		TOO_FAR,
		TOO_SHORT,
		NO_HOOK,
		ALREADY,
		NOT_ENOUGH
	}

	private Festoons() {}

	public static double reach() {
		return CMConfig.SERVER.festoonReach();
	}

	/** Where the hook at pos, facing out of its wall, holds the string: its eye (built facing south, at 8, 12, 1 px). */
	public static Vec3 eye(BlockPos pos, Direction facing) {
		return Vec3.atCenterOf(pos).add(0, 4 / 16.0, 0).add(Vec3.atLowerCornerOf(facing.getNormal()).scale(1 / 16.0 - 0.5));
	}

	/** A point along the string from a to b, t from 0 to 1: it sags a twelfth of its length in the middle, as spans do. */
	public static Vec3 along(Vec3 a, Vec3 b, double t) {
		double sag = a.distanceTo(b) / 12;
		return a.lerp(b, t).subtract(0, sag * 4 * t * (1 - t), 0);
	}

	/** Festoon Lights items a string of this length takes (P14). */
	public static int cost(double length) {
		return Math.max(1, (int) Math.ceil(length / BLOCKS_PER_ITEM - 1e-6));
	}

	/** Where along a string its bulbs hang (0 to 1). */
	public static double[] bulbs(double length) {
		return spaced(length, BULB_EVERY);
	}

	private static double[] spaced(double length, double every) {
		int n = Math.max(1, (int) Math.round(length / every));
		double[] out = new double[n];
		for (int k = 0; k < n; k++)
			out[k] = (k + 0.5) / n;
		return out;
	}

	/** The blocks a string's light spots go in: under it, every 4 blocks along (where its bulbs hang). */
	public static List<BlockPos> spotCells(BlockPos a, Direction fa, BlockPos b, Direction fb) {
		Vec3 ea = eye(a, fa), eb = eye(b, fb);
		List<BlockPos> out = new ArrayList<>();
		for (double t : spaced(ea.distanceTo(eb), SPOT_EVERY)) {
			BlockPos cell = BlockPos.containing(along(ea, eb, t).subtract(0, 0.25, 0));
			if (!cell.equals(a) && !cell.equals(b) && !out.contains(cell))
				out.add(cell);
		}
		return out;
	}

	/** A hook can be at cell facing so: one's there already, or it's air in front of something to hang it on. */
	public static boolean canHook(Level level, BlockPos cell, Direction facing) {
		if (!facing.getAxis().isHorizontal() || !level.isLoaded(cell))
			return false;
		BlockState there = level.getBlockState(cell);
		if (there.getBlock() instanceof FestoonHookBlock)
			return there.getValue(FestoonHookBlock.FACING) == facing;
		BlockPos support = cell.relative(facing.getOpposite());
		return (there.isAir() || there.getBlock() instanceof LightSpotBlock) && level.isLoaded(support)
			&& FestoonHookBlock.holds(level, support, facing);
	}

	/**
	 * Hangs a string from the hook at a (facing fa) to the hook at b, putting up either hook that isn't there; it's paid
	 * for from {@code pay} (null: free, as in creative). Each hook put up is a block placement by {@code player}, so
	 * protection mods can refuse it (P31): refused, neither hook stays, no string hangs and nothing is paid.
	 */
	public static Result hang(Level level, BlockPos a, Direction fa, BlockPos b, Direction fb, @Nullable ItemStack pay, @Nullable Player player) {
		return hang(level, a, fa, b, fb, pay, player, false);
	}

	/**
	 * As above; a hook already at either end is asked about too (a string is hung on it, its spots go round it), unless
	 * it's at b and {@code clickedB}: the player clicked that hook itself, so the click was already asked about.
	 */
	public static Result hang(Level level, BlockPos a, Direction fa, BlockPos b, Direction fb, @Nullable ItemStack pay, @Nullable Player player,
							  boolean clickedB) {
		if (a.equals(b) || !canHook(level, a, fa) || !canHook(level, b, fb))
			return Result.NO_HOOK;
		double length = eye(a, fa).distanceTo(eye(b, fb));
		if (length > reach())
			return Result.TOO_FAR;
		if (length < 1.5)
			return Result.TOO_SHORT;
		if (level.getBlockEntity(a) instanceof FestoonBlockEntity ha && ha.partners().contains(b))
			return Result.ALREADY;
		int cost = cost(length);
		if (pay != null && pay.getCount() < cost)
			return Result.NOT_ENOUGH;
		// The hooks are placed here, each with its own event, not left to the item use's capture: that would fire one
		// event after the string was already on a hook that was there (left hanging from it if refused), and count the
		// spots of light a lit hook puts up as placed by the player too.
		boolean capturing = level.captureBlockSnapshots;
		level.captureBlockSnapshots = false;
		try {
			List<BlockSnapshot> put = new ArrayList<>(2);
			if (!putUp(level, a, fa, player, true, put) || !putUp(level, b, fb, player, !clickedB, put)) {
				takeDown(level, put);
				return Result.NO_HOOK;
			}
			if (!(level.getBlockEntity(a) instanceof FestoonBlockEntity ha) || !(level.getBlockEntity(b) instanceof FestoonBlockEntity hb))
				return Result.NO_HOOK;
			ha.addSpan(new FestoonBlockEntity.Span(b, fb, true, pay != null));
			hb.addSpan(new FestoonBlockEntity.Span(a, fa, false, pay != null));
		} finally {
			level.captureBlockSnapshots = capturing;
		}
		if (pay != null)
			pay.shrink(cost);
		RunLightBlockEntity.runsChanged(level);
		level.playSound(null, a, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.6f, 1.3f);
		return Result.HUNG;
	}

	/**
	 * Puts up a hook at cell if there isn't one, as a placement by player against the block it hangs on (as a block item
	 * places one: placed, then the event, so listeners see the hook). False if the event was refused (the hook comes down
	 * again); what it put up is added to {@code put}. A hook already there is asked about the same way if {@code askIfThere}
	 * (it's someone's hook a string is going on to), and nothing is put up.
	 */
	private static boolean putUp(Level level, BlockPos cell, Direction facing, @Nullable Player player, boolean askIfThere, List<BlockSnapshot> put) {
		if (level.getBlockState(cell).getBlock() instanceof FestoonHookBlock)
			return !askIfThere || !EventHooks.onBlockPlace(player, BlockSnapshot.create(level.dimension(), level, cell), facing);
		boolean post = level.getBlockState(cell.relative(facing.getOpposite())).getBlock() instanceof com.comedymonkey.createaddons.content.line.LinePostBlock;
		BlockSnapshot was = BlockSnapshot.create(level.dimension(), level, cell);
		if (!level.setBlock(cell, ModBlocks.FESTOON_HOOK.get().defaultBlockState().setValue(FestoonHookBlock.FACING, facing)
			.setValue(FestoonHookBlock.POST, post), Block.UPDATE_ALL))
			return false;
		put.add(was);
		return !EventHooks.onBlockPlace(player, was, facing);
	}

	/** Hooks put up for a string that was refused come down again, leaving what was there before (nothing dropped). */
	private static void takeDown(Level level, List<BlockSnapshot> put) {
		for (int i = put.size() - 1; i >= 0; i--) {
			level.restoringBlockSnapshots = true;
			try {
				put.get(i).restore(put.get(i).getFlags() | Block.UPDATE_CLIENTS);
			} finally {
				level.restoringBlockSnapshots = false;
			}
		}
	}

	/**
	 * The hook at pos is coming down: each string on it comes down too, giving back the Festoon Lights it was hung with
	 * (once: here, unless the far hook is unloaded and holds it, when it gives them back on finding it gone); a hook left
	 * with no string goes.
	 */
	static void cut(Level level, BlockPos pos, FestoonBlockEntity be) {
		Direction facing = be.getBlockState().getValue(FestoonHookBlock.FACING);
		for (FestoonBlockEntity.Span span : be.spans()) {
			boolean there = level.isLoaded(span.other()) && level.getBlockEntity(span.other()) instanceof FestoonBlockEntity;
			if (span.paid() && (there || span.held()))
				Block.popResource(level, pos, new ItemStack(ModItems.FESTOON_LIGHTS.get(), cost(eye(pos, facing).distanceTo(eye(span.other(), span.facing())))));
			if (!there || !(level.getBlockEntity(span.other()) instanceof FestoonBlockEntity other))
				continue;
			other.removeSpan(pos);
			if (other.spans().isEmpty())
				level.removeBlock(span.other(), false);
		}
		be.clearSpans();
		RunLightBlockEntity.runsChanged(level);
	}
}
