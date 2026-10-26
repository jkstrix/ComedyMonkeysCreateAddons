package com.comedymonkey.createaddons.content.line;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * What spans of Power Line tie to, by insulator number: a Line Post's head, or an Industrial Battery (at its master).
 * Each insulator holds {@link LinePostBlockEntity#SPANS_PER_INSULATOR} spans of one tier and colour. Spans run between
 * posts, or from a post to a battery (the post's end draws it then); never from one battery to another.
 */
public interface SpanHolder {
	List<Span> spans();

	/** Where a span ties onto an insulator, in the world. */
	Vec3 tie(int insulator);

	boolean hasInsulator(int insulator);

	/** The insulator nearest a point clicked on it. */
	int nearestInsulator(Vec3 at);

	void addSpan(Span span);

	/** The far end took a span down: it goes here too. Returns whether this end still held it. */
	boolean removeSpan(Span span);

	/** An Industrial Battery: it only takes spans to posts, and never draws them. */
	default boolean isBattery() {
		return false;
	}

	default List<Span> spansAt(int insulator) {
		List<Span> out = new ArrayList<>();
		for (Span s : spans())
			if (s.from() == insulator)
				out.add(s);
		return out;
	}

	/** The tier of the cable tied to an insulator, or null if none is. */
	@Nullable
	default LineTier tierAt(int insulator) {
		for (Span s : spans())
			if (s.from() == insulator)
				return s.tier();
		return null;
	}

	/** The colour of cable on an insulator (null: plain, or none tied yet; see {@link #tierAt}). */
	@Nullable
	default DyeColor dyeAt(int insulator) {
		for (Span s : spans())
			if (s.from() == insulator)
				return s.dye();
		return null;
	}

	/** Whether it takes spans now (a Battery only while it's an Industrial Battery's master). */
	default boolean holdsSpans() {
		return true;
	}

	/** Where it is (the end a span's offset is measured from). */
	BlockPos holderPos();

	/** A block that's part of something spans tie to: a block of an Industrial Battery, whose master holds them. */
	interface Part {
		@Nullable
		SpanHolder spanHolder();
	}

	/** What holds spans at pos, if anything. */
	@Nullable
	static SpanHolder at(Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof SpanHolder h && h.holdsSpans() ? h : null;
	}

	/** What a span ties to when pos is clicked: a post's head, or the battery a block is part of. */
	@Nullable
	static SpanHolder clicked(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof LinePostBlock)
			return state.getValue(LinePostBlock.ROLE).isHead() ? at(level, pos) : null;
		return level.getBlockEntity(pos) instanceof Part p ? p.spanHolder() : null;
	}

	/** Where a span ties at its far end, for drawing it: a post by its block, a battery by its block entity (null: nothing there). */
	@Nullable
	static Vec3 farTie(Level level, BlockPos pos, int insulator) {
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof LinePostBlock)
			return LinePostBlock.tie(state, pos, insulator);
		SpanHolder h = at(level, pos);
		return h != null && h.isBattery() && h.hasInsulator(insulator) ? h.tie(insulator) : null;
	}
}
