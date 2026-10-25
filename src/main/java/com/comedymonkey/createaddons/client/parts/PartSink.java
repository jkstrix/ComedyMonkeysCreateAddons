package com.comedymonkey.createaddons.client.parts;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Where a machine's model parts go. Each renderer poses its parts once, in one method, and hands them to a sink: with
 * Flywheel on, {@link PartsVisual} turns them into instances moved on the GPU; without it (or where Flywheel can't
 * draw, such as Ponder and contraptions) {@link #screen} draws them as the renderer always did. Either way the poses
 * come from the same code, so the two can't drift apart.
 */
@FunctionalInterface
public interface PartSink {
	/** Draws {@code model} at the pose's current transform, lit at {@code light} (the block's, or full-bright). */
	void part(ModelResourceLocation model, PoseStack ms, int light, RenderType type);

	default void part(ModelResourceLocation model, PoseStack ms, int light) {
		part(model, ms, light, RenderType.cutout());
	}

	/** Draws nothing: Flywheel has these parts. */
	PartSink NONE = (model, ms, light, type) -> {};

	/** Draws straight to the screen, as a block entity renderer does. */
	static PartSink screen(MultiBufferSource buffers, BlockState state, int overlay) {
		return (model, ms, light, type) -> Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
			ms.last(), buffers.getBuffer(type), state, Minecraft.getInstance().getModelManager().getModel(model), 1, 1, 1,
			light, overlay, ModelData.EMPTY, type);
	}

	/** Where a renderer should send its parts: nowhere when Flywheel draws them, else the screen. */
	static PartSink forRenderer(BlockEntity be, MultiBufferSource buffers, int overlay) {
		return instanced(be) ? NONE : screen(buffers, be.getBlockState(), overlay);
	}

	/** True when Flywheel draws this block entity's parts (its visual exists), so its renderer leaves them out. */
	static boolean instanced(BlockEntity be) {
		return be.getLevel() != null && VisualizationManager.supportsVisualization(be.getLevel());
	}
}
