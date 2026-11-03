package com.comedymonkey.createaddons.compat.curios.client;

import com.comedymonkey.createaddons.client.jetpack.JetpackWorn;
import com.comedymonkey.createaddons.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Draws a Clockwork Jetpack worn in the Curios Jetpack slot on its wearer's back (see {@link JetpackWorn}). */
public class JetpackCurioRenderer implements ICurioRenderer {
	/** Only called with Curios installed. */
	public static void register() {
		CuriosRendererRegistry.register(ModItems.CLOCKWORK_JETPACK.get(), JetpackCurioRenderer::new);
		CuriosRendererRegistry.register(ModItems.NETHERITE_CLOCKWORK_JETPACK.get(), JetpackCurioRenderer::new);
	}

	@Override
	public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack poseStack,
		RenderLayerParent<T, M> renderLayerParent, MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount,
		float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
		// Curios draws its slots on invisible wearers too
		if (renderLayerParent.getModel() instanceof HumanoidModel<?> model && !slotContext.entity().isInvisible())
			JetpackWorn.render(slotContext.entity(), stack, model, poseStack, buffers, light, partialTicks);
	}
}
