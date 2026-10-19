package com.comedymonkey.createaddons.client;

import com.comedymonkey.createaddons.content.wellspring.WellspringBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Renders the stored water or lava inside the casing. The column spans exactly the height of the glass windows,
 * so the visible level matches the fill percentage and the gauge notches on the frame (25/50/75%).
 */
public class WellspringRenderer implements BlockEntityRenderer<WellspringBlockEntity> {
	private static final float INSET = 2.05f / 16f;
	private static final float FLOOR = 3f / 16f;
	private static final float CEILING = 13f / 16f;

	public WellspringRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public void render(WellspringBlockEntity be, float partialTicks, PoseStack poseStack, MultiBufferSource buffers,
					   int light, int overlay) {
		FluidStack fluid = be.getTank().getFluid();
		if (fluid.isEmpty())
			return;

		float fill = be.getRenderedFill(partialTicks);
		if (fill <= 0.01f)
			return;

		float top = FLOOR + (CEILING - FLOOR) * Math.min(fill, 1f);
		NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluid, INSET, FLOOR, INSET, 1 - INSET, top, 1 - INSET,
			buffers, poseStack, light, false, true);
	}
}
