package com.comedymonkey.createaddons.content.line;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Which spans a Schematicannon prints from a post or an Industrial Battery's master, read in the cannon's schematic level
 * (where both ends of a span in the schematic stand). A schematic file can say anything, so a span prints only as a
 * player could have hung it: held at both ends in the schematic (the same span), the post's end drawing it (a
 * battery's never does), to insulators the ends have, two to an insulator of one tier and colour, not battery to
 * battery, in reach and clear sight; and its wire is what its length takes. Each end works this out alike, so both
 * print the same span or neither does.
 */
public final class SpanPrinting {
	private SpanPrinting() {}

	public static List<Span> printable(Level level, BlockPos pos, SpanHolder here) {
		List<Span> out = new ArrayList<>();
		for (Span s : new LinkedHashSet<>(here.spans())) {
			Span p = printable(level, pos, here, s);
			if (p != null)
				out.add(p);
		}
		return out;
	}

	@Nullable
	private static Span printable(Level level, BlockPos pos, SpanHolder here, Span s) {
		if (s.offset().equals(BlockPos.ZERO))
			return null;
		BlockPos farPos = pos.offset(s.offset());
		// (the far end as it stands in the schematic: its master isn't where it's printed, so not SpanHolder.at)
		if (!(level.getBlockEntity(farPos) instanceof SpanHolder far) || !far.spans().contains(s.reversed()))
			return null;
		if (here.isBattery() && far.isBattery())
			return null;
		// the post's end draws it and pays for it
		if (here.isBattery() ? s.owner() : far.isBattery() && !s.owner())
			return null;
		if (!has(level, pos, here, s.from()) || !has(level, farPos, far, s.to()) || !fits(here, s.from()) || !fits(far, s.to()))
			return null;
		Vec3 a = tie(level, pos, here, s.from()), b = tie(level, farPos, far, s.to());
		double length = a.distanceTo(b);
		if (length > s.tier().spanReach() || !clear(level, a, b, pos, farPos))
			return null;
		return new Span(s.from(), s.offset(), s.to(), s.tier(), s.dye(), s.owner(), LinePostBlockEntity.wireCount(length));
	}

	/**
	 * As PowerLineItem.clear, in the schematic: only what's in it is in the way (Create's schematic level makes the layer
	 * under a schematic dirt, which a hanging post's span dips into).
	 */
	private static boolean clear(Level level, Vec3 a, Vec3 b, BlockPos postA, BlockPos postB) {
		if (!(level instanceof net.createmod.catnip.levelWrappers.SchematicLevel schematic))
			return PowerLineItem.clear(level, a, b, postA, postB);
		Vec3 step = b.subtract(a);
		int n = (int) Math.ceil(step.length() * 4);
		for (int i = 1; i < n; i++) {
			Vec3 p = a.add(step.scale(i / (double) n));
			BlockPos bp = BlockPos.containing(p);
			if (bp.equals(postA) || bp.equals(postB) || !schematic.getBounds().isInside(bp.subtract(schematic.anchor)))
				continue;
			BlockState s = level.getBlockState(bp);
			if (s.getBlock() instanceof LinePostBlock || s.getBlock() instanceof com.comedymonkey.createaddons.content.electric.BatteryBlock
				|| s.getCollisionShape(level, bp).isEmpty())
				continue;
			for (net.minecraft.world.phys.AABB box : s.getCollisionShape(level, bp).toAabbs())
				if (box.move(bp).inflate(0.02).contains(p))
					return false;
		}
		return true;
	}

	private static boolean has(Level level, BlockPos pos, SpanHolder h, int insulator) {
		if (h.isBattery())
			return insulator >= 0 && insulator < 4;
		return LinePostBlock.hasInsulator(level.getBlockState(pos), insulator);
	}

	/** Two spans at most on an insulator, all of one tier and colour (as PowerLineItem.canTie lets them be hung). */
	private static boolean fits(SpanHolder h, int insulator) {
		List<Span> on = h.spansAt(insulator);
		if (on.size() > LinePostBlockEntity.SPANS_PER_INSULATOR)
			return false;
		for (Span o : on)
			if (o.tier() != on.get(0).tier() || !Objects.equals(o.dye(), on.get(0).dye()))
				return false;
		return true;
	}

	private static Vec3 tie(Level level, BlockPos pos, SpanHolder h, int insulator) {
		BlockState state = level.getBlockState(pos);
		return state.getBlock() instanceof LinePostBlock ? LinePostBlock.tie(state, pos, insulator) : h.tie(insulator);
	}
}
