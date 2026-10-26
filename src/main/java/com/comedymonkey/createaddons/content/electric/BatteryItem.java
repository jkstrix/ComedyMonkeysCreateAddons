package com.comedymonkey.createaddons.content.electric;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Battery as an item keeps its charge: shown as a bar, in its tooltip, and in how far its tubes are lit. Placed on top
 * of an Industrial Battery (or under it), it fills the whole 3x3 layer at once, like Create's vaults, if there are
 * enough Batteries in hand and the layer is clear.
 */
public class BatteryItem extends BlockItem {
	public BatteryItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public InteractionResult place(BlockPlaceContext context) {
		InteractionResult result = super.place(context);
		if (result.consumesAction() && !context.getLevel().isClientSide)
			fillLayer(context);
		return result;
	}

	/** The rest of the layer round the Battery just placed against a tower's top or bottom. */
	private void fillLayer(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		if (face.getAxis() != Direction.Axis.Y)
			return;
		Level level = context.getLevel();
		BlockPos placed = context.getClickedPos();
		if (!(level.getBlockEntity(placed.relative(face.getOpposite())) instanceof BatteryBlockEntity tower) || !tower.isFormed()
			|| tower.master() == null)
			return;
		BlockPos centre = BatteryTowers.center(tower.master(), tower.towerFacing()).atY(placed.getY());
		List<BlockPos> rest = new ArrayList<>();
		for (int dz = -1; dz <= 1; dz++)
			for (int dx = -1; dx <= 1; dx++) {
				BlockPos p = centre.offset(dx, 0, dz);
				if (p.equals(placed))
					continue;
				if (!level.getBlockState(p).canBeReplaced())
					return;
				rest.add(p);
			}
		Player player = context.getPlayer();
		ItemStack stack = context.getItemInHand();
		boolean creative = player != null && player.getAbilities().instabuild;
		if (!creative && stack.getCount() < rest.size())
			return;
		// they face the tower's way, as the one placed does when it re-forms
		BlockState state = getBlock().defaultBlockState().setValue(BatteryBlock.FACING, tower.towerFacing());
		for (BlockPos p : rest) {
			level.setBlock(p, BatteryBlock.joins(state, level, p), Block.UPDATE_ALL_IMMEDIATE);
			BlockItem.updateCustomBlockEntityTag(level, player, p, stack);
			if (level.getBlockEntity(p) instanceof BatteryBlockEntity be)
				be.applyComponentsFromItemStack(stack);
			if (!creative)
				stack.shrink(1);
		}
	}

	public static int charge(ItemStack stack) {
		return stack.getOrDefault(ModDataComponents.BATTERY_CHARGE.get(), 0);
	}

	/** How full, 0 to 1 (one from a tower may hold more than a Battery takes: full). */
	public static float fill(ItemStack stack) {
		return Mth.clamp(charge(stack) / (float) CMConfig.SERVER.batteryCapacity(), 0, 1);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return charge(stack) > 0;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13 * fill(stack));
	}

	/** The cyan of its lit tubes. */
	@Override
	public int getBarColor(ItemStack stack) {
		return 0x6FB7C4;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		tooltip.add(Component.translatable("item.comedymonkeysaddons.battery.charge",
				String.format(Locale.ROOT, "%,d", charge(stack)), String.format(Locale.ROOT, "%,d", CMConfig.SERVER.batteryCapacity()))
			.withStyle(ChatFormatting.AQUA));
	}
}
