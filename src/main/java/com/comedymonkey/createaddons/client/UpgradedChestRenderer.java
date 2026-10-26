package com.comedymonkey.createaddons.client;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.parts.PartSink;
import com.comedymonkey.createaddons.content.chest.ChestTier;
import com.comedymonkey.createaddons.content.chest.UpgradedChestBlock;
import com.comedymonkey.createaddons.content.chest.UpgradedChestBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * Animates an upgraded chest's lid (and latch) while it is open, hinged at the back like a vanilla chest lid. When the
 * chest is closed the lid is part of the block model instead, so it also looks right anywhere block entity renderers
 * don't run (e.g. a chest carried with Carry On). The lid is a part model: with Flywheel on, a closed chest's visual
 * does nothing a frame, and an open one's moves the lid instance (Flywheel makes the visual again when it opens or
 * closes, a block state change; see {@link com.comedymonkey.createaddons.client.parts.MachineVisuals}).
 */
public class UpgradedChestRenderer implements BlockEntityRenderer<UpgradedChestBlockEntity> {
	/** Hinge: top-back edge of the body, in block pixels (model space, front facing north). */
	private static final float HINGE_Y = 10 / 16f;
	private static final float HINGE_Z = 15 / 16f;

	/** Every tier's lid for each chest type, by their ordinals, named once. */
	private static final ModelResourceLocation[][] LIDS = new ModelResourceLocation[ChestTier.values().length][ChestType.values().length];

	static {
		for (ChestTier tier : ChestTier.values())
			for (ChestType type : ChestType.values())
				LIDS[tier.ordinal()][type.ordinal()] = ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource(
					"block/upgraded_chest_" + tier.getSerializedName() + "_" + type.getSerializedName() + "_lid"));
	}

	public UpgradedChestRenderer(BlockEntityRendererProvider.Context context) {}

	public static ModelResourceLocation lidModel(ChestTier tier, ChestType type) {
		return LIDS[tier.ordinal()][type.ordinal()];
	}

	@Override
	public void render(UpgradedChestBlockEntity chest, float partialTicks, PoseStack poseStack, MultiBufferSource buffers,
					   int light, int overlay) {
		if (!PartSink.instanced(chest))
			parts(chest, partialTicks, poseStack, PartSink.screen(buffers, chest.getBlockState(), overlay), light);
	}

	/** Whether an open lid needs drawing (closed, it's part of the block model). */
	public static boolean open(UpgradedChestBlockEntity chest) {
		BlockState state = chest.getBlockState();
		return state.getBlock() instanceof UpgradedChestBlock && state.getValue(UpgradedChestBlock.OPEN);
	}

	public static void parts(UpgradedChestBlockEntity chest, float partialTicks, PoseStack poseStack, PartSink sink, int light) {
		BlockState state = chest.getBlockState();
		// Closed chests draw their lid as part of the block model
		if (!(state.getBlock() instanceof UpgradedChestBlock) || !state.getValue(UpgradedChestBlock.OPEN))
			return;

		// Same easing as vanilla chests
		float openness = 1 - chest.getOpenness(partialTicks);
		openness = 1 - openness * openness * openness;

		ModelResourceLocation lid = lidModel(state.getValue(UpgradedChestBlock.TIER), state.getValue(UpgradedChestBlock.TYPE));

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
		sink.part(lid, poseStack, light, RenderType.cutout());
		poseStack.popPose();
	}
}
