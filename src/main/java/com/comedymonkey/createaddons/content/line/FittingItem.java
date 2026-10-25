package com.comedymonkey.createaddons.content.line;

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
 * A Valve, Filter, Meter, Splitter or Breaker: right-clicked onto a straight piece of a line that takes it, it clips on
 * round the line, one per block. The wrench takes it off again.
 */
public class FittingItem extends Item {
	private final Fitting fitting;

	public FittingItem(Fitting fitting, Properties properties) {
		super(properties);
		this.fitting = fitting;
	}

	public Fitting fitting() {
		return fitting;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof LineBlock line) || !(level.getBlockEntity(pos) instanceof LineBlockEntity be))
			return InteractionResult.PASS;
		Player player = context.getPlayer();
		if (!line.takes(fitting)) {
			tell(level, player, fitting.fitsFluidLines() ? "fitting_fluid_only" : "fitting_power_only");
			return InteractionResult.FAIL;
		}
		if (be.fitting() != Fitting.NONE) {
			tell(level, player, "fitting_already");
			return InteractionResult.FAIL;
		}
		Direction side = line.straightSide(level, pos, state);
		if (side == null) {
			tell(level, player, "fitting_needs_straight");
			return InteractionResult.FAIL;
		}
		if (!level.isClientSide) {
			be.setFitting(fitting, side);
			if (fitting == Fitting.VALVE || fitting == Fitting.BREAKER)
				be.setPowered(level.hasNeighborSignal(pos));
			if (player == null || !player.getAbilities().instabuild)
				context.getItemInHand().shrink(1);
			level.playSound(null, pos, SoundEvents.COPPER_PLACE, SoundSource.BLOCKS, 0.8f, 1.1f);
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	private static void tell(Level level, Player player, String key) {
		if (player != null && level.isClientSide)
			LineInteractions.tell(player, key);
	}
}
