package com.comedymonkey.createaddons.content.line;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModDataComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Power Lines in hand: placed against a face, they lie on it as wire. Used on a Line Post's insulator, then another's,
 * they hang a span between the two: one Power Line for every four blocks, the line between clear when it's hung.
 */
public class PowerLineItem extends BlockItem {
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
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof LinePostBlock) || !state.getValue(LinePostBlock.ROLE).isHead())
			return super.useOn(context);
		Player player = context.getPlayer();
		ItemStack stack = context.getItemInHand();
		int insulator = LinePostBlock.nearestInsulator(state, pos, context.getClickLocation());
		if (!(level.getBlockEntity(pos) instanceof LinePostBlockEntity post))
			return InteractionResult.FAIL;
		SpanStart start = stack.get(ModDataComponents.SPAN_START.get());
		if (level.isClientSide)
			return InteractionResult.SUCCESS;
		if (start == null || !level.isLoaded(start.pos())) {
			String problem = canTie(post, insulator);
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

	/** Why a span of this tier can't be tied to an insulator, or null if it can. */
	@Nullable
	private String canTie(LinePostBlockEntity post, int insulator) {
		if (post.spansAt(insulator).size() >= LinePostBlockEntity.SPANS_PER_INSULATOR)
			return "span_insulator_full";
		LineTier there = post.tierAt(insulator);
		if (there != null && there != tier)
			return "span_other_tier";
		return null;
	}

	/** Hangs a span from start to insulator {@code to} of the post at pos; returns why it couldn't, or null. */
	@Nullable
	private String hang(Level level, SpanStart start, BlockPos pos, int to, ItemStack stack, @Nullable Player player) {
		if (!(level.getBlockEntity(start.pos()) instanceof LinePostBlockEntity from) || !LinePostBlock.hasInsulator(from.getBlockState(), start.insulator()))
			return "span_start_gone";
		if (!(level.getBlockEntity(pos) instanceof LinePostBlockEntity far))
			return "span_start_gone";
		if (start.pos().equals(pos))
			return "span_same_post";
		String problem = canTie(from, start.insulator());
		if (problem == null)
			problem = canTie(far, to);
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
		Span span = new Span(start.insulator(), offset, to, tier, stack.get(ModDataComponents.LINE_DYE.get()), true, cost);
		from.addSpan(span);
		far.addSpan(span.reversed());
		if (!creative)
			stack.shrink(cost);
		if (player != null)
			com.comedymonkey.createaddons.advancement.ModAdvancements.award(player, com.comedymonkey.createaddons.advancement.ModAdvancements.SPAN_HUNG,
				ItemStack.EMPTY, (int) Math.floor(length));
		return null;
	}

	/** Nothing solid between the two insulators (the posts themselves aside). */
	static boolean clear(Level level, Vec3 a, Vec3 b, BlockPos postA, BlockPos postB) {
		Vec3 step = b.subtract(a);
		int n = (int) Math.ceil(step.length() * 4);
		for (int i = 1; i < n; i++) {
			Vec3 p = a.add(step.scale(i / (double) n));
			BlockPos bp = BlockPos.containing(p);
			if (bp.equals(postA) || bp.equals(postB))
				continue;
			BlockState s = level.getBlockState(bp);
			if (s.getBlock() instanceof LinePostBlock || s.getCollisionShape(level, bp).isEmpty())
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
