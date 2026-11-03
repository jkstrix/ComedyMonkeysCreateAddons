package com.comedymonkey.createaddons.content.jetpack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A player's Clockwork Jetpack toggles, kept on the player like Create Jetpack's (jetpack spec D8, D10, D14): the engine
 * (G, on by default), hover (H, off) and the wings (J, flight mode). Saved, kept through death, and sent to everyone who
 * can see the player, since the wings show on their back.
 */
public record JetpackState(boolean engine, boolean hover, boolean wings) {
	public static final JetpackState DEFAULT = new JetpackState(true, false, false);

	public static final Codec<JetpackState> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.BOOL.optionalFieldOf("engine", true).forGetter(JetpackState::engine),
		Codec.BOOL.optionalFieldOf("hover", false).forGetter(JetpackState::hover),
		Codec.BOOL.optionalFieldOf("wings", false).forGetter(JetpackState::wings)
	).apply(i, JetpackState::new));

	public static final StreamCodec<ByteBuf, JetpackState> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, JetpackState::engine,
		ByteBufCodecs.BOOL, JetpackState::hover,
		ByteBufCodecs.BOOL, JetpackState::wings,
		JetpackState::new);

	public JetpackState withEngine(boolean engine) {
		return new JetpackState(engine, hover, wings);
	}

	public JetpackState withHover(boolean hover) {
		return new JetpackState(engine, hover, wings);
	}

	public JetpackState withWings(boolean wings) {
		return new JetpackState(engine, hover, wings);
	}
}
