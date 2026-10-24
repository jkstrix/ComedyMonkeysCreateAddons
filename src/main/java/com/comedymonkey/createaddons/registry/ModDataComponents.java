package com.comedymonkey.createaddons.registry;

import java.util.function.Supplier;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.tradebot.TradeBotData;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Data the mod keeps on its own items. */
public final class ModDataComponents {
	public static final DeferredRegister.DataComponents DATA_COMPONENTS =
		DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ComedyMonkeysCreateAddons.MOD_ID);

	/** On a packed-up Trade Bot: his charge, earnings, load, reputation and links. */
	public static final Supplier<DataComponentType<TradeBotData>> TRADE_BOT_DATA = DATA_COMPONENTS.registerComponentType("trade_bot",
		builder -> builder.persistent(TradeBotData.CODEC).networkSynchronized(TradeBotData.STREAM_CODEC));

	private ModDataComponents() {}
}
