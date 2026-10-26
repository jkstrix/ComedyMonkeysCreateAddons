package com.comedymonkey.createaddons.content.line;

import com.comedymonkey.createaddons.content.Wrenches;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * What a player's clicks do to a piece of line, fluid or power alike: dye it, wash the dye off, set a filter, turn a
 * Valve, flip a Breaker, switch a Splitter's mode, encase it in a block, and the wrench's jobs.
 */
public final class LineInteractions {
	private LineInteractions() {}

	public static ItemInteractionResult useItem(ItemStack stack, LineBlockEntity be, Level level, BlockPos pos, Player player) {
		// skip the empty-hand use, so the wrench's own use (onWrenched) runs
		if (Wrenches.isWrench(stack))
			return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
		if (stack.getItem() instanceof DyeItem dye) {
			if (be.dye() == dye.getDyeColor())
				return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
			if (!level.isClientSide) {
				be.setDye(dye.getDyeColor());
				if (!player.getAbilities().instabuild)
					stack.shrink(1);
				level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1f, 1f);
			}
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		}
		FluidStack fluid = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
		if (!fluid.isEmpty()) {
			if (be.fitting() == Fitting.INTAKE || be.fitting() == Fitting.FILTER) {
				if (!level.isClientSide) {
					be.setFilter(fluid);
					tell(player, be.fitting() == Fitting.INTAKE ? "intake_filter_set" : "filter_set", fluid.getHoverName());
					level.playSound(null, pos, SoundEvents.COPPER_HIT, SoundSource.BLOCKS, 0.6f, 1.4f);
				}
				return ItemInteractionResult.sidedSuccess(level.isClientSide);
			}
			// water washes the dye off
			if (fluid.getFluid().isSame(Fluids.WATER) && be.dye() != null) {
				if (!level.isClientSide) {
					be.setDye(null);
					level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.6f, 1.2f);
				}
				return ItemInteractionResult.sidedSuccess(level.isClientSide);
			}
		}
		if (be.encasing() == null && stack.getItem() instanceof BlockItem item && encasable(item.getBlock(), level, pos)) {
			if (!level.isClientSide) {
				BlockState casing = item.getBlock().defaultBlockState();
				be.setEncasing(casing);
				com.comedymonkey.createaddons.advancement.ModAdvancements.award(player, com.comedymonkey.createaddons.advancement.ModAdvancements.LINE_ENCASED);
				if (!player.getAbilities().instabuild)
					stack.shrink(1);
				level.playSound(null, pos, casing.getSoundType(level, pos, player).getPlaceSound(), SoundSource.BLOCKS, 1f, 0.9f);
			}
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		}
		// anything else in hand: its own use (placing a block beside the line, say), never the empty-hand one
		return stack.isEmpty() ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
	}

	/** Any plain full block can hide a line: no block entity, nothing see-through, not another line. */
	static boolean encasable(Block block, Level level, BlockPos pos) {
		if (block instanceof EntityBlock || block instanceof LineBlock)
			return false;
		BlockState state = block.defaultBlockState();
		return state.getRenderShape() == RenderShape.MODEL && state.isSolidRender(level, pos) && state.isCollisionShapeFullBlock(level, pos);
	}

	/** An empty hand: clears a filter, turns a Valve (sneak to open it), switches a Splitter, flips a Breaker. */
	public static InteractionResult useEmpty(LineBlockEntity be, Level level, BlockPos pos, Player player) {
		switch (be.fitting()) {
			case INTAKE, FILTER -> {
				if (be.filter().isEmpty())
					return InteractionResult.PASS;
				if (!level.isClientSide) {
					be.setFilter(FluidStack.EMPTY);
					tell(player, be.fitting() == Fitting.INTAKE ? "intake_filter_cleared" : "filter_cleared");
				}
			}
			case VALVE -> {
				if (!level.isClientSide) {
					be.turnValve(player.isShiftKeyDown() ? 1 : -1);
					tell(player, "valve", be.valveOpen() * 10);
					level.playSound(null, pos, SoundEvents.CHAIN_STEP, SoundSource.BLOCKS, 0.6f, 0.8f + be.valveOpen() * 0.04f);
				}
			}
			case SPLITTER -> {
				// a Power Line Splitter's priorities are set with a wrench, in its screen (encased: the block's own use)
				if (be instanceof PowerLineBlockEntity && be.encasing() != null)
					return InteractionResult.PASS;
				if (be instanceof PowerLineBlockEntity) {
					if (!level.isClientSide)
						tell(player, "splitter_use_wrench");
				} else if (!level.isClientSide) {
					be.nextSplitMode();
					tell(player, "splitter_" + be.splitMode().key());
					level.playSound(null, pos, SoundEvents.COPPER_HIT, SoundSource.BLOCKS, 0.6f, 1.2f);
				}
			}
			case BREAKER -> {
				if (!level.isClientSide) {
					be.flipLever();
					tell(player, be.leverOn() ? "breaker_on" : "breaker_off");
					level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6f, be.leverOn() ? 0.6f : 0.5f);
				}
			}
			default -> {
				return InteractionResult.PASS;
			}
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	/** The wrench: sneaking, it takes the encasing off; otherwise it takes off a fitting, or works the joint on that side. */
	public static InteractionResult wrench(LineBlockEntity be, Level level, Player player, Direction side, boolean sneaking) {
		if (level.isClientSide)
			return InteractionResult.SUCCESS;
		if (sneaking) {
			BlockState casing = be.encasing();
			if (casing == null)
				return InteractionResult.PASS;
			be.setEncasing(null);
			be.give(player, new ItemStack(casing.getBlock()));
			level.playSound(null, be.getBlockPos(), casing.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1f, 1f);
			return InteractionResult.SUCCESS;
		}
		if (be.fitting() != Fitting.NONE)
			be.removeFitting(player);
		else
			be.wrenchJoint(side);
		return InteractionResult.SUCCESS;
	}

	/** A short message over the hotbar. */
	public static void tell(Player player, String key, Object... args) {
		player.displayClientMessage(Component.translatable("comedymonkeysaddons.line." + key, args), true);
	}
}
