package com.comedymonkey.createaddons.compat.pipez.cover;

import com.comedymonkey.createaddons.registry.ModBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A cover fitted to a Pipez pipe: snug around the pipe or a full block, dressed in a block's look like a Create
 * copycat. With no material yet it shows the plain cover. Kept on the pipe's block entity as a data attachment.
 */
public record PipeCover(Kind kind, BlockState material) {
	/** Never stored: the attachment's required default. */
	public static final PipeCover NONE = new PipeCover(Kind.SNUG, Blocks.AIR.defaultBlockState());

	public static final Codec<PipeCover> CODEC = RecordCodecBuilder.create(i -> i.group(
		Kind.CODEC.fieldOf("kind").forGetter(PipeCover::kind),
		// Lenient: a material from a mod that has since been removed leaves a plain cover rather than losing it
		BlockState.CODEC.lenientOptionalFieldOf("material", Blocks.AIR.defaultBlockState()).forGetter(PipeCover::material)
	).apply(i, PipeCover::new));

	public static final StreamCodec<ByteBuf, PipeCover> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.idMapper(i -> Kind.values()[i], Kind::ordinal), PipeCover::kind,
		ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), PipeCover::material,
		PipeCover::new);

	public boolean hasMaterial() {
		return !material.isAir();
	}

	/** What it looks like: its material, or the plain cover. */
	public BlockState appearance() {
		return hasMaterial() ? material : ModBlocks.PIPE_COVER_BLANK.get().defaultBlockState();
	}

	/** Hides the pipe entirely, so the pipe needn't be drawn underneath. */
	public boolean isOpaque() {
		return appearance().canOcclude();
	}

	public PipeCover withMaterial(BlockState material) {
		return new PipeCover(kind, material);
	}

	public enum Kind implements StringRepresentable {
		/** Hugs the pipe, a pixel proud of it all round. */
		SNUG,
		/** Fills the whole block. */
		FULL;

		public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

		@Override
		public String getSerializedName() {
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}
}
