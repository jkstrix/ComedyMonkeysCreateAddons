package com.comedymonkey.createaddons.content.line;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Intake: clipped onto the side of a Fluid Line that faces a tank, it draws that tank's fluid into the line (the
 * only part that does: it is what replaces the pump). One per piece of line. Its filter is set on the line afterwards.
 */
public class IntakeItem extends Item {
	public IntakeItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof FluidLineBlock) || !(level.getBlockEntity(pos) instanceof LineBlockEntity be))
			return InteractionResult.PASS;
		Player player = context.getPlayer();
		if (be.fitting() != Fitting.NONE) {
			tell(player, "fitting_already");
			return InteractionResult.FAIL;
		}
		Direction side = tankSide(state, pos, context);
		if (side == null) {
			tell(player, "intake_needs_tank");
			return InteractionResult.FAIL;
		}
		if (!level.isClientSide) {
			be.setFitting(Fitting.INTAKE, side);
			if (player != null)
				com.comedymonkey.createaddons.advancement.ModAdvancements.award(player, com.comedymonkey.createaddons.advancement.ModAdvancements.INTAKE_FITTED);
			if (player == null || !player.getAbilities().instabuild)
				context.getItemInHand().shrink(1);
			level.playSound(null, pos, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 0.8f, 1.1f);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	/** The side to fit it on: the tank side the click landed nearest, or the only tank side there is. */
	@Nullable
	private static Direction tankSide(BlockState state, BlockPos pos, UseOnContext context) {
		Direction hit = FluidLineBlock.nearestSide(pos, context.getClickLocation(), context.getClickedFace());
		if (FluidLineBlock.side(state, hit) == LineSide.TANK)
			return hit;
		Direction only = null;
		for (Direction d : Direction.values()) {
			if (FluidLineBlock.side(state, d) != LineSide.TANK)
				continue;
			if (only != null)
				return null;
			only = d;
		}
		return only;
	}

	private static void tell(@Nullable Player player, String key) {
		if (player != null && player.level().isClientSide)
			LineInteractions.tell(player, key);
	}
}
