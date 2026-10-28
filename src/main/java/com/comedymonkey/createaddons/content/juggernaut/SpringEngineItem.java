package com.comedymonkey.createaddons.content.juggernaut;

import java.util.List;

import com.comedymonkey.createaddons.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** A Spring Engine as an item: it keeps the wind it held (D38), shown in its tooltip and as a bar. */
public class SpringEngineItem extends BlockItem {
	public SpringEngineItem(Block block, Properties properties) {
		super(block, properties);
	}

	private static float fill(ItemStack stack) {
		return (float) ((double) stack.getOrDefault(ModDataComponents.SPRING_WIND.get(), 0L) / SpringEngineBlockEntity.capacity());
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		tooltip.add(Component.translatable("comedymonkeysaddons.spring_engine.item_wound", Math.round(fill(stack) * 100))
			.withStyle(ChatFormatting.GRAY));
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return stack.has(ModDataComponents.SPRING_WIND.get());
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13 * Math.min(1, fill(stack)));
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return 0x4F8FAE; // his runes' frost-blue
	}
}
