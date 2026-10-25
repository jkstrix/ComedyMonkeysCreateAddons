package com.comedymonkey.createaddons.client;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.parts.PartSink;
import com.comedymonkey.createaddons.content.wellspring.WellspringBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;

import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Renders the stored fluid inside the tank. The column spans exactly the height of the glass windows, so the visible
 * level matches the fill percentage and the ticks on the corner posts (25/50/75%). Also stands a copper canister in
 * each capacity socket on the lid, one per fitted Capacity Upgrade (the generation modules are in the block models).
 */
public class WellspringRenderer implements BlockEntityRenderer<WellspringBlockEntity> {
	private static final float INSET = 2.05f / 16f;
	private static final float FLOOR = 3f / 16f;
	private static final float CEILING = 13f / 16f;
	/** One canister, modelled in the first capacity socket; the next sockets are 5 px further along x. */
	public static final ModelResourceLocation CANISTER =
		ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/wellspring_canister"));
	private static final float SOCKET_STEP = 5f / 16f;
	private static final int SOCKETS = 3;

	public WellspringRenderer(BlockEntityRendererProvider.Context context) {}

	/** The canister isn't referenced by any blockstate, so it's loaded explicitly. */
	public static void registerModels(ModelEvent.RegisterAdditional event) {
		event.register(CANISTER);
	}

	@Override
	public void render(WellspringBlockEntity be, float partialTicks, PoseStack poseStack, MultiBufferSource buffers,
					   int light, int overlay) {
		parts(be, partialTicks, poseStack, PartSink.forRenderer(be, buffers, overlay), light);

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

	/** The canisters are its only part models (for this renderer, or Flywheel's visual); the fluid stays here. */
	public static void parts(WellspringBlockEntity be, float partialTicks, PoseStack ms, PartSink sink, int light) {
		int count = Math.min(be.getCapacityUpgradeCount(), SOCKETS);
		for (int i = 0; i < count; i++) {
			ms.pushPose();
			ms.translate(i * SOCKET_STEP, 0, 0);
			sink.part(CANISTER, ms, light);
			ms.popPose();
		}
	}
}
