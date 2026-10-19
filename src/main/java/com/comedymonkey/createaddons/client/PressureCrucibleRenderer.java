package com.comedymonkey.createaddons.client;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.crucible.PressureCrucibleBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Draws the moving parts of a Pressure Crucible: the shaft end turning in the back wall, the press plate (which starts
 * at the top, lowers over exactly the time one item takes, then eases back up) on a pole reaching up into the
 * housing, and the dust and results in the bowl. Kinetic blocks normally get a Flywheel visual; without one Flywheel
 * falls back to this renderer, which also covers Ponder and contraptions.
 */
public class PressureCrucibleRenderer extends SafeBlockEntityRenderer<PressureCrucibleBlockEntity> {
	public static final ModelResourceLocation RAM = model("block/pressure_crucible_ram");
	public static final ModelResourceLocation ROD = model("block/pressure_crucible_rod");
	public static final ModelResourceLocation SHAFT = model("block/pressure_crucible_shaft");

	/** Underside of the press plate at the top and bottom of its travel, and the housing it hangs from, in blocks. */
	private static final float PLATE_TOP = 11 / 16f;
	private static final float PLATE_BOTTOM = 4.5f / 16;
	private static final float PLATE_THICKNESS = 2 / 16f;
	private static final float HOUSING = 13 / 16f;

	public PressureCrucibleRenderer(BlockEntityRendererProvider.Context context) {}

	private static ModelResourceLocation model(String path) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource(path));
	}

	@Override
	protected void renderSafe(PressureCrucibleBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
							  int light, int overlay) {
		BlockState state = be.getBlockState();
		if (!state.hasProperty(HorizontalKineticBlock.HORIZONTAL_FACING))
			return;
		Direction back = state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
		VertexConsumer solid = buffer.getBuffer(RenderType.solid());

		// Shaft end, turning with the network. The model points south; turn it to the back, then spin it about the
		// world axis exactly as Create spins shafts so it lines up with the shaft it connects to.
		Direction.Axis axis = back.getAxis();
		float angle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), axis);
		ms.pushPose();
		ms.translate(0.5, 0.5, 0.5);
		ms.mulPose(axis == Direction.Axis.X ? Axis.XP.rotation(angle) : Axis.ZP.rotation(angle));
		ms.mulPose(Axis.YP.rotationDegrees(-back.toYRot()));
		ms.translate(-0.5, -0.5, -0.5);
		renderModel(SHAFT, state, ms, solid, light, overlay);
		ms.popPose();

		// Press plate: top of travel when idle, lowering steadily as the current item is pressed
		float plateY = PLATE_TOP - be.getPlate(partialTicks) * (PLATE_TOP - PLATE_BOTTOM);
		ms.pushPose();
		ms.translate(0, plateY, 0);
		renderModel(RAM, state, ms, solid, light, overlay);
		ms.popPose();

		// Pole from the plate up into the housing
		float rodBottom = plateY + PLATE_THICKNESS;
		ms.pushPose();
		ms.translate(0, rodBottom, 0);
		ms.scale(1, HOUSING - rodBottom + 0.5f / 16, 1);
		renderModel(ROD, state, ms, solid, light, overlay);
		ms.popPose();

		// Contents lying in the bowl, under the plate
		ItemStack input = be.getInventory().getStackInSlot(PressureCrucibleBlockEntity.INPUT);
		ItemStack output = be.getInventory().getStackInSlot(PressureCrucibleBlockEntity.OUTPUT);
		int layers = input.isEmpty() ? 0 : Math.min(4, 1 + input.getCount() / 16);
		for (int i = 0; i < layers; i++)
			renderFlat(be, input, ms, buffer, light, overlay, 0.5f + (i % 2 == 0 ? -0.03f : 0.03f),
				0.5f + (i < 2 ? -0.03f : 0.03f), 4.05f / 16 + i * 0.008f, 37 * i);
		if (!output.isEmpty())
			renderFlat(be, output, ms, buffer, light, overlay, 0.5f, 0.5f, 4.05f / 16 + layers * 0.008f + 0.008f, 15);
	}

	private static void renderModel(ModelResourceLocation location, BlockState state, PoseStack ms, VertexConsumer buffer,
									int light, int overlay) {
		Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(ms.last(), buffer, state,
			Minecraft.getInstance().getModelManager().getModel(location), 1, 1, 1, light, overlay, ModelData.EMPTY,
			RenderType.solid());
	}

	private static void renderFlat(PressureCrucibleBlockEntity be, ItemStack stack, PoseStack ms, MultiBufferSource buffer,
								   int light, int overlay, float x, float z, float y, float spin) {
		ms.pushPose();
		ms.translate(x, y, z);
		ms.mulPose(Axis.YP.rotationDegrees(spin));
		ms.mulPose(Axis.XP.rotationDegrees(90));
		// Small enough to fit under the press plate
		ms.scale(0.3f, 0.3f, 0.3f);
		Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, ms,
			buffer, be.getLevel(), 0);
		ms.popPose();
	}
}
