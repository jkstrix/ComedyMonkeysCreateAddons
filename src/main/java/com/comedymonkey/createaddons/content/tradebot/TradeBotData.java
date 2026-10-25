package com.comedymonkey.createaddons.content.tradebot;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Everything a packed-up Trade Bot carries in his item (design/specs/trade-bot.md D27): his charge, what he earned
 * and was carrying, his reputation with each villager (and how long ago, in his own active ticks, he last traded with
 * each), and his station's links (relative to the station, so they still point the right way if he's put back where he
 * was).
 */
public record TradeBotData(int charge, long emeralds, int xpBank, Map<UUID, Integer> reputation, ItemContainerContents load,
	List<BlockPos> inputs, Optional<BlockPos> output, Map<UUID, Long> tradedAgo) {

	public static final TradeBotData EMPTY = new TradeBotData(0, 0, 0, Map.of(), ItemContainerContents.EMPTY, List.of(), Optional.empty());

	public static final Codec<TradeBotData> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.optionalFieldOf("charge", 0).forGetter(TradeBotData::charge),
		Codec.LONG.optionalFieldOf("emeralds", 0L).forGetter(TradeBotData::emeralds),
		Codec.INT.optionalFieldOf("xp", 0).forGetter(TradeBotData::xpBank),
		Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT).optionalFieldOf("reputation", Map.of()).forGetter(TradeBotData::reputation),
		ItemContainerContents.CODEC.optionalFieldOf("load", ItemContainerContents.EMPTY).forGetter(TradeBotData::load),
		BlockPos.CODEC.listOf().optionalFieldOf("inputs", List.of()).forGetter(TradeBotData::inputs),
		BlockPos.CODEC.optionalFieldOf("output").forGetter(TradeBotData::output),
		Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG).optionalFieldOf("traded_ago", Map.of()).forGetter(TradeBotData::tradedAgo)
	).apply(i, TradeBotData::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, TradeBotData> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

	/** Without dates: each villager's week starts when he's next unpacked. */
	public TradeBotData(int charge, long emeralds, int xpBank, Map<UUID, Integer> reputation, ItemContainerContents load,
		List<BlockPos> inputs, Optional<BlockPos> output) {
		this(charge, emeralds, xpBank, reputation, load, inputs, output, Map.of());
	}

	public TradeBotData {
		reputation = Map.copyOf(reputation);
		inputs = List.copyOf(inputs);
		tradedAgo = Map.copyOf(tradedAgo);
	}
}
