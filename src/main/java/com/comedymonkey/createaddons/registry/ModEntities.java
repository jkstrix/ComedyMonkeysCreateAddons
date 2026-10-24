package com.comedymonkey.createaddons.registry;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.tradebot.TradeBotEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
	public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
		DeferredRegister.create(Registries.ENTITY_TYPE, ComedyMonkeysCreateAddons.MOD_ID);

	/** 0.6 wide like a villager (his chassis is 10 px, his arms swing wider), so he fits through an open door; 1.5 blocks tall.
	 * Tracked from 10 chunks, with position updates every tick for smooth hops. */
	public static final DeferredHolder<EntityType<?>, EntityType<TradeBotEntity>> TRADE_BOT = ENTITY_TYPES.register("trade_bot",
		() -> EntityType.Builder.of(TradeBotEntity::new, MobCategory.MISC).sized(0.6f, 1.5f).clientTrackingRange(10)
			.updateInterval(1).build("trade_bot"));

	private ModEntities() {}

	public static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(TRADE_BOT.get(), TradeBotEntity.createAttributes().build());
	}
}
