package com.comedymonkey.createaddons.registry;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.juggernaut.ClockworkJuggernaut;
import com.comedymonkey.createaddons.content.juggernaut.ThrownCog;
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

	/** The boss: 6 wide to match his shield pauldrons, 7 blocks tall (D20); fire and lava can't touch him (D50). */
	public static final DeferredHolder<EntityType<?>, EntityType<ClockworkJuggernaut>> CLOCKWORK_JUGGERNAUT = ENTITY_TYPES.register("clockwork_juggernaut",
		() -> EntityType.Builder.of(ClockworkJuggernaut::new, MobCategory.MONSTER).sized(6f, 7f).fireImmune().clientTrackingRange(10)
			.updateInterval(1).build("clockwork_juggernaut"));

	/** The cog he throws at a target he can't reach (D22). */
	public static final DeferredHolder<EntityType<?>, EntityType<ThrownCog>> THROWN_COG = ENTITY_TYPES.register("thrown_cog",
		() -> EntityType.Builder.<ThrownCog>of(ThrownCog::new, MobCategory.MISC).sized(1.2f, 1.2f).clientTrackingRange(6)
			.updateInterval(2).build("thrown_cog"));

	private ModEntities() {}

	public static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(TRADE_BOT.get(), TradeBotEntity.createAttributes().build());
		event.put(CLOCKWORK_JUGGERNAUT.get(), ClockworkJuggernaut.createAttributes().build());
	}
}
