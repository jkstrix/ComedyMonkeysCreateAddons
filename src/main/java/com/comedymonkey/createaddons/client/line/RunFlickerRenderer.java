package com.comedymonkey.createaddons.client.line;

import com.comedymonkey.createaddons.client.LightRenderer;
import com.comedymonkey.createaddons.content.light.LightBlockEntity;
import com.comedymonkey.createaddons.content.light.LightStripBlock;
import com.comedymonkey.createaddons.content.light.RunLightBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * A starving Light Strip's or Edge Light's flicker (P3): for its second of flicker its baked model shows no diffuser
 * (RunLook "body"), and this draws it frame by frame, lit in its colour or the unlit glass, as a lamp's lens flickers.
 * The rest of the time it draws nothing.
 */
public class RunFlickerRenderer extends SafeBlockEntityRenderer<RunLightBlockEntity> {
	public RunFlickerRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	protected void renderSafe(RunLightBlockEntity be, float pt, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		if (be.getLevel() == null || be.phase() != LightBlockEntity.Phase.FADING)
			return;
		boolean lit = LightRenderer.lit(be, be.getLevel().getGameTime(), pt);
		BlockState state = be.getBlockState();
		ModelData data = state.getBlock() instanceof LightStripBlock ? StripModel.diffuser(be.getLevel(), be.getBlockPos(), state, lit)
			: EdgeModel.diffuser(be.getLevel(), be.getBlockPos(), state, lit);
		BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
		int c = lit ? LightRenderer.runGlow(be.dye(), be.step()) : 0xFFFFFF;
		float r = (c >> 16 & 255) / 255f, g = (c >> 8 & 255) / 255f, b = (c & 255) / 255f;
		VertexConsumer vc = buffer.getBuffer(RenderType.cutout());
		for (BakedQuad q : model.getQuads(state, null, RandomSource.create(42), data, null))
			vc.putBulkData(ms.last(), q, r, g, b, 1f, lit ? LightTexture.FULL_BRIGHT : light, overlay);
	}
}
