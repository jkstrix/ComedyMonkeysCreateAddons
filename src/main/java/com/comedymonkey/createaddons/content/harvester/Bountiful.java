package com.comedymonkey.createaddons.content.harvester;

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
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

/**
 * The Bountiful enchantment (I to III), for the Auto Harvester only: each level makes its plant grow faster. It is
 * defined in data ({@code data/comedymonkeysaddons/enchantment/bountiful.json}), isn't offered by enchanting tables,
 * and comes as a book from Librarians: in their usual random book trades, and as an extra Apprentice trade.
 */
public final class Bountiful {
	public static final ResourceKey<Enchantment> KEY =
		ResourceKey.create(Registries.ENCHANTMENT, ComedyMonkeysCreateAddons.asResource("bountiful"));
	public static final int MAX_LEVEL = 3;
	/** Librarian level the extra trade is unlocked at (2 = Apprentice). */
	private static final int TRADE_LEVEL = 2;

	private Bountiful() {}

	public static int levelIn(ItemEnchantments enchantments) {
		for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet())
			if (entry.getKey().is(KEY))
				return entry.getIntValue();
		return 0;
	}

	/** How many times faster than normal a plant grows at this level. */
	public static float speedMultiplier(int level) {
		double bonus = CMConfig.SERVER_SPEC.isLoaded() ? CMConfig.SERVER.harvesterBountifulBonus.get()
			: CMConfig.SERVER.harvesterBountifulBonus.getDefault();
		return (float) (1 + bonus * Math.max(0, level));
	}

	/** A Bountiful book of a random level, priced like the Librarian's own enchanted books. */
	public static void addTrades(VillagerTradesEvent event) {
		if (event.getType() != VillagerProfession.LIBRARIAN)
			return;
		event.getTrades().get(TRADE_LEVEL).add((trader, random) -> {
			Optional<Holder.Reference<Enchantment>> enchantment =
				trader.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(KEY);
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
