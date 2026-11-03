package com.comedymonkey.createaddons.client.jetpack;

import com.comedymonkey.createaddons.content.jetpack.Jetpacks;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Draws a Clockwork Jetpack worn in the chest slot (jetpack spec D18: without Curios, or by choice) on the wearer's back:
 * players, armour stands and humanoid mobs alike. One in the Jetpack slot takes precedence: then this draws nothing, as
 * only that one flies (D23).
 */
public class JetpackLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
	public JetpackLayer(RenderLayerParent<T, M> parent) {
		super(parent);
	}

	public static void addLayers(EntityRenderersEvent.AddLayers event) {
		for (PlayerSkin.Model skin : event.getSkins())
			add(event.getSkin(skin));
		for (EntityType<?> type : event.getEntityTypes())
			add(event.getRenderer(type));
	}

	/** Any living renderer whose model is a humanoid's: players, armour stands, zombies, skeletons, piglins... */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void add(EntityRenderer<?> renderer) {
		if (renderer instanceof LivingEntityRenderer living && living.getModel() instanceof HumanoidModel)
			living.addLayer(new JetpackLayer(living));
	}

	@Override
	public void render(PoseStack ms, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
					   float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
		ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
		if (!Jetpacks.isJetpack(chest) || Jetpacks.worn(entity) != chest || entity.isInvisible())
			return;
		JetpackWorn.render(entity, chest, getParentModel(), ms, buffers, light, partialTicks);
	}
}
