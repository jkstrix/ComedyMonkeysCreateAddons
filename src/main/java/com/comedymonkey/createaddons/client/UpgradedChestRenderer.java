package com.comedymonkey.createaddons.client;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.content.chest.ChestTier;
import com.comedymonkey.createaddons.content.chest.UpgradedChestBlock;
import com.comedymonkey.createaddons.content.chest.UpgradedChestBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Animates an upgraded chest's lid (and latch) while it is open, hinged at the back like a vanilla chest lid. When the
 * chest is closed the lid is part of the block model instead, so it also looks right anywhere block entity renderers
 * don't run (e.g. a chest carried with Carry On).
 */
public class UpgradedChestRenderer implements BlockEntityRenderer<UpgradedChestBlockEntity> {
	/** Hinge: top-back edge of the body, in block pixels (model space, front facing north). */
	private static final float HINGE_Y = 10 / 16f;
	private static final float HINGE_Z = 15 / 16f;

	public UpgradedChestRenderer(BlockEntityRendererProvider.Context context) {}

	public static ModelResourceLocation lidModel(ChestTier tier, ChestType type) {
		return ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource(
			"block/upgraded_chest_" + tier.getSerializedName() + "_" + type.getSerializedName() + "_lid"));
	}

	@Override
	public void render(UpgradedChestBlockEntity chest, float partialTicks, PoseStack poseStack, MultiBufferSource buffers,
					   int light, int overlay) {
		BlockState state = chest.getBlockState();
		// Closed chests draw their lid as part of the block model
		if (!(state.getBlock() instanceof UpgradedChestBlock) || !state.getValue(UpgradedChestBlock.OPEN))
			return;

		// Same easing as vanilla chests
		float openness = 1 - chest.getOpenness(partialTicks);
		openness = 1 - openness * openness * openness;

		BakedModel lid = Minecraft.getInstance().getModelManager()
			.getModel(lidModel(state.getValue(UpgradedChestBlock.TIER), state.getValue(UpgradedChestBlock.TYPE)));

		poseStack.pushPose();
		// Match the blockstate's y rotation (models are authored facing north)
		poseStack.translate(0.5, 0, 0.5);
		poseStack.mulPose(Axis.YP.rotationDegrees(-(state.getValue(UpgradedChestBlock.FACING).toYRot() + 180)));
		poseStack.translate(-0.5, 0, -0.5);
		if (openness > 0) {
			poseStack.translate(0, HINGE_Y, HINGE_Z);
			poseStack.mulPose(Axis.XP.rotationDegrees(openness * 90));
			poseStack.translate(0, -HINGE_Y, -HINGE_Z);
		}
		RenderType renderType = RenderType.cutout();
		Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(poseStack.last(),
			buffers.getBuffer(renderType), state, lid, 1, 1, 1, light, overlay, ModelData.EMPTY, renderType);
		poseStack.popPose();
	}
}
