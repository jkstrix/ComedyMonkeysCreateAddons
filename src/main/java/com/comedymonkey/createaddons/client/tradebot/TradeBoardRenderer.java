package com.comedymonkey.createaddons.client.tradebot;

import com.comedymonkey.createaddons.ComedyMonkeysCreateAddons;
import com.comedymonkey.createaddons.client.parts.PartSink;
import com.comedymonkey.createaddons.content.tradebot.board.TradeBoardEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * Draws a Trade Board (design/briefs/trade-board.md) from its model, exported in the board's own space (hung on a
 * north-facing face, the container's face at z 16), turned to the face it hangs on.
 */
public class TradeBoardRenderer extends EntityRenderer<TradeBoardEntity> {
	public static final ModelResourceLocation BODY = ModelResourceLocation.standalone(ComedyMonkeysCreateAddons.asResource("block/trade_board"));

	public TradeBoardRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	public static void registerModels(ModelEvent.RegisterAdditional event) {
		event.register(BODY);
	}

	@Override
	public void render(TradeBoardEntity board, float yaw, float partialTick, PoseStack ms, MultiBufferSource buffers, int light) {
		BlockPos pos = board.getPos();
		ms.pushPose();
		// from the entity's position to the block it hangs in, then turned about the block's centre to its face
		ms.translate(pos.getX() - board.getX(), pos.getY() - board.getY(), pos.getZ() - board.getZ());
		ms.translate(0.5, 0.5, 0.5);
		switch (board.getDirection()) {
			case SOUTH -> ms.mulPose(Axis.YP.rotationDegrees(180));
			case EAST -> ms.mulPose(Axis.YP.rotationDegrees(-90));
			case WEST -> ms.mulPose(Axis.YP.rotationDegrees(90));
			case UP -> ms.mulPose(Axis.XP.rotationDegrees(90));
			case DOWN -> ms.mulPose(Axis.XP.rotationDegrees(-90));
			default -> {}
		}
		ms.translate(-0.5, -0.5, -0.5);
		PartSink.screen(buffers, Blocks.AIR.defaultBlockState(), OverlayTexture.NO_OVERLAY).part(BODY, ms, light);
		ms.popPose();
		super.render(board, yaw, partialTick, ms, buffers, light);
	}

	@Override
	public ResourceLocation getTextureLocation(TradeBoardEntity board) {
		return InventoryMenu.BLOCK_ATLAS;
	}
}
