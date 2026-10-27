package com.comedymonkey.createaddons.content.cage;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Monster Chow: what the cages' monsters eat (each new one eats its chow cost). People can eat it too, badly (D66): like
 * rotten flesh, with a 25% chance of a Monster Belch (a random monster's sound, which others hear) and a 5% chance of
 * Chow Breath (Nausea and a green puff).
 */
public class MonsterChowItem extends Item {
	/** Like rotten flesh: 4 hunger, and Hunger 80% of the time. */
	public static final FoodProperties FOOD = new FoodProperties.Builder().nutrition(4).saturationModifier(0.1f)
		.effect(new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.8f).build();
	private static final SoundEvent[] BELCHES = {SoundEvents.ZOMBIE_AMBIENT, SoundEvents.CREEPER_PRIMED, SoundEvents.GHAST_AMBIENT,
		SoundEvents.SKELETON_AMBIENT, SoundEvents.SPIDER_AMBIENT, SoundEvents.HUSK_AMBIENT, SoundEvents.WITCH_CELEBRATE};

	public MonsterChowItem(Properties properties) {
		super(properties.food(FOOD));
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
		if (level instanceof ServerLevel server) {
			float roll = server.random.nextFloat();
			if (roll < 0.25f) {
				SoundEvent belch = BELCHES[server.random.nextInt(BELCHES.length)];
				server.playSound(null, eater.getX(), eater.getY(), eater.getZ(), belch, SoundSource.PLAYERS, 1,
					0.9f + 0.2f * server.random.nextFloat());
			} else if (roll < 0.30f) {
				eater.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
				server.playSound(null, eater.getX(), eater.getY(), eater.getZ(), com.comedymonkey.createaddons.registry.ModSounds.cage("chow_breath"),
					SoundSource.PLAYERS, 1, 1);
				Vec3 mouth = eater.getEyePosition().add(eater.getLookAngle().scale(0.4));
				server.sendParticles(ParticleTypes.SNEEZE, mouth.x, mouth.y, mouth.z, 12, 0.1, 0.1, 0.1, 0.02);
			}
		}
		return super.finishUsingItem(stack, level, eater);
	}
}
