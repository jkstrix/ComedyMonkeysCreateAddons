package com.comedymonkey.createaddons.content.line;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.registry.ModDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** A Fluid or Power Line as an item: a dyed one says its colour ("Red Copper Power Line"). */
public class LineItem extends BlockItem {
	public LineItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public Component getName(ItemStack stack) {
		return named(stack.get(ModDataComponents.LINE_DYE.get()), super.getName(stack));
	}

	/** One line of this block as an item, in the colour of the line there. */
	public static ItemStack of(Block block, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
		ItemStack stack = new ItemStack(block);
		if (level.getBlockEntity(pos) instanceof LineBlockEntity be && be.dye() != null)
			stack.set(ModDataComponents.LINE_DYE.get(), be.dye());
		return stack;
	}

	/** A line's name with its colour in front, if it's dyed. */
	public static Component named(@Nullable DyeColor dye, Component name) {
		if (dye == null)
			return name;
		return Component.translatable("item.comedymonkeysaddons.dyed_line", Component.translatable("color.minecraft." + dye.getName()), name);
	}
}
