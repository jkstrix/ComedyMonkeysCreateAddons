package com.comedymonkey.createaddons.registry;

import java.util.function.Supplier;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.tradebot.TradeBotData;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.DyeColor;
import com.comedymonkey.createaddons.content.line.SpanStart;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Data the mod keeps on its own items. */
public final class ModDataComponents {
	public static final DeferredRegister.DataComponents DATA_COMPONENTS =
		DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ComedyMonkeysCreateAddons.MOD_ID);

	/** On a packed-up Trade Bot: his charge, earnings, load, reputation and links. */
	public static final Supplier<DataComponentType<TradeBotData>> TRADE_BOT_DATA = DATA_COMPONENTS.registerComponentType("trade_bot",
		builder -> builder.persistent(TradeBotData.CODEC).networkSynchronized(TradeBotData.STREAM_CODEC));

	/** On a Fluid Line or Power Line: its dye (none: its own material). */
	public static final Supplier<DataComponentType<DyeColor>> LINE_DYE = DATA_COMPONENTS.registerComponentType("line_dye",
		builder -> builder.persistent(DyeColor.CODEC).networkSynchronized(DyeColor.STREAM_CODEC));

	/** On Power Lines in hand while a span is being hung: the insulator it starts from. */
	public static final Supplier<DataComponentType<SpanStart>> SPAN_START = DATA_COMPONENTS.registerComponentType("span_start",
		builder -> builder.persistent(SpanStart.CODEC).networkSynchronized(SpanStart.STREAM_CODEC));

	/** On a Battery: the FE it holds. */
	public static final Supplier<DataComponentType<Integer>> BATTERY_CHARGE = DATA_COMPONENTS.registerComponentType("battery_charge",
		builder -> builder.persistent(com.mojang.serialization.Codec.INT).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

	private ModDataComponents() {}
}
