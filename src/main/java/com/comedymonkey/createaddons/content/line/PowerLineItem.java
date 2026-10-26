package com.comedymonkey.createaddons.content.line;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Power Lines in hand: placed against a face, they lie on it as wire. Used on a Line Post's insulator, then another's
 * (or an Industrial Battery's), they hang a span between the two: one Power Line for every four blocks, the line between
 * clear when it's hung. A battery's spans only go to posts.
 */
public class PowerLineItem extends LineItem {
	private final LineTier tier;

	public PowerLineItem(Block block, LineTier tier, Properties properties) {
		super(block, properties);
		this.tier = tier;
	}

	public LineTier tier() {
		return tier;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		SpanHolder post = SpanHolder.clicked(level, pos);
		if (post == null)
			return super.useOn(context);
		Player player = context.getPlayer();
		ItemStack stack = context.getItemInHand();
		int insulator = post.nearestInsulator(context.getClickLocation());
		pos = post.holderPos();
		SpanStart start = stack.get(ModDataComponents.SPAN_START.get());
		if (level.isClientSide)
			return InteractionResult.SUCCESS;
		if (start == null || !level.isLoaded(start.pos())) {
			String problem = canTie(post, insulator, stack.get(ModDataComponents.LINE_DYE.get()));
			if (problem != null) {
				tell(player, problem);
				return InteractionResult.FAIL;
			}
			stack.set(ModDataComponents.SPAN_START.get(), new SpanStart(pos.immutable(), insulator));
			tell(player, "span_started");
			level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.6f, 1.2f);
			return InteractionResult.SUCCESS;
		}
		stack.remove(ModDataComponents.SPAN_START.get());
		if (start.pos().equals(pos) && start.insulator() == insulator) {
			tell(player, "span_cancelled");
			return InteractionResult.SUCCESS;
		}
		String problem = hang(level, start, pos, insulator, stack, player);
		if (problem != null) {
			tell(player, problem);
			return InteractionResult.FAIL;
		}
		level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.8f, 0.9f);
		return InteractionResult.SUCCESS;
	}

	/** Why a span of this tier and colour can't be tied to an insulator, or null if it can. */
	@Nullable
	private String canTie(SpanHolder post, int insulator, @Nullable net.minecraft.world.item.DyeColor dye) {
		if (post.spansAt(insulator).size() >= LinePostBlockEntity.SPANS_PER_INSULATOR)
			return "span_insulator_full";
		LineTier there = post.tierAt(insulator);
		if (there != null && there != tier)
			return "span_other_tier";
		if (there != null && post.dyeAt(insulator) != dye)
			return "span_other_colour";
		return null;
	}

	/** Hangs a span from start to insulator {@code to} of the post at pos; returns why it couldn't, or null. */
	@Nullable
	private String hang(Level level, SpanStart start, BlockPos pos, int to, ItemStack stack, @Nullable Player player) {
		SpanHolder from = SpanHolder.at(level, start.pos()), far = SpanHolder.at(level, pos);
		if (from == null || !from.hasInsulator(start.insulator()) || far == null)
			return "span_start_gone";
		if (start.pos().equals(pos))
			return "span_same_post";
		if (from.isBattery() && far.isBattery())
			return "span_battery_to_battery";
		net.minecraft.world.item.DyeColor dye = stack.get(ModDataComponents.LINE_DYE.get());
		String problem = canTie(from, start.insulator(), dye);
		if (problem == null)
			problem = canTie(far, to, dye);
		if (problem != null)
			return problem;
		Vec3 a = from.tie(start.insulator()), b = far.tie(to);
		double length = a.distanceTo(b);
		if (length > tier.spanReach())
			return "span_too_long";
		if (!clear(level, a, b, start.pos(), pos))
			return "span_blocked";
		int cost = LinePostBlockEntity.wireCount(length);
		boolean creative = player != null && player.getAbilities().instabuild;
		if (!creative && stack.getCount() < cost)
			return "span_needs_more";
		BlockPos offset = pos.subtract(start.pos());
		// the post's end draws it (a battery's never does)
		Span span = new Span(start.insulator(), offset, to, tier, dye, !from.isBattery(), cost);
		from.addSpan(span);
		far.addSpan(span.reversed());
		if (!creative)
			stack.shrink(cost);
		if (player != null)
			com.comedymonkey.createaddons.advancement.ModAdvancements.award(player, com.comedymonkey.createaddons.advancement.ModAdvancements.SPAN_HUNG,
				ItemStack.EMPTY, (int) Math.floor(length));
		return null;
	}

	/** Nothing solid between the two insulators (the posts and batteries themselves aside). */
	static boolean clear(Level level, Vec3 a, Vec3 b, BlockPos postA, BlockPos postB) {
		Vec3 step = b.subtract(a);
		int n = (int) Math.ceil(step.length() * 4);
		for (int i = 1; i < n; i++) {
			Vec3 p = a.add(step.scale(i / (double) n));
			BlockPos bp = BlockPos.containing(p);
			if (bp.equals(postA) || bp.equals(postB))
				continue;
			BlockState s = level.getBlockState(bp);
			// the posts and batteries it hangs between are in its way only where it ties on
			if (s.getBlock() instanceof LinePostBlock || s.getBlock() instanceof com.comedymonkey.createaddons.content.electric.BatteryBlock
				|| s.getCollisionShape(level, bp).isEmpty())
				continue;
			for (net.minecraft.world.phys.AABB box : s.getCollisionShape(level, bp).toAabbs())
				if (box.move(bp).inflate(0.02).contains(p))
					return false;
		}
		return true;
	}

	private static void tell(@Nullable Player player, String key) {
		if (player != null)
			LineInteractions.tell(player, key);
	}
}
