package com.comedymonkey.createaddons.content.light;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Festoon Lights (Festoons): click a wall face or the side of a post where one end goes, then where the other goes; the
 * string hangs between them, using one item per 4 blocks of it. Clicking the first end again stops.
 */
public class FestoonItem extends Item {
	private static final String KEY = ComedyMonkeysCreateAddons.MOD_ID + ".festoon.";

	public FestoonItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Level level = ctx.getLevel();
		Player player = ctx.getPlayer();
		BlockPos clicked = ctx.getClickedPos();
		BlockState state = level.getBlockState(clicked);
		BlockPos cell = state.getBlock() instanceof FestoonHookBlock ? clicked : clicked.relative(ctx.getClickedFace());
		Direction facing = state.getBlock() instanceof FestoonHookBlock ? state.getValue(FestoonHookBlock.FACING) : ctx.getClickedFace();
		if (!Festoons.canHook(level, cell, facing)) {
			tell(player, level, Component.translatable(KEY + "no_hook"));
			return InteractionResult.FAIL;
		}
		ItemStack stack = ctx.getItemInHand();
		CompoundTag from = from(stack, level);
		if (from == null) {
			CompoundTag tag = new CompoundTag();
			tag.putLong("FestoonFrom", cell.asLong());
			tag.putInt("FestoonFacing", facing.get3DDataValue());
			tag.putString("FestoonLevel", level.dimension().location().toString());
			stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
			tell(player, level, Component.translatable(KEY + "first"));
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		stack.remove(DataComponents.CUSTOM_DATA);
		BlockPos a = BlockPos.of(from.getLong("FestoonFrom"));
		if (a.equals(cell)) {
			tell(player, level, Component.translatable(KEY + "stopped"));
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		if (level.isClientSide)
			return InteractionResult.SUCCESS;
		boolean free = player != null && player.getAbilities().instabuild;
		var result = Festoons.hang(level, a, Direction.from3DDataValue(from.getInt("FestoonFacing")), cell, facing, free ? null : stack);
		tell(player, level, Component.translatable(KEY + result.name().toLowerCase(java.util.Locale.ROOT), (int) Festoons.reach(),
			Festoons.cost(Festoons.eye(a, Direction.from3DDataValue(from.getInt("FestoonFacing"))).distanceTo(Festoons.eye(cell, facing)))));
		return InteractionResult.SUCCESS;
	}

	/** The first end it was clicked on, in this level, or null. */
	private static CompoundTag from(ItemStack stack, Level level) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null)
			return null;
		CompoundTag tag = data.copyTag();
		return tag.contains("FestoonFrom") && level.dimension().location().toString().equals(tag.getString("FestoonLevel")) ? tag : null;
	}

	private static void tell(Player player, Level level, Component message) {
		if (player != null && !level.isClientSide)
			player.displayClientMessage(message, true);
	}

	/** It shines while one end is chosen. */
	@Override
	public boolean isFoil(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data != null && data.contains("FestoonFrom") || super.isFoil(stack);
	}
}
