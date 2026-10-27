package com.comedymonkey.createaddons.client.helperbot;

import com.comedymonkey.createaddons.content.tradebot.clip.Clip;
import com.comedymonkey.createaddons.registry.ModDataComponents;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** The Helper Bot item in 3D, standing at rest in his colour, facing out. */
public class HelperBotItemRenderer extends BlockEntityWithoutLevelRenderer {
	public HelperBotItemRenderer() {
		super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
		HelperBotModels.Baked model = HelperBotModels.baked();
		HelperBotRig rig = model.rig();
		rig.reset();
		Clip idle = HelperBotModels.clips().clip("idle");
		rig.pose(idle, 0, null, 0, 1);
		rig.hideCollapsed();
		HelperBotModels.draw(ms, buffers, light, overlay, Direction.SOUTH, stack.get(ModDataComponents.HELPER_BOT_DYE.get()), model);
	}
}
