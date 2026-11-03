package com.comedymonkey.createaddons.content.jetpack;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.compat.curios.JetpackCurios;
import com.comedymonkey.createaddons.config.CMConfig;
import com.comedymonkey.createaddons.registry.ModSounds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

/**
 * A Clockwork Jetpack (jetpack spec): the Juggernaut's Mainspring on your back, worn in the Curios Jetpack slot or, without
 * Curios, the chest (D18). It's an armour piece only so the chest can take it: it gives a Copper Backtank's armour (the
 * Netherite one a Netherite Backtank's, D20), has no durability, and draws nothing of its own on the armour layer (the
 * jetpack renderer draws it). Its wind is the {@code jetpack_wind} component; Overwound gives it more (D34).
 */
public class ClockworkJetpackItem extends ArmorItem {
	/** A clear armour texture: the jetpack's own renderer draws it, wherever it's worn. */
	private static final ResourceLocation NO_ARMOUR = ComedyMonkeysCreateAddons.asResource("textures/models/armor/clockwork_jetpack_empty.png");

	public ClockworkJetpackItem(Holder<ArmorMaterial> material, Properties properties) {
		super(material, Type.CHESTPLATE, properties.stacksTo(1));
	}

	/** With Curios, putting it on goes to the Jetpack slot, so the chestplate stays on (D7); without, to the chest. */
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (ModList.get().isLoaded(ComedyMonkeysCreateAddons.CURIOS_ID) && JetpackCurios.tryEquip(player, stack))
			return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
		return super.use(level, player, hand);
	}

	@Override
	public Holder<SoundEvent> getEquipSound() {
		Holder<SoundEvent> equip = ModSounds.JETPACK.get("equip");
		return equip != null ? equip : super.getEquipSound();
	}

	@Override
	@Nullable
	public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
		return NO_ARMOUR;
	}

	/** No enchanting table: Overwound comes as a book (D37). */
	@Override
	public int getEnchantmentValue() {
		return 0;
	}

	/** Create Jetpack's enchantment list: the ones it refuses, or (not a blacklist) the only ones it takes (D25). */
	@Override
	public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
		if (CMConfig.SERVER_SPEC.isLoaded()) {
			String id = enchantment.unwrapKey().map(key -> key.location().toString()).orElse("");
			boolean listed = CMConfig.SERVER.jetpackEnchantments.get().contains(id);
			if (listed == CMConfig.SERVER.jetpackEnchantmentsIsBlacklist.get())
				return false;
		}
		return super.supportsEnchantment(stack, enchantment);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(13 * Jetpacks.fill(stack));
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return 0x8A949C; // the Mainspring's spring steel, as it shows through the porthole
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		super.appendHoverText(stack, context, tooltip, flag);
		String base = ComedyMonkeysCreateAddons.MOD_ID + ".clockwork_jetpack.";
		tooltip.add(Component.translatable(base + "wound", Math.round(Jetpacks.fill(stack) * 100)).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable(base + "controls", Component.keybind("key.jump"), Component.keybind("key.sneak"),
			Component.keybind("key.comedymonkeysaddons.jetpack_engine"), Component.keybind("key.comedymonkeysaddons.jetpack_hover"),
			Component.keybind("key.comedymonkeysaddons.jetpack_wings")).withStyle(ChatFormatting.DARK_GRAY));
	}
}
