package com.comedymonkey.createaddons.content;

import java.util.List;
import java.util.Optional;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.config.CMConfig;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

/**
 * Enchantments for machines, each for one machine's item and each I to III: <b>Bountiful</b> grows the Auto
 * Harvester's plant faster, <b>Fruitful</b> makes the Animal Pens breed faster. They're defined in data
 * ({@code data/comedymonkeysaddons/enchantment/}), go on with an anvil, aren't offered by enchanting tables, and come
 * as books from Librarians: in their usual random book trades, and as extra Apprentice trades.
 */
public final class MachineEnchantments {
	public static final ResourceKey<Enchantment> BOUNTIFUL = key("bountiful");
	public static final ResourceKey<Enchantment> FRUITFUL = key("fruitful");
	public static final int MAX_LEVEL = 3;
	/** Librarian level the extra trades are unlocked at (2 = Apprentice). */
	private static final int TRADE_LEVEL = 2;

	private MachineEnchantments() {}

	private static ResourceKey<Enchantment> key(String name) {
		return ResourceKey.create(Registries.ENCHANTMENT, ComedyMonkeysCreateAddons.asResource(name));
	}

	public static int levelIn(ItemEnchantments enchantments, ResourceKey<Enchantment> key) {
		for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet())
			if (entry.getKey().is(key))
				return entry.getIntValue();
		return 0;
	}

	/** How many times faster than normal at this level, with {@code bonus} added per level. */
	public static float speedMultiplier(int level, ModConfigSpec.DoubleValue bonus) {
		double perLevel = CMConfig.SERVER_SPEC.isLoaded() ? bonus.get() : bonus.getDefault();
		return (float) (1 + perLevel * Math.max(0, level));
	}

	/** A book of each, of a random level, priced like the Librarian's own enchanted books. */
	public static void addTrades(VillagerTradesEvent event) {
		if (event.getType() != VillagerProfession.LIBRARIAN)
			return;
		for (ResourceKey<Enchantment> key : List.of(BOUNTIFUL, FRUITFUL))
			event.getTrades().get(TRADE_LEVEL).add((trader, random) -> {
				Optional<Holder.Reference<Enchantment>> enchantment =
					trader.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(key);
				if (enchantment.isEmpty())
					return null;
				int level = 1 + random.nextInt(MAX_LEVEL);
				ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(enchantment.get(), level));
				int cost = Math.min(64, 2 + random.nextInt(5 + level * 10) + 3 * level);
				return new MerchantOffer(new ItemCost(Items.EMERALD, cost), Optional.of(new ItemCost(Items.BOOK)), book,
					12, 5 * level, 0.2f);
			});
	}
}
