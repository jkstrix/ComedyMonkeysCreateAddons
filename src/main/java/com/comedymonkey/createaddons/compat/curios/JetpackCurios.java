package com.comedymonkey.createaddons.compat.curios;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.jetpack.Jetpacks;
import com.comedymonkey.createaddons.registry.ModItems;
import com.comedymonkey.createaddons.registry.ModSounds;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

/**
 * The Curios Jetpack slot (jetpack spec D7, D21, D22): one slot, so a jetpack and a back-slot backpack are worn together.
 * It takes the items in {@code curios:jetpack}: the two Clockwork Jetpacks, which give their chest armour from the slot
 * (D20). Never with a backtank or Create's jetpack in the chest (D23). Only loaded with Curios installed.
 */
public final class JetpackCurios {
	public static final String SLOT = "jetpack";
	/** Curios' own back slot, where Sophisticated Backpacks and the like go. */
	public static final String BACK = "back";

	private JetpackCurios() {}

	public static void init() {
		ICurioItem curio = new ICurioItem() {
			@Override
			public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slot, ResourceLocation id, ItemStack stack) {
				Multimap<Holder<Attribute>, AttributeModifier> modifiers = HashMultimap.create();
				if (SLOT.equals(slot.identifier()))
					chestArmour(stack.getItem(), id, modifiers);
				return modifiers;
			}

			@Override
			public ICurio.SoundInfo getEquipSound(SlotContext slot, ItemStack stack) {
				SoundEvent sound = ModSounds.jetpack("equip");
				return new ICurio.SoundInfo(sound != null ? sound : SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1, 1);
			}

			@Override
			public boolean canEquipFromUse(SlotContext slot, ItemStack stack) {
				return true;
			}

			@Override
			public boolean canEquip(SlotContext slot, ItemStack stack) {
				return !Jetpacks.isBacktank(slot.entity().getItemBySlot(EquipmentSlot.CHEST));
			}
		};
		CuriosApi.registerCurio(ModItems.CLOCKWORK_JETPACK.get(), curio);
		CuriosApi.registerCurio(ModItems.NETHERITE_CLOCKWORK_JETPACK.get(), curio);
	}

	/** An armour item's chest modifiers, under ids of this slot's own (so a chestplate's don't clash with them). */
	private static void chestArmour(Item item, ResourceLocation id, Multimap<Holder<Attribute>, AttributeModifier> into) {
		if (!(item instanceof ArmorItem armour))
			return;
		armour.getDefaultAttributeModifiers().modifiers().forEach(entry -> {
			if (entry.slot() == EquipmentSlotGroup.CHEST || entry.slot() == EquipmentSlotGroup.ARMOR || entry.slot() == EquipmentSlotGroup.ANY)
				into.put(entry.attribute(), new AttributeModifier(id.withSuffix("/" + entry.modifier().id().getPath()),
					entry.modifier().amount(), entry.modifier().operation()));
		});
	}

	/** The Clockwork Jetpack in this entity's Jetpack slot, or empty. */
	public static ItemStack findJetpack(LivingEntity entity) {
		return CuriosApi.getCuriosInventory(entity).flatMap(inventory -> inventory.getStacksHandler(SLOT)).map(handler -> {
			for (int i = 0; i < handler.getSlots(); i++) {
				ItemStack stack = handler.getStacks().getStackInSlot(i);
				if (Jetpacks.isJetpack(stack))
					return stack;
			}
			return ItemStack.EMPTY;
		}).orElse(ItemStack.EMPTY);
	}

	/** Takes the Clockwork Jetpack out of this entity's Jetpack slot; empty if there's none. */
	public static ItemStack takeJetpack(LivingEntity entity) {
		return CuriosApi.getCuriosInventory(entity).flatMap(inventory -> inventory.getStacksHandler(SLOT)).map(handler -> {
			for (int i = 0; i < handler.getSlots(); i++) {
				ItemStack stack = handler.getStacks().getStackInSlot(i);
				if (Jetpacks.isJetpack(stack)) {
					handler.getStacks().setStackInSlot(i, ItemStack.EMPTY);
					return stack;
				}
			}
			return ItemStack.EMPTY;
		}).orElse(ItemStack.EMPTY);
	}

	/** Puts the jetpack in hand into an empty Jetpack slot; false if there's none free, or a backtank's worn (D23). */
	public static boolean tryEquip(Player player, ItemStack stack) {
		if (Jetpacks.isBacktank(player.getItemBySlot(EquipmentSlot.CHEST)))
			return false;
		return CuriosApi.getCuriosInventory(player).flatMap(inventory -> inventory.getStacksHandler(SLOT)).map(handler -> {
			for (int i = 0; i < handler.getSlots(); i++)
				if (handler.getStacks().getStackInSlot(i).isEmpty()) {
					if (!player.level().isClientSide()) {
						handler.getStacks().setStackInSlot(i, stack.copyWithCount(1));
						SoundEvent sound = ModSounds.jetpack("equip");
						if (sound != null)
							player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, player.getSoundSource(), 1, 1);
					}
					stack.shrink(1);
					return true;
				}
			return false;
		}).orElse(false);
	}

	/** Whether something's shown in Curios' back slot (a backpack, not hidden): the jetpack is then built into it (pack mode). */
	public static boolean wearsOnBack(LivingEntity entity) {
		return CuriosApi.getCuriosInventory(entity).flatMap(inventory -> inventory.getStacksHandler(BACK)).map(JetpackCurios::anyWorn).orElse(false);
	}

	private static boolean anyWorn(ICurioStacksHandler handler) {
		for (int i = 0; i < handler.getSlots(); i++)
			if (!handler.getStacks().getStackInSlot(i).isEmpty() && (i >= handler.getRenders().size() || handler.getRenders().get(i)))
				return true;
		return false;
	}

	static String modId() {
		return ComedyMonkeysCreateAddons.MOD_ID;
	}
}
