package com.comedymonkey.createaddons.content.pen;

import java.util.UUID;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.registry.ModItems;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Catching animals for the Animal Pen: use a Capture Bottle on any animal (anything that breeds, modded ones too)
 * to get it as a Bottled Animal. Done in the interact event so it comes before the animal's own use (a horse would
 * otherwise be mounted). Someone else's tamed animal (pets, and horses, donkeys, llamas and camels) can't be taken,
 * nor one that is being ridden, and the entity type tag {@code comedymonkeysaddons:pen_blacklist} rules out any others.
 */
public final class AnimalCapture {
	public static final TagKey<EntityType<?>> BLACKLIST =
		TagKey.create(Registries.ENTITY_TYPE, ComedyMonkeysCreateAddons.asResource("pen_blacklist"));

	private AnimalCapture() {}

	public static boolean canCapture(Player player, net.minecraft.world.entity.Entity target) {
		if (!(target instanceof Animal animal) || !animal.isAlive() || animal.getType().is(BLACKLIST) || animal.isVehicle())
			return false;
		// Horses and the like aren't TamableAnimals, but they have owners too
		UUID owner = animal instanceof OwnableEntity ownable ? ownable.getOwnerUUID() : null;
		return owner == null || owner.equals(player.getUUID());
	}

	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		Player player = event.getEntity();
		ItemStack held = event.getItemStack();
		if (!held.is(ModItems.CAPTURE_BOTTLE.get()) || !canCapture(player, event.getTarget()))
			return;
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
		if (event.getLevel().isClientSide)
			return;
		Animal animal = (Animal) event.getTarget();
		ItemStack bottle = BottledAnimalItem.of(animal);
		// A removed animal never ticks its lead again, so it would just vanish
		if (animal.isLeashed())
			animal.dropLeash(true, true);
		animal.discard();
		event.getLevel().playSound(null, animal.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1, 1);
		if (!player.getAbilities().instabuild)
			held.shrink(1);
		player.getInventory().placeItemBackInInventory(bottle);
	}
}
