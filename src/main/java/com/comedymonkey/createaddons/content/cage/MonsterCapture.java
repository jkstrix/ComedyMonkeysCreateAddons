package com.comedymonkey.createaddons.content.cage;

import com.comedymonkey.createaddons.registry.ModItems;
import com.comedymonkey.createaddons.registry.ModSounds;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Catching monsters in jars (D63): an empty Iron Jar catches any monster a cage takes (not the Wither), an empty
 * Netherite Jar the Wither, at any health (D22). The wrong catcher is refused with a hint naming the right one: a
 * Capture Bottle on a monster, a jar on an animal, an Iron Jar on the Wither.
 */
public final class MonsterCapture {
	private MonsterCapture() {}

	public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
		Player player = event.getEntity();
		ItemStack held = event.getItemStack();
		Entity target = event.getTarget();
		boolean monster = target instanceof Enemy && !(target instanceof Animal);
		if (held.is(ModItems.CAPTURE_BOTTLE.get()) && monster) {
			hint(event, player, "jar_needed");
			return;
		}
		if (!(held.getItem() instanceof MonsterJarItem jar) || MonsterJarItem.isFilled(held))
			return;
		if (target instanceof Animal) {
			hint(event, player, "bottle_needed");
			return;
		}
		if (!CageRules.isCageable(target) || target.isVehicle())
			return;
		if (!canCatch(jar, target)) {
			hint(event, player, jar.isNetherite() ? "iron_jar_needed" : "netherite_jar_needed");
			return;
		}
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
		if (!(event.getLevel() instanceof ServerLevel level))
			return;
		catchInto(level, player, event.getHand(), held, jar, target);
	}

	/** Whether this empty jar takes the monster: one a cage takes, not ridden, and the Wither only in a Netherite Jar. */
	public static boolean canCatch(MonsterJarItem jar, Entity target) {
		return !(target instanceof Animal) && CageRules.isCageable(target) && !target.isVehicle() && jar.holds(target.getType());
	}

	/** Takes the monster out of the world into a filled jar (by hand, a Deployer or a Dispenser). */
	public static ItemStack jar(ServerLevel level, MonsterJarItem jar, Entity target) {
		ItemStack filled = jar.fill(target);
		target.discard();
		level.playSound(null, target.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1, 0.8f);
		level.playSound(null, target.blockPosition(), ModSounds.cage("jar_catch"), SoundSource.PLAYERS, 1, 1);
		return filled;
	}

	/** Catches the monster into one of the held empty jars; shared with Deployers. */
	public static void catchInto(ServerLevel level, Player player, InteractionHand hand, ItemStack held, MonsterJarItem jar,
								 Entity target) {
		ItemStack filled = jar(level, jar, target);
		if (player.getAbilities().instabuild) {
			player.getInventory().placeItemBackInInventory(filled);
		} else if (held.getCount() == 1) {
			player.setItemInHand(hand, filled);
		} else {
			held.shrink(1);
			player.getInventory().placeItemBackInInventory(filled);
		}
	}

	private static void hint(PlayerInteractEvent.EntityInteract event, Player player, String key) {
		if (!event.getLevel().isClientSide)
			player.displayClientMessage(Component.translatable("comedymonkeysaddons.gui.monster_cage." + key), true);
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));
	}
}
