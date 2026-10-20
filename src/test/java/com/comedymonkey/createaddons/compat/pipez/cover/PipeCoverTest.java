package com.comedymonkey.createaddons.compat.pipez.cover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.comedymonkey.createaddons.registry.ModBlocks;
import com.comedymonkey.createaddons.registry.ModItems;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

class PipeCoverTest {
	private static final BlockState AIR = Blocks.AIR.defaultBlockState();
	private static final BlockState STONE = Blocks.STONE.defaultBlockState();

	@Test
	void noneIsASnugCoverWithoutMaterial() {
		assertEquals(PipeCover.Kind.SNUG, PipeCover.NONE.kind());
		assertFalse(PipeCover.NONE.hasMaterial());
	}

	@Test
	void hasMaterialOnlyForNonAirBlocks() {
		assertFalse(new PipeCover(PipeCover.Kind.FULL, AIR).hasMaterial());
		assertTrue(new PipeCover(PipeCover.Kind.FULL, STONE).hasMaterial());
	}

	@Test
	void withMaterialKeepsTheKind() {
		PipeCover full = new PipeCover(PipeCover.Kind.FULL, AIR);
		PipeCover dressed = full.withMaterial(STONE);
		assertEquals(PipeCover.Kind.FULL, dressed.kind());
		assertSame(STONE, dressed.material());
		// Records are immutable: the original is untouched
		assertSame(AIR, full.material());
		assertEquals(full, dressed.withMaterial(AIR));
	}

	@Test
	void appearanceIsTheMaterialOrTheBlankCover() {
		assertSame(STONE, new PipeCover(PipeCover.Kind.SNUG, STONE).appearance());
		assertSame(ModBlocks.PIPE_COVER_BLANK.get().defaultBlockState(),
			new PipeCover(PipeCover.Kind.SNUG, AIR).appearance());
	}

	@Test
	void opaqueFollowsTheAppearance() {
		assertTrue(new PipeCover(PipeCover.Kind.FULL, STONE).isOpaque());
		// Glass doesn't occlude, so the pipe inside still has to be drawn
		assertFalse(new PipeCover(PipeCover.Kind.FULL, Blocks.GLASS.defaultBlockState()).isOpaque());
		// The plain cover is a solid block
		assertTrue(new PipeCover(PipeCover.Kind.SNUG, AIR).isOpaque());
	}

	@Test
	void kindSerializedNames() {
		assertEquals("snug", PipeCover.Kind.SNUG.getSerializedName());
		assertEquals("full", PipeCover.Kind.FULL.getSerializedName());
	}

	@Test
	void coverItemsCarryTheirKind() {
		assertEquals(PipeCover.Kind.SNUG, ModItems.SNUG_PIPEZ_COVER.get().kind());
		assertEquals(PipeCover.Kind.FULL, ModItems.FULL_PIPEZ_COVER.get().kind());
	}

	@Test
	void codecRoundTripsEveryKindWithAndWithoutMaterial() {
		BlockState log = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
		for (PipeCover.Kind kind : PipeCover.Kind.values())
			for (BlockState material : new BlockState[] {AIR, STONE, log}) {
				PipeCover cover = new PipeCover(kind, material);
				assertEquals(cover, nbtRoundTrip(cover), cover.toString());
			}
	}

	@Test
	void codecWritesTheKindByName() {
		Tag tag = PipeCover.CODEC.encodeStart(NbtOps.INSTANCE, new PipeCover(PipeCover.Kind.FULL, STONE)).getOrThrow();
		assertEquals("full", ((CompoundTag) tag).getString("kind"));
	}

	@Test
	void codecDefaultsAMissingMaterialToAir() {
		CompoundTag tag = new CompoundTag();
		tag.putString("kind", "snug");
		PipeCover cover = PipeCover.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
		assertEquals(new PipeCover(PipeCover.Kind.SNUG, AIR), cover);
	}

	@Test
	void codecKeepsTheCoverWhenItsMaterialIsUnknown() {
		CompoundTag material = new CompoundTag();
		material.putString("Name", "some_removed_mod:fancy_block");
		CompoundTag tag = new CompoundTag();
		tag.putString("kind", "full");
		tag.put("material", material);
		var result = PipeCover.CODEC.parse(NbtOps.INSTANCE, tag);
		assertTrue(result.isSuccess(), () -> "decode failed: " + result);
		assertEquals(new PipeCover(PipeCover.Kind.FULL, AIR), result.getOrThrow());
	}

	@Test
	void streamCodecRoundTrips() {
		BlockState log = Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z);
		for (PipeCover.Kind kind : PipeCover.Kind.values())
			for (BlockState material : new BlockState[] {AIR, STONE, log}) {
				PipeCover cover = new PipeCover(kind, material);
				ByteBuf buf = Unpooled.buffer();
				PipeCover.STREAM_CODEC.encode(buf, cover);
				assertEquals(cover, PipeCover.STREAM_CODEC.decode(buf));
				assertEquals(0, buf.readableBytes(), "trailing bytes");
			}
	}

	private static PipeCover nbtRoundTrip(PipeCover cover) {
		Tag tag = PipeCover.CODEC.encodeStart(NbtOps.INSTANCE, cover).getOrThrow();
		return PipeCover.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
	}
}
