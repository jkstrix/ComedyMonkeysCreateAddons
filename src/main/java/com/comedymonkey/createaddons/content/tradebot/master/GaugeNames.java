package com.comedymonkey.createaddons.content.tradebot.master;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * A gauge's item as his screen and goggles name it: what the gauge asks for, exactly (design/specs/trade-master.md P7).
 * An enchanted book is named by its enchantments ("Mending", "Sharpness V"), as a gauge for one counts only those
 * (user, 2026-11-04: "enchanted book should be the type of book"); anything else by its own name.
 */
public final class GaugeNames {
	private GaugeNames() {}

	public static Component of(ItemStack kind) {
		ItemEnchantments stored = kind.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
		if (stored.isEmpty())
			return kind.getHoverName();
		MutableComponent name = Component.empty();
		boolean first = true;
		for (var entry : stored.entrySet()) {
			if (!first)
				name.append(", ");
			name.append(Enchantment.getFullname(entry.getKey(), entry.getIntValue()).copy().withStyle(style -> style.withColor((TextColor) null)));
			first = false;
		}
		return name;
	}
}
