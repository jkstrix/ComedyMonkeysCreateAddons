package com.comedymonkey.createaddons.content.line;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Where a span being hung starts: a Line Post head and which of its insulators. */
public record SpanStart(BlockPos pos, int insulator) {
	public static final Codec<SpanStart> CODEC = RecordCodecBuilder.create(i -> i.group(
		BlockPos.CODEC.fieldOf("pos").forGetter(SpanStart::pos),
		Codec.INT.fieldOf("insulator").forGetter(SpanStart::insulator)
	).apply(i, SpanStart::new));

	public static final StreamCodec<ByteBuf, SpanStart> STREAM_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, SpanStart::pos,
		ByteBufCodecs.VAR_INT, SpanStart::insulator,
		SpanStart::new);
}
