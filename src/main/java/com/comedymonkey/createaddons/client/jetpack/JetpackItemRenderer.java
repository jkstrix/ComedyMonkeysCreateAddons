package com.comedymonkey.createaddons.client.jetpack;

import com.comedymonkey.createaddons.client.parts.PartSink;
import com.comedymonkey.createaddons.content.jetpack.Jetpacks;
import com.comedymonkey.createaddons.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * The Clockwork Jetpack as an item: the jetpack itself, wings folded, its porthole showing how wound it is, drawn from its
 * part models (the item model, builtin/entity, sets the angles). Enchanted, it shines.
 */
public class JetpackItemRenderer extends BlockEntityWithoutLevelRenderer {
	public JetpackItemRenderer() {
		super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack ms, MultiBufferSource buffers, int light, int overlay) {
		JetpackModel.Variant variant = stack.is(ModItems.NETHERITE_CLOCKWORK_JETPACK.get()) ? JetpackModel.Variant.NETHERITE : JetpackModel.Variant.CLOCKWORK;
		boolean foil = stack.hasFoil();
		PartSink sink = (model, pose, partLight, type, rgb) -> Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(),
			foil ? ItemRenderer.getFoilBufferDirect(buffers, type, true, true) : buffers.getBuffer(type), null,
			Minecraft.getInstance().getModelManager().getModel(model), 1, 1, 1, partLight, overlay, ModelData.EMPTY, RenderType.cutout());
		JetpackModel.draw(ms, sink, light, Sheets.cutoutBlockSheet(), variant, JetpackModel.Pose.still(Jetpacks.fill(stack)));
	}
}
