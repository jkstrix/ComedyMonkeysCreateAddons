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

	/**
	 * What a Schematicannon spends on {@code count} lines of this block as {@code be} is: lines of exactly its colour
	 * (plain ones for a plain line, so a covered cable is printed from covered cable), its fitting, and the block it's
	 * encased in.
	 */
	public static com.simibubi.create.content.schematics.requirement.ItemRequirement printed(Block block,
			@Nullable net.minecraft.world.level.block.entity.BlockEntity be, int count) {
		ItemStack stack = new ItemStack(block, Math.max(1, count));
		if (be instanceof LineBlockEntity line && line.dye() != null)
			stack.set(ModDataComponents.LINE_DYE.get(), line.dye());
		var cost = new com.comedymonkey.createaddons.content.PrintCost().addExact(stack);
		if (be instanceof LineBlockEntity line) {
			if (line.fitting() != Fitting.NONE)
				cost.add(new ItemStack(line.fitting().item()));
			if (line.encasing() != null && line.encasing().getBlock().asItem() != net.minecraft.world.item.Items.AIR)
				cost.add(new ItemStack(line.encasing().getBlock().asItem()));
		}
		return cost.build();
	}

	/** A line's name with its colour in front, if it's dyed. */
	public static Component named(@Nullable DyeColor dye, Component name) {
		if (dye == null)
			return name;
		return Component.translatable("item.comedymonkeysaddons.dyed_line", Component.translatable("color.minecraft." + dye.getName()), name);
	}
}
