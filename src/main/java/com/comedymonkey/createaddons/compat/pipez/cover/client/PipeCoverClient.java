package com.comedymonkey.createaddons.compat.pipez.cover.client;

import com.comedymonkey.createaddons.compat.pipez.cover.PipeCover;
import com.comedymonkey.createaddons.compat.pipez.cover.PipeCoverShapes;
import com.comedymonkey.createaddons.network.PipeCoverPayload;
import com.comedymonkey.createaddons.registry.ModAttachments;

import de.maxhenkel.pipez.blocks.PipeBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** Client side of Pipez Covers: the sync packet, and tints (grass, leaves...) taken from the cover's block. */
public final class PipeCoverClient {
	private PipeCoverClient() {}

	public static void handle(PipeCoverPayload payload) {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null)
			return;
		BlockEntity be = level.getBlockEntity(payload.pos());
		if (be == null)
			return;
		payload.cover().ifPresentOrElse(cover -> be.setData(ModAttachments.PIPE_COVER, cover),
			() -> be.removeData(ModAttachments.PIPE_COVER));
		// Rebuild the chunk section so the pipe's model is drawn with (or without) its cover
		BlockState state = be.getBlockState();
		level.sendBlockUpdated(payload.pos(), state, state, Block.UPDATE_IMMEDIATE);
	}

	public static void registerColors(RegisterColorHandlersEvent.Block event) {
		Block[] pipes = BuiltInRegistries.BLOCK.stream().filter(b -> b instanceof PipeBlock).toArray(Block[]::new);
		event.register((state, level, pos, tint) -> {
			if (level == null || pos == null)
				return -1;
			PipeCover cover = PipeCoverShapes.cover(level, pos);
			if (cover == null || !cover.hasMaterial())
				return -1;
			return event.getBlockColors().getColor(cover.material(), level, pos, tint);
		}, pipes);
	}
}
