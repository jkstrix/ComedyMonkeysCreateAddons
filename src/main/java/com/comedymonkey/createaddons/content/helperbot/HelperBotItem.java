package com.comedymonkey.createaddons.content.helperbot;

import com.comedymonkey.createaddons.registry.ModDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** A Helper Bot, plain or dyed (one dye per bot, in a crafting table; bulk washing takes it off: D5, D16). */
public class HelperBotItem extends BlockItem {
	public HelperBotItem(Block block, Properties properties) {
		super(block, properties);
	}

	/** "Red Helper Bot" and so on. */
	@Override
	public Component getName(ItemStack stack) {
		DyeColor dye = stack.get(ModDataComponents.HELPER_BOT_DYE.get());
		Component name = super.getName(stack);
		return dye == null ? name : Component.translatable("item.comedymonkeysaddons.dyed_helper_bot", Component.translatable("color.minecraft." + dye.getName()), name);
	}
}
